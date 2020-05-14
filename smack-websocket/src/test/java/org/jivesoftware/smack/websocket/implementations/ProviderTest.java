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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.InvocationTargetException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.smack.c2s.internal.ModularXmppClientToServerConnectionInternal;
import org.jivesoftware.smack.websocket.XmppWebsocketTransportModule.XmppWebsocketTransport.DiscoveredWebsocketEndpoints;

import org.jivesoftware.smack.websocket.implementations.okhttp.OkHttpWebsocket;
import org.jivesoftware.smack.websocket.rce.WebsocketRemoteConnectionEndpoint;
import org.jivesoftware.smack.websocket.rce.WebsocketRemoteConnectionEndpointLookup.Result;

import org.junit.jupiter.api.Test;

public class ProviderTest {
    @Test
    public void providerTest() {
        assertThrows(IllegalArgumentException.class, () -> WebsocketImplProvider.getWebsocketImpl(OkHttpWebsocket.class, null, null));
    }

    @Test
    public void getImplTest() throws NoSuchMethodException, SecurityException, InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, URISyntaxException {
        WebsocketRemoteConnectionEndpoint endpoint = new WebsocketRemoteConnectionEndpoint("wss://localhost.org:7443/ws/");

        List<WebsocketRemoteConnectionEndpoint> discoveredRemoteConnectionEndpoints = new ArrayList<>();
        discoveredRemoteConnectionEndpoints.add(endpoint);

        Result result = new Result(discoveredRemoteConnectionEndpoints, null);

        DiscoveredWebsocketEndpoints discoveredWebsocketEndpoints = mock(DiscoveredWebsocketEndpoints.class);
        when(discoveredWebsocketEndpoints.getResult()).thenReturn(result);

        ModularXmppClientToServerConnectionInternal connectionInternal = mock(ModularXmppClientToServerConnectionInternal.class);

        assertNotNull(WebsocketImplProvider.getWebsocketImpl(OkHttpWebsocket.class, connectionInternal, discoveredWebsocketEndpoints));
    }
}
