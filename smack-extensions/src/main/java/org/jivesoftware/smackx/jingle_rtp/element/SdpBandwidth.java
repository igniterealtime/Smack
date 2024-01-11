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
 * A representation of the <code>bandwidth</code> element used in RTP <code>description</code> elements.
 * XEP-0167: Jingle RTP Sessions 1.2.1 (2020-09-29)
 * @see <a href="https://xmpp.org/extensions/xep-0167.html#format">XEP-0167 § 4. Application Format</a>
 *
 * For RTP sessions, often the <code>bandwidth</code> element will specify the "session bandwidth" as described in Section 6.2
 * of RFC 3550, measured in kilobits per second as described in Section 5.2 of RFC 4566.
 *
 * @author Emil Ivov
 * @author Eng Chong Meng
 */
public class SdpBandwidth extends AbstractXmlElement {
    /**
     * The name of the "bandwidth" element.
     */
    public static final String ELEMENT = "bandwidth";

    /**
     * The name of the "bandwidth" element.
     */
    public static final String NAMESPACE = RtpDescription.NAMESPACE;

    /**
     * The name of the type argument.
     */
    public static final String ATTR_TYPE = "type";

    /**
     * <code>SdpBandwidth</code> default constructor; use in DefaultXmlElementProvider, and newInstance() etc.
     */
    public SdpBandwidth() {
        super(getBuilder());
    }

    /**
     * Initializes a new <code>SdpBandwidth</code> instance.; required by DefaultXmlElementProvider().
     *
     * @param builder Builder instance
     */
    public SdpBandwidth(Builder builder) {
        super(builder);
    }

    /**
     * Returns the value of the optional <code>type</code> argument in the <code>bandwidth</code> element.
     *
     * @return a <code>String</code> value which would often be one of the <code>bwtype</code> values specified by SDP
     */
    public String getType() {
        return getAttributeValue(ATTR_TYPE);
    }

    /**
     * Returns the value of this bandwidth extension.
     *
     * @return the value of this bandwidth extension.
     */
    public String getBandwidth() {
        return getText();
    }

    public static Builder getBuilder() {
        return new Builder(ELEMENT, NAMESPACE);
    }

    /**
     * Builder for SdpBandwidth. Use {@link AbstractXmlElement.Builder#Builder(String, String)}
     * to obtain a new instance and {@link #build} to build the SdpBandwidth.
     */
    public static class Builder extends AbstractXmlElement.Builder<Builder, SdpBandwidth> {
        protected Builder(String element, String namespace) {
            super(element, namespace);
        }

        /**
         * Sets the value of the optional <code>type</code> argument in the <code>bandwidth</code> element.
         *
         * @param type a <code>String</code> value which would often be one of the <code>bwtype</code> values specified by SDP
         * @return builder instance
         */
        public Builder setType(String type) {
            addAttribute(ATTR_TYPE, type);
            return this;
        }

        /**
         * Sets the value of this bandwidth extension.
         *
         * @param bw the value of this bandwidth extension.
         * @return builder instance
         */
        public Builder setBandwidth(String bw) {
            setText(bw);
            return this;
        }

        @Override
        public SdpBandwidth build() {
            return new SdpBandwidth(this);
        }

        @Override
        public Builder getThis() {
            return this;
        }
    }
}
