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
/*
 * Created on 2009-07-09
 */
package org.jivesoftware.smackx.pubsub;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.OrFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.packet.IQ.Type;
import org.jivesoftware.smackx.Form;
import org.jivesoftware.smackx.packet.DelayInformation;
import org.jivesoftware.smackx.packet.DiscoverInfo;
import org.jivesoftware.smackx.packet.Header;
import org.jivesoftware.smackx.packet.HeadersExtension;
import org.jivesoftware.smackx.pubsub.listener.ItemDeleteListener;
import org.jivesoftware.smackx.pubsub.listener.ItemEventListener;
import org.jivesoftware.smackx.pubsub.listener.NodeConfigListener;
import org.jivesoftware.smackx.pubsub.packet.PubSub;
import org.jivesoftware.smackx.pubsub.packet.PubSubNamespace;
import org.jivesoftware.smackx.pubsub.packet.SyncPacketSend;
import org.jivesoftware.smackx.pubsub.util.NodeUtils;

abstract public class Node
{
	protected Connection con;
	protected String id;
	protected String to;
	
	protected ConcurrentHashMap<ItemEventListener<Item>, PacketListener> itemEventToListenerMap = new ConcurrentHashMap<ItemEventListener<Item>, PacketListener>();
	protected ConcurrentHashMap<ItemDeleteListener, PacketListener> itemDeleteToListenerMap = new ConcurrentHashMap<ItemDeleteListener, PacketListener>();
	protected ConcurrentHashMap<NodeConfigListener, PacketListener> configEventToListenerMap = new ConcurrentHashMap<NodeConfigListener, PacketListener>();
	
	/**
	 * Construct a node associated to the supplied connection with the specified 
	 * node id.
	 * 
	 * @param connection The connection the node is associated with
	 * @param nodeName The node id
	 */
	Node(Connection connection, String nodeName)
	{
		con = connection;
		id = nodeName;
	}

	/**
	 * Some XMPP servers may require a specific service to be addressed on the 
	 * server.
	 * 
	 *   For example, OpenFire requires the server to be prefixed by <b>pubsub</b>
	 */
	void setTo(String toAddress)
	{
		to = toAddress;
	}

	/**
	 * Get the NodeId
	 * 
	 * @return the node id
	 */
	public String getId() 
	{
		return id;
	}
	/**
	 * Returns a configuration form, from which you can create an answer form to be submitted
	 * via the {@link #sendConfigurationForm(Form)}.
	 * 
	 * @return the configuration form
	 */
	public ConfigureForm getNodeConfiguration()
		throws XMPPException
	{
		Packet reply = sendPubsubPacket(Type.GET, new NodeExtension(PubSubElementType.CONFIGURE_OWNER, getId()), PubSubNamespace.OWNER);
		return NodeUtils.getFormFromPacket(reply, PubSubElementType.CONFIGURE_OWNER);
	}
	
	/**
	 * Update the configuration with the contents of the new {@link Form}
	 * 
	 * @param submitForm
	 */
	public void sendConfigurationForm(Form submitForm)
		throws XMPPException
	{
		PubSub packet = createPubsubPacket(Type.SET, new FormNode(FormNodeType.CONFIGURE_OWNER, getId(), submitForm), PubSubNamespace.OWNER);
		SyncPacketSend.getReply(con, packet);
	}
	
	/**
	 * Discover node information in standard {@link DiscoverInfo} format.
	 * 
	 * @return The discovery information about the node.
	 * 
	 * @throws XMPPException
	 */
	public DiscoverInfo discoverInfo()
		throws XMPPException
	{
		DiscoverInfo info = new DiscoverInfo();
		info.setTo(to);
		info.setNode(getId());
		return (DiscoverInfo)SyncPacketSend.getReply(con, info);
	}
	
	/**
	 * Get the subscriptions currently associated with this node.
	 * 
	 * @return List of {@link Subscription}
	 * 
	 * @throws XMPPException
	 */
	public List<Subscription> getSubscriptions()
		throws XMPPException
	{
		PubSub reply = (PubSub)sendPubsubPacket(Type.GET, new NodeExtension(PubSubElementType.SUBSCRIPTIONS, getId()));
		SubscriptionsExtension subElem = (SubscriptionsExtension)reply.getExtension(PubSubElementType.SUBSCRIPTIONS);
		return subElem.getSubscriptions();
	}

