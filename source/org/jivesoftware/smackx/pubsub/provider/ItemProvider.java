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

import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smackx.pubsub.Item;
import org.jivesoftware.smackx.pubsub.PayloadItem;
import org.jivesoftware.smackx.pubsub.SimplePayload;
import org.jivesoftware.smackx.pubsub.packet.PubSubNamespace;
import org.xmlpull.v1.XmlPullParser;

/**
 * Parses an <b>item</b> element as is defined in both the {@link PubSubNamespace#BASIC} and {@link PubSubNamespace#EVENT}
 * namespaces.  To parse the item contents, it will use whatever {@link PacketExtensionProvider} is registered in 
 * <b>smack.providers</b> for its element name and namespace.  If no provider is registered, it will return a {@link SimplePayload}.
 * 
 * @author Robin Collier
 */
public class ItemProvider implements PacketExtensionProvider
{
	public PacketExtension parseExtension(XmlPullParser parser) throws Exception
	{
		String id = parser.getAttributeValue(null, "id");
		String elem = parser.getName();
		
		int tag = parser.next();
		
		if (tag == XmlPullParser.END_TAG)
		{
			return new Item(id);		
		}
		else
		{
			String payloadElemName = parser.getName();
			String payloadNS = parser.getNamespace();
			
	        if (ProviderManager.getInstance().getExtensionProvider(payloadElemName, payloadNS) == null)
	        {
	    		boolean done = false;
	    		StringBuilder payloadText = new StringBuilder();
	    		
	    		while (!done)
	    		{
	    			if (tag == XmlPullParser.END_TAG && parser.getName().equals(elem))
	    				done = true;
	    			else if (!((tag == XmlPullParser.START_TAG) && parser.isEmptyElementTag()))
    					payloadText.append(parser.getText());
	    			
	    			if (!done)
	    				tag = parser.next();
	    		}
	    		return new PayloadItem<SimplePayload>(id, new SimplePayload(payloadElemName, payloadNS, payloadText.toString()));
	        }
	        else
	        {
	        	return new PayloadItem<PacketExtension>(id, PacketParserUtils.parsePacketExtension(payloadElemName, payloadNS, parser));
	        }
		}
	}

}
