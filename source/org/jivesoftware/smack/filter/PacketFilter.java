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
 *    contact webmaster@coolservlets.com.
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
 * Defines a way to filter packets for particular attributes. Packet filters are
 * used when constructing packet listeners or collectors -- the filter defines
 * what packets match the criteria of the collector or listener for further
 * packet processing.<p>
 *
 * Several pre-defined filters are defined. These filters can be logically combined
 * for more complex packet filtering by using the
 * {@link org.jivesoftware.smack.filter.AndFilter AndFilter} and
 * {@link org.jivesoftware.smack.filter.OrFilter OrFilter} filters. It's also possible
 * to define your own filters by implementing this interface. The code example below
 * creates a trivial filter for packets with a specific ID.
 *
 * <pre>
 * // Use an anonymous inner class to define a packet filter that returns
 * // all packets that have a packet ID of "RS145".
 * PacketFilter myFilter = new PacketFilter() {
 *     public boolean accept(Packet packet) {
 *         return "RS145".equals(packet.getPacketID());
 *     }
 * };
 * // Create a new packet collector using the filter we created.
 * PacketCollector myCollector = packetReader.createPacketCollector(myFilter);
 * </pre>
 *
 * @see org.jivesoftware.smack.PacketCollector
 * @see org.jivesoftware.smack.PacketListener
 * @author Matt Tucker
 */
public interface PacketFilter {

    /**
     * Tests whether or not the specified packet should pass the filter.
     *
     * @param packet the packet to test.
     * @return true if and only if <tt>packet</tt> passes the filter.
     */
    public boolean accept(Packet packet);
}
