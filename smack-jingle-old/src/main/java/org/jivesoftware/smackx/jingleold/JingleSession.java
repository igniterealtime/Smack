/**
 *
 * Copyright the original author or authors
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
package org.jivesoftware.smackx.jingleold;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.AbstractConnectionClosedListener;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smackx.jingleold.listeners.JingleListener;
import org.jivesoftware.smackx.jingleold.listeners.JingleMediaListener;
import org.jivesoftware.smackx.jingleold.listeners.JingleSessionListener;
import org.jivesoftware.smackx.jingleold.listeners.JingleTransportListener;
import org.jivesoftware.smackx.jingleold.media.JingleMediaManager;
import org.jivesoftware.smackx.jingleold.media.JingleMediaSession;
import org.jivesoftware.smackx.jingleold.media.MediaNegotiator;
import org.jivesoftware.smackx.jingleold.media.MediaReceivedListener;
import org.jivesoftware.smackx.jingleold.media.PayloadType;
import org.jivesoftware.smackx.jingleold.nat.JingleTransportManager;
import org.jivesoftware.smackx.jingleold.nat.TransportCandidate;
import org.jivesoftware.smackx.jingleold.nat.TransportNegotiator;
import org.jivesoftware.smackx.jingleold.nat.TransportResolver;
import org.jivesoftware.smackx.jingleold.packet.Jingle;
import org.jivesoftware.smackx.jingleold.packet.JingleError;
import org.jxmpp.jid.Jid;

/**
 * An abstract Jingle session. <p/> This class contains some basic properties of
 * every Jingle session. However, the concrete implementation can be found in
 * subclasses.
 * 
 * @author Alvaro Saurin
 * @author Jeff Williams
 */
public class JingleSession extends JingleNegotiator implements MediaReceivedListener {

    private static final Logger LOGGER = Logger.getLogger(JingleSession.class.getName());

    // static
    private static final HashMap<XMPPConnection, JingleSession> sessions = new HashMap<XMPPConnection, JingleSession>();

    private static final Random randomGenerator = new Random();

    // non-static

    private Jid initiator; // Who started the communication

    private Jid responder; // The other endpoint

    private String sid; // A unique id that identifies this session

    ConnectionListener connectionListener;

    StanzaListener packetListener;

    StanzaFilter packetFilter;

    protected List<JingleMediaManager> jingleMediaManagers = null;

    private JingleSessionState sessionState;

    private List<ContentNegotiator> contentNegotiators;

    private XMPPConnection connection;

    private String sessionInitPacketID;

    private Map<String, JingleMediaSession> mediaSessionMap;

    /**
     * Full featured JingleSession constructor.
     * 
     * @param conn
     *            the XMPPConnection which is used
     * @param initiator
     *            the initiator JID
     * @param responder
     *            the responder JID
     * @param sessionid
     *            the session ID
     * @param jingleMediaManagers
     *            the jingleMediaManager
     */
    public JingleSession(XMPPConnection conn, Jid initiator, Jid responder, String sessionid,
            List<JingleMediaManager> jingleMediaManagers) {
        super();

        this.initiator = initiator;
        this.responder = responder;
        this.sid = sessionid;
        this.jingleMediaManagers = jingleMediaManagers;
        this.setSession(this);
        this.connection = conn;

        // Initially, we don't known the session state.
        setSessionState(JingleSessionStateUnknown.getInstance());

        contentNegotiators = new ArrayList<ContentNegotiator>();
        mediaSessionMap = new HashMap<String, JingleMediaSession>();

        // Add the session to the list and register the listeneres
        registerInstance();
        installConnectionListeners(conn);
    }

    /**
     * JingleSession constructor (for an outgoing Jingle session).
     * 
     * @param conn
     *            Connection
     * @param initiator
     *            the initiator JID
     * @param responder
     *            the responder JID
     * @param jingleMediaManagers
     *            the jingleMediaManager
     */
    public JingleSession(XMPPConnection conn, JingleSessionRequest request, Jid initiator, Jid responder,
            List<JingleMediaManager> jingleMediaManagers) {
        this(conn, initiator, responder, generateSessionId(), jingleMediaManagers);
        //sessionRequest = request; // unused
    }

