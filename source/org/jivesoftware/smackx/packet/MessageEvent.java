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

package org.jivesoftware.smackx.packet;

import java.util.*;

import org.jivesoftware.smack.packet.PacketExtension;

/**
 * Represents message events relating to the delivery, display, composition and cancellation of 
 * messages.
 * 
 * There are four message events currently defined in this namespace:
 * 1.Offline
 * Indicates that the message has been stored offline by the intended recipient's server. This 
 * event is triggered only if the intended recipient's server supports offline storage, has that 
 * support enabled, and the recipient is offline when the server receives the message for delivery.
 * 
 * 2.Delivered
 * Indicates that the message has been delivered to the recipient. This signifies that the message
 * has reached the recipient's XMPP client, but does not necessarily mean that the message has 
 * been displayed. This event is to be raised by the XMPP client.
 * 
 * 3.Displayed
 * Once the message has been received by the recipient's XMPP client, it may be displayed to the
 * user. This event indicates that the message has been displayed, and is to be raised by the 
 * XMPP client. Even if a message is displayed multiple times, this event should be raised only 
 * once.
 * 
 * 4.Composing
 * In threaded chat conversations, this indicates that the recipient is composing a reply to a 
 * message. The event is to be raised by the recipient's XMPP client. A XMPP client is allowed
 * to raise this event multiple times in response to the same request, providing the original 
 * event is cancelled first.
 *
 * @author Gaston Dombiak
 */
public class MessageEvent implements PacketExtension {

    public static final String OFFLINE = "offline";
    public static final String COMPOSING = "composing";
    public static final String DISPLAYED = "displayed";
    public static final String DELIVERED = "delivered";
    public static final String CANCELLED = "cancelled";

    private boolean offline = false;
    private boolean delivered = false;
    private boolean displayed = false;
    private boolean composing = false;
    private boolean cancelled = true;

    private String packetID = null;

    /**
    * Returns the XML element name of the extension sub-packet root element.
    * Always returns "x"
    *
    * @return the XML element name of the packet extension.
    */
    public String getElementName() {
        return "x";
    }

    /** 
     * Returns the XML namespace of the extension sub-packet root element.
     * According the specification the namespace is always "jabber:x:event"
     *
     * @return the XML namespace of the packet extension.
     */
    public String getNamespace() {
        return "jabber:x:event";
    }

    /**
     * When the message is a request returns if the sender of the message requests to be notified
     * when the receiver is composing a reply.
     * When the message is a notification returns if the receiver of the message is composing a 
     * reply.
     * 
     * @return true if the sender is requesting to be notified when composing or when notifying
     * that the receiver of the message is composing a reply
     */
    public boolean isComposing() {
        return composing;
    }

    /**
     * When the message is a request returns if the sender of the message requests to be notified
     * when the message is delivered.
     * When the message is a notification returns if the message was delivered or not.
     * 
     * @return true if the sender is requesting to be notified when delivered or when notifying 
     * that the message was delivered 
     */
    public boolean isDelivered() {
        return delivered;
    }

    /**
     * When the message is a request returns if the sender of the message requests to be notified
     * when the message is displayed.
     * When the message is a notification returns if the message was displayed or not.
     * 
     * @return true if the sender is requesting to be notified when displayed or when notifying 
     * that the message was displayed
     */
    public boolean isDisplayed() {
        return displayed;
    }

    /**
     * When the message is a request returns if the sender of the message requests to be notified
     * when the receiver of the message is offline.
     * When the message is a notification returns if the receiver of the message was offline.
     * 
     * @return true if the sender is requesting to be notified when offline or when notifying 
     * that the receiver of the message is offline
     */
    public boolean isOffline() {
        return offline;
    }

    /**
     * When the message is a notification returns if the receiver of the message cancelled 
     * composing a reply.
     * 
     * @return true if the receiver of the message cancelled composing a reply
     */
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * Returns the unique ID of the message that requested to be notified of the event.
     * The packet id is not used when the message is a request for notifications
     *
     * @return the message id that requested to be notified of the event.
     */
    public String getPacketID() {
        return packetID;
    }

    /**
     * Returns the types of events. The type of event could be:
     * "offline", "composing","delivered","displayed", "offline"
     *
     * @return an iterator over all the types of events of the MessageEvent.
     */
    public Iterator getEventTypes() {
        ArrayList allEvents = new ArrayList();
        if (isDelivered()) {
            allEvents.add(MessageEvent.DELIVERED);
        }
        if (isCancelled()) {
            allEvents.add(MessageEvent.CANCELLED);
        }
        if (isComposing()) {
            allEvents.add(MessageEvent.COMPOSING);
        }
        if (isDisplayed()) {
            allEvents.add(MessageEvent.DISPLAYED);
        }
        if (isOffline()) {
            allEvents.add(MessageEvent.OFFLINE);
        }
        return allEvents.iterator();
    }

