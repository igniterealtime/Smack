/**
 * $RCSfile: JingleSessionListener.java,v $
 * $Revision: 1.1 $
 * $Date: 2007/07/02 17:41:12 $11-07-2006
 *
 * Copyright 2003-2006 Jive Software.
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
package org.jivesoftware.smackx.jingle.listeners;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.jingle.JingleSession;
import org.jivesoftware.smackx.jingle.media.PayloadType;
import org.jivesoftware.smackx.jingle.nat.TransportCandidate;

/**
 * Interface for listening for session events.
 * @author Thiago Camargo
 */
public interface JingleSessionListener extends JingleListener {
    /**
     * Notification that the session has been established. Arguments specify
     * the payload type and transport to use.
     *
     * @param pt            the Payload tyep to use
     * @param remoteCandidate            the remote candidate to use for connecting to the remote
     *                      service.
     * @param localCandidate            the local candidate where we must listen for connections
     * @param jingleSession Session that called the method
     */
    public void sessionEstablished(PayloadType pt, TransportCandidate remoteCandidate,
                                   TransportCandidate localCandidate, JingleSession jingleSession);

    /**
     * Notification that the session was declined.
     *
     * @param reason        the reason (if any).
     * @param jingleSession Session that called the method
     */
    public void sessionDeclined(String reason, JingleSession jingleSession);

    /**
     * Notification that the session was redirected.
     *
     * @param redirection
     * @param jingleSession session that called the method
     */
    public void sessionRedirected(String redirection, JingleSession jingleSession);

    /**
     * Notification that the session was closed normally.
     *
     * @param reason        the reason (if any).
     * @param jingleSession Session that called the method
     */
    public void sessionClosed(String reason, JingleSession jingleSession);

    /**
     * Notification that the session was closed due to an exception.
     *
     * @param e             the exception.
     * @param jingleSession session that called the method
     */
    public void sessionClosedOnError(XMPPException e, JingleSession jingleSession);

    /**
     * Notification that the Media has arrived for this session.
     *
     * @param jingleSession session that called the method
     * @param participant description of the participant
     */
    public void sessionMediaReceived(JingleSession jingleSession, String participant);
        
}
