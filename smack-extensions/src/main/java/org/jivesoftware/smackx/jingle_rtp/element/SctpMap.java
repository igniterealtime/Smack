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
 * SctpMap extension in transport packet extension.
 * @see <a href="https://xmpp.org/extensions/xep-0343.html">XEP-0343: Signaling WebRTC datachannels in Jingle 0.3.1 (2020-03-20)</a>
 * @see <a href="https://datatracker.ietf.org/doc/html/rfc3758">RFC3758: Stream Control Transmission Protocol (SCTP) Partial Reliability Extension May 2004</a>
 *
 * @author Eng Chong Meng
 */
public class SctpMap extends AbstractXmlElement {
    public static final String ELEMENT = "sctpmap";
    public static final String NAMESPACE = "urn:xmpp:jingle:transports:dtls-sctp:1";
    public static final QName QNAME = new QName(NAMESPACE, ELEMENT);

    /**
     * Port number of "sctpmap" element.
     */
    public static final String ATTR_NUMBER = "number";

    /**
     * Protocol name of "sctpmap" element.
     */
    public static final String ATTR_PROTOCOL = "protocol";

    /**
     * Protocol enumeration of <code>SctpMap</code>. Currently it only contains WEBRTC_CHANNEL.
     */
    public enum Protocol {
        webrtc_datachannel;

        public static Protocol fromString(String string) {
            return Protocol.valueOf(string.replace('-', '_'));
        }

        @Override
        public String toString() {
            return name().replace('_', '-');
        }
    }

    /**
     * Creates a new <code>RtpExtmap</code>; required by DefaultXmlElementProvider().
     *
     * @param builder Builder instance
     */
    public SctpMap(Builder builder) {
        super(builder);
    }

    public int getPort() {
        return getAttributeAsInt(ATTR_NUMBER);
    }

    public String getProtocol() {
        return getAttributeValue(ATTR_PROTOCOL);
    }

    public static Builder getBuilder() {
        return new Builder(ELEMENT, NAMESPACE);
    }

    /**
     * Builder for SctpMap. Use {@link AbstractXmlElement.Builder#Builder(String, String)}
     * to obtain a new instance and {@link #build} to build the SctpMap.
     */
    public static final class Builder extends AbstractXmlElement.Builder<Builder, SctpMap> {
        protected Builder(String element, String namespace) {
            super(element, namespace);
        }

        public Builder setPort(int port) {
            addAttribute(ATTR_NUMBER, port);
            return this;
        }

        public Builder setProtocol(String protocol) {
            addAttribute(ATTR_PROTOCOL, protocol);
            return this;
        }

        public Builder setProtocol(Protocol protocol) {
            addAttribute(ATTR_PROTOCOL, protocol.toString());
            return this;
        }

        @Override
        public SctpMap build() {
            return new SctpMap(this);
        }

        @Override
        protected Builder getThis() {
            return this;
        }
    }
}
