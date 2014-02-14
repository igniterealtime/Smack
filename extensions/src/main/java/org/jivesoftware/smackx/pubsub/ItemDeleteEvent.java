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

import java.util.Collections;
import java.util.List;

/**
 * Represents an event in which items have been deleted from the node.
 * 
 * @author Robin Collier
 */
public class ItemDeleteEvent extends SubscriptionEvent
{
	private List<String> itemIds = Collections.EMPTY_LIST;
	
	/**
	 * Constructs an <tt>ItemDeleteEvent</tt> that indicates the the supplied
	 * items (by id) have been deleted, and that the event matches the listed
	 * subscriptions.  The subscriptions would have been created by calling 
	 * {@link LeafNode#subscribe(String)}.
	 * 
	 * @param nodeId The id of the node the event came from
	 * @param deletedItemIds The item ids of the items that were deleted.
	 * @param subscriptionIds The subscriptions that match the event.
	 */
	public ItemDeleteEvent(String nodeId, List<String> deletedItemIds, List<String> subscriptionIds)
	{
		super(nodeId, subscriptionIds);
		
		if (deletedItemIds == null)
			throw new IllegalArgumentException("deletedItemIds cannot be null");
		itemIds = deletedItemIds;
	}
	
	/**
	 * Get the item id's of the items that have been deleted.
	 * 
	 * @return List of item id's
	 */
	public List<String> getItemIds()
	{
		return Collections.unmodifiableList(itemIds);
	}
	
	@Override
	public String toString()
	{
		return getClass().getName() + "  [subscriptions: " + getSubscriptions() + "], [Deleted Items: " + itemIds + ']';
	}
}
