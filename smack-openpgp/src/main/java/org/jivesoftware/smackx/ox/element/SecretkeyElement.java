/**
 *
 * Copyright 2018 Paul Schaub.
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

import java.nio.charset.Charset;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.util.Objects;
import org.jivesoftware.smack.util.XmlStringBuilder;

/**
 * This class represents a secretkey element which contains a users OpenPGP secret key.
 *
 * TODO: Update reflink
 * @see <a href="https://xmpp.org/extensions/xep-0373.html#sect-idm46443026813600">
 *     XEP-0373: §5.2.2 PEP Service Success Response</a>
 */
public class SecretkeyElement implements ExtensionElement {

    public static final String NAMESPACE = OpenPgpElement.NAMESPACE;
    public static final String ELEMENT = "secretkey";

    private final byte[] b64Data;

    public SecretkeyElement(byte[] b64Data) {
        this.b64Data = Objects.requireNonNull(b64Data);
    }

    public byte[] getB64Data() {
        return b64Data;
    }

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public XmlStringBuilder toXML(String enclosingNamespace) {
        XmlStringBuilder xml = new XmlStringBuilder(this)
                .rightAngleBracket()
                .append(new String(b64Data, Charset.forName("UTF-8")))
                .closeElement(this);
        return xml;
    }
}
