/**
 *
 * Copyright 2020 Paul Schaub.
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
package org.jivesoftware.smackx.muc.packet;

import static org.jivesoftware.smack.test.util.XmlAssertUtil.assertXmlSimilar;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;

import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.test.util.TestUtils;
import org.jivesoftware.smack.xml.XmlPullParserException;
import org.jivesoftware.smackx.muc.provider.GroupChatInvitationProvider;

import org.junit.jupiter.api.Test;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.impl.JidCreate;

public class GroupChatInvitationElementTest {
    private static final GroupChatInvitationProvider TEST_PROVIDER = new GroupChatInvitationProvider();

    private static final EntityBareJid mucJid = JidCreate.entityBareFromOrThrowUnchecked("darkcave@macbeth.shakespeare.lit");

    @Test
    public void serializeFullElement() throws XmlPullParserException, IOException, SmackParsingException {
        final String expectedXml = "" +
                "<x xmlns='jabber:x:conference'" +
                "     continue='true'" +
                "     jid='darkcave@macbeth.shakespeare.lit'" +
                "     password='cauldronburn'" +
                "     reason='Hey Hecate, this is the place for all good witches!'" +
                "     thread='e0ffe42b28561960c6b12b944a092794b9683a38'/>";

        GroupChatInvitation invitation = new GroupChatInvitation(mucJid,
                "Hey Hecate, this is the place for all good witches!",
                "cauldronburn",
                true,
                "e0ffe42b28561960c6b12b944a092794b9683a38");
        assertXmlSimilar(expectedXml, invitation.toXML());

        GroupChatInvitation parsed = TEST_PROVIDER.parse(TestUtils.getParser(expectedXml));
        assertEquals(invitation, parsed);
    }

    @Test
    public void serializeMinimalElementTest() throws XmlPullParserException, IOException, SmackParsingException {
        final String expectedXml = "<x xmlns='jabber:x:conference' jid='darkcave@macbeth.shakespeare.lit'/>";

        GroupChatInvitation invitation = new GroupChatInvitation(mucJid);
        assertXmlSimilar(expectedXml, invitation.toXML());

        GroupChatInvitation parsed = TEST_PROVIDER.parse(TestUtils.getParser(expectedXml));
        assertEquals(invitation, parsed);
    }
}
