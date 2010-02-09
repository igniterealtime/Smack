/**
 * $RCSfile: JingleNegotiator.java,v $
 * $Revision: 1.6 $
 * $Date: 2007/07/17 22:13:16 $
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

import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smackx.jingle.listeners.JingleListener;

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
 * @author Jeff Williams
 */
public abstract class JingleNegotiator {

	private static final SmackLogger LOGGER = SmackLogger.getLogger(JingleNegotiator.class);

	//private Connection connection; // The connection associated

    protected JingleSession session;

    private final List<JingleListener> listeners = new ArrayList<JingleListener>();

    private String expectedAckId;

    private JingleNegotiatorState state;
    
    private boolean isStarted;
    
    /**
     * Default constructor.
     */
    public JingleNegotiator() {
        this(null);
    }

    /**
     * Default constructor with a Connection
     *
     * @param connection the connection associated
     */
    public JingleNegotiator(JingleSession session) {
        this.session = session;
        state = JingleNegotiatorState.PENDING;
    }

    public JingleNegotiatorState getNegotiatorState() {
        return state;
    }

    public void setNegotiatorState(JingleNegotiatorState stateIs) {
        
        JingleNegotiatorState stateWas = state;
        
        LOGGER.debug("Negotiator state change: " + stateWas + "->" + stateIs  + "(" + this.getClass().getSimpleName() + ")");

        switch (stateIs) {
            case PENDING:
                break;

            case FAILED:
                break;

            case SUCCEEDED:
                break;

            default:
                break;
        }

       this.state = stateIs;
    }

    public Connection getConnection() {
        if (session != null) {
            return session.getConnection();
        } else {
            return null;
        }
    }

    /**
      * Get the XMPP connection associated with this negotiation.
      *
      * @return the connection
      */
    public JingleSession getSession() {
        return session;
    }

    /**
     * Set the XMPP connection associated.
     *
     * @param connection the connection to set
     */
    public void setSession(JingleSession session) {
        this.session = session;
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
        } else {
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
    protected List<JingleListener> getListenersList() {
        ArrayList<JingleListener> result;

        synchronized (listeners) {
            result = new ArrayList<JingleListener>(listeners);
        }

        return result;
    }

    /**
     * Dispatch an incoming packet.
     * 
     * The negotiators form a tree relationship that roughly matches the Jingle packet format:
     * 
     * JingleSession
     *      Content Negotiator
     *          Media Negotiator
     *          Transport Negotiator
     *      Content Negotiator
     *          Media Negotiator
     *          Transport Negotiator
     *          
     * <jingle>
     *      <content>
     *          <description>
     *          <transport>
     *      <content>
     *          <description>
     *          <transport>
     *          
     * This way, each segment of a Jingle packet has a corresponding negotiator that know how to deal with that
     * part of the Jingle packet.  It also allows us to support Jingle packets of arbitraty complexity.
     * 
     * Each parent calls dispatchIncomingPacket for each of its children.  The children then pass back a List<> of
     * results that will get sent when we reach the top level negotiator (JingleSession).
     *
     * @param iq the packet received
     * @param id the ID of the response that will be sent
     * @return the new packet to send (either a Jingle or an IQ error).
     * @throws XMPPException
     */
    public abstract List<IQ> dispatchIncomingPacket(IQ iq, String id) throws XMPPException;

    
    public void start() {
    	isStarted = true;
    	doStart();
    }
    
    public boolean isStarted() {
    	return isStarted;
    }
    
    /**
     * Each of the negotiators has their individual behavior when they start.
     */
    protected abstract void doStart();
    
    /**
     * Close the negotiation.
     */
    public void close() {

    }
}
