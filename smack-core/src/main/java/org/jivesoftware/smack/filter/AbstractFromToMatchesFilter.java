/**
 *
 * Copyright 2017-2024 Florian Schmaus.
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

    enum MatchMode {
        exact,
        child,
        strictChild,
        ignoreResourcepart,
    }

    /**
     * Flag that indicates if the checking will be done against bare JID addresses or full JIDs.
     */
    private final MatchMode matchMode;

    /**
     * Creates a filter matching on the address returned by {@link #getAddressToCompare(Stanza)}. The address must be
     * the same as the filter address. The second parameter specifies whether the full or the bare addresses are
     * compared.
     *
     * @param address The address to filter for. If <code>null</code> is given, then
     *        {@link #getAddressToCompare(Stanza)} must also return <code>null</code> to match.
     * @param ignoreResourcepart TODO javadoc me please
     */
    protected AbstractFromToMatchesFilter(Jid address, boolean ignoreResourcepart) {
        this(
            ignoreResourcepart && address != null ? address.asBareJid() : address,
            ignoreResourcepart ? MatchMode.ignoreResourcepart : MatchMode.exact
        );
    }

    /**
     * Creates a filter matching on the address returned by {@link #getAddressToCompare(Stanza)}. The address must be
     * the same as the filter address. The second parameter specifies whether the full or the bare addresses are
     * compared.
     *
     * @param address The address to filter for. If <code>null</code> is given, then
     *        {@link #getAddressToCompare(Stanza)} must also return <code>null</code> to match.
     * @param matchMode the match mode.
     */
    protected AbstractFromToMatchesFilter(Jid address, MatchMode matchMode) {
        if (matchMode == MatchMode.strictChild && address.hasResource())
            throw new IllegalArgumentException("Can't create a strict child match with a resource that as a resourcepart, because no address would match");

        this.address = address;
        this.matchMode = matchMode;
    }

    @Override
    public final boolean accept(final Stanza stanza) {
        Jid stanzaAddress = getAddressToCompare(stanza);

        if (stanzaAddress == null) {
            return address == null;
        }

        if (address == null) {
            // stanzaAddress is not null, but matching address is null.
            return false;
        }

        switch (matchMode) {
        case exact:
            return stanzaAddress.equals(address);
        case child:
            return address.isParentOf(stanzaAddress);
        case strictChild:
            return address.isStrictParentOf(stanzaAddress);
        case ignoreResourcepart:
            stanzaAddress = stanzaAddress.asBareJid();
            return address.equals(stanzaAddress);
        default:
            throw new IllegalStateException("Unknown matchMode: " + matchMode);
        }
    }

    protected abstract Jid getAddressToCompare(Stanza stanza);

    @Override
    public final String toString() {
        return getClass().getSimpleName() + " (" + matchMode + "): " + address;
    }
}
