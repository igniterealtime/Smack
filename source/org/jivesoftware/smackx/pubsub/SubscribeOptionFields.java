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

import java.util.Calendar;

/**
 * Defines the possible field options for a subscribe options form as defined 
 * by <a href="http://xmpp.org/extensions/xep-0060.html#registrar-formtypes-subscribe">Section 16.4.2</a>.
 * 
 * @author Robin Collier
 */
public enum SubscribeOptionFields
{
	/**
	 * Whether an entity wants to receive or disable notifications
	 * 
	 * <p><b>Value: boolean</b></p>
	 */
	deliver,

	/**
	 * Whether an entity wants to receive digests (aggregations) of 
	 * notifications or all notifications individually.
	 * 
	 * <p><b>Value: boolean</b></p>
	 */
	digest,
	
	/**
	 * The minimum number of seconds between sending any two notifications digests
	 * 
	 * <p><b>Value: int</b></p>
	 */
	digest_frequency,

	/**
	 * The DateTime at which a leased subsscription will end ro has ended.
	 * 
	 * <p><b>Value: {@link Calendar}</b></p>
	 */
	expire,

	/**
	 * Whether an entity wants to receive an XMPP message body in addition to 
	 * the payload format.
	 *
	 * <p><b>Value: boolean</b></p>
	 */
	include_body,
	
	/**
	 * The presence states for which an entity wants to receive notifications.
	 *
	 * <p><b>Value: {@link PresenceState}</b></p>
	 */
	show_values,
	
	/**
	 * 
	 * 
	 * <p><b>Value: </b></p>
	 */
	subscription_type,
	
	/**
	 * 
	 * <p><b>Value: </b></p>
	 */
	subscription_depth;
	
	public String getFieldName()
	{
		if (this == show_values)
			return "pubsub#" + toString().replace('_', '-');
		return "pubsub#" + toString();
	}
	
	static public SubscribeOptionFields valueOfFromElement(String elementName)
	{
		String portion = elementName.substring(elementName.lastIndexOf('#' + 1));
		
		if ("show-values".equals(portion))
			return show_values;
		else
			return valueOf(portion);
	}
}
