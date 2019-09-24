/**
 *
 * Copyright 2019 Florian Schmaus
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
package org.jivesoftware.smack.provider;

import java.io.IOException;
import java.util.Map;

import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.sasl.packet.SaslNonza.SASLFailure;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

public final class SaslFailureProvider extends NonzaProvider<SASLFailure> {

    public static final SaslFailureProvider INSTANCE = new SaslFailureProvider();

    private SaslFailureProvider() {
    }

    @Override
    public SASLFailure parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment) throws XmlPullParserException, IOException {
        String condition = null;
        Map<String, String> descriptiveTexts = null;
        outerloop: while (true) {
            XmlPullParser.TagEvent eventType = parser.nextTag();
            switch (eventType) {
            case START_ELEMENT:
                String name = parser.getName();
                if (name.equals("text")) {
                    descriptiveTexts = PacketParserUtils.parseDescriptiveTexts(parser, descriptiveTexts);
                }
                else {
                    assert condition == null;
                    condition = parser.getName();
                }
                break;
            case END_ELEMENT:
                if (parser.getDepth() == initialDepth) {
                    break outerloop;
                }
                break;
            }
        }
        return new SASLFailure(condition, descriptiveTexts);
    }

}
