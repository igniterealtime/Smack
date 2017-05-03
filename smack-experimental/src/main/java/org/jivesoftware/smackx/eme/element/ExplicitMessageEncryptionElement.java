/**
 *
 * Copyright 2017 Florian Schmaus
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
package org.jivesoftware.smackx.eme.element;

import java.util.HashMap;
import java.util.Map;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.XmlStringBuilder;

public class ExplicitMessageEncryptionElement implements ExtensionElement {

    private static final Map<String, ExplicitMessageEncryptionProtocol> PROTOCOL_LUT = new HashMap<>();

    public static final String ELEMENT = "encryption";

    public static final String NAMESPACE = "urn:xmpp:eme:0";

    public enum ExplicitMessageEncryptionProtocol {

        /**
         * The encryption method specified in <a href="https://xmpp.org/extensions/xep-0373.html">XEP-0373: OpenPGP for
         * XMPP</a>.
         */
        openpgpV0("urn:xmpp:openpgp:0", "OpenPGP for XMPP (XEP-0373)"),

        otrV0("urn:xmpp:otr:0", "Off-the-Record Messaging (XEP-0364)"),

        legacyOpenPGP("jabber:x:encrypted", "Legacy OpenPGP for XMPP [DANGEROUS, DO NOT USE!]"),
        ;

        private final String namespace;
        private final String name;

        private ExplicitMessageEncryptionProtocol(String namespace, String name) {
            this.namespace = namespace;
            this.name = name;
            PROTOCOL_LUT.put(namespace, this);
        }

        public String getNamespace() {
            return namespace;
        }

        public String getName() {
            return name;
        }

        public static ExplicitMessageEncryptionProtocol from(String namespace) {
            return PROTOCOL_LUT.get(namespace);
        }
    }

    private final String encryptionNamespace;

    private final String name;

    private boolean isUnknownProtocol;

    private ExplicitMessageEncryptionProtocol protocolCache;

    public ExplicitMessageEncryptionElement(ExplicitMessageEncryptionProtocol protocol) {
        this(protocol.getNamespace(), protocol.getName());
    }

    public ExplicitMessageEncryptionElement(String encryptionNamespace) {
        this(encryptionNamespace, null);
    }

    public ExplicitMessageEncryptionElement(String encryptionNamespace, String name) {
        this.encryptionNamespace = StringUtils.requireNotNullOrEmpty(encryptionNamespace,
                        "encryptionNamespace must not be null");
        this.name = name;
    }

    public ExplicitMessageEncryptionProtocol getProtocol() {
        if (protocolCache != null) {
            return protocolCache;
        }

        if (isUnknownProtocol) {
            return null;
        }

        ExplicitMessageEncryptionProtocol protocol = PROTOCOL_LUT.get(encryptionNamespace);
        if (protocol == null) {
            isUnknownProtocol = true;
            return null;
        }

        protocolCache = protocol;
        return protocol;
    }

    public String getEncryptionNamespace() {
        return encryptionNamespace;
    }

    /**
     * Get the optional name of the encryption method.
     *
     * @return the name of the encryption method or <code>null</code>.
     */
    public String getName() {
        return name;
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

    @Override
    public XmlStringBuilder toXML() {
        XmlStringBuilder xml = new XmlStringBuilder(this);
        xml.attribute("namespace", getEncryptionNamespace());
        xml.optAttribute("name", getName());
        xml.closeEmptyElement();
        return xml;
    }

    public static ExplicitMessageEncryptionElement from(Message message) {
        return message.getExtension(ELEMENT, NAMESPACE);
    }
}
