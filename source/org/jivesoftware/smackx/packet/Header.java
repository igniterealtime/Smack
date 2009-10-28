/*
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

import org.jivesoftware.smack.packet.PacketExtension;

/**
 * Represents a <b>Header</b> entry as specified by the <a href="http://xmpp.org/extensions/xep-031.html">Stanza Headers and Internet Metadata (SHIM)</a>

 * @author Robin Collier
 */
public class Header implements PacketExtension
{
	private String name;
	private String value;
	
	public Header(String name, String value)
	{
		this.name = name;
		this.value = value;
	}
	
	public String getName()
	{
		return name;
	}

	public String getValue()
	{
		return value;
	}

	public String getElementName()
	{
		return "header";
	}

	public String getNamespace()
	{
		return HeadersExtension.NAMESPACE;
	}

	public String toXML()
	{
		return "<header name='" + name + "'>" + value + "</header>";
	}

}
