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
 * SDP extmap-allow-mixed extension.
 * Jingle's Discovery Info URN for "XEP-0294: Jingle RTP Header Extensions Negotiation" support.
 * @see <a href="https://xmpp.org/extensions/xep-0294.html">XEP-0294: Jingle RTP Header Extensions Negotiation 1.1.1 (2021-10-23)</a>
 *
 * Note: This specification defines a new element, <code>extmap-allow-mixed</code>, that can be inserted in the <code>description</code>
 * element of a XEP-0167 RTP session. The element has no attributes.
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc8285#section-6">RFC8285 § 6. SDP Signaling for Support of Mixed One-Byte and Two-Byte Header
 * Extensions</a>
 *
 *  @author Eng Chong Meng
 */
public class RtpExtmap extends AbstractXmlElement {
    public static final String ELEMENT = "extmap-allow-mixed";
    public static final String NAMESPACE = "urn:xmpp:jingle:apps:rtp:rtp-hdrext:0";

    public RtpExtmap() {
        super(getBuilder());
    }

    /**
     * Creates a new <code>RtpExtmap</code>; required by DefaultXmlElementProvider().
     *
     * @param builder Builder instance
     */
    public RtpExtmap(Builder builder) {
        super(builder);
    }

    public static Builder getBuilder() {
        return new Builder(ELEMENT, NAMESPACE);
    }

    /**
     * Builder for RtpExtmap. Use {@link AbstractXmlElement.Builder#Builder(String, String)}
     * to obtain a new instance and {@link #build} to build the RtpExtmap.
     */
    public static final class Builder extends AbstractXmlElement.Builder<Builder, RtpExtmap> {
        protected Builder(String element, String namespace) {
            super(element, namespace);
        }

        @Override
        public RtpExtmap build() {
            return new RtpExtmap(this);
        }

        @Override
        protected Builder getThis() {
            return this;
        }
    }
}
