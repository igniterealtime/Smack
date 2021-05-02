/**
 *
 * Copyright 2014 Georg Lukas, 2021 Florian Schmaus.
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
import org.jivesoftware.smack.packet.IqData;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.provider.IqProvider;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.jivesoftware.smackx.iqversion.packet.Version;
import org.jivesoftware.smackx.iqversion.packet.VersionBuilder;

public class VersionProvider extends IqProvider<Version> {

    @Override
    public Version parse(XmlPullParser parser, int initialDepth, IqData iqData, XmlEnvironment xmlEnvironment)
                    throws XmlPullParserException, IOException {
        VersionBuilder versionBuilder = Version.builder(iqData);

        outerloop: while (true) {
            XmlPullParser.Event eventType = parser.next();
            switch (eventType) {
            case START_ELEMENT:
                String tagName = parser.getName();
                switch (tagName) {
                case "name":
                    String name = parser.nextText();
                    versionBuilder.setName(name);
                    break;
                case "version":
                    String version = parser.nextText();
                    versionBuilder.setVersion(version);
                    break;
                case "os":
                    String os = parser.nextText();
                    versionBuilder.setOs(os);
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

        return versionBuilder.build();
    }
}
