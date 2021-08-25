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
package org.jivesoftware.smackx.jingle.transport.jingle_s5b.element;

import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jivesoftware.smackx.bytestreams.socks5.packet.Bytestream;
import org.jivesoftware.smackx.jingle.element.JingleContentTransportCandidateElement;
import org.jivesoftware.smackx.jingle.element.JingleContentTransportElement;
import org.jivesoftware.smackx.jingle.element.JingleContentTransportInfoElement;
import org.jivesoftware.smackx.jingle.transport.jingle_s5b.JingleS5BTransport;

/**
 * Socks5Bytestream transport element.
 */
public class JingleS5BTransportElement extends JingleContentTransportElement {
    public static final String ATTR_DSTADDR = "dstaddr";
    public static final String ATTR_MODE = "mode";
    public static final String ATTR_SID = "sid";

    private final String sid;
    private final String dstAddr;
    private final Bytestream.Mode mode;

    protected JingleS5BTransportElement(String streamId, List<JingleContentTransportCandidateElement> candidates, JingleContentTransportInfoElement info, String dstAddr, Bytestream.Mode mode) {
        super(candidates, info);
        StringUtils.requireNotNullNorEmpty(streamId, "sid MUST be neither null, nor empty.");
        this.sid = streamId;
        this.dstAddr = dstAddr;
        this.mode = mode;
    }

    public String getStreamId() {
        return sid;
    }

    public String getDestinationAddress() {
        return dstAddr;
    }

    public Bytestream.Mode getMode() {
        return mode == null ? Bytestream.Mode.tcp : mode;
    }

    @Override
    public String getNamespace() {
        return JingleS5BTransport.NAMESPACE;
    }

    @Override
    protected void addExtraAttributes(XmlStringBuilder xml) {
        xml.optAttribute(ATTR_DSTADDR, dstAddr);
        xml.optAttribute(ATTR_MODE, mode);
        xml.attribute(ATTR_SID, sid);
    }

    public boolean hasCandidate(String candidateId) {
        return getCandidate(candidateId) != null;
    }

    public JingleS5BTransportCandidateElement getCandidate(String candidateId) {
        for (JingleContentTransportCandidateElement c : candidates) {
            JingleS5BTransportCandidateElement candidate = (JingleS5BTransportCandidateElement) c;
            if (candidate.getCandidateId().equals(candidateId)) {
                return candidate;
            }
        }
        return null;
    }

    public static Builder getBuilder() {
        return new Builder();
    }

    public static class Builder {
        private String streamId;
        private String dstAddr;
        private Bytestream.Mode mode;
        private final ArrayList<JingleContentTransportCandidateElement> candidates = new ArrayList<>();
        private JingleContentTransportInfoElement info;

        public Builder setStreamId(String sid) {
            this.streamId = sid;
            return this;
        }

        public Builder setDestinationAddress(String dstAddr) {
            this.dstAddr = dstAddr;
            return this;
        }

        public Builder setMode(Bytestream.Mode mode) {
            this.mode = mode;
            return this;
        }

        public Builder addTransportCandidate(JingleS5BTransportCandidateElement candidate) {
            if (info != null) {
                throw new IllegalStateException("Builder has already an info set. " +
                        "The transport can only have either an info or transport candidates, not both.");
            }
            this.candidates.add(candidate);
            return this;
        }

        public Builder setTransportInfo(JingleContentTransportInfoElement info) {
            if (!candidates.isEmpty()) {
                throw new IllegalStateException("Builder has already at least one candidate set. " +
                        "The transport can only have either an info or transport candidates, not both.");
            }
            if (this.info != null) {
                throw new IllegalStateException("Builder has already an info set.");
            }
            this.info = info;
            return this;
        }

        public JingleS5BTransportElement build() {
            return new JingleS5BTransportElement(streamId, candidates, info, dstAddr, mode);
        }

        public Builder setCandidateUsed(String candidateId) {
            return setTransportInfo(new JingleS5BTransportInfoElement.CandidateUsed(candidateId));
        }

        public Builder setCandidateActivated(String candidateId) {
            return setTransportInfo(new JingleS5BTransportInfoElement.CandidateActivated(candidateId));
        }

        public Builder setCandidateError() {
            return setTransportInfo(JingleS5BTransportInfoElement.CandidateError.INSTANCE);
        }

        public Builder setProxyError() {
            return setTransportInfo(JingleS5BTransportInfoElement.ProxyError.INSTANCE);
        }

    }
}

