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

import org.jivesoftware.smackx.Form;
import org.jivesoftware.smackx.packet.DiscoverInfo;

import java.util.Iterator;

/**
 * Represents the room information that was discovered using Service Discovery. It's possible to
 * obtain information about a room before joining the room but only for rooms that are public (i.e.
 * rooms that may be discovered).
 *
 * @author Gaston Dombiak
 */
public class RoomInfo {

    /**
     * JID of the room. The node of the JID is commonly used as the ID of the room or name.
     */
    private String room;
    /**
     * Description of the room.
     */
    private String description = "";
    /**
     * Last known subject of the room.
     */
    private String subject = "";
    /**
     * Current number of occupants in the room.
     */
    private int occupantsCount = -1;
    /**
     * A room is considered members-only if an invitation is required in order to enter the room.
     * Any user that is not a member of the room won't be able to join the room unless the user
     * decides to register with the room (thus becoming a member).
     */
    private boolean membersOnly;
    /**
     * Moderated rooms enable only participants to speak. Users that join the room and aren't
     * participants can't speak (they are just visitors).
     */
    private boolean moderated;
    /**
     * Every presence packet can include the JID of every occupant unless the owner deactives this
     * configuration.
     */
    private boolean nonanonymous;
    /**
     * Indicates if users must supply a password to join the room.
     */
    private boolean passwordProtected;
    /**
     * Persistent rooms are saved to the database to make sure that rooms configurations can be
     * restored in case the server goes down.
     */
    private boolean persistent;

    RoomInfo(DiscoverInfo info) {
        super();
        this.room = info.getFrom();
        // Get the information based on the discovered features
        this.membersOnly = info.containsFeature("muc_membersonly");
        this.moderated = info.containsFeature("muc_moderated");
        this.nonanonymous = info.containsFeature("muc_nonanonymous");
        this.passwordProtected = info.containsFeature("muc_passwordprotected");
        this.persistent = info.containsFeature("muc_persistent");
        // Get the information based on the discovered extended information
        Form form = Form.getFormFrom(info);
        if (form != null) {
            this.description =
                    form.getField("muc#roominfo_description").getValues().next();
            Iterator<String> values = form.getField("muc#roominfo_subject").getValues();
            if (values.hasNext()) {
                this.subject = values.next();
            }
            else {
                this.subject = "";
            }
            this.occupantsCount =
                    Integer.parseInt(form.getField("muc#roominfo_occupants").getValues()
                    .next());
        }
    }

    /**
     * Returns the JID of the room whose information was discovered.
     *
     * @return the JID of the room whose information was discovered.
     */
    public String getRoom() {
        return room;
    }

    /**
     * Returns the discovered description of the room.
     *
     * @return the discovered description of the room.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the discovered subject of the room. The subject may be empty if the room does not
     * have a subject.
     *
     * @return the discovered subject of the room.
     */
    public String getSubject() {
        return subject;
    }

    /**
     * Returns the discovered number of occupants that are currently in the room. If this
     * information was not discovered (i.e. the server didn't send it) then a value of -1 will be
     * returned.
     *
     * @return the number of occupants that are currently in the room or -1 if that information was
     * not provided by the server.
     */
    public int getOccupantsCount() {
        return occupantsCount;
    }

    /**
     * Returns true if the room has restricted the access so that only members may enter the room.
     *
     * @return true if the room has restricted the access so that only members may enter the room.
     */
    public boolean isMembersOnly() {
        return membersOnly;
    }

    /**
     * Returns true if the room enabled only participants to speak. Occupants with a role of
     * visitor won't be able to speak in the room.
     *
     * @return true if the room enabled only participants to speak.
     */
    public boolean isModerated() {
        return moderated;
    }

    /**
     * Returns true if presence packets will include the JID of every occupant.
     *
     * @return true if presence packets will include the JID of every occupant.
     */
    public boolean isNonanonymous() {
        return nonanonymous;
    }

    /**
     * Returns true if users musy provide a valid password in order to join the room.
     *
     * @return true if users musy provide a valid password in order to join the room.
     */
    public boolean isPasswordProtected() {
        return passwordProtected;
    }

    /**
     * Returns true if the room will persist after the last occupant have left the room.
     *
     * @return true if the room will persist after the last occupant have left the room.
     */
    public boolean isPersistent() {
        return persistent;
    }

}
