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
package org.jivesoftware.smackx.jingle.transports.jingle_s5b;

import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;

import org.jivesoftware.smackx.bytestreams.socks5.Socks5BytestreamSession;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5Client;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5ClientForInitiator;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5Proxy;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5Utils;
import org.jivesoftware.smackx.bytestreams.socks5.packet.Bytestream;
import org.jivesoftware.smackx.jingle.JingleManager;
import org.jivesoftware.smackx.jingle.JingleSession;
import org.jivesoftware.smackx.jingle.element.Jingle;
import org.jivesoftware.smackx.jingle.element.JingleContent;
import org.jivesoftware.smackx.jingle.element.JingleContentTransport;
import org.jivesoftware.smackx.jingle.element.JingleContentTransportCandidate;
import org.jivesoftware.smackx.jingle.transports.JingleTransportInitiationCallback;
import org.jivesoftware.smackx.jingle.transports.JingleTransportSession;
import org.jivesoftware.smackx.jingle.transports.jingle_s5b.elements.JingleS5BTransport;
import org.jivesoftware.smackx.jingle.transports.jingle_s5b.elements.JingleS5BTransportCandidate;
import org.jivesoftware.smackx.jingle.transports.jingle_s5b.elements.JingleS5BTransportInfo;

/**
 * Handler that handles Jingle Socks5Bytestream transports (XEP-0260).
 */
public class JingleS5BTransportSession extends JingleTransportSession<JingleS5BTransport> {
    private static final Logger LOGGER = Logger.getLogger(JingleS5BTransportSession.class.getName());

    private JingleTransportInitiationCallback callback;

    public JingleS5BTransportSession(JingleSession jingleSession) {
        super(jingleSession);
    }

    private UsedCandidate ourChoice, theirChoice;

    @Override
    public JingleS5BTransport createTransport() {
        if (ourProposal == null) {
            ourProposal = createTransport(JingleManager.randomId(), Bytestream.Mode.tcp);
        }
        return ourProposal;
    }

    @Override
    public void setTheirProposal(JingleContentTransport transport) {
        theirProposal = (JingleS5BTransport) transport;
    }

    public JingleS5BTransport createTransport(String sid, Bytestream.Mode mode) {
        JingleS5BTransport.Builder jb = JingleS5BTransport.getBuilder()
                .setStreamId(sid).setMode(mode).setDestinationAddress(
                        Socks5Utils.createDigest(sid, jingleSession.getLocal(), jingleSession.getRemote()));

        // Local host
        if (JingleS5BTransportManager.isUseLocalCandidates()) {
            for (Bytestream.StreamHost host : transportManager().getLocalStreamHosts()) {
                jb.addTransportCandidate(new JingleS5BTransportCandidate(host, 100, JingleS5BTransportCandidate.Type.direct));
            }
        }

        List<Bytestream.StreamHost> remoteHosts = Collections.emptyList();
        if (JingleS5BTransportManager.isUseExternalCandidates()) {
            try {
                remoteHosts = transportManager().getAvailableStreamHosts();
            } catch (InterruptedException | XMPPException.XMPPErrorException | SmackException.NotConnectedException | SmackException.NoResponseException e) {
                LOGGER.log(Level.WARNING, "Could not determine available StreamHosts.", e);
            }
        }

        for (Bytestream.StreamHost host : remoteHosts) {
            jb.addTransportCandidate(new JingleS5BTransportCandidate(host, 0, JingleS5BTransportCandidate.Type.proxy));
        }

        return jb.build();
    }

    public void setTheirTransport(JingleContentTransport transport) {
        theirProposal = (JingleS5BTransport) transport;
    }

    @Override
    public void initiateOutgoingSession(JingleTransportInitiationCallback callback) {
        this.callback = callback;
        initiateSession();
    }

    @Override
    public void initiateIncomingSession(JingleTransportInitiationCallback callback) {
        this.callback = callback;
        initiateSession();
    }

    private void initiateSession() {
        Socks5Proxy.getSocks5Proxy().addTransfer(createTransport().getDestinationAddress());
        JingleContent content = jingleSession.getContents().get(0);
        UsedCandidate usedCandidate = chooseFromProposedCandidates(theirProposal);
        if (usedCandidate == null) {
            ourChoice = CANDIDATE_FAILURE;
            Jingle candidateError = transportManager().createCandidateError(
                    jingleSession.getRemote(), jingleSession.getInitiator(), jingleSession.getSessionId(),
                    content.getSenders(), content.getCreator(), content.getName(), theirProposal.getStreamId());
            try {
                jingleSession.getConnection().sendStanza(candidateError);
            } catch (SmackException.NotConnectedException | InterruptedException e) {
                LOGGER.log(Level.WARNING, "Could not send candidate-error.", e);
            }
        } else {
            ourChoice = usedCandidate;
            Jingle jingle = transportManager().createCandidateUsed(jingleSession.getRemote(), jingleSession.getInitiator(), jingleSession.getSessionId(),
                    content.getSenders(), content.getCreator(), content.getName(), theirProposal.getStreamId(), ourChoice.candidate.getCandidateId());
            try {
                jingleSession.getConnection().sendIqRequestAndWaitForResponse(jingle);
            } catch (InterruptedException | XMPPException.XMPPErrorException | SmackException.NotConnectedException | SmackException.NoResponseException e) {
                LOGGER.log(Level.WARNING, "Could not send candidate-used.", e);
            }
        }
        connectIfReady();
    }

