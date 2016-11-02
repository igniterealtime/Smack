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

import java.util.HashMap;

import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smackx.muclight.MUCLightAffiliation;
import org.jivesoftware.smackx.muclight.MUCLightRoomConfiguration;
import org.jivesoftware.smackx.muclight.element.MUCLightInfoIQ;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.xmlpull.v1.XmlPullParser;

/**
 * MUC Light info IQ provider class.
 * 
 * @author Fernando Ramirez
 *
 */
public class MUCLightInfoIQProvider extends IQProvider<MUCLightInfoIQ> {

    @Override
    public MUCLightInfoIQ parse(XmlPullParser parser, int initialDepth) throws Exception {
        String version = null;
        String roomName = null;
        String subject = null;
        HashMap<String, String> customConfigs = null;
        HashMap<Jid, MUCLightAffiliation> occupants = new HashMap<>();

        outerloop: while (true) {
            int eventType = parser.next();

            if (eventType == XmlPullParser.START_TAG) {

                if (parser.getName().equals("version")) {
                    version = parser.nextText();
                }
                if (parser.getName().equals("configuration")) {

                    int depth = parser.getDepth();

                    outerloop2: while (true) {
                        eventType = parser.next();

                        if (eventType == XmlPullParser.START_TAG) {
                            if (parser.getName().equals("roomname")) {
                                roomName = parser.nextText();
                            } else if (parser.getName().equals("subject")) {
                                subject = parser.nextText();
                            } else {
                                if (customConfigs == null) {
                                    customConfigs = new HashMap<>();
                                }
                                customConfigs.put(parser.getName(), parser.nextText());
                            }

                        } else if (eventType == XmlPullParser.END_TAG) {
                            if (parser.getDepth() == depth) {
                                break outerloop2;
                            }
                        }
                    }
                }

                if (parser.getName().equals("occupants")) {
                    occupants = iterateOccupants(parser);
                }

            } else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getDepth() == initialDepth) {
                    break outerloop;
                }
            }
        }

        return new MUCLightInfoIQ(version, new MUCLightRoomConfiguration(roomName, subject, customConfigs), occupants);
    }

    private HashMap<Jid, MUCLightAffiliation> iterateOccupants(XmlPullParser parser) throws Exception {
        HashMap<Jid, MUCLightAffiliation> occupants = new HashMap<>();
        int depth = parser.getDepth();

        outerloop: while (true) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals("user")) {
                    MUCLightAffiliation affiliation = MUCLightAffiliation
                            .fromString(parser.getAttributeValue("", "affiliation"));
                    occupants.put(JidCreate.from(parser.nextText()), affiliation);
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getDepth() == depth) {
                    break outerloop;
                }
            }
        }

        return occupants;
    }

}
