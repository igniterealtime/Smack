/**
 *
 * Copyright 2003-2014 Jive Software.
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

import java.util.Locale;

import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.util.StringUtils;

/**
 * Filter for packets where the "from" field exactly matches a specified JID. If the specified
 * address is a bare JID then the filter will match any address whose bare JID matches the
 * specified JID. But if the specified address is a full JID then the filter will only match
 * if the sender of the packet matches the specified resource.
 *
 * @author Gaston Dombiak
 */
public class FromMatchesFilter implements PacketFilter {

    private String address;
    /**
     * Flag that indicates if the checking will be done against bare JID addresses or full JIDs.
     */
    private boolean matchBareJID = false;

    /**
     * Creates a filter matching on the "from" field. The from address must be the same as the
     * filter address. The second parameter specifies whether the full or the bare addresses are
     * compared.
     *
     * @param address The address to filter for. If <code>null</code> is given, the packet must not
     *        have a from address.
     * @param matchBare
     */
    public FromMatchesFilter(String address, boolean matchBare) {
        this.address = (address == null) ? null : address.toLowerCase(Locale.US);
        matchBareJID = matchBare;
    }

    /**
     * Creates a filter matching on the "from" field. If the filter address is bare, compares
     * the filter address with the bare from address. Otherwise, compares the filter address
     * with the full from address.
     *
     * @param address The address to filter for. If <code>null</code> is given, the packet must not
     *        have a from address.
     */
    public static FromMatchesFilter create(String address) {
        return new FromMatchesFilter(address, "".equals(StringUtils.parseResource(address))) ;
    }

    /**
     * Creates a filter matching on the "from" field. Compares the bare version of from and filter
     * address.
     *
     * @param address The address to filter for. If <code>null</code> is given, the packet must not
     *        have a from address.
     */
    public static FromMatchesFilter createBare(String address) {
        address = (address == null) ? null : StringUtils.parseBareAddress(address);
        return new FromMatchesFilter(address, true);
    }


    /**
     * Creates a filter matching on the "from" field. Compares the full version of from and filter
     * address.
     *
     * @param address The address to filter for. If <code>null</code> is given, the packet must not
     *        have a from address.
     */
    public static FromMatchesFilter createFull(String address) {
        return new FromMatchesFilter(address, false);
    }

    public boolean accept(Packet packet) {
        String from = packet.getFrom();
        if (from == null) {
            return address == null;
        }
        // Simplest form of NAMEPREP/STRINGPREP
        from = from.toLowerCase(Locale.US);
        if (matchBareJID) {
            from = StringUtils.parseBareAddress(from);
        }
        return from.equals(address);
    }

    public String toString() {
        String matchMode = matchBareJID ? "bare" : "full";
        return "FromMatchesFilter (" +matchMode + "): " + address;
    }
}
