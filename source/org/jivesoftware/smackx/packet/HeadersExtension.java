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

package org.jivesoftware.smackx.packet;

import java.util.Collection;
import java.util.Collections;

import org.jivesoftware.smack.packet.PacketExtension;

/**
 * Extension representing a list of headers as specified in <a href="http://xmpp.org/extensions/xep-0131">Stanza Headers and Internet Metadata (SHIM)</a>
 * 
 * @see Header
 * 
 * @author Robin Collier
 */
public class HeadersExtension implements PacketExtension
{
	public static final String NAMESPACE = "http://jabber.org/protocol/shim";
	
	private Collection<Header> headers = Collections.EMPTY_LIST;
	
	public HeadersExtension(Collection<Header> headerList)
	{
		if (headerList != null)
			headers = headerList;
	}
	
	public Collection<Header> getHeaders()
	{
		return headers;
	}

	public String getElementName()
	{
		return "headers";
	}

	public String getNamespace()
	{
		return NAMESPACE;
	}

	public String toXML()
	{
		StringBuilder builder = new StringBuilder("<" + getElementName() + " xmlns='" + getNamespace() + "'>");
		
		for (Header header : headers)
		{
			builder.append(header.toXML());
		}
		builder.append("</" + getElementName() + '>');

		return builder.toString();
	}

}
