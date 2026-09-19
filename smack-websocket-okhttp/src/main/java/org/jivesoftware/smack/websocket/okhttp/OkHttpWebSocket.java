/*
 *
 * Copyright 2020 Aditya Borikar, 2020-2025 Florian Schmaus
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

import org.jivesoftware.smack.c2s.internal.ModularXmppClientToServerConnectionInternal;
import org.jivesoftware.smack.proxy.ProxyInfo;
import org.jivesoftware.smack.websocket.impl.AbstractWebSocket;
import org.jivesoftware.smack.websocket.rce.WebSocketRemoteConnectionEndpoint;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.time.Duration;
import java.util.logging.Level;

import javax.net.ssl.SSLSession;

import okhttp3.Authenticator;
import okhttp3.Credentials;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public final class OkHttpWebSocket extends AbstractWebSocket {

    private final WebSocket okHttpWebSocket;

    OkHttpWebSocket(WebSocketRemoteConnectionEndpoint endpoint,
                    ModularXmppClientToServerConnectionInternal connectionInternal) {
        this(endpoint, null, connectionInternal);
    }

    OkHttpWebSocket(WebSocketRemoteConnectionEndpoint endpoint,
                    Long connectionTimeout,
                    ModularXmppClientToServerConnectionInternal connectionInternal) {

        super(endpoint, connectionInternal);

        var okHttpClientBuilder = new OkHttpClient.Builder();
        var tlsContext = connectionInternal.getSmackTlsContext();
        if (tlsContext != null) {
            var customTrustManager = tlsContext.customTrustManager;
            if (customTrustManager != null) {
                okHttpClientBuilder.sslSocketFactory(tlsContext.sslContext.getSocketFactory(), customTrustManager);
            }
        }

        if (connectionTimeout != null)
            okHttpClientBuilder.connectTimeout(Duration.ofMillis(connectionTimeout));

        // Checking if connection is not null is only necessary since we use mocked objects where it may be null in test
        // cases. Otherwise, the 'connection' field will always be non-null.
        if (connectionInternal.connection != null) {
            var customHostnameVerifier = connectionInternal.connection.getConfiguration().getHostnameVerifier();
            if (customHostnameVerifier != null) {
                okHttpClientBuilder.hostnameVerifier(customHostnameVerifier);
            }

            applyProxySettings(okHttpClientBuilder, connectionInternal.connection.getConfiguration().getProxyInfo());
        }
        var okHttpClient = okHttpClientBuilder.build();

        final String url = endpoint.getRawString();
        Request request = new Request.Builder()
                              .url(url)
                              .header(SEC_WEBSOCKET_PROTOCOL_HEADER_FILED_NAME, SEC_WEBSOCKET_PROTOCOL_HEADER_FILED_VALUE_XMPP)
                              .build();

        okHttpWebSocket = okHttpClient.newWebSocket(request, listener);
    }

    private void applyProxySettings(OkHttpClient.Builder okHttpClientBuilder, ProxyInfo proxyInfo) {
        if (proxyInfo == null || proxyInfo.getProxyType() == null) {
            return;
        }

        switch (proxyInfo.getProxyType()) {
            case HTTP:
                applyHttpProxy(okHttpClientBuilder, proxyInfo);
                break;
            case SOCKS4:
            case SOCKS5:
                applySOCKSProxy(okHttpClientBuilder, proxyInfo);
                break;
            default:
                throw new IllegalStateException("Unsupported proxy type");
        }
    }

    private void applySOCKSProxy(OkHttpClient.Builder okHttpClientBuilder, ProxyInfo proxyInfo) {
        if (proxyInfo == null) {
            return;
        }
        String proxyHost = proxyInfo.getProxyAddress();
        int proxyPort = proxyInfo.getProxyPort();
        if (proxyHost == null || proxyHost.isBlank()
                || proxyPort == 0) {
            return;
        }


        Proxy proxy = new Proxy(Proxy.Type.SOCKS, InetSocketAddress.createUnresolved(proxyHost, proxyPort));
        okHttpClientBuilder.proxy(proxy);
        // The authenticator must be set e.g. via java.net.Authenticator.setDefault();
    }

    private void applyHttpProxy(OkHttpClient.Builder okHttpClientBuilder, ProxyInfo proxyInfo) {

        final String PROXY_AUTH_HEADER = "Proxy-Authorization";

        if (proxyInfo == null) {
            return;
        }
        String proxyHost = proxyInfo.getProxyAddress();
        int proxyPort = proxyInfo.getProxyPort();
        if (proxyHost == null || proxyHost.isBlank()
                || proxyPort == 0) {
            return;
        }


        var proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(proxyHost, proxyPort));
        okHttpClientBuilder.proxy(proxy);

        String username = proxyInfo.getProxyUsername();
        if (username == null || username.isBlank()) {
            return;
        }
        String password = proxyInfo.getProxyPassword() != null ? proxyInfo.getProxyPassword() : "";

        Authenticator proxyAuthenticator = (route, response) ->
        {
            // If the proxy header is already set, the credentials are probably wrong.
            // So do nothing!
            if (response.request().header(PROXY_AUTH_HEADER) != null) {
                return null;
            }

            // Generate Basic Auth credentials
            String credentials = Credentials.basic(username, password);

            // Add the 'Proxy-Authorization' header to the request
            return response.request()
                    .newBuilder()
                    .header(PROXY_AUTH_HEADER, credentials)
                    .build();
        };
        okHttpClientBuilder.proxyAuthenticator(proxyAuthenticator);

    }

    private final WebSocketListener listener = new WebSocketListener() {

        @Override
        public void onOpen(WebSocket webSocket, Response response) {
            LOGGER.log(Level.FINER, "OkHttp invoked onOpen() for {0}. Response: {1}",
                            new Object[] { webSocket, response });
            future.setResult(OkHttpWebSocket.this);
        }

        @Override
        public void onMessage(WebSocket webSocket, String text) {
            onIncomingWebSocketElement(text);
        }

        @Override
        public void onFailure(WebSocket webSocket, Throwable throwable, Response response) {
            LOGGER.log(Level.FINER, "OkHttp invoked onFailure() for " + webSocket + ". Response: " + response, throwable);
            onWebSocketFailure(throwable);
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
    public void send(String element) {
        okHttpWebSocket.send(element);
    }

    @Override
    public void disconnect(int code, String message) {
        okHttpWebSocket.close(code, message);
    }

    @Override
    public SSLSession getSSLSession() {
        // TODO: What shall we do about this method, as it appears that OkHttp does not provide access to the used SSLSession?
        return null;
    }
}
