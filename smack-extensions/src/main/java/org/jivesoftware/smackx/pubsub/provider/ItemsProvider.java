/**
 *
 * Copyright the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.provider.EmbeddedExtensionProvider;
import org.jivesoftware.smackx.pubsub.ItemsExtension;

/**
 * Parses the <b>items</b> element out of the message event stanza from 
 * the server as specified in the <a href="http://xmpp.org/extensions/xep-0060.html#schemas-event">items schema</a>.
 * 
 * @author Robin Collier
 */
public class ItemsProvider extends EmbeddedExtensionProvider<ItemsExtension>
{

	@Override
	protected ItemsExtension createReturnExtension(String currentElement, String currentNamespace, Map<String, String> attributeMap, List<? extends ExtensionElement> content)
	{
        return new ItemsExtension(ItemsExtension.ItemsElementType.items, attributeMap.get("node"), content);
	}

}
