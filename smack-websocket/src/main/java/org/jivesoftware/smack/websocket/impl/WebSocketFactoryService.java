/**
 *
 * Copyright 2020-2021 Florian Schmaus.
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
package org.jivesoftware.smack.websocket.impl;

import java.util.Iterator;
import java.util.ServiceLoader;

import org.jivesoftware.smack.c2s.internal.ModularXmppClientToServerConnectionInternal;
import org.jivesoftware.smack.websocket.rce.WebSocketRemoteConnectionEndpoint;

public final class WebSocketFactoryService {

    private static final ServiceLoader<WebSocketFactory> SERVICE_LOADER = ServiceLoader.load(WebSocketFactory.class);

    public static AbstractWebSocket createWebSocket(WebSocketRemoteConnectionEndpoint endpoint,
                    ModularXmppClientToServerConnectionInternal connectionInternal) {
        assert connectionInternal != null;

        Iterator<WebSocketFactory> websocketFactories = SERVICE_LOADER.iterator();
        if (!websocketFactories.hasNext()) {
            throw new IllegalStateException("No smack websocket service configured");
        }

        WebSocketFactory websocketFactory = websocketFactories.next();
        return websocketFactory.create(endpoint, connectionInternal);
    }

}
