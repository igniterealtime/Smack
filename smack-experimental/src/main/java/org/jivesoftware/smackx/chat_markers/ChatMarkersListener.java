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
package org.jivesoftware.smackx.chat_markers;

import org.jivesoftware.smack.chat2.Chat;
import org.jivesoftware.smack.packet.Message;

/**
 * Chat Markers Manager class (XEP-0333).
 *
 * @author Miguel Hincapie
 * @see <a href="http://xmpp.org/extensions/xep-0333.html">XEP-0333: Chat
 * Markers</a>
 */
public interface ChatMarkersListener {
    /**
     * Called in ChatMarkersManager when a new message with a markable tag arrives.
     *
     * @param chatMarkersState the current state of the message.
     * @param message          the new incoming message with a markable XML tag.
     * @param chat             associated to the message. This element can be <code>NULL</code>.
     */
    void newChatMarkerMessage(ChatMarkersState chatMarkersState, Message message, Chat chat);
}
