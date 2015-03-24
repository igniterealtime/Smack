/**
 *
 * Copyright 2014 Florian Schmaus
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

import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Presence.Type;
import org.jivesoftware.smack.util.Objects;

/**
 * A filter for Presence types. Returns true only if the stanza is an Presence stanza(/packet) and it matches the type provided in the
 * constructor.
 */
public class PresenceTypeFilter extends FlexibleStanzaTypeFilter<Presence> {

    public static final PresenceTypeFilter AVAILABLE = new PresenceTypeFilter(Type.available);
    public static final PresenceTypeFilter UNAVAILABLE = new PresenceTypeFilter(Type.unavailable);
    public static final PresenceTypeFilter SUBSCRIBE = new PresenceTypeFilter(Type.subscribe);
    public static final PresenceTypeFilter SUBSCRIBED = new PresenceTypeFilter(Type.subscribed);
    public static final PresenceTypeFilter UNSUBSCRIBE = new PresenceTypeFilter(Type.unsubscribe);
    public static final PresenceTypeFilter UNSUBSCRIBED = new PresenceTypeFilter(Type.unsubscribed);
    public static final PresenceTypeFilter ERROR = new PresenceTypeFilter(Type.error);
    public static final PresenceTypeFilter PROBE = new PresenceTypeFilter(Type.probe);

    private final Presence.Type type;

    private PresenceTypeFilter(Presence.Type type) {
        super(Presence.class);
        this.type = Objects.requireNonNull(type, "type must not be null");
    }

    @Override
    protected boolean acceptSpecific(Presence presence) {
        return presence.getType() == type;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ": type=" + type;
    }
}
