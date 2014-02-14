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

/**
 * Represents a request to subscribe to a node.
 * 
 * @author Robin Collier
 */
public class SubscribeExtension extends NodeExtension
{
	protected String jid;
	
	public SubscribeExtension(String subscribeJid)
	{
		super(PubSubElementType.SUBSCRIBE);
		jid = subscribeJid;
	}
	
	public SubscribeExtension(String subscribeJid, String nodeId)
	{
		super(PubSubElementType.SUBSCRIBE, nodeId);
		jid = subscribeJid;
	}

	public String getJid()
	{
		return jid;
	}

	@Override
	public String toXML()
	{
		StringBuilder builder = new StringBuilder("<");
		builder.append(getElementName());
		
		if (getNode() != null)
		{
			builder.append(" node='");
			builder.append(getNode());
			builder.append("'");
		}
		builder.append(" jid='");
		builder.append(getJid());
		builder.append("'/>");
		
		return builder.toString();
	}
}
