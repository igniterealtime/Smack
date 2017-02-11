/**
 *
 * Copyright 2003-2005 Jive Software.
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
package org.jivesoftware.smackx.jingleold.media;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.jingleold.ContentNegotiator;
import org.jivesoftware.smackx.jingleold.JingleActionEnum;
import org.jivesoftware.smackx.jingleold.JingleException;
import org.jivesoftware.smackx.jingleold.JingleNegotiator;
import org.jivesoftware.smackx.jingleold.JingleNegotiatorState;
import org.jivesoftware.smackx.jingleold.JingleSession;
import org.jivesoftware.smackx.jingleold.listeners.JingleListener;
import org.jivesoftware.smackx.jingleold.listeners.JingleMediaListener;
import org.jivesoftware.smackx.jingleold.packet.Jingle;
import org.jivesoftware.smackx.jingleold.packet.JingleContent;
import org.jivesoftware.smackx.jingleold.packet.JingleDescription;
import org.jivesoftware.smackx.jingleold.packet.JingleError;

/**
 * Manager for jmf descriptor negotiation. <p/> <p/> This class is responsible
 * for managing the descriptor negotiation process, handling all the xmpp
 * packets interchange and the stage control. handling all the xmpp packets
 * interchange and the stage control.
 * 
 * @author Thiago Camargo
 */
public class MediaNegotiator extends JingleNegotiator {

    private static final Logger LOGGER = Logger.getLogger(MediaNegotiator.class.getName());

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
     * @param session
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
     * the stanza(/packet) type and, depending on the current state, delivering the
     * stanza(/packet) to the right event handler and wait for a response.
     * 
     * @param iq
     *            the stanza(/packet) received
     * @return the new Jingle stanza(/packet) to send.
     * @throws XMPPException
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    @Override
    public List<IQ> dispatchIncomingPacket(IQ iq, String id) throws XMPPException, NotConnectedException, InterruptedException {
        List<IQ> responses = new ArrayList<IQ>();
        IQ response = null;

        if (iq.getType().equals(IQ.Type.error)) {
            // Process errors
            setNegotiatorState(JingleNegotiatorState.FAILED);
            triggerMediaClosed(getBestCommonAudioPt());
            // This next line seems wrong, and may subvert the normal closing process.
            throw new JingleException(iq.getError().getDescriptiveText());
        } else if (iq.getType().equals(IQ.Type.result)) {
            // Process ACKs
            if (isExpectedId(iq.getStanzaId())) {
                receiveResult(iq);
                removeExpectedId(iq.getStanzaId());
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
            addExpectedId(response.getStanzaId());
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
      *  @return the iq
     * @throws NotConnectedException 
     * @throws InterruptedException 
      */
    private IQ receiveContentAcceptAction(Jingle jingle, JingleDescription description) throws XMPPException, NotConnectedException, InterruptedException {
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
            LOGGER.severe("Media choice:" + getBestCommonAudioPt().getName());

            response = session.createAck(jingle);
        }

        return response;
    }

    /**
     *  Receive a session-initiate packet.
     *  @param jingle
     *  @param description
     *  @return the iq
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

//    /**
//    * Create an offer for the list of audio payload types.
//    * 
//    * @return a new Jingle packet with the list of audio Payload Types
//    */
//    private Jingle createAudioPayloadTypesOffer() {
//
//        JingleContent jingleContent = new JingleContent(parentNegotiator.getCreator(), parentNegotiator.getName());
//        JingleDescription audioDescr = new JingleDescription.Audio();
//
//        // Add the list of payloads for audio and create a
//        // JingleDescription
//        // where we announce our payloads...
//        audioDescr.addAudioPayloadTypes(localAudioPts);
//        jingleContent.setDescription(audioDescr);
//
//        Jingle jingle = new Jingle(JingleActionEnum.CONTENT_ACCEPT);
//        jingle.addContent(jingleContent);
//
//        return jingle;
//    }

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
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    protected void triggerMediaEstablished(PayloadType bestPt) throws NotConnectedException, InterruptedException {
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
     */
    @Override
    protected void doStart() {

    }

    /**
     * Terminate the jmf negotiator.
     */
    @Override
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
