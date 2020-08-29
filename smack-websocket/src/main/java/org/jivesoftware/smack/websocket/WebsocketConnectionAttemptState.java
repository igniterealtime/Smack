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

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.smack.c2s.internal.ModularXmppClientToServerConnectionInternal;
import org.jivesoftware.smack.websocket.XmppWebsocketTransportModule.EstablishingWebsocketConnectionState;
import org.jivesoftware.smack.websocket.implementations.AbstractWebsocket;
import org.jivesoftware.smack.websocket.implementations.WebsocketImplProvider;
import org.jivesoftware.smack.websocket.implementations.okhttp.OkHttpWebsocket;
import org.jivesoftware.smack.websocket.rce.WebsocketRemoteConnectionEndpoint;

public final class WebsocketConnectionAttemptState {
    private final ModularXmppClientToServerConnectionInternal connectionInternal;
    private final XmppWebsocketTransportModule.XmppWebsocketTransport.DiscoveredWebsocketEndpoints discoveredEndpoints;

    private WebsocketRemoteConnectionEndpoint connectedEndpoint;

    WebsocketConnectionAttemptState(ModularXmppClientToServerConnectionInternal connectionInternal,
                    XmppWebsocketTransportModule.XmppWebsocketTransport.DiscoveredWebsocketEndpoints discoveredWebsocketEndpoints,
                    EstablishingWebsocketConnectionState establishingWebsocketConnectionState) {
        assert discoveredWebsocketEndpoints != null;
        this.connectionInternal = connectionInternal;
        this.discoveredEndpoints = discoveredWebsocketEndpoints;
    }

    /**
     * Establish  a websocket connection with one of the discoveredRemoteConnectionEndpoints.<br>
     *
     * @return {@link AbstractWebsocket} with which connection is establised
     * @throws InterruptedException if the calling thread was interrupted
     * @throws WebsocketException if encounters a websocket exception
     */
    AbstractWebsocket establishWebsocketConnection() throws InterruptedException, WebsocketException {
        List<WebsocketRemoteConnectionEndpoint> endpoints = discoveredEndpoints.result.discoveredRemoteConnectionEndpoints;

        if (endpoints.isEmpty()) {
            throw new WebsocketException(new Throwable("No Endpoints discovered to establish connection"));
        }

        List<Throwable> connectionFailureList = new ArrayList<>();
        AbstractWebsocket websocket;

        try {
            // Obtain desired websocket implementation by using WebsocketImplProvider
            websocket = WebsocketImplProvider.getWebsocketImpl(OkHttpWebsocket.class, connectionInternal, discoveredEndpoints);
        } catch (NoSuchMethodException | SecurityException | InstantiationException |
                IllegalAccessException | IllegalArgumentException | InvocationTargetException exception) {
            throw new WebsocketException(exception);
        }

        // Keep iterating over available endpoints until a connection is establised or all endpoints are tried to create a connection with.
        for (WebsocketRemoteConnectionEndpoint endpoint : endpoints) {
            try {
                websocket.connect(endpoint);
                connectedEndpoint = endpoint;
                break;
            } catch (Throwable t) {
                connectionFailureList.add(t);

                // If the number of entries in connectionFailureList is equal to the number of endpoints,
                // it means that all endpoints have been tried and have been unsuccessful.
                if (connectionFailureList.size() == endpoints.size()) {
                    WebsocketException websocketException = new WebsocketException(connectionFailureList);
                    throw new WebsocketException(websocketException);
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
    public WebsocketRemoteConnectionEndpoint getConnectedEndpoint() {
        return connectedEndpoint;
    }
}
