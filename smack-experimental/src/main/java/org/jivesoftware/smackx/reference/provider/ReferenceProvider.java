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

import java.net.URI;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.util.ParserUtils;
import org.jivesoftware.smackx.reference.element.ReferenceElement;

import org.xmlpull.v1.XmlPullParser;

public class ReferenceProvider extends ExtensionElementProvider<ReferenceElement> {

    public static final ReferenceProvider TEST_PROVIDER = new ReferenceProvider();

    @Override
    public ReferenceElement parse(XmlPullParser parser, int initialDepth) throws Exception {
        Integer begin = ParserUtils.getIntegerAttribute(parser, ReferenceElement.ATTR_BEGIN);
        Integer end =   ParserUtils.getIntegerAttribute(parser, ReferenceElement.ATTR_END);
        String typeString = parser.getAttributeValue(null, ReferenceElement.ATTR_TYPE);
        ReferenceElement.Type type = ReferenceElement.Type.valueOf(typeString);
        String anchor = parser.getAttributeValue(null, ReferenceElement.ATTR_ANCHOR);
        String uriString = parser.getAttributeValue(null, ReferenceElement.ATTR_URI);
        URI uri = uriString != null ? new URI(uriString) : null;
        ExtensionElement child = null;
        outerloop: while (true) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                String elementName = parser.getName();
                String namespace = parser.getNamespace();
                ExtensionElementProvider<?> provider = ProviderManager.getExtensionProvider(elementName, namespace);
                if (provider != null) {
                    child = provider.parse(parser);
                }
            }
            if (eventType == XmlPullParser.END_TAG) {
                break outerloop;
            }
        }

        return new ReferenceElement(begin, end, type, anchor, uri, child);
    }
}
