/**
 * $RCSfile$
 * $Revision: 7329 $
 * $Date: 2007-02-28 20:59:28 -0300 (qua, 28 fev 2007) $
 *
 * Copyright 2003-2005 Jive Software.
 *
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
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
package org.jivesoftware.smackx.jingle.media;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.jingle.JingleNegotiator;
import org.jivesoftware.smackx.jingle.JingleSession;
import org.jivesoftware.smackx.jingle.listeners.JingleListener;
import org.jivesoftware.smackx.jingle.listeners.JingleMediaListener;
import org.jivesoftware.smackx.packet.Jingle;
import org.jivesoftware.smackx.packet.JingleContentDescription;
import org.jivesoftware.smackx.packet.JingleContentDescription.JinglePayloadType;
import org.jivesoftware.smackx.packet.JingleError;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Manager for jmf descriptor negotiation.
 * <p/>
 * <p/>
 * This class is responsible for managing the descriptor negotiation process,
 * handling all the xmpp packets interchange and the stage control.
 *
 * @author Thiago Camargo
 */
public class MediaNegotiator extends JingleNegotiator {

    private final JingleSession session; // The session this negotiation

    // Local and remote payload types...

    private final List<PayloadType> localAudioPts = new ArrayList<PayloadType>();

    private final List<PayloadType> remoteAudioPts = new ArrayList<PayloadType>();

    private PayloadType.Audio bestCommonAudioPt;

    // states

    private final Inviting inviting;

    private final Accepting accepting;

    private final Pending pending;

    private final Active active;

    /**
     * Default constructor. The constructor establishes some basic parameters,
     * but it does not start the negotiation. For starting the negotiation, call
     * startNegotiation.
     *
     * @param js The jingle session.
     */
    public MediaNegotiator(JingleSession js, List<PayloadType> pts) {
        super(js.getConnection());

        session = js;

        bestCommonAudioPt = null;

        if (pts != null) {
            if (pts.size() > 0) {
                localAudioPts.addAll(pts);
            }
        }

        // Create the states...
        inviting = new Inviting(this);
        accepting = new Accepting(this);
        pending = new Pending(this);
        active = new Active(this);
    }

    /**
     * Dispatch an incomming packet. The medthod is responsible for recognizing
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
            if (iq == null) {
                // With a null packet, we are just inviting the other end...
                setState(inviting);
                jout = getState().eventInvite();
            }
            else {
                if (iq instanceof Jingle) {
                    // If there is no specific jmf action associated, then we
                    // are being invited to a new session...
                    setState(accepting);
                    jout = getState().eventInitiate((Jingle) iq);
                }
                else {
                    throw new IllegalStateException(
                            "Invitation IQ received is not a Jingle packet in Media negotiator.");
                }
            }
        }
        else {
            if (iq == null) {
                return null;
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
                    // Get the action from the Jingle packet
                    Jingle jin = (Jingle) iq;
                    Jingle.Action action = jin.getAction();

                    if (action != null) {
                        if (action.equals(Jingle.Action.CONTENTACCEPT)) {
                            jout = getState().eventAccept(jin);
                        }
                        else if (action.equals(Jingle.Action.CONTENTDECLINE)) {
                            jout = getState().eventDecline(jin);
                        }
                        else if (action.equals(Jingle.Action.DESCRIPTIONINFO)) {
                            jout = getState().eventInfo(jin);
                        }
                        else if (action.equals(Jingle.Action.CONTENTMODIFY)) {
                            jout = getState().eventModify(jin);
                        }
                        // Any unknown action will be ignored: it is not a msg
                        // to us...
                    }
                }
            }
        }

        // Save the Id for any ACK
        if (id != null) {
            addExpectedId(id);
        }
        else {
            if (jout != null) {
                addExpectedId(jout.getPacketID());
            }
        }

        return jout;
    }

    /**
     * Return true if the content is negotiated.
     *
     * @return true if the content is negotiated.
     */
    public boolean isEstablished() {
        return getBestCommonAudioPt() != null;
    }

    /**
     * Return true if the content is fully negotiated.
     *
     * @return true if the content is fully negotiated.
     */
    public boolean isFullyEstablished() {
        return isEstablished() && getState() == active;
    }

    // Payload types

    private PayloadType.Audio calculateBestCommonAudioPt(List remoteAudioPts) {
        final ArrayList<PayloadType> commonAudioPtsHere = new ArrayList<PayloadType>();
        final ArrayList<PayloadType> commonAudioPtsThere = new ArrayList<PayloadType>();
        PayloadType.Audio result = null;

        if (!remoteAudioPts.isEmpty()) {
            commonAudioPtsHere.addAll(localAudioPts);
            commonAudioPtsHere.retainAll(remoteAudioPts);

            commonAudioPtsThere.addAll(remoteAudioPts);
            commonAudioPtsThere.retainAll(localAudioPts);

            if (!commonAudioPtsHere.isEmpty() && !commonAudioPtsThere.isEmpty()) {

                PayloadType.Audio bestPtHere = null;
          

                if (bestPtHere == null)
                    for (PayloadType payloadType : commonAudioPtsHere)
                        if (payloadType instanceof PayloadType.Audio) {
                            bestPtHere = (PayloadType.Audio) payloadType;
                            break;
                        }

                PayloadType.Audio bestPtThere = null;
                for (PayloadType payloadType : commonAudioPtsThere)
                    if (payloadType instanceof PayloadType.Audio) {
                        bestPtThere = (PayloadType.Audio) payloadType;
                        break;
                    }

                if (session.getInitiator().equals(session.getConnection().getUser()))
                    result = bestPtHere;
                else
                    result = bestPtThere;
            }
        }

        return result;
    }

