/**
 *
 * Copyright 2017-2022 Florian Schmaus
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
package org.jivesoftware.smackx.jingle.provider;

import java.io.IOException;
import java.util.logging.Logger;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.IqData;
import org.jivesoftware.smack.packet.StandardExtensionElement;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.parsing.StandardExtensionElementProvider;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.provider.IqProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smack.util.ParserUtils;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;
import org.jivesoftware.smackx.jingle.element.Jingle;
import org.jivesoftware.smackx.jingle.element.JingleAction;
import org.jivesoftware.smackx.jingle.element.JingleContent;
import org.jivesoftware.smackx.jingle.element.JingleContentDescription;
import org.jivesoftware.smackx.jingle.element.JingleContentTransport;
import org.jivesoftware.smackx.jingle.element.JingleReason;
import org.jivesoftware.smackx.jingle.element.JingleReason.Reason;
import org.jivesoftware.smackx.jingle.element.UnknownJingleContentDescription;
import org.jivesoftware.smackx.jingle.element.UnknownJingleContentTransport;
import org.jivesoftware.smackx.jingle_rtp.AbstractXmlElement;
import org.jivesoftware.smackx.jingle_rtp.element.SessionInfo;
import org.jivesoftware.smackx.jingle_rtp.element.SessionInfoType;

import org.jxmpp.jid.FullJid;

/**
 * An implementation of a Jingle IQ provider that parses incoming Jingle IQs.
 *
 * @author Florian Schmaus
 * @author Eng Chong Meng
 * @author Emil Ivov
 */
public class JingleProvider extends IqProvider<Jingle> {
    private static final Logger LOGGER = Logger.getLogger(JingleProvider.class.getName());

