/**
 *
 * Copyright 2018-2019 Florian Schmaus
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
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;

import org.jivesoftware.smack.AbstractXmppNioConnection;
import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.ConnectionException;
import org.jivesoftware.smack.SmackException.ConnectionUnexpectedTerminatedException;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.SmackException.SecurityRequiredByClientException;
import org.jivesoftware.smack.SmackException.SecurityRequiredByServerException;
import org.jivesoftware.smack.SmackException.SmackWrappedException;
import org.jivesoftware.smack.SmackReactor.ChannelSelectedCallback;
import org.jivesoftware.smack.SmackReactor.SelectionKeyAttachment;
import org.jivesoftware.smack.SynchronizationPoint;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.XMPPException.FailedNonzaException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.XmppInputOutputFilter;
import org.jivesoftware.smack.fsm.ConnectionStateEvent.DetailedTransitionIntoInformation;
import org.jivesoftware.smack.fsm.StateDescriptor;
import org.jivesoftware.smack.fsm.StateDescriptorGraph;
import org.jivesoftware.smack.fsm.StateDescriptorGraph.GraphVertex;
import org.jivesoftware.smack.packet.Nonza;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.packet.StartTls;
import org.jivesoftware.smack.packet.StreamClose;
import org.jivesoftware.smack.packet.StreamOpen;
import org.jivesoftware.smack.packet.TlsFailure;
import org.jivesoftware.smack.packet.TlsProceed;
import org.jivesoftware.smack.packet.TopLevelStreamElement;
import org.jivesoftware.smack.sasl.SASLErrorException;
import org.jivesoftware.smack.util.ArrayBlockingQueueWithShutdown;
import org.jivesoftware.smack.util.Async;
import org.jivesoftware.smack.util.CollectionUtil;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.UTF8;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jivesoftware.smack.util.dns.HostAddress;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.jxmpp.jid.DomainBareJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.util.JidUtil;
import org.jxmpp.stringprep.XmppStringprepException;
import org.jxmpp.xml.splitter.Utf8ByteXmppXmlSplitter;
import org.jxmpp.xml.splitter.XmlPrettyPrinter;
import org.jxmpp.xml.splitter.XmlPrinter;
import org.jxmpp.xml.splitter.XmppElementCallback;
import org.jxmpp.xml.splitter.XmppXmlSplitter;

/**
 * Represents and manages a client connection to an XMPP server via TCP.
 *
 * <h2>Smack XMPP TCP NIO connection states</h2>
 * <p>
 * The graph below shows the current graph of states of this XMPP connection. Only some states are final states, most
 * states are intermediate states in order to reach a final state.
 * </p>
 * <img src="doc-files/XmppNioTcpConnectionStateGraph.png" alt="The state graph of XmppNioTcpConnection">
 *
 */
public class XmppNioTcpConnection extends AbstractXmppNioConnection {

    private static final Logger LOGGER = Logger.getLogger(XmppNioTcpConnection.class.getName());

    private static final Set<Class<? extends StateDescriptor>> BACKWARD_EDGES_STATE_DESCRIPTORS = new HashSet<>();

    static final GraphVertex<StateDescriptor> INITIAL_STATE_DESCRIPTOR_VERTEX;

