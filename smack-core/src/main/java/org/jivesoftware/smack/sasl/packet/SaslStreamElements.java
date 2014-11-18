/**
 *
 * Copyright 2014 Florian Schmaus
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

import java.util.Collections;
import java.util.Locale;
import java.util.Map;

import org.jivesoftware.smack.packet.PlainStreamElement;
import org.jivesoftware.smack.sasl.SASLError;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.XmlStringBuilder;

public class SaslStreamElements {
    public static final String NAMESPACE = "urn:ietf:params:xml:ns:xmpp-sasl";

    /**
     * Initiating SASL authentication by select a mechanism.
     */
    public static class AuthMechanism extends PlainStreamElement {
        public static final String ELEMENT = "auth";

        private final String mechanism;
        private final String authenticationText;

        public AuthMechanism(String mechanism, String authenticationText) {
            if (mechanism == null) {
                throw new NullPointerException("SASL mechanism shouldn't be null.");
            }
            if (StringUtils.isNullOrEmpty(authenticationText)) {
                throw new IllegalArgumentException("SASL authenticationText must not be null or empty (RFC6120 6.4.2)");
            }
            this.mechanism = mechanism;
            this.authenticationText = authenticationText;
        }

        @Override
        public XmlStringBuilder toXML() {
            XmlStringBuilder xml = new XmlStringBuilder();
            xml.halfOpenElement(ELEMENT).xmlnsAttribute(NAMESPACE).attribute("mechanism", mechanism).rightAngleBracket();
            xml.optAppend(authenticationText);
            xml.closeElement(ELEMENT);
            return xml;
        }

        public String getMechanism() {
            return mechanism;
        }

        public String getAuthenticationText() {
            return authenticationText;
        }
    }

    /**
     * A SASL challenge stream element.
     */
    public static class Challenge extends PlainStreamElement {
        public static final String ELEMENT = "challenge";

        private final String data;

        public Challenge(String data) {
            this.data = StringUtils.returnIfNotEmptyTrimmed(data);
        }

        @Override
        public XmlStringBuilder toXML() {
            XmlStringBuilder xml = new XmlStringBuilder().halfOpenElement(ELEMENT).xmlnsAttribute(
                            NAMESPACE).rightAngleBracket();
            xml.optAppend(data);
            xml.closeElement(ELEMENT);
            return xml;
        }
    }

    /**
     * A SASL response stream element.
     */
    public static class Response extends PlainStreamElement {
        public static final String ELEMENT = "response";

        private final String authenticationText;

        public Response() {
            authenticationText = null;
        }

        public Response(String authenticationText) {
            this.authenticationText = StringUtils.returnIfNotEmptyTrimmed(authenticationText);
        }

        @Override
        public XmlStringBuilder toXML() {
            XmlStringBuilder xml = new XmlStringBuilder();
            xml.halfOpenElement(ELEMENT).xmlnsAttribute(NAMESPACE).rightAngleBracket();
            xml.optAppend(authenticationText);
            xml.closeElement(ELEMENT);
            return xml;
        }

        public String getAuthenticationText() {
            return authenticationText;
        }
    }

    /**
     * A SASL success stream element.
     */
    public static class Success extends PlainStreamElement {
        public static final String ELEMENT = "success";

        final private String data;

        /**
         * Construct a new SASL success stream element with optional additional data for the SASL layer
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
        public XmlStringBuilder toXML() {
            XmlStringBuilder xml = new XmlStringBuilder();
            xml.halfOpenElement(ELEMENT).xmlnsAttribute(NAMESPACE).rightAngleBracket();
            xml.optAppend(data);
            xml.closeElement(ELEMENT);
            return xml;
        }
    }

    /**
     * A SASL failure stream element.
     */
    public static class SASLFailure extends PlainStreamElement {
        public static final String ELEMENT = "failure";

        private final SASLError saslError;
        private final String saslErrorString;
        private final Map<String, String> descriptiveTexts;

        public SASLFailure(String saslError) {
            this(saslError, null);
        }

        public SASLFailure(String saslError, Map<String, String> descriptiveTexts) {
            if (descriptiveTexts != null) {
                this.descriptiveTexts = descriptiveTexts;
            } else {
                this.descriptiveTexts = Collections.emptyMap();
            }
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
         * @return the SASL error as String
         */
        public String getSASLErrorString() {
            return saslErrorString;
        }

        /**
         * Get the descriptive text of this SASLFailure.
         * <p>
         * Returns the descriptive text of this SASLFailure in the system default language if possible. May return null.
         * </p>
         * 
         * @return the descriptive text or null.
         */
        public String getDescriptiveText() {
            String defaultLocale = Locale.getDefault().getLanguage();
            String descriptiveText = getDescriptiveText(defaultLocale);
            if (descriptiveText == null) {
                descriptiveText = getDescriptiveText(null);
            }
            return descriptiveText;
        }

        /**
         * Get the descriptive test of this SASLFailure.
         * <p>
         * Returns the descriptive text of this SASLFailure in the given language. May return null if not available.
         * </p>
         * 
         * @param xmllang the language.
         * @return the descriptive text or null.
         */
        public String getDescriptiveText(String xmllang) {
            return descriptiveTexts.get(xmllang);
        }

        @Override
        public XmlStringBuilder toXML() {
            XmlStringBuilder xml = new XmlStringBuilder();
            xml.halfOpenElement(ELEMENT).xmlnsAttribute(NAMESPACE).rightAngleBracket();
            xml.emptyElement(saslErrorString);
            for (Map.Entry<String, String> entry : descriptiveTexts.entrySet()) {
                String xmllang = entry.getKey();
                String text = entry.getValue();
                xml.halfOpenElement("text").xmllangAttribute(xmllang).rightAngleBracket();
                xml.escape(text);
                xml.closeElement("text");
            }
            xml.closeElement(ELEMENT);
            return xml;
        }
    }
}
