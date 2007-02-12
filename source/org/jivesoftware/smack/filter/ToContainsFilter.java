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
 * Filters for packets where the "to" field contains a specified value. For example,
 * the filter could be used to listen for all packets sent to a group chat nickname.
 *
 * @author Matt Tucker
 */
public class ToContainsFilter implements PacketFilter {

    private String to;

    /**
     * Creates a "to" contains filter using the "to" field part.
     *
     * @param to the to field value the packet must contain.
     */
    public ToContainsFilter(String to) {
        if (to == null) {
            throw new IllegalArgumentException("Parameter cannot be null.");
        }
        this.to = to.toLowerCase();
    }

    public boolean accept(Packet packet) {
        if (packet.getTo() == null) {
            return false;
        }
        else {
            return packet.getTo().toLowerCase().indexOf(to) != -1;
        }
    }
}