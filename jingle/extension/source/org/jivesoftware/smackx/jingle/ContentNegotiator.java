/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright 2003-2007 Jive Software.
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
package org.jivesoftware.smackx.jingle;

import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.jingle.listeners.JingleListener;
import org.jivesoftware.smackx.jingle.listeners.JingleSessionListener;
import org.jivesoftware.smackx.jingle.media.JingleMediaManager;
import org.jivesoftware.smackx.jingle.media.JingleMediaSession;
import org.jivesoftware.smackx.jingle.media.MediaNegotiator;
import org.jivesoftware.smackx.jingle.media.PayloadType;
import org.jivesoftware.smackx.jingle.nat.JingleTransportManager;
import org.jivesoftware.smackx.jingle.nat.TransportCandidate;
import org.jivesoftware.smackx.jingle.nat.TransportNegotiator;
import org.jivesoftware.smackx.packet.Jingle;
import org.jivesoftware.smackx.packet.JingleContent;

/**
 *  @author Jeff Williams
 */
public class ContentNegotiator extends JingleNegotiator {

    public static final String INITIATOR = "initiator";
    public static final String RESPONDER = "responder";

    private List<TransportNegotiator> transportNegotiators;
    private MediaNegotiator mediaNeg; // The description...
    private TransportNegotiator transNeg; // and transport negotiators
    private JingleTransportManager jingleTransportManager;
    private String creator;
    private String name;
    private JingleMediaSession jingleMediaSession = null;

    public ContentNegotiator(JingleSession session, String inCreator, String inName) {
        super(session);
        creator = inCreator;
        name = inName;
        transportNegotiators = new ArrayList<TransportNegotiator>();
    }

    public List<IQ> dispatchIncomingPacket(IQ iq, String id) throws XMPPException {
        List<IQ> responses = new ArrayList<IQ>();

        // First only process IQ packets that contain <content> stanzas that
        // match this media manager.

        if (iq != null) {
            if (iq.getType().equals(IQ.Type.ERROR)) {
                // Process errors
                // TODO getState().eventError(iq);
            } else if (iq.getType().equals(IQ.Type.RESULT)) {
                // Process ACKs
                if (isExpectedId(iq.getPacketID())) {
                    removeExpectedId(iq.getPacketID());
                }
            } else if (iq instanceof Jingle) {
                Jingle jingle = (Jingle) iq;

                // There are 1 or more <content> sections in a Jingle packet.
                // Find out which <content> section belongs to this content negotiator, and
                // then dispatch the Jingle packet to the media and transport negotiators.

                for (JingleContent jingleContent : jingle.getContentsList()) {
                    if (jingleContent.getName().equals(name)) {
                        if (mediaNeg != null) {
                            responses.addAll(mediaNeg.dispatchIncomingPacket(iq, id));
                        }

                        if (transNeg != null) {
                            responses.addAll(transNeg.dispatchIncomingPacket(iq, id));
                        }
                    }

                }
            }
        }
        return responses;
    }

    public String getCreator() {
        return creator;
    }

    public String getName() {
        return name;
    }

    /**
     * Get the JingleMediaSession of this Jingle Session
     * 
     * @return the JingleMediaSession
     */
    public JingleMediaSession getJingleMediaSession() {
        return jingleMediaSession;
    }

    public void addTransportNegotiator(TransportNegotiator transportNegotiator) {
        transportNegotiators.add(transportNegotiator);
    }

    /**
     * @param jingleTransportManager
     */
    public void setJingleTransportManager(JingleTransportManager jingleTransportManager) {
        this.jingleTransportManager = jingleTransportManager;
    }

    /**
     * @return
     */
    public JingleTransportManager getTransportManager() {
        return jingleTransportManager;
    }

    /**
     * Called from above when starting a new session.
     */
    protected void doStart() {
        JingleContent result = new JingleContent(creator, name);

        //        result.setDescription(mediaNeg.start());
        //        result.addJingleTransport(transNeg.start());
        //
        //        return result;

        mediaNeg.start();
        transNeg.start();
    }

    /**
     * Prepare to close the media manager.
     */
    public void close() {
        destroyMediaNegotiator();
        destroyTransportNegotiator();
    }

    /**
     * Obtain the description negotiator for this session
     * 
     * @return the description negotiator
     */
    public MediaNegotiator getMediaNegotiator() {
        return mediaNeg;
    }

    /**
     * Set the jmf negotiator.
     * 
     * @param mediaNeg
     *            the description negotiator to set
     */
    protected void setMediaNegotiator(MediaNegotiator mediaNeg) {
        destroyMediaNegotiator();
        this.mediaNeg = mediaNeg;
    }

    /**
     * Destroy the jmf negotiator.
     */
    protected void destroyMediaNegotiator() {
        if (mediaNeg != null) {
            mediaNeg.close();
            mediaNeg = null;
        }
    }

    /**
     * Obtain the transport negotiator for this session.
     * 
     * @return the transport negotiator instance
     */
    public TransportNegotiator getTransportNegotiator() {
        return transNeg;
    }

