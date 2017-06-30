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
public class JingleReason implements NamedElement {

    public static final String ELEMENT = "reason";

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

    public JingleReason(Reason reason) {
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


    public static class AlternativeSession extends JingleReason {

        public static final String SID = "sid";
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
            xml.openElement(SID);
            xml.append(sessionId);
            xml.closeElement(SID);
            xml.closeElement(reason.asString);

            xml.closeElement(this);
            return xml;
        }
    }
}
