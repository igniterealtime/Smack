/*
 *
 * Copyright © 2014-2018 Florian Schmaus
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
package org.jivesoftware.smack.sm.provider;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.smack.packet.AbstractTextElement;
import org.jivesoftware.smack.packet.StanzaError;
import org.jivesoftware.smack.packet.StanzaErrorTextElement;
import org.jivesoftware.smack.sm.packet.StreamManagement.AckAnswer;
import org.jivesoftware.smack.sm.packet.StreamManagement.AckRequest;
import org.jivesoftware.smack.sm.packet.StreamManagement.Enabled;
import org.jivesoftware.smack.sm.packet.StreamManagement.Failed;
import org.jivesoftware.smack.sm.packet.StreamManagement.Resumed;
import org.jivesoftware.smack.util.ParserUtils;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

public class ParseStreamManagement {

    public static Enabled enabled(XmlPullParser parser) throws XmlPullParserException, IOException {
        ParserUtils.assertAtStartTag(parser);
        boolean resume = ParserUtils.getBooleanAttribute(parser, "resume", false);
        String id = parser.getAttributeValue("", "id");
        String location = parser.getAttributeValue("", "location");
        int max = ParserUtils.getIntegerAttribute(parser, "max", -1);
        parser.next();
        ParserUtils.assertAtEndTag(parser);
        return new Enabled(id, resume, location, max);
    }

    public static Failed failed(XmlPullParser parser) throws XmlPullParserException, IOException {
        ParserUtils.assertAtStartTag(parser);
        String name;
        StanzaError.Condition condition = null;
        List<StanzaErrorTextElement> textElements = new ArrayList<>(4);
        outerloop:
        while (true) {
            XmlPullParser.Event event = parser.next();
            switch (event) {
            case START_ELEMENT:
                name = parser.getName();
                String namespace = parser.getNamespace();
                if (StanzaError.ERROR_CONDITION_AND_TEXT_NAMESPACE.equals(namespace)) {
                    if (name.equals(AbstractTextElement.ELEMENT)) {
                        String lang = ParserUtils.getXmlLang(parser);
                        String text = parser.nextText();
                        StanzaErrorTextElement stanzaErrorTextElement = new StanzaErrorTextElement(text, lang);
                        textElements.add(stanzaErrorTextElement);
                    } else {
                        condition = StanzaError.Condition.fromString(name);
                    }
                }
                break;
            case END_ELEMENT:
                name = parser.getName();
                if (Failed.ELEMENT.equals(name)) {
                    break outerloop;
                }
                break;
            default:
                // Catch all for incomplete switch (MissingCasesInEnumSwitch) statement.
                break;
            }
        }
        ParserUtils.assertAtEndTag(parser);
        return new Failed(condition, textElements);
    }

    public static Resumed resumed(XmlPullParser parser) throws XmlPullParserException, IOException {
        ParserUtils.assertAtStartTag(parser);
        long h = ParserUtils.getLongAttribute(parser, "h");
        String previd = parser.getAttributeValue("", "previd");
        parser.next();
        ParserUtils.assertAtEndTag(parser);
        return new Resumed(h, previd);
    }

    public static AckAnswer ackAnswer(XmlPullParser parser) throws XmlPullParserException, IOException {
        ParserUtils.assertAtStartTag(parser);
        long h = ParserUtils.getLongAttribute(parser, "h");
        parser.next();
        ParserUtils.assertAtEndTag(parser);
        return new AckAnswer(h);
    }

    public static AckRequest ackRequest(XmlPullParser parser) throws XmlPullParserException, IOException {
        ParserUtils.assertAtStartTag(parser);
        parser.next();
        ParserUtils.assertAtEndTag(parser);
        return AckRequest.INSTANCE;
    }
}
