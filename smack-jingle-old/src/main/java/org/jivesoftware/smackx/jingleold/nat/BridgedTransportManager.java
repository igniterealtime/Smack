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
package org.jivesoftware.smackx.jingleold.nat;

import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.jingleold.JingleSession;
import org.jivesoftware.smackx.jingleold.listeners.CreatedJingleSessionListener;
import org.jivesoftware.smackx.jingleold.listeners.JingleSessionListener;
import org.jivesoftware.smackx.jingleold.media.PayloadType;

/**
 * A Jingle Transport Manager implementation to be used for NAT Networks.
 * This kind of transport needs that the connected XMPP Server provide a Bridge Service. (http://www.jivesoftware.com/protocol/rtpbridge)
 * To relay the jmf outside the NAT.
 *
 * @author Thiago Camargo
 */
public class BridgedTransportManager extends JingleTransportManager implements JingleSessionListener, CreatedJingleSessionListener {

    XMPPConnection xmppConnection;

    public BridgedTransportManager(XMPPConnection xmppConnection) {
        super();
        this.xmppConnection = xmppConnection;
    }

    /**
     * Return the correspondent resolver
     *
     * @param session correspondent Jingle Session
     * @return resolver
     */
    @Override
    protected TransportResolver createResolver(JingleSession session) {
        BridgedResolver bridgedResolver = new BridgedResolver(this.xmppConnection);
        return bridgedResolver;
    }

    // Implement a Session Listener to relay candidates after establishment

    @Override
    public void sessionEstablished(PayloadType pt, TransportCandidate rc, TransportCandidate lc, JingleSession jingleSession) throws NotConnectedException, InterruptedException {
        RTPBridge rtpBridge = RTPBridge.relaySession(lc.getConnection(), lc.getSessionId(), lc.getPassword(), rc, lc);
    }

    @Override
    public void sessionDeclined(String reason, JingleSession jingleSession) {
    }

    @Override
    public void sessionRedirected(String redirection, JingleSession jingleSession) {
    }

    @Override
    public void sessionClosed(String reason, JingleSession jingleSession) {
    }

    @Override
    public void sessionClosedOnError(XMPPException e, JingleSession jingleSession) {
    }

    @Override
    public void sessionMediaReceived(JingleSession jingleSession, String participant) {
        // Do Nothing
    }

    // Session Created

    @Override
    public void sessionCreated(JingleSession jingleSession) {
        jingleSession.addListener(this);
    }
}
