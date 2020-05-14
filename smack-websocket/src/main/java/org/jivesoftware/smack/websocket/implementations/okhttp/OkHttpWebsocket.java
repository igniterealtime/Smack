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
package org.jivesoftware.smack.websocket.implementations.okhttp;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLSession;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.c2s.internal.ModularXmppClientToServerConnectionInternal;
import org.jivesoftware.smack.packet.TopLevelStreamElement;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smack.websocket.WebsocketException;
import org.jivesoftware.smack.websocket.XmppWebsocketTransportModule.XmppWebsocketTransport.DiscoveredWebsocketEndpoints;
import org.jivesoftware.smack.websocket.elements.WebsocketOpenElement;
import org.jivesoftware.smack.websocket.implementations.AbstractWebsocket;
import org.jivesoftware.smack.websocket.rce.WebsocketRemoteConnectionEndpoint;
import org.jivesoftware.smack.xml.XmlPullParserException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public final class OkHttpWebsocket extends AbstractWebsocket {

    private static final Logger LOGGER = Logger.getLogger(OkHttpWebsocket.class.getName());

    private static OkHttpClient okHttpClient = null;

    private final ModularXmppClientToServerConnectionInternal connectionInternal;
    private final LoggingInterceptor interceptor;

    private String openStreamHeader;
    private WebSocket currentWebsocket;
    private WebsocketConnectionPhase phase;
    private WebsocketRemoteConnectionEndpoint connectedEndpoint;

    public OkHttpWebsocket(ModularXmppClientToServerConnectionInternal connectionInternal,
            DiscoveredWebsocketEndpoints discoveredWebsocketEndpoints) {
        this.connectionInternal = connectionInternal;

        if (okHttpClient == null) {
            // Creates an instance of okHttp client.
            OkHttpClient.Builder builder = new OkHttpClient.Builder();
            okHttpClient = builder.build();
        }
        // Add some mechanism to enable and disable this interceptor.
        if (connectionInternal.smackDebugger != null) {
            interceptor = new LoggingInterceptor(connectionInternal.smackDebugger);
        } else {
            interceptor = null;
        }
    }

    @Override
    public void connect(WebsocketRemoteConnectionEndpoint endpoint) throws InterruptedException, SmackException, XMPPException {
        final String currentUri = endpoint.getWebsocketEndpoint().toString();
        Request request = new Request.Builder()
                              .url(currentUri)
                              .header("Sec-WebSocket-Protocol", "xmpp")
                              .build();

        WebSocketListener listener = new WebSocketListener() {

            @Override
            public void onOpen(WebSocket webSocket, Response response) {
                LOGGER.log(Level.FINER, "Websocket is open");
                phase = WebsocketConnectionPhase.openFrameSent;
                if (interceptor != null) {
                    interceptor.interceptOpenResponse(response);
                }
                send(new WebsocketOpenElement(connectionInternal.connection.getXMPPServiceDomain()));
            }

            @Override
            public void onMessage(WebSocket webSocket, String text) {
                if (interceptor != null) {
                    interceptor.interceptReceivedText(text);
                }
                if (isCloseElement(text)) {
                    connectionInternal.onStreamClosed();
                    return;
                }

                String closingStream = "</stream>";
                switch (phase) {
                case openFrameSent:
                    if (isOpenElement(text)) {
                        // Converts the <open> element received into <stream> element.
                        openStreamHeader = getStreamFromOpenElement(text);
                        phase = WebsocketConnectionPhase.exchangingTopLevelStreamElements;

                        try {
                            connectionInternal.onStreamOpen(PacketParserUtils.getParserFor(openStreamHeader));
                        } catch (XmlPullParserException | IOException e) {
                            LOGGER.log(Level.WARNING, "Exception caught:", e);
                        }
                    } else {
                        LOGGER.log(Level.WARNING, "Unexpected Frame received", text);
                    }
                    break;
                case exchangingTopLevelStreamElements:
                    connectionInternal.parseAndProcessElement(openStreamHeader + text + closingStream);
                    break;
                default:
                    LOGGER.log(Level.INFO, "Default text: " + text);
                }
            }

            @Override
            public void onFailure(WebSocket webSocket, Throwable t, Response response) {
                LOGGER.log(Level.INFO, "Exception caught", t);
                WebsocketException websocketException = new WebsocketException(t);
                if (connectionInternal.connection.isConnected()) {
                    connectionInternal.notifyConnectionError(websocketException);
                } else {
                    connectionInternal.setCurrentConnectionExceptionAndNotify(websocketException);
                }
            }
        };

        // Creates an instance of websocket through okHttpClient.
        currentWebsocket = okHttpClient.newWebSocket(request, listener);

        // Open a new stream and wait until features are received.
        connectionInternal.waitForFeaturesReceived("Waiting to receive features");

        connectedEndpoint = endpoint;
    }

    @Override
    public void send(TopLevelStreamElement element) {
        String textToBeSent = element.toXML().toString();
        if (interceptor != null) {
            interceptor.interceptSentText(textToBeSent);
        }
        currentWebsocket.send(textToBeSent);
    }

    @Override
    public void disconnect(int code, String message) {
        currentWebsocket.close(code, message);
        LOGGER.log(Level.INFO, "Websocket has been closed with message: " + message);
    }

    @Override
    public boolean isConnectionSecure() {
        return connectedEndpoint.isSecureEndpoint();
    }

    @Override
    public boolean isConnected() {
        return connectedEndpoint == null ? false : true;
    }

    @Override
    public SSLSession getSSLSession() {
        return null;
    }
}