    static {
        BACKWARD_EDGES_STATE_DESCRIPTORS.add(LookupHostAddressesStateDescriptor.class);
        BACKWARD_EDGES_STATE_DESCRIPTORS.add(EnableStreamManagementStateDescriptor.class);
        BACKWARD_EDGES_STATE_DESCRIPTORS.add(ResumeStreamStateDescriptor.class);
        BACKWARD_EDGES_STATE_DESCRIPTORS.add(InstantStreamResumptionStateDescriptor.class);
        BACKWARD_EDGES_STATE_DESCRIPTORS.add(Bind2StateDescriptor.class);
        BACKWARD_EDGES_STATE_DESCRIPTORS.add(InstantShutdownStateDescriptor.class);
        BACKWARD_EDGES_STATE_DESCRIPTORS.add(ShutdownStateDescriptor.class);

        try {
            INITIAL_STATE_DESCRIPTOR_VERTEX = StateDescriptorGraph.constructStateDescriptorGraph(BACKWARD_EDGES_STATE_DESCRIPTORS);
        } catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                        | NoSuchMethodException | SecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    private static final int CALLBACK_MAX_BYTES_READ = 10 * 1024 * 1024;
    private static final int CALLBACK_MAX_BYTES_WRITEN = CALLBACK_MAX_BYTES_READ;

    private static final int MAX_ELEMENT_SIZE = 64 * 1024;

    private SelectionKey selectionKey;
    private SelectionKeyAttachment selectionKeyAttachment;
    private SocketChannel socketChannel;
    private InetSocketAddress remoteAddress;
    private TlsState tlsState;

    /**
     * Note that this field is effective final, but due to https://stackoverflow.com/q/30360824/194894 we have to declare it non-final.
     */
    private Utf8ByteXmppXmlSplitter splitter;

    /**
     * Note that this field is effective final, but due to https://stackoverflow.com/q/30360824/194894 we have to declare it non-final.
     */
    private XmppXmlSplitter outputDebugSplitter;

    private static final Level STREAM_OPEN_CLOSE_DEBUG_LOG_LEVEL = Level.FINER;

    private final XmppElementCallback xmppElementCallback = new XmppElementCallback() {
        private String streamOpen;
        private String streamClose;

        @Override
        public void onCompleteElement(String completeElement) {
            assert streamOpen != null;
            assert streamClose != null;

            if (debugger != null) {
                debugger.onIncomingElementCompleted();
            }

            String wrappedCompleteElement = streamOpen + completeElement + streamClose;
            try {
                parseAndProcessElement(wrappedCompleteElement);
            } catch (Exception e) {
                notifyConnectionError(e);
            }
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
                case "id":
                    streamId = attributeValue;
                    break;
                case "version":
                    break;
                case "xml:lang":
                    streamOpen.append(" xml:lang='").append(attributeValue).append('\'');
                    break;
                case "to":
                    break;
                case "from":
                    DomainBareJid reportedServerDomain;
                    try {
                        reportedServerDomain = JidCreate.domainBareFrom(attributeValue);
                    } catch (XmppStringprepException e) {
                        IllegalStateException ise = new IllegalStateException(
                                "Reporting server domain '" + attributeValue + "' is not a valid JID", e);
                        notifyConnectionError(ise);
                        return;
                    }
                    assert config.getXMPPServiceDomain().equals(reportedServerDomain);
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
            onStreamOpen(streamOpenParser);
        }

        @Override
        public void streamClosed() {
            if (LOGGER.isLoggable(STREAM_OPEN_CLOSE_DEBUG_LOG_LEVEL)) {
                LOGGER.log(STREAM_OPEN_CLOSE_DEBUG_LOG_LEVEL, "Stream of " + this + " closed");
            }

            closingStreamReceived.reportSuccess();
        }
    };

    private final ArrayBlockingQueueWithShutdown<TopLevelStreamElement> outgoingElementsQueue = new ArrayBlockingQueueWithShutdown<>(
                    100, true);

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

    private boolean useDirectTls = false;

    private boolean useSm = false;
    private boolean useSmResumption = false;
    private boolean useIsr = false;

    private boolean useBind2 = false;

    public XmppNioTcpConnection(XMPPTCPConnectionConfiguration configuration) {
        super(configuration, INITIAL_STATE_DESCRIPTOR_VERTEX);

        XmlPrinter incomingDebugPrettyPrinter = null;
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

    private final ChannelSelectedCallback channelSelectedCallback =
            (selectedChannel, selectedSelectionKey) -> {
        assert selectionKey == null || selectionKey == selectedSelectionKey;
        SocketChannel selectedSocketChannel = (SocketChannel) selectedChannel;
        // We are *always* interested in OP_READ.
        int newInterestedOps = SelectionKey.OP_READ;
        boolean newPendingOutputFilterData = false;

        if (!channelSelectedCallbackLock.tryLock()) {
            rejectedChannelSelectedCallbacks.incrementAndGet();
            return;
        }

        // LOGGER.info("Accepted channel selected callback");

        handledChannelSelectedCallbacks++;

        long callbackBytesRead = 0;
        long callbackBytesWritten = 0;

        try {
            boolean destinationAddressChanged = false;
            boolean isLastPartOfElement = false;
            TopLevelStreamElement currentlyOutgonigTopLevelStreamElement = null;
            StringBuilder outgoingStreamForDebugger = null;

            writeLoop: while (true) {
                final boolean moreDataAvailable = !isLastPartOfElement || !outgoingElementsQueue.isEmpty();

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
                            firePacketSendingListeners(elementJustSend);
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

                    for (ListIterator<XmppInputOutputFilter> it = getXmppInputOutputFilterBeginIterator(); it.hasNext();) {
                        XmppInputOutputFilter inputOutputFilter = it.next();
                        XmppInputOutputFilter.OutputResult outputResult;
                        try {
                            outputResult = inputOutputFilter.output(outputFilterInputData, isLastPartOfElement,
                                    destinationAddressChanged, moreDataAvailable);
                        } catch (IOException e) {
                            notifyConnectionError(e);
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
                } else if (!outgoingElementsQueue.isEmpty()) {
                    currentlyOutgonigTopLevelStreamElement = outgoingElementsQueue.poll();
                    if (currentlyOutgonigTopLevelStreamElement instanceof Stanza) {
                        Stanza currentlyOutgoingStanza = (Stanza) currentlyOutgonigTopLevelStreamElement;
                        Jid currentDestinationAddress = currentlyOutgoingStanza.getTo();
                        destinationAddressChanged = !JidUtil.equals(lastDestinationAddress, currentDestinationAddress);
                        lastDestinationAddress = currentDestinationAddress;
                    }
                    CharSequence nextCharSequence = currentlyOutgonigTopLevelStreamElement.toXML(StreamOpen.CLIENT_NAMESPACE);
                    if (nextCharSequence instanceof XmlStringBuilder) {
                        XmlStringBuilder xmlStringBuilder = (XmlStringBuilder) nextCharSequence;
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

                // We have successfully read something. It is now possible that a filter is now also able to write
                // additional data (for example SSLEngine).
                if (pendingWriteInterestAfterRead) {
                    pendingWriteInterestAfterRead = false;
                    newInterestedOps |= SelectionKey.OP_WRITE;
                }

                callbackBytesRead += bytesRead;

                ByteBuffer filteredIncomingBuffer = incomingBuffer;
                for (ListIterator<XmppInputOutputFilter> it = getXmppInputOutputFilterEndIterator(); it.hasPrevious();) {
                    filteredIncomingBuffer.flip();

                    ByteBuffer newFilteredIncomingBuffer;
                    try {
                        newFilteredIncomingBuffer = it.previous().input(filteredIncomingBuffer);
                    } catch (IOException e) {
                        notifyConnectionError(e);
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
                    notifyConnectionError(e);
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
        if (!outgoingElementsQueue.isEmpty()) {
            setWriteInterestAfterChannelSelectedCallback.incrementAndGet();
            newInterestedOps |= SelectionKey.OP_WRITE;
        }

        setInterestOps(selectionKey, newInterestedOps);
    };

    private void handleReadWriteIoException(IOException e) {
        if (e instanceof ClosedChannelException && !isConnected()) {
            // The connection is already closed.
            return;
        }

        notifyConnectionError(e);
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

            channelSelectedCallback.onChannelSelected(channel, key);
        } finally {
            channelSelectedCallbackLock.unlock();
        }
    }

    private abstract static class TcpHostEvent extends DetailedTransitionIntoInformation {
        protected final InetSocketAddress inetSocketAddress;

        protected TcpHostEvent(State state, InetSocketAddress inetSocketAddress) {
            super(state);
            this.inetSocketAddress = inetSocketAddress;
        }

        public InetSocketAddress getInetSocketAddress() {
            return inetSocketAddress;
        }

        @Override
        public String toString() {
            return super.toString() + ": " + inetSocketAddress;
        }
    }

    public static final class ConnectingToHostEvent extends TcpHostEvent {
        private ConnectingToHostEvent(State state, InetSocketAddress inetSocketAddress) {
            super(state, inetSocketAddress);
        }
    }

    public static final class ConnectedToHostEvent extends TcpHostEvent {
        private final boolean connectionEstablishedImmediately;

        private ConnectedToHostEvent(State state, InetSocketAddress inetSocketAddress, boolean immediately) {
            super(state, inetSocketAddress);
            this.connectionEstablishedImmediately = immediately;
        }

        @Override
        public String toString() {
            return super.toString() + (connectionEstablishedImmediately ? "" : " not") + " connected immediately";
        }
    }

    public static final class ConnectionToHostFailedEvent extends TcpHostEvent {
        private final IOException ioException;

        private ConnectionToHostFailedEvent(State state, InetSocketAddress inetSocketAddress, IOException ioException) {
            super(state, inetSocketAddress);
            this.ioException = ioException;
        }

        @Override
        public String toString() {
            return super.toString() + ioException;
        }
    }

    private final class ConnectionAttemptState {
        private final ConnectingToHostState connectingToHostState;
        InetSocketAddress inetSocketAddress;
        // TODO: Check if we can re-use the socket channel in case some InetSocketAddress fail to connect to.
        final SocketChannel socketChannel;
        final Iterator<InetSocketAddress> remainingAddresses;
        final List<HostAddress> failedAddresses;
        final SynchronizationPoint<ConnectionException> tcpConnectionEstablishedSyncPoint;

        private ConnectionAttemptState(List<InetSocketAddress> inetSocketAddresses, List<HostAddress> failedAddresses,
                ConnectingToHostState connectingToHostState) throws IOException {
            socketChannel = SocketChannel.open();
            socketChannel.configureBlocking(false);
            remainingAddresses = inetSocketAddresses.iterator();
            inetSocketAddress = remainingAddresses.next();
            this.failedAddresses = failedAddresses;
            this.connectingToHostState = connectingToHostState;

            tcpConnectionEstablishedSyncPoint = new SynchronizationPoint<>(XmppNioTcpConnection.this,
                    "TCP connection establishment");
        }

        private void establishTcpConnection() {
            ConnectingToHostEvent connectingToHostEvent = new ConnectingToHostEvent(connectingToHostState, inetSocketAddress);
            invokeConnectionStateMachineListener(connectingToHostEvent);

            final boolean connected;
            try {
                connected = socketChannel.connect(inetSocketAddress);
            } catch (IOException e) {
                onIOExceptionWhenEstablishingTcpConnection(e);
                return;
            }

            if (connected) {
                ConnectedToHostEvent connectedToHostEvent = new ConnectedToHostEvent(connectingToHostState,
                        inetSocketAddress, true);
                invokeConnectionStateMachineListener(connectedToHostEvent);

                tcpConnectionEstablishedSyncPoint.reportSuccess();
                return;
            }

            try {
                registerWithSelector(socketChannel, SelectionKey.OP_CONNECT,
                        (selectedChannel, selectedSelectionKey) -> {
                            SocketChannel selectedSocketChannel = (SocketChannel) selectedChannel;

                            boolean finishConnected;
                            try {
                                finishConnected = selectedSocketChannel.finishConnect();
                            } catch (IOException e) {
                                Async.go(() -> onIOExceptionWhenEstablishingTcpConnection(e));
                                return;
                            }

                            if (!finishConnected) {
                                Async.go(() -> onIOExceptionWhenEstablishingTcpConnection(new IOException("finishConnect() failed")));
                                return;
                            }

                            ConnectedToHostEvent connectedToHostEvent = new ConnectedToHostEvent(connectingToHostState, inetSocketAddress, false);
                            invokeConnectionStateMachineListener(connectedToHostEvent);

                            // Do not set 'state' here, since this is processed by a reactor thread, which doesn't hold
                            // the objects lock.
                            tcpConnectionEstablishedSyncPoint.reportSuccess();
                        });
            } catch (ClosedChannelException e) {
                onIOExceptionWhenEstablishingTcpConnection(e);
            }
        }

        private void onIOExceptionWhenEstablishingTcpConnection(IOException exception) {
            if (!remainingAddresses.hasNext()) {
                ConnectionException connectionException = ConnectionException.from(failedAddresses);
                tcpConnectionEstablishedSyncPoint.reportFailure(connectionException);
                return;
            }

            tcpConnectionEstablishedSyncPoint.resetTimeout();

            HostAddress failedHostAddress = new HostAddress(inetSocketAddress, exception);
            failedAddresses.add(failedHostAddress);

            ConnectionToHostFailedEvent connectionToHostFailedEvent = new ConnectionToHostFailedEvent(
                    connectingToHostState, inetSocketAddress, exception);
            invokeConnectionStateMachineListener(connectionToHostFailedEvent);

            inetSocketAddress = remainingAddresses.next();

            establishTcpConnection();
        }
    }

    @Override
    protected void connectInternal() throws SmackException, IOException, XMPPException, InterruptedException {
        // TODO: Check if those initialization methods can be invoked later.
        outgoingElementsQueue.start();
        closingStreamReceived.init();

        WalkStateGraphContext walkStateGraphContext = buildNewWalkTo(ConnectedButUnauthenticatedStateDescriptor.class)
                .build();

        walkStateGraph(walkStateGraphContext);
    }

    private List<HostAddress> failedAddresses;
    private List<InetSocketAddress> inetSocketAddresses;

    private static final class LookupHostAddressesStateDescriptor extends StateDescriptor {
        private LookupHostAddressesStateDescriptor() {
            super(LookupHostAddressesState.class);
            addPredeccessor(DisconnectedStateDescriptor.class);
            addSuccessor(ConnectingToHostStateDescriptor.class);
            addSuccessor(DirectTlsConnectionToHostStateDescriptor.class);
        }
    }

    private final class LookupHostAddressesState extends State {
        private LookupHostAddressesState(StateDescriptor stateDescriptor) {
            super(stateDescriptor);
        }

        @Override
        protected TransitionIntoResult transitionInto(WalkStateGraphContext walkStateGraphContext) throws ConnectionException {
            failedAddresses = populateHostAddresses();
            if (hostAddresses.isEmpty()) {
                throw ConnectionException.from(failedAddresses);
            }

            inetSocketAddresses = new ArrayList<>(2 * hostAddresses.size());
            for (HostAddress hostAddress : XmppNioTcpConnection.this.hostAddresses) {
                List<InetAddress> inetAddresses = hostAddress.getInetAddresses();
                for (InetAddress inetAddress : inetAddresses) {
                    InetSocketAddress inetSocketAddress = new InetSocketAddress(inetAddress, hostAddress.getPort());
                    inetSocketAddresses.add(inetSocketAddress);
                }
            }

            return new HostLookupResult(inetSocketAddresses);
        }

        @Override
        protected void resetState() {
            failedAddresses = null;
            inetSocketAddresses = null;
        }
    }

    public static final class HostLookupResult extends TransitionSuccessResult {
        private final List<InetSocketAddress> remoteAddresses;

        private HostLookupResult(List<InetSocketAddress> remoteAddresses) {
            super("Host lookup yielded the following addressess: " + remoteAddresses);

            List<InetSocketAddress> remoteAddressesLocal = new ArrayList<>(remoteAddresses.size());
            remoteAddressesLocal.addAll(remoteAddresses);
            this.remoteAddresses = Collections.unmodifiableList(remoteAddressesLocal);
        }

        public List<InetSocketAddress> getRemoteAddresses() {
            return remoteAddresses;
        }
    }

    private static final class DirectTlsConnectionToHostStateDescriptor extends StateDescriptor {
        private DirectTlsConnectionToHostStateDescriptor() {
            super(DirectTlsConnectionToHostState.class, 368, StateDescriptor.Property.notImplemented);
            addPredeccessor(LookupHostAddressesStateDescriptor.class);
            addSuccessor(ConnectedButUnauthenticatedStateDescriptor.class);
            declarePrecedenceOver(ConnectingToHostStateDescriptor.class);
        }
    }

    private final class DirectTlsConnectionToHostState extends State {
        private DirectTlsConnectionToHostState(StateDescriptor stateDescriptor) {
            super(stateDescriptor);
        }

        @Override
        protected TransitionImpossibleReason isTransitionToPossible(WalkStateGraphContext walkStateGraphContext) {
            if (!useDirectTls) {
                return new TransitionImpossibleReason("Direct TLS not enabled");
            }

            // TODO: Check if lookup yielded any xmpps SRV RRs.

            throw new IllegalStateException("Direct TLS not implemented");
        }

        @Override
        protected TransitionIntoResult transitionInto(WalkStateGraphContext walkStateGraphContext) {
            throw new IllegalStateException("Direct TLS not implemented");
        }
    }

    private static final class ConnectingToHostStateDescriptor extends StateDescriptor {
        private ConnectingToHostStateDescriptor() {
            super(ConnectingToHostState.class);
            addSuccessor(EstablishTlsStateDescriptor.class);
            addSuccessor(ConnectedButUnauthenticatedStateDescriptor.class);
        }
    }

    private final class ConnectingToHostState extends State {
        private ConnectingToHostState(StateDescriptor stateDescriptor) {
            super(stateDescriptor);
        }

        @Override
        protected TransitionIntoResult transitionInto(WalkStateGraphContext walkStateGraphContext)
                throws IOException, InterruptedException, NoResponseException, ConnectionException,
                ConnectionUnexpectedTerminatedException, NotConnectedException {
            // The fields inetSocketAddress and failedAddresses are handed over from LookupHostAddresses to
            // ConnectingToHost.
            ConnectionAttemptState connectionAttemptState = new ConnectionAttemptState(inetSocketAddresses,
                    failedAddresses, this);
            connectionAttemptState.establishTcpConnection();

            try {
                connectionAttemptState.tcpConnectionEstablishedSyncPoint.checkIfSuccessOrWaitOrThrow();
            } catch (SmackWrappedException e) {
                // Should never throw SmackWrappedException.
                throw new AssertionError(e);
            }

            socketChannel = connectionAttemptState.socketChannel;
            remoteAddress = (InetSocketAddress) socketChannel.socket().getRemoteSocketAddress();

            selectionKey = registerWithSelector(socketChannel, SelectionKey.OP_READ, channelSelectedCallback);
            selectionKeyAttachment = (SelectionKeyAttachment) selectionKey.attachment();

            newStreamOpenWaitForFeaturesSequence("stream features after initial connection");

            return new TcpSocketConnectedResult(remoteAddress);
        }

        @Override
        protected void resetState() {
            cleanUpSelectionKeyAndSocketChannel();
        }
    }

    public static final class TcpSocketConnectedResult extends TransitionSuccessResult {
        private final InetSocketAddress remoteAddress;

        private TcpSocketConnectedResult(InetSocketAddress remoteAddress) {
            super("TCP connection established to " + remoteAddress);
            this.remoteAddress = remoteAddress;
        }

        public InetSocketAddress getRemoteAddress() {
            return remoteAddress;
        }
    }

    private static final class EstablishTlsStateDescriptor extends StateDescriptor {
        private EstablishTlsStateDescriptor() {
            super(EstablishTlsState.class, "RFC 6120 § 5");
            addSuccessor(ConnectedButUnauthenticatedStateDescriptor.class);
            declarePrecedenceOver(ConnectedButUnauthenticatedStateDescriptor.class);
        }
    }

    private final class EstablishTlsState extends State {
        private EstablishTlsState(StateDescriptor stateDescriptor) {
            super(stateDescriptor);
        }

        @Override
        protected TransitionImpossibleReason isTransitionToPossible(WalkStateGraphContext walkStateGraphContext)
                throws SecurityRequiredByClientException, SecurityRequiredByServerException {
            StartTls startTlsFeature = getFeature(StartTls.ELEMENT, StartTls.NAMESPACE);
            SecurityMode securityMode = config.getSecurityMode();

            switch (securityMode) {
            case required:
            case ifpossible:
                if (startTlsFeature == null) {
                    if (securityMode == SecurityMode.ifpossible) {
                        return new TransitionImpossibleReason("Server does not announce support for TLS and we do not required it");
                    }
                    throw new SecurityRequiredByClientException();
                }
                // Allows transition by returning null.
                return null;
            case disabled:
                if (startTlsFeature != null && startTlsFeature.required()) {
                    throw new SecurityRequiredByServerException();
                }
                return new TransitionImpossibleReason("TLS disabled in client settings and server does not require it");
            default:
                throw new AssertionError("Unknown security mode: " + securityMode);
            }
        }

        @Override
        protected TransitionIntoResult transitionInto(WalkStateGraphContext walkStateGraphContext)
                        throws SmackWrappedException, FailedNonzaException, IOException, InterruptedException,
                        ConnectionUnexpectedTerminatedException, NoResponseException, NotConnectedException {
            sendAndWaitForResponse(StartTls.INSTANCE, TlsProceed.class, TlsFailure.class);

            SmackTlsContext smackTlsContext;
            try {
                smackTlsContext = getSmackTlsContext();
            } catch (KeyManagementException | UnrecoverableKeyException | NoSuchAlgorithmException
                    | CertificateException | KeyStoreException | NoSuchProviderException e) {
                throw new SmackWrappedException(e);
            }

            tlsState = new TlsState(smackTlsContext);
            addXmppInputOutputFilter(tlsState);

            channelSelectedCallbackLock.lock();
            try {
                pendingOutputFilterData = true;
                // The beginHandshake() is possibly not really required here, but it does not hurt either.
                tlsState.engine.beginHandshake();
                tlsState.handshakeStatus = TlsHandshakeStatus.initiated;
            } finally {
                channelSelectedCallbackLock.unlock();
            }
            setInterestOps(selectionKey, SelectionKey.OP_WRITE | SelectionKey.OP_READ);

            try {
                tlsState.waitForHandshakeFinished();
            } catch (CertificateException e) {
                throw new SmackWrappedException(e);
            }

            newStreamOpenWaitForFeaturesSequence("stream features after TLS established");

            return new TlsEstablishedResult(tlsState.engine);
        }

        @Override
        protected void resetState() {
            tlsState = null;
        }
    }

    public static final class TlsEstablishedResult extends TransitionSuccessResult {

        private TlsEstablishedResult(SSLEngine sslEngine) {
            super("TLS established: " + sslEngine.getSession());
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
            engine = smackTlsContext.sslContext.createSSLEngine(config.getXMPPServiceDomain().toString(), remoteAddress.getPort());
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

        @Override
        public ByteBuffer input(ByteBuffer inputData) throws SSLException {
            ByteBuffer accumulatedData;
            if (pendingInputData == null) {
                accumulatedData = inputData;
            } else {
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
                        // A delegated task is asynchronously running. Signal that there is pending input data and
                        // cycle again through the smack reactor.
                        addAsPendingInputData(accumulatedData);
                        break;
                    case NEED_UNWRAP:
                        continue;
                    case NEED_WRAP:
                        // NEED_WRAP means that the SSLEngine needs to send data, probably without consuming data.
                        // We exploit here the fact that the channelSelectedCallback is single threaded and that the
                        // input processing is after the output processing.
                        asyncGo(() -> callChannelSelectedCallback(false, true));
                        break;
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
            pendingInputData = ByteBuffer.allocate(byteBuffer.remaining());
            pendingInputData.put(byteBuffer).flip();
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
                    asyncGo(wrappedDelegatedTask);
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
            synchronized (this) {
                notifyAll();
            }
        }

        private void onHandshakeFinished() {
            handshakeStatus = TlsHandshakeStatus.successful;
            synchronized (this) {
                notifyAll();
            }
        }

        private boolean isHandshakeFinished() {
            return handshakeStatus == TlsHandshakeStatus.successful || handshakeStatus == TlsHandshakeStatus.failed;
        }

        private void waitForHandshakeFinished() throws InterruptedException, CertificateException, SSLException, ConnectionUnexpectedTerminatedException, NoResponseException {
            final long deadline = System.currentTimeMillis() + getReplyTimeout();

            synchronized (this) {
                while (!isHandshakeFinished() && currentConnectionException == null) {
                    final long now = System.currentTimeMillis();
                    if (now >= deadline) break;
                    wait(deadline - now);
                }
            }

            if (currentConnectionException != null) {
                throw new SmackException.ConnectionUnexpectedTerminatedException(currentConnectionException);
            }

            if (!isHandshakeFinished()) {
                throw NoResponseException.newWith(XmppNioTcpConnection.this, "TLS Handshake finsih");
            }

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
                ConnectionUnexpectedTerminatedException, NoResponseException {
            waitForHandshakeFinished();
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
                    + "unwrap-ratio: " + unwrapRatio
                    ;

            return toStringCache;
        }
    }

    protected static final class EnableStreamManagementStateDescriptor extends StateDescriptor {
        private EnableStreamManagementStateDescriptor() {
            super(EnableStreamManagementState.class, 198, StateDescriptor.Property.notImplemented);
            addPredeccessor(ResourceBindingStateDescriptor.class);
            addSuccessor(AuthenticatedAndResourceBoundStateDescriptor.class);
            declarePrecedenceOver(AuthenticatedAndResourceBoundStateDescriptor.class);
        }
    }

    private final class EnableStreamManagementState extends State {
        private EnableStreamManagementState(StateDescriptor stateDescriptor) {
            super(stateDescriptor);
        }

        @Override
        protected TransitionImpossibleReason isTransitionToPossible(WalkStateGraphContext walkStateGraphContext) {
            if (!useSm) {
                return new TransitionImpossibleReason("Stream management not enabled");
            }

            throw new IllegalStateException("SM not implemented");
        }

        @Override
        protected TransitionIntoResult transitionInto(WalkStateGraphContext walkStateGraphContext) {
            throw new IllegalStateException("SM not implemented");
        }
    }

    private static final class ResumeStreamStateDescriptor extends StateDescriptor {
        private ResumeStreamStateDescriptor() {
            super(ResumeStreamState.class, 198, StateDescriptor.Property.notImplemented);
            addPredeccessor(AuthenticatedButUnboundStateDescriptor.class);
            addSuccessor(AuthenticatedAndResourceBoundStateDescriptor.class);
            declarePrecedenceOver(ResourceBindingStateDescriptor.class);
            declareInferiortyTo(CompressionStateDescriptor.class);
        }
    }

    private final class ResumeStreamState extends State {
        private ResumeStreamState(StateDescriptor stateDescriptor) {
            super(stateDescriptor);
        }

        @Override
        protected TransitionImpossibleReason isTransitionToPossible(WalkStateGraphContext walkStateGraphContext) {
            if (!useSmResumption) {
                return new TransitionImpossibleReason("Stream resumption not enabled");
            }

            throw new IllegalStateException("Stream resumptionimplemented");
        }

        @Override
        protected TransitionIntoResult transitionInto(WalkStateGraphContext walkStateGraphContext) throws XMPPErrorException,
                SASLErrorException, IOException, SmackException, InterruptedException, FailedNonzaException {
            throw new IllegalStateException("Stream resumptionimplemented");
        }
    }

    private static final class InstantStreamResumptionStateDescriptor extends StateDescriptor {
        private InstantStreamResumptionStateDescriptor() {
            super(InstantStreamResumptionState.class, 397, StateDescriptor.Property.notImplemented);
            addSuccessor(AuthenticatedAndResourceBoundStateDescriptor.class);
            addPredeccessor(ConnectedButUnauthenticatedStateDescriptor.class);
            declarePrecedenceOver(SaslAuthenticationStateDescriptor.class);
        }
    }

    private final class InstantStreamResumptionState extends State {
        private InstantStreamResumptionState(StateDescriptor stateDescriptor) {
            super(stateDescriptor);
        }

        @Override
        protected TransitionImpossibleReason isTransitionToPossible(WalkStateGraphContext walkStateGraphContext) {
            if (!useIsr) {
                return new TransitionImpossibleReason("Instant stream resumption not enabled nor implemented");
            }

            throw new IllegalStateException("Instant stream resumption not implemented");
        }

        @Override
        protected TransitionIntoResult transitionInto(WalkStateGraphContext walkStateGraphContext) {
            throw new IllegalStateException("Instant stream resumption not implemented");
        }
    }

    private static final class Bind2StateDescriptor extends StateDescriptor {
        private Bind2StateDescriptor() {
            super(Bind2State.class, 386, StateDescriptor.Property.notImplemented);
            addPredeccessor(ConnectedButUnauthenticatedStateDescriptor.class);
            addSuccessor(AuthenticatedAndResourceBoundStateDescriptor.class);
            declarePrecedenceOver(SaslAuthenticationStateDescriptor.class);
        }
    }

    private final class Bind2State extends State {
        private Bind2State(StateDescriptor stateDescriptor) {
            super(stateDescriptor);
        }

        @Override
        protected TransitionImpossibleReason isTransitionToPossible(WalkStateGraphContext walkStateGraphContext) {
            if (!useBind2) {
                return new TransitionImpossibleReason("Bind2 not enabled nor implemented");
            }

            throw new IllegalStateException("Bind2 not implemented");
        }

        @Override
        protected TransitionIntoResult transitionInto(WalkStateGraphContext walkStateGraphContext) {
            throw new IllegalStateException("Bind2 not implemented");
        }
    }

    static final class InstantShutdownStateDescriptor extends StateDescriptor {
        private InstantShutdownStateDescriptor() {
            super(InstantShutdownState.class);
            addSuccessor(CloseConnectionStateDescriptor.class);
            addPredeccessor(AuthenticatedAndResourceBoundStateDescriptor.class);
            addPredeccessor(ConnectedButUnauthenticatedStateDescriptor.class);
        }
    }

    private final class InstantShutdownState extends State {
        private InstantShutdownState(StateDescriptor stateDescriptor) {
            super(stateDescriptor);
        }

        @Override
        protected TransitionImpossibleReason isTransitionToPossible(WalkStateGraphContext walkStateGraphContext) {
            ensureNotOnOurWayToAuthenticatedAndResourceBound(walkStateGraphContext);
            return null;
        }

        @Override
        protected TransitionIntoResult transitionInto(WalkStateGraphContext walkStateGraphContext) {
            outgoingElementsQueue.shutdown();
            afterOutgoingElementsQueueModified();

            return TransitionSuccessResult.EMPTY_INSTANCE;
        }
    }

    private static final class ShutdownStateDescriptor extends StateDescriptor {
        private ShutdownStateDescriptor() {
            super(ShutdownState.class);
            addSuccessor(CloseConnectionStateDescriptor.class);
            addPredeccessor(AuthenticatedAndResourceBoundStateDescriptor.class);
            addPredeccessor(ConnectedButUnauthenticatedStateDescriptor.class);
        }
    }

    private final class ShutdownState extends State {
        private ShutdownState(StateDescriptor stateDescriptor) {
            super(stateDescriptor);
        }

        @Override
        protected TransitionImpossibleReason isTransitionToPossible(WalkStateGraphContext walkStateGraphContext) {
            ensureNotOnOurWayToAuthenticatedAndResourceBound(walkStateGraphContext);
            return null;
        }

        @Override
        protected TransitionIntoResult transitionInto(WalkStateGraphContext walkStateGraphContext) {
            closingStreamReceived.init();

            boolean streamCloseIssued = outgoingElementsQueue.offerAndShutdown(StreamClose.INSTANCE);

            afterOutgoingElementsQueueModified();

            if (streamCloseIssued) {
                boolean successfullyReceivedStreamClose = waitForClosingStreamTagFromServer();
                if (successfullyReceivedStreamClose) {
                    for (Iterator<XmppInputOutputFilter> it = getXmppInputOutputFilterBeginIterator(); it.hasNext();) {
                        XmppInputOutputFilter filter = it.next();
                        filter.closeInputOutput();
                    }

                    // Flush the new state.
                    pendingInputFilterData = pendingOutputFilterData = true;
                    afterOutgoingElementsQueueModified();

                    for (Iterator<XmppInputOutputFilter> it = getXmppInputOutputFilterBeginIterator(); it.hasNext();) {
                        XmppInputOutputFilter filter = it.next();
                        try {
                            filter.waitUntilInputOutputClosed();
                        } catch (IOException | CertificateException | InterruptedException | SmackException e) {
                            LOGGER.log(Level.WARNING, "waitUntilInputOutputClosed() threw", e);
                        }
                    }
                }
            }

            return TransitionSuccessResult.EMPTY_INSTANCE;
        }
    }

    private static final class CloseConnectionStateDescriptor extends StateDescriptor {
        private CloseConnectionStateDescriptor() {
            super(CloseConnectionState.class);
            addSuccessor(DisconnectedStateDescriptor.class);
        }
    }

    private final class CloseConnectionState extends State {
        private CloseConnectionState(StateDescriptor stateDescriptor) {
            super(stateDescriptor);
        }

        @Override
        protected TransitionIntoResult transitionInto(WalkStateGraphContext walkStateGraphContext) {
            cleanUpSelectionKeyAndSocketChannel();

            return TransitionSuccessResult.EMPTY_INSTANCE;
        }
    }

    @Override
    public boolean isSecureConnection() {
        final TlsState tlsState = this.tlsState;
        return tlsState != null && tlsState.handshakeStatus == TlsHandshakeStatus.successful;
    }

    private void sendTopLevelStreamElement(TopLevelStreamElement topLevelStreamElement)
                    throws InterruptedException {
        outgoingElementsQueue.put(topLevelStreamElement);
        afterOutgoingElementsQueueModified();
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
        setInterestOps(selectionKey, SelectionKey.OP_WRITE | SelectionKey.OP_READ);
    }

    @Override
    protected void throwNotConnectedExceptionIfAppropriate() throws NotConnectedException {
        if (!connected && !isSmResumptionPossible()) {
            throw new NotConnectedException(this, "XMPP connection not connected");
        }
    }

    @Override
    protected void sendStanzaInternal(Stanza stanza) throws NotConnectedException, InterruptedException {
        sendTopLevelStreamElement(stanza);
        // TODO: Here would be stream management code once this connection type supports it.
    }

    @Override
    public void sendNonza(Nonza nonza) throws NotConnectedException, InterruptedException {
        sendTopLevelStreamElement(nonza);
    }

    @Override
    protected void shutdown() {
        shutdown(false);
    }

    @Override
    public synchronized void instantShutdown() {
        shutdown(true);
    }

    private void shutdown(boolean instant) {
        Class<? extends StateDescriptor> mandatoryIntermediateState;
        if (instant) {
            mandatoryIntermediateState = InstantShutdownStateDescriptor.class;
        } else {
            mandatoryIntermediateState = ShutdownStateDescriptor.class;
        }

        WalkStateGraphContext context = buildNewWalkTo(DisconnectedStateDescriptor.class)
                .withMandatoryIntermediateState(mandatoryIntermediateState)
                .build();

        try {
            walkStateGraph(context);
        } catch (XMPPErrorException | SASLErrorException | IOException | SmackException | InterruptedException | FailedNonzaException e) {
            throw new IllegalStateException("A walk to disconnected state should never throw", e);
        }
    }

    private void cleanUpSelectionKeyAndSocketChannel() {
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

    public boolean isSmResumptionPossible() {
        return false;
    }

    public Stats getStats() {
        return new Stats(this);
    }

    public static final class Stats {
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
        public final List<Object> filterStats;

        private Stats(XmppNioTcpConnection connection) {
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

            filterStats = connection.getFilterStats();
        }

        private transient String toStringCache;

        @Override
        public String toString() {
            if (toStringCache != null) {
                return toStringCache;
            }

            StringBuilder sb = new StringBuilder(
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
            );

            if (!filterStats.isEmpty()) {
                sb.append("Filter Stats\n");
                for (Object filterStat : filterStats) {
                    sb.append(filterStat);
                }
            }

            toStringCache = sb.toString();

            return toStringCache;
        }
    }

    private static List<? extends Buffer> pruneBufferList(Collection<? extends Buffer> buffers) {
        return CollectionUtil.removeUntil(buffers, b -> b.hasRemaining());
    }

    @Override
    protected SSLSession getSSLSession() {
        if (tlsState == null) {
            return null;
        }
        return tlsState.engine.getSession();
    }

    public static Set<Class<? extends StateDescriptor>> getBackwardEdgesStateDescriptors() {
        return Collections.unmodifiableSet(BACKWARD_EDGES_STATE_DESCRIPTORS);
    }

}
