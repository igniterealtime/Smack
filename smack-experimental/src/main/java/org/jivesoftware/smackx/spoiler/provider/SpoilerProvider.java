/**
 *
 * Copyright 2018 Paul Schaub
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
package org.jivesoftware.smackx.spoiler.provider;

import static org.xmlpull.v1.XmlPullParser.END_TAG;
import static org.xmlpull.v1.XmlPullParser.TEXT;

import java.io.IOException;

import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.util.ParserUtils;
import org.jivesoftware.smackx.spoiler.element.SpoilerElement;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public class SpoilerProvider extends ExtensionElementProvider<SpoilerElement> {

    public static SpoilerProvider INSTANCE = new SpoilerProvider();

    @Override
    public SpoilerElement parse(XmlPullParser parser, int initialDepth) throws XmlPullParserException, IOException {
        String lang = ParserUtils.getXmlLang(parser);
        String hint = null;

        outerloop: while (true) {
            int tag = parser.next();
            switch (tag) {
                case TEXT:
                    hint = parser.getText();
                    break;
                case END_TAG:
                    break outerloop;
            }
        }
        return new SpoilerElement(lang, hint);
    }
}
