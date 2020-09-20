/**
 *
 * Copyright 2019-2020 Florian Schmaus
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jivesoftware.smack.tcp;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;

import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.SecurityRequiredByClientException;
import org.jivesoftware.smack.SmackException.SecurityRequiredByServerException;
import org.jivesoftware.smack.SmackException.SmackCertificateException;
import org.jivesoftware.smack.SmackFuture;
import org.jivesoftware.smack.SmackFuture.InternalSmackFuture;
import org.jivesoftware.smack.SmackReactor.SelectionKeyAttachment;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.XmppInputOutputFilter;
import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnection.ConnectedButUnauthenticatedStateDescriptor;
import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnection.LookupRemoteConnectionEndpointsStateDescriptor;
import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnectionModule;
import org.jivesoftware.smack.c2s.XmppClientToServerTransport;
import org.jivesoftware.smack.c2s.internal.ModularXmppClientToServerConnectionInternal;
import org.jivesoftware.smack.c2s.internal.WalkStateGraphContext;
import org.jivesoftware.smack.debugger.SmackDebugger;
import org.jivesoftware.smack.fsm.State;
import org.jivesoftware.smack.fsm.StateDescriptor;
import org.jivesoftware.smack.fsm.StateTransitionResult;
import org.jivesoftware.smack.internal.SmackTlsContext;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.packet.StartTls;
import org.jivesoftware.smack.packet.StreamOpen;
import org.jivesoftware.smack.packet.TlsFailure;
import org.jivesoftware.smack.packet.TlsProceed;
import org.jivesoftware.smack.packet.TopLevelStreamElement;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.tcp.XmppTcpTransportModule.XmppTcpNioTransport.DiscoveredTcpEndpoints;
import org.jivesoftware.smack.tcp.rce.RemoteXmppTcpConnectionEndpoints;
import org.jivesoftware.smack.tcp.rce.RemoteXmppTcpConnectionEndpoints.Result;
import org.jivesoftware.smack.tcp.rce.Rfc6120TcpRemoteConnectionEndpoint;
import org.jivesoftware.smack.util.CollectionUtil;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.UTF8;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jivesoftware.smack.util.rce.RemoteConnectionEndpointLookupFailure;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.jxmpp.jid.Jid;
import org.jxmpp.jid.util.JidUtil;
import org.jxmpp.xml.splitter.Utf8ByteXmppXmlSplitter;
import org.jxmpp.xml.splitter.XmlPrettyPrinter;
import org.jxmpp.xml.splitter.XmlPrinter;
import org.jxmpp.xml.splitter.XmppElementCallback;
import org.jxmpp.xml.splitter.XmppXmlSplitter;

public class XmppTcpTransportModule extends ModularXmppClientToServerConnectionModule<XmppTcpTransportModuleDescriptor> {

    private static final Logger LOGGER = Logger.getLogger(XmppTcpTransportModule.class.getName());

    private static final int CALLBACK_MAX_BYTES_READ = 10 * 1024 * 1024;
    private static final int CALLBACK_MAX_BYTES_WRITEN = CALLBACK_MAX_BYTES_READ;

    private static final int MAX_ELEMENT_SIZE = 64 * 1024;

    private final XmppTcpNioTransport tcpNioTransport;

    private SelectionKey selectionKey;
    private SelectionKeyAttachment selectionKeyAttachment;
    private SocketChannel socketChannel;
    private InetSocketAddress remoteAddress;

    private TlsState tlsState;

    private Iterator<CharSequence> outgoingCharSequenceIterator;

    private final List<TopLevelStreamElement> currentlyOutgoingElements = new ArrayList<>();
    private final Map<ByteBuffer, List<TopLevelStreamElement>> bufferToElementMap = new IdentityHashMap<>();

    private ByteBuffer outgoingBuffer;
    private ByteBuffer filteredOutgoingBuffer;
    private final List<ByteBuffer> networkOutgoingBuffers = new ArrayList<>();
    private long networkOutgoingBuffersBytes;

    // TODO: Make the size of the incomingBuffer configurable.
    private final ByteBuffer incomingBuffer = ByteBuffer.allocateDirect(2 * 4096);

    private final ReentrantLock channelSelectedCallbackLock = new ReentrantLock();

    private long totalBytesRead;
    private long totalBytesWritten;
    private long totalBytesReadAfterFilter;
    private long totalBytesWrittenBeforeFilter;
    private long handledChannelSelectedCallbacks;
    private long callbackPreemtBecauseBytesWritten;
    private long callbackPreemtBecauseBytesRead;
    private int sslEngineDelegatedTasks;
    private int maxPendingSslEngineDelegatedTasks;

    // TODO: Use LongAdder once Smack's minimum Android API level is 24 or higher.
    private final AtomicLong setWriteInterestAfterChannelSelectedCallback = new AtomicLong();
    private final AtomicLong reactorThreadAlreadyRacing = new AtomicLong();
    private final AtomicLong afterOutgoingElementsQueueModifiedSetInterestOps = new AtomicLong();
    private final AtomicLong rejectedChannelSelectedCallbacks = new AtomicLong();

    private Jid lastDestinationAddress;

    private boolean pendingInputFilterData;
    private boolean pendingOutputFilterData;

    private boolean pendingWriteInterestAfterRead;

    /**
     * Note that this field is effective final, but due to https://stackoverflow.com/q/30360824/194894 we have to declare it non-final.
     */
    private Utf8ByteXmppXmlSplitter splitter;

    /**
     * Note that this field is effective final, but due to https://stackoverflow.com/q/30360824/194894 we have to declare it non-final.
     */
    private XmppXmlSplitter outputDebugSplitter;

    private static final Level STREAM_OPEN_CLOSE_DEBUG_LOG_LEVEL = Level.FINER;

    XmppTcpTransportModule(XmppTcpTransportModuleDescriptor moduleDescriptor, ModularXmppClientToServerConnectionInternal connectionInternal) {
        super(moduleDescriptor, connectionInternal);

        tcpNioTransport = new XmppTcpNioTransport(connectionInternal);

        XmlPrinter incomingDebugPrettyPrinter = null;
        final SmackDebugger debugger = connectionInternal.smackDebugger;
        if (debugger != null) {
            // Incoming stream debugging.
            incomingDebugPrettyPrinter = XmlPrettyPrinter.builder()
                    .setPrettyWriter(sb -> debugger.incomingStreamSink(sb))
                    .build();

            // Outgoing stream debugging.
            XmlPrinter outgoingDebugPrettyPrinter = XmlPrettyPrinter.builder()
                    .setPrettyWriter(sb -> debugger.outgoingStreamSink(sb))
                    .build();
            outputDebugSplitter = new XmppXmlSplitter(outgoingDebugPrettyPrinter);
        }

        XmppXmlSplitter xmppXmlSplitter = new XmppXmlSplitter(MAX_ELEMENT_SIZE, xmppElementCallback,
                incomingDebugPrettyPrinter);
        splitter = new Utf8ByteXmppXmlSplitter(xmppXmlSplitter);
    }

    private final XmppElementCallback xmppElementCallback = new XmppElementCallback() {
        private String streamOpen;
        private String streamClose;

        @Override
        public void onCompleteElement(String completeElement) {
            assert streamOpen != null;
            assert streamClose != null;

            connectionInternal.withSmackDebugger(debugger -> debugger.onIncomingElementCompleted());

            String wrappedCompleteElement = streamOpen + completeElement + streamClose;
            connectionInternal.parseAndProcessElement(wrappedCompleteElement);
        }


        @Override
        public void streamOpened(String prefix, Map<String, String> attributes) {
            if (LOGGER.isLoggable(STREAM_OPEN_CLOSE_DEBUG_LOG_LEVEL)) {
                LOGGER.log(STREAM_OPEN_CLOSE_DEBUG_LOG_LEVEL,
                                "Stream of " + this + " opened. prefix=" + prefix + " attributes=" + attributes);
            }

            final String prefixXmlns = "xmlns:" + prefix;
            final StringBuilder streamClose = new StringBuilder(32);
            final StringBuilder streamOpen = new StringBuilder(256);

            streamOpen.append('<');
            streamClose.append("</");
            if (StringUtils.isNotEmpty(prefix)) {
                streamOpen.append(prefix).append(':');
                streamClose.append(prefix).append(':');
            }
            streamOpen.append("stream");
            streamClose.append("stream>");
            for (Entry<String, String> entry : attributes.entrySet()) {
                String attributeName = entry.getKey();
                String attributeValue = entry.getValue();
                switch (attributeName) {
                case "to":
                case "from":
                case "id":
                case "version":
                    break;
                case "xml:lang":
                    streamOpen.append(" xml:lang='").append(attributeValue).append('\'');
                    break;
                case "xmlns":
                    streamOpen.append(" xmlns='").append(attributeValue).append('\'');
                    break;
                default:
                    if (attributeName.equals(prefixXmlns)) {
                        streamOpen.append(' ').append(prefixXmlns).append("='").append(attributeValue).append('\'');
                        break;
                    }
                    LOGGER.info("Unknown <stream/> attribute: " + attributeName);
                    break;
                }
            }
            streamOpen.append('>');

            this.streamOpen = streamOpen.toString();
            this.streamClose = streamClose.toString();

            XmlPullParser streamOpenParser;
            try {
                streamOpenParser = PacketParserUtils.getParserFor(this.streamOpen);
            } catch (XmlPullParserException | IOException e) {
                // Should never happen.
                throw new AssertionError(e);
            }
            connectionInternal.onStreamOpen(streamOpenParser);
        }

        @Override
        public void streamClosed() {
            if (LOGGER.isLoggable(STREAM_OPEN_CLOSE_DEBUG_LOG_LEVEL)) {
                LOGGER.log(STREAM_OPEN_CLOSE_DEBUG_LOG_LEVEL, "Stream of " + this + " closed");
            }

           connectionInternal.onStreamClosed();
        }
    };

    private void onChannelSelected(SelectableChannel selectedChannel, SelectionKey selectedSelectionKey) {
        assert selectionKey == null || selectionKey == selectedSelectionKey;
        SocketChannel selectedSocketChannel = (SocketChannel) selectedChannel;
        // We are *always* interested in OP_READ.
        int newInterestedOps = SelectionKey.OP_READ;
        boolean newPendingOutputFilterData = false;

        if (!channelSelectedCallbackLock.tryLock()) {
            rejectedChannelSelectedCallbacks.incrementAndGet();
            return;
        }

        handledChannelSelectedCallbacks++;

        long callbackBytesRead = 0;
        long callbackBytesWritten = 0;

        try {
            boolean destinationAddressChanged = false;
            boolean isLastPartOfElement = false;
            TopLevelStreamElement currentlyOutgonigTopLevelStreamElement = null;
            StringBuilder outgoingStreamForDebugger = null;

            writeLoop: while (true) {
                final boolean moreDataAvailable = !isLastPartOfElement || !connectionInternal.outgoingElementsQueue.isEmpty();

                if (filteredOutgoingBuffer != null || !networkOutgoingBuffers.isEmpty()) {
                    if (filteredOutgoingBuffer != null) {
                        networkOutgoingBuffers.add(filteredOutgoingBuffer);
                        networkOutgoingBuffersBytes += filteredOutgoingBuffer.remaining();

                        filteredOutgoingBuffer = null;
                        if (moreDataAvailable && networkOutgoingBuffersBytes < 8096) {
                            continue;
                        }
                    }

                    ByteBuffer[] output = networkOutgoingBuffers.toArray(new ByteBuffer[networkOutgoingBuffers.size()]);
                    long bytesWritten;
                    try {
                        bytesWritten = selectedSocketChannel.write(output);
                    } catch (IOException e) {
                        // We have seen here so far
                        // - IOException "Broken pipe"
                        handleReadWriteIoException(e);
                        break;
                    }

                    if (bytesWritten == 0) {
                        newInterestedOps |= SelectionKey.OP_WRITE;
                        break;
                    }

                    callbackBytesWritten += bytesWritten;

                    networkOutgoingBuffersBytes -= bytesWritten;

                    List<? extends Buffer> prunedBuffers = pruneBufferList(networkOutgoingBuffers);

                    for (Buffer prunedBuffer : prunedBuffers) {
                        List<TopLevelStreamElement> sendElements = bufferToElementMap.remove(prunedBuffer);
                        if (sendElements == null) {
                            continue;
                        }
                        for (TopLevelStreamElement elementJustSend : sendElements) {
                            connectionInternal.fireFirstLevelElementSendListeners(elementJustSend);
                        }
                    }

                    // Prevent one callback from dominating the reactor thread. Break out of the write-loop if we have
                    // written a certain amount.
                    if (callbackBytesWritten > CALLBACK_MAX_BYTES_WRITEN) {
                        newInterestedOps |= SelectionKey.OP_WRITE;
                        callbackPreemtBecauseBytesWritten++;
                        break;
                    }
                } else if (outgoingBuffer != null || pendingOutputFilterData) {
                    pendingOutputFilterData = false;

                    if (outgoingBuffer != null) {
                        totalBytesWrittenBeforeFilter += outgoingBuffer.remaining();
                        if (isLastPartOfElement) {
                            assert currentlyOutgonigTopLevelStreamElement != null;
                            currentlyOutgoingElements.add(currentlyOutgonigTopLevelStreamElement);
                        }
                    }

                    ByteBuffer outputFilterInputData = outgoingBuffer;
                    // We can now null the outgoingBuffer since the filter step will take care of it from now on.
                    outgoingBuffer = null;

                    for (ListIterator<XmppInputOutputFilter> it = connectionInternal.getXmppInputOutputFilterBeginIterator(); it.hasNext();) {
                        XmppInputOutputFilter inputOutputFilter = it.next();
                        XmppInputOutputFilter.OutputResult outputResult;
                        try {
                            outputResult = inputOutputFilter.output(outputFilterInputData, isLastPartOfElement,
                                    destinationAddressChanged, moreDataAvailable);
                        } catch (IOException e) {
                            connectionInternal.notifyConnectionError(e);
                            break writeLoop;
                        }
                        newPendingOutputFilterData |= outputResult.pendingFilterData;
                        outputFilterInputData = outputResult.filteredOutputData;
                        if (outputFilterInputData != null) {
                            outputFilterInputData.flip();
                        }
                    }

                    // It is ok if outpuFilterInputData is 'null' here, this is expected behavior.
                    if (outputFilterInputData != null && outputFilterInputData.hasRemaining()) {
                        filteredOutgoingBuffer = outputFilterInputData;
                    } else {
                        filteredOutgoingBuffer = null;
                    }

                    // If the filters did eventually not produce any output data but if there is
                    // pending output data then we have a pending write request after read.
                    if (filteredOutgoingBuffer == null && newPendingOutputFilterData) {
                        pendingWriteInterestAfterRead = true;
                    }

                    if (filteredOutgoingBuffer != null && isLastPartOfElement) {
                        bufferToElementMap.put(filteredOutgoingBuffer, new ArrayList<>(currentlyOutgoingElements));
                        currentlyOutgoingElements.clear();
                    }

                    // Reset that the destination address has changed.
                    if (destinationAddressChanged) {
                        destinationAddressChanged = false;
                    }
                } else if (outgoingCharSequenceIterator != null) {
                    CharSequence nextCharSequence = outgoingCharSequenceIterator.next();
                    outgoingBuffer = UTF8.encode(nextCharSequence);
                    if (!outgoingCharSequenceIterator.hasNext()) {
                        outgoingCharSequenceIterator = null;
                        isLastPartOfElement = true;
                    } else {
                        isLastPartOfElement = false;
                    }

                    final SmackDebugger debugger = connectionInternal.smackDebugger;
                    if (debugger != null) {
                        if (outgoingStreamForDebugger == null) {
                            outgoingStreamForDebugger = new StringBuilder();
                        }
                        outgoingStreamForDebugger.append(nextCharSequence);

                        if (isLastPartOfElement) {
                            try {
                                outputDebugSplitter.append(outgoingStreamForDebugger);
                            } catch (IOException e) {
                                throw new AssertionError(e);
                            }
                            debugger.onOutgoingElementCompleted();
                            outgoingStreamForDebugger = null;
                        }
                    }
                } else if (!connectionInternal.outgoingElementsQueue.isEmpty()) {
                    currentlyOutgonigTopLevelStreamElement = connectionInternal.outgoingElementsQueue.poll();
                    if (currentlyOutgonigTopLevelStreamElement instanceof Stanza) {
                        Stanza currentlyOutgoingStanza = (Stanza) currentlyOutgonigTopLevelStreamElement;
                        Jid currentDestinationAddress = currentlyOutgoingStanza.getTo();
                        destinationAddressChanged = !JidUtil.equals(lastDestinationAddress, currentDestinationAddress);
                        lastDestinationAddress = currentDestinationAddress;
                    }
                    CharSequence nextCharSequence = currentlyOutgonigTopLevelStreamElement.toXML(StreamOpen.CLIENT_NAMESPACE);
                    if (nextCharSequence instanceof XmlStringBuilder) {
                        XmlStringBuilder xmlStringBuilder = (XmlStringBuilder) nextCharSequence;
                        XmlEnvironment outgoingStreamXmlEnvironment = connectionInternal.getOutgoingStreamXmlEnvironment();
                        outgoingCharSequenceIterator = xmlStringBuilder.toList(outgoingStreamXmlEnvironment).iterator();
                    } else {
                        outgoingCharSequenceIterator = Collections.singletonList(nextCharSequence).iterator();
                    }
                    assert outgoingCharSequenceIterator != null;
                } else {
                    // There is nothing more to write.
                    break;
                }
            }

            pendingOutputFilterData = newPendingOutputFilterData;
            if (!pendingWriteInterestAfterRead && pendingOutputFilterData) {
                newInterestedOps |= SelectionKey.OP_WRITE;
            }

            readLoop: while (true) {
                // Prevent one callback from dominating the reactor thread. Break out of the read-loop if we have
                // read a certain amount.
                if (callbackBytesRead > CALLBACK_MAX_BYTES_READ) {
                    callbackPreemtBecauseBytesRead++;
                    break;
                }

                int bytesRead;
                incomingBuffer.clear();
                try {
                    bytesRead = selectedSocketChannel.read(incomingBuffer);
                } catch (IOException e) {
                    handleReadWriteIoException(e);
                    return;
                }

                if (bytesRead < 0) {
                    LOGGER.finer("NIO read() returned " + bytesRead
                            + " for " + this + ". This probably means that the TCP connection was terminated.");
                    // According to the socket channel javadoc section about "asynchronous reads" a socket channel's
                    // read() may return -1 if the input side of a socket is shut down.
                     // Note that we do not call notifyConnectionError() here because the connection may be
                    // cleanly shutdown which would also cause read() to return '-1. I assume that this socket
                    // will be selected again, on which read() would throw an IOException, which will be catched
                    // and invoke notifyConnectionError() (see a few lines above).
                    /*
                    IOException exception = new IOException("NIO read() returned " + bytesRead);
                    notifyConnectionError(exception);
                    */
                    return;
                }

                if (!pendingInputFilterData) {
                    if (bytesRead == 0) {
                        // Nothing more to read.
                        break;
                    }
                } else {
                    pendingInputFilterData = false;
                }

                if (pendingWriteInterestAfterRead) {
                    // We have successfully read something and someone announced a write interest after a read. It is
                    // now possible that a filter is now also able to write additional data (for example SSLEngine).
                    pendingWriteInterestAfterRead = false;
                    newInterestedOps |= SelectionKey.OP_WRITE;
                }

                callbackBytesRead += bytesRead;

                ByteBuffer filteredIncomingBuffer = incomingBuffer;
                for (ListIterator<XmppInputOutputFilter> it = connectionInternal.getXmppInputOutputFilterEndIterator(); it.hasPrevious();) {
                    filteredIncomingBuffer.flip();

                    ByteBuffer newFilteredIncomingBuffer;
                    try {
                        newFilteredIncomingBuffer = it.previous().input(filteredIncomingBuffer);
                    } catch (IOException e) {
                        connectionInternal.notifyConnectionError(e);
                        return;
                    }
                    if (newFilteredIncomingBuffer == null) {
                        break readLoop;
                    }
                    filteredIncomingBuffer = newFilteredIncomingBuffer;
                }

                final int bytesReadAfterFilter = filteredIncomingBuffer.flip().remaining();

                totalBytesReadAfterFilter += bytesReadAfterFilter;

                try {
                    splitter.write(filteredIncomingBuffer);
                } catch (IOException e) {
                    connectionInternal.notifyConnectionError(e);
                    return;
                }
            }
        } finally {
            totalBytesWritten += callbackBytesWritten;
            totalBytesRead += callbackBytesRead;

            channelSelectedCallbackLock.unlock();
        }

        // Indicate that there is no reactor thread racing towards handling this selection key.
        final SelectionKeyAttachment selectionKeyAttachment = this.selectionKeyAttachment;
        if (selectionKeyAttachment != null) {
            selectionKeyAttachment.resetReactorThreadRacing();
        }

        // Check the queue again to prevent lost wakeups caused by elements inserted before we
        // called resetReactorThreadRacing() a few lines above.
        if (!connectionInternal.outgoingElementsQueue.isEmpty()) {
            setWriteInterestAfterChannelSelectedCallback.incrementAndGet();
            newInterestedOps |= SelectionKey.OP_WRITE;
        }

        connectionInternal.setInterestOps(selectionKey, newInterestedOps);
    }

    private void handleReadWriteIoException(IOException e) {
        if (e instanceof ClosedChannelException && !tcpNioTransport.isConnected()) {
            // The connection is already closed.
            return;
        }

       connectionInternal.notifyConnectionError(e);
    }

    /**
     * This is the interface between the "lookup remote connection endpoints" state and the "establish TCP connection"
     * state. The field is indirectly populated by {@link XmppTcpNioTransport#lookupConnectionEndpoints()} and consumed
     * by {@link ConnectionAttemptState}.
     */
    DiscoveredTcpEndpoints discoveredTcpEndpoints;

    final class XmppTcpNioTransport extends XmppClientToServerTransport {

        protected XmppTcpNioTransport(ModularXmppClientToServerConnectionInternal connectionInternal) {
            super(connectionInternal);
        }

        @Override
        protected void resetDiscoveredConnectionEndpoints() {
            discoveredTcpEndpoints = null;
        }

        @Override
        protected List<SmackFuture<LookupConnectionEndpointsResult, Exception>> lookupConnectionEndpoints() {
            // Assert that there are no stale discovered endpoints prior performing the lookup.
            assert discoveredTcpEndpoints == null;

            List<SmackFuture<LookupConnectionEndpointsResult, Exception>> futures = new ArrayList<>(2);

            InternalSmackFuture<LookupConnectionEndpointsResult, Exception> tcpEndpointsLookupFuture = new InternalSmackFuture<>();
            connectionInternal.asyncGo(() -> {
                Result<Rfc6120TcpRemoteConnectionEndpoint> result = RemoteXmppTcpConnectionEndpoints.lookup(
                                connectionInternal.connection.getConfiguration());

                LookupConnectionEndpointsResult endpointsResult;
                if (result.discoveredRemoteConnectionEndpoints.isEmpty()) {
                    endpointsResult = new TcpEndpointDiscoveryFailed(result);
                } else {
                    endpointsResult = new DiscoveredTcpEndpoints(result);
                }
                tcpEndpointsLookupFuture.setResult(endpointsResult);
            });
            futures.add(tcpEndpointsLookupFuture);

            if (moduleDescriptor.isDirectTlsEnabled()) {
                // TODO: Implement this.
                throw new IllegalArgumentException("DirectTLS is not implemented yet");
            }

            return futures;
        }

        @Override
        protected void loadConnectionEndpoints(LookupConnectionEndpointsSuccess lookupConnectionEndpointsSuccess) {
            // The API contract stats that we will be given the instance we handed out with lookupConnectionEndpoints,
            // which must be of type DiscoveredTcpEndpoints here. Hence if we can not cast it, then there is an internal
            // Smack error.
            discoveredTcpEndpoints = (DiscoveredTcpEndpoints) lookupConnectionEndpointsSuccess;
        }

        @Override
        protected void afterFiltersClosed() {
            pendingInputFilterData = pendingOutputFilterData = true;
            afterOutgoingElementsQueueModified();
        }

        @Override
        protected void disconnect() {
            XmppTcpTransportModule.this.closeSocketAndCleanup();
        }

        @Override
        protected void notifyAboutNewOutgoingElements() {
            afterOutgoingElementsQueueModified();
        }

        @Override
        public SSLSession getSslSession() {
            TlsState tlsState = XmppTcpTransportModule.this.tlsState;
            if (tlsState == null) {
                return null;
            }

            return tlsState.engine.getSession();
        }

        @Override
        public boolean isConnected() {
            SocketChannel socketChannel = XmppTcpTransportModule.this.socketChannel;
            if (socketChannel == null) {
                return false;
            }

            return socketChannel.isConnected();
        }

        @Override
        public boolean isTransportSecured() {
            final TlsState tlsState = XmppTcpTransportModule.this.tlsState;
            return tlsState != null && tlsState.handshakeStatus == TlsHandshakeStatus.successful;
        }

        @Override
        public XmppTcpTransportModule.Stats getStats() {
            return XmppTcpTransportModule.this.getStats();
        }

        final class DiscoveredTcpEndpoints implements LookupConnectionEndpointsSuccess {
            final RemoteXmppTcpConnectionEndpoints.Result<Rfc6120TcpRemoteConnectionEndpoint> result;
            DiscoveredTcpEndpoints(RemoteXmppTcpConnectionEndpoints.Result<Rfc6120TcpRemoteConnectionEndpoint> result) {
                this.result = result;
            }
        }

        final class TcpEndpointDiscoveryFailed implements LookupConnectionEndpointsFailed {
            final List<RemoteConnectionEndpointLookupFailure> lookupFailures;
            TcpEndpointDiscoveryFailed(RemoteXmppTcpConnectionEndpoints.Result<Rfc6120TcpRemoteConnectionEndpoint> result) {
                lookupFailures = result.lookupFailures;
            }
        }
    }

    private void afterOutgoingElementsQueueModified() {
        final SelectionKeyAttachment selectionKeyAttachment = this.selectionKeyAttachment;
        if (selectionKeyAttachment != null && selectionKeyAttachment.isReactorThreadRacing()) {
            // A reactor thread is already racing to the channel selected callback and will take care of this.
            reactorThreadAlreadyRacing.incrementAndGet();
            return;
        }

        afterOutgoingElementsQueueModifiedSetInterestOps.incrementAndGet();

        // Add OP_WRITE to the interested Ops, since we have now new things to write. Note that this may cause
        // multiple reactor threads to race to the channel selected callback in case we perform this right after
        // a select() returned with this selection key in the selected-key set. Hence we use tryLock() in the
        // channel selected callback to keep the invariant that only exactly one thread is performing the
        // callback.
        // Note that we need to perform setInterestedOps() *without* holding the channelSelectedCallbackLock, as
        // otherwise the reactor thread racing to the channel selected callback may found the lock still locked, which
        // would result in the outgoingElementsQueue not being handled.
        connectionInternal.setInterestOps(selectionKey, SelectionKey.OP_WRITE | SelectionKey.OP_READ);
    }

    @Override
    protected XmppTcpNioTransport getTransport() {
        return tcpNioTransport;
    }

    static final class EstablishingTcpConnectionStateDescriptor extends StateDescriptor {
        private EstablishingTcpConnectionStateDescriptor() {
            super(XmppTcpTransportModule.EstablishingTcpConnectionState.class);
            addPredeccessor(LookupRemoteConnectionEndpointsStateDescriptor.class);
            addSuccessor(EstablishTlsStateDescriptor.class);
            addSuccessor(ConnectedButUnauthenticatedStateDescriptor.class);
        }

        @Override
        protected XmppTcpTransportModule.EstablishingTcpConnectionState constructState(ModularXmppClientToServerConnectionInternal connectionInternal) {
            XmppTcpTransportModule tcpTransportModule = connectionInternal.connection.getConnectionModuleFor(XmppTcpTransportModuleDescriptor.class);
            return tcpTransportModule.constructEstablishingTcpConnectionState(this, connectionInternal);
        }
    }

    private EstablishingTcpConnectionState constructEstablishingTcpConnectionState(
                    EstablishingTcpConnectionStateDescriptor stateDescriptor,
                    ModularXmppClientToServerConnectionInternal connectionInternal) {
        return new EstablishingTcpConnectionState(stateDescriptor, connectionInternal);
    }

    final class EstablishingTcpConnectionState extends State {
        private EstablishingTcpConnectionState(EstablishingTcpConnectionStateDescriptor stateDescriptor,
                        ModularXmppClientToServerConnectionInternal connectionInternal) {
            super(stateDescriptor, connectionInternal);
        }

        @Override
        public StateTransitionResult.AttemptResult transitionInto(WalkStateGraphContext walkStateGraphContext)
                        throws InterruptedException, IOException, SmackException, XMPPException {
            // The fields inetSocketAddress and failedAddresses are handed over from LookupHostAddresses to
            // ConnectingToHost.
            ConnectionAttemptState connectionAttemptState = new ConnectionAttemptState(connectionInternal, discoveredTcpEndpoints,
                    this);
            StateTransitionResult.Failure failure = connectionAttemptState.establishTcpConnection();
            if (failure != null) {
                return failure;
            }

            socketChannel = connectionAttemptState.socketChannel;
            remoteAddress = (InetSocketAddress) socketChannel.socket().getRemoteSocketAddress();

            selectionKey = connectionInternal.registerWithSelector(socketChannel, SelectionKey.OP_READ,
                            XmppTcpTransportModule.this::onChannelSelected);
            selectionKeyAttachment = (SelectionKeyAttachment) selectionKey.attachment();

            connectionInternal.setTransport(tcpNioTransport);

            connectionInternal.newStreamOpenWaitForFeaturesSequence("stream features after initial connection");

            return new TcpSocketConnectedResult(remoteAddress);
        }

        @Override
        public void resetState() {
            closeSocketAndCleanup();
        }
    }

    public static final class TcpSocketConnectedResult extends StateTransitionResult.Success {
        private final InetSocketAddress remoteAddress;

        private TcpSocketConnectedResult(InetSocketAddress remoteAddress) {
            super("TCP connection established to " + remoteAddress);
            this.remoteAddress = remoteAddress;
        }

        public InetSocketAddress getRemoteAddress() {
            return remoteAddress;
        }
    }

    public static final class TlsEstablishedResult extends StateTransitionResult.Success {

        private TlsEstablishedResult(SSLEngine sslEngine) {
            super("TLS established: " + sslEngine.getSession());
        }
    }

    static final class EstablishTlsStateDescriptor extends StateDescriptor {
        private EstablishTlsStateDescriptor() {
            super(XmppTcpTransportModule.EstablishTlsState.class, "RFC 6120 § 5");
            addSuccessor(ConnectedButUnauthenticatedStateDescriptor.class);
            declarePrecedenceOver(ConnectedButUnauthenticatedStateDescriptor.class);
        }

        @Override
        protected EstablishTlsState constructState(ModularXmppClientToServerConnectionInternal connectionInternal) {
            XmppTcpTransportModule tcpTransportModule = connectionInternal.connection.getConnectionModuleFor(XmppTcpTransportModuleDescriptor.class);
            return tcpTransportModule.constructEstablishingTlsState(this, connectionInternal);
        }
    }

    private EstablishTlsState constructEstablishingTlsState(
                    EstablishTlsStateDescriptor stateDescriptor,
                    ModularXmppClientToServerConnectionInternal connectionInternal) {
        return new EstablishTlsState(stateDescriptor, connectionInternal);
    }

    private final class EstablishTlsState extends State {
        private EstablishTlsState(EstablishTlsStateDescriptor stateDescriptor,
                        ModularXmppClientToServerConnectionInternal connectionInternal) {
            super(stateDescriptor, connectionInternal);
        }

        @Override
        public StateTransitionResult.TransitionImpossible isTransitionToPossible(WalkStateGraphContext walkStateGraphContext)
                throws SecurityRequiredByClientException, SecurityRequiredByServerException {
            StartTls startTlsFeature = connectionInternal.connection.getFeature(StartTls.class);
            SecurityMode securityMode = connectionInternal.connection.getConfiguration().getSecurityMode();

            switch (securityMode) {
            case required:
            case ifpossible:
                if (startTlsFeature == null) {
                    if (securityMode == SecurityMode.ifpossible) {
                        return new StateTransitionResult.TransitionImpossibleReason("Server does not announce support for TLS and we do not required it");
                    }
                    throw new SecurityRequiredByClientException();
                }
                // Allows transition by returning null.
                return null;
            case disabled:
                if (startTlsFeature != null && startTlsFeature.required()) {
                    throw new SecurityRequiredByServerException();
                }
                return new StateTransitionResult.TransitionImpossibleReason("TLS disabled in client settings and server does not require it");
            default:
                throw new AssertionError("Unknown security mode: " + securityMode);
            }
        }

        @Override
        public StateTransitionResult.AttemptResult transitionInto(WalkStateGraphContext walkStateGraphContext)
                        throws IOException, InterruptedException, SmackException, XMPPException {
            connectionInternal.sendAndWaitForResponse(StartTls.INSTANCE, TlsProceed.class, TlsFailure.class);

            SmackTlsContext smackTlsContext = connectionInternal.getSmackTlsContext();

            tlsState = new TlsState(smackTlsContext);
            connectionInternal.addXmppInputOutputFilter(tlsState);

            channelSelectedCallbackLock.lock();
            try {
                pendingOutputFilterData = true;
                // The beginHandshake() is possibly not really required here, but it does not hurt either.
                tlsState.engine.beginHandshake();
                tlsState.handshakeStatus = TlsHandshakeStatus.initiated;
            } finally {
                channelSelectedCallbackLock.unlock();
            }
            connectionInternal.setInterestOps(selectionKey, SelectionKey.OP_WRITE | SelectionKey.OP_READ);

            try {
                tlsState.waitForHandshakeFinished();
            } catch (CertificateException e) {
                throw new SmackCertificateException(e);
            }

            connectionInternal.newStreamOpenWaitForFeaturesSequence("stream features after TLS established");

            return new TlsEstablishedResult(tlsState.engine);
        }

        @Override
        public void resetState() {
            tlsState = null;
        }
    }

    private enum TlsHandshakeStatus {
        initial,
        initiated,
        successful,
        failed,
    }

    private static final Level SSL_ENGINE_DEBUG_LOG_LEVEL = Level.FINEST;

    private static void debugLogSslEngineResult(String operation, SSLEngineResult result) {
        if (!LOGGER.isLoggable(SSL_ENGINE_DEBUG_LOG_LEVEL)) {
            return;
        }

        LOGGER.log(SSL_ENGINE_DEBUG_LOG_LEVEL, "SSLEngineResult of " + operation + "(): " + result);
    }

    private final class TlsState implements XmppInputOutputFilter {

        private static final int MAX_PENDING_OUTPUT_BYTES = 8096;

        private final SmackTlsContext smackTlsContext;
        private final SSLEngine engine;

        private TlsHandshakeStatus handshakeStatus = TlsHandshakeStatus.initial;
        private SSLException handshakeException;

        private ByteBuffer myNetData;
        private ByteBuffer peerAppData;

        private final List<ByteBuffer> pendingOutputData = new ArrayList<>();
        private int pendingOutputBytes;
        private ByteBuffer pendingInputData;

        private final AtomicInteger pendingDelegatedTasks = new AtomicInteger();

        private long wrapInBytes;
        private long wrapOutBytes;

        private long unwrapInBytes;
        private long unwrapOutBytes;

        private TlsState(SmackTlsContext smackTlsContext) throws IOException {
            this.smackTlsContext = smackTlsContext;

            // Call createSSLEngine()'s variant with two parameters as this allows for TLS session resumption.

            // Note that it is not really clear what the value of peer host should be. It could be A) the XMPP service's
            // domainpart or B) the DNS name of the host we are connecting to (usually the DNS SRV RR target name). While
            // the javadoc of createSSLEngine(String, int) indicates with "Some cipher suites (such as Kerberos) require
            // remote hostname information, in which case peerHost needs to be specified." that A should be used. TLS
            // session resumption may would need or at least benefit from B. Variant A would also be required if the
            // String is used for certificate verification. And it appears at least likely that TLS session resumption
            // would not be hurt by using variant A. Therefore we currently use variant A.
            // TODO: Should we use the ACE representation of the XMPP service domain? Compare with f60e4055ec529f0b8160acedf13275592ab10a4b
            // If yes, then we should probably introduce getXmppServiceDomainAceEncodedIfPossible().
            String peerHost = connectionInternal.connection.getConfiguration().getXMPPServiceDomain().toString();
            engine = smackTlsContext.sslContext.createSSLEngine(peerHost, remoteAddress.getPort());
            engine.setUseClientMode(true);

            SSLSession session = engine.getSession();
            int applicationBufferSize = session.getApplicationBufferSize();
            int packetBufferSize = session.getPacketBufferSize();

            myNetData = ByteBuffer.allocateDirect(packetBufferSize);
            peerAppData = ByteBuffer.allocate(applicationBufferSize);
        }

        @Override
        public OutputResult output(ByteBuffer outputData, boolean isFinalDataOfElement, boolean destinationAddressChanged,
                boolean moreDataAvailable) throws SSLException {
            if (outputData != null) {
                pendingOutputData.add(outputData);
                pendingOutputBytes += outputData.remaining();
                if (moreDataAvailable && pendingOutputBytes < MAX_PENDING_OUTPUT_BYTES) {
                    return OutputResult.NO_OUTPUT;
                }
            }

            ByteBuffer[] outputDataArray = pendingOutputData.toArray(new ByteBuffer[pendingOutputData.size()]);

            myNetData.clear();

            while (true) {
                SSLEngineResult result;
                try {
                    result = engine.wrap(outputDataArray, myNetData);
                } catch (SSLException e) {
                    handleSslException(e);
                    throw e;
                }

                debugLogSslEngineResult("wrap", result);

                SSLEngineResult.Status engineResultStatus = result.getStatus();

                pendingOutputBytes -= result.bytesConsumed();

                if (engineResultStatus == SSLEngineResult.Status.OK) {
                    wrapInBytes += result.bytesConsumed();
                    wrapOutBytes += result.bytesProduced();

                    SSLEngineResult.HandshakeStatus handshakeStatus = handleHandshakeStatus(result);
                    switch (handshakeStatus) {
                        case NEED_UNWRAP:
                            // NEED_UNWRAP means that we need to receive something in order to continue the handshake. The
                            // standard channelSelectedCallback logic will take care of this, as there is eventually always
                            // a interest to read from the socket.
                            break;
                        case NEED_WRAP:
                            // Same as need task: Cycle the reactor.
                        case NEED_TASK:
                            // Note that we also set pendingOutputFilterData in the OutputResult in the NEED_TASK case, as
                            // we also want to retry the wrap() operation above in this case.
                            return new OutputResult(true, myNetData);
                        default:
                            break;
                    }
                }

                switch (engineResultStatus) {
                case OK:
                    // No need to outputData.compact() here, since we do not reuse the buffer.
                    // Clean up the pending output data.
                    pruneBufferList(pendingOutputData);
                    return new OutputResult(!pendingOutputData.isEmpty(), myNetData);
                case CLOSED:
                    pendingOutputData.clear();
                    return OutputResult.NO_OUTPUT;
                case BUFFER_OVERFLOW:
                    LOGGER.warning("SSLEngine status BUFFER_OVERFLOW, this is hopefully uncommon");
                    int outputDataRemaining = outputData != null ? outputData.remaining() : 0;
                    int newCapacity = (int) (1.3 * outputDataRemaining);
                    // If newCapacity would not increase myNetData, then double it.
                    if (newCapacity <= myNetData.capacity()) {
                        newCapacity = 2 * myNetData.capacity();
                    }
                    ByteBuffer newMyNetData = ByteBuffer.allocateDirect(newCapacity);
                    myNetData.flip();
                    newMyNetData.put(myNetData);
                    myNetData = newMyNetData;
                    continue;
                case BUFFER_UNDERFLOW:
                    throw new IllegalStateException(
                            "Buffer underflow as result of SSLEngine.wrap() should never happen");
                }
            }
        }

        @SuppressWarnings("ReferenceEquality")
        @Override
        public ByteBuffer input(ByteBuffer inputData) throws SSLException {
            ByteBuffer accumulatedData;
            if (pendingInputData == null) {
                accumulatedData = inputData;
            } else {
                assert pendingInputData != inputData;

                int accumulatedDataBytes = pendingInputData.remaining() + inputData.remaining();
                accumulatedData = ByteBuffer.allocate(accumulatedDataBytes);
                accumulatedData.put(pendingInputData)
                               .put(inputData)
                               .flip();
                pendingInputData = null;
            }

            peerAppData.clear();

            while (true) {
                SSLEngineResult result;
                try {
                    result = engine.unwrap(accumulatedData, peerAppData);
                } catch (SSLException e) {
                    handleSslException(e);
                    throw e;
                }

                debugLogSslEngineResult("unwrap", result);

                SSLEngineResult.Status engineResultStatus = result.getStatus();

                if (engineResultStatus == SSLEngineResult.Status.OK) {
                    unwrapInBytes += result.bytesConsumed();
                    unwrapOutBytes += result.bytesProduced();

                    SSLEngineResult.HandshakeStatus handshakeStatus = handleHandshakeStatus(result);
                    switch (handshakeStatus) {
                    case NEED_TASK:
                        // A delegated task is asynchronously running. Take care of the remaining accumulatedData.
                        addAsPendingInputData(accumulatedData);
                        // Return here, as the async task created by handleHandshakeStatus will continue calling the
                        // cannelSelectedCallback.
                        return null;
                    case NEED_UNWRAP:
                        continue;
                    case NEED_WRAP:
                        // NEED_WRAP means that the SSLEngine needs to send data, probably without consuming data.
                        // We exploit here the fact that the channelSelectedCallback is single threaded and that the
                        // input processing is after the output processing.
                        addAsPendingInputData(accumulatedData);
                        // Note that it is ok that we the provided argument for pending input filter data to channel
                        // selected callback is false, as setPendingInputFilterData() will have set the internal state
                        // boolean accordingly.
                        connectionInternal.asyncGo(() -> callChannelSelectedCallback(false, true));
                        // Do not break here, but instead return and let the asynchronously invoked
                        // callChannelSelectedCallback() do its work.
                        return null;
                    default:
                        break;
                    }
                }

                switch (engineResultStatus) {
                case OK:
                    // SSLEngine's unwrap() may not consume all bytes from the source buffer. If this is the case, then
                    // simply perform another unwrap until accumlatedData has no remaining bytes.
                    if (accumulatedData.hasRemaining()) {
                        continue;
                    }
                    return peerAppData;
                case CLOSED:
                    return null;
                case BUFFER_UNDERFLOW:
                    // There were not enough source bytes available to make a complete packet. Let it in
                    // pendingInputData. Note that we do not resize SSLEngine's source buffer - inputData in our case -
                    // as it is not possible.
                    addAsPendingInputData(accumulatedData);
                    return null;
                case BUFFER_OVERFLOW:
                    int applicationBufferSize = engine.getSession().getApplicationBufferSize();
                    assert peerAppData.remaining() < applicationBufferSize;
                    peerAppData = ByteBuffer.allocate(applicationBufferSize);
                    continue;
                }
            }
        }

        private void addAsPendingInputData(ByteBuffer byteBuffer) {
            // Note that we can not simply write
            // pendingInputData = byteBuffer;
            // we have to copy the provided byte buffer, because it is possible that this byteBuffer is re-used by some
            // higher layer. That is, here 'byteBuffer' is typically 'incomingBuffer', which is a direct buffer only
            // allocated once per connection for performance reasons and hence re-used for read() calls.
            pendingInputData = ByteBuffer.allocate(byteBuffer.remaining());
            pendingInputData.put(byteBuffer).flip();

            pendingInputFilterData = pendingInputData.hasRemaining();
        }

        private SSLEngineResult.HandshakeStatus handleHandshakeStatus(SSLEngineResult sslEngineResult) {
            SSLEngineResult.HandshakeStatus handshakeStatus = sslEngineResult.getHandshakeStatus();
            switch (handshakeStatus) {
            case NEED_TASK:
                while (true) {
                    final Runnable delegatedTask = engine.getDelegatedTask();
                    if (delegatedTask == null) {
                        break;
                    }
                    sslEngineDelegatedTasks++;
                    int currentPendingDelegatedTasks = pendingDelegatedTasks.incrementAndGet();
                    if (currentPendingDelegatedTasks > maxPendingSslEngineDelegatedTasks) {
                        maxPendingSslEngineDelegatedTasks = currentPendingDelegatedTasks;
                    }

                    Runnable wrappedDelegatedTask = () -> {
                        delegatedTask.run();
                        int wrappedCurrentPendingDelegatedTasks = pendingDelegatedTasks.decrementAndGet();
                        if (wrappedCurrentPendingDelegatedTasks == 0) {
                            callChannelSelectedCallback(true, true);
                        }
                    };
                    connectionInternal.asyncGo(wrappedDelegatedTask);
                }
                break;
            case FINISHED:
                onHandshakeFinished();
                break;
            default:
                break;
            }

            SSLEngineResult.HandshakeStatus afterHandshakeStatus = engine.getHandshakeStatus();
            return afterHandshakeStatus;
        }

        private void handleSslException(SSLException e) {
            handshakeException = e;
            handshakeStatus = TlsHandshakeStatus.failed;
            connectionInternal.notifyWaitingThreads();
        }

        private void onHandshakeFinished() {
            handshakeStatus = TlsHandshakeStatus.successful;
            connectionInternal.notifyWaitingThreads();
        }

        private boolean isHandshakeFinished() {
            return handshakeStatus == TlsHandshakeStatus.successful || handshakeStatus == TlsHandshakeStatus.failed;
        }

        private void waitForHandshakeFinished() throws InterruptedException, CertificateException, SSLException, SmackException, XMPPException {
            connectionInternal.waitForConditionOrThrowConnectionException(() -> isHandshakeFinished(), "TLS handshake to finish");

            if (handshakeStatus == TlsHandshakeStatus.failed) {
                throw handshakeException;
            }

            assert handshakeStatus == TlsHandshakeStatus.successful;

            if (smackTlsContext.daneVerifier != null) {
                smackTlsContext.daneVerifier.finish(engine.getSession());
            }
        }

        @Override
        public Object getStats() {
            return new TlsStateStats(this);
        }

        @Override
        public void closeInputOutput() {
            engine.closeOutbound();
            try {
                engine.closeInbound();
            } catch (SSLException e) {
                LOGGER.log(Level.FINEST,
                        "SSLException when closing inbound TLS session. This can likely be ignored if a possible truncation attack is suggested."
                        + " You may want to ask your XMPP server vendor to implement a clean TLS session shutdown sending close_notify after </stream>",
                        e);
            }
        }

        @Override
        public void waitUntilInputOutputClosed() throws IOException, CertificateException, InterruptedException,
                SmackException, XMPPException {
            waitForHandshakeFinished();
        }

        @Override
        public String getFilterName() {
            return "TLS (" + engine + ')';
        }
    }

    public static final class TlsStateStats {
        public final long wrapInBytes;
        public final long wrapOutBytes;
        public final double wrapRatio;

        public final long unwrapInBytes;
        public final long unwrapOutBytes;
        public final double unwrapRatio;

        private TlsStateStats(TlsState tlsState) {
            wrapOutBytes = tlsState.wrapOutBytes;
            wrapInBytes = tlsState.wrapInBytes;
            wrapRatio = (double) wrapOutBytes / wrapInBytes;

            unwrapOutBytes = tlsState.unwrapOutBytes;
            unwrapInBytes = tlsState.unwrapInBytes;
            unwrapRatio = (double) unwrapInBytes / unwrapOutBytes;
        }

        private transient String toStringCache;

        @Override
        public String toString() {
            if (toStringCache != null) {
                return toStringCache;
            }

            toStringCache =
                      "wrap-in-bytes: " + wrapInBytes + '\n'
                    + "wrap-out-bytes: " + wrapOutBytes + '\n'
                    + "wrap-ratio: " + wrapRatio + '\n'
                    + "unwrap-in-bytes: " + unwrapInBytes + '\n'
                    + "unwrap-out-bytes: " + unwrapOutBytes + '\n'
                    + "unwrap-ratio: " + unwrapRatio + '\n'
                    ;

            return toStringCache;
        }
    }

    private void callChannelSelectedCallback(boolean setPendingInputFilterData, boolean setPendingOutputFilterData) {
        final SocketChannel channel = socketChannel;
        final SelectionKey key = selectionKey;
        if (channel == null || key == null) {
            LOGGER.info("Not calling channel selected callback because the connection was eventually disconnected");
            return;
        }

        channelSelectedCallbackLock.lock();
        try {
            // Note that it is important that we send the pending(Input|Output)FilterData flags while holding the lock.
            if (setPendingInputFilterData) {
                pendingInputFilterData = true;
            }
            if (setPendingOutputFilterData) {
                pendingOutputFilterData = true;
            }

            onChannelSelected(channel, key);
        } finally {
            channelSelectedCallbackLock.unlock();
        }
    }

    private void closeSocketAndCleanup() {
        final SelectionKey selectionKey = this.selectionKey;
        if (selectionKey != null) {
            selectionKey.cancel();
        }
        final SocketChannel socketChannel = this.socketChannel;
        if (socketChannel != null) {
            try {
                socketChannel.close();
            } catch (IOException e) {

            }
        }

        this.selectionKey = null;
        this.socketChannel = null;

        selectionKeyAttachment = null;
        remoteAddress = null;
    }

    private static List<? extends Buffer> pruneBufferList(Collection<? extends Buffer> buffers) {
        return CollectionUtil.removeUntil(buffers, b -> b.hasRemaining());
    }

    public XmppTcpTransportModule.Stats getStats() {
        return new Stats(this);
    }

    public static final class Stats extends XmppClientToServerTransport.Stats {
        public final long totalBytesWritten;
        public final long totalBytesWrittenBeforeFilter;
        public final double writeRatio;

        public final long totalBytesRead;
        public final long totalBytesReadAfterFilter;
        public final double readRatio;

        public final long handledChannelSelectedCallbacks;
        public final long setWriteInterestAfterChannelSelectedCallback;
        public final long reactorThreadAlreadyRacing;
        public final long afterOutgoingElementsQueueModifiedSetInterestOps;
        public final long rejectedChannelSelectedCallbacks;
        public final long totalCallbackRequests;
        public final long callbackPreemtBecauseBytesWritten;
        public final long callbackPreemtBecauseBytesRead;
        public final int sslEngineDelegatedTasks;
        public final int maxPendingSslEngineDelegatedTasks;

        private Stats(XmppTcpTransportModule connection) {
            totalBytesWritten = connection.totalBytesWritten;
            totalBytesWrittenBeforeFilter = connection.totalBytesWrittenBeforeFilter;
            writeRatio = (double) totalBytesWritten / totalBytesWrittenBeforeFilter;

            totalBytesReadAfterFilter = connection.totalBytesReadAfterFilter;
            totalBytesRead = connection.totalBytesRead;
            readRatio = (double) totalBytesRead / totalBytesReadAfterFilter;

            handledChannelSelectedCallbacks = connection.handledChannelSelectedCallbacks;
            setWriteInterestAfterChannelSelectedCallback = connection.setWriteInterestAfterChannelSelectedCallback.get();
            reactorThreadAlreadyRacing = connection.reactorThreadAlreadyRacing.get();
            afterOutgoingElementsQueueModifiedSetInterestOps = connection.afterOutgoingElementsQueueModifiedSetInterestOps
                    .get();
            rejectedChannelSelectedCallbacks = connection.rejectedChannelSelectedCallbacks.get();

            totalCallbackRequests = handledChannelSelectedCallbacks + rejectedChannelSelectedCallbacks;

            callbackPreemtBecauseBytesRead = connection.callbackPreemtBecauseBytesRead;
            callbackPreemtBecauseBytesWritten = connection.callbackPreemtBecauseBytesWritten;

            sslEngineDelegatedTasks = connection.sslEngineDelegatedTasks;
            maxPendingSslEngineDelegatedTasks = connection.maxPendingSslEngineDelegatedTasks;
        }

        private transient String toStringCache;

        @Override
        public String toString() {
            if (toStringCache != null) {
                return toStringCache;
            }

            toStringCache =
              "Total bytes\n"
            + "recv: " + totalBytesRead + '\n'
            + "send: " + totalBytesWritten + '\n'
            + "recv-aft-filter: " + totalBytesReadAfterFilter + '\n'
            + "send-bef-filter: " + totalBytesWrittenBeforeFilter + '\n'
            + "read-ratio: " + readRatio + '\n'
            + "write-ratio: " + writeRatio + '\n'
            + "Events\n"
            + "total-callback-requests: " + totalCallbackRequests + '\n'
            + "handled-channel-selected-callbacks: " + handledChannelSelectedCallbacks + '\n'
            + "rejected-channel-selected-callbacks: " + rejectedChannelSelectedCallbacks + '\n'
            + "set-write-interest-after-callback: " + setWriteInterestAfterChannelSelectedCallback + '\n'
            + "reactor-thread-already-racing: " + reactorThreadAlreadyRacing + '\n'
            + "after-queue-modified-set-interest-ops: " + afterOutgoingElementsQueueModifiedSetInterestOps + '\n'
            + "callback-preemt-because-bytes-read: " + callbackPreemtBecauseBytesRead + '\n'
            + "callback-preemt-because-bytes-written: " + callbackPreemtBecauseBytesWritten + '\n'
            + "ssl-engine-delegated-tasks: " + sslEngineDelegatedTasks + '\n'
            + "max-pending-ssl-engine-delegated-tasks: " + maxPendingSslEngineDelegatedTasks + '\n'
            ;

            return toStringCache;
        }
    }
}
