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
 * Parses an <b>item</b> element as is defined in both the {@link PubSubNamespace#BASIC} and
 * {@link PubSubNamespace#EVENT} namespaces. To parse the item contents, it will use whatever
 * {@link PacketExtensionProvider} is registered in <b>smack.providers</b> for its element name and namespace. If no
 * provider is registered, it will return a {@link SimplePayload}.
 * 
 * @author Robin Collier
 */
public class ItemProvider implements PacketExtensionProvider 
{
    public PacketExtension parseExtension(XmlPullParser parser) throws Exception 
    {
        String id = parser.getAttributeValue(null, "id");
        String node = parser.getAttributeValue(null, "node");
        String elem = parser.getName();

        int tag = parser.next();

        if (tag == XmlPullParser.END_TAG) 
        {
            return new Item(id, node);
        }
        else 
        {
            String payloadElemName = parser.getName();
            String payloadNS = parser.getNamespace();

            if (ProviderManager.getInstance().getExtensionProvider(payloadElemName, payloadNS) == null) 
            {
                boolean done = false;
                boolean isEmptyElement = false;
                StringBuilder payloadText = new StringBuilder();

                while (!done) 
                {
                    if (tag == XmlPullParser.END_TAG && parser.getName().equals(elem)) 
                    {
                        done = true;
                        continue;
                    }
                    else if (parser.getEventType() == XmlPullParser.START_TAG) 
                    {
                        payloadText.append("<").append(parser.getName());

                        if (parser.getName().equals(payloadElemName) && (payloadNS.length() > 0))
                            payloadText.append(" xmlns=\"").append(payloadNS).append("\"");
                        int n = parser.getAttributeCount();

                        for (int i = 0; i < n; i++) 
                            payloadText.append(" ").append(parser.getAttributeName(i)).append("=\"")
                                    .append(parser.getAttributeValue(i)).append("\"");

                        if (parser.isEmptyElementTag()) 
                        {
                            payloadText.append("/>");
                            isEmptyElement = true;
                        }
                        else 
                        {
                            payloadText.append(">");
                        }
                    }
                    else if (parser.getEventType() == XmlPullParser.END_TAG) 
                    {
                        if (isEmptyElement) 
                        {
                            isEmptyElement = false;
                        }
                        else 
                        {
                            payloadText.append("</").append(parser.getName()).append(">");
                        }
                    }
                    else if (parser.getEventType() == XmlPullParser.TEXT) 
                    {
                        payloadText.append(parser.getText());
                    }
                    tag = parser.next();
                }
                return new PayloadItem<SimplePayload>(id, node, new SimplePayload(payloadElemName, payloadNS, payloadText.toString()));
            }
            else 
            {
                return new PayloadItem<PacketExtension>(id, node, PacketParserUtils.parsePacketExtension(payloadElemName, payloadNS, parser));
            }
        }
    }

}
