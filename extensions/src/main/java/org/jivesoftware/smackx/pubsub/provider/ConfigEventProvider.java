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

package org.jivesoftware.smackx.pubsub.provider;

import java.util.List;
import java.util.Map;

import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smackx.packet.DataForm;
import org.jivesoftware.smackx.provider.EmbeddedExtensionProvider;
import org.jivesoftware.smackx.pubsub.ConfigurationEvent;
import org.jivesoftware.smackx.pubsub.ConfigureForm;

/**
 * Parses the node configuration element out of the message event stanza from 
 * the server as specified in the <a href="http://xmpp.org/extensions/xep-0060.html#schemas-event">configuration schema</a>.
 * 
 * @author Robin Collier
 */
public class ConfigEventProvider extends EmbeddedExtensionProvider
{
	@Override
	protected PacketExtension createReturnExtension(String currentElement, String currentNamespace, Map<String, String> attMap, List<? extends PacketExtension> content)
	{
		if (content.size() == 0)
			return new ConfigurationEvent(attMap.get("node"));
		else
			return new ConfigurationEvent(attMap.get("node"), new ConfigureForm((DataForm)content.iterator().next()));
	}
}
