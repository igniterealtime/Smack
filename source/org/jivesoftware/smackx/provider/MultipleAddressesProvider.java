/**
 * $RCSfile$
 * $Revision: $
 * $Date: $
 *
 * Copyright 2003-2006 Jive Software.
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

import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.jivesoftware.smackx.packet.MultipleAddresses;
import org.xmlpull.v1.XmlPullParser;

/**
 * The MultipleAddressesProvider parses {@link MultipleAddresses} packets.
 *
 * @author Gaston Dombiak
 */
public class MultipleAddressesProvider implements PacketExtensionProvider {

    /**
     * Creates a new MultipleAddressesProvider.
     * ProviderManager requires that every PacketExtensionProvider has a public, no-argument
     * constructor.
     */
    public MultipleAddressesProvider() {
    }

    public PacketExtension parseExtension(XmlPullParser parser) throws Exception {
        boolean done = false;
        MultipleAddresses multipleAddresses = new MultipleAddresses();
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals("address")) {
                    String type = parser.getAttributeValue("", "type");
                    String jid = parser.getAttributeValue("", "jid");
                    String node = parser.getAttributeValue("", "node");
                    String desc = parser.getAttributeValue("", "desc");
                    boolean delivered = "true".equals(parser.getAttributeValue("", "delivered"));
                    String uri = parser.getAttributeValue("", "uri");
                    // Add the parsed address
                    multipleAddresses.addAddress(type, jid, node, desc, delivered, uri);
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals(multipleAddresses.getElementName())) {
                    done = true;
                }
            }
        }
        return multipleAddresses;
    }
}
