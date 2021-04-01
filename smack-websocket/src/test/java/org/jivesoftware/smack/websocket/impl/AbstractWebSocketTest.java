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
package org.jivesoftware.smack.websocket.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public final class AbstractWebSocketTest {
    private static final String OPEN_ELEMENT = "<open from='localhost.org' id='aov9ihhmmn' xmlns='urn:ietf:params:xml:ns:xmpp-framing' xml:lang='en' version='1.0'/>";
    private static final String OPEN_STREAM = "<stream:stream from='localhost.org' id='aov9ihhmmn' xmlns='jabber:client' xml:lang='en' version='1.0' xmlns:stream='http://etherx.jabber.org/streams'>";
    private static final String CLOSE_ELEMENT = "<close xmlns='urn:ietf:params:xml:ns:xmpp-framing'/>";

    @Test
    public void getStreamFromOpenElementTest() {
        String generatedOpenStream = AbstractWebSocket.getStreamFromOpenElement(OPEN_ELEMENT);
        assertEquals(generatedOpenStream, OPEN_STREAM);
    }

    @Test
    public void isOpenElementTest() {
        assertTrue(AbstractWebSocket.isOpenElement(OPEN_ELEMENT));
        assertFalse(AbstractWebSocket.isOpenElement(OPEN_STREAM));
    }

    @Test
    public void isCloseElementTest() {
        assertTrue(AbstractWebSocket.isCloseElement(CLOSE_ELEMENT));
        assertFalse(AbstractWebSocket.isCloseElement(OPEN_STREAM));
    }
}
