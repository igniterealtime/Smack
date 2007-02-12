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

/**
 * A listener that is fired anytime a participant's status in a room is changed, such as the 
 * user being kicked, banned, or granted admin permissions.
 * 
 * @author Gaston Dombiak
 */
public interface ParticipantStatusListener {

    /**
     * Called when a new room occupant has joined the room. Note: Take in consideration that when
     * you join a room you will receive the list of current occupants in the room. This message will
     * be sent for each occupant.
     *
     * @param participant the participant that has just joined the room
     * (e.g. room@conference.jabber.org/nick).
     */
    public abstract void joined(String participant);

    /**
     * Called when a room occupant has left the room on its own. This means that the occupant was
     * neither kicked nor banned from the room.
     *
     * @param participant the participant that has left the room on its own.
     * (e.g. room@conference.jabber.org/nick).
     */
    public abstract void left(String participant);

    /**
     * Called when a room participant has been kicked from the room. This means that the kicked 
     * participant is no longer participating in the room.
     * 
     * @param participant the participant that was kicked from the room 
     * (e.g. room@conference.jabber.org/nick).
     * @param actor the moderator that kicked the occupant from the room (e.g. user@host.org).
     * @param reason the reason provided by the actor to kick the occupant from the room.
     */
    public abstract void kicked(String participant, String actor, String reason);

    /**
     * Called when a moderator grants voice to a visitor. This means that the visitor 
     * can now participate in the moderated room sending messages to all occupants.
     * 
     * @param participant the participant that was granted voice in the room 
     * (e.g. room@conference.jabber.org/nick).
     */
    public abstract void voiceGranted(String participant);

    /**
     * Called when a moderator revokes voice from a participant. This means that the participant 
     * in the room was able to speak and now is a visitor that can't send messages to the room 
     * occupants.
     * 
     * @param participant the participant that was revoked voice from the room 
     * (e.g. room@conference.jabber.org/nick).
     */
    public abstract void voiceRevoked(String participant);

    /**
     * Called when an administrator or owner banned a participant from the room. This means that 
     * banned participant will no longer be able to join the room unless the ban has been removed.
     * 
     * @param participant the participant that was banned from the room 
     * (e.g. room@conference.jabber.org/nick).
     * @param actor the administrator that banned the occupant (e.g. user@host.org).
     * @param reason the reason provided by the administrator to ban the occupant.
     */
    public abstract void banned(String participant, String actor, String reason);

    /**
     * Called when an administrator grants a user membership to the room. This means that the user 
     * will be able to join the members-only room.
     * 
     * @param participant the participant that was granted membership in the room 
     * (e.g. room@conference.jabber.org/nick).
     */
    public abstract void membershipGranted(String participant);

    /**
     * Called when an administrator revokes a user membership to the room. This means that the 
     * user will not be able to join the members-only room.
     * 
     * @param participant the participant that was revoked membership from the room 
     * (e.g. room@conference.jabber.org/nick).
     */
    public abstract void membershipRevoked(String participant);

    /**
     * Called when an administrator grants moderator privileges to a user. This means that the user 
     * will be able to kick users, grant and revoke voice, invite other users, modify room's 
     * subject plus all the partcipants privileges.
     * 
     * @param participant the participant that was granted moderator privileges in the room 
     * (e.g. room@conference.jabber.org/nick).
     */
    public abstract void moderatorGranted(String participant);

    /**
     * Called when an administrator revokes moderator privileges from a user. This means that the 
     * user will no longer be able to kick users, grant and revoke voice, invite other users, 
     * modify room's subject plus all the partcipants privileges.
     * 
     * @param participant the participant that was revoked moderator privileges in the room 
     * (e.g. room@conference.jabber.org/nick).
     */
    public abstract void moderatorRevoked(String participant);

    /**
     * Called when an owner grants a user ownership on the room. This means that the user 
     * will be able to change defining room features as well as perform all administrative 
     * functions.
     * 
     * @param participant the participant that was granted ownership on the room 
     * (e.g. room@conference.jabber.org/nick).
     */
    public abstract void ownershipGranted(String participant);

    /**
     * Called when an owner revokes a user ownership on the room. This means that the user 
     * will no longer be able to change defining room features as well as perform all 
     * administrative functions.
     * 
     * @param participant the participant that was revoked ownership on the room 
     * (e.g. room@conference.jabber.org/nick).
     */
    public abstract void ownershipRevoked(String participant);

    /**
     * Called when an owner grants administrator privileges to a user. This means that the user 
     * will be able to perform administrative functions such as banning users and edit moderator 
     * list.
     * 
     * @param participant the participant that was granted administrator privileges 
     * (e.g. room@conference.jabber.org/nick).
     */
    public abstract void adminGranted(String participant);

    /**
     * Called when an owner revokes administrator privileges from a user. This means that the user 
     * will no longer be able to perform administrative functions such as banning users and edit 
     * moderator list.
     * 
     * @param participant the participant that was revoked administrator privileges 
     * (e.g. room@conference.jabber.org/nick).
     */
    public abstract void adminRevoked(String participant);

    /**
     * Called when a participant changed his/her nickname in the room. The new participant's 
     * nickname will be informed with the next available presence.
     * 
     * @param participant the participant that was revoked administrator privileges
     * (e.g. room@conference.jabber.org/nick).
     * @param newNickname the new nickname that the participant decided to use.
     */
    public abstract void nicknameChanged(String participant, String newNickname);

}
