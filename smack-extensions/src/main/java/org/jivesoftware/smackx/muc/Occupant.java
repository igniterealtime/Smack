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

package org.jivesoftware.smackx.muc;

import java.util.logging.Logger;

import org.jivesoftware.smackx.muc.packet.MUCItem;
import org.jivesoftware.smackx.muc.packet.MUCUser;
import org.jivesoftware.smack.packet.Presence;
import org.jxmpp.jid.EntityFullJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.parts.Resourcepart;

/**
 * Represents the information about an occupant in a given room. The information will always have
 * the affiliation and role of the occupant in the room. The full JID and nickname are optional.
 *
 * @author Gaston Dombiak
 */
public class Occupant {

    private static final Logger LOGGER = Logger.getLogger(Occupant.class.getName());

    // Fields that must have a value
    private final MUCAffiliation affiliation;
    private final MUCRole role;
    // Fields that may have a value
    private final Jid jid;
    private final Resourcepart nick;

    Occupant(MUCItem item) {
        this.jid = item.getJid();
        this.affiliation = item.getAffiliation();
        this.role = item.getRole();
        this.nick = item.getNick();
    }

    Occupant(Presence presence) {
        MUCUser mucUser = (MUCUser) presence.getExtension("x",
                "http://jabber.org/protocol/muc#user");
        MUCItem item = mucUser.getItem();
        this.jid = item.getJid();
        this.affiliation = item.getAffiliation();
        this.role = item.getRole();
        // Get the nickname from the FROM attribute of the presence
        EntityFullJid from = presence.getFrom().asEntityFullJidIfPossible();
        if (from == null) {
            LOGGER.warning("Occupant presence without resource: " + presence.getFrom());
            this.nick = null;
        } else { 
            this.nick = from.getResourcepart();
        }
    }

    /**
     * Returns the full JID of the occupant. If this information was extracted from a presence and
     * the room is semi or full-anonymous then the answer will be null. On the other hand, if this
     * information was obtained while maintaining the voice list or the moderator list then we will
     * always have a full JID.
     *
     * @return the full JID of the occupant.
     */
    public Jid getJid() {
        return jid;
    }

    /**
     * Returns the affiliation of the occupant. Possible affiliations are: "owner", "admin",
     * "member", "outcast". This information will always be available.
     *
     * @return the affiliation of the occupant.
     */
    public MUCAffiliation getAffiliation() {
        return affiliation;
    }

    /**
     * Returns the current role of the occupant in the room. This information will always be
     * available.
     *
     * @return the current role of the occupant in the room.
     */
    public MUCRole getRole() {
        return role;
    }

    /**
     * Returns the current nickname of the occupant in the room. If this information was extracted
     * from a presence then the answer will be null.
     *
     * @return the current nickname of the occupant in the room or null if this information was
     *         obtained from a presence.
     */
    public Resourcepart getNick() {
        return nick;
    }

    @Override
    public boolean equals(Object obj) {
        if(!(obj instanceof Occupant)) {
            return false;
        }
        Occupant occupant = (Occupant)obj;
        return jid.equals(occupant.jid);
    }

    @Override
    public int hashCode() {
        int result;
        result = affiliation.hashCode();
        result = 17 * result + role.hashCode();
        result = 17 * result + jid.hashCode();
        result = 17 * result + (nick != null ? nick.hashCode() : 0);
        return result;
    }
}
