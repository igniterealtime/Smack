/*
 *
 * Copyright 2026 Florian Schmaus
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
package org.jivesoftware.smack.fast.element;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import javax.xml.namespace.QName;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.sasl.packet.Sasl2MechanismsInlineFeature;
import org.jivesoftware.smack.util.XmlStringBuilder;

public class FastElements {

    public static final String NAMESPACE = "urn:xmpp:fast:0";

    /**
     * Represents the &lt;fast/&gt; element in XEP-0484.
     * Can be used as a SASL2 inline feature advertisement or inside &lt;authenticate/&gt;.
     */
    public static class Fast implements ExtensionElement, Sasl2MechanismsInlineFeature {
        public static final String ELEMENT = "fast";
        public static final QName QNAME = new QName(NAMESPACE, ELEMENT);

        private final List<String> mechanisms;
        private final Boolean tls0rtt;
        private final Long count;
        private final Boolean invalidate;

        public Fast(List<String> mechanisms, Boolean tls0rtt) {
            this(mechanisms, tls0rtt, null, null);
        }

        public Fast(Long count, Boolean invalidate) {
            this(null, null, count, invalidate);
        }

        public Fast(List<String> mechanisms, Boolean tls0rtt, Long count, Boolean invalidate) {
            this.mechanisms = mechanisms == null ? Collections.emptyList() : Collections.unmodifiableList(new ArrayList<>(mechanisms));
            this.tls0rtt = tls0rtt;
            this.count = count;
            this.invalidate = invalidate;
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
        public List<String> getMechanisms() {
            return mechanisms;
        }

        public Boolean isTls0rtt() {
            return tls0rtt;
        }

        public Long getCount() {
            return count;
        }

        public Boolean isInvalidate() {
            return invalidate;
        }

        @Override
        public XmlStringBuilder toXML(XmlEnvironment xmlEnvironment) {
            XmlStringBuilder xml = new XmlStringBuilder(this, xmlEnvironment);
            xml.optBooleanAttribute("tls-0rtt", tls0rtt != null && tls0rtt);
            if (count != null) {
                xml.attribute("count", Long.toString(count));
            }
            xml.optBooleanAttribute("invalidate", invalidate != null && invalidate);

            if (mechanisms.isEmpty()) {
                xml.closeEmptyElement();
                return xml;
            }

            xml.rightAngleBracket();
            for (String mechanism : mechanisms) {
                xml.element("mechanism", mechanism);
            }
            xml.closeElement(this);
            return xml;
        }
    }

    /**
     * Represents the &lt;request-token/&gt; element in XEP-0484.
     */
    public static class RequestToken implements ExtensionElement {
        public static final String ELEMENT = "request-token";
        public static final QName QNAME = new QName(NAMESPACE, ELEMENT);

        private final String mechanism;

        public RequestToken(String mechanism) {
            this.mechanism = Objects.requireNonNull(mechanism, "mechanism must not be null");
        }

        @Override
        public String getElementName() {
            return ELEMENT;
        }

        @Override
        public String getNamespace() {
            return NAMESPACE;
        }

        public String getMechanism() {
            return mechanism;
        }

        @Override
        public XmlStringBuilder toXML(XmlEnvironment xmlEnvironment) {
            XmlStringBuilder xml = new XmlStringBuilder(this, xmlEnvironment);
            xml.attribute("mechanism", mechanism);
            xml.closeEmptyElement();
            return xml;
        }
    }

    /**
     * Represents the &lt;token/&gt; element in XEP-0484.
     */
    public static class Token implements ExtensionElement {
        public static final String ELEMENT = "token";
        public static final QName QNAME = new QName(NAMESPACE, ELEMENT);

        private final String token;
        private final Instant expiry;

        public Token(String token, Instant expiry) {
            this.token = Objects.requireNonNull(token, "token must not be null");
            this.expiry = expiry;
        }

        @Override
        public String getElementName() {
            return ELEMENT;
        }

        @Override
        public String getNamespace() {
            return NAMESPACE;
        }

        public String getToken() {
            return token;
        }

        public Instant getExpiry() {
            return expiry;
        }

        @Override
        public XmlStringBuilder toXML(XmlEnvironment xmlEnvironment) {
            XmlStringBuilder xml = new XmlStringBuilder(this, xmlEnvironment);
            if (expiry != null) {
                xml.attribute("expiry", DateTimeFormatter.ISO_INSTANT.format(expiry));
            }
            xml.attribute("token", token);
            xml.closeEmptyElement();
            return xml;
        }
    }
}
