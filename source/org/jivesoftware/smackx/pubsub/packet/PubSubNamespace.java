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
package org.jivesoftware.smackx.pubsub.packet;

/**
 * Defines all the valid namespaces that are used with the {@link PubSub} packet
 * as defined by the specification.
 * 
 * @author Robin Collier
 */
public enum PubSubNamespace
{
	BASIC(null),
	ERROR("errors"),
	EVENT("event"),
	OWNER("owner");
	
	private String fragment;
	
	private PubSubNamespace(String fragment)
	{
		this.fragment = fragment;
	}
	
	public String getXmlns()
	{
		String ns = "http://jabber.org/protocol/pubsub";
		
		if (fragment != null)
			ns += '#' + fragment;
		
		return ns;
	}
	
	public String getFragment()
	{
		return fragment;
	}

	public static PubSubNamespace valueOfFromXmlns(String ns)
	{
		int index = ns.lastIndexOf('#');
		
		if (index != -1)
		{
			String suffix = ns.substring(ns.lastIndexOf('#')+1);
			return valueOf(suffix.toUpperCase());
		}
		else
			return BASIC;
	}
}
