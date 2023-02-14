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

import java.util.ArrayList;
import java.util.List;

import javax.xml.namespace.QName;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.XmlStringBuilder;

import org.jivesoftware.smackx.bytestreams.socks5.packet.Bytestream;
import org.jivesoftware.smackx.jingle.element.JingleContentTransport;
import org.jivesoftware.smackx.jingle.element.JingleContentTransportCandidate;
import org.jivesoftware.smackx.jingle.element.JingleContentTransportInfo;

/**
 * Socks5Bytestream transport element.
 */
public class JingleS5BTransport extends JingleContentTransport implements ExtensionElement {
    public static final String NAMESPACE_V1 = "urn:xmpp:jingle:transports:s5b:1";
    public static final String ATTR_DSTADDR = "dstaddr";
    public static final String ATTR_MODE = "mode";
    public static final String ATTR_SID = "sid";

    public static final QName QNAME = new QName(NAMESPACE_V1, ELEMENT);

    private final String streamId;
    private final String dstAddr;
    private final Bytestream.Mode mode;

    protected JingleS5BTransport(List<JingleContentTransportCandidate> candidates, JingleContentTransportInfo info, String streamId, String dstAddr, Bytestream.Mode mode) {
        super(candidates, info);
        StringUtils.requireNotNullNorEmpty(streamId, "sid MUST be neither null, nor empty.");
        this.streamId = streamId;
        this.dstAddr = dstAddr;
        this.mode = mode;
    }

    public String getStreamId() {
        return streamId;
    }

    public String getDestinationAddress() {
        return dstAddr;
    }

    public Bytestream.Mode getMode() {
        return mode == null ? Bytestream.Mode.tcp : mode;
    }

    @Override
    public String getNamespace() {
        return QNAME.getNamespaceURI();
    }

    @Override
    protected void addExtraAttributes(XmlStringBuilder xml) {
        xml.optAttribute(ATTR_DSTADDR, dstAddr);
        xml.optAttribute(ATTR_MODE, mode);
        xml.attribute(ATTR_SID, streamId);
    }

    public boolean hasCandidate(String candidateId) {
        return getCandidate(candidateId) != null;
    }

    public JingleS5BTransportCandidate getCandidate(String candidateId) {
        for (JingleContentTransportCandidate c : candidates) {
            JingleS5BTransportCandidate candidate = (JingleS5BTransportCandidate) c;
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
        private final ArrayList<JingleContentTransportCandidate> candidates = new ArrayList<>();
        private JingleContentTransportInfo info;

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

        public Builder addTransportCandidate(JingleS5BTransportCandidate candidate) {
            if (info != null) {
                throw new IllegalStateException("Builder has already an info set. " +
                        "The transport can only have either an info or transport candidates, not both.");
            }
            this.candidates.add(candidate);
            return this;
        }

        public Builder setTransportInfo(JingleContentTransportInfo info) {
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

        public Builder setCandidateUsed(String candidateId) {
            return setTransportInfo(new JingleS5BTransportInfo.CandidateUsed(candidateId));
        }

        public Builder setCandidateActivated(String candidateId) {
            return setTransportInfo(new JingleS5BTransportInfo.CandidateActivated(candidateId));
        }

        public Builder setCandidateError() {
            return setTransportInfo(JingleS5BTransportInfo.CandidateError.INSTANCE);
        }

        public Builder setProxyError() {
            return setTransportInfo(JingleS5BTransportInfo.ProxyError.INSTANCE);
        }

        public JingleS5BTransport build() {
            return new JingleS5BTransport(candidates, info, streamId, dstAddr, mode);
        }
    }
}
