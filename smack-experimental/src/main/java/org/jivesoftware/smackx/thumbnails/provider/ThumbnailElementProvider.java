/*
 *
 * Copyright 2023 Paul Schaub
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
package org.jivesoftware.smackx.thumbnails.provider;

import java.io.IOException;
import java.text.ParseException;

import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.util.ParserUtils;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.jivesoftware.smackx.thumbnails.element.ThumbnailElement;

import org.jxmpp.JxmppContext;

public class ThumbnailElementProvider extends ExtensionElementProvider<ThumbnailElement> {
    @Override
    public ThumbnailElement parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment, JxmppContext jxmppContext)
            throws XmlPullParserException, IOException, SmackParsingException, ParseException {
        String uri = parser.getAttributeValue(ThumbnailElement.ELEM_URI);
        String mediaType = parser.getAttributeValue(ThumbnailElement.ELEM_MEDIA_TYPE);
        Integer width = ParserUtils.getIntegerAttribute(parser, ThumbnailElement.ELEM_WIDTH);
        Integer height = ParserUtils.getIntegerAttribute(parser, ThumbnailElement.ELEM_HEIGHT);

        return new ThumbnailElement(
                uri,
                mediaType,
                width,
                height
        );
    }
}
