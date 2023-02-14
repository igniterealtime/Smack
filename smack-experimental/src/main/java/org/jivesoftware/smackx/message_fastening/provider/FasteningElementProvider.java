/**
 *
 * Copyright 2019 Paul Schaub
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
package org.jivesoftware.smackx.message_fastening.provider;

import java.io.IOException;

import org.jivesoftware.smack.packet.XmlElement;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smack.util.ParserUtils;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.jivesoftware.smackx.message_fastening.MessageFasteningManager;
import org.jivesoftware.smackx.message_fastening.element.ExternalElement;
import org.jivesoftware.smackx.message_fastening.element.FasteningElement;

public class FasteningElementProvider extends ExtensionElementProvider<FasteningElement> {

    public static final FasteningElementProvider TEST_INSTANCE = new FasteningElementProvider();

    @Override
    public FasteningElement parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment) throws XmlPullParserException, IOException, SmackParsingException {
        FasteningElement.Builder builder = FasteningElement.builder();
        builder.setOriginId(parser.getAttributeValue("", FasteningElement.ATTR_ID));
        if (ParserUtils.getBooleanAttribute(parser, FasteningElement.ATTR_CLEAR, false)) {
            builder.setClear();
        }
        if (ParserUtils.getBooleanAttribute(parser, FasteningElement.ATTR_SHELL, false)) {
            builder.setShell();
        }

        outerloop: while (true) {
            XmlPullParser.Event tag = parser.next();
            switch (tag) {
                case START_ELEMENT:
                    String name = parser.getName();
                    String namespace = parser.getNamespace();

                    // Parse external payload
                    if (MessageFasteningManager.NAMESPACE.equals(namespace) && ExternalElement.ELEMENT.equals(name)) {
                        ExternalElement external = new ExternalElement(
                                parser.getAttributeValue("", ExternalElement.ATTR_NAME),
                                parser.getAttributeValue("", ExternalElement.ATTR_ELEMENT_NAMESPACE));
                        builder.addExternalPayload(external);
                        continue;
                    }

                    // Parse wrapped payload
                    XmlElement wrappedPayload = PacketParserUtils.parseExtensionElement(name, namespace, parser, xmlEnvironment);
                    builder.addWrappedPayload(wrappedPayload);
                    break;

                case END_ELEMENT:
                    if (parser.getDepth() == initialDepth) {
                        break outerloop;
                    }
                    break;
                default:
                    break;
            }
        }
        return builder.build();
    }
}
