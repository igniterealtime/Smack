/**
 *
 * Copyright 2018 Miguel Hincapie.
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
package org.jivesoftware.smackx.chat_markers.filter;

import org.jivesoftware.smack.filter.StanzaExtensionFilter;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smackx.chatstates.ChatState;
import org.jivesoftware.smackx.chatstates.ChatStateManager;

/**
 * Chat Markers Manager class (XEP-0333).
 *
 * @author Miguel Hincapie
 * @see <a href="http://xmpp.org/extensions/xep-0333.html">XEP-0333: Chat
 * Markers</a>
 */
public final class EligibleForChatMarkerFilter extends StanzaExtensionFilter {

    public static final EligibleForChatMarkerFilter INSTANCE = new EligibleForChatMarkerFilter(ChatStateManager.NAMESPACE);

    private EligibleForChatMarkerFilter(String namespace) {
        super(namespace);
    }

    /**
     * From XEP-0333, Protocol Format: The Chat Marker MUST have an 'id' which is the 'id' of the
     * message being marked.<br>
     * In order to make Chat Markers works together with XEP-0085 as it said in
     * 8.5 Interaction with Chat States, only messages with <tt>active</tt> chat
     * state are accepted.
     *
     * @param message to be analyzed.
     * @return true if the message contains a stanza Id.
     * @see <a href="http://xmpp.org/extensions/xep-0333.html">XEP-0333: Chat Markers</a>
     */
    @Override
    public boolean accept(Stanza message) {
        if (!message.hasStanzaIdSet()) {
            return false;
        }

        if (super.accept(message)) {
            ExtensionElement extension = message.getExtension(ChatStateManager.NAMESPACE);
            String chatStateElementName = extension.getElementName();

            ChatState state;
            try {
                state = ChatState.valueOf(chatStateElementName);
                return (state == ChatState.active);
            }
            catch (Exception ex) {
                return false;
            }
        }

        return true;
    }
}
