/**
 *
 * Copyright © 2014-2017 Florian Schmaus
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
package org.jivesoftware.smackx.muc;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.AbstractConnectionListener;
import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPConnectionRegistry;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.MessageTypeFilter;
import org.jivesoftware.smack.filter.StanzaExtensionFilter;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.filter.NotFilter;
import org.jivesoftware.smack.filter.StanzaTypeFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.util.Async;
import org.jivesoftware.smackx.disco.AbstractNodeInformationProvider;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.disco.packet.DiscoverInfo;
import org.jivesoftware.smackx.disco.packet.DiscoverItems;
import org.jivesoftware.smackx.muc.MultiUserChatException.NotAMucServiceException;
import org.jivesoftware.smackx.muc.packet.MUCInitialPresence;
import org.jivesoftware.smackx.muc.packet.MUCUser;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.DomainBareJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.parts.Resourcepart;
import org.jxmpp.jid.EntityJid;

/**
 * A manager for Multi-User Chat rooms.
 * <p>
 * Use {@link #getMultiUserChat(EntityBareJid)} to retrieve an object representing a Multi-User Chat room.
 * </p>
 * <p>
 * <b>Automatic rejoin:</b> The manager supports automatic rejoin of MultiUserChat rooms once the connection got
 * re-established. This mechanism is disabled by default. To enable it, use {@link #setAutoJoinOnReconnect(boolean)}.
 * You can set a {@link AutoJoinFailedCallback} via {@link #setAutoJoinFailedCallback(AutoJoinFailedCallback)} to get
 * notified if this mechanism failed for some reason. Note that as soon as rejoining for a single room failed, no
 * further attempts will be made for the other rooms.
 * </p>
 * 
 * @see <a href="http://xmpp.org/extensions/xep-0045.html">XEP-0045: Multi-User Chat</a>
 */
public final class MultiUserChatManager extends Manager {
    private final static String DISCO_NODE = MUCInitialPresence.NAMESPACE + "#rooms";

    private static final Logger LOGGER = Logger.getLogger(MultiUserChatManager.class.getName());

    static {
        XMPPConnectionRegistry.addConnectionCreationListener(new ConnectionCreationListener() {
            @Override
            public void connectionCreated(final XMPPConnection connection) {
                // Set on every established connection that this client supports the Multi-User
                // Chat protocol. This information will be used when another client tries to
                // discover whether this client supports MUC or not.
                ServiceDiscoveryManager.getInstanceFor(connection).addFeature(MUCInitialPresence.NAMESPACE);

                // Set the NodeInformationProvider that will provide information about the
                // joined rooms whenever a disco request is received
                final WeakReference<XMPPConnection> weakRefConnection = new WeakReference<XMPPConnection>(connection);
                ServiceDiscoveryManager.getInstanceFor(connection).setNodeInformationProvider(DISCO_NODE,
                                new AbstractNodeInformationProvider() {
                                    @Override
                                    public List<DiscoverItems.Item> getNodeItems() {
                                        XMPPConnection connection = weakRefConnection.get();
                                        if (connection == null)
                                            return Collections.emptyList();
                                        Set<EntityBareJid> joinedRooms = MultiUserChatManager.getInstanceFor(connection).getJoinedRooms();
                                        List<DiscoverItems.Item> answer = new ArrayList<DiscoverItems.Item>();
                                        for (EntityBareJid room : joinedRooms) {
                                            answer.add(new DiscoverItems.Item(room));
                                        }
                                        return answer;
                                    }
                                });
            }
        });
    }

    private static final Map<XMPPConnection, MultiUserChatManager> INSTANCES = new WeakHashMap<XMPPConnection, MultiUserChatManager>();

    /**
     * Get a instance of a multi user chat manager for the given connection.
     * 
     * @param connection
     * @return a multi user chat manager.
     */
    public static synchronized MultiUserChatManager getInstanceFor(XMPPConnection connection) {
        MultiUserChatManager multiUserChatManager = INSTANCES.get(connection);
        if (multiUserChatManager == null) {
            multiUserChatManager = new MultiUserChatManager(connection);
            INSTANCES.put(connection, multiUserChatManager);
        }
        return multiUserChatManager;
    }

    private static final StanzaFilter INVITATION_FILTER = new AndFilter(StanzaTypeFilter.MESSAGE, new StanzaExtensionFilter(new MUCUser()),
                    new NotFilter(MessageTypeFilter.ERROR));

