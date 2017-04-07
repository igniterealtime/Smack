/**
 *
 * Copyright the original author or authors
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
package org.jivesoftware.smackx.pubsub;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.packet.EmptyResultIQ;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smack.packet.IQ.Type;
import org.jivesoftware.smack.packet.XMPPError.Condition;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.disco.packet.DiscoverInfo;
import org.jivesoftware.smackx.disco.packet.DiscoverItems;
import org.jivesoftware.smackx.pubsub.packet.PubSub;
import org.jivesoftware.smackx.pubsub.packet.PubSubNamespace;
import org.jivesoftware.smackx.pubsub.util.NodeUtils;
import org.jivesoftware.smackx.xdata.Form;
import org.jivesoftware.smackx.xdata.FormField;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.DomainBareJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

/**
 * This is the starting point for access to the pubsub service.  It
 * will provide access to general information about the service, as
 * well as create or retrieve pubsub {@link LeafNode} instances.  These 
 * instances provide the bulk of the functionality as defined in the 
 * pubsub specification <a href="http://xmpp.org/extensions/xep-0060.html">XEP-0060</a>.
 * 
 * @author Robin Collier
 */
public final class PubSubManager extends Manager {

    private static final Logger LOGGER = Logger.getLogger(PubSubManager.class.getName());
    private static final Map<XMPPConnection, Map<BareJid, PubSubManager>> INSTANCES = new WeakHashMap<>();

    /**
     * The JID of the PubSub service this manager manages.
     */
    private final BareJid pubSubService;

    /**
     * A map of node IDs to Nodes, used to cache those Nodes. This does only cache the type of Node,
     * i.e. {@link CollectionNode} or {@link LeafNode}.
     */
    private final Map<String, Node> nodeMap = new ConcurrentHashMap<String, Node>();

    /**
     * Get a PubSub manager for the default PubSub service of the connection.
     * 
     * @param connection
     * @return the default PubSub manager.
     */
    public static PubSubManager getInstance(XMPPConnection connection) {
        DomainBareJid pubSubService = null;
        if (connection.isAuthenticated()) {
            try {
                pubSubService = getPubSubService(connection);
            }
            catch (NoResponseException | XMPPErrorException | NotConnectedException e) {
                LOGGER.log(Level.WARNING, "Could not determine PubSub service", e);
            }
            catch (InterruptedException e) {
                LOGGER.log(Level.FINE, "Interupted while trying to determine PubSub service", e);
            }
        }
        if (pubSubService == null) {
            try {
                // Perform an educated guess about what the PubSub service's domain bare JID may be
                pubSubService = JidCreate.domainBareFrom("pubsub." + connection.getXMPPServiceDomain());
            }
            catch (XmppStringprepException e) {
                throw new RuntimeException(e);
            }
        }
        return getInstance(connection, pubSubService);
    }

    /**
     * Get the PubSub manager for the given connection and PubSub service.
     * 
     * @param connection the XMPP connection.
     * @param pubSubService the PubSub service.
     * @return a PubSub manager for the connection and service.
     */
    public static synchronized PubSubManager getInstance(XMPPConnection connection, BareJid pubSubService) {
        Map<BareJid, PubSubManager> managers = INSTANCES.get(connection);
        if (managers == null) {
            managers = new HashMap<>();
            INSTANCES.put(connection, managers);
        }
        PubSubManager pubSubManager = managers.get(pubSubService);
        if (pubSubManager == null) {
            pubSubManager = new PubSubManager(connection, pubSubService);
            managers.put(pubSubService, pubSubManager);
        }
        return pubSubManager;
    }

    /**
     * Create a pubsub manager associated to the specified connection where
     * the pubsub requests require a specific to address for packets.
     * 
     * @param connection The XMPP connection
     * @param toAddress The pubsub specific to address (required for some servers)
     */
    PubSubManager(XMPPConnection connection, BareJid toAddress)
    {
        super(connection);
        pubSubService = toAddress;
    }