    /**
     * Parses a Jingle IQ sub-document and returns a {@link Jingle} instance.
     *
     * @param parser an XML parser.
     * @return a new {@link Jingle} instance.
     * @throws IOException, XmlPullParserException, ParseException if an error occurs parsing the XML.
     */
    @Override
    public Jingle parse(XmlPullParser parser, int initialDepth, IqData iqData, XmlEnvironment xmlEnvironment)
            throws XmlPullParserException, IOException, SmackParsingException {
        Jingle.Builder builder = Jingle.builder(iqData);

        String actionString = parser.getAttributeValue("", Jingle.ATTR_ACTION);
        if (actionString != null) {
            JingleAction action = JingleAction.fromString(actionString);
            builder.setAction(action);
        }

        FullJid initiator = ParserUtils.getFullJidAttribute(parser, Jingle.ATTR_INITIATOR);
        builder.setInitiator(initiator);

        FullJid responder = ParserUtils.getFullJidAttribute(parser, Jingle.ATTR_RESPONDER);
        builder.setResponder(responder);

        String sessionId = parser.getAttributeValue("", Jingle.ATTR_SESSION_ID);
        builder.setSessionId(sessionId);

        outerloop:
        while (true) {
            XmlPullParser.Event eventType = parser.next();
            switch (eventType) {
                case START_ELEMENT:
                    String tagName = parser.getName();
                    String namespace = parser.getNamespace();
                    switch (tagName) {
                        case JingleContent.ELEMENT:
                            JingleContent content = parseJingleContent(parser, parser.getDepth());
                            builder.addJingleContent(content);
                            break;

                        case JingleReason.ELEMENT:
                            JingleReason reason = parseJingleReason(parser);
                            builder.setReason(reason);
                            break;

                        default:
                            // session-info element handlers for <mute/> <active/> and etc
                            if (namespace.equals(SessionInfo.NAMESPACE)) {
                                LOGGER.info("Handle Jingle Session-Info: <" + tagName + " xml: " + namespace + ">");
                                SessionInfoType type = SessionInfoType.valueOf(tagName);

                                // <mute/> <unmute/>
                                if (type == SessionInfoType.mute || type == SessionInfoType.unmute) {
                                    String name = parser.getAttributeValue("", SessionInfo.ATTR_NAME);
                                    String creator = parser.getAttributeValue("", SessionInfo.ATTR_CREATOR);
                                    builder.setSessionInfo(SessionInfo.builder(type)
                                            .setName(name)
                                            .setCreator(creator)
                                            .build()
                                    );
                                }
                                // <active/>, <hold/>, <unhold/>, and <ringing/> etc.
                                else {
                                    builder.setSessionInfo(SessionInfo.builder(type).build());
                                }
                            // Handle all the aTalk AbstractExtensionElement extensions embedded in JingleIQ
                            } else {
                                ExtensionElementProvider<?> provider = ProviderManager.getExtensionProvider(tagName, namespace);
                                if (provider != null) {
                                    LOGGER.info("Found provider for EE<" + tagName + " " + namespace + "/>");
                                    ExtensionElement childExtension = provider.parse(parser);
                                    if (childExtension instanceof AbstractXmlElement) {
                                        builder.addExtension(childExtension);
                                    }
                                    else
                                        LOGGER.severe("Unknown Jingle extension element: " + tagName);
                                }
                                else {
                                    // Extension element provider may not have been added properly if null
                                    LOGGER.severe("No provider for EE<" + tagName + " " + namespace + "/>");
                                }
                            }
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
        return builder.build();
    }

    public static JingleContent parseJingleContent(XmlPullParser parser, final int initialDepth)
            throws XmlPullParserException, IOException, SmackParsingException {
        JingleContent.Builder builder = JingleContent.getBuilder();

        String creatorString = parser.getAttributeValue("", JingleContent.ATTR_CREATOR);
        JingleContent.Creator creator = JingleContent.Creator.valueOf(creatorString);
        builder.setCreator(creator);

        String disposition = parser.getAttributeValue("", JingleContent.ATTR_DISPOSITION);
        builder.setDisposition(disposition);

        String name = parser.getAttributeValue("", JingleContent.ATTR_NAME);
        builder.setName(name);

        String sendersString = parser.getAttributeValue("", JingleContent.ATTR_SENDERS);
        if (sendersString != null) {
            JingleContent.Senders senders = JingleContent.Senders.valueOf(sendersString);
            builder.setSenders(senders);
        }

        outerloop:
        while (true) {
            XmlPullParser.Event eventType = parser.next();
            switch (eventType) {
                case START_ELEMENT:
                    String tagName = parser.getName();
                    String namespace = parser.getNamespace();
                    switch (tagName) {
                        case JingleContentDescription.ELEMENT: {
                            JingleContentDescription description;
                            JingleContentDescriptionProvider<?> provider = JingleContentProviderManager.getJingleContentDescriptionProvider(namespace);
                            if (provider == null) {
                                StandardExtensionElement standardExtensionElement = StandardExtensionElementProvider.INSTANCE.parse(parser);
                                description = new UnknownJingleContentDescription(standardExtensionElement);
                            } else {
                                description = provider.parse(parser);
                            }
                            builder.setDescription(description);
                            break;
                        }

                        case JingleContentTransport.ELEMENT: {
                            JingleContentTransport transport;
                            JingleContentTransportProvider<?> provider = JingleContentProviderManager.getJingleContentTransportProvider(namespace);
                            if (provider == null) {
                                StandardExtensionElement standardExtensionElement = StandardExtensionElementProvider.INSTANCE.parse(parser);
                                transport = new UnknownJingleContentTransport(standardExtensionElement);
                            } else {
                                transport = provider.parse(parser);
                            }
                            builder.setTransport(transport);
                            break;
                        }
                        default:
                            // Handle all the aTalk AbstractExtensionElement extensions embedded in JingleContent
                            ExtensionElementProvider<?> provider = ProviderManager.getExtensionProvider(tagName, namespace);
                            if (provider != null) {
                                LOGGER.info("Found provider for EE<" + tagName + " " + namespace + "/>");
                                ExtensionElement childExtension = provider.parse(parser);
                                if (childExtension instanceof AbstractXmlElement) {
                                    builder.addChildElement(childExtension);
                                }
                                else
                                    LOGGER.severe("Unknown Jingle content element: " + tagName);
                            }
                            else {
                                // Extension element provider may not have been added properly if null
                                LOGGER.severe("No provider for content EE<" + tagName + " " + namespace + "/>");
                            }
                            break;
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

        return builder.build();
    }

    public static JingleReason parseJingleReason(XmlPullParser parser)
            throws XmlPullParserException, IOException, SmackParsingException {
        ParserUtils.assertAtStartTag(parser);
        final int initialDepth = parser.getDepth();
        final String jingleNamespace = parser.getNamespace();

        JingleReason.Reason reason = null;
        ExtensionElement element = null;
        String text = null;

        // 'sid' is only set if the reason is 'alternative-session'.
        String sid = null;

        outerloop:
        while (true) {
            XmlPullParser.TagEvent event = parser.nextTag();
            switch (event) {
                case START_ELEMENT:
                    String elementName = parser.getName();
                    String namespace = parser.getNamespace();
                    if (namespace.equals(jingleNamespace)) {
                        switch (elementName) {
                            case "text":
                                text = parser.nextText();
                                break;
                            case "alternative-session":
                                parser.next();
                                sid = parser.nextText();
                                break;
                            default:
                                reason = Reason.fromString(elementName);
                                break;
                        }
                    } else {
                        element = PacketParserUtils.parseExtensionElement(elementName, namespace, parser, null);
                    }
                    break;
                case END_ELEMENT:
                    if (parser.getDepth() == initialDepth) {
                        break outerloop;
                    }
                    break;
            }
        }

        JingleReason res;
        if (sid != null) {
            res = new JingleReason.AlternativeSession(sid, text, element);
        } else {
            res = new JingleReason(reason, text, element);
        }
        return res;
    }
}