    private List obtainPayloads(Jingle jin) {
        List result = new ArrayList();
        Iterator iDescr = jin.getDescriptions();

        // Add the list of payloads: iterate over the descriptions...
        while (iDescr.hasNext()) {
            JingleContentDescription.Audio descr = (JingleContentDescription.Audio) iDescr
                    .next();

            if (descr != null) {
                // ...and, then, over the payloads.
                // Note: we use the last "description" in the packet...
                result.clear();
                result.addAll(descr.getAudioPayloadTypesList());
            }
        }

        return result;
    }

    /**
     * Adds a payload type to the list of remote payloads.
     *
     * @param pt the remote payload type
     */
    public void addRemoteAudioPayloadType(PayloadType.Audio pt) {
        if (pt != null) {
            synchronized (remoteAudioPts) {
                remoteAudioPts.add(pt);
            }
        }
    }

    /**
     * Create an offer for the list of audio payload types.
     *
     * @return a new Jingle packet with the list of audio Payload Types
     */
    private Jingle getAudioPayloadTypesOffer() {
        JingleContentDescription.Audio audioDescr = new JingleContentDescription.Audio();

        // Add the list of payloads for audio and create a
        // JingleContentDescription
        // where we announce our payloads...
        audioDescr.addAudioPayloadTypes(localAudioPts);

        return new Jingle(audioDescr);
    }

    // Predefined messages and Errors

    /**
     * Create an IQ "accept" message.
     */
    private Jingle createAcceptMessage() {
        Jingle jout = null;

        // If we hava a common best codec, send an accept right now...
        jout = new Jingle(Jingle.Action.CONTENTACCEPT);
        jout.addDescription(new JingleContentDescription.Audio(
                new JinglePayloadType.Audio(bestCommonAudioPt)));

        return jout;
    }

    // Payloads

    /**
     * Get the best common codec between both parts.
     *
     * @return The best common PayloadType codec.
     */
    public PayloadType.Audio getBestCommonAudioPt() {
        return bestCommonAudioPt;
    }

    // Events

    /**
     * Trigger a session established event.
     *
     * @param bestPt payload type that has been agreed.
     */
    protected void triggerMediaEstablished(PayloadType bestPt) {
        ArrayList listeners = getListenersList();
        Iterator iter = listeners.iterator();
        while (iter.hasNext()) {
            JingleListener li = (JingleListener) iter.next();
            if (li instanceof JingleMediaListener) {
                JingleMediaListener mli = (JingleMediaListener) li;
                mli.mediaEstablished(bestPt);
            }
        }
    }

    /**
     * Trigger a jmf closed event.
     *
     * @param currPt current payload type that is cancelled.
     */
    protected void triggerMediaClosed(PayloadType currPt) {
        ArrayList listeners = getListenersList();
        Iterator iter = listeners.iterator();
        while (iter.hasNext()) {
            JingleListener li = (JingleListener) iter.next();
            if (li instanceof JingleMediaListener) {
                JingleMediaListener mli = (JingleMediaListener) li;
                mli.mediaClosed(currPt);
            }
        }
    }

    /**
     * Terminate the jmf negotiator
     */
    public void close() {
        super.close();
    }

    // States

    /**
     * First stage when we send a session request.
     */
    public class Inviting extends JingleNegotiator.State {

        public Inviting(MediaNegotiator neg) {
            super(neg);
        }

        /**
         * Create an initial Jingle packet, with the list of payload types that
         * we support. The list is in order of preference.
         */
        public Jingle eventInvite() {
            return getAudioPayloadTypesOffer();
        }

        /**
         * We have received the ACK for our invitation.
         *
         * @see org.jivesoftware.smackx.jingle.JingleNegotiator.State#eventAck(org.jivesoftware.smack.packet.IQ)
         */
        public Jingle eventAck(IQ iq) {
            setState(pending);
            return null;
        }
    }

    /**
     * We are accepting connections.
     */
    public class Accepting extends JingleNegotiator.State {

        public Accepting(MediaNegotiator neg) {
            super(neg);
        }

        /**
         * We have received an invitation! Respond with a list of our payload
         * types...
         */
        public Jingle eventInitiate(Jingle jin) {
            synchronized (remoteAudioPts) {
                remoteAudioPts.addAll(obtainPayloads(jin));
            }

            return getAudioPayloadTypesOffer();
        }

