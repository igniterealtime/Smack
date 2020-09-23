/**
 *
 * Copyright 2013-2014 Georg Lukas, 2020 Florian Schmaus
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
package org.jivesoftware.smackx.forward.provider;

import java.io.IOException;
import java.util.logging.Logger;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.jivesoftware.smackx.delay.packet.DelayInformation;
import org.jivesoftware.smackx.delay.provider.DelayInformationProvider;
import org.jivesoftware.smackx.forward.packet.Forwarded;

/**
 * This class implements the {@link ExtensionElementProvider} to parse
 * forwarded messages from a packet.  It will return a {@link Forwarded} stanza extension.
 *
 * @author Georg Lukas
 */
public class ForwardedProvider extends ExtensionElementProvider<Forwarded<?>> {

    public static final ForwardedProvider INSTANCE = new ForwardedProvider();

    private static final Logger LOGGER = Logger.getLogger(ForwardedProvider.class.getName());

    @Override
    public Forwarded<?> parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment) throws XmlPullParserException, IOException, SmackParsingException {
        DelayInformation di = null;
        Stanza packet = null;

        outerloop: while (true) {
            XmlPullParser.Event eventType = parser.next();
            switch (eventType) {
            case START_ELEMENT:
                String name = parser.getName();
                String namespace = parser.getNamespace();
                switch (name) {
                case DelayInformation.ELEMENT:
                    if (DelayInformation.NAMESPACE.equals(namespace)) {
                        di = DelayInformationProvider.INSTANCE.parse(parser, parser.getDepth(), null);
                    } else {
                        LOGGER.warning("Namespace '" + namespace + "' does not match expected namespace '"
                                        + DelayInformation.NAMESPACE + "'");
                    }
                    break;
                case Message.ELEMENT:
                    packet = PacketParserUtils.parseMessage(parser);
                    break;
                default:
                    LOGGER.warning("Unsupported forwarded packet type: " + name);
                }
                break;
            case END_ELEMENT:
                if (parser.getDepth() == initialDepth) {
                    break outerloop;
                }
                break;
            default:
                // Catch all for incomplete switch (MissingCasesInEnumSwitch) statement.
                break;
            }
        }

        if (packet == null) {
            // TODO: Should be SmackParseException.
            throw new IOException("forwarded extension must contain a packet");
        }
        return new Forwarded<>(packet, di);
    }

    public static Forwarded<Message> parseForwardedMessage(XmlPullParser parser, XmlEnvironment xmlEnvironment)
                    throws XmlPullParserException, IOException, SmackParsingException {
        return parseForwardedMessage(parser, parser.getDepth(), xmlEnvironment);
    }

    @SuppressWarnings("unchecked")
    public static Forwarded<Message> parseForwardedMessage(XmlPullParser parser, int initialDepth,
                    XmlEnvironment xmlEnvironment) throws XmlPullParserException, IOException, SmackParsingException {
        Forwarded<?> forwarded = INSTANCE.parse(parser, initialDepth, xmlEnvironment);
        if (!forwarded.isForwarded(Message.class)) {
            throw new SmackParsingException("Expecting a forwarded message, but got " + forwarded);
        }
        return (Forwarded<Message>) forwarded;
    }
}
