/**
* $RCSfile$
* $Revision$
* $Date$
*
* Copyright (C) 2002-2003 Jive Software. All rights reserved.
* ====================================================================
* The Jive Software License (based on Apache Software License, Version 1.1)
*
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions
* are met:
*
* 1. Redistributions of source code must retain the above copyright
*    notice, this list of conditions and the following disclaimer.
*
* 2. Redistributions in binary form must reproduce the above copyright
*    notice, this list of conditions and the following disclaimer in
*    the documentation and/or other materials provided with the
*    distribution.
*
* 3. The end-user documentation included with the redistribution,
*    if any, must include the following acknowledgment:
*       "This product includes software developed by
*        Jive Software (http://www.jivesoftware.com)."
*    Alternately, this acknowledgment may appear in the software itself,
*    if and wherever such third-party acknowledgments normally appear.
*
* 4. The names "Smack" and "Jive Software" must not be used to
*    endorse or promote products derived from this software without
*    prior written permission. For written permission, please
*    contact webmaster@jivesoftware.com.
*
* 5. Products derived from this software may not be called "Smack",
*    nor may "Smack" appear in their name, without prior written
*    permission of Jive Software.
*
* THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
* WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
* OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
* DISCLAIMED.  IN NO EVENT SHALL JIVE SOFTWARE OR
* ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
* SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
* LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
* USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
* ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
* OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
* OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
* SUCH DAMAGE.
* ====================================================================
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
     * Called when a room participant has been kicked from the room. This means that the kicked 
     * participant is no longer participating in the room.
     * 
     * @param participant the participant that was kicked from the room 
     * (e.g. room@conference.jabber.org/nick).
     */
    public abstract void kicked(String participant);

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
     */
    public abstract void banned(String participant);

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
     * @param nickname the old nickname that the participant decided to change.
     */
    public abstract void nicknameChanged(String nickname);

}
