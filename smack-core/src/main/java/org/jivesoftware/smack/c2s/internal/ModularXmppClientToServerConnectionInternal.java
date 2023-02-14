/**
 *
 * Copyright 2020-2021 Florian Schmaus
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
package org.jivesoftware.smack.c2s.internal;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.util.ListIterator;
import java.util.Queue;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.SmackReactor;
import org.jivesoftware.smack.SmackReactor.ChannelSelectedCallback;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.XMPPException.FailedNonzaException;
import org.jivesoftware.smack.XmppInputOutputFilter;
import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnection;
import org.jivesoftware.smack.c2s.XmppClientToServerTransport;
import org.jivesoftware.smack.debugger.SmackDebugger;
import org.jivesoftware.smack.fsm.ConnectionStateEvent;
import org.jivesoftware.smack.internal.SmackTlsContext;
import org.jivesoftware.smack.packet.Nonza;
import org.jivesoftware.smack.packet.TopLevelStreamElement;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.util.Consumer;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smack.util.Supplier;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

public abstract class ModularXmppClientToServerConnectionInternal {

    private final SmackReactor reactor;

    public final ModularXmppClientToServerConnection connection;

    public final SmackDebugger smackDebugger;

    public final Queue<TopLevelStreamElement> outgoingElementsQueue;

    public ModularXmppClientToServerConnectionInternal(ModularXmppClientToServerConnection connection, SmackReactor reactor,
                    SmackDebugger smackDebugger, Queue<TopLevelStreamElement> outgoingElementsQueue) {
        this.connection = connection;
        this.reactor = reactor;
        this.smackDebugger = smackDebugger;
        this.outgoingElementsQueue = outgoingElementsQueue;
    }

    public SelectionKey registerWithSelector(SelectableChannel channel, int ops, ChannelSelectedCallback callback)
                    throws ClosedChannelException {
        return reactor.registerWithSelector(channel, ops, callback);
    }

    public void setInterestOps(SelectionKey selectionKey, int interestOps) {
        reactor.setInterestOps(selectionKey, interestOps);
    }

    public final void withSmackDebugger(Consumer<SmackDebugger> smackDebuggerConsumer) {
        if (smackDebugger == null) {
            return;
        }

        smackDebuggerConsumer.accept(smackDebugger);
    }

    public abstract XmlEnvironment getOutgoingStreamXmlEnvironment();

    // TODO: The incomingElement parameter was previously of type TopLevelStreamElement, but I believe it has to be
    // of type string. But would this also work for BOSH or WebSocket?
    public abstract void parseAndProcessElement(String wrappedCompleteIncomingElement);

    public abstract void notifyConnectionError(Exception e);

    public final String onStreamOpen(String streamOpen) {
        XmlPullParser streamOpenParser;
        try {
            streamOpenParser = PacketParserUtils.getParserFor(streamOpen);
        } catch (XmlPullParserException | IOException e) {
            // Should never happen.
            throw new AssertionError(e);
        }
        String streamClose = onStreamOpen(streamOpenParser);
        return streamClose;
    }

    public abstract String onStreamOpen(XmlPullParser parser);

    public abstract void onStreamClosed();

    public abstract void fireFirstLevelElementSendListeners(TopLevelStreamElement element);

    public abstract void invokeConnectionStateMachineListener(ConnectionStateEvent connectionStateEvent);

    public abstract void addXmppInputOutputFilter(XmppInputOutputFilter xmppInputOutputFilter);

    public abstract ListIterator<XmppInputOutputFilter> getXmppInputOutputFilterBeginIterator();

    public abstract ListIterator<XmppInputOutputFilter> getXmppInputOutputFilterEndIterator();

    public abstract void waitForFeaturesReceived(String waitFor) throws InterruptedException, SmackException, XMPPException;

    public abstract void newStreamOpenWaitForFeaturesSequence(String waitFor) throws InterruptedException,
                    NoResponseException, NotConnectedException, SmackException, XMPPException;

    public abstract SmackTlsContext getSmackTlsContext();

    public abstract <SN extends Nonza, FN extends Nonza> SN sendAndWaitForResponse(Nonza nonza,
                    Class<SN> successNonzaClass, Class<FN> failedNonzaClass)
                    throws NoResponseException, NotConnectedException, FailedNonzaException, InterruptedException;

    public abstract void asyncGo(Runnable runnable);

    public abstract void waitForConditionOrThrowConnectionException(Supplier<Boolean> condition, String waitFor) throws InterruptedException, SmackException, XMPPException;

    public abstract void notifyWaitingThreads();

    public abstract void setCompressionEnabled(boolean compressionEnabled);

    /**
     * Set the active transport (TCP, BOSH, WebSocket, …) to be used for the XMPP connection. Also marks the connection
     * as connected.
     *
     * @param xmppTransport the active transport.
     */
    public abstract void setTransport(XmppClientToServerTransport xmppTransport);
}
