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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.packet.IQ.Type;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smackx.disco.packet.DiscoverItems;
import org.jivesoftware.smackx.pubsub.packet.PubSub;

/**
 * The main class for the majority of pubsub functionality.  In general
 * almost all pubsub capabilities are related to the concept of a node.
 * All items are published to a node, and typically subscribed to by other
 * users.  These users then retrieve events based on this subscription.
 * 
 * @author Robin Collier
 */
public class LeafNode extends Node
{
    LeafNode(PubSubManager pubSubManager, String nodeId)
    {
        super(pubSubManager, nodeId);
    }

    /**
     * Get information on the items in the node in standard
     * {@link DiscoverItems} format.
     * 
     * @return The item details in {@link DiscoverItems} format
     * @throws XMPPErrorException 
     * @throws NoResponseException if there was no response from the server.
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    public DiscoverItems discoverItems() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException
    {
        DiscoverItems items = new DiscoverItems();
        items.setTo(pubSubManager.getServiceJid());
        items.setNode(getId());
        return pubSubManager.getConnection().createStanzaCollectorAndSend(items).nextResultOrThrow();
    }

    /**
     * Get the current items stored in the node.
     * 
     * @return List of {@link Item} in the node
     * @throws XMPPErrorException
     * @throws NoResponseException if there was no response from the server.
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    public <T extends Item> List<T> getItems() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException
    {
        return getItems((List<ExtensionElement>) null, (List<ExtensionElement>) null);
    }

    /**
     * Get the current items stored in the node based
     * on the subscription associated with the provided 
     * subscription id.
     * 
     * @param subscriptionId -  The subscription id for the 
     * associated subscription.
     * @return List of {@link Item} in the node
     * @throws XMPPErrorException
     * @throws NoResponseException if there was no response from the server.
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    public <T extends Item> List<T> getItems(String subscriptionId) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException
    {
        PubSub request = createPubsubPacket(Type.get, new GetItemsRequest(getId(), subscriptionId));
        return getItems(request);
    }

    /**
     * Get the items specified from the node.  This would typically be
     * used when the server does not return the payload due to size 
     * constraints.  The user would be required to retrieve the payload 
     * after the items have been retrieved via {@link #getItems()} or an
     * event, that did not include the payload.
     * 
     * @param ids Item ids of the items to retrieve
     * 
     * @return The list of {@link Item} with payload
     * @throws XMPPErrorException 
     * @throws NoResponseException if there was no response from the server.
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    public <T extends Item> List<T> getItems(Collection<String> ids) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException
    {
        List<Item> itemList = new ArrayList<Item>(ids.size());

        for (String id : ids)
        {
            itemList.add(new Item(id));
        }
        PubSub request = createPubsubPacket(Type.get, new ItemsExtension(ItemsExtension.ItemsElementType.items, getId(), itemList));
        return getItems(request);
    }

    /**
     * Get items persisted on the node, limited to the specified number.
     * 
     * @param maxItems Maximum number of items to return
     * 
     * @return List of {@link Item}
     * @throws XMPPErrorException
     * @throws NoResponseException if there was no response from the server.
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    public <T extends Item> List<T> getItems(int maxItems) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException
    {
        PubSub request = createPubsubPacket(Type.get, new GetItemsRequest(getId(), maxItems));
        return getItems(request);
    }

    /**
     * Get items persisted on the node, limited to the specified number
     * based on the subscription associated with the provided subscriptionId.
     * 
     * @param maxItems Maximum number of items to return
     * @param subscriptionId The subscription which the retrieval is based
     * on.
     * 
     * @return List of {@link Item}
     * @throws XMPPErrorException
     * @throws NoResponseException if there was no response from the server.
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    public <T extends Item> List<T> getItems(int maxItems, String subscriptionId) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException
    {
        PubSub request = createPubsubPacket(Type.get, new GetItemsRequest(getId(), subscriptionId, maxItems));
        return getItems(request);
    }

    /**
     * Get items persisted on the node.
     * <p>
     * {@code additionalExtensions} can be used e.g. to add a "Result Set Management" extension.
     * {@code returnedExtensions} will be filled with the stanza(/packet) extensions found in the answer.
     * </p>
     * 
     * @param additionalExtensions additional {@code PacketExtensions} to be added to the request.
     *        This is an optional argument, if provided as null no extensions will be added.
     * @param returnedExtensions a collection that will be filled with the returned packet
     *        extensions. This is an optional argument, if provided as null it won't be populated.
     * @return List of {@link Item}
     * @throws NoResponseException
     * @throws XMPPErrorException
     * @throws NotConnectedException
     * @throws InterruptedException 
     */
    public <T extends Item> List<T> getItems(List<ExtensionElement> additionalExtensions,
                    List<ExtensionElement> returnedExtensions) throws NoResponseException,
                    XMPPErrorException, NotConnectedException, InterruptedException {
        PubSub request = createPubsubPacket(Type.get, new GetItemsRequest(getId()));
        request.addExtensions(additionalExtensions);
        return getItems(request, returnedExtensions);
    }

