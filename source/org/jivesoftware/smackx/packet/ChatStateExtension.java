/**
 * $RCSfile$
 * $Revision: 2407 $
 * $Date: 2004-11-02 15:37:00 -0800 (Tue, 02 Nov 2004) $
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

package org.jivesoftware.smackx.packet;

import org.jivesoftware.smackx.ChatState;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.xmlpull.v1.XmlPullParser;

/**
 * Represents a chat state which is an extension to message packets which is used to indicate
 * the current status of a chat participant.
 *
 * @author Alexander Wenckus
 * @see org.jivesoftware.smackx.ChatState
 */
public class ChatStateExtension implements PacketExtension {

    private ChatState state;

    /**
     * Default constructor. The argument provided is the state that the extension will represent.
     *
     * @param state the state that the extension represents.
     */
    public ChatStateExtension(ChatState state) {
        this.state = state;
    }

    public String getElementName() {
        return state.name();
    }

    public String getNamespace() {
        return "http://jabber.org/protocol/chatstates";
    }

    public String toXML() {
        return "<" + getElementName() + " xmlns=\"" + getNamespace() + "\" />";
    }

    public static class Provider implements PacketExtensionProvider {

        public PacketExtension parseExtension(XmlPullParser parser) throws Exception {
            ChatState state;
            try {
                state = ChatState.valueOf(parser.getName());
            }
            catch (Exception ex) {
                state = ChatState.active;
            }
            return new ChatStateExtension(state);
        }
    }
}
