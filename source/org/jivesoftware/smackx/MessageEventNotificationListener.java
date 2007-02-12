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

package org.jivesoftware.smackx;

/**
 *
 * A listener that is fired anytime a message event notification is received.
 * Message event notifications are received as a consequence of the request
 * to receive notifications when sending a message.
 *
 * @author Gaston Dombiak
 */
public interface MessageEventNotificationListener {

    /**
     * Called when a notification of message delivered is received.
     *  
     * @param from the user that sent the notification.
     * @param packetID the id of the message that was sent.
     */
    public void deliveredNotification(String from, String packetID);

    /**
     * Called when a notification of message displayed is received.
     *  
     * @param from the user that sent the notification.
     * @param packetID the id of the message that was sent.
     */
    public void displayedNotification(String from, String packetID);

    /**
     * Called when a notification that the receiver of the message is composing a reply is 
     * received.
     *  
     * @param from the user that sent the notification.
     * @param packetID the id of the message that was sent.
     */
    public void composingNotification(String from, String packetID);

    /**
     * Called when a notification that the receiver of the message is offline is received.
     *  
     * @param from the user that sent the notification.
     * @param packetID the id of the message that was sent.
     */
    public void offlineNotification(String from, String packetID);

    /**
     * Called when a notification that the receiver of the message cancelled the reply 
     * is received.
     *  
     * @param from the user that sent the notification.
     * @param packetID the id of the message that was sent.
     */
    public void cancelledNotification(String from, String packetID);
}
