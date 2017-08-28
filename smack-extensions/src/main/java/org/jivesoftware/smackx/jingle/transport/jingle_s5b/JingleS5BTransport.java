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
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.util.Async;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5BytestreamSession;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5Proxy;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5Utils;
import org.jivesoftware.smackx.bytestreams.socks5.packet.Bytestream;
import org.jivesoftware.smackx.jingle.JingleManager;
import org.jivesoftware.smackx.jingle.callback.JingleTransportCallback;
import org.jivesoftware.smackx.jingle.component.JingleSession;
import org.jivesoftware.smackx.jingle.component.JingleTransport;
import org.jivesoftware.smackx.jingle.component.JingleTransportCandidate;
import org.jivesoftware.smackx.jingle.element.JingleContentTransportCandidateElement;
import org.jivesoftware.smackx.jingle.element.JingleContentTransportElement;
import org.jivesoftware.smackx.jingle.element.JingleContentTransportInfoElement;
import org.jivesoftware.smackx.jingle.element.JingleElement;
import org.jivesoftware.smackx.jingle.exception.FailedTransportException;
import org.jivesoftware.smackx.jingle.transport.jingle_s5b.element.JingleS5BTransportCandidateElement;
import org.jivesoftware.smackx.jingle.transport.jingle_s5b.element.JingleS5BTransportElement;
import org.jivesoftware.smackx.jingle.transport.jingle_s5b.element.JingleS5BTransportInfoElement;

import org.jxmpp.jid.FullJid;

/**
 * Jingle SOCKS5Bytestream transport component.
 */
public class JingleS5BTransport extends JingleTransport<JingleS5BTransportElement> {

    private static final Logger LOGGER = Logger.getLogger(JingleS5BTransport.class.getName());

    public static final String NAMESPACE_V1 = "urn:xmpp:jingle:transports:s5b:1";
    public static final String NAMESPACE = NAMESPACE_V1;

    private static final int MAX_TIMEOUT = 10 * 1000;

    private final String sid;

    private String ourDstAddr;
    private String theirDstAddr;

    private Bytestream.Mode mode;

    // PEERS candidate of OUR choice.
    private JingleS5BTransportCandidate ourSelectedCandidate;
    private JingleS5BTransportCandidate theirSelectedCandidate;

    private JingleTransportCallback callback;

    /**
     * Create transport as initiator.
     * @param initiator initiator of the Jingle session.
     * @param responder responder.
     * @param sid sessionId of the Jingle session.
     * @param mode TCP/UDP.
     * @param ourCandidates our proxy candidates.
     */
    JingleS5BTransport(FullJid initiator, FullJid responder, String sid, Bytestream.Mode mode, List<JingleTransportCandidate<?>> ourCandidates) {
        this.sid = sid;
        this.mode = mode;
        this.ourDstAddr = Socks5Utils.createDigest(sid, initiator, responder);
        Socks5Proxy.getSocks5Proxy().addTransfer(ourDstAddr);

        for (JingleTransportCandidate<?> c : ourCandidates) {
            addOurCandidate(c);
        }
    }

    /**
     * Create simple transport as responder.
     * @param initiator initiator of the Jingle session.
     * @param responder responder.
     * @param ourCandidates our proxy candidates.
     * @param other transport of the other party.
     */
    JingleS5BTransport(FullJid initiator, FullJid responder, List<JingleTransportCandidate<?>> ourCandidates, JingleS5BTransport other) {
        this.sid = other.getStreamId();
        this.mode = other.mode;
        this.ourDstAddr = Socks5Utils.createDigest(sid, responder, initiator);
        Socks5Proxy.getSocks5Proxy().addTransfer(ourDstAddr);
        this.theirDstAddr = other.theirDstAddr;

        for (JingleTransportCandidate<?> c : ourCandidates) {
            addOurCandidate(c);
        }

        for (JingleTransportCandidate<?> c : other.getTheirCandidates()) {
            addTheirCandidate(c);
        }
    }

