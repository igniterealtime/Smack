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

import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smackx.muclight.element.MUCLightElements.ConfigurationsChangeExtension;
import org.xmlpull.v1.XmlPullParser;

/**
 * MUC Light configurations change provider class.
 * 
 * @author Fernando Ramirez
 *
 */
public class MUCLightConfigurationsChangeProvider extends ExtensionElementProvider<ConfigurationsChangeExtension> {

    @Override
    public ConfigurationsChangeExtension parse(XmlPullParser parser, int initialDepth) throws Exception {
        String prevVersion = null;
        String version = null;
        String roomName = null;
        String subject = null;
        HashMap<String, String> customConfigs = null;

        outerloop: while (true) {
            int eventType = parser.next();

            if (eventType == XmlPullParser.START_TAG) {

                if (parser.getName().equals("prev-version")) {
                    prevVersion = parser.nextText();
                } else if (parser.getName().equals("version")) {
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

        return new ConfigurationsChangeExtension(prevVersion, version, roomName, subject, customConfigs);
    }

}
