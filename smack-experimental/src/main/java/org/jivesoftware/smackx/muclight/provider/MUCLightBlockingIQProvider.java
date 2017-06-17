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
package org.jivesoftware.smackx.muclight.provider;

import java.io.IOException;
import java.util.HashMap;

import org.jivesoftware.smack.packet.IQ.Type;
import org.jivesoftware.smack.provider.IQProvider;

import org.jivesoftware.smackx.muclight.element.MUCLightBlockingIQ;

import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * MUC Light blocking IQ provider class.
 * 
 * @author Fernando Ramirez
 *
 */
public class MUCLightBlockingIQProvider extends IQProvider<MUCLightBlockingIQ> {

    @Override
    public MUCLightBlockingIQ parse(XmlPullParser parser, int initialDepth) throws Exception {
        HashMap<Jid, Boolean> rooms = null;
        HashMap<Jid, Boolean> users = null;

        outerloop: while (true) {
            int eventType = parser.next();

            if (eventType == XmlPullParser.START_TAG) {

                if (parser.getName().equals("room")) {
                    rooms = parseBlocking(parser, rooms);
                }

                if (parser.getName().equals("user")) {
                    users = parseBlocking(parser, users);
                }

            } else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getDepth() == initialDepth) {
                    break outerloop;
                }
            }
        }

        MUCLightBlockingIQ mucLightBlockingIQ = new MUCLightBlockingIQ(rooms, users);
        mucLightBlockingIQ.setType(Type.result);
        return mucLightBlockingIQ;
    }

    private HashMap<Jid, Boolean> parseBlocking(XmlPullParser parser, HashMap<Jid, Boolean> map)
            throws XmppStringprepException, XmlPullParserException, IOException {
        if (map == null) {
            map = new HashMap<>();
        }
        String action = parser.getAttributeValue("", "action");

        if (action.equals("deny")) {
            map.put(JidCreate.from(parser.nextText()), false);
        } else if (action.equals("allow")) {
            map.put(JidCreate.from(parser.nextText()), true);
        }
        return map;
    }

}
