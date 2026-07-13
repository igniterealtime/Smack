/*
 *
 * Copyright 2017-2022 Eng Chong Meng
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
package org.jivesoftware.smackx.jinglemessage.provider;

import java.io.IOException;

import org.jivesoftware.smack.packet.NamedElement;
import org.jivesoftware.smack.packet.StandardExtensionElement;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.jivesoftware.smackx.jingle.element.JingleReason;
import org.jivesoftware.smackx.jingle.provider.JingleProvider;
import org.jivesoftware.smackx.jingle_rtp.element.RtpDescription;
import org.jivesoftware.smackx.jinglemessage.element.JingleMessage;
import org.jivesoftware.smackx.jinglemessage.element.MigratedElement;
import org.jivesoftware.smackx.jinglemessage.element.TieBreakElement;

import org.jxmpp.JxmppContext;

/**
 * The JingleMessageProvider parses Jingle Message extension.
 * @see <a href="https://xmpp.org/extensions/xep-0353.html">XEP-0353: Jingle Message Initiation</a>
 *
 * @author Eng Chong Meng
 */
public class JingleMessageProvider {
    public static JingleMessage parse(StandardExtensionElement extElement)
            throws XmlPullParserException, IOException, SmackParsingException {

        JingleReason reason = null;
        RtpDescription rtpDescription = null;
        NamedElement element = null;

        String action = extElement.getElementName();
        String uuid = extElement.getAttributeValue(JingleMessage.ATTR_ID);

        XmlPullParser parser = PacketParserUtils.getParserFor(extElement.toXML().toString());
        while (true) {
            XmlPullParser.Event eventType = parser.next();
            if (eventType == XmlPullParser.Event.START_ELEMENT) {
                String elementName = parser.getName();
                switch (elementName) {
                case JingleReason.ELEMENT:
                    reason = JingleProvider.parseJingleReason(parser, JxmppContext.getDefaultContext());
                    break;

                case RtpDescription.ELEMENT:
                    ExtensionElementProvider<?> provider = ProviderManager.getExtensionProvider(
                            RtpDescription.ELEMENT, RtpDescription.NAMESPACE);
                    rtpDescription = (RtpDescription) provider.parse(parser);
                    break;

                case MigratedElement.ELEMENT:
                    String toId = extElement.getFirstElement(elementName).getAttributeValue(MigratedElement.ATTR_TO_ID);
                    element = new MigratedElement(toId);
                    break;

                case TieBreakElement.ELEMENT:
                    element = new TieBreakElement();
                    break;
                }
            }
            else if (eventType == XmlPullParser.Event.END_ELEMENT) {
                break;
            }
        }

        JingleMessage jingleMessage = new JingleMessage(action, uuid, reason, element);
        jingleMessage.addElement(rtpDescription);
        return jingleMessage;
    }
}
