/**
 *
 * Copyright 2018 Florian Schmaus.
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

public abstract class AbstractPossibleJidTypeFilter extends AbstractJidTypeFilter {

    protected AbstractPossibleJidTypeFilter(JidType jidType) {
        super(jidType);
    }

    @Override
    public final boolean accept(Stanza stanza) {
        final Jid jid = getJidToInspect(stanza);

        if (jid == null) {
            return false;
        }

        switch (jidType) {
        case entityFull:
            return null != jid.asEntityFullJidIfPossible();
        case entityBare:
            return null != jid.asEntityBareJidIfPossible();
        case domainFull:
            return null != jid.asDomainFullJidIfPossible();
        case domainBare:
        case any:
            return true;
        default:
            throw new AssertionError();
        }
    }

}
