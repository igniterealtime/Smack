/**
 *
 * Copyright 2019 Florian Schmaus
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
package org.jivesoftware.smack.packet;

import org.jivesoftware.smack.packet.id.StanzaIdSource;

public final class StanzaFactory {

    private final StanzaIdSource stanzaIdSource;

    public StanzaFactory(StanzaIdSource stanzaIdSource) {
        this.stanzaIdSource = stanzaIdSource;
    }

    public MessageBuilder buildMessageStanza() {
        return new MessageBuilder(stanzaIdSource);
    }

    public MessageBuilder buildMessageStanzaFrom(Message message) {
        return new MessageBuilder(message, stanzaIdSource);
    }

    public PresenceBuilder buildPresenceStanza() {
        return new PresenceBuilder(stanzaIdSource);
    }

    public PresenceBuilder buildPresenceStanzaFrom(Presence presence) {
        return new PresenceBuilder(presence, stanzaIdSource);
    }

    public IqBuilder buildIqStanza() {
        return new IqBuilder(stanzaIdSource);
    }

}
