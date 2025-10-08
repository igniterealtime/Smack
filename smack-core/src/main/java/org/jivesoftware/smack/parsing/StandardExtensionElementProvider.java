/*
 *
 * Copyright 2015-2019 Florian Schmaus.
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
package org.jivesoftware.smack.parsing;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

import org.jivesoftware.smack.packet.StandardExtensionElement;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.util.ParserUtils;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.jxmpp.JxmppContext;

/**
 * The parser for {@link StandardExtensionElement}s.
 *
 * @author Florian Schmaus
 *
 */
public class StandardExtensionElementProvider extends ExtensionElementProvider<StandardExtensionElement> {

    public static StandardExtensionElementProvider INSTANCE = new StandardExtensionElementProvider();

    @Override
    public StandardExtensionElement parse(final XmlPullParser parser, final int initialDepth, XmlEnvironment xmlEnvironment, JxmppContext jxmppContext)
                    throws XmlPullParserException, IOException {
        // Unlike most (all?) other providers, we don't know the name and namespace of the element
        // we are parsing here.
        String name = parser.getName();
        String namespace = parser.getNamespace();
        StandardExtensionElement.Builder builder = StandardExtensionElement.builder(name, namespace);
        final int namespaceCount = parser.getNamespaceCount();
        final int attributeCount = parser.getAttributeCount();
        final Map<String, String> attributes = new LinkedHashMap<>(namespaceCount + attributeCount);
        for (int i = 0; i < namespaceCount; i++) {
            String nsprefix = parser.getNamespacePrefix(i);
            if (nsprefix == null) {
                // Skip the default namespace.
                continue;
            }
            // XmlPullParser must either return null or a non-empty String.
            assert StringUtils.isNotEmpty(nsprefix);
            String nsuri = parser.getNamespaceUri(i);
            attributes.put("xmlns:" + nsprefix, nsuri);
        }
        for (int i = 0; i < attributeCount; i++) {
            String attributePrefix = parser.getAttributePrefix(i);
            String attributeName = parser.getAttributeName(i);
            String attributeValue = parser.getAttributeValue(i);
            String attributeKey;
            if (StringUtils.isNullOrEmpty(attributePrefix)) {
                attributeKey = attributeName;
            }
            else {
                attributeKey = attributePrefix + ':' + attributeName;
            }
            attributes.put(attributeKey, attributeValue);
        }
        builder.addAttributes(attributes);

        outerloop: while (true) {
            XmlPullParser.Event event = parser.next();
            switch (event) {
            case START_ELEMENT:
                var element = parse(parser, parser.getDepth(), xmlEnvironment, jxmppContext);
                builder.addElement(element);
                break;
            case TEXT_CHARACTERS:
                builder.setText(parser.getText());
                break;
            case END_ELEMENT:
                if (initialDepth == parser.getDepth()) {
                    break outerloop;
                }
                break;
            default:
                // Catch all for incomplete switch (MissingCasesInEnumSwitch) statement.
                break;
            }
        }

        ParserUtils.assertAtEndTag(parser);
        return builder.build();
    }
}
