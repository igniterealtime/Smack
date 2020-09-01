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
import org.jivesoftware.smack.websocket.XmppWebsocketTransportModule.XmppWebsocketTransport.DiscoveredWebsocketEndpoints;
import org.jivesoftware.smack.websocket.elements.WebsocketCloseElement;
import org.jivesoftware.smack.websocket.elements.WebsocketOpenElement;
import org.jivesoftware.smack.websocket.impl.AbstractWebsocket;
import org.jivesoftware.smack.websocket.rce.WebsocketRemoteConnectionEndpoint;
import org.jivesoftware.smack.websocket.rce.WebsocketRemoteConnectionEndpointLookup;
import org.jivesoftware.smack.websocket.rce.WebsocketRemoteConnectionEndpointLookup.Result;

import org.jxmpp.jid.DomainBareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

/**
 * The websocket transport module that goes with Smack's modular architecture.
 */
public final class XmppWebsocketTransportModule
                extends ModularXmppClientToServerConnectionModule<XmppWebsocketTransportModuleDescriptor> {
    private final XmppWebsocketTransport websocketTransport;

    private AbstractWebsocket websocket;

    protected XmppWebsocketTransportModule(XmppWebsocketTransportModuleDescriptor moduleDescriptor,
                    ModularXmppClientToServerConnectionInternal connectionInternal) {
        super(moduleDescriptor, connectionInternal);

        websocketTransport = new XmppWebsocketTransport(connectionInternal);
    }

    @Override
    protected XmppWebsocketTransport getTransport() {
        return websocketTransport;
    }

    static final class EstablishingWebsocketConnectionStateDescriptor extends StateDescriptor {
        private EstablishingWebsocketConnectionStateDescriptor() {
            super(XmppWebsocketTransportModule.EstablishingWebsocketConnectionState.class);
            addPredeccessor(LookupRemoteConnectionEndpointsStateDescriptor.class);
            addSuccessor(ConnectedButUnauthenticatedStateDescriptor.class);

            // This states preference to TCP transports over this Websocket transport implementation.
            declareInferiorityTo("org.jivesoftware.smack.tcp.XmppTcpTransportModule$EstablishingTcpConnectionStateDescriptor");
        }

        @Override
        protected State constructState(ModularXmppClientToServerConnectionInternal connectionInternal) {
            XmppWebsocketTransportModule websocketTransportModule = connectionInternal.connection.getConnectionModuleFor(
                            XmppWebsocketTransportModuleDescriptor.class);
            return websocketTransportModule.constructEstablishingWebsocketConnectionState(this, connectionInternal);
        }
    }

    final class EstablishingWebsocketConnectionState extends State {
        protected EstablishingWebsocketConnectionState(StateDescriptor stateDescriptor,
                        ModularXmppClientToServerConnectionInternal connectionInternal) {
            super(stateDescriptor, connectionInternal);
        }

        @Override
        public AttemptResult transitionInto(WalkStateGraphContext walkStateGraphContext)
                        throws IOException, SmackException, InterruptedException, XMPPException {
            WebsocketConnectionAttemptState connectionAttemptState = new WebsocketConnectionAttemptState(
                            connectionInternal, discoveredWebsocketEndpoints, this);

            try {
                websocket = connectionAttemptState.establishWebsocketConnection();
            } catch (InterruptedException | WebsocketException e) {
                StateTransitionResult.Failure failure = new StateTransitionResult.FailureCausedByException<Exception>(e);
                return failure;
            }

            connectionInternal.setTransport(websocketTransport);

            WebsocketRemoteConnectionEndpoint connectedEndpoint = connectionAttemptState.getConnectedEndpoint();

            // Construct a WebsocketConnectedResult using the connected endpoint.
            return new WebsocketConnectedResult(connectedEndpoint);
        }
    }

    public EstablishingWebsocketConnectionState constructEstablishingWebsocketConnectionState(
                    EstablishingWebsocketConnectionStateDescriptor establishingWebsocketConnectionStateDescriptor,
                    ModularXmppClientToServerConnectionInternal connectionInternal) {
        return new EstablishingWebsocketConnectionState(establishingWebsocketConnectionStateDescriptor,
                        connectionInternal);
    }

    public static final class WebsocketConnectedResult extends StateTransitionResult.Success {
        final WebsocketRemoteConnectionEndpoint connectedEndpoint;

        public WebsocketConnectedResult(WebsocketRemoteConnectionEndpoint connectedEndpoint) {
            super("Websocket connection establised with endpoint: " + connectedEndpoint.getWebsocketEndpoint());
            this.connectedEndpoint = connectedEndpoint;
        }
    }

    private DiscoveredWebsocketEndpoints discoveredWebsocketEndpoints;

    /**
     * Transport class for {@link ModularXmppClientToServerConnectionModule}'s websocket implementation.
     */
    public final class XmppWebsocketTransport extends XmppClientToServerTransport {

        AsyncButOrdered<Queue<TopLevelStreamElement>> asyncButOrderedOutgoingElementsQueue;

        protected XmppWebsocketTransport(ModularXmppClientToServerConnectionInternal connectionInternal) {
            super(connectionInternal);
            asyncButOrderedOutgoingElementsQueue = new AsyncButOrdered<Queue<TopLevelStreamElement>>();
        }

        @Override
        protected void resetDiscoveredConnectionEndpoints() {
            discoveredWebsocketEndpoints = null;
        }

        @Override
        protected List<SmackFuture<LookupConnectionEndpointsResult, Exception>> lookupConnectionEndpoints() {
            // Assert that there are no stale discovered endpoints prior performing the lookup.
            assert discoveredWebsocketEndpoints == null;

            InternalSmackFuture<LookupConnectionEndpointsResult, Exception> websocketEndpointsLookupFuture = new InternalSmackFuture<>();

            connectionInternal.asyncGo(() -> {

                WebsocketRemoteConnectionEndpoint providedEndpoint = null;

                // Check if there is a websocket endpoint already configured.
                URI uri = moduleDescriptor.getExplicitlyProvidedUri();
                if (uri != null) {
                    providedEndpoint = new WebsocketRemoteConnectionEndpoint(uri);
                }

                if (!moduleDescriptor.isWebsocketEndpointDiscoveryEnabled()) {
                    // If discovery is disabled, assert that the provided endpoint isn't null.
                    assert providedEndpoint != null;

                    SecurityMode mode = connectionInternal.connection.getConfiguration().getSecurityMode();
                    if ((providedEndpoint.isSecureEndpoint() &&
                            mode.equals(SecurityMode.disabled))
                            || (!providedEndpoint.isSecureEndpoint() &&
                                    mode.equals(SecurityMode.required))) {
                        throw new IllegalStateException("Explicitly configured uri: " + providedEndpoint.getWebsocketEndpoint().toString()
                                + " does not comply with the configured security mode: " + mode);
                    }

                    // Generate Result for explicitly configured endpoint.
                    Result manualResult = new Result(Arrays.asList(providedEndpoint), null);

                    LookupConnectionEndpointsResult endpointsResult = new DiscoveredWebsocketEndpoints(manualResult);

                    websocketEndpointsLookupFuture.setResult(endpointsResult);
                } else {
                    DomainBareJid host = connectionInternal.connection.getXMPPServiceDomain();
                    ModularXmppClientToServerConnectionConfiguration configuration = connectionInternal.connection.getConfiguration();
                    SecurityMode mode = configuration.getSecurityMode();

                    // Fetch remote endpoints.
                    Result xep0156result = WebsocketRemoteConnectionEndpointLookup.lookup(host, mode);

                    List<WebsocketRemoteConnectionEndpoint> discoveredEndpoints = xep0156result.discoveredRemoteConnectionEndpoints;

                    // Generate result considering both manual and fetched endpoints.
                    Result finalResult = new Result(discoveredEndpoints, xep0156result.getLookupFailures());

                    LookupConnectionEndpointsResult endpointsResult = new DiscoveredWebsocketEndpoints(finalResult);

                    websocketEndpointsLookupFuture.setResult(endpointsResult);
                }
            });

            return Collections.singletonList(websocketEndpointsLookupFuture);
        }

        @Override
        protected void loadConnectionEndpoints(LookupConnectionEndpointsSuccess lookupConnectionEndpointsSuccess) {
            discoveredWebsocketEndpoints = (DiscoveredWebsocketEndpoints) lookupConnectionEndpointsSuccess;
        }

        @Override
        protected void afterFiltersClosed() {
        }

        @Override
        protected void disconnect() {
            websocket.disconnect(1000, "Websocket closed normally");
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
                        return new WebsocketOpenElement(JidCreate.domainBareFrom(to));
                    } catch (XmppStringprepException e) {
                        Logger.getAnonymousLogger().log(Level.WARNING, "Couldn't create OpenElement", e);
                        return null;
                    }
                }
                @Override
                public AbstractStreamClose createStreamClose() {
                    return new WebsocketCloseElement();
                }
            };
        }

        /**
         * Contains {@link Result} for successfully discovered endpoints.
         */
        public final class DiscoveredWebsocketEndpoints implements LookupConnectionEndpointsSuccess {
            final WebsocketRemoteConnectionEndpointLookup.Result result;

            DiscoveredWebsocketEndpoints(Result result) {
                assert result != null;
                this.result = result;
            }

            public WebsocketRemoteConnectionEndpointLookup.Result getResult() {
                return result;
            }
        }

        /**
         * Contains list of {@link RemoteConnectionEndpointLookupFailure} when no endpoint
         * could be found during http lookup.
         */
        final class WebsocketEndpointsDiscoveryFailed implements LookupConnectionEndpointsFailed {
            final List<RemoteConnectionEndpointLookupFailure> lookupFailures;

            WebsocketEndpointsDiscoveryFailed(
                            WebsocketRemoteConnectionEndpointLookup.Result result) {
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
