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

import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smack.util.ParserUtils;

import org.jivesoftware.smackx.jingle.element.Jingle;
import org.jivesoftware.smackx.jingle.element.JingleAction;
import org.jivesoftware.smackx.jingle.element.JingleContent;
import org.jivesoftware.smackx.jingle.element.JingleContentDescription;
import org.jivesoftware.smackx.jingle.element.JingleContentTransport;
import org.jivesoftware.smackx.jingle.element.JingleReason;
import org.jivesoftware.smackx.jingle.element.JingleReason.Reason;

import org.jxmpp.jid.FullJid;
import org.xmlpull.v1.XmlPullParser;

public class JingleProvider extends IQProvider<Jingle> {

    private static final Logger LOGGER = Logger.getLogger(JingleProvider.class.getName());

    @Override
    public Jingle parse(XmlPullParser parser, int initialDepth) throws Exception {
        Jingle.Builder builder = Jingle.getBuilder();

        String actionString = parser.getAttributeValue("", Jingle.ACTION_ATTRIBUTE_NAME);
        if (actionString != null) {
            JingleAction action = JingleAction.fromString(actionString);
            builder.setAction(action);
        }

        FullJid initiator = ParserUtils.getFullJidAttribute(parser, Jingle.INITIATOR_ATTRIBUTE_NAME);
        builder.setInitiator(initiator);

        FullJid responder = ParserUtils.getFullJidAttribute(parser, Jingle.RESPONDER_ATTRIBUTE_NAME);
        builder.setResponder(responder);

        String sessionId = parser.getAttributeValue("", Jingle.SESSION_ID_ATTRIBUTE_NAME);
        builder.setSessionId(sessionId);


        outerloop: while (true) {
            int eventType = parser.next();
            switch (eventType) {
            case XmlPullParser.START_TAG:
                String tagName = parser.getName();
                switch (tagName) {
                case JingleContent.ELEMENT:
                    JingleContent content = parseJingleContent(parser, parser.getDepth());
                    builder.addJingleContent(content);
                    break;
                case JingleReason.ELEMENT:
                    parser.next();
                    String reasonString = parser.getName();
                    Reason reason = Reason.fromString(reasonString);
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

    public static JingleContent parseJingleContent(XmlPullParser parser, final int initialDepth)
                    throws Exception {
        JingleContent.Builder builder = JingleContent.getBuilder();

        String creatorString = parser.getAttributeValue("", JingleContent.CREATOR_ATTRIBUTE_NAME);
        JingleContent.Creator creator = JingleContent.Creator.valueOf(creatorString);
        builder.setCreator(creator);

        String disposition = parser.getAttributeValue("", JingleContent.DISPOSITION_ATTRIBUTE_NAME);
        builder.setDisposition(disposition);

        String name = parser.getAttributeValue("", JingleContent.NAME_ATTRIBUTE_NAME);
        builder.setName(name);

        String sendersString = parser.getAttributeValue("", JingleContent.SENDERS_ATTRIBUTE_NAME);
        if (sendersString != null) {
            JingleContent.Senders senders = JingleContent.Senders.valueOf(sendersString);
            builder.setSenders(senders);
        }

        outerloop: while (true) {
            int eventType = parser.next();
            switch (eventType) {
            case XmlPullParser.START_TAG:
                String tagName = parser.getName();
                String namespace = parser.getNamespace();
                switch (tagName) {
                case JingleContentDescription.ELEMENT: {
                    JingleContentDescriptionProvider<?> provider = JingleContentProviderManager.getJingleContentDescriptionProvider(namespace);
                    if (provider == null) {
                        // TODO handle this case (DefaultExtensionElement wrapped in something?)
                        break;
                    }
                    JingleContentDescription description = provider.parse(parser);
                    builder.setDescription(description);
                    break;
                }
                case JingleContentTransport.ELEMENT: {
                    JingleContentTransportProvider<?> provider = JingleContentProviderManager.getJingleContentTransportProvider(namespace);
                    if (provider == null) {
                        // TODO handle this case (DefaultExtensionElement wrapped in something?)
                        break;
                    }
                    JingleContentTransport transport = provider.parse(parser);
                    builder.setTransport(transport);
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
