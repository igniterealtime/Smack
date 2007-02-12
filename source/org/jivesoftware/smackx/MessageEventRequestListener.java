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
 * A listener that is fired anytime a message event request is received.
 * Message event requests are received when the received message includes an extension 
 * like this:
 * 
 * <pre>
 * &lt;x xmlns='jabber:x:event'&gt;
 *  &lt;offline/&gt;
 *  &lt;delivered/&gt;
 *  &lt;composing/&gt;
 * &lt;/x&gt;
 * </pre>
 * 
 * In this example you can see that the sender of the message requests to be notified
 * when the user couldn't receive the message because he/she is offline, the message 
 * was delivered or when the receiver of the message is composing a reply. 
 *
 * @author Gaston Dombiak
 */
public interface MessageEventRequestListener {

    /**
     * Called when a request for message delivered notification is received.
     *  
     * @param from the user that sent the notification.
     * @param packetID the id of the message that was sent.
     * @param messageEventManager the messageEventManager that fired the listener.
     */
    public void deliveredNotificationRequested(String from, String packetID,
            MessageEventManager messageEventManager);

    /**
     * Called when a request for message displayed notification is received.
     *  
     * @param from the user that sent the notification.
     * @param packetID the id of the message that was sent.
     * @param messageEventManager the messageEventManager that fired the listener.
     */
    public void displayedNotificationRequested(String from, String packetID,
            MessageEventManager messageEventManager);

    /**
     * Called when a request that the receiver of the message is composing a reply notification is 
     * received.
     *  
     * @param from the user that sent the notification.
     * @param packetID the id of the message that was sent.
     * @param messageEventManager the messageEventManager that fired the listener.
     */
    public void composingNotificationRequested(String from, String packetID,
                MessageEventManager messageEventManager);

    /**
     * Called when a request that the receiver of the message is offline is received.
     *  
     * @param from the user that sent the notification.
     * @param packetID the id of the message that was sent.
     * @param messageEventManager the messageEventManager that fired the listener.
     */
    public void offlineNotificationRequested(String from, String packetID,
            MessageEventManager messageEventManager);

}
