/**
 *
 * Copyright 2013 Robin Collier
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
package org.jivesoftware.smack.test.util;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smack.util.ParserUtils;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public final class TestUtils {
    private TestUtils() {
    }

    public static XmlPullParser getIQParser(String stanza) {
        return getParser(stanza, "iq");
    }

    public static XmlPullParser getMessageParser(String stanza) {
        return getParser(stanza, "message");
    }

    public static XmlPullParser getPresenceParser(String stanza) {
        return getParser(stanza, "presence");
    }

    public static XmlPullParser getParser(String string) {
        return getParser(string, null);
    }

    public static XmlPullParser getParser(String string, String startTag) {
        return getParser(new StringReader(string), startTag);
    }

    public static XmlPullParser getParser(Reader reader, String startTag) {
        XmlPullParser parser;
        try {
            parser = PacketParserUtils.newXmppParser(reader);
            if (startTag == null) {
                while (parser.getEventType() != XmlPullParser.START_TAG) {
                    parser.next();
                }
                return parser;
            }
            boolean found = false;

            while (!found) {
                if ((parser.next() == XmlPullParser.START_TAG) && parser.getName().equals(startTag))
                    found = true;
            }

            if (!found)
                throw new IllegalArgumentException("Can not find start tag '" + startTag + "'");
        } catch (XmlPullParserException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return parser;
    }

    public static <EE extends ExtensionElement> EE parseExtensionElement(String elementString)
                    throws Exception {
        return parseExtensionElement(getParser(elementString), null);
    }

    @SuppressWarnings("unchecked")
    public static <EE extends ExtensionElement> EE parseExtensionElement(XmlPullParser parser, XmlEnvironment outerXmlEnvironment)
                    throws Exception {
        ParserUtils.assertAtStartTag(parser);
        final String elementName = parser.getName();
        final String namespace = parser.getNamespace();
        return (EE) PacketParserUtils.parseExtensionElement(elementName, namespace, parser, outerXmlEnvironment);
    }
}
