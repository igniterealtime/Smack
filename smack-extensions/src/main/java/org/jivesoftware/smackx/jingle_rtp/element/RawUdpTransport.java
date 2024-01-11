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
 * An {@link AbstractXmlElement} implementation for Raw UDP transport elements.
 *
 * @see <a href="https://xmpp.org/extensions/xep-0177.html">XEP-0177: Jingle Raw UDP Transport Method 1.1.1 (2020-12-10)</a>
 *
 * @author Emil Ivov
 * @author Lyubomir Marinov
 * @author Eng Chong Meng
 */
public class RawUdpTransport extends IceUdpTransport {
    /**
     * The nameSpace of the "transport" element.
     */
    public static final String NAMESPACE = "urn:xmpp:jingle:transports:raw-udp:1";

    public RawUdpTransport() {
        super(getBuilder());
    }

    /**
     * Creates a new {@link RawUdpTransport}.
     *
     * @param builder Builder instance
     */
    public RawUdpTransport(Builder builder) {
        super(builder);
    }

    public static Builder getBuilder() {
        return new Builder(ELEMENT, NAMESPACE);
    }

    /**
     * Builder for RawUdpTransport. Use {@link AbstractXmlElement.Builder#Builder(String, String)}
     * to obtain a new instance and {@link #build} to build the RawUdpTransport.
     */
    public static final class Builder extends IceUdpTransport.Builder {
        protected Builder(String element, String namespace) {
            super(element, namespace);
        }

        @Override
        public RawUdpTransport build() {
            return new RawUdpTransport(this);
        }

        @Override
        protected Builder getThis() {
            return this;
        }
    }
}
