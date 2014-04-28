/**
 *
 * Copyright 2014 Georg Lukas.
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

package org.jivesoftware.smackx.iqversion.provider;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smackx.iqversion.packet.Version;
import org.xmlpull.v1.XmlPullParser;

public class VersionProvider implements IQProvider {
    public IQ parseIQ(XmlPullParser parser) throws Exception {
        String name = null, version = null, os = null;

        boolean done = false;
        while (!done) {
            int eventType = parser.next();
            String tagName = parser.getName();
            if (eventType == XmlPullParser.START_TAG) {
                if (tagName.equals("name")) {
                    name = parser.nextText();
                }
                else if (tagName.equals("version")) {
                    version = parser.nextText();
                }
                else if (tagName.equals("os")) {
                    os = parser.nextText();
                }
            } else if (eventType == XmlPullParser.END_TAG && tagName.equals("query")) {
                done = true;
            }
        }
        return new Version(name, version, os);
    }
}
