/**
 *
 * Copyright 2020 Aditya Borikar, 2021 Florian Schmaus
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.net.URISyntaxException;

import org.jivesoftware.smack.datatypes.UInt16;

import org.junit.jupiter.api.Test;

public class WebSocketRemoteConnectionEndpointTest {

    @Test
    public void endpointTest() throws URISyntaxException {
        String endpointString = "ws://fooDomain.org:7070/ws/";
        WebSocketRemoteConnectionEndpoint endpoint = WebSocketRemoteConnectionEndpoint.from(endpointString);
        assertEquals("fooDomain.org", endpoint.getHost());
        assertEquals(UInt16.from(7070), endpoint.getPort());
        assertEquals(endpointString, endpoint.getUri().toString());
    }

    @Test
    public void faultyEndpointTest() {
        String faultyProtocolString = "wst://fooDomain.org:7070/ws/";
        assertThrows(IllegalArgumentException.class, () -> {
            WebSocketRemoteConnectionEndpoint.from(faultyProtocolString);
        });
    }

}
