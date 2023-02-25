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

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Message;

import org.jivesoftware.smackx.muc.packet.MUCUser;

import org.jxmpp.jid.EntityJid;

/**
 * A listener that is fired anytime an invitation to join a MUC room is received.
 *
 * @author Gaston Dombiak
 */
public interface InvitationListener {

    /**
     * Called when the an invitation to join a MUC room is received.<p>
     *
     * If the room is password-protected, the invitee will receive a password to use to join
     * the room. If the room is members-only, the invitee may be added to the member list.
     *
     * @param conn the XMPPConnection that received the invitation.
     * @param room the room that invitation refers to.
     * @param inviter the inviter that sent the invitation. (e.g. crone1@shakespeare.lit).
     * @param reason the reason why the inviter sent the invitation.
     * @param password the password to use when joining the room.
     * @param message the message used by the inviter to send the invitation.
     * @param invitation the raw invitation received with the message.
     */
    void invitationReceived(XMPPConnection conn, MultiUserChat room, EntityJid inviter, String reason,
                                            String password, Message message, MUCUser.Invite invitation);

}
