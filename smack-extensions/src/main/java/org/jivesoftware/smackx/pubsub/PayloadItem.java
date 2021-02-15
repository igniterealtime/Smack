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

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.util.XmlStringBuilder;

import org.jivesoftware.smackx.pubsub.form.ConfigureForm;
import org.jivesoftware.smackx.pubsub.provider.ItemProvider;

/**
 * This class represents an item that has been, or will be published to a
 * pubsub node.  An <code>Item</code> has several properties that are dependent
 * on the configuration of the node to which it has been or will be published.
 *
 * <p>
 * <b>An Item received from a node (via {@link LeafNode#getItems()} or {@link LeafNode#addItemEventListener(org.jivesoftware.smackx.pubsub.listener.ItemEventListener)}</b>
 * </p>
 * <ul>
 * <li>Will always have an id (either user or server generated) unless node configuration has both
 * {@link ConfigureForm#isPersistItems()} and {@link ConfigureForm#isDeliverPayloads()}set to false.</li>
 * <li>Will have a payload if the node configuration has {@link ConfigureForm#isDeliverPayloads()} set
 * to true, otherwise it will be null.</li>
 * </ul>
 *
 * <p>
 * <b>An Item created to send to a node (via {@link LeafNode#publish()}</b>
 * </p>
 * <ul>
 * <li>The id is optional, since the server will generate one if necessary, but should be used if it is
 * meaningful in the context of the node.  This value must be unique within the node that it is sent to, since
 * resending an item with the same id will overwrite the one that already exists if the items are persisted.</li>
 * <li>Will require payload if the node configuration has {@link ConfigureForm#isDeliverPayloads()} set
 * to true.</li>
 * </ul>
 *
 *
 * <p>To customise the payload object being returned from the {@link #getPayload()} method, you can
 * add a custom parser as explained in {@link ItemProvider}.</p>
 *
 * @author Robin Collier
 */
public class PayloadItem<E extends ExtensionElement> extends Item {
    private final E payload;

    /**
     * Create an <code>Item</code> with no id and a payload  The id will be set by the server.
     *
     * @param payloadExt A {@link ExtensionElement} which represents the payload data.
     */
    public PayloadItem(E payloadExt) {
        super();

        if (payloadExt == null)
            throw new IllegalArgumentException("payload cannot be 'null'");
        payload = payloadExt;
    }

    /**
     * Create an <code>Item</code> with an id and payload.
     *
     * @param itemId The id of this item.  It can be null if we want the server to set the id.
     * @param payloadExt A {@link ExtensionElement} which represents the payload data.
     */
    public PayloadItem(String itemId, E payloadExt) {
        super(itemId);

        if (payloadExt == null)
            throw new IllegalArgumentException("payload cannot be 'null'");
        payload = payloadExt;
    }

    /**
     * Create an <code>Item</code> with an id, node id and payload.
     *
     * <p>
     * <b>Note:</b> This is not valid for publishing an item to a node, only receiving from
     * one as part of {@link Message}.  If used to create an Item to publish
     * (via {@link LeafNode#publish(Item)}, the server <i>may</i> return an
     * error for an invalid packet.
     * </p>
     *
     * @param itemId The id of this item.
     * @param nodeId The id of the node the item was published to.
     * @param payloadExt A {@link ExtensionElement} which represents the payload data.
     */
    public PayloadItem(String itemId, String nodeId, E payloadExt) {
        this(ItemNamespace.pubsub, itemId, nodeId, payloadExt);
    }

    /**
     * Create an <code>Item</code> with an id, node id and payload.
     *
     * <p>
     * <b>Note:</b> This is not valid for publishing an item to a node, only receiving from
     * one as part of {@link Message}.  If used to create an Item to publish
     * (via {@link LeafNode#publish(Item)}, the server <i>may</i> return an
     * error for an invalid packet.
     * </p>
     *
     * @param itemNamespace the namespace of the item.
     * @param itemId The id of this item.
     * @param nodeId The id of the node the item was published to.
     * @param payloadExt A {@link ExtensionElement} which represents the payload data.
     */
    public PayloadItem(ItemNamespace itemNamespace, String itemId, String nodeId, E payloadExt) {
        super(itemNamespace, itemId, nodeId);

        if (payloadExt == null)
            throw new IllegalArgumentException("payload cannot be 'null'");
        payload = payloadExt;
    }

    /**
     * Get the payload associated with this <code>Item</code>.  Customising the payload
     * parsing from the server can be accomplished as described in {@link ItemProvider}.
     *
     * @return The payload as a {@link ExtensionElement}.
     */
    public E getPayload() {
        return payload;
    }

    @Override
    protected void addXml(XmlStringBuilder xml) {
        xml.optAttribute("id", getId());
        xml.rightAngleBracket();
        xml.append(payload);
        xml.closeElement(this);
    }

    @Override
    public String toString() {
        return getClass().getName() + " | Content [" + toXML() + "]";
    }
}