    /**
     * Create custom transport as responder.
     * @param sid sessionId of the Jingle session.
     * @param mode UPD/TCP.
     * @param ourDstAddr SOCKS5 destination address (digest)
     * @param theirDstAddr SOCKS5 destination address (digest)
     * @param ourCandidates our proxy candidates.
     * @param theirCandidates their proxy candidates.
     */
    JingleS5BTransport(String sid, Bytestream.Mode mode, String ourDstAddr, String theirDstAddr, List<JingleTransportCandidate<?>> ourCandidates, List<JingleTransportCandidate<?>> theirCandidates) {
        this.sid = sid;
        this.mode = mode;
        this.ourDstAddr = ourDstAddr;
        Socks5Proxy.getSocks5Proxy().addTransfer(ourDstAddr);
        this.theirDstAddr = theirDstAddr;

        for (JingleTransportCandidate<?> c : (ourCandidates != null ? ourCandidates :
                Collections.<JingleS5BTransportCandidate>emptySet())) {
            addOurCandidate(c);
        }

        for (JingleTransportCandidate<?> c : (theirCandidates != null ? theirCandidates :
                Collections.<JingleS5BTransportCandidate>emptySet())) {
            addTheirCandidate(c);
        }
    }

    /**
     * Copy constructor.
     * @param original which will be copied.
     */
    public JingleS5BTransport(JingleS5BTransport original) {
        this.sid = original.sid;
        this.ourDstAddr = original.ourDstAddr;
        this.theirDstAddr = original.theirDstAddr;
        this.mode = original.mode;

        for (JingleTransportCandidate<?> c : original.getOurCandidates()) {
            addOurCandidate(new JingleS5BTransportCandidate((JingleS5BTransportCandidate) c));
        }

        for (JingleTransportCandidate<?> c : original.getTheirCandidates()) {
            addTheirCandidate(new JingleS5BTransportCandidate((JingleS5BTransportCandidate) c));
        }
    }

    @Override
    public JingleS5BTransportElement getElement() {
        JingleS5BTransportElement.Builder builder = JingleS5BTransportElement.getBuilder()
                .setStreamId(sid)
                .setDestinationAddress(ourDstAddr)
                .setMode(mode);

        for (JingleTransportCandidate<?> candidate : getOurCandidates()) {
            builder.addTransportCandidate((JingleS5BTransportCandidateElement) candidate.getElement());
        }

        return builder.build();
    }

    public String getStreamId() {
        return sid;
    }

    public String getOurDstAddr() {
        return ourDstAddr;
    }

    public String getTheirDstAddr() {
        return theirDstAddr;
    }

    public Bytestream.Mode getMode() {
        return mode;
    }

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

    @Override
    public void prepare(XMPPConnection connection) {
        JingleSession session = getParent().getParent();
        if (getOurDstAddr() == null) {
            ourDstAddr = Socks5Utils.createDigest(getStreamId(), session.getOurJid(), session.getPeer());
            Socks5Proxy.getSocks5Proxy().addTransfer(ourDstAddr);
        }

        if (mode == null) {
            mode = Bytestream.Mode.tcp;
        }

        if (getOurCandidates().size() == 0) {
            List<JingleTransportCandidate<?>> candidates = JingleS5BTransportManager.getInstanceFor(connection).collectCandidates();
            for (JingleTransportCandidate<?> c : candidates) {
                addOurCandidate(c);
            }
        }
    }

    @Override
    public void establishIncomingBytestreamSession(XMPPConnection connection, JingleTransportCallback callback, JingleSession session)
            throws SmackException.NotConnectedException, InterruptedException {
        LOGGER.log(Level.FINE, "Establishing incoming bytestream.");
        this.callback = callback;
        establishBytestreamSession(connection);
    }

    @Override
    public void establishOutgoingBytestreamSession(XMPPConnection connection, JingleTransportCallback callback, JingleSession session)
            throws SmackException.NotConnectedException, InterruptedException {
        LOGGER.log(Level.FINE, "Establishing outgoing bytestream.");
        this.callback = callback;
        establishBytestreamSession(connection);
    }

    @SuppressWarnings("ReferenceEquality")
    private void establishBytestreamSession(XMPPConnection connection)
            throws SmackException.NotConnectedException, InterruptedException {
        Socks5Proxy.getSocks5Proxy().addTransfer(ourDstAddr);
        this.ourSelectedCandidate = connectToCandidates(MAX_TIMEOUT);

        if (ourSelectedCandidate == CANDIDATE_FAILURE) {
            connection.createStanzaCollectorAndSend(JingleS5BTransportManager.createCandidateError(this));
            return;
        }

        if (ourSelectedCandidate == null) {
            throw new AssertionError("MUST NOT BE NULL.");
        }

        connection.createStanzaCollectorAndSend(JingleS5BTransportManager.createCandidateUsed(this, ourSelectedCandidate));
        connectIfReady();
    }

