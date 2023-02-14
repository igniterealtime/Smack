/**
 *
 * Copyright 2017-2022 Jive Software
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
package org.jivesoftware.smackx.jingle_rtp;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

/**
 * A provider that parses incoming stanza extensions into instances of the {@link Class} that it has
 * been instantiated for.
 *
 * @param <EE> Class that the stanzas we will be parsing belong to
 * @author Emil Ivov
 * @author Eng Chong Meng
 */
public class DefaultXmlElementProvider<EE extends AbstractXmlElement> extends ExtensionElementProvider<EE> {
    /**
     * The {@link Class} that the stanza we will be parsing here belong to.
     */
    private final Class<EE> stanzaClass;

    private final String nameSpace;

    private static final Logger LOGGER = Logger.getLogger(DefaultXmlElementProvider.class.getName());

    /**
     * Creates a new stanza provider for the specified stanza extensions.
     *
     * @param c the {@link Class} that the stanza we will be parsing belong to.
     * @param nameSpace stanzas builder with the modified nameSpace
     */
    public DefaultXmlElementProvider(Class<EE> c, String nameSpace) {
        stanzaClass = c;
        this.nameSpace = nameSpace;
    }

    public DefaultXmlElementProvider(Class<EE> c) {
        stanzaClass = c;
        nameSpace = null;
    }

    /**
     * Parse an extension sub-stanza and create a <code>EE</code> instance. At the beginning of the
     * method call, the xml parser will be positioned on the opening element of the stanza extension
     * and at the end of the method call it will be on the closing element of the stanza extension.
     *
     * @param parser an XML parser positioned at the stanza's starting element.
     * @return a new stanza extension instance.
     * @throws IOException if an error occurs in IO.
     * @throws XmlPullParserException if an error occurs pull parsing the XML.
     * @throws SmackParsingException if an error occurs parsing the XML.
     */
    @Override
    public EE parse(XmlPullParser parser, int depth, XmlEnvironment xmlEnvironment)
            throws IOException, XmlPullParserException, SmackParsingException {
        EE stanzaExtension;
        try {
            stanzaExtension = stanzaClass.getDeclaredConstructor().newInstance();
        } catch (IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException ignore) {
            LOGGER.log(Level.WARNING, "Unknown stanza class: " + parser.getName());
            return null;
        }
        AbstractXmlElement.Builder<?, ?> mBuilder = stanzaExtension.getBuilder(nameSpace);

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
                        LOGGER.log(Level.WARNING, "No provider for EE<" + name + " " + namespace + "/>");
                    } else {
                        ExtensionElement childExtension = provider.parse(parser);
                        if (childExtension instanceof AbstractXmlElement) {
                            mBuilder.addChildElement(childExtension);
                        } else
                            LOGGER.log(Level.INFO, "Invalid Abstract Element: " + childExtension.getQName());

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
        return stanzaClass.cast(mBuilder.build());
    }
}
