/**
 *
 * Copyright 2016 Fernando Ramirez
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
package org.jivesoftware.smackx.muclight;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.filter.IQReplyFilter;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.IQ.Type;

import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.disco.packet.DiscoverItems;
import org.jivesoftware.smackx.muclight.element.MUCLightBlockingIQ;

import org.jxmpp.jid.DomainBareJid;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.Jid;

/**
 * Multi-User Chat Light manager class.
 * 
 * @author Fernando Ramirez
 *
 */
public final class MultiUserChatLightManager extends Manager {

    private static final Map<XMPPConnection, MultiUserChatLightManager> INSTANCES = new WeakHashMap<XMPPConnection, MultiUserChatLightManager>();

    /**
     * Get a instance of a MUC Light manager for the given connection.
     *
     * @param connection
     * @return a MUCLight manager.
     */
    public static synchronized MultiUserChatLightManager getInstanceFor(XMPPConnection connection) {
        MultiUserChatLightManager multiUserChatLightManager = INSTANCES.get(connection);
        if (multiUserChatLightManager == null) {
            multiUserChatLightManager = new MultiUserChatLightManager(connection);
            INSTANCES.put(connection, multiUserChatLightManager);
        }
        return multiUserChatLightManager;
    }

    /**
     * A Map of MUC Light JIDs to instances. We use weak references for the
     * values in order to allow those instances to get garbage collected.
     */
    private final Map<EntityBareJid, WeakReference<MultiUserChatLight>> multiUserChatLights = new HashMap<>();

    private MultiUserChatLightManager(XMPPConnection connection) {
        super(connection);
    }

    /**
     * Obtain the MUC Light.
     *
     * @param jid
     * @return the MUCLight.
     */
    public synchronized MultiUserChatLight getMultiUserChatLight(EntityBareJid jid) {
        WeakReference<MultiUserChatLight> weakRefMultiUserChat = multiUserChatLights.get(jid);
        if (weakRefMultiUserChat == null) {
            return createNewMucLightAndAddToMap(jid);
        }
        MultiUserChatLight multiUserChatLight = weakRefMultiUserChat.get();
        if (multiUserChatLight == null) {
            return createNewMucLightAndAddToMap(jid);
        }
        return multiUserChatLight;
    }

    private MultiUserChatLight createNewMucLightAndAddToMap(EntityBareJid jid) {
        MultiUserChatLight multiUserChatLight = new MultiUserChatLight(connection(), jid);
        multiUserChatLights.put(jid, new WeakReference<MultiUserChatLight>(multiUserChatLight));
        return multiUserChatLight;
    }

