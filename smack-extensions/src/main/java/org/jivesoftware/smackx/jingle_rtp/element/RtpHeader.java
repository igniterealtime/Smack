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

import java.net.URI;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smackx.jingle.element.JingleContent.Senders;
import org.jivesoftware.smackx.jingle_rtp.AbstractXmlElement;

/**
 * RTP header extension.
 *
 * Jingle's Discovery Info URN for "XEP-0294: Jingle RTP Header Extensions Negotiation" support.
 * @see <a href="https://xmpp.org/extensions/xep-0294.html">XEP-0294: Jingle RTP Header Extensions Negotiation  1.1.1 (2021-10-23)</a>
 *
 * Note: Any type of RTP Header Extension that requires extra parameters in the a=b form can embed <code>parameter</code>
 * elements to describe it. Any other form of parameter can be stored as the 'key' attribute in a parameter
 * element with an empty value.
 * @see <a href="https://xmpp.org/extensions/xep-0294.html#element">XEP-0294 § 3. New elements</a>
 *
 * @author Sebastien Vincent
 * @author Eng Chong Meng
 */
public class RtpHeader extends AbstractXmlElement {
    public static final String ELEMENT = "rtp-hdrext";
    public static final String NAMESPACE = "urn:xmpp:jingle:apps:rtp:rtp-hdrext:0";

    /**
     * The name of the ID attribute.
     */
    public static final String ATTR_ID = "id";

    /**
     * The name of the senders attribute.
     */
    public static final String ATTR_SENDERS = "senders";

    /**
     * The name of the URI attribute.
     */
    public static final String ATTR_URI = "uri";

    /**
     * The name of the <code>attributes</code> attribute in the <code>extmap</code> element.
     */
    public static final String ATTR_ATTRIBUTES = "attributes";

    public RtpHeader() {
        super(getBuilder());
    }

    /**
     * Creates a new <code>RtpHeader</code>; required by DefaultXmlElementProvider().
     *
     * @param builder Builder instance
     */
    public RtpHeader(Builder builder) {
        super(builder);
    }

    /**
     * Get the ID.
     *
     * @return the ID
     */
    public String getId() {
        return getAttributeValue(ATTR_ID);
    }

    /**
     * Get the direction.
     *
     * @return the direction
     */
    public Senders getSenders() {
        String attributeVal = getAttributeValue(ATTR_SENDERS);

        return (attributeVal == null) ? null : Senders.valueOf(attributeVal);
    }

    /**
     * Get the URI.
     *
     * @return the URI
     */
    public URI getURI() {
        String uri = getAttributeValue(ATTR_URI);
        return URI.create(uri);
    }

    /**
     * Get "attributes" value.
     *
     * @return "attributes" value
     */
    public String getExtAttributes() {
        for (ExtensionElement ext : getChildElements()) {
            if (ext instanceof ParameterElement) {
                ParameterElement p = (ParameterElement) ext;
                if (p.getName().equals(ATTR_ATTRIBUTES)) {
                    return p.getValue();
                }
            }
        }
        return null;
    }

    /**
     * Set attributes.
     *
     * @param attributes attributes value
     */
    public void setExtAttributes(String attributes) {
        // The rtp-hdrext extension can only contain a single "parameter" child
        removeChildElement(new ParameterElement());

        addChildElement(ParameterElement.builder(RtpHeader.NAMESPACE)
                .setNameValue(ATTR_ATTRIBUTES, attributes)
                .build());
    }

    public static Builder getBuilder() {
        return new Builder(ELEMENT, NAMESPACE);
    }

    /**
     * Builder for RtpHeader. Use {@link AbstractXmlElement.Builder#Builder(String, String)}
     * to obtain a new instance and {@link #build} to build the RtpHeader.
     */
    public static final class Builder extends AbstractXmlElement.Builder<Builder, RtpHeader> {
        protected Builder(String element, String namespace) {
            super(element, namespace);
        }

        /**
         * Set the ID.
         *
         * @param id ID to set
         * @return builder instance
         */
        public Builder setID(String id) {
            addAttribute(ATTR_ID, id);
            return this;
        }

        /**
         * Set the direction.
         *
         * @param senders the direction
         * @return builder instance
         */
        public Builder setSenders(Senders senders) {
            addAttribute(ATTR_SENDERS, senders.toString());
            return this;
        }

        /**
         * Set the URI.
         *
         * @param uri URI to set
         * @return builder instance
         */
        public Builder setURI(URI uri) {
            addAttribute(ATTR_URI, uri.toString());
            return this;
        }

        /**
         * Set attributes.
         *
         * @param attributes attributes value
         * @return builder instance
         */
        public Builder setExtAttributes(String attributes) {
            // The rtp-hdrext extension can only contain a single "parameter" child
            removeChildElement(new ParameterElement());
            addChildElement(ParameterElement.builder(RtpHeader.NAMESPACE)
                    .setNameValue(ATTR_ATTRIBUTES, attributes)
                    .build());
            return this;
        }

        @Override
        public RtpHeader build() {
            return new RtpHeader(this);
        }

        @Override
        protected Builder getThis() {
            return this;
        }
    }
}
