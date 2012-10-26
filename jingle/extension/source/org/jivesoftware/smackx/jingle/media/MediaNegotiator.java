/**
 * $RCSfile: MediaNegotiator.java,v $
 * $Revision: 1.10 $
 * $Date: 2007/07/04 00:12:39 $
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

import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.jingle.ContentNegotiator;
import org.jivesoftware.smackx.jingle.JingleActionEnum;
import org.jivesoftware.smackx.jingle.JingleException;
import org.jivesoftware.smackx.jingle.JingleNegotiator;
import org.jivesoftware.smackx.jingle.JingleNegotiatorState;
import org.jivesoftware.smackx.jingle.JingleSession;
import org.jivesoftware.smackx.jingle.SmackLogger;
import org.jivesoftware.smackx.jingle.listeners.JingleListener;
import org.jivesoftware.smackx.jingle.listeners.JingleMediaListener;
import org.jivesoftware.smackx.packet.Jingle;
import org.jivesoftware.smackx.packet.JingleContent;
import org.jivesoftware.smackx.packet.JingleDescription;
import org.jivesoftware.smackx.packet.JingleError;

/**
 * Manager for jmf descriptor negotiation. <p/> <p/> This class is responsible
 * for managing the descriptor negotiation process, handling all the xmpp
 * packets interchange and the stage control. handling all the xmpp packets
 * interchange and the stage control.
 * 
 * @author Thiago Camargo
 */
public class MediaNegotiator extends JingleNegotiator {

	private static final SmackLogger LOGGER = SmackLogger.getLogger(MediaNegotiator.class);

	//private JingleSession session; // The session this negotiation

    private final JingleMediaManager mediaManager;

    // Local and remote payload types...

    private final List<PayloadType> localAudioPts = new ArrayList<PayloadType>();

    private final List<PayloadType> remoteAudioPts = new ArrayList<PayloadType>();

    private PayloadType bestCommonAudioPt;

    private ContentNegotiator parentNegotiator;

    /**
     * Default constructor. The constructor establishes some basic parameters,
     * but it does not start the negotiation. For starting the negotiation, call
     * startNegotiation.
     * 
     * @param js
     *            The jingle session.
     */
    public MediaNegotiator(JingleSession session, JingleMediaManager mediaManager, List<PayloadType> pts,
            ContentNegotiator parentNegotiator) {
        super(session);

        this.mediaManager = mediaManager;
        this.parentNegotiator = parentNegotiator;

        bestCommonAudioPt = null;

        if (pts != null) {
            if (pts.size() > 0) {
                localAudioPts.addAll(pts);
            }
        }
    }

    /**
     * Return   The media manager for this negotiator.
     */
    public JingleMediaManager getMediaManager() {
        return mediaManager;
    }

    /**
     * Dispatch an incoming packet. The method is responsible for recognizing
     * the packet type and, depending on the current state, delivering the
     * packet to the right event handler and wait for a response.
     * 
     * @param iq
     *            the packet received
     * @return the new Jingle packet to send.
     * @throws XMPPException
     */
    public List<IQ> dispatchIncomingPacket(IQ iq, String id) throws XMPPException {
        List<IQ> responses = new ArrayList<IQ>();
        IQ response = null;

        if (iq.getType().equals(IQ.Type.ERROR)) {
            // Process errors
            setNegotiatorState(JingleNegotiatorState.FAILED);
            triggerMediaClosed(getBestCommonAudioPt());
            // This next line seems wrong, and may subvert the normal closing process.
            throw new JingleException(iq.getError().getMessage());
        } else if (iq.getType().equals(IQ.Type.RESULT)) {
            // Process ACKs
            if (isExpectedId(iq.getPacketID())) {
                receiveResult(iq);
                removeExpectedId(iq.getPacketID());
            }
        } else if (iq instanceof Jingle) {
            Jingle jingle = (Jingle) iq;
            JingleActionEnum action = jingle.getAction();

            // Only act on the JingleContent sections that belong to this media negotiator.
            for (JingleContent jingleContent : jingle.getContentsList()) {
                if (jingleContent.getName().equals(parentNegotiator.getName())) {

                    JingleDescription description = jingleContent.getDescription();

                    if (description != null) {

                        switch (action) {
                            case CONTENT_ACCEPT:
                                response = receiveContentAcceptAction(jingle, description);
                                break;

                            case CONTENT_MODIFY:
                                break;

                            case CONTENT_REMOVE:
                                break;

                            case SESSION_INFO:
                                response = receiveSessionInfoAction(jingle, description);
                                break;

                            case SESSION_INITIATE:
                                response = receiveSessionInitiateAction(jingle, description);
                                break;

                            case SESSION_ACCEPT:
                                response = receiveSessionAcceptAction(jingle, description);
                                break;

                            default:
                                break;
                        }
                    }
                }
            }

        }

        if (response != null) {
            addExpectedId(response.getPacketID());
            responses.add(response);
        }

        return responses;
    }

