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
package org.jivesoftware.smackx.jingle;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.jingle.listeners.JingleListener;
import org.jivesoftware.smackx.packet.Jingle;
import org.jivesoftware.smackx.packet.JingleError;

import java.util.ArrayList;

/**
 * Basic Jingle negotiator.
 * <p/>
 * </p>
 * <p/>
 * JingleNegotiator implements some basic behavior for every Jingle negotiation.
 * It implements a "state" pattern: each stage should process Jingle packets and
 * act depending on the current state in the negotiation...
 * <p/>
 * </p>
 *
 * @author Alvaro Saurin
 */
public abstract class JingleNegotiator {

    private State state; // Current negotiation state

    private XMPPConnection connection; // The connection associated

    private final ArrayList listeners = new ArrayList();

    private String expectedAckId;

    /**
     * Default constructor.
     */
    public JingleNegotiator() {
        this(null);
    }

    /**
     * Default constructor with a XMPPConnection
     *
     * @param connection the connection associated
     */
    public JingleNegotiator(XMPPConnection connection) {
        this.connection = connection;
        state = null;
    }

    /**
     * Get the XMPP connection associated with this negotiation.
     *
     * @return the connection
     */
    public XMPPConnection getConnection() {
        return connection;
    }

    /**
     * Set the XMPP connection associated.
     *
     * @param connection the connection to set
     */
    public void setConnection(XMPPConnection connection) {
        this.connection = connection;
    }

    /**
     * Inform if current state is null
     *
     * @return true if current state is null
     */
    public boolean invalidState() {
        return state == null;
    }

    /**
     * Return the current state
     *
     * @return the state
     */
    public State getState() {
        return state;
    }

    /**
     * Return the current state class
     *
     * @return the state
     */
    public Class getStateClass() {
        if (state != null) {
            return state.getClass();
        }
        else {
            return Object.class;
        }
    }

    /**
     * Set the new state.
     *
     * @param newState the state to set
     * @throws XMPPException
     */
    protected void setState(State newState) {
        boolean transition = newState != state;

        if (transition && state != null) {
            state.eventExit();
        }

        state = newState;

        if (transition && state != null) {
            state.eventEnter();
        }
    }

    // Acks management

    /**
     * Add expected ID
     *
     * @param id
     */
    public void addExpectedId(String id) {
        expectedAckId = id;
    }

    /**
     * Check if the passed ID is the expected ID
     *
     * @param id
     * @return
     */
    public boolean isExpectedId(String id) {
        if (id != null) {
            return id.equals(expectedAckId);
        }
        else {
            return false;
        }
    }

    /**
     * Remove and expected ID
     *
     * @param id
     */
    public void removeExpectedId(String id) {
        addExpectedId((String) null);
    }

    // Listeners

    /**
     * Add a Jingle session listener to listen to incoming session requests.
     *
     * @param li The listener
     * @see org.jivesoftware.smackx.jingle.listeners.JingleListener
     */
    public void addListener(JingleListener li) {
        synchronized (listeners) {
            listeners.add(li);
        }
    }

    /**
     * Removes a Jingle session listener.
     *
     * @param li The jingle session listener to be removed
     * @see org.jivesoftware.smackx.jingle.listeners.JingleListener
     */
    public void removeListener(JingleListener li) {
        synchronized (listeners) {
            listeners.remove(li);
        }
    }

    /**
     * Get a copy of the listeners
     *
     * @return a copy of the listeners
     */
    protected ArrayList getListenersList() {
        ArrayList result;

        synchronized (listeners) {
            result = new ArrayList(listeners);
        }

        return result;
    }

    /**
     * Dispatch an incomming packet. This method is responsible for recognizing
     * the packet type and, depending on the current state, deliverying the
     * packet to the right event handler and wait for a response.
     *
     * @param iq the packet received
     * @param id the ID of the response that will be sent
     * @return the new packet to send (either a Jingle or an IQ error).
     * @throws XMPPException
     */
    public abstract IQ dispatchIncomingPacket(IQ iq, String id)
            throws XMPPException;

    /**
     * Close the negotiation.
     */
    public void close() {
        setState(null);
    }

    /**
     * A Jingle exception.
     *
     * @author Alvaro Saurin <alvaro.saurin@gmail.com>
     */
    public static class JingleException extends XMPPException {

        private final JingleError error;

        /**
         * Default constructor.
         */
        public JingleException() {
            super();
            error = null;
        }

        /**
         * Constructor with an error message.
         *
         * @param msg The message.
         */
        public JingleException(String msg) {
            super(msg);
            error = null;
        }

        /**
         * Constructor with an error response.
         *
         * @param error The error message.
         */
        public JingleException(JingleError error) {
            super();
            this.error = error;
        }

        /**
         * Return the error message.
         *
         * @return the error
         */
        public JingleError getError() {
            return error;
        }
    }

    /**
     * Negotiation state and events.
     * <p/>
     * </p>
     * <p/>
     * Describes the negotiation stage.
     */
    public static class State {

        private JingleNegotiator neg; // The negotiator

        /**
         * Default constructor, with a reference to the negotiator.
         *
         * @param neg The negotiator instance.
         */
        public State(JingleNegotiator neg) {
            this.neg = neg;
        }

        /**
         * Get the negotiator
         *
         * @return the negotiator.
         */
        public JingleNegotiator getNegotiator() {
            return neg;
        }

        /**
         * Set the negotiator.
         *
         * @param neg the neg to set
         */
        public void setNegotiator(JingleNegotiator neg) {
            this.neg = neg;
        }

        // State transition events

        public Jingle eventAck(IQ iq) throws XMPPException {
            // We have received an Ack
            return null;
        }

        public void eventError(IQ iq) throws XMPPException {
            throw new JingleException(iq.getError().getMessage());
        }

        public Jingle eventInvite() throws XMPPException {
            throw new IllegalStateException(
                    "Negotiation can not be started in this state.");
        }

        public Jingle eventInitiate(Jingle jin) throws XMPPException {
            return null;
        }

        public Jingle eventAccept(Jingle jin) throws XMPPException {
            return null;
        }

        public Jingle eventRedirect(Jingle jin) throws XMPPException {
            return null;
        }

        public Jingle eventModify(Jingle jin) throws XMPPException {
            return null;
        }

        public Jingle eventDecline(Jingle jin) throws XMPPException {
            return null;
        }

        public Jingle eventInfo(Jingle jin) throws XMPPException {
            return null;
        }

        public Jingle eventTerminate(Jingle jin) throws XMPPException {
            if (neg != null) {
                neg.close();
            }
            return null;
        }

        public void eventEnter() {
        }

        public void eventExit() {
            if (neg != null) {
                neg.removeExpectedId(null);
            }
        }
    }
}
