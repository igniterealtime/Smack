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
    public void connected(XMPPConnection connection);

    /**
     * Notification that the connection has been authenticated.
     *
     * @param connection the XMPPConnection which successfully authenticated.
     * @param resumed true if a previous XMPP session's stream was resumed.
     */
    public void authenticated(XMPPConnection connection, boolean resumed);

    /**
     * Notification that the connection was closed normally.
     */
    public void connectionClosed();

    /**
     * Notification that the connection was closed due to an exception. When
     * abruptly disconnected it is possible for the connection to try reconnecting
     * to the server.
     *
     * @param e the exception.
     */
    public void connectionClosedOnError(Exception e);

    /**
     * The connection has reconnected successfully to the server. Connections will
     * reconnect to the server when the previous socket connection was abruptly closed.
     */
    public void reconnectionSuccessful();

    // The next two methods *must* only be invoked by ReconnectionManager

    /**
     * The connection will retry to reconnect in the specified number of seconds.
     * <p>
     * Note: This method is only called if {@link ReconnectionManager#isAutomaticReconnectEnabled()} returns true, i.e.
     * only when the reconnection manager is enabled for the connection.
     * </p>
     * 
     * @param seconds remaining seconds before attempting a reconnection.
     */
    public void reconnectingIn(int seconds);

    /**
     * An attempt to connect to the server has failed. The connection will keep trying reconnecting to the server in a
     * moment.
     * <p>
     * Note: This method is only called if {@link ReconnectionManager#isAutomaticReconnectEnabled()} returns true, i.e.
     * only when the reconnection manager is enabled for the connection.
     * </p>
     *
     * @param e the exception that caused the reconnection to fail.
     */
    public void reconnectionFailed(Exception e);
}
