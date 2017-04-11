/**
 *
 * Copyright © 2014 Florian Schmaus
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

/**
 * Match based on the 'to' attribute of a Stanza.
 *
 * @deprecated use {@link ToMatchesFilter} instead.
 */
@Deprecated
public class ToFilter implements StanzaFilter {

    private final Jid to;

    public ToFilter(Jid to) {
        this.to = to;
    }

    @Override
    public boolean accept(Stanza packet) {
        Jid packetTo = packet.getTo();
        if (packetTo == null) {
            return false;
        }
        return packetTo.equals(to);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ": to=" + to;
    }
}