    @SuppressWarnings("ReferenceEquality")
    private JingleS5BTransportCandidate connectToCandidates(int timeout) {

        if (getTheirCandidates().size() == 0) {
            LOGGER.log(Level.FINE, "They provided 0 candidates.");
            return CANDIDATE_FAILURE;
        }

        int _timeout = timeout / getTheirCandidates().size(); //TODO: Wise?
        for (JingleTransportCandidate<?> c : getTheirCandidates()) {
            JingleS5BTransportCandidate candidate = (JingleS5BTransportCandidate) c;
            try {
                return candidate.connect(_timeout, true);
            } catch (IOException | TimeoutException | InterruptedException | SmackException | XMPPException e) {
                LOGGER.log(Level.FINE, "Exception while connecting to candidate: " + e, e);
            }
        }

        // Failed to connect to any candidate.
        return CANDIDATE_FAILURE;
    }

    @Override
    public void cleanup() {
        Socks5Proxy.getSocks5Proxy().removeTransfer(ourDstAddr);
    }

    @SuppressWarnings("ReferenceEquality")
    private void connectIfReady() {
        final JingleSession session = getParent().getParent();

        if (ourSelectedCandidate == null || theirSelectedCandidate == null) {
            // Not yet ready if we or peer did not yet decide on a candidate.
            LOGGER.log(Level.FINEST, "Not ready.");
            return;
        }

        if (ourSelectedCandidate == CANDIDATE_FAILURE && theirSelectedCandidate == CANDIDATE_FAILURE) {
            LOGGER.log(Level.FINE, "Failure.");
            callback.onTransportFailed(new FailedTransportException(null));
            return;
        }

        LOGGER.log(Level.FINE, (session.isInitiator() ? "Initiator" : "Responder") + " is ready.");

        //Determine nominated candidate.
        JingleS5BTransportCandidate nominated;
        if (ourSelectedCandidate != CANDIDATE_FAILURE && theirSelectedCandidate != CANDIDATE_FAILURE) {

            if (ourSelectedCandidate.getPriority() > theirSelectedCandidate.getPriority()) {
                nominated = ourSelectedCandidate;
            } else if (ourSelectedCandidate.getPriority() < theirSelectedCandidate.getPriority()) {
                nominated = theirSelectedCandidate;
            } else {
                nominated = getParent().getParent().isInitiator() ? ourSelectedCandidate : theirSelectedCandidate;
            }

        } else if (ourSelectedCandidate != CANDIDATE_FAILURE) {
            nominated = ourSelectedCandidate;
        } else {
            nominated = theirSelectedCandidate;
        }

        boolean isProxy = nominated.getType() == JingleS5BTransportCandidateElement.Type.proxy;

        if (nominated == theirSelectedCandidate) {

            LOGGER.log(Level.FINE, "Their choice, so our proposed candidate is used.");

            try {
                nominated = nominated.connect(MAX_TIMEOUT, false);
            } catch (InterruptedException | IOException | XMPPException | SmackException | TimeoutException e) {
                LOGGER.log(Level.INFO, "Could not connect to our candidate.", e);

                Async.go(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            session.getJingleManager().getConnection().createStanzaCollectorAndSend(JingleS5BTransportManager.createProxyError(JingleS5BTransport.this));
                        } catch (SmackException.NotConnectedException | InterruptedException e1) {
                            LOGGER.log(Level.SEVERE, "Could not send proxy error.", e);
                        }
                    }
                });

