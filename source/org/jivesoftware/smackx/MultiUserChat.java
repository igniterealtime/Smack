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

import java.lang.ref.WeakReference;
import java.util.*;

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.filter.*;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smackx.packet.*;

/**
 * A MultiUserChat is a conversation that takes place among many users in a virtual
 * room. A room could have many occupants with different affiliation and roles.
 * Possible affiliatons are "owner", "admin", "member", and "outcast". Possible roles 
 * are "moderator", "participant", and "visitor". Each role and affiliation guarantees
 * different privileges (e.g. Send messages to all occupants, Kick participants and visitors,
 * Grant voice, Edit member list, etc.). 
 *
 * @author Gaston Dombiak
 */
public class MultiUserChat {

    private final static String discoNamespace = "http://jabber.org/protocol/muc";
    private final static String discoNode = "http://jabber.org/protocol/muc#rooms";
    
    private static Map joinedRooms = new WeakHashMap();

    private XMPPConnection connection;
    private String room;
    private String nickname = null;
    private boolean joined = false;
    private Map participantsMap = new HashMap();

    private List invitationRejectionListeners = new ArrayList();

    private PacketFilter presenceFilter;
    private PacketListener presenceListener;
    private PacketFilter messageFilter;
    private PacketFilter declinesFilter;
    private PacketListener declinesListener;
    private PacketCollector messageCollector;

    static {
        XMPPConnection.addConnectionListener(new ConnectionEstablishedListener() {
            public void connectionEstablished(final XMPPConnection connection) {
                // Set on every established connection that this client supports the Multi-User 
                // Chat protocol. This information will be used when another client tries to 
                // discover whether this client supports MUC or not.
                ServiceDiscoveryManager.getInstanceFor(connection).addFeature(discoNamespace);
                // Set the NodeInformationProvider that will provide information about the
                // joined rooms whenever a disco request is received 
                ServiceDiscoveryManager.getInstanceFor(connection).setNodeInformationProvider(
                    discoNode,
                    new NodeInformationProvider() {
                        public Iterator getNodeItems() {
                            return MultiUserChat.getJoinedRooms(connection);
                        }
                    });
            }
        });
    }

    /**
     * Creates a new multi user chat with the specified connection and room name. Note: no
     * information is sent to or received from the server until you attempt to
     * {@link #join(String) join} the chat room. On some server implementations,
     * the room will not be created until the first person joins it.<p>
     *
     * Most XMPP servers use a sub-domain for the chat service (eg chat.example.com
     * for the XMPP server example.com). You must ensure that the room address you're
     * trying to connect to includes the proper chat sub-domain.
     *
     * @param connection the XMPP connection.
     * @param room the name of the room in the form "roomName@service", where
     *      "service" is the hostname at which the multi-user chat
     *      service is running.
     */
    public MultiUserChat(XMPPConnection connection, String room) {
        this.connection = connection;
        this.room = room;
        // Create a collector for all incoming messages.
        messageFilter =
            new AndFilter(new FromContainsFilter(room), new PacketTypeFilter(Message.class));
        messageFilter = new AndFilter(messageFilter, new PacketFilter() {
            public boolean accept(Packet packet) {
                Message msg = (Message) packet;
                return msg.getType() == Message.Type.GROUP_CHAT;
            }
        });
        messageCollector = connection.createPacketCollector(messageFilter);
        // Create a listener for all presence updates.
        presenceFilter =
            new AndFilter(new FromContainsFilter(room), new PacketTypeFilter(Presence.class));
        presenceListener = new PacketListener() {
            public void processPacket(Packet packet) {
                Presence presence = (Presence) packet;
                String from = presence.getFrom();
                if (presence.getType() == Presence.Type.AVAILABLE) {
                    synchronized (participantsMap) {
                        participantsMap.put(from, presence);
                    }
                }
                else if (presence.getType() == Presence.Type.UNAVAILABLE) {
                    synchronized (participantsMap) {
                        participantsMap.remove(from);
                    }
                }
            }
        };
        connection.addPacketListener(presenceListener, presenceFilter);

        // Listens for all messages that include a MUCUser extension and fire the invitation 
        // rejection listeners if the message includes an invitation rejection.
        declinesFilter = new PacketExtensionFilter("x", "http://jabber.org/protocol/muc#user");
        declinesListener = new PacketListener() {
            public void processPacket(Packet packet) {
                // Get the MUC User extension
                MUCUser mucUser = getMUCUserExtension(packet);
                // Check if the MUCUser informs that the invitee has declined the invitation
                if (mucUser.getDecline() != null) {
                    // Fire event for invitation rejection listeners
                    fireInvitationRejectionListeners(
                        mucUser.getDecline().getFrom(),
                        mucUser.getDecline().getReason());
                }
            };
        };
        connection.addPacketListener(declinesListener, declinesFilter);
    }

