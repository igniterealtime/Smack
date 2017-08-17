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
package org.jivesoftware.smackx.jingle.transport.jingle_s5b;

import java.io.IOException;
import java.net.Socket;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5Client;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5ClientForInitiator;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5Utils;
import org.jivesoftware.smackx.bytestreams.socks5.packet.Bytestream;
import org.jivesoftware.smackx.jingle.component.JingleContent;
import org.jivesoftware.smackx.jingle.component.JingleSession;
import org.jivesoftware.smackx.jingle.component.JingleTransportCandidate;
import org.jivesoftware.smackx.jingle.transport.jingle_s5b.element.JingleS5BTransportCandidateElement;

/**
 * Jingle SOCKS5Bytestream transport candidate.
 */
public class JingleS5BTransportCandidate extends JingleTransportCandidate<JingleS5BTransportCandidateElement> {
    private static final Logger LOGGER = Logger.getLogger(JingleS5BTransportCandidate.class.getName());

    private final String candidateId;
    private final Bytestream.StreamHost streamHost;
    private final JingleS5BTransportCandidateElement.Type type;

    private Socket socket;

    JingleS5BTransportCandidate(JingleS5BTransportCandidateElement element) {
        this(element.getCandidateId(), new Bytestream.StreamHost(element.getJid(), element.getHost(), element.getPort()), element.getPriority(), element.getType());
    }

    JingleS5BTransportCandidate(String candidateId,
                                Bytestream.StreamHost streamHost,
                                int priority,
                                JingleS5BTransportCandidateElement.Type type) {
        this.candidateId = candidateId;
        this.streamHost = streamHost;
        this.type = type;

        setPriority(priority);
    }

    JingleS5BTransportCandidate(JingleS5BTransportCandidate other) {
        this(other.candidateId,
                other.getStreamHost(),
                other.getPriority(),
                other.type);
    }

    static JingleS5BTransportCandidate fromElement(JingleS5BTransportCandidateElement element) {
        return new JingleS5BTransportCandidate(element.getCandidateId(), element.getStreamHost(), element.getPriority(), element.getType());
    }

    String getCandidateId() {
        return candidateId;
    }

    public Bytestream.StreamHost getStreamHost() {
        return streamHost;
    }

    public JingleS5BTransportCandidateElement.Type getType() {
        return type;
    }

    @Override
    public JingleS5BTransportCandidateElement getElement() {
        return new JingleS5BTransportCandidateElement(
                getCandidateId(), getStreamHost().getAddress(),
                getStreamHost().getJID(), getStreamHost().getPort(),
                getPriority(), getType());
    }

    public JingleS5BTransportCandidate connect(int timeout, boolean peersProposal) throws InterruptedException, TimeoutException, SmackException, XMPPException, IOException {
        JingleS5BTransport transport = (JingleS5BTransport) getParent();

        switch (getType()) {
            case proxy:
            case direct:
                Socks5Client client;
                if (peersProposal) {
                    String dstAddr = transport.getTheirDstAddr();
                    if (dstAddr == null) {
                        dstAddr = Socks5Utils.createDigest(transport.getStreamId(), transport.getParent().getParent().getPeer(), transport.getParent().getParent().getOurJid());
                    }
                    LOGGER.log(Level.INFO, "Connect to foreign candidate " + getCandidateId() + " using " + dstAddr);
                    LOGGER.log(Level.INFO, getStreamHost().getAddress() + ":" + getStreamHost().getPort() + " " + getStreamHost().getJID().toString() + " " + getType());
                    client = new Socks5Client(getStreamHost(), dstAddr);
                } else {
                    LOGGER.log(Level.INFO, "Connect to our candidate " + getCandidateId() + " using " + transport.getOurDstAddr());
                    LOGGER.log(Level.INFO, getStreamHost().getAddress() + ":" + getStreamHost().getPort() + " " + getStreamHost().getJID().toString() + " " + getType());
                    JingleContent content = transport.getParent();
                    JingleSession session = content.getParent();
                    client = new Socks5ClientForInitiator(getStreamHost(), transport.getOurDstAddr(), session.getJingleManager().getConnection(), transport.getStreamId(), session.getPeer());
                }
                this.socket = client.getSocket(timeout);
                break;

            default:
                LOGGER.log(Level.INFO, "Unsupported candidate type: " + getType());
                break;
        }

        return this;
    }

    public Socket getSocket() {
        return socket;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }

        if (!(other instanceof JingleS5BTransportCandidate)) {
            return false;
        }

        JingleS5BTransportCandidate o = (JingleS5BTransportCandidate) other;

        return o.getCandidateId().equals(this.getCandidateId()) &&
                o.getType() == this.getType() &&
                o.getStreamHost().equals(this.getStreamHost());
    }

    @Override
    public int hashCode() {
        return getCandidateId().hashCode() + 3 * getType().hashCode() + 5 * getStreamHost().hashCode();
    }
}
