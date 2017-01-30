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

public final class FromTypeFilter extends AbstractJidTypeFilter {

    public static final FromTypeFilter ENTITY_FULL_JID = new FromTypeFilter(JidType.entityFull);
    public static final FromTypeFilter ENTITY_BARE_JID = new FromTypeFilter(JidType.entityBare);
    public static final FromTypeFilter DOMAIN_FULL_JID = new FromTypeFilter(JidType.domainFull);
    public static final FromTypeFilter DOMAIN_BARE_JID = new FromTypeFilter(JidType.domainBare);

    private FromTypeFilter(JidType jidType) {
        super(jidType);
    }

    @Override
    protected Jid getJidToInspect(Stanza stanza) {
        return stanza.getFrom();
    }

}
