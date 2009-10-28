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

import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smackx.pubsub.packet.PubSubNamespace;

/**
 * Represents and item that has been deleted from a node.
 * 
 * @author Robin Collier
 */
public class RetractItem implements PacketExtension
{
	private String id;

	/**
	 * Construct a <tt>RetractItem</tt> with the specified id.
	 * 
	 * @param itemId The id if the item deleted
	 */
	public RetractItem(String itemId)
	{
		if (itemId == null)
			throw new IllegalArgumentException("itemId must not be 'null'");
		id = itemId;
	}
	
	public String getId()
	{
		return id;
	}

	public String getElementName()
	{
		return "retract";
	}

	public String getNamespace()
	{
		return PubSubNamespace.EVENT.getXmlns();
	}

	public String toXML()
	{
		return "<retract id='" + id + "'/>";
	}
}
