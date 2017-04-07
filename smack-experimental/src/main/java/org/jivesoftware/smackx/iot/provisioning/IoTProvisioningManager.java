/**
 *
 * Copyright 2016 Florian Schmaus
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
package org.jivesoftware.smackx.iot.provisioning;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPConnectionRegistry;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.StanzaExtensionFilter;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.filter.StanzaTypeFilter;
import org.jivesoftware.smack.iqrequest.AbstractIqRequestHandler;
import org.jivesoftware.smack.iqrequest.IQRequestHandler.Mode;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.IQ.Type;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.roster.AbstractPresenceEventListener;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.SubscribeListener;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.disco.packet.DiscoverInfo;
import org.jivesoftware.smackx.iot.IoTManager;
import org.jivesoftware.smackx.iot.discovery.IoTDiscoveryManager;
import org.jivesoftware.smackx.iot.provisioning.element.ClearCache;
import org.jivesoftware.smackx.iot.provisioning.element.ClearCacheResponse;
import org.jivesoftware.smackx.iot.provisioning.element.Constants;
import org.jivesoftware.smackx.iot.provisioning.element.Friend;
import org.jivesoftware.smackx.iot.provisioning.element.IoTIsFriend;
import org.jivesoftware.smackx.iot.provisioning.element.IoTIsFriendResponse;
import org.jivesoftware.smackx.iot.provisioning.element.Unfriend;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.DomainBareJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.util.cache.LruCache;

/**
 * A manager for XEP-0324: Internet of Things - Provisioning.
 *
 * @author Florian Schmaus {@literal <flo@geekplace.eu>}
 * @see <a href="http://xmpp.org/extensions/xep-0324.html">XEP-0324: Internet of Things - Provisioning</a>
 */
public final class IoTProvisioningManager extends Manager {

    private static final Logger LOGGER = Logger.getLogger(IoTProvisioningManager.class.getName());

    private static final StanzaFilter FRIEND_MESSAGE = new AndFilter(StanzaTypeFilter.MESSAGE,
            new StanzaExtensionFilter(Friend.ELEMENT, Friend.NAMESPACE));
    private static final StanzaFilter UNFRIEND_MESSAGE = new AndFilter(StanzaTypeFilter.MESSAGE,
                    new StanzaExtensionFilter(Unfriend.ELEMENT, Unfriend.NAMESPACE));

    private static final Map<XMPPConnection, IoTProvisioningManager> INSTANCES = new WeakHashMap<>();

    // Ensure a IoTProvisioningManager exists for every connection.
    static {
        XMPPConnectionRegistry.addConnectionCreationListener(new ConnectionCreationListener() {
            @Override
            public void connectionCreated(XMPPConnection connection) {
                if (!IoTManager.isAutoEnableActive()) return;
                getInstanceFor(connection);
            }
        });
    }

    /**
     * Get the manger instance responsible for the given connection.
     *
     * @param connection the XMPP connection.
     * @return a manager instance.
     */
    public static synchronized IoTProvisioningManager getInstanceFor(XMPPConnection connection) {
        IoTProvisioningManager manager = INSTANCES.get(connection);
        if (manager == null) {
            manager = new IoTProvisioningManager(connection);
            INSTANCES.put(connection, manager);
        }
        return manager;
    }

    private final Roster roster;
    private final LruCache<Jid, LruCache<BareJid, Void>> negativeFriendshipRequestCache = new LruCache<>(8);
    private final LruCache<BareJid, Void> friendshipDeniedCache = new LruCache<>(16);

    private final LruCache<BareJid, Void> friendshipRequestedCache = new LruCache<>(16);

    private final Set<BecameFriendListener> becameFriendListeners = new CopyOnWriteArraySet<>();

    private final Set<WasUnfriendedListener> wasUnfriendedListeners = new CopyOnWriteArraySet<>();

    private Jid configuredProvisioningServer;

