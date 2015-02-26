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
package org.jivesoftware.smackx.rsm.provider;

import java.io.IOException;

import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.util.ParserUtils;
import org.jivesoftware.smackx.rsm.packet.RSMSet;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class RSMSetProvider extends ExtensionElementProvider<RSMSet> {

    @Override
    public RSMSet parse(XmlPullParser parser, int initialDepth)
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
            int event = parser.next();
            switch (event) {
            case XmlPullParser.START_TAG:
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
            case XmlPullParser.END_TAG:
                if (parser.getDepth() == initialDepth) {
                    break outerloop;
                }
            }
        }
        return new RSMSet(after, before, count, index, last, max, firstString,
                        firstIndex);
    }

}
