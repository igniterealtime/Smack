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

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketIDFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smackx.entitycaps.EntityCapsManager;
import org.jivesoftware.smackx.packet.DiscoverInfo;
import org.jivesoftware.smackx.packet.DiscoverInfo.Identity;
import org.jivesoftware.smackx.packet.DiscoverItems;
import org.jivesoftware.smackx.packet.DataForm;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages discovery of services in XMPP entities. This class provides:
 * <ol>
 * <li>A registry of supported features in this XMPP entity.
 * <li>Automatic response when this XMPP entity is queried for information.
 * <li>Ability to discover items and information of remote XMPP entities.
 * <li>Ability to publish publicly available items.
 * </ol>  
 * 
 * @author Gaston Dombiak
 */
public class ServiceDiscoveryManager {

    private static final String DEFAULT_IDENTITY_NAME = "Smack";
    private static final String DEFAULT_IDENTITY_CATEGORY = "client";
    private static final String DEFAULT_IDENTITY_TYPE = "pc";

    private static List<DiscoverInfo.Identity> identities = new LinkedList<DiscoverInfo.Identity>();

    private EntityCapsManager capsManager;

    private static Map<Connection, ServiceDiscoveryManager> instances =
            new ConcurrentHashMap<Connection, ServiceDiscoveryManager>();

    private Connection connection;
    private final Set<String> features = new HashSet<String>();
    private DataForm extendedInfo = null;
    private Map<String, NodeInformationProvider> nodeInformationProviders =
            new ConcurrentHashMap<String, NodeInformationProvider>();

    // Create a new ServiceDiscoveryManager on every established connection
    static {
        Connection.addConnectionCreationListener(new ConnectionCreationListener() {
            public void connectionCreated(Connection connection) {
                new ServiceDiscoveryManager(connection);
            }
        });
        identities.add(new Identity(DEFAULT_IDENTITY_CATEGORY, DEFAULT_IDENTITY_NAME, DEFAULT_IDENTITY_TYPE));
    }

    /**
     * Creates a new ServiceDiscoveryManager for a given Connection. This means that the 
     * service manager will respond to any service discovery request that the connection may
     * receive. 
     * 
     * @param connection the connection to which a ServiceDiscoveryManager is going to be created.
     */
    public ServiceDiscoveryManager(Connection connection) {
        this.connection = connection;

        init();
    }

    /**
     * Returns the ServiceDiscoveryManager instance associated with a given Connection.
     * 
     * @param connection the connection used to look for the proper ServiceDiscoveryManager.
     * @return the ServiceDiscoveryManager associated with a given Connection.
     */
    public static ServiceDiscoveryManager getInstanceFor(Connection connection) {
        return instances.get(connection);
    }

    /**
     * Returns the name of the client that will be returned when asked for the client identity
     * in a disco request. The name could be any value you need to identity this client.
     * 
     * @return the name of the client that will be returned when asked for the client identity
     *          in a disco request.
     */
    public static String getIdentityName() {
        DiscoverInfo.Identity identity = identities.get(0);
        if (identity != null) {
            return identity.getName();
        } else {
            return null;
        }
    }

    /**
     * Sets the name of the client that will be returned when asked for the client identity
     * in a disco request. The name could be any value you need to identity this client.
     * 
     * @param name the name of the client that will be returned when asked for the client identity
     *          in a disco request.
     */
    public static void setIdentityName(String name) {
        DiscoverInfo.Identity identity = identities.remove(0);
        identity = new DiscoverInfo.Identity(DEFAULT_IDENTITY_CATEGORY, name, DEFAULT_IDENTITY_TYPE);
        identities.add(identity);
    }