    private IoTProvisioningManager(XMPPConnection connection) {
        super(connection);

        // Stanza listener for XEP-0324 § 3.2.3.
        connection.addAsyncStanzaListener(new StanzaListener() {
            @Override
            public void processStanza(Stanza stanza) throws NotConnectedException, InterruptedException {
                if (!isFromProvisioningService(stanza, true)) {
                    return;
                }

                Message message = (Message) stanza;
                Unfriend unfriend = Unfriend.from(message);
                BareJid unfriendJid = unfriend.getJid();
                final XMPPConnection connection = connection();
                Roster roster = Roster.getInstanceFor(connection);
                if (!roster.isSubscribedToMyPresence(unfriendJid)) {
                    LOGGER.warning("Ignoring <unfriend/> request '" + stanza + "' because " + unfriendJid
                                    + " is already not subscribed to our presence.");
                    return;
                }
                Presence unsubscribed = new Presence(Presence.Type.unsubscribed);
                unsubscribed.setTo(unfriendJid);
                connection.sendStanza(unsubscribed);
            }
        }, UNFRIEND_MESSAGE);

        // Stanza listener for XEP-0324 § 3.2.4 "Recommending Friendships".
        // Also includes business logic for thing-to-thing friendship recommendations, which is not
        // (yet) part of the XEP.
        connection.addAsyncStanzaListener(new StanzaListener() {
            @Override
            public void processStanza(final Stanza stanza) throws NotConnectedException, InterruptedException {
                final Message friendMessage = (Message) stanza;
                final Friend friend = Friend.from(friendMessage);
                final BareJid friendJid = friend.getFriend();

                if (isFromProvisioningService(friendMessage, false)) {
                    // We received a recommendation from a provisioning server.
                    // Notify the recommended friend that we will now accept his
                    // friendship requests.
                    final XMPPConnection connection = connection();
                    Friend friendNotifiacation = new Friend(connection.getUser().asBareJid());
                    Message notificationMessage = new Message(friendJid, friendNotifiacation);
                    connection.sendStanza(notificationMessage);
                } else {
                    // Check is the message was send from a thing we previously
                    // tried to become friends with. If this is the case, then
                    // thing is likely telling us that we can become now
                    // friends.
                    BareJid bareFrom = friendMessage.getFrom().asBareJid();
                    if (!friendshipDeniedCache.containsKey(bareFrom)) {
                        LOGGER.log(Level.WARNING, "Ignoring friendship recommendation "
                                        + friendMessage
                                        + " because friendship to this JID was not previously denied.");
                        return;
                    }

                    // Sanity check: If a thing recommends us itself as friend,
                    // which should be the case once we reach this code, then
                    // the bare 'from' JID should be equals to the JID of the
                    // recommended friend.
                    if (!bareFrom.equals(friendJid)) {
                        LOGGER.log(Level.WARNING,
                                        "Ignoring friendship recommendation " + friendMessage
                                                        + " because it does not recommend itself, but "
                                                        + friendJid + '.');
                        return;
                    }

                    // Re-try the friendship request.
                    sendFriendshipRequest(friendJid);
                }
            }
        }, FRIEND_MESSAGE);

        connection.registerIQRequestHandler(
                        new AbstractIqRequestHandler(ClearCache.ELEMENT, ClearCache.NAMESPACE, Type.set, Mode.async) {
                            @Override
                            public IQ handleIQRequest(IQ iqRequest) {
                                if (!isFromProvisioningService(iqRequest, true)) {
                                    return null;
                                }

                                ClearCache clearCache = (ClearCache) iqRequest;

                                // Handle <clearCache/> request.
                                Jid from = iqRequest.getFrom();
                                LruCache<BareJid, Void> cache = negativeFriendshipRequestCache.lookup(from);
                                if (cache != null) {
                                    cache.clear();
                                }

                                return new ClearCacheResponse(clearCache);
                            }
                        });

        roster = Roster.getInstanceFor(connection);
        roster.addSubscribeListener(new SubscribeListener() {
            @Override
            public SubscribeAnswer processSubscribe(Jid from, Presence subscribeRequest) {
                // First check if the subscription request comes from a known registry and accept the request if so.
                try {
                    if (IoTDiscoveryManager.getInstanceFor(connection()).isRegistry(from.asBareJid())) {
                        return SubscribeAnswer.Approve;
                    }
                }
                catch (NoResponseException | XMPPErrorException | NotConnectedException | InterruptedException e) {
                    LOGGER.log(Level.WARNING, "Could not determine if " + from + " is a registry", e);
                }

                Jid provisioningServer = null;
                try {
                    provisioningServer = getConfiguredProvisioningServer();
                }
                catch (NoResponseException | XMPPErrorException | NotConnectedException | InterruptedException e) {
                    LOGGER.log(Level.WARNING,
                                    "Could not determine privisioning server. Ignoring friend request from " + from, e);
                }
                if (provisioningServer == null) {
                    return null;
                }

                boolean isFriend;
                try {
                    isFriend = isFriend(provisioningServer, from.asBareJid());
                }
                catch (NoResponseException | XMPPErrorException | NotConnectedException | InterruptedException e) {
                    LOGGER.log(Level.WARNING, "Could not determine if " + from + " is a friend.", e);
                    return null;
                }

                if (isFriend) {
                    return SubscribeAnswer.Approve;
                }
                else {
                    return SubscribeAnswer.Deny;
                }
            }
        });

        roster.addPresenceEventListener(new AbstractPresenceEventListener() {
            @Override
            public void presenceSubscribed(BareJid address, Presence subscribedPresence) {
                friendshipRequestedCache.remove(address);
                for (BecameFriendListener becameFriendListener : becameFriendListeners) {
                    becameFriendListener.becameFriend(address, subscribedPresence);
                }
            }
            @Override
            public void presenceUnsubscribed(BareJid address, Presence unsubscribedPresence) {
                if (friendshipRequestedCache.containsKey(address)) {
                    friendshipDeniedCache.put(address, null);
                }
                for (WasUnfriendedListener wasUnfriendedListener : wasUnfriendedListeners) {
                    wasUnfriendedListener.wasUnfriendedListener(address, unsubscribedPresence);
                }
            }
        });
    }

