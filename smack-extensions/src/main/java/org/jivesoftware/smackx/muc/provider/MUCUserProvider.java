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


import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.jivesoftware.smackx.muc.packet.MUCUser;
import org.xmlpull.v1.XmlPullParser;

/**
 * The MUCUserProvider parses packets with extended presence information about 
 * roles and affiliations.
 *
 * @author Gaston Dombiak
 */
public class MUCUserProvider implements PacketExtensionProvider {

    /**
     * Parses a MUCUser packet (extension sub-packet).
     *
     * @param parser the XML parser, positioned at the starting element of the extension.
     * @return a PacketExtension.
     * @throws Exception if a parsing error occurs.
     */
    public PacketExtension parseExtension(XmlPullParser parser) throws Exception {
        MUCUser mucUser = new MUCUser();
        boolean done = false;
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals("invite")) {
                    mucUser.setInvite(parseInvite(parser));
                }
                if (parser.getName().equals("item")) {
                    mucUser.setItem(MUCParserUtils.parseItem(parser));
                }
                if (parser.getName().equals("password")) {
                    mucUser.setPassword(parser.nextText());
                }
                if (parser.getName().equals("status")) {
                    String statusString = parser.getAttributeValue("", "code");
                    mucUser.addStatusCode(MUCUser.Status.create(statusString));
                }
                if (parser.getName().equals("decline")) {
                    mucUser.setDecline(parseDecline(parser));
                }
                if (parser.getName().equals("destroy")) {
                    mucUser.setDestroy(MUCParserUtils.parseDestroy(parser));
                }
            }
            else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals("x")) {
                    done = true;
                }
            }
        }

        return mucUser;
    }

    private static MUCUser.Invite parseInvite(XmlPullParser parser) throws Exception {
        boolean done = false;
        MUCUser.Invite invite = new MUCUser.Invite();
        invite.setFrom(parser.getAttributeValue("", "from"));
        invite.setTo(parser.getAttributeValue("", "to"));
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals("reason")) {
                    invite.setReason(parser.nextText());
                }
            }
            else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals("invite")) {
                    done = true;
                }
            }
        }
        return invite;
    }

    private static MUCUser.Decline parseDecline(XmlPullParser parser) throws Exception {
        boolean done = false;
        MUCUser.Decline decline = new MUCUser.Decline();
        decline.setFrom(parser.getAttributeValue("", "from"));
        decline.setTo(parser.getAttributeValue("", "to"));
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals("reason")) {
                    decline.setReason(parser.nextText());
                }
            }
            else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals("decline")) {
                    done = true;
                }
            }
        }
        return decline;
    }
}
