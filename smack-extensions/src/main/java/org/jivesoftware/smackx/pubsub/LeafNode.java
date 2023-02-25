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
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.XmlElement;

import org.jivesoftware.smackx.disco.packet.DiscoverItems;
import org.jivesoftware.smackx.pubsub.form.ConfigureForm;
import org.jivesoftware.smackx.pubsub.packet.PubSub;

/**
 * The main class for the majority of PubSub functionality. In general
 * almost all PubSub capabilities are related to the concept of a node.
 * All items are published to a node, and typically subscribed to by other
 * users.  These users then retrieve events based on this subscription.
 *
 * @author Robin Collier
 */
public class LeafNode extends Node {
    LeafNode(PubSubManager pubSubManager, String nodeId) {
        super(pubSubManager, nodeId);
    }

    /**
     * Get information on the items in the node in standard
     * {@link DiscoverItems} format.
     *
     * @return The item details in {@link DiscoverItems} format
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NoResponseException if there was no response from the server.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public DiscoverItems discoverItems() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        DiscoverItems items = new DiscoverItems();
        items.setTo(pubSubManager.getServiceJid());
        items.setNode(getId());
        return pubSubManager.getConnection().sendIqRequestAndWaitForResponse(items);
    }

    /**
     * Get the current items stored in the node.
     *
     * @param <T> type of the items.
     * @return List of {@link Item} in the node
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NoResponseException if there was no response from the server.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public <T extends Item> List<T> getItems() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        return getItems((List<XmlElement>) null, null);
    }

    /**
     * Get the current items stored in the node based
     * on the subscription associated with the provided
     * subscription id.
     *
     * @param subscriptionId -  The subscription id for the
     * associated subscription.
     * @param <T> type of the items.
     *
     * @return List of {@link Item} in the node
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NoResponseException if there was no response from the server.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public <T extends Item> List<T> getItems(String subscriptionId) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        PubSub request = createPubsubPacket(IQ.Type.get, new GetItemsRequest(getId(), subscriptionId));
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
     * @param <T> type of the items.
     *
     * @return The list of {@link Item} with payload
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NoResponseException if there was no response from the server.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public <T extends Item> List<T> getItems(Collection<String> ids) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        List<Item> itemList = new ArrayList<>(ids.size());

        for (String id : ids) {
            itemList.add(new Item(id));
        }
        PubSub request = createPubsubPacket(IQ.Type.get, new ItemsExtension(ItemsExtension.ItemsElementType.items, getId(), itemList));
        return getItems(request);
    }

    /**
     * Get items persisted on the node, limited to the specified number.
     *
     * @param maxItems Maximum number of items to return
     * @param <T> type of the items.
     *
     * @return List of {@link Item}
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NoResponseException if there was no response from the server.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public <T extends Item> List<T> getItems(int maxItems) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        PubSub request = createPubsubPacket(IQ.Type.get, new GetItemsRequest(getId(), maxItems));
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
     * @param <T> type of the items.
     *
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NoResponseException if there was no response from the server.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public <T extends Item> List<T> getItems(int maxItems, String subscriptionId) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        PubSub request = createPubsubPacket(IQ.Type.get, new GetItemsRequest(getId(), subscriptionId, maxItems));
        return getItems(request);
    }

    /**
     * Get items persisted on the node.
     * <p>
     * {@code additionalExtensions} can be used e.g. to add a "Result Set Management" extension.
     * {@code returnedExtensions} will be filled with the stanza extensions found in the answer.
     * </p>
     *
     * @param additionalExtensions additional {@code PacketExtensions} to be added to the request.
     *        This is an optional argument, if provided as null no extensions will be added.
     * @param returnedExtensions a collection that will be filled with the returned packet
     *        extensions. This is an optional argument, if provided as null it won't be populated.
     * @param <T> type of the items.
     *
     * @return List of {@link Item}
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public <T extends Item> List<T> getItems(List<XmlElement> additionalExtensions,
                    List<XmlElement> returnedExtensions) throws NoResponseException,
                    XMPPErrorException, NotConnectedException, InterruptedException {
        PubSub request = createPubsubPacket(IQ.Type.get, new GetItemsRequest(getId()));
        request.addExtensions(additionalExtensions);
        return getItems(request, returnedExtensions);
    }

    private <T extends Item> List<T> getItems(PubSub request) throws NoResponseException,
                    XMPPErrorException, NotConnectedException, InterruptedException {
        return getItems(request, null);
    }

    @SuppressWarnings("unchecked")
    private <T extends Item> List<T> getItems(PubSub request,
                    List<XmlElement> returnedExtensions) throws NoResponseException,
                    XMPPErrorException, NotConnectedException, InterruptedException {
        PubSub result = pubSubManager.getConnection().sendIqRequestAndWaitForResponse(request);
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
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NoResponseException if there was no response from the remote entity.
     * @deprecated use {@link #publish()} instead.
     */
    @Deprecated
    public void send() throws NotConnectedException, InterruptedException, NoResponseException, XMPPErrorException {
        publish();
    }

