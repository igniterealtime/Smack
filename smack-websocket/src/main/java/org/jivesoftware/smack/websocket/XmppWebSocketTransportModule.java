/**
 *
 * Copyright 2020 Aditya Borikar
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
package org.jivesoftware.smack.websocket;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLSession;

import org.jivesoftware.smack.AsyncButOrdered;
import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackFuture;
import org.jivesoftware.smack.SmackFuture.InternalSmackFuture;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnection.ConnectedButUnauthenticatedStateDescriptor;
import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnection.LookupRemoteConnectionEndpointsStateDescriptor;
import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnectionConfiguration;
import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnectionModule;
import org.jivesoftware.smack.c2s.StreamOpenAndCloseFactory;
import org.jivesoftware.smack.c2s.XmppClientToServerTransport;
import org.jivesoftware.smack.c2s.internal.ModularXmppClientToServerConnectionInternal;
import org.jivesoftware.smack.c2s.internal.WalkStateGraphContext;
import org.jivesoftware.smack.fsm.State;
import org.jivesoftware.smack.fsm.StateDescriptor;
import org.jivesoftware.smack.fsm.StateTransitionResult;
import org.jivesoftware.smack.fsm.StateTransitionResult.AttemptResult;
import org.jivesoftware.smack.packet.AbstractStreamClose;
import org.jivesoftware.smack.packet.AbstractStreamOpen;
import org.jivesoftware.smack.packet.TopLevelStreamElement;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.rce.RemoteConnectionEndpointLookupFailure;
import org.jivesoftware.smack.websocket.XmppWebSocketTransportModule.XmppWebSocketTransport.DiscoveredWebSocketEndpoints;
import org.jivesoftware.smack.websocket.elements.WebSocketCloseElement;
import org.jivesoftware.smack.websocket.elements.WebSocketOpenElement;
import org.jivesoftware.smack.websocket.impl.AbstractWebSocket;
import org.jivesoftware.smack.websocket.rce.WebSocketRemoteConnectionEndpoint;
import org.jivesoftware.smack.websocket.rce.WebSocketRemoteConnectionEndpointLookup;
import org.jivesoftware.smack.websocket.rce.WebSocketRemoteConnectionEndpointLookup.Result;

import org.jxmpp.jid.DomainBareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

/**
 * The websocket transport module that goes with Smack's modular architecture.
 */
