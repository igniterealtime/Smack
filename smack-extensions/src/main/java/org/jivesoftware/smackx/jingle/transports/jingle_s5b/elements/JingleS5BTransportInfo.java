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

    private static CandidateError CEI;
    private static ProxyError PEI;

    public static CandidateUsed CandidateUsed(String candidateId) {
        return new CandidateUsed(candidateId);
    }

    public static CandidateActivated CandidateActivated(String candidateId) {
        return new CandidateActivated(candidateId);
    }

    public static CandidateError CandidateError() {
        if (CEI == null) {
            CEI = new CandidateError();
        }
        return CEI;
    }

    public static ProxyError ProxyError() {
        if (PEI == null) {
            PEI = new ProxyError();
        }
        return PEI;
    }

    public static final class CandidateActivated extends JingleS5BTransportInfo {
        public static final String ELEMENT = "candidate-activated";
        public static final String ATTR_CID = "cid";

        private final String candidateId;

        public CandidateActivated(String candidateId) {
            this.candidateId = candidateId;
        }

        public String getCandidateId() {
            return candidateId;
        }

        @Override
        public String getElementName() {
            return ELEMENT;
        }

        @Override
        public CharSequence toXML() {
            XmlStringBuilder xml = new XmlStringBuilder();
            xml.halfOpenElement(this);
            xml.attribute(ATTR_CID, candidateId);
            xml.closeEmptyElement();
            return xml;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof CandidateActivated &&
                    ((CandidateActivated) other).getCandidateId().equals(candidateId);
        }

        @Override
        public int hashCode() {
            return toXML().toString().hashCode();
        }
    }

    public static final class CandidateUsed extends JingleS5BTransportInfo {
        public static final String ELEMENT = "candidate-used";
        public static final String ATTR_CID = "cid";

        private final String candidateId;

        public CandidateUsed(String candidateId) {
            this.candidateId = candidateId;
        }

        public String getCandidateId() {
            return candidateId;
        }

        @Override
        public String getElementName() {
            return ELEMENT;
        }

        @Override
        public CharSequence toXML() {
            XmlStringBuilder xml = new XmlStringBuilder();
            xml.halfOpenElement(this);
            xml.attribute(ATTR_CID, candidateId);
            xml.closeEmptyElement();
            return xml;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof CandidateUsed &&
                    ((CandidateUsed) other).getCandidateId().equals(candidateId);
        }

        @Override
        public int hashCode() {
            return toXML().toString().hashCode();
        }
    }

    public static final class CandidateError extends JingleS5BTransportInfo {
        public static final String ELEMENT = "candidate-error";

        private CandidateError() {

        }

        @Override
        public String getElementName() {
            return ELEMENT;
        }

        @Override
        public CharSequence toXML() {
            XmlStringBuilder xml = new XmlStringBuilder();
            xml.halfOpenElement(this);
            xml.closeEmptyElement();
            return xml;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof CandidateError;
        }

        @Override
        public int hashCode() {
            return toXML().toString().hashCode();
        }
    }

    public static final class ProxyError extends JingleS5BTransportInfo {
        public static final String ELEMENT = "proxy-error";

        private ProxyError() {

        }

        @Override
        public String getElementName() {
            return ELEMENT;
        }

        @Override
        public CharSequence toXML() {
            XmlStringBuilder xml = new XmlStringBuilder();
            xml.halfOpenElement(this);
            xml.closeEmptyElement();
            return xml;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof ProxyError;
        }

        @Override
        public int hashCode() {
            return toXML().toString().hashCode();
        }
    }
}