    /**
     * Returns true if the specified user supports the Multi-User Chat protocol.
     *
     * @param connection the connection to use to perform the service discovery.
     * @param user the user to check. A fully qualified xmpp ID, e.g. jdoe@example.com.
     * @return a boolean indicating whether the specified user supports the MUC protocol.
     */
    public static boolean isServiceEnabled(XMPPConnection connection, String user) {
        try {
            DiscoverInfo result =
                ServiceDiscoveryManager.getInstanceFor(connection).discoverInfo(user);
            return result.containsFeature(discoNamespace);
        }
        catch (XMPPException e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Returns an Iterator on the rooms where the user has joined using a given connection.
     * The Iterator will contain Strings where each String represents a room 
     * (e.g. room@muc.jabber.org).
     * 
     * @param connection the connection used to join the rooms.
     * @return an Iterator on the rooms where the user has joined using a given connection.
     */
    private static Iterator getJoinedRooms(XMPPConnection connection) {
        ArrayList rooms = (ArrayList)joinedRooms.get(connection);
        if (rooms != null) {
            return rooms.iterator();
        }
        // Return an iterator on an empty collection (i.e. the user never joined a room) 
        return new ArrayList().iterator();
    }
    
    /**
     * Returns an Iterator on the rooms where the requested user has joined. The Iterator will 
     * contain Strings where each String represents a room (e.g. room@muc.jabber.org).
     * 
     * @param connection the connection to use to perform the service discovery.
     * @param user the user to check. A fully qualified xmpp ID, e.g. jdoe@example.com.
     * @return an Iterator on the rooms where the requested user has joined.
     */
    public static Iterator getJoinedRooms(XMPPConnection connection, String user) {
        try {
            ArrayList answer = new ArrayList();
            // Send the disco packet to the user
            DiscoverItems result =
                ServiceDiscoveryManager.getInstanceFor(connection).discoverItems(user, discoNode);
            // Collect the entityID for each returned item
            for (Iterator items=result.getItems(); items.hasNext();) {
                answer.add(((DiscoverItems.Item)items.next()).getEntityID());
            }
            return answer.iterator();
        }
        catch (XMPPException e) {
            e.printStackTrace();
            // Return an iterator on an empty collection 
            return new ArrayList().iterator();
        }
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
     * Creates the room according to some default configuration, assign the requesting user 
     * as the room owner, and add the owner to the room but not allow anyone else to enter 
     * the room (effectively "locking" the room). The requesting user will join the room
     * under the specified nickname as soon as the room has been created.<p>
     * 
     * To create an "Instant Room", that means a room with some default configuration that is 
     * available for immediate access, the room's owner should send an empty form after creating 
     * the room. (@see MultiUserChat.sendConfigurationForm(Form))<p>
     *   
     * To create a "Reserved Room", that means a room manually configured by the room creator 
     * before anyone is allowed to enter, the room's owner should complete and send a form after 
     * creating the room. Once the completed configutation form is sent to the server, the server  
     * will unlock the room. (@see MultiUserChat.sendConfigurationForm(Form))
     * 
     * @param nickname the nickname to use.
     * @throws XMPPException if the room couldn't be created for some reason
     *          (e.g. room already exists; user already joined to an existant room or
     *          405 error if the user is not allowed to create the room) 
     */
    public synchronized void create(String nickname) throws XMPPException {
        if (nickname == null || nickname.equals("")) {
            throw new IllegalArgumentException("Nickname must not be null or blank.");
        }
        // If we've already joined the room, leave it before joining under a new
        // nickname.
        if (joined) {
            throw new IllegalStateException("Creation failed - User already joined the room.");
        }
        // We create a room by sending a presence packet to room@service/nick
        // and signal support for MUC. The owner will be automatically logged into the room.
        Presence joinPresence = new Presence(Presence.Type.AVAILABLE);
        joinPresence.setTo(room + "/" + nickname);
        // Indicate the the client supports MUC          
        joinPresence.addExtension(new MUCInitialPresence());

        // Wait for a presence packet back from the server.
        PacketFilter responseFilter =
            new AndFilter(
                new FromContainsFilter(room + "/" + nickname),
                new PacketTypeFilter(Presence.class));
        PacketCollector response = connection.createPacketCollector(responseFilter);
        // Send create & join packet.
        connection.sendPacket(joinPresence);
        // Wait up to a certain number of seconds for a reply.
        Presence presence =
            (Presence) response.nextResult(SmackConfiguration.getPacketReplyTimeout());
        if (presence == null) {
            throw new XMPPException("No response from server.");
        }
        else if (presence.getError() != null) {
            throw new XMPPException(presence.getError());
        }
        // Whether the room existed before or was created, the user has joined the room
        this.nickname = nickname;
        joined = true;
        userHasJoined();

        // Look for confirmation of room creation from the server
        MUCUser mucUser = getMUCUserExtension(presence);
        if (mucUser != null && mucUser.getStatus() != null) {
            if ("201".equals(mucUser.getStatus().getCode())) {
                // Room was created and the user has joined the room
                return;
            }
        }
        // We need to leave the room since it seems that the room already existed
        leave();
        throw new XMPPException("Creation failed - Missing acknowledge of room creation.");
    }

    /**
     * Joins the chat room using the specified nickname. If already joined
     * using another nickname, this method will first leave the room and then
     * re-join using the new nickname. The default timeout of Smack for a reply
     * from the group chat server that the join succeeded will be used. After
     * joining the room, the room will decide the amount of history to send. 
     *
     * @param nickname the nickname to use.
     * @throws XMPPException if an error occurs joining the room. In particular, a 
     *      401 error can occur if no password was provided and one is required; or a 
     *      403 error can occur if the user is banned; or a 
     *      404 error can occur if the room does not exist or is locked; or a 
     *      407 error can occur if user is not on the member list; or a 
     *      409 error can occur if someone is already in the group chat with the same nickname.
     */
    public void join(String nickname) throws XMPPException {
        join(nickname, SmackConfiguration.getPacketReplyTimeout(), null, -1, -1, -1, null);
    }

    /**
     * Joins the chat room using the specified nickname and password. If already joined
     * using another nickname, this method will first leave the room and then
     * re-join using the new nickname. The default timeout of Smack for a reply
     * from the group chat server that the join succeeded will be used. After
     * joining the room, the room will decide the amount of history to send.<p> 
     * 
     * A password is required when joining password protected rooms. If the room does
     * not require a password there is no need to provide one.
     *
     * @param nickname the nickname to use.
     * @param password the password to use.
     * @throws XMPPException if an error occurs joining the room. In particular, a 
     *      401 error can occur if no password was provided and one is required; or a 
     *      403 error can occur if the user is banned; or a 
     *      404 error can occur if the room does not exist or is locked; or a 
     *      407 error can occur if user is not on the member list; or a 
     *      409 error can occur if someone is already in the group chat with the same nickname.
     */
    public void join(String nickname, String password) throws XMPPException {
        join(nickname, SmackConfiguration.getPacketReplyTimeout(), password, -1, -1, -1, null);
    }

    /**
     * Joins the chat room using the specified nickname and password. If already joined
     * using another nickname, this method will first leave the room and then
     * re-join using the new nickname.<p>
     * 
     * This method provides different parameters to control the amount of history to receive when 
     * joining the room. If you don't complete any of these parameters the room will decide the 
     * amount of history to return. If you decide to control the amount of history to receive, you 
     * can use some or all of the following parameters:
     * <ul>
     *  <li>maxchars -> total number of characters to receive in the history.
     *  <li>maxstanzas -> total number of messages to receive in the history.
     *  <li>seconds -> only the messages received in the last "X" seconds will be included in the 
     * history.
     *  <li>since -> only the messages received since the datetime specified will be included in 
     * the history.
     * </ul>
     * 
     * A password is required when joining password protected rooms. If the room does
     * not require a password there is no need to provide one.<p>
     * 
     * If the room does not already exist when the user seeks to enter it, the server will
     * decide to create a new room or not. 
     * 
     * @param nickname the nickname to use.
     * @param timeout the number of seconds to wait for reply from the group chat server.
     * @param password the password to use.
     * @param maxchars the total number of characters to receive in the history.
     * @param maxstanzas the total number of messages to receive in the history.
     * @param seconds the number of seconds to use to filter the messages received during 
     *      that time.
     * @param since the since date to use to filter the messages received during that time.
     * @throws XMPPException if an error occurs joining the room. In particular, a 
     *      401 error can occur if no password was provided and one is required; or a 
     *      403 error can occur if the user is banned; or a 
     *      404 error can occur if the room does not exist or is locked; or a 
     *      407 error can occur if user is not on the member list; or a 
     *      409 error can occur if someone is already in the group chat with the same nickname.
     */
    public synchronized void join(
        String nickname,
        long timeout,
        String password,
        int maxchars,
        int maxstanzas,
        int seconds,
        Date since)
        throws XMPPException {
        // TODO Review protocol (too many params). Use setters or history class?
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

        // Indicate the the client supports MUC          
        MUCInitialPresence mucInitialPresence = new MUCInitialPresence();
        if (password != null) {
            mucInitialPresence.setPassword(password);
        }
        if (maxchars > -1 || maxstanzas > -1 || seconds > -1 || since != null) {
            MUCInitialPresence.History history = new MUCInitialPresence.History();
            if (maxchars > -1) {
                history.setMaxChars(maxchars);
            }
            if (maxstanzas > -1) {
                history.setMaxStanzas(maxstanzas);
            }
            if (seconds > -1) {
                history.setSeconds(seconds);
            }
            if (since != null) {
                history.setSince(since);
            }
            mucInitialPresence.setHistory(history);
        }
        joinPresence.addExtension(mucInitialPresence);

        // Wait for a presence packet back from the server.
        PacketFilter responseFilter =
            new AndFilter(
                new FromContainsFilter(room + "/" + nickname),
                new PacketTypeFilter(Presence.class));
        PacketCollector response = connection.createPacketCollector(responseFilter);
        // Send join packet.
        connection.sendPacket(joinPresence);
        // Wait up to a certain number of seconds for a reply.
        Presence presence = (Presence) response.nextResult(timeout);
        if (presence == null) {
            throw new XMPPException("No response from server.");
        }
        else if (presence.getError() != null) {
            throw new XMPPException(presence.getError());
        }
        this.nickname = nickname;
        joined = true;
        userHasJoined();
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
        participantsMap = new HashMap();
        nickname = null;
        joined = false;
        userHasLeft();
    }

    /**
     * Returns the room's configuration form that the room's owner can use or <tt>null</tt> if 
     * no configuration is possible. The configuration form allows to set the room's language, 
     * enable logging, specify room's type, etc..  
     * 
     * @return the Form that contains the fields to complete together with the instrucions or 
     * <tt>null</tt> if no configuration is possible.
     * @throws XMPPException if an error occurs asking the configuration form for the room.
     */
    public Form getConfigurationForm() throws XMPPException {
        MUCOwner iq = new MUCOwner();
        iq.setTo(room);
        iq.setType(IQ.Type.GET);

        // Wait for a presence packet back from the server.
        PacketFilter responseFilter =
            new AndFilter(new FromContainsFilter(room), new PacketTypeFilter(IQ.class));
        PacketCollector response = connection.createPacketCollector(responseFilter);
        // Request the configuration form to the server.
        connection.sendPacket(iq);
        // Wait up to a certain number of seconds for a reply.
        IQ answer = (IQ) response.nextResult(SmackConfiguration.getPacketReplyTimeout());
        // Stop queuing results
        response.cancel();

        if (answer == null) {
            throw new XMPPException("No response from server.");
        }
        else if (answer.getError() != null) {
            throw new XMPPException(answer.getError());
        }
        return Form.getFormFrom(answer);
    }

    /**
     * Sends the completed configuration form to the server. The room will be configured
     * with the new settings defined in the form. If the form is empty then the server
     * will create an instant room (will use default configuration).
     * 
     * @param form the form with the new settings.
     * @throws XMPPException if an error occurs setting the new rooms' configuration.
     */
    public void sendConfigurationForm(Form form) throws XMPPException {
        MUCOwner iq = new MUCOwner();
        iq.setTo(room);
        iq.setType(IQ.Type.SET);
        iq.addExtension(form.getDataFormToSend());

        // Send the completed configuration form to the server.
        connection.sendPacket(iq);

        // TODO Check for possible returned errors? permission errors?
        // TODO Check that the form is of type "submit" or "cancel"
    }

    /**
     * Returns the room's registration form that an unaffiliated user, can use to become a member 
     * of the room or <tt>null</tt> if no registration is possible. Some rooms may restrict the 
     * privilege to register members and allow only room admins to add new members.<p>
     * 
     * If the user requesting registration requirements is not allowed to register with the room 
     * (e.g. because that privilege has been restricted), the room will return a "Not Allowed" 
     * error to the user (error code 405).
     * 
     * @return the registration Form that contains the fields to complete together with the 
     * instrucions or <tt>null</tt> if no registration is possible.
     * @throws XMPPException if an error occurs asking the registration form for the room or a 
     * 405 error if the user is not allowed to register with the room.
     */
    public Form getRegistrationForm() throws XMPPException {
        Registration reg = new Registration();
        reg.setType(IQ.Type.GET);
        reg.setTo(room);

        PacketFilter filter =
            new AndFilter(new PacketIDFilter(reg.getPacketID()), new PacketTypeFilter(IQ.class));
        PacketCollector collector = connection.createPacketCollector(filter);
        connection.sendPacket(reg);
        IQ result = (IQ) collector.nextResult(SmackConfiguration.getPacketReplyTimeout());
        collector.cancel();
        if (result == null) {
            throw new XMPPException("No response from server.");
        }
        else if (result.getType() == IQ.Type.ERROR) {
            throw new XMPPException(result.getError());
        }
        return Form.getFormFrom(result);
    }

    /**
     * Sends the completed registration form to the server. After the user successfully submits 
     * the form, the room may queue the request for review by the room admins or may immediately 
     * add the user to the member list by changing the user's affiliation from "none" to "member.<p>
     * 
     * If the desired room nickname is already reserved for that room, the room will return a 
     * "Conflict" error to the user (error code 409). If the room does not support registration, 
     * it will return a "Service Unavailable" error to the user (error code 503).
     * 
     * @param form the completed registration form.
     * @throws XMPPException if an error occurs submitting the registration form. In particular, a 
     *      409 error can occur if the desired room nickname is already reserved for that room; 
     *      or a 503 error can occur if the room does not support registration.
     */
    public void sendRegistrationForm(Form form) throws XMPPException {
        Registration reg = new Registration();
        reg.setType(IQ.Type.SET);
        reg.setTo(room);
        reg.addExtension(form.getDataFormToSend());

        PacketFilter filter =
            new AndFilter(new PacketIDFilter(reg.getPacketID()), new PacketTypeFilter(IQ.class));
        PacketCollector collector = connection.createPacketCollector(filter);
        connection.sendPacket(reg);
        IQ result = (IQ) collector.nextResult(SmackConfiguration.getPacketReplyTimeout());
        collector.cancel();
        if (result == null) {
            throw new XMPPException("No response from server.");
        }
        else if (result.getType() == IQ.Type.ERROR) {
            throw new XMPPException(result.getError());
        }
    }

    /**
     * Sends a request to the server to destroy the room. The sender of the request
     * should be the room's owner. If the sender of the destroy request is not the room's owner
     * then the server will answer a "Forbidden" error (403).
     * 
     * @param reason the reason for the room destruction.
     * @param alternateJID the JID of an alternate location.
     * @throws XMPPException if an error occurs while trying to destroy the room.
     *      An error can occur which will be wrapped by an XMPPException --
     *      XMPP error code 403. The error code can be used to present more
     *      appropiate error messages to end-users.
     */
    public void destroy(String reason, String alternateJID) throws XMPPException {
        MUCOwner iq = new MUCOwner();
        iq.setTo(room);
        iq.setType(IQ.Type.SET);

        // Create the reason for the room destruction
        MUCOwner.Destroy destroy = new MUCOwner.Destroy();
        destroy.setReason(reason);
        destroy.setJid(alternateJID);
        iq.setDestroy(destroy);

        // Wait for a presence packet back from the server.
        PacketFilter responseFilter =
            new AndFilter(new FromContainsFilter(room), new PacketTypeFilter(IQ.class));
        PacketCollector response = connection.createPacketCollector(responseFilter);
        // Send the room destruction request.
        connection.sendPacket(iq);
        // Wait up to a certain number of seconds for a reply.
        IQ answer = (IQ) response.nextResult(SmackConfiguration.getPacketReplyTimeout());
        // Stop queuing results
        response.cancel();

        if (answer == null) {
            throw new XMPPException("No response from server.");
        }
        else if (answer.getError() != null) {
            throw new XMPPException(answer.getError());
        }
    }

    /**
     * Invites another user to the room in which one is an occupant. The invitation 
     * will be sent to the room which in turn will forward the invitation to the invitee.<p>
     * 
     * If the room is password-protected, the invitee will receive a password to use to join
     * the room. If the room is members-only, the the invitee may be added to the member list.
     * 
     * @param participant the user to invite to the room.(e.g. hecate@shakespeare.lit)
     * @param reason the reason why the user is being invited.
     */
    public void invite(String participant, String reason) {
        // TODO listen for 404 error code when inviter supplies a non-existent JID
        Message message = new Message(room);

        // Create the MUCUser packet that will include the invitation
        MUCUser mucUser = new MUCUser();
        MUCUser.Invite invite = new MUCUser.Invite();
        invite.setTo(participant);
        invite.setReason(reason);
        mucUser.setInvite(invite);
        // Add the MUCUser packet that includes the invitation to the message
        message.addExtension(mucUser);

        connection.sendPacket(message);

    }

    /**
     * Informs the sender of an invitation that the invitee declines the invitation. The rejection 
     * will be sent to the room which in turn will forward the rejection to the inviter.
     * 
     * @param conn the connection to use for sending the rejection.
     * @param room the room that sent the original invitation.
     * @param inviter the inviter of the declined invitation.
     * @param reason the reason why the invitee is declining the invitation.
     */
    public static void decline(XMPPConnection conn, String room, String inviter, String reason) {
        Message message = new Message(room);

        // Create the MUCUser packet that will include the rejection
        MUCUser mucUser = new MUCUser();
        MUCUser.Decline decline = new MUCUser.Decline();
        decline.setTo(inviter);
        decline.setReason(reason);
        mucUser.setDecline(decline);
        // Add the MUCUser packet that includes the rejection
        message.addExtension(mucUser);

        conn.sendPacket(message);
    }

    /**
     * Adds a listener to invitation notifications. The listener will be fired anytime 
     * an invitation is received.
     *
     * @param conn the connection where the listener will be applied.
     * @param listener an invitation listener.
     */
    public static void addInvitationListener(XMPPConnection conn, InvitationListener listener) {
        InvitationsMonitor.getInvitationsMonitor(conn).addInvitationListener(listener);
    }

    /**
     * Removes a listener to invitation notifications. The listener will be fired anytime 
     * an invitation is received.
     *
     * @param conn the connection where the listener was applied.
     * @param listener an invitation listener.
     */
    public static void removeInvitationListener(XMPPConnection conn, InvitationListener listener) {
        InvitationsMonitor.getInvitationsMonitor(conn).removeInvitationListener(listener);
    }

    /**
     * Adds a listener to invitation rejections notifications. The listener will be fired anytime 
     * an invitation is declined.
     *
     * @param listener an invitation rejection listener.
     */
    public void addInvitationRejectionListener(InvitationRejectionListener listener) {
        synchronized (invitationRejectionListeners) {
            if (!invitationRejectionListeners.contains(listener)) {
                invitationRejectionListeners.add(listener);
            }
        }
    }

    /**
     * Removes a listener from invitation rejections notifications. The listener will be fired 
     * anytime an invitation is declined.
     *
     * @param listener an invitation rejection listener.
     */
    public void removeInvitationRejectionListener(InvitationRejectionListener listener) {
        synchronized (invitationRejectionListeners) {
            invitationRejectionListeners.remove(listener);
        }
    }

    /**
     * Fires invitation rejection listeners.
     */
    private void fireInvitationRejectionListeners(String invitee, String reason) {
        InvitationRejectionListener[] listeners = null;
        synchronized (invitationRejectionListeners) {
            listeners = new InvitationRejectionListener[invitationRejectionListeners.size()];
            invitationRejectionListeners.toArray(listeners);
        }
        for (int i = 0; i < listeners.length; i++) {
            listeners[i].invitationDeclined(invitee, reason);
        }
    }

    /**
     * Returns the reserved room nickname for the user in the room. A user may have a reserved 
     * nickname, for example through explicit room registration or database integration. In such 
     * cases it may be desirable for the user to discover the reserved nickname before attempting 
     * to enter the room.
     *
     * @return the reserved room nickname or <tt>null</tt> if none.
     */
    public String getReservedNickname() {
        try {
            DiscoverInfo result =
                ServiceDiscoveryManager.getInstanceFor(connection).discoverInfo(
                    room,
                    "x-roomuser-item");
            // Look for an Identity that holds the reserved nickname and return its name 
            for (Iterator identities = result.getIdentities(); identities.hasNext();) {
                DiscoverInfo.Identity identity = (DiscoverInfo.Identity) identities.next();
                return identity.getName();
            }
            // If no Identity was found then the user does not have a reserved room nickname
            return null;
        }
        catch (XMPPException e) {
            e.printStackTrace();
            return null;
        }
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
     * Changes the participant's nickname to a new nickname within the room. Each room participant
     * will receive two presence packets. One of type "unavailable" for the old nickname and one 
     * indicating availability for the new nickname. The unavailable presence will contain the new 
     * nickname and an appropriate status code (namely 303) as extended presence information. The 
     * status code 303 indicates that the participant is changing his/her nickname.
     * 
     * @param nickname the new nickname within the room.
     * @throws XMPPException if the new nickname is already in use by another occupant.
     */
    public void changeNickname(String nickname) throws XMPPException {
        if (nickname == null || nickname.equals("")) {
            throw new IllegalArgumentException("Nickname must not be null or blank.");
        }
        // Check that we already have joined the room before attempting to change the
        // nickname.
        if (!joined) {
            throw new IllegalStateException("Must be logged into the room to change nickname.");
        }
        // We change the nickname by sending a presence packet where the "to"
        // field is in the form "roomName@service/nickname"
        // We don't have to signal the MUC support again
        Presence joinPresence = new Presence(Presence.Type.AVAILABLE);
        joinPresence.setTo(room + "/" + nickname);

        // Wait for a presence packet back from the server.
        PacketFilter responseFilter =
            new AndFilter(
                new FromContainsFilter(room + "/" + nickname),
                new PacketTypeFilter(Presence.class));
        PacketCollector response = connection.createPacketCollector(responseFilter);
        // Send join packet.
        connection.sendPacket(joinPresence);
        // Wait up to a certain number of seconds for a reply.
        Presence presence =
            (Presence) response.nextResult(SmackConfiguration.getPacketReplyTimeout());
        if (presence == null) {
            throw new XMPPException("No response from server.");
        }
        else if (presence.getError() != null) {
            throw new XMPPException(presence.getError());
        }
        this.nickname = nickname;
    }

    /**
     * Changes the participant's availability status within the room. The presence type
     * will remain available but with a new status that describes the presence update and
     * a new presence mode (e.g. Extended away).
     * 
     * @param status a text message describing the presence update.
     * @param mode the mode type for the presence update.
     */
    public void changeAvailabilityStatus(String status, Presence.Mode mode) {
        if (nickname == null || nickname.equals("")) {
            throw new IllegalArgumentException("Nickname must not be null or blank.");
        }
        // Check that we already have joined the room before attempting to change the
        // availability status.
        if (!joined) {
            throw new IllegalStateException(
                "Must be logged into the room to change the " + "availability status.");
        }
        // We change the availability status by sending a presence packet to the room with the
        // new presence status and mode
        Presence joinPresence = new Presence(Presence.Type.AVAILABLE);
        joinPresence.setStatus(status);
        joinPresence.setMode(mode);
        joinPresence.setTo(room + "/" + nickname);

        // Send join packet.
        connection.sendPacket(joinPresence);
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
        synchronized (participantsMap) {
            return participantsMap.size();
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
        synchronized (participantsMap) {
            return Collections.unmodifiableList(new ArrayList(participantsMap.keySet())).iterator();
        }
    }

    /**
     * Returns the presence info for a particular participant, or <tt>null</tt> if the participant
     * is not in the room.<p>
     * 
     * @param participant the room occupant to search for his presence. The format of participant must
     * be: roomName@service/nickname (e.g. darkcave@macbeth.shakespeare.lit/thirdwitch).
     * @return the participant's current presence, or <tt>null</tt> if the user is unavailable
     *      or if no presence information is available.
     */
    public Presence getParticipantPresence(String participant) {
        return (Presence) participantsMap.get(participant);
    }

    /**
     * Returns the participant's full JID when joining a Non-Anonymous room or <tt>null</tt>
     * if the room is of type anonymous. If the room is of type semi-anonymous only the 
     * moderators will have access to the participants full JID.    
     * 
     * @param participant the room occupant to search for his JID. The format of participant must
     * be: roomName@service/nickname (e.g. darkcave@macbeth.shakespeare.lit/thirdwitch).
     * @return the participant's full JID when joining a Non-Anonymous room otherwise returns 
     * <tt>null</tt>.
     */
    public String getParticipantJID(String participant) {
        // Get the participant's presence
        Presence presence = getParticipantPresence(participant);
        // Get the MUC User extension
        MUCUser mucUser = getMUCUserExtension(presence);
        if (mucUser != null) {
            return mucUser.getItem().getJid();
        }
        return null;
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
     * Returns a new Chat for sending private messages to a given room participant.
     * The Chat's participant address is the room's JID (i.e. roomName@service/nick). The server 
     * service will change the 'from' address to the sender's room JID and delivering the message 
     * to the intended recipient's full JID.
     *
     * @param participant occupant unique room JID (e.g. 'darkcave@macbeth.shakespeare.lit/Paul').
     * @return new Chat for sending private messages to a given room participant.
     */
    public Chat createPrivateChat(String participant) {
        return new Chat(connection, participant);
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
        return (Message) messageCollector.pollResult();
    }

    /**
     * Returns the next available message in the chat. The method call will block
     * (not return) until a message is available.
     *
     * @return the next message.
     */
    public Message nextMessage() {
        return (Message) messageCollector.nextResult();
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
        return (Message) messageCollector.nextResult(timeout);
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

    /**
     * Notification message that the user has joined the room. 
     */
    private synchronized void userHasJoined() {
        // Update the list of joined rooms through this connection
        ArrayList rooms = (ArrayList)joinedRooms.get(connection);
        if (rooms == null) {
            rooms = new ArrayList();
            joinedRooms.put(connection, rooms);
        }
        rooms.add(room);
    }
    
    /**
     * Notification message that the user has left the room. 
     */
    private synchronized void userHasLeft() {
        // Update the list of joined rooms through this connection
        ArrayList rooms = (ArrayList)joinedRooms.get(connection);
        if (rooms == null) {
            return;
        }
        rooms.remove(room);
    }
    
    /**
     * Returns the MUCUser packet extension included in the packet or <tt>null</tt> if none.
     * 
     * @param packet the packet that may include the MUCUser extension.
     * @return the MUCUser found in the packet.
     */
    private MUCUser getMUCUserExtension(Packet packet) {
        if (packet != null) {
            // Get the MUC User extension
            return (MUCUser) packet.getExtension("x", "http://jabber.org/protocol/muc#user");
        }
        return null;
    }

    public void finalize() {
        if (connection != null) {
            connection.removePacketListener(presenceListener);
            connection.removePacketListener(declinesListener);
        }
    }

    /**
     * An InvitationsMonitor monitors a given connection to detect room invitations. Every
     * time the InvitationsMonitor detects a new invitation it will fire the invitation listeners. 
     * 
     * @author Gaston Dombiak
     */
    private static class InvitationsMonitor implements ConnectionListener {
        // We use a WeakHashMap so that the GC can collect the monitor when the
        // connection is no longer referenced by any object.
        private static Map monitors = new WeakHashMap();

        private List invitationsListeners = new ArrayList();
        private XMPPConnection connection;
        private PacketFilter invitationFilter;
        private PacketListener invitationPacketListener;

        /**
         * Returns a new or existing InvitationsMonitor for a given connection.
         * 
         * @param connection the connection to monitor for room invitations.
         * @return a new or existing InvitationsMonitor for a given connection.
         */
        public static InvitationsMonitor getInvitationsMonitor(XMPPConnection conn) {
            synchronized (monitors) {
                if (!monitors.containsKey(conn)) {
                    // We need to use a WeakReference because the monitor references the 
                    // connection and this could prevent the GC from collecting the monitor
                    // when no other object references the monitor
                    monitors.put(conn, new WeakReference(new InvitationsMonitor(conn)));
                }
                // Return the InvitationsMonitor that monitors the connection
                return (InvitationsMonitor) ((WeakReference) monitors.get(conn)).get();
            }
        }

        /**
         * Creates a new InvitationsMonitor that will monitor invitations received
         * on a given connection.
         * 
         * @param connection the connection to monitor for possible room invitations
         */
        private InvitationsMonitor(XMPPConnection connection) {
            this.connection = connection;
        }

        /**
         * Adds a listener to invitation notifications. The listener will be fired anytime 
         * an invitation is received.<p>
         * 
         * If this is the first monitor's listener then the monitor will be initialized in 
         * order to start listening to room invitations.
         * 
         * @param listener an invitation listener.
         */
        public void addInvitationListener(InvitationListener listener) {
            synchronized (invitationsListeners) {
                // If this is the first monitor's listener then initialize the listeners 
                // on the connection to detect room invitations
                if (invitationsListeners.size() == 0) {
                    init();
                }
                if (!invitationsListeners.contains(listener)) {
                    invitationsListeners.add(listener);
                }
            }
        }

        /**
         * Removes a listener to invitation notifications. The listener will be fired anytime 
         * an invitation is received.<p>
         * 
         * If there are no more listeners to notifiy for room invitations then the monitor will
         * be stopped. As soon as a new listener is added to the monitor, the monitor will resume
         * monitoring the connection for new room invitations.
         * 
         * @param listener an invitation listener.
         */
        public void removeInvitationListener(InvitationListener listener) {
            synchronized (invitationsListeners) {
                if (invitationsListeners.contains(listener)) {
                    invitationsListeners.remove(listener);
                }
                // If there are no more listeners to notifiy for room invitations
                // then proceed to cancel/release this monitor 
                if (invitationsListeners.size() == 0) {
                    cancel();
                }
            }
        }

        /**
         * Fires invitation listeners.
         */
        private void fireInvitationListeners(
            String room,
            String inviter,
            String reason,
            String password) {
            InvitationListener[] listeners = null;
            synchronized (invitationsListeners) {
                listeners = new InvitationListener[invitationsListeners.size()];
                invitationsListeners.toArray(listeners);
            }
            for (int i = 0; i < listeners.length; i++) {
                listeners[i].invitationReceived(connection, room, inviter, reason, password);
            }
        }

        public void connectionClosed() {
            cancel();
        }

        public void connectionClosedOnError(Exception e) {
            cancel();
        }

        /**
         * Initializes the listeners to detect received room invitations and to detect when the 
         * connection gets closed. As soon as a room invitation is received the invitations 
         * listeners will be fired. When the connection gets closed the monitor will remove
         * his listeners on the connection.       
         */
        private void init() {
            // Listens for all messages that include a MUCUser extension and fire the invitation 
            // listeners if the message includes an invitation.
            invitationFilter =
                new PacketExtensionFilter("x", "http://jabber.org/protocol/muc#user");
            invitationPacketListener = new PacketListener() {
                public void processPacket(Packet packet) {
                    // Get the MUCUser extension
                    MUCUser mucUser = 
                        (MUCUser) packet.getExtension("x", "http://jabber.org/protocol/muc#user");
                    // Check if the MUCUser extension includes an invitation
                    if (mucUser.getInvite() != null) {
                        // Fire event for invitation listeners
                        fireInvitationListeners(
                            packet.getFrom(),
                            mucUser.getInvite().getFrom(),
                            mucUser.getInvite().getReason(),
                            mucUser.getPassword());
                    }
                };
            };
            connection.addPacketListener(invitationPacketListener, invitationFilter);
            // Add a listener to detect when the connection gets closed in order to
            // cancel/release this monitor 
            connection.addConnectionListener(this);
        }

        /**
         * Cancels all the listeners that this InvitationsMonitor has added to the connection.
         */
        private void cancel() {
            connection.removePacketListener(invitationPacketListener);
            connection.removeConnectionListener(this);
        }

    }
}
