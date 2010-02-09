/**
 * $RCSfile: BridgedTransportManager.java,v $
 * $Revision: 1.1 $
 * $Date: 15/11/2006
 *
 * Copyright 2003-2006 Jive Software.
 *
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jivesoftware.smackx.jingle.nat;

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.jingle.JingleSession;
import org.jivesoftware.smackx.jingle.listeners.CreatedJingleSessionListener;
import org.jivesoftware.smackx.jingle.listeners.JingleSessionListener;
import org.jivesoftware.smackx.jingle.media.PayloadType;

/**
 * A Jingle Transport Manager implementation to be used for NAT Networks.
 * This kind of transport needs that the connected XMPP Server provide a Bridge Service. (http://www.jivesoftware.com/protocol/rtpbridge)
 * To relay the jmf outside the NAT.
 *
 * @author Thiago Camargo
 */
public class BridgedTransportManager extends JingleTransportManager implements JingleSessionListener, CreatedJingleSessionListener {

    Connection xmppConnection;

    public BridgedTransportManager(Connection xmppConnection) {
        super();
        this.xmppConnection = xmppConnection;
    }

    /**
     * Return the correspondent resolver
     *
     * @param session correspondent Jingle Session
     * @return resolver
     */
    protected TransportResolver createResolver(JingleSession session) {
        BridgedResolver bridgedResolver = new BridgedResolver(this.xmppConnection);
        return bridgedResolver;
    }

    // Implement a Session Listener to relay candidates after establishment

    public void sessionEstablished(PayloadType pt, TransportCandidate rc, TransportCandidate lc, JingleSession jingleSession) {
        RTPBridge rtpBridge = RTPBridge.relaySession(lc.getConnection(), lc.getSessionId(), lc.getPassword(), rc, lc);
    }

    public void sessionDeclined(String reason, JingleSession jingleSession) {
    }

    public void sessionRedirected(String redirection, JingleSession jingleSession) {
    }

    public void sessionClosed(String reason, JingleSession jingleSession) {
    }

    public void sessionClosedOnError(XMPPException e, JingleSession jingleSession) {
    }

    public void sessionMediaReceived(JingleSession jingleSession, String participant) {
        // Do Nothing
    }

    // Session Created

    public void sessionCreated(JingleSession jingleSession) {
        jingleSession.addListener(this);
    }
}
