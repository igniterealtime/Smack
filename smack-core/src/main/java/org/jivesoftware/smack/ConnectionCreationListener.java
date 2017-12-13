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
 * Implementors of this interface will be notified when a new {@link XMPPConnection}
 * has been created. The newly created connection will not be actually connected to
 * the server. Use {@link XMPPConnectionRegistry#addConnectionCreationListener(ConnectionCreationListener)}
 * to add new listeners.
 *
 * @author Gaston Dombiak
 */
public interface ConnectionCreationListener {

    /**
     * Notification that a new connection has been created. The new connection
     * will not yet be connected to the server.
     * 
     * @param connection the newly created connection.
     */
    void connectionCreated(XMPPConnection connection);

}
