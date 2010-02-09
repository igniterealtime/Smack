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

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.packet.PacketExtension;

/**
 * Represents a affiliation between a user and a node, where the {@link #type} defines 
 * the type of affiliation.
 * 
 * Affiliations are retrieved from the {@link PubSubManager#getAffiliations()} method, which 
 * gets affiliations for the calling user, based on the identity that is associated with 
 * the {@link Connection}.
 * 
 * @author Robin Collier
 */
public class Affiliation implements PacketExtension
{
	protected String node;
	protected Type type;
	
	public enum Type
	{
		member, none, outcast, owner, publisher
	}

	/**
	 * Constructs an affiliation.
	 * 
	 * @param nodeId The node the user is affiliated with.
	 * @param affiliation The type of affiliation.
	 */
	public Affiliation(String nodeId, Type affiliation)
	{
		node = nodeId;
		type = affiliation;
	}
	
	public String getNodeId()
	{
		return node;
	}
	
	public Type getType()
	{
		return type;
	}
	
	public String getElementName()
	{
		return "subscription";
	}

	public String getNamespace()
	{
		return null;
	}

	public String toXML()
	{
		StringBuilder builder = new StringBuilder("<");
		builder.append(getElementName());
		appendAttribute(builder, "node", node);
		appendAttribute(builder, "affiliation", type.toString());
		
		builder.append("/>");
		return builder.toString();
	}

	private void appendAttribute(StringBuilder builder, String att, String value)
	{
		builder.append(" ");
		builder.append(att);
		builder.append("='");
		builder.append(value);
		builder.append("'");
	}
}