    /**
     * Get the session initiator.
     * 
     * @return the initiator
     */
    public Jid getInitiator() {
        return initiator;
    }

    @Override
    public XMPPConnection getConnection() {
        return connection;
    }

    /**
     * Set the session initiator.
     * 
     * @param initiator
     *            the initiator to set
     */
    public void setInitiator(Jid initiator) {
        this.initiator = initiator;
    }

    /**
     * Get the Media Manager of this Jingle Session.
     * 
     * @return the JingleMediaManagers
     */
    public List<JingleMediaManager> getMediaManagers() {
        return jingleMediaManagers;
    }

    /**
     * Set the Media Manager of this Jingle Session.
     * 
     * @param jingleMediaManagers
     */
    public void setMediaManagers(List<JingleMediaManager> jingleMediaManagers) {
        this.jingleMediaManagers = jingleMediaManagers;
    }

    /**
     * Get the session responder.
     * 
     * @return the responder
     */
    public Jid getResponder() {
        return responder;
    }

    /**
     * Set the session responder.
     * 
     * @param responder
     *            the receptor to set
     */
    public void setResponder(Jid responder) {
        this.responder = responder;
    }

    /**
     * Get the session ID.
     * 
     * @return the sid
     */
    public String getSid() {
        return sid;
    }

    /**
     * Set the session ID
     * 
     * @param sessionId
     *            the sid to set
     */
    protected void setSid(String sessionId) {
        sid = sessionId;
    }

    /**
     * Generate a unique session ID.
     */
    protected static String generateSessionId() {
        return String.valueOf(Math.abs(randomGenerator.nextLong()));
    }

    /**
     * Validate the state changes.
     */

    public void setSessionState(JingleSessionState stateIs) {

        LOGGER.fine("Session state change: " + sessionState + "->" + stateIs);
        stateIs.enter();
        sessionState = stateIs;
    }

    public JingleSessionState getSessionState() {
        return sessionState;
    }

    /**
     * Return true if all of the media managers have finished.
     */
    public boolean isFullyEstablished() {
        boolean result = true;
        for (ContentNegotiator contentNegotiator : contentNegotiators) {
            if (!contentNegotiator.isFullyEstablished())
                result = false;
        }
        return result;
    }

    // ----------------------------------------------------------------------------------------------------------
    // Receive section
    // ----------------------------------------------------------------------------------------------------------

    /**
     * Process and respond to an incoming packet. <p/> This method is called
     * from the stanza(/packet) listener dispatcher when a new stanza(/packet) has arrived. The
     * method is responsible for recognizing the stanza(/packet) type and, depending on
     * the current state, delivering it to the right event handler and wait for
     * a response. The response will be another Jingle stanza(/packet) that will be sent
     * to the other end point.
     * 
     * @param iq
     *            the stanza(/packet) received
     * @throws XMPPException
     * @throws SmackException 
     * @throws InterruptedException 
     */
    public synchronized void receivePacketAndRespond(IQ iq) throws XMPPException, SmackException, InterruptedException {
        List<IQ> responses = new ArrayList<IQ>();

        String responseId = null;

        LOGGER.fine("Packet: " + iq.toXML());

        try {

            // Dispatch the packet to the JingleNegotiators and get back a list of the results.
            responses.addAll(dispatchIncomingPacket(iq, null));

            if (iq != null) {
                responseId = iq.getStanzaId();

                // Send the IQ to each of the content negotiators for further processing.
                // Each content negotiator may pass back a list of JingleContent for addition to the response packet.
                // CHECKSTYLE:OFF
                for (ContentNegotiator contentNegotiator : contentNegotiators) {
                    // If at this point the content negotiator isn't started, it's because we sent a session-init jingle
                    // packet from startOutgoing() and we're waiting for the other side to let us know they're ready
                    // to take jingle packets.  (This packet might be a session-terminate, but that will get handled
                    // later.
                    if (!contentNegotiator.isStarted()) {
                        contentNegotiator.start();
                    }
                    responses.addAll(contentNegotiator.dispatchIncomingPacket(iq, responseId));
                }
                // CHECKSTYLE:ON

            }
            // Acknowledge the IQ reception
            // Not anymore.  The state machine generates an appropriate response IQ that
            // gets sent back at the end of this routine.
            //sendAck(iq);

        } catch (JingleException e) {
            // Send an error message, if present
            JingleError error = e.getError();
            if (error != null) {
                responses.add(createJingleError(iq, error));
            }

            // Notify the session end and close everything...
            triggerSessionClosedOnError(e);
        }

        //        // If the response is anything other than a RESULT then send it now.
        //        if ((response != null) && (!response.getType().equals(IQ.Type.result))) {
        //            getConnection().sendStanza(response);
        //        }

        // Loop through all of the responses and send them.
        for (IQ response : responses) {
            sendStanza(response);
        }
    }

