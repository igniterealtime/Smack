/*
 *
 * Copyright 2022 Eng Chong Meng
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
package org.jivesoftware.smackx.jingle_rtp.provider;

import java.io.IOException;
import java.text.ParseException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.packet.NamedElement;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.provider.Provider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.jivesoftware.smackx.jingle.provider.JingleContentDescriptionProvider;
import org.jivesoftware.smackx.jingle.provider.JingleContentProviderManager;
import org.jivesoftware.smackx.jingle_rtp.element.RtpDescription;

import org.jxmpp.JxmppContext;

/**
 * Provider for RtpDescription elements.
 *
 * @author Eng Chong Meng
 */
public class JingleRTPDescriptionProvider extends JingleContentDescriptionProvider<RtpDescription> {
    private static final Logger LOGGER = Logger.getLogger(JingleRTPDescriptionProvider.class.getName());

    /**
     * Parse an extension sub-stanza and create a <code>IceUdpTransport</code> instance. At the beginning
     * of the method call, the xml parser will be positioned on the opening element of the stanza extension
     * and at the end of the method call it will be on the closing element of the stanza extension.
     *
     * @param parser an XML parser positioned at the stanza's starting element.
     *
     * @return a new stanza extension instance.
     *
     * @throws IOException if an error occurs in IO.
     * @throws XmlPullParserException if an error occurs pull parsing the XML.
     */
    @Override
    public RtpDescription parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment, JxmppContext jxmppContext)
            throws XmlPullParserException, IOException, SmackParsingException, ParseException {
        RtpDescription.Builder mBuilder = RtpDescription.getBuilder();

        // first, set all the attributes
        int attrCount = parser.getAttributeCount();
        for (int i = 0; i < attrCount; i++) {
            mBuilder.addAttribute(parser.getAttributeName(i), parser.getAttributeValue(i));
        }

        outerloop:
        while (true) {
            XmlPullParser.Event event = parser.next();
            switch (event) {
            case START_ELEMENT:
                String name = parser.getName();
                String namespace = parser.getNamespace();

                // Timber.d("<%s %s/> class: %s", elementName, namespace, stanzaExtension.getClass().getSimpleName());
                Provider<?> provider = JingleContentProviderManager.getJingleContentElementProvider(name);
                if (provider == null) {
                    provider = ProviderManager.getExtensionProvider(name, namespace);
                }

                if (provider == null) {
                    LOGGER.log(Level.WARNING, "No RtpDescription provider for EE<", name + " " + namespace + "/>");
                }
                else {
                    // LOGGER.log(Level.SEVERE, "Provider for RtpDescription Element: " + name + " " + provider);
                    try {
                        NamedElement childExtension = (NamedElement) provider.parse(parser);
                        if (childExtension != null) {
                            mBuilder.addChildElement(childExtension);
                        }
                        else
                            LOGGER.log(Level.WARNING, "Invalid RtpDescription Element: " + name);
                    }
                    catch (SmackParsingException e) {
                        LOGGER.log(Level.WARNING, "Parse childElement exception: " + e.getMessage());
                    }
                }
                break;

            case TEXT_CHARACTERS:
                mBuilder.setText(parser.getText());
                break;

            case END_ELEMENT:
                if (initialDepth == parser.getDepth()) {
                    break outerloop;
                }
                break;

            // Catch all for incomplete switch (event) statement.
            default:
                break;
            }
        }
        return mBuilder.build();
    }
}
