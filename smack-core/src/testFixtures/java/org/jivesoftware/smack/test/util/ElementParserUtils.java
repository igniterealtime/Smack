/*
 *
 * Copyright 2025 Florian Schmaus.
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

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.packet.StanzaError;
import org.jivesoftware.smack.packet.StreamError;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.jxmpp.JxmppContext;

/**
 * Try to avoid using functions in this class. Use {@link SmackTestUtil} instead.
 */
// TODO: Remove all call sites of functions in this class and remove this class.
public class ElementParserUtils {

    @SuppressWarnings({ "unchecked", "TypeParameterUnusedInFormals" })
    public static <S extends Stanza> S parseStanza(String stanza)
                    throws XmlPullParserException, SmackParsingException, IOException {
        return (S) PacketParserUtils.parseStanza(PacketParserUtils.getParserFor(stanza), XmlEnvironment.EMPTY,
                        JxmppContext.getDefaultContext());
    }

    public static Message parseMessage(XmlPullParser parser)
                    throws XmlPullParserException, IOException, SmackParsingException {
        return PacketParserUtils.parseMessage(parser, XmlEnvironment.EMPTY, JxmppContext.getDefaultContext());
    }

    public static Presence parsePresence(XmlPullParser parser)
                    throws XmlPullParserException, IOException, SmackParsingException {
        return PacketParserUtils.parsePresence(parser, XmlEnvironment.EMPTY, JxmppContext.getDefaultContext());
    }

    public static IQ parseIQ(XmlPullParser parser) throws Exception {
        return PacketParserUtils.parseIQ(parser, null, JxmppContext.getDefaultContext());
    }

    public static StreamError parseStreamError(XmlPullParser parser)
                    throws XmlPullParserException, IOException, SmackParsingException {
        return PacketParserUtils.parseStreamError(parser, null, JxmppContext.getDefaultContext());
    }

    public static StanzaError parseError(XmlPullParser parser)
                    throws XmlPullParserException, IOException, SmackParsingException {
        return PacketParserUtils.parseError(parser, null, JxmppContext.getDefaultContext());
    }

}
