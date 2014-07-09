/**
 *
 * Copyright 2009 Robin Collier.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.filter.OrFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.packet.IQ.Type;
import org.jivesoftware.smackx.delay.packet.DelayInformation;
import org.jivesoftware.smackx.disco.packet.DiscoverInfo;
import org.jivesoftware.smackx.pubsub.listener.ItemDeleteListener;
import org.jivesoftware.smackx.pubsub.listener.ItemEventListener;
import org.jivesoftware.smackx.pubsub.listener.NodeConfigListener;
import org.jivesoftware.smackx.pubsub.packet.PubSub;
import org.jivesoftware.smackx.pubsub.packet.PubSubNamespace;
import org.jivesoftware.smackx.pubsub.util.NodeUtils;
import org.jivesoftware.smackx.shim.packet.Header;
import org.jivesoftware.smackx.shim.packet.HeadersExtension;
import org.jivesoftware.smackx.xdata.Form;

abstract public class Node
{
	protected XMPPConnection con;
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
	Node(XMPPConnection connection, String nodeName)
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
	 * @throws XMPPErrorException 
	 * @throws NoResponseException 
	 * @throws NotConnectedException 
	 */
	public ConfigureForm getNodeConfiguration() throws NoResponseException, XMPPErrorException, NotConnectedException
	{
        PubSub pubSub = createPubsubPacket(Type.GET, new NodeExtension(
                        PubSubElementType.CONFIGURE_OWNER, getId()), PubSubNamespace.OWNER);
		Packet reply = sendPubsubPacket(pubSub);
		return NodeUtils.getFormFromPacket(reply, PubSubElementType.CONFIGURE_OWNER);
	}
	
	/**
	 * Update the configuration with the contents of the new {@link Form}
	 * 
	 * @param submitForm
	 * @throws XMPPErrorException 
	 * @throws NoResponseException 
	 * @throws NotConnectedException 
	 */
	public void sendConfigurationForm(Form submitForm) throws NoResponseException, XMPPErrorException, NotConnectedException
	{
        PubSub packet = createPubsubPacket(Type.SET, new FormNode(FormNodeType.CONFIGURE_OWNER,
                        getId(), submitForm), PubSubNamespace.OWNER);
		con.createPacketCollectorAndSend(packet).nextResultOrThrow();
	}
	
	/**
	 * Discover node information in standard {@link DiscoverInfo} format.
	 * 
	 * @return The discovery information about the node.
	 * @throws XMPPErrorException 
	 * @throws NoResponseException if there was no response from the server.
	 * @throws NotConnectedException 
	 */
	public DiscoverInfo discoverInfo() throws NoResponseException, XMPPErrorException, NotConnectedException
	{
		DiscoverInfo info = new DiscoverInfo();
		info.setTo(to);
		info.setNode(getId());
		return (DiscoverInfo) con.createPacketCollectorAndSend(info).nextResultOrThrow();
	}
	
	/**
	 * Get the subscriptions currently associated with this node.
	 * 
	 * @return List of {@link Subscription}
	 * @throws XMPPErrorException 
	 * @throws NoResponseException 
	 * @throws NotConnectedException 
	 * 
	 */
	public List<Subscription> getSubscriptions() throws NoResponseException, XMPPErrorException, NotConnectedException
	{
        return getSubscriptions(null, null);
	}

    /**
     * Get the subscriptions currently associated with this node.
     * <p>
     * {@code additionalExtensions} can be used e.g. to add a "Result Set Management" extension.
     * {@code returnedExtensions} will be filled with the packet extensions found in the answer.
     * </p>
     *
     * @param additionalExtensions
     * @param returnedExtensions a collection that will be filled with the returned packet
     *        extensions
     * @return List of {@link Subscription}
     * @throws NoResponseException
     * @throws XMPPErrorException
     * @throws NotConnectedException
     */
    public List<Subscription> getSubscriptions(List<PacketExtension> additionalExtensions, Collection<PacketExtension> returnedExtensions)
                    throws NoResponseException, XMPPErrorException, NotConnectedException {
        PubSub pubSub = createPubsubPacket(Type.GET, new NodeExtension(
                        PubSubElementType.SUBSCRIPTIONS, getId()));
        if (additionalExtensions != null) {
            for (PacketExtension pe : additionalExtensions) {
                pubSub.addExtension(pe);
            }
        }
        PubSub reply = (PubSub) sendPubsubPacket(pubSub);
        if (returnedExtensions != null) {
            returnedExtensions.addAll(reply.getExtensions());
        }
        SubscriptionsExtension subElem = (SubscriptionsExtension) reply.getExtension(PubSubElementType.SUBSCRIPTIONS);
        return subElem.getSubscriptions();
    }

	/**
	 * Get the affiliations of this node.
	 *
	 * @return List of {@link Affiliation}
	 * @throws NoResponseException
	 * @throws XMPPErrorException
	 * @throws NotConnectedException
	 */
    public List<Affiliation> getAffiliations() throws NoResponseException, XMPPErrorException,
                    NotConnectedException {
        return getAffiliations(null, null);
    }

    /**
     * Get the affiliations of this node.
     * <p>
     * {@code additionalExtensions} can be used e.g. to add a "Result Set Management" extension.
     * {@code returnedExtensions} will be filled with the packet extensions found in the answer.
     * </p>
     *
     * @param additionalExtensions additional {@code PacketExtensions} add to the request
     * @param returnedExtensions a collection that will be filled with the returned packet
     *        extensions
     * @return List of {@link Affiliation}
     * @throws NoResponseException
     * @throws XMPPErrorException
     * @throws NotConnectedException
     */
    public List<Affiliation> getAffiliations(List<PacketExtension> additionalExtensions, Collection<PacketExtension> returnedExtensions)
                    throws NoResponseException, XMPPErrorException, NotConnectedException {
        PubSub pubSub = createPubsubPacket(Type.GET, new NodeExtension(PubSubElementType.AFFILIATIONS, getId()));
        if (additionalExtensions != null) {
            for (PacketExtension pe : additionalExtensions) {
                pubSub.addExtension(pe);
            }
        }
        PubSub reply = (PubSub) sendPubsubPacket(pubSub);
        if (returnedExtensions != null) {
            returnedExtensions.addAll(reply.getExtensions());
        }
        AffiliationsExtension affilElem = (AffiliationsExtension) reply.getExtension(PubSubElementType.AFFILIATIONS);
        return affilElem.getAffiliations();
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
	 * @throws XMPPErrorException 
	 * @throws NoResponseException 
	 * @throws NotConnectedException 
	 */
	public Subscription subscribe(String jid) throws NoResponseException, XMPPErrorException, NotConnectedException
	{
	    PubSub pubSub = createPubsubPacket(Type.SET, new SubscribeExtension(jid, getId()));
		PubSub reply = (PubSub)sendPubsubPacket(pubSub);
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
	 * @throws XMPPErrorException 
	 * @throws NoResponseException 
	 * @throws NotConnectedException 
	 */
	public Subscription subscribe(String jid, SubscribeForm subForm) throws NoResponseException, XMPPErrorException, NotConnectedException
	{
	    PubSub request = createPubsubPacket(Type.SET, new SubscribeExtension(jid, getId()));
		request.addExtension(new FormNode(FormNodeType.OPTIONS, subForm));
		PubSub reply = (PubSub)PubSubManager.sendPubsubPacket(con, request);
		return (Subscription)reply.getExtension(PubSubElementType.SUBSCRIPTION);
	}

	/**
	 * Remove the subscription related to the specified JID.  This will only 
	 * work if there is only 1 subscription.  If there are multiple subscriptions,
	 * use {@link #unsubscribe(String, String)}.
	 * 
	 * @param jid The JID used to subscribe to the node
	 * @throws XMPPErrorException 
	 * @throws NoResponseException 
	 * @throws NotConnectedException 
	 * 
	 */
	public void unsubscribe(String jid) throws NoResponseException, XMPPErrorException, NotConnectedException
	{
		unsubscribe(jid, null);
	}
	
	/**
	 * Remove the specific subscription related to the specified JID.
	 * 
	 * @param jid The JID used to subscribe to the node
	 * @param subscriptionId The id of the subscription being removed
	 * @throws XMPPErrorException 
	 * @throws NoResponseException 
	 * @throws NotConnectedException 
	 */
	public void unsubscribe(String jid, String subscriptionId) throws NoResponseException, XMPPErrorException, NotConnectedException
	{
		sendPubsubPacket(createPubsubPacket(Type.SET, new UnsubscribeExtension(jid, getId(), subscriptionId)));
	}

	/**
	 * Returns a SubscribeForm for subscriptions, from which you can create an answer form to be submitted
	 * via the {@link #sendConfigurationForm(Form)}.
	 * 
	 * @return A subscription options form
	 * @throws XMPPErrorException 
	 * @throws NoResponseException 
	 * @throws NotConnectedException 
	 */
	public SubscribeForm getSubscriptionOptions(String jid) throws NoResponseException, XMPPErrorException, NotConnectedException
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
	 * @throws XMPPErrorException 
	 * @throws NoResponseException 
	 * @throws NotConnectedException 
	 * 
	 */
	public SubscribeForm getSubscriptionOptions(String jid, String subscriptionId) throws NoResponseException, XMPPErrorException, NotConnectedException
	{
		PubSub packet = (PubSub)sendPubsubPacket(createPubsubPacket(Type.GET, new OptionsExtension(jid, getId(), subscriptionId)));
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
	@SuppressWarnings("unchecked")
    public void addItemEventListener(@SuppressWarnings("rawtypes") ItemEventListener listener)
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
	public void removeItemEventListener(@SuppressWarnings("rawtypes") ItemEventListener listener)
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
		return PubSub.createPubsubPacket(to, type, ext, ns);
	}

	protected Packet sendPubsubPacket(PubSub packet) throws NoResponseException, XMPPErrorException, NotConnectedException
	{
		return PubSubManager.sendPubsubPacket(con, packet);
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
		@SuppressWarnings("rawtypes")
        private ItemEventListener listener;

		public ItemEventTranslator(@SuppressWarnings("rawtypes") ItemEventListener eventListener)
		{
			listener = eventListener;
		}
		
		@SuppressWarnings({ "rawtypes", "unchecked" })
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
				@SuppressWarnings("unchecked")
                Collection<RetractItem> pubItems = (Collection<RetractItem>) itemsElem.getItems();
				List<String> items = new ArrayList<String>(pubItems.size());

				for (RetractItem item : pubItems)
				{
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
