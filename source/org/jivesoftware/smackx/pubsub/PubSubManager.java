/**
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
package org.jivesoftware.smackx.pubsub;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.packet.IQ.Type;
import org.jivesoftware.smackx.Form;
import org.jivesoftware.smackx.FormField;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.packet.DiscoverInfo;
import org.jivesoftware.smackx.packet.DiscoverItems;
import org.jivesoftware.smackx.pubsub.packet.PubSub;
import org.jivesoftware.smackx.pubsub.packet.PubSubNamespace;
import org.jivesoftware.smackx.pubsub.packet.SyncPacketSend;
import org.jivesoftware.smackx.pubsub.util.NodeUtils;

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
	private Connection con;
	private String to;
	private Map<String, Node> nodeMap = new ConcurrentHashMap<String, Node>();
	
	/**
	 * Create a pubsub manager associated to the specified connection.
	 * 
	 * @param connection The XMPP connection
	 */
	public PubSubManager(Connection connection)
	{
		con = connection;
	}
	
	/**
	 * Create a pubsub manager associated to the specified connection where
	 * the pubsub requests require a specific to address for packets.
	 * 
	 * @param connection The XMPP connection
	 * @param toAddress The pubsub specific to address (required for some servers)
	 */
	public PubSubManager(Connection connection, String toAddress)
	{
		con = connection;
		to = toAddress;
	}
	
	/**
	 * Creates an instant node, if supported.
	 * 
	 * @return The node that was created
	 * @exception XMPPException
	 */
	public LeafNode createNode()
		throws XMPPException
	{
		PubSub reply = (PubSub)sendPubsubPacket(Type.SET, new NodeExtension(PubSubElementType.CREATE));
		NodeExtension elem = (NodeExtension)reply.getExtension("create", PubSubNamespace.BASIC.getXmlns());
		
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
	 * @exception XMPPException
	 */
	public LeafNode createNode(String id)
		throws XMPPException
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
	 * @exception XMPPException
	 */
	public Node createNode(String name, Form config)
		throws XMPPException
	{
		PubSub request = createPubsubPacket(to, Type.SET, new NodeExtension(PubSubElementType.CREATE, name));
		boolean isLeafNode = true;
		
		if (config != null)
		{
			request.addExtension(new FormNode(FormNodeType.CONFIGURE, config));
			FormField nodeTypeField = config.getField(ConfigureNodeFields.node_type.getFieldName());
			
			if (nodeTypeField != null)
				isLeafNode = nodeTypeField.getValues().next().equals(NodeType.leaf.toString());
		}

		// Errors will cause exceptions in getReply, so it only returns
		// on success.
		sendPubsubPacket(con, to, Type.SET, request);
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
	 * @throws XMPPException The node does not exist
	 */
	public Node getNode(String id)
		throws XMPPException
	{
		Node node = nodeMap.get(id);
		
		if (node == null)
		{
			DiscoverInfo info = new DiscoverInfo();
			info.setTo(to);
			info.setNode(id);
			
			DiscoverInfo infoReply = (DiscoverInfo)SyncPacketSend.getReply(con, info);
			
			if (infoReply.getIdentities().next().getType().equals(NodeType.leaf.toString()))
				node = new LeafNode(con, id);
			else
				node = new CollectionNode(con, id);
			node.setTo(to);
			nodeMap.put(id, node);
		}
		return node;
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
	 * 
	 * @throws XMPPException
	 */
	public DiscoverItems discoverNodes(String nodeId)
		throws XMPPException
	{
		DiscoverItems items = new DiscoverItems();
		
		if (nodeId != null)
			items.setNode(nodeId);
		items.setTo(to);
		DiscoverItems nodeItems = (DiscoverItems)SyncPacketSend.getReply(con, items);
		return nodeItems;
	}
	
	/**
	 * Gets the subscriptions on the root node.
	 * 
	 * @return List of exceptions
	 * 
	 * @throws XMPPException
	 */
	public List<Subscription> getSubscriptions()
		throws XMPPException
	{
		Packet reply = sendPubsubPacket(Type.GET, new NodeExtension(PubSubElementType.SUBSCRIPTIONS));
		SubscriptionsExtension subElem = (SubscriptionsExtension)reply.getExtension(PubSubElementType.SUBSCRIPTIONS.getElementName(), PubSubElementType.SUBSCRIPTIONS.getNamespace().getXmlns());
		return subElem.getSubscriptions();
	}
	
	/**
	 * Gets the affiliations on the root node.
	 * 
	 * @return List of affiliations
	 * 
	 * @throws XMPPException
	 */
	public List<Affiliation> getAffiliations()
		throws XMPPException
	{
		PubSub reply = (PubSub)sendPubsubPacket(Type.GET, new NodeExtension(PubSubElementType.AFFILIATIONS));
		AffiliationsExtension listElem = (AffiliationsExtension)reply.getExtension(PubSubElementType.AFFILIATIONS);
		return listElem.getAffiliations();
	}

	/**
	 * Delete the specified node
	 * 
	 * @param nodeId
	 * @throws XMPPException
	 */
	public void deleteNode(String nodeId)
		throws XMPPException
	{
		sendPubsubPacket(Type.SET, new NodeExtension(PubSubElementType.DELETE, nodeId), PubSubElementType.DELETE.getNamespace());
		nodeMap.remove(nodeId);
	}
	
	/**
	 * Returns the default settings for Node configuration.
	 * 
	 * @return configuration form containing the default settings.
	 */
	public ConfigureForm getDefaultConfiguration()
		throws XMPPException
	{
		// Errors will cause exceptions in getReply, so it only returns
		// on success.
		PubSub reply = (PubSub)sendPubsubPacket(Type.GET, new NodeExtension(PubSubElementType.DEFAULT), PubSubElementType.DEFAULT.getNamespace());
		return NodeUtils.getFormFromPacket(reply, PubSubElementType.DEFAULT);
	}
	
	/**
	 * Gets the supported features of the servers pubsub implementation
	 * as a standard {@link DiscoverInfo} instance.
	 * 
	 * @return The supported features
	 * 
	 * @throws XMPPException
	 */
	public DiscoverInfo getSupportedFeatures()
		throws XMPPException
	{
		ServiceDiscoveryManager mgr = ServiceDiscoveryManager.getInstanceFor(con);
		return mgr.discoverInfo(to);
	}
	
	private Packet sendPubsubPacket(Type type, PacketExtension ext, PubSubNamespace ns)
		throws XMPPException
	{
		return sendPubsubPacket(con, to, type, ext, ns);
	}

	private Packet sendPubsubPacket(Type type, PacketExtension ext)
		throws XMPPException
	{
		return sendPubsubPacket(type, ext, null);
	}

	static PubSub createPubsubPacket(String to, Type type, PacketExtension ext)
	{
		return createPubsubPacket(to, type, ext, null);
	}
	
	static PubSub createPubsubPacket(String to, Type type, PacketExtension ext, PubSubNamespace ns)
	{
		PubSub request = new PubSub();
		request.setTo(to);
		request.setType(type);
		
		if (ns != null)
		{
			request.setPubSubNamespace(ns);
		}
		request.addExtension(ext);
		
		return request;
	}

	static Packet sendPubsubPacket(Connection con, String to, Type type, PacketExtension ext)
		throws XMPPException
	{
		return sendPubsubPacket(con, to, type, ext, null);
	}
	
	static Packet sendPubsubPacket(Connection con, String to, Type type, PacketExtension ext, PubSubNamespace ns)
		throws XMPPException
	{
		return SyncPacketSend.getReply(con, createPubsubPacket(to, type, ext, ns));
	}

	static Packet sendPubsubPacket(Connection con, String to, Type type, PubSub packet)
		throws XMPPException
	{
		return sendPubsubPacket(con, to, type, packet, null);
	}

	static Packet sendPubsubPacket(Connection con, String to, Type type, PubSub packet, PubSubNamespace ns)
		throws XMPPException
	{
		return SyncPacketSend.getReply(con, packet);
	}

}
