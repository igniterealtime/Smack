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

/**
 * Represents a request to subscribe to a node.
 * 
 * @author Robin Collier
 */
public class GetItemsRequest extends NodeExtension
{
	protected String subId;
	protected int maxItems;
	
	public GetItemsRequest(String nodeId)
	{
		super(PubSubElementType.ITEMS, nodeId);
	}
	
	public GetItemsRequest(String nodeId, String subscriptionId)
	{
		super(PubSubElementType.ITEMS, nodeId);
		subId = subscriptionId;
	}

	public GetItemsRequest(String nodeId, int maxItemsToReturn)
	{
		super(PubSubElementType.ITEMS, nodeId);
		maxItems = maxItemsToReturn;
	}

	public GetItemsRequest(String nodeId, String subscriptionId, int maxItemsToReturn)
	{
		this(nodeId, maxItemsToReturn);
		subId = subscriptionId;
	}

	public String getSubscriptionId()
	{
		return subId;
	}

	public int getMaxItems()
	{
		return maxItems;
	}

	@Override
	public String toXML()
	{
		StringBuilder builder = new StringBuilder("<");
		builder.append(getElementName());
		
		builder.append(" node='");
		builder.append(getNode());
		builder.append("'");

		if (getSubscriptionId() != null)
		{
			builder.append(" subid='");
			builder.append(getSubscriptionId());
			builder.append("'");
		}

		if (getMaxItems() > 0)
		{
			builder.append(" max_items='");
			builder.append(getMaxItems());
			builder.append("'");
		}
		builder.append("/>");
		return builder.toString();
	}
}
