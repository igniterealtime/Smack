/**
 *
 * Copyright 2015 Florian Schmaus
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

package org.jivesoftware.smack.roster;

import org.jivesoftware.smack.packet.Presence;
import org.jxmpp.jid.Jid;

import java.util.Collection;

/**
 * Provides empty implementations for {@link RosterListener}.
 *
 * @since 4.2
 */
public abstract class AbstractRosterListener implements RosterListener {

    @Override
    public void entriesAdded(Collection<Jid> addresses) {
    }

    @Override
    public void entriesUpdated(Collection<Jid> addresses) {
    }

    @Override
    public void entriesDeleted(Collection<Jid> addresses) {
    }

    @Override
    public void presenceChanged(Presence presence) {
    }
}
