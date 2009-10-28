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
package org.jivesoftware.smackx.pubsub.util;

import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smackx.Form;
import org.jivesoftware.smackx.pubsub.ConfigureForm;
import org.jivesoftware.smackx.pubsub.FormNode;
import org.jivesoftware.smackx.pubsub.PubSubElementType;

/**
 * Utility for extracting information from packets.
 * 
 * @author Robin Collier
 */
public class NodeUtils
{
	/** 
	 * Get a {@link ConfigureForm} from a packet.
	 * 
	 * @param packet
	 * @param elem
	 * @return The configuration form
	 */
	public static ConfigureForm getFormFromPacket(Packet packet, PubSubElementType elem)
	{
		FormNode config = (FormNode)packet.getExtension(elem.getElementName(), elem.getNamespace().getXmlns());
		Form formReply = config.getForm();
		return new ConfigureForm(formReply);

	}
}