    /**
     * Returns the type of client that will be returned when asked for the client identity in a 
     * disco request. The valid types are defined by the category client. Follow this link to learn 
     * the possible types: <a href="http://xmpp.org/registrar/disco-categories.html#client">Jabber::Registrar</a>.
     * 
     * @return the type of client that will be returned when asked for the client identity in a 
     *          disco request.
     */
    public static String getIdentityType() {
        DiscoverInfo.Identity identity = identities.get(0);
        if (identity != null) {
            return identity.getType();
        } else {
            return null;
        }
    }

    /**
     * Sets the type of client that will be returned when asked for the client identity in a 
     * disco request. The valid types are defined by the category client. Follow this link to learn 
     * the possible types: <a href="http://xmpp.org/registrar/disco-categories.html#client">Jabber::Registrar</a>.
     * 
     * @param type the type of client that will be returned when asked for the client identity in a 
     *          disco request.
     */
    public static void setIdentityType(String type) {
        DiscoverInfo.Identity identity = identities.get(0);
        if (identity != null) {
            identity.setType(type);
        } else {
            identity = new DiscoverInfo.Identity(DEFAULT_IDENTITY_CATEGORY, DEFAULT_IDENTITY_NAME, type);
            identities.add(identity);
        }
    }

    /**
     * Returns all identities of this client as unmodifiable Collection
     * 
     * @return
     */
    public static List<DiscoverInfo.Identity> getIdentities() {
        return Collections.unmodifiableList(identities);
    }

    /**
     * Initializes the packet listeners of the connection that will answer to any
     * service discovery request. 
     */
    private void init() {
        // Register the new instance and associate it with the connection 
        instances.put(connection, this);

        addFeature(DiscoverInfo.NAMESPACE);
        addFeature(DiscoverItems.NAMESPACE);

        // Add a listener to the connection that removes the registered instance when
        // the connection is closed
        connection.addConnectionListener(new ConnectionListener() {
            public void connectionClosed() {
                // Unregister this instance since the connection has been closed
                instances.remove(connection);
            }

            public void connectionClosedOnError(Exception e) {
                // ignore
            }

            public void reconnectionFailed(Exception e) {
                // ignore
            }

            public void reconnectingIn(int seconds) {
                // ignore
            }

            public void reconnectionSuccessful() {
                // ignore
            }
        });

        // Listen for disco#items requests and answer with an empty result        
        PacketFilter packetFilter = new PacketTypeFilter(DiscoverItems.class);
        PacketListener packetListener = new PacketListener() {
            public void processPacket(Packet packet) {
                DiscoverItems discoverItems = (DiscoverItems) packet;
                // Send back the items defined in the client if the request is of type GET
                if (discoverItems != null && discoverItems.getType() == IQ.Type.GET) {
                    DiscoverItems response = new DiscoverItems();
                    response.setType(IQ.Type.RESULT);
                    response.setTo(discoverItems.getFrom());
                    response.setPacketID(discoverItems.getPacketID());
                    response.setNode(discoverItems.getNode());

                    // Add the defined items related to the requested node. Look for 
                    // the NodeInformationProvider associated with the requested node.  
                    NodeInformationProvider nodeInformationProvider =
                            getNodeInformationProvider(discoverItems.getNode());
                    if (nodeInformationProvider != null) {
                        // Specified node was found, add node items
                        response.addItems(nodeInformationProvider.getNodeItems());
                        // Add packet extensions
                        response.addExtensions(nodeInformationProvider.getNodePacketExtensions());
                    } else if(discoverItems.getNode() != null) {
                        // Return <item-not-found/> error since client doesn't contain
                        // the specified node
                        response.setType(IQ.Type.ERROR);
                        response.setError(new XMPPError(XMPPError.Condition.item_not_found));
                    }
                    connection.sendPacket(response);
                }
            }
        };
        connection.addPacketListener(packetListener, packetFilter);

        // Listen for disco#info requests and answer the client's supported features 
        // To add a new feature as supported use the #addFeature message        
        packetFilter = new PacketTypeFilter(DiscoverInfo.class);
        packetListener = new PacketListener() {
            public void processPacket(Packet packet) {
                DiscoverInfo discoverInfo = (DiscoverInfo) packet;
                // Answer the client's supported features if the request is of the GET type
                if (discoverInfo != null && discoverInfo.getType() == IQ.Type.GET) {
                    DiscoverInfo response = new DiscoverInfo();
                    response.setType(IQ.Type.RESULT);
                    response.setTo(discoverInfo.getFrom());
                    response.setPacketID(discoverInfo.getPacketID());
                    response.setNode(discoverInfo.getNode());
                    // Add the client's identity and features only if "node" is null
                    // and if the request was not send to a node. If Entity Caps are
                    // enabled the client's identity and features are may also added
                    // if the right node is chosen
                    if (discoverInfo.getNode() == null) {
                        addDiscoverInfoTo(response);
                    }
                    else {
                        // Disco#info was sent to a node. Check if we have information of the
                        // specified node
                        NodeInformationProvider nodeInformationProvider =
                                getNodeInformationProvider(discoverInfo.getNode());
                        if (nodeInformationProvider != null) {
                            // Node was found. Add node features
                            response.addFeatures(nodeInformationProvider.getNodeFeatures());
                            // Add node identities
                            response.addIdentities(nodeInformationProvider.getNodeIdentities());
                            // Add packet extensions
                            response.addExtensions(nodeInformationProvider.getNodePacketExtensions());
                        }
                        else {
                            // Return <item-not-found/> error since specified node was not found
                            response.setType(IQ.Type.ERROR);
                            response.setError(new XMPPError(XMPPError.Condition.item_not_found));
                        }
                    }
                    connection.sendPacket(response);
                }
            }
        };
        connection.addPacketListener(packetListener, packetFilter);
    }

