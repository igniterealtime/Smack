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
 * An implementation of the "zrtp-hash" attribute as described in the currently deferred.
 *
 * @author Emil Ivov
 * @author Eng Chong Meng
 * @see <a href="https://xmpp.org/extensions/xep-0262.html">XEP-0262: Use of ZRTP in Jingle RTP Sessions 1.0 (2011-06-15)</a>
 */
public class ZrtpHash extends AbstractXmlElement {
    /**
     * The name of the "zrtp-hash" element.
     */
    public static final String ELEMENT = "zrtp-hash";

    /**
     * The namespace for the "zrtp-hash" element.
     */
    public static final String NAMESPACE = "urn:xmpp:jingle:apps:rtp:zrtp:1";

    /**
     * The name of the <code>version</code> attribute.
     */
    public static final String ATTR_VERSION = "version";

    /**
     * Creates a {@link ZrtpHash} instance for the specified <code>namespace</code> and <code>elementName</code>.
     */
    public ZrtpHash() {
        super(getBuilder());
    }

    /**
     * Creates a new <code>ZrtpHash</code>; required by DefaultXmlElementProvider().
     *
     * @param builder Builder instance
     */
    public ZrtpHash(Builder builder) {
        super(builder);
    }

    /**
     * Returns the ZRTP version used by the implementation that created the hash.
     *
     * @return the ZRTP version used by the implementation that created the hash.
     */
    public String getVersion() {
        return getAttributeValue(ATTR_VERSION);
    }

    /**
     * Returns the value of the ZRTP hash this element is carrying.
     *
     * @return the value of the ZRTP hash this element is carrying.
     */
    public String getHashValue() {
        return getText();
    }

    public static Builder getBuilder() {
        return new Builder(ELEMENT, NAMESPACE);
    }

    /**
     * Builder for ZrtpHash. Use {@link AbstractXmlElement.Builder#Builder(String, String)}
     * to obtain a new instance and {@link #build} to build the ZrtpHash.
     */
    public static final class Builder extends AbstractXmlElement.Builder<Builder, ZrtpHash> {
        protected Builder(String element, String namespace) {
            super(element, namespace);
        }

        /**
         * Sets the value of the ZRTP hash this element will be carrying.
         *
         * @param value the value of the ZRTP hash this element will be carrying.
         * @return builder instance
         */
        public Builder setHashValue(String value) {
            setText(value);
            return this;
        }

        /**
         * Sets the ZRTP version used by the implementation that created the hash.
         *
         * @param version the ZRTP version used by the implementation that created the hash.
         * @return builder instance
         */
        public Builder setVersion(String version) {
            addAttribute(ATTR_VERSION, version);
            return this;
        }

        @Override
        public ZrtpHash build() {
            return new ZrtpHash(this);
        }

        @Override
        public Builder getThis() {
            return this;
        }
    }
}
