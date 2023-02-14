/**
 *
 * Copyright 2020 Aditya Borikar, 2020-2021 Florian Schmaus.
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

import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.SmackFuture;
import org.jivesoftware.smack.c2s.internal.ModularXmppClientToServerConnectionInternal;
import org.jivesoftware.smack.fsm.StateTransitionResult;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.websocket.impl.AbstractWebSocket;
import org.jivesoftware.smack.websocket.impl.WebSocketFactory;
import org.jivesoftware.smack.websocket.rce.WebSocketRemoteConnectionEndpoint;
import org.jivesoftware.smack.websocket.rce.WebSocketRemoteConnectionEndpointLookup;

public final class WebSocketConnectionAttemptState {

    private final ModularXmppClientToServerConnectionInternal connectionInternal;
    private final XmppWebSocketTransportModule.XmppWebSocketTransport.DiscoveredWebSocketEndpoints discoveredEndpoints;
    private final WebSocketFactory webSocketFactory;

    private AbstractWebSocket webSocket;

    WebSocketConnectionAttemptState(ModularXmppClientToServerConnectionInternal connectionInternal,
                    XmppWebSocketTransportModule.XmppWebSocketTransport.DiscoveredWebSocketEndpoints discoveredWebSocketEndpoints,
                    WebSocketFactory webSocketFactory) {
        assert discoveredWebSocketEndpoints != null;
        assert !discoveredWebSocketEndpoints.result.isEmpty();

        this.connectionInternal = connectionInternal;
        this.discoveredEndpoints = discoveredWebSocketEndpoints;
        this.webSocketFactory = webSocketFactory;
    }

    /**
     * Establish  a websocket connection with one of the discoveredRemoteConnectionEndpoints.<br>
     *
     * @return {@link AbstractWebSocket} with which connection is establised
     * @throws InterruptedException if the calling thread was interrupted
     */
    @SuppressWarnings({"incomplete-switch", "MissingCasesInEnumSwitch"})
    StateTransitionResult.Failure establishWebSocketConnection() throws InterruptedException {
        final WebSocketRemoteConnectionEndpointLookup.Result endpointLookupResult = discoveredEndpoints.result;
        final List<Exception> failures = new ArrayList<>(endpointLookupResult.discoveredEndpointCount());

        webSocket = null;

        SecurityMode securityMode = connectionInternal.connection.getConfiguration().getSecurityMode();
        switch (securityMode) {
        case required:
        case ifpossible:
            establishWebSocketConnection(endpointLookupResult.discoveredSecureEndpoints, failures);
            if (webSocket != null) {
                return null;
            }
        }

        establishWebSocketConnection(endpointLookupResult.discoveredInsecureEndpoints, failures);
        if (webSocket != null) {
            return null;
        }

        StateTransitionResult.Failure failure = FailedToConnectToAnyWebSocketEndpoint.create(failures);
        return failure;
    }

    private void establishWebSocketConnection(List<? extends WebSocketRemoteConnectionEndpoint> webSocketEndpoints,
                    List<Exception> failures) throws InterruptedException {
        final int endpointCount = webSocketEndpoints.size();

        List<SmackFuture<AbstractWebSocket, Exception>> futures = new ArrayList<>(endpointCount);
        {
            List<AbstractWebSocket> webSockets = new ArrayList<>(endpointCount);
            // First only create the AbstractWebSocket instances, in case a constructor throws.
            for (WebSocketRemoteConnectionEndpoint endpoint : webSocketEndpoints) {
                AbstractWebSocket webSocket = webSocketFactory.create(endpoint, connectionInternal);
                webSockets.add(webSocket);
            }

            for (AbstractWebSocket webSocket : webSockets) {
                SmackFuture<AbstractWebSocket, Exception> future = webSocket.getFuture();
                futures.add(future);
            }
        }

        SmackFuture.await(futures, connectionInternal.connection.getReplyTimeout());

        for (SmackFuture<AbstractWebSocket, Exception> future : futures) {
            AbstractWebSocket connectedWebSocket = future.getIfAvailable();
            if (connectedWebSocket == null) {
                Exception exception = future.getExceptionIfAvailable();
                assert exception != null;
                failures.add(exception);
                continue;
            }

            if (webSocket == null) {
                webSocket = connectedWebSocket;
                // Continue here since we still need to read out the failure exceptions from potential further remaining
                // futures and close remaining successfully connected ones.
                continue;
            }

            connectedWebSocket.disconnect(1000, "Using other connection endpoint at " + webSocket.getEndpoint());
        }
    }

    public AbstractWebSocket getConnectedWebSocket() {
        return webSocket;
    }

    public static final class FailedToConnectToAnyWebSocketEndpoint extends StateTransitionResult.Failure {

        private final List<Exception> failures;

        private FailedToConnectToAnyWebSocketEndpoint(String failureMessage, List<Exception> failures) {
            super(failureMessage);
            this.failures = failures;
        }

        public List<Exception> getFailures() {
            return failures;
        }

        private static FailedToConnectToAnyWebSocketEndpoint create(List<Exception> failures) {
            StringBuilder sb = new StringBuilder(256);
            StringUtils.appendTo(failures, sb, e -> sb.append(e.getMessage()));
            String message = sb.toString();
            return new FailedToConnectToAnyWebSocketEndpoint(message, failures);
        }
    }
}
