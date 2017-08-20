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
package org.jivesoftware.smackx.jet.element;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jivesoftware.smackx.jet.component.JetSecurity;
import org.jivesoftware.smackx.jingle.element.JingleContentSecurityElement;

/**
 * Implementation of the Jingle security element as specified in XEP-XXXX (Jingle Encrypted Transfers).
 * <jingle>
 *     <content>
 *         <description/>
 *         <transport/>
 *         <security/> <- You are here.
 *     </content>
 * </jingle>
 */
public class JetSecurityElement extends JingleContentSecurityElement {
    public static final String ATTR_CONTENT_NAME = "name";
    public static final String ATTR_ENVELOPE_TYPE = "type";
    public static final String ATTR_CIPHER_TYPE = "cipher";

    private final ExtensionElement child;
    private final String contentName;
    private final String cipherName;

    public JetSecurityElement(String contentName, String cipherName, ExtensionElement child) {
        this.contentName = contentName;
        this.child = child;
        this.cipherName = cipherName;
    }

    @Override
    public CharSequence toXML() {
        XmlStringBuilder xml = new XmlStringBuilder(this);
        xml.attribute(ATTR_CONTENT_NAME, contentName)
                .attribute(ATTR_CIPHER_TYPE, cipherName)
                .attribute(ATTR_ENVELOPE_TYPE, child.getNamespace());
        xml.rightAngleBracket();
        xml.element(child);
        xml.closeElement(this);
        return xml;
    }

    @Override
    public String getNamespace() {
        return JetSecurity.NAMESPACE;
    }

    public String getEnvelopeNamespace() {
        return child.getNamespace();
    }

    public ExtensionElement getChild() {
        return child;
    }

    public String getContentName() {
        return contentName;
    }

    public String getCipherName() {
        return cipherName;
    }
}
