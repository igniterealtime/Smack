package org.jivesoftware.smackx.jingle.nat;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.jingle.ContentNegotiator;
import org.jivesoftware.smackx.jingle.JingleSession;
import org.jivesoftware.smackx.jingle.listeners.CreatedJingleSessionListener;
import org.jivesoftware.smackx.jingle.listeners.JingleSessionListener;
import org.jivesoftware.smackx.jingle.media.PayloadType;

/**
 * $RCSfile: FixedTransportManager.java,v $
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

/**
 * A Fixed Jingle Transport Manager implementation.
 *  
 */
public class FixedTransportManager extends JingleTransportManager implements JingleSessionListener, CreatedJingleSessionListener {
    
    FixedResolver resolver;
    
    public FixedTransportManager(FixedResolver inResolver) {
        resolver = inResolver;
    }

    protected TransportResolver createResolver(JingleSession session) {
        return resolver;
    }
    
    public void sessionEstablished(PayloadType pt, TransportCandidate rc, TransportCandidate lc, JingleSession jingleSession) {
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
    
    public void sessionCreated(JingleSession jingleSession) {
        jingleSession.addListener(this);
    }
}
