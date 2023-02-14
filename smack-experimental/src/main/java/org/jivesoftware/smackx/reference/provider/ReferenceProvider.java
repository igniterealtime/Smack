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
package org.jivesoftware.smackx.reference.provider;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

import org.jivesoftware.smack.packet.XmlElement;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.util.ParserUtils;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.jivesoftware.smackx.reference.element.ReferenceElement;

public class ReferenceProvider extends ExtensionElementProvider<ReferenceElement> {

    public static final ReferenceProvider TEST_PROVIDER = new ReferenceProvider();

    @Override
    public ReferenceElement parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment) throws XmlPullParserException, IOException, SmackParsingException {
        Integer begin = ParserUtils.getIntegerAttribute(parser, ReferenceElement.ATTR_BEGIN);
        Integer end =   ParserUtils.getIntegerAttribute(parser, ReferenceElement.ATTR_END);
        String typeString = parser.getAttributeValue(null, ReferenceElement.ATTR_TYPE);
        ReferenceElement.Type type = ReferenceElement.Type.valueOf(typeString);
        String anchor = parser.getAttributeValue(null, ReferenceElement.ATTR_ANCHOR);
        String uriString = parser.getAttributeValue(null, ReferenceElement.ATTR_URI);
        URI uri;
        try {
            uri = uriString != null ? new URI(uriString) : null;
        }
        catch (URISyntaxException e) {
            // TODO: Should be SmackParseException and probably be factored into ParserUtils.
            throw new IOException(e);
        }
        XmlElement child = null;
        outerloop: while (true) {
            XmlPullParser.Event eventType = parser.next();
            if (eventType == XmlPullParser.Event.START_ELEMENT) {
                String elementName = parser.getName();
                String namespace = parser.getNamespace();
                ExtensionElementProvider<?> provider = ProviderManager.getExtensionProvider(elementName, namespace);
                if (provider != null) {
                    child = provider.parse(parser);
                }
            }
            else if (eventType == XmlPullParser.Event.END_ELEMENT) {
                break outerloop;
            }
        }

        return new ReferenceElement(begin, end, type, anchor, uri, child);
    }
}
