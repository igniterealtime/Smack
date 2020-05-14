/**
 *
 * Copyright 2020 Paul Schaub
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
package org.jivesoftware.smackx.message_retraction.provider;

import java.io.IOException;
import java.util.Date;

import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.util.ParserUtils;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;
import org.jivesoftware.smackx.message_retraction.element.RetractedElement;
import org.jivesoftware.smackx.sid.StableUniqueStanzaIdManager;
import org.jivesoftware.smackx.sid.element.OriginIdElement;
import org.jivesoftware.smackx.sid.provider.OriginIdProvider;

public class RetractedElementProvider extends ExtensionElementProvider<RetractedElement> {

    @Override
    public RetractedElement parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment)
            throws XmlPullParserException, IOException, SmackParsingException {
        Date date = ParserUtils.getDateFromXep82String(parser.getAttributeValue("", RetractedElement.ATTR_STAMP));

        OriginIdElement originIdElement = null;
        while (originIdElement == null) {
            XmlPullParser.TagEvent tag = parser.nextTag();
            if (tag == XmlPullParser.TagEvent.START_ELEMENT
                    && OriginIdElement.ELEMENT.equals(parser.getName())
                    && StableUniqueStanzaIdManager.NAMESPACE.equals(parser.getNamespace())) {
                originIdElement = OriginIdProvider.INSTANCE.parse(parser);
            }
        }

        return new RetractedElement(date, originIdElement);
    }
}
