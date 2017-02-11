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

public abstract class AbstractFromToMatchesFilter implements StanzaFilter {

    private final Jid address;

    /**
     * Flag that indicates if the checking will be done against bare JID addresses or full JIDs.
     */
    private final boolean ignoreResourcepart;

    /**
     * Creates a filter matching on the address returned by {@link #getAddressToCompare(Stanza)}. The address must be
     * the same as the filter address. The second parameter specifies whether the full or the bare addresses are
     * compared.
     *
     * @param address The address to filter for. If <code>null</code> is given, then
     *        {@link #getAddressToCompare(Stanza)} must also return <code>null</code> to match.
     * @param ignoreResourcepart
     */
    protected AbstractFromToMatchesFilter(Jid address, boolean ignoreResourcepart) {
        if (address != null && ignoreResourcepart) {
            this.address = address.asBareJid();
        }
        else {
            this.address = address;
        }
        this.ignoreResourcepart = ignoreResourcepart;
    }

    @Override
    public final boolean accept(final Stanza stanza) {
        Jid stanzaAddress = getAddressToCompare(stanza);

        if (stanzaAddress == null) {
            return address == null;
        }

        if (ignoreResourcepart) {
            stanzaAddress = stanzaAddress.asBareJid();
        }

        return stanzaAddress.equals(address);
    }

    protected abstract Jid getAddressToCompare(Stanza stanza);

    @Override
    public final String toString() {
        String matchMode = ignoreResourcepart ? "ignoreResourcepart" : "full";
        return getClass().getSimpleName() + " (" + matchMode + "): " + address;
    }
}
