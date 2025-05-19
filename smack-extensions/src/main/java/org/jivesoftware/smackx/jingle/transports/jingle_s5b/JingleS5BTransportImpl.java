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
import java.util.ArrayList;
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
import org.jivesoftware.smackx.jingle.JingleUtil;
import org.jivesoftware.smackx.jingle.callbacks.JingleTransportCallback;
import org.jivesoftware.smackx.jingle.component.JingleSessionImpl;
import org.jivesoftware.smackx.jingle.component.JingleTransport;
import org.jivesoftware.smackx.jingle.component.JingleTransportCandidate;
import org.jivesoftware.smackx.jingle.element.Jingle;
import org.jivesoftware.smackx.jingle.element.JingleContentTransport;
import org.jivesoftware.smackx.jingle.element.JingleContentTransportCandidate;
import org.jivesoftware.smackx.jingle.element.JingleContentTransportInfo;
import org.jivesoftware.smackx.jingle.exception.FailedTransportException;
import org.jivesoftware.smackx.jingle.transports.jingle_s5b.elements.JingleS5BTransport;
import org.jivesoftware.smackx.jingle.transports.jingle_s5b.elements.JingleS5BTransportCandidate;
import org.jivesoftware.smackx.jingle.transports.jingle_s5b.elements.JingleS5BTransportInfo;
import org.jxmpp.jid.FullJid;

/**
 * Jingle SOCKS5Bytestream transport component.
 *
 * @author Paul Schaub
 * @author Eng Chong Meng
 */
public class JingleS5BTransportImpl extends JingleTransport<JingleS5BTransport> {

    private static final Logger LOGGER = Logger.getLogger(JingleS5BTransportImpl.class.getName());

    public static final String NAMESPACE = JingleS5BTransport.NAMESPACE_V1;

    /* timeout to connect to all SOCKS5 proxies */
    private static final int TOTAL_CONNECTION_TIMEOUT = 10 * 1000;

    private final String sid;

    private String ourDstAddr;
    private String theirDstAddr;

    private Bytestream.Mode mode;

    XMPPConnection mConnection;
    JingleS5BTransportManager mTransportManager;

    // PEERS candidate of OUR choice.
    private JingleS5BTransportCandidateImpl ourSelectedCandidate;
    private JingleS5BTransportCandidateImpl theirSelectedCandidate;

    private JingleTransportCallback callback;

    // Just for handling Unused Variable warning
    JingleS5BTransportInfo mInfo;