    /**
     * Returns true if Multi-User Chat Light feature is supported by the server.
     *
     * @param mucLightService
     * @return true if Multi-User Chat Light feature is supported by the server.
     * @throws NotConnectedException
     * @throws XMPPErrorException
     * @throws NoResponseException
     * @throws InterruptedException
     */
    public boolean isFeatureSupported(DomainBareJid mucLightService)
            throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        return ServiceDiscoveryManager.getInstanceFor(connection()).discoverInfo(mucLightService)
                .containsFeature(MultiUserChatLight.NAMESPACE);
    }

    /**
     * Returns a List of the rooms the user occupies.
     *
     * @param mucLightService
     * @return a List of the rooms the user occupies.
     * @throws XMPPErrorException
     * @throws NoResponseException
     * @throws NotConnectedException
     * @throws InterruptedException
     */
    public List<Jid> getOccupiedRooms(DomainBareJid mucLightService)
            throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        DiscoverItems result = ServiceDiscoveryManager.getInstanceFor(connection()).discoverItems(mucLightService);
        List<DiscoverItems.Item> items = result.getItems();
        List<Jid> answer = new ArrayList<>(items.size());

        for (DiscoverItems.Item item : items) {
            Jid mucLight = item.getEntityID();
            answer.add(mucLight);
        }

        return answer;
    }

    /**
     * Returns a collection with the XMPP addresses of the MUC Light services.
     *
     * @return a collection with the XMPP addresses of the MUC Light services.
     * @throws XMPPErrorException
     * @throws NoResponseException
     * @throws NotConnectedException
     * @throws InterruptedException
     */
    public List<DomainBareJid> getLocalServices()
            throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        ServiceDiscoveryManager sdm = ServiceDiscoveryManager.getInstanceFor(connection());
        return sdm.findServices(MultiUserChatLight.NAMESPACE, false, false);
    }

    /**
     * Get users and rooms blocked.
     * 
     * @param mucLightService
     * @return the list of users and rooms blocked
     * @throws NoResponseException
     * @throws XMPPErrorException
     * @throws NotConnectedException
     * @throws InterruptedException
     */
    public List<Jid> getUsersAndRoomsBlocked(DomainBareJid mucLightService)
            throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        MUCLightBlockingIQ muclIghtBlockingIQResult = getBlockingList(mucLightService);

        List<Jid> jids = new ArrayList<>();
        if (muclIghtBlockingIQResult.getRooms() != null) {
            jids.addAll(muclIghtBlockingIQResult.getRooms().keySet());
        }

        if (muclIghtBlockingIQResult.getUsers() != null) {
            jids.addAll(muclIghtBlockingIQResult.getUsers().keySet());
        }

        return jids;
    }

    /**
     * Get rooms blocked.
     * 
     * @param mucLightService
     * @return the list of rooms blocked
     * @throws NoResponseException
     * @throws XMPPErrorException
     * @throws NotConnectedException
     * @throws InterruptedException
     */
    public List<Jid> getRoomsBlocked(DomainBareJid mucLightService)
            throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        MUCLightBlockingIQ muclIghtBlockingIQResult = getBlockingList(mucLightService);

        List<Jid> jids = new ArrayList<>();
        if (muclIghtBlockingIQResult.getRooms() != null) {
            jids.addAll(muclIghtBlockingIQResult.getRooms().keySet());
        }

        return jids;
    }

    /**
     * Get users blocked.
     * 
     * @param mucLightService
     * @return the list of users blocked
     * @throws NoResponseException
     * @throws XMPPErrorException
     * @throws NotConnectedException
     * @throws InterruptedException
     */
    public List<Jid> getUsersBlocked(DomainBareJid mucLightService)
            throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        MUCLightBlockingIQ muclIghtBlockingIQResult = getBlockingList(mucLightService);

        List<Jid> jids = new ArrayList<>();
        if (muclIghtBlockingIQResult.getUsers() != null) {
            jids.addAll(muclIghtBlockingIQResult.getUsers().keySet());
        }

        return jids;
    }

    private MUCLightBlockingIQ getBlockingList(DomainBareJid mucLightService)
            throws NoResponseException, XMPPErrorException, InterruptedException, NotConnectedException {
        MUCLightBlockingIQ mucLightBlockingIQ = new MUCLightBlockingIQ(null, null);
        mucLightBlockingIQ.setType(Type.get);
        mucLightBlockingIQ.setTo(mucLightService);

        StanzaFilter responseFilter = new IQReplyFilter(mucLightBlockingIQ, connection());
        IQ responseIq = connection().createStanzaCollectorAndSend(responseFilter, mucLightBlockingIQ)
                .nextResultOrThrow();
        MUCLightBlockingIQ muclIghtBlockingIQResult = (MUCLightBlockingIQ) responseIq;

        return muclIghtBlockingIQResult;
    }

    /**
     * Block a room.
     * 
     * @param mucLightService
     * @param roomJid
     * @throws NoResponseException
     * @throws XMPPErrorException
     * @throws NotConnectedException
     * @throws InterruptedException
     */
    public void blockRoom(DomainBareJid mucLightService, Jid roomJid)
            throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        HashMap<Jid, Boolean> rooms = new HashMap<>();
        rooms.put(roomJid, false);
        sendBlockRooms(mucLightService, rooms);
    }

    /**
     * Block rooms.
     * 
     * @param mucLightService
     * @param roomsJids
     * @throws NoResponseException
     * @throws XMPPErrorException
     * @throws NotConnectedException
     * @throws InterruptedException
     */
    public void blockRooms(DomainBareJid mucLightService, List<Jid> roomsJids)
            throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        HashMap<Jid, Boolean> rooms = new HashMap<>();
        for (Jid jid : roomsJids) {
            rooms.put(jid, false);
        }
        sendBlockRooms(mucLightService, rooms);
    }

    private void sendBlockRooms(DomainBareJid mucLightService, HashMap<Jid, Boolean> rooms)
            throws NoResponseException, XMPPErrorException, InterruptedException, NotConnectedException {
        MUCLightBlockingIQ mucLightBlockingIQ = new MUCLightBlockingIQ(rooms, null);
        mucLightBlockingIQ.setType(Type.set);
        mucLightBlockingIQ.setTo(mucLightService);
        connection().createStanzaCollectorAndSend(mucLightBlockingIQ).nextResultOrThrow();
    }

    /**
     * Block a user.
     * 
     * @param mucLightService
     * @param userJid
     * @throws NoResponseException
     * @throws XMPPErrorException
     * @throws NotConnectedException
     * @throws InterruptedException
     */
    public void blockUser(DomainBareJid mucLightService, Jid userJid)
            throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        HashMap<Jid, Boolean> users = new HashMap<>();
        users.put(userJid, false);
        sendBlockUsers(mucLightService, users);
    }

    /**
     * Block users.
     * 
     * @param mucLightService
     * @param usersJids
     * @throws NoResponseException
     * @throws XMPPErrorException
     * @throws NotConnectedException
     * @throws InterruptedException
     */
    public void blockUsers(DomainBareJid mucLightService, List<Jid> usersJids)
            throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        HashMap<Jid, Boolean> users = new HashMap<>();
        for (Jid jid : usersJids) {
            users.put(jid, false);
        }
        sendBlockUsers(mucLightService, users);
    }

    private void sendBlockUsers(DomainBareJid mucLightService, HashMap<Jid, Boolean> users)
            throws NoResponseException, XMPPErrorException, InterruptedException, NotConnectedException {
        MUCLightBlockingIQ mucLightBlockingIQ = new MUCLightBlockingIQ(null, users);
        mucLightBlockingIQ.setType(Type.set);
        mucLightBlockingIQ.setTo(mucLightService);
        connection().createStanzaCollectorAndSend(mucLightBlockingIQ).nextResultOrThrow();
    }

    /**
     * Unblock a room.
     * 
     * @param mucLightService
     * @param roomJid
     * @throws NoResponseException
     * @throws XMPPErrorException
     * @throws NotConnectedException
     * @throws InterruptedException
     */
    public void unblockRoom(DomainBareJid mucLightService, Jid roomJid)
            throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        HashMap<Jid, Boolean> rooms = new HashMap<>();
        rooms.put(roomJid, true);
        sendUnblockRooms(mucLightService, rooms);
    }

    /**
     * Unblock rooms.
     * 
     * @param mucLightService
     * @param roomsJids
     * @throws NoResponseException
     * @throws XMPPErrorException
     * @throws NotConnectedException
     * @throws InterruptedException
     */
    public void unblockRooms(DomainBareJid mucLightService, List<Jid> roomsJids)
            throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        HashMap<Jid, Boolean> rooms = new HashMap<>();
        for (Jid jid : roomsJids) {
            rooms.put(jid, true);
        }
        sendUnblockRooms(mucLightService, rooms);
    }

    private void sendUnblockRooms(DomainBareJid mucLightService, HashMap<Jid, Boolean> rooms)
            throws NoResponseException, XMPPErrorException, InterruptedException, NotConnectedException {
        MUCLightBlockingIQ mucLightBlockingIQ = new MUCLightBlockingIQ(rooms, null);
        mucLightBlockingIQ.setType(Type.set);
        mucLightBlockingIQ.setTo(mucLightService);
        connection().createStanzaCollectorAndSend(mucLightBlockingIQ).nextResultOrThrow();
    }

    /**
     * Unblock a user.
     * 
     * @param mucLightService
     * @param userJid
     * @throws NoResponseException
     * @throws XMPPErrorException
     * @throws NotConnectedException
     * @throws InterruptedException
     */
    public void unblockUser(DomainBareJid mucLightService, Jid userJid)
            throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        HashMap<Jid, Boolean> users = new HashMap<>();
        users.put(userJid, true);
        sendUnblockUsers(mucLightService, users);
    }

    /**
     * Unblock users.
     * 
     * @param mucLightService
     * @param usersJids
     * @throws NoResponseException
     * @throws XMPPErrorException
     * @throws NotConnectedException
     * @throws InterruptedException
     */
    public void unblockUsers(DomainBareJid mucLightService, List<Jid> usersJids)
            throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        HashMap<Jid, Boolean> users = new HashMap<>();
        for (Jid jid : usersJids) {
            users.put(jid, true);
        }
        sendUnblockUsers(mucLightService, users);
    }

    private void sendUnblockUsers(DomainBareJid mucLightService, HashMap<Jid, Boolean> users)
            throws NoResponseException, XMPPErrorException, InterruptedException, NotConnectedException {
        MUCLightBlockingIQ mucLightBlockingIQ = new MUCLightBlockingIQ(null, users);
        mucLightBlockingIQ.setType(Type.set);
        mucLightBlockingIQ.setTo(mucLightService);
        connection().createStanzaCollectorAndSend(mucLightBlockingIQ).nextResultOrThrow();
    }

}
