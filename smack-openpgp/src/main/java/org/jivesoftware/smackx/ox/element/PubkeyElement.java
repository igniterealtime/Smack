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
import java.util.Date;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.NamedElement;
import org.jivesoftware.smack.util.Objects;
import org.jivesoftware.smack.util.XmlStringBuilder;

public class PubkeyElement implements ExtensionElement {

    public static final String NAMESPACE = OpenPgpElement.NAMESPACE;
    public static final String ELEMENT = "pubkey";
    public static final String ATTR_DATE = "date";

    private final PubkeyDataElement dataElement;
    private final Date date;

    public PubkeyElement(PubkeyDataElement dataElement, Date date) {
        this.dataElement = Objects.requireNonNull(dataElement);
        this.date = date;
    }

    public PubkeyDataElement getDataElement() {
        return dataElement;
    }

    public Date getDate() {
        return date;
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
                .optAttribute(ATTR_DATE, date)
                .rightAngleBracket()
                .element(getDataElement())
                .closeElement(this);
        return xml;
    }

    public static class PubkeyDataElement implements NamedElement {

        public static final String ELEMENT = "data";

        private final byte[] b64Data;

        public PubkeyDataElement(byte[] b64Data) {
            this.b64Data = b64Data;
        }

        public byte[] getB64Data() {
            return b64Data;
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
}
