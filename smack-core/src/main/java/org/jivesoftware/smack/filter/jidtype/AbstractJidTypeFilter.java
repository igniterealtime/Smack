/**
 *
 * Copyright 2015 Florian Schmaus.
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
package org.jivesoftware.smack.filter.jidtype;

import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.util.Objects;
import org.jxmpp.jid.Jid;

/**
 * Base class for XMPP address type filters.
 *
 * @author Florian Schmaus
 *
 */
public abstract class AbstractJidTypeFilter implements StanzaFilter {

    private final JidType jidType;

    protected AbstractJidTypeFilter(JidType jidType) {
        this.jidType = Objects.requireNonNull(jidType, "jidType must not be null");
    }

    @Override
    public boolean accept(Stanza stanza) {
        Jid toMatch = getJidToMatchFrom(stanza);
        if (toMatch == null) {
            return false;
        }
        return jidType.isTypeOf(toMatch);
    }

    protected abstract Jid getJidToMatchFrom(Stanza stanza);

    @Override
    public final String toString() {
        return getClass().getSimpleName() + ": " + jidType;
    }

    public enum JidType {
        BareJid,
        DomainBareJid,
        DomainFullJid,
        DomainJid,
        EntityBareJid,
        EntityFullJid,
        EntityJid,
        FullJid,
        ;

        public boolean isTypeOf(Jid jid) {
            if (jid == null) {
                return false;
            }
            switch (this) {
            case BareJid:
                return jid.hasNoResource();
            case DomainBareJid:
                return jid.isDomainBareJid();
            case DomainFullJid:
                return jid.isDomainFullJid();
            case EntityBareJid:
                return jid.isEntityBareJid();
            case EntityFullJid:
                return jid.isEntityFullJid();
            case EntityJid:
                return jid.isEntityJid();
            case FullJid:
                return jid.hasResource();
            default:
                throw new IllegalStateException();
            }
        }
    }
}
