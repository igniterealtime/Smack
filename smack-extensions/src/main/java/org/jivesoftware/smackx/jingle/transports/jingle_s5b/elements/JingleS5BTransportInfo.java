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

import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jivesoftware.smackx.jingle.element.JingleContentTransportInfo;

/**
 * Class representing possible SOCKS5 TransportInfo elements.
 */
public abstract class JingleS5BTransportInfo extends JingleContentTransportInfo {

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
        public final XmlStringBuilder toXML(String enclosingNamespace) {
            XmlStringBuilder xml = new XmlStringBuilder();
            xml.halfOpenElement(this);
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
            return toXML(null).equals(otherCandidateTransportInfo.toXML(null));
        }

        @Override
        public final int hashCode() {
            return getCandidateId().hashCode();
        }
    }

    public static final class CandidateActivated extends JingleS5BCandidateTransportInfo {
        public static final String ELEMENT = "candidate-activated";

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
        public XmlStringBuilder toXML(String enclosingNamespace) {
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
            return toXML(null).toString().hashCode();
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
        public CharSequence toXML(String enclosingNamespace) {
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
            return toXML(null).toString().hashCode();
        }
    }
}
