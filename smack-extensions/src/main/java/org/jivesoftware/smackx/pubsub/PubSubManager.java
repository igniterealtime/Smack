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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.QName;

import org.jivesoftware.smack.Manager;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.packet.EmptyResultIQ;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.packet.StanzaError;
import org.jivesoftware.smack.packet.StanzaError.Condition;
import org.jivesoftware.smack.packet.XmlElement;
import org.jivesoftware.smack.util.StringUtils;

import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.disco.packet.DiscoverInfo;
import org.jivesoftware.smackx.disco.packet.DiscoverItems;
import org.jivesoftware.smackx.pubsub.PubSubException.NotALeafNodeException;
import org.jivesoftware.smackx.pubsub.PubSubException.NotAPubSubNodeException;
import org.jivesoftware.smackx.pubsub.form.ConfigureForm;
import org.jivesoftware.smackx.pubsub.form.FillableConfigureForm;
import org.jivesoftware.smackx.pubsub.packet.PubSub;
import org.jivesoftware.smackx.pubsub.packet.PubSubNamespace;
import org.jivesoftware.smackx.pubsub.util.NodeUtils;
import org.jivesoftware.smackx.xdata.packet.DataForm;

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

    public static final String PLUS_NOTIFY = "+notify";

    public static final String AUTO_CREATE_FEATURE = "http://jabber.org/protocol/pubsub#auto-create";

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
    private final Map<String, Node> nodeMap = new ConcurrentHashMap<>();

    /**
     * Get a PubSub manager for the default PubSub service of the connection.
     *
     * @param connection TODO javadoc me please
     * @return the default PubSub manager.
     */
    // CHECKSTYLE:OFF:RegexpSingleline
    public static PubSubManager getInstanceFor(XMPPConnection connection) {
    // CHECKSTYLE:ON:RegexpSingleline
        DomainBareJid pubSubService = null;
        if (connection.isAuthenticated()) {
            try {
                pubSubService = getPubSubService(connection);
            }
            catch (NoResponseException | XMPPErrorException | NotConnectedException e) {
                LOGGER.log(Level.WARNING, "Could not determine PubSub service", e);
            }
            catch (InterruptedException e) {
                LOGGER.log(Level.FINE, "Interrupted while trying to determine PubSub service", e);
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
        return getInstanceFor(connection, pubSubService);
    }

    /**
     * Get the PubSub manager for the given connection and PubSub service. Use <code>null</code> as argument for
     * pubSubService to retrieve a PubSubManager for the users PEP service.
     *
     * @param connection the XMPP connection.
     * @param pubSubService the PubSub service, may be <code>null</code>.
     * @return a PubSub manager for the connection and service.
     */
    // CHECKSTYLE:OFF:RegexpSingleline
    public static PubSubManager getInstanceFor(XMPPConnection connection, BareJid pubSubService) {
    // CHECKSTYLE:ON:RegexpSingleline
        if (pubSubService != null && connection.isAuthenticated() && connection.getUser().asBareJid().equals(pubSubService)) {
            // PEP service.
            pubSubService = null;
        }

        PubSubManager pubSubManager;
        Map<BareJid, PubSubManager> managers;
        synchronized (INSTANCES) {
            managers = INSTANCES.get(connection);
            if (managers == null) {
                managers = new HashMap<>();
                INSTANCES.put(connection, managers);
            }
        }
        synchronized (managers) {
            pubSubManager = managers.get(pubSubService);
            if (pubSubManager == null) {
                pubSubManager = new PubSubManager(connection, pubSubService);
                managers.put(pubSubService, pubSubManager);
            }
        }

        return pubSubManager;
    }

    /**
     * Deprecated.
     *
     * @param connection the connection.
     * @return the PubSub manager for the given connection.
     * @deprecated use {@link #getInstanceFor(XMPPConnection)} instead.
     */
    @Deprecated
    // TODO: Remove in Smack 4.5.
    public static PubSubManager getInstance(XMPPConnection connection) {
        return getInstanceFor(connection);
    }

    /**
     * Deprecated.
     *
     * @param connection the connection.
     * @param pubSubService the XMPP address of the PubSub service.
     * @return the PubSub manager for the given connection.
     * @deprecated use {@link #getInstanceFor(XMPPConnection, BareJid)} instead.
     */
    @Deprecated
    // TODO: Remove in Smack 4.5.
    public static PubSubManager getInstance(XMPPConnection connection, BareJid pubSubService) {
        return getInstanceFor(connection, pubSubService);
    }

    /**
     * Create a pubsub manager associated to the specified connection where
     * the pubsub requests require a specific to address for packets.
     *
     * @param connection The XMPP connection
     * @param toAddress The pubsub specific to address (required for some servers)
     */
    PubSubManager(XMPPConnection connection, BareJid toAddress) {
        super(connection);
        pubSubService = toAddress;
    }

    private void checkIfXmppErrorBecauseOfNotLeafNode(String nodeId, XMPPErrorException xmppErrorException)
                    throws XMPPErrorException, NotALeafNodeException {
        Condition condition = xmppErrorException.getStanzaError().getCondition();
        if (condition == Condition.feature_not_implemented) {
            // XEP-0060 § 6.5.9.5: Item retrieval not supported, e.g. because node is a collection node
            throw new PubSubException.NotALeafNodeException(nodeId, pubSubService);
        }

        throw xmppErrorException;
    }

    /**
     * Creates an instant node, if supported.
     *
     * @return The node that was created
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public LeafNode createNode() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        PubSub reply = sendPubsubPacket(IQ.Type.set, new NodeExtension(PubSubElementType.CREATE), null);
        QName qname = new QName(PubSubNamespace.basic.getXmlns(), "create");
        NodeExtension elem = (NodeExtension) reply.getExtension(qname);

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
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public LeafNode createNode(String nodeId) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
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
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public Node createNode(String nodeId, FillableConfigureForm config) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        PubSub request = PubSub.createPubsubPacket(pubSubService, IQ.Type.set, new NodeExtension(PubSubElementType.CREATE, nodeId));
        boolean isLeafNode = true;

        if (config != null) {
            DataForm submitForm = config.getDataFormToSubmit();
            request.addExtension(new FormNode(FormNodeType.CONFIGURE, submitForm));
            NodeType nodeType = config.getNodeType();
            // Note that some implementations do to have the pubsub#node_type field in their defauilt configuration,
            // which I believe to be a bug. However, since PubSub specifies the default node type to be 'leaf' we assume
            // leaf if the field does not exist.
            isLeafNode = nodeType == null || nodeType == NodeType.leaf;
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
     *
     * @return the node
     * @throws XMPPErrorException The node does not exist
     * @throws NoResponseException if there was no response from the server.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     * @throws NotAPubSubNodeException if a involved node is not a PubSub node.
     */
    public Node getNode(String id) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException, NotAPubSubNodeException {
        StringUtils.requireNotNullNorEmpty(id, "The node ID can not be null or the empty string");
        Node node = nodeMap.get(id);

        if (node == null) {
            XMPPConnection connection = connection();
            DiscoverInfo info = DiscoverInfo.builder(connection)
                    .to(pubSubService)
                    .setNode(id)
                    .build();

            DiscoverInfo infoReply = connection.sendIqRequestAndWaitForResponse(info);

            if (infoReply.hasIdentity(PubSub.ELEMENT, "leaf")) {
                node = new LeafNode(this, id);
            }
            else if (infoReply.hasIdentity(PubSub.ELEMENT, "collection")) {
                node = new CollectionNode(this, id);
            }
            else {
                throw new PubSubException.NotAPubSubNodeException(id, infoReply);
            }
            nodeMap.put(id, node);
        }
        return node;
    }

    /**
     * Try to get a leaf node and create one if it does not already exist.
     *
     * @param id The unique ID of the node.
     * @return the leaf node.
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NotALeafNodeException in case the node already exists as collection node.
     * @since 4.2.1
     */
    public LeafNode getOrCreateLeafNode(final String id)
                    throws NoResponseException, NotConnectedException, InterruptedException, XMPPErrorException, NotALeafNodeException {
        try {
            return getLeafNode(id);
        }
        catch (NotAPubSubNodeException e) {
            return createNode(id);
        }
        catch (XMPPErrorException e1) {
            if (e1.getStanzaError().getCondition() == Condition.item_not_found) {
                try {
                    return createNode(id);
                }
                catch (XMPPErrorException e2) {
                    if (e2.getStanzaError().getCondition() == Condition.conflict) {
                        // The node was created in the meantime, re-try getNode(). Note that this case should be rare.
                        try {
                            return getLeafNode(id);
                        }
                        catch (NotAPubSubNodeException e) {
                            // Should not happen
                            throw new IllegalStateException(e);
                        }
                    }
                    throw e2;
                }
            }
            if (e1.getStanzaError().getCondition() == Condition.service_unavailable) {
                // This could be caused by Prosody bug #805 (see https://prosody.im/issues/issue/805). Prosody does not
                // answer to disco#info requests on the node ID, which makes it undecidable if a node is a leaf or
                // collection node.
                LOGGER.warning("The PubSub service " + pubSubService
                        + " threw an DiscoInfoNodeAssertionError, trying workaround for Prosody bug #805 (https://prosody.im/issues/issue/805)");
                return getOrCreateLeafNodeProsodyWorkaround(id);
            }
            throw e1;
        }
    }

    /**
     * Try to get a leaf node with the given node ID.
     *
     * @param id the node ID.
     * @return the requested leaf node.
     * @throws NotALeafNodeException in case the node exists but is a collection node.
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NotAPubSubNodeException if a involved node is not a PubSub node.
     * @since 4.2.1
     */
    public LeafNode getLeafNode(String id) throws NotALeafNodeException, NoResponseException, NotConnectedException,
                    InterruptedException, XMPPErrorException, NotAPubSubNodeException {
        Node node;
        try {
            node = getNode(id);
        }
        catch (XMPPErrorException e) {
            if (e.getStanzaError().getCondition() == Condition.service_unavailable) {
                // This could be caused by Prosody bug #805 (see https://prosody.im/issues/issue/805). Prosody does not
                // answer to disco#info requests on the node ID, which makes it undecidable if a node is a leaf or
                // collection node.
                return getLeafNodeProsodyWorkaround(id);
            }
            throw e;
        }

        if (node instanceof LeafNode) {
            return (LeafNode) node;
        }

        throw new PubSubException.NotALeafNodeException(id, pubSubService);
    }

    private LeafNode getLeafNodeProsodyWorkaround(final String id) throws NoResponseException, NotConnectedException,
                    InterruptedException, NotALeafNodeException, XMPPErrorException {
        LeafNode leafNode = new LeafNode(this, id);
        try {
            // Try to ensure that this is not a collection node by asking for one item form the node.
            leafNode.getItems(1);
        } catch (XMPPErrorException e) {
            checkIfXmppErrorBecauseOfNotLeafNode(id, e);
        }

        nodeMap.put(id, leafNode);

        return leafNode;
    }

    private LeafNode getOrCreateLeafNodeProsodyWorkaround(final String id)
                    throws XMPPErrorException, NoResponseException, NotConnectedException, InterruptedException, NotALeafNodeException {
        try {
            return createNode(id);
        }
        catch (XMPPErrorException e1) {
            if (e1.getStanzaError().getCondition() == Condition.conflict) {
                return getLeafNodeProsodyWorkaround(id);
            }
            throw e1;
        }
    }

    /**
     * Try to publish an item and, if the node with the given ID does not exists, auto-create the node.
     * <p>
     * Not every PubSub service supports automatic node creation. You can discover if this service supports it by using
     * {@link #supportsAutomaticNodeCreation()}.
     * </p>
     *
     * @param id The unique id of the node.
     * @param item The item to publish.
     * @param <I> type of the item.
     *
     * @return the LeafNode on which the item was published.
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     * @throws NotALeafNodeException if a PubSub leaf node operation was attempted on a non-leaf node.
     * @since 4.2.1
     */
    public <I extends Item> LeafNode tryToPublishAndPossibleAutoCreate(String id, I item)
                    throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException,
                    NotALeafNodeException {
        LeafNode leafNode = new LeafNode(this, id);

        try {
            leafNode.publish(item);
        } catch (XMPPErrorException e) {
            checkIfXmppErrorBecauseOfNotLeafNode(id, e);
        }

        // If LeafNode.publish() did not throw then we have successfully published an item and possible auto-created
        // (XEP-0163 § 3., XEP-0060 § 7.1.4) the node. So we can put the node into the nodeMap.
        nodeMap.put(id, leafNode);

        return leafNode;
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
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NoResponseException if there was no response from the server.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public DiscoverItems discoverNodes(String nodeId) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        DiscoverItems items = new DiscoverItems();

        if (nodeId != null)
            items.setNode(nodeId);
        items.setTo(pubSubService);
        DiscoverItems nodeItems = connection().sendIqRequestAndWaitForResponse(items);
        return nodeItems;
    }

    /**
     * Gets the subscriptions on the root node.
     *
     * @return List of exceptions
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public List<Subscription> getSubscriptions() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        Stanza reply = sendPubsubPacket(IQ.Type.get, new NodeExtension(PubSubElementType.SUBSCRIPTIONS), null);
        SubscriptionsExtension subElem = (SubscriptionsExtension) reply.getExtensionElement(PubSubElementType.SUBSCRIPTIONS.getElementName(), PubSubElementType.SUBSCRIPTIONS.getNamespace().getXmlns());
        return subElem.getSubscriptions();
    }

    /**
     * Gets the affiliations on the root node.
     *
     * @return List of affiliations
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     *
     */
    public List<Affiliation> getAffiliations() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        PubSub reply = sendPubsubPacket(IQ.Type.get, new NodeExtension(PubSubElementType.AFFILIATIONS), null);
        AffiliationsExtension listElem = reply.getExtension(PubSubElementType.AFFILIATIONS);
        return listElem.getAffiliations();
    }

    /**
     * Delete the specified node.
     *
     * @param nodeId TODO javadoc me please
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     * @return <code>true</code> if this node existed and was deleted and <code>false</code> if this node did not exist.
     */
    public boolean deleteNode(String nodeId) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        boolean res = true;
        try {
            sendPubsubPacket(IQ.Type.set, new NodeExtension(PubSubElementType.DELETE, nodeId), PubSubElementType.DELETE.getNamespace());
        } catch (XMPPErrorException e) {
            if (e.getStanzaError().getCondition() == StanzaError.Condition.item_not_found) {
                res = false;
            } else {
                throw e;
            }
        }
        nodeMap.remove(nodeId);
        return res;
    }

    /**
     * Returns the default settings for Node configuration.
     *
     * @return configuration form containing the default settings.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public ConfigureForm getDefaultConfiguration() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        // Errors will cause exceptions in getReply, so it only returns
        // on success.
        PubSub reply = sendPubsubPacket(IQ.Type.get, new NodeExtension(PubSubElementType.DEFAULT), PubSubElementType.DEFAULT.getNamespace());
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
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public DiscoverInfo getSupportedFeatures() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        ServiceDiscoveryManager mgr = ServiceDiscoveryManager.getInstanceFor(connection());
        return mgr.discoverInfo(pubSubService);
    }

    /**
     * Check if the PubSub service supports automatic node creation.
     *
     * @return true if the PubSub service supports automatic node creation.
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     * @since 4.2.1
     * @see <a href="https://xmpp.org/extensions/xep-0060.html#publisher-publish-autocreate">XEP-0060 § 7.1.4 Automatic Node Creation</a>
     */
    public boolean supportsAutomaticNodeCreation()
                    throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        ServiceDiscoveryManager sdm = ServiceDiscoveryManager.getInstanceFor(connection());
        return sdm.supportsFeature(pubSubService, AUTO_CREATE_FEATURE);
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
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     * @throws XMPPErrorException if there was an XMPP error returned.
     */
    public boolean canCreateNodesAndPublishItems() throws NoResponseException, NotConnectedException, InterruptedException, XMPPErrorException {
        LeafNode leafNode = null;
        try {
            leafNode = createNode();
        }
        catch (XMPPErrorException e) {
            if (e.getStanzaError().getCondition() == StanzaError.Condition.forbidden) {
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

    private PubSub sendPubsubPacket(IQ.Type type, XmlElement ext, PubSubNamespace ns)
                    throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        return sendPubsubPacket(pubSubService, type, Collections.singletonList(ext), ns);
    }

    XMPPConnection getConnection() {
        return connection();
    }

    PubSub sendPubsubPacket(Jid to, IQ.Type type, List<XmlElement> extList, PubSubNamespace ns)
                    throws NoResponseException, XMPPErrorException, NotConnectedException,
                    InterruptedException {
// CHECKSTYLE:OFF
        PubSub pubSub = new PubSub(to, type, ns);
        for (XmlElement pe : extList) {
            pubSub.addExtension(pe);
        }
// CHECKSTYLE:ON
        return sendPubsubPacket(pubSub);
    }

    PubSub sendPubsubPacket(PubSub packet) throws NoResponseException, XMPPErrorException,
                    NotConnectedException, InterruptedException {
        IQ resultIQ = connection().sendIqRequestAndWaitForResponse(packet);
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
     * @param connection TODO javadoc me please
     * @return the default PubSub service or <code>null</code>.
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
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
