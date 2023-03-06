/**
 *
 * Copyright 2017-2022 Eng Chong Meng
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
package org.jivesoftware.smackx.jingle_rtp.element;

import javax.xml.namespace.QName;

import org.jivesoftware.smackx.jingle_rtp.AbstractXmlElement;

import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

/**
 * Implements <code>ExtensionElement</code> for the "transfer" element defined below.
 * XEP-0251: Jingle Session Transfer 0.2 (2009-10-05)
 * @see <a href="https://xmpp.org/extensions/xep-0251.html#unattended">XEP-0251 § 2. Unattended Transfer</a>
 * @see <a href="https://xmpp.org/extensions/xep-0251.html#attended">XEP-0251 § 3. Attended Transfer</a>
 *
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc5359#page-50">RFC5359 § 2.4. Transfer-Unattended/Attended</a>
 *
 * @author Lyubomir Marinov
 * @author Eng Chong Meng
 */
public class SdpTransfer extends AbstractXmlElement {
    /**
     * The name of the "transfer" element.
     */
    public static final String ELEMENT = "transfer";

    /**
     * The namespace of the "transfer" element.
     */
    public static final String NAMESPACE = "urn:xmpp:jingle:transfer:0";

    public static final QName QNAME = new QName(NAMESPACE, ELEMENT);

    /**
     * The name of the "from" attribute of the "transfer" element.
     * Both the ATTR_FROM and ATTR_TO are mutually exclusive in a Unattended Transfer
     * @see <a href="https://xmpp.org/extensions/xep-0251.html#unattended">XEP-0251 § 2. Unattended Transfer</a>
     */
    public static final String ATTR_FROM = "from";

    /**
     * The name of the "sid" attribute of the "transfer" element. Used only in Attended Transfer (Example 17).
     * @see <a href="https://xmpp.org/extensions/xep-0251.html#attended">XEP-0251 § 3. Attended Transfer</a>
     */
    public static final String ATTR_SID = "sid";

    /**
     * The name of the "to" attribute of the "transfer" element.
     */
    public static final String ATTR_TO = "to";

    public SdpTransfer() {
        super(getBuilder());
    }

    /**
     * Initializes a new <code>SdpTransfer</code> instance.; required by DefaultXmlElementProvider().
     *
     * @param builder Builder instance
     */
    public SdpTransfer(Builder builder) {
        super(builder);
    }

    /**
     * Gets the value of the "from" attribute of this "transfer" element.
     *
     * @return the value of the "from" attribute of this "transfer" element
     */
    public Jid getFrom() {
        try {
            return JidCreate.from(getAttributeValue(ATTR_FROM));
        } catch (XmppStringprepException | IllegalArgumentException e) {
            return null;
        }
    }

    /**
     * Gets the value of the "sid" attribute of this "transfer" element.
     *
     * @return the value of the "sid" attribute of this "transfer" element
     */
    public String getSid() {
        return getAttributeValue(ATTR_SID);
    }

    /**
     * Gets the value of the "to" attribute of this "transfer" element.
     *
     * @return the value of the "to" attribute of this "transfer" element
     */
    public Jid getTo() {
        try {
            return JidCreate.from(getAttributeValue(ATTR_TO));
        } catch (XmppStringprepException e) {
            return null;
        }
    }

    public static Builder getBuilder() {
        return new Builder(ELEMENT, NAMESPACE);
    }

    /**
     * Builder for SdpTransfer. Use {@link AbstractXmlElement.Builder#Builder(String, String)}
     * to obtain a new instance and {@link #build} to build the SdpTransfer.
     */
    public static class Builder extends AbstractXmlElement.Builder<Builder, SdpTransfer> {
        protected Builder(String element, String namespace) {
            super(element, namespace);
        }

        /**
         * Sets the value of the "from" attribute of this "transfer" element.
         *
         * @param from the value of the "from" attribute of this "transfer" element
         * @return builder instance
         */
        public Builder setFrom(Jid from) {
            addAttribute(ATTR_FROM, from.toString());
            return this;
        }

        /**
         * Sets the value of the "sid" attribute of this "transfer" element.
         *
         * @param sid the value of the "sid" attribute of this "transfer" element
         * @return builder instance
         */
        public Builder setSid(String sid) {
            addAttribute(ATTR_SID, sid);
            return this;
        }

        /**
         * Sets the value of the "to" attribute of this "transfer" element.
         *
         * @param to the value of the "to" attribute of this "transfer" element
         * @return builder instance
         */
        public Builder setTo(Jid to) {
            addAttribute(ATTR_TO, to.toString());
            return this;
        }

        @Override
        public SdpTransfer build() {
            return new SdpTransfer(this);
        }

        @Override
        public Builder getThis() {
            return this;
        }
    }
}
