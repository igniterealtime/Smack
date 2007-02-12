/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright 2003-2007 Jive Software.
 *
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
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

import org.jivesoftware.smack.packet.Packet;

/**
 * Implements the logical NOT operation on a packet filter. In other words, packets
 * pass this filter if they do not pass the supplied filter.
 *
 * @author Matt Tucker
 */
public class NotFilter implements PacketFilter {

    private PacketFilter filter;

    /**
     * Creates a NOT filter using the specified filter.
     *
     * @param filter the filter.
     */
    public NotFilter(PacketFilter filter) {
        if (filter == null) {
            throw new IllegalArgumentException("Parameter cannot be null.");
        }
        this.filter = filter;
    }

    public boolean accept(Packet packet) {
        return !filter.accept(packet);
    }
}
