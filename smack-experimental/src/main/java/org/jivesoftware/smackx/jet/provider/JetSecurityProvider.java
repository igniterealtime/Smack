/**
 *
 * Copyright 2017-2022 Paul Schaub
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
package org.jivesoftware.smackx.jet.provider;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.util.Objects;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;
import org.jivesoftware.smackx.jet.JetManager;
import org.jivesoftware.smackx.jet.component.JetSecurityImpl;
import org.jivesoftware.smackx.jet.element.JetSecurity;
import org.jivesoftware.smackx.jingle.provider.JingleContentSecurityProvider;

/**
 * Provider for the Jingle security element for XEP-0391.
 * @see <a href="https://xmpp.org/extensions/xep-0391.html">XEP-0391: Jingle Encrypted Transports 0.1.2 (2018-07-31))</a>
 *
 * @author Paul Schaub
 * @author Eng Chong Meng
 */
public class JetSecurityProvider extends JingleContentSecurityProvider<JetSecurity> {
    private static final Logger LOGGER = Logger.getLogger(JetSecurityProvider.class.getName());

    @Override
    public JetSecurity parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment)
            throws IOException, XmlPullParserException, SmackParsingException {
        String name = parser.getAttributeValue("", JetSecurity.ATTR_NAME);
        String cipher = parser.getAttributeValue("", JetSecurity.ATTR_CIPHER);
        String type = parser.getAttributeValue("", JetSecurity.ATTR_TYPE);
        ExtensionElement child;

        Objects.requireNonNull(type);
        Objects.requireNonNull(cipher);

        ExtensionElementProvider<?> encryptionElementProvider = JetManager.getEnvelopeProvider(type);
        if (encryptionElementProvider != null) {
            child = encryptionElementProvider.parse(parser);
        } else {
            LOGGER.log(Level.WARNING, "Unknown child element in JetSecurity: " + type);
            return null;
        }
        return new JetSecurity(name, cipher, child);
    }

    @Override
    public String getNamespace() {
        return JetSecurityImpl.NAMESPACE;
    }
}
