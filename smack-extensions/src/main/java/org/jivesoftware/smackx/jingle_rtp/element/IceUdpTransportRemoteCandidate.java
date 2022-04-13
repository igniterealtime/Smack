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

/**
 * A representation of the <code>remote-candidate</code> ICE transport element.
 * XEP-0371: Jingle ICE Transport Method 0.3.1 (2021-03-04)
 * @see <a href="https://xmpp.org/extensions/xep-0371.html#protocol-acceptance">XEP-0371 § 5.8 Acceptance of Successful Candidate</a>
 *
 * cmeng: Seems not use in aTalk
 *
 * @author Emil Ivov
 * @author Eng Chong Meng
 */
public class IceUdpTransportRemoteCandidate extends IceUdpTransportCandidate {
    /**
     * The name of the "remote-candidate" element.
     */
    public static final String ELEMENT = "remote-candidate";

    public IceUdpTransportRemoteCandidate() {
        super(getBuilder());
    }

    /**
     * Creates a new IceUdpTransportRemoteCandidate.
     *
     * @param builder Builder instance
     */
    public IceUdpTransportRemoteCandidate(Builder builder) {
        super(builder);
    }

    public static Builder getBuilder() {
        return new Builder(ELEMENT, NAMESPACE);
    }

    /**
     * Builder for IceUdpTransportRemoteCandidate. Use {@link IceUdpTransportRemoteCandidate#getBuilder()} to
     * obtain a new instance and {@link #build} to build the IceUdpTransportRemoteCandidate.
     */
    public static final class Builder extends IceUdpTransportCandidate.Builder {
        protected Builder(String element, String namespace) {
            super(element, namespace);
        }

        @Override
        public IceUdpTransportRemoteCandidate build() {
            return new IceUdpTransportRemoteCandidate(this);
        }

        @Override
        protected Builder getThis() {
            return this;
        }
    }
}
