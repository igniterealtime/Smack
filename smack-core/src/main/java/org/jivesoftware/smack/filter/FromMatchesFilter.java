/**
 *
 * Copyright 2003-2014 Jive Software, 2017 Florian Schmaus.
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
import org.jxmpp.jid.Jid;

/**
 * Filter for packets where the "from" field exactly matches a specified JID. If the specified
 * address is a bare JID then the filter will match any address whose bare JID matches the
 * specified JID. But if the specified address is a full JID then the filter will only match
 * if the sender of the stanza(/packet) matches the specified resource.
 *
 * @author Gaston Dombiak
 */
public final class FromMatchesFilter extends AbstractFromToMatchesFilter {

    public final static FromMatchesFilter MATCH_NO_FROM_SET = create(null);

    /**
     * Creates a filter matching on the "from" field. The from address must be the same as the
     * filter address. The second parameter specifies whether the full or the bare addresses are
     * compared.
     *
     * @param address The address to filter for. If <code>null</code> is given, the stanza(/packet) must not
     *        have a from address.
     * @param ignoreResourcepart
     */
    public FromMatchesFilter(Jid address, boolean ignoreResourcepart) {
        super(address, ignoreResourcepart);
    }

    /**
     * Creates a filter matching on the "from" field. If the filter address is bare, compares
     * the filter address with the bare from address. Otherwise, compares the filter address
     * with the full from address.
     *
     * @param address The address to filter for. If <code>null</code> is given, the stanza must not
     *        have a from address.
     */
    public static FromMatchesFilter create(Jid address) {
        return new FromMatchesFilter(address, address != null ? address.hasNoResource() : false) ;
    }

    /**
     * Creates a filter matching on the "from" field. Compares the bare version of from and filter
     * address.
     *
     * @param address The address to filter for. If <code>null</code> is given, the stanza must not
     *        have a from address.
     */
    public static FromMatchesFilter createBare(Jid address) {
        return new FromMatchesFilter(address, true);
    }

    /**
     * Creates a filter matching on the "from" field. Compares the full version, if available, of from and filter
     * address.
     *
     * @param address The address to filter for. If <code>null</code> is given, the stanza must not
     *        have a from address.
     */
    public static FromMatchesFilter createFull(Jid address) {
        return new FromMatchesFilter(address, false);
    }

    @Override
    protected Jid getAddressToCompare(Stanza stanza) {
        return stanza.getFrom();
    }

}
