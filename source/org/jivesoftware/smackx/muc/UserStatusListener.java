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
 * A listener that is fired anytime your participant's status in a room is changed, such as the 
 * user being kicked, banned, or granted admin permissions.
 * 
 * @author Gaston Dombiak
 */
public interface UserStatusListener {

    /**
     * Called when a moderator kicked your user from the room. This means that you are no longer
     * participanting in the room.
     * 
     * @param actor the moderator that kicked your user from the room (e.g. user@host.org).
     * @param reason the reason provided by the actor to kick you from the room.
     */
    public abstract void kicked(String actor, String reason);

    /**
     * Called when a moderator grants voice to your user. This means that you were a visitor in 
     * the moderated room before and now you can participate in the room by sending messages to 
     * all occupants.
     * 
     */
    public abstract void voiceGranted();

    /**
     * Called when a moderator revokes voice from your user. This means that you were a 
     * participant in the room able to speak and now you are a visitor that can't send 
     * messages to the room occupants.
     * 
     */
    public abstract void voiceRevoked();

    /**
     * Called when an administrator or owner banned your user from the room. This means that you 
     * will no longer be able to join the room unless the ban has been removed.
     * 
     * @param actor the administrator that banned your user (e.g. user@host.org).
     * @param reason the reason provided by the administrator to banned you.
     */
    public abstract void banned(String actor, String reason);

    /**
     * Called when an administrator grants your user membership to the room. This means that you 
     * will be able to join the members-only room. 
     * 
     */
    public abstract void membershipGranted();

    /**
     * Called when an administrator revokes your user membership to the room. This means that you 
     * will not be able to join the members-only room.
     * 
     */
    public abstract void membershipRevoked();

    /**
     * Called when an administrator grants moderator privileges to your user. This means that you 
     * will be able to kick users, grant and revoke voice, invite other users, modify room's 
     * subject plus all the partcipants privileges.
     * 
     */
    public abstract void moderatorGranted();

    /**
     * Called when an administrator revokes moderator privileges from your user. This means that 
     * you will no longer be able to kick users, grant and revoke voice, invite other users, 
     * modify room's subject plus all the partcipants privileges.
     * 
     */
    public abstract void moderatorRevoked();

    /**
     * Called when an owner grants to your user ownership on the room. This means that you 
     * will be able to change defining room features as well as perform all administrative 
     * functions.
     * 
     */
    public abstract void ownershipGranted();

    /**
     * Called when an owner revokes from your user ownership on the room. This means that you 
     * will no longer be able to change defining room features as well as perform all 
     * administrative functions.
     * 
     */
    public abstract void ownershipRevoked();

    /**
     * Called when an owner grants administrator privileges to your user. This means that you 
     * will be able to perform administrative functions such as banning users and edit moderator 
     * list.
     * 
     */
    public abstract void adminGranted();

    /**
     * Called when an owner revokes administrator privileges from your user. This means that you 
     * will no longer be able to perform administrative functions such as banning users and edit 
     * moderator list.
     * 
     */
    public abstract void adminRevoked();

}
