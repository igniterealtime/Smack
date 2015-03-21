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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.logging.Level;
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
     * Name of the room.
     */
    private final String name;

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
     * Every presence stanza(/packet) can include the JID of every occupant unless the owner deactives this
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

    /**
     * Maximum number of history messages returned by the room.
     */
    private final int maxhistoryfetch;

    /**
     * Contact Address
     */
    private final List<String> contactJid;

    /**
     * Natural Language for Room Discussions
     */
    private final String lang;

    /**
     * An associated LDAP group that defined room membership. Should be an LDAP
     * Distinguished Name
     */
    private final String ldapgroup;

    /**
     * True if the room subject can be modified by participants
     */
    private final Boolean subjectmod;

    /**
     * URL for archived discussion logs
     */
    private final URL logs;

    /**
     * An associated pubsub node
     */
    private final String pubsub;

    /**
     * The rooms extended configuration form;
     */
    private final Form form;

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
            this.name = identities.get(0).getName();
        } else {
            LOGGER.warning("DiscoverInfo does not contain any Identity: " + info.toXML());
            this.name = "";
        }
        String subject = "";
        int occupantsCount = -1;
        String description = "";
        int maxhistoryfetch = -1;
        List<String> contactJid = null;
        String lang = null;
        String ldapgroup = null;
        Boolean subjectmod = null;
        URL logs = null;
        String pubsub = null;
        // Get the information based on the discovered extended information
        form = Form.getFormFrom(info);
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
            if (occCountField != null && !occCountField.getValues().isEmpty()) {
                occupantsCount = Integer.parseInt(occCountField.getValues().get(
                                0));
            }

            FormField maxhistoryfetchField = form.getField("muc#maxhistoryfetch");
            if (maxhistoryfetchField != null && !maxhistoryfetchField.getValues().isEmpty()) {
                maxhistoryfetch = Integer.parseInt(maxhistoryfetchField.getValues().get(
                                0));
            }

            FormField contactJidField = form.getField("muc#roominfo_contactjid");
            if (contactJidField != null && !contactJidField.getValues().isEmpty()) {
                contactJid = contactJidField.getValues();
            }

            FormField langField = form.getField("muc#roominfo_lang");
            if (langField != null && !langField.getValues().isEmpty()) {
                lang = langField.getValues().get(0);
            }

            FormField ldapgroupField = form.getField("muc#roominfo_ldapgroup");
            if (ldapgroupField != null && !ldapgroupField.getValues().isEmpty()) {
                ldapgroup = ldapgroupField.getValues().get(0);
            }

            FormField subjectmodField = form.getField("muc#roominfo_subjectmod");
            if (subjectmodField != null && !subjectmodField.getValues().isEmpty()) {
                subjectmod = Boolean.valueOf(subjectmodField.getValues().get(0));
            }

            FormField urlField = form.getField("muc#roominfo_logs");
            if (urlField != null && !urlField.getValues().isEmpty()) {
                String urlString = urlField.getValues().get(0);
                try {
                    logs = new URL(urlString);
                } catch (MalformedURLException e) {
                    LOGGER.log(Level.SEVERE, "Could not parse URL", e);
                }
            }

            FormField pubsubField = form.getField("muc#roominfo_pubsub");
            if (pubsubField != null && !pubsubField.getValues().isEmpty()) {
                pubsub = pubsubField.getValues().get(0);
            }
        }
        this.description = description;
        this.subject = subject;
        this.occupantsCount = occupantsCount;
        this.maxhistoryfetch = maxhistoryfetch;
        this.contactJid = contactJid;
        this.lang = lang;
        this.ldapgroup = ldapgroup;
        this.subjectmod = subjectmod;
        this.logs = logs;
        this.pubsub = pubsub;
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
     * Returns the room name.
     * <p>
     * The name returnd here was provided as value of the name attribute
     * of the returned identity within the disco#info result.
     * </p>
     * 
     * @return the name of the room.
     */
    public String getName() {
        return name;
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

    /**
     * Returns the maximum number of history messages which are returned by the
     * room or '-1' if this property is not reported by the room.
     *
     * @return the maximum number of history messages or '-1'
     */
    public int getMaxHistoryFetch() {
        return maxhistoryfetch;
    }

    /**
     * Returns Contact Addresses as JIDs, if such are reported.
     *
     * @return a list of contact addresses for this room.
     */
    public List<String> getContactJids() {
        return contactJid;
    }

    /**
     * Returns the natural language of the room discussion, or <code>null</code>.
     *
     * @return the language of the room discussion or <code>null</code>.
     */
    public String getLang() {
        return lang;
    }

    /**
     * Returns an associated LDAP group that defines room membership. The
     * value should be an LDAP Distinguished Name according to an
     * implementation-specific or deployment-specific definition of a group.
     *
     * @return an associated LDAP group or <code>null</code>
     */
    public String getLdapGroup() {
        return ldapgroup;
    }

    /**
     * Returns an Boolean instance with the value 'true' if the subject can be
     * modified by the room participants, 'false' if not, or <code>null</code>
     * if this information is reported by the room.
     *
     * @return an boolean that is true if the subject can be modified by
     *         participants or <code>null</code>
     */
    public Boolean isSubjectModifiable() {
        return subjectmod;
    }

    /**
     * An associated pubsub node for this room or <code>null</code>.
     *
     * @return the associated pubsub node or <code>null</code>
     */
    public String getPubSub() {
        return pubsub;
    }

    /**
     * Returns the URL where archived discussion logs can be found or
     * <code>null</code> if there is no such URL.
     *
     * @return the URL where archived logs can be found or <code>null</code>
     */
    public URL getLogsUrl() {
        return logs;
    }

    /**
     * Returns the form included in the extended disco info result or
     * <code>null</code> if no such form was sent.
     *
     * @return The room info form or <code>null</code>
     * @see <a
     *      href="http://xmpp.org/extensions/xep-0045.html#disco-roominfo">XEP-45:
     *      Multi User Chat - 6.5 Querying for Room Information</a>
     */
    public Form getForm() {
        return form;
    }

}
