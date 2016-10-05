/**
 *
 * Copyright © 2016 Fernando Ramirez
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
package org.jivesoftware.smack.isr.provider;

import java.io.IOException;

import org.jivesoftware.smack.isr.element.InstantStreamResumption;
import org.jivesoftware.smack.isr.element.InstantStreamResumption.Enabled;
import org.jivesoftware.smack.isr.element.InstantStreamResumption.Failed;
import org.jivesoftware.smack.isr.element.InstantStreamResumption.InstResumed;
import org.jivesoftware.smack.util.ParserUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

/**
 * Parse Instant Stream Resumption class.
 * 
 * @author Fernando Ramirez
 * @see <a href="http://xmpp.org/extensions/inbox/isr.html">XEP-xxxx: Instant
 *      Stream Resumption</a>
 * 
 */
public class ParseInstantStreamResumption {

    public static Enabled enabled(XmlPullParser parser) throws XmlPullParserException, IOException {
        ParserUtils.assertAtStartTag(parser);
        String key = parser.getAttributeValue(InstantStreamResumption.NAMESPACE, "key");
        String location = parser.getAttributeValue(InstantStreamResumption.NAMESPACE, "location");
        parser.next();
        ParserUtils.assertAtEndTag(parser);
        return new Enabled(key, location);
    }

    public static Failed failed(XmlPullParser parser) throws XmlPullParserException, IOException {
        ParserUtils.assertAtStartTag(parser);
        Long handledCount = ParserUtils.getLongAttribute(parser, "h");
        parser.next();
        ParserUtils.assertAtEndTag(parser);

        if (handledCount == null) {
            return new Failed();
        } else {
            return new Failed(handledCount);
        }
    }

    public static InstResumed resumed(XmlPullParser parser) throws XmlPullParserException, IOException {
        // inst-resumed
        ParserUtils.assertAtStartTag(parser);
        String key = parser.getAttributeValue("", "key");
        Long handledCount = ParserUtils.getLongAttribute(parser, "h");

        // hmac
        parser.next();
        ParserUtils.assertAtStartTag(parser);

        // hash
        parser.next();
        ParserUtils.assertAtStartTag(parser);
        String algo = parser.getAttributeValue("", "algo");
        String hash = parser.nextText();
        ParserUtils.assertAtEndTag(parser);

        // close hmac
        parser.next();
        ParserUtils.assertAtEndTag(parser);

        // close inst-resumed
        parser.next();
        ParserUtils.assertAtEndTag(parser);

        if (handledCount == null) {
            return new InstResumed(key, hash, algo);
        } else {
            return new InstResumed(key, handledCount, hash, algo);
        }
    }

}