    /**
     * Creates an instant node, if supported.
     * 
     * @return The node that was created
     * @throws XMPPErrorException 
     * @throws NoResponseException 
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    public LeafNode createNode() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException
    {
        PubSub reply = sendPubsubPacket(Type.set, new NodeExtension(PubSubElementType.CREATE), null);
        NodeExtension elem = reply.getExtension("create", PubSubNamespace.BASIC.getXmlns());

        LeafNode newNode = new LeafNode(this, elem.getNode());
        nodeMap.put(newNode.getId(), newNode);

        return newNode;
    }

    /**
     * Creates a node with default configuration.
     * 
     * @param nodeId The id of the node, which must be unique within the 
     * pubsub service
     * @return The node that was created
     * @throws XMPPErrorException 
     * @throws NoResponseException 
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    public LeafNode createNode(String nodeId) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException
    {
        return (LeafNode) createNode(nodeId, null);
    }

    /**
     * Creates a node with specified configuration.
     * 
     * Note: This is the only way to create a collection node.
     * 
     * @param nodeId The name of the node, which must be unique within the 
     * pubsub service
     * @param config The configuration for the node
     * @return The node that was created
     * @throws XMPPErrorException 
     * @throws NoResponseException 
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    public Node createNode(String nodeId, Form config) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException
    {
        PubSub request = PubSub.createPubsubPacket(pubSubService, Type.set, new NodeExtension(PubSubElementType.CREATE, nodeId), null);
        boolean isLeafNode = true;

        if (config != null)
        {
            request.addExtension(new FormNode(FormNodeType.CONFIGURE, config));
            FormField nodeTypeField = config.getField(ConfigureNodeFields.node_type.getFieldName());

            if (nodeTypeField != null)
                isLeafNode = nodeTypeField.getValues().get(0).equals(NodeType.leaf.toString());
        }

        // Errors will cause exceptions in getReply, so it only returns
        // on success.
        sendPubsubPacket(request);
        Node newNode = isLeafNode ? new LeafNode(this, nodeId) : new CollectionNode(this, nodeId);
        nodeMap.put(newNode.getId(), newNode);

        return newNode;
    }

    /**
     * Retrieves the requested node, if it exists.  It will throw an 
     * exception if it does not.
     * 
     * @param id - The unique id of the node
     * @return the node
     * @throws XMPPErrorException The node does not exist
     * @throws NoResponseException if there was no response from the server.
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    public <T extends Node> T getNode(String id) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException
    {
        Node node = nodeMap.get(id);

        if (node == null)
        {
            DiscoverInfo info = new DiscoverInfo();
            info.setTo(pubSubService);
            info.setNode(id);

            DiscoverInfo infoReply = connection().createStanzaCollectorAndSend(info).nextResultOrThrow();

            if (infoReply.hasIdentity(PubSub.ELEMENT, "leaf")) {
                node = new LeafNode(this, id);
            }
            else if (infoReply.hasIdentity(PubSub.ELEMENT, "collection")) {
                node = new CollectionNode(this, id);
            }
            else {
                // XEP-60 5.3 states that
                // "The 'disco#info' result MUST include an identity with a category of 'pubsub' and a type of either 'leaf' or 'collection'."
                // If this is not the case, then we are dealing with an PubSub implementation that doesn't follow the specification.
                throw new PubSubAssertionError.DiscoInfoNodeAssertionError(pubSubService, id);
            }
            nodeMap.put(id, node);
        }
        @SuppressWarnings("unchecked")
        T res = (T) node;
        return res;
    }

    /**
     * Try to get a leaf node and create one if it does not already exist.
     *
     * @param id The unique ID of the node.
     * @return the leaf node.
     * @throws NoResponseException
     * @throws NotConnectedException
     * @throws InterruptedException
     * @throws XMPPErrorException
     * @since 4.2.1
     */
    public LeafNode getOrCreateLeafNode(final String id)
                    throws NoResponseException, NotConnectedException, InterruptedException, XMPPErrorException {
        try {
            return getNode(id);
        }
        catch (XMPPErrorException e1) {
            if (e1.getXMPPError().getCondition() == Condition.item_not_found) {
                try {
                    return createNode(id);
                }
                catch (XMPPErrorException e2) {
                    if (e2.getXMPPError().getCondition() == Condition.conflict) {
                        // The node was created in the meantime, re-try getNode(). Note that this case should be rare.
                        return getNode(id);
                    }
                    throw e2;
                }
            }
            throw e1;
        }
        catch (PubSubAssertionError.DiscoInfoNodeAssertionError e) {
            // This could be caused by Prosody bug #805 (see https://prosody.im/issues/issue/805). Prosody does not
            // answer to disco#info requests on the node ID, which makes it undecidable if a node is a leaf or
            // collection node.
            LOGGER.warning("The PubSub service " + pubSubService
                            + " threw an DiscoInfoNodeAssertionError, trying workaround for Prosody bug #805 (https://prosody.im/issues/issue/805)");
            return getOrCreateLeafNodeProsodyWorkaround(id);
        }
    }

