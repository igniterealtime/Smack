/**
 *
 * Copyright 2003-2007 Jive Software, 2018 Paul Schaub.
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

package org.jivesoftware.smackx.chatstates;

import org.jivesoftware.smack.chat2.Chat;
import org.jivesoftware.smack.packet.Message;

/**
 * Events for when the state of a user in a chat changes.
 *
 * @author Alexander Wenckus
 * @author Paul Schaub
 */
public interface ChatStateListener {

    /**
     * Fired when the state of a chat with another user changes.
     *
     * @param chat the chat in which the state has changed.
     * @param state the new state of the participant.
     * @param message the message carrying the chat state.
     */
    void stateChanged(Chat chat, ChatState state, Message message);
}
