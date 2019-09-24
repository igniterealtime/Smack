/**
 *
 * Copyright 2014-2019 Florian Schmaus
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
package org.jivesoftware.smack.sasl.packet;

import java.util.Map;

import javax.xml.namespace.QName;

import org.jivesoftware.smack.packet.AbstractError;
import org.jivesoftware.smack.packet.Nonza;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.sasl.SASLError;
import org.jivesoftware.smack.util.Objects;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.XmlStringBuilder;

public interface SaslNonza extends Nonza {
    String NAMESPACE = "urn:ietf:params:xml:ns:xmpp-sasl";

    @Override
    default String getNamespace() {
        return NAMESPACE;
    }

    /**
     * Initiating SASL authentication by select a mechanism.
     */
    class AuthMechanism implements SaslNonza {
        public static final String ELEMENT = "auth";
        public static final QName QNAME = new QName(NAMESPACE, ELEMENT);

        private final String mechanism;
        private final String authenticationText;

        public AuthMechanism(String mechanism, String authenticationText) {
            this.mechanism = Objects.requireNonNull(mechanism, "SASL mechanism shouldn't be null.");
            this.authenticationText = StringUtils.requireNotNullNorEmpty(authenticationText,
                            "SASL authenticationText must not be null nor empty (RFC6120 6.4.2)");
        }

        @Override
        public XmlStringBuilder toXML(XmlEnvironment xmlEnvironment) {
            XmlStringBuilder xml = new XmlStringBuilder(this, xmlEnvironment);
            xml.attribute("mechanism", mechanism).rightAngleBracket();
            xml.escape(authenticationText);
            xml.closeElement(this);
            return xml;
        }

        public String getMechanism() {
            return mechanism;
        }

        public String getAuthenticationText() {
            return authenticationText;
        }

        @Override
        public String getElementName() {
            return ELEMENT;
        }
    }

    /**
     * A SASL challenge stream element.
     */
    class Challenge implements SaslNonza {
        public static final String ELEMENT = "challenge";
        public static final QName QNAME = new QName(NAMESPACE, ELEMENT);

        private final String data;

        public Challenge(String data) {
            this.data = StringUtils.returnIfNotEmptyTrimmed(data);
        }

        public String getData() {
            return data;
        }

        @Override
        public XmlStringBuilder toXML(XmlEnvironment xmlEnvironment) {
            XmlStringBuilder xml = new XmlStringBuilder(this, xmlEnvironment);
            xml.optTextChild(data, this);
            return xml;
        }

        @Override
        public String getElementName() {
            return ELEMENT;
        }
    }

    /**
     * A SASL response stream element.
     */
    class Response implements SaslNonza {
        public static final String ELEMENT = "response";
        public static final QName QNAME = new QName(NAMESPACE, ELEMENT);

        private final String authenticationText;

        public Response() {
            authenticationText = null;
        }

        public Response(String authenticationText) {
            this.authenticationText = StringUtils.returnIfNotEmptyTrimmed(authenticationText);
        }

        @Override
        public XmlStringBuilder toXML(XmlEnvironment xmlEnvironment) {
            XmlStringBuilder xml = new XmlStringBuilder(this, xmlEnvironment);
            xml.optTextChild(authenticationText, this);
            return xml;
        }

        public String getAuthenticationText() {
            return authenticationText;
        }

        @Override
        public String getElementName() {
            return ELEMENT;
        }
    }

    /**
     * A SASL success stream element.
     */
    class Success implements SaslNonza {
        public static final String ELEMENT = "success";
        public static final QName QNAME = new QName(NAMESPACE, ELEMENT);

        private final String data;

        /**
         * Construct a new SASL success stream element with optional additional data for the SASL layer.
         * (RFC6120 6.3.10)
         *
         * @param data additional data for the SASL layer or <code>null</code>
         */
        public Success(String data) {
            this.data = StringUtils.returnIfNotEmptyTrimmed(data);
        }

        /**
         * Returns additional data for the SASL layer or <code>null</code>.
         *
         * @return additional data or <code>null</code>
         */
        public String getData() {
            return data;
        }

        @Override
        public XmlStringBuilder toXML(XmlEnvironment xmlEnvironment) {
            XmlStringBuilder xml = new XmlStringBuilder(this, xmlEnvironment);
            xml.optTextChild(data, this);
            return xml;
        }

        @Override
        public String getElementName() {
            return ELEMENT;
        }
    }

    /**
     * A SASL failure stream element, also called "SASL Error".
     * @see <a href="http://xmpp.org/rfcs/rfc6120.html#sasl-errors">RFC 6120 6.5 SASL Errors</a>
     */
    class SASLFailure extends AbstractError implements SaslNonza {
        public static final String ELEMENT = "failure";
        public static final QName QNAME = new QName(NAMESPACE, ELEMENT);

        private final SASLError saslError;
        private final String saslErrorString;

        public SASLFailure(String saslError) {
            this(saslError, null);
        }

        public SASLFailure(String saslError, Map<String, String> descriptiveTexts) {
            super(descriptiveTexts);
            SASLError error = SASLError.fromString(saslError);
            if (error == null) {
                // RFC6120 6.5 states that unknown condition must be treat as generic authentication
                // failure.
                this.saslError = SASLError.not_authorized;
            }
            else {
                this.saslError = error;
            }
            this.saslErrorString = saslError;
        }

        /**
         * Get the SASL related error condition.
         *
         * @return the SASL related error condition.
         */
        public SASLError getSASLError() {
            return saslError;
        }

        /**
         * Get the SASL error as String.
         * @return the SASL error as String
         */
        public String getSASLErrorString() {
            return saslErrorString;
        }

        @Override
        public XmlStringBuilder toXML(org.jivesoftware.smack.packet.XmlEnvironment enclosingNamespace) {
            XmlStringBuilder xml = new XmlStringBuilder();
            xml.halfOpenElement(ELEMENT).xmlnsAttribute(NAMESPACE).rightAngleBracket();
            xml.emptyElement(saslErrorString);
            addDescriptiveTextsAndExtensions(xml);
            xml.closeElement(ELEMENT);
            return xml;
        }

        @Override
        public String toString() {
            return toXML().toString();
        }

        @Override
        public String getElementName() {
            return ELEMENT;
        }
    }
}
