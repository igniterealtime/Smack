/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright (C) 2002-2006 Jive Software. All rights reserved.
 * ====================================================================
 * The Jive Software License (based on Apache Software License, Version 1.1)
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by
 *        Jive Software (http://www.jivesoftware.com)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Smack" and "Jive Software" must not be used to
 *    endorse or promote products derived from this software without
 *    prior written permission. For written permission, please
 *    contact webmaster@jivesoftware.com.
 *
 * 5. Products derived from this software may not be called "Smack",
 *    nor may "Smack" appear in their name, without prior written
 *    permission of Jive Software.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL JIVE SOFTWARE OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 */

package org.jivesoftware.smackx.jingle;

import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smackx.jingle.listeners.*;
import org.jivesoftware.smackx.jingle.media.*;
import org.jivesoftware.smackx.jingle.nat.TransportCandidate;
import org.jivesoftware.smackx.jingle.nat.TransportNegotiator;
import org.jivesoftware.smackx.packet.Jingle;
import org.jivesoftware.smackx.packet.JingleContentDescription;
import org.jivesoftware.smackx.packet.JingleContentDescription.JinglePayloadType;
import org.jivesoftware.smackx.packet.JingleContentInfo;
import org.jivesoftware.smackx.packet.JingleError;
import org.jivesoftware.smackx.packet.JingleTransport.JingleTransportCandidate;

import java.util.*;

/**
 * An abstract Jingle session.
 * <p/>
 * This class contains some basic properties of every Jingle session. However,
 * the concrete implementation can be found in subclasses.
 *
 * @author Alvaro Saurin
 * @see IncomingJingleSession
 * @see OutgoingJingleSession
 */
public abstract class JingleSession extends JingleNegotiator {

    // static
    private static final HashMap sessions = new HashMap();

    private static final Random randomGenerator = new Random();

    // non-static

    private String initiator; // Who started the communication

    private String responder; // The other endpoint

    private String sid; // A unique id that identifies this session

    private MediaNegotiator mediaNeg; // The description...

    private TransportNegotiator transNeg; // and transport negotiators

    PacketListener packetListener;

    PacketFilter packetFilter;

    protected JingleMediaManager jingleMediaManager = null;

    protected JingleMediaSession jingleMediaSession = null;

    private boolean closed = false;

    private List<JingleSessionStateListener> stateListeners = new ArrayList<JingleSessionStateListener>();

    /**
     * Full featured JingleSession constructor
     *
     * @param conn               XMPPConnection
     * @param initiator          the initiator JID
     * @param responder          the responder JID
     * @param sessionid          the session ID
     * @param jingleMediaManager the jingleMediaManager
     */
    protected JingleSession(XMPPConnection conn, String initiator, String responder,
            String sessionid, JingleMediaManager jingleMediaManager) {
        super(conn);

        this.mediaNeg = null;
        this.transNeg = null;

        this.initiator = initiator;
        this.responder = responder;
        this.sid = sessionid;

        this.jingleMediaManager = jingleMediaManager;

        // Add the session to the list and register the listeneres
        registerInstance();
        installConnectionListeners(conn);
    }

    /**
     * JingleSession constructor
     *
     * @param conn      XMPPConnection
     * @param initiator the initiator JID
     * @param responder the responder JID
     */
    protected JingleSession(XMPPConnection conn, String initiator, String responder) {
        this(conn, initiator, responder, null, null);
    }

    /**
     * JingleSession constructor
     *
     * @param conn               XMPPConnection
     * @param initiator          the initiator JID
     * @param responder          the responder JID
     * @param jingleMediaManager the jingleMediaManager
     */
    protected JingleSession(XMPPConnection conn, String initiator, String responder, JingleMediaManager jingleMediaManager) {
        this(conn, initiator, responder, null, jingleMediaManager);
    }

    /**
     * Get the session initiator
     *
     * @return the initiator
     */
    public String getInitiator() {
        return initiator;
    }

    /**
     * Set the session initiator
     *
     * @param initiator the initiator to set
     */
    public void setInitiator(String initiator) {
        this.initiator = initiator;
    }

    /**
     * Get the Media Manager of this Jingle Session
     *
     * @return
     */
    public JingleMediaManager getMediaManager() {
        return jingleMediaManager;
    }

    /**
     * Set the Media Manager of this Jingle Session
     *
     * @param jingleMediaManager
     */
    public void setMediaManager(JingleMediaManager jingleMediaManager) {
        this.jingleMediaManager = jingleMediaManager;
    }

    /**
     * Get the session responder
     *
     * @return the responder
     */
    public String getResponder() {
        return responder;
    }

    /**
     * Set the session responder.
     *
     * @param responder the receptor to set
     */
    public void setResponder(String responder) {
        this.responder = responder;
    }

    /**
     * Get the session ID
     *
     * @return the sid
     */
    public String getSid() {
        return sid;
    }

    /**
     * Set the session ID
     *
     * @param sessionId the sid to set
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
     * Obtain the description negotiator for this session
     *
     * @return the description negotiator
     */
    protected MediaNegotiator getMediaNeg() {
        return mediaNeg;
    }

    /**
     * Set the jmf negotiator.
     *
     * @param mediaNeg the description negotiator to set
     */
    protected void setMediaNeg(MediaNegotiator mediaNeg) {
        destroyMediaNeg();
        this.mediaNeg = mediaNeg;
    }

    /**
     * Destroy the jmf negotiator.
     */
    protected void destroyMediaNeg() {
        if (mediaNeg != null) {
            mediaNeg.close();
            mediaNeg = null;
        }
    }

    /**
     * Adds a State Listener for the Session. It will be called twice every time the Session State changed. One before State change and other after.
     *
     * @param listener listener to be added
     */
    public void addStateListener(JingleSessionStateListener listener) {
        stateListeners.add(listener);
    }

    /**
     * Removes a JingleStateListener
     *
     * @param listener listener to be removed
     */
    public void removedStateListener(JingleSessionStateListener listener) {
        stateListeners.remove(listener);
    }

    /**
     * Removes all JingleSessionStateListeners.
     */
    public void removeAllStateListeners() {
        stateListeners.clear();
    }

    /**
     * Overides JingleNegiociator Method to add listener capabilities
     *
     * @param newState new State
     */
    protected void setState(State newState) {
        boolean proceed = true;
        State old = getState();

        for (JingleSessionStateListener listener : stateListeners)
            try {
                listener.beforeChange(old, newState);
            }
            catch (JingleException e) {
                proceed = false;
            }

        if (proceed)
            super.setState(newState);

        for (JingleSessionStateListener listener : stateListeners)
            listener.afterChanged(old, getState());
    }

    /**
     * Obtain the transport negotiator for this session.
     *
     * @return the transport negotiator instance
     */
    protected TransportNegotiator getTransportNeg() {
        return transNeg;
    }

    /**
     * Set TransportNegociator
     *
     * @param transNeg the transNeg to set
     */
    protected void setTransportNeg(TransportNegotiator transNeg) {
        destroyTransportNeg();
        this.transNeg = transNeg;
    }

    /**
     * Destroy the transport negotiator.
     */
    protected void destroyTransportNeg() {
        if (transNeg != null) {
            transNeg.close();
            transNeg = null;
        }
    }

    /**
     * Return true if the transport and content negotiators have finished
     */
    public boolean isFullyEstablished() {
        if (!isValid()) {
            return false;
        }
        if (!getTransportNeg().isFullyEstablished()
                || !getMediaNeg().isFullyEstablished()) {
            return false;
        }
        return true;
    }

    /**
     * Return true if the session is valid (<i>ie</i>, it has all the required
     * elements initialized).
     *
     * @return true if the session is valid.
     */
    public boolean isValid() {
        return mediaNeg != null && transNeg != null && sid != null && initiator != null;
    }

    /**
     * Dispatch an incoming packet. The medthod is responsible for recognizing
     * the packet type and, depending on the current state, deliverying the
     * packet to the right event handler and wait for a response.
     *
     * @param iq the packet received
     * @return the new Jingle packet to send.
     * @throws XMPPException
     */
    public IQ dispatchIncomingPacket(IQ iq, String id) throws XMPPException {
        IQ jout = null;

        if (invalidState()) {
            throw new IllegalStateException(
                    "Illegal state in dispatch packet in Session manager.");
        }
        else {
            if (iq == null) {
                // If there is no input packet, then we must be inviting...
                jout = getState().eventInvite();
            }
            else {
                if (iq.getType().equals(IQ.Type.ERROR)) {
                    // Process errors
                    getState().eventError(iq);
                }
                else if (iq.getType().equals(IQ.Type.RESULT)) {
                    // Process ACKs
                    if (isExpectedId(iq.getPacketID())) {
                        jout = getState().eventAck(iq);
                        removeExpectedId(iq.getPacketID());
                    }
                }
                else if (iq instanceof Jingle) {
                    // It is not an error: it is a Jingle packet...
                    Jingle jin = (Jingle) iq;
                    Jingle.Action action = jin.getAction();

                    if (action != null) {
                        if (action.equals(Jingle.Action.SESSIONACCEPT)) {
                            jout = getState().eventAccept(jin);
                        }
                        else if (action.equals(Jingle.Action.SESSIONINFO)) {
                            jout = getState().eventInfo(jin);
                        }
                        else if (action.equals(Jingle.Action.SESSIONINITIATE)) {
                            if (getState() != null)
                                jout = getState().eventInitiate(jin);
                        }
                        else if (action.equals(Jingle.Action.SESSIONREDIRECT)) {
                            jout = getState().eventRedirect(jin);
                        }
                        else if (action.equals(Jingle.Action.SESSIONTERMINATE)) {
                            jout = getState().eventTerminate(jin);
                        }
                    }
                    else {
                        jout = errorMalformedStanza(iq);
                    }
                }
            }

            if (jout != null) {
                // Save the packet id, for recognizing ACKs...
                addExpectedId(jout.getPacketID());
            }
        }

        return jout;
    }

    /**
     * Process and respond to an incomming packet.
     * <p/>
     * This method is called from the packet listener dispatcher when a new
     * packet has arrived. The medthod is responsible for recognizing the packet
     * type and, depending on the current state, deliverying it to the right
     * event handler and wait for a response. The response will be another
     * Jingle packet that will be sent to the other endpoint.
     *
     * @param iq the packet received
     * @return the new Jingle packet to send.
     * @throws XMPPException
     */
    public synchronized IQ respond(IQ iq) throws XMPPException {
        IQ response = null;

        if (isValid()) {
            String responseId = null;
            IQ sessionResponse = null;
            IQ descriptionResponse = null;
            IQ transportResponse = null;

            // Send the packet to the right event handler for the session...
            try {
                sessionResponse = dispatchIncomingPacket(iq, null);
                if (sessionResponse != null) {
                    responseId = sessionResponse.getPacketID();
                }

                // ... and do the same for the Description and Transport
                // parts...
                if (mediaNeg != null) {
                    descriptionResponse = mediaNeg.dispatchIncomingPacket(iq, responseId);
                }

                if (transNeg != null) {
                    transportResponse = transNeg.dispatchIncomingPacket(iq, responseId);
                }

                // Acknowledge the IQ reception
                sendAck(iq);

                // ... and send all these parts in a Jingle response.
                response = sendJingleParts(iq, (Jingle) sessionResponse,
                        (Jingle) descriptionResponse, (Jingle) transportResponse);

            }
            catch (JingleException e) {
                // Send an error message, if present
                JingleError error = e.getError();
                if (error != null) {
                    sendFormattedError(iq, error);
                }

                // Notify the session end and close everything...
                triggerSessionClosedOnError(e);
            }
        }

        return response;
    }

    // Packet formatting and delivery

    /**
     * Put together all the parts ina Jingle packet.
     *
     * @return the new Jingle packet
     */
    private Jingle sendJingleParts(IQ iq, Jingle jSes, Jingle jDesc,
            Jingle jTrans) {
        Jingle response = null;

        if (jSes != null) {
            jSes.addDescriptions(jDesc.getDescriptionsList());
            jSes.addTransports(jTrans.getTransportsList());

            response = sendFormattedJingle(iq, jSes);
        }
        else {
            // If we don't have a valid session message, then we must send
            // separated messages for transport and jmf...
            if (jDesc != null) {
                response = sendFormattedJingle(iq, jDesc);
            }

            if (jTrans != null) {
                response = sendFormattedJingle(iq, jTrans);
            }
        }

        return response;
    }

    /**
     * Complete and send an error. Complete all the null fields in an IQ error
     * reponse, using the sesssion information we have or some info from the
     * incoming packet.
     *
     * @param iq    The Jingle packet we are responing to
     * @param error the IQ packet we want to complete and send
     */
    public IQ sendFormattedError(IQ iq, JingleError error) {
        IQ perror = null;
        if (error != null) {
            perror = createIQ(getSid(), iq.getFrom(), iq.getTo(), IQ.Type.ERROR);

            // Fill in the fields with the info from the Jingle packet
            perror.setPacketID(iq.getPacketID());
            perror.addExtension(error);

            getConnection().sendPacket(perror);
            System.err.println(error.toXML());
        }
        return perror;
    }

    /**
     * Complete and send a packet. Complete all the null fields in a Jingle
     * reponse, using the session information we have or some info from the
     * incoming packet.
     *
     * @param iq   The Jingle packet we are responing to
     * @param jout the Jingle packet we want to complete and send
     */
    public Jingle sendFormattedJingle(IQ iq, Jingle jout) {
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

            String me = getConnection().getUser();
            String other = getResponder().equals(me) ? getInitiator() : getResponder();

            if (jout.getTo() == null) {
                if (iq != null) {
                    jout.setTo(iq.getFrom());
                }
                else {
                    jout.setTo(other);
                }
            }

            if (jout.getFrom() == null) {
                if (iq != null) {
                    jout.setFrom(iq.getTo());
                }
                else {
                    jout.setFrom(me);
                }
            }
            getConnection().sendPacket(jout);
        }
        return jout;
    }

    /**
     * Complete and send a packet. Complete all the null fields in a Jingle
     * reponse, using the session information we have.
     *
     * @param jout the Jingle packet we want to complete and send
     */
    public Jingle sendFormattedJingle(Jingle jout) {
        return sendFormattedJingle(null, jout);
    }

    /**
     * Send an error indicating that the stanza is malformed.
     *
     * @param iq
     */
    protected IQ errorMalformedStanza(IQ iq) {
        // FIXME: implement with the right message...
        return createError(iq.getPacketID(), iq.getFrom(), getConnection().getUser(),
                400, "Bad Request");
    }

    /**
     * Check if we have an established session and, in that case, send an Accept
     * packet.
     */
    protected Jingle sendAcceptIfFullyEstablished() {
        Jingle result = null;
        if (isFullyEstablished()) {
            // Ok, send a packet saying that we accept this session
            Jingle jout = new Jingle(Jingle.Action.SESSIONACCEPT);
            jout.setType(IQ.Type.SET);

            result = sendFormattedJingle(jout);
        }
        return result;
    }

    /**
     * Acknowledge a IQ packet.
     *
     * @param iq The IQ to acknowledge
     */
    public IQ sendAck(IQ iq) {
        IQ result = null;

        if (iq != null) {
            // Don't acknowledge ACKs, errors...
            if (iq.getType().equals(IQ.Type.SET)) {
                IQ ack = createIQ(iq.getPacketID(), iq.getFrom(), iq.getTo(),
                        IQ.Type.RESULT);

                getConnection().sendPacket(ack);
                result = ack;
            }
        }
        return result;
    }

    /**
     * Send a content info message.
     */
    public synchronized void sendContentInfo(ContentInfo ci) {
        if (isValid()) {
            sendFormattedJingle(new Jingle(new JingleContentInfo(ci)));
        }
    }

    /**
     * Get the content description the other part has accepted.
     *
     * @param jin The Jingle packet where they have accepted the session.
     * @return The audio PayloadType they have accepted.
     * @throws XMPPException
     */
    protected PayloadType.Audio getAcceptedAudioPayloadType(Jingle jin)
            throws XMPPException {
        PayloadType.Audio acceptedPayloadType = null;
        ArrayList jda = jin.getDescriptionsList();

        if (jin.getAction().equals(Jingle.Action.SESSIONACCEPT)) {

            if (jda.size() > 1) {
                throw new XMPPException(
                        "Unsupported feature: the number of accepted content descriptions is greater than 1.");
            }
            else if (jda.size() == 1) {
                JingleContentDescription jd = (JingleContentDescription) jda.get(0);
                if (jd.getJinglePayloadTypesCount() > 1) {
                    throw new XMPPException(
                            "Unsupported feature: the number of accepted payload types is greater than 1.");
                }
                if (jd.getJinglePayloadTypesCount() == 1) {
                    JinglePayloadType jpt = (JinglePayloadType) jd
                            .getJinglePayloadTypesList().get(0);
                    acceptedPayloadType = (PayloadType.Audio) jpt.getPayloadType();
                }
            }
        }
        return acceptedPayloadType;
    }

    /**
     * Get the accepted local candidate we have previously offered.
     *
     * @param jin The jingle packet where they accept the session
     * @return The transport candidate they have accepted.
     * @throws XMPPException
     */
    protected TransportCandidate getAcceptedLocalCandidate(Jingle jin)
            throws XMPPException {
        ArrayList jta = jin.getTransportsList();
        TransportCandidate acceptedLocalCandidate = null;

        if (jin.getAction().equals(Jingle.Action.SESSIONACCEPT)) {
            if (jta.size() > 1) {
                throw new XMPPException(
                        "Unsupported feature: the number of accepted transports is greater than 1.");
            }
            else if (jta.size() == 1) {
                org.jivesoftware.smackx.packet.JingleTransport jt = (org.jivesoftware.smackx.packet.JingleTransport) jta.get(0);

                if (jt.getCandidatesCount() > 1) {
                    throw new XMPPException(
                            "Unsupported feature: the number of accepted transport candidates is greater than 1.");
                }
                else if (jt.getCandidatesCount() == 1) {
                    JingleTransportCandidate jtc = (JingleTransportCandidate) jt
                            .getCandidatesList().get(0);
                    acceptedLocalCandidate = jtc.getMediaTransport();
                }
            }
        }

        return acceptedLocalCandidate;
    }

    /*
             * (non-Javadoc)
             *
             * @see java.lang.Object#hashCode()
             */
    public int hashCode() {
        return Jingle.getSessionHash(getSid(), getInitiator());
    }

    /*
             * (non-Javadoc)
             *
             * @see java.lang.Object#equals(java.lang.Object)
             */
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
        }
        else if (!initiator.equals(other.initiator)) {
            //Todo check behavior
            //    return false;
        }

        if (responder == null) {
            if (other.responder != null) {
                return false;
            }
        }
        else if (!responder.equals(other.responder)) {
            return false;
        }

        if (sid == null) {
            if (other.sid != null) {
                return false;
            }
        }
        else if (!sid.equals(other.sid)) {
            return false;
        }

        return true;
    }

    // Instances management

    /**
     * Clean a session from the list.
     *
     * @param connection The connection to clean up
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
     * @param con A XMPP connection
     * @return a Jingle session
     */
    public static JingleSession getInstanceFor(XMPPConnection con) {
        if (con == null) {
            throw new IllegalArgumentException("Connection cannot be null");
        }

        JingleSession result = null;
        synchronized (sessions) {
            if (sessions.containsKey(con)) {
                result = (JingleSession) sessions.get(con);
            }
        }

        return result;
    }


    /**
     * Configure a session, setting some action listeners...
     *
     * @param connection The connection to set up
     */
    private void installConnectionListeners(final XMPPConnection connection) {
        if (connection != null) {
            connection.addConnectionListener(new ConnectionListener() {
                public void connectionClosed() {
                    unregisterInstanceFor(connection);
                }

                public void connectionClosedOnError(java.lang.Exception e) {
                    unregisterInstanceFor(connection);
                }

                public void reconnectingIn(int i) {
                }

                public void reconnectionSuccessful() {
                }

                public void reconnectionFailed(Exception exception) {
                }
            });
        }
    }

    /**
     * Remove the packet listener used for processing packet.
     */
    protected void removePacketListener() {
        if (packetListener != null) {
            getConnection().removePacketListener(packetListener);

            System.out.println("REMOVE PACKET LISTENER");
        }
    }

    /**
     * Install the packet listener. The listener is responsible for responding
     * to any packet that we receive...
     */
    protected void updatePacketListener() {
        removePacketListener();

        System.out.println("UpdatePacketListener");

        packetListener = new PacketListener() {
            public void processPacket(Packet packet) {
                try {
                    respond((IQ) packet);
                }
                catch (XMPPException e) {
                    e.printStackTrace();
                }
            }
        };

        packetFilter = new PacketFilter() {
            public boolean accept(Packet packet) {

                if (packet instanceof IQ) {
                    IQ iq = (IQ) packet;

                    String me = getConnection().getUser();

                    if (!iq.getTo().equals(me)) {
                        return false;
                    }

                    String other = getResponder().equals(me) ? getInitiator()
                            : getResponder();

                    if (iq.getFrom() == null || !iq.getFrom().equals(other == null ? "" : other)) {
                        return false;
                    }

                    if (iq instanceof Jingle) {
                        Jingle jin = (Jingle) iq;

                        System.out.println("Jingle: " + iq.toXML());

                        String sid = jin.getSid();
                        if (sid == null || !sid.equals(getSid())) {
                            System.out.println("Ignored Jingle(SID) " + sid + "|" + getSid() + " :" + iq.toXML());
                            return false;
                        }
                        String ini = jin.getInitiator();
                        if (!ini.equals(getInitiator())) {
                            System.out.println("Ignored Jingle(INI): " + iq.toXML());
                            return false;
                        }
                    }
                    else {
                        // We accept some non-Jingle IQ packets: ERRORs and ACKs
                        if (iq.getType().equals(IQ.Type.SET)) {
                            System.out.println("Ignored Jingle(TYPE): " + iq.toXML());
                            return false;
                        }
                        else if (iq.getType().equals(IQ.Type.GET)) {
                            System.out.println("Ignored Jingle(TYPE): " + iq.toXML());
                            return false;
                        }
                    }
                    return true;
                }
                return false;
            }
        };

        getConnection().addPacketListener(packetListener, packetFilter);
    }

    // Listeners

    /**
     * Add a listener for jmf negotiation events
     *
     * @param li The listener
     */
    public void addMediaListener(JingleMediaListener li) {
        if (getMediaNeg() != null) {
            getMediaNeg().addListener(li);
        }
    }

    /**
     * Remove a listener for jmf negotiation events
     *
     * @param li The listener
     */
    public void removeMediaListener(JingleMediaListener li) {
        if (getMediaNeg() != null) {
            getMediaNeg().removeListener(li);
        }
    }

    /**
     * Add a listener for transport negotiation events
     *
     * @param li The listener
     */
    public void addTransportListener(JingleTransportListener li) {
        if (getTransportNeg() != null) {
            getTransportNeg().addListener(li);
        }
    }

    /**
     * Remove a listener for transport negotiation events
     *
     * @param li The listener
     */
    public void removeTransportListener(JingleTransportListener li) {
        if (getTransportNeg() != null) {
            getTransportNeg().removeListener(li);
        }
    }

    // Triggers

    /**
     * Trigger a session closed event.
     */
    protected void triggerSessionClosed(String reason) {
        for (TransportCandidate candidate : this.getTransportNeg().getOfferedCandidates())
            candidate.removeCandidateEcho();

        ArrayList listeners = getListenersList();
        Iterator iter = listeners.iterator();
        while (iter.hasNext()) {
            JingleListener li = (JingleListener) iter.next();
            if (li instanceof JingleSessionListener) {
                JingleSessionListener sli = (JingleSessionListener) li;
                sli.sessionClosed(reason, this);
            }
        }
        close();
        if (jingleMediaSession != null) {
            jingleMediaSession.stopTrasmit();
            jingleMediaSession.stopReceive();
        }
    }

    /**
     * Trigger a session closed event due to an error.
     */
    protected void triggerSessionClosedOnError(XMPPException exc) {
        for (TransportCandidate candidate : this.getTransportNeg().getOfferedCandidates())
            candidate.removeCandidateEcho();
        ArrayList listeners = getListenersList();
        Iterator iter = listeners.iterator();
        while (iter.hasNext()) {
            JingleListener li = (JingleListener) iter.next();
            if (li instanceof JingleSessionListener) {
                JingleSessionListener sli = (JingleSessionListener) li;
                sli.sessionClosedOnError(exc, this);
            }
        }
        close();
        if (jingleMediaSession != null) {
            jingleMediaSession.stopTrasmit();
            jingleMediaSession.stopReceive();
        }
    }

    /**
     * Trigger a session established event.
     */
    protected void triggerSessionEstablished(PayloadType pt,
            TransportCandidate rc, TransportCandidate lc) {
        ArrayList listeners = getListenersList();
        Iterator iter = listeners.iterator();
        while (iter.hasNext()) {
            JingleListener li = (JingleListener) iter.next();
            if (li instanceof JingleSessionListener) {
                JingleSessionListener sli = (JingleSessionListener) li;
                sli.sessionEstablished(pt, rc, lc, this);
            }
        }
        if (jingleMediaManager != null) {
            rc.removeCandidateEcho();
            lc.removeCandidateEcho();

            jingleMediaSession = jingleMediaManager.createMediaSession(pt, rc, lc);
            if (jingleMediaSession != null) {

                jingleMediaSession.startTrasmit();
                jingleMediaSession.startReceive();

                for (TransportCandidate candidate : this.getTransportNeg().getOfferedCandidates())
                    candidate.removeCandidateEcho();

            }
        }

    }

    /**
     * Trigger a session redirect event.
     */
    protected void triggerSessionRedirect(String arg) {
        ArrayList listeners = getListenersList();
        Iterator iter = listeners.iterator();
        while (iter.hasNext()) {
            JingleListener li = (JingleListener) iter.next();
            if (li instanceof JingleSessionListener) {
                JingleSessionListener sli = (JingleSessionListener) li;
                sli.sessionRedirected(arg, this);
            }
        }
    }

    /**
     * Trigger a session redirect event.
     */
    protected void triggerSessionDeclined(String reason) {
        ArrayList listeners = getListenersList();
        Iterator iter = listeners.iterator();
        while (iter.hasNext()) {
            JingleListener li = (JingleListener) iter.next();
            if (li instanceof JingleSessionListener) {
                JingleSessionListener sli = (JingleSessionListener) li;
                sli.sessionDeclined(reason, this);
            }
        }
        for (TransportCandidate candidate : this.getTransportNeg().getOfferedCandidates())
            candidate.removeCandidateEcho();
    }

    /**
     * Start the negotiation.
     *
     * @throws JingleException
     * @throws XMPPException
     */
    public abstract void start(JingleSessionRequest jin) throws XMPPException;

    /**
     * Terminate the session.
     *
     * @throws XMPPException
     */
    public void terminate() throws XMPPException {
        if (isClosed()) return;
        System.out.println("State: " + this.getState());
        Jingle jout = new Jingle(Jingle.Action.SESSIONTERMINATE);
        jout.setType(IQ.Type.SET);
        sendFormattedJingle(jout);
        triggerSessionClosed("Closed Locally");
    }

    /**
     * Terminate negotiations.
     */
    public void close() {
        if (isClosed()) return;
        destroyMediaNeg();
        destroyTransportNeg();
        removePacketListener();
        System.out.println("Negotiation Closed: " + getConnection().getUser() + " " + sid);
        closed = true;
        super.close();
    }

    public boolean isClosed() {
        return closed;
    }

    // Packet and error creation

    /**
     * A convience method to create an IQ packet.
     *
     * @param ID   The packet ID of the
     * @param to   To whom the packet is addressed.
     * @param from From whom the packet is sent.
     * @param type The iq type of the packet.
     * @return The created IQ packet.
     */
    public static IQ createIQ(String ID, String to, String from,
            IQ.Type type) {
        IQ iqPacket = new IQ() {
            public String getChildElementXML() {
                return null;
            }
        };

        iqPacket.setPacketID(ID);
        iqPacket.setTo(to);
        iqPacket.setFrom(from);
        iqPacket.setType(type);

        return iqPacket;
    }

    /**
     * A convience method to create an error packet.
     *
     * @param ID      The packet ID of the
     * @param to      To whom the packet is addressed.
     * @param from    From whom the packet is sent.
     * @param errCode The error code.
     * @param errStr  The error string.
     * @return The created IQ packet.
     */
    public static IQ createError(String ID, String to, String from,
            int errCode, String errStr) {

        IQ iqError = createIQ(ID, to, from, IQ.Type.ERROR);
        XMPPError error = new XMPPError(new XMPPError.Condition(errStr));
        iqError.setError(error);

        System.out.println("Created Error Packet:" + iqError.toXML());

        return iqError;
    }
}
