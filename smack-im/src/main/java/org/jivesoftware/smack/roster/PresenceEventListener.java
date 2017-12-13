/**
 *
 * Copyright 2016 Florian Schmaus
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

import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.FullJid;
import org.jxmpp.jid.Jid;

public interface PresenceEventListener {

    void presenceAvailable(FullJid address, Presence availablePresence);

    void presenceUnavailable(FullJid address, Presence presence);

    void presenceError(Jid address, Presence errorPresence);

    void presenceSubscribed(BareJid address, Presence subscribedPresence);

    void presenceUnsubscribed(BareJid address, Presence unsubscribedPresence);
}
