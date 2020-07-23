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
package org.jivesoftware.smackx.stanza_content_encryption.provider;

import java.io.IOException;
import java.util.Date;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smack.util.ParserUtils;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;
import org.jivesoftware.smackx.stanza_content_encryption.element.AffixElement;
import org.jivesoftware.smackx.stanza_content_encryption.element.ContentElement;
import org.jivesoftware.smackx.stanza_content_encryption.element.FromAffixElement;
import org.jivesoftware.smackx.stanza_content_encryption.element.PayloadElement;
import org.jivesoftware.smackx.stanza_content_encryption.element.RandomPaddingAffixElement;
import org.jivesoftware.smackx.stanza_content_encryption.element.TimestampAffixElement;
import org.jivesoftware.smackx.stanza_content_encryption.element.ToAffixElement;

import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

public class ContentElementProvider extends ExtensionElementProvider<ContentElement> {

    @Override
    public ContentElement parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment)
            throws XmlPullParserException, IOException, SmackParsingException {
        ContentElement.Builder builder = ContentElement.builder();

        while (true) {
            XmlPullParser.Event tag = parser.next();
            if (tag == XmlPullParser.Event.START_ELEMENT) {
                String name = parser.getName();
                switch (name) {
                    case ToAffixElement.ELEMENT:
                        parseToAffix(parser, builder);
                        break;

                    case FromAffixElement.ELEMENT:
                        parseFromAffix(parser, builder);
                        break;

                    case TimestampAffixElement.ELEMENT:
                        parseTimestampAffix(parser, builder);
                        break;

                    case RandomPaddingAffixElement.ELEMENT:
                        parseRPadAffix(parser, builder);
                        break;

                    case PayloadElement.ELEMENT:
                        parsePayload(parser, xmlEnvironment, builder);
                        break;

                    default:
                        parseCustomAffix(parser, xmlEnvironment, builder);
                        break;
                }
            } else if (tag == XmlPullParser.Event.END_ELEMENT) {
                if (parser.getDepth() == initialDepth) {
                    break;
                }
            }
        }
        return builder.build();
    }

    private static void parseCustomAffix(XmlPullParser parser, XmlEnvironment outerXmlEnvironment, ContentElement.Builder builder)
            throws XmlPullParserException, IOException, SmackParsingException {
        String name = parser.getName();
        String namespace = parser.getNamespace();

        AffixElement element = (AffixElement) PacketParserUtils.parseExtensionElement(name, namespace, parser, outerXmlEnvironment);
        builder.addFurtherAffixElement(element);
    }

    private static void parsePayload(XmlPullParser parser, XmlEnvironment outerXmlEnvironment, ContentElement.Builder builder)
            throws IOException, XmlPullParserException, SmackParsingException {
        final int initialDepth = parser.getDepth();
        while (true) {
            XmlPullParser.Event tag = parser.next();

            if (tag == XmlPullParser.Event.START_ELEMENT) {
                String name = parser.getName();
                String namespace = parser.getNamespace();
                ExtensionElement element = PacketParserUtils.parseExtensionElement(name, namespace, parser, outerXmlEnvironment);
                builder.addPayloadItem(element);
            }

            if (tag == XmlPullParser.Event.END_ELEMENT && parser.getDepth() == initialDepth) {
                return;
            }
        }
    }

    private static void parseRPadAffix(XmlPullParser parser, ContentElement.Builder builder)
            throws IOException, XmlPullParserException {
        builder.setRandomPadding(parser.nextText());
    }

    private static void parseTimestampAffix(XmlPullParser parser, ContentElement.Builder builder)
            throws SmackParsingException.SmackTextParseException {
        Date timestamp = ParserUtils.getDateFromXep82String(
                parser.getAttributeValue("", TimestampAffixElement.ATTR_STAMP));
        builder.setTimestamp(timestamp);
    }

    private static void parseFromAffix(XmlPullParser parser, ContentElement.Builder builder)
            throws XmppStringprepException {
        String jidString = parser.getAttributeValue("", FromAffixElement.ATTR_JID);
        builder.setFrom(JidCreate.from(jidString));
    }

    private static void parseToAffix(XmlPullParser parser, ContentElement.Builder builder)
            throws XmppStringprepException {
        String jidString = parser.getAttributeValue("", ToAffixElement.ATTR_JID);
        builder.addTo(JidCreate.from(jidString));
    }
}
