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
import org.jivesoftware.smackx.packet.MUCOwner;

/**
 * Represents an affiliation of a user to a given room. The affiliate's information will always have
 * the bare jid of the real user and its affiliation. If the affiliate is an occupant of the room
 * then we will also have information about the role and nickname of the user in the room.
 *
 * @author Gaston Dombiak
 */
public class Affiliate {
    // Fields that must have a value
    private String jid;
    private String affiliation;

    // Fields that may have a value
    private String role;
    private String nick;

    Affiliate(MUCOwner.Item item) {
        super();
        this.jid = item.getJid();
        this.affiliation = item.getAffiliation();
        this.role = item.getRole();
        this.nick = item.getNick();
    }

    Affiliate(MUCAdmin.Item item) {
        super();
        this.jid = item.getJid();
        this.affiliation = item.getAffiliation();
        this.role = item.getRole();
        this.nick = item.getNick();
    }

    /**
     * Returns the bare JID of the affiliated user. This information will always be available.
     *
     * @return the bare JID of the affiliated user.
     */
    public String getJid() {
        return jid;
    }

    /**
     * Returns the affiliation of the afffiliated user. Possible affiliations are: "owner", "admin",
     * "member", "outcast". This information will always be available.
     *
     * @return the affiliation of the afffiliated user.
     */
    public String getAffiliation() {
        return affiliation;
    }

    /**
     * Returns the current role of the affiliated user if the user is currently in the room.
     * If the user is not present in the room then the answer will be null.
     *
     * @return the current role of the affiliated user in the room or null if the user is not in
     *         the room.
     */
    public String getRole() {
        return role;
    }

    /**
     * Returns the current nickname of the affiliated user if the user is currently in the room.
     * If the user is not present in the room then the answer will be null.
     *
     * @return the current nickname of the affiliated user in the room or null if the user is not in
     *         the room.
     */
    public String getNick() {
        return nick;
    }
}
