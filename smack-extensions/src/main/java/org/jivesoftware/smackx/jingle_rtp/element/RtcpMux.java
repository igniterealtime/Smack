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
 * A 'rtcp-mux' extension: Multiplexing RTP Data and Control Packets on a Single Port.
 *
 * XEP-0167: Jingle RTP Sessions 1.2.1 (2020-09-29)
 * @see <a href="https://xmpp.org/extensions/xep-0167.html#format">XEP-0167 § 4. Application Format</a>
 * The <code>description</code> element MAY contain a <code>rtcp-mux</code> element that specifies the ability to multiplex
 * RTP Data and Control Packets on a single port as described in RFC 5761 [13].
 *
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc5761#section-5">RFC5761 § 5.  Multiplexing RTP and RTCP on a Single Port</a>
 *
 * @author Boris Grozev
 * @author Eng Chong Meng
 */
public class RtcpMux extends AbstractXmlElement {
    /**
     * The name of the "encryption" element.
     */
    public static final String ELEMENT = "rtcp-mux";

    /**
     * <code>RtcpMux</code> default constructor use by DefaultXmlElementProvider newInstance() etc.
     * Default to use RtpDescription.NAMESPACE, and to be modified in:
     *
     * @see #getBuilder(String)
     */
    public RtcpMux() {
        super(builder(RtpDescription.NAMESPACE));
    }

    /**
     * Creates a new <code>RtcpMux</code>; required by DefaultXmlElementProvider().
     *
     * @param builder Builder instance
     */
    public RtcpMux(Builder builder) {
        super(builder);
    }

    public static Builder builder(String nameSpace) {
        return new Builder(ELEMENT, nameSpace);
    }

    /**
     * Builder for RtcpMux. Use {@link AbstractXmlElement.Builder#Builder(String, String)}
     * to obtain a new instance and {@link #build} to build the RtcpMux.
     */
    public static final class Builder extends AbstractXmlElement.Builder<Builder, RtcpMux> {
        protected Builder(String element, String namespace) {
            super(element, namespace);
        }


        @Override
        public RtcpMux build() {
            return new RtcpMux(this);
        }

        @Override
        public Builder getThis() {
            return this;
        }
    }
}
