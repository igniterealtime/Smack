/**
 *
 * Copyright 2017-2022 Jive Software
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
package org.jivesoftware.smackx.jingle;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smackx.AbstractXmlElement;

import java.util.List;

import javax.xml.namespace.QName;

/**
 * IceUdpTransport element.
 *
 * @see <a href="https://xmpp.org/extensions/xep-0176.html">XEP-0176: Jingle ICE-UDP Transport Method 1.1.1 (2021-03-04)</a>
 */
public class IceUdpTransport extends AbstractXmlElement {
    public static final String ELEMENT = "transport";

    public static final String NAMESPACE = "urn:xmpp:jingle:transports:ice-udp:1";

    public static final QName QNAME = new QName(NAMESPACE, ELEMENT);

    /**
     * The name of the <code>pwd</code> ICE attribute. A Password as defined in ICE-CORE.
     */
    public static final String ATTR_PWD = "pwd";

    /**
     * The name of the <code>ufrag</code> ICE attribute. A User Fragment as defined in ICE-CORE.
     */
    public static final String ATTR_UFRAG = "ufrag";

    /**
     * A list of one or more candidates representing each of the initiator's higher-priority
     * transport candidates as determined in accordance with the ICE methodology.
     */
    private List<IceUdpTransportCandidate> candidateList;

    /**
     * Once the parties have connectivity and therefore the initiator has completed ICE as
     * explained in RFC 5245, the initiator MAY communicate the in-use candidate pair in the
     * signalling channel by sending a transport-info message that contains a "remote-candidate" element.
     */
    // private RemoteCandidateExtension remoteCandidate;
    public IceUdpTransport() {
        super(builder());
    }

    public IceUdpTransport(Builder builder) {
        super(builder);
    }

    /**
     * Returns the ICE defined password attribute.
     *
     * @return a password <code>String</code> as defined in RFC 5245
     */
    public String getPassword() {
        return getAttributeValue(ATTR_PWD);
    }

    /**
     * Returns the ICE defined user fragment attribute.
     *
     * @return a user fragment <code>String</code> as defined in RFC 5245
     */
    public String getUfrag() {
        return getAttributeValue(ATTR_UFRAG);
    }

    /**
     * Checks whether an 'rtcp-mux' extension has been added to this <code>IceUdpTransport</code>.
     *
     * @return <code>true</code> if this <code>IceUdpTransport</code> has a child with the 'rtcp-mux' name.
     */
    public boolean isRtcpMux() {
        for (ExtensionElement packetExtension : getChildElements()) {
            if (RtcpMux.ELEMENT.equals(packetExtension.getElementName()))
                return true;
        }
        return false;
    }

    public IceUdpTransportCandidate getCandidate(String candidateId) {
        for (IceUdpTransportCandidate candidate : getChildElements(IceUdpTransportCandidate.class)) {
            if (candidate.getID().equals(candidateId)) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * Returns the list of {@link IceUdpTransportCandidate}s currently registered with this transport.
     *
     * @return the list of {@link IceUdpTransportCandidate}s currently registered with this transport.
     */
    public List<IceUdpTransportCandidate> getCandidateList() {
        if (candidateList == null) {
            candidateList = getChildElements(IceUdpTransportCandidate.class);
        }
        return candidateList;
    }

    /**
     * Removes <code>candidate</code> from the list of {@link IceUdpTransportCandidate}s registered with this transport.
     *
     * @param candidate the <code>CandidateExtensionElement</code> to remove from this transport element
     * @return <code>true</code> if the list of <code>CandidateExtensionElement</code>s registered with this
     * transport contains the specified <code>candidate</code>
     */
    public boolean removeCandidate(IceUdpTransportCandidate candidate) {
        if (removeChildElement(candidate)) {
            candidateList = getChildElements(IceUdpTransportCandidate.class);
            return true;
        }
        return false;
    }

    /**
     * Clones a specific <code>IceUdpTransport</code> and its candidates.
     *
     * @param src the <code>IceUdpTransport</code> to be cloned
     * @param copyDtls if <code>true</code> will also copy {@link SrtpFingerprint}.
     * @return a new <code>IceUdpTransport</code> instance which has the same run-time
     * type, attributes, namespace, text and candidates as the specified <code>src</code>
     */
    public static IceUdpTransport cloneTransportAndCandidates(final IceUdpTransport src, boolean copyDtls) {
        IceUdpTransport dst = null;
        if (src != null) {
            dst = clone(src);
            // Copy candidates
            for (IceUdpTransportCandidate srcCand : src.getCandidateList()) {
                if (!(srcCand instanceof IceUdpTransportRemoteCandidate)) {
                    dst.addChildElement(clone(srcCand));
                }
            }
            // Copy "web-socket" extensions.
            // cmeng - NPE for src during testing; force to use final hopefully it helps
            for (WebSocketExtension wspe : src.getChildElements(WebSocketExtension.class)) {
                dst.addChildElement(WebSocketExtension.builder()
                        .setUrl(wspe.getUrl())
                        .build());
            }

            // Copy RTCP MUX
            if (src.isRtcpMux()) {
                dst.addChildElement(RtcpMux.builder(IceUdpTransport.NAMESPACE).build());
            }

            // Optionally copy DTLS
            if (copyDtls) {
                for (SrtpFingerprint srtpFingerprint : src.getChildElements(SrtpFingerprint.class)) {
                    SrtpFingerprint.Builder fpBuilder = SrtpFingerprint.builder();

                    fpBuilder.setFingerprint(srtpFingerprint.getFingerprint());
                    fpBuilder.setHash(srtpFingerprint.getHash());
                    fpBuilder.setSetup(srtpFingerprint.getSetup());
                    dst.addChildElement(fpBuilder.build());
                }
            }
        }
        return dst;
    }

    public static Builder builder() {
        return new Builder(ELEMENT, NAMESPACE);
    }

    /**
     * Builder for JingleContentTransport. Use {@link AbstractXmlElement.Builder#Builder(String, String)}
     * to obtain a new instance and {@link #build} to build the JingleContentTransport.
     */
    public static class Builder extends AbstractXmlElement.Builder<Builder, IceUdpTransport> {
        protected Builder(String element, String namespace) {
            super(element, namespace);
        }

        public Builder setPassword(String pwd) {
            addAttribute(ATTR_PWD, pwd);
            return this;
        }

        public Builder setUfrag(String ufrag) {
            addAttribute(ATTR_UFRAG, ufrag);
            return this;
        }

//        public Builder addTransportCandidate(IceUdpTransportCandidate candidate)
//        {
//            if (info != null) {
//                throw new IllegalStateException("Builder has already an info set. " +
//                        "The transport can only have either an info or transport candidates, not both.");
//            }
//            this.candidates.add(candidate);
//            return this;
//        }
//
//        public Builder setTransportInfo(JingleContentTransportInfo info)
//        {
//            if (!candidates.isEmpty()) {
//                throw new IllegalStateException("Builder has already at least one candidate set. " +
//                        "The transport can only have either an info or transport candidates, not both.");
//            }
//            if (this.info != null) {
//                throw new IllegalStateException("Builder has already an info set.");
//            }
//            this.info = info;
//            return this;
//        }

        @Override
        public IceUdpTransport build() {
            return new IceUdpTransport(this);
        }

        @Override
        protected Builder getThis() {
            return this;
        }
    }
}
