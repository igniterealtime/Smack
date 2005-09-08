/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright 2003-2004 Jive Software.
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
 * events. Listeners are registered with XMPPConnection objects.
 *
 * @see XMPPConnection#addConnectionListener
 * @see XMPPConnection#removeConnectionListener
 * 
 * @author Matt Tucker
 */
public interface ConnectionListener {

    /**
     * Notification that the connection was closed normally.
     */
    public void connectionClosed();

    /**
     * Notification that the connection was closed due to an exception.
     *
     * @param e the exception.
     */
    public void connectionClosedOnError(Exception e);
}