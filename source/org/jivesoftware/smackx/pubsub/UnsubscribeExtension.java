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

import org.jivesoftware.smackx.pubsub.util.XmlUtils;


/**
 * Represents an unsubscribe element.
 * 
 * @author Robin Collier
 */
public class UnsubscribeExtension extends NodeExtension
{
	protected String jid;
	protected String id;
	
	public UnsubscribeExtension(String subscriptionJid)
	{
		this(subscriptionJid, null, null);
	}
	
	public UnsubscribeExtension(String subscriptionJid, String nodeId)
	{
		this(subscriptionJid, nodeId, null);
	}
	
	public UnsubscribeExtension(String jid, String nodeId, String subscriptionId)
	{
		super(PubSubElementType.UNSUBSCRIBE, nodeId);
		this.jid = jid;
		id = subscriptionId;
	}
	
	public String getJid()
	{
		return jid;
	}
	
	public String getId()
	{
		return id;
	}
	
	@Override
	public String toXML()
	{
		StringBuilder builder = new StringBuilder("<");
		builder.append(getElementName());
		XmlUtils.appendAttribute(builder, "jid", jid);
		
		if (getNode() != null)
			XmlUtils.appendAttribute(builder, "node", getNode());
		
		if (id != null)
			XmlUtils.appendAttribute(builder, "subid", id);
		
		builder.append("/>");
		return builder.toString();
	}

}
