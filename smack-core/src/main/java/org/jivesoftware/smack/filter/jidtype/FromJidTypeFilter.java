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

import org.jivesoftware.smack.packet.Stanza;

import org.jxmpp.jid.Jid;

/**
 * Filter based on the 'from' XMPP address type.
 *
 * @author Florian Schmaus
 *
 */
public class FromJidTypeFilter extends AbstractJidTypeFilter {

    public static final FromJidTypeFilter ENTITY_BARE_JID = new FromJidTypeFilter(JidType.EntityBareJid);

    public FromJidTypeFilter(JidType jidType) {
        super(jidType);
    }

    @Override
    protected Jid getJidToMatchFrom(Stanza stanza) {
        return stanza.getFrom();
    }

}
