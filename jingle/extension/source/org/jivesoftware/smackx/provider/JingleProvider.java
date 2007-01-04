/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright 2003-2005 Jive Software.
 *
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
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

package org.jivesoftware.smackx.provider;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smackx.packet.Jingle;
import org.jivesoftware.smackx.packet.JingleContentDescription;
import org.jivesoftware.smackx.packet.JingleContentInfo;
import org.jivesoftware.smackx.packet.JingleTransport;
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
        Jingle.Action action;
        String initiator = "";
        String responder = "";
        boolean done = false;

        // Sub-elements providers
        JingleContentDescriptionProvider jdpAudio = new JingleContentDescriptionProvider.Audio();
        JingleTransportProvider jtpRawUdp = new JingleTransportProvider.RawUdp();
        JingleTransportProvider jtpIce = new JingleTransportProvider.Ice();
        JingleContentInfoProvider jmipAudio = new JingleContentInfoProvider.Audio();

        int eventType;
        String elementName;
        String namespace;

        // Get some attributes for the <jingle> element
        sid = parser.getAttributeValue("", "sid");
        action = Jingle.Action.getAction(parser.getAttributeValue("", "action"));
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

                if (elementName.equals(JingleContentDescription.NODENAME)
                        && namespace.equals(JingleContentDescription.Audio.NAMESPACE)) {
                    jingle.addDescription((JingleContentDescription) jdpAudio
                            .parseExtension(parser));
                } else if (elementName.equals(JingleTransport.NODENAME)) {

                    // Parse the possible transport namespaces
                    if (namespace.equals(JingleTransport.RawUdp.NAMESPACE)) {
                        jingle.addTransport((JingleTransport) jtpRawUdp
                                .parseExtension(parser));
                    } else if (namespace.equals(JingleTransport.Ice.NAMESPACE)) {
                        jingle.addTransport((JingleTransport) jtpIce
                                .parseExtension(parser));
                    } else {
                        throw new XMPPException("Unknown transport namespace \""
                                + namespace + "\" in Jingle packet.");
                    }
                } else if (namespace.equals(JingleContentInfo.Audio.NAMESPACE)) {
                    jingle.setContentInfo((JingleContentInfo) jmipAudio
                            .parseExtension(parser));
                } else if (elementName.equals("content")) {
                    //TODO Separate Contents (XEP-0166)
                } else {
                    throw new XMPPException("Unknown combination of namespace \""
                            + namespace + "\" and element name \"" + elementName
                            + "\" in Jingle packet.");
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
