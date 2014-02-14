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
package org.jivesoftware.smackx.pubsub.listener;

import org.jivesoftware.smackx.pubsub.Item;
import org.jivesoftware.smackx.pubsub.ItemPublishEvent;
import org.jivesoftware.smackx.pubsub.LeafNode;

/**
 * Defines the listener for items being published to a node.
 * 
 * @see LeafNode#addItemEventListener(ItemEventListener)
 *
 * @author Robin Collier
 */
public interface ItemEventListener <T extends Item> 
{
	/**
	 * Called whenever an item is published to the node the listener
	 * is registered with.
	 * 
	 * @param items The publishing details.
	 */
	void handlePublishedItems(ItemPublishEvent<T> items);
}
