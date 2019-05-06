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

import java.io.IOException;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.jivesoftware.smackx.iqversion.packet.Version;

public class VersionProvider extends IQProvider<Version> {

    @Override
    public Version parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment) throws XmlPullParserException, IOException {
        String name = null, version = null, os = null;

        outerloop: while (true) {
            XmlPullParser.Event eventType = parser.next();
            switch (eventType) {
            case START_ELEMENT:
                String tagName = parser.getName();
                switch (tagName) {
                case "name":
                    name = parser.nextText();
                    break;
                case "version":
                    version = parser.nextText();
                    break;
                case "os":
                    os = parser.nextText();
                    break;
                }
                break;
            case END_ELEMENT:
                if (parser.getDepth() == initialDepth && parser.getName().equals(IQ.QUERY_ELEMENT)) {
                    break outerloop;
                }
                break;
            default:
                // Catch all for incomplete switch (MissingCasesInEnumSwitch) statement.
                break;
            }
        }
        if (name == null && version == null && os == null) {
            return new Version();
        }
        return new Version(name, version, os);
    }
}
