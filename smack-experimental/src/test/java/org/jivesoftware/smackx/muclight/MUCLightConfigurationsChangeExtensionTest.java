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
package org.jivesoftware.smackx.muclight;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.util.PacketParserUtils;

import org.jivesoftware.smackx.muclight.element.MUCLightElements.ConfigurationsChangeExtension;

import org.junit.jupiter.api.Test;

public class MUCLightConfigurationsChangeExtensionTest {

    private static final String messageWithSubjectChangeExample = "<message xmlns='jabber:client' to='crone1@shakespeare.lit' from='coven@muclight.shakespeare.lit' id='newsubject' type='groupchat'>"
            + "<body></body>" + "<x xmlns='urn:xmpp:muclight:0#configuration'>"
            + "<prev-version>asdfghj000</prev-version>" + "<version>asdfghj</version>"
            + "<subject>To be or not to be?</subject>" + "</x>" + "</message>";

    private static final String messageWithRoomNameChangeExample = "<message xmlns='jabber:client' to='crone1@shakespeare.lit' from='coven@muclight.shakespeare.lit' id='newsubject' type='groupchat'>"
            + "<body></body>" + "<x xmlns='urn:xmpp:muclight:0#configuration'>" + "<prev-version>zaqwsx</prev-version>"
            + "<version>zxcvbnm</version>" + "<roomname>A Darker Cave</roomname>" + "</x>" + "</message>";

    private static final String messageWithConfigsChangeExample = "<message xmlns='jabber:client' to='crone1@shakespeare.lit' from='coven@muclight.shakespeare.lit' id='newsubject' type='groupchat'>"
            + "<body></body>" + "<x xmlns='urn:xmpp:muclight:0#configuration'>" + "<prev-version>zaqwsx</prev-version>"
            + "<version>zxcvbnm</version>" + "<roomname>A Darker Cave</roomname>" + "<color>blue</color>" + "</x>"
            + "</message>";

    @Test
    public void checkSubjectChangeExtension() throws Exception {
        Message configurationsMessage = PacketParserUtils.parseStanza(messageWithSubjectChangeExample);
        ConfigurationsChangeExtension configurationsChangeExtension = ConfigurationsChangeExtension
                .from(configurationsMessage);

        assertEquals("asdfghj000", configurationsChangeExtension.getPrevVersion());
        assertEquals("asdfghj", configurationsChangeExtension.getVersion());
        assertEquals("To be or not to be?", configurationsChangeExtension.getSubject());
        assertNull(configurationsChangeExtension.getRoomName());
        assertNull(configurationsChangeExtension.getCustomConfigs());
        assertEquals(messageWithSubjectChangeExample, configurationsMessage.toXML().toString());
    }

    @Test
    public void checkRoomNameChangeExtension() throws Exception {
        Message configurationsMessage = PacketParserUtils.parseStanza(messageWithRoomNameChangeExample);
        ConfigurationsChangeExtension configurationsChangeExtension = ConfigurationsChangeExtension
                .from(configurationsMessage);

        assertEquals("zaqwsx", configurationsChangeExtension.getPrevVersion());
        assertEquals("zxcvbnm", configurationsChangeExtension.getVersion());
        assertEquals("A Darker Cave", configurationsChangeExtension.getRoomName());
        assertNull(configurationsChangeExtension.getSubject());
        assertNull(configurationsChangeExtension.getCustomConfigs());
        assertEquals(messageWithRoomNameChangeExample, configurationsMessage.toXML().toString());
    }

    @Test
    public void checkConfigsChangeExtension() throws Exception {
        Message configurationsMessage = PacketParserUtils.parseStanza(messageWithConfigsChangeExample);
        ConfigurationsChangeExtension configurationsChangeExtension = ConfigurationsChangeExtension
                .from(configurationsMessage);

        assertEquals("zaqwsx", configurationsChangeExtension.getPrevVersion());
        assertEquals("zxcvbnm", configurationsChangeExtension.getVersion());
        assertEquals("A Darker Cave", configurationsChangeExtension.getRoomName());
        assertNull(configurationsChangeExtension.getSubject());
        assertEquals("blue", configurationsChangeExtension.getCustomConfigs().get("color"));
        assertEquals(messageWithConfigsChangeExample, configurationsMessage.toXML().toString());
    }

}
