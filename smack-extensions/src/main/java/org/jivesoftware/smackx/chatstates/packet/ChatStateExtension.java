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

package org.jivesoftware.smackx.chatstates.packet;

import org.jivesoftware.smackx.chatstates.ChatState;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.util.XmlStringBuilder;

/**
 * Represents a chat state which is an extension to message packets which is used to indicate
 * the current status of a chat participant.
 *
 * @author Alexander Wenckus
 * @see org.jivesoftware.smackx.chatstates.ChatState
 */
public class ChatStateExtension implements ExtensionElement {

    public static final String NAMESPACE = "http://jabber.org/protocol/chatstates";

    private final ChatState state;

    /**
     * Default constructor. The argument provided is the state that the extension will represent.
     *
     * @param state the state that the extension represents.
     */
    public ChatStateExtension(ChatState state) {
        this.state = state;
    }

    @Override
    public String getElementName() {
        return state.name();
    }

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

    public ChatState getChatState() {
        return state;
    }

    @Override
    public XmlStringBuilder toXML() {
        XmlStringBuilder xml = new XmlStringBuilder(this);
        xml.closeEmptyElement();
        return xml;
    }

}