    /**
     * Set TransportNegociator
     * 
     * @param transNeg
     *            the transNeg to set
     */
    protected void setTransportNegotiator(TransportNegotiator transNeg) {
        destroyTransportNegotiator();
        this.transNeg = transNeg;
    }

    /**
     * Destroy the transport negotiator.
     */
    protected void destroyTransportNegotiator() {
        if (transNeg != null) {
            transNeg.close();
            transNeg = null;
        }
    }

    /**
     * Return true if the transport and content negotiators have finished
     */
    public boolean isFullyEstablished() {
        boolean result = true;

        MediaNegotiator mediaNeg = getMediaNegotiator();
        if ((mediaNeg == null) || (!mediaNeg.isFullyEstablished())) {
            result = false;
        }

        TransportNegotiator transNeg = getTransportNegotiator();
        if ((transNeg == null) || (!transNeg.isFullyEstablished())) {
            result = false;
        }

        return result;
    }

    public JingleContent getJingleContent() {
        JingleContent result = new JingleContent(creator, name);

        //            PayloadType.Audio bestCommonAudioPt = getMediaNegotiator().getBestCommonAudioPt();
        //            TransportCandidate bestRemoteCandidate = getTransportNegotiator().getBestRemoteCandidate();
        //    
        //            // Ok, send a packet saying that we accept this session
        //            // with the audio payload type and the transport
        //            // candidate
        //            result.setDescription(new JingleDescription.Audio(new PayloadType(bestCommonAudioPt)));
        //            result.addJingleTransport(this.getTransportNegotiator().getJingleTransport(bestRemoteCandidate));
        if (mediaNeg != null) {
            result.setDescription(mediaNeg.getJingleDescription());
        }
        if (transNeg != null) {
            result.addJingleTransport(transNeg.getJingleTransport());
        }

        return result;
    }

    public void triggerContentEstablished() {

        PayloadType bestCommonAudioPt = getMediaNegotiator().getBestCommonAudioPt();
        TransportCandidate bestRemoteCandidate = getTransportNegotiator().getBestRemoteCandidate();
        TransportCandidate acceptedLocalCandidate = getTransportNegotiator().getAcceptedLocalCandidate();

        // Trigger the session established flag
        triggerContentEstablished(bestCommonAudioPt, bestRemoteCandidate, acceptedLocalCandidate);
    }

    /**
     * Trigger a session established event.
     */
    private void triggerContentEstablished(PayloadType pt, TransportCandidate rc, TransportCandidate lc) {

        // Let the session know that we've established a content/media segment.
        JingleSession session = getSession();
        if (session != null) {
            List<JingleListener> listeners = session.getListenersList();
            for (JingleListener li : listeners) {
                if (li instanceof JingleSessionListener) {
                    JingleSessionListener sli = (JingleSessionListener) li;
                    sli.sessionEstablished(pt, rc, lc, session);
                }
            }
        }

        // Create a media session for each media manager in the session.
        if (mediaNeg.getMediaManager() != null) {
            rc.removeCandidateEcho();
            lc.removeCandidateEcho();

            jingleMediaSession = getMediaNegotiator().getMediaManager().createMediaSession(pt, rc, lc, session);
            jingleMediaSession.addMediaReceivedListener(session);
            if (jingleMediaSession != null) {

                jingleMediaSession.startTrasmit();
                jingleMediaSession.startReceive();

                for (TransportCandidate candidate : getTransportNegotiator().getOfferedCandidates())
                    candidate.removeCandidateEcho();
            }
            JingleMediaManager mediaManager = getMediaNegotiator().getMediaManager();
            getSession().addJingleMediaSession(mediaManager.getName(), jingleMediaSession);
        }

    }

    /**
     *  Stop a Jingle media session.
     */
    public void stopJingleMediaSession() {

        if (jingleMediaSession != null) {
            jingleMediaSession.stopTrasmit();
            jingleMediaSession.stopReceive();
        }
    }

    /**
     * The negotiator state for the ContentNegotiators is a special case.
     * It is a roll-up of the sub-negotiator states.
     */
    public JingleNegotiatorState getNegotiatorState() {
        JingleNegotiatorState result = JingleNegotiatorState.PENDING;

        if ((mediaNeg != null) && (transNeg != null)) {

            if ((mediaNeg.getNegotiatorState() == JingleNegotiatorState.SUCCEEDED)
                    || (transNeg.getNegotiatorState() == JingleNegotiatorState.SUCCEEDED))
                result = JingleNegotiatorState.SUCCEEDED;

            if ((mediaNeg.getNegotiatorState() == JingleNegotiatorState.FAILED)
                    || (transNeg.getNegotiatorState() == JingleNegotiatorState.FAILED))
                result = JingleNegotiatorState.FAILED;
        }

        // Store the state (to make it easier to know when debugging.)
        setNegotiatorState(result);

        return result;
    }
}