                callback.onTransportFailed(new S5BTransportException.CandidateError(e));
                return;
            }

            if (isProxy) {
                LOGGER.log(Level.FINE, "Send candidate-activate.");
                JingleElement candidateActivate = JingleS5BTransportManager.createCandidateActivated((JingleS5BTransport) nominated.getParent(), nominated);

                try {
                    session.getJingleManager().getConnection().createStanzaCollectorAndSend(candidateActivate)
                            .nextResultOrThrow();
                } catch (InterruptedException | XMPPException.XMPPErrorException | SmackException.NotConnectedException | SmackException.NoResponseException e) {
                    LOGGER.log(Level.WARNING, "Could not send candidate-activated", e);
                    callback.onTransportFailed(new S5BTransportException.ProxyError(e));
                    return;
                }
            }

            LOGGER.log(Level.FINE, "Start transmission on " + nominated.getCandidateId());
            this.bytestreamSession = new Socks5BytestreamSession(nominated.getSocket(), !isProxy);
            callback.onTransportReady(this.bytestreamSession);

        }
        //Our choice
        else {
            LOGGER.log(Level.FINE, "Our choice, so their candidate was used.");
            if (!isProxy) {
                LOGGER.log(Level.FINE, "Start transmission on " + nominated.getCandidateId());
                this.bytestreamSession = new Socks5BytestreamSession(nominated.getSocket(), true);
                callback.onTransportReady(this.bytestreamSession);
            } else {
                LOGGER.log(Level.FINE, "Our choice was their external proxy. wait for candidate-activate.");
            }
        }
    }

    @Override
    public void handleSessionAccept(JingleContentTransportElement transportElement, XMPPConnection connection) {
        JingleS5BTransportElement transport = (JingleS5BTransportElement) transportElement;
        theirDstAddr = transport.getDestinationAddress();
        for (JingleContentTransportCandidateElement c : transport.getCandidates()) {
            JingleS5BTransportCandidateElement candidate = (JingleS5BTransportCandidateElement) c;
            addTheirCandidate(new JingleS5BTransportCandidate(candidate));
        }
    }

    @Override
    public IQ handleTransportInfo(JingleContentTransportInfoElement info, JingleElement wrapping) {
        switch (info.getElementName()) {

            case JingleS5BTransportInfoElement.CandidateUsed.ELEMENT:
                handleCandidateUsed((JingleS5BTransportInfoElement) info, wrapping);
                break;

            case JingleS5BTransportInfoElement.CandidateActivated.ELEMENT:
                handleCandidateActivate((JingleS5BTransportInfoElement) info);
                break;

            case JingleS5BTransportInfoElement.CandidateError.ELEMENT:
                handleCandidateError((JingleS5BTransportInfoElement) info);
                break;

            case JingleS5BTransportInfoElement.ProxyError.ELEMENT:
                handleProxyError((JingleS5BTransportInfoElement) info);
                break;

            default:
                throw new AssertionError("Unknown transport-info element: " + info.getElementName());
        }

        return IQ.createResultIQ(wrapping);
    }

    private void handleCandidateUsed(JingleS5BTransportInfoElement info, JingleElement wrapping) {
        JingleManager jingleManager = getParent().getParent().getJingleManager();
        String candidateId = ((JingleS5BTransportInfoElement.CandidateUsed) info).getCandidateId();

        // Received second candidate-used -> out-of-order!
        if (theirSelectedCandidate != null) {
            try {
                jingleManager.getConnection().sendStanza(JingleElement.createJingleErrorOutOfOrder(wrapping));
                //jingleManager.getConnection().createStanzaCollectorAndSend(JingleElement.createJingleErrorOutOfOrder(wrapping));
            } catch (SmackException.NotConnectedException | InterruptedException e) {
                LOGGER.log(Level.SEVERE, "Could not respond to candidate-used transport-info.", e);
            }
            return;
        }

        for (JingleTransportCandidate<?> jingleTransportCandidate : getOurCandidates()) {
            JingleS5BTransportCandidate candidate = (JingleS5BTransportCandidate) jingleTransportCandidate;
            if (candidate.getCandidateId().equals(candidateId)) {
                theirSelectedCandidate = candidate;
            }
        }

        if (theirSelectedCandidate == null) {
            LOGGER.log(Level.SEVERE, "Unknown candidateID.");
            //TODO: Alert! Illegal candidateId!
            return;
        }

        connectIfReady();
    }

    private void handleCandidateActivate(JingleS5BTransportInfoElement info) {
        this.bytestreamSession = new Socks5BytestreamSession(ourSelectedCandidate.getSocket(),
                ourSelectedCandidate.getStreamHost().getJID().asBareJid().equals(getParent().getParent().getPeer().asBareJid()));
        callback.onTransportReady(this.bytestreamSession);
    }

    private void handleCandidateError(JingleS5BTransportInfoElement info) {
        theirSelectedCandidate = CANDIDATE_FAILURE;
        connectIfReady();
    }

    private void handleProxyError(JingleS5BTransportInfoElement info) {
        callback.onTransportFailed(new S5BTransportException.ProxyError(null));
    }

    /**
     * Internal dummy candidate used to represent failure.
     * Kinda depressing, isn't it?
     */
    private final static JingleS5BTransportCandidate CANDIDATE_FAILURE = new JingleS5BTransportCandidate(null, null, -1, null);
}