    /**
     * When the message is a request sets if the sender of the message requests to be notified
     * when the receiver is composing a reply.
     * When the message is a notification sets if the receiver of the message is composing a 
     * reply.
     * 
     * @param composing sets if the sender is requesting to be notified when composing or when 
     * notifying that the receiver of the message is composing a reply
     */
    public void setComposing(boolean composing) {
        this.composing = composing;
        setCancelled(false);
    }

    /**
     * When the message is a request sets if the sender of the message requests to be notified
     * when the message is delivered.
     * When the message is a notification sets if the message was delivered or not.
     * 
     * @param delivered sets if the sender is requesting to be notified when delivered or when 
     * notifying that the message was delivered 
     */
    public void setDelivered(boolean delivered) {
        this.delivered = delivered;
        setCancelled(false);
    }

    /**
     * When the message is a request sets if the sender of the message requests to be notified
     * when the message is displayed.
     * When the message is a notification sets if the message was displayed or not.
     * 
     * @param displayed sets if the sender is requesting to be notified when displayed or when 
     * notifying that the message was displayed
     */
    public void setDisplayed(boolean displayed) {
        this.displayed = displayed;
        setCancelled(false);
    }

    /**
     * When the message is a request sets if the sender of the message requests to be notified
     * when the receiver of the message is offline.
     * When the message is a notification sets if the receiver of the message was offline.
     * 
     * @param offline sets if the sender is requesting to be notified when offline or when 
     * notifying that the receiver of the message is offline
     */
    public void setOffline(boolean offline) {
        this.offline = offline;
        setCancelled(false);
    }

    /**
     * When the message is a notification sets if the receiver of the message cancelled 
     * composing a reply.
     * The Cancelled event is never requested explicitly. It is requested implicitly when
     * requesting to be notified of the Composing event.
     * 
     * @param cancelled sets if the receiver of the message cancelled composing a reply
     */
    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    /**
     * Sets the unique ID of the message that requested to be notified of the event.
     * The packet id is not used when the message is a request for notifications
     *
     * @param packetID the message id that requested to be notified of the event.
     */
    public void setPacketID(String packetID) {
        this.packetID = packetID;
    }

    /**
     * Returns true if this MessageEvent is a request for notifications.
     * Returns false if this MessageEvent is a notification of an event.
     *
    * @return true if this message is a request for notifications.
     */
    public boolean isMessageEventRequest() {
        return this.packetID == null;
    }

    /**
     * Returns the XML representation of a Message Event according the specification.
     * 
     * Usually the XML representation will be inside of a Message XML representation like
     * in the following examples:
     * 
     * Request to be notified when displayed:
     * <message
     *    to='romeo@montague.net/orchard'
     *    from='juliet@capulet.com/balcony'
     *    id='message22'>
     * <x xmlns='jabber:x:event'>
     *   <displayed/>
     * </x>
     * </message>
     * 
     * Notification of displayed:
     * <message
     *    from='romeo@montague.net/orchard'
     *    to='juliet@capulet.com/balcony'>
     * <x xmlns='jabber:x:event'>
     *   <displayed/>
     *   <id>message22</id>
     * </x>
     * </message>
     * 
     */
    public String toXML() {
        StringBuffer buf = new StringBuffer();
        buf.append("<").append(getElementName()).append(" xmlns=\"").append(getNamespace()).append(
            "\">");
        // Note: Cancellation events don't specify any tag. They just send the packetID

        // Add the offline tag if the sender requests to be notified of offline events or if 
        // the target is offline
        if (isOffline())
            buf.append("<").append(MessageEvent.OFFLINE).append("/>");
        // Add the delivered tag if the sender requests to be notified when the message is 
        // delivered or if the target notifies that the message has been delivered
        if (isDelivered())
            buf.append("<").append(MessageEvent.DELIVERED).append("/>");
        // Add the displayed tag if the sender requests to be notified when the message is 
        // displayed or if the target notifies that the message has been displayed
        if (isDisplayed())
            buf.append("<").append(MessageEvent.DISPLAYED).append("/>");
        // Add the composing tag if the sender requests to be notified when the target is 
        // composing a reply or if the target notifies that he/she is composing a reply
        if (isComposing())
            buf.append("<").append(MessageEvent.COMPOSING).append("/>");
        // Add the id tag only if the MessageEvent is a notification message (not a request)
        if (getPacketID() != null)
            buf.append("<id>").append(getPacketID()).append("</id>");
        buf.append("</").append(getElementName()).append(">");
        return buf.toString();
    }

}
