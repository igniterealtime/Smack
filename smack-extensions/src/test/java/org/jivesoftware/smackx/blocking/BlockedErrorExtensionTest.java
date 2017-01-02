/**
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

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smackx.blocking.element.BlockedErrorExtension;
import org.junit.Assert;
import org.junit.Test;

public class BlockedErrorExtensionTest {

    String messageWithoutError = "<message from='gardano@erlang-solutions.com' "
            + "to='griveroa-inaka@erlang-solutions.com/9b7b3fce28742983' "
            + "type='normal' xml:lang='en' id='5x41G-120'>" + "</message>";

    String messageWithError = "<message from='gardano@erlang-solutions.com' "
            + "to='griveroa-inaka@erlang-solutions.com/9b7b3fce28742983' "
            + "type='error' xml:lang='en' id='5x41G-121'>" + "<error code='406' type='cancel'>"
            + "<not-acceptable xmlns='urn:ietf:params:xml:ns:xmpp-stanzas'/>" + "</error>" + "</message>";

    String messageWithBlockedError = "<message from='gardano@erlang-solutions.com' "
            + "to='griveroa-inaka@erlang-solutions.com/9b7b3fce28742983' "
            + "type='error' xml:lang='en' id='5x41G-122'>" + "<error code='406' type='cancel'>"
            + "<blocked xmlns='urn:xmpp:blocking:errors'/>"
            + "<not-acceptable xmlns='urn:ietf:params:xml:ns:xmpp-stanzas'/>" + "</error>" + "</message>";

    @Test
    public void checkErrorHasBlockedExtension() throws Exception {
        Message message1 = (Message) PacketParserUtils.parseStanza(messageWithoutError);
        Assert.assertFalse(BlockedErrorExtension.isInside(message1));

        Message message2 = (Message) PacketParserUtils.parseStanza(messageWithError);
        Assert.assertFalse(BlockedErrorExtension.isInside(message2));

        Message message3 = (Message) PacketParserUtils.parseStanza(messageWithBlockedError);
        Assert.assertTrue(BlockedErrorExtension.isInside(message3));
    }

}
