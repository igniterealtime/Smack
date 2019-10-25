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

public abstract class MessageOrPresenceBuilder<MP extends MessageOrPresence<? extends MessageOrPresenceBuilder<MP, SB>>, SB extends StanzaBuilder<SB>>
                extends StanzaBuilder<SB> {

    protected MessageOrPresenceBuilder(Stanza stanza, StanzaIdSource stanzaIdSource) {
        super(stanza, stanzaIdSource);
    }

    protected MessageOrPresenceBuilder(Stanza stanza, String stanzaId) {
        super(stanza, stanzaId);
    }

    protected MessageOrPresenceBuilder(StanzaIdSource stanzaIdSource) {
        super(stanzaIdSource);
    }

    protected MessageOrPresenceBuilder(String stanzaId) {
        super(stanzaId);
    }

    public abstract MP build();

}
