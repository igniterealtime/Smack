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
package org.jivesoftware.smack.websocket.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static org.mockito.Mockito.mock;

import java.net.URISyntaxException;

import org.jivesoftware.smack.c2s.internal.ModularXmppClientToServerConnectionInternal;
import org.jivesoftware.smack.websocket.impl.AbstractWebSocket;
import org.jivesoftware.smack.websocket.impl.WebSocketFactoryService;
import org.jivesoftware.smack.websocket.rce.WebSocketRemoteConnectionEndpoint;

public class WebSocketFactoryServiceTestUtil {

    public static void createWebSocketTest(Class<? extends AbstractWebSocket> expected) throws URISyntaxException {
        WebSocketRemoteConnectionEndpoint endpoint = WebSocketRemoteConnectionEndpoint.from("wss://example.org");

        ModularXmppClientToServerConnectionInternal connectionInternal = mock(ModularXmppClientToServerConnectionInternal.class);

        AbstractWebSocket websocket = WebSocketFactoryService.createWebSocket(endpoint, connectionInternal);
        assertEquals(expected, websocket.getClass());
    }

}
