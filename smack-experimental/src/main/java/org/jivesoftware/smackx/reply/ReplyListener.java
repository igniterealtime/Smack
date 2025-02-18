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
package org.jivesoftware.smackx.reply;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smackx.reply.element.ReplyElement;

public interface ReplyListener {

    /**
     * Listener method that gets called when a {@link Message} containing a {@link ReplyElement} is received.
     *
     * @param message message
     * @param reply Reply element
     * @param replyBody body that is marked as reply
     */
    void onReplyReceived(Message message, ReplyElement reply, String replyBody);

}
