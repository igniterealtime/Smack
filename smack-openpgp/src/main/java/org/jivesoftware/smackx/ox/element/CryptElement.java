/**
 *
 * Copyright 2017 Florian Schmaus, 2018 Paul Schaub.
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
package org.jivesoftware.smackx.ox.element;

import java.util.Date;
import java.util.List;
import java.util.Set;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.util.XmlStringBuilder;

import org.jxmpp.jid.Jid;

/**
 * This class describes an OpenPGP content element which is encrypted, but not signed.
 */
public class CryptElement extends EncryptedOpenPgpContentElement {

    public static final String ELEMENT = "crypt";

    public CryptElement(Set<Jid> to, String rpad, Date timestamp, List<ExtensionElement> payload) {
        super(to, rpad, timestamp, payload);
    }

    public CryptElement(Set<Jid> to, List<ExtensionElement> payload) {
        super(to, payload);
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public XmlStringBuilder toXML(String enclosingNamespace) {
        XmlStringBuilder xml = new XmlStringBuilder(this).rightAngleBracket();
        addCommonXml(xml);
        xml.closeElement(this);
        return xml;
    }

}
