/**
 *
 * Copyright 2003-2007 Jive Software, 2014-2015 Florian Schmaus
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

import org.jivesoftware.smack.packet.NamedElement;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jivesoftware.smackx.muc.MUCAffiliation;
import org.jivesoftware.smackx.muc.MUCRole;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.parts.Resourcepart;

/**
 * Item child that holds information about roles, affiliation, jids and nicks.
 *
 * @author Gaston Dombiak
 */
public class MUCItem implements NamedElement {
    public static final String ELEMENT = Stanza.ITEM;

    private final MUCAffiliation affiliation;
    private final MUCRole role;
    private final Jid actor;
    private final Resourcepart actorNick;
    private final String reason;
    private final Jid jid;
    private final Resourcepart nick;

    public MUCItem(MUCAffiliation affiliation) {
        this(affiliation, null, null, null, null, null, null);
    }

    public MUCItem(MUCRole role) {
        this(null, role, null, null, null, null, null);
    }

    public MUCItem(MUCRole role, Resourcepart nick) {
        this(null, role, null, null, null, nick, null);
    }

    public MUCItem(MUCAffiliation affiliation, Jid jid, String reason) {
        this(affiliation, null, null, reason, jid, null, null);
    }

    public MUCItem(MUCAffiliation affiliation, Jid jid) {
        this(affiliation, null, null, null, jid, null, null);
    }

    public MUCItem(MUCRole role, Resourcepart nick, String reason) {
        this(null, role, null, reason, null, nick, null);
    }

    /**
     * Creates a new item child.
     * 
     * @param affiliation the actor's affiliation to the room
     * @param role the privilege level of an occupant within a room.
     * @param actor
     * @param reason
     * @param jid
     * @param nick
     * @param actorNick
     */
    public MUCItem(MUCAffiliation affiliation, MUCRole role, Jid actor,
                    String reason, Jid jid, Resourcepart nick, Resourcepart actorNick) {
        this.affiliation = affiliation;
        this.role = role;
        this.actor = actor;
        this.reason = reason;
        this.jid = jid;
        this.nick = nick;
        this.actorNick = actorNick;
    }

    /**
     * Returns the actor (JID of an occupant in the room) that was kicked or banned.
     * 
     * @return the JID of an occupant in the room that was kicked or banned.
     */
    public Jid getActor() {
        return actor;
    }

    /**
     * Get the nickname of the actor.
     *
     * @return the nickname of the actor.
     * @since 4.2
     */
    public Resourcepart getActorNick() {
        return actorNick;
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
    public MUCAffiliation getAffiliation() {
        return affiliation;
    }

    /**
     * Returns the <room@service/nick> by which an occupant is identified within the context of a
     * room. If the room is non-anonymous, the JID will be included in the item.
     * 
     * @return the room JID by which an occupant is identified within the room.
     */
    public Jid getJid() {
        return jid;
    }

    /**
     * Returns the new nickname of an occupant that is changing his/her nickname. The new nickname
     * is sent as part of the unavailable presence.
     * 
     * @return the new nickname of an occupant that is changing his/her nickname.
     */
    public Resourcepart getNick() {
        return nick;
    }

    /**
     * Returns the temporary position or privilege level of an occupant within a room. The possible
     * roles are "moderator", "participant", "visitor" and "none" (it is also possible to have no defined
     * role). A role lasts only for the duration of an occupant's visit to a room.
     * 
     * @return the privilege level of an occupant within a room.
     */
    public MUCRole getRole() {
        return role;
    }

    @Override
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
        xml.closeElement(Stanza.ITEM);
        return xml;
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }
}
