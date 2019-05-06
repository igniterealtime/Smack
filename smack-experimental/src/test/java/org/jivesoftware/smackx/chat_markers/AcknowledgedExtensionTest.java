/**
 *
 * Copyright © 2016 Fernando Ramirez
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
package org.jivesoftware.smackx.chat_markers;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.StreamOpen;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smack.xml.XmlPullParser;

import org.jivesoftware.smackx.chat_markers.element.ChatMarkersElements;
import org.jivesoftware.smackx.chat_markers.element.ChatMarkersElements.AcknowledgedExtension;
import org.jivesoftware.smackx.chat_markers.provider.AcknowledgedProvider;

import org.junit.jupiter.api.Test;
import org.jxmpp.jid.impl.JidCreate;

public class AcknowledgedExtensionTest {

    String acknowledgedMessageStanza = "<message to='northumberland@shakespeare.lit/westminster' id='message-2'>"
            + "<acknowledged xmlns='urn:xmpp:chat-markers:0' id='message-1'/>" + "</message>";

    String acknowledgedExtension = "<acknowledged xmlns='urn:xmpp:chat-markers:0' id='message-1'/>";

    @Test
    public void checkDisplayedExtension() throws Exception {
        Message message = new Message(JidCreate.from("northumberland@shakespeare.lit/westminster"));
        message.setStanzaId("message-2");
        message.addExtension(new ChatMarkersElements.AcknowledgedExtension("message-1"));
        assertEquals(acknowledgedMessageStanza, message.toXML(StreamOpen.CLIENT_NAMESPACE).toString());
    }

    @Test
    public void checkDisplayedProvider() throws Exception {
        XmlPullParser parser = PacketParserUtils.getParserFor(acknowledgedExtension);
        AcknowledgedExtension acknowledgedExtension1 = new AcknowledgedProvider().parse(parser);
        assertEquals("message-1", acknowledgedExtension1.getId());

        Message message = PacketParserUtils.parseStanza(acknowledgedMessageStanza);
        AcknowledgedExtension acknowledgedExtension2 = AcknowledgedExtension.from(message);
        assertEquals("message-1", acknowledgedExtension2.getId());
    }

}
