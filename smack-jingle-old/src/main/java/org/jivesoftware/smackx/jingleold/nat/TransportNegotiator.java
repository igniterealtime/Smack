/**
 *
 * Copyright 2003-2006 Jive Software.
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

package org.jivesoftware.smackx.jingleold.nat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.packet.IQ;

import org.jivesoftware.smackx.jingleold.ContentNegotiator;
import org.jivesoftware.smackx.jingleold.JingleActionEnum;
import org.jivesoftware.smackx.jingleold.JingleException;
import org.jivesoftware.smackx.jingleold.JingleNegotiator;
import org.jivesoftware.smackx.jingleold.JingleNegotiatorState;
import org.jivesoftware.smackx.jingleold.JingleSession;
import org.jivesoftware.smackx.jingleold.listeners.JingleListener;
import org.jivesoftware.smackx.jingleold.listeners.JingleTransportListener;
import org.jivesoftware.smackx.jingleold.packet.Jingle;
import org.jivesoftware.smackx.jingleold.packet.JingleContent;
import org.jivesoftware.smackx.jingleold.packet.JingleTransport;
import org.jivesoftware.smackx.jingleold.packet.JingleTransport.JingleTransportCandidate;

/**
 * Transport negotiator.
 *
 * This class is responsible for managing the transport negotiation process,
 * handling all the stanza interchange and the stage control.
 *
 * @author Alvaro Saurin
 */
@SuppressWarnings("UnusedVariable")
public abstract class TransportNegotiator extends JingleNegotiator {

    private static final Logger LOGGER = Logger.getLogger(TransportNegotiator.class.getName());

    // The time we give to the candidates check before we accept or decline the
    // transport (in milliseconds)
    public static final int CANDIDATES_ACCEPT_PERIOD = 4000;

    // The session this negotiator belongs to
    // private final JingleSession session;

    // The transport manager
    private final TransportResolver resolver;

    // Transport candidates we have offered
    private final List<TransportCandidate> offeredCandidates = new ArrayList<>();

    // List of remote transport candidates
    private final List<TransportCandidate> remoteCandidates = new ArrayList<>();

    // Valid remote candidates
    private final List<TransportCandidate> validRemoteCandidates = new ArrayList<>();

    // Accepted Remote Candidates
    private final List<TransportCandidate> acceptedRemoteCandidates = new ArrayList<>();

    // The best local candidate we have offered (and accepted by the other part)
    private TransportCandidate acceptedLocalCandidate;

    // The thread that will report the result to the other end
    private Thread resultThread;

    // Listener for the resolver
    private TransportResolverListener.Resolver resolverListener;

    private final ContentNegotiator parentNegotiator;

    /**
    * Default constructor.
    *
    * @param session            The Jingle session
    * @param transResolver The JingleTransportManager to use
    * @param parentNegotiator the parent ngeotiator.
    */
    public TransportNegotiator(JingleSession session, TransportResolver transResolver, ContentNegotiator parentNegotiator) {
        super(session);

        resolver = transResolver;
        this.parentNegotiator = parentNegotiator;

        resultThread = null;
    }

    /**
     * Get a new instance of the right TransportNegotiator class with this
     * candidate.
     *
     * @param cand the transport candidate.
     * @return A TransportNegotiator instance
     */
    public abstract JingleTransport getJingleTransport(TransportCandidate cand);

    /**
     * Return true if the transport candidate is acceptable for the current
     * negotiator.
     *
     * @param tc the transport candidate.
     * @param localCandidates a list of local transport candidates.
     * @return true if the transport candidate is acceptable
     */
    public abstract boolean acceptableTransportCandidate(TransportCandidate tc, List<TransportCandidate> localCandidates);

    /**
     * Obtain the best local candidate we want to offer.
     *
     * @return the best local candidate
     */
    public final TransportCandidate getBestLocalCandidate() {
        return resolver.getPreferredCandidate();
    }

