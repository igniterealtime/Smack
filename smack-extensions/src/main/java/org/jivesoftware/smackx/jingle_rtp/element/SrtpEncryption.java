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

import java.util.List;

import org.jivesoftware.smackx.jingle_rtp.AbstractXmlElement;

/**
 * The element transporting encryption information during jingle session establishment.
 * Crypto supported: ZRTP and SDES;
 * Note: SDES_SRTP fingerPrints element is embedded in <code>transport</code> element
 *
 * XEP-0167: Jingle RTP Sessions 1.2.1 (2020-09-29)
 *
 * @author Emil Ivov
 * @author Eng Chong Meng
 * @see <a href="https://xmpp.org/extensions/xep-0167.html#srtp">XEP-0167 § 7. Negotiation of SRTP</a>
 *
 * @see <a href="https://xmpp.org/extensions/xep-0262.html">XEP-0262: Use of ZRTP in Jingle RTP Sessions 1.0 (2011-06-15)</a>
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc6189">ZRTP: Media Path Key Agreement for Unicast Secure RTP (April 2011)</a>
 */
public class SrtpEncryption extends AbstractXmlElement {
    /**
     * The name of the "encryption" element.
     */
    public static final String ELEMENT = "encryption";

    /**
     * The namespace of the "encryption" element. It it set to "not null" only for GTalk SDES support
     * (may be set to null once GTalk supports jingle).
     */
    public static final String NAMESPACE = RtpDescription.NAMESPACE; // root NameSpace

    /**
     * The name of the <code>required</code> attribute.
     */
    public static final String ATTR_REQUIRED = "required";

    /**
     * Creates a new instance of this <code>EncryptionExtensionElement</code>.
     */
    public SrtpEncryption() {
        super(getBuilder());
    }

    public SrtpEncryption(Builder builder) {
        super(builder);
    }

    /**
     * Returns <code>true</code> if encryption is required for this session and <code>false</code>
     * otherwise. Default value is <code>false</code>.
     *
     * @return <code>true</code> if encryption is required for this session and <code>false</code> otherwise.
     */
    public boolean isRequired() {
        String required = getAttributeValue(ATTR_REQUIRED);
        return Boolean.parseBoolean(required) || "1".equals(required);
    }

    /**
     * Returns a <b>reference</b> to the list of <code>crypto</code> elements that we have registered
     * with this encryption element so far.
     *
     * @return a <b>reference</b> to the list of <code>crypto</code> elements that we have registered
     * with this encryption element so far.
     */
    public List<SdpCrypto> getCryptoList() {
        return getChildElements(SdpCrypto.class);
    }

    public static Builder getBuilder() {
        return new Builder(ELEMENT, NAMESPACE);
    }

    /**
     * Builder for SrtpEncryption. Use {@link AbstractXmlElement.Builder#Builder(String, String)}
     * to obtain a new instance and {@link #build} to build the SrtpEncryption.
     */
    public static final class Builder extends AbstractXmlElement.Builder<Builder, SrtpEncryption> {
        protected Builder(String element, String namespace) {
            super(element, namespace);
        }

        /**
         * Specifies whether encryption is required for this session or not.
         *
         * @param required <code>true</code> if encryption is required for this session and <code>false</code> otherwise.
         * @return builder instance
         */
        public Builder setRequired(boolean required) {
            if (required)
                addAttribute(ATTR_REQUIRED, Boolean.toString(required));
            else
                addAttribute(ATTR_REQUIRED, null);
            return this;
        }

        /**
         * Adds a new <code>crypto</code> element to this encryption element.
         *
         * @param crypto the new <code>crypto</code> element to add.
         * @return builder instance
         */
        public Builder addCrypto(SdpCrypto crypto) {
            addChildElement(crypto);
            return this;
        }

        @Override
        public SrtpEncryption build() {
            return new SrtpEncryption(this);
        }

        @Override
        public Builder getThis() {
            return this;
        }
    }
}