    /**
     * Dispatch an incoming packet. The method is responsible for recognizing
     * the stanza(/packet) type and, depending on the current state, delivering the
     * stanza(/packet) to the right event handler and wait for a response.
     * 
     * @param iq
     *            the stanza(/packet) received
     * @return the new Jingle stanza(/packet) to send.
     * @throws XMPPException
     * @throws SmackException 
     * @throws InterruptedException 
     */
    @Override
    public List<IQ> dispatchIncomingPacket(IQ iq, String id) throws XMPPException, SmackException, InterruptedException {
        List<IQ> responses = new ArrayList<IQ>();
        IQ response = null;

        if (iq != null) {
            if (iq.getType().equals(IQ.Type.error)) {
                // Process errors
                // TODO getState().eventError(iq);
            } else if (iq.getType().equals(IQ.Type.result)) {
                // Process ACKs
                if (isExpectedId(iq.getStanzaId())) {

                    // The other side provisionally accepted our session-initiate.
                    // Kick off some negotiators.
                    if (iq.getStanzaId().equals(sessionInitPacketID)) {
                        startNegotiators();
                    }
                    removeExpectedId(iq.getStanzaId());
                }
            } else if (iq instanceof Jingle) {
                // It is not an error: it is a Jingle packet...
                Jingle jin = (Jingle) iq;
                JingleActionEnum action = jin.getAction();

                // Depending on the state we're in we'll get different processing actions.
                // (See Design Patterns AKA GoF State behavioral pattern.)
                response = getSessionState().processJingle(this, jin, action);
            }
        }

        if (response != null) {
            // Save the packet id, for recognizing ACKs...
            addExpectedId(response.getStanzaId());
            responses.add(response);
        }

        return responses;
    }

    /**
     * Add a new content negotiator on behalf of a <content> section received.
     */
    public void addContentNegotiator(ContentNegotiator inContentNegotiator) {
        contentNegotiators.add(inContentNegotiator);
    }



     // ----------------------------------------------------------------------------------------------------------
    // Send section
    // ----------------------------------------------------------------------------------------------------------

    public void sendStanza(IQ iq) throws NotConnectedException, InterruptedException {

        if (iq instanceof Jingle) {

            sendFormattedJingle((Jingle) iq);

        } else {

            getConnection().sendStanza(iq);
        }
    }

