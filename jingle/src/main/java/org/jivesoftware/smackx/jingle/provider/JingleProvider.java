/**
 *
 * Copyright 2003-2005 Jive Software.
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

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smackx.jingle.JingleActionEnum;
import org.jivesoftware.smackx.jingle.packet.*;
import org.xmlpull.v1.XmlPullParser;

/**
 * The JingleProvider parses Jingle packets.
 * 
 * @author Alvaro Saurin
 */
public class JingleProvider implements IQProvider {

    /**
     * Creates a new provider. ProviderManager requires that every
     * PacketExtensionProvider has a public, no-argument constructor
     */
    public JingleProvider() {
        super();
    }

    /**
     * Parse a iq/jingle element.
     */
    public IQ parseIQ(final XmlPullParser parser) throws Exception {

        Jingle jingle = new Jingle();
        String sid = "";
        JingleActionEnum action;
        String initiator = "";
        String responder = "";
        boolean done = false;
        JingleContent currentContent = null;

        // Sub-elements providers
        JingleContentProvider jcp = new JingleContentProvider();
        JingleDescriptionProvider jdpAudio = new JingleDescriptionProvider.Audio();
        JingleTransportProvider jtpRawUdp = new JingleTransportProvider.RawUdp();
        JingleTransportProvider jtpIce = new JingleTransportProvider.Ice();
        JingleContentInfoProvider jmipAudio = new JingleContentInfoProvider.Audio();

        int eventType;
        String elementName;
        String namespace;

        // Get some attributes for the <jingle> element
        sid = parser.getAttributeValue("", "sid");
        action = JingleActionEnum.getAction(parser.getAttributeValue("", "action"));
        initiator = parser.getAttributeValue("", "initiator");
        responder = parser.getAttributeValue("", "responder");

        jingle.setSid(sid);
        jingle.setAction(action);
        jingle.setInitiator(initiator);
        jingle.setResponder(responder);

        // Start processing sub-elements
        while (!done) {
            eventType = parser.next();
            elementName = parser.getName();
            namespace = parser.getNamespace();

            if (eventType == XmlPullParser.START_TAG) {

                // Parse some well know subelements, depending on the namespaces
                // and element names...

                if (elementName.equals(JingleContent.NODENAME)) {
                    // Add a new <content> element to the jingle
                    currentContent = (JingleContent) jcp.parseExtension(parser);
                    jingle.addContent(currentContent);
                } else if (elementName.equals(JingleDescription.NODENAME) && namespace.equals(JingleDescription.Audio.NAMESPACE)) {
                    // Set the <description> element of the <content>
                    currentContent.setDescription((JingleDescription) jdpAudio.parseExtension(parser));
                } else if (elementName.equals(JingleTransport.NODENAME)) {
                    // Add all of the <transport> elements to the <content> of the jingle

                    // Parse the possible transport namespaces
                    if (namespace.equals(JingleTransport.RawUdp.NAMESPACE)) {
                        currentContent.addJingleTransport((JingleTransport) jtpRawUdp.parseExtension(parser));
                    } else if (namespace.equals(JingleTransport.Ice.NAMESPACE)) {
                        currentContent.addJingleTransport((JingleTransport) jtpIce.parseExtension(parser));
                    } else {
                        throw new XMPPException("Unknown transport namespace \"" + namespace + "\" in Jingle packet.");
                    }
                } else if (namespace.equals(JingleContentInfo.Audio.NAMESPACE)) {
                    jingle.setContentInfo((JingleContentInfo) jmipAudio.parseExtension(parser));
                } else {
                    throw new XMPPException("Unknown combination of namespace \"" + namespace + "\" and element name \""
                            + elementName + "\" in Jingle packet.");
                }

            } else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals(Jingle.getElementName())) {
                    done = true;
                }
            }
        }

        return jingle;
    }
}
