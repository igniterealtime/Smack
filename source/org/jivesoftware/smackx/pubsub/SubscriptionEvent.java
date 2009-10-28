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
 * Base class to represents events that are associated to subscriptions.
 * 
 * @author Robin Collier
 */
abstract public class SubscriptionEvent extends NodeEvent
{
	private List<String> subIds = Collections.EMPTY_LIST;

	/**
	 * Construct an event with no subscription id's.  This can 
	 * occur when there is only one subscription to a node.  The
	 * event may or may not report the subscription id along 
	 * with the event.
	 * 
	 * @param nodeId The id of the node the event came from
	 */
	protected SubscriptionEvent(String nodeId)
	{
		super(nodeId);
	}

	/**
	 * Construct an event with multiple subscriptions.
	 * 
	 * @param nodeId The id of the node the event came from
	 * @param subscriptionIds The list of subscription id's
	 */
	protected SubscriptionEvent(String nodeId, List<String> subscriptionIds)
	{
		super(nodeId);
		
		if (subscriptionIds != null)
			subIds = subscriptionIds;
	}

	/** 
	 * Get the subscriptions this event is associated with.
	 * 
	 * @return List of subscription id's
	 */
	public List<String> getSubscriptions()
	{
		return Collections.unmodifiableList(subIds);
	}
	
	/**
	 * Set the list of subscription id's for this event.
	 * 
	 * @param subscriptionIds The list of subscription id's
	 */
	protected void setSubscriptions(List<String> subscriptionIds)
	{
		if (subscriptionIds != null)
			subIds = subscriptionIds;
	}
}
