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

import java.util.ArrayList;
import java.util.Collection;

/**
 * Represents a request to publish an item(s) to a specific node.
 * 
 * @author Robin Collier
 */
public class PublishItem <T extends Item> extends NodeExtension
{
	protected Collection<T> items;
	
	/**
	 * Construct a request to publish an item to a node.
	 * 
	 * @param nodeId The node to publish to
	 * @param toPublish The {@link Item} to publish
	 */
	public PublishItem(String nodeId, T toPublish)
	{
		super(PubSubElementType.PUBLISH, nodeId);
		items = new ArrayList<T>(1);
		items.add(toPublish);
	}

	/**
	 * Construct a request to publish multiple items to a node.
	 * 
	 * @param nodeId The node to publish to
	 * @param toPublish The list of {@link Item} to publish
	 */
	public PublishItem(String nodeId, Collection<T> toPublish)
	{
		super(PubSubElementType.PUBLISH, nodeId);
		items = toPublish;
	}

	@Override
	public String toXML()
	{
		StringBuilder builder = new StringBuilder("<");
		builder.append(getElementName());
		builder.append(" node='");
		builder.append(getNode());
		builder.append("'>");
		
		for (Item item : items)
		{
			builder.append(item.toXML());
		}
		builder.append("</publish>");
		
		return builder.toString();
	}
}
