/**
 *
 * Copyright 2003-2007 Jive Software.
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

package org.jivesoftware.smackx.disco.provider;

import java.io.IOException;

import org.jivesoftware.smack.packet.IqData;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.provider.IqProvider;
import org.jivesoftware.smack.util.ParserUtils;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.jivesoftware.smackx.disco.packet.DiscoverItems;

import org.jxmpp.jid.Jid;

/**
* The DiscoverInfoProvider parses Service Discovery items packets.
*
* @author Gaston Dombiak
*/
public class DiscoverItemsProvider extends IqProvider<DiscoverItems> {

    @Override
    public DiscoverItems parse(XmlPullParser parser, int initialDepth, IqData iqData, XmlEnvironment xmlEnvironment)
                    throws XmlPullParserException, IOException {
        DiscoverItems discoverItems = new DiscoverItems();
        boolean done = false;
        DiscoverItems.Item item;
        Jid jid = null;
        String name = "";
        String action = "";
        String node = "";
        discoverItems.setNode(parser.getAttributeValue("", "node"));
        while (!done) {
            XmlPullParser.Event eventType = parser.next();

            if (eventType == XmlPullParser.Event.START_ELEMENT && "item".equals(parser.getName())) {
                // Initialize the variables from the parsed XML
                jid = ParserUtils.getJidAttribute(parser);
                name = parser.getAttributeValue("", "name");
                node = parser.getAttributeValue("", "node");
                action = parser.getAttributeValue("", "action");
            }
            else if (eventType == XmlPullParser.Event.END_ELEMENT && "item".equals(parser.getName())) {
                // Create a new Item and add it to DiscoverItems.
                item = new DiscoverItems.Item(jid);
                item.setName(name);
                item.setNode(node);
                item.setAction(action);
                discoverItems.addItem(item);
            }
            else if (eventType == XmlPullParser.Event.END_ELEMENT && "query".equals(parser.getName())) {
                done = true;
            }
        }

        return discoverItems;
    }
}