    /**
     * Set the best local transport candidate we have offered and accepted by
     * the other endpoint.
     *
     * @param bestLocalCandidate the acceptedLocalCandidate to set
     */
    private void setAcceptedLocalCandidate(TransportCandidate bestLocalCandidate) {
        for (int i = 0; i < resolver.getCandidateCount(); i++) {
            // TODO FIX The EQUAL Sentence
            if (resolver.getCandidate(i).getIp().equals(bestLocalCandidate.getIp())
                    && resolver.getCandidate(i).getPort() == bestLocalCandidate.getPort()) {
                acceptedLocalCandidate = resolver.getCandidate(i);
                return;
            }
        }
        LOGGER.fine("BEST: ip=" + bestLocalCandidate.getIp() + " port=" + bestLocalCandidate.getPort() + " has not been offered.");
        // throw new XMPPException("Local transport candidate has not be offered.");
    }

    /**
     * Get the best accepted local candidate we have offered.
     *
     * @return a transport candidate we have offered.
     */
    public TransportCandidate getAcceptedLocalCandidate() {
        return acceptedLocalCandidate;
    }

    /**
     *  Called from above to start the negotiator during a session-initiate.
     */
    @Override
    protected void doStart() {

        try {
            sendTransportCandidatesOffer();
            setNegotiatorState(JingleNegotiatorState.PENDING);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            LOGGER.log(Level.WARNING, "exception", e);
        }

    }

    /**
     * Called from above to session-terminate.
     */
    @Override
    public void close() {
        super.close();

    }

    /**
     *  Return a JingleTransport that best reflects this transport negotiator.
     *
     *  @return the jingle transport.
     */
    public JingleTransport getJingleTransport() {
        return getJingleTransport(getBestRemoteCandidate());
    }

    public List<TransportCandidate> getOfferedCandidates() {
        return offeredCandidates;
    }

    /**
     * Obtain the best common transport candidate obtained in the negotiation.
     *
     * @return the bestRemoteCandidate
     */
    public abstract TransportCandidate getBestRemoteCandidate();

    /**
     * Get the list of remote candidates.
     *
     * @return the remoteCandidates
     */
    private List<TransportCandidate> getRemoteCandidates() {
        return remoteCandidates;
    }

    /**
     * Add a remote candidate to the list. The candidate will be checked in
     * order to verify if it is usable.
     *
     * @param rc a remote candidate to add and check.
     */
    private void addRemoteCandidate(TransportCandidate rc) {
        // Add the candidate to the list
        if (rc != null) {
            if (acceptableTransportCandidate(rc, offeredCandidates)) {
                synchronized (remoteCandidates) {
                    remoteCandidates.add(rc);
                }

                // Check if the new candidate can be used.
                checkRemoteCandidate(rc);
            }
        }
    }

    /**
     * Add a offered candidate to the list.
     *
     * @param rc a remote candidate we have offered.
     */
    private void addOfferedCandidate(TransportCandidate rc) {
        // Add the candidate to the list
        if (rc != null) {
            synchronized (offeredCandidates) {
                offeredCandidates.add(rc);
            }
        }
    }

    /**
     * Check asynchronously the new transport candidate.
     *
     * @param offeredCandidate a transport candidates to check
     */
    private void checkRemoteCandidate(final TransportCandidate offeredCandidate) {
        offeredCandidate.addListener(new TransportResolverListener.Checker() {
            @Override
            public void candidateChecked(TransportCandidate cand, final boolean validCandidate) {
                if (validCandidate) {
                    if (getNegotiatorState() == JingleNegotiatorState.PENDING)
                        addValidRemoteCandidate(offeredCandidate);
                }
            }

            @Override
            public void candidateChecking(TransportCandidate cand) {
            }

        });
        offeredCandidate.check(resolver.getCandidatesList());
    }

    /**
     * Return true if the transport is established.
     *
     * @return true if the transport is established.
     */
    private boolean isEstablished() {
        return getBestRemoteCandidate() != null && getAcceptedLocalCandidate() != null;
    }

    /**
     * Return true if the transport is fully established.
     *
     * @return true if the transport is fully established.
     */
    public final boolean isFullyEstablished() {
        return isEstablished() && ((getNegotiatorState() == JingleNegotiatorState.SUCCEEDED) || (getNegotiatorState() == JingleNegotiatorState.FAILED));
    }