    private UsedCandidate chooseFromProposedCandidates(JingleS5BTransport proposal) {
        for (JingleContentTransportCandidate c : proposal.getCandidates()) {
            JingleS5BTransportCandidate candidate = (JingleS5BTransportCandidate) c;

            try {
                return connectToTheirCandidate(candidate);
            } catch (InterruptedException | TimeoutException | XMPPException | SmackException | IOException e) {
                LOGGER.log(Level.WARNING, "Could not connect to " + candidate.getHost(), e);
            }
        }
        LOGGER.log(Level.WARNING, "Failed to connect to any candidate.");
        return null;
    }

    private UsedCandidate connectToTheirCandidate(JingleS5BTransportCandidate candidate)
            throws InterruptedException, TimeoutException, SmackException, XMPPException, IOException {
        Bytestream.StreamHost streamHost = candidate.getStreamHost();
        InetAddress address = streamHost.getAddress().asInetAddress();
        Socks5Client socks5Client = new Socks5Client(streamHost, theirProposal.getDestinationAddress());
        Socket socket = socks5Client.getSocket(10 * 1000);
        LOGGER.log(Level.INFO, "Connected to their StreamHost " + address + " using dstAddr "
                + theirProposal.getDestinationAddress());
        return new UsedCandidate(theirProposal, candidate, socket);
    }

    private UsedCandidate connectToOurCandidate(JingleS5BTransportCandidate candidate)
            throws InterruptedException, TimeoutException, SmackException, XMPPException, IOException {
        Bytestream.StreamHost streamHost = candidate.getStreamHost();
        InetAddress address = streamHost.getAddress().asInetAddress();
        Socks5ClientForInitiator socks5Client = new Socks5ClientForInitiator(
                streamHost, ourProposal.getDestinationAddress(), jingleSession.getConnection(),
                ourProposal.getStreamId(), jingleSession.getRemote());
        Socket socket = socks5Client.getSocket(10 * 1000);
        LOGGER.log(Level.INFO, "Connected to our StreamHost " + address + " using dstAddr "
                + ourProposal.getDestinationAddress());
        return new UsedCandidate(ourProposal, candidate, socket);
    }

    @Override
    public String getNamespace() {
        return JingleS5BTransport.NAMESPACE_V1;
    }

    @Override
    public IQ handleTransportInfo(Jingle transportInfo) {
        JingleS5BTransportInfo info = (JingleS5BTransportInfo) transportInfo.getContents().get(0).getTransport().getInfo();

        switch (info.getElementName()) {
            case JingleS5BTransportInfo.CandidateUsed.ELEMENT:
                return handleCandidateUsed(transportInfo);

            case JingleS5BTransportInfo.CandidateActivated.ELEMENT:
                return handleCandidateActivate(transportInfo);

            case JingleS5BTransportInfo.CandidateError.ELEMENT:
                return handleCandidateError(transportInfo);

            case JingleS5BTransportInfo.ProxyError.ELEMENT:
                return handleProxyError(transportInfo);
        }
        // We should never go here, but lets be gracious...
        return IQ.createResultIQ(transportInfo);
    }

    public IQ handleCandidateUsed(Jingle jingle) {
        JingleS5BTransportInfo info = (JingleS5BTransportInfo) jingle.getContents().get(0).getTransport().getInfo();
        String candidateId = ((JingleS5BTransportInfo.CandidateUsed) info).getCandidateId();
        theirChoice = new UsedCandidate(ourProposal, ourProposal.getCandidate(candidateId), null);

        if (theirChoice.candidate == null) {
            /*
            TODO: Booooooh illegal candidateId!! Go home!!!!11elf
             */
        }

        connectIfReady();

        return IQ.createResultIQ(jingle);
    }

    public IQ handleCandidateActivate(Jingle jingle) {
        LOGGER.log(Level.INFO, "handleCandidateActivate");
        Socks5BytestreamSession bs = new Socks5BytestreamSession(ourChoice.socket,
                ourChoice.candidate.getJid().asBareJid().equals(jingleSession.getRemote().asBareJid()));
        callback.onSessionInitiated(bs);
        return IQ.createResultIQ(jingle);
    }

    public IQ handleCandidateError(Jingle jingle) {
        theirChoice = CANDIDATE_FAILURE;
        connectIfReady();
        return IQ.createResultIQ(jingle);
    }

    public IQ handleProxyError(Jingle jingle) {
        // TODO
        return IQ.createResultIQ(jingle);
    }

