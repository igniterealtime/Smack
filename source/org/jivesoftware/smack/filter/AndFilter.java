/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright (C) 2002-2003 Jive Software. All rights reserved.
 * ====================================================================
 * The Jive Software License (based on Apache Software License, Version 1.1)
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by
 *        Jive Software (http://www.jivesoftware.com)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Smack" and "Jive Software" must not be used to
 *    endorse or promote products derived from this software without
 *    prior written permission. For written permission, please
 *    contact webmaster@jivesoftware.com.
 *
 * 5. Products derived from this software may not be called "Smack",
 *    nor may "Smack" appear in their name, without prior written
 *    permission of Jive Software.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL JIVE SOFTWARE OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 */

package org.jivesoftware.smack.filter;

import org.jivesoftware.smack.packet.Packet;

/**
 * Implements the logical AND operation over two or more packet filters.
 * In other words, packets pass this filter if they pass <b>all</b> of the filters.
 *
 * @author Matt Tucker
 */
public class AndFilter implements PacketFilter {

    /**
     * The current number of elements in the filter.
     */
    private int size;

    /**
     * The list of filters.
     */
    private PacketFilter [] filters;

    /**
     * Creates an empty AND filter. Filters should be added using the
     * {@link #addFilter(PacketFilter) method.
     */
    public AndFilter() {
        size = 0;
        filters = new PacketFilter[3];
    }

    /**
     * Creates an AND filter using the two specified filters.
     *
     * @param filter1 the first packet filter.
     * @param filter2 the second packet filter.
     */
    public AndFilter(PacketFilter filter1, PacketFilter filter2) {
        if (filter1 == null || filter2 == null) {
            throw new IllegalArgumentException("Parameters cannot be null.");
        }
        size = 2;
        filters = new PacketFilter[2];
        filters[0] = filter1;
        filters[1] = filter2;
    }

    /**
     * Adds a filter to the filter list for the AND operation. A packet
     * will pass the filter if all of the filters in the list accept it.
     *
     * @param filter a filter to add to the filter list.
     */
    public void addFilter(PacketFilter filter) {
        if (filter == null) {
            throw new IllegalArgumentException("Parameter cannot be null.");
        }
        // If there is no more room left in the filters array, expand it.
        if (size == filters.length) {
            PacketFilter [] newFilters = new PacketFilter[filters.length+2];
            for (int i=0; i<filters.length; i++) {
                newFilters[i] = filters[i];
            }
            filters = newFilters;
        }
        // Add the new filter to the array.
        filters[size] = filter;
        size++;
    }

    public boolean accept(Packet packet) {
        for (int i=0; i<size; i++) {
            if (!filters[i].accept(packet)) {
                return false;
            }
        }
        return true;
    }

    public String toString() {
        return filters.toString();
    }
}
