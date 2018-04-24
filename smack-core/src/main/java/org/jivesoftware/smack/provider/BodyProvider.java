/**
 *
 * Copyright © 2018 Paul Schaub
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

import org.jivesoftware.smack.packet.BodyElement;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.util.ParserUtils;

import org.xmlpull.v1.XmlPullParser;

public class BodyProvider extends ExtensionElementProvider<BodyElement> {

    public static final BodyProvider TEST_INSTANCE = new BodyProvider();

    @Override
    public BodyElement parse(XmlPullParser parser, int initialDepth) throws Exception {
        String xmlLang = ParserUtils.getXmlLang(parser);
        if (xmlLang == null) {
            xmlLang = Stanza.getDefaultLanguage();
        }

        String body = parser.nextText();
        return new BodyElement(xmlLang, body);
    }
}
