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

public final class ToTypeFilter extends AbstractJidTypeFilter {

    public static final ToTypeFilter ENTITY_FULL_JID = new ToTypeFilter(JidType.entityFull);
    public static final ToTypeFilter ENTITY_BARE_JID = new ToTypeFilter(JidType.entityBare);
    public static final ToTypeFilter DOMAIN_FULL_JID = new ToTypeFilter(JidType.domainFull);
    public static final ToTypeFilter DOMAIN_BARE_JID = new ToTypeFilter(JidType.domainBare);

    public static final StanzaFilter ENTITY_FULL_OR_BARE_JID = new OrFilter(ENTITY_FULL_JID, ENTITY_BARE_JID);

    private ToTypeFilter(JidType jidType) {
        super(jidType);
    }

    @Override
    protected Jid getJidToInspect(Stanza stanza) {
        return stanza.getTo();
    }

}