    /**
     * Set the configured provisioning server. Use <code>null</code> as provisioningServer to use
     * automatic discovery of the provisioning server (the default behavior).
     * 
     * @param provisioningServer
     */
    public void setConfiguredProvisioningServer(Jid provisioningServer) {
        this.configuredProvisioningServer = provisioningServer;
    }

    public Jid getConfiguredProvisioningServer()
                    throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        if (configuredProvisioningServer == null) {
            configuredProvisioningServer = findProvisioningServerComponent();
        }
        return configuredProvisioningServer;
    }

    /**
     * Try to find a provisioning server component.
     * 
     * @return the XMPP address of the provisioning server component if one was found.
     * @throws NoResponseException
     * @throws XMPPErrorException
     * @throws NotConnectedException
     * @throws InterruptedException
     * @see <a href="http://xmpp.org/extensions/xep-0324.html#servercomponent">XEP-0324 § 3.1.2 Provisioning Server as a server component</a>
     */
    public DomainBareJid findProvisioningServerComponent() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        final XMPPConnection connection = connection();
        ServiceDiscoveryManager sdm = ServiceDiscoveryManager.getInstanceFor(connection);
        List<DiscoverInfo> discoverInfos = sdm.findServicesDiscoverInfo(Constants.IOT_PROVISIONING_NAMESPACE, true, true);
        if (discoverInfos.isEmpty()) {
            return null;
        }
        Jid jid = discoverInfos.get(0).getFrom();
        assert (jid.isDomainBareJid());
        return jid.asDomainBareJid();
    }

    /**
     * As the given provisioning server is the given JID is a friend.
     *
     * @param provisioningServer the provisioning server to ask.
     * @param friendInQuestion the JID to ask about.
     * @return <code>true</code> if the JID is a friend, <code>false</code> otherwise.
     * @throws NoResponseException
     * @throws XMPPErrorException
     * @throws NotConnectedException
     * @throws InterruptedException
     */
    public boolean isFriend(Jid provisioningServer, BareJid friendInQuestion) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        LruCache<BareJid, Void> cache = negativeFriendshipRequestCache.lookup(provisioningServer);
        if (cache != null && cache.containsKey(friendInQuestion)) {
            // We hit a cached negative isFriend response for this provisioning server.
            return false;
        }

        IoTIsFriend iotIsFriend = new IoTIsFriend(friendInQuestion);
        iotIsFriend.setTo(provisioningServer);
        IoTIsFriendResponse response = connection().createStanzaCollectorAndSend(iotIsFriend).nextResultOrThrow();
        assert (response.getJid().equals(friendInQuestion));
        boolean isFriend = response.getIsFriendResult();
        if (!isFriend) {
            // Cache the negative is friend response.
            if (cache == null) {
                cache = new LruCache<>(1024);
                negativeFriendshipRequestCache.put(provisioningServer, cache);
            }
            cache.put(friendInQuestion, null);
        }
        return isFriend;
    }

    public boolean iAmFriendOf(BareJid otherJid) {
        return roster.iAmSubscribedTo(otherJid);
    }

    public void sendFriendshipRequest(BareJid bareJid) throws NotConnectedException, InterruptedException {
        Presence presence = new Presence(Presence.Type.subscribe);
        presence.setTo(bareJid);

        friendshipRequestedCache.put(bareJid, null);

        connection().sendStanza(presence);
    }

    public void sendFriendshipRequestIfRequired(BareJid jid) throws NotConnectedException, InterruptedException {
        if (iAmFriendOf(jid)) return;

        sendFriendshipRequest(jid);
    }

    public boolean isMyFriend(Jid friendInQuestion) {
        return roster.isSubscribedToMyPresence(friendInQuestion);
    }

    public void unfriend(Jid friend) throws NotConnectedException, InterruptedException {
        if (isMyFriend(friend)) {
            Presence presence = new Presence(Presence.Type.unsubscribed);
            presence.setTo(friend);
            connection().sendStanza(presence);
        }
    }

    public boolean addBecameFriendListener(BecameFriendListener becameFriendListener) {
        return becameFriendListeners.add(becameFriendListener);
    }

    public boolean removeBecameFriendListener(BecameFriendListener becameFriendListener) {
        return becameFriendListeners.remove(becameFriendListener);
    }

    public boolean addWasUnfriendedListener(WasUnfriendedListener wasUnfriendedListener) {
        return wasUnfriendedListeners.add(wasUnfriendedListener);
    }

    public boolean removeWasUnfriendedListener(WasUnfriendedListener wasUnfriendedListener) {
        return wasUnfriendedListeners.remove(wasUnfriendedListener);
    }

    private boolean isFromProvisioningService(Stanza stanza, boolean log) {
        Jid provisioningServer;
        try {
            provisioningServer = getConfiguredProvisioningServer();
        }
        catch (NotConnectedException | InterruptedException | NoResponseException | XMPPErrorException e) {
            LOGGER.log(Level.WARNING, "Could determine provisioning server", e);
            return false;
        }
        if (provisioningServer == null) {
            if (log) {
                LOGGER.warning("Ignoring request '" + stanza
                                + "' because no provisioning server configured.");
            }
            return false;
        }
        if (!provisioningServer.equals(stanza.getFrom())) {
            if (log) {
                LOGGER.warning("Ignoring  request '" + stanza
                                + "' because not from provising server '" + provisioningServer
                                + "'.");
            }
            return false;
        }
        return true;
    }
}
