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
package org.jivesoftware.smack.websocket.okhttp;

import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLSession;

import org.jivesoftware.smack.SmackFuture;
import org.jivesoftware.smack.c2s.internal.ModularXmppClientToServerConnectionInternal;
import org.jivesoftware.smack.websocket.WebSocketException;
import org.jivesoftware.smack.websocket.impl.AbstractWebSocket;
import org.jivesoftware.smack.websocket.rce.WebSocketRemoteConnectionEndpoint;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public final class OkHttpWebSocket extends AbstractWebSocket {

    private static final Logger LOGGER = Logger.getLogger(OkHttpWebSocket.class.getName());

    private static final OkHttpClient okHttpClient = new OkHttpClient();

    // This is a potential candidate to be placed into AbstractWebSocket, but I keep it here until smack-websocket-java11
    // arrives.
    private final SmackFuture.InternalSmackFuture<AbstractWebSocket, Exception> future = new SmackFuture.InternalSmackFuture<>();

    private final LoggingInterceptor interceptor;

    private final WebSocket okHttpWebSocket;

    public OkHttpWebSocket(WebSocketRemoteConnectionEndpoint endpoint,
                    ModularXmppClientToServerConnectionInternal connectionInternal) {
        super(endpoint, connectionInternal);

        if (connectionInternal.smackDebugger != null) {
            interceptor = new LoggingInterceptor(connectionInternal.smackDebugger);
        } else {
            interceptor = null;
        }

        final URI uri = endpoint.getUri();
        final String url = uri.toString();

        Request request = new Request.Builder()
                              .url(url)
                              .header("Sec-WebSocket-Protocol", "xmpp")
                              .build();

        okHttpWebSocket = okHttpClient.newWebSocket(request, listener);
    }

    private final WebSocketListener listener = new WebSocketListener() {

        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            LOGGER.log(Level.FINER, "OkHttp invoked onOpen() for {0}. Response: {1}",
                            new Object[] { webSocket, response });

            if (interceptor != null) {
                interceptor.interceptOpenResponse(response);
            }

            future.setResult(OkHttpWebSocket.this);
        }

        @Override
        public void onMessage(WebSocket webSocket, String text) {
            if (interceptor != null) {
                interceptor.interceptReceivedText(text);
            }

            onIncomingWebSocketElement(text);
        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable throwable, Response response) {
            LOGGER.log(Level.FINER, "OkHttp invoked onFailure() for " + webSocket + ". Response: " + response, throwable);
            WebSocketException websocketException = new WebSocketException(throwable);

            // If we are already connected, then we need to notify the connection that it got tear down. Otherwise we
            // need to notify the thread calling connect() that the connection failed.
            if (future.wasSuccessful()) {
                connectionInternal.notifyConnectionError(websocketException);
            } else {
                future.setException(websocketException);
            }
        }

        @Override
        public void onClosing(WebSocket webSocket, int code, String reason) {
            LOGGER.log(Level.FINER, "OkHttp invoked onClosing() for " + webSocket + ". Code: " + code + ". Reason: " + reason);
        }

        @Override
        public void onClosed(WebSocket webSocket, int code, String reason) {
            LOGGER.log(Level.FINER, "OkHttp invoked onClosed() for " + webSocket + ". Code: " + code + ". Reason: " + reason);
        }

    };

    @Override
    public SmackFuture<AbstractWebSocket, Exception> getFuture() {
        return future;
    }

    @Override
    public void send(String element) {
        if (interceptor != null) {
            interceptor.interceptSentText(element);
        }
        okHttpWebSocket.send(element);
    }

    @Override
    public void disconnect(int code, String message) {
        LOGGER.log(Level.INFO, "WebSocket closing with code: " + code + " and message: " + message);
        okHttpWebSocket.close(code, message);
    }

    @Override
    public boolean isConnectionSecure() {
        return endpoint.isSecureEndpoint();
    }

    @Override
    public boolean isConnected() {
        // TODO: Do we need this method at all if we create an AbstractWebSocket object for every endpoint?
        return true;
    }

    @Override
    public SSLSession getSSLSession() {
        // TODO: What shall we do about this method, as it appears that OkHttp does not provide access to the used SSLSession?
        return null;
    }
}
