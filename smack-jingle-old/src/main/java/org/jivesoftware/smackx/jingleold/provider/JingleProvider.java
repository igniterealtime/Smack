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

package org.jivesoftware.smackx.jingleold.provider;

import java.io.IOException;

import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smack.util.ParserUtils;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.jivesoftware.smackx.jingleold.JingleActionEnum;
import org.jivesoftware.smackx.jingleold.packet.Jingle;
import org.jivesoftware.smackx.jingleold.packet.JingleContent;
import org.jivesoftware.smackx.jingleold.packet.JingleContentInfo;
import org.jivesoftware.smackx.jingleold.packet.JingleDescription;
import org.jivesoftware.smackx.jingleold.packet.JingleTransport;

import org.jxmpp.jid.Jid;

/**
 * The JingleProvider parses Jingle packets.
 *
 * @author Alvaro Saurin
 */
public class JingleProvider extends IQProvider<Jingle> {

    /**
     * Parse a iq/jingle element.
     * @throws XmlPullParserException if an error in the XML parser occured.
     * @throws IOException if an I/O error occured.
     * @throws SmackParsingException if the Smack parser (provider) encountered invalid input.
     */
    @Override
    public Jingle parse(XmlPullParser parser, int intialDepth, XmlEnvironment xmlEnvironment) throws IOException, XmlPullParserException, SmackParsingException {

        Jingle jingle = new Jingle();
        String sid;
        JingleActionEnum action;
        Jid initiator, responder;
        boolean done = false;
        JingleContent currentContent = null;

        // Sub-elements providers
        JingleContentProvider jcp = new JingleContentProvider();
        JingleDescriptionProvider jdpAudio = new JingleDescriptionProvider.Audio();
        JingleTransportProvider jtpRawUdp = new JingleTransportProvider.RawUdp();
        JingleTransportProvider jtpIce = new JingleTransportProvider.Ice();
        ExtensionElementProvider<?> jmipAudio = new JingleContentInfoProvider.Audio();

        XmlPullParser.Event eventType;
        String elementName;
        String namespace;

        // Get some attributes for the <jingle> element
        sid = parser.getAttributeValue("", "sid");
        action = JingleActionEnum.getAction(parser.getAttributeValue("", "action"));
        initiator = ParserUtils.getJidAttribute(parser, "initiator");
        responder = ParserUtils.getJidAttribute(parser, "responder");

        jingle.setSid(sid);
        jingle.setAction(action);
        jingle.setInitiator(initiator);
        jingle.setResponder(responder);

        // Start processing sub-elements
        while (!done) {
            eventType = parser.next();
            elementName = parser.getName();
            namespace = parser.getNamespace();

            if (eventType == XmlPullParser.Event.START_ELEMENT) {

                // Parse some well know subelements, depending on the namespaces
                // and element names...

                if (elementName.equals(JingleContent.NODENAME)) {
                    // Add a new <content> element to the jingle
                    currentContent = jcp.parse(parser);
                    jingle.addContent(currentContent);
                } else if (elementName.equals(JingleDescription.NODENAME) && namespace.equals(JingleDescription.Audio.NAMESPACE)) {
                    // Set the <description> element of the <content>
                    currentContent.setDescription(jdpAudio.parse(parser));
                } else if (elementName.equals(JingleTransport.NODENAME)) {
                    // Add all of the <transport> elements to the <content> of the jingle

                    // Parse the possible transport namespaces
                    if (namespace.equals(JingleTransport.RawUdp.NAMESPACE)) {
                        currentContent.addJingleTransport(jtpRawUdp.parse(parser));
                    } else if (namespace.equals(JingleTransport.Ice.NAMESPACE)) {
                        currentContent.addJingleTransport(jtpIce.parse(parser));
                    } else {
                        // TODO: Should be SmackParseException.
                        throw new IOException("Unknown transport namespace \"" + namespace + "\" in Jingle packet.");
                    }
                } else if (namespace.equals(JingleContentInfo.Audio.NAMESPACE)) {
                    jingle.setContentInfo((JingleContentInfo) jmipAudio.parse(parser));
                } else {
                    // TODO: Should be SmackParseException.
                    throw new IOException("Unknown combination of namespace \"" + namespace + "\" and element name \""
                            + elementName + "\" in Jingle packet.");
                }

            } else if (eventType == XmlPullParser.Event.END_ELEMENT) {
                if (parser.getName().equals(Jingle.getElementName())) {
                    done = true;
                }
            }
        }

        return jingle;
    }
}
