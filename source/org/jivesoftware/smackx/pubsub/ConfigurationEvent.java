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
import java.util.Collections;
import java.util.List;

import org.jivesoftware.smack.packet.PacketExtension;

/**
 * Represents the <b>configuration</b> element of a pubsub message event which 
 * associates a configuration form to the node which was configured.  The form 
 * contains the current node configuration.
 *  
 * @author Robin Collier
 */
public class ConfigurationEvent extends NodeExtension implements EmbeddedPacketExtension
{
	private ConfigureForm form;
	
	public ConfigurationEvent(String nodeId)
	{
		super(PubSubElementType.CONFIGURATION, nodeId);
	}
	
	public ConfigurationEvent(String nodeId, ConfigureForm configForm)
	{
		super(PubSubElementType.CONFIGURATION, nodeId);
		form = configForm;
	}
	
	public ConfigureForm getConfiguration()
	{
		return form;
	}

	public List<PacketExtension> getExtensions()
	{
		if (getConfiguration() == null)
			return Collections.EMPTY_LIST;
		else
			return Arrays.asList(((PacketExtension)getConfiguration().getDataFormToSend()));
	}
}
