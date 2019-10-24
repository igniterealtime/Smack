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
import org.jivesoftware.smack.packet.StanzaBuilder;
import org.jivesoftware.smack.packet.StreamOpen;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smack.xml.XmlPullParser;

import org.jivesoftware.smackx.chat_markers.element.ChatMarkersElements;
import org.jivesoftware.smackx.chat_markers.element.ChatMarkersElements.ReceivedExtension;
import org.jivesoftware.smackx.chat_markers.provider.ReceivedProvider;

import org.junit.jupiter.api.Test;

public class ReceivedExtensionTest {

    String receivedMessageStanza = "<message to='northumberland@shakespeare.lit/westminster' id='message-2'>"
            + "<received xmlns='urn:xmpp:chat-markers:0' id='message-1'/>" + "</message>";

    String receivedExtension = "<received xmlns='urn:xmpp:chat-markers:0' id='message-1'/>";

    @Test
    public void checkReceivedExtension() throws Exception {
        Message message = StanzaBuilder.buildMessage("message-2")
                .to("northumberland@shakespeare.lit/westminster")
                .addExtension(new ChatMarkersElements.ReceivedExtension("message-1"))
                .build();
        assertEquals(receivedMessageStanza, message.toXML(StreamOpen.CLIENT_NAMESPACE).toString());
    }

    @Test
    public void checkReceivedProvider() throws Exception {
        XmlPullParser parser = PacketParserUtils.getParserFor(receivedExtension);
        ReceivedExtension receivedExtension1 = new ReceivedProvider().parse(parser);
        assertEquals("message-1", receivedExtension1.getId());

        Message message = PacketParserUtils.parseStanza(receivedMessageStanza);
        ReceivedExtension receivedExtension2 = ReceivedExtension.from(message);
        assertEquals("message-1", receivedExtension2.getId());
    }

}
