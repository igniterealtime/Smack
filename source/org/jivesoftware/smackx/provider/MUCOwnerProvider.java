/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright 2003-2007 Jive Software.
 *
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
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

package org.jivesoftware.smackx.provider;

import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.provider.*;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smackx.packet.MUCOwner;
import org.xmlpull.v1.XmlPullParser;

/**
 * The MUCOwnerProvider parses MUCOwner packets. (@see MUCOwner)
 * 
 * @author Gaston Dombiak
 */
public class MUCOwnerProvider implements IQProvider {

    public IQ parseIQ(XmlPullParser parser) throws Exception {
        MUCOwner mucOwner = new MUCOwner();
        boolean done = false;
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals("item")) {
                    mucOwner.addItem(parseItem(parser));
                }
                else if (parser.getName().equals("destroy")) {
                    mucOwner.setDestroy(parseDestroy(parser));
                }
                // Otherwise, it must be a packet extension.
                else {
                    mucOwner.addExtension(PacketParserUtils.parsePacketExtension(parser.getName(),
                            parser.getNamespace(), parser));
                }
            }
            else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals("query")) {
                    done = true;
                }
            }
        }

        return mucOwner;
    }

    private MUCOwner.Item parseItem(XmlPullParser parser) throws Exception {
        boolean done = false;
        MUCOwner.Item item = new MUCOwner.Item(parser.getAttributeValue("", "affiliation"));
        item.setNick(parser.getAttributeValue("", "nick"));
        item.setRole(parser.getAttributeValue("", "role"));
        item.setJid(parser.getAttributeValue("", "jid"));
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals("actor")) {
                    item.setActor(parser.getAttributeValue("", "jid"));
                }
                if (parser.getName().equals("reason")) {
                    item.setReason(parser.nextText());
                }
            }
            else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals("item")) {
                    done = true;
                }
            }
        }
        return item;
    }

    private MUCOwner.Destroy parseDestroy(XmlPullParser parser) throws Exception {
        boolean done = false;
        MUCOwner.Destroy destroy = new MUCOwner.Destroy();
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
