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

/**
 * Implements <code>ExtensionElement</code> for the <code>fingerprint</code> element.
 *
 * @author Lyubomir Marinov
 * @author Eng Chong Meng
 * @see <a href="https://xmpp.org/extensions/xep-0320.html">XEP-0320: Use of DTLS-SRTP in Jingle Sessions 1.0.0 (2020-05-26)</a>
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc4145">TCP-Based Media Transport in the Session Description Protocol (SDP)</a>
 */
public class SrtpFingerprint extends AbstractXmlElement {
    /**
     * The XML name of the <code>fingerprint</code> element defined by: XEP-0320: Use of DTLS-SRTP in Jingle Sessions.
     */
    public static final String ELEMENT = "fingerprint";

    /**
     * The XML namespace of the <code>fingerprint</code> element defined by XEP-0320: Use of DTLS-SRTP in Jingle Sessions.
     */
    public static final String NAMESPACE = "urn:xmpp:jingle:apps:dtls:0";

    /**
     * The XML name of the <code>fingerprint</code> element's attribute which specifies the hash
     * function utilized to calculate the fingerprint.
     */
    private static final String ATTR_HASH = "hash";

    /**
     * The XML name of the <code>fingerprint</code> element's attribute which specifies setup role that
     * indicates which of the end points should initiate the connection establishment. Valid values:<br/>
     * <li>'active'  : The endpoint will initiate an outgoing connection.</li>
     * <li>'passive' : The endpoint will accept an incoming connection.</li>
     * <li>'actpass' : The endpoint is willing to accept an incoming connection or to initiate an outgoing connection.</li>
     * <li>'holdconn': The endpoint does not want the connection to be established for the time being.</li>
     *
     * see https://datatracker.ietf.org/doc/html/rfc4145#section-4 (4. Setup Attribute )
     */
    private static final String ATTR_SETUP = "setup";

    public SrtpFingerprint() {
        super(getBuilder());
    }

    /**
     * Creates a new <code>SrtpFingerprint</code>; required by DefaultXmlElementProvider().
     *
     * @param builder Builder instance
     */
    public SrtpFingerprint(Builder builder) {
        super(builder);
    }

    /**
     * Gets the fingerprint carried/represented by this instance.
     *
     * @return the fingerprint carried/represented by this instance
     */
    public String getFingerprint() {
        return getText();
    }

    /**
     * Gets the hash function utilized to calculate the fingerprint carried/represented by this instance.
     *
     * @return the hash function utilized to calculate the fingerprint carried/represented by this instance
     */
    public String getHash() {
        return getAttributeValue(ATTR_HASH);
    }

    /**
     * Gets the setup attribute value.
     *
     * @return value of 'setup' attribute. See {@link #ATTR_SETUP} for more info.
     */
    public String getSetup() {
        return getAttributeValue(ATTR_SETUP);
    }

    public static Builder getBuilder() {
        return new Builder(ELEMENT, NAMESPACE);
    }

    public static Builder from(SrtpFingerprint content) {
        Builder builder = new Builder(ELEMENT, NAMESPACE);
        builder.addAttributes(content.getAttributes());
        builder.addChildElements(content.getChildElements());
        if (content.getText() != null)
            builder.setText(content.getText());
        return builder;
    }

    /**
     * Builder for SrtpEncryption. Use {@link AbstractXmlElement.Builder#Builder(String, String)}
     * to obtain a new instance and {@link #build} to build the SrtpEncryption.
     */
    public static final class Builder extends AbstractXmlElement.Builder<Builder, SrtpFingerprint> {
        protected Builder(String element, String namespace) {
            super(element, namespace);
        }

        /**
         * Sets the fingerprint to be carried/represented by this instance.
         *
         * @param fingerprint the fingerprint to be carried/represented by this instance
         * @return builder instance
         */
        public Builder setFingerprint(String fingerprint) {
            setText(fingerprint);
            return this;
        }

        /**
         * Sets the hash function utilized to calculate the fingerprint carried/represented by this instance.
         *
         * @param hash the hash function utilized to calculate the fingerprint carried/represented by this instance
         * @return builder instance
         */
        public Builder setHash(String hash) {
            addAttribute(ATTR_HASH, hash);
            return this;
        }

        /**
         * Sets new value for 'setup' attribute.
         *
         * @param setup see {@link #ATTR_SETUP} for the list of allowed values.
         * @return builder instance
         */
        public Builder setSetup(String setup) {
            addAttribute(ATTR_SETUP, setup);
            return this;
        }

        @Override
        public SrtpFingerprint build() {
            return new SrtpFingerprint(this);
        }

        @Override
        protected Builder getThis() {
            return this;
        }
    }
}
