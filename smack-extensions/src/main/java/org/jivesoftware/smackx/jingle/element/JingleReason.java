/**
 *
 * Copyright 2017-2022 Florian Schmaus
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

import java.util.HashMap;
import java.util.Map;

import org.jivesoftware.smack.packet.XmlElement;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.XmlStringBuilder;

/**
 * The Jingle 'reason' element.
 *
 * @see <a href="https://xmpp.org/extensions/xep-0166.html#def-reason">XEP-0166 § 7.4</a>
 *
 */
public class JingleReason implements XmlElement {

    public static final String ELEMENT = "reason";
    public static final String NAMESPACE = Jingle.NAMESPACE;
    public static final String TEXT_ELEMENT = "text";

    public static AlternativeSession AlternativeSession(String sessionId) {
        return new AlternativeSession(sessionId);
    }

    public static final JingleReason Busy = new JingleReason(Reason.busy);
    public static final JingleReason Cancel = new JingleReason(Reason.cancel);
    public static final JingleReason ConnectivityError = new JingleReason(Reason.connectivity_error);
    public static final JingleReason Decline = new JingleReason(Reason.decline);
    public static final JingleReason Expired = new JingleReason(Reason.expired);
    public static final JingleReason FailedApplication = new JingleReason(Reason.failed_application);
    public static final JingleReason FailedTransport = new JingleReason(Reason.failed_transport);
    public static final JingleReason GeneralError = new JingleReason(Reason.general_error);
    public static final JingleReason Gone = new JingleReason(Reason.gone);
    public static final JingleReason IncompatibleParameters = new JingleReason(Reason.incompatible_parameters);
    public static final JingleReason MediaError = new JingleReason(Reason.media_error);
    public static final JingleReason SecurityError = new JingleReason(Reason.security_error);
    public static final JingleReason Success = new JingleReason(Reason.success);
    public static final JingleReason Timeout = new JingleReason(Reason.timeout);
    public static final JingleReason UnsupportedApplications = new JingleReason(Reason.unsupported_applications);
    public static final JingleReason UnsupportedTransports = new JingleReason(Reason.unsupported_transports);

    public enum Reason {
        alternative_session,
        busy,
        cancel,
        connectivity_error,
        decline,
        expired,
        failed_application,
        failed_transport,
        general_error,
        gone,
        incompatible_parameters,
        media_error,
        security_error,
        success,
        timeout,
        unsupported_applications,
        unsupported_transports,
        ;

        static final Map<String, Reason> LUT = new HashMap<>(Reason.values().length);

        static {
            for (Reason reason : Reason.values()) {
                LUT.put(reason.toString(), reason);
            }
        }

        final String asString;

        Reason() {
            asString = name().replace('_', '-');
        }

        @Override
        public String toString() {
            return asString;
        }

        public static Reason fromString(String string) {
            Reason reason = LUT.get(string);
            if (reason == null) {
                throw new IllegalArgumentException("Unknown reason: " + string);
            }
            return reason;
        }
    }

    protected final Reason reason;
    private final String text;
    private final XmlElement element;

    public JingleReason(Reason reason) {
        this(reason, null, null);
    }

    public JingleReason(Reason reason, String text, XmlElement element) {
        this.reason = reason;
        this.text = text;
        this.element = element;
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

    /**
     * An optional text that provides human-readable information about the reason for the action.
     *
     * @return a human-readable text with information regarding this reason or <code>null</code>.
     * @since 4.4.5
     */
    public String getText() {
        return text;
    }

    /**
     * An optional element that provides more detailed machine-readable information about the reason for the action.
     *
     * @return an element with machine-readable information about this reason or <code>null</code>.
     * @since 4.4.5
     */
    public XmlElement getElement() {
        return element;
    }

    @Override
    public XmlStringBuilder toXML(XmlEnvironment enclosingXmlEnvironment) {
        XmlStringBuilder xml = new XmlStringBuilder(this, enclosingXmlEnvironment);
        xml.rightAngleBracket();

        xml.emptyElement(reason);
        xml.optElement(TEXT_ELEMENT, text);
        xml.optAppend(element);

        xml.closeElement(this);
        return xml;
    }

    public Reason asEnum() {
        return reason;
    }


    public static class AlternativeSession extends JingleReason {

        public static final String SID = "sid";
        private final String sessionId;

        public AlternativeSession(String sessionId) {
            this(sessionId, null, null);
        }

        public AlternativeSession(String sessionId, String text, XmlElement element) {
            super(Reason.alternative_session, text, element);
            if (StringUtils.isNullOrEmpty(sessionId)) {
                throw new NullPointerException("SessionID must not be null or empty.");
            }
            this.sessionId = sessionId;
        }

        @Override
        public XmlStringBuilder toXML(org.jivesoftware.smack.packet.XmlEnvironment enclosingNamespace) {
            XmlStringBuilder xml = new XmlStringBuilder(this, enclosingNamespace);
            xml.rightAngleBracket();

            xml.openElement(reason.asString);
            xml.openElement(SID);
            xml.append(sessionId);
            xml.closeElement(SID);
            xml.closeElement(reason.asString);

            xml.closeElement(this);
            return xml;
        }

        public String getAlternativeSessionId() {
            return sessionId;
        }
    }
}
