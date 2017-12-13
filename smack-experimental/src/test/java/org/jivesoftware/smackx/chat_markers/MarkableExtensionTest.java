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

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.util.PacketParserUtils;

import org.jivesoftware.smackx.chat_markers.element.ChatMarkersElements;
import org.jivesoftware.smackx.chat_markers.element.ChatMarkersElements.MarkableExtension;
import org.jivesoftware.smackx.chat_markers.provider.MarkableProvider;

import org.junit.Assert;
import org.junit.Test;
import org.jxmpp.jid.impl.JidCreate;
import org.xmlpull.v1.XmlPullParser;

public class MarkableExtensionTest {

    String markableMessageStanza = "<message to='ingrichard@royalty.england.lit/throne' id='message-1'>"
            + "<body>My lord, dispatch; read o&apos;er these articles.</body>"
            + "<markable xmlns='urn:xmpp:chat-markers:0'/>" + "</message>";

    String markableExtension = "<markable xmlns='urn:xmpp:chat-markers:0'/>";

    @Test
    public void checkMarkableExtension() throws Exception {
        Message message = new Message(JidCreate.from("ingrichard@royalty.england.lit/throne"));
        message.setStanzaId("message-1");
        message.setBody("My lord, dispatch; read o'er these articles.");
        message.addExtension(new ChatMarkersElements.MarkableExtension());
        Assert.assertEquals(markableMessageStanza, message.toXML().toString());
    }

    @Test
    public void checkMarkableProvider() throws Exception {
        XmlPullParser parser = PacketParserUtils.getParserFor(markableExtension);
        MarkableExtension markableExtension1 = new MarkableProvider().parse(parser);
        Assert.assertEquals(markableExtension, markableExtension1.toXML().toString());

        Message message = PacketParserUtils.parseStanza(markableMessageStanza);
        MarkableExtension markableExtension2 = MarkableExtension.from(message);
        Assert.assertEquals(markableExtension, markableExtension2.toXML().toString());
    }

}