    private <T extends Item> List<T> getItems(PubSub request) throws NoResponseException,
                    XMPPErrorException, NotConnectedException, InterruptedException {
        return getItems(request, null);
    }

    @SuppressWarnings("unchecked")
    private <T extends Item> List<T> getItems(PubSub request,
                    List<ExtensionElement> returnedExtensions) throws NoResponseException,
                    XMPPErrorException, NotConnectedException, InterruptedException {
        PubSub result = pubSubManager.getConnection().createStanzaCollectorAndSend(request).nextResultOrThrow();
        ItemsExtension itemsElem = result.getExtension(PubSubElementType.ITEMS);
        if (returnedExtensions != null) {
            returnedExtensions.addAll(result.getExtensions());
        }
        return (List<T>) itemsElem.getItems();
    }

    /**
     * Publishes an event to the node.  This is an empty event
     * with no item.
     * 
     * This is only acceptable for nodes with {@link ConfigureForm#isPersistItems()}=false
     * and {@link ConfigureForm#isDeliverPayloads()}=false.
     * 
     * This is an asynchronous call which returns as soon as the 
     * stanza(/packet) has been sent.
     * 
     * For synchronous calls use {@link #send() send()}.
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    public void publish() throws NotConnectedException, InterruptedException
    {
        PubSub packet = createPubsubPacket(Type.set, new NodeExtension(PubSubElementType.PUBLISH, getId()));

        pubSubManager.getConnection().sendStanza(packet);
    }

    /**
     * Publishes an event to the node.  This is a simple item
     * with no payload.
     * 
     * If the id is null, an empty item (one without an id) will be sent.
     * Please note that this is not the same as {@link #send()}, which
     * publishes an event with NO item.
     * 
     * This is an asynchronous call which returns as soon as the 
     * stanza(/packet) has been sent.
     * 
     * For synchronous calls use {@link #send(Item) send(Item))}.
     * 
     * @param item - The item being sent
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    @SuppressWarnings("unchecked")
    public <T extends Item> void publish(T item) throws NotConnectedException, InterruptedException
    {
        Collection<T> items = new ArrayList<T>(1);
        items.add((T)(item == null ? new Item() : item));
        publish(items);
    }

    /**
     * Publishes multiple events to the node.  Same rules apply as in {@link #publish(Item)}.
     * 
     * In addition, if {@link ConfigureForm#isPersistItems()}=false, only the last item in the input
     * list will get stored on the node, assuming it stores the last sent item.
     * 
     * This is an asynchronous call which returns as soon as the 
     * stanza(/packet) has been sent.
     * 
     * For synchronous calls use {@link #send(Collection) send(Collection))}.
     * 
     * @param items - The collection of items being sent
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    public <T extends Item> void publish(Collection<T> items) throws NotConnectedException, InterruptedException
    {
        PubSub packet = createPubsubPacket(Type.set, new PublishItem<T>(getId(), items));

        pubSubManager.getConnection().sendStanza(packet);
    }

    /**
     * Publishes an event to the node.  This is an empty event
     * with no item.
     * 
     * This is only acceptable for nodes with {@link ConfigureForm#isPersistItems()}=false
     * and {@link ConfigureForm#isDeliverPayloads()}=false.
     * 
     * This is a synchronous call which will throw an exception 
     * on failure.
     * 
     * For asynchronous calls, use {@link #publish() publish()}.
     * @throws XMPPErrorException 
     * @throws NoResponseException 
     * @throws NotConnectedException 
     * @throws InterruptedException 
     * 
     */
    public void send() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException
    {
        PubSub packet = createPubsubPacket(Type.set, new NodeExtension(PubSubElementType.PUBLISH, getId()));

        pubSubManager.getConnection().createStanzaCollectorAndSend(packet).nextResultOrThrow();
    }

