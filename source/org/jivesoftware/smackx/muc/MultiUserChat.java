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

package org.jivesoftware.smackx.muc;

import java.lang.ref.WeakReference;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.PacketInterceptor;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.FromMatchesFilter;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.filter.PacketExtensionFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketIDFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Registration;
import org.jivesoftware.smackx.Form;
import org.jivesoftware.smackx.NodeInformationProvider;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.packet.DiscoverInfo;
import org.jivesoftware.smackx.packet.DiscoverItems;
import org.jivesoftware.smackx.packet.MUCAdmin;
import org.jivesoftware.smackx.packet.MUCInitialPresence;
import org.jivesoftware.smackx.packet.MUCOwner;
import org.jivesoftware.smackx.packet.MUCUser;

/**
 * A MultiUserChat is a conversation that takes place among many users in a virtual
 * room. A room could have many occupants with different affiliation and roles.
 * Possible affiliatons are "owner", "admin", "member", and "outcast". Possible roles
 * are "moderator", "participant", and "visitor". Each role and affiliation guarantees
 * different privileges (e.g. Send messages to all occupants, Kick participants and visitors,
 * Grant voice, Edit member list, etc.).
 *
 * @author Gaston Dombiak, Larry Kirschner
 */
public class MultiUserChat {

    private final static String discoNamespace = "http://jabber.org/protocol/muc";
    private final static String discoNode = "http://jabber.org/protocol/muc#rooms";

    private static Map<Connection, List<String>> joinedRooms =
            new WeakHashMap<Connection, List<String>>();

    private Connection connection;
    private String room;
    private String subject;
    private String nickname = null;
    private boolean joined = false;
    private Map<String, Presence> occupantsMap = new ConcurrentHashMap<String, Presence>();

    private final List<InvitationRejectionListener> invitationRejectionListeners =
            new ArrayList<InvitationRejectionListener>();
    private final List<SubjectUpdatedListener> subjectUpdatedListeners =
            new ArrayList<SubjectUpdatedListener>();
    private final List<UserStatusListener> userStatusListeners =
            new ArrayList<UserStatusListener>();
    private final List<ParticipantStatusListener> participantStatusListeners =
            new ArrayList<ParticipantStatusListener>();

    private PacketFilter presenceFilter;
    private List<PacketInterceptor> presenceInterceptors = new ArrayList<PacketInterceptor>();
    private PacketFilter messageFilter;
    private RoomListenerMultiplexor roomListenerMultiplexor;
    private ConnectionDetachedPacketCollector messageCollector;
    private List<PacketListener> connectionListeners = new ArrayList<PacketListener>();

