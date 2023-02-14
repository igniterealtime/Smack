/**
 *
 * Copyright 2018-2019 Paul Schaub.
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
package org.jivesoftware.smackx.ox.provider;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;

import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.util.ParserUtils;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.jivesoftware.smackx.ox.element.PubkeyElement;

/**
 * {@link ExtensionElementProvider} implementation for the {@link PubkeyElement}.
 */
public class PubkeyElementProvider extends ExtensionElementProvider<PubkeyElement> {

    public static final PubkeyElementProvider INSTANCE = new PubkeyElementProvider();

    @Override
    public PubkeyElement parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment)
                    throws XmlPullParserException, IOException, ParseException {
        String dateString = parser.getAttributeValue(null, PubkeyElement.ATTR_DATE);
        Date date = ParserUtils.getDateFromOptionalXep82String(dateString);
        while (true) {
            XmlPullParser.Event tag = parser.next();
            if (tag == XmlPullParser.Event.START_ELEMENT) {
                String name = parser.getName();
                switch (name) {
                    case PubkeyElement.PubkeyDataElement.ELEMENT:
                        String base64EncodedOpenPgpPubKey = parser.nextText();
                        PubkeyElement.PubkeyDataElement pubkeyDataElement = new PubkeyElement.PubkeyDataElement(base64EncodedOpenPgpPubKey);
                        return new PubkeyElement(pubkeyDataElement, date);
                }
            }
        }
    }
}
