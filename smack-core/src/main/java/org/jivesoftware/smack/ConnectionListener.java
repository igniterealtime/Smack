/**
 *
 * Copyright 2003-2007 Jive Software.
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

package org.jivesoftware.smack;

/**
 * Interface that allows for implementing classes to listen for connection closing
 * and reconnection events. Listeners are registered with XMPPConnection objects.
 *
 * @see XMPPConnection#addConnectionListener
 * @see XMPPConnection#removeConnectionListener
 * 
 * @author Matt Tucker
 */
public interface ConnectionListener {

    /**
     * Notification that the connection has been successfully connected to the remote endpoint (e.g. the XMPP server).
     * <p>
     * Note that the connection is likely not yet authenticated and therefore only limited operations like registering
     * an account may be possible.
     * </p>
     *
     * @param connection the XMPPConnection which successfully connected to its endpoint.
     */
    void connected(XMPPConnection connection);

    /**
     * Notification that the connection has been authenticated.
     *
     * @param connection the XMPPConnection which successfully authenticated.
     * @param resumed true if a previous XMPP session's stream was resumed.
     */
    void authenticated(XMPPConnection connection, boolean resumed);

    /**
     * Notification that the connection was closed normally.
     */
    void connectionClosed();

    /**
     * Notification that the connection was closed due to an exception. When
     * abruptly disconnected it is possible for the connection to try reconnecting
     * to the server.
     *
     * @param e the exception.
     */
    void connectionClosedOnError(Exception e);

}
