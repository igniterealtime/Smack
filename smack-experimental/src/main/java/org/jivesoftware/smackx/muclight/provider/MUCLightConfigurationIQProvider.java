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

import org.jivesoftware.smack.provider.IQProvider;

import org.jivesoftware.smackx.muclight.MUCLightRoomConfiguration;
import org.jivesoftware.smackx.muclight.element.MUCLightConfigurationIQ;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * MUC Light configuration IQ provider class.
 *
 * @author Fernando Ramirez
 *
 */
public class MUCLightConfigurationIQProvider extends IQProvider<MUCLightConfigurationIQ> {

    @Override
    public MUCLightConfigurationIQ parse(XmlPullParser parser, int initialDepth) throws XmlPullParserException, IOException {
        String version = null;
        String roomName = null;
        String subject = null;
        HashMap<String, String> customConfigs = null;

        outerloop: while (true) {
            int eventType = parser.next();

            if (eventType == XmlPullParser.START_TAG) {

                if (parser.getName().equals("version")) {
                    version = parser.nextText();
                } else if (parser.getName().equals("roomname")) {
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
                if (parser.getDepth() == initialDepth) {
                    break outerloop;
                }
            }
        }

        MUCLightRoomConfiguration configuration = new MUCLightRoomConfiguration(roomName, subject, customConfigs);

        return new MUCLightConfigurationIQ(version, configuration);
    }

}
