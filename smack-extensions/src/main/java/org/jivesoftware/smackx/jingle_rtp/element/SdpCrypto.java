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

import org.jivesoftware.smackx.jingle_rtp.AbstractXmlElement;

import java.util.Objects;

import javax.xml.namespace.QName;

/**
 * The element containing details about an encryption algorithm that could be used during a jingle session.
 * @see <a href="https://xmpp.org/extensions/xep-0167.html#srtp">XEP-0167: Jingle RTP Sessions 1.2.1 (2020-09-29)</a>
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc4568">Session Description Protocol (SDP) Security Descriptions for Media Streams</a>
 *
 * @author Emil Ivov
 * @author Vincent Lucas
 * @author Eng Chong Meng
 */
public class SdpCrypto extends AbstractXmlElement {
    /**
     * The name of the "crypto" element.
     */
    public static final String ELEMENT = "crypto";

    /**
     * The namespace for the "crypto" element. It it set to "not null" only for GTalk SDES support
     * (may be set to null once gtalk supports jingle).
     */
    public static final String NAMESPACE = RtpDescription.NAMESPACE;

    public static final QName QNAME = new QName(NAMESPACE, ELEMENT);

    /**
     * The name of the 'crypto-suite' argument.
     */
    public static final String ATTR_CRYPTO_SUITE = "crypto-suite";

    /**
     * The name of the 'key-params' argument.
     */
    public static final String ATTR_KEY_PARAMS = "key-params";

    /**
     * The name of the 'session-params' argument.
     */
    public static final String ATTR_SESSION_PARAMS = "session-params";

    /**
     * The name of the 'tag' argument.
     */
    public static final String ATTR_TAG = "tag";

    /**
     * <code>SdpCrypto</code> default constructor; use in DefaultXmlElementProvider, and newInstance() etc.
     */
    public SdpCrypto() {
        super(getBuilder());
    }

    /**
     * Creates a new <code>SdpCrypto</code> element; required by DefaultXmlElementProvider().
     *
     * @param builder Builder instance
     */
    public SdpCrypto(Builder builder) {
        super(builder);
    }

    /**
     * Returns the value of the <code>crypto-suite</code> attribute.
     *
     * @return a <code>String</code> that describes the encryption and authentication algorithms.
     */
    public String getCryptoSuite() {
        return getAttributeValue(ATTR_CRYPTO_SUITE);
    }

    /**
     * Returns the value of the <code>key-params</code> attribute.
     *
     * @return a <code>String</code> that provides one or more sets of keying material for the crypto-suite in question.
     */
    public String getKeyParams() {
        return getAttributeValue(ATTR_KEY_PARAMS);
    }

    /**
     * Returns the value of the <code>session-params</code> attribute.
     *
     * @return a <code>String</code> that provides transport-specific parameters for SRTP negotiation.
     */
    public String getSessionParams() {
        return getAttributeValue(ATTR_SESSION_PARAMS);
    }

    /**
     * Returns the value of the <code>tag</code> attribute.
     *
     * @return a <code>String</code> containing a decimal number used as an identifier for a particular crypto element.
     */
    public String getTag() {
        return getAttributeValue(ATTR_TAG);
    }

    /**
     * Returns if the current crypto suite equals the one given in parameter.
     *
     * @param cryptoSuite a <code>String</code> that describes the encryption and authentication algorithms.
     * @return True if the current crypto suite equals the one given in parameter. False, otherwise.
     */
    public boolean equalsCryptoSuite(String cryptoSuite) {
        return equalsStrings(getCryptoSuite(), cryptoSuite);
    }

    /**
     * Returns if the current key params equals the one given in parameter.
     *
     * @param keyParams that provides one or more sets of keying material for the crypto-suite in question.
     * @return True if the current key params equals the one given in parameter. False, otherwise.
     */
    public boolean equalsKeyParams(String keyParams) {
        return equalsStrings(getKeyParams(), keyParams);
    }

    /**
     * Returns if the current session params equals the one given in parameter.
     *
     * @param sessionParams a <code>String</code> that provides transport-specific parameters for SRTP negotiation.
     * @return True if the current session params equals the one given in parameter. False, otherwise.
     */
    public boolean equalsSessionParams(String sessionParams) {
        return equalsStrings(getSessionParams(), sessionParams);
    }

    /**
     * Returns if the current tag equals the one given in parameter.
     *
     * @param tag a <code>String</code> containing a decimal number used as an identifier for a particular crypto element.
     * @return True if the current tag equals the one given in parameter. False, otherwise.
     */
    public boolean equalsTag(String tag) {
        return equalsStrings(getTag(), tag);
    }

