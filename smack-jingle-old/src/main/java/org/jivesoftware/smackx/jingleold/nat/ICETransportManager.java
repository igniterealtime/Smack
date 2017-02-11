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

import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.jingleold.JingleSession;
import org.jivesoftware.smackx.jingleold.listeners.CreatedJingleSessionListener;
import org.jivesoftware.smackx.jingleold.listeners.JingleSessionListener;
import org.jivesoftware.smackx.jingleold.media.PayloadType;
import org.jivesoftware.smackx.jingleold.nat.ICECandidate.Type;

public class ICETransportManager extends JingleTransportManager implements JingleSessionListener, CreatedJingleSessionListener {
    private static final Logger LOGGER = Logger.getLogger(ICETransportManager.class.getName());

    ICEResolver iceResolver = null;

    public ICETransportManager(XMPPConnection xmppConnection, String server, int port) {
        iceResolver = new ICEResolver(xmppConnection, server, port);
        try {
            iceResolver.initializeAndWait();
        }
        catch (Exception e) {
            LOGGER.log(Level.WARNING, "exception", e);
        }
    }

    @Override
    protected TransportResolver createResolver(JingleSession session) throws SmackException, InterruptedException {
        try {
            iceResolver.resolve(session);
        }
        catch (XMPPException e) {
            LOGGER.log(Level.WARNING, "exception", e);
        }
        return iceResolver;
    }

    // Implement a Session Listener to relay candidates after establishment

    @Override
    public void sessionEstablished(PayloadType pt, TransportCandidate rc, TransportCandidate lc, JingleSession jingleSession) throws NotConnectedException, InterruptedException {
        if (lc instanceof ICECandidate) {
            if (((ICECandidate) lc).getType().equals(Type.relay)) {
                RTPBridge rtpBridge = RTPBridge.relaySession(lc.getConnection(), lc.getSessionId(), lc.getPassword(), rc, lc);
            }
        }
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
