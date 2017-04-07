/**
 *
 * Copyright 2003-2007 Jive Software.
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
import org.jivesoftware.smack.util.StringUtils;

/**
 * Filters for Stanzas with a particular stanza ID.
 *
 * @author Matt Tucker
 */
public class StanzaIdFilter implements StanzaFilter {

    private final String stanzaId;

    /**
     * Creates a new stanza ID filter using the specified stanza's ID.
     *
     * @param stanza the stanza which the ID is taken from.
     */
    public StanzaIdFilter(Stanza stanza) {
        this(stanza.getStanzaId());
    }

    /**
     * Creates a new stanza ID filter using the specified stanza ID.
     *
     * @param stanzaID the stanza ID to filter for.
     */
    public StanzaIdFilter(String stanzaID) {
        this.stanzaId = StringUtils.requireNotNullOrEmpty(stanzaID, "Stanza ID must not be null or empty.");
    }

    @Override
    public boolean accept(Stanza stanza) {
        return stanzaId.equals(stanza.getStanzaId());
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ": id=" + stanzaId;
    }
}
