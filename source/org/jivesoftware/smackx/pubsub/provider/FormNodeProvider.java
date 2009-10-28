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
import org.jivesoftware.smackx.Form;
import org.jivesoftware.smackx.packet.DataForm;
import org.jivesoftware.smackx.provider.EmbeddedExtensionProvider;
import org.jivesoftware.smackx.pubsub.FormNode;
import org.jivesoftware.smackx.pubsub.FormNodeType;

/**
 * Parses one of several elements used in pubsub that contain a form of some kind as a child element.  The
 * elements and namespaces supported is defined in {@link FormNodeType}.
 * 
 * @author Robin Collier
 */
public class FormNodeProvider extends EmbeddedExtensionProvider
{
	@Override
	protected PacketExtension createReturnExtension(String currentElement, String currentNamespace, Map<String, String> attributeMap, List<? extends PacketExtension> content)
	{
        return new FormNode(FormNodeType.valueOfFromElementName(currentElement, currentNamespace), attributeMap.get("node"), new Form((DataForm)content.iterator().next()));
	}
}
