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
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.packet.Presence;

/**
 * Filters for packets of a particular type. The type is given as a Class object, so
 * example types would:
 * <ul>
 *      <li><tt>Message.class</tt>
 *      <li><tt>IQ.class</tt>
 *      <li><tt>Presence.class</tt>
 * </ul>
 *
 * @author Matt Tucker
 * @deprecated use {@link StanzaTypeFilter} instead.
 */
@Deprecated
public class PacketTypeFilter implements StanzaFilter {

    public static final PacketTypeFilter PRESENCE = new PacketTypeFilter(Presence.class);
    public static final PacketTypeFilter MESSAGE = new PacketTypeFilter(Message.class);

    private final Class<? extends Stanza> packetType;

    /**
     * Creates a new stanza(/packet) type filter that will filter for packets that are the
     * same type as <tt>packetType</tt>.
     *
     * @param packetType the Class type.
     */
    public PacketTypeFilter(Class<? extends Stanza> packetType) {
        this.packetType = packetType;
    }

    @Override
    public boolean accept(Stanza packet) {
        return packetType.isInstance(packet);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ": " + packetType.getName();
    }
}
