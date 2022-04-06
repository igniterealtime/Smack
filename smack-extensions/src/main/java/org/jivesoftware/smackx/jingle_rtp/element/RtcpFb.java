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

import javax.xml.namespace.QName;

/**
 * Packet extension that holds RTCP feedback types of the {@link PayloadType}.
 * @see <a href="https://xmpp.org/extensions/xep-0293.html">XEP-0293: Jingle RTP Feedback Negotiation 1.0.1 (2018-11-03)</a>
 *
 * @author Pawel Domas
 * @author Eng Chong Meng
 */
public class RtcpFb extends AbstractXmlElement {
    /**
     * The name of the RTCP feedback element.
     */
    public static final String ELEMENT = "rtcp-fb";

    /**
     * The name space for RTP feedback elements.
     */
    public static final String NAMESPACE = "urn:xmpp:jingle:apps:rtp:rtcp-fb:0";

    public static final QName QNAME = new QName(NAMESPACE, ELEMENT);

    /**
     * The name the attribute that holds the feedback type. The type of feedback.
     */
    public static final String ATTR_TYPE = "type";

    /**
     * The name the attribute that holds the feedback subtype. The subtype optional (depends on the type).
     */
    public static final String ATTR_SUBTYPE = "subtype";

    /**
     * <code>RtcpFb</code> default constructor; use in DefaultXmlElementProvider, and newInstance() etc.
     */
    public RtcpFb() {
        super(getBuilder());
    }

    /**
     * Initializes a new <code>RtcpFb</code> instance.; required by DefaultXmlElementProvider().
     *
     * @param builder Builder instance
     */
    public RtcpFb(Builder builder) {
        super(builder);
    }

    /**
     * Returns RTCP feedback type attribute value if already set or <code>null</code> otherwise.
     *
     * @return RTCP feedback type attribute if already set or <code>null</code> otherwise.
     */
    public String getFeedbackType() {
        return getAttributeValue(ATTR_TYPE);
    }

    /**
     * Returns RTCP feedback subtype attribute value if already set or <code>null</code> otherwise.
     *
     * @return RTCP feedback subtype attribute if already set or <code>null</code> otherwise.
     */
    public String getFeedbackSubtype() {
        return getAttributeValue(ATTR_SUBTYPE);
    }

    public static Builder getBuilder() {
        return new Builder(ELEMENT, NAMESPACE);
    }

    /**
     * Builder for RtcpFb. Use {@link AbstractXmlElement.Builder#Builder(String, String)}
     * to obtain a new instance and {@link #build} to build the RtcpFb.
     */
    public static final class Builder extends AbstractXmlElement.Builder<Builder, RtcpFb> {
        protected Builder(String element, String namespace) {
            super(element, namespace);
        }

        /**
         * Adds a specific parameter (as defined by Source-Specific Media Attributes in Jingle) to this source.
         *
         * @param parameter the <code>ParameterElement</code> to add to this source
         * @return builder instance
         */
        public Builder addParameter(ParameterElement parameter) {
            addChildElement(parameter);
            return this;
        }

        /**
         * Sets RTCP feedback type attribute.
         *
         * @param feedbackType the RTCP feedback type to set.
         * @return builder instance
         */
        public Builder setFeedbackType(String feedbackType) {
            addAttribute(ATTR_TYPE, feedbackType);
            return this;
        }

        /**
         * Sets RTCP feedback subtype attribute.
         *
         * @param feedbackSubType the RTCP feedback subtype to set.
         * @return builder instance
         */
        public Builder setFeedbackSubtype(String feedbackSubType) {
            addAttribute(ATTR_SUBTYPE, feedbackSubType);
            return this;
        }

        @Override
        public RtcpFb build() {
            return new RtcpFb(this);
        }

        @Override
        public Builder getThis() {
            return this;
        }
    }
}
