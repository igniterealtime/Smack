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
package org.jivesoftware.smackx.bob;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Message.Type;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smackx.bob.element.BoBExtension;
import org.jivesoftware.smackx.bob.provider.BoBExtensionProvider;
import org.junit.Assert;
import org.junit.Test;
import org.jxmpp.jid.impl.JidCreate;
import org.xmlpull.v1.XmlPullParser;

public class BoBExtensionTest {

    String sampleMessageWithBoBExtension = "<message to='macbeth@chat.shakespeare.lit' id='sarasa' type='groupchat'>"
            + "<body>Yet here's a spot.</body>" + "<html xmlns='http://jabber.org/protocol/xhtml-im'>"
            + "<body xmlns='http://www.w3.org/1999/xhtml'>"
            + "<img alt='A spot' src='cid:sha1+8f35fef110ffc5df08d579a50083ff9308fb6242@bob.xmpp.org'/>" + "</body>"
            + "</html>" + "</message>";

    String sampleBoBExtension = "<html xmlns='http://jabber.org/protocol/xhtml-im'>"
            + "<body xmlns='http://www.w3.org/1999/xhtml'>"
            + "<img alt='A spot' src='cid:sha1+8f35fef110ffc5df08d579a50083ff9308fb6242@bob.xmpp.org'/>" + "</body>"
            + "</html>";

    @Test
    public void checkBoBMessageExtension() throws Exception {
        Message message = (Message) PacketParserUtils.parseStanza(sampleMessageWithBoBExtension);

        BoBHash bobHash = new BoBHash("8f35fef110ffc5df08d579a50083ff9308fb6242", "sha1");

        Message createdMessage = new Message(JidCreate.from("macbeth@chat.shakespeare.lit"));
        createdMessage.setStanzaId("sarasa");
        createdMessage.setType(Type.groupchat);
        createdMessage.setBody("Yet here's a spot.");
        createdMessage.addExtension(new BoBExtension(bobHash, "A spot", null));

        Assert.assertEquals(message.toXML().toString(), createdMessage.toXML().toString());
    }

    @Test
    public void checkBoBExtensionProvider() throws Exception {
        XmlPullParser parser = PacketParserUtils.getParserFor(sampleBoBExtension);
        BoBExtension bobExtension = new BoBExtensionProvider().parse(parser);

        Assert.assertEquals("A spot", bobExtension.getAlt());
        Assert.assertEquals("sha1", bobExtension.getBoBHash().getHashType());
        Assert.assertEquals("8f35fef110ffc5df08d579a50083ff9308fb6242", bobExtension.getBoBHash().getHash());
    }

}