    /**
     * Determine, which candidate (ours/theirs) is the nominated one.
     * Connect to this candidate. If it is a proxy and it is ours, activate it and connect.
     * If its a proxy and it is theirs, wait for activation.
     * If it is not a proxy, just connect.
     */
    private void connectIfReady() {
        JingleContent content = jingleSession.getContents().get(0);
        if (ourChoice == null || theirChoice == null) {
            // Not yet ready.
            LOGGER.log(Level.INFO, "Not ready.");
            return;
        }

        if (ourChoice == CANDIDATE_FAILURE && theirChoice == CANDIDATE_FAILURE) {
            LOGGER.log(Level.INFO, "Failure.");
            jingleSession.onTransportMethodFailed(getNamespace());
            return;
        }

        LOGGER.log(Level.INFO, "Ready.");

        // Determine nominated candidate.
        UsedCandidate nominated;
        if (ourChoice != CANDIDATE_FAILURE && theirChoice != CANDIDATE_FAILURE) {
            if (ourChoice.candidate.getPriority() > theirChoice.candidate.getPriority()) {
                nominated = ourChoice;
            } else if (ourChoice.candidate.getPriority() < theirChoice.candidate.getPriority()) {
                nominated = theirChoice;
            } else {
                nominated = jingleSession.isInitiator() ? ourChoice : theirChoice;
            }
        } else if (ourChoice != CANDIDATE_FAILURE) {
            nominated = ourChoice;
        } else {
            nominated = theirChoice;
        }

        if (nominated == theirChoice) {
            LOGGER.log(Level.INFO, "Their choice, so our proposed candidate is used.");
            boolean isProxy = nominated.candidate.getType() == JingleS5BTransportCandidate.Type.proxy;
            try {
                nominated = connectToOurCandidate(nominated.candidate);
            } catch (InterruptedException | IOException | XMPPException | SmackException | TimeoutException e) {
                LOGGER.log(Level.INFO, "Could not connect to our candidate.", e);
                // TODO: Proxy-Error
                return;
            }

            if (isProxy) {
                LOGGER.log(Level.INFO, "Is external proxy. Activate it.");
                Bytestream activate = new Bytestream(ourProposal.getStreamId());
                activate.setMode(null);
                activate.setType(IQ.Type.set);
                activate.setTo(nominated.candidate.getJid());
                activate.setToActivate(jingleSession.getRemote());
                activate.setFrom(jingleSession.getLocal());
                try {
                    jingleSession.getConnection().sendIqRequestAndWaitForResponse(activate);
                } catch (InterruptedException | XMPPException.XMPPErrorException | SmackException.NotConnectedException | SmackException.NoResponseException e) {
                    LOGGER.log(Level.WARNING, "Could not activate proxy.", e);
                    return;
                }

                LOGGER.log(Level.INFO, "Send candidate-activate.");
                Jingle candidateActivate = transportManager().createCandidateActivated(
                        jingleSession.getRemote(), jingleSession.getInitiator(), jingleSession.getSessionId(),
                        content.getSenders(), content.getCreator(), content.getName(), nominated.transport.getStreamId(),
                        nominated.candidate.getCandidateId());
                try {
                    jingleSession.getConnection().sendIqRequestAndWaitForResponse(candidateActivate);
                } catch (InterruptedException | XMPPException.XMPPErrorException | SmackException.NotConnectedException | SmackException.NoResponseException e) {
                    LOGGER.log(Level.WARNING, "Could not send candidate-activated", e);
                    return;
                }
            }

            LOGGER.log(Level.INFO, "Start transmission.");
            Socks5BytestreamSession bs = new Socks5BytestreamSession(nominated.socket, !isProxy);
            callback.onSessionInitiated(bs);

        }
        // Our choice
        else {
            LOGGER.log(Level.INFO, "Our choice, so their candidate was used.");
            boolean isProxy = nominated.candidate.getType() == JingleS5BTransportCandidate.Type.proxy;
            if (!isProxy) {
                LOGGER.log(Level.INFO, "Direct connection.");
                Socks5BytestreamSession bs = new Socks5BytestreamSession(nominated.socket, true);
                callback.onSessionInitiated(bs);
            } else {
                LOGGER.log(Level.INFO, "Our choice was their external proxy. wait for candidate-activate.");
            }
        }
    }

    @Override
    public JingleS5BTransportManager transportManager() {
        return JingleS5BTransportManager.getInstanceFor(jingleSession.getConnection());
    }

    private static final class UsedCandidate {
        private final Socket socket;
        private final JingleS5BTransport transport;
        private final JingleS5BTransportCandidate candidate;

        private UsedCandidate(JingleS5BTransport transport, JingleS5BTransportCandidate candidate, Socket socket) {
            this.socket = socket;
            this.transport = transport;
            this.candidate = candidate;
        }
    }

    private static final UsedCandidate CANDIDATE_FAILURE = new UsedCandidate(null, null, null);
}
