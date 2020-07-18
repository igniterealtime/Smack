/**
 *
 * Copyright 2003-2007 Jive Software, 2020 Paul Schaub, 2022-2023 Florian Schmaus.
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
package org.jivesoftware.smackx.muc.provider;

import static org.jivesoftware.smackx.muc.packet.GroupChatInvitation.ATTR_CONTINUE;
import static org.jivesoftware.smackx.muc.packet.GroupChatInvitation.ATTR_PASSWORD;
import static org.jivesoftware.smackx.muc.packet.GroupChatInvitation.ATTR_REASON;
import static org.jivesoftware.smackx.muc.packet.GroupChatInvitation.ATTR_THREAD;

import java.io.IOException;

import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.util.ParserUtils;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;
import org.jivesoftware.smackx.muc.packet.GroupChatInvitation;

import org.jxmpp.jid.EntityBareJid;

public class GroupChatInvitationProvider extends ExtensionElementProvider<GroupChatInvitation> {

    @Override
    public GroupChatInvitation parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment)
            throws XmlPullParserException, IOException, SmackParsingException {

        EntityBareJid roomJid = ParserUtils.getBareJidAttribute(parser);
        String password = parser.getAttributeValue(ATTR_PASSWORD);
        String reason = parser.getAttributeValue(ATTR_REASON);
        boolean isContinue = ParserUtils.getBooleanAttribute(parser, ATTR_CONTINUE, false);
        String thread = parser.getAttributeValue(ATTR_THREAD);

        return new GroupChatInvitation(roomJid, reason, password, isContinue, thread);
    }
}
