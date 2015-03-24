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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.packet.EmptyResultIQ;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.IQ.Type;
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
final public class PubSubManager
{
	private XMPPConnection con;
	private DomainBareJid to;
	private Map<String, Node> nodeMap = new ConcurrentHashMap<String, Node>();

	/**
	 * Create a pubsub manager associated to the specified connection.  Defaults the service
	 * name to <i>pubsub</i>
	 * 
	 * @param connection The XMPP connection
	 * @throws XmppStringprepException 
	 */
	public PubSubManager(XMPPConnection connection) throws XmppStringprepException
	{
		con = connection;
		to = JidCreate.domainBareFrom("pubsub." + connection.getServiceName());
	}

	/**
	 * Create a pubsub manager associated to the specified connection where
	 * the pubsub requests require a specific to address for packets.
	 * 
	 * @param connection The XMPP connection
	 * @param toAddress The pubsub specific to address (required for some servers)
	 */
	public PubSubManager(XMPPConnection connection, DomainBareJid toAddress)
	{
		con = connection;
		to = toAddress;
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

		LeafNode newNode = new LeafNode(con, elem.getNode());
		newNode.setTo(to);
		nodeMap.put(newNode.getId(), newNode);

		return newNode;
	}

	/**
	 * Creates a node with default configuration.
	 * 
	 * @param id The id of the node, which must be unique within the 
	 * pubsub service
	 * @return The node that was created
	 * @throws XMPPErrorException 
	 * @throws NoResponseException 
	 * @throws NotConnectedException 
	 * @throws InterruptedException 
	 */
	public LeafNode createNode(String id) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException
	{
		return (LeafNode)createNode(id, null);
	}

	/**
	 * Creates a node with specified configuration.
	 * 
	 * Note: This is the only way to create a collection node.
	 * 
	 * @param name The name of the node, which must be unique within the 
	 * pubsub service
	 * @param config The configuration for the node
	 * @return The node that was created
	 * @throws XMPPErrorException 
	 * @throws NoResponseException 
	 * @throws NotConnectedException 
	 * @throws InterruptedException 
	 */
	public Node createNode(String name, Form config) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException
	{
		PubSub request = PubSub.createPubsubPacket(to, Type.set, new NodeExtension(PubSubElementType.CREATE, name), null);
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
		sendPubsubPacket(con, request);
		Node newNode = isLeafNode ? new LeafNode(con, name) : new CollectionNode(con, name);
		newNode.setTo(to);
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
			info.setTo(to);
			info.setNode(id);

			DiscoverInfo infoReply = (DiscoverInfo) con.createPacketCollectorAndSend(info).nextResultOrThrow();

            if (infoReply.hasIdentity(PubSub.ELEMENT, "leaf")) {
                node = new LeafNode(con, id);
            }
            else if (infoReply.hasIdentity(PubSub.ELEMENT, "collection")) {
                node = new CollectionNode(con, id);
            }
            else {
                // XEP-60 5.3 states that
                // "The 'disco#info' result MUST include an identity with a category of 'pubsub' and a type of either 'leaf' or 'collection'."
                // If this is not the case, then we are dealing with an PubSub implementation that doesn't follow the specification.
                throw new AssertionError(
                                "PubSub service '"
                                                + to
                                                + "' returned disco info result for node '"
                                                + id
                                                + "', but it did not contain an Identity of type 'leaf' or 'collection' (and category 'pubsub'), which is not allowed according to XEP-60 5.3.");
            }
			node.setTo(to);
			nodeMap.put(id, node);
		}
		@SuppressWarnings("unchecked")
		T res = (T) node;
		return res;
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
		items.setTo(to);
		DiscoverItems nodeItems = (DiscoverItems) con.createPacketCollectorAndSend(items).nextResultOrThrow();
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
	 * Delete the specified node
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
		ServiceDiscoveryManager mgr = ServiceDiscoveryManager.getInstanceFor(con);
		return mgr.discoverInfo(to);
	}

    private PubSub sendPubsubPacket(Type type, ExtensionElement ext, PubSubNamespace ns)
                    throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        return sendPubsubPacket(con, to, type, Collections.singletonList(ext), ns);
    }

	static PubSub sendPubsubPacket(XMPPConnection con, Jid to, Type type, List<ExtensionElement> extList, PubSubNamespace ns) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException
	{
// CHECKSTYLE:OFF
	    PubSub pubSub = new PubSub(to, type, ns);
	    for (ExtensionElement pe : extList) {
	        pubSub.addExtension(pe);
	    }
// CHECKSTYLE:ON
		return sendPubsubPacket(con ,pubSub);
	}

	static PubSub sendPubsubPacket(XMPPConnection con, PubSub packet) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException
	{
		IQ resultIQ = con.createPacketCollectorAndSend(packet).nextResultOrThrow();
        if (resultIQ instanceof EmptyResultIQ) {
            return null;
        }
        return (PubSub) resultIQ;
	}

}
