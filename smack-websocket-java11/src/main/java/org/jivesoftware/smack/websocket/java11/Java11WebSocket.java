/**
 *
 * Copyright 2021 Florian Schmaus
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
package org.jivesoftware.smack.websocket.java11;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.nio.ByteBuffer;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;

import javax.net.ssl.SSLSession;

import org.jivesoftware.smack.c2s.internal.ModularXmppClientToServerConnectionInternal;
import org.jivesoftware.smack.util.LazyStringBuilder;
import org.jivesoftware.smack.websocket.impl.AbstractWebSocket;
import org.jivesoftware.smack.websocket.rce.WebSocketRemoteConnectionEndpoint;

public final class Java11WebSocket extends AbstractWebSocket {

    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder().build();

    private WebSocket webSocket;

    enum PingPong {
        ping,
        pong,
    };

    Java11WebSocket(WebSocketRemoteConnectionEndpoint endpoint,
                    ModularXmppClientToServerConnectionInternal connectionInternal) {
        super(endpoint, connectionInternal);

        final WebSocket.Listener listener = new WebSocket.Listener() {
            @Override
            public void onOpen(WebSocket webSocket) {
                LOGGER.finer(webSocket + " opened");
                webSocket.request(1);
            }

            LazyStringBuilder received = new LazyStringBuilder();

            @Override
            public CompletionStage<?> onText(WebSocket webSocket, CharSequence data, boolean last) {
                received.append(data);
                webSocket.request(1);

                if (last) {
                    String wholeMessage = received.toString();
                    received = new LazyStringBuilder();
                    onIncomingWebSocketElement(wholeMessage);
                }

                return null;
            }

            @Override
            public void onError(WebSocket webSocket, Throwable error) {
                onWebSocketFailure(error);
            }

            @Override
            public CompletionStage<?> onClose(WebSocket webSocket, int statusCode, String reason) {
                LOGGER.finer(webSocket + " closed with status code " + statusCode + ". Provided reason: " + reason);
                // TODO: What should we do here? What if some server implementation closes the WebSocket out of the
                // blue? Ideally, we should react on this. Some situation in the okhttp implementation.
                return null;
            }

            @Override
            public CompletionStage<?> onPing(WebSocket webSocket, ByteBuffer message) {
                logPingPong(PingPong.ping, webSocket, message);

                webSocket.request(1);
                return null;
            }

            @Override
            public CompletionStage<?> onPong(WebSocket webSocket, ByteBuffer message) {
                logPingPong(PingPong.pong, webSocket, message);

                webSocket.request(1);
                return null;
            }

            private void logPingPong(PingPong pingPong, WebSocket webSocket, ByteBuffer message) {
                final Level pingPongLogLevel = Level.FINER;
                if (!LOGGER.isLoggable(pingPongLogLevel)) {
                    return;
                }

                LOGGER.log(pingPongLogLevel, "Received " + pingPong + " over " + webSocket + ". Message: " + message);
            }
        };

        final URI uri = endpoint.getUri();
        CompletionStage<WebSocket> webSocketFuture = HTTP_CLIENT.newWebSocketBuilder()
                        .subprotocols(SEC_WEBSOCKET_PROTOCOL_HEADER_FILED_VALUE_XMPP)
                        .buildAsync(uri, listener);

        webSocketFuture.whenComplete((webSocket, throwable) -> {
            if (throwable == null) {
                this.webSocket = webSocket;
                future.setResult(this);
            } else {
                onWebSocketFailure(throwable);
            }
        });
    }

    @Override
    protected void send(String element) {
        CompletableFuture<WebSocket> completableFuture = webSocket.sendText(element, true);
        try {
            completableFuture.get();
        } catch (ExecutionException e) {
            onWebSocketFailure(e);
        } catch (InterruptedException e) {
            // This thread should never be interrupted, as it is a Smack internal thread.
            throw new AssertionError(e);
        }
    }

    @Override
    public void disconnect(int code, String message) {
        CompletableFuture<WebSocket> completableFuture = webSocket.sendClose(code, message);
        try {
            completableFuture.get();
        } catch (ExecutionException e) {
            onWebSocketFailure(e);
        } catch (InterruptedException e) {
            // This thread should never be interrupted, as it is a Smack internal thread.
            throw new AssertionError(e);
        } finally {
            webSocket.abort();
        }
    }

    @Override
    public SSLSession getSSLSession() {
        return null;
    }

    private void onWebSocketFailure(ExecutionException executionException) {
        Throwable cause = executionException.getCause();
        onWebSocketFailure(cause);
    }
}
