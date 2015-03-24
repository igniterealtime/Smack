/**
 *
 * Copyright 2014-2015 Florian Schmaus
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

import java.lang.reflect.ParameterizedType;

import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.util.Objects;

/**
 * Filters for stanzas of a particular type and allows a custom method to further filter the packets.
 *
 * @author Florian Schmaus
 */
public abstract class FlexibleStanzaTypeFilter<S extends Stanza> implements StanzaFilter {

    protected final Class<S> stanzaType;

    public FlexibleStanzaTypeFilter(Class<S> packetType) {
        this.stanzaType = Objects.requireNonNull(packetType, "Type must not be null");
    }

    @SuppressWarnings("unchecked")
    public FlexibleStanzaTypeFilter() {
        stanzaType = (Class<S>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    }

    @Override
    @SuppressWarnings("unchecked")
    public final boolean accept(Stanza packet) {
        if (stanzaType.isInstance(packet)) {
            return acceptSpecific((S) packet);
        }
        return false;
    }

    protected abstract boolean acceptSpecific(S packet);

    @Override
    public String toString() {
        return getClass().getSimpleName() + ": " + stanzaType.toString();
    }
}
