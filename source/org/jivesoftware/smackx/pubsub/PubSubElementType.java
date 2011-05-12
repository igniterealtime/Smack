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

import org.jivesoftware.smackx.pubsub.packet.PubSubNamespace;

/**
 * Defines all the possible element types as defined for all the pubsub
 * schemas in all 3 namespaces.
 * 
 * @author Robin Collier
 */
public enum PubSubElementType
{
	CREATE("create", PubSubNamespace.BASIC),
	DELETE("delete", PubSubNamespace.OWNER),
	DELETE_EVENT("delete", PubSubNamespace.EVENT),
	CONFIGURE("configure", PubSubNamespace.BASIC),
	CONFIGURE_OWNER("configure", PubSubNamespace.OWNER),
	CONFIGURATION("configuration", PubSubNamespace.EVENT),
	OPTIONS("options", PubSubNamespace.BASIC),
	DEFAULT("default", PubSubNamespace.OWNER),	
	ITEMS("items", PubSubNamespace.BASIC),
	ITEMS_EVENT("items", PubSubNamespace.EVENT),
	ITEM("item", PubSubNamespace.BASIC),
	ITEM_EVENT("item", PubSubNamespace.EVENT),
	PUBLISH("publish", PubSubNamespace.BASIC),
	PUBLISH_OPTIONS("publish-options", PubSubNamespace.BASIC), 
	PURGE_OWNER("purge", PubSubNamespace.OWNER),
	PURGE_EVENT("purge", PubSubNamespace.EVENT),
	RETRACT("retract", PubSubNamespace.BASIC), 
	AFFILIATIONS("affiliations", PubSubNamespace.BASIC), 
	SUBSCRIBE("subscribe", PubSubNamespace.BASIC), 
	SUBSCRIPTION("subscription", PubSubNamespace.BASIC),
	SUBSCRIPTIONS("subscriptions", PubSubNamespace.BASIC), 
	UNSUBSCRIBE("unsubscribe", PubSubNamespace.BASIC);

	private String eName;
	private PubSubNamespace nSpace;
	
	private PubSubElementType(String elemName, PubSubNamespace ns)
	{
		eName = elemName;
		nSpace = ns;
	}
	
	public PubSubNamespace getNamespace()
	{
		return nSpace;
	}
	
	public String getElementName()
	{
		return eName;
	}
	
	public static PubSubElementType valueOfFromElemName(String elemName, String namespace)
	{
		int index = namespace.lastIndexOf('#');
		String fragment = (index == -1 ? null : namespace.substring(index+1));
		
		if (fragment != null)
		{
			return valueOf((elemName + '_' + fragment).toUpperCase());
		}
		return valueOf(elemName.toUpperCase().replace('-', '_'));
	}

}