    /**
     * Add discover info response data.
     * 
     * @see <a href="http://xmpp.org/extensions/xep-0030.html#info-basic">XEP-30 Basic Protocol; Example 2</a>
     *
     * @param response the discover info response packet
     */
    public void addDiscoverInfoTo(DiscoverInfo response) {
        // First add the identities of the connection
        response.addIdentities(identities);

        // Add the registered features to the response
        synchronized (features) {
            for (Iterator<String> it = getFeatures(); it.hasNext();) {
                response.addFeature(it.next());
            }
            response.addExtension(extendedInfo);
        }
    }

    /**
     * Returns the NodeInformationProvider responsible for providing information 
     * (ie items) related to a given node or <tt>null</null> if none.<p>
     * 
     * In MUC, a node could be 'http://jabber.org/protocol/muc#rooms' which means that the
     * NodeInformationProvider will provide information about the rooms where the user has joined.
     * 
     * @param node the node that contains items associated with an entity not addressable as a JID.
     * @return the NodeInformationProvider responsible for providing information related 
     * to a given node.
     */
    private NodeInformationProvider getNodeInformationProvider(String node) {
        if (node == null) {
            return null;
        }
        return nodeInformationProviders.get(node);
    }

    /**
     * Sets the NodeInformationProvider responsible for providing information 
     * (ie items) related to a given node. Every time this client receives a disco request
     * regarding the items of a given node, the provider associated to that node will be the 
     * responsible for providing the requested information.<p>
     * 
     * In MUC, a node could be 'http://jabber.org/protocol/muc#rooms' which means that the
     * NodeInformationProvider will provide information about the rooms where the user has joined. 
     * 
     * @param node the node whose items will be provided by the NodeInformationProvider.
     * @param listener the NodeInformationProvider responsible for providing items related
     *      to the node.
     */
    public void setNodeInformationProvider(String node, NodeInformationProvider listener) {
        nodeInformationProviders.put(node, listener);
    }

    /**
     * Removes the NodeInformationProvider responsible for providing information 
     * (ie items) related to a given node. This means that no more information will be
     * available for the specified node.
     * 
     * In MUC, a node could be 'http://jabber.org/protocol/muc#rooms' which means that the
     * NodeInformationProvider will provide information about the rooms where the user has joined. 
     * 
     * @param node the node to remove the associated NodeInformationProvider.
     */
    public void removeNodeInformationProvider(String node) {
        nodeInformationProviders.remove(node);
    }