        /**
         * Process the ACK of our list of codecs (our offer).
         *
         * @see org.jivesoftware.smackx.jingle.JingleNegotiator.State#eventAck(org.jivesoftware.smack.packet.IQ)
         */
        public Jingle eventAck(IQ iq) throws XMPPException {
            Jingle response = null;

            if (!remoteAudioPts.isEmpty()) {
                // Calculate the best common codec
                bestCommonAudioPt = calculateBestCommonAudioPt(remoteAudioPts);

                // and send an accept if we havee an agreement...
                if (bestCommonAudioPt != null) {
                    response = createAcceptMessage();
                }
                else {
                    throw new JingleException(JingleError.NO_COMMON_PAYLOAD);
                }

                setState(pending);
            }

            return response;
        }
    }

    /**
     * Pending class: we are waiting for the other enpoint, that must say if it
     * accepts or not...
     */
    public class Pending extends JingleNegotiator.State {

        public Pending(MediaNegotiator neg) {
            super(neg);
        }

        /**
         * A content info has been received. This is done for publishing the
         * list of payload types...
         *
         * @param jin The input packet
         * @return a Jingle packet
         * @throws JingleException
         */
        public Jingle eventInfo(Jingle jin) throws JingleException {
            PayloadType.Audio oldBestCommonAudioPt = bestCommonAudioPt;
            List offeredPayloads = new ArrayList();
            Jingle response = null;
            boolean ptChange = false;

            offeredPayloads = obtainPayloads(jin);
            if (!offeredPayloads.isEmpty()) {

                synchronized (remoteAudioPts) {
                    remoteAudioPts.clear();
                    remoteAudioPts.addAll(offeredPayloads);
                }

                // Calculate the best common codec
                bestCommonAudioPt = calculateBestCommonAudioPt(remoteAudioPts);
                if (bestCommonAudioPt != null) {
                    // and send an accept if we have an agreement...
                    ptChange = !bestCommonAudioPt.equals(oldBestCommonAudioPt);
                    if (oldBestCommonAudioPt == null || ptChange) {
                        response = createAcceptMessage();
                    }
                }
                else {
                    throw new JingleException(JingleError.NO_COMMON_PAYLOAD);
                }
            }

            // Parse the Jingle and get the payload accepted
            return response;
        }

        /**
         * A jmf description has been accepted. In this case, we must save the
         * accepted payload type and notify any listener...
         *
         * @param jin The input packet
         * @return a Jingle packet
         * @throws JingleException
         */
        public Jingle eventAccept(Jingle jin) throws JingleException {
            PayloadType.Audio agreedCommonAudioPt;
            List offeredPayloads = new ArrayList();
            Jingle response = null;

            if (bestCommonAudioPt == null) {
                // Update the best common audio PT
                bestCommonAudioPt = calculateBestCommonAudioPt(remoteAudioPts);
                response = createAcceptMessage();
            }

            offeredPayloads = obtainPayloads(jin);
            if (!offeredPayloads.isEmpty()) {
                if (offeredPayloads.size() == 1) {
                    agreedCommonAudioPt = (PayloadType.Audio) offeredPayloads.get(0);
                    if (bestCommonAudioPt != null) {
                        // If the accepted PT matches the best payload
                        // everything is fine
                        if (!agreedCommonAudioPt.equals(bestCommonAudioPt)) {
                            throw new JingleException(JingleError.NEGOTIATION_ERROR);
                        }
                    }

                }
                else if (offeredPayloads.size() > 1) {
                    throw new JingleException(JingleError.MALFORMED_STANZA);
                }
            }

            return response;
        }

        /**
         * The other part has declined the our codec...
         *
         * @throws JingleException
         */
        public Jingle eventDecline(Jingle inJingle) throws JingleException {
            triggerMediaClosed(getBestCommonAudioPt());
            throw new JingleException();
        }

        /*
           * (non-Javadoc)
           *
           * @see org.jivesoftware.smackx.jingle.JingleNegotiator.State#eventError(org.jivesoftware.smack.packet.IQ)
           */
        public void eventError(IQ iq) throws XMPPException {
            triggerMediaClosed(getBestCommonAudioPt());
            super.eventError(iq);
        }

        /**
         * ACK received.
         *
         * @see org.jivesoftware.smackx.jingle.JingleNegotiator.State#eventAck(org.jivesoftware.smack.packet.IQ)
         */
        public Jingle eventAck(IQ iq) {
            if (isEstablished()) {
                setState(active);
            }

            return null;
        }
    }

    /**
     * "Active" state: we have an agreement about the codec...
     */
    public class Active extends JingleNegotiator.State {

        public Active(MediaNegotiator neg) {
            super(neg);
        }

        /**
         * We have an agreement.
         *
         * @see org.jivesoftware.smackx.jingle.JingleNegotiator.State#eventEnter()
         */
        public void eventEnter() {
            triggerMediaEstablished(getBestCommonAudioPt());
            super.eventEnter();
        }

        /**
         * We are breaking the contract...
         *
         * @see org.jivesoftware.smackx.jingle.JingleNegotiator.State#eventExit()
         */
        public void eventExit() {
            triggerMediaClosed(getBestCommonAudioPt());
            super.eventExit();
        }
    }
}
