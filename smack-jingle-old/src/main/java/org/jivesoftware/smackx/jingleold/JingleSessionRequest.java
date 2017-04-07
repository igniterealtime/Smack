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
package org.jivesoftware.smackx.jingleold;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.jingleold.packet.Jingle;
import org.jxmpp.jid.Jid;

/**
 * A Jingle session request.
 * <p/>
 * This class is a facade of a received Jingle request. The user can have direct
 * access to the Jingle stanza(/packet) (<i>JingleSessionRequest.getJingle() </i>) of
 * the request or can use the convencience methods provided by this class.
 *
 * @author Alvaro Saurin
 */
public class JingleSessionRequest {

    private static final Logger LOGGER = Logger.getLogger(JingleSessionRequest.class.getName());

    private final Jingle jingle; // The Jingle packet

    private final JingleManager manager; // The manager associated to this

    // request

    /**
     * A recieve request is constructed from the Jingle Initiation request
     * received from the initator.
     *
     * @param manager The manager handling this request
     * @param jingle  The jingle IQ recieved from the initiator.
     */
    public JingleSessionRequest(JingleManager manager, Jingle jingle) {
        this.manager = manager;
        this.jingle = jingle;
    }

    /**
     * Returns the fully-qualified jabber ID of the user that requested this
     * session.
     *
     * @return Returns the fully-qualified jabber ID of the user that requested
     *         this session.
     */
    public Jid getFrom() {
        return jingle.getFrom();
    }

    /**
     * Returns the session ID that uniquely identifies this session.
     *
     * @return Returns the session ID that uniquely identifies this session
     */
    public String getSessionID() {
        return jingle.getSid();
    }

    /**
     * Returns the Jingle stanza(/packet) that was sent by the requester which contains
     * the parameters of the session.
     */
    public Jingle getJingle() {
        return jingle;
    }

    /**
     * Accepts this request and creates the incoming Jingle session.
     *
     * @param pts list of supported Payload Types
     * @return Returns the <b><i>IncomingJingleSession</b></i> on which the
     *         negotiation can be carried out.
     */
//    public synchronized JingleSession accept(List<PayloadType> pts) throws XMPPException {
//        JingleSession session = null;
//        synchronized (manager) {
//            session = manager.createIncomingJingleSession(this, pts);
//            // Acknowledge the IQ reception
//            session.setSid(this.getSessionID());
//            //session.sendAck(this.getJingle());
//            //session.respond(this.getJingle());
//        }
//        return session;
//    }

    /**
     * Accepts this request and creates the incoming Jingle session.
     *
     * @return Returns the <b><i>IncomingJingleSession</b></i> on which the
     *         negotiation can be carried out.
     * @throws SmackException 
     * @throws InterruptedException 
     */
    public synchronized JingleSession accept() throws XMPPException, SmackException, InterruptedException {
        JingleSession session = null;
        synchronized (manager) {
            session = manager.createIncomingJingleSession(this);
            // Acknowledge the IQ reception
            session.setSid(this.getSessionID());
            //session.sendAck(this.getJingle());
            session.updatePacketListener();
            session.receivePacketAndRespond(this.getJingle());
        }
        return session;
    }

    /**
     * Rejects the session request.
     */
    public synchronized void reject() {
        JingleSession session = null;
        synchronized (manager) {
            try {
                session = manager.createIncomingJingleSession(this);
                // Acknowledge the IQ reception
                session.setSid(this.getSessionID());
                //session.sendAck(this.getJingle());
                session.updatePacketListener();
                session.terminate("Declined");
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Exception in reject", e);
            }
        }
     }
}
