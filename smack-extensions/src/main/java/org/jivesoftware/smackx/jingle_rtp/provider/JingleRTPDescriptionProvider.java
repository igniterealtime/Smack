/**
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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;
import org.jivesoftware.smackx.jingle.provider.JingleContentDescriptionProvider;
import org.jivesoftware.smackx.jingle_rtp.AbstractXmlElement;
import org.jivesoftware.smackx.jingle_rtp.element.RtpDescription;

/**
 * Provider for RtpDescription elements.
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
     * @return a new stanza extension instance.
     * @throws IOException, XmlPullParserException, ParseException if an error occurs parsing the XML.
     */
    @Override
    public RtpDescription parse(XmlPullParser parser, int depth, XmlEnvironment xmlEnvironment)
            throws IOException, XmlPullParserException {
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
                    ExtensionElementProvider<?> provider = ProviderManager.getExtensionProvider(name, namespace);
                    // Extension element provider may not have been added properly if null
                    if (provider == null) { //  && !JingleFileTransfer.NAMESPACE_V5.equals(namespace)) {
                        LOGGER.log(Level.WARNING, "No provider for EE<", name + " " + namespace + "/>");
                    } else {
                        try {
                            ExtensionElement childExtension = provider.parse(parser);
                            if (childExtension instanceof AbstractXmlElement) {
                                mBuilder.addChildElement(childExtension);
                            } else
                                LOGGER.log(Level.WARNING, "Invalid Abstract Element: " + childExtension.getQName());
                        } catch (SmackParsingException e) {
                            LOGGER.log(Level.WARNING, "Parse childElement exception: " + e.getMessage());
                        }
                    }
                    break;

                case TEXT_CHARACTERS:
                    mBuilder.setText(parser.getText());
                    break;

                case END_ELEMENT:
                    if (depth == parser.getDepth()) {
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
