/**
 * $RCSfile$
 * $Revision$
 * $Date$
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

import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.jivesoftware.smackx.packet.MessageEvent;
import org.xmlpull.v1.XmlPullParser;

/**
 *
 * The MessageEventProvider parses Message Event packets.
*
 * @author Gaston Dombiak
 */
public class MessageEventProvider implements PacketExtensionProvider {

    /**
     * Creates a new MessageEventProvider.
     * ProviderManager requires that every PacketExtensionProvider has a public, no-argument constructor
     */
    public MessageEventProvider() {
    }

    /**
     * Parses a MessageEvent packet (extension sub-packet).
     *
     * @param parser the XML parser, positioned at the starting element of the extension.
     * @return a PacketExtension.
     * @throws Exception if a parsing error occurs.
     */
    public PacketExtension parseExtension(XmlPullParser parser)
        throws Exception {
        MessageEvent messageEvent = new MessageEvent();
        boolean done = false;
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals("id"))
                    messageEvent.setPacketID(parser.nextText());
                if (parser.getName().equals(MessageEvent.COMPOSING))
                    messageEvent.setComposing(true);
                if (parser.getName().equals(MessageEvent.DELIVERED))
                    messageEvent.setDelivered(true);
                if (parser.getName().equals(MessageEvent.DISPLAYED))
                    messageEvent.setDisplayed(true);
                if (parser.getName().equals(MessageEvent.OFFLINE))
                    messageEvent.setOffline(true);
            } else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals("x")) {
                    done = true;
                }
            }
        }

        return messageEvent;
    }

}