    /**
     * Launch a thread that checks, after some time, if any of the candidates
     * offered by the other endpoint is usable. The thread does not check the
     * candidates: it just checks if we have got a valid one and sends an Accept
     * in that case.
     */
    private void delayedCheckBestCandidate(final JingleSession js, final Jingle jin) {
        //
        // If this is the first insertion in the list, start the thread that
        // will send the result of our checks...
        //
        if (resultThread == null && !getRemoteCandidates().isEmpty()) {
            resultThread = new Thread(new Runnable() {

                @Override
                public void run() {

                    // Sleep for some time, waiting for the candidates checks

                    int totalTime = CANDIDATES_ACCEPT_PERIOD + TransportResolver.CHECK_TIMEOUT;
                    int tries = (int) Math.ceil(totalTime / 1000);

                    for (int i = 0; i < tries - 1; i++) {
                        try {
                            Thread.sleep(1000);
                        } catch (InterruptedException e) {
                            LOGGER.log(Level.WARNING, "exception", e);
                        }

                        // Once we are in pending state, look for any valid remote
                        // candidate, and send an "accept" if we have one...
                        TransportCandidate bestRemote = getBestRemoteCandidate();
                        // State state = getState();

                        if (bestRemote != null
                                && getNegotiatorState() == JingleNegotiatorState.PENDING) {
                            // Accepting the remote candidate
                            if (!acceptedRemoteCandidates.contains(bestRemote)) {
                                Jingle jout = new Jingle(JingleActionEnum.CONTENT_ACCEPT);
                                JingleContent content = parentNegotiator.getJingleContent();
                                content.addJingleTransport(getJingleTransport(bestRemote));
                                jout.addContent(content);

                                // Send the packet
                                try {
                                    js.sendFormattedJingle(jin, jout);
                                }
                                catch (InterruptedException | NotConnectedException e) {
                                    throw new IllegalStateException(e);
                                }
                                acceptedRemoteCandidates.add(bestRemote);
                            }
                            if (isEstablished() && getNegotiatorState() == JingleNegotiatorState.PENDING) {
                                setNegotiatorState(JingleNegotiatorState.SUCCEEDED);
                                try {
                                    triggerTransportEstablished(getAcceptedLocalCandidate(), bestRemote);
                                }
                                catch (InterruptedException | NotConnectedException | NoResponseException | XMPPErrorException e) {
                                    throw new IllegalStateException(e);
                                }
                                break;
                            }
                        }
                    }

                    // Once we are in pending state, look for any valid remote
                    // candidate, and send an "accept" if we have one...
                    TransportCandidate bestRemote = getBestRemoteCandidate();

                    if (bestRemote == null) {
                        boolean foundRemoteRelay = false;
                        for (TransportCandidate candidate : remoteCandidates) {
                            if (candidate instanceof ICECandidate) {
                                ICECandidate iceCandidate = (ICECandidate) candidate;
                                if (iceCandidate.getType().equals(ICECandidate.Type.relay)) {
                                    // TODO Check if the relay is reachable.
                                    addValidRemoteCandidate(iceCandidate);
                                    foundRemoteRelay = true;
                                }
                            }
                        }

                        // If not found, check if we offered a relay. If yes, we should accept any remote candidate.
                        // We should accept the Public One if we received it, otherwise, accepts any.
                        if (!foundRemoteRelay) {
                            boolean foundLocalRelay = false;
                            for (TransportCandidate candidate : offeredCandidates) {
                                if (candidate instanceof ICECandidate) {
                                    ICECandidate iceCandidate = (ICECandidate) candidate;
                                    if (iceCandidate.getType().equals(ICECandidate.Type.relay)) {
                                        foundLocalRelay = true;
                                    }
                                }
                            }
                            if (foundLocalRelay) {
                                boolean foundRemotePublic = false;
                                for (TransportCandidate candidate : remoteCandidates) {
                                    if (candidate instanceof ICECandidate) {
                                        ICECandidate iceCandidate = (ICECandidate) candidate;
                                        if (iceCandidate.getType().equals(ICECandidate.Type.srflx)) {
                                            addValidRemoteCandidate(iceCandidate);
                                            foundRemotePublic = true;
                                        }
                                    }
                                }
                                if (!foundRemotePublic) {
                                    for (TransportCandidate candidate : remoteCandidates) {
                                        if (candidate instanceof ICECandidate) {
                                            ICECandidate iceCandidate = (ICECandidate) candidate;
                                            addValidRemoteCandidate(iceCandidate);
                                        }
                                    }
                                }
                            }
                        }
                    }

                    for (int i = 0; i < 6; i++) {
                        try {
                            Thread.sleep(500);
                        } catch (InterruptedException e) {
                            LOGGER.log(Level.WARNING, "exception", e);
                        }

                        bestRemote = getBestRemoteCandidate();
                        // State state = getState();
                        if (bestRemote != null
                                && getNegotiatorState() == JingleNegotiatorState.PENDING) {
                            if (!acceptedRemoteCandidates.contains(bestRemote)) {
                                Jingle jout = new Jingle(JingleActionEnum.CONTENT_ACCEPT);
                                JingleContent content = parentNegotiator.getJingleContent();
                                content.addJingleTransport(getJingleTransport(bestRemote));
                                jout.addContent(content);

                                // Send the packet
                                try {
                                    js.sendFormattedJingle(jin, jout);
                                }
                                catch (InterruptedException | NotConnectedException e) {
                                    throw new IllegalStateException(e);
                                }
                                acceptedRemoteCandidates.add(bestRemote);
                            }
                            if (isEstablished()) {
                                setNegotiatorState(JingleNegotiatorState.SUCCEEDED);
                                break;
                            }
                        }
                    }

                    if (getNegotiatorState() != JingleNegotiatorState.SUCCEEDED) {
                        try {
                            session
                                    .terminate("Unable to negotiate session. This may be caused by firewall configuration problems.");
                        } catch (Exception e) {
                            LOGGER.log(Level.WARNING, "exception", e);
                        }
                    }
                }
            }, "Waiting for all the transport candidates checks...");

            resultThread.setName("Transport Resolver Result");
            resultThread.start();
        }
    }

