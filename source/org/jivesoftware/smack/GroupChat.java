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
 *    contact webmaster@coolservlets.com.
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

package org.jivesoftware.smack;

import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.filter.*;

/**
 * A GroupChat is a conversation that takes plaaces among many users in a virtual
 * room. When joining a group chat, you specify a nickname, which is the identity
 * that other chat room users see.
 *
 * @see XMPPConnection#createGroupChat(String)
 * @author Matt Tucker
 */
public class GroupChat {

    private XMPPConnection connection;
    private String room;
    private String nickname = null;
    private boolean joined = false;

    private PacketCollector collector;

    /**
     * Creates a new group chat with the specified connection and room name.
     *
     * @param connection
     * @param room the name of the room in the form "roomName@service", where
     *      "service" is the hostname at which the multi-user chat
     *      service is running.
     */
    public GroupChat(XMPPConnection connection, String room) {
        this.connection = connection;
        this.room = room;
        collector = connection.getPacketReader().createPacketCollector(
                new FromContainsFilter(room));
    }

    /**
     * Joins the chat room using the specified nickname. If already joined as
     * another nickname, will leave as that name first before joining under the new
     * name.
     *
     * @param nickname the nicknam to use.
     * @throws XMPPException if an error occurs joining the room.
     */
    public synchronized void join(String nickname) throws XMPPException {
        if (nickname == null || nickname.equals("")) {
            throw new IllegalArgumentException("Nickname must not be null or blank.");
        }
        // If we've already joined the room, leave it before joining under a new
        // nickname.
        if (joined) {
            leave();
        }
        // We join a room by sending a presence packet where the "to"
        // field is in the form "roomName@service/nickname"
        Presence joinPresence = new Presence(Presence.Type.AVAILABLE);
        joinPresence.setTo(room + "/" + nickname);
        connection.getPacketWriter().sendPacket(joinPresence);
        // Wait for a presence packet back from the server.
        PacketFilter responseFilter = new AndFilter(
                new FromContainsFilter(room + "/" + nickname),
                new PacketTypeFilter(Presence.class));
        PacketCollector response = connection.getPacketReader().createPacketCollector(
                responseFilter);
        // Wait up to five seconds for a reply.
        Presence presence = (Presence)response.nextResult(5000);
        if (presence == null) {
            throw new XMPPException("No response from server.");
        }
        else if (presence.getError() != null) {
            throw new XMPPException(presence.getError().getMessage());

        }
        this.nickname = nickname;
        joined = true;
    }

    /**
     * Leave the chat room.
     */
    public synchronized void leave() {
        // If not joined already, do nothing.
        if (!joined) {
            return;
        }
        // We leave a room by sending a presence packet where the "to"
        // field is in the form "roomName@service/nickname"
        Presence leavePresence = new Presence(Presence.Type.UNAVAILABLE);
        leavePresence.setTo(room + "/" + nickname);
        connection.getPacketWriter().sendPacket(leavePresence);
        nickname = null;
        joined = false;
    }

    /**
     * Returns the nickname that was used to join the room, or <tt>null</tt> if not
     * currently joined.
     *
     * @return the nickname currently being used..
     */
    public String getNickname() {
        return nickname;
    }

    /**
     * Sends a message to the chat room.
     *
     * @param text the text of the message to send.
     * @throws XMPPException if sending the message fails.
     */
    public void sendMessage(String text) throws XMPPException {
        Message message = new Message(room, Message.CHAT);
        message.setBody(text);
        connection.getPacketWriter().sendPacket(message);
    }

    /**
     * Creates a new Message to send to the chat room.
     *
     * @return a new Message addressed to the chat room.
     */
    public Message createMessage() {
        return new Message(room, Message.CHAT);
    }

    /**
     * Sends a Message to the chat room.
     *
     * @param message the message.
     * @throws XMPPException if sending the message fails.
     */
    public void sendMessage(Message message) throws XMPPException {
        connection.getPacketWriter().sendPacket(message);
    }
}