    static {
        Connection.addConnectionCreationListener(new ConnectionCreationListener() {
            public void connectionCreated(final Connection connection) {
                // Set on every established connection that this client supports the Multi-User
                // Chat protocol. This information will be used when another client tries to
                // discover whether this client supports MUC or not.
                ServiceDiscoveryManager.getInstanceFor(connection).addFeature(discoNamespace);
                // Set the NodeInformationProvider that will provide information about the
                // joined rooms whenever a disco request is received
                ServiceDiscoveryManager.getInstanceFor(connection).setNodeInformationProvider(
                    discoNode,
                    new NodeInformationProvider() {
                        public List<DiscoverItems.Item> getNodeItems() {
                            List<DiscoverItems.Item> answer = new ArrayList<DiscoverItems.Item>();
                            Iterator<String> rooms=MultiUserChat.getJoinedRooms(connection);
                            while (rooms.hasNext()) {
                                answer.add(new DiscoverItems.Item(rooms.next()));
                            }
                            return answer;
                        }

                        public List<String> getNodeFeatures() {
                            return null;
                        }

                        public List<DiscoverInfo.Identity> getNodeIdentities() {
                            return null;
                        }

                        @Override
                        public List<PacketExtension> getNodePacketExtensions() {
                            return null;
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
     *      service is running. Make sure to provide a valid JID.
     */
    public MultiUserChat(Connection connection, String room) {
        this.connection = connection;
        this.room = room.toLowerCase();
        init();
    }

    /**
     * Returns true if the specified user supports the Multi-User Chat protocol.
     *
     * @param connection the connection to use to perform the service discovery.
     * @param user the user to check. A fully qualified xmpp ID, e.g. jdoe@example.com.
     * @return a boolean indicating whether the specified user supports the MUC protocol.
     */
    public static boolean isServiceEnabled(Connection connection, String user) {
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
    private static Iterator<String> getJoinedRooms(Connection connection) {
        List<String> rooms = joinedRooms.get(connection);
        if (rooms != null) {
            return rooms.iterator();
        }
        // Return an iterator on an empty collection (i.e. the user never joined a room)
        return new ArrayList<String>().iterator();
    }

    /**
     * Returns an Iterator on the rooms where the requested user has joined. The Iterator will
     * contain Strings where each String represents a room (e.g. room@muc.jabber.org).
     *
     * @param connection the connection to use to perform the service discovery.
     * @param user the user to check. A fully qualified xmpp ID, e.g. jdoe@example.com.
     * @return an Iterator on the rooms where the requested user has joined.
     */
    public static Iterator<String> getJoinedRooms(Connection connection, String user) {
        try {
            ArrayList<String> answer = new ArrayList<String>();
            // Send the disco packet to the user
            DiscoverItems result =
                ServiceDiscoveryManager.getInstanceFor(connection).discoverItems(user, discoNode);
            // Collect the entityID for each returned item
            for (Iterator<DiscoverItems.Item> items=result.getItems(); items.hasNext();) {
                answer.add(items.next().getEntityID());
            }
            return answer.iterator();
        }
        catch (XMPPException e) {
            e.printStackTrace();
            // Return an iterator on an empty collection
            return new ArrayList<String>().iterator();
        }
    }

    /**
     * Returns the discovered information of a given room without actually having to join the room.
     * The server will provide information only for rooms that are public.
     *
     * @param connection the XMPP connection to use for discovering information about the room.
     * @param room the name of the room in the form "roomName@service" of which we want to discover
     *        its information.
     * @return the discovered information of a given room without actually having to join the room.
     * @throws XMPPException if an error occured while trying to discover information of a room.
     */
    public static RoomInfo getRoomInfo(Connection connection, String room)
            throws XMPPException {
        DiscoverInfo info = ServiceDiscoveryManager.getInstanceFor(connection).discoverInfo(room);
        return new RoomInfo(info);
    }

    /**
     * Returns a collection with the XMPP addresses of the Multi-User Chat services.
     *
     * @param connection the XMPP connection to use for discovering Multi-User Chat services.
     * @return a collection with the XMPP addresses of the Multi-User Chat services.
     * @throws XMPPException if an error occured while trying to discover MUC services.
     */
    public static Collection<String> getServiceNames(Connection connection) throws XMPPException {
        final List<String> answer = new ArrayList<String>();
        ServiceDiscoveryManager discoManager = ServiceDiscoveryManager.getInstanceFor(connection);
        DiscoverItems items = discoManager.discoverItems(connection.getServiceName());
        for (Iterator<DiscoverItems.Item> it = items.getItems(); it.hasNext();) {
            DiscoverItems.Item item = it.next();
            try {
                DiscoverInfo info = discoManager.discoverInfo(item.getEntityID());
                if (info.containsFeature("http://jabber.org/protocol/muc")) {
                    answer.add(item.getEntityID());
                }
            }
            catch (XMPPException e) {
                // Trouble finding info in some cases. This is a workaround for
                // discovering info on remote servers.
            }
        }
        return answer;
    }

    /**
     * Returns a collection of HostedRooms where each HostedRoom has the XMPP address of the room
     * and the room's name. Once discovered the rooms hosted by a chat service it is possible to
     * discover more detailed room information or join the room.
     *
     * @param connection the XMPP connection to use for discovering hosted rooms by the MUC service.
     * @param serviceName the service that is hosting the rooms to discover.
     * @return a collection of HostedRooms.
     * @throws XMPPException if an error occured while trying to discover the information.
     */
    public static Collection<HostedRoom> getHostedRooms(Connection connection, String serviceName)
            throws XMPPException {
        List<HostedRoom> answer = new ArrayList<HostedRoom>();
        ServiceDiscoveryManager discoManager = ServiceDiscoveryManager.getInstanceFor(connection);
        DiscoverItems items = discoManager.discoverItems(serviceName);
        for (Iterator<DiscoverItems.Item> it = items.getItems(); it.hasNext();) {
            answer.add(new HostedRoom(it.next()));
        }
        return answer;
    }

    /**
     * Returns the name of the room this MultiUserChat object represents.
     *
     * @return the multi user chat room name.
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
     * the room. {@link #sendConfigurationForm(Form)}<p>
     *
     * To create a "Reserved Room", that means a room manually configured by the room creator
     * before anyone is allowed to enter, the room's owner should complete and send a form after
     * creating the room. Once the completed configutation form is sent to the server, the server
     * will unlock the room. {@link #sendConfigurationForm(Form)}
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
        Presence joinPresence = new Presence(Presence.Type.available);
        joinPresence.setTo(room + "/" + nickname);
        // Indicate the the client supports MUC
        joinPresence.addExtension(new MUCInitialPresence());
        // Invoke presence interceptors so that extra information can be dynamically added
        for (PacketInterceptor packetInterceptor : presenceInterceptors) {
            packetInterceptor.interceptPacket(joinPresence);
        }

        // Wait for a presence packet back from the server.
        PacketFilter responseFilter =
            new AndFilter(
                new FromMatchesFilter(room + "/" + nickname),
                new PacketTypeFilter(Presence.class));
        PacketCollector response = connection.createPacketCollector(responseFilter);
        // Send create & join packet.
        connection.sendPacket(joinPresence);
        // Wait up to a certain number of seconds for a reply.
        Presence presence =
            (Presence) response.nextResult(SmackConfiguration.getPacketReplyTimeout());
        // Stop queuing results
        response.cancel();

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
        join(nickname, null, null, SmackConfiguration.getPacketReplyTimeout());
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
        join(nickname, password, null, SmackConfiguration.getPacketReplyTimeout());
    }

    /**
     * Joins the chat room using the specified nickname and password. If already joined
     * using another nickname, this method will first leave the room and then
     * re-join using the new nickname.<p>
     *
     * To control the amount of history to receive while joining a room you will need to provide
     * a configured DiscussionHistory object.<p>
     *
     * A password is required when joining password protected rooms. If the room does
     * not require a password there is no need to provide one.<p>
     *
     * If the room does not already exist when the user seeks to enter it, the server will
     * decide to create a new room or not.
     *
     * @param nickname the nickname to use.
     * @param password the password to use.
     * @param history the amount of discussion history to receive while joining a room.
     * @param timeout the amount of time to wait for a reply from the MUC service(in milleseconds).
     * @throws XMPPException if an error occurs joining the room. In particular, a
     *      401 error can occur if no password was provided and one is required; or a
     *      403 error can occur if the user is banned; or a
     *      404 error can occur if the room does not exist or is locked; or a
     *      407 error can occur if user is not on the member list; or a
     *      409 error can occur if someone is already in the group chat with the same nickname.
     */
    public synchronized void join(
        String nickname,
        String password,
        DiscussionHistory history,
        long timeout)
        throws XMPPException {
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
        Presence joinPresence = new Presence(Presence.Type.available);
        joinPresence.setTo(room + "/" + nickname);

        // Indicate the the client supports MUC
        MUCInitialPresence mucInitialPresence = new MUCInitialPresence();
        if (password != null) {
            mucInitialPresence.setPassword(password);
        }
        if (history != null) {
            mucInitialPresence.setHistory(history.getMUCHistory());
        }
        joinPresence.addExtension(mucInitialPresence);
        // Invoke presence interceptors so that extra information can be dynamically added
        for (PacketInterceptor packetInterceptor : presenceInterceptors) {
            packetInterceptor.interceptPacket(joinPresence);
        }

        // Wait for a presence packet back from the server.
        PacketFilter responseFilter =
                new AndFilter(
                        new FromMatchesFilter(room + "/" + nickname),
                        new PacketTypeFilter(Presence.class));
        PacketCollector response = null;
        Presence presence;
        try {
            response = connection.createPacketCollector(responseFilter);
            // Send join packet.
            connection.sendPacket(joinPresence);
            // Wait up to a certain number of seconds for a reply.
            presence = (Presence) response.nextResult(timeout);
        }
        finally {
            // Stop queuing results
            if (response != null) {
                response.cancel();
            }
        }

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
     * Returns true if currently in the multi user chat (after calling the {@link
     * #join(String)} method).
     *
     * @return true if currently in the multi user chat room.
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
        Presence leavePresence = new Presence(Presence.Type.unavailable);
        leavePresence.setTo(room + "/" + nickname);
        // Invoke presence interceptors so that extra information can be dynamically added
        for (PacketInterceptor packetInterceptor : presenceInterceptors) {
            packetInterceptor.interceptPacket(leavePresence);
        }
        connection.sendPacket(leavePresence);
        // Reset occupant information.
        occupantsMap.clear();
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

        // Filter packets looking for an answer from the server.
        PacketFilter responseFilter = new PacketIDFilter(iq.getPacketID());
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

        // Filter packets looking for an answer from the server.
        PacketFilter responseFilter = new PacketIDFilter(iq.getPacketID());
        PacketCollector response = connection.createPacketCollector(responseFilter);
        // Send the completed configuration form to the server.
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
        PacketFilter responseFilter = new PacketIDFilter(iq.getPacketID());
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
        // Reset occupant information.
        occupantsMap.clear();
        nickname = null;
        joined = false;
        userHasLeft();
    }

    /**
     * Invites another user to the room in which one is an occupant. The invitation
     * will be sent to the room which in turn will forward the invitation to the invitee.<p>
     *
     * If the room is password-protected, the invitee will receive a password to use to join
     * the room. If the room is members-only, the the invitee may be added to the member list.
     *
     * @param user the user to invite to the room.(e.g. hecate@shakespeare.lit)
     * @param reason the reason why the user is being invited.
     */
    public void invite(String user, String reason) {
        invite(new Message(), user, reason);
    }

    /**
     * Invites another user to the room in which one is an occupant using a given Message. The invitation
     * will be sent to the room which in turn will forward the invitation to the invitee.<p>
     *
     * If the room is password-protected, the invitee will receive a password to use to join
     * the room. If the room is members-only, the the invitee may be added to the member list.
     *
     * @param message the message to use for sending the invitation.
     * @param user the user to invite to the room.(e.g. hecate@shakespeare.lit)
     * @param reason the reason why the user is being invited.
     */
    public void invite(Message message, String user, String reason) {
        // TODO listen for 404 error code when inviter supplies a non-existent JID
        message.setTo(room);

        // Create the MUCUser packet that will include the invitation
        MUCUser mucUser = new MUCUser();
        MUCUser.Invite invite = new MUCUser.Invite();
        invite.setTo(user);
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
    public static void decline(Connection conn, String room, String inviter, String reason) {
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
    public static void addInvitationListener(Connection conn, InvitationListener listener) {
        InvitationsMonitor.getInvitationsMonitor(conn).addInvitationListener(listener);
    }

    /**
     * Removes a listener to invitation notifications. The listener will be fired anytime
     * an invitation is received.
     *
     * @param conn the connection where the listener was applied.
     * @param listener an invitation listener.
     */
    public static void removeInvitationListener(Connection conn, InvitationListener listener) {
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
     *
     * @param invitee the user being invited.
     * @param reason the reason for the rejection
     */
    private void fireInvitationRejectionListeners(String invitee, String reason) {
        InvitationRejectionListener[] listeners;
        synchronized (invitationRejectionListeners) {
            listeners = new InvitationRejectionListener[invitationRejectionListeners.size()];
            invitationRejectionListeners.toArray(listeners);
        }
        for (InvitationRejectionListener listener : listeners) {
            listener.invitationDeclined(invitee, reason);
        }
    }

    /**
     * Adds a listener to subject change notifications. The listener will be fired anytime
     * the room's subject changes.
     *
     * @param listener a subject updated listener.
     */
    public void addSubjectUpdatedListener(SubjectUpdatedListener listener) {
        synchronized (subjectUpdatedListeners) {
            if (!subjectUpdatedListeners.contains(listener)) {
                subjectUpdatedListeners.add(listener);
            }
        }
    }

    /**
     * Removes a listener from subject change notifications. The listener will be fired
     * anytime the room's subject changes.
     *
     * @param listener a subject updated listener.
     */
    public void removeSubjectUpdatedListener(SubjectUpdatedListener listener) {
        synchronized (subjectUpdatedListeners) {
            subjectUpdatedListeners.remove(listener);
        }
    }

    /**
     * Fires subject updated listeners.
     */
    private void fireSubjectUpdatedListeners(String subject, String from) {
        SubjectUpdatedListener[] listeners;
        synchronized (subjectUpdatedListeners) {
            listeners = new SubjectUpdatedListener[subjectUpdatedListeners.size()];
            subjectUpdatedListeners.toArray(listeners);
        }
        for (SubjectUpdatedListener listener : listeners) {
            listener.subjectUpdated(subject, from);
        }
    }

    /**
     * Adds a new {@link PacketInterceptor} that will be invoked every time a new presence
     * is going to be sent by this MultiUserChat to the server. Packet interceptors may
     * add new extensions to the presence that is going to be sent to the MUC service.
     *
     * @param presenceInterceptor the new packet interceptor that will intercept presence packets.
     */
    public void addPresenceInterceptor(PacketInterceptor presenceInterceptor) {
        presenceInterceptors.add(presenceInterceptor);
    }

    /**
     * Removes a {@link PacketInterceptor} that was being invoked every time a new presence
     * was being sent by this MultiUserChat to the server. Packet interceptors may
     * add new extensions to the presence that is going to be sent to the MUC service.
     *
     * @param presenceInterceptor the packet interceptor to remove.
     */
    public void removePresenceInterceptor(PacketInterceptor presenceInterceptor) {
        presenceInterceptors.remove(presenceInterceptor);
    }

    /**
     * Returns the last known room's subject or <tt>null</tt> if the user hasn't joined the room
     * or the room does not have a subject yet. In case the room has a subject, as soon as the
     * user joins the room a message with the current room's subject will be received.<p>
     *
     * To be notified every time the room's subject change you should add a listener
     * to this room. {@link #addSubjectUpdatedListener(SubjectUpdatedListener)}<p>
     *
     * To change the room's subject use {@link #changeSubject(String)}.
     *
     * @return the room's subject or <tt>null</tt> if the user hasn't joined the room or the
     * room does not have a subject yet.
     */
    public String getSubject() {
        return subject;
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
            for (Iterator<DiscoverInfo.Identity> identities = result.getIdentities();
                 identities.hasNext();) {
                DiscoverInfo.Identity identity = identities.next();
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
     * Changes the occupant's nickname to a new nickname within the room. Each room occupant
     * will receive two presence packets. One of type "unavailable" for the old nickname and one
     * indicating availability for the new nickname. The unavailable presence will contain the new
     * nickname and an appropriate status code (namely 303) as extended presence information. The
     * status code 303 indicates that the occupant is changing his/her nickname.
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
        Presence joinPresence = new Presence(Presence.Type.available);
        joinPresence.setTo(room + "/" + nickname);
        // Invoke presence interceptors so that extra information can be dynamically added
        for (PacketInterceptor packetInterceptor : presenceInterceptors) {
            packetInterceptor.interceptPacket(joinPresence);
        }

        // Wait for a presence packet back from the server.
        PacketFilter responseFilter =
            new AndFilter(
                new FromMatchesFilter(room + "/" + nickname),
                new PacketTypeFilter(Presence.class));
        PacketCollector response = connection.createPacketCollector(responseFilter);
        // Send join packet.
        connection.sendPacket(joinPresence);
        // Wait up to a certain number of seconds for a reply.
        Presence presence =
            (Presence) response.nextResult(SmackConfiguration.getPacketReplyTimeout());
        // Stop queuing results
        response.cancel();

        if (presence == null) {
            throw new XMPPException("No response from server.");
        }
        else if (presence.getError() != null) {
            throw new XMPPException(presence.getError());
        }
        this.nickname = nickname;
    }

    /**
     * Changes the occupant's availability status within the room. The presence type
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
        Presence joinPresence = new Presence(Presence.Type.available);
        joinPresence.setStatus(status);
        joinPresence.setMode(mode);
        joinPresence.setTo(room + "/" + nickname);
        // Invoke presence interceptors so that extra information can be dynamically added
        for (PacketInterceptor packetInterceptor : presenceInterceptors) {
            packetInterceptor.interceptPacket(joinPresence);
        }

        // Send join packet.
        connection.sendPacket(joinPresence);
    }

    /**
     * Kicks a visitor or participant from the room. The kicked occupant will receive a presence
     * of type "unavailable" including a status code 307 and optionally along with the reason
     * (if provided) and the bare JID of the user who initiated the kick. After the occupant
     * was kicked from the room, the rest of the occupants will receive a presence of type
     * "unavailable". The presence will include a status code 307 which means that the occupant
     * was kicked from the room.
     *
     * @param nickname the nickname of the participant or visitor to kick from the room
     * (e.g. "john").
     * @param reason the reason why the participant or visitor is being kicked from the room.
     * @throws XMPPException if an error occurs kicking the occupant. In particular, a
     *      405 error can occur if a moderator or a user with an affiliation of "owner" or "admin"
     *      was intended to be kicked (i.e. Not Allowed error); or a
     *      403 error can occur if the occupant that intended to kick another occupant does
     *      not have kicking privileges (i.e. Forbidden error); or a
     *      400 error can occur if the provided nickname is not present in the room.
     */
    public void kickParticipant(String nickname, String reason) throws XMPPException {
        changeRole(nickname, "none", reason);
    }

    /**
     * Grants voice to visitors in the room. In a moderated room, a moderator may want to manage
     * who does and does not have "voice" in the room. To have voice means that a room occupant
     * is able to send messages to the room occupants.
     *
     * @param nicknames the nicknames of the visitors to grant voice in the room (e.g. "john").
     * @throws XMPPException if an error occurs granting voice to a visitor. In particular, a
     *      403 error can occur if the occupant that intended to grant voice is not
     *      a moderator in this room (i.e. Forbidden error); or a
     *      400 error can occur if the provided nickname is not present in the room.
     */
    public void grantVoice(Collection<String> nicknames) throws XMPPException {
        changeRole(nicknames, "participant");
    }

    /**
     * Grants voice to a visitor in the room. In a moderated room, a moderator may want to manage
     * who does and does not have "voice" in the room. To have voice means that a room occupant
     * is able to send messages to the room occupants.
     *
     * @param nickname the nickname of the visitor to grant voice in the room (e.g. "john").
     * @throws XMPPException if an error occurs granting voice to a visitor. In particular, a
     *      403 error can occur if the occupant that intended to grant voice is not
     *      a moderator in this room (i.e. Forbidden error); or a
     *      400 error can occur if the provided nickname is not present in the room.
     */
    public void grantVoice(String nickname) throws XMPPException {
        changeRole(nickname, "participant", null);
    }

    /**
     * Revokes voice from participants in the room. In a moderated room, a moderator may want to
     * revoke an occupant's privileges to speak. To have voice means that a room occupant
     * is able to send messages to the room occupants.
     *
     * @param nicknames the nicknames of the participants to revoke voice (e.g. "john").
     * @throws XMPPException if an error occurs revoking voice from a participant. In particular, a
     *      405 error can occur if a moderator or a user with an affiliation of "owner" or "admin"
     *      was tried to revoke his voice (i.e. Not Allowed error); or a
     *      400 error can occur if the provided nickname is not present in the room.
     */
    public void revokeVoice(Collection<String> nicknames) throws XMPPException {
        changeRole(nicknames, "visitor");
    }

    /**
     * Revokes voice from a participant in the room. In a moderated room, a moderator may want to
     * revoke an occupant's privileges to speak. To have voice means that a room occupant
     * is able to send messages to the room occupants.
     *
     * @param nickname the nickname of the participant to revoke voice (e.g. "john").
     * @throws XMPPException if an error occurs revoking voice from a participant. In particular, a
     *      405 error can occur if a moderator or a user with an affiliation of "owner" or "admin"
     *      was tried to revoke his voice (i.e. Not Allowed error); or a
     *      400 error can occur if the provided nickname is not present in the room.
     */
    public void revokeVoice(String nickname) throws XMPPException {
        changeRole(nickname, "visitor", null);
    }

    /**
     * Bans users from the room. An admin or owner of the room can ban users from a room. This
     * means that the banned user will no longer be able to join the room unless the ban has been
     * removed. If the banned user was present in the room then he/she will be removed from the
     * room and notified that he/she was banned along with the reason (if provided) and the bare
     * XMPP user ID of the user who initiated the ban.
     *
     * @param jids the bare XMPP user IDs of the users to ban.
     * @throws XMPPException if an error occurs banning a user. In particular, a
     *      405 error can occur if a moderator or a user with an affiliation of "owner" or "admin"
     *      was tried to be banned (i.e. Not Allowed error).
     */
    public void banUsers(Collection<String> jids) throws XMPPException {
        changeAffiliationByAdmin(jids, "outcast");
    }

    /**
     * Bans a user from the room. An admin or owner of the room can ban users from a room. This
     * means that the banned user will no longer be able to join the room unless the ban has been
     * removed. If the banned user was present in the room then he/she will be removed from the
     * room and notified that he/she was banned along with the reason (if provided) and the bare
     * XMPP user ID of the user who initiated the ban.
     *
     * @param jid the bare XMPP user ID of the user to ban (e.g. "user@host.org").
     * @param reason the optional reason why the user was banned.
     * @throws XMPPException if an error occurs banning a user. In particular, a
     *      405 error can occur if a moderator or a user with an affiliation of "owner" or "admin"
     *      was tried to be banned (i.e. Not Allowed error).
     */
    public void banUser(String jid, String reason) throws XMPPException {
        changeAffiliationByAdmin(jid, "outcast", reason);
    }

    /**
     * Grants membership to other users. Only administrators are able to grant membership. A user
     * that becomes a room member will be able to enter a room of type Members-Only (i.e. a room
     * that a user cannot enter without being on the member list).
     *
     * @param jids the XMPP user IDs of the users to grant membership.
     * @throws XMPPException if an error occurs granting membership to a user.
     */
    public void grantMembership(Collection<String> jids) throws XMPPException {
        changeAffiliationByAdmin(jids, "member");
    }

    /**
     * Grants membership to a user. Only administrators are able to grant membership. A user
     * that becomes a room member will be able to enter a room of type Members-Only (i.e. a room
     * that a user cannot enter without being on the member list).
     *
     * @param jid the XMPP user ID of the user to grant membership (e.g. "user@host.org").
     * @throws XMPPException if an error occurs granting membership to a user.
     */
    public void grantMembership(String jid) throws XMPPException {
        changeAffiliationByAdmin(jid, "member", null);
    }

    /**
     * Revokes users' membership. Only administrators are able to revoke membership. A user
     * that becomes a room member will be able to enter a room of type Members-Only (i.e. a room
     * that a user cannot enter without being on the member list). If the user is in the room and
     * the room is of type members-only then the user will be removed from the room.
     *
     * @param jids the bare XMPP user IDs of the users to revoke membership.
     * @throws XMPPException if an error occurs revoking membership to a user.
     */
    public void revokeMembership(Collection<String> jids) throws XMPPException {
        changeAffiliationByAdmin(jids, "none");
    }

    /**
     * Revokes a user's membership. Only administrators are able to revoke membership. A user
     * that becomes a room member will be able to enter a room of type Members-Only (i.e. a room
     * that a user cannot enter without being on the member list). If the user is in the room and
     * the room is of type members-only then the user will be removed from the room.
     *
     * @param jid the bare XMPP user ID of the user to revoke membership (e.g. "user@host.org").
     * @throws XMPPException if an error occurs revoking membership to a user.
     */
    public void revokeMembership(String jid) throws XMPPException {
        changeAffiliationByAdmin(jid, "none", null);
    }

    /**
     * Grants moderator privileges to participants or visitors. Room administrators may grant
     * moderator privileges. A moderator is allowed to kick users, grant and revoke voice, invite
     * other users, modify room's subject plus all the partcipants privileges.
     *
     * @param nicknames the nicknames of the occupants to grant moderator privileges.
     * @throws XMPPException if an error occurs granting moderator privileges to a user.
     */
    public void grantModerator(Collection<String> nicknames) throws XMPPException {
        changeRole(nicknames, "moderator");
    }

    /**
     * Grants moderator privileges to a participant or visitor. Room administrators may grant
     * moderator privileges. A moderator is allowed to kick users, grant and revoke voice, invite
     * other users, modify room's subject plus all the partcipants privileges.
     *
     * @param nickname the nickname of the occupant to grant moderator privileges.
     * @throws XMPPException if an error occurs granting moderator privileges to a user.
     */
    public void grantModerator(String nickname) throws XMPPException {
        changeRole(nickname, "moderator", null);
    }

    /**
     * Revokes moderator privileges from other users. The occupant that loses moderator
     * privileges will become a participant. Room administrators may revoke moderator privileges
     * only to occupants whose affiliation is member or none. This means that an administrator is
     * not allowed to revoke moderator privileges from other room administrators or owners.
     *
     * @param nicknames the nicknames of the occupants to revoke moderator privileges.
     * @throws XMPPException if an error occurs revoking moderator privileges from a user.
     */
    public void revokeModerator(Collection<String> nicknames) throws XMPPException {
        changeRole(nicknames, "participant");
    }

    /**
     * Revokes moderator privileges from another user. The occupant that loses moderator
     * privileges will become a participant. Room administrators may revoke moderator privileges
     * only to occupants whose affiliation is member or none. This means that an administrator is
     * not allowed to revoke moderator privileges from other room administrators or owners.
     *
     * @param nickname the nickname of the occupant to revoke moderator privileges.
     * @throws XMPPException if an error occurs revoking moderator privileges from a user.
     */
    public void revokeModerator(String nickname) throws XMPPException {
        changeRole(nickname, "participant", null);
    }

    /**
     * Grants ownership privileges to other users. Room owners may grant ownership privileges.
     * Some room implementations will not allow to grant ownership privileges to other users.
     * An owner is allowed to change defining room features as well as perform all administrative
     * functions.
     *
     * @param jids the collection of bare XMPP user IDs of the users to grant ownership.
     * @throws XMPPException if an error occurs granting ownership privileges to a user.
     */
    public void grantOwnership(Collection<String> jids) throws XMPPException {
        changeAffiliationByAdmin(jids, "owner");
    }

    /**
     * Grants ownership privileges to another user. Room owners may grant ownership privileges.
     * Some room implementations will not allow to grant ownership privileges to other users.
     * An owner is allowed to change defining room features as well as perform all administrative
     * functions.
     *
     * @param jid the bare XMPP user ID of the user to grant ownership (e.g. "user@host.org").
     * @throws XMPPException if an error occurs granting ownership privileges to a user.
     */
    public void grantOwnership(String jid) throws XMPPException {
        changeAffiliationByAdmin(jid, "owner", null);
    }

    /**
     * Revokes ownership privileges from other users. The occupant that loses ownership
     * privileges will become an administrator. Room owners may revoke ownership privileges.
     * Some room implementations will not allow to grant ownership privileges to other users.
     *
     * @param jids the bare XMPP user IDs of the users to revoke ownership.
     * @throws XMPPException if an error occurs revoking ownership privileges from a user.
     */
    public void revokeOwnership(Collection<String> jids) throws XMPPException {
        changeAffiliationByAdmin(jids, "admin");
    }

    /**
     * Revokes ownership privileges from another user. The occupant that loses ownership
     * privileges will become an administrator. Room owners may revoke ownership privileges.
     * Some room implementations will not allow to grant ownership privileges to other users.
     *
     * @param jid the bare XMPP user ID of the user to revoke ownership (e.g. "user@host.org").
     * @throws XMPPException if an error occurs revoking ownership privileges from a user.
     */
    public void revokeOwnership(String jid) throws XMPPException {
        changeAffiliationByAdmin(jid, "admin", null);
    }

    /**
     * Grants administrator privileges to other users. Room owners may grant administrator
     * privileges to a member or unaffiliated user. An administrator is allowed to perform
     * administrative functions such as banning users and edit moderator list.
     *
     * @param jids the bare XMPP user IDs of the users to grant administrator privileges.
     * @throws XMPPException if an error occurs granting administrator privileges to a user.
     */
    public void grantAdmin(Collection<String> jids) throws XMPPException {
        changeAffiliationByOwner(jids, "admin");
    }

    /**
     * Grants administrator privileges to another user. Room owners may grant administrator
     * privileges to a member or unaffiliated user. An administrator is allowed to perform
     * administrative functions such as banning users and edit moderator list.
     *
     * @param jid the bare XMPP user ID of the user to grant administrator privileges
     * (e.g. "user@host.org").
     * @throws XMPPException if an error occurs granting administrator privileges to a user.
     */
    public void grantAdmin(String jid) throws XMPPException {
        changeAffiliationByOwner(jid, "admin");
    }

    /**
     * Revokes administrator privileges from users. The occupant that loses administrator
     * privileges will become a member. Room owners may revoke administrator privileges from
     * a member or unaffiliated user.
     *
     * @param jids the bare XMPP user IDs of the user to revoke administrator privileges.
     * @throws XMPPException if an error occurs revoking administrator privileges from a user.
     */
    public void revokeAdmin(Collection<String> jids) throws XMPPException {
        changeAffiliationByOwner(jids, "member");
    }

    /**
     * Revokes administrator privileges from a user. The occupant that loses administrator
     * privileges will become a member. Room owners may revoke administrator privileges from
     * a member or unaffiliated user.
     *
     * @param jid the bare XMPP user ID of the user to revoke administrator privileges
     * (e.g. "user@host.org").
     * @throws XMPPException if an error occurs revoking administrator privileges from a user.
     */
    public void revokeAdmin(String jid) throws XMPPException {
        changeAffiliationByOwner(jid, "member");
    }

    private void changeAffiliationByOwner(String jid, String affiliation) throws XMPPException {
        MUCOwner iq = new MUCOwner();
        iq.setTo(room);
        iq.setType(IQ.Type.SET);
        // Set the new affiliation.
        MUCOwner.Item item = new MUCOwner.Item(affiliation);
        item.setJid(jid);
        iq.addItem(item);

        // Wait for a response packet back from the server.
        PacketFilter responseFilter = new PacketIDFilter(iq.getPacketID());
        PacketCollector response = connection.createPacketCollector(responseFilter);
        // Send the change request to the server.
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

    private void changeAffiliationByOwner(Collection<String> jids, String affiliation)
            throws XMPPException {
        MUCOwner iq = new MUCOwner();
        iq.setTo(room);
        iq.setType(IQ.Type.SET);
        for (String jid : jids) {
            // Set the new affiliation.
            MUCOwner.Item item = new MUCOwner.Item(affiliation);
            item.setJid(jid);
            iq.addItem(item);
        }

        // Wait for a response packet back from the server.
        PacketFilter responseFilter = new PacketIDFilter(iq.getPacketID());
        PacketCollector response = connection.createPacketCollector(responseFilter);
        // Send the change request to the server.
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
     * Tries to change the affiliation with an 'muc#admin' namespace
     *
     * @param jid
     * @param affiliation
     * @param reason the reason for the affiliation change (optional)
     * @throws XMPPException
     */
    private void changeAffiliationByAdmin(String jid, String affiliation, String reason)
            throws XMPPException {
        MUCAdmin iq = new MUCAdmin();
        iq.setTo(room);
        iq.setType(IQ.Type.SET);
        // Set the new affiliation.
        MUCAdmin.Item item = new MUCAdmin.Item(affiliation, null);
        item.setJid(jid);
        item.setReason(reason);
        iq.addItem(item);

        // Wait for a response packet back from the server.
        PacketFilter responseFilter = new PacketIDFilter(iq.getPacketID());
        PacketCollector response = connection.createPacketCollector(responseFilter);
        // Send the change request to the server.
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

    private void changeAffiliationByAdmin(Collection<String> jids, String affiliation)
            throws XMPPException {
        MUCAdmin iq = new MUCAdmin();
        iq.setTo(room);
        iq.setType(IQ.Type.SET);
        for (String jid : jids) {
            // Set the new affiliation.
            MUCAdmin.Item item = new MUCAdmin.Item(affiliation, null);
            item.setJid(jid);
            iq.addItem(item);
        }

        // Wait for a response packet back from the server.
        PacketFilter responseFilter = new PacketIDFilter(iq.getPacketID());
        PacketCollector response = connection.createPacketCollector(responseFilter);
        // Send the change request to the server.
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

    private void changeRole(String nickname, String role, String reason) throws XMPPException {
        MUCAdmin iq = new MUCAdmin();
        iq.setTo(room);
        iq.setType(IQ.Type.SET);
        // Set the new role.
        MUCAdmin.Item item = new MUCAdmin.Item(null, role);
        item.setNick(nickname);
        item.setReason(reason);
        iq.addItem(item);

        // Wait for a response packet back from the server.
        PacketFilter responseFilter = new PacketIDFilter(iq.getPacketID());
        PacketCollector response = connection.createPacketCollector(responseFilter);
        // Send the change request to the server.
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

    private void changeRole(Collection<String> nicknames, String role) throws XMPPException {
        MUCAdmin iq = new MUCAdmin();
        iq.setTo(room);
        iq.setType(IQ.Type.SET);
        for (String nickname : nicknames) {
            // Set the new role.
            MUCAdmin.Item item = new MUCAdmin.Item(null, role);
            item.setNick(nickname);
            iq.addItem(item);
        }

        // Wait for a response packet back from the server.
        PacketFilter responseFilter = new PacketIDFilter(iq.getPacketID());
        PacketCollector response = connection.createPacketCollector(responseFilter);
        // Send the change request to the server.
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
     * Returns the number of occupants in the group chat.<p>
     *
     * Note: this value will only be accurate after joining the group chat, and
     * may fluctuate over time. If you query this value directly after joining the
     * group chat it may not be accurate, as it takes a certain amount of time for
     * the server to send all presence packets to this client.
     *
     * @return the number of occupants in the group chat.
     */
    public int getOccupantsCount() {
        return occupantsMap.size();
    }

    /**
     * Returns an Iterator (of Strings) for the list of fully qualified occupants
     * in the group chat. For example, "conference@chat.jivesoftware.com/SomeUser".
     * Typically, a client would only display the nickname of the occupant. To
     * get the nickname from the fully qualified name, use the
     * {@link org.jivesoftware.smack.util.StringUtils#parseResource(String)} method.
     * Note: this value will only be accurate after joining the group chat, and may
     * fluctuate over time.
     *
     * @return an Iterator for the occupants in the group chat.
     */
    public Iterator<String> getOccupants() {
        return Collections.unmodifiableList(new ArrayList<String>(occupantsMap.keySet()))
                .iterator();
    }

    /**
     * Returns the presence info for a particular user, or <tt>null</tt> if the user
     * is not in the room.<p>
     *
     * @param user the room occupant to search for his presence. The format of user must
     * be: roomName@service/nickname (e.g. darkcave@macbeth.shakespeare.lit/thirdwitch).
     * @return the occupant's current presence, or <tt>null</tt> if the user is unavailable
     *      or if no presence information is available.
     */
    public Presence getOccupantPresence(String user) {
        return occupantsMap.get(user);
    }

    /**
     * Returns the Occupant information for a particular occupant, or <tt>null</tt> if the
     * user is not in the room. The Occupant object may include information such as full
     * JID of the user as well as the role and affiliation of the user in the room.<p>
     *
     * @param user the room occupant to search for his presence. The format of user must
     * be: roomName@service/nickname (e.g. darkcave@macbeth.shakespeare.lit/thirdwitch).
     * @return the Occupant or <tt>null</tt> if the user is unavailable (i.e. not in the room).
     */
    public Occupant getOccupant(String user) {
        Presence presence = occupantsMap.get(user);
        if (presence != null) {
            return new Occupant(presence);
        }
        return null;
    }

    /**
     * Adds a packet listener that will be notified of any new Presence packets
     * sent to the group chat. Using a listener is a suitable way to know when the list
     * of occupants should be re-loaded due to any changes.
     *
     * @param listener a packet listener that will be notified of any presence packets
     *      sent to the group chat.
     */
    public void addParticipantListener(PacketListener listener) {
        connection.addPacketListener(listener, presenceFilter);
        connectionListeners.add(listener);
    }

    /**
     * Remoces a packet listener that was being notified of any new Presence packets
     * sent to the group chat.
     *
     * @param listener a packet listener that was being notified of any presence packets
     *      sent to the group chat.
     */
    public void removeParticipantListener(PacketListener listener) {
        connection.removePacketListener(listener);
        connectionListeners.remove(listener);
    }

    /**
     * Returns a collection of <code>Affiliate</code> with the room owners.
     *
     * @return a collection of <code>Affiliate</code> with the room owners.
     * @throws XMPPException if an error occured while performing the request to the server or you
     *         don't have enough privileges to get this information.
     */
    public Collection<Affiliate> getOwners() throws XMPPException {
        return getAffiliatesByAdmin("owner");
    }

    /**
     * Returns a collection of <code>Affiliate</code> with the room administrators.
     *
     * @return a collection of <code>Affiliate</code> with the room administrators.
     * @throws XMPPException if an error occured while performing the request to the server or you
     *         don't have enough privileges to get this information.
     */
    public Collection<Affiliate> getAdmins() throws XMPPException {
        return getAffiliatesByOwner("admin");
    }

    /**
     * Returns a collection of <code>Affiliate</code> with the room members.
     *
     * @return a collection of <code>Affiliate</code> with the room members.
     * @throws XMPPException if an error occured while performing the request to the server or you
     *         don't have enough privileges to get this information.
     */
    public Collection<Affiliate> getMembers() throws XMPPException {
        return getAffiliatesByAdmin("member");
    }

    /**
     * Returns a collection of <code>Affiliate</code> with the room outcasts.
     *
     * @return a collection of <code>Affiliate</code> with the room outcasts.
     * @throws XMPPException if an error occured while performing the request to the server or you
     *         don't have enough privileges to get this information.
     */
    public Collection<Affiliate> getOutcasts() throws XMPPException {
        return getAffiliatesByAdmin("outcast");
    }

    /**
     * Returns a collection of <code>Affiliate</code> that have the specified room affiliation
     * sending a request in the owner namespace.
     *
     * @param affiliation the affiliation of the users in the room.
     * @return a collection of <code>Affiliate</code> that have the specified room affiliation.
     * @throws XMPPException if an error occured while performing the request to the server or you
     *         don't have enough privileges to get this information.
     */
    private Collection<Affiliate> getAffiliatesByOwner(String affiliation) throws XMPPException {
        MUCOwner iq = new MUCOwner();
        iq.setTo(room);
        iq.setType(IQ.Type.GET);
        // Set the specified affiliation. This may request the list of owners/admins/members/outcasts.
        MUCOwner.Item item = new MUCOwner.Item(affiliation);
        iq.addItem(item);

        // Wait for a response packet back from the server.
        PacketFilter responseFilter = new PacketIDFilter(iq.getPacketID());
        PacketCollector response = connection.createPacketCollector(responseFilter);
        // Send the request to the server.
        connection.sendPacket(iq);
        // Wait up to a certain number of seconds for a reply.
        MUCOwner answer = (MUCOwner) response.nextResult(SmackConfiguration.getPacketReplyTimeout());
        // Stop queuing results
        response.cancel();

        if (answer == null) {
            throw new XMPPException("No response from server.");
        }
        else if (answer.getError() != null) {
            throw new XMPPException(answer.getError());
        }
        // Get the list of affiliates from the server's answer
        List<Affiliate> affiliates = new ArrayList<Affiliate>();
        for (Iterator<MUCOwner.Item> it = answer.getItems(); it.hasNext();) {
            affiliates.add(new Affiliate(it.next()));
        }
        return affiliates;
    }

    /**
     * Returns a collection of <code>Affiliate</code> that have the specified room affiliation
     * sending a request in the admin namespace.
     *
     * @param affiliation the affiliation of the users in the room.
     * @return a collection of <code>Affiliate</code> that have the specified room affiliation.
     * @throws XMPPException if an error occured while performing the request to the server or you
     *         don't have enough privileges to get this information.
     */
    private Collection<Affiliate> getAffiliatesByAdmin(String affiliation) throws XMPPException {
        MUCAdmin iq = new MUCAdmin();
        iq.setTo(room);
        iq.setType(IQ.Type.GET);
        // Set the specified affiliation. This may request the list of owners/admins/members/outcasts.
        MUCAdmin.Item item = new MUCAdmin.Item(affiliation, null);
        iq.addItem(item);

        // Wait for a response packet back from the server.
        PacketFilter responseFilter = new PacketIDFilter(iq.getPacketID());
        PacketCollector response = connection.createPacketCollector(responseFilter);
        // Send the request to the server.
        connection.sendPacket(iq);
        // Wait up to a certain number of seconds for a reply.
        MUCAdmin answer = (MUCAdmin) response.nextResult(SmackConfiguration.getPacketReplyTimeout());
        // Stop queuing results
        response.cancel();

        if (answer == null) {
            throw new XMPPException("No response from server.");
        }
        else if (answer.getError() != null) {
            throw new XMPPException(answer.getError());
        }
        // Get the list of affiliates from the server's answer
        List<Affiliate> affiliates = new ArrayList<Affiliate>();
        for (Iterator<MUCAdmin.Item> it = answer.getItems(); it.hasNext();) {
            affiliates.add(new Affiliate(it.next()));
        }
        return affiliates;
    }

    /**
     * Returns a collection of <code>Occupant</code> with the room moderators.
     *
     * @return a collection of <code>Occupant</code> with the room moderators.
     * @throws XMPPException if an error occured while performing the request to the server or you
     *         don't have enough privileges to get this information.
     */
    public Collection<Occupant> getModerators() throws XMPPException {
        return getOccupants("moderator");
    }

    /**
     * Returns a collection of <code>Occupant</code> with the room participants.
     *
     * @return a collection of <code>Occupant</code> with the room participants.
     * @throws XMPPException if an error occured while performing the request to the server or you
     *         don't have enough privileges to get this information.
     */
    public Collection<Occupant> getParticipants() throws XMPPException {
        return getOccupants("participant");
    }

    /**
     * Returns a collection of <code>Occupant</code> that have the specified room role.
     *
     * @param role the role of the occupant in the room.
     * @return a collection of <code>Occupant</code> that have the specified room role.
     * @throws XMPPException if an error occured while performing the request to the server or you
     *         don't have enough privileges to get this information.
     */
    private Collection<Occupant> getOccupants(String role) throws XMPPException {
        MUCAdmin iq = new MUCAdmin();
        iq.setTo(room);
        iq.setType(IQ.Type.GET);
        // Set the specified role. This may request the list of moderators/participants.
        MUCAdmin.Item item = new MUCAdmin.Item(null, role);
        iq.addItem(item);

        // Wait for a response packet back from the server.
        PacketFilter responseFilter = new PacketIDFilter(iq.getPacketID());
        PacketCollector response = connection.createPacketCollector(responseFilter);
        // Send the request to the server.
        connection.sendPacket(iq);
        // Wait up to a certain number of seconds for a reply.
        MUCAdmin answer = (MUCAdmin) response.nextResult(SmackConfiguration.getPacketReplyTimeout());
        // Stop queuing results
        response.cancel();

        if (answer == null) {
            throw new XMPPException("No response from server.");
        }
        else if (answer.getError() != null) {
            throw new XMPPException(answer.getError());
        }
        // Get the list of participants from the server's answer
        List<Occupant> participants = new ArrayList<Occupant>();
        for (Iterator<MUCAdmin.Item> it = answer.getItems(); it.hasNext();) {
            participants.add(new Occupant(it.next()));
        }
        return participants;
    }

    /**
     * Sends a message to the chat room.
     *
     * @param text the text of the message to send.
     * @throws XMPPException if sending the message fails.
     */
    public void sendMessage(String text) throws XMPPException {
        Message message = new Message(room, Message.Type.groupchat);
        message.setBody(text);
        connection.sendPacket(message);
    }

    /**
     * Returns a new Chat for sending private messages to a given room occupant.
     * The Chat's occupant address is the room's JID (i.e. roomName@service/nick). The server
     * service will change the 'from' address to the sender's room JID and delivering the message
     * to the intended recipient's full JID.
     *
     * @param occupant occupant unique room JID (e.g. 'darkcave@macbeth.shakespeare.lit/Paul').
     * @param listener the listener is a message listener that will handle messages for the newly
     * created chat.
     * @return new Chat for sending private messages to a given room occupant.
     */
    public Chat createPrivateChat(String occupant, MessageListener listener) {
        return connection.getChatManager().createChat(occupant, listener);
    }

    /**
     * Creates a new Message to send to the chat room.
     *
     * @return a new Message addressed to the chat room.
     */
    public Message createMessage() {
        return new Message(room, Message.Type.groupchat);
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
     * PacketListener directly with the Connection with the appropriate
     * PacketListener.
     *
     * @param listener a packet listener.
     */
    public void addMessageListener(PacketListener listener) {
        connection.addPacketListener(listener, messageFilter);
        connectionListeners.add(listener);
    }

    /**
     * Removes a packet listener that was being notified of any new messages in the
     * multi user chat. Only "group chat" messages addressed to this multi user chat were
     * being delivered to the listener.
     *
     * @param listener a packet listener.
     */
    public void removeMessageListener(PacketListener listener) {
        connection.removePacketListener(listener);
        connectionListeners.remove(listener);
    }

    /**
     * Changes the subject within the room. As a default, only users with a role of "moderator"
     * are allowed to change the subject in a room. Although some rooms may be configured to
     * allow a mere participant or even a visitor to change the subject.
     *
     * @param subject the new room's subject to set.
     * @throws XMPPException if someone without appropriate privileges attempts to change the
     *          room subject will throw an error with code 403 (i.e. Forbidden)
     */
    public void changeSubject(final String subject) throws XMPPException {
        Message message = new Message(room, Message.Type.groupchat);
        message.setSubject(subject);
        // Wait for an error or confirmation message back from the server.
        PacketFilter responseFilter =
            new AndFilter(
                new FromMatchesFilter(room),
                new PacketTypeFilter(Message.class));
        responseFilter = new AndFilter(responseFilter, new PacketFilter() {
            public boolean accept(Packet packet) {
                Message msg = (Message) packet;
                return subject.equals(msg.getSubject());
            }
        });
        PacketCollector response = connection.createPacketCollector(responseFilter);
        // Send change subject packet.
        connection.sendPacket(message);
        // Wait up to a certain number of seconds for a reply.
        Message answer =
            (Message) response.nextResult(SmackConfiguration.getPacketReplyTimeout());
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
     * Notification message that the user has joined the room.
     */
    private synchronized void userHasJoined() {
        // Update the list of joined rooms through this connection
        List<String> rooms = joinedRooms.get(connection);
        if (rooms == null) {
            rooms = new ArrayList<String>();
            joinedRooms.put(connection, rooms);
        }
        rooms.add(room);
    }

    /**
     * Notification message that the user has left the room.
     */
    private synchronized void userHasLeft() {
        // Update the list of joined rooms through this connection
        List<String> rooms = joinedRooms.get(connection);
        if (rooms == null) {
            return;
        }
        rooms.remove(room);
        cleanup();
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

    /**
     * Adds a listener that will be notified of changes in your status in the room
     * such as the user being kicked, banned, or granted admin permissions.
     *
     * @param listener a user status listener.
     */
    public void addUserStatusListener(UserStatusListener listener) {
        synchronized (userStatusListeners) {
            if (!userStatusListeners.contains(listener)) {
                userStatusListeners.add(listener);
            }
        }
    }

    /**
     * Removes a listener that was being notified of changes in your status in the room
     * such as the user being kicked, banned, or granted admin permissions.
     *
     * @param listener a user status listener.
     */
    public void removeUserStatusListener(UserStatusListener listener) {
        synchronized (userStatusListeners) {
            userStatusListeners.remove(listener);
        }
    }

    private void fireUserStatusListeners(String methodName, Object[] params) {
        UserStatusListener[] listeners;
        synchronized (userStatusListeners) {
            listeners = new UserStatusListener[userStatusListeners.size()];
            userStatusListeners.toArray(listeners);
        }
        // Get the classes of the method parameters
        Class<?>[] paramClasses = new Class[params.length];
        for (int i = 0; i < params.length; i++) {
            paramClasses[i] = params[i].getClass();
        }
        try {
            // Get the method to execute based on the requested methodName and parameters classes
            Method method = UserStatusListener.class.getDeclaredMethod(methodName, paramClasses);
            for (UserStatusListener listener : listeners) {
                method.invoke(listener, params);
            }
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * Adds a listener that will be notified of changes in occupants status in the room
     * such as the user being kicked, banned, or granted admin permissions.
     *
     * @param listener a participant status listener.
     */
    public void addParticipantStatusListener(ParticipantStatusListener listener) {
        synchronized (participantStatusListeners) {
            if (!participantStatusListeners.contains(listener)) {
                participantStatusListeners.add(listener);
            }
        }
    }

    /**
     * Removes a listener that was being notified of changes in occupants status in the room
     * such as the user being kicked, banned, or granted admin permissions.
     *
     * @param listener a participant status listener.
     */
    public void removeParticipantStatusListener(ParticipantStatusListener listener) {
        synchronized (participantStatusListeners) {
            participantStatusListeners.remove(listener);
        }
    }

    private void fireParticipantStatusListeners(String methodName, List<String> params) {
        ParticipantStatusListener[] listeners;
        synchronized (participantStatusListeners) {
            listeners = new ParticipantStatusListener[participantStatusListeners.size()];
            participantStatusListeners.toArray(listeners);
        }
        try {
            // Get the method to execute based on the requested methodName and parameter
            Class<?>[] classes = new Class[params.size()];
            for (int i=0;i<params.size(); i++) {
                classes[i] = String.class;
            }
            Method method = ParticipantStatusListener.class.getDeclaredMethod(methodName, classes);
            for (ParticipantStatusListener listener : listeners) {
                method.invoke(listener, params.toArray());
            }
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private void init() {
        // Create filters
        messageFilter =
            new AndFilter(
                new FromMatchesFilter(room),
                new MessageTypeFilter(Message.Type.groupchat));
        messageFilter = new AndFilter(messageFilter, new PacketFilter() {
            public boolean accept(Packet packet) {
                Message msg = (Message) packet;
                return msg.getBody() != null;
            }
        });
        presenceFilter =
            new AndFilter(new FromMatchesFilter(room), new PacketTypeFilter(Presence.class));

        // Create a collector for incoming messages.
        messageCollector = new ConnectionDetachedPacketCollector();

        // Create a listener for subject updates.
        PacketListener subjectListener = new PacketListener() {
            public void processPacket(Packet packet) {
                Message msg = (Message) packet;
                // Update the room subject
                subject = msg.getSubject();
                // Fire event for subject updated listeners
                fireSubjectUpdatedListeners(
                    msg.getSubject(),
                    msg.getFrom());

            }
        };

        // Create a listener for all presence updates.
        PacketListener presenceListener = new PacketListener() {
            public void processPacket(Packet packet) {
                Presence presence = (Presence) packet;
                String from = presence.getFrom();
                String myRoomJID = room + "/" + nickname;
                boolean isUserStatusModification = presence.getFrom().equals(myRoomJID);
                if (presence.getType() == Presence.Type.available) {
                    Presence oldPresence = occupantsMap.put(from, presence);
                    if (oldPresence != null) {
                        // Get the previous occupant's affiliation & role
                        MUCUser mucExtension = getMUCUserExtension(oldPresence);
                        String oldAffiliation = mucExtension.getItem().getAffiliation();
                        String oldRole = mucExtension.getItem().getRole();
                        // Get the new occupant's affiliation & role
                        mucExtension = getMUCUserExtension(presence);
                        String newAffiliation = mucExtension.getItem().getAffiliation();
                        String newRole = mucExtension.getItem().getRole();
                        // Fire role modification events
                        checkRoleModifications(oldRole, newRole, isUserStatusModification, from);
                        // Fire affiliation modification events
                        checkAffiliationModifications(
                            oldAffiliation,
                            newAffiliation,
                            isUserStatusModification,
                            from);
                    }
                    else {
                        // A new occupant has joined the room
                        if (!isUserStatusModification) {
                            List<String> params = new ArrayList<String>();
                            params.add(from);
                            fireParticipantStatusListeners("joined", params);
                        }
                    }
                }
                else if (presence.getType() == Presence.Type.unavailable) {
                    occupantsMap.remove(from);
                    MUCUser mucUser = getMUCUserExtension(presence);
                    if (mucUser != null && mucUser.getStatus() != null) {
                        // Fire events according to the received presence code
                        checkPresenceCode(
                            mucUser.getStatus().getCode(),
                            presence.getFrom().equals(myRoomJID),
                            mucUser,
                            from);
                    } else {
                        // An occupant has left the room
                        if (!isUserStatusModification) {
                            List<String> params = new ArrayList<String>();
                            params.add(from);
                            fireParticipantStatusListeners("left", params);
                        }
                    }
                }
            }
        };

        // Listens for all messages that include a MUCUser extension and fire the invitation
        // rejection listeners if the message includes an invitation rejection.
        PacketListener declinesListener = new PacketListener() {
            public void processPacket(Packet packet) {
                // Get the MUC User extension
                MUCUser mucUser = getMUCUserExtension(packet);
                // Check if the MUCUser informs that the invitee has declined the invitation
                if (mucUser.getDecline() != null &&
                        ((Message) packet).getType() != Message.Type.error) {
                    // Fire event for invitation rejection listeners
                    fireInvitationRejectionListeners(
                        mucUser.getDecline().getFrom(),
                        mucUser.getDecline().getReason());
                }
            }
        };

        PacketMultiplexListener packetMultiplexor = new PacketMultiplexListener(
                messageCollector, presenceListener, subjectListener,
                declinesListener);

        roomListenerMultiplexor = RoomListenerMultiplexor.getRoomMultiplexor(connection);

        roomListenerMultiplexor.addRoom(room, packetMultiplexor);
    }

    /**
     * Fires notification events if the role of a room occupant has changed. If the occupant that
     * changed his role is your occupant then the <code>UserStatusListeners</code> added to this
     * <code>MultiUserChat</code> will be fired. On the other hand, if the occupant that changed
     * his role is not yours then the <code>ParticipantStatusListeners</code> added to this
     * <code>MultiUserChat</code> will be fired. The following table shows the events that will
     * be fired depending on the previous and new role of the occupant.
     *
     * <pre>
     * <table border="1">
     * <tr><td><b>Old</b></td><td><b>New</b></td><td><b>Events</b></td></tr>
     *
     * <tr><td>None</td><td>Visitor</td><td>--</td></tr>
     * <tr><td>Visitor</td><td>Participant</td><td>voiceGranted</td></tr>
     * <tr><td>Participant</td><td>Moderator</td><td>moderatorGranted</td></tr>
     *
     * <tr><td>None</td><td>Participant</td><td>voiceGranted</td></tr>
     * <tr><td>None</td><td>Moderator</td><td>voiceGranted + moderatorGranted</td></tr>
     * <tr><td>Visitor</td><td>Moderator</td><td>voiceGranted + moderatorGranted</td></tr>
     *
     * <tr><td>Moderator</td><td>Participant</td><td>moderatorRevoked</td></tr>
     * <tr><td>Participant</td><td>Visitor</td><td>voiceRevoked</td></tr>
     * <tr><td>Visitor</td><td>None</td><td>kicked</td></tr>
     *
     * <tr><td>Moderator</td><td>Visitor</td><td>voiceRevoked + moderatorRevoked</td></tr>
     * <tr><td>Moderator</td><td>None</td><td>kicked</td></tr>
     * <tr><td>Participant</td><td>None</td><td>kicked</td></tr>
     * </table>
     * </pre>
     *
     * @param oldRole the previous role of the user in the room before receiving the new presence
     * @param newRole the new role of the user in the room after receiving the new presence
     * @param isUserModification whether the received presence is about your user in the room or not
     * @param from the occupant whose role in the room has changed
     * (e.g. room@conference.jabber.org/nick).
     */
    private void checkRoleModifications(
        String oldRole,
        String newRole,
        boolean isUserModification,
        String from) {
        // Voice was granted to a visitor
        if (("visitor".equals(oldRole) || "none".equals(oldRole))
            && "participant".equals(newRole)) {
            if (isUserModification) {
                fireUserStatusListeners("voiceGranted", new Object[] {});
            }
            else {
                List<String> params = new ArrayList<String>();
                params.add(from);
                fireParticipantStatusListeners("voiceGranted", params);
            }
        }
        // The participant's voice was revoked from the room
        else if (
            "participant".equals(oldRole)
                && ("visitor".equals(newRole) || "none".equals(newRole))) {
            if (isUserModification) {
                fireUserStatusListeners("voiceRevoked", new Object[] {});
            }
            else {
                List<String> params = new ArrayList<String>();
                params.add(from);
                fireParticipantStatusListeners("voiceRevoked", params);
            }
        }
        // Moderator privileges were granted to a participant
        if (!"moderator".equals(oldRole) && "moderator".equals(newRole)) {
            if ("visitor".equals(oldRole) || "none".equals(oldRole)) {
                if (isUserModification) {
                    fireUserStatusListeners("voiceGranted", new Object[] {});
                }
                else {
                    List<String> params = new ArrayList<String>();
                    params.add(from);
                    fireParticipantStatusListeners("voiceGranted", params);
                }
            }
            if (isUserModification) {
                fireUserStatusListeners("moderatorGranted", new Object[] {});
            }
            else {
                List<String> params = new ArrayList<String>();
                params.add(from);
                fireParticipantStatusListeners("moderatorGranted", params);
            }
        }
        // Moderator privileges were revoked from a participant
        else if ("moderator".equals(oldRole) && !"moderator".equals(newRole)) {
            if ("visitor".equals(newRole) || "none".equals(newRole)) {
                if (isUserModification) {
                    fireUserStatusListeners("voiceRevoked", new Object[] {});
                }
                else {
                    List<String> params = new ArrayList<String>();
                    params.add(from);
                    fireParticipantStatusListeners("voiceRevoked", params);
                }
            }
            if (isUserModification) {
                fireUserStatusListeners("moderatorRevoked", new Object[] {});
            }
            else {
                List<String> params = new ArrayList<String>();
                params.add(from);
                fireParticipantStatusListeners("moderatorRevoked", params);
            }
        }
    }

    /**
     * Fires notification events if the affiliation of a room occupant has changed. If the
     * occupant that changed his affiliation is your occupant then the
     * <code>UserStatusListeners</code> added to this <code>MultiUserChat</code> will be fired.
     * On the other hand, if the occupant that changed his affiliation is not yours then the
     * <code>ParticipantStatusListeners</code> added to this <code>MultiUserChat</code> will be
     * fired. The following table shows the events that will be fired depending on the previous
     * and new affiliation of the occupant.
     *
     * <pre>
     * <table border="1">
     * <tr><td><b>Old</b></td><td><b>New</b></td><td><b>Events</b></td></tr>
     *
     * <tr><td>None</td><td>Member</td><td>membershipGranted</td></tr>
     * <tr><td>Member</td><td>Admin</td><td>membershipRevoked + adminGranted</td></tr>
     * <tr><td>Admin</td><td>Owner</td><td>adminRevoked + ownershipGranted</td></tr>
     *
     * <tr><td>None</td><td>Admin</td><td>adminGranted</td></tr>
     * <tr><td>None</td><td>Owner</td><td>ownershipGranted</td></tr>
     * <tr><td>Member</td><td>Owner</td><td>membershipRevoked + ownershipGranted</td></tr>
     *
     * <tr><td>Owner</td><td>Admin</td><td>ownershipRevoked + adminGranted</td></tr>
     * <tr><td>Admin</td><td>Member</td><td>adminRevoked + membershipGranted</td></tr>
     * <tr><td>Member</td><td>None</td><td>membershipRevoked</td></tr>
     *
     * <tr><td>Owner</td><td>Member</td><td>ownershipRevoked + membershipGranted</td></tr>
     * <tr><td>Owner</td><td>None</td><td>ownershipRevoked</td></tr>
     * <tr><td>Admin</td><td>None</td><td>adminRevoked</td></tr>
     * <tr><td><i>Anyone</i></td><td>Outcast</td><td>banned</td></tr>
     * </table>
     * </pre>
     *
     * @param oldAffiliation the previous affiliation of the user in the room before receiving the
     * new presence
     * @param newAffiliation the new affiliation of the user in the room after receiving the new
     * presence
     * @param isUserModification whether the received presence is about your user in the room or not
     * @param from the occupant whose role in the room has changed
     * (e.g. room@conference.jabber.org/nick).
     */
    private void checkAffiliationModifications(
        String oldAffiliation,
        String newAffiliation,
        boolean isUserModification,
        String from) {
        // First check for revoked affiliation and then for granted affiliations. The idea is to
        // first fire the "revoke" events and then fire the "grant" events.

        // The user's ownership to the room was revoked
        if ("owner".equals(oldAffiliation) && !"owner".equals(newAffiliation)) {
            if (isUserModification) {
                fireUserStatusListeners("ownershipRevoked", new Object[] {});
            }
            else {
                List<String> params = new ArrayList<String>();
                params.add(from);
                fireParticipantStatusListeners("ownershipRevoked", params);
            }
        }
        // The user's administrative privileges to the room were revoked
        else if ("admin".equals(oldAffiliation) && !"admin".equals(newAffiliation)) {
            if (isUserModification) {
                fireUserStatusListeners("adminRevoked", new Object[] {});
            }
            else {
                List<String> params = new ArrayList<String>();
                params.add(from);
                fireParticipantStatusListeners("adminRevoked", params);
            }
        }
        // The user's membership to the room was revoked
        else if ("member".equals(oldAffiliation) && !"member".equals(newAffiliation)) {
            if (isUserModification) {
                fireUserStatusListeners("membershipRevoked", new Object[] {});
            }
            else {
                List<String> params = new ArrayList<String>();
                params.add(from);
                fireParticipantStatusListeners("membershipRevoked", params);
            }
        }

        // The user was granted ownership to the room
        if (!"owner".equals(oldAffiliation) && "owner".equals(newAffiliation)) {
            if (isUserModification) {
                fireUserStatusListeners("ownershipGranted", new Object[] {});
            }
            else {
                List<String> params = new ArrayList<String>();
                params.add(from);
                fireParticipantStatusListeners("ownershipGranted", params);
            }
        }
        // The user was granted administrative privileges to the room
        else if (!"admin".equals(oldAffiliation) && "admin".equals(newAffiliation)) {
            if (isUserModification) {
                fireUserStatusListeners("adminGranted", new Object[] {});
            }
            else {
                List<String> params = new ArrayList<String>();
                params.add(from);
                fireParticipantStatusListeners("adminGranted", params);
            }
        }
        // The user was granted membership to the room
        else if (!"member".equals(oldAffiliation) && "member".equals(newAffiliation)) {
            if (isUserModification) {
                fireUserStatusListeners("membershipGranted", new Object[] {});
            }
            else {
                List<String> params = new ArrayList<String>();
                params.add(from);
                fireParticipantStatusListeners("membershipGranted", params);
            }
        }
    }

    /**
     * Fires events according to the received presence code.
     *
     * @param code
     * @param isUserModification
     * @param mucUser
     * @param from
     */
    private void checkPresenceCode(
        String code,
        boolean isUserModification,
        MUCUser mucUser,
        String from) {
        // Check if an occupant was kicked from the room
        if ("307".equals(code)) {
            // Check if this occupant was kicked
            if (isUserModification) {
                joined = false;

                fireUserStatusListeners(
                    "kicked",
                    new Object[] { mucUser.getItem().getActor(), mucUser.getItem().getReason()});

                // Reset occupant information.
                occupantsMap.clear();
                nickname = null;
                userHasLeft();
            }
            else {
                List<String> params = new ArrayList<String>();
                params.add(from);
                params.add(mucUser.getItem().getActor());
                params.add(mucUser.getItem().getReason());
                fireParticipantStatusListeners("kicked", params);
            }
        }
        // A user was banned from the room
        else if ("301".equals(code)) {
            // Check if this occupant was banned
            if (isUserModification) {
                joined = false;

                fireUserStatusListeners(
                    "banned",
                    new Object[] { mucUser.getItem().getActor(), mucUser.getItem().getReason()});

                // Reset occupant information.
                occupantsMap.clear();
                nickname = null;
                userHasLeft();
            }
            else {
                List<String> params = new ArrayList<String>();
                params.add(from);
                params.add(mucUser.getItem().getActor());
                params.add(mucUser.getItem().getReason());
                fireParticipantStatusListeners("banned", params);
            }
        }
        // A user's membership was revoked from the room
        else if ("321".equals(code)) {
            // Check if this occupant's membership was revoked
            if (isUserModification) {
                joined = false;

                fireUserStatusListeners("membershipRevoked", new Object[] {});

                // Reset occupant information.
                occupantsMap.clear();
                nickname = null;
                userHasLeft();
            }
        }
        // A occupant has changed his nickname in the room
        else if ("303".equals(code)) {
            List<String> params = new ArrayList<String>();
            params.add(from);
            params.add(mucUser.getItem().getNick());
            fireParticipantStatusListeners("nicknameChanged", params);
        }
    }

    private void cleanup() {
        try {
            if (connection != null) {
                roomListenerMultiplexor.removeRoom(room);
                // Remove all the PacketListeners added to the connection by this chat
                for (PacketListener connectionListener : connectionListeners) {
                    connection.removePacketListener(connectionListener);
                }
            }
        } catch (Exception e) {
            // Do nothing
        }
    }

    protected void finalize() throws Throwable {
        cleanup();
        super.finalize();
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
        private final static Map<Connection, WeakReference<InvitationsMonitor>> monitors =
                new WeakHashMap<Connection, WeakReference<InvitationsMonitor>>();

        private final List<InvitationListener> invitationsListeners =
                new ArrayList<InvitationListener>();
        private Connection connection;
        private PacketFilter invitationFilter;
        private PacketListener invitationPacketListener;

        /**
         * Returns a new or existing InvitationsMonitor for a given connection.
         *
         * @param conn the connection to monitor for room invitations.
         * @return a new or existing InvitationsMonitor for a given connection.
         */
        public static InvitationsMonitor getInvitationsMonitor(Connection conn) {
            synchronized (monitors) {
                if (!monitors.containsKey(conn)) {
                    // We need to use a WeakReference because the monitor references the
                    // connection and this could prevent the GC from collecting the monitor
                    // when no other object references the monitor
                    monitors.put(conn, new WeakReference<InvitationsMonitor>(new InvitationsMonitor(conn)));
                }
                // Return the InvitationsMonitor that monitors the connection
                return monitors.get(conn).get();
            }
        }

        /**
         * Creates a new InvitationsMonitor that will monitor invitations received
         * on a given connection.
         * 
         * @param connection the connection to monitor for possible room invitations
         */
        private InvitationsMonitor(Connection connection) {
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
        private void fireInvitationListeners(String room, String inviter, String reason, String password,
                                             Message message) {
            InvitationListener[] listeners;
            synchronized (invitationsListeners) {
                listeners = new InvitationListener[invitationsListeners.size()];
                invitationsListeners.toArray(listeners);
            }
            for (InvitationListener listener : listeners) {
                listener.invitationReceived(connection, room, inviter, reason, password, message);
            }
        }

        public void connectionClosed() {
            cancel();
        }

        public void connectionClosedOnError(Exception e) {
            // ignore              
        }

        public void reconnectingIn(int seconds) {
            // ignore
        }

        public void reconnectionSuccessful() {
            // ignore
        }

        public void reconnectionFailed(Exception e) {
            // ignore
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
                    if (mucUser.getInvite() != null &&
                            ((Message) packet).getType() != Message.Type.error) {
                        // Fire event for invitation listeners
                        fireInvitationListeners(packet.getFrom(), mucUser.getInvite().getFrom(),
                                mucUser.getInvite().getReason(), mucUser.getPassword(), (Message) packet);
                    }
                }
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
