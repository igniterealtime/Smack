/**
 *
 * Copyright 2020-2021 Aditya Borikar, Florian Schmaus
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
package org.jivesoftware.smack.websocket.rce;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.datatypes.UInt16;
import org.jivesoftware.smack.util.rce.RemoteConnectionEndpoint;

public abstract class WebSocketRemoteConnectionEndpoint implements RemoteConnectionEndpoint {

    public static final String INSECURE_WEB_SOCKET_SCHEME = "ws";
    public static final String SECURE_WEB_SOCKET_SCHEME = INSECURE_WEB_SOCKET_SCHEME + "s";

    private static final Logger LOGGER = Logger.getAnonymousLogger();

    private final URI uri;
    private final UInt16 port;

    protected WebSocketRemoteConnectionEndpoint(URI uri) {
        this.uri = uri;
        int portInt = uri.getPort();
        if (portInt >= 0) {
            port = UInt16.from(portInt);
        } else {
            port = null;
        }
    }

    public final URI getUri() {
        return uri;
    }

    @Override
    public final String getHost() {
        return uri.getHost();
    }

    @Override
    public UInt16 getPort() {
        return port;
    }

    public abstract boolean isSecureEndpoint();

    private List<? extends InetAddress> inetAddresses;

    private void resolveInetAddressesIfRequired() {
        if (inetAddresses != null) {
            return;
        }

        String host = getHost();
        InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(host);
        } catch (UnknownHostException e) {
            LOGGER.log(Level.WARNING, "Could not resolve IP addresses of " + host, e);
            return;
        }
        inetAddresses = Arrays.asList(addresses);
    }

    @Override
    public Collection<? extends InetAddress> getInetAddresses() {
        resolveInetAddressesIfRequired();
        return inetAddresses;
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public String toString() {
        return uri.toString();
    }

    public static WebSocketRemoteConnectionEndpoint from(CharSequence uriCharSequence) throws URISyntaxException {
        String uriString = uriCharSequence.toString();
        URI uri = URI.create(uriString);
        return from(uri);
    }

    public static WebSocketRemoteConnectionEndpoint from(URI uri) {
        String scheme = uri.getScheme();
        switch (scheme) {
        case INSECURE_WEB_SOCKET_SCHEME:
            return new InsecureWebSocketRemoteConnectionEndpoint(uri);
        case SECURE_WEB_SOCKET_SCHEME:
            return new SecureWebSocketRemoteConnectionEndpoint(uri);
        default:
            throw new IllegalArgumentException("Only allowed protocols are " + INSECURE_WEB_SOCKET_SCHEME + " and " + SECURE_WEB_SOCKET_SCHEME);
        }
    }
}
