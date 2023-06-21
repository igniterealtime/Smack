/**
 *
 * Copyright 2017-2019 Florian Schmaus
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
package org.jivesoftware.smackx.jingle.element;

import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.jingle_rtp.AbstractXmlElement;

/**
 * Jingle content element.
 *
 * @see <a href="https://xmpp.org/extensions/xep-0166.html">XEP-0166: Jingle 1.1.2 (2018-09-19)</a>
 * @see <a href="https://xmpp.org/extensions/xep-0166.html#def-content">XEP-0166 § 7.3 Content Element</a>
 *
 * @author Florian Schmaus
 * @author Eng Chong Meng
 */
public final class JingleContent extends AbstractXmlElement {
    public static final String ELEMENT = "content";
    public static final String NAMESPACE = Jingle.NAMESPACE;

    public static final String ATTR_CREATOR = "creator";
    public static final String ATTR_DISPOSITION = "disposition";
    public static final String ATTR_NAME = "name";
    public static final String ATTR_SENDERS = "senders";

    private JingleContentDescription mDescription;
    private JingleContentTransport mTransport;
    private JingleContentSecurity mSecurity;

    /**
     * The values we currently support for the creator field.
     */
    public enum Creator {
        /**
         * Indicates that content type was originally generated by the session initiator.
         */
        initiator,

        /**
         * Indicates that content type was originally generated by the session addressee.
         */
        responder
    }

    /**
     * The values we currently support for the <code>senders</code> field.
     */
    public enum Senders {
        /**
         * Indicates that both parties in this session will be generating content.
         */
        both,

        /**
         * Indicates that only the initiator will be generating content.
         */
        initiator,

        /**
         * Indicates that no one in this session will be generating content.
         */
        none,

        /**
         * Indicates that only the responder will be generating content.
         */
        responder
    }

    /**
     * <code>JingleContent</code> default constructor; use in DefaultXmlElementProvider, and newInstance() etc.
     */
    public JingleContent() {
        super(getBuilder());
    }

    /**
     * Creates a new <code>JingleContent</code> element; required by DefaultXmlElementProvider().
     *
     * @param builder Builder instance
     */
    public JingleContent(Builder builder) {
        super(builder);
        mDescription = builder.description;
        mTransport = builder.transport;
        mSecurity = builder.security;
    }

    public Creator getCreator() {
        String attributeVal = getAttributeValue(ATTR_CREATOR);
        return attributeVal == null ? null : Creator.valueOf(getAttributeValue(ATTR_CREATOR));
    }

    public String getDisposition() {
        return getAttributeValue(ATTR_DISPOSITION);
    }

    public String getName() {
        return getAttributeValue(ATTR_NAME);
    }

    public Senders getSenders() {
        String attributeVal = getAttributeValue(ATTR_SENDERS);
        return attributeVal == null ? null : Senders.valueOf(attributeVal);
    }

    /**
     * Gets the description for this Jingle content.
     *
     * @return The description.
     */
    public JingleContentDescription getDescription() {
        return mDescription;
    }

    /**
     * Returns an Iterator for the JingleTransports in the packet.
     *
     * @return an Iterator for the JingleTransports in the packet.
     */
    public JingleContentTransport getTransport() {
        return mTransport;
    }

    public JingleContentSecurity getSecurity() {
        return mSecurity;
    }

    public void setSenders(Senders senders) {
        setAttribute(ATTR_SENDERS, senders.toString());
    }

    public static Builder getBuilder() {
        return new Builder(ELEMENT, NAMESPACE);
    }

    /**
     * Builder for JingleContent. Use {@link AbstractXmlElement.Builder#Builder(String, String)}
     * to obtain a new instance and {@link #build} to build the JingleContent.
     */
    public static final class Builder extends AbstractXmlElement.Builder<Builder, JingleContent> {
        private JingleContentDescription description;
        private JingleContentTransport transport;
        private JingleContentSecurity security;

        public Builder(String element, String namespace) {
            super(element, namespace);
        }

        public Builder setCreator(Creator creator) {
            if (creator != null) {
                addAttribute(ATTR_CREATOR, creator.toString());
            }
            return this;
        }

        public Builder setDisposition(String disposition) {
            addAttribute(ATTR_DISPOSITION, disposition);
            return this;
        }

        public Builder setName(String name) {
            addAttribute(ATTR_NAME, name);
            return this;
        }

        public Builder setSenders(Senders senders) {
            if (senders != null)
                addAttribute(ATTR_SENDERS, senders.toString());
            return this;
        }

        // Not use: Is there a need to check for existing JingleContentDescription?
        public Builder setDescription(JingleContentDescription description) {
            if (elements != null && elements.containsKey(description.getQName())) {
                throw new IllegalStateException("Jingle content description already set");
            }
            this.description = description;
            addChildElement(description);
            return this;
        }

        public Builder setTransport(JingleContentTransport transport) {
            this.transport = transport;
            addChildElement(transport);
            return this;
        }

        public Builder setSecurity(JingleContentSecurity security) {
            this.security = security;
            addChildElement(security);
            return this;
        }

        @Override
        public JingleContent build() {
            // Jingle.Group.Content does not have ATTR_CREATOR
            // StringUtils.requireNotNullNorEmpty(getAttribute(ATTR_CREATOR), "Jingle content creator must not be null");
            StringUtils.requireNotNullNorEmpty(getAttribute(ATTR_NAME), "Jingle content name must not be null nor empty");
            return new JingleContent(this);
        }

        @Override
        public JingleContent.Builder getThis() {
            return this;
        }
    }
}
