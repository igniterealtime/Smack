/**
 *
 * Copyright © 2014 Florian Schmaus
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

import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smack.sm.packet.StreamManagement.AckAnswer;
import org.jivesoftware.smack.sm.packet.StreamManagement.AckRequest;
import org.jivesoftware.smack.sm.packet.StreamManagement.Enabled;
import org.jivesoftware.smack.sm.packet.StreamManagement.Failed;
import org.jivesoftware.smack.sm.packet.StreamManagement.Resumed;
import org.jivesoftware.smack.util.ParserUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

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
        XMPPError.Condition condition = null;
        outerloop:
        while(true) {
            int event = parser.next();
            switch (event) {
            case XmlPullParser.START_TAG:
                name = parser.getName();
                String namespace = parser.getNamespace();
                if (XMPPError.NAMESPACE.equals(namespace)) {
                    condition = XMPPError.Condition.fromString(name);
                }
                break;
            case XmlPullParser.END_TAG:
                name = parser.getName();
                if (Failed.ELEMENT.equals(name)) {
                    break outerloop;
                }
                break;
            }
        }
        ParserUtils.assertAtEndTag(parser);
        return new Failed(condition);
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
