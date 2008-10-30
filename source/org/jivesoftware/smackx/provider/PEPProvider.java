/**
 * $RCSfile: PEPProvider.java,v $
 * $Revision: 1.2 $
 * $Date: 2007/11/06 02:05:09 $
 *
 * Copyright 2003-2007 Jive Software.
 *
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

package org.jivesoftware.smackx.provider;

import java.util.HashMap;
import java.util.Map;

import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.xmlpull.v1.XmlPullParser;

/**
 *
 * The PEPProvider parses incoming PEPEvent packets.
 * (XEP-163 has a weird asymmetric deal: outbound PEP are <iq> + <pubsub> and inbound are <message> + <event>.
 * The provider only deals with inbound, and so it only deals with <message>.
 * 
 * Anyhoo...
 * 
 * The way this works is that PEPxxx classes are generic <pubsub> and <message> providers, and anyone who
 * wants to publish/receive PEPs, such as <tune>, <geoloc>, etc., simply need to extend PEPItem and register (here)
 * a PacketExtensionProvider that knows how to parse that PEPItem extension.
 *
 * @author Jeff Williams
 */
public class PEPProvider implements PacketExtensionProvider {

    Map<String, PacketExtensionProvider> nodeParsers = new HashMap<String, PacketExtensionProvider>();
    PacketExtension pepItem;
    
    /**
     * Creates a new PEPProvider.
     * ProviderManager requires that every PacketExtensionProvider has a public, no-argument constructor
     */
    public PEPProvider() {
    }

    public void registerPEPParserExtension(String node, PacketExtensionProvider pepItemParser) {
        nodeParsers.put(node, pepItemParser);
    }

    /**
     * Parses a PEPEvent packet and extracts a PEPItem from it.
     * (There is only one per <event>.)
     *
     * @param parser the XML parser, positioned at the starting element of the extension.
     * @return a PacketExtension.
     * @throws Exception if a parsing error occurs.
     */
    public PacketExtension parseExtension(XmlPullParser parser) throws Exception {

        boolean done = false;
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals("event")) {
                } else if (parser.getName().equals("items")) {
                    // Figure out the node for this event.
                    String node = parser.getAttributeValue("", "node");
                    // Get the parser for this kind of node, and if found then parse the node.
                    PacketExtensionProvider nodeParser = nodeParsers.get(node);
                    if (nodeParser != null) {
                        pepItem = nodeParser.parseExtension(parser);
                    }
                 }
            } else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals("event")) {
                    done = true;
                }
            }
        }

        return pepItem;
    }
}
