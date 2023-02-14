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

/**
 * Represents the <code>payload-type</code> elements described.
 * XEP-0167: Jingle RTP Sessions 1.2.1 (2020-09-29)
 *
 * @author Emil Ivov
 * @author Eng Chong Meng
 * @see <a href="https://xmpp.org/extensions/xep-0167.html#format">XEP-0167 § 4. Application Format</a>
 */
public class PayloadType extends AbstractXmlElement {
    /**
     * The name of the "payload-type" element.
     */
    public static final String ELEMENT = "payload-type";

    public static final QName QNAME = new QName(RtpDescription.NAMESPACE, ELEMENT);

    /**
     * The name of the <code>channels</code> <code>payload-type</code> argument.
     * If omitted, it MUST be assumed to contain one channel.
     */
    public static final String ATTR_CHANNELS = "channels";

    /**
     * The name of the <code>clockrate</code> SDP argument. The sampling frequency in Hertz.
     */
    public static final String ATTR_CLOCKRATE = "clockrate";

    /**
     * The name of the payload <code>id</code> SDP argument. The payload identifier.
     */
    public static final String ATTR_ID = "id";

    /**
     * The name of the <code>maxptime</code> SDP argument. Maximum packet time as specified in RFC 4566.
     */
    public static final String ATTR_MAXPTIME = "maxptime";

    /**
     * The name of the <code>name</code> SDP argument. The appropriate subtype of the MIME type.
     */
    public static final String ATTR_NAME = "name";

    /**
     * The name of the <code>ptime</code> SDP argument. Packet time as specified in RFC 4566.
     */
    public static final String ATTR_PTIME = "ptime";

    /**
     * Creates a new {@link PayloadType} instance.
     *
     * The namespace of the "payload-type" element;
     * PayloadType is currently a child element of standard RtpDescription;
     * // @see org.jivesoftware.smackx.colibri.ColibriConferenceIQ
     */
    public PayloadType() {
        super(builder(RtpDescription.NAMESPACE));
    }

    public PayloadType(Builder builder) {
        super(builder);
    }

    /**
     * Returns the number of channels in this payload type.
     *
     * @return the number of channels in this payload type.
     */
    public int getChannels() {
        /*
         * XEP-0167: Jingle RTP Sessions says: if omitted, it MUST be assumed to contain one channel.
         */
        int channels = getAttributeAsInt(ATTR_CHANNELS);
        return (channels > 0) ? channels : 1;
    }

    /**
     * Returns the sampling frequency in Hertz used by this encoding.
     *
     * @return the sampling frequency in Hertz used by this encoding.
     */
    public int getClockrate() {
        return getAttributeAsInt(ATTR_CLOCKRATE);
    }

    /**
     * Returns the payload identifier for this encoding (as specified by RFC 3551 or a dynamic one).
     *
     * @return the payload identifier for this encoding (as specified by RFC 3551 or a dynamic one).
     */
    public int getID() {
        return getAttributeAsInt(ATTR_ID);
    }

    /**
     * Returns maximum packet time as specified in RFC 4566.
     *
     * @return maximum packet time as specified in RFC 4566
     */
    public int getMaxptime() {
        return getAttributeAsInt(ATTR_MAXPTIME);
    }

    /**
     * Returns packet time as specified in RFC 4566.
     *
     * @return packet time as specified in RFC 4566
     */
    public int getPtime() {
        return getAttributeAsInt(ATTR_PTIME);
    }

    /**
     * Returns the name of the encoding, or as per the XEP: the appropriate subtype of the MIME
     * type. Setting this field is RECOMMENDED for static payload types, REQUIRED for dynamic payload types.
     *
     * @return the name of the encoding, or as per the XEP: the appropriate subtype of the MIME
     * type. Setting this field is RECOMMENDED for static payload types, REQUIRED for dynamic payload types.
     */
    public String getName() {
        return getAttributeValue(ATTR_NAME);
    }

    // ===============================

    /**
     * Sets the number of channels in this payload type. If omitted, it will be assumed to contain one channel.
     *
     * @param channels the number of channels in this payload type.
     */
    public void setChannels(int channels) {
        setAttribute(ATTR_CHANNELS, channels);
    }

    public static Builder builder(String nameSpace) {
        return new Builder(ELEMENT, nameSpace);
    }

    /**
     * Builder for JingleContentTransport. Use {@link AbstractXmlElement.Builder#Builder(String, String)}
     * to obtain a new instance and {@link #build} to build the JingleContentTransport.
     */
    public static class Builder extends AbstractXmlElement.Builder<Builder, PayloadType> {
        protected Builder(String element, String namespace) {
            super(element, namespace);
        }

        /**
         * Sets the number of channels in this payload type. If omitted, it will be assumed to contain one channel.
         *
         * @param channels the number of channels in this payload type.
         * @return builder instance
         */
        public Builder setChannels(int channels) {
            addAttribute(ATTR_CHANNELS, channels);
            return this;
        }

        /**
         * Specifies the sampling frequency in Hertz used by this encoding.
         *
         * @param clockrate the sampling frequency in Hertz used by this encoding.
         * @return builder instance
         */
        public Builder setClockrate(int clockrate) {
            addAttribute(ATTR_CLOCKRATE, clockrate);
            return this;
        }

        /**
         * Specifies the payload identifier for this encoding.
         *
         * @param id the payload type id
         * @return builder instance
         */
        public Builder setId(int id) {
            addAttribute(ATTR_ID, id);
            return this;
        }

        /**
         * Sets the maximum packet time as specified in RFC 4566.
         *
         * @param maxptime the maximum packet time as specified in RFC 4566
         * @return builder instance
         */
        public Builder setMaxptime(int maxptime) {
            addAttribute(ATTR_MAXPTIME, maxptime);
            return this;
        }

        /**
         * Sets the packet time as specified in RFC 4566.
         *
         * @param ptime the packet time as specified in RFC 4566
         * @return builder instance
         */
        public Builder setPtime(int ptime) {
            addAttribute(ATTR_PTIME, ptime);
            return this;
        }

        /**
         * Sets the name of the encoding, or as per the XEP: the appropriate subtype of the MIME type.
         * Setting this field is RECOMMENDED for static payload types, REQUIRED for dynamic payload types.
         *
         * @param name the name of this encoding.
         * @return builder instance
         */
        public Builder setName(String name) {
            addAttribute(ATTR_NAME, name);
            return this;
        }

        /**
         * Adds an SDP parameter to the list that we already have registered for this payload type.
         *
         * @param parameter an SDP parameter for this encoding.
         * @return builder instance
         */
        public Builder addParameter(ParameterElement parameter) {
            // parameters are the only extensions we can have so let's use super's list.
            addChildElement(parameter);
            return this;
        }

        /**
         * Adds an RTCP feedback type to the list that we already have registered for this payload type.
         *
         * @param rtcpFbPacketExtension RTCP feedback type for this encoding.
         * @return builder instance
         */
        public Builder addRtcpFeedbackType(RtcpFb rtcpFbPacketExtension) {
            addChildElement(rtcpFbPacketExtension);
            return this;
        }

        @Override
        public PayloadType build() {
            return new PayloadType(this);
        }

        @Override
        protected PayloadType.Builder getThis() {
            return this;
        }
    }
}
