/**
 *
 * Copyright 2018 Paul Schaub
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
package org.jivesoftware.smackx.omemo.util;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.util.Objects;

import org.jivesoftware.smackx.omemo.OmemoMessage;

public class MessageOrOmemoMessage {

    private final Message message;
    private final OmemoMessage.Received omemoMessage;

    public MessageOrOmemoMessage(Message message) {
        this.message = Objects.requireNonNull(message);
        this.omemoMessage = null;
    }

    public MessageOrOmemoMessage(OmemoMessage.Received omemoMessage) {
        this.omemoMessage = Objects.requireNonNull(omemoMessage);
        this.message = null;
    }

    public boolean isOmemoMessage() {
        return omemoMessage != null;
    }

    public Message getMessage() {
        return message;
    }

    public OmemoMessage.Received getOmemoMessage() {
        return omemoMessage;
    }
}
