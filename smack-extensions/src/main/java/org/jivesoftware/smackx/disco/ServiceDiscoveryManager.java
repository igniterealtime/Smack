/**
 *
 * Copyright 2003-2007 Jive Software.
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
package org.jivesoftware.smackx.disco;

import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.XMPPConnectionRegistry;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.iqrequest.AbstractIqRequestHandler;
import org.jivesoftware.smack.iqrequest.IQRequestHandler.Mode;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smack.util.Objects;
import org.jivesoftware.smackx.caps.EntityCapsManager;
import org.jivesoftware.smackx.disco.packet.DiscoverInfo;
import org.jivesoftware.smackx.disco.packet.DiscoverItems;
import org.jivesoftware.smackx.disco.packet.DiscoverInfo.Identity;
import org.jivesoftware.smackx.xdata.packet.DataForm;
import org.jxmpp.jid.DomainBareJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.util.cache.Cache;
import org.jxmpp.util.cache.ExpirationCache;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

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
public final class ServiceDiscoveryManager extends Manager {

    private static final Logger LOGGER = Logger.getLogger(ServiceDiscoveryManager.class.getName());

    private static final String DEFAULT_IDENTITY_NAME = "Smack";
    private static final String DEFAULT_IDENTITY_CATEGORY = "client";
    private static final String DEFAULT_IDENTITY_TYPE = "pc";

    private static DiscoverInfo.Identity defaultIdentity = new Identity(DEFAULT_IDENTITY_CATEGORY,
            DEFAULT_IDENTITY_NAME, DEFAULT_IDENTITY_TYPE);

    private Set<DiscoverInfo.Identity> identities = new HashSet<DiscoverInfo.Identity>();
    private DiscoverInfo.Identity identity = defaultIdentity;

    private EntityCapsManager capsManager;

    private static Map<XMPPConnection, ServiceDiscoveryManager> instances = new WeakHashMap<>();

    private final Set<String> features = new HashSet<String>();
    private DataForm extendedInfo = null;
    private Map<String, NodeInformationProvider> nodeInformationProviders =
            new ConcurrentHashMap<String, NodeInformationProvider>();

    // Create a new ServiceDiscoveryManager on every established connection
    static {
        XMPPConnectionRegistry.addConnectionCreationListener(new ConnectionCreationListener() {
            @Override
            public void connectionCreated(XMPPConnection connection) {
                getInstanceFor(connection);
            }
        });
    }

    /**
     * Set the default identity all new connections will have. If unchanged the default identity is an
     * identity where category is set to 'client', type is set to 'pc' and name is set to 'Smack'.
     * 
     * @param identity
     */
    public static void setDefaultIdentity(DiscoverInfo.Identity identity) {
        defaultIdentity = identity;
    }

    /**
     * Creates a new ServiceDiscoveryManager for a given XMPPConnection. This means that the 
     * service manager will respond to any service discovery request that the connection may
     * receive. 
     * 
     * @param connection the connection to which a ServiceDiscoveryManager is going to be created.
     */
    private ServiceDiscoveryManager(XMPPConnection connection) {
        super(connection);

        addFeature(DiscoverInfo.NAMESPACE);
        addFeature(DiscoverItems.NAMESPACE);

        // Listen for disco#items requests and answer with an empty result        
        connection.registerIQRequestHandler(new AbstractIqRequestHandler(DiscoverItems.ELEMENT, DiscoverItems.NAMESPACE, IQ.Type.get, Mode.async) {
            @Override
            public IQ handleIQRequest(IQ iqRequest) {
                DiscoverItems discoverItems = (DiscoverItems) iqRequest;
                DiscoverItems response = new DiscoverItems();
                response.setType(IQ.Type.result);
                response.setTo(discoverItems.getFrom());
                response.setStanzaId(discoverItems.getStanzaId());
                response.setNode(discoverItems.getNode());

                // Add the defined items related to the requested node. Look for
                // the NodeInformationProvider associated with the requested node.
                NodeInformationProvider nodeInformationProvider = getNodeInformationProvider(discoverItems.getNode());
                if (nodeInformationProvider != null) {
                    // Specified node was found, add node items
                    response.addItems(nodeInformationProvider.getNodeItems());
                    // Add packet extensions
                    response.addExtensions(nodeInformationProvider.getNodePacketExtensions());
                } else if (discoverItems.getNode() != null) {
                    // Return <item-not-found/> error since client doesn't contain
                    // the specified node
                    response.setType(IQ.Type.error);
                    response.setError(XMPPError.getBuilder(XMPPError.Condition.item_not_found));
                }
                return response;
            }
        });

        // Listen for disco#info requests and answer the client's supported features 
        // To add a new feature as supported use the #addFeature message        
        connection.registerIQRequestHandler(new AbstractIqRequestHandler(DiscoverInfo.ELEMENT, DiscoverInfo.NAMESPACE, IQ.Type.get, Mode.async) {
            @Override
            public IQ handleIQRequest(IQ iqRequest) {
                DiscoverInfo discoverInfo = (DiscoverInfo) iqRequest;
                // Answer the client's supported features if the request is of the GET type
                DiscoverInfo response = new DiscoverInfo();
                response.setType(IQ.Type.result);
                response.setTo(discoverInfo.getFrom());
                response.setStanzaId(discoverInfo.getStanzaId());
                response.setNode(discoverInfo.getNode());
                // Add the client's identity and features only if "node" is null
                // and if the request was not send to a node. If Entity Caps are
                // enabled the client's identity and features are may also added
                // if the right node is chosen
                if (discoverInfo.getNode() == null) {
                    addDiscoverInfoTo(response);
                } else {
                    // Disco#info was sent to a node. Check if we have information of the
                    // specified node
                    NodeInformationProvider nodeInformationProvider = getNodeInformationProvider(discoverInfo.getNode());
                    if (nodeInformationProvider != null) {
                        // Node was found. Add node features
                        response.addFeatures(nodeInformationProvider.getNodeFeatures());
                        // Add node identities
                        response.addIdentities(nodeInformationProvider.getNodeIdentities());
                        // Add packet extensions
                        response.addExtensions(nodeInformationProvider.getNodePacketExtensions());
                    } else {
                        // Return <item-not-found/> error since specified node was not found
                        response.setType(IQ.Type.error);
                        response.setError(XMPPError.getBuilder(XMPPError.Condition.item_not_found));
                    }
                }
                return response;
            }
        });
    }

    /**
     * Returns the name of the client that will be returned when asked for the client identity
     * in a disco request. The name could be any value you need to identity this client.
     * 
     * @return the name of the client that will be returned when asked for the client identity
     *          in a disco request.
     */
    public String getIdentityName() {
        return identity.getName();
    }

    /**
     * Sets the default identity the client will report.
     *
     * @param identity
     */
    public synchronized void setIdentity(Identity identity) {
        this.identity = Objects.requireNonNull(identity, "Identity can not be null");
        // Notify others of a state change of SDM. In order to keep the state consistent, this
        // method is synchronized
        renewEntityCapsVersion();
    }

    /**
     * Return the default identity of the client.
     *
     * @return the default identity.
     */
    public Identity getIdentity() {
        return identity;
    }

    /**
     * Returns the type of client that will be returned when asked for the client identity in a 
     * disco request. The valid types are defined by the category client. Follow this link to learn 
     * the possible types: <a href="http://xmpp.org/registrar/disco-categories.html#client">Jabber::Registrar</a>.
     * 
     * @return the type of client that will be returned when asked for the client identity in a 
     *          disco request.
     */
    public String getIdentityType() {
        return identity.getType();
    }

    /**
     * Add an further identity to the client.
     * 
     * @param identity
     */
    public synchronized void addIdentity(DiscoverInfo.Identity identity) {
        identities.add(identity);
        // Notify others of a state change of SDM. In order to keep the state consistent, this
        // method is synchronized
        renewEntityCapsVersion();
    }

    /**
     * Remove an identity from the client. Note that the client needs at least one identity, the default identity, which
     * can not be removed.
     * 
     * @param identity
     * @return true, if successful. Otherwise the default identity was given.
     */
    public synchronized boolean removeIdentity(DiscoverInfo.Identity identity) {
        if (identity.equals(this.identity)) return false;
        identities.remove(identity);
        // Notify others of a state change of SDM. In order to keep the state consistent, this
        // method is synchronized
        renewEntityCapsVersion();
        return true;
    }

    /**
     * Returns all identities of this client as unmodifiable Collection.
     * 
     * @return all identies as set
     */
    public Set<DiscoverInfo.Identity> getIdentities() {
        Set<Identity> res = new HashSet<Identity>(identities);
        // Add the default identity that must exist
        res.add(defaultIdentity);
        return Collections.unmodifiableSet(res);
    }

    /**
     * Returns the ServiceDiscoveryManager instance associated with a given XMPPConnection.
     * 
     * @param connection the connection used to look for the proper ServiceDiscoveryManager.
     * @return the ServiceDiscoveryManager associated with a given XMPPConnection.
     */
    public static synchronized ServiceDiscoveryManager getInstanceFor(XMPPConnection connection) {
        ServiceDiscoveryManager sdm = instances.get(connection);
        if (sdm == null) {
            sdm = new ServiceDiscoveryManager(connection);
            // Register the new instance and associate it with the connection
            instances.put(connection, sdm);
        }
        return sdm;
    }

    /**
     * Add discover info response data.
     * 
     * @see <a href="http://xmpp.org/extensions/xep-0030.html#info-basic">XEP-30 Basic Protocol; Example 2</a>
     *
     * @param response the discover info response packet
     */
    public synchronized void addDiscoverInfoTo(DiscoverInfo response) {
        // First add the identities of the connection
        response.addIdentities(getIdentities());

        // Add the registered features to the response
        for (String feature : getFeatures()) {
            response.addFeature(feature);
        }
        response.addExtension(extendedInfo);
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
     * <p>
     * The result is a copied modifiable list of the original features.
     * </p>
     * 
     * @return a List of the supported features by this XMPP entity.
     */
    public synchronized List<String> getFeatures() {
        return new ArrayList<String>(features);
    }

    /**
     * Registers that a new feature is supported by this XMPP entity. When this client is 
     * queried for its information the registered features will be answered.<p>
     *
     * Since no stanza(/packet) is actually sent to the server it is safe to perform this operation
     * before logging to the server. In fact, you may want to configure the supported features
     * before logging to the server so that the information is already available if it is required
     * upon login.
     *
     * @param feature the feature to register as supported.
     */
    public synchronized void addFeature(String feature) {
        features.add(feature);
        // Notify others of a state change of SDM. In order to keep the state consistent, this
        // method is synchronized
        renewEntityCapsVersion();
    }

    /**
     * Removes the specified feature from the supported features by this XMPP entity.<p>
     *
     * Since no stanza(/packet) is actually sent to the server it is safe to perform this operation
     * before logging to the server.
     *
     * @param feature the feature to remove from the supported features.
     */
    public synchronized void removeFeature(String feature) {
        features.remove(feature);
        // Notify others of a state change of SDM. In order to keep the state consistent, this
        // method is synchronized
        renewEntityCapsVersion();
    }

    /**
     * Returns true if the specified feature is registered in the ServiceDiscoveryManager.
     *
     * @param feature the feature to look for.
     * @return a boolean indicating if the specified featured is registered or not.
     */
    public synchronized boolean includesFeature(String feature) {
        return features.contains(feature);
    }

    /**
     * Registers extended discovery information of this XMPP entity. When this
     * client is queried for its information this data form will be returned as
     * specified by XEP-0128.
     * <p>
     *
     * Since no stanza(/packet) is actually sent to the server it is safe to perform this
     * operation before logging to the server. In fact, you may want to
     * configure the extended info before logging to the server so that the
     * information is already available if it is required upon login.
     *
     * @param info
     *            the data form that contains the extend service discovery
     *            information.
     */
    public synchronized void setExtendedInfo(DataForm info) {
      extendedInfo = info;
      // Notify others of a state change of SDM. In order to keep the state consistent, this
      // method is synchronized
      renewEntityCapsVersion();
    }

    /**
     * Returns the data form that is set as extended information for this Service Discovery instance (XEP-0128).
     * 
     * @see <a href="http://xmpp.org/extensions/xep-0128.html">XEP-128: Service Discovery Extensions</a>
     * @return the data form
     */
    public DataForm getExtendedInfo() {
        return extendedInfo;
    }

    /**
     * Returns the data form as List of PacketExtensions, or null if no data form is set.
     * This representation is needed by some classes (e.g. EntityCapsManager, NodeInformationProvider)
     * 
     * @return the data form as List of PacketExtensions
     */
    public List<ExtensionElement> getExtendedInfoAsList() {
        List<ExtensionElement> res = null;
        if (extendedInfo != null) {
            res = new ArrayList<ExtensionElement>(1);
            res.add(extendedInfo);
        }
        return res;
    }

    /**
     * Removes the data form containing extended service discovery information
     * from the information returned by this XMPP entity.<p>
     *
     * Since no stanza(/packet) is actually sent to the server it is safe to perform this
     * operation before logging to the server.
     */
    public synchronized void removeExtendedInfo() {
       extendedInfo = null;
       // Notify others of a state change of SDM. In order to keep the state consistent, this
       // method is synchronized
       renewEntityCapsVersion();
    }

    /**
     * Returns the discovered information of a given XMPP entity addressed by its JID.
     * Use null as entityID to query the server
     * 
     * @param entityID the address of the XMPP entity or null.
     * @return the discovered information.
     * @throws XMPPErrorException 
     * @throws NoResponseException 
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    public DiscoverInfo discoverInfo(Jid entityID) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
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
            if (EntityCapsManager.verifyDiscoverInfoVersion(nvh.getVer(), nvh.getHash(), info))
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
     * @throws XMPPErrorException if the operation failed for some reason.
     * @throws NoResponseException if there was no response from the server.
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    public DiscoverInfo discoverInfo(Jid entityID, String node) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        // Discover the entity's info
        DiscoverInfo disco = new DiscoverInfo();
        disco.setType(IQ.Type.get);
        disco.setTo(entityID);
        disco.setNode(node);

        Stanza result = connection().createStanzaCollectorAndSend(disco).nextResultOrThrow();

        return (DiscoverInfo) result;
    }

    /**
     * Returns the discovered items of a given XMPP entity addressed by its JID.
     * 
     * @param entityID the address of the XMPP entity.
     * @return the discovered information.
     * @throws XMPPErrorException if the operation failed for some reason.
     * @throws NoResponseException if there was no response from the server.
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    public DiscoverItems discoverItems(Jid entityID) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException  {
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
     * @throws XMPPErrorException if the operation failed for some reason.
     * @throws NoResponseException if there was no response from the server.
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    public DiscoverItems discoverItems(Jid entityID, String node) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        // Discover the entity's items
        DiscoverItems disco = new DiscoverItems();
        disco.setType(IQ.Type.get);
        disco.setTo(entityID);
        disco.setNode(node);

        Stanza result = connection().createStanzaCollectorAndSend(disco).nextResultOrThrow();
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
     * @throws XMPPErrorException 
     * @throws NoResponseException 
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    public boolean canPublishItems(Jid entityID) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        DiscoverInfo info = discoverInfo(entityID);
        return canPublishItems(info);
     }

     /**
      * Returns true if the server supports publishing of items. A client may wish to publish items
      * to the server so that the server can provide items associated to the client. These items will
      * be returned by the server whenever the server receives a disco request targeted to the bare
      * address of the client (i.e. user@host.com).
      * 
      * @param info the discover info stanza(/packet) to check.
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
     * @throws XMPPErrorException 
     * @throws NoResponseException 
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    public void publishItems(Jid entityID, DiscoverItems discoverItems) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
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
     * @throws XMPPErrorException if the operation failed for some reason.
     * @throws NoResponseException if there was no response from the server.
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    public void publishItems(Jid entityID, String node, DiscoverItems discoverItems) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException
            {
        discoverItems.setType(IQ.Type.set);
        discoverItems.setTo(entityID);
        discoverItems.setNode(node);

        connection().createStanzaCollectorAndSend(discoverItems).nextResultOrThrow();
    }

    /**
     * Returns true if the server supports the given feature.
     *
     * @param feature
     * @return true if the server supports the given feature.
     * @throws NoResponseException
     * @throws XMPPErrorException
     * @throws NotConnectedException
     * @throws InterruptedException 
     * @since 4.1
     */
    public boolean serverSupportsFeature(CharSequence feature) throws NoResponseException, XMPPErrorException,
                    NotConnectedException, InterruptedException {
        return serverSupportsFeatures(feature);
    }

    public boolean serverSupportsFeatures(CharSequence... features) throws NoResponseException,
                    XMPPErrorException, NotConnectedException, InterruptedException {
        return serverSupportsFeatures(Arrays.asList(features));
    }

    public boolean serverSupportsFeatures(Collection<? extends CharSequence> features)
                    throws NoResponseException, XMPPErrorException, NotConnectedException,
                    InterruptedException {
        return supportsFeatures(connection().getXMPPServiceDomain(), features);
    }

    /**
     * Queries the remote entity for it's features and returns true if the given feature is found.
     *
     * @param jid the JID of the remote entity
     * @param feature
     * @return true if the entity supports the feature, false otherwise
     * @throws XMPPErrorException 
     * @throws NoResponseException 
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    public boolean supportsFeature(Jid jid, CharSequence feature) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        return supportsFeatures(jid, feature);
    }

    public boolean supportsFeatures(Jid jid, CharSequence... features) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        return supportsFeatures(jid, Arrays.asList(features));
    }

    public boolean supportsFeatures(Jid jid, Collection<? extends CharSequence> features) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        DiscoverInfo result = discoverInfo(jid);
        for (CharSequence feature : features) {
            if (!result.containsFeature(feature)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Create a cache to hold the 25 most recently lookup services for a given feature for a period
     * of 24 hours.
     */
    private Cache<String, List<DiscoverInfo>> services = new ExpirationCache<>(25,
                    24 * 60 * 60 * 1000);

    /**
     * Find all services under the users service that provide a given feature.
     * 
     * @param feature the feature to search for
     * @param stopOnFirst if true, stop searching after the first service was found
     * @param useCache if true, query a cache first to avoid network I/O
     * @return a possible empty list of services providing the given feature
     * @throws NoResponseException
     * @throws XMPPErrorException
     * @throws NotConnectedException
     * @throws InterruptedException 
     */
    public List<DiscoverInfo> findServicesDiscoverInfo(String feature, boolean stopOnFirst, boolean useCache)
                    throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        List<DiscoverInfo> serviceDiscoInfo = null;
        DomainBareJid serviceName = connection().getXMPPServiceDomain();
        if (useCache) {
            serviceDiscoInfo = services.lookup(feature);
            if (serviceDiscoInfo != null) {
                return serviceDiscoInfo;
            }
        }
        serviceDiscoInfo = new LinkedList<>();
        // Send the disco packet to the server itself
        DiscoverInfo info;
        try {
            info = discoverInfo(serviceName);
        } catch (XMPPErrorException e) {
            // Be extra robust here: Return the empty linked list and log this situation
            LOGGER.log(Level.WARNING, "Could not discover information about service", e);
            return serviceDiscoInfo;
        }
        // Check if the server supports the feature
        if (info.containsFeature(feature)) {
            serviceDiscoInfo.add(info);
            if (stopOnFirst) {
                if (useCache) {
                    // Cache the discovered information
                    services.put(feature, serviceDiscoInfo);
                }
                return serviceDiscoInfo;
            }
        }
        DiscoverItems items;
        try {
            // Get the disco items and send the disco packet to each server item
            items = discoverItems(serviceName);
        } catch (XMPPErrorException e) {
            LOGGER.log(Level.WARNING, "Could not discover items about service", e);
            return serviceDiscoInfo;
        }
        for (DiscoverItems.Item item : items.getItems()) {
            try {
                // TODO is it OK here in all cases to query without the node attribute?
                // MultipleRecipientManager queried initially also with the node attribute, but this
                // could be simply a fault instead of intentional.
                info = discoverInfo(item.getEntityID());
            }
            catch (XMPPErrorException | NoResponseException e) {
                // Don't throw this exceptions if one of the server's items fail
                LOGGER.log(Level.WARNING, "Exception while discovering info for feature " + feature
                                + " of " + item.getEntityID() + " node: " + item.getNode(), e);
                continue;
            }
            if (info.containsFeature(feature)) {
                serviceDiscoInfo.add(info);
                //serviceAddresses.add(item.getEntityID().asDomainBareJid());
                if (stopOnFirst) {
                    break;
                }
            }
        }
        if (useCache) {
            // Cache the discovered information
            services.put(feature, serviceDiscoInfo);
        }
        return serviceDiscoInfo;
    }

    /**
     * Find all services under the users service that provide a given feature.
     * 
     * @param feature the feature to search for
     * @param stopOnFirst if true, stop searching after the first service was found
     * @param useCache if true, query a cache first to avoid network I/O
     * @return a possible empty list of services providing the given feature
     * @throws NoResponseException
     * @throws XMPPErrorException
     * @throws NotConnectedException
     * @throws InterruptedException 
     */
    public List<DomainBareJid> findServices(String feature, boolean stopOnFirst, boolean useCache) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        List<DiscoverInfo> services = findServicesDiscoverInfo(feature, stopOnFirst, useCache);
        List<DomainBareJid> res = new ArrayList<>(services.size());
        for (DiscoverInfo info : services) {
            res.add(info.getFrom().asDomainBareJid());
        }
        return res;
    }

    public DomainBareJid findService(String feature, boolean useCache, String category, String type)
                    throws NoResponseException, XMPPErrorException, NotConnectedException,
                    InterruptedException {
        List<DiscoverInfo> services = findServicesDiscoverInfo(feature, true, useCache);
        if (services.isEmpty()) {
            return null;
        }
        DiscoverInfo info = services.get(0);
        if (category != null && type != null) {
            if (!info.hasIdentity(category, type)) {
                return null;
            }
        }
        else if (category != null || type != null) {
            throw new IllegalArgumentException("Must specify either both, category and type, or none");
        }
        return info.getFrom().asDomainBareJid();
    }

    public DomainBareJid findService(String feature, boolean useCache) throws NoResponseException,
                    XMPPErrorException, NotConnectedException, InterruptedException {
        return findService(feature, useCache, null, null);
    }

    /**
     * Entity Capabilities
     */

    /**
     * Loads the ServiceDiscoveryManager with an EntityCapsManger that speeds up certain lookups.
     * 
     * @param manager
     */
    public void setEntityCapsManager(EntityCapsManager manager) {
        capsManager = manager;
    }

    /**
     * Updates the Entity Capabilities Verification String if EntityCaps is enabled.
     */
    private void renewEntityCapsVersion() {
        if (capsManager != null && capsManager.entityCapsEnabled())
            capsManager.updateLocalEntityCaps();
    }
}