    /**
     * Complete and send a packet. Complete all the null fields in a Jingle
     * reponse, using the session information we have.
     * 
     * @param jout
     *            the Jingle stanza(/packet) we want to complete and send
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    public Jingle sendFormattedJingle(Jingle jout) throws NotConnectedException, InterruptedException {
        return sendFormattedJingle(null, jout);
    }

    /**
     * Complete and send a packet. Complete all the null fields in a Jingle
     * reponse, using the session information we have or some info from the
     * incoming packet.
     * 
     * @param iq
     *            The Jingle stanza(/packet) we are responing to
     * @param jout
     *            the Jingle stanza(/packet) we want to complete and send
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    public Jingle sendFormattedJingle(IQ iq, Jingle jout) throws NotConnectedException, InterruptedException {
        if (jout != null) {
            if (jout.getInitiator() == null) {
                jout.setInitiator(getInitiator());
            }

            if (jout.getResponder() == null) {
                jout.setResponder(getResponder());
            }

            if (jout.getSid() == null) {
                jout.setSid(getSid());
            }

            Jid me = getConnection().getUser();
            Jid other = getResponder().equals(me) ? getInitiator() : getResponder();

            if (jout.getTo() == null) {
                if (iq != null) {
                    jout.setTo(iq.getFrom());
                } else {
                    jout.setTo(other);
                }
            }

            if (jout.getFrom() == null) {
                if (iq != null) {
                    jout.setFrom(iq.getTo());
                } else {
                    jout.setFrom(me);
                }
            }

            // The the packet.
            // CHECKSTYLE:OFF
            if ((getConnection() != null) && (getConnection().isConnected()))
                getConnection().sendStanza(jout);
            // CHECKSTYLE:ON
        }
        return jout;
    }

    /**
     *  @param inJingle
     *  @param inAction
     */
    //    private void sendUnknownStateAction(Jingle inJingle, JingleActionEnum inAction) {
    //
    //        if (inAction == JingleActionEnum.SESSION_INITIATE) {
    //            // Prepare to receive and act on response packets.
    //            updatePacketListener();
    //
    //            // Send the actual packet.
    //            sendStanza(inJingle);
    //
    //            // Change to the PENDING state.
    //            setSessionState(JingleSessionStateEnum.PENDING);
    //        } else {
    //            throw new IllegalStateException("Only session-initiate allowed in the UNKNOWN state.");
    //        }
    //    }

    /**
     * Acknowledge a IQ packet.
     * 
     * @param iq
     *            The IQ to acknowledge
     */
    public IQ createAck(IQ iq) {
        IQ result = null;

        if (iq != null) {
            // Don't acknowledge ACKs, errors...
            if (iq.getType().equals(IQ.Type.set)) {
                IQ ack = IQ.createResultIQ(iq);

                // No! Don't send it.  Let it flow to the normal way IQ results get processed and sent.
                // getConnection().sendStanza(ack);
                result = ack;
            }
        }
        return result;
    }

