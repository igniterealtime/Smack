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

import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.util.StringUtils;

/**
 * Filters for packets with a particular stanza(/packet) ID.
 *
 * @author Matt Tucker
 * @deprecated use {@link StanzaIdFilter} instead.
 */
@Deprecated
public class PacketIDFilter implements StanzaFilter {

    private final String packetID;

    /**
     * Creates a new stanza(/packet) ID filter using the specified packet's ID.
     *
     * @param packet the stanza(/packet) which the ID is taken from.
     * @deprecated use {@link StanzaIdFilter#StanzaIdFilter(Stanza)} instead.
     */
    @Deprecated
    public PacketIDFilter(Stanza packet) {
        this(packet.getStanzaId());
    }

    /**
     * Creates a new stanza(/packet) ID filter using the specified stanza(/packet) ID.
     *
     * @param packetID the stanza(/packet) ID to filter for.
     * @deprecated use {@link StanzaIdFilter#StanzaIdFilter(Stanza)} instead.
     */
    @Deprecated
    public PacketIDFilter(String packetID) {
        StringUtils.requireNotNullOrEmpty(packetID, "Packet ID must not be null or empty.");
        this.packetID = packetID;
    }

    @Override
    public boolean accept(Stanza packet) {
        return packetID.equals(packet.getStanzaId());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ": id=" + packetID;
    }
}
