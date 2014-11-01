/**
 *
 * Copyright 2003-2014 Jive Software.
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
package org.jivesoftware.smack.serverless;


import org.jivesoftware.smack.packet.Message;

/**
 * Interface for handeling link-local service events such as 
 * service closing, service crashes and other events.
 */
public interface LLServiceStateListener {

    /**
     * Notification that the service name was changed.
     *
     * @param newName the new service name
     * @param oldName the previous service name
     */
    public void serviceNameChanged(String newName, String oldName);

    /**
     * Notification that the connection was closed normally.
     */
    public void serviceClosed();

    /**
     * Notification that the connection was closed due to an exception.
     *
     * @param e the exception.
     */
    public void serviceClosedOnError(Exception e);

    /**
     * Notification that a message with unknown presence was received.
     * This could be someone being invisible, meaning no presece is
     * announced.
     *
     * @param e the exception.
     */
    public void unknownOriginMessage(Message e);
}