    /**
     * Process the ACK of our list of codecs (our offer).
     */
    private Jingle receiveResult(IQ iq) throws XMPPException {
        Jingle response = null;

//        if (!remoteAudioPts.isEmpty()) {
//            // Calculate the best common codec
//            bestCommonAudioPt = calculateBestCommonAudioPt(remoteAudioPts);
//
//            // and send an accept if we havee an agreement...
//            if (bestCommonAudioPt != null) {
//                response = createAcceptMessage();
//            } else {
//                throw new JingleException(JingleError.NO_COMMON_PAYLOAD);
//            }
//        }
        return response;
    }

    /**
      *  The other side has sent us a content-accept.  The payload types in that message may not match with what
      *  we sent, but XEP-167 says that the other side should retain the order of the payload types we first sent.
      *  
      *  This means we can walk through our list, in order, until we find one from their list that matches.  This
      *  will be the best payload type to use.
      *  
      *  @param jingle
      *  @return
      */
    private IQ receiveContentAcceptAction(Jingle jingle, JingleDescription description) throws XMPPException {
        IQ response = null;
        List<PayloadType> offeredPayloads = new ArrayList<PayloadType>();

        offeredPayloads = description.getAudioPayloadTypesList();
        bestCommonAudioPt = calculateBestCommonAudioPt(offeredPayloads);

        if (bestCommonAudioPt == null) {
            
            setNegotiatorState(JingleNegotiatorState.FAILED);
            response = session.createJingleError(jingle, JingleError.NEGOTIATION_ERROR);
            
        } else {

            setNegotiatorState(JingleNegotiatorState.SUCCEEDED);
            triggerMediaEstablished(getBestCommonAudioPt());
            LOGGER.error("Media choice:" + getBestCommonAudioPt().getName());

            response = session.createAck(jingle);
        }

        return response;
    }

    /**
     *  Receive a session-initiate packet.
     *  @param jingle
     *  @param description
     *  @return
     */
    private IQ receiveSessionInitiateAction(Jingle jingle, JingleDescription description) {
        IQ response = null;

        List<PayloadType> offeredPayloads = new ArrayList<PayloadType>();

        offeredPayloads = description.getAudioPayloadTypesList();
        bestCommonAudioPt = calculateBestCommonAudioPt(offeredPayloads);
        
        synchronized (remoteAudioPts) {
            remoteAudioPts.addAll(offeredPayloads);
        }

        // If there are suitable/matching payload types then accept this content.
        if (bestCommonAudioPt != null) {
            // Let thre transport negotiators sort-out connectivity and content-accept instead.
            //response = createAudioPayloadTypesOffer();
            setNegotiatorState(JingleNegotiatorState.PENDING);
        } else {
            // Don't really know what to send here.  XEP-166 is not clear.
            setNegotiatorState(JingleNegotiatorState.FAILED);
        }

        return response;
    }

    /**
     * A content info has been received. This is done for publishing the
     * list of payload types...
     * 
     * @param jin
     *            The input packet
     * @return a Jingle packet
     * @throws JingleException
     */
    private IQ receiveSessionInfoAction(Jingle jingle, JingleDescription description) throws JingleException {
        IQ response = null;
        PayloadType oldBestCommonAudioPt = bestCommonAudioPt;
        List<PayloadType> offeredPayloads;
        boolean ptChange = false;

        offeredPayloads = description.getAudioPayloadTypesList();
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
                    //response = createAcceptMessage();
                }
            } else {
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
     * @param jin
     *            The input packet
     * @return a Jingle packet
     * @throws JingleException
     */
    private IQ receiveSessionAcceptAction(Jingle jingle, JingleDescription description) throws JingleException {
        IQ response = null;
        PayloadType.Audio agreedCommonAudioPt;
        List<PayloadType> offeredPayloads = new ArrayList<PayloadType>();

        if (bestCommonAudioPt == null) {
            // Update the best common audio PT
            bestCommonAudioPt = calculateBestCommonAudioPt(remoteAudioPts);
            //response = createAcceptMessage();
        }

        offeredPayloads = description.getAudioPayloadTypesList();
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

            } else if (offeredPayloads.size() > 1) {
                throw new JingleException(JingleError.MALFORMED_STANZA);
            }
        }

        return response;
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
        return (isEstablished() && ((getNegotiatorState() == JingleNegotiatorState.SUCCEEDED) || (getNegotiatorState() == JingleNegotiatorState.FAILED)));
    }

    // Payload types

    private PayloadType calculateBestCommonAudioPt(List<PayloadType> remoteAudioPts) {
        final ArrayList<PayloadType> commonAudioPtsHere = new ArrayList<PayloadType>();
        final ArrayList<PayloadType> commonAudioPtsThere = new ArrayList<PayloadType>();
        PayloadType result = null;

        if (!remoteAudioPts.isEmpty()) {
            commonAudioPtsHere.addAll(localAudioPts);
            commonAudioPtsHere.retainAll(remoteAudioPts);

            commonAudioPtsThere.addAll(remoteAudioPts);
            commonAudioPtsThere.retainAll(localAudioPts);

            if (!commonAudioPtsHere.isEmpty() && !commonAudioPtsThere.isEmpty()) {

                if (session.getInitiator().equals(session.getConnection().getUser())) {
                    PayloadType.Audio bestPtHere = null;

                    PayloadType payload = mediaManager.getPreferredPayloadType();

                    if (payload != null && payload instanceof PayloadType.Audio)
                        if (commonAudioPtsHere.contains(payload))
                            bestPtHere = (PayloadType.Audio) payload;

                    if (bestPtHere == null)
                        for (PayloadType payloadType : commonAudioPtsHere)
                            if (payloadType instanceof PayloadType.Audio) {
                                bestPtHere = (PayloadType.Audio) payloadType;
                                break;
                            }

                    result = bestPtHere;
                } else {
                    PayloadType.Audio bestPtThere = null;
                    for (PayloadType payloadType : commonAudioPtsThere)
                        if (payloadType instanceof PayloadType.Audio) {
                            bestPtThere = (PayloadType.Audio) payloadType;
                            break;
                        }

                    result = bestPtThere;
                }
            }
        }

        return result;
    }

    /**
     * Adds a payload type to the list of remote payloads.
     * 
     * @param pt
     *            the remote payload type
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
    private Jingle createAudioPayloadTypesOffer() {

        JingleContent jingleContent = new JingleContent(parentNegotiator.getCreator(), parentNegotiator.getName());
        JingleDescription audioDescr = new JingleDescription.Audio();

        // Add the list of payloads for audio and create a
        // JingleDescription
        // where we announce our payloads...
        audioDescr.addAudioPayloadTypes(localAudioPts);
        jingleContent.setDescription(audioDescr);

        Jingle jingle = new Jingle(JingleActionEnum.CONTENT_ACCEPT);
        jingle.addContent(jingleContent);

        return jingle;
    }

    // Predefined messages and Errors

    /**
     * Create an IQ "accept" message.
     */
//    private Jingle createAcceptMessage() {
//        Jingle jout = null;
//
//        // If we have a common best codec, send an accept right now...
//        jout = new Jingle(JingleActionEnum.CONTENT_ACCEPT);
//        JingleContent content = new JingleContent(parentNegotiator.getCreator(), parentNegotiator.getName());
//        content.setDescription(new JingleDescription.Audio(bestCommonAudioPt));
//        jout.addContent(content);
//
//        return jout;
//    }

    // Payloads

    /**
     * Get the best common codec between both parts.
     * 
     * @return The best common PayloadType codec.
     */
    public PayloadType getBestCommonAudioPt() {
        return bestCommonAudioPt;
    }

    // Events

    /**
     * Trigger a session established event.
     * 
     * @param bestPt
     *            payload type that has been agreed.
     */
    protected void triggerMediaEstablished(PayloadType bestPt) {
        List<JingleListener> listeners = getListenersList();
        for (JingleListener li : listeners) {
            if (li instanceof JingleMediaListener) {
                JingleMediaListener mli = (JingleMediaListener) li;
                mli.mediaEstablished(bestPt);
            }
        }
    }

    /**
     * Trigger a jmf closed event.
     * 
     * @param currPt
     *            current payload type that is cancelled.
     */
    protected void triggerMediaClosed(PayloadType currPt) {
        List<JingleListener> listeners = getListenersList();
        for (JingleListener li : listeners) {
            if (li instanceof JingleMediaListener) {
                JingleMediaListener mli = (JingleMediaListener) li;
                mli.mediaClosed(currPt);
            }
        }
    }

    /**
     *  Called from above when starting a new session.
     *  @return
     */
    protected void doStart() {

    }

    /**
     * Terminate the jmf negotiator
     */
    public void close() {
        super.close();
        triggerMediaClosed(getBestCommonAudioPt());
    }

    /**
     *  Create a JingleDescription that matches this negotiator.
     */
    public JingleDescription getJingleDescription() {
        JingleDescription result = null;
        PayloadType payloadType = getBestCommonAudioPt();
        if (payloadType != null) {
            result = new JingleDescription.Audio(payloadType);
        } else {
            // If we haven't settled on a best payload type yet then just use the first one in our local list.
            result = new JingleDescription.Audio();
            result.addAudioPayloadTypes(localAudioPts);
        }
        return result;
    }
}
