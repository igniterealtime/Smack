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
package org.jivesoftware.smackx.pubsub;

import org.jivesoftware.smack.util.XmlStringBuilder;

/**
 * A packet extension representing the <b>options</b> element. 
 * 
 * @author Robin Collier
 */
public class OptionsExtension extends NodeExtension
{
	protected String jid;
	protected String id;
	
	public OptionsExtension(String subscriptionJid)
	{
		this(subscriptionJid, null, null);
	}
	
	public OptionsExtension(String subscriptionJid, String nodeId)
	{
		this(subscriptionJid, nodeId, null);
	}
	
	public OptionsExtension(String jid, String nodeId, String subscriptionId)
	{
		super(PubSubElementType.OPTIONS, nodeId);
		this.jid = jid;
		id = subscriptionId;
	}
	
	public String getJid()
	{
		return jid;
	}
	
	public String getId()
	{
		return id;
	}

    @Override
    public XmlStringBuilder toXML() {
        XmlStringBuilder xml = new XmlStringBuilder();
        xml.halfOpenElement(getElementName());
        xml.attribute("jid", jid);
        xml.optAttribute("node", getNode());
        xml.optAttribute("subid", id);
        xml.closeEmptyElement();
        return xml;
    }
}
