/**
 *
 * Copyright 2017 Florian Schmaus
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

import org.jivesoftware.smack.packet.NamedElement;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.XmlStringBuilder;

/**
 * The Jingle 'reason' element.
 *
 * @see <a href="https://xmpp.org/extensions/xep-0166.html#def-reason">XEP-0166 § 7.4</a>
 *
 */
public class JingleReasonElement implements NamedElement {

    public static final String ELEMENT = "reason";

    public static AlternativeSession AlternativeSession(String sessionId) {
        return new AlternativeSession(sessionId);
    }

    public static final JingleReasonElement Busy = new JingleReasonElement(Reason.busy);
    public static final JingleReasonElement Cancel = new JingleReasonElement(Reason.cancel);
    public static final JingleReasonElement ConnectivityError = new JingleReasonElement(Reason.connectivity_error);
    public static final JingleReasonElement Decline = new JingleReasonElement(Reason.decline);
    public static final JingleReasonElement Expired = new JingleReasonElement(Reason.expired);
    public static final JingleReasonElement FailedApplication = new JingleReasonElement(Reason.failed_application);
    public static final JingleReasonElement FailedTransport = new JingleReasonElement(Reason.failed_transport);
    public static final JingleReasonElement GeneralError = new JingleReasonElement(Reason.general_error);
    public static final JingleReasonElement Gone = new JingleReasonElement(Reason.gone);
    public static final JingleReasonElement IncompatibleParameters = new JingleReasonElement(Reason.incompatible_parameters);
    public static final JingleReasonElement MediaError = new JingleReasonElement(Reason.media_error);
    public static final JingleReasonElement SecurityError = new JingleReasonElement(Reason.security_error);
    public static final JingleReasonElement Success = new JingleReasonElement(Reason.success);
    public static final JingleReasonElement Timeout = new JingleReasonElement(Reason.timeout);
    public static final JingleReasonElement UnsupportedApplications = new JingleReasonElement(Reason.unsupported_applications);
    public static final JingleReasonElement UnsupportedTransports = new JingleReasonElement(Reason.unsupported_transports);

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

        protected static final Map<String, Reason> LUT = new HashMap<>(Reason.values().length);

        static {
            for (Reason reason : Reason.values()) {
                LUT.put(reason.toString(), reason);
            }
        }

        protected final String asString;

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

    public JingleReasonElement(Reason reason) {
        this.reason = reason;
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public XmlStringBuilder toXML() {
        XmlStringBuilder xml = new XmlStringBuilder(this);
        xml.rightAngleBracket();

        xml.emptyElement(reason.asString);

        xml.closeElement(this);
        return xml;
    }

    public Reason asEnum() {
        return reason;
    }


    public static class AlternativeSession extends JingleReasonElement {

        public static final String ATTR_SID = "sid";
        private final String sessionId;

        public AlternativeSession(String sessionId) {
            super(Reason.alternative_session);
            if (StringUtils.isNullOrEmpty(sessionId)) {
                throw new NullPointerException("SessionID must not be null or empty.");
            }
            this.sessionId = sessionId;
        }

        @Override
        public XmlStringBuilder toXML() {
            XmlStringBuilder xml = new XmlStringBuilder(this);
            xml.rightAngleBracket();

            xml.openElement(reason.asString);
            xml.openElement(ATTR_SID);
            xml.append(sessionId);
            xml.closeElement(ATTR_SID);
            xml.closeElement(reason.asString);

            xml.closeElement(this);
            return xml;
        }

        public String getAlternativeSessionId() {
            return sessionId;
        }
    }
}
