/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright (C) 2002-2003 Jive Software. All rights reserved.
 * ====================================================================
 * The Jive Software License (based on Apache Software License, Version 1.1)
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by
 *        Jive Software (http://www.jivesoftware.com)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Smack" and "Jive Software" must not be used to
 *    endorse or promote products derived from this software without
 *    prior written permission. For written permission, please
 *    contact webmaster@jivesoftware.com.
 *
 * 5. Products derived from this software may not be called "Smack",
 *    nor may "Smack" appear in their name, without prior written
 *    permission of Jive Software.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL JIVE SOFTWARE OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
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
     * @param packetId the id of the message that was sent.
     * @param messageEventManager the messageEventManager that fired the listener.
     */
    public void deliveredNotificationRequested(
        String from,
        String packetID,
        MessageEventManager messageEventManager);

    /**
     * Called when a request for message displayed notification is received.
     *  
     * @param from the user that sent the notification.
     * @param packetId the id of the message that was sent.
     * @param messageEventManager the messageEventManager that fired the listener.
     */
    public void displayedNotificationRequested(
        String from,
        String packetID,
        MessageEventManager messageEventManager);

    /**
     * Called when a request that the receiver of the message is composing a reply notification is 
     * received.
     *  
     * @param from the user that sent the notification.
     * @param packetId the id of the message that was sent.
     * @param messageEventManager the messageEventManager that fired the listener.
     */
    public void composingNotificationRequested(
        String from,
        String packetID,
        MessageEventManager messageEventManager);

    /**
     * Called when a request that the receiver of the message is offline is received.
     *  
     * @param from the user that sent the notification.
     * @param packetId the id of the message that was sent.
     * @param messageEventManager the messageEventManager that fired the listener.
     */
    public void offlineNotificationRequested(
        String from,
        String packetID,
        MessageEventManager messageEventManager);

}