	/**
	 * The user subscribes to the node using the supplied jid.  The
	 * bare jid portion of this one must match the jid for the connection.
	 * 
	 * Please note that the {@link Subscription.State} should be checked 
	 * on return since more actions may be required by the caller.
	 * {@link Subscription.State#pending} - The owner must approve the subscription 
	 * request before messages will be received.
	 * {@link Subscription.State#unconfigured} - If the {@link Subscription#isConfigRequired()} is true, 
	 * the caller must configure the subscription before messages will be received.  If it is false
	 * the caller can configure it but is not required to do so.
	 * @param jid The jid to subscribe as.
	 * @return The subscription
	 * @exception XMPPException
	 */
	public Subscription subscribe(String jid)
		throws XMPPException
	{
		PubSub reply = (PubSub)sendPubsubPacket(Type.SET, new SubscribeExtension(jid, getId()));
		return (Subscription)reply.getExtension(PubSubElementType.SUBSCRIPTION);
	}
	
	/**
	 * The user subscribes to the node using the supplied jid and subscription
	 * options.  The bare jid portion of this one must match the jid for the 
	 * connection.
	 * 
	 * Please note that the {@link Subscription.State} should be checked 
	 * on return since more actions may be required by the caller.
	 * {@link Subscription.State#pending} - The owner must approve the subscription 
	 * request before messages will be received.
	 * {@link Subscription.State#unconfigured} - If the {@link Subscription#isConfigRequired()} is true, 
	 * the caller must configure the subscription before messages will be received.  If it is false
	 * the caller can configure it but is not required to do so.
	 * @param jid The jid to subscribe as.
	 * @return The subscription
	 * @exception XMPPException
	 */
	public Subscription subscribe(String jid, SubscribeForm subForm)
		throws XMPPException
	{
		PubSub request = createPubsubPacket(Type.SET, new SubscribeExtension(jid, getId()));
		request.addExtension(new FormNode(FormNodeType.OPTIONS, subForm));
		PubSub reply = (PubSub)PubSubManager.sendPubsubPacket(con, jid, Type.SET, request);
		return (Subscription)reply.getExtension(PubSubElementType.SUBSCRIPTION);
	}

	/**
	 * Remove the subscription related to the specified JID.  This will only 
	 * work if there is only 1 subscription.  If there are multiple subscriptions,
	 * use {@link #unsubscribe(String, String)}.
	 * 
	 * @param jid The JID used to subscribe to the node
	 * 
	 * @throws XMPPException
	 */
	public void unsubscribe(String jid)
		throws XMPPException
	{
		unsubscribe(jid, null);
	}
	
	/**
	 * Remove the specific subscription related to the specified JID.
	 * 
	 * @param jid The JID used to subscribe to the node
	 * @param subscriptionId The id of the subscription being removed
	 * 
	 * @throws XMPPException
	 */
	public void unsubscribe(String jid, String subscriptionId)
		throws XMPPException
	{
		sendPubsubPacket(Type.SET, new UnsubscribeExtension(jid, getId(), subscriptionId));
	}

	/**
	 * Returns a SubscribeForm for subscriptions, from which you can create an answer form to be submitted
	 * via the {@link #sendConfigurationForm(Form)}.
	 * 
	 * @return A subscription options form
	 * 
	 * @throws XMPPException
	 */
	public SubscribeForm getSubscriptionOptions(String jid)
		throws XMPPException
	{
		return getSubscriptionOptions(jid, null);
	}


	/**
	 * Get the options for configuring the specified subscription.
	 * 
	 * @param jid JID the subscription is registered under
	 * @param subscriptionId The subscription id
	 * 
	 * @return The subscription option form
	 * 
	 * @throws XMPPException
	 */
	public SubscribeForm getSubscriptionOptions(String jid, String subscriptionId)
		throws XMPPException
	{
		PubSub packet = (PubSub)sendPubsubPacket(Type.GET, new OptionsExtension(jid, getId(), subscriptionId));
		FormNode ext = (FormNode)packet.getExtension(PubSubElementType.OPTIONS);
		return new SubscribeForm(ext.getForm());
	}

