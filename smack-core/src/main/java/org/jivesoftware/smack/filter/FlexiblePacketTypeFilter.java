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

import java.lang.reflect.ParameterizedType;

import org.jivesoftware.smack.packet.Stanza;

/**
 * Filters for packets of a particular type and allows a custom method to further filter the packets.
 *
 * @author Florian Schmaus
 */
public abstract class FlexiblePacketTypeFilter<P extends Stanza> implements PacketFilter {

    protected final Class<P> packetType;

    public FlexiblePacketTypeFilter(Class<P> packetType) {
        this.packetType = packetType;
    }

    @SuppressWarnings("unchecked")
    public FlexiblePacketTypeFilter() {
        packetType = (Class<P>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean accept(Stanza packet) {
        if (packetType.isInstance(packet)) {
            return acceptSpecific((P) packet);
        }
        return false;
    }

    protected abstract boolean acceptSpecific(P packet);
}
