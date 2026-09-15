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
package org.jivesoftware.smack.bind2.provider;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.jivesoftware.smack.bind2.element.Bind2Elements;
import org.jivesoftware.smack.packet.XmlElement;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.jxmpp.JxmppContext;

public class Bind2Provider {

    public static final class BindProvider extends ExtensionElementProvider<Bind2Elements.Bind> {

        public static final BindProvider INSTANCE = new BindProvider();

        private BindProvider() {
        }

        @Override
        public Bind2Elements.Bind parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment,
                        JxmppContext jxmppContext)
                        throws XmlPullParserException, IOException, SmackParsingException, ParseException {
            Set<String> inlineFeatures = new LinkedHashSet<>();
            String tag = null;
            List<XmlElement> extensionElements = new ArrayList<>();

            outerloop: while (true) {
                XmlPullParser.Event eventType = parser.next();
                if (eventType == XmlPullParser.Event.START_ELEMENT) {
                    String name = parser.getName();
                    String namespace = parser.getNamespace();

                    if ("inline".equals(name) && Bind2Elements.NAMESPACE.equals(namespace)) {
                        parseInlineFeatures(parser, parser.getDepth(), inlineFeatures);
                    } else if ("tag".equals(name) && Bind2Elements.NAMESPACE.equals(namespace)) {
                        tag = parser.nextText();
                    } else {
                        XmlElement extension = PacketParserUtils.parseExtensionElement(name, namespace, parser, xmlEnvironment, jxmppContext);
                        if (extension != null) {
                            extensionElements.add(extension);
                        }
                    }
                } else if (eventType == XmlPullParser.Event.END_ELEMENT) {
                    if (parser.getDepth() == initialDepth) {
                        break outerloop;
                    }
                }
            }

            return new Bind2Elements.Bind(inlineFeatures, tag, extensionElements);
        }

        private static void parseInlineFeatures(XmlPullParser parser, int initialDepth, Set<String> inlineFeatures)
                        throws XmlPullParserException, IOException {
            outerloop: while (true) {
                XmlPullParser.Event eventType = parser.next();
                if (eventType == XmlPullParser.Event.START_ELEMENT) {
                    if ("feature".equals(parser.getName()) && Bind2Elements.NAMESPACE.equals(parser.getNamespace())) {
                        String var = parser.getAttributeValue(null, "var");
                        if (var != null) {
                            inlineFeatures.add(var);
                        }
                    }
                } else if (eventType == XmlPullParser.Event.END_ELEMENT) {
                    if (parser.getDepth() == initialDepth) {
                        break outerloop;
                    }
                }
            }
        }
    }

    public static final class BoundProvider extends ExtensionElementProvider<Bind2Elements.Bound> {

        public static final BoundProvider INSTANCE = new BoundProvider();

        private BoundProvider() {
        }

        @Override
        public Bind2Elements.Bound parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment,
                        JxmppContext jxmppContext)
                        throws XmlPullParserException, IOException, SmackParsingException, ParseException {
            XmlElement mamMetadata = null;

            outerloop: while (true) {
                XmlPullParser.Event eventType = parser.next();
                if (eventType == XmlPullParser.Event.START_ELEMENT) {
                    String name = parser.getName();
                    String namespace = parser.getNamespace();
                    if ("metadata".equals(name) && "urn:xmpp:mam:2".equals(namespace)) {
                        mamMetadata = PacketParserUtils.parseExtensionElement(name, namespace, parser, xmlEnvironment, jxmppContext);
                    } else {
                        PacketParserUtils.parseExtensionElement(name, namespace, parser, xmlEnvironment, jxmppContext);
                    }
                } else if (eventType == XmlPullParser.Event.END_ELEMENT) {
                    if (parser.getDepth() == initialDepth) {
                        break outerloop;
                    }
                }
            }

            return new Bind2Elements.Bound(mamMetadata);
        }

    }
}
