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

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smackx.jingle.element.JingleContentDescription;
import org.jivesoftware.smackx.jingle_rtp.AbstractXmlElement;

/**
 * Represents the content <code>description</code> elements described below.
 * @see <a href="https://xmpp.org/extensions/xep-0167.html">XEP-0167: Jingle RTP Sessions 1.2.0 (2020-04-22)</a>
 *
 * Multiplexing RTP Data and Control Packets on a Single Port (April 2010),
 * https://tools.ietf.org/html/rfc5761 (5.1.3. Interactions with ICE) seem to propose <code>rtpc-mux</code> to be included in transport
 *
 * @author Emil Ivov
 * @author Eng Chong Meng
 */
public class RtpDescription extends JingleContentDescription {
    /**
     * The name of the "description" element.
     */
    public static final String ELEMENT = "description";

    /**
     * The name space for RTP description elements.
     */
    public static final String NAMESPACE = "urn:xmpp:jingle:apps:rtp:1";

    /**
     * The name of the <code>media</code> description argument.
     */
    public static final String ATTR_MEDIA = "media";

    /**
     * The name of the <code>ssrc</code> description argument.
     */
    public static final String ATTR_SSRC = "ssrc";

    public RtpDescription() {
        super(getBuilder());
    }

    /**
     * Creates a new <code>RtpDescription</code>.
     *
     * @param builder Builder instance
     */
    public RtpDescription(Builder builder) {
        super(builder);
    }

    /**
     * Returns the media type for the stream that this description element represents, such as "audio" or "video".
     *
     * @return the media type for the stream that this description element represents, such as "audio" or "video".
     */
    public String getMedia() {
        return getAttributeValue(ATTR_MEDIA);
    }

    /**
     * Returns the synchronization source ID (SSRC as per RFC 3550) that the stream represented by
     * this description element will be using.
     *
     * @return the synchronization source ID (SSRC as per RFC 3550) that the stream represented by
     * this description element will be using.
     */
    public String getSsrc() {
        return getAttributeValue(ATTR_SSRC);
    }

    /**
     * Sets the synchronization source ID (SSRC as per RFC 3550) that the stream represented by this
     * description element will be using.
     *
     * @param ssrc the SSRC ID that the RTP stream represented here will be using.
     */
    public void setSsrc(String ssrc) {
        setAttribute(ATTR_SSRC, ssrc);
    }

    public static Builder getBuilder() {
        return new Builder(ELEMENT, NAMESPACE);
    }

    /**
     * Builder for RtpDescription. Use {@link AbstractXmlElement.Builder#Builder(String, String)}
     * to obtain a new instance and {@link #build} to build the RtpDescription.
     */
    public static final class Builder extends JingleContentDescription.Builder {
        protected Builder(String element, String namespace) {
            super(element, namespace);
        }

        /**
         * Specify the media type for the stream that this description element represents, such as "audio" or "video".
         *
         * @param media the media type for the stream that this element represents such as "audio" or "video".
         * @return builder instance
         */
        public Builder setMedia(String media) {
            addAttribute(ATTR_MEDIA, media);
            return this;
        }

        /**
         * Set the synchronization source ID (SSRC as per RFC 3550) that the stream represented by this
         * description element will be using.
         *
         * @param ssrc the SSRC ID that the RTP stream represented here will be using.
         * @return builder instance
         */
        public Builder setSsrc(String ssrc) {
            addAttribute(ATTR_SSRC, ssrc);
            return this;
        }

        /**
         * Set the optional encryption element that contains encryption parameters for this session.
         *
         * @param srtpEncryption the encryption {@link ExtensionElement} we'd like to add to this packet.
         * @return builder instance
         */
        public Builder addEncryption(SrtpEncryption srtpEncryption) {
            addChildElement(srtpEncryption);
            return this;
        }

        /**
         * Set the optional rtcpmux element that contains rtcpmux parameters for this session.
         *
         * @param rtcpmux the rtcpmux {@link ExtensionElement} we'd like to add to this packet.
         * @return builder instance
         */
        public Builder addRtcpMux(RtcpMux rtcpmux) {
            addChildElement(rtcpmux);
            return this;
        }

        /**
         * Sets an optional bandwidth element that specifies the allowable or preferred bandwidth for
         * use by this application type.
         *
         * @param bandwidth the max/preferred bandwidth indication that we'd like to add to this packet.
         * @return builder instance
         */
        public Builder addBandwidth(SdpBandwidth bandwidth) {
            addChildElement(bandwidth);
            return this;
        }

        /**
         * Adds an optional <code>rtpHeader</code> element that allows negotiation RTP extension headers as per RFC 5282.
         *
         * @param rtpHeader an optional <code>rtpHeader</code> element that allows negotiation RTP extension headers as per RFC 5282.
         * @return builder instance
         */
        public Builder addRtpHeader(RtpHeader rtpHeader) {
            addChildElement(rtpHeader);
            return this;
        }

        /**
         * Adds a new payload type to this description element.
         *
         * @param payloadType the new payload to add.
         * @return builder instance
         */
        public Builder addPayloadType(PayloadType payloadType) {
            addChildElement(payloadType);
            return this;
        }

        @Override
        public RtpDescription build() {
            return new RtpDescription(this);
        }

        @Override
        protected Builder getThis() {
            return this;
        }
    }
}