	/**
	 * Register a listener for item publication events.  This 
	 * listener will get called whenever an item is published to 
	 * this node.
	 * 
	 * @param listener The handler for the event
	 */
	public void addItemEventListener(ItemEventListener listener)
	{
		PacketListener conListener = new ItemEventTranslator(listener); 
		itemEventToListenerMap.put(listener, conListener);
		con.addPacketListener(conListener, new EventContentFilter(EventElementType.items.toString(), "item"));
	}

	/**
	 * Unregister a listener for publication events.
	 * 
	 * @param listener The handler to unregister
	 */
	public void removeItemEventListener(ItemEventListener listener)
	{
		PacketListener conListener = itemEventToListenerMap.remove(listener);
		
		if (conListener != null)
			con.removePacketListener(conListener);
	}

	/**
	 * Register a listener for configuration events.  This listener
	 * will get called whenever the node's configuration changes.
	 * 
	 * @param listener The handler for the event
	 */
	public void addConfigurationListener(NodeConfigListener listener)
	{
		PacketListener conListener = new NodeConfigTranslator(listener); 
		configEventToListenerMap.put(listener, conListener);
		con.addPacketListener(conListener, new EventContentFilter(EventElementType.configuration.toString()));
	}

	/**
	 * Unregister a listener for configuration events.
	 * 
	 * @param listener The handler to unregister
	 */
	public void removeConfigurationListener(NodeConfigListener listener)
	{
		PacketListener conListener = configEventToListenerMap .remove(listener);
		
		if (conListener != null)
			con.removePacketListener(conListener);
	}
	
	/**
	 * Register an listener for item delete events.  This listener
	 * gets called whenever an item is deleted from the node.
	 * 
	 * @param listener The handler for the event
	 */
	public void addItemDeleteListener(ItemDeleteListener listener)
	{
		PacketListener delListener = new ItemDeleteTranslator(listener); 
		itemDeleteToListenerMap.put(listener, delListener);
		EventContentFilter deleteItem = new EventContentFilter(EventElementType.items.toString(), "retract");
		EventContentFilter purge = new EventContentFilter(EventElementType.purge.toString());
		
		con.addPacketListener(delListener, new OrFilter(deleteItem, purge));
	}

	/**
	 * Unregister a listener for item delete events.
	 * 
	 * @param listener The handler to unregister
	 */
	public void removeItemDeleteListener(ItemDeleteListener listener)
	{
		PacketListener conListener = itemDeleteToListenerMap .remove(listener);
		
		if (conListener != null)
			con.removePacketListener(conListener);
	}

	@Override
	public String toString()
	{
		return super.toString() + " " + getClass().getName() + " id: " + id;
	}
	
	protected PubSub createPubsubPacket(Type type, PacketExtension ext)
	{
		return createPubsubPacket(type, ext, null);
	}
	
	protected PubSub createPubsubPacket(Type type, PacketExtension ext, PubSubNamespace ns)
	{
		return PubSubManager.createPubsubPacket(to, type, ext, ns);
	}

	protected Packet sendPubsubPacket(Type type, NodeExtension ext)
		throws XMPPException
	{
		return PubSubManager.sendPubsubPacket(con, to, type, ext);
	}

	protected Packet sendPubsubPacket(Type type, NodeExtension ext, PubSubNamespace ns)
		throws XMPPException
	{
		return PubSubManager.sendPubsubPacket(con, to, type, ext, ns);
	}


	private static List<String> getSubscriptionIds(Packet packet)
	{
		HeadersExtension headers = (HeadersExtension)packet.getExtension("headers", "http://jabber.org/protocol/shim");
		List<String> values = null;
		
		if (headers != null)
		{
			values = new ArrayList<String>(headers.getHeaders().size());
			
			for (Header header : headers.getHeaders())
			{
				values.add(header.getValue());
			}
		}
		return values;
	}

	/**
	 * This class translates low level item publication events into api level objects for 
	 * user consumption.
	 * 
	 * @author Robin Collier
	 */
	public class ItemEventTranslator implements PacketListener
	{
		private ItemEventListener listener;

		public ItemEventTranslator(ItemEventListener eventListener)
		{
			listener = eventListener;
		}
		