    /**
     * Returns the supported features by this XMPP entity.
     * 
     * @return an Iterator on the supported features by this XMPP entity.
     */
    public Iterator<String> getFeatures() {
        synchronized (features) {
            return Collections.unmodifiableList(new ArrayList<String>(features)).iterator();
        }
    }

    /**
     * Returns the supported features by this XMPP entity.
     * 
     * @return a copy of the List on the supported features by this XMPP entity.
     */
    public List<String> getFeaturesList() {
        synchronized (features) {
            return new LinkedList<String>(features);
        }
    }

    /**
     * Registers that a new feature is supported by this XMPP entity. When this client is 
     * queried for its information the registered features will be answered.<p>
     *
     * Since no packet is actually sent to the server it is safe to perform this operation
     * before logging to the server. In fact, you may want to configure the supported features
     * before logging to the server so that the information is already available if it is required
     * upon login.
     *
     * @param feature the feature to register as supported.
     */
    public void addFeature(String feature) {
        synchronized (features) {
            features.add(feature);
            renewEntityCapsVersion();
        }
    }

    /**
     * Removes the specified feature from the supported features by this XMPP entity.<p>
     *
     * Since no packet is actually sent to the server it is safe to perform this operation
     * before logging to the server.
     *
     * @param feature the feature to remove from the supported features.
     */
    public void removeFeature(String feature) {
        synchronized (features) {
            features.remove(feature);
            renewEntityCapsVersion();
        }
    }

    /**
     * Returns true if the specified feature is registered in the ServiceDiscoveryManager.
     *
     * @param feature the feature to look for.
     * @return a boolean indicating if the specified featured is registered or not.
     */
    public boolean includesFeature(String feature) {
        synchronized (features) {
            return features.contains(feature);
        }
    }

    /**
     * Registers extended discovery information of this XMPP entity. When this
     * client is queried for its information this data form will be returned as
     * specified by XEP-0128.
     * <p>
     *
     * Since no packet is actually sent to the server it is safe to perform this
     * operation before logging to the server. In fact, you may want to
     * configure the extended info before logging to the server so that the
     * information is already available if it is required upon login.
     *
     * @param info
     *            the data form that contains the extend service discovery
     *            information.
     */
    public void setExtendedInfo(DataForm info) {
      extendedInfo = info;
      renewEntityCapsVersion();
    }

    /**
     * Returns the data form that is set as extended information for this Service Discovery instance (XEP-0128)
     * 
     * @see <a href="http://xmpp.org/extensions/xep-0128.html">XEP-128: Service Discovery Extensions</a>
     * @return
     */
    public DataForm getExtendedInfo() {
        return extendedInfo;
    }

    /**
     * Returns the data form as List of PacketExtensions, or null if no data form is set.
     * This representation is needed by some classes (e.g. EntityCapsManager, NodeInformationProvider)
     * 
     * @return
     */
    public List<PacketExtension> getExtendedInfoAsList() {
        List<PacketExtension> res = null;
        if (extendedInfo != null) {
            res = new ArrayList<PacketExtension>(1);
            res.add(extendedInfo);
        }
        return res;
    }

    /**
     * Removes the data form containing extended service discovery information
     * from the information returned by this XMPP entity.<p>
     *
     * Since no packet is actually sent to the server it is safe to perform this
     * operation before logging to the server.
     */
    public void removeExtendedInfo() {
       extendedInfo = null;
       renewEntityCapsVersion();
    }

