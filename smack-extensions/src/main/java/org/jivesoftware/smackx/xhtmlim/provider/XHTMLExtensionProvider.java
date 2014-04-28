/**
 *
 * Copyright 2003-2007 Jive Software, 2014 Vyacheslav Blinov
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
package org.jivesoftware.smackx.xhtmlim.provider;

import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.xhtmlim.packet.XHTMLExtension;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

/**
 * The XHTMLExtensionProvider parses XHTML packets.
 *
 * @author Vyacheslav Blinov
 */
public class XHTMLExtensionProvider implements PacketExtensionProvider {
    public static final String BODY_ELEMENT = "body";

    @Override
    public PacketExtension parseExtension(XmlPullParser parser) throws IOException, XmlPullParserException {
        XHTMLExtension xhtmlExtension = new XHTMLExtension();
        final String XHTML_EXTENSION_ELEMENT_NAME = xhtmlExtension.getElementName();

        int startDepth = parser.getDepth();
        int tagDepth = parser.getDepth();
        boolean tagStarted = false;
        StringBuilder buffer = new StringBuilder();

        while (true) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                boolean appendNamespace = false;
                if (BODY_ELEMENT.equals(parser.getName())) {
                    buffer = new StringBuilder();
                    tagDepth = parser.getDepth();
                    appendNamespace = true;
                }
                maybeCloseTag(tagStarted, buffer);
                appendStartTagPartial(buffer, parser, appendNamespace);
                tagStarted = true;
            } else if (eventType == XmlPullParser.TEXT) {
                tagStarted = maybeCloseTag(tagStarted, buffer);
                appendText(buffer, parser);
            } else if (eventType == XmlPullParser.END_TAG) {
                String name = parser.getName();
                if (XHTML_EXTENSION_ELEMENT_NAME.equals(name) && parser.getDepth() <= startDepth) {
                    return xhtmlExtension;
                } else {
                    // xpp does not allows us to detect if tag is self-closing, so we have to
                    // handle self-closing tags by our own means
                    appendEndTag(buffer, parser, tagStarted);
                    tagStarted = false;
                    if (BODY_ELEMENT.equals(name) && parser.getDepth() <= tagDepth) {
                        xhtmlExtension.addBody(buffer.toString());
                    }
                }
            }
        }
    }

    private static void appendStartTagPartial(StringBuilder builder, XmlPullParser parser, boolean withNamespace) {
        builder.append('<');

        String prefix = parser.getPrefix();
        if (StringUtils.isNotEmpty(prefix)) {
            builder.append(prefix).append(':');
        }
        builder.append(parser.getName());

        int attributesCount = parser.getAttributeCount();
        // handle namespace
        if (withNamespace) {
            String namespace = parser.getNamespace();
            if (StringUtils.isNotEmpty(namespace)) {
                builder.append(" xmlns='").append(namespace).append('\'');
            }
        }
        // handle attributes
        for (int i = 0; i < attributesCount; ++i) {
            builder.append(' ');
            String attributeNamespace = parser.getAttributeNamespace(i);
            if (StringUtils.isNotEmpty(attributeNamespace)) {
                builder.append(attributeNamespace).append(':');
            }
            builder.append(parser.getAttributeName(i));
            String value = parser.getAttributeValue(i);
            if (value != null) {
                // We need to return valid XML so any inner text needs to be re-escaped
                builder.append("='").append(StringUtils.escapeForXML(value)).append('\'');
            }
        }
    }


    private static void appendEndTag(StringBuilder builder, XmlPullParser parser, boolean tagStarted) {
        if (tagStarted) {
            builder.append("/>");
        } else {
            builder.append("</").append(parser.getName()).append('>');
        }
    }

    private static boolean appendText(StringBuilder builder, XmlPullParser parser) {
        String text = parser.getText();
        if (text == null) {
            return false;
        } else {
            // We need to return valid XML so any inner text needs to be re-escaped
            builder.append(StringUtils.escapeForXML(parser.getText()));
            return true;
        }
    }

    private static boolean maybeCloseTag(boolean tagStarted, StringBuilder builder) {
        if (tagStarted) {
            builder.append('>');
        }
        return false;
    }
}