    /**
     * Returns if the first String equals the second one.
     *
     * @param string1 A String to be compared with the second one.
     * @param string2 A String to be compared with the first one.
     * @return True if both strings are null, or if they represent the same sequence of characters. False, otherwise.
     */
    private static boolean equalsStrings(String string1, String string2) {
        return ((string1 == null) && (string2 == null))
                || ((string1 != null) && string1.equals(string2)
        );
    }

    /**
     * Returns if the current CryptoExtensionElement equals the one given in parameter.
     *
     * @param obj an object which might be an instance of CryptoExtensionElement.
     * @return True if the object in parameter is a CryptoPAcketExtension with all fields
     * (crypto-suite, key-params, session-params and tag) corresponding to the current one. False, otherwise.
     */
    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SdpCrypto) {
            SdpCrypto crypto = (SdpCrypto) obj;

            return crypto.equalsCryptoSuite(getCryptoSuite())
                    && crypto.equalsKeyParams(getKeyParams())
                    && crypto.equalsSessionParams(getSessionParams())
                    && crypto.equalsTag(getTag());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                getCryptoSuite(),
                getKeyParams(),
                getSessionParams(),
                getTag());
    }

    public static Builder getBuilder() {
        return new Builder(ELEMENT, NAMESPACE);
    }

    /**
     * Builder for SdpCrypto. Use {@link AbstractXmlElement.Builder#Builder(String, String)}
     * to obtain a new instance and {@link #build} to build the SdpCrypto.
     */
    public static final class Builder extends AbstractXmlElement.Builder<Builder, SdpCrypto> {
        protected Builder(String element, String namespace) {
            super(element, namespace);
        }

        /**
         * Creates a new {@link SdpCrypto} instance with the proper element name
         * and namespace and initialises it with the parameters contained by the cryptoAttribute.
         *
         * @param tag a <code>String</code> containing a decimal number used as an
         * identifier for a particular crypto element.
         * @param cryptoSuite a <code>String</code> that describes the encryption and authentication algorithms.
         * @param keyParams a <code>String</code> that provides one or more sets of keying material for the crypto-suite in question.
         * @param sessionParams a <code>String</code> that provides transport-specific parameters for SRTP negotiation.
         * @return builder instance
         */
        public Builder setCrypto(int tag, String cryptoSuite, String keyParams, String sessionParams) {
            // Encode the tag element.
            setTag(Integer.toString(tag));
            // Encode the crypto-suite element.
            setCryptoSuite(cryptoSuite);
            // Encode the key-params element.
            setKeyParams(keyParams);
            // Encode the session-params element (optional).
            if (sessionParams != null) {
                setSessionParams(sessionParams);
            }
            return this;
        }

        /**
         * Sets the value of the <code>crypto-suite</code> attribute: an identifier that describes the
         * encryption and authentication algorithms.
         *
         * @param cryptoSuite a <code>String</code> that describes the encryption and authentication algorithms.
         * @return builder instance
         */
        public Builder setCryptoSuite(String cryptoSuite) {
            addAttribute(ATTR_CRYPTO_SUITE, cryptoSuite);
            return this;
        }

        /**
         * Sets the value of the <code>tag</code> attribute: a decimal number used as an identifier for a particular crypto element.
         *
         * @param tag a <code>String</code> containing a decimal number used as an identifier for a particular crypto element.
         * @return builder instance
         */
        public Builder setTag(String tag) {
            addAttribute(ATTR_TAG, tag);
            return this;
        }

        /**
         * Sets the value of the <code>key-params</code> attribute that provides one or more sets of keying
         * material for the crypto-suite in question).
         *
         * @param keyParams a <code>String</code> that provides one or more sets of keying material for the crypto-suite in question.
         * @return builder instance
         */
        public Builder setKeyParams(String keyParams) {
            addAttribute(ATTR_KEY_PARAMS, keyParams);
            return this;
        }

        /**
         * Sets the value of the <code>session-params</code> attribute that provides transport-specific parameters for SRTP negotiation.
         *
         * @param sessionParams a <code>String</code> that provides transport-specific parameters for SRTP negotiation.
         * @return builder instance
         */
        public Builder setSessionParams(String sessionParams) {
            addAttribute(ATTR_SESSION_PARAMS, sessionParams);
            return this;
        }

        @Override
        public SdpCrypto build() {
            return new SdpCrypto(this);
        }

        @Override
        public Builder getThis() {
            return this;
        }
    }
}
