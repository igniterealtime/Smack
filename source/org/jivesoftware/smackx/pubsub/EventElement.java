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

import java.util.Arrays;
import java.util.List;

import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smackx.pubsub.packet.PubSubNamespace;

/**
 * Represents the top level element of a pubsub event extension.  All types of pubsub events are
 * represented by this class.  The specific type can be found by {@link #getEventType()}.  The 
 * embedded event information, which is specific to the event type, can be retrieved by the {@link #getEvent()}
 * method.
 * 
 * @author Robin Collier
 */
public class EventElement implements EmbeddedPacketExtension
{
	private EventElementType type;
	private NodeExtension ext;
	
	public EventElement(EventElementType eventType, NodeExtension eventExt)
	{
		type = eventType;
		ext = eventExt;
	}
	
	public EventElementType getEventType()
	{
		return type;
	}

	public List<PacketExtension> getExtensions()
	{
		return Arrays.asList(new PacketExtension[]{getEvent()});
	}

	public NodeExtension getEvent()
	{
		return ext;
	}

	public String getElementName()
	{
		return "event";
	}

	public String getNamespace()
	{
		return PubSubNamespace.EVENT.getXmlns();
	}

	public String toXML()
	{
		StringBuilder builder = new StringBuilder("<event xmlns='" + PubSubNamespace.EVENT.getXmlns() + "'>");

		builder.append(ext.toXML());
		builder.append("</event>");
		return builder.toString();
	}
}
