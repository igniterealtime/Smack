/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright 2003-2007 Jive Software.
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

package org.jivesoftware.smack;

/**
 * Interface that allows for implementing classes to listen for connection closing
 * and reconnection events. Listeners are registered with Connection objects.
 *
 * @see Connection#addConnectionListener
 * @see Connection#removeConnectionListener
 * 
 * @author Matt Tucker
 */
public interface ConnectionListener {

    /**
     * Notification that the connection was closed normally or that the reconnection
     * process has been aborted.
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
     * The connection will retry to reconnect in the specified number of seconds.
     * 
     * @param seconds remaining seconds before attempting a reconnection.
     */
    public void reconnectingIn(int seconds);
    
    /**
     * The connection has reconnected successfully to the server. Connections will
     * reconnect to the server when the previous socket connection was abruptly closed.
     */
    public void reconnectionSuccessful();
    
    /**
     * An attempt to connect to the server has failed. The connection will keep trying
     * reconnecting to the server in a moment.
     *
     * @param e the exception that caused the reconnection to fail.
     */
    public void reconnectionFailed(Exception e);
}