/*
 *
 * Copyright 2026 Florian Schmaus
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
package org.jivesoftware.smack.fast.provider;

import java.io.IOException;
import java.text.ParseException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.smack.fast.element.FastElements;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.util.ParserUtils;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.jxmpp.JxmppContext;
import org.jxmpp.util.XmppDateTime;

public class FastProvider {

    public static final class FastElementProvider extends ExtensionElementProvider<FastElements.Fast> {
        public static final FastElementProvider INSTANCE = new FastElementProvider();

        private FastElementProvider() {
        }

        @Override
        public FastElements.Fast parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment,
                        JxmppContext jxmppContext)
                        throws XmlPullParserException, IOException, SmackParsingException, ParseException {
            Boolean tls0rtt = ParserUtils.getBooleanAttribute(parser, "tls-0rtt");
            Long count = ParserUtils.getLongAttribute(parser, "count");
            Boolean invalidate = ParserUtils.getBooleanAttribute(parser, "invalidate");
            List<String> mechanisms = new ArrayList<>();

            outerloop: while (true) {
                XmlPullParser.Event eventType = parser.next();
                if (eventType == XmlPullParser.Event.START_ELEMENT) {
                    String name = parser.getName();
                    String namespace = parser.getNamespace();

                    if ("mechanism".equals(name) && FastElements.NAMESPACE.equals(namespace)) {
                        mechanisms.add(parser.nextText().trim());
                    }
                } else if (eventType == XmlPullParser.Event.END_ELEMENT) {
                    if (parser.getDepth() == initialDepth) {
                        break outerloop;
                    }
                }
            }

            return new FastElements.Fast(mechanisms, tls0rtt, count, invalidate);
        }
    }

    public static final class RequestTokenProvider extends ExtensionElementProvider<FastElements.RequestToken> {
        public static final RequestTokenProvider INSTANCE = new RequestTokenProvider();

        private RequestTokenProvider() {
        }

        @Override
        public FastElements.RequestToken parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment,
                        JxmppContext jxmppContext)
                        throws XmlPullParserException, IOException, SmackParsingException, ParseException {
            String mechanism = parser.getAttributeValue(null, "mechanism");
            if (mechanism == null) {
                throw new SmackParsingException("Missing 'mechanism' attribute on request-token element");
            }
            return new FastElements.RequestToken(mechanism);
        }
    }

    public static final class TokenProvider extends ExtensionElementProvider<FastElements.Token> {
        public static final TokenProvider INSTANCE = new TokenProvider();

        private TokenProvider() {
        }

        @Override
        public FastElements.Token parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment,
                        JxmppContext jxmppContext)
                        throws XmlPullParserException, IOException, SmackParsingException, ParseException {
            String token = parser.getAttributeValue(null, "token");
            if (token == null) {
                throw new SmackParsingException("Missing 'token' attribute on token element");
            }
            String expiryString = parser.getAttributeValue(null, "expiry");
            Instant expiry = null;
            if (expiryString != null) {
                try {
                    expiry = Instant.parse(expiryString);
                } catch (Exception e) {
                    expiry = XmppDateTime.parseDate(expiryString).toInstant();
                }
            }
            return new FastElements.Token(token, expiry);
        }
    }
}
