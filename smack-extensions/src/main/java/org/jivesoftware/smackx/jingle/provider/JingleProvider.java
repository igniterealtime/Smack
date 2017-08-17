/**
 *
 * Copyright 2017 Florian Schmaus
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

import java.util.logging.Logger;

import org.jivesoftware.smack.packet.StandardExtensionElement;
import org.jivesoftware.smack.parsing.StandardExtensionElementProvider;
import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smack.util.ParserUtils;
import org.jivesoftware.smackx.jingle.JingleManager;
import org.jivesoftware.smackx.jingle.element.JingleAction;
import org.jivesoftware.smackx.jingle.element.JingleContentDescriptionElement;
import org.jivesoftware.smackx.jingle.element.JingleContentElement;
import org.jivesoftware.smackx.jingle.element.JingleContentSecurityElement;
import org.jivesoftware.smackx.jingle.element.JingleContentTransportElement;
import org.jivesoftware.smackx.jingle.element.JingleElement;
import org.jivesoftware.smackx.jingle.element.JingleReasonElement;
import org.jivesoftware.smackx.jingle.element.UnknownJingleContentDescriptionElement;
import org.jivesoftware.smackx.jingle.element.UnknownJingleContentSecurityElement;
import org.jivesoftware.smackx.jingle.element.UnknownJingleContentTransportElement;

import org.jxmpp.jid.FullJid;
import org.xmlpull.v1.XmlPullParser;

public class JingleProvider extends IQProvider<JingleElement> {

    private static final Logger LOGGER = Logger.getLogger(JingleProvider.class.getName());

    @Override
    public JingleElement parse(XmlPullParser parser, int initialDepth) throws Exception {
        JingleElement.Builder builder = JingleElement.getBuilder();

        String actionString = parser.getAttributeValue("", JingleElement.ACTION_ATTRIBUTE_NAME);
        if (actionString != null) {
            JingleAction action = JingleAction.fromString(actionString);
            builder.setAction(action);
        }

        FullJid initiator = ParserUtils.getFullJidAttribute(parser, JingleElement.INITIATOR_ATTRIBUTE_NAME);
        builder.setInitiator(initiator);

        FullJid responder = ParserUtils.getFullJidAttribute(parser, JingleElement.RESPONDER_ATTRIBUTE_NAME);
        builder.setResponder(responder);

        String sessionId = parser.getAttributeValue("", JingleElement.SESSION_ID_ATTRIBUTE_NAME);
        builder.setSessionId(sessionId);


        outerloop: while (true) {
            int eventType = parser.next();
            switch (eventType) {
                case XmlPullParser.START_TAG:
                    String tagName = parser.getName();
                    switch (tagName) {
                        case JingleContentElement.ELEMENT:
                            JingleContentElement content = parseJingleContent(parser, parser.getDepth());
                            builder.addJingleContent(content);
                            break;
                        case JingleReasonElement.ELEMENT:
                            parser.next();
                            String reasonString = parser.getName();
                            JingleReasonElement reason;
                            if (reasonString.equals("alternative-session")) {
                                parser.next();
                                String sid = parser.nextText();
                                reason = new JingleReasonElement.AlternativeSession(sid);
                            } else {
                                reason = new JingleReasonElement(JingleReasonElement.Reason.fromString(reasonString));
                            }
                            builder.setReason(reason);
                            break;
                        default:
                            LOGGER.severe("Unknown Jingle element: " + tagName);
                            break;
                    }
                    break;
                case XmlPullParser.END_TAG:
                    if (parser.getDepth() == initialDepth) {
                        break outerloop;
                    }
            }
        }

        return builder.build();
    }

    private static JingleContentElement parseJingleContent(XmlPullParser parser, final int initialDepth)
            throws Exception {
        JingleContentElement.Builder builder = JingleContentElement.getBuilder();

        String creatorString = parser.getAttributeValue("", JingleContentElement.CREATOR_ATTRIBUTE_NAME);
        JingleContentElement.Creator creator = JingleContentElement.Creator.valueOf(creatorString);
        builder.setCreator(creator);

        String disposition = parser.getAttributeValue("", JingleContentElement.DISPOSITION_ATTRIBUTE_NAME);
        builder.setDisposition(disposition);

        String name = parser.getAttributeValue("", JingleContentElement.NAME_ATTRIBUTE_NAME);
        builder.setName(name);

        String sendersString = parser.getAttributeValue("", JingleContentElement.SENDERS_ATTRIBUTE_NAME);
        if (sendersString != null) {
            JingleContentElement.Senders senders = JingleContentElement.Senders.valueOf(sendersString);
            builder.setSenders(senders);
        }

        outerloop: while (true) {
            int eventType = parser.next();
            switch (eventType) {
                case XmlPullParser.START_TAG:
                    String tagName = parser.getName();
                    String namespace = parser.getNamespace();
                    switch (tagName) {
                        case JingleContentDescriptionElement.ELEMENT: {
                            JingleContentDescriptionElement description;
                            JingleContentDescriptionProvider<?> provider = JingleManager.getJingleDescriptionProvider(namespace);
                            if (provider == null) {
                                StandardExtensionElement standardExtensionElement = StandardExtensionElementProvider.INSTANCE.parse(parser);
                                description = new UnknownJingleContentDescriptionElement(standardExtensionElement);
                            }
                            else {
                                description = provider.parse(parser);
                            }
                            builder.setDescription(description);
                            break;
                        }
                        case JingleContentTransportElement.ELEMENT: {
                            JingleContentTransportElement transport;
                            JingleContentTransportProvider<?> provider = JingleManager.getJingleTransportProvider(namespace);
                            if (provider == null) {
                                StandardExtensionElement standardExtensionElement = StandardExtensionElementProvider.INSTANCE.parse(parser);
                                transport = new UnknownJingleContentTransportElement(standardExtensionElement);
                            }
                            else {
                                transport = provider.parse(parser);
                            }
                            builder.setTransport(transport);
                            break;
                        }
                        case JingleContentSecurityElement.ELEMENT: {
                            JingleContentSecurityElement security;
                            JingleContentSecurityProvider<?> provider = JingleManager.getJingleSecurityProvider(namespace);
                            if (provider == null) {
                                StandardExtensionElement standardExtensionElement = StandardExtensionElementProvider.INSTANCE.parse(parser);
                                security = new UnknownJingleContentSecurityElement(standardExtensionElement);
                            } else {
                                security = provider.parse(parser);
                            }
                            builder.setSecurity(security);
                            break;
                        }
                        default:
                            LOGGER.severe("Unknown Jingle content element: " + tagName);
                            break;
                    }
                    break;
                case XmlPullParser.END_TAG:
                    if (parser.getDepth() == initialDepth) {
                        break outerloop;
                    }
            }
        }

        return builder.build();
    }
}
