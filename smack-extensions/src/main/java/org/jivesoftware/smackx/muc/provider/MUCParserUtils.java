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

import org.jivesoftware.smackx.muc.MUCAffiliation;
import org.jivesoftware.smackx.muc.MUCRole;
import org.jivesoftware.smackx.muc.packet.Destroy;
import org.jivesoftware.smackx.muc.packet.MUCItem;
import org.xmlpull.v1.XmlPullParser;

public class MUCParserUtils {
    public static MUCItem parseItem(XmlPullParser parser) throws Exception {
        int initialDepth = parser.getDepth();
        MUCAffiliation affiliation = MUCAffiliation.fromString(parser.getAttributeValue("", "affiliation"));
        String nick = parser.getAttributeValue("", "nick");
        MUCRole role = MUCRole.fromString(parser.getAttributeValue("", "role"));
        String jid = parser.getAttributeValue("", "jid");
        String actor = null;
        String reason = null;
        outerloop: while (true) {
            int eventType = parser.next();
            switch (eventType) {
            case XmlPullParser.START_TAG:
                String name = parser.getName();
                switch (name) {
                case "actor":
                    actor = parser.getAttributeValue("", "jid");
                    break;
                case "reason":
                    reason = parser.nextText();
                    break;
                }
            case XmlPullParser.END_TAG:
                if (parser.getDepth() == initialDepth) {
                    break outerloop;
                }
                break;
            }
        }
        return new MUCItem(affiliation, role, actor, reason, jid, nick);
    }

    public static Destroy parseDestroy(XmlPullParser parser) throws Exception {
        boolean done = false;
        Destroy destroy = new Destroy();
        destroy.setJid(parser.getAttributeValue("", "jid"));
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals("reason")) {
                    destroy.setReason(parser.nextText());
                }
            }
            else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals("destroy")) {
                    done = true;
                }
            }
        }
        return destroy;
    }
}
