/**
 *
 * Copyright 2017-2022 Jive Software
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
package org.jivesoftware.smackx.jingle;

import org.jivesoftware.smackx.AbstractXmlElement;

import javax.xml.namespace.QName;

/**
 * @author Boris Grozev
 * @author Eng Chong Meng
 */
public class WebSocketExtension extends AbstractXmlElement {
    /**
     * The name of the "web-socket" element.
     */
    public static final String ELEMENT = "web-socket";

    public static final String NAMESPACE = "http://jitsi.org/protocol/colibri";

    public static final QName QNAME = new QName(NAMESPACE, ELEMENT);

    /**
     * The name of the "url" attribute.
     */
    public static final String ATTR_URL = "url";

    /**
     * Creates a new <code>WebSocketExtension</code>; required by DefaultXmlElementProvider().
     * @param build Builder instance
     */
    public WebSocketExtension(Builder build) {
        super(build);
    }

    public static Builder builder() {
        return new Builder(ELEMENT, NAMESPACE);
    }

    /**
     * @return the URL.
     */
    public String getUrl() {
        return super.getAttributeValue(ATTR_URL);
    }

    /**
     * Builder for WebSocketExtension. Use {@link AbstractXmlElement.Builder#Builder(String, String)}
     * to obtain a new instance and {@link #build} to build the WebSocketExtension.
     */
    public static final class Builder extends AbstractXmlElement.Builder<Builder, WebSocketExtension> {
        protected Builder(String element, String namespace) {
            super(element, namespace);
        }

        /**
         * Sets the URL.
         * @param url URL value
         * @return builder instance
         */
        public Builder setUrl(String url) {
            super.addAttribute(ATTR_URL, url);
            return this;
        }

        @Override
        public WebSocketExtension build() {
            return new WebSocketExtension(this);
        }

        @Override
        protected Builder getThis() {
            return this;
        }
    }
}