    /**
     * Add a valid remote candidate to the list. The remote candidate has been
     * checked, and the remote
     *
     * @param remoteCandidate a remote candidate to add
     */
    private void addValidRemoteCandidate(TransportCandidate remoteCandidate) {
        // Add the candidate to the list
        if (remoteCandidate != null) {
            synchronized (validRemoteCandidates) {
                LOGGER.fine("Added valid candidate: " + remoteCandidate.getIp() + ":" + remoteCandidate.getPort());
                validRemoteCandidates.add(remoteCandidate);
            }
        }
    }

    /**
     * Get the list of valid (ie, checked) remote candidates.
     *
     * @return The list of valid (ie, already checked) remote candidates.
     */
    final ArrayList<TransportCandidate> getValidRemoteCandidatesList() {
        synchronized (validRemoteCandidates) {
            return new ArrayList<>(validRemoteCandidates);
        }
    }

    /**
     * Get an iterator for the list of valid (ie, checked) remote candidates.
     *
     * @return The iterator for the list of valid (ie, already checked) remote
     *         candidates.
     */
    public final Iterator<TransportCandidate> getValidRemoteCandidates() {
        return Collections.unmodifiableList(getRemoteCandidates()).iterator();
    }

    /**
     * Add an offered remote candidate. The transport candidate can be unusable:
     * we must check if we can use it.
     *
     * @param rc the remote candidate to add.
     */
    private void addRemoteCandidates(List<TransportCandidate> rc) {
        if (rc != null) {
            if (rc.size() > 0) {
                for (TransportCandidate aRc : rc) {
                    addRemoteCandidate(aRc);
                }
            }
        }
    }

    /**
     * Parse the list of transport candidates from a Jingle packet.
     *
     * @param jingle The input jingle packet
     */
    private List<TransportCandidate> obtainCandidatesList(Jingle jingle) {
        List<TransportCandidate> result = new ArrayList<>();

        if (jingle != null) {
            // Get the list of candidates from the packet
            for (JingleContent jingleContent : jingle.getContentsList()) {
                if (jingleContent.getName().equals(parentNegotiator.getName())) {
                    for (JingleTransport jingleTransport : jingleContent.getJingleTransportsList()) {
                        for (JingleTransportCandidate jingleTransportCandidate : jingleTransport.getCandidatesList()) {
                            TransportCandidate transCand = jingleTransportCandidate.getMediaTransport();
                            result.add(transCand);
                        }
                    }
                }
            }
        }

        return result;
    }

