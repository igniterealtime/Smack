package org.jivesoftware.smackx.jingle.nat;

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.jingle.JingleSession;
import org.jivesoftware.smackx.jingle.listeners.CreatedJingleSessionListener;
import org.jivesoftware.smackx.jingle.listeners.JingleSessionListener;
import org.jivesoftware.smackx.jingle.media.PayloadType;

/**
 * $RCSfile: ICETransportManager.java,v $
 * $Revision: 1.1 $
 * $Date: 02/01/2007
 * <p/>
 * Copyright 2003-2006 Jive Software.
 * <p/>
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
public class ICETransportManager extends JingleTransportManager implements JingleSessionListener, CreatedJingleSessionListener {

    ICEResolver iceResolver = null;

    public ICETransportManager(Connection xmppConnection, String server, int port) {
        iceResolver = new ICEResolver(xmppConnection, server, port);
        try {
            iceResolver.initializeAndWait();
        }
        catch (XMPPException e) {
            e.printStackTrace();
        }
    }

    protected TransportResolver createResolver(JingleSession session) {
        try {
            iceResolver.resolve(session);
        }
        catch (XMPPException e) {
            e.printStackTrace();
        }
        return iceResolver;
    }

    // Implement a Session Listener to relay candidates after establishment

    public void sessionEstablished(PayloadType pt, TransportCandidate rc, TransportCandidate lc, JingleSession jingleSession) {
        if (lc instanceof ICECandidate) {
            if (((ICECandidate) lc).getType().equals("relay")) {
                RTPBridge rtpBridge = RTPBridge.relaySession(lc.getConnection(), lc.getSessionId(), lc.getPassword(), rc, lc);
            }
        }
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
