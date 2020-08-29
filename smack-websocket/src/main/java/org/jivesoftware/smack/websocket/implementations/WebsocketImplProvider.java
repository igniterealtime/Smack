/**
 *
 * Copyright 2020 Aditya Borikar.
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
package org.jivesoftware.smack.websocket.implementations;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.jivesoftware.smack.c2s.internal.ModularXmppClientToServerConnectionInternal;
import org.jivesoftware.smack.util.Objects;
import org.jivesoftware.smack.websocket.XmppWebsocketTransportModule.XmppWebsocketTransport.DiscoveredWebsocketEndpoints;

public final class WebsocketImplProvider {

    public static AbstractWebsocket getWebsocketImpl(Class<? extends AbstractWebsocket> websocketImpl, ModularXmppClientToServerConnectionInternal connectionInternal, DiscoveredWebsocketEndpoints discoveredWebsocketEndpoints) throws NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        Objects.requireNonNull(connectionInternal, "ConnectionInternal cannot be null");

        // Creates an instance of the constructor for the desired websocket implementation.
        Constructor<? extends AbstractWebsocket> constructor = websocketImpl.getConstructor(ModularXmppClientToServerConnectionInternal.class, DiscoveredWebsocketEndpoints.class);
        return (AbstractWebsocket) constructor.newInstance(connectionInternal, discoveredWebsocketEndpoints);
    }
}
