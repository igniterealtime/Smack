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
package org.jivesoftware.smack.websocket.rce;

import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.datatypes.UInt16;
import org.jivesoftware.smack.util.rce.RemoteConnectionEndpoint;

public final class WebsocketRemoteConnectionEndpoint implements RemoteConnectionEndpoint {

    private static final Logger LOGGER = Logger.getAnonymousLogger();

    private final URI uri;

    public WebsocketRemoteConnectionEndpoint(String uri) throws URISyntaxException {
        this(new URI(uri));
    }

    public WebsocketRemoteConnectionEndpoint(URI uri) {
        this.uri = uri;
        String scheme = uri.getScheme();
        if (!(scheme.equals("ws") || scheme.equals("wss"))) {
            throw new IllegalArgumentException("Only allowed protocols are ws and wss");
        }
    }

    public URI getWebsocketEndpoint() {
        return uri;
    }

    public boolean isSecureEndpoint() {
        if (uri.getScheme().equals("wss")) {
            return true;
        }
        return false;
    }

    @Override
    public CharSequence getHost() {
        return uri.getHost();
    }

    @Override
    public UInt16 getPort() {
        return UInt16.from(uri.getPort());
    }

    @Override
    public Collection<? extends InetAddress> getInetAddresses() {
        try {
            InetAddress address = InetAddress.getByName(getHost().toString());
            return Collections.singletonList(address);
        } catch (UnknownHostException e) {
            LOGGER.log(Level.INFO, "Unknown Host Exception ", e);
        }
        return null;
    }

    @Override
    public String getDescription() {
        return null;
    }
}
