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
package org.jivesoftware.smackx.jet.element;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jivesoftware.smackx.jet.component.JetSecurityImpl;
import org.jivesoftware.smackx.jingle.element.JingleContentSecurity;

/**
 * Implementation of the Jingle security element as specified in XEP-0391.
 * @see <a href="https://xmpp.org/extensions/xep-0391.html">XEP-0391: Jingle Encrypted Transports 0.1.2 (2018-07-31))</a>
 *
 * @author Paul Schaub
 * @author Eng Chong Meng
 */
public class JetSecurity extends JingleContentSecurity {
    public static final String ATTR_NAME = "name";
    public static final String ATTR_TYPE = "type";
    public static final String ATTR_CIPHER = "cipher";

    private final ExtensionElement child;
    private final String contentName;
    private final String cipherName;

    public JetSecurity(String contentName, String cipherName, ExtensionElement child) {
        this.contentName = contentName;
        this.child = child;
        this.cipherName = cipherName;
    }

    @Override
    public CharSequence toXML(XmlEnvironment xmlEnvironment) {
        XmlStringBuilder xml = new XmlStringBuilder(this);
        xml.attribute(ATTR_NAME, contentName)
                .attribute(ATTR_CIPHER, cipherName)
                .attribute(ATTR_TYPE, child.getNamespace());
        xml.rightAngleBracket();
        xml.append(child);
        xml.closeElement(this);
        return xml;
    }

    @Override
    public String getNamespace() {
        return JetSecurityImpl.NAMESPACE;
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