    /**
     * Publishes an event to the node.  This can be either a simple item
     * with no payload, or one with it.  This is determined by the Node
     * configuration.
     * 
     * If the node has <b>deliver_payload=false</b>, the Item must not
     * have a payload.
     * 
     * If the id is null, an empty item (one without an id) will be sent.
     * Please note that this is not the same as {@link #send()}, which
     * publishes an event with NO item.
     * 
     * This is a synchronous call which will throw an exception 
     * on failure.
     * 
     * For asynchronous calls, use {@link #publish(Item) publish(Item)}.
     * 
     * @param item - The item being sent
     * @throws XMPPErrorException 
     * @throws NoResponseException 
     * @throws NotConnectedException 
     * @throws InterruptedException 
     * 
     */
    @SuppressWarnings("unchecked")
    public <T extends Item> void send(T item) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException
    {
        Collection<T> items = new ArrayList<T>(1);
        items.add((item == null ? (T)new Item() : item));
        send(items);
    }

    /**
     * Publishes multiple events to the node.  Same rules apply as in {@link #send(Item)}.
     * 
     * In addition, if {@link ConfigureForm#isPersistItems()}=false, only the last item in the input
     * list will get stored on the node, assuming it stores the last sent item.
     *  
     * This is a synchronous call which will throw an exception 
     * on failure.
     * 
     * For asynchronous calls, use {@link #publish(Collection) publish(Collection))}.
     * 
     * @param items - The collection of {@link Item} objects being sent
     * @throws XMPPErrorException 
     * @throws NoResponseException 
     * @throws NotConnectedException 
     * @throws InterruptedException 
     * 
     */
    public <T extends Item> void send(Collection<T> items) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException
    {
        PubSub packet = createPubsubPacket(Type.set, new PublishItem<T>(getId(), items));

        pubSubManager.getConnection().createStanzaCollectorAndSend(packet).nextResultOrThrow();
    }

    /**
     * Purges the node of all items.
     *   
     * <p>Note: Some implementations may keep the last item
     * sent.
     * @throws XMPPErrorException 
     * @throws NoResponseException if there was no response from the server.
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    public void deleteAllItems() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException
    {
        PubSub request = createPubsubPacket(Type.set, new NodeExtension(PubSubElementType.PURGE_OWNER, getId()), PubSubElementType.PURGE_OWNER.getNamespace());

        pubSubManager.getConnection().createStanzaCollectorAndSend(request).nextResultOrThrow();
    }

    /**
     * Delete the item with the specified id from the node.
     * 
     * @param itemId The id of the item
     * @throws XMPPErrorException 
     * @throws NoResponseException 
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    public void deleteItem(String itemId) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException
    {
        Collection<String> items = new ArrayList<String>(1);
        items.add(itemId);
        deleteItem(items);
    }

    /**
     * Delete the items with the specified id's from the node.
     * 
     * @param itemIds The list of id's of items to delete
     * @throws XMPPErrorException
     * @throws NoResponseException if there was no response from the server.
     * @throws NotConnectedException 
     * @throws InterruptedException 
     */
    public void deleteItem(Collection<String> itemIds) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException
    {
        List<Item> items = new ArrayList<Item>(itemIds.size());

        for (String id : itemIds)
        {
            items.add(new Item(id));
        }
        PubSub request = createPubsubPacket(Type.set, new ItemsExtension(ItemsExtension.ItemsElementType.retract, getId(), items));
        pubSubManager.getConnection().createStanzaCollectorAndSend(request).nextResultOrThrow();
    }
}
