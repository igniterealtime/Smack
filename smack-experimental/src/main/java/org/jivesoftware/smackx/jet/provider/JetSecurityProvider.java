/**
 *
 * Copyright 2017 Paul Schaub
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

import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.util.Objects;
import org.jivesoftware.smackx.jet.JetManager;
import org.jivesoftware.smackx.jet.component.JetSecurity;
import org.jivesoftware.smackx.jet.element.JetSecurityElement;
import org.jivesoftware.smackx.jingle.provider.JingleContentSecurityProvider;

import org.xmlpull.v1.XmlPullParser;

/**
 * Provider for the Jingle security element for XEP-XXXX (Jingle Encrypted Transfers).
 */
public class JetSecurityProvider extends JingleContentSecurityProvider<JetSecurityElement> {
    private static final Logger LOGGER = Logger.getLogger(JetSecurityProvider.class.getName());

    @Override
    public JetSecurityElement parse(XmlPullParser parser, int initialDepth) throws Exception {
        String name = parser.getAttributeValue("", JetSecurityElement.ATTR_CONTENT_NAME);
        String cipher = parser.getAttributeValue("", JetSecurityElement.ATTR_CIPHER_TYPE);
        String type = parser.getAttributeValue("", JetSecurityElement.ATTR_ENVELOPE_TYPE);
        ExtensionElement child;

        Objects.requireNonNull(type);
        Objects.requireNonNull(cipher);

        ExtensionElementProvider<?> encryptionElementProvider =
                JetManager.getEnvelopeProvider(type);

        if (encryptionElementProvider != null) {
            child = encryptionElementProvider.parse(parser);
        } else {
            LOGGER.log(Level.WARNING, "Unknown child element in JetSecurityElement: " + type);
            return null;
        }

        return new JetSecurityElement(name, cipher, child);
    }

    @Override
    public String getNamespace() {
        return JetSecurity.NAMESPACE;
    }
}
