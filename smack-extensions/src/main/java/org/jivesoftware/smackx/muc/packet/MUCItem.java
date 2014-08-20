/**
 *
 * Copyright 2003-2007 Jive Software, 2014 Florian Schmaus
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
package org.jivesoftware.smackx.muc.packet;

import org.jivesoftware.smack.packet.Element;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.util.XmlStringBuilder;

/**
 * Item child that holds information about roles, affiliation, jids and nicks.
 *
 * @author Gaston Dombiak
 */
public class MUCItem implements Element {
    public static final String ELEMENT = IQ.ITEM;

    private final String affiliation;
    private String role;
    private String actor;
    private String reason;
    private String jid;
    private String nick;

    /**
     * Creates a new item child.
     * 
     * @param affiliation the actor's affiliation to the room
     */
    public MUCItem(String affiliation) {
        this.affiliation = affiliation;
    }

    /**
     * Creates a new item child.
     * 
     * @param affiliation the actor's affiliation to the room
     * @param role the privilege level of an occupant within a room.
     */
    public MUCItem(String affiliation, String role) {
        this(affiliation);
        this.role = role;
    }

    /**
     * Returns the actor (JID of an occupant in the room) that was kicked or banned.
     * 
     * @return the JID of an occupant in the room that was kicked or banned.
     */
    public String getActor() {
        return actor;
    }

    /**
     * Returns the reason for the item child. The reason is optional and could be used to explain
     * the reason why a user (occupant) was kicked or banned.
     * 
     * @return the reason for the item child.
     */
    public String getReason() {
        return reason;
    }

    /**
     * Returns the occupant's affiliation to the room. The affiliation is a semi-permanent
     * association or connection with a room. The possible affiliations are "owner", "admin",
     * "member", and "outcast" (naturally it is also possible to have no affiliation). An
     * affiliation lasts across a user's visits to a room.
     * 
     * @return the actor's affiliation to the room
     */
    public String getAffiliation() {
        return affiliation;
    }

    /**
     * Returns the <room@service/nick> by which an occupant is identified within the context of a
     * room. If the room is non-anonymous, the JID will be included in the item.
     * 
     * @return the room JID by which an occupant is identified within the room.
     */
    public String getJid() {
        return jid;
    }

    /**
     * Returns the new nickname of an occupant that is changing his/her nickname. The new nickname
     * is sent as part of the unavailable presence.
     * 
     * @return the new nickname of an occupant that is changing his/her nickname.
     */
    public String getNick() {
        return nick;
    }

    /**
     * Returns the temporary position or privilege level of an occupant within a room. The possible
     * roles are "moderator", "participant", and "visitor" (it is also possible to have no defined
     * role). A role lasts only for the duration of an occupant's visit to a room.
     * 
     * @return the privilege level of an occupant within a room.
     */
    public String getRole() {
        return role;
    }

    /**
     * Sets the actor (JID of an occupant in the room) that was kicked or banned.
     * 
     * @param actor the actor (JID of an occupant in the room) that was kicked or banned.
     */
    public void setActor(String actor) {
        this.actor = actor;
    }

    /**
     * Sets the reason for the item child. The reason is optional and could be used to explain the
     * reason why a user (occupant) was kicked or banned.
     * 
     * @param reason the reason why a user (occupant) was kicked or banned.
     */
    public void setReason(String reason) {
        this.reason = reason;
    }

    /**
     * Sets the <room@service/nick> by which an occupant is identified within the context of a room.
     * If the room is non-anonymous, the JID will be included in the item.
     * 
     * @param jid the JID by which an occupant is identified within a room.
     */
    public void setJid(String jid) {
        this.jid = jid;
    }

    /**
     * Sets the new nickname of an occupant that is changing his/her nickname. The new nickname is
     * sent as part of the unavailable presence.
     * 
     * @param nick the new nickname of an occupant that is changing his/her nickname.
     */
    public void setNick(String nick) {
        this.nick = nick;
    }

    /**
     * Sets the temporary position or privilege level of an occupant within a room. The possible
     * roles are "moderator", "participant", and "visitor" (it is also possible to have no defined
     * role). A role lasts only for the duration of an occupant's visit to a room.
     * 
     * @param role the new privilege level of an occupant within a room.
     */
    public void setRole(String role) {
        this.role = role;
    }

    public XmlStringBuilder toXML() {
        XmlStringBuilder xml = new XmlStringBuilder(this);
        xml.optAttribute("affiliation", getAffiliation());
        xml.optAttribute("jid", getJid());
        xml.optAttribute("nick", getNick());
        xml.optAttribute("role", getRole());
        xml.rightAngleBracket();
        xml.optElement("reason", getReason());
        if (getActor() != null) {
            xml.halfOpenElement("actor").attribute("jid", getActor()).closeEmptyElement();
        }
        xml.closeElement(IQ.ITEM);
        return xml;
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }
}
