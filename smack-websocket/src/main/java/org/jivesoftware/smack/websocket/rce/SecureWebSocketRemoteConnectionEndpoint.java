/**
 *
 * Copyright 2020-2021 Florian Schmaus
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

import java.net.URI;

public class SecureWebSocketRemoteConnectionEndpoint extends WebSocketRemoteConnectionEndpoint {

    protected SecureWebSocketRemoteConnectionEndpoint(URI uri) {
        super(uri);
    }

    @Override
    public final boolean isSecureEndpoint() {
        return true;
    }

    public static final SecureWebSocketRemoteConnectionEndpoint from(CharSequence cs) {
        URI uri = URI.create(cs.toString());
        if (!uri.getScheme().equals(SECURE_WEB_SOCKET_SCHEME)) {
            throw new IllegalArgumentException(uri + " is not a secure WebSocket");
        }
        return new SecureWebSocketRemoteConnectionEndpoint(uri);
    }
}