    private final Set<InvitationListener> invitationsListeners = new CopyOnWriteArraySet<InvitationListener>();
    private final Set<EntityBareJid> joinedRooms = new HashSet<>();

    /**
     * A Map of MUC JIDs to {@link MultiUserChat} instances. We use weak references for the values in order to allow
     * those instances to get garbage collected. Note that MultiUserChat instances can not get garbage collected while
     * the user is joined, because then the MUC will have PacketListeners added to the XMPPConnection.
     */
    private final Map<EntityBareJid, WeakReference<MultiUserChat>> multiUserChats = new HashMap<>();

    private boolean autoJoinOnReconnect;

    private AutoJoinFailedCallback autoJoinFailedCallback;

    private MultiUserChatManager(XMPPConnection connection) {
        super(connection);
        // Listens for all messages that include a MUCUser extension and fire the invitation
        // listeners if the message includes an invitation.
        StanzaListener invitationPacketListener = new StanzaListener() {
            @Override
            public void processStanza(Stanza packet) {
                final Message message = (Message) packet;
                // Get the MUCUser extension
                final MUCUser mucUser = MUCUser.from(message);
                // Check if the MUCUser extension includes an invitation
                if (mucUser.getInvite() != null) {
                    EntityBareJid mucJid = message.getFrom().asEntityBareJidIfPossible();
                    if (mucJid == null) {
                        LOGGER.warning("Invite to non bare JID: '" + message.toXML() + "'");
                        return;
                    }
                    // Fire event for invitation listeners
                    final MultiUserChat muc = getMultiUserChat(mucJid);
                    final XMPPConnection connection = connection();
                    final MUCUser.Invite invite = mucUser.getInvite();
                    final EntityJid from = invite.getFrom();
                    final String reason = invite.getReason();
                    final String password = mucUser.getPassword();
                    for (final InvitationListener listener : invitationsListeners) {
                        listener.invitationReceived(connection, muc, from, reason, password, message, invite);
                    }
                }
            }
        };
        connection.addAsyncStanzaListener(invitationPacketListener, INVITATION_FILTER);

        connection.addConnectionListener(new AbstractConnectionListener() {
            @Override
            public void authenticated(XMPPConnection connection, boolean resumed) {
                if (resumed) return;
                if (!autoJoinOnReconnect) return;

                final Set<EntityBareJid> mucs = getJoinedRooms();
                if (mucs.isEmpty()) return;

                Async.go(new Runnable() {
                    @Override
                    public void run() {
                        final AutoJoinFailedCallback failedCallback = autoJoinFailedCallback;
                        for (EntityBareJid mucJid : mucs) {
                            MultiUserChat muc = getMultiUserChat(mucJid);

                            if (!muc.isJoined()) return;

                            Resourcepart nickname = muc.getNickname();
                            if (nickname == null) return;

                            try {
                                muc.leave();
                            } catch (NotConnectedException | InterruptedException e) {
                                if (failedCallback != null) {
                                    failedCallback.autoJoinFailed(muc, e);
                                } else {
                                    LOGGER.log(Level.WARNING, "Could not leave room", e);
                                }
                                return;
                            }
                            try {
                                muc.join(nickname);
                            } catch (NotAMucServiceException | NoResponseException | XMPPErrorException
                                    | NotConnectedException | InterruptedException e) {
                                if (failedCallback != null) {
                                    failedCallback.autoJoinFailed(muc, e);
                                } else {
                                    LOGGER.log(Level.WARNING, "Could not leave room", e);
                                }
                                return;
                            }
                        }
                    }

                });
            }
        });
    }

    /**
     * Creates a multi user chat. Note: no information is sent to or received from the server until you attempt to
     * {@link MultiUserChat#join(org.jxmpp.jid.parts.Resourcepart) join} the chat room. On some server implementations, the room will not be
     * created until the first person joins it.
     * <p>
     * Most XMPP servers use a sub-domain for the chat service (eg chat.example.com for the XMPP server example.com).
     * You must ensure that the room address you're trying to connect to includes the proper chat sub-domain.
     * </p>
     *
     * @param jid the name of the room in the form "roomName@service", where "service" is the hostname at which the
     *        multi-user chat service is running. Make sure to provide a valid JID.
     */
    public synchronized MultiUserChat getMultiUserChat(EntityBareJid jid) {
        WeakReference<MultiUserChat> weakRefMultiUserChat = multiUserChats.get(jid);
        if (weakRefMultiUserChat == null) {
            return createNewMucAndAddToMap(jid);
        }
        MultiUserChat multiUserChat = weakRefMultiUserChat.get();
        if (multiUserChat == null) {
            return createNewMucAndAddToMap(jid);
        }
        return multiUserChat;
    }