    /**
     * Publishes an event to the node.  This is a simple item
     * with no payload.
     *
     * If the id is null, an empty item (one without an id) will be sent.
     * Please note that this is not the same as {@link #send()}, which
     * publishes an event with NO item.
     *
     * @param item - The item being sent
     * @param <T> type of the items.
     *
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NoResponseException if there was no response from the remote entity.
     * @deprecated use {@link #publish(Item)} instead.
     */
    @Deprecated
    public <T extends Item> void send(T item) throws NotConnectedException, InterruptedException, NoResponseException, XMPPErrorException {
        publish(item);
    }

    /**
     * Publishes multiple events to the node.  Same rules apply as in {@link #publish(Item)}.
     *
     * In addition, if {@link ConfigureForm#isPersistItems()}=false, only the last item in the input
     * list will get stored on the node, assuming it stores the last sent item.
     *
     * @param items - The collection of items being sent
     * @param <T> type of the items.
     *
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NoResponseException if there was no response from the remote entity.
     * @deprecated use {@link #publish(Collection)} instead.
     */
    @Deprecated
    public <T extends Item> void send(Collection<T> items) throws NotConnectedException, InterruptedException, NoResponseException, XMPPErrorException {
        publish(items);
    }

    /**
     * Publishes an event to the node.  This is an empty event
     * with no item.
     *
     * This is only acceptable for nodes with {@link ConfigureForm#isPersistItems()}=false
     * and {@link ConfigureForm#isDeliverPayloads()}=false.
     *
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     *
     */
    public void publish() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        PubSub packet = createPubsubPacket(IQ.Type.set, new NodeExtension(PubSubElementType.PUBLISH, getId()));

        pubSubManager.getConnection().sendIqRequestAndWaitForResponse(packet);
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
     * @param item - The item being sent
     * @param <T> type of the items.
     *
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     *
     */
    @SuppressWarnings("unchecked")
    public <T extends Item> void publish(T item) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        Collection<T> items = new ArrayList<>(1);
        items.add(item == null ? (T) new Item() : item);
        publish(items);
    }

    /**
     * Publishes multiple events to the node.  Same rules apply as in {@link #send(Item)}.
     *
     * In addition, if {@link ConfigureForm#isPersistItems()}=false, only the last item in the input
     * list will get stored on the node, assuming it stores the last sent item.
     *
     * @param items - The collection of {@link Item} objects being sent
     * @param <T> type of the items.
     *
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     *
     */
    public <T extends Item> void publish(Collection<T> items) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        PubSub packet = createPubsubPacket(IQ.Type.set, new PublishItem<>(getId(), items));

        pubSubManager.getConnection().sendIqRequestAndWaitForResponse(packet);
    }

    /**
     * Purges the node of all items.
     *
     * <p>Note: Some implementations may keep the last item
     * sent.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NoResponseException if there was no response from the server.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public void deleteAllItems() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        PubSub request = createPubsubPacket(IQ.Type.set, new NodeExtension(PubSubElementType.PURGE_OWNER, getId()));

        pubSubManager.getConnection().sendIqRequestAndWaitForResponse(request);
    }

    /**
     * Delete the item with the specified id from the node.
     *
     * @param itemId The id of the item
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public void deleteItem(String itemId) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        Collection<String> items = new ArrayList<>(1);
        items.add(itemId);
        deleteItem(items);
    }

    /**
     * Delete the items with the specified id's from the node.
     *
     * @param itemIds The list of id's of items to delete
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws NoResponseException if there was no response from the server.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public void deleteItem(Collection<String> itemIds) throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        List<Item> items = new ArrayList<>(itemIds.size());

        for (String id : itemIds) {
             items.add(new Item(id));
        }
        PubSub request = createPubsubPacket(IQ.Type.set, new ItemsExtension(ItemsExtension.ItemsElementType.retract, getId(), items));
        pubSubManager.getConnection().sendIqRequestAndWaitForResponse(request);
    }
}
