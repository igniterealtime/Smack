/**
 *
 * Copyright © 2014 Florian Schmaus
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

/**
 * Filters message stanzas which have at least one body
 */
public class MessageWithBodiesFilter extends FlexibleStanzaTypeFilter<Message> {

    public static final StanzaFilter INSTANCE = new MessageWithBodiesFilter();

    private MessageWithBodiesFilter() {
        super(Message.class);
    }

    @Override
    protected boolean acceptSpecific(Message message) {
        // Accept only messages which have at least one body
        return !message.getBodies().isEmpty();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}
