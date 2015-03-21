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

package org.jivesoftware.smackx.pep.provider;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

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
public class PEPProvider extends ExtensionElementProvider<ExtensionElement> {

    private static final Map<String, ExtensionElementProvider<?>> nodeParsers = new HashMap<String, ExtensionElementProvider<?>>();

    public static void registerPEPParserExtension(String node, ExtensionElementProvider<?> pepItemParser) {
        nodeParsers.put(node, pepItemParser);
    }

    /**
     * Parses a PEPEvent stanza(/packet) and extracts a PEPItem from it.
     * (There is only one per <event>.)
     *
     * @param parser the XML parser, positioned at the starting element of the extension.
     * @return a PacketExtension.
     * @throws IOException 
     * @throws XmlPullParserException 
     * @throws SmackException 
     */
    @Override
    public ExtensionElement parse(XmlPullParser parser, int initialDepth)
                    throws XmlPullParserException, IOException, SmackException {
        ExtensionElement pepItem = null;
        boolean done = false;
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals("event")) {
                } else if (parser.getName().equals("items")) {
                    // Figure out the node for this event.
                    String node = parser.getAttributeValue("", "node");
                    // Get the parser for this kind of node, and if found then parse the node.
                    ExtensionElementProvider<?> nodeParser = nodeParsers.get(node);
                    if (nodeParser != null) {
                        pepItem = nodeParser.parse(parser);
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