    private MultiUserChat createNewMucAndAddToMap(EntityBareJid jid) {
        MultiUserChat multiUserChat = new MultiUserChat(connection(), jid, this);
        multiUserChats.put(jid, new WeakReference<MultiUserChat>(multiUserChat));
        return multiUserChat;
    }

    /**
     * Returns true if the specified user supports the Multi-User Chat protocol.
     *
     * @param user the user to check. A fully qualified xmpp ID, e.g. jdoe@example.com.
     * @return a boolean indicating whether the specified user supports the MUC protocol.
     * @throws XMPPErrorException
     * @throws NoResponseException
     * @throws NotConnectedException
     * @throws InterruptedException 
     */
    public boolean isServiceEnabled(Jid user) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        return ServiceDiscoveryManager.getInstanceFor(connection()).supportsFeature(user, MUCInitialPresence.NAMESPACE);
    }

    /**
     * Returns a Set of the rooms where the user has joined. The Iterator will contain Strings where each String
     * represents a room (e.g. room@muc.jabber.org).
     *
     * @return a List of the rooms where the user has joined using a given connection.
     */
    public Set<EntityBareJid> getJoinedRooms() {
        return Collections.unmodifiableSet(joinedRooms);
    }

    /**
     * Returns a List of the rooms where the requested user has joined. The Iterator will contain Strings where each
     * String represents a room (e.g. room@muc.jabber.org).
     *
     * @param user the user to check. A fully qualified xmpp ID, e.g. jdoe@example.com.
     * @return a List of the rooms where the requested user has joined.
     * @throws XMPPErrorException
     * @throws NoResponseException
     * @throws NotConnectedException
     * @throws InterruptedException 
     */
    public List<EntityBareJid> getJoinedRooms(EntityJid user) throws NoResponseException, XMPPErrorException,
                    NotConnectedException, InterruptedException {
        // Send the disco packet to the user
        DiscoverItems result = ServiceDiscoveryManager.getInstanceFor(connection()).discoverItems(user, DISCO_NODE);
        List<DiscoverItems.Item> items = result.getItems();
        List<EntityBareJid> answer = new ArrayList<>(items.size());
        // Collect the entityID for each returned item
        for (DiscoverItems.Item item : items) {
            EntityBareJid muc = item.getEntityID().asEntityBareJidIfPossible();
            if (muc == null) {
                LOGGER.warning("Not a bare JID: " + item.getEntityID());
                continue;
            }
            answer.add(muc);
        }
        return answer;
    }

    /**
     * Returns the discovered information of a given room without actually having to join the room. The server will
     * provide information only for rooms that are public.
     *
     * @param room the name of the room in the form "roomName@service" of which we want to discover its information.
     * @return the discovered information of a given room without actually having to join the room.
     * @throws XMPPErrorException
     * @throws NoResponseException
     * @throws NotConnectedException
     * @throws InterruptedException 
     */
    public RoomInfo getRoomInfo(EntityBareJid room) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        DiscoverInfo info = ServiceDiscoveryManager.getInstanceFor(connection()).discoverInfo(room);
        return new RoomInfo(info);
    }

    /**
     * Returns a collection with the XMPP addresses of the Multi-User Chat services.
     *
     * @return a collection with the XMPP addresses of the Multi-User Chat services.
     * @throws XMPPErrorException
     * @throws NoResponseException
     * @throws NotConnectedException
     * @throws InterruptedException 
     */
    public List<DomainBareJid> getXMPPServiceDomains() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        ServiceDiscoveryManager sdm = ServiceDiscoveryManager.getInstanceFor(connection());
        return sdm.findServices(MUCInitialPresence.NAMESPACE, false, false);
    }

    /**
     * Check if the provided domain bare JID provides a MUC service.
     * 
     * @param domainBareJid the domain bare JID to check.
     * @return <code>true</code> if the provided JID provides a MUC service, <code>false</code> otherwise.
     * @throws NoResponseException
     * @throws XMPPErrorException
     * @throws NotConnectedException
     * @throws InterruptedException
     * @see <a href="http://xmpp.org/extensions/xep-0045.html#disco-service-features">XEP-45 § 6.2 Discovering the Features Supported by a MUC Service</a>
     * @since 4.2
     */
    public boolean providesMucService(DomainBareJid domainBareJid) throws NoResponseException,
                    XMPPErrorException, NotConnectedException, InterruptedException {
        return ServiceDiscoveryManager.getInstanceFor(connection()).supportsFeature(domainBareJid,
                        MUCInitialPresence.NAMESPACE);
    }

    /**
     * Returns a List of HostedRooms where each HostedRoom has the XMPP address of the room and the room's name.
     * Once discovered the rooms hosted by a chat service it is possible to discover more detailed room information or
     * join the room.
     *
     * @param serviceName the service that is hosting the rooms to discover.
     * @return a collection of HostedRooms.
     * @throws XMPPErrorException
     * @throws NoResponseException
     * @throws NotConnectedException
     * @throws InterruptedException 
     * @throws NotAMucServiceException 
     */
    public List<HostedRoom> getHostedRooms(DomainBareJid serviceName) throws NoResponseException, XMPPErrorException,
                    NotConnectedException, InterruptedException, NotAMucServiceException {
        if (!providesMucService(serviceName)) {
            throw new NotAMucServiceException(serviceName);
        }
        ServiceDiscoveryManager discoManager = ServiceDiscoveryManager.getInstanceFor(connection());
        DiscoverItems discoverItems = discoManager.discoverItems(serviceName);
        List<DiscoverItems.Item> items = discoverItems.getItems();
        List<HostedRoom> answer = new ArrayList<HostedRoom>(items.size());
        for (DiscoverItems.Item item : items) {
            answer.add(new HostedRoom(item));
        }
        return answer;
    }

    /**
     * Informs the sender of an invitation that the invitee declines the invitation. The rejection will be sent to the
     * room which in turn will forward the rejection to the inviter.
     *
     * @param room the room that sent the original invitation.
     * @param inviter the inviter of the declined invitation.
     * @param reason the reason why the invitee is declining the invitation.
     * @throws NotConnectedException
     * @throws InterruptedException 
     */
    public void decline(EntityBareJid room, EntityBareJid inviter, String reason) throws NotConnectedException, InterruptedException {
        Message message = new Message(room);

        // Create the MUCUser packet that will include the rejection
        MUCUser mucUser = new MUCUser();
        MUCUser.Decline decline = new MUCUser.Decline(reason, inviter);
        mucUser.setDecline(decline);
        // Add the MUCUser packet that includes the rejection
        message.addExtension(mucUser);

        connection().sendStanza(message);
    }

    /**
     * Adds a listener to invitation notifications. The listener will be fired anytime an invitation is received.
     *
     * @param listener an invitation listener.
     */
    public void addInvitationListener(InvitationListener listener) {
        invitationsListeners.add(listener);
    }

    /**
     * Removes a listener to invitation notifications. The listener will be fired anytime an invitation is received.
     *
     * @param listener an invitation listener.
     */
    public void removeInvitationListener(InvitationListener listener) {
        invitationsListeners.remove(listener);
    }

    /**
     * If automatic join on reconnect is enabled, then the manager will try to auto join MUC rooms after the connection
     * got re-established.
     *
     * @param autoJoin <code>true</code> to enable, <code>false</code> to disable.
     */
    public void setAutoJoinOnReconnect(boolean autoJoin) {
        autoJoinOnReconnect = autoJoin;
    }

    /**
     * Set a callback invoked by this manager when automatic join on reconnect failed. If failedCallback is not
     * <code>null</code>,then automatic rejoin get also enabled.
     *
     * @param failedCallback the callback.
     */
    public void setAutoJoinFailedCallback(AutoJoinFailedCallback failedCallback) {
        autoJoinFailedCallback = failedCallback;
        if (failedCallback != null) {
            setAutoJoinOnReconnect(true);
        }
    }

    void addJoinedRoom(EntityBareJid room) {
        joinedRooms.add(room);
    }

    void removeJoinedRoom(EntityBareJid room) {
        joinedRooms.remove(room);
    }
}
