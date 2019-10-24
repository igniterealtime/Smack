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

import org.jivesoftware.smack.packet.Presence.Mode;
import org.jivesoftware.smack.packet.id.StanzaIdSource;
import org.jivesoftware.smack.util.Objects;
import org.jivesoftware.smack.util.ToStringUtil;

public final class PresenceBuilder extends StanzaBuilder<PresenceBuilder> implements PresenceView {
    static final PresenceBuilder EMPTY = new PresenceBuilder(() -> {
        return null;
    });

    Presence.Type type = Presence.Type.available;

    String status;

    Byte priority;

    Presence.Mode mode;

    PresenceBuilder(Presence presence, String stanzaId) {
        super(presence, stanzaId);
        copyFromPresence(presence);
    }

    PresenceBuilder(Presence presence, StanzaIdSource stanzaIdSource) {
        super(presence, stanzaIdSource);
        copyFromPresence(presence);
    }

    PresenceBuilder(StanzaIdSource stanzaIdSource) {
        super(stanzaIdSource);
    }

    PresenceBuilder(String stanzaId) {
        super(stanzaId);
    }

    private void copyFromPresence(Presence presence) {
        type = presence.getType();
        status = presence.getStatus();
        priority = presence.getPriorityByte();
        mode = presence.getMode();
    }

    @Override
    protected void addStanzaSpecificAttributes(ToStringUtil.Builder builder) {
        builder.addValue("type", type)
               .addValue("mode", mode)
               .addValue("priority", priority)
               .addValue("status", status)
               ;
    }

    public PresenceBuilder ofType(Presence.Type type) {
        this.type = Objects.requireNonNull(type, "Type cannot be null");
        return getThis();
    }

    public PresenceBuilder setStatus(String status) {
        this.status = status;
        return getThis();
    }

    public PresenceBuilder setPriority(int priority) {
        if (priority < -128 || priority > 127) {
            throw new IllegalArgumentException("Priority value " + priority +
                    " is not valid. Valid range is -128 through 127.");
        }
        Byte priorityByte = (byte) priority;
        return setPriority(priorityByte);
    }

    public PresenceBuilder setPriority(Byte priority) {
        this.priority = priority;
        return getThis();
    }

    public PresenceBuilder setMode(Presence.Mode mode) {
        this.mode = mode;
        return getThis();
    }

    @Override
    public PresenceBuilder getThis() {
        return this;
    }

    public Presence build() {
        return new Presence(this);
    }

    @Override
    public Presence.Type getType() {
        return type;
    }

    @Override
    public String getStatus() {
        return status;
    }

    @Override
    public int getPriority() {
        return getPriorityByte();
    }

    @Override
    public byte getPriorityByte() {
        if (priority == null) {
            return 0;
        }
        return priority;
    }

    @Override
    public Presence.Mode getMode() {
        if (mode == null) {
            return Mode.available;
        }
        return mode;
   }

}
