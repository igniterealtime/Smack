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
package org.jivesoftware.smackx.jingleold.listeners;

import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPException;

import org.jivesoftware.smackx.jingleold.nat.TransportCandidate;

/**
 * Interface for listening to transport events.
 * 
 * @author Thiago Camargo
 */
public interface JingleTransportListener extends JingleListener {

    /**
     * Notification that the transport has been established.
     *
     * @param local  The transport candidate that has been used for listening
     *               in the local machine
     * @param remote The transport candidate that has been used for
     *               transmitting to the remote machine
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    void transportEstablished(TransportCandidate local,
                                     TransportCandidate remote) throws NotConnectedException, InterruptedException;

    /**
     * Notification that a transport must be cancelled.
     *
     * @param cand The transport candidate that must be cancelled. A value
     *             of "null" means all the transports for this session.
     */
    void transportClosed(TransportCandidate cand);

    /**
     * Notification that the transport was closed due to an exception.
     *
     * @param e the exception.
     */
    void transportClosedOnError(XMPPException e);
}