    /**
     * Send an offer for a transport candidate
     *
     * @param cand TODO javadoc me please
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    private synchronized void sendTransportCandidateOffer(TransportCandidate cand) throws NotConnectedException, InterruptedException {
        if (!cand.isNull()) {
            // Offer our new candidate...
            addOfferedCandidate(cand);
            JingleContent content = parentNegotiator.getJingleContent();
            content.addJingleTransport(getJingleTransport(cand));
            Jingle jingle = new Jingle(JingleActionEnum.TRANSPORT_INFO);
            jingle.addContent(content);

            // We SHOULD NOT be sending packets directly.
            // This circumvents the state machinery.
            // TODO - work this into the state machinery.
            session.sendFormattedJingle(jingle);
        }
    }

    /**
     * Create a Jingle stanza where we announce our transport candidates.
     *
     * @throws XMPPException if an XMPP protocol error was received.
     * @throws SmackException if Smack detected an exceptional situation.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    private void sendTransportCandidatesOffer() throws XMPPException, SmackException, InterruptedException {
        List<TransportCandidate> notOffered = resolver.getCandidatesList();

        notOffered.removeAll(offeredCandidates);

        // Send any unset candidate
        for (Object aNotOffered : notOffered) {
            sendTransportCandidateOffer((TransportCandidate) aNotOffered);
        }

        // .. and start a listener that will send any future candidate
        if (resolverListener == null) {
            // Add a listener that sends the offer when the resolver finishes...
            resolverListener = new TransportResolverListener.Resolver() {
                @Override
                public void candidateAdded(TransportCandidate cand) throws NotConnectedException, InterruptedException {
                    sendTransportCandidateOffer(cand);
                }

                @Override
                public void end() {
                }

                @Override
                public void init() {
                }
            };

            resolver.addListener(resolverListener);
        }

        if (!(resolver.isResolving() || resolver.isResolved())) {
            // Resolve our IP and port
            LOGGER.fine("RESOLVER CALLED");
            resolver.resolve(session);
        }
    }

    /**
     * Dispatch an incoming packet. The method is responsible for recognizing
     * the stanza type and, depending on the current state, delivering the
     * stanza to the right event handler and wait for a response.
     *
     * @param iq the stanza received
     * @return the new Jingle stanza to send.
     * @throws XMPPException if an XMPP protocol error was received.
     * @throws SmackException if Smack detected an exceptional situation.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    @Override
    public final List<IQ> dispatchIncomingPacket(IQ iq, String id) throws XMPPException, SmackException, InterruptedException {
        List<IQ> responses = new ArrayList<>();
        IQ response = null;

        if (iq != null) {
            if (iq.getType().equals(IQ.Type.error)) {
                // Process errors
                setNegotiatorState(JingleNegotiatorState.FAILED);
                triggerTransportClosed(null);
                // This next line seems wrong, and may subvert the normal closing process.
                throw new JingleException(iq.getError().getDescriptiveText());
            } else if (iq.getType().equals(IQ.Type.result)) {
                // Process ACKs
                if (isExpectedId(iq.getStanzaId())) {
                    response = receiveResult(iq);
                    removeExpectedId(iq.getStanzaId());
                }
            } else if (iq instanceof Jingle) {
                // Get the action from the Jingle packet
                Jingle jingle = (Jingle) iq;
                JingleActionEnum action = jingle.getAction();

                switch (action) {
                    case CONTENT_ACCEPT:
                        response = receiveContentAcceptAction(jingle);
                        break;

                    case CONTENT_MODIFY:
                        break;

                    case CONTENT_REMOVE:
                        break;

                    case SESSION_INFO:
                        break;

                    case SESSION_INITIATE:
                        response = receiveSessionInitiateAction(jingle);
                        break;

                    case SESSION_ACCEPT:
                        response = receiveSessionAcceptAction(jingle);
                        break;

                    case TRANSPORT_INFO:
                        response = receiveTransportInfoAction(jingle);
                        break;

                    default:
                        break;
                }
            }
        }

        if (response != null) {
            addExpectedId(response.getStanzaId());
            responses.add(response);
        }

        return responses;
    }

    /**
     * The other endpoint has partially accepted our invitation: start
     * offering a list of candidates.
     *
     * @return an IQ packet
     * @throws XMPPException if an XMPP protocol error was received.
     * @throws SmackException if Smack detected an exceptional situation.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    private Jingle receiveResult(IQ iq) throws XMPPException, SmackException, InterruptedException {
        Jingle response = null;

        sendTransportCandidatesOffer();
        setNegotiatorState(JingleNegotiatorState.PENDING);

        return response;
    }

    /**
     *  @param jingle TODO javadoc me please
     *  @return the iq
     * @throws SmackException if Smack detected an exceptional situation.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    private IQ receiveSessionInitiateAction(Jingle jingle) throws XMPPException, SmackException, InterruptedException {
        IQ response = null;

        // Parse the Jingle and get any proposed transport candidates
        // addRemoteCandidates(obtainCandidatesList(jin));

        // Start offering candidates
        sendTransportCandidatesOffer();

        // All these candidates will be checked asynchronously. Wait for some
        // time and check if we have a valid candidate to use...
        delayedCheckBestCandidate(session, jingle);

        // Set the next state
        setNegotiatorState(JingleNegotiatorState.PENDING);

        return response;
    }

    /**
     *  @param jingle TODO javadoc me please
     *  @return the iq
     */
    private IQ receiveTransportInfoAction(Jingle jingle) {
        IQ response = null;

        // Parse the Jingle and get any proposed transport candidates
        // addRemoteCandidates(obtainCandidatesList(jin));

        //        // Start offering candidates
        //        sendTransportCandidatesOffer();
        //
        //        // All these candidates will be checked asynchronously. Wait for some
        //        // time and check if we have a valid candidate to use...
        //        delayedCheckBestCandidate(session, jingle);
        //
        //        // Set the next state
        //        setNegotiatorState(JingleNegotiatorState.PENDING);

        // Parse the Jingle and get any proposed transport candidates
        addRemoteCandidates(obtainCandidatesList(jingle));

        // Wait for some time and check if we have a valid candidate to
        // use...
        delayedCheckBestCandidate(session, jingle);

        response = session.createAck(jingle);

        return response;
    }

