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

import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.filter.OrFilter;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.IQ.Type;
import org.jivesoftware.smackx.delay.DelayInformationManager;
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
	
	protected ConcurrentHashMap<ItemEventListener<Item>, StanzaListener> itemEventToListenerMap = new ConcurrentHashMap<ItemEventListener<Item>, StanzaListener>();
	protected ConcurrentHashMap<ItemDeleteListener, StanzaListener> itemDeleteToListenerMap = new ConcurrentHashMap<ItemDeleteListener, StanzaListener>();
	protected ConcurrentHashMap<NodeConfigListener, StanzaListener> configEventToListenerMap = new ConcurrentHashMap<NodeConfigListener, StanzaListener>();
	
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
        PubSub pubSub = createPubsubPacket(Type.get, new NodeExtension(
                        PubSubElementType.CONFIGURE_OWNER, getId()), PubSubNamespace.OWNER);
		Stanza reply = sendPubsubPacket(pubSub);
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
        PubSub packet = createPubsubPacket(Type.set, new FormNode(FormNodeType.CONFIGURE_OWNER,
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
     * {@code returnedExtensions} will be filled with the stanza(/packet) extensions found in the answer.
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
    public List<Subscription> getSubscriptions(List<ExtensionElement> additionalExtensions, Collection<ExtensionElement> returnedExtensions)
                    throws NoResponseException, XMPPErrorException, NotConnectedException {
        return getSubscriptions(additionalExtensions, returnedExtensions, null);
    }

    /**
     * Get the subscriptions currently associated with this node as owner.
     *
     * @return List of {@link Subscription}
     * @throws XMPPErrorException
     * @throws NoResponseException
     * @throws NotConnectedException
     * @see #getSubscriptionsAsOwner(List, Collection)
     * @since 4.1
     */
    public List<Subscription> getSubscriptionsAsOwner() throws NoResponseException, XMPPErrorException,
                    NotConnectedException {
        return getSubscriptionsAsOwner(null, null);
    }

    /**
     * Get the subscriptions currently associated with this node as owner.
     * <p>
     * Unlike {@link #getSubscriptions(List, Collection)}, which only retrieves the subscriptions of the current entity
     * ("user"), this method returns a list of <b>all</b> subscriptions. This requires the entity to have the sufficient
     * privileges to manage subscriptions.
     * </p>
     * <p>
     * {@code additionalExtensions} can be used e.g. to add a "Result Set Management" extension.
     * {@code returnedExtensions} will be filled with the stanza(/packet) extensions found in the answer.
     * </p>
     *
     * @param additionalExtensions
     * @param returnedExtensions a collection that will be filled with the returned stanza(/packet) extensions
     * @return List of {@link Subscription}
     * @throws NoResponseException
     * @throws XMPPErrorException
     * @throws NotConnectedException
     * @see <a href="http://www.xmpp.org/extensions/xep-0060.html#owner-subscriptions-retrieve">XEP-60 § 8.8.1 -
     *      Retrieve Subscriptions List</a>
     * @since 4.1
     */
    public List<Subscription> getSubscriptionsAsOwner(List<ExtensionElement> additionalExtensions,
                    Collection<ExtensionElement> returnedExtensions) throws NoResponseException, XMPPErrorException,
                    NotConnectedException {
        return getSubscriptions(additionalExtensions, returnedExtensions, PubSubNamespace.OWNER);
    }

    private List<Subscription> getSubscriptions(List<ExtensionElement> additionalExtensions,
                    Collection<ExtensionElement> returnedExtensions, PubSubNamespace pubSubNamespace)
                    throws NoResponseException, XMPPErrorException, NotConnectedException {
        PubSub pubSub = createPubsubPacket(Type.get, new NodeExtension(PubSubElementType.SUBSCRIPTIONS, getId()), pubSubNamespace);
        if (additionalExtensions != null) {
            for (ExtensionElement pe : additionalExtensions) {
                pubSub.addExtension(pe);
            }
        }
        PubSub reply = sendPubsubPacket(pubSub);
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
     * {@code returnedExtensions} will be filled with the stanza(/packet) extensions found in the answer.
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
    public List<Affiliation> getAffiliations(List<ExtensionElement> additionalExtensions, Collection<ExtensionElement> returnedExtensions)
                    throws NoResponseException, XMPPErrorException, NotConnectedException {
        PubSub pubSub = createPubsubPacket(Type.get, new NodeExtension(PubSubElementType.AFFILIATIONS, getId()));
        if (additionalExtensions != null) {
            for (ExtensionElement pe : additionalExtensions) {
                pubSub.addExtension(pe);
            }
        }
        PubSub reply = sendPubsubPacket(pubSub);
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
	    PubSub pubSub = createPubsubPacket(Type.set, new SubscribeExtension(jid, getId()));
		PubSub reply = sendPubsubPacket(pubSub);
		return reply.getExtension(PubSubElementType.SUBSCRIPTION);
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
	    PubSub request = createPubsubPacket(Type.set, new SubscribeExtension(jid, getId()));
		request.addExtension(new FormNode(FormNodeType.OPTIONS, subForm));
		PubSub reply = PubSubManager.sendPubsubPacket(con, request);
		return reply.getExtension(PubSubElementType.SUBSCRIPTION);
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
		sendPubsubPacket(createPubsubPacket(Type.set, new UnsubscribeExtension(jid, getId(), subscriptionId)));
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
		PubSub packet = sendPubsubPacket(createPubsubPacket(Type.get, new OptionsExtension(jid, getId(), subscriptionId)));
		FormNode ext = packet.getExtension(PubSubElementType.OPTIONS);
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
		StanzaListener conListener = new ItemEventTranslator(listener); 
		itemEventToListenerMap.put(listener, conListener);
		con.addSyncStanzaListener(conListener, new EventContentFilter(EventElementType.items.toString(), "item"));
	}

	/**
	 * Unregister a listener for publication events.
	 * 
	 * @param listener The handler to unregister
	 */
	public void removeItemEventListener(@SuppressWarnings("rawtypes") ItemEventListener listener)
	{
		StanzaListener conListener = itemEventToListenerMap.remove(listener);
		
		if (conListener != null)
			con.removeSyncStanzaListener(conListener);
	}

	/**
	 * Register a listener for configuration events.  This listener
	 * will get called whenever the node's configuration changes.
	 * 
	 * @param listener The handler for the event
	 */
	public void addConfigurationListener(NodeConfigListener listener)
	{
		StanzaListener conListener = new NodeConfigTranslator(listener); 
		configEventToListenerMap.put(listener, conListener);
		con.addSyncStanzaListener(conListener, new EventContentFilter(EventElementType.configuration.toString()));
	}

	/**
	 * Unregister a listener for configuration events.
	 * 
	 * @param listener The handler to unregister
	 */
	public void removeConfigurationListener(NodeConfigListener listener)
	{
		StanzaListener conListener = configEventToListenerMap .remove(listener);
		
		if (conListener != null)
			con.removeSyncStanzaListener(conListener);
	}
	
	/**
	 * Register an listener for item delete events.  This listener
	 * gets called whenever an item is deleted from the node.
	 * 
	 * @param listener The handler for the event
	 */
	public void addItemDeleteListener(ItemDeleteListener listener)
	{
		StanzaListener delListener = new ItemDeleteTranslator(listener); 
		itemDeleteToListenerMap.put(listener, delListener);
		EventContentFilter deleteItem = new EventContentFilter(EventElementType.items.toString(), "retract");
		EventContentFilter purge = new EventContentFilter(EventElementType.purge.toString());
		
		con.addSyncStanzaListener(delListener, new OrFilter(deleteItem, purge));
	}

	/**
	 * Unregister a listener for item delete events.
	 * 
	 * @param listener The handler to unregister
	 */
	public void removeItemDeleteListener(ItemDeleteListener listener)
	{
		StanzaListener conListener = itemDeleteToListenerMap .remove(listener);
		
		if (conListener != null)
			con.removeSyncStanzaListener(conListener);
	}

	@Override
	public String toString()
	{
		return super.toString() + " " + getClass().getName() + " id: " + id;
	}
	
	protected PubSub createPubsubPacket(Type type, ExtensionElement ext)
	{
		return createPubsubPacket(type, ext, null);
	}
	
	protected PubSub createPubsubPacket(Type type, ExtensionElement ext, PubSubNamespace ns)
	{
		return PubSub.createPubsubPacket(to, type, ext, ns);
	}

	protected PubSub sendPubsubPacket(PubSub packet) throws NoResponseException, XMPPErrorException, NotConnectedException
	{
		return PubSubManager.sendPubsubPacket(con, packet);
	}


	private static List<String> getSubscriptionIds(Stanza packet)
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
	public class ItemEventTranslator implements StanzaListener
	{
		@SuppressWarnings("rawtypes")
        private ItemEventListener listener;

		public ItemEventTranslator(@SuppressWarnings("rawtypes") ItemEventListener eventListener)
		{
			listener = eventListener;
		}
		
		@SuppressWarnings({ "rawtypes", "unchecked" })
        public void processPacket(Stanza packet)
		{
	        EventElement event = (EventElement)packet.getExtension("event", PubSubNamespace.EVENT.getXmlns());
			ItemsExtension itemsElem = (ItemsExtension)event.getEvent();
            ItemPublishEvent eventItems = new ItemPublishEvent(itemsElem.getNode(), (List<Item>)itemsElem.getItems(), getSubscriptionIds(packet), DelayInformationManager.getDelayTimestamp(packet));
			listener.handlePublishedItems(eventItems);
		}
	}

	/**
	 * This class translates low level item deletion events into api level objects for 
	 * user consumption.
	 * 
	 * @author Robin Collier
	 */
	public class ItemDeleteTranslator implements StanzaListener
	{
		private ItemDeleteListener listener;

		public ItemDeleteTranslator(ItemDeleteListener eventListener)
		{
			listener = eventListener;
		}
		
		public void processPacket(Stanza packet)
		{
	        EventElement event = (EventElement)packet.getExtension("event", PubSubNamespace.EVENT.getXmlns());
	        
	        List<ExtensionElement> extList = event.getExtensions();
	        
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
	public class NodeConfigTranslator implements StanzaListener
	{
		private NodeConfigListener listener;

		public NodeConfigTranslator(NodeConfigListener eventListener)
		{
			listener = eventListener;
		}
		
		public void processPacket(Stanza packet)
		{
	        EventElement event = (EventElement)packet.getExtension("event", PubSubNamespace.EVENT.getXmlns());
			ConfigurationEvent config = (ConfigurationEvent)event.getEvent();

			listener.handleNodeConfiguration(config);
		}
	}

	/**
	 * Filter for {@link StanzaListener} to filter out events not specific to the 
	 * event type expected for this node.
	 * 
	 * @author Robin Collier
	 */
	class EventContentFilter implements StanzaFilter
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

		public boolean accept(Stanza packet)
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
					List<ExtensionElement> secondLevelList = ((EmbeddedPacketExtension)embedEvent).getExtensions();
					
					if (secondLevelList.size() > 0 && secondLevelList.get(0).getElementName().equals(secondElement))
						return true;
				}
			}
			return false;
		}
	}
}
