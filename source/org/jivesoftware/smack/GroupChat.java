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

import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.filter.*;

import java.util.*;

/**
 * A GroupChat is a conversation that takes place among many users in a virtual
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
    private List participants = new ArrayList();

    private PacketFilter presenceFilter;
    private PacketFilter messageFilter;
    private PacketCollector messageCollector;

    /**
     * Creates a new group chat with the specified connection and room name. Note: no
     * information is sent to or received from the server until you attempt to
     * {@link #join(String) join} the chat room. On some server implementations,
     * the room will not be created until the first person joins it.<p>
     *
     *  Most XMPP servers use a sub-domain for the chat service (eg chat.example.com
     * for the XMPP server example.com). You must ensure that the room address you're
     * trying to connect to includes the proper chat sub-domain.
     *
     * @param connection the XMPP connection.
     * @param room the name of the room in the form "roomName@service", where
     *      "service" is the hostname at which the multi-user chat
     *      service is running.
     */
    public GroupChat(XMPPConnection connection, String room) {
        this.connection = connection;
        this.room = room;
        // Create a collector for all incoming messages.
        messageFilter = new AndFilter(new FromContainsFilter(room),
                new PacketTypeFilter(Message.class));
        messageFilter = new AndFilter(messageFilter, new PacketFilter() {
            public boolean accept(Packet packet) {
                Message msg = (Message)packet;
                return msg.getType() == Message.Type.GROUP_CHAT;
            }
        });
        messageCollector = connection.createPacketCollector(messageFilter);
        // Create a listener for all presence updates.
        presenceFilter = new AndFilter(new FromContainsFilter(room),
                new PacketTypeFilter(Presence.class));
        connection.addPacketListener(new PacketListener() {
            public void processPacket(Packet packet) {
                Presence presence = (Presence)packet;
                String from = presence.getFrom();
                if (presence.getType() == Presence.Type.AVAILABLE) {
                    synchronized (participants) {
                        if (!participants.contains(from)) {
                            participants.add(from);
                        }
                    }
                }
                else if (presence.getType() == Presence.Type.UNAVAILABLE) {
                    synchronized (participants) {
                        participants.remove(from);
                    }
                }
            }
        }, presenceFilter);
    }

    /**
     * Returns the name of the room this GroupChat object represents.
     *
     * @return the groupchat room name.
     */
    public String getRoom() {
        return room;
    }

    /**
     * Joins the chat room using the specified nickname. If already joined
     * using another nickname, this method will first leave the room and then
     * re-join using the new nickname. The default timeout of 5 seconds for a reply
     * from the group chat server that the join succeeded will be used.
     *
     * @param nickname the nickname to use.
     * @throws XMPPException if an error occurs joining the room. In particular, a
     *      409 error can occur if someone is already in the group chat with the same
     *      nickname.
     */
    public synchronized void join(String nickname) throws XMPPException {
        join(nickname, SmackConfiguration.getPacketReplyTimeout());
    }

    /**
     * Joins the chat room using the specified nickname. If already joined as
     * another nickname, will leave as that name first before joining under the new
     * name.
     *
     * @param nickname the nickname to use.
     * @param timeout the number of milleseconds to wait for a reply from the
     *      group chat that joining the room succeeded.
     * @throws XMPPException if an error occurs joining the room. In particular, a
     *      409 error can occur if someone is already in the group chat with the same
     *      nickname.
     */
    public synchronized void join(String nickname, long timeout) throws XMPPException {
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
        // Wait for a presence packet back from the server.
        PacketFilter responseFilter = new AndFilter(
                new FromContainsFilter(room + "/" + nickname),
                new PacketTypeFilter(Presence.class));
        PacketCollector response = connection.createPacketCollector(responseFilter);
        // Send join packet.
        connection.sendPacket(joinPresence);
        // Wait up to a certain number of seconds for a reply.
        Presence presence = (Presence)response.nextResult(timeout);
        response.cancel();
        if (presence == null) {
            throw new XMPPException("No response from server.");
        }
        else if (presence.getError() != null) {
            throw new XMPPException(presence.getError());
        }
        this.nickname = nickname;
        joined = true;
    }

    /**
     * Returns true if currently in the group chat (after calling the {@link
     * #join(String)} method.
     *
     * @return true if currently in the group chat room.
     */
    public boolean isJoined() {
        return joined;
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
        connection.sendPacket(leavePresence);
        // Reset participant information.
        participants = new ArrayList();
        nickname = null;
        joined = false;
    }

    /**
     * Returns the nickname that was used to join the room, or <tt>null</tt> if not
     * currently joined.
     *
     * @return the nickname currently being used.
     */
    public String getNickname() {
        return nickname;
    }

    /**
     * Returns the number of participants in the group chat.<p>
     *
     * Note: this value will only be accurate after joining the group chat, and
     * may fluctuate over time. If you query this value directly after joining the
     * group chat it may not be accurate, as it takes a certain amount of time for
     * the server to send all presence packets to this client.
     *
     * @return the number of participants in the group chat.
     */
    public int getParticipantCount() {
        synchronized (participants) {
            return participants.size();
        }
    }

    /**
     * Returns an Iterator (of Strings) for the list of fully qualified participants
     * in the group chat. For example, "conference@chat.jivesoftware.com/SomeUser".
     * Typically, a client would only display the nickname of the participant. To
     * get the nickname from the fully qualified name, use the
     * {@link org.jivesoftware.smack.util.StringUtils#parseResource(String)} method.
     * Note: this value will only be accurate after joining the group chat, and may
     * fluctuate over time.
     *
     * @return an Iterator for the participants in the group chat.
     */
    public Iterator getParticipants() {
        synchronized (participants) {
            return Collections.unmodifiableList(new ArrayList(participants)).iterator();
        }
    }

    /**
     * Adds a packet listener that will be notified of any new Presence packets
     * sent to the group chat. Using a listener is a suitable way to know when the list
     * of participants should be re-loaded due to any changes.
     *
     * @param listener a packet listener that will be notified of any presence packets
     *      sent to the group chat.
     */
    public void addParticipantListener(PacketListener listener) {
        connection.addPacketListener(listener, presenceFilter);
    }

    /**
     * Sends a message to the chat room.
     *
     * @param text the text of the message to send.
     * @throws XMPPException if sending the message fails.
     */
    public void sendMessage(String text) throws XMPPException {
        Message message = new Message(room, Message.Type.GROUP_CHAT);
        message.setBody(text);
        connection.sendPacket(message);
    }

    /**
     * Creates a new Message to send to the chat room.
     *
     * @return a new Message addressed to the chat room.
     */
    public Message createMessage() {
        return new Message(room, Message.Type.GROUP_CHAT);
    }

    /**
     * Sends a Message to the chat room.
     *
     * @param message the message.
     * @throws XMPPException if sending the message fails.
     */
    public void sendMessage(Message message) throws XMPPException {
        connection.sendPacket(message);
    }

     /**
     * Polls for and returns the next message, or <tt>null</tt> if there isn't
     * a message immediately available. This method provides significantly different
     * functionalty than the {@link #nextMessage()} method since it's non-blocking.
     * In other words, the method call will always return immediately, whereas the
     * nextMessage method will return only when a message is available (or after
     * a specific timeout).
     *
     * @return the next message if one is immediately available and
     *      <tt>null</tt> otherwise.
     */
    public Message pollMessage() {
        return (Message)messageCollector.pollResult();
    }

    /**
     * Returns the next available message in the chat. The method call will block
     * (not return) until a message is available.
     *
     * @return the next message.
     */
    public Message nextMessage() {
        return (Message)messageCollector.nextResult();
    }

    /**
     * Returns the next available message in the chat. The method call will block
     * (not return) until a packet is available or the <tt>timeout</tt> has elapased.
     * If the timeout elapses without a result, <tt>null</tt> will be returned.
     *
     * @param timeout the maximum amount of time to wait for the next message.
     * @return the next message, or <tt>null</tt> if the timeout elapses without a
     *      message becoming available.
     */
    public Message nextMessage(long timeout) {
        return (Message)messageCollector.nextResult(timeout);
    }

    /**
     * Adds a packet listener that will be notified of any new messages in the
     * group chat. Only "group chat" messages addressed to this group chat will
     * be delivered to the listener. If you wish to listen for other packets
     * that may be associated with this group chat, you should register a
     * PacketListener directly with the XMPPConnection with the appropriate
     * PacketListener.
     *
     * @param listener a packet listener.
     */
    public void addMessageListener(PacketListener listener) {
        connection.addPacketListener(listener, messageFilter);
    }

    public void finalize() throws Throwable {
        super.finalize();
        try {
            if (messageCollector != null) {
                messageCollector.cancel();
            }
        }
        catch (Exception e) {}
    }
}