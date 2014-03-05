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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jivesoftware.smack.packet.Packet;

/**
 * Implements the logical OR operation over two or more packet filters. In
 * other words, packets pass this filter if they pass <b>any</b> of the filters.
 *
 * @author Matt Tucker
 */
public class OrFilter implements PacketFilter {

    /**
     * The list of filters.
     */
    private final List<PacketFilter> filters;

    /**
     * Creates an empty OR filter. Filters should be added using the
     * {@link #addFilter(PacketFilter)} method.
     */
    public OrFilter() {
        filters = new ArrayList<PacketFilter>();
    }

    /**
     * Creates an OR filter using the specified filters.
     *
     * @param filters the filters to add.
     */
    public OrFilter(PacketFilter... filters) {
        if (filters == null) {
            throw new IllegalArgumentException("Parameter must not be null.");
        }
        for(PacketFilter filter : filters) {
            if(filter == null) {
                throw new IllegalArgumentException("Parameter must not be null.");
            }
        }
        this.filters = new ArrayList<PacketFilter>(Arrays.asList(filters));
    }

    /**
     * Adds a filter to the filter list for the OR operation. A packet
     * will pass the filter if any filter in the list accepts it.
     *
     * @param filter a filter to add to the filter list.
     */
    public void addFilter(PacketFilter filter) {
        if (filter == null) {
            throw new IllegalArgumentException("Parameter must not be null.");
        }
        filters.add(filter);
    }

    public boolean accept(Packet packet) {
        for (PacketFilter filter : filters) {
            if (filter.accept(packet)) {
                return true;
            }
        }
        return false;
    }

    public String toString() {
        return filters.toString();
    }
}
