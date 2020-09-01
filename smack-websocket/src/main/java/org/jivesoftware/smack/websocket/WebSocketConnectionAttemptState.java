/**
 *
 * Copyright 2020 Aditya Borikar, Florian Schmaus.
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

import org.jivesoftware.smack.c2s.internal.ModularXmppClientToServerConnectionInternal;
import org.jivesoftware.smack.websocket.XmppWebSocketTransportModule.EstablishingWebSocketConnectionState;
import org.jivesoftware.smack.websocket.impl.AbstractWebSocket;
import org.jivesoftware.smack.websocket.impl.WebSocketFactoryService;
import org.jivesoftware.smack.websocket.rce.WebSocketRemoteConnectionEndpoint;

public final class WebSocketConnectionAttemptState {
    private final ModularXmppClientToServerConnectionInternal connectionInternal;
    private final XmppWebSocketTransportModule.XmppWebSocketTransport.DiscoveredWebSocketEndpoints discoveredEndpoints;

    private WebSocketRemoteConnectionEndpoint connectedEndpoint;

    WebSocketConnectionAttemptState(ModularXmppClientToServerConnectionInternal connectionInternal,
                    XmppWebSocketTransportModule.XmppWebSocketTransport.DiscoveredWebSocketEndpoints discoveredWebSocketEndpoints,
                    EstablishingWebSocketConnectionState establishingWebSocketConnectionState) {
        assert discoveredWebSocketEndpoints != null;
        this.connectionInternal = connectionInternal;
        this.discoveredEndpoints = discoveredWebSocketEndpoints;
    }

    /**
     * Establish  a websocket connection with one of the discoveredRemoteConnectionEndpoints.<br>
     *
     * @return {@link AbstractWebSocket} with which connection is establised
     * @throws InterruptedException if the calling thread was interrupted
     * @throws WebSocketException if encounters a websocket exception
     */
    AbstractWebSocket establishWebSocketConnection() throws InterruptedException, WebSocketException {
        List<WebSocketRemoteConnectionEndpoint> endpoints = discoveredEndpoints.result.discoveredRemoteConnectionEndpoints;

        if (endpoints.isEmpty()) {
            throw new WebSocketException(new Throwable("No Endpoints discovered to establish connection"));
        }

        List<Throwable> connectionFailureList = new ArrayList<>();
        AbstractWebSocket websocket = WebSocketFactoryService.createWebSocket(connectionInternal);

        // Keep iterating over available endpoints until a connection is establised or all endpoints are tried to create a connection with.
        for (WebSocketRemoteConnectionEndpoint endpoint : endpoints) {
            try {
                websocket.connect(endpoint);
                connectedEndpoint = endpoint;
                break;
            } catch (Throwable t) {
                connectionFailureList.add(t);

                // If the number of entries in connectionFailureList is equal to the number of endpoints,
                // it means that all endpoints have been tried and have been unsuccessful.
                if (connectionFailureList.size() == endpoints.size()) {
                    WebSocketException websocketException = new WebSocketException(connectionFailureList);
                    throw new WebSocketException(websocketException);
                }
            }
        }

        assert connectedEndpoint != null;

        // Return connected websocket when no failure occurs.
        return websocket;
    }

    /**
     * Returns the connected websocket endpoint.
     *
     * @return connected websocket endpoint
     */
    public WebSocketRemoteConnectionEndpoint getConnectedEndpoint() {
        return connectedEndpoint;
    }
}
