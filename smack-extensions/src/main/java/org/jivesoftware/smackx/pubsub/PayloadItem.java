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

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smackx.pubsub.provider.ItemProvider;

/**
 * This class represents an item that has been, or will be published to a
 * pubsub node.  An <tt>Item</tt> has several properties that are dependent
 * on the configuration of the node to which it has been or will be published.
 * 
 * <h3>An Item received from a node (via {@link LeafNode#getItems()} or {@link LeafNode#addItemEventListener(org.jivesoftware.smackx.pubsub.listener.ItemEventListener)}</h3>
 * <ul>
 * <li>Will always have an id (either user or server generated) unless node configuration has both
 * {@link ConfigureForm#isPersistItems()} and {@link ConfigureForm#isDeliverPayloads()}set to false.</li>
 * <li>Will have a payload if the node configuration has {@link ConfigureForm#isDeliverPayloads()} set 
 * to true, otherwise it will be null.</li>
 * </ul>
 * 
 * <h3>An Item created to send to a node (via {@link LeafNode#send()} or {@link LeafNode#publish()}</h3>
 * <ul>
 * <li>The id is optional, since the server will generate one if necessary, but should be used if it is 
 * meaningful in the context of the node.  This value must be unique within the node that it is sent to, since
 * resending an item with the same id will overwrite the one that already exists if the items are persisted.</li>
 * <li>Will require payload if the node configuration has {@link ConfigureForm#isDeliverPayloads()} set
 * to true.</li>
 * </ul>
 * 
 * <p>To customise the payload object being returned from the {@link #getPayload()} method, you can
 * add a custom parser as explained in {@link ItemProvider}.</p>
 * 
 * @author Robin Collier
 */
public class PayloadItem<E extends ExtensionElement> extends Item
{
	private E payload;

	/**
	 * Create an <tt>Item</tt> with no id and a payload  The id will be set by the server.  
	 * 
	 * @param payloadExt A {@link ExtensionElement} which represents the payload data.
	 */
	public PayloadItem(E payloadExt)
	{
		super();

		if (payloadExt == null)
			throw new IllegalArgumentException("payload cannot be 'null'");
		payload = payloadExt;
	}

	/**
	 * Create an <tt>Item</tt> with an id and payload.  
	 * 
	 * @param itemId The id of this item.  It can be null if we want the server to set the id.
	 * @param payloadExt A {@link ExtensionElement} which represents the payload data.
	 */
	public PayloadItem(String itemId, E payloadExt)
	{
		super(itemId);

		if (payloadExt == null)
			throw new IllegalArgumentException("payload cannot be 'null'");
		payload = payloadExt;
	}

	/**
	 * Create an <tt>Item</tt> with an id, node id and payload.  
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
	public PayloadItem(String itemId, String nodeId, E payloadExt)
	{
		super(itemId, nodeId);

		if (payloadExt == null)
			throw new IllegalArgumentException("payload cannot be 'null'");
		payload = payloadExt;
	}

	/**
	 * Get the payload associated with this <tt>Item</tt>.  Customising the payload
	 * parsing from the server can be accomplished as described in {@link ItemProvider}.
	 * 
	 * @return The payload as a {@link ExtensionElement}.
	 */
	public E getPayload()
	{
		return payload;
	}

	@Override
	public String toXML()
	{
		StringBuilder builder = new StringBuilder("<item");

		if (getId() != null)
		{
			builder.append(" id='");
			builder.append(getId());
			builder.append('\'');
		}

        if (getNode() != null) {
            builder.append(" node='");
            builder.append(getNode());
            builder.append('\'');
        }
		builder.append('>');
		builder.append(payload.toXML());
		builder.append("</item>");

		return builder.toString();
	}

	@Override
	public String toString()
	{
		return getClass().getName() + " | Content [" + toXML() + "]";
	}
}
