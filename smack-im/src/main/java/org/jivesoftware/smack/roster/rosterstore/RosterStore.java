/**
 *
 * Copyright the original author or authors
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
package org.jivesoftware.smack.roster.rosterstore;

import java.util.Collection;

import org.jivesoftware.smack.roster.packet.RosterPacket;
import org.jxmpp.jid.Jid;

/**
 * This is an interface for persistent roster store needed to implement
 * roster versioning as per RFC 6121.
 */

public interface RosterStore {

    /**
     * This method returns a collection of all roster items contained in this store.
     * @return List of {@link org.jivesoftware.smack.roster.RosterEntry}
     */
    public Collection<RosterPacket.Item> getEntries();
    /**
     * This method returns the roster item in this store for the given JID.
     * @param bareJid The bare JID of the RosterEntry
     * @return The {@link org.jivesoftware.smack.roster.RosterEntry} which belongs to that user
     */
    public RosterPacket.Item getEntry(Jid bareJid);
    /**
     * This method returns the version number as specified by the "ver" attribute
     * of the local store. For a fresh store, this MUST be the empty string.
     * @return local roster version
     */
    public String getRosterVersion();
    /**
     * This method stores a new roster entry in this store or updates an existing one.
     * @param item the entry to store
     * @param version the new roster version
     * @return True if successful
     */
    public boolean addEntry(RosterPacket.Item item, String version);
    /**
     * This method updates the store so that it contains only the given entries.
     * @param items the entries to store
     * @param version the new roster version
     * @return True if successful
     */
    public boolean resetEntries(Collection<RosterPacket.Item> items, String version);
    /**
     * Removes an entry from the store
     * @param bareJid The bare JID of the entry to be removed
     * @param version the new roster version
     * @return True if successful
     */
    public boolean removeEntry(Jid bareJid, String version);

}