    private LeafNode getOrCreateLeafNodeProsodyWorkaround(final String id)
                    throws XMPPErrorException, NoResponseException, NotConnectedException, InterruptedException {
        try {
            return createNode(id);
        }
        catch (XMPPErrorException e1) {
            if (e1.getXMPPError().getCondition() == Condition.conflict) {
                Constructor<?> constructor = LeafNode.class.getDeclaredConstructors()[0];
                constructor.setAccessible(true);
                LeafNode res;
                try {
                    res = (LeafNode) constructor.newInstance(this, id);
                }
                catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                                | InvocationTargetException e2) {
                    throw new AssertionError(e2);
                }
                // TODO: How to verify that this is actually a leafe node and not a conflict with a collection node?
                return res;
            }
            throw e1;
        }
    }

    /**
     * Get all the nodes that currently exist as a child of the specified
     * collection node.  If the service does not support collection nodes
     * then all nodes will be returned.
     * 
     * To retrieve contents of the root collection node (if it exists), 
     * or there is no root collection node, pass null as the nodeId.
     * 
     * @param nodeId - The id of the collection node for which the child 
     * nodes will be returned.  
     * @return {@link DiscoverItems} representing the existing nodes
     * @throws XMPPErrorException 
     * @throws NoResponseException if there was no response from the server.
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    public DiscoverItems discoverNodes(String nodeId) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException
    {
        DiscoverItems items = new DiscoverItems();

        if (nodeId != null)
            items.setNode(nodeId);
        items.setTo(pubSubService);
        DiscoverItems nodeItems = connection().createStanzaCollectorAndSend(items).nextResultOrThrow();
        return nodeItems;
    }

    /**
     * Gets the subscriptions on the root node.
     * 
     * @return List of exceptions
     * @throws XMPPErrorException 
     * @throws NoResponseException 
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    public List<Subscription> getSubscriptions() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException
    {
        Stanza reply = sendPubsubPacket(Type.get, new NodeExtension(PubSubElementType.SUBSCRIPTIONS), null);
        SubscriptionsExtension subElem = reply.getExtension(PubSubElementType.SUBSCRIPTIONS.getElementName(), PubSubElementType.SUBSCRIPTIONS.getNamespace().getXmlns());
        return subElem.getSubscriptions();
    }

    /**
     * Gets the affiliations on the root node.
     * 
     * @return List of affiliations
     * @throws XMPPErrorException 
     * @throws NoResponseException 
     * @throws NotConnectedException 
     * @throws InterruptedException 
     * 
     */
    public List<Affiliation> getAffiliations() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException
    {
        PubSub reply = sendPubsubPacket(Type.get, new NodeExtension(PubSubElementType.AFFILIATIONS), null);
        AffiliationsExtension listElem = reply.getExtension(PubSubElementType.AFFILIATIONS);
        return listElem.getAffiliations();
    }

    /**
     * Delete the specified node.
     * 
     * @param nodeId
     * @throws XMPPErrorException 
     * @throws NoResponseException 
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    public void deleteNode(String nodeId) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException
    {
        sendPubsubPacket(Type.set, new NodeExtension(PubSubElementType.DELETE, nodeId), PubSubElementType.DELETE.getNamespace());
        nodeMap.remove(nodeId);
    }

    /**
     * Returns the default settings for Node configuration.
     * 
     * @return configuration form containing the default settings.
     * @throws XMPPErrorException 
     * @throws NoResponseException 
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    public ConfigureForm getDefaultConfiguration() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException
    {
        // Errors will cause exceptions in getReply, so it only returns
        // on success.
        PubSub reply = sendPubsubPacket(Type.get, new NodeExtension(PubSubElementType.DEFAULT), PubSubElementType.DEFAULT.getNamespace());
        return NodeUtils.getFormFromPacket(reply, PubSubElementType.DEFAULT);
    }

    /**
     * Get the JID of the PubSub service managed by this manager.
     *
     * @return the JID of the PubSub service.
     */
    public BareJid getServiceJid() {
        return pubSubService;
    }

    /**
     * Gets the supported features of the servers pubsub implementation
     * as a standard {@link DiscoverInfo} instance.
     * 
     * @return The supported features
     * @throws XMPPErrorException 
     * @throws NoResponseException 
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    public DiscoverInfo getSupportedFeatures() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException
    {
        ServiceDiscoveryManager mgr = ServiceDiscoveryManager.getInstanceFor(connection());
        return mgr.discoverInfo(pubSubService);
    }

    /**
     * Check if it is possible to create PubSub nodes on this service. It could be possible that the
     * PubSub service allows only certain XMPP entities (clients) to create nodes and publish items
     * to them.
     * <p>
     * Note that since XEP-60 does not provide an API to determine if an XMPP entity is allowed to
     * create nodes, therefore this method creates an instant node calling {@link #createNode()} to
     * determine if it is possible to create nodes.
     * </p>
     *
     * @return <code>true</code> if it is possible to create nodes, <code>false</code> otherwise.
     * @throws NoResponseException
     * @throws NotConnectedException
     * @throws InterruptedException
     * @throws XMPPErrorException
     */
    public boolean canCreateNodesAndPublishItems() throws NoResponseException, NotConnectedException, InterruptedException, XMPPErrorException {
        LeafNode leafNode = null;
        try {
            leafNode = createNode();
        }
        catch (XMPPErrorException e) {
            if (e.getXMPPError().getCondition() == XMPPError.Condition.forbidden) {
                return false;
            }
            throw e;
        } finally {
            if (leafNode != null) {
                deleteNode(leafNode.getId());
            }
        }
        return true;
    }

    private PubSub sendPubsubPacket(Type type, ExtensionElement ext, PubSubNamespace ns)
                    throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        return sendPubsubPacket(pubSubService, type, Collections.singletonList(ext), ns);
    }

    XMPPConnection getConnection() {
        return connection();
    }

    PubSub sendPubsubPacket(Jid to, Type type, List<ExtensionElement> extList, PubSubNamespace ns)
                    throws NoResponseException, XMPPErrorException, NotConnectedException,
                    InterruptedException {
// CHECKSTYLE:OFF
        PubSub pubSub = new PubSub(to, type, ns);
        for (ExtensionElement pe : extList) {
            pubSub.addExtension(pe);
        }
// CHECKSTYLE:ON
        return sendPubsubPacket(pubSub);
    }

    PubSub sendPubsubPacket(PubSub packet) throws NoResponseException, XMPPErrorException,
                    NotConnectedException, InterruptedException {
        IQ resultIQ = connection().createStanzaCollectorAndSend(packet).nextResultOrThrow();
        if (resultIQ instanceof EmptyResultIQ) {
            return null;
        }
        return (PubSub) resultIQ;
    }

    /**
     * Get the "default" PubSub service for a given XMPP connection. The default PubSub service is
     * simply an arbitrary XMPP service with the PubSub feature and an identity of category "pubsub"
     * and type "service".
     * 
     * @param connection
     * @return the default PubSub service or <code>null</code>.
     * @throws NoResponseException
     * @throws XMPPErrorException
     * @throws NotConnectedException
     * @throws InterruptedException
     * @see <a href="http://xmpp.org/extensions/xep-0060.html#entity-features">XEP-60 § 5.1 Discover
     *      Features</a>
     */
    public static DomainBareJid getPubSubService(XMPPConnection connection)
                    throws NoResponseException, XMPPErrorException, NotConnectedException,
                    InterruptedException {
        return ServiceDiscoveryManager.getInstanceFor(connection).findService(PubSub.NAMESPACE,
                        true, "pubsub", "service");
    }
}
