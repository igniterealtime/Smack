/**
 *
 * Copyright 2020 Aditya Borikar, 2020-2021 Florian Schmaus
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

import java.util.Collections;
import java.util.List;
import java.util.Queue;

import javax.net.ssl.SSLSession;

import org.jivesoftware.smack.AsyncButOrdered;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
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
import org.jivesoftware.smack.websocket.impl.WebSocketFactory;
import org.jivesoftware.smack.websocket.impl.WebSocketFactoryService;
import org.jivesoftware.smack.websocket.rce.InsecureWebSocketRemoteConnectionEndpoint;
import org.jivesoftware.smack.websocket.rce.SecureWebSocketRemoteConnectionEndpoint;
import org.jivesoftware.smack.websocket.rce.WebSocketRemoteConnectionEndpoint;
import org.jivesoftware.smack.websocket.rce.WebSocketRemoteConnectionEndpointLookup;
import org.jivesoftware.smack.websocket.rce.WebSocketRemoteConnectionEndpointLookup.Result;

import org.jxmpp.jid.DomainBareJid;

/**
 * The websocket transport module that goes with Smack's modular architecture.
 */
public final class XmppWebSocketTransportModule
                extends ModularXmppClientToServerConnectionModule<XmppWebSocketTransportModuleDescriptor> {

    private static final int WEBSOCKET_NORMAL_CLOSURE = 1000;

    private final XmppWebSocketTransport websocketTransport;

    private AbstractWebSocket websocket;

    XmppWebSocketTransportModule(XmppWebSocketTransportModuleDescriptor moduleDescriptor,
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

    final class EstablishingWebSocketConnectionState extends State.AbstractTransport {
        EstablishingWebSocketConnectionState(StateDescriptor stateDescriptor,
                        ModularXmppClientToServerConnectionInternal connectionInternal) {
            super(websocketTransport, stateDescriptor, connectionInternal);
        }

        @Override
        public AttemptResult transitionInto(WalkStateGraphContext walkStateGraphContext) throws InterruptedException,
                        NoResponseException, NotConnectedException, SmackException, XMPPException {
            final WebSocketFactory webSocketFactory;
            if (moduleDescriptor.webSocketFactory != null) {
                webSocketFactory = moduleDescriptor.webSocketFactory;
            } else {
                webSocketFactory = WebSocketFactoryService::createWebSocket;
            }

            WebSocketConnectionAttemptState connectionAttemptState = new WebSocketConnectionAttemptState(
                            connectionInternal, discoveredWebSocketEndpoints, webSocketFactory);

            StateTransitionResult.Failure failure = connectionAttemptState.establishWebSocketConnection();
            if (failure != null) {
                return failure;
            }

            websocket = connectionAttemptState.getConnectedWebSocket();

            connectionInternal.setTransport(websocketTransport);

            // TODO: It appears this should be done in a generic way. I'd assume we always
            // have to wait for stream features after the connection was established. But I
            // am not yet 100% positive that this is the case for every transport. Hence keep it here for now(?).
            // See also similar comment in XmppTcpTransportModule.
            // Maybe move this into ConnectedButUnauthenticated state's transitionInto() method? That seems to be the
            // right place.
            connectionInternal.newStreamOpenWaitForFeaturesSequence("stream features after initial connection");

            // Construct a WebSocketConnectedResult using the connected endpoint.
            return new WebSocketConnectedResult(websocket.getEndpoint());
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
            super("WebSocket connection establised with endpoint: " + connectedEndpoint);
            this.connectedEndpoint = connectedEndpoint;
        }
    }

    private DiscoveredWebSocketEndpoints discoveredWebSocketEndpoints;

    /**
     * Transport class for {@link ModularXmppClientToServerConnectionModule}'s websocket implementation.
     */
    public final class XmppWebSocketTransport extends XmppClientToServerTransport {

        AsyncButOrdered<Queue<TopLevelStreamElement>> asyncButOrderedOutgoingElementsQueue;

        XmppWebSocketTransport(ModularXmppClientToServerConnectionInternal connectionInternal) {
            super(connectionInternal);
            asyncButOrderedOutgoingElementsQueue = new AsyncButOrdered<Queue<TopLevelStreamElement>>();
        }

        @Override
        protected void resetDiscoveredConnectionEndpoints() {
            discoveredWebSocketEndpoints = null;
        }

        @Override
        public boolean hasUseableConnectionEndpoints() {
            return discoveredWebSocketEndpoints != null;
        }

        @SuppressWarnings("incomplete-switch")
        @Override
        protected List<SmackFuture<LookupConnectionEndpointsResult, Exception>> lookupConnectionEndpoints() {
            // Assert that there are no stale discovered endpoints prior performing the lookup.
            assert discoveredWebSocketEndpoints == null;

            InternalSmackFuture<LookupConnectionEndpointsResult, Exception> websocketEndpointsLookupFuture = new InternalSmackFuture<>();

            connectionInternal.asyncGo(() -> {
                Result result = null;

                ModularXmppClientToServerConnectionConfiguration configuration = connectionInternal.connection.getConfiguration();
                DomainBareJid host = configuration.getXMPPServiceDomain();

                if (moduleDescriptor.isWebSocketEndpointDiscoveryEnabled()) {
                    // Fetch remote endpoints.
                    result = WebSocketRemoteConnectionEndpointLookup.lookup(host);
                }

                WebSocketRemoteConnectionEndpoint providedEndpoint = moduleDescriptor.getExplicitlyProvidedEndpoint();
                if (providedEndpoint != null) {
                    // If there was not automatic lookup that produced a result, then create a result now.
                    if (result == null) {
                        result = new Result();
                    }

                    // We insert the provided endpoint at the beginning of the list, so that it is used first.
                    final int INSERT_INDEX = 0;
                    if (providedEndpoint instanceof SecureWebSocketRemoteConnectionEndpoint) {
                        SecureWebSocketRemoteConnectionEndpoint secureEndpoint = (SecureWebSocketRemoteConnectionEndpoint) providedEndpoint;
                        result.discoveredSecureEndpoints.add(INSERT_INDEX, secureEndpoint);
                    } else if (providedEndpoint instanceof InsecureWebSocketRemoteConnectionEndpoint) {
                        InsecureWebSocketRemoteConnectionEndpoint insecureEndpoint = (InsecureWebSocketRemoteConnectionEndpoint) providedEndpoint;
                        result.discoveredInsecureEndpoints.add(INSERT_INDEX, insecureEndpoint);
                    } else {
                        throw new AssertionError();
                    }
                }

                if (moduleDescriptor.isImplicitWebSocketEndpointEnabled()) {
                    String urlWithoutScheme = "://" + host + ":5443/ws";

                    SecureWebSocketRemoteConnectionEndpoint implicitSecureEndpoint = SecureWebSocketRemoteConnectionEndpoint.from(
                                    WebSocketRemoteConnectionEndpoint.SECURE_WEB_SOCKET_SCHEME + urlWithoutScheme);
                    result.discoveredSecureEndpoints.add(implicitSecureEndpoint);

                    InsecureWebSocketRemoteConnectionEndpoint implicitInsecureEndpoint = InsecureWebSocketRemoteConnectionEndpoint.from(
                                    WebSocketRemoteConnectionEndpoint.INSECURE_WEB_SOCKET_SCHEME + urlWithoutScheme);
                    result.discoveredInsecureEndpoints.add(implicitInsecureEndpoint);
                }

                final LookupConnectionEndpointsResult endpointsResult;
                if (result.isEmpty()) {
                    endpointsResult = new WebSocketEndpointsDiscoveryFailed(result.lookupFailures);
                } else {
                    endpointsResult = new DiscoveredWebSocketEndpoints(result);
                }

                websocketEndpointsLookupFuture.setResult(endpointsResult);
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
            websocket.disconnect(WEBSOCKET_NORMAL_CLOSURE, "WebSocket closed normally");
        }

        @Override
        protected void notifyAboutNewOutgoingElements() {
            final Queue<TopLevelStreamElement> outgoingElementsQueue = connectionInternal.outgoingElementsQueue;
            asyncButOrderedOutgoingElementsQueue.performAsyncButOrdered(outgoingElementsQueue, () -> {
                for (TopLevelStreamElement topLevelStreamElement; (topLevelStreamElement = outgoingElementsQueue.poll()) != null;) {
                    websocket.send(topLevelStreamElement);
                }
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
        public Stats getStats() {
            return null;
        }

        @Override
        public StreamOpenAndCloseFactory getStreamOpenAndCloseFactory() {
            // TODO: Create extra class for this?
            return new StreamOpenAndCloseFactory() {
                @Override
                public AbstractStreamOpen createStreamOpen(DomainBareJid to, CharSequence from, String id, String lang) {
                    return new WebSocketOpenElement(to);
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
        }

        /**
         * Contains list of {@link RemoteConnectionEndpointLookupFailure} when no endpoint
         * could be found during http lookup.
         */
        final class WebSocketEndpointsDiscoveryFailed implements LookupConnectionEndpointsFailed {
            final List<RemoteConnectionEndpointLookupFailure> lookupFailures;

            WebSocketEndpointsDiscoveryFailed(RemoteConnectionEndpointLookupFailure lookupFailure) {
                this(Collections.singletonList(lookupFailure));
            }

            WebSocketEndpointsDiscoveryFailed(List<RemoteConnectionEndpointLookupFailure> lookupFailures) {
                assert lookupFailures != null;
                this.lookupFailures = Collections.unmodifiableList(lookupFailures);
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
