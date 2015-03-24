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

package org.jivesoftware.smack.filter;

import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Message.Type;


/**
 * Filters for packets of a specific type of Message (e.g. CHAT).
 * 
 * @see org.jivesoftware.smack.packet.Message.Type
 * @author Ward Harold
 */
public class MessageTypeFilter extends FlexibleStanzaTypeFilter<Message> {

    public static final StanzaFilter NORMAL = new MessageTypeFilter(Type.normal);
    public static final StanzaFilter CHAT = new MessageTypeFilter(Type.chat);
    public static final StanzaFilter GROUPCHAT = new MessageTypeFilter(Type.groupchat);
    public static final StanzaFilter HEADLINE = new MessageTypeFilter(Type.headline);
    public static final StanzaFilter ERROR = new MessageTypeFilter(Type.error);
    public static final StanzaFilter NORMAL_OR_CHAT = new OrFilter(NORMAL, CHAT);
    public static final StanzaFilter NORMAL_OR_CHAT_OR_HEADLINE = new OrFilter(NORMAL_OR_CHAT,
                    HEADLINE);

    private final Message.Type type;

    /**
     * Creates a new message type filter using the specified message type.
     * 
     * @param type the message type.
     */
    private MessageTypeFilter(Message.Type type) {
        super(Message.class);
        this.type = type;
    }

    @Override
    protected boolean acceptSpecific(Message message) {
        return message.getType() == type;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ": type=" + type;
    }
}