    /**
     * One of our transport candidates has been accepted.
     *
     * @return a Jingle packet
     * @throws XMPPException an exception
     * @see org.jivesoftware.smackx.jingleold.JingleNegotiator.State#eventAccept(org.jivesoftware.smackx.jingleold.packet.Jingle)
     */
    private IQ receiveContentAcceptAction(Jingle jingle) throws XMPPException {
        IQ response = null;

        // Parse the Jingle and get the accepted candidate
        List<TransportCandidate> accepted = obtainCandidatesList(jingle);
        if (!accepted.isEmpty()) {

            for (TransportCandidate cand : accepted) {
                LOGGER.fine("Remote accepted candidate addr: " + cand.getIp());
            }

            TransportCandidate cand = accepted.get(0);
            setAcceptedLocalCandidate(cand);

            if (isEstablished()) {
                LOGGER.fine(cand.getIp() + " is set active");
                // setNegotiatorState(JingleNegotiatorState.SUCCEEDED);
            }
        }
        return response;
    }

    /**
     *  @param jingle TODO javadoc me please
     *  @return the iq
     */
    private static IQ receiveSessionAcceptAction(Jingle jingle) {
        IQ response = null;

        LOGGER.fine("Transport established");
        // triggerTransportEstablished(getAcceptedLocalCandidate(), getBestRemoteCandidate());

        // setNegotiatorState(JingleNegotiatorState.SUCCEEDED);

        return response;
    }

    /**
     * Trigger a Transport session established event.
     *
     * @param local  TransportCandidate that has been agreed.
     * @param remote TransportCandidate that has been agreed.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NoResponseException if there was no response from the remote entity.
     */
    private void triggerTransportEstablished(TransportCandidate local, TransportCandidate remote) throws NotConnectedException, InterruptedException, NoResponseException, XMPPErrorException {
        List<JingleListener> listeners = getListenersList();
        for (JingleListener li : listeners) {
            if (li instanceof JingleTransportListener) {
                JingleTransportListener mli = (JingleTransportListener) li;
                LOGGER.fine("triggerTransportEstablished " + local.getLocalIp() + ":" + local.getPort() + " <-> "
                        + remote.getIp() + ":" + remote.getPort());
                mli.transportEstablished(local, remote);
            }
        }
    }

    /**
     * Trigger a Transport closed event.
     *
     * @param cand current TransportCandidate that is cancelled.
     */
    private void triggerTransportClosed(TransportCandidate cand) {
        List<JingleListener> listeners = getListenersList();
        for (JingleListener li : listeners) {
            if (li instanceof JingleTransportListener) {
                JingleTransportListener mli = (JingleTransportListener) li;
                mli.transportClosed(cand);
            }
        }
    }

