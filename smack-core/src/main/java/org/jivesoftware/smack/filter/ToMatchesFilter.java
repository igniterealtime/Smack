/**
 *
 * Copyright 2017 Florian Schmaus.
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

public final class ToMatchesFilter extends AbstractFromToMatchesFilter {

    public static final ToMatchesFilter MATCH_NO_TO_SET = create(null);

    public ToMatchesFilter(Jid address, boolean ignoreResourcepart) {
        super(address, ignoreResourcepart);
    }

    /**
     * Creates a filter matching on the "to" field. If the filter address is bare, compares
     * the filter address with the bare from address. Otherwise, compares the filter address
     * with the full from address.
     *
     * @param address The address to filter for. If <code>null</code> is given, the stanza must not
     *        have a from address.
     */
    public static ToMatchesFilter create(Jid address) {
        return new ToMatchesFilter(address, address != null ? address.hasNoResource() : false) ;
    }

    /**
     * Creates a filter matching on the "to" field. Compares the bare version of to and filter
     * address.
     *
     * @param address The address to filter for. If <code>null</code> is given, the stanza must not
     *        have a from address.
     */
    public static ToMatchesFilter createBare(Jid address) {
        return new ToMatchesFilter(address, true);
    }

    /**
     * Creates a filter matching on the "to" field. Compares the full version, if available, of to and filter
     * address.
     *
     * @param address The address to filter for. If <code>null</code> is given, the stanza must not
     *        have a from address.
     */
    public static ToMatchesFilter createFull(Jid address) {
        return new ToMatchesFilter(address, false);
    }

    @Override
    protected Jid getAddressToCompare(Stanza stanza) {
        return stanza.getTo();
    }

}
