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

package org.jivesoftware.smackx.xevent.provider;

import java.io.IOException;

import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.jivesoftware.smackx.xevent.packet.MessageEvent;

/**
 *
 * The MessageEventProvider parses Message Event packets.
*
 * @author Gaston Dombiak
 */
public class MessageEventProvider extends ExtensionElementProvider<MessageEvent> {

    /**
     * Parses a MessageEvent stanza (extension sub-packet).
     *
     * @param parser the XML parser, positioned at the starting element of the extension.
     * @return a PacketExtension.
     * @throws IOException if an I/O error occured.
     * @throws XmlPullParserException if an error in the XML parser occured.
     */
    @Override
    public MessageEvent parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment)
                    throws XmlPullParserException, IOException {
        MessageEvent messageEvent = new MessageEvent();
        boolean done = false;
        while (!done) {
            XmlPullParser.Event eventType = parser.next();
            if (eventType == XmlPullParser.Event.START_ELEMENT) {
                if (parser.getName().equals("id"))
                    messageEvent.setStanzaId(parser.nextText());
                if (parser.getName().equals(MessageEvent.COMPOSING))
                    messageEvent.setComposing(true);
                if (parser.getName().equals(MessageEvent.DELIVERED))
                    messageEvent.setDelivered(true);
                if (parser.getName().equals(MessageEvent.DISPLAYED))
                    messageEvent.setDisplayed(true);
                if (parser.getName().equals(MessageEvent.OFFLINE))
                    messageEvent.setOffline(true);
            } else if (eventType == XmlPullParser.Event.END_ELEMENT) {
                if (parser.getName().equals("x")) {
                    done = true;
                }
            }
        }

        return messageEvent;
    }

}
