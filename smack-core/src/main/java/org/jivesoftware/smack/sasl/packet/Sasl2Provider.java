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
package org.jivesoftware.smack.sasl.packet;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.jivesoftware.smack.packet.XmlElement;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.provider.NonzaProvider;
import org.jivesoftware.smack.sasl.SASLError;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.jxmpp.JxmppContext;

public class Sasl2Provider {

    public static final class Sasl2FeatureProvider extends ExtensionElementProvider<Sasl2Feature> {

        public static final Sasl2FeatureProvider INSTANCE = new Sasl2FeatureProvider();

        private Sasl2FeatureProvider() {
        }

        @Override
        public Sasl2Feature parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment,
                        JxmppContext jxmppContext)
                        throws XmlPullParserException, IOException, SmackParsingException, ParseException {
            List<String> mechanisms = new ArrayList<>();
            Sasl2Feature.Inline inline = null;

            outerloop: while (true) {
                XmlPullParser.Event eventType = parser.next();
                if (eventType == XmlPullParser.Event.START_ELEMENT) {
                    String name = parser.getName();
                    String namespace = parser.getNamespace();

                    if ("mechanism".equals(name) && Sasl2Nonza.NAMESPACE.equals(namespace)) {
                        mechanisms.add(parser.nextText().trim());
                    } else if ("inline".equals(name) && Sasl2Nonza.NAMESPACE.equals(namespace)) {
                        inline = InlineProvider.INSTANCE.parse(parser, parser.getDepth(), xmlEnvironment, jxmppContext);
                    }
                } else if (eventType == XmlPullParser.Event.END_ELEMENT) {
                    if (parser.getDepth() == initialDepth) {
                        break outerloop;
                    }
                }
            }

            return new Sasl2Feature(mechanisms, inline);
        }
    }

    public static final class InlineProvider extends ExtensionElementProvider<Sasl2Feature.Inline> {

        public static final InlineProvider INSTANCE = new InlineProvider();

        private InlineProvider() {
        }

        @Override
        public Sasl2Feature.Inline parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment,
                        JxmppContext jxmppContext)
                        throws XmlPullParserException, IOException, SmackParsingException {
            List<XmlElement> inlineFeatures = new ArrayList<>();
            outerloop: while (true) {
                XmlPullParser.Event eventType = parser.next();
                if (eventType == XmlPullParser.Event.START_ELEMENT) {
                    XmlElement extension = PacketParserUtils.parseExtensionElement(
                                    parser.getName(), parser.getNamespace(), parser, xmlEnvironment, jxmppContext);
                    if (extension != null) {
                        inlineFeatures.add(extension);
                    }
                } else if (eventType == XmlPullParser.Event.END_ELEMENT) {
                    if (parser.getDepth() == initialDepth) {
                        break outerloop;
                    }
                }
            }
            return new Sasl2Feature.Inline(inlineFeatures);
        }
    }

    public static final class AuthenticateProvider extends NonzaProvider<Sasl2Nonza.Authenticate> {

        public static final AuthenticateProvider INSTANCE = new AuthenticateProvider();

        private AuthenticateProvider() {
        }

        @Override
        public Sasl2Nonza.Authenticate parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment,
                        JxmppContext jxmppContext)
                        throws XmlPullParserException, IOException, SmackParsingException, ParseException {
            String mechanism = parser.getAttributeValue(null, "mechanism");
            String initialResponse = null;
            Sasl2Nonza.UserAgent userAgent = null;
            List<XmlElement> extensionElements = new ArrayList<>();

            outerloop: while (true) {
                XmlPullParser.Event eventType = parser.next();
                if (eventType == XmlPullParser.Event.START_ELEMENT) {
                    String name = parser.getName();
                    String namespace = parser.getNamespace();

                    if ("initial-response".equals(name) && Sasl2Nonza.NAMESPACE.equals(namespace)) {
                        initialResponse = parser.nextText();
                    } else if ("user-agent".equals(name) && Sasl2Nonza.NAMESPACE.equals(namespace)) {
                        userAgent = parseUserAgent(parser, parser.getDepth());
                    } else {
                        XmlElement extension = PacketParserUtils.parseExtensionElement(
                                        name, namespace, parser, xmlEnvironment, jxmppContext);
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

            return new Sasl2Nonza.Authenticate(mechanism, initialResponse, userAgent, extensionElements);
        }

        private static Sasl2Nonza.UserAgent parseUserAgent(XmlPullParser parser, int initialDepth)
                        throws XmlPullParserException, IOException {
            String software = null;
            String device = null;

            String idString = parser.getAttributeValue("id");
            var uuid = UUID.fromString(idString);


            outerloop: while (true) {
                XmlPullParser.Event eventType = parser.next();
                if (eventType == XmlPullParser.Event.START_ELEMENT) {
                    String name = parser.getName();
                    String namespace = parser.getNamespace();
                    if (Sasl2Nonza.NAMESPACE.equals(namespace)) {
                        if ("software".equals(name)) {
                            software = parser.nextText();
                        } else if ("device".equals(name)) {
                            device = parser.nextText();
                        }
                    }
                } else if (eventType == XmlPullParser.Event.END_ELEMENT) {
                    if (parser.getDepth() == initialDepth) {
                        break outerloop;
                    }
                }
            }

            return new Sasl2Nonza.UserAgent(uuid, software, device);
        }

    }

    public static final class ChallengeProvider extends NonzaProvider<Sasl2Nonza.Challenge> {

        public static final ChallengeProvider INSTANCE = new ChallengeProvider();

        private ChallengeProvider() {
        }

        @Override
        public Sasl2Nonza.Challenge parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment,
                        JxmppContext jxmppContext)
                        throws IOException, XmlPullParserException {
            String data = parser.nextText();
            return new Sasl2Nonza.Challenge(data);
        }

    }

    public static final class ResponseProvider extends NonzaProvider<Sasl2Nonza.Response> {

        public static final ResponseProvider INSTANCE = new ResponseProvider();

        private ResponseProvider() {
        }

        @Override
        public Sasl2Nonza.Response parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment,
                        JxmppContext jxmppContext)
                        throws IOException, XmlPullParserException {
            String data = parser.nextText();
            return new Sasl2Nonza.Response(data);
        }

    }

    public static final class SuccessProvider extends NonzaProvider<Sasl2Nonza.Success> {

        public static final SuccessProvider INSTANCE = new SuccessProvider();

        private SuccessProvider() {
        }

        @Override
        public Sasl2Nonza.Success parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment,
                        JxmppContext jxmppContext)
                        throws XmlPullParserException, IOException, SmackParsingException, ParseException {
            String additionalData = null;
            String authorizationIdentifier = null;
            List<XmlElement> extensionElements = new ArrayList<>();

            outerloop: while (true) {
                XmlPullParser.Event eventType = parser.next();
                if (eventType == XmlPullParser.Event.START_ELEMENT) {
                    String name = parser.getName();
                    String namespace = parser.getNamespace();
                    if (Sasl2Nonza.NAMESPACE.equals(namespace)) {
                        if ("additional-data".equals(name)) {
                            additionalData = parser.nextText();
                        } else if ("authorization-identifier".equals(name)) {
                            authorizationIdentifier = parser.nextText();
                        }
                    } else {
                        XmlElement extension = PacketParserUtils.parseExtensionElement(
                                        name, namespace, parser, xmlEnvironment, jxmppContext);
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

            return new Sasl2Nonza.Success(additionalData, authorizationIdentifier, extensionElements);
        }

    }

    public static final class FailureProvider extends NonzaProvider<Sasl2Nonza.Failure> {

        public static final FailureProvider INSTANCE = new FailureProvider();

        private FailureProvider() {
        }

        @Override
        public Sasl2Nonza.Failure parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment,
                        JxmppContext jxmppContext)
                        throws XmlPullParserException, IOException, SmackParsingException, ParseException {
            String condition = null;
            Map<String, String> descriptiveTexts = null;
            List<XmlElement> extensionElements = new ArrayList<>();

            outerloop: while (true) {
                XmlPullParser.Event eventType = parser.next();
                if (eventType == XmlPullParser.Event.START_ELEMENT) {
                    String name = parser.getName();
                    String namespace = parser.getNamespace();
                    if ("text".equals(name) && Sasl2Nonza.NAMESPACE.equals(namespace)) {
                        descriptiveTexts = PacketParserUtils.parseDescriptiveTexts(parser, descriptiveTexts);
                    } else if (SaslNonza.NAMESPACE.equals(namespace) || (Sasl2Nonza.NAMESPACE.equals(namespace) && SASLError.fromString(name) != null)) {
                        condition = name;
                    } else {
                        XmlElement extension = PacketParserUtils.parseExtensionElement(
                                        name, namespace, parser, xmlEnvironment, jxmppContext);
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

            return new Sasl2Nonza.Failure(condition, descriptiveTexts, extensionElements);
        }

    }

    public static final class ContinueProvider extends NonzaProvider<Sasl2Nonza.Continue> {

        public static final ContinueProvider INSTANCE = new ContinueProvider();

        private ContinueProvider() {
        }

        @Override
        public Sasl2Nonza.Continue parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment,
                        JxmppContext jxmppContext)
                        throws XmlPullParserException, IOException {
            String additionalData = null;
            List<String> tasks = new ArrayList<>();
            String text = null;

            outerloop: while (true) {
                XmlPullParser.Event eventType = parser.next();
                if (eventType == XmlPullParser.Event.START_ELEMENT) {
                    String name = parser.getName();
                    String namespace = parser.getNamespace();
                    if (Sasl2Nonza.NAMESPACE.equals(namespace)) {
                        if ("additional-data".equals(name)) {
                            additionalData = parser.nextText();
                        } else if ("tasks".equals(name)) {
                            parseTasks(parser, parser.getDepth(), tasks);
                        } else if ("text".equals(name)) {
                            text = parser.nextText();
                        }
                    }
                } else if (eventType == XmlPullParser.Event.END_ELEMENT) {
                    if (parser.getDepth() == initialDepth) {
                        break outerloop;
                    }
                }
            }

            return new Sasl2Nonza.Continue(additionalData, tasks, text);
        }

        private static void parseTasks(XmlPullParser parser, int initialDepth, List<String> tasks)
                        throws XmlPullParserException, IOException {
            outerloop: while (true) {
                XmlPullParser.Event eventType = parser.next();
                if (eventType == XmlPullParser.Event.START_ELEMENT) {
                    if ("task".equals(parser.getName()) && Sasl2Nonza.NAMESPACE.equals(parser.getNamespace())) {
                        tasks.add(parser.nextText());
                    }
                } else if (eventType == XmlPullParser.Event.END_ELEMENT) {
                    if (parser.getDepth() == initialDepth) {
                        break outerloop;
                    }
                }
            }
        }

    }

    public static final class NextProvider extends NonzaProvider<Sasl2Nonza.Next> {

        public static final NextProvider INSTANCE = new NextProvider();

        private NextProvider() {
        }

        @Override
        public Sasl2Nonza.Next parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment,
                        JxmppContext jxmppContext)
                        throws XmlPullParserException, IOException, SmackParsingException, ParseException {
            String task = parser.getAttributeValue(null, "task");
            List<XmlElement> extensionElements = new ArrayList<>();

            outerloop: while (true) {
                XmlPullParser.Event eventType = parser.next();
                if (eventType == XmlPullParser.Event.START_ELEMENT) {
                    XmlElement extension = PacketParserUtils.parseExtensionElement(
                                    parser.getName(), parser.getNamespace(), parser, xmlEnvironment, jxmppContext);
                    if (extension != null) {
                        extensionElements.add(extension);
                    }
                } else if (eventType == XmlPullParser.Event.END_ELEMENT) {
                    if (parser.getDepth() == initialDepth) {
                        break outerloop;
                    }
                }
            }

            return new Sasl2Nonza.Next(task, extensionElements);
        }

    }

    public static final class TaskDataProvider extends NonzaProvider<Sasl2Nonza.TaskData> {

        public static final TaskDataProvider INSTANCE = new TaskDataProvider();

        private TaskDataProvider() {
        }

        @Override
        public Sasl2Nonza.TaskData parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment,
                        JxmppContext jxmppContext)
                        throws XmlPullParserException, IOException, SmackParsingException, ParseException {
            List<XmlElement> extensionElements = new ArrayList<>();

            outerloop: while (true) {
                XmlPullParser.Event eventType = parser.next();
                if (eventType == XmlPullParser.Event.START_ELEMENT) {
                    XmlElement extension = PacketParserUtils.parseExtensionElement(
                                    parser.getName(), parser.getNamespace(), parser, xmlEnvironment, jxmppContext);
                    if (extension != null) {
                        extensionElements.add(extension);
                    }
                } else if (eventType == XmlPullParser.Event.END_ELEMENT) {
                    if (parser.getDepth() == initialDepth) {
                        break outerloop;
                    }
                }
            }

            return new Sasl2Nonza.TaskData(extensionElements);
        }

    }

    public static final class AbortProvider extends NonzaProvider<Sasl2Nonza.Abort> {

        public static final AbortProvider INSTANCE = new AbortProvider();

        private AbortProvider() {
        }

        @Override
        public Sasl2Nonza.Abort parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment,
                        JxmppContext jxmppContext)
                        throws XmlPullParserException, IOException, SmackParsingException, ParseException {
            String text = null;
            List<XmlElement> extensionElements = new ArrayList<>();

            outerloop: while (true) {
                XmlPullParser.Event eventType = parser.next();
                if (eventType == XmlPullParser.Event.START_ELEMENT) {
                    String name = parser.getName();
                    String namespace = parser.getNamespace();
                    if ("text".equals(name) && Sasl2Nonza.NAMESPACE.equals(namespace)) {
                        text = parser.nextText();
                    } else {
                        XmlElement extension = PacketParserUtils.parseExtensionElement(
                                        name, namespace, parser, xmlEnvironment, jxmppContext);
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

            return new Sasl2Nonza.Abort(text, extensionElements);
        }

    }
}
