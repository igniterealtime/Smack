/**
 *
 * Copyright © 2014-2019 Florian Schmaus
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
package org.jivesoftware.smackx.rsm.provider;

import java.io.IOException;

import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.util.ParserUtils;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.jivesoftware.smackx.rsm.packet.RSMSet;

public class RSMSetProvider extends ExtensionElementProvider<RSMSet> {

    public static final RSMSetProvider INSTANCE = new RSMSetProvider();

    @Override
    public RSMSet parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment)
                    throws XmlPullParserException, IOException {
        String after = null;
        String before = null;
        int count = -1;
        int index = -1;
        String last = null;
        int max = -1;
        String firstString = null;
        int firstIndex = -1;

        outerloop: while (true) {
            XmlPullParser.Event event = parser.next();
            switch (event) {
            case START_ELEMENT:
                String name = parser.getName();
                switch (name) {
                case "after":
                    after = parser.nextText();
                    break;
                case "before":
                    before = parser.nextText();
                    break;
                case "count":
                    count = ParserUtils.getIntegerFromNextText(parser);
                    break;
                case "first":
                    firstIndex = ParserUtils.getIntegerAttribute(parser,
                                    "index", -1);
                    firstString = parser.nextText();
                    break;
                case "index":
                    index = ParserUtils.getIntegerFromNextText(parser);
                    break;
                case "last":
                    last = parser.nextText();
                    break;
                case "max":
                    max = ParserUtils.getIntegerFromNextText(parser);
                    break;
                }
                break;
            case END_ELEMENT:
                if (parser.getDepth() == initialDepth) {
                    break outerloop;
                }
                break;
            default:
                // Catch all for incomplete switch (MissingCasesInEnumSwitch) statement.
                break;
            }
        }
        return new RSMSet(after, before, count, index, last, max, firstString,
                        firstIndex);
    }

}