		public void processPacket(Packet packet)
		{
	        EventElement event = (EventElement)packet.getExtension("event", PubSubNamespace.EVENT.getXmlns());
			ItemsExtension itemsElem = (ItemsExtension)event.getEvent();
			DelayInformation delay = (DelayInformation)packet.getExtension("delay", "urn:xmpp:delay");
			
			// If there was no delay based on XEP-0203, then try XEP-0091 for backward compatibility
			if (delay == null)
			{
				delay = (DelayInformation)packet.getExtension("x", "jabber:x:delay");
			}
			ItemPublishEvent eventItems = new ItemPublishEvent(itemsElem.getNode(), (List<Item>)itemsElem.getItems(), getSubscriptionIds(packet), (delay == null ? null : delay.getStamp()));
			listener.handlePublishedItems(eventItems);
		}
	}

	/**
	 * This class translates low level item deletion events into api level objects for 
	 * user consumption.
	 * 
	 * @author Robin Collier
	 */
	public class ItemDeleteTranslator implements PacketListener
	{
		private ItemDeleteListener listener;

		public ItemDeleteTranslator(ItemDeleteListener eventListener)
		{
			listener = eventListener;
		}
		
		public void processPacket(Packet packet)
		{
	        EventElement event = (EventElement)packet.getExtension("event", PubSubNamespace.EVENT.getXmlns());
	        
	        List<PacketExtension> extList = event.getExtensions();
	        
	        if (extList.get(0).getElementName().equals(PubSubElementType.PURGE_EVENT.getElementName()))
	        {
	        	listener.handlePurge();
	        }
	        else
	        {
				ItemsExtension itemsElem = (ItemsExtension)event.getEvent();
				Collection<? extends PacketExtension> pubItems = itemsElem.getItems();
				Iterator<RetractItem> it = (Iterator<RetractItem>)pubItems.iterator();
				List<String> items = new ArrayList<String>(pubItems.size());

				while (it.hasNext())
				{
					RetractItem item = it.next();
					items.add(item.getId());
				}

				ItemDeleteEvent eventItems = new ItemDeleteEvent(itemsElem.getNode(), items, getSubscriptionIds(packet));
				listener.handleDeletedItems(eventItems);
	        }
		}
	}
	
	/**
	 * This class translates low level node configuration events into api level objects for 
	 * user consumption.
	 * 
	 * @author Robin Collier
	 */
	public class NodeConfigTranslator implements PacketListener
	{
		private NodeConfigListener listener;

		public NodeConfigTranslator(NodeConfigListener eventListener)
		{
			listener = eventListener;
		}
		
		public void processPacket(Packet packet)
		{
	        EventElement event = (EventElement)packet.getExtension("event", PubSubNamespace.EVENT.getXmlns());
			ConfigurationEvent config = (ConfigurationEvent)event.getEvent();

			listener.handleNodeConfiguration(config);
		}
	}

	/**
	 * Filter for {@link PacketListener} to filter out events not specific to the 
	 * event type expected for this node.
	 * 
	 * @author Robin Collier
	 */
	class EventContentFilter implements PacketFilter
	{
		private String firstElement;
		private String secondElement;
		
		EventContentFilter(String elementName)
		{
			firstElement = elementName;
		}

		EventContentFilter(String firstLevelEelement, String secondLevelElement)
		{
			firstElement = firstLevelEelement;
			secondElement = secondLevelElement;
		}

		public boolean accept(Packet packet)
		{
			if (!(packet instanceof Message))
				return false;

			EventElement event = (EventElement)packet.getExtension("event", PubSubNamespace.EVENT.getXmlns());
			
			if (event == null)
				return false;

			NodeExtension embedEvent = event.getEvent();
			
			if (embedEvent == null)
				return false;
			
			if (embedEvent.getElementName().equals(firstElement))
			{
				if (!embedEvent.getNode().equals(getId()))
					return false;
				
				if (secondElement == null)
					return true;
				
				if (embedEvent instanceof EmbeddedPacketExtension)
				{
					List<PacketExtension> secondLevelList = ((EmbeddedPacketExtension)embedEvent).getExtensions();
					
					if (secondLevelList.size() > 0 && secondLevelList.get(0).getElementName().equals(secondElement))
						return true;
				}
			}
			return false;
		}
	}
}