    /**
     * Returns the discovered information of a given XMPP entity addressed by its JID.
     * Use null as entityID to query the server
     * 
     * @param entityID the address of the XMPP entity or null.
     * @return the discovered information.
     * @throws XMPPException if the operation failed for some reason.
     */
    public DiscoverInfo discoverInfo(String entityID) throws XMPPException {
        if (entityID == null)
            return discoverInfo(null, null);

        // Check if the have it cached in the Entity Capabilities Manager
        DiscoverInfo info = EntityCapsManager.getDiscoverInfoByUser(entityID);

        if (info != null) {
            // We were able to retrieve the information from Entity Caps and
            // avoided a disco request, hurray!
            return info;
        }

        // Try to get the newest node#version if it's known, otherwise null is
        // returned
        EntityCapsManager.NodeVerHash nvh = EntityCapsManager.getNodeVerHashByJid(entityID);

        // Discover by requesting the information from the remote entity
        // Note that wee need to use NodeVer as argument for Node if it exists
        info = discoverInfo(entityID, nvh != null ? nvh.getNodeVer() : null);

        // If the node version is known, store the new entry.
        if (nvh != null) {
            if (EntityCapsManager.verifyDiscvoerInfoVersion(nvh.getVer(), nvh.getHash(), info))
                EntityCapsManager.addDiscoverInfoByNode(nvh.getNodeVer(), info);
        }

        return info;
    }

    /**
     * Returns the discovered information of a given XMPP entity addressed by its JID and
     * note attribute. Use this message only when trying to query information which is not 
     * directly addressable.
     * 
     * @see <a href="http://xmpp.org/extensions/xep-0030.html#info-basic">XEP-30 Basic Protocol</a>
     * @see <a href="http://xmpp.org/extensions/xep-0030.html#info-nodes">XEP-30 Info Nodes</a>
     * 
     * @param entityID the address of the XMPP entity.
     * @param node the optional attribute that supplements the 'jid' attribute.
     * @return the discovered information.
     * @throws XMPPException if the operation failed for some reason.
     */
    public DiscoverInfo discoverInfo(String entityID, String node) throws XMPPException {
        // Discover the entity's info
        DiscoverInfo disco = new DiscoverInfo();
        disco.setType(IQ.Type.GET);
        disco.setTo(entityID);
        disco.setNode(node);

        // Create a packet collector to listen for a response.
        PacketCollector collector =
            connection.createPacketCollector(new PacketIDFilter(disco.getPacketID()));

        connection.sendPacket(disco);

        // Wait up to 5 seconds for a result.
        IQ result = (IQ) collector.nextResult(SmackConfiguration.getPacketReplyTimeout());
        // Stop queuing results
        collector.cancel();
        if (result == null) {
            throw new XMPPException("No response from the server.");
        }
        if (result.getType() == IQ.Type.ERROR) {
            throw new XMPPException(result.getError());
        }
        return (DiscoverInfo) result;
    }

    /**
     * Returns the discovered items of a given XMPP entity addressed by its JID.
     * 
     * @param entityID the address of the XMPP entity.
     * @return the discovered information.
     * @throws XMPPException if the operation failed for some reason.
     */
    public DiscoverItems discoverItems(String entityID) throws XMPPException {
        return discoverItems(entityID, null);
    }

    /**
     * Returns the discovered items of a given XMPP entity addressed by its JID and
     * note attribute. Use this message only when trying to query information which is not 
     * directly addressable.
     * 
     * @param entityID the address of the XMPP entity.
     * @param node the optional attribute that supplements the 'jid' attribute.
     * @return the discovered items.
     * @throws XMPPException if the operation failed for some reason.
     */
    public DiscoverItems discoverItems(String entityID, String node) throws XMPPException {
        // Discover the entity's items
        DiscoverItems disco = new DiscoverItems();
        disco.setType(IQ.Type.GET);
        disco.setTo(entityID);
        disco.setNode(node);

        // Create a packet collector to listen for a response.
        PacketCollector collector =
            connection.createPacketCollector(new PacketIDFilter(disco.getPacketID()));

        connection.sendPacket(disco);

        // Wait up to 5 seconds for a result.
        IQ result = (IQ) collector.nextResult(SmackConfiguration.getPacketReplyTimeout());
        // Stop queuing results
        collector.cancel();
        if (result == null) {
            throw new XMPPException("No response from the server.");
        }
        if (result.getType() == IQ.Type.ERROR) {
            throw new XMPPException(result.getError());
        }
        return (DiscoverItems) result;
    }

