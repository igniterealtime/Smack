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

/**
 * A class which represents a common element within the pubsub defined
 * schemas.  One which has a <b>node</b> as an attribute.  This class is 
 * used on its own as well as a base class for many others, since the 
 * node is a central concept to most pubsub functionality.
 * 
 * @author Robin Collier
 */
public class NodeExtension implements PacketExtension
{
	private PubSubElementType element;
	private String node;
	
	/**
	 * Constructs a <tt>NodeExtension</tt> with an element name specified
	 * by {@link PubSubElementType} and the specified node id.
	 * 
	 * @param elem Defines the element name and namespace
	 * @param nodeId Specifies the id of the node
	 */
	public NodeExtension(PubSubElementType elem, String nodeId)
	{
		element = elem;
		this.node = nodeId;
	}

	/**
	 * Constructs a <tt>NodeExtension</tt> with an element name specified
	 * by {@link PubSubElementType}.
	 * 
	 * @param elem Defines the element name and namespace
	 */
	public NodeExtension(PubSubElementType elem)
	{
		this(elem, null);
	}

	/**
	 * Gets the node id
	 * 
	 * @return The node id
	 */
	public String getNode()
	{
		return node;
	}
	
	public String getElementName()
	{
		return element.getElementName();
	}

	public String getNamespace()
	{
		return element.getNamespace().getXmlns();
	}

	public String toXML()
	{
		return '<' + getElementName() + (node == null ? "" : " node='" + node + '\'') + "/>";
	}

	@Override
	public String toString()
	{
		return getClass().getName() + " - content [" + toXML() + "]";
	}
}
