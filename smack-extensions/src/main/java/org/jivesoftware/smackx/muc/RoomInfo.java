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

import java.util.List;
import java.util.logging.Logger;

import org.jivesoftware.smackx.disco.packet.DiscoverInfo;
import org.jivesoftware.smackx.xdata.Form;
import org.jivesoftware.smackx.xdata.FormField;

/**
 * Represents the room information that was discovered using Service Discovery. It's possible to
 * obtain information about a room before joining the room but only for rooms that are public (i.e.
 * rooms that may be discovered).
 *
 * @author Gaston Dombiak
 */
public class RoomInfo {

    private static final Logger LOGGER = Logger.getLogger(RoomInfo.class.getName());

    /**
     * JID of the room. The node of the JID is commonly used as the ID of the room or name.
     */
    private final String room;
    /**
     * Description of the room.
     */
    private final String description;

    /**
     * Short Description of the room.
     */
    private final String shortDescription;

    /**
     * Last known subject of the room.
     */
    private final String subject;
    /**
     * Current number of occupants in the room.
     */
    private final int occupantsCount;
    /**
     * A room is considered members-only if an invitation is required in order to enter the room.
     * Any user that is not a member of the room won't be able to join the room unless the user
     * decides to register with the room (thus becoming a member).
     */
    private final boolean membersOnly;
    /**
     * Moderated rooms enable only participants to speak. Users that join the room and aren't
     * participants can't speak (they are just visitors).
     */
    private final boolean moderated;
    /**
     * Every presence packet can include the JID of every occupant unless the owner deactives this
     * configuration.
     */
    private final boolean nonanonymous;
    /**
     * Indicates if users must supply a password to join the room.
     */
    private final boolean passwordProtected;
    /**
     * Persistent rooms are saved to the database to make sure that rooms configurations can be
     * restored in case the server goes down.
     */
    private final boolean persistent;

    RoomInfo(DiscoverInfo info) {
        this.room = info.getFrom();
        // Get the information based on the discovered features
        this.membersOnly = info.containsFeature("muc_membersonly");
        this.moderated = info.containsFeature("muc_moderated");
        this.nonanonymous = info.containsFeature("muc_nonanonymous");
        this.passwordProtected = info.containsFeature("muc_passwordprotected");
        this.persistent = info.containsFeature("muc_persistent");

        List<DiscoverInfo.Identity> identities = info.getIdentities();
        // XEP-45 6.4 is not really clear on the topic if an identity needs to
        // be send together with the disco result and how to call this description.
        if (!identities.isEmpty()) {
            this.shortDescription = identities.get(0).getName();
        } else {
            LOGGER.fine("DiscoverInfo does not contain any Identity: " + info.toXML());
            this.shortDescription = "";
        }
        String subject = "";
        int occupantsCount = -1;
        String description = "";
        // Get the information based on the discovered extended information
        Form form = Form.getFormFrom(info);
        if (form != null) {
            FormField descField = form.getField("muc#roominfo_description");
            if (descField != null && !descField.getValues().isEmpty()) {
                // Prefer the extended result description
                description = descField.getValues().get(0);
            }

            FormField subjField = form.getField("muc#roominfo_subject");
            if (subjField != null && !subjField.getValues().isEmpty()) {
                subject = subjField.getValues().get(0);
            }

            FormField occCountField = form.getField("muc#roominfo_occupants");
            if (occCountField != null) {
                occupantsCount = Integer.parseInt(occCountField.getValues().get(
                                0));
            }
        }
        this.description = description;
        this.subject = subject;
        this.occupantsCount = occupantsCount;
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
     * Returns the discovered short description.
     * <p>
     * The description return here was provided as value of the name attribute
     * of the returned identity within the disco#info result.
     * </p>
     * 
     * @return the discovered short description of the room.
     */
    public String getShortDesription() {
        return shortDescription;
    }

    /**
     * Returns the discovered description of the room.
     * <p>
     * The description returned by this method was provided as value of the form
     * field of the extended disco info result. It may be <code>null</code>.
     * </p>
     * 
     * @return the discovered description of the room or null
     */
    public String getDescription() {
        return description;
    }

    /**
     * Returns the discovered subject of the room. The subject may be null if the room does not
     * have a subject.
     *
     * @return the discovered subject of the room or null
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
