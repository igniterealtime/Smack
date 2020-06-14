/**
 *
 * Copyright 2019-2020 Florian Schmaus
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

import org.jivesoftware.smack.XMPPConnection;

public abstract class MessageOrPresence<MPB extends MessageOrPresenceBuilder<?, ?>> extends Stanza {

    @Deprecated
    // TODO: Remove in Smack 4.5.
    protected MessageOrPresence() {
    }

    protected MessageOrPresence(StanzaBuilder<?> stanzaBuilder) {
        super(stanzaBuilder);
    }

    protected MessageOrPresence(Stanza other) {
        super(other);
    }

    public abstract MPB asBuilder();

    public abstract MPB asBuilder(String id);

    public abstract MPB asBuilder(XMPPConnection connection);

}
