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

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.util.Objects;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.XmlStringBuilder;

/**
 * Jingle content element.
 *
 * @see <a href="https://xmpp.org/extensions/xep-0166.html">XEP-0166: Jingle 1.1.2 (2018-09-19)</a>
 * @see <a href="https://xmpp.org/extensions/xep-0166.html#def-content">XEP-0166 § 7.3 Content Element</a>
 *
 * @author Florian Schmaus
 * @author Eng Chong Meng
 */
public final class JingleContent implements ExtensionElement {

    public static final String ELEMENT = "content";
    public static final String NAMESPACE = Jingle.NAMESPACE;

    public static final String ATTR_CREATOR = "creator";
    public static final String ATTR_DISPOSITION = "disposition";
    public static final String ATTR_NAME = "name";
    public static final String ATTR_SENDERS = "senders";

    /**
     * Which party originally generated the content type. Defined values are 'initiator' and 'responder'.
     * Default is 'initiator'.
     */
    private final Creator creator;

    private final JingleContentDescription description;

    private final String disposition;

    private final String name;

    private JingleContentSecurity security;

    private final JingleContentTransport transport;

    /**
     * Which parties in the session will be generation the content. Defined values are 'both', 'initiator', 'none' and
     * 'responder. Default is 'both'.
     */
    private final Senders senders;

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
     * Creates a content description..
     */
    private JingleContent(Creator creator, String disposition, String name, Senders senders,
                    JingleContentDescription description, JingleContentTransport transport, JingleContentSecurity security) {
        this.creator = Objects.requireNonNull(creator, "Jingle content creator must not be null");
        this.disposition = disposition;
        this.name = StringUtils.requireNotNullNorEmpty(name, "Jingle content name must not be null nor empty");
        this.senders = senders;
        this.description = description;
        this.transport = transport;
        this.security = security;
    }

    public Creator getCreator() {
        return creator;
    }

    public String getDisposition() {
        return disposition;
    }

    public String getName() {
        return name;
    }

    public Senders getSenders() {
        return senders;
    }

    /**
     * Gets the description for this Jingle content.
     *
     * @return The description.
     */
    public JingleContentDescription getDescription() {
        return description;
    }

    /**
     * Returns an Iterator for the JingleTransports in the packet.
     *
     * @return an Iterator for the JingleTransports in the packet.
     * @deprecated use {@link #getTransport()} instead.
     */
    @Deprecated
    public JingleContentTransport getJingleTransport() {
        return getTransport();
    }

    /**
     * Returns an Iterator for the JingleTransports in the packet.
     *
     * @return an Iterator for the JingleTransports in the packet.
     */
    public JingleContentTransport getTransport() {
        return transport;
    }

    public JingleContentSecurity getSecurity() {
        return security;
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

    @Override
    public XmlStringBuilder toXML(XmlEnvironment enclosingXmlEnvironment) {
        XmlStringBuilder xml = new XmlStringBuilder(this, enclosingXmlEnvironment);
        xml.attribute(ATTR_CREATOR, creator);
        xml.optAttribute(ATTR_DISPOSITION, disposition);
        xml.attribute(ATTR_NAME, name);
        xml.optAttribute(ATTR_SENDERS, senders);

        if (description == null && transport == null) {
            return xml.closeEmptyElement();
        }

        xml.rightAngleBracket();

        xml.optAppend(description);
        xml.optElement(transport);
        xml.optElement(security);

        xml.closeElement(this);
        return xml;
    }

    public static Builder getBuilder() {
        return new Builder();
    }

    public static final class Builder {
        private Creator creator;

        private String disposition;

        private String name;

        private Senders senders;

        private JingleContentDescription description;

        private JingleContentTransport transport;

        private JingleContentSecurity security;

        private Builder() {
        }

        public Builder setCreator(Creator creator) {
            this.creator = creator;
            return this;
        }

        public Builder setDisposition(String disposition) {
            this.disposition = disposition;
            return this;
        }

        public Builder setName(String name) {
            this.name = name;
            return this;
        }

        public Builder setSenders(Senders senders) {
            this.senders = senders;
            return this;
        }

        public Builder setDescription(JingleContentDescription description) {
            if (this.description != null) {
                throw new IllegalStateException("Jingle content description already set");
            }
            this.description = description;
            return this;
        }

        public Builder setTransport(JingleContentTransport transport) {
            this.transport = transport;
            return this;
        }

        public Builder setSecurity(JingleContentSecurity security) {
            this.security = security;
            return this;
        }

        public JingleContent build() {
            return new JingleContent(creator, disposition, name, senders, description, transport, security);
        }
    }
}
