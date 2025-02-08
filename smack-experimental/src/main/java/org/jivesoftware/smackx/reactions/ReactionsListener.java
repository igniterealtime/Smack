/**
 *
 * Copyright 2025 Ismael Nunes Campos
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
package org.jivesoftware.smackx.reactions;

import org.jivesoftware.smack.packet.Message;

import org.jivesoftware.smackx.reactions.element.ReactionsElement;

public interface ReactionsListener {

    /**
     * Listener method that gets called when a {@link Message} containing a {@link ReactionsElement} is received.
     * @param reactionsElement reactionsElement
     * @param message message
     */
    void onReactionReceived(Message message, ReactionsElement reactionsElement);

}
