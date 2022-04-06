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

import java.util.List;

import javax.xml.namespace.QName;

/**
 * Represents <code>ssrc-group</code> elements described.
 * @see <a href="https://xmpp.org/extensions/xep-0339.html">XEP-0339: Source-Specific Media Attributes in Jingle</a>
 *
 * @author George Politis
 * @author Pawel Domas
 * @author Eng Chong Meng
 */
public class SdpSourceGroup extends AbstractXmlElement {
    /**
     * The name of the "ssrc-group" element.
     */
    public static final String ELEMENT = "ssrc-group";

    /**
     * The namespace for the "ssrc-group" element.
     */
    public static final String NAMESPACE = "urn:xmpp:jingle:apps:rtp:ssma:0";

    public static final QName QNAME = new QName(NAMESPACE, ELEMENT);

    /**
     * The name of the payload <code>id</code> SDP argument.
     */
    public static final String ATTR_SEMANTICS = "semantics";

    /**
     * The constant used for signaling simulcast semantics.
     */
    public static final String SEMANTICS_SIMULCAST = "SIM";

    /**
     * The constant used for flow identification (see RFC5888).
     */
    public static final String SEMANTICS_FID = "FID";

    /**
     * The constant used for fec (see RFC5956).
     */
    public static final String SEMANTICS_FEC = "FEC-FR";

    /**
     * <code>SdpSourceGroup</code> default constructor; use in DefaultXmlElementProvider, and newInstance() etc.
     */
    public SdpSourceGroup() {
        super(getBuilder());
    }

    /**
     * Initializes a new <code>SdpSourceGroup</code> instance.; required by DefaultXmlElementProvider().
     *
     * @param builder Builder instance
     */
    public SdpSourceGroup(Builder builder) {
        super(builder);
    }

    /**
     * Gets the semantics of this source group.
     *
     * @return the semantics of this source group.
     */
    public String getSemantics() {
        return getAttributeValue(ATTR_SEMANTICS);
    }

    /**
     * Gets the sources of this source group.
     *
     * @return the sources of this source group.
     */
    public List<SdpSource> getSources() {
        return getChildElements(SdpSource.class);
    }

    public static Builder getBuilder() {
        return new Builder(ELEMENT, NAMESPACE);
    }

    /**
     * Builder for SdpSourceGroup. Use {@link AbstractXmlElement.Builder#Builder(String, String)}
     * to obtain a new instance and {@link #build} to build the SdpSourceGroup.
     */
    public static class Builder extends AbstractXmlElement.Builder<Builder, SdpSourceGroup> {
        protected Builder(String element, String namespace) {
            super(element, namespace);
        }

        /**
         * Sets the semantics of this source group.
         *
         * @param semantics Semantics string
         * @return builder instance
         */
        public Builder setSemantics(String semantics) {
            addAttribute(ATTR_SEMANTICS, semantics);
            return this;
        }

        /**
         * Sets the sources of this source group.
         *
         * @param sources the sources of this source group.
         * @return builder instance
         */
        public Builder addSources(List<SdpSource> sources) {
            if (sources != null && sources.size() != 0) {
                addChildElements(sources);
            }
            return this;
        }

        @Override
        public SdpSourceGroup build() {
            return new SdpSourceGroup(this);
        }

        @Override
        public Builder getThis() {
            return this;
        }
    }
}