    // Subclasses

    /**
     * Raw-UDP transport negotiator.
     *
     * @author Alvaro Saurin
     */
    public static final class RawUdp extends TransportNegotiator {

        /**
         * Default constructor, with a JingleSession and transport manager.
         *
         * @param js  The Jingle session this negotiation belongs to.
         * @param res The transport resolver to use.
         * @param parentNegotiator the parent content negotiator.
         */
        public RawUdp(JingleSession js, final TransportResolver res, ContentNegotiator parentNegotiator) {
            super(js, res, parentNegotiator);
        }

        /**
         * Get a TransportNegotiator instance.
         */
        @Override
        public org.jivesoftware.smackx.jingleold.packet.JingleTransport getJingleTransport(TransportCandidate bestRemote) {
            org.jivesoftware.smackx.jingleold.packet.JingleTransport.RawUdp jt = new org.jivesoftware.smackx.jingleold.packet.JingleTransport.RawUdp();
            jt.addCandidate(new org.jivesoftware.smackx.jingleold.packet.JingleTransport.RawUdp.Candidate(bestRemote));
            return jt;
        }

        /**
         * Obtain the best common transport candidate obtained in the
         * negotiation.
         *
         * @return the bestRemoteCandidate
         */
        @Override
        public TransportCandidate getBestRemoteCandidate() {
            // Hopefully, we only have one validRemoteCandidate
            ArrayList<TransportCandidate> cands = getValidRemoteCandidatesList();
            if (!cands.isEmpty()) {
                LOGGER.fine("RAW CAND");
                return cands.get(0);
            } else {
                LOGGER.fine("No Remote Candidate");
                return null;
            }
        }

        /**
         * Return true for fixed candidates.
         */
        @Override
        public boolean acceptableTransportCandidate(TransportCandidate tc, List<TransportCandidate> localCandidates) {
            return tc instanceof TransportCandidate.Fixed;
        }
    }

    /**
     * Ice transport negotiator.
     *
     * @author Alvaro Saurin
     */
    public static final class Ice extends TransportNegotiator {

        /**
         * Default constructor, with a JingleSession and transport manager.
         *
         * @param js  The Jingle session this negotiation belongs to.
         * @param res The transport manager to use.
         * @param parentNegotiator the parent content negotiator.
         */
        public Ice(JingleSession js, final TransportResolver res, ContentNegotiator parentNegotiator) {
            super(js, res, parentNegotiator);
        }

        /**
         * Get a TransportNegotiator instance.
         *
         * @param candidate TODO javadoc me please
         */
        @Override
        public org.jivesoftware.smackx.jingleold.packet.JingleTransport getJingleTransport(TransportCandidate candidate) {
            org.jivesoftware.smackx.jingleold.packet.JingleTransport.Ice jt = new org.jivesoftware.smackx.jingleold.packet.JingleTransport.Ice();
            jt.addCandidate(new org.jivesoftware.smackx.jingleold.packet.JingleTransport.Ice.Candidate(candidate));
            return jt;
        }

        /**
         * Obtain the best remote candidate obtained in the negotiation so far.
         *
         * @return the bestRemoteCandidate
         */
        @Override
        public TransportCandidate getBestRemoteCandidate() {
            ICECandidate result = null;

            ArrayList<TransportCandidate> cands = getValidRemoteCandidatesList();
            if (!cands.isEmpty()) {
                int highest = -1;
                ICECandidate chose = null;
                for (TransportCandidate transportCandidate : cands) {
                    if (transportCandidate instanceof ICECandidate) {
                        ICECandidate icecandidate = (ICECandidate) transportCandidate;
                        if (icecandidate.getPreference() > highest) {
                            chose = icecandidate;
                            highest = icecandidate.getPreference();
                        }
                    }
                }
                result = chose;
            }

            if (result != null && result.getType().equals(ICECandidate.Type.relay))
                LOGGER.fine("Relay Type");

            return result;
        }

        /**
         * Return true for ICE candidates.
         */
        @Override
        public boolean acceptableTransportCandidate(TransportCandidate tc, List<TransportCandidate> localCandidates) {
            return tc instanceof ICECandidate;
        }
    }
}