    /**
     * Send a content info message.
     */
    //    public synchronized void sendContentInfo(ContentInfo ci) {
    //        sendStanza(new Jingle(new JingleContentInfo(ci)));
    //    }
    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return Jingle.getSessionHash(getSid(), getInitiator());
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }

        final JingleSession other = (JingleSession) obj;

        if (initiator == null) {
            if (other.initiator != null) {
                return false;
            }
        } else if (!initiator.equals(other.initiator)) {
            // Todo check behavior
            // return false;
        }

        if (responder == null) {
            if (other.responder != null) {
                return false;
            }
        } else if (!responder.equals(other.responder)) {
            return false;
        }

        if (sid == null) {
            if (other.sid != null) {
                return false;
            }
        } else if (!sid.equals(other.sid)) {
            return false;
        }

        return true;
    }

    // Instances management

    /**
     * Clean a session from the list.
     * 
     * @param connection
     *            The connection to clean up
     */
    private void unregisterInstanceFor(XMPPConnection connection) {
        synchronized (sessions) {
            sessions.remove(connection);
        }
    }

    /**
     * Register this instance.
     */
    private void registerInstance() {
        synchronized (sessions) {
            sessions.put(getConnection(), this);
        }
    }

    /**
     * Returns the JingleSession related to a particular connection.
     * 
     * @param con
     *            A XMPP connection
     * @return a Jingle session
     */
    public static JingleSession getInstanceFor(XMPPConnection con) {
        if (con == null) {
            throw new IllegalArgumentException("XMPPConnection cannot be null");
        }

        JingleSession result = null;
        synchronized (sessions) {
            if (sessions.containsKey(con)) {
                result = sessions.get(con);
            }
        }

        return result;
    }

    /**
     * Configure a session, setting some action listeners...
     * 
     * @param connection
     *            The connection to set up
     */
    private void installConnectionListeners(final XMPPConnection connection) {
        if (connection != null) {
            connectionListener = new AbstractConnectionClosedListener() {
                @Override
                public void connectionTerminated() {
                    unregisterInstanceFor(connection);
                }
            };
            connection.addConnectionListener(connectionListener);
        }
    }

    private void removeConnectionListener() {
        // CHECKSTYLE:OFF
        if (connectionListener != null) {
            getConnection().removeConnectionListener(connectionListener);

            LOGGER.fine("JINGLE SESSION: REMOVE CONNECTION LISTENER");
        }
        // CHECKSTYLE:ON
    }

    /**
     * Remove the stanza(/packet) listener used for processing packet.
     */
    protected void removeAsyncPacketListener() {
        if (packetListener != null) {
            getConnection().removeAsyncStanzaListener(packetListener);

            LOGGER.fine("JINGLE SESSION: REMOVE PACKET LISTENER");
        }
    }

    /**
     * Install the stanza(/packet) listener. The listener is responsible for responding
     * to any stanza(/packet) that we receive...
     */
    protected void updatePacketListener() {
        removeAsyncPacketListener();

        LOGGER.fine("UpdatePacketListener");

        packetListener = new StanzaListener() {
            @Override
            public void processStanza(Stanza packet) {
                try {
                    receivePacketAndRespond((IQ) packet);
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "exception", e);
                }
            }
        };

        packetFilter = new StanzaFilter() {
            @Override
            public boolean accept(Stanza packet) {

                if (packet instanceof IQ) {
                    IQ iq = (IQ) packet;

                    Jid me = getConnection().getUser();

                    if (!iq.getTo().equals(me)) {
                        return false;
                    }

                    Jid other = getResponder().equals(me) ? getInitiator() : getResponder();

                    if (iq.getFrom() == null || !iq.getFrom().equals(other == null ? "" : other)) {
                        return false;
                    }

                    if (iq instanceof Jingle) {
                        Jingle jin = (Jingle) iq;

                        String sid = jin.getSid();
                        if (sid == null || !sid.equals(getSid())) {
                            LOGGER.fine("Ignored Jingle(SID) " + sid + "|" + getSid() + " :" + iq.toXML());
                            return false;
                        }
                        Jid ini = jin.getInitiator();
                        if (!ini.equals(getInitiator())) {
                            LOGGER.fine("Ignored Jingle(INI): " + iq.toXML());
                            return false;
                        }
                    } else {
                        // We accept some non-Jingle IQ packets: ERRORs and ACKs
                        if (iq.getType().equals(IQ.Type.set)) {
                            LOGGER.fine("Ignored Jingle(TYPE): " + iq.toXML());
                            return false;
                        } else if (iq.getType().equals(IQ.Type.get)) {
                            LOGGER.fine("Ignored Jingle(TYPE): " + iq.toXML());
                            return false;
                        }
                    }
                    return true;
                }
                return false;
            }
        };

        getConnection().addAsyncStanzaListener(packetListener, packetFilter);
    }

    // Listeners

    /**
     * Add a listener for jmf negotiation events.
     * 
     * @param li
     *            The listener
     */
    public void addMediaListener(JingleMediaListener li) {
        for (ContentNegotiator contentNegotiator : contentNegotiators) {
            if (contentNegotiator.getMediaNegotiator() != null) {
                contentNegotiator.getMediaNegotiator().addListener(li);
            }
        }

    }

    /**
     * Remove a listener for jmf negotiation events.
     * 
     * @param li
     *            The listener
     */
    public void removeMediaListener(JingleMediaListener li) {
        for (ContentNegotiator contentNegotiator : contentNegotiators) {
            if (contentNegotiator.getMediaNegotiator() != null) {
                contentNegotiator.getMediaNegotiator().removeListener(li);
            }
        }
    }

    /**
     * Add a listener for transport negotiation events.
     * 
     * @param li
     *            The listener
     */
    public void addTransportListener(JingleTransportListener li) {
        for (ContentNegotiator contentNegotiator : contentNegotiators) {
            if (contentNegotiator.getTransportNegotiator() != null) {
                contentNegotiator.getTransportNegotiator().addListener(li);
            }
        }
    }

    /**
     * Remove a listener for transport negotiation events.
     * 
     * @param li
     *            The listener
     */
    public void removeTransportListener(JingleTransportListener li) {
        for (ContentNegotiator contentNegotiator : contentNegotiators) {
            if (contentNegotiator.getTransportNegotiator() != null) {
                contentNegotiator.getTransportNegotiator().removeListener(li);
            }
        }
    }

    /**
     * Setup the listeners that act on events coming from the lower level negotiators.
     */

    public void setupListeners() {

        JingleMediaListener jingleMediaListener = new JingleMediaListener() {
            @Override
            public void mediaClosed(PayloadType cand) {
            }

            @Override
            public void mediaEstablished(PayloadType pt) throws NotConnectedException, InterruptedException {
                if (isFullyEstablished()) {
                    Jingle jout = new Jingle(JingleActionEnum.SESSION_ACCEPT);

                    // Build up a response packet from each media manager.
                    for (ContentNegotiator contentNegotiator : contentNegotiators) {
                        if (contentNegotiator.getNegotiatorState() == JingleNegotiatorState.SUCCEEDED)
                            jout.addContent(contentNegotiator.getJingleContent());
                    }
                    // Send the "accept" and wait for the ACK
                    addExpectedId(jout.getStanzaId());
                    sendStanza(jout);

                    //triggerSessionEstablished();

                }
            }
        };

        JingleTransportListener jingleTransportListener = new JingleTransportListener() {

            @Override
            public void transportEstablished(TransportCandidate local, TransportCandidate remote) throws NotConnectedException, InterruptedException {
                if (isFullyEstablished()) {
                // CHECKSTYLE:OFF
                    // Indicate that this session is active.
                    setSessionState(JingleSessionStateActive.getInstance());

                    for (ContentNegotiator contentNegotiator : contentNegotiators) {
                // CHECKSTYLE:ON
                        if (contentNegotiator.getNegotiatorState() == JingleNegotiatorState.SUCCEEDED)
                            contentNegotiator.triggerContentEstablished();
                    }

                    if (getSessionState().equals(JingleSessionStatePending.getInstance())) {

                        Jingle jout = new Jingle(JingleActionEnum.SESSION_ACCEPT);

                        // Build up a response packet from each media manager.
                        for (ContentNegotiator contentNegotiator : contentNegotiators) {
                            if (contentNegotiator.getNegotiatorState() == JingleNegotiatorState.SUCCEEDED)
                                jout.addContent(contentNegotiator.getJingleContent());
                        }
                        // Send the "accept" and wait for the ACK
                        addExpectedId(jout.getStanzaId());
                        sendStanza(jout);
                    }
                }
            }

            @Override
            public void transportClosed(TransportCandidate cand) {
            }

            @Override
            public void transportClosedOnError(XMPPException e) {
            }
        };

        addMediaListener(jingleMediaListener);
        addTransportListener(jingleTransportListener);
    }

    // Triggers

    /**
     * Trigger a session closed event.
     */
    protected void triggerSessionClosed(String reason) {
        //        for (ContentNegotiator contentNegotiator : contentNegotiators) {
        //
        //            contentNegotiator.stopJingleMediaSession();
        //
        //            for (TransportCandidate candidate : contentNegotiator.getTransportNegotiator().getOfferedCandidates())
        //                candidate.removeCandidateEcho();
        //        }

        List<JingleListener> listeners = getListenersList();
        for (JingleListener li : listeners) {
            if (li instanceof JingleSessionListener) {
                JingleSessionListener sli = (JingleSessionListener) li;
                sli.sessionClosed(reason, this);
            }
        }
        close();
    }

    /**
     * Trigger a session closed event due to an error.
     */
    protected void triggerSessionClosedOnError(XMPPException exc) {
        for (ContentNegotiator contentNegotiator : contentNegotiators) {

            contentNegotiator.stopJingleMediaSession();

            for (TransportCandidate candidate : contentNegotiator.getTransportNegotiator().getOfferedCandidates())
                candidate.removeCandidateEcho();
        }
        List<JingleListener> listeners = getListenersList();
        for (JingleListener li : listeners) {
            if (li instanceof JingleSessionListener) {
                JingleSessionListener sli = (JingleSessionListener) li;
                sli.sessionClosedOnError(exc, this);
            }
        }
        close();
    }

    /**
     * Trigger a session established event.
     */
    //    protected void triggerSessionEstablished() {
    //        List<JingleListener> listeners = getListenersList();
    //        for (JingleListener li : listeners) {
    //            if (li instanceof JingleSessionListener) {
    //                JingleSessionListener sli = (JingleSessionListener) li;
    //                sli.sessionEstablished(this);
    //            }
    //        }
    //    }
    /**
     * Trigger a media received event.
     */
    protected void triggerMediaReceived(String participant) {
        List<JingleListener> listeners = getListenersList();
        for (JingleListener li : listeners) {
            if (li instanceof JingleSessionListener) {
                JingleSessionListener sli = (JingleSessionListener) li;
                sli.sessionMediaReceived(this, participant);
            }
        }
    }

    /**
     * Trigger a session redirect event.
     */
    //    protected void triggerSessionRedirect(String arg) {
    //        List<JingleListener> listeners = getListenersList();
    //        for (JingleListener li : listeners) {
    //            if (li instanceof JingleSessionListener) {
    //                JingleSessionListener sli = (JingleSessionListener) li;
    //                sli.sessionRedirected(arg, this);
    //            }
    //        }
    //    }
    /**
     * Trigger a session decline event.
     */
    //    protected void triggerSessionDeclined(String reason) {
    //        List<JingleListener> listeners = getListenersList();
    //        for (JingleListener li : listeners) {
    //            if (li instanceof JingleSessionListener) {
    //                JingleSessionListener sli = (JingleSessionListener) li;
    //                sli.sessionDeclined(reason, this);
    //            }
    //        }
    //        for (ContentNegotiator contentNegotiator : contentNegotiators) {
    //            for (TransportCandidate candidate : contentNegotiator.getTransportNegotiator().getOfferedCandidates())
    //                candidate.removeCandidateEcho();
    //        }
    //    }
    /**
     * Terminates the session with default reason.
     * 
     * @throws XMPPException
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    public void terminate() throws XMPPException, NotConnectedException, InterruptedException {
        terminate("Closed Locally");
    }

    /**
     * Terminates the session with a custom reason.
     * 
     * @throws XMPPException
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    public void terminate(String reason) throws XMPPException, NotConnectedException, InterruptedException {
        if (isClosed())
            return;
        LOGGER.fine("Terminate " + reason);
        Jingle jout = new Jingle(JingleActionEnum.SESSION_TERMINATE);
        jout.setType(IQ.Type.set);
        sendStanza(jout);
        triggerSessionClosed(reason);
    }

    /**
     * Terminate negotiations.
     */
    @Override
    public void close() {
        if (isClosed())
            return;

        // Set the session state to ENDED.
        setSessionState(JingleSessionStateEnded.getInstance());

        for (ContentNegotiator contentNegotiator : contentNegotiators) {

            contentNegotiator.stopJingleMediaSession();

            for (TransportCandidate candidate : contentNegotiator.getTransportNegotiator().getOfferedCandidates())
                candidate.removeCandidateEcho();

            contentNegotiator.close();
        }
        removeAsyncPacketListener();
        removeConnectionListener();
        getConnection().removeConnectionListener(connectionListener);
        LOGGER.fine("Negotiation Closed: " + getConnection().getUser() + " " + sid);
        super.close();

    }

    public boolean isClosed() {
        return getSessionState().equals(JingleSessionStateEnded.getInstance());
    }

    // Packet and error creation

    /**
     * Complete and send an error. Complete all the null fields in an IQ error
     * reponse, using the sesssion information we have or some info from the
     * incoming packet.
     * 
     * @param iq
     *            The Jingle stanza(/packet) we are responing to
     * @param jingleError
     *            the IQ stanza(/packet) we want to complete and send
     */
    public IQ createJingleError(IQ iq, JingleError jingleError) {
        IQ errorPacket = null;
        if (jingleError != null) {
            // TODO This is wrong according to XEP-166 § 10, but this jingle implementation is deprecated anyways
            XMPPError.Builder builder = XMPPError.getBuilder(XMPPError.Condition.undefined_condition);
            builder.addExtension(jingleError);

            errorPacket = IQ.createErrorResponse(iq, builder);

            //            errorPacket.addExtension(jingleError);

            // NO! Let the normal state machinery do all of the sending.
            // getConnection().sendStanza(perror);
            LOGGER.severe("Error sent: " + errorPacket.toXML());
        }
        return errorPacket;
    }

    /**
     * Called when new Media is received.
     */
    @Override
    public void mediaReceived(String participant) {
        triggerMediaReceived(participant);
    }

    /**
     * This is the starting point for intitiating a new session.
     * 
     * @throws IllegalStateException
     * @throws SmackException 
     * @throws InterruptedException 
     */
    public void startOutgoing() throws IllegalStateException, SmackException, InterruptedException {

        updatePacketListener();
        setSessionState(JingleSessionStatePending.getInstance());

        Jingle jingle = new Jingle(JingleActionEnum.SESSION_INITIATE);

        // Create a content negotiator for each media manager on the session.
        for (JingleMediaManager mediaManager : getMediaManagers()) {
            ContentNegotiator contentNeg = new ContentNegotiator(this, ContentNegotiator.INITIATOR, mediaManager.getName());

            // Create the media negotiator for this content description.
            contentNeg.setMediaNegotiator(new MediaNegotiator(this, mediaManager, mediaManager.getPayloads(), contentNeg));

            JingleTransportManager transportManager = mediaManager.getTransportManager();
            TransportResolver resolver = null;
            try {
                resolver = transportManager.getResolver(this);
            } catch (XMPPException e) {
                LOGGER.log(Level.WARNING, "exception", e);
            }

            if (resolver.getType().equals(TransportResolver.Type.rawupd)) {
                contentNeg.setTransportNegotiator(new TransportNegotiator.RawUdp(this, resolver, contentNeg));
            }
            if (resolver.getType().equals(TransportResolver.Type.ice)) {
                contentNeg.setTransportNegotiator(new TransportNegotiator.Ice(this, resolver, contentNeg));
            }

            addContentNegotiator(contentNeg);
        }

        // Give each of the content negotiators a chance to return a portion of the structure to make the Jingle packet.
        for (ContentNegotiator contentNegotiator : contentNegotiators) {
            jingle.addContent(contentNegotiator.getJingleContent());
        }

        // Save the session-initiate packet ID, so that we can respond to it.
        sessionInitPacketID = jingle.getStanzaId();

        sendStanza(jingle);

        // Now setup to track the media negotiators, so that we know when (if) to send a session-accept.
        setupListeners();

        // Give each of the content negotiators a chance to start 
        // and return a portion of the structure to make the Jingle packet.

// Don't do this anymore.  The problem is that the other side might not be ready.
// Later when we receive our first jingle packet from the other side we'll fire-up the negotiators
// before processing it.  (See receivePacketAndRespond() above.
//        for (ContentNegotiator contentNegotiator : contentNegotiators) {
//            contentNegotiator.start();
//        }
    }

    /**
     *  This is the starting point for responding to a new session.
     */
    public void startIncoming() {

        //updatePacketListener();
    }

    @Override
    protected void doStart() {

    }

    /**
     * When we initiate a session we need to start a bunch of negotiators right after we receive the result
     * stanza(/packet) for our session-initiate.  This is where we start them.
     * 
     */
    private void startNegotiators() {

        for (ContentNegotiator contentNegotiator : contentNegotiators) {
            TransportNegotiator transNeg = contentNegotiator.getTransportNegotiator();
            transNeg.start();
        }
    }

    /**
     * The jingle session may have one or more media managers that are trying to establish media sessions.
     * When the media manager succeeds in creating a media session is registers it with the session by the
     * media manager's static name.  This routine is where the media manager does the registering.
     */
    public void addJingleMediaSession(String mediaManagerName, JingleMediaSession mediaSession) {
        mediaSessionMap.put(mediaManagerName, mediaSession);
    }

    /**
     * The jingle session may have one or more media managers that are trying to establish media sessions.
     * When the media manager succeeds in creating a media session is registers it with the session by the
     * media manager's static name. This routine is where other objects can access the registered media sessions.
     * NB: If the media manager has not succeeded in establishing a media session then this could return null.
     */
    public JingleMediaSession getMediaSession(String mediaManagerName) {
        return mediaSessionMap.get(mediaManagerName);
    }
}
