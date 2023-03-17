/**
 *
 * Copyright 2017 Paul Schaub
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
package org.jivesoftware.smackx.jingle.transports.jingle_s5b.elements;

import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.util.XmlStringBuilder;

import org.jivesoftware.smackx.jingle.element.JingleContentTransportInfo;

/**
 * Class representing possible SOCKS5 TransportInfo elements.
 * @see <a href="https://xmpp.org/extensions/xep-0260.html">XEP-0260: Jingle SOCKS5 Bytestreams Transport Method 1.0.3 (2018-05-15)</a>
 */
public abstract class JingleS5BTransportInfo implements JingleContentTransportInfo {

    public static final String NAMESPACE = JingleS5BTransport.NAMESPACE_V1;

    @Override
    public final String getNamespace() {
        return NAMESPACE;
    }

    public abstract static class JingleS5BCandidateTransportInfo extends JingleS5BTransportInfo {
        public static final String ATTR_CID = "cid";

        private final String candidateId;

        protected JingleS5BCandidateTransportInfo(String candidateId) {
            this.candidateId = candidateId;
        }

        public final String getCandidateId() {
            return candidateId;
        }

        @Override
        public final XmlStringBuilder toXML(XmlEnvironment xmlEnvironment) {
            XmlStringBuilder xml = new XmlStringBuilder(this, xmlEnvironment);
            xml.attribute(ATTR_CID, getCandidateId());
            xml.closeEmptyElement();
            return xml;
        }

        @Override
        public final boolean equals(Object other) {
            if (!(other instanceof JingleS5BCandidateTransportInfo)) {
                return false;
            }

            JingleS5BCandidateTransportInfo otherCandidateTransportInfo = (JingleS5BCandidateTransportInfo) other;
            return toXML().toString().equals(otherCandidateTransportInfo.toXML().toString());
        }

        @Override
        public final int hashCode() {
            return getCandidateId().toString().hashCode();
        }
    }

    public static final class CandidateActivated extends JingleS5BCandidateTransportInfo {
        public static final String ELEMENT = "activated";

        public CandidateActivated(String candidateId) {
            super(candidateId);
        }

        @Override
        public String getElementName() {
            return ELEMENT;
        }
    }

    public static final class CandidateUsed extends JingleS5BCandidateTransportInfo {
        public static final String ELEMENT = "candidate-used";

        public CandidateUsed(String candidateId) {
            super(candidateId);
        }

        @Override
        public String getElementName() {
            return ELEMENT;
        }
    }

    public static final class CandidateError extends JingleS5BTransportInfo {
        public static final CandidateError INSTANCE = new CandidateError();

        public static final String ELEMENT = "candidate-error";

        private CandidateError() {
        }

        @Override
        public String getElementName() {
            return ELEMENT;
        }

        @Override
        public XmlStringBuilder toXML(org.jivesoftware.smack.packet.XmlEnvironment enclosingNamespace) {
            XmlStringBuilder xml = new XmlStringBuilder();
            xml.halfOpenElement(this);
            xml.closeEmptyElement();
            return xml;
        }

        @Override
        public boolean equals(Object other) {
            return other == INSTANCE;
        }

        @Override
        public int hashCode() {
            return toXML().toString().hashCode();
        }
    }

    public static final class ProxyError extends JingleS5BTransportInfo {
        public static final ProxyError INSTANCE = new ProxyError();

        public static final String ELEMENT = "proxy-error";

        private ProxyError() {
        }

        @Override
        public String getElementName() {
            return ELEMENT;
        }

        @Override
        public CharSequence toXML(org.jivesoftware.smack.packet.XmlEnvironment enclosingNamespace) {
            XmlStringBuilder xml = new XmlStringBuilder();
            xml.halfOpenElement(this);
            xml.closeEmptyElement();
            return xml;
        }

        @Override
        public boolean equals(Object other) {
            return other == INSTANCE;
        }

        @Override
        public int hashCode() {
            return toXML().toString().hashCode();
        }
    }
}
