/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright 2003-2007 Jive Software.
 *
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
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

import org.jivesoftware.smackx.packet.MUCAdmin;
import org.jivesoftware.smackx.packet.MUCUser;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.util.StringUtils;

/**
 * Represents the information about an occupant in a given room. The information will always have
 * the affiliation and role of the occupant in the room. The full JID and nickname are optional.
 *
 * @author Gaston Dombiak
 */
public class Occupant {
    // Fields that must have a value
    private String affiliation;
    private String role;
    // Fields that may have a value
    private String jid;
    private String nick;

    Occupant(MUCAdmin.Item item) {
        super();
        this.jid = item.getJid();
        this.affiliation = item.getAffiliation();
        this.role = item.getRole();
        this.nick = item.getNick();
    }

    Occupant(Presence presence) {
        super();
        MUCUser mucUser = (MUCUser) presence.getExtension("x",
                "http://jabber.org/protocol/muc#user");
        MUCUser.Item item = mucUser.getItem();
        this.jid = item.getJid();
        this.affiliation = item.getAffiliation();
        this.role = item.getRole();
        // Get the nickname from the FROM attribute of the presence
        this.nick = StringUtils.parseResource(presence.getFrom());
    }

    /**
     * Returns the full JID of the occupant. If this information was extracted from a presence and
     * the room is semi or full-anonymous then the answer will be null. On the other hand, if this
     * information was obtained while maintaining the voice list or the moderator list then we will
     * always have a full JID.
     *
     * @return the full JID of the occupant.
     */
    public String getJid() {
        return jid;
    }

    /**
     * Returns the affiliation of the occupant. Possible affiliations are: "owner", "admin",
     * "member", "outcast". This information will always be available.
     *
     * @return the affiliation of the occupant.
     */
    public String getAffiliation() {
        return affiliation;
    }

    /**
     * Returns the current role of the occupant in the room. This information will always be
     * available.
     *
     * @return the current role of the occupant in the room.
     */
    public String getRole() {
        return role;
    }

    /**
     * Returns the current nickname of the occupant in the room. If this information was extracted
     * from a presence then the answer will be null.
     *
     * @return the current nickname of the occupant in the room or null if this information was
     *         obtained from a presence.
     */
    public String getNick() {
        return nick;
    }

    public boolean equals(Object obj) {
        if(!(obj instanceof Occupant)) {
            return false;
        }
        Occupant occupant = (Occupant)obj;
        return jid.equals(occupant.jid);
    }

    public int hashCode() {
        int result;
        result = affiliation.hashCode();
        result = 17 * result + role.hashCode();
        result = 17 * result + jid.hashCode();
        result = 17 * result + (nick != null ? nick.hashCode() : 0);
        return result;
    }
}