public final class XmppWebSocketTransportModule
                extends ModularXmppClientToServerConnectionModule<XmppWebSocketTransportModuleDescriptor> {
    private final XmppWebSocketTransport websocketTransport;

    private AbstractWebSocket websocket;

    protected XmppWebSocketTransportModule(XmppWebSocketTransportModuleDescriptor moduleDescriptor,
                    ModularXmppClientToServerConnectionInternal connectionInternal) {
        super(moduleDescriptor, connectionInternal);

        websocketTransport = new XmppWebSocketTransport(connectionInternal);
    }

    @Override
    protected XmppWebSocketTransport getTransport() {
        return websocketTransport;
    }

    static final class EstablishingWebSocketConnectionStateDescriptor extends StateDescriptor {
        private EstablishingWebSocketConnectionStateDescriptor() {
            super(XmppWebSocketTransportModule.EstablishingWebSocketConnectionState.class);
            addPredeccessor(LookupRemoteConnectionEndpointsStateDescriptor.class);
            addSuccessor(ConnectedButUnauthenticatedStateDescriptor.class);

            // This states preference to TCP transports over this WebSocket transport implementation.
            declareInferiorityTo("org.jivesoftware.smack.tcp.XmppTcpTransportModule$EstablishingTcpConnectionStateDescriptor");
        }

        @Override
        protected State constructState(ModularXmppClientToServerConnectionInternal connectionInternal) {
            XmppWebSocketTransportModule websocketTransportModule = connectionInternal.connection.getConnectionModuleFor(
                            XmppWebSocketTransportModuleDescriptor.class);
            return websocketTransportModule.constructEstablishingWebSocketConnectionState(this, connectionInternal);
        }
    }

    final class EstablishingWebSocketConnectionState extends State {
        protected EstablishingWebSocketConnectionState(StateDescriptor stateDescriptor,
                        ModularXmppClientToServerConnectionInternal connectionInternal) {
            super(stateDescriptor, connectionInternal);
        }

        @Override
        public AttemptResult transitionInto(WalkStateGraphContext walkStateGraphContext)
                        throws IOException, SmackException, InterruptedException, XMPPException {
            WebSocketConnectionAttemptState connectionAttemptState = new WebSocketConnectionAttemptState(
                            connectionInternal, discoveredWebSocketEndpoints, this);

            try {
                websocket = connectionAttemptState.establishWebSocketConnection();
            } catch (InterruptedException | WebSocketException e) {
                StateTransitionResult.Failure failure = new StateTransitionResult.FailureCausedByException<Exception>(e);
                return failure;
            }

            connectionInternal.setTransport(websocketTransport);

            WebSocketRemoteConnectionEndpoint connectedEndpoint = connectionAttemptState.getConnectedEndpoint();

            // Construct a WebSocketConnectedResult using the connected endpoint.
            return new WebSocketConnectedResult(connectedEndpoint);
        }
    }

    public EstablishingWebSocketConnectionState constructEstablishingWebSocketConnectionState(
                    EstablishingWebSocketConnectionStateDescriptor establishingWebSocketConnectionStateDescriptor,
                    ModularXmppClientToServerConnectionInternal connectionInternal) {
        return new EstablishingWebSocketConnectionState(establishingWebSocketConnectionStateDescriptor,
                        connectionInternal);
    }

    public static final class WebSocketConnectedResult extends StateTransitionResult.Success {
        final WebSocketRemoteConnectionEndpoint connectedEndpoint;

        public WebSocketConnectedResult(WebSocketRemoteConnectionEndpoint connectedEndpoint) {
            super("WebSocket connection establised with endpoint: " + connectedEndpoint.getWebSocketEndpoint());
            this.connectedEndpoint = connectedEndpoint;
        }
    }

    private DiscoveredWebSocketEndpoints discoveredWebSocketEndpoints;

    /**
     * Transport class for {@link ModularXmppClientToServerConnectionModule}'s websocket implementation.
     */
    public final class XmppWebSocketTransport extends XmppClientToServerTransport {

        AsyncButOrdered<Queue<TopLevelStreamElement>> asyncButOrderedOutgoingElementsQueue;

        protected XmppWebSocketTransport(ModularXmppClientToServerConnectionInternal connectionInternal) {
            super(connectionInternal);
            asyncButOrderedOutgoingElementsQueue = new AsyncButOrdered<Queue<TopLevelStreamElement>>();
        }

        @Override
        protected void resetDiscoveredConnectionEndpoints() {
            discoveredWebSocketEndpoints = null;
        }

        @Override
        protected List<SmackFuture<LookupConnectionEndpointsResult, Exception>> lookupConnectionEndpoints() {
            // Assert that there are no stale discovered endpoints prior performing the lookup.
            assert discoveredWebSocketEndpoints == null;

            InternalSmackFuture<LookupConnectionEndpointsResult, Exception> websocketEndpointsLookupFuture = new InternalSmackFuture<>();

            connectionInternal.asyncGo(() -> {

                WebSocketRemoteConnectionEndpoint providedEndpoint = null;

                // Check if there is a websocket endpoint already configured.
                URI uri = moduleDescriptor.getExplicitlyProvidedUri();
                if (uri != null) {
                    providedEndpoint = new WebSocketRemoteConnectionEndpoint(uri);
                }

                if (!moduleDescriptor.isWebSocketEndpointDiscoveryEnabled()) {
                    // If discovery is disabled, assert that the provided endpoint isn't null.
                    assert providedEndpoint != null;

                    SecurityMode mode = connectionInternal.connection.getConfiguration().getSecurityMode();
                    if ((providedEndpoint.isSecureEndpoint() &&
                            mode.equals(SecurityMode.disabled))
                            || (!providedEndpoint.isSecureEndpoint() &&
                                    mode.equals(SecurityMode.required))) {
                        throw new IllegalStateException("Explicitly configured uri: " + providedEndpoint.getWebSocketEndpoint().toString()
                                + " does not comply with the configured security mode: " + mode);
                    }

                    // Generate Result for explicitly configured endpoint.
                    Result manualResult = new Result(Arrays.asList(providedEndpoint), null);

                    LookupConnectionEndpointsResult endpointsResult = new DiscoveredWebSocketEndpoints(manualResult);

                    websocketEndpointsLookupFuture.setResult(endpointsResult);
                } else {
                    DomainBareJid host = connectionInternal.connection.getXMPPServiceDomain();
                    ModularXmppClientToServerConnectionConfiguration configuration = connectionInternal.connection.getConfiguration();
                    SecurityMode mode = configuration.getSecurityMode();

                    // Fetch remote endpoints.
                    Result xep0156result = WebSocketRemoteConnectionEndpointLookup.lookup(host, mode);

                    List<WebSocketRemoteConnectionEndpoint> discoveredEndpoints = xep0156result.discoveredRemoteConnectionEndpoints;

                    // Generate result considering both manual and fetched endpoints.
                    Result finalResult = new Result(discoveredEndpoints, xep0156result.getLookupFailures());

                    LookupConnectionEndpointsResult endpointsResult = new DiscoveredWebSocketEndpoints(finalResult);

                    websocketEndpointsLookupFuture.setResult(endpointsResult);
                }
            });

            return Collections.singletonList(websocketEndpointsLookupFuture);
        }

        @Override
        protected void loadConnectionEndpoints(LookupConnectionEndpointsSuccess lookupConnectionEndpointsSuccess) {
            discoveredWebSocketEndpoints = (DiscoveredWebSocketEndpoints) lookupConnectionEndpointsSuccess;
        }

        @Override
        protected void afterFiltersClosed() {
        }

        @Override
        protected void disconnect() {
            websocket.disconnect(1000, "WebSocket closed normally");
        }

        @Override
        protected void notifyAboutNewOutgoingElements() {
            Queue<TopLevelStreamElement> outgoingElementsQueue = connectionInternal.outgoingElementsQueue;
            asyncButOrderedOutgoingElementsQueue.performAsyncButOrdered(outgoingElementsQueue, () -> {
                // Once new outgoingElement is notified, send the top level stream element obtained by polling.
                TopLevelStreamElement topLevelStreamElement = outgoingElementsQueue.poll();
                websocket.send(topLevelStreamElement);
            });
        }

        @Override
        public SSLSession getSslSession() {
           return websocket.getSSLSession();
        }

        @Override
        public boolean isTransportSecured() {
            return websocket.isConnectionSecure();
        }

        @Override
        public boolean isConnected() {
            return websocket.isConnected();
        }

        @Override
        public Stats getStats() {
            return null;
        }

        @Override
        public StreamOpenAndCloseFactory getStreamOpenAndCloseFactory() {
            return new StreamOpenAndCloseFactory() {
                @Override
                public AbstractStreamOpen createStreamOpen(CharSequence to, CharSequence from, String id, String lang) {
                    try {
                        return new WebSocketOpenElement(JidCreate.domainBareFrom(to));
                    } catch (XmppStringprepException e) {
                        Logger.getAnonymousLogger().log(Level.WARNING, "Couldn't create OpenElement", e);
                        return null;
                    }
                }
                @Override
                public AbstractStreamClose createStreamClose() {
                    return new WebSocketCloseElement();
                }
            };
        }

        /**
         * Contains {@link Result} for successfully discovered endpoints.
         */
        public final class DiscoveredWebSocketEndpoints implements LookupConnectionEndpointsSuccess {
            final WebSocketRemoteConnectionEndpointLookup.Result result;

            DiscoveredWebSocketEndpoints(Result result) {
                assert result != null;
                this.result = result;
            }

            public WebSocketRemoteConnectionEndpointLookup.Result getResult() {
                return result;
            }
        }

        /**
         * Contains list of {@link RemoteConnectionEndpointLookupFailure} when no endpoint
         * could be found during http lookup.
         */
        final class WebSocketEndpointsDiscoveryFailed implements LookupConnectionEndpointsFailed {
            final List<RemoteConnectionEndpointLookupFailure> lookupFailures;

            WebSocketEndpointsDiscoveryFailed(
                            WebSocketRemoteConnectionEndpointLookup.Result result) {
                assert result != null;
                lookupFailures = Collections.unmodifiableList(result.lookupFailures);
            }

            @Override
            public String toString() {
                StringBuilder str = new StringBuilder();
                StringUtils.appendTo(lookupFailures, str);
                return str.toString();
            }
        }
    }
}
