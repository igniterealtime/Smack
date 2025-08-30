/**
 *
 * Copyright 2003-2007 Jive Software.
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

import java.io.IOException;

import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.util.ParserUtils;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.jivesoftware.smackx.muc.packet.MUCUser;

import org.jxmpp.JxmppContext;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.EntityJid;

/**
 * The MUCUserProvider parses packets with extended presence information about
 * roles and affiliations.
 *
 * @author Gaston Dombiak
 */
public class MUCUserProvider extends ExtensionElementProvider<MUCUser> {

    /**
     * Parses a MUCUser stanza (extension sub-packet).
     *
     * @param parser the XML parser, positioned at the starting element of the extension.
     * @return a PacketExtension.
     * @throws IOException if an I/O error occurred.
     * @throws XmlPullParserException if an error in the XML parser occurred.
     */
    @Override
    public MUCUser parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment, JxmppContext jxmppContext) throws XmlPullParserException, IOException {
        MUCUser mucUser = new MUCUser();
        outerloop: while (true) {
            switch (parser.next()) {
            case START_ELEMENT:
                switch (parser.getName()) {
                case "invite":
                    var invite = parseInvite(parser, jxmppContext);
                    mucUser.setInvite(invite);
                    break;
                case "item":
                    var item = MUCParserUtils.parseItem(parser, jxmppContext);
                    mucUser.setItem(item);
                    break;
                case "password":
                    mucUser.setPassword(parser.nextText());
                    break;
                case "status":
                    String statusString = parser.getAttributeValue("", "code");
                    mucUser.addStatusCode(MUCUser.Status.create(statusString));
                    break;
                case "decline":
                    var decline = parseDecline(parser, jxmppContext);
                    mucUser.setDecline(decline);
                    break;
                case "destroy":
                    var destroy = MUCParserUtils.parseDestroy(parser, jxmppContext);
                    mucUser.setDestroy(destroy);
                    break;
                }
                break;
            case END_ELEMENT:
                if (parser.getDepth() == initialDepth) {
                    break outerloop;
                }
                break;
            default:
                // Catch all for incomplete switch (MissingCasesInEnumSwitch) statement.
                break;
            }
        }

        return mucUser;
    }

    private static MUCUser.Invite parseInvite(XmlPullParser parser, JxmppContext jxmppContext) throws XmlPullParserException, IOException {
        String reason = null;
        EntityBareJid to = ParserUtils.getBareJidAttribute(parser, "to", jxmppContext);
        EntityJid from = ParserUtils.getEntityJidAttribute(parser, "from", jxmppContext);

        outerloop: while (true) {
            XmlPullParser.Event eventType = parser.next();
            if (eventType == XmlPullParser.Event.START_ELEMENT) {
                if (parser.getName().equals("reason")) {
                    reason = parser.nextText();
                }
            }
            else if (eventType == XmlPullParser.Event.END_ELEMENT) {
                if (parser.getName().equals("invite")) {
                    break outerloop;
                }
            }
        }
        return new MUCUser.Invite(reason, from, to);
    }

    private static MUCUser.Decline parseDecline(XmlPullParser parser, JxmppContext jxmppContext) throws XmlPullParserException, IOException {
        String reason = null;
        EntityBareJid to = ParserUtils.getBareJidAttribute(parser, "to", jxmppContext);
        EntityBareJid from = ParserUtils.getBareJidAttribute(parser, "from", jxmppContext);

        outerloop: while (true) {
            XmlPullParser.Event eventType = parser.next();
            if (eventType == XmlPullParser.Event.START_ELEMENT) {
                if (parser.getName().equals("reason")) {
                    reason = parser.nextText();
                }
            }
            else if (eventType == XmlPullParser.Event.END_ELEMENT) {
                if (parser.getName().equals("decline")) {
                    break outerloop;
                }
            }
        }
        return new MUCUser.Decline(reason, from, to);
    }
}
