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
import org.jivesoftware.smackx.pubsub.Subscription;
import org.xmlpull.v1.XmlPullParser;

/**
 * Parses the <b>subscription</b> element out of the pubsub IQ message from 
 * the server as specified in the <a href="http://xmpp.org/extensions/xep-0060.html#schemas-pubsub">subscription schema</a>.
 * 
 * @author Robin Collier
 */
public class SubscriptionProvider implements PacketExtensionProvider
{
	public PacketExtension parseExtension(XmlPullParser parser) throws Exception
	{
		String jid = parser.getAttributeValue(null, "jid");
		String nodeId = parser.getAttributeValue(null, "node");
		String subId = parser.getAttributeValue(null, "subid");
		String state = parser.getAttributeValue(null, "subscription");
		boolean isRequired = false;

		int tag = parser.next();
		
		if ((tag == XmlPullParser.START_TAG) && parser.getName().equals("subscribe-options"))
		{
			tag = parser.next();
			
			if ((tag == XmlPullParser.START_TAG) && parser.getName().equals("required"))
				isRequired = true;
			
			while (parser.next() != XmlPullParser.END_TAG && parser.getName() != "subscribe-options");
		}
		while (parser.getEventType() != XmlPullParser.END_TAG) parser.next();
		return new Subscription(jid, nodeId, subId, (state == null ? null : Subscription.State.valueOf(state)), isRequired);
	}

}
