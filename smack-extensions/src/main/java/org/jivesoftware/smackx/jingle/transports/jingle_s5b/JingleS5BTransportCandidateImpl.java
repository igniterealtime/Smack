/**
 *
 * Copyright 2017-2022 Paul Schaub
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
package org.jivesoftware.smackx.jingle.transports.jingle_s5b;

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
import org.jivesoftware.smackx.jingle.component.JingleContentImpl;
import org.jivesoftware.smackx.jingle.component.JingleSessionImpl;
import org.jivesoftware.smackx.jingle.component.JingleTransportCandidate;
import org.jivesoftware.smackx.jingle.transports.jingle_s5b.elements.JingleS5BTransportCandidate;

/**
 * Jingle SOCKS5Bytestream transport candidate.
 *
 * @author Paul Schaub
 * @author Eng Chong Meng
 */
public class JingleS5BTransportCandidateImpl extends JingleTransportCandidate<JingleS5BTransportCandidate> {
    private static final Logger LOGGER = Logger.getLogger(JingleS5BTransportCandidateImpl.class.getName());

    private final String candidateId;
    private final Bytestream.StreamHost streamHost;
    private final JingleS5BTransportCandidate.Type type;

    private Socket socket;

    JingleS5BTransportCandidateImpl(JingleS5BTransportCandidate element) {
        this(element.getCandidateId(), new Bytestream.StreamHost(element.getJid(), element.getHost(), element.getPort()), element.getPriority(), element.getType());
    }

    JingleS5BTransportCandidateImpl(String candidateId,
                                Bytestream.StreamHost streamHost,
                                int priority,
                                JingleS5BTransportCandidate.Type type) {
        this.candidateId = candidateId;
        this.streamHost = streamHost;
        this.type = type;

        setPriority(priority);
    }

    JingleS5BTransportCandidateImpl(JingleS5BTransportCandidateImpl other) {
        this(other.candidateId,
                other.getStreamHost(),
                other.getPriority(),
                other.type);
    }

    static JingleS5BTransportCandidateImpl fromElement(JingleS5BTransportCandidate element) {
        return new JingleS5BTransportCandidateImpl(element.getCandidateId(), element.getStreamHost(), element.getPriority(), element.getType());
    }

    String getCandidateId() {
        return candidateId;
    }

    public Bytestream.StreamHost getStreamHost() {
        return streamHost;
    }

    public JingleS5BTransportCandidate.Type getType() {
        return type;
    }

    @Override
    public JingleS5BTransportCandidate getElement() {
        return new JingleS5BTransportCandidate(
                getCandidateId(), getStreamHost().getAddress(),
                getStreamHost().getJID(), getStreamHost().getPort(),
                getPriority(), getType());
    }

    public JingleS5BTransportCandidateImpl connect(int timeout, boolean peersProposal) throws InterruptedException, TimeoutException, SmackException, XMPPException, IOException {
        JingleS5BTransportImpl transport = (JingleS5BTransportImpl) getParent();

        switch (getType()) {
            case proxy:
            case direct:
                Socks5Client client;
                if (peersProposal) {
                    String dstAddr = transport.getTheirDstAddr();
                    if (dstAddr == null) {
                        dstAddr = Socks5Utils.createDigest(transport.getStreamId(), transport.getParent().getParent().getRemote(), transport.getParent().getParent().getLocal());
                    }
                    LOGGER.log(Level.INFO, "Connect to foreign candidate " + getCandidateId() + " using " + dstAddr);
                    LOGGER.log(Level.INFO, getStreamHost().getAddress() + ":" + getStreamHost().getPort() + " " + getStreamHost().getJID().toString() + " " + getType());
                    client = new Socks5Client(getStreamHost(), dstAddr);
                } else {
                    LOGGER.log(Level.INFO, "Connect to our candidate " + getCandidateId() + " using " + transport.getOurDstAddr());
                    LOGGER.log(Level.INFO, getStreamHost().getAddress() + ":" + getStreamHost().getPort() + " " + getStreamHost().getJID().toString() + " " + getType());
                    JingleContentImpl content = transport.getParent();
                    JingleSessionImpl session = content.getParent();
                    client = new Socks5ClientForInitiator(getStreamHost(), transport.getOurDstAddr(), session.getConnection(), transport.getStreamId(), session.getRemote());
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

        if (!(other instanceof JingleS5BTransportCandidateImpl)) {
            return false;
        }

        JingleS5BTransportCandidateImpl o = (JingleS5BTransportCandidateImpl) other;

        return o.getCandidateId().equals(this.getCandidateId()) &&
                o.getType() == this.getType() &&
                o.getStreamHost().equals(this.getStreamHost());
    }

    @Override
    public int hashCode() {
        return getCandidateId().hashCode() + 3 * getType().hashCode() + 5 * getStreamHost().hashCode();
    }
}