    /**
     * Returns true if the server supports publishing of items. A client may wish to publish items
     * to the server so that the server can provide items associated to the client. These items will
     * be returned by the server whenever the server receives a disco request targeted to the bare
     * address of the client (i.e. user@host.com).
     * 
     * @param entityID the address of the XMPP entity.
     * @return true if the server supports publishing of items.
     * @throws XMPPException if the operation failed for some reason.
     */
    public boolean canPublishItems(String entityID) throws XMPPException {
        DiscoverInfo info = discoverInfo(entityID);
        return canPublishItems(info);
     }

     /**
      * Returns true if the server supports publishing of items. A client may wish to publish items
      * to the server so that the server can provide items associated to the client. These items will
      * be returned by the server whenever the server receives a disco request targeted to the bare
      * address of the client (i.e. user@host.com).
      * 
      * @param DiscoverInfo the discover info packet to check.
      * @return true if the server supports publishing of items.
      */
     public static boolean canPublishItems(DiscoverInfo info) {
         return info.containsFeature("http://jabber.org/protocol/disco#publish");
     }

    /**
     * Publishes new items to a parent entity. The item elements to publish MUST have at least 
     * a 'jid' attribute specifying the Entity ID of the item, and an action attribute which 
     * specifies the action being taken for that item. Possible action values are: "update" and 
     * "remove".
     * 
     * @param entityID the address of the XMPP entity.
     * @param discoverItems the DiscoveryItems to publish.
     * @throws XMPPException if the operation failed for some reason.
     */
    public void publishItems(String entityID, DiscoverItems discoverItems)
            throws XMPPException {
        publishItems(entityID, null, discoverItems);
    }

    /**
     * Publishes new items to a parent entity and node. The item elements to publish MUST have at 
     * least a 'jid' attribute specifying the Entity ID of the item, and an action attribute which 
     * specifies the action being taken for that item. Possible action values are: "update" and 
     * "remove".
     * 
     * @param entityID the address of the XMPP entity.
     * @param node the attribute that supplements the 'jid' attribute.
     * @param discoverItems the DiscoveryItems to publish.
     * @throws XMPPException if the operation failed for some reason.
     */
    public void publishItems(String entityID, String node, DiscoverItems discoverItems)
            throws XMPPException {
        discoverItems.setType(IQ.Type.SET);
        discoverItems.setTo(entityID);
        discoverItems.setNode(node);

        // Create a packet collector to listen for a response.
        PacketCollector collector =
            connection.createPacketCollector(new PacketIDFilter(discoverItems.getPacketID()));

        connection.sendPacket(discoverItems);

        // Wait up to 5 seconds for a result.
        IQ result = (IQ) collector.nextResult(SmackConfiguration.getPacketReplyTimeout());
        // Stop queuing results
        collector.cancel();
        if (result == null) {
            throw new XMPPException("No response from the server.");
        }
        if (result.getType() == IQ.Type.ERROR) {
            throw new XMPPException(result.getError());
        }
    }

    /**
     * Entity Capabilities
     */

    /**
     * Loads the ServiceDiscoveryManager with an EntityCapsManger
     * that speeds up certain lookups
     * @param manager
     */
    public void setEntityCapsManager(EntityCapsManager manager) {
        capsManager = manager;
    }

    /**
     * Updates the Entity Capabilities Verification String
     * if EntityCaps is enabled
     */
    private void renewEntityCapsVersion() {
        if (capsManager != null && capsManager.entityCapsEnabled())
            capsManager.updateLocalEntityCaps();
    }
}
