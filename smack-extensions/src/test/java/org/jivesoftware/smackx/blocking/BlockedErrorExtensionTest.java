/*
 *
 * Copyright 2016 Fernando Ramirez
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
package org.jivesoftware.smackx.blocking;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.test.util.ElementParserUtils;

import org.jivesoftware.smackx.blocking.element.BlockedErrorExtension;

import org.junit.jupiter.api.Test;

public class BlockedErrorExtensionTest {

    private static final String messageWithoutError = "<message from='gardano@erlang-solutions.com' "
            + "to='griveroa-inaka@erlang-solutions.com/9b7b3fce28742983' "
            + "type='normal' xml:lang='en' id='5x41G-120'>" + "</message>";

    private static final String messageWithError = "<message from='gardano@erlang-solutions.com' "
            + "to='griveroa-inaka@erlang-solutions.com/9b7b3fce28742983' "
            + "type='error' xml:lang='en' id='5x41G-121'>" + "<error code='406' type='cancel'>"
            + "<not-acceptable xmlns='urn:ietf:params:xml:ns:xmpp-stanzas'/>" + "</error>" + "</message>";

    private static final String messageWithBlockedError = "<message from='gardano@erlang-solutions.com' "
            + "to='griveroa-inaka@erlang-solutions.com/9b7b3fce28742983' "
            + "type='error' xml:lang='en' id='5x41G-122'>" + "<error code='406' type='cancel'>"
            + "<blocked xmlns='urn:xmpp:blocking:errors'/>"
            + "<not-acceptable xmlns='urn:ietf:params:xml:ns:xmpp-stanzas'/>" + "</error>" + "</message>";

    @Test
    public void checkErrorHasBlockedExtension() throws Exception {
        Message message1 = ElementParserUtils.parseStanza(messageWithoutError);
        assertFalse(BlockedErrorExtension.isInside(message1));

        Message message2 = ElementParserUtils.parseStanza(messageWithError);
        assertFalse(BlockedErrorExtension.isInside(message2));

        Message message3 = ElementParserUtils.parseStanza(messageWithBlockedError);
        assertTrue(BlockedErrorExtension.isInside(message3));
    }

}