    /**
     * Create transport as initiator.
     * @param initiator initiator of the Jingle session.
     * @param responder responder.
     * @param sid sessionId of the Jingle session.
     * @param mode TCP/UDP.
     * @param ourCandidates our proxy candidates.
     */
    JingleS5BTransportImpl(FullJid initiator, FullJid responder, String sid, Bytestream.Mode mode, List<JingleTransportCandidate<?>> ourCandidates) {
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
    JingleS5BTransportImpl(FullJid initiator, FullJid responder, List<JingleTransportCandidate<?>> ourCandidates, JingleS5BTransportImpl other) {
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
    JingleS5BTransportImpl(String sid, Bytestream.Mode mode, String ourDstAddr, String theirDstAddr, List<JingleTransportCandidate<?>> ourCandidates, List<JingleTransportCandidate<?>> theirCandidates) {
        this.sid = sid;
        this.mode = mode;
        this.ourDstAddr = ourDstAddr;
        Socks5Proxy.getSocks5Proxy().addTransfer(ourDstAddr);
        this.theirDstAddr = theirDstAddr;

        for (JingleTransportCandidate<?> c : ourCandidates != null ? ourCandidates :
                Collections.<JingleS5BTransportCandidateImpl>emptySet()) {
            addOurCandidate(c);
        }

        for (JingleTransportCandidate<?> c : theirCandidates != null ? theirCandidates :
                Collections.<JingleS5BTransportCandidateImpl>emptySet()) {
            addTheirCandidate(c);
        }
    }

    /**
     * Copy constructor.
     * @param original which will be copied.
     */
    public JingleS5BTransportImpl(JingleS5BTransportImpl original) {
        this.sid = original.sid;
        this.mode = original.mode;
        this.ourDstAddr = original.ourDstAddr;
        this.theirDstAddr = original.theirDstAddr;

        for (JingleTransportCandidate<?> c : original.getOurCandidates()) {
            addOurCandidate(new JingleS5BTransportCandidateImpl((JingleS5BTransportCandidateImpl) c));
        }

        for (JingleTransportCandidate<?> c : original.getTheirCandidates()) {
            addTheirCandidate(new JingleS5BTransportCandidateImpl((JingleS5BTransportCandidateImpl) c));
        }
    }

    @Override
    public JingleS5BTransport getElement() {
        JingleS5BTransport.Builder builder = JingleS5BTransport.getBuilder()
                .setStreamId(sid)
                .setDestinationAddress(ourDstAddr)
                .setMode(mode);

        for (JingleTransportCandidate<?> candidate : getOurCandidates()) {
            builder.addTransportCandidate((JingleS5BTransportCandidate) candidate.getElement());
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
        JingleSessionImpl session = getParent().getParent();
        mConnection = connection;
        mTransportManager = JingleS5BTransportManager.getInstanceFor(connection);

        if (getOurDstAddr() == null) {
            ourDstAddr = Socks5Utils.createDigest(getStreamId(), session.getLocal(), session.getRemote());
            Socks5Proxy.getSocks5Proxy().addTransfer(ourDstAddr);
        }

        if (mode == null) {
            mode = Bytestream.Mode.tcp;
        }

        if (getOurCandidates().isEmpty()) {
            List<JingleTransportCandidate<?>> candidates = JingleS5BTransportManager.getInstanceFor(connection).collectCandidates();
            for (JingleTransportCandidate<?> c : candidates) {
                addOurCandidate(c);
            }
        }
    }

    @Override
    public void establishIncomingBytestreamSession(XMPPConnection connection, JingleTransportCallback callback, JingleSessionImpl session)
            throws SmackException.NotConnectedException, InterruptedException {
        LOGGER.log(Level.INFO, "Establishing incoming bytestream.");
        this.callback = callback;
        establishBytestreamSession(connection);
    }

    @Override
    public void establishOutgoingBytestreamSession(XMPPConnection connection, JingleTransportCallback callback, JingleSessionImpl session)
            throws SmackException.NotConnectedException, InterruptedException {
        LOGGER.log(Level.INFO, "Establishing outgoing bytestream.");
        this.callback = callback;
        establishBytestreamSession(connection);
    }

    @SuppressWarnings("ReferenceEquality")
    private void establishBytestreamSession(XMPPConnection connection)
            throws SmackException.NotConnectedException, InterruptedException {
        Socks5Proxy.getSocks5Proxy().addTransfer(ourDstAddr);
        this.ourSelectedCandidate = connectToCandidates(TOTAL_CONNECTION_TIMEOUT);

        if (ourSelectedCandidate == CANDIDATE_FAILURE) {
            connection.createStanzaCollectorAndSend(mTransportManager.createCandidateError(this));
            connectIfReady();
            return;
        }

        if (ourSelectedCandidate == null) {
            throw new AssertionError("MUST NOT BE NULL.");
        }

        connection.createStanzaCollectorAndSend(mTransportManager.createCandidateUsed(this, ourSelectedCandidate));
        connectIfReady();
    }

    @SuppressWarnings("ReferenceEquality")
    private JingleS5BTransportCandidateImpl connectToCandidates(int timeout) {
        // fix ConcurrentModificationException
        ArrayList<JingleTransportCandidate<?>> theirCandidates = new ArrayList<>(getTheirCandidates());
        if (theirCandidates.isEmpty()) {
            LOGGER.log(Level.INFO, "They provided 0 candidates.");
            return CANDIDATE_FAILURE;
        }

        // Use timeout for each candidate instead.
        // int _timeout = timeout / getTheirCandidates().size();
        for (JingleTransportCandidate<?> c : theirCandidates) {
            JingleS5BTransportCandidateImpl candidate = (JingleS5BTransportCandidateImpl) c;
            try {
                return candidate.connect(timeout, true);
            } catch (IOException | TimeoutException | InterruptedException | SmackException | XMPPException e) {
                LOGGER.log(Level.WARNING, "Establishing connection candidate failed: " + candidate.getCandidateId()
                        + " (" + candidate.getStreamHost() + ") " + e.getMessage());
            }
        }
        LOGGER.log(Level.WARNING, "Establishing connection candidate failed all: " + theirCandidates.size());

        // Failed to connect to any candidate.
        return CANDIDATE_FAILURE;
    }

    @SuppressWarnings("ReferenceEquality")
    private void connectIfReady() {
        final JingleSessionImpl session = getParent().getParent();

        if (ourSelectedCandidate == null || theirSelectedCandidate == null) {
            // Not yet ready if we or peer did not yet decide on a candidate.
            LOGGER.log(Level.INFO, "Not ready.");
            return;
        }

        if (ourSelectedCandidate == CANDIDATE_FAILURE && theirSelectedCandidate == CANDIDATE_FAILURE) {
            LOGGER.log(Level.INFO, "Failure.");
            callback.onTransportFailed(new FailedTransportException(null));
            return;
        }

        LOGGER.log(Level.INFO, (session.isInitiator() ? "Initiator" : "Responder") + " is ready.");

        // Determine nominated candidate.
        JingleS5BTransportCandidateImpl nominated;
        if (ourSelectedCandidate != CANDIDATE_FAILURE && theirSelectedCandidate != CANDIDATE_FAILURE) {

            if (ourSelectedCandidate.getPriority() > theirSelectedCandidate.getPriority()) {
                nominated = ourSelectedCandidate;
            }
            else if (ourSelectedCandidate.getPriority() < theirSelectedCandidate.getPriority()) {
                nominated = theirSelectedCandidate;
            }
            else {
                nominated = getParent().getParent().isInitiator() ? ourSelectedCandidate : theirSelectedCandidate;
            }

        }
        else if (ourSelectedCandidate != CANDIDATE_FAILURE) {
            nominated = ourSelectedCandidate;
        }
        else {
            nominated = theirSelectedCandidate;
        }

        boolean isProxy = nominated.getType() == JingleS5BTransportCandidate.Type.proxy;
        if (nominated == theirSelectedCandidate) {
            LOGGER.log(Level.INFO, "Their choice, so our proposed candidate is used.");

            try {
                nominated = nominated.connect(TOTAL_CONNECTION_TIMEOUT, false);
            } catch (InterruptedException | IOException | XMPPException | SmackException | TimeoutException e) {
                LOGGER.log(Level.INFO, "Could not connect to our candidate.", e);

                Async.go(() -> {
                    try {
                        mConnection.createStanzaCollectorAndSend(mTransportManager.createProxyError(JingleS5BTransportImpl.this));
                    } catch (SmackException.NotConnectedException | InterruptedException e1) {
                        LOGGER.log(Level.SEVERE, "Could not send proxy error: " + e, e);
                    }
                });

                callback.onTransportFailed(new S5BTransportException.CandidateError(e));
                return;
            }

            if (isProxy) {
                LOGGER.log(Level.INFO, "Send candidate activated.");
                Jingle candidateActivate = mTransportManager.createCandidateActivated((JingleS5BTransportImpl) nominated.getParent(), nominated);

                try {
                    mConnection.createStanzaCollectorAndSend(candidateActivate).nextResultOrThrow();
                } catch (InterruptedException | XMPPException.XMPPErrorException |
                         SmackException.NotConnectedException | SmackException.NoResponseException e) {
                    LOGGER.log(Level.WARNING, "Could not send candidate activated", e);
                    callback.onTransportFailed(new S5BTransportException.ProxyError(e));
                    return;
                }
            }

            LOGGER.log(Level.INFO, "Start transmission on " + nominated.getCandidateId());
            mBytestreamSession = new Socks5BytestreamSession(nominated.getSocket(), !isProxy);
            callback.onTransportReady(mBytestreamSession);
        }
        // Our choice
        else {
            LOGGER.log(Level.INFO, "Our choice, so their candidate is used.");
            if (!isProxy) {
                LOGGER.log(Level.INFO, "Start transmission on " + nominated.getCandidateId());
                mBytestreamSession = new Socks5BytestreamSession(nominated.getSocket(), true);
                callback.onTransportReady(mBytestreamSession);
            }
            else {
                LOGGER.log(Level.INFO, "Our choice was their external proxy. wait for candidate activated.");
            }
        }
    }

    @Override
    public void handleSessionAccept(JingleContentTransport transportElement, XMPPConnection connection) {
        JingleS5BTransport transport = (JingleS5BTransport) transportElement;
        theirDstAddr = transport.getDestinationAddress();
        for (JingleContentTransportCandidate c : transport.getCandidates()) {
            JingleS5BTransportCandidate candidate = (JingleS5BTransportCandidate) c;
            addTheirCandidate(new JingleS5BTransportCandidateImpl(candidate));
        }
    }

    @Override
    public IQ handleTransportInfo(JingleContentTransportInfo info, Jingle wrapping) {
        if (info != null) {
            switch (info.getElementName()) {
                case JingleS5BTransportInfo.CandidateUsed.ELEMENT:
                    handleCandidateUsed((JingleS5BTransportInfo) info, wrapping);
                    break;

                case JingleS5BTransportInfo.CandidateActivated.ELEMENT:
                    handleCandidateActivate((JingleS5BTransportInfo) info);
                    break;

                case JingleS5BTransportInfo.CandidateError.ELEMENT:
                    handleCandidateError((JingleS5BTransportInfo) info);
                    break;

                case JingleS5BTransportInfo.ProxyError.ELEMENT:
                    handleProxyError((JingleS5BTransportInfo) info);
                    break;

                default:
                    throw new AssertionError("Unknown transport-info element: " + info.getElementName());
            }
        }
        return IQ.createResultIQ(wrapping);
    }

    private void handleCandidateUsed(JingleS5BTransportInfo info, Jingle wrapping) {
        String candidateId = ((JingleS5BTransportInfo.CandidateUsed) info).getCandidateId();

        // Received second candidate-used -> out-of-order!
        if (theirSelectedCandidate != null) {
            try {
                new JingleUtil(mConnection).sendErrorOutOfOrder(wrapping);
            } catch (SmackException.NotConnectedException | InterruptedException e) {
                LOGGER.log(Level.SEVERE, "Could not respond to candidate-used transport-info: " + e, e);
            }
            return;
        }

        for (JingleTransportCandidate<?> jingleTransportCandidate : getOurCandidates()) {
            JingleS5BTransportCandidateImpl candidate = (JingleS5BTransportCandidateImpl) jingleTransportCandidate;
            if (candidate.getCandidateId().equals(candidateId)) {
                theirSelectedCandidate = candidate;
                break;
            }
        }

        if (theirSelectedCandidate == null) {
            LOGGER.severe("ILLEGAL CANDIDATE ID!!!");
            // TODO: Alert! Illegal candidateId!
        }
        connectIfReady();
    }

    private void handleCandidateActivate(JingleS5BTransportInfo info) {
        mInfo = info;
        this.mBytestreamSession = new Socks5BytestreamSession(ourSelectedCandidate.getSocket(),
                ourSelectedCandidate.getStreamHost().getJID().asBareJid().equals(getParent().getParent().getRemote().asBareJid()));
        callback.onTransportReady(mBytestreamSession);
    }

    private void handleCandidateError(JingleS5BTransportInfo info) {
        mInfo = info;
        theirSelectedCandidate = CANDIDATE_FAILURE;
        connectIfReady();
    }

    private void handleProxyError(JingleS5BTransportInfo info) {
        mInfo = info;
        callback.onTransportFailed(new S5BTransportException.ProxyError(null));
    }

    /**
     * Internal dummy candidate used to represent failure.
     * Kinda depressing, isn't it?
     */
    private static final JingleS5BTransportCandidateImpl CANDIDATE_FAILURE = new JingleS5BTransportCandidateImpl(null, null, -1, null);
}
