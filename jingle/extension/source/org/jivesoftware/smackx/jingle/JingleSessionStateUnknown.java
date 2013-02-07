/**
 * $RCSfile$
 * $Revision: $
 * $Date: $
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

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.jingle.media.JingleMediaManager;
import org.jivesoftware.smackx.jingle.media.MediaNegotiator;
import org.jivesoftware.smackx.jingle.media.PayloadType;
import org.jivesoftware.smackx.jingle.nat.JingleTransportManager;
import org.jivesoftware.smackx.jingle.nat.TransportNegotiator;
import org.jivesoftware.smackx.jingle.nat.TransportResolver;
import org.jivesoftware.smackx.packet.Jingle;
import org.jivesoftware.smackx.packet.JingleContent;
import org.jivesoftware.smackx.packet.JingleDescription;
import org.jivesoftware.smackx.packet.JingleError;
import org.jivesoftware.smackx.packet.JingleTransport;

/**
 *  @author Jeff Williams
 *  @see JingleSessionState
 */
public class JingleSessionStateUnknown extends JingleSessionState {
    private static JingleSessionStateUnknown INSTANCE = null;

    protected JingleSessionStateUnknown() {
        // Prevent instantiation of the class.
    }

    /**
     *  A thread-safe means of getting the one instance of this class.
     *  @return The singleton instance of this class.
     */
    public synchronized static JingleSessionState getInstance() {
        if (INSTANCE == null) {
            INSTANCE = new JingleSessionStateUnknown();
        }
        return INSTANCE;
    }

    public void enter() {
        // TODO Auto-generated method stub

    }

    public void exit() {
        // TODO Auto-generated method stub

    }

    public IQ processJingle(JingleSession session, Jingle jingle, JingleActionEnum action) {
        IQ response = null;

        switch (action) {
            case SESSION_INITIATE:
                response = receiveSessionInitiateAction(session, jingle);
                break;

            case SESSION_TERMINATE:
                response = receiveSessionTerminateAction(session, jingle);
                break;

            default:
                // Anything other than session-initiate is an error.
                response = session.createJingleError(jingle, JingleError.MALFORMED_STANZA);
                break;
        }

        return response;
    }

    /**
     * In the UNKNOWN state we received a <session-initiate> action.
     * This method processes that action.
     */

    private IQ receiveSessionInitiateAction(JingleSession session, Jingle inJingle) {

        IQ response = null;
        boolean shouldAck = true;

        // According to XEP-166 when we get a session-initiate we need to check for:
        //      1. Initiator unknown
        //      2. Receiver redirection
        //      3. Does not support Jingle
        //      4. Does not support any <description> formats
        //      5. Does not support any <transport> formats
        // If all of the above are OK then we send an IQ type = result to ACK the session-initiate.

        // 1. Initiator unknown
        // TODO

        // 2. Receiver redirection
        // TODO

        // 3. Does not support Jingle
        // Handled by Smack's lower layer.

        // 4. Does not support any <description> formats
        // TODO

        // 5. Does not support any <transport> formats
        // TODO

        if (!shouldAck) {

            response = session.createJingleError(inJingle, JingleError.NEGOTIATION_ERROR);

        } else {

            // Create the Ack
            response = session.createAck(inJingle);

            session.setSessionState(JingleSessionStatePending.getInstance());

            // Now set up all of the initial content negotiators for the session.
            for (JingleContent jingleContent : inJingle.getContentsList()) {
                // First create the content negotiator for this <content> section.
                ContentNegotiator contentNeg = new ContentNegotiator(session, jingleContent.getCreator(), jingleContent
                        .getName());

                // Get the media negotiator that goes with the <description> of this content.
                JingleDescription jingleDescription = jingleContent.getDescription();

                // Loop through each media manager looking for the ones that matches the incoming 
                // session-initiate <content> choices.
                // (Set the first media manager as the default, so that in case things don't match we can still negotiate.)
                JingleMediaManager chosenMediaManager = session.getMediaManagers().get(0);
                for (JingleMediaManager mediaManager : session.getMediaManagers()) {
                    boolean matches = true;
                    for (PayloadType mediaPayloadType : mediaManager.getPayloads()) {
                        for (PayloadType descPayloadType2 : jingleDescription.getPayloadTypesList()) {
                            if (mediaPayloadType.getId() != descPayloadType2.getId()) {
                                matches = false;
                            }
                        }
                        if (matches) {
                            chosenMediaManager = mediaManager;
                        }
                    }
                }

                // Create the media negotiator for this content description.
                contentNeg.setMediaNegotiator(new MediaNegotiator(session, chosenMediaManager, jingleDescription
                        .getPayloadTypesList(), contentNeg));

                // For each transport type in this content, try to find the corresponding transport manager.
                // Then create a transport negotiator for that transport.
                for (JingleTransport jingleTransport : jingleContent.getJingleTransportsList()) {
                    for (JingleMediaManager mediaManager : session.getMediaManagers()) {

                        JingleTransportManager transportManager = mediaManager.getTransportManager();
                        TransportResolver resolver = null;
                        try {
                            resolver = transportManager.getResolver(session);
                        } catch (XMPPException e) {
                            e.printStackTrace();
                        }

                        if (resolver.getType().equals(TransportResolver.Type.rawupd)) {
                            contentNeg.setTransportNegotiator(new TransportNegotiator.RawUdp(session, resolver, contentNeg));
                        }
                        if (resolver.getType().equals(TransportResolver.Type.ice)) {
                            contentNeg.setTransportNegotiator(new TransportNegotiator.Ice(session, resolver, contentNeg));
                        }
                    }
                }

                // Add the content negotiator to the session.
                session.addContentNegotiator(contentNeg);
            }

            // Now setup to track the media negotiators, so that we know when (if) to send a session-accept.
            session.setupListeners();
        }

        return response;
    }

    /**
     * Receive and process the <session-terminate> action.
     */
    private IQ receiveSessionTerminateAction(JingleSession session, Jingle jingle) {

        // According to XEP-166 the only thing we can do is ack.
        IQ response = session.createAck(jingle);

        try {
            session.terminate("Closed remotely");
        } catch (XMPPException e) {
            e.printStackTrace();
        }

        return response;
    }

}
