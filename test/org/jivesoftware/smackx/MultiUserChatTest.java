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

package org.jivesoftware.smackx;

import java.util.Iterator;

import org.jivesoftware.smack.*;
import org.jivesoftware.smack.filter.*;
import org.jivesoftware.smack.packet.*;

import junit.framework.TestCase;

/**
 * Tests the new MUC functionalities.
 * 
 * @author Gaston Dombiak
 */
public class MultiUserChatTest extends TestCase {

    private String host = "gatoux";
    private String room = "fruta124@conference." + host;

    private XMPPConnection conn1 = null;
    private XMPPConnection conn2 = null;
    private XMPPConnection conn3 = null;

    private String user1 = null;
    private String user2 = null;
    private String user3 = null;

    private MultiUserChat muc;

    /**
     * Constructor for MultiUserChatTest.
     * @param arg0
     */
    public MultiUserChatTest(String arg0) {
        super(arg0);
    }

    public void testParticipantPresence() {
        try {
            // User2 joins the new room
            MultiUserChat muc2 = new MultiUserChat(conn2, room);
            muc2.join("testbot2",SmackConfiguration.getPacketReplyTimeout(),null,0,-1,-1,null);
            Thread.sleep(300);
            
            // User1 checks the presence of user2 in the room
            Presence presence = muc.getParticipantPresence(room + "/testbot2");
            assertNotNull("Presence of user2 in room is missing", presence);
            assertEquals(
                "Presence mode of user2 is wrong",
                Presence.Mode.AVAILABLE,
                presence.getMode());

            // User2 changes his availability to AWAY
            muc2.changeAvailabilityStatus("Gone to have lunch", Presence.Mode.AWAY);
            Thread.sleep(200);
            // User1 checks the presence of user2 in the room
            presence = muc.getParticipantPresence(room + "/testbot2");
            assertNotNull("Presence of user2 in room is missing", presence);
            assertEquals("Presence mode of user2 is wrong", Presence.Mode.AWAY, presence.getMode());
            assertEquals(
                "Presence status of user2 is wrong",
                "Gone to have lunch",
                presence.getStatus());

            // User2 changes his nickname
            muc2.changeNickname("testbotII");
            Thread.sleep(200);
            // User1 checks the presence of user2 in the room
            presence = muc.getParticipantPresence(room + "/testbot2");
            assertNull("Presence of participant testbot2 still exists", presence);
            presence = muc.getParticipantPresence(room + "/testbotII");
            assertNotNull("Presence of participant testbotII does not exist", presence);
            assertEquals(
                "Presence of participant testbotII has a wrong from",
                room + "/testbotII",
                presence.getFrom());

            // User2 leaves the room
            muc2.leave();
            Thread.sleep(200);
            // User1 checks the presence of user2 in the room
            presence = muc.getParticipantPresence(room + "/testbotII");
            assertNull("Presence of participant testbotII still exists", presence);

        }
        catch (Exception e) {
            fail(e.getMessage());
        }
    }

    public void testInvitation() {
        final String[] answer = new String[2];
        try {
            // User2 joins the new room
            MultiUserChat muc2 = new MultiUserChat(conn2, room);
            muc2.join("testbot2",SmackConfiguration.getPacketReplyTimeout(),null,0,-1,-1,null);

            // User3 is listening to MUC invitations
            MultiUserChat.addInvitationListener(conn3, new InvitationListener() {
                public void invitationReceived(
                    XMPPConnection conn,
                    String room,
                    String inviter,
                    String reason,
                    String password) {
                        // Indicate that the invitation was received 
                        answer[0] = reason;
                        // Reject the invitation
                        MultiUserChat.decline(conn, room, inviter, "I'm busy right now");
                }
            });

            // User2 is listening to invitation rejections            
            muc2.addInvitationRejectionListener(new InvitationRejectionListener() {
                public void invitationDeclined(String invitee, String reason) {
                    // Indicate that the rejection was received 
                    answer[1] = reason;
                }
            });

            // User2 invites user3 to join to the room
            muc2.invite(user3, "Meet me in this excellent room");
            Thread.sleep(300);
            
            assertEquals("Invitation was not received", "Meet me in this excellent room", answer[0]);
            // TODO This line was commented because jabberd2 is not accepting rejections 
            // from users that aren't participants of the room. Remove the commented line when  
            // running the test against a server that correctly implements JEP-45  
            //assertEquals("Rejection was not received", "I'm busy right now", answer[1]);
        }
        catch (Exception e) {
            fail(e.getMessage());
        }
    }

    // TODO This test is commented because jabberd2 is responding an incorrect disco packet
    /*public void testDiscoverJoinedRooms() {
        try {
            // Check that user1 has joined only to one room
            Iterator joinedRooms = MultiUserChat.getJoinedRooms(conn2, user1);
            assertTrue("Joined rooms shouldn't be empty", joinedRooms.hasNext());
            assertEquals(
                "Joined room is incorrect",
                joinedRooms.next(),
                room);
            assertFalse("User has joined more than one room", joinedRooms.hasNext());
    
            // Leave the new room
            muc.leave();
    
            // Check that user1 is not currently join any room
            joinedRooms = MultiUserChat.getJoinedRooms(conn2, user1);
            assertFalse("Joined rooms should be empty", joinedRooms.hasNext());

            muc.join("testbot");
        }
        catch (XMPPException e) {
            fail(e.getMessage());
        }
    }*/
    
    public void testPrivateChat() {
        try {
            // User2 joins the new room
            MultiUserChat muc2 = new MultiUserChat(conn2, room);
            muc2.join("testbot2",SmackConfiguration.getPacketReplyTimeout(),null,0,-1,-1,null);

            conn1.addPacketListener(new PacketListener() {
                public void processPacket(Packet packet) {
                    Message message = (Message) packet;
                    Chat chat2 = new Chat(conn1, message.getFrom(), message.getThread());
                    assertEquals(
                        "Sender of chat is incorrect",
                        room + "/testbot2",
                        message.getFrom());
                    try {
                        chat2.sendMessage("ACK");
                    }
                    catch (XMPPException e) {
                        fail(e.getMessage());
                    }
                }
            },
                new AndFilter(
                    new MessageTypeFilter(Message.Type.CHAT),
                    new PacketTypeFilter(Message.class))
            );

            // Start a private chat with another participant            
            Chat chat = muc2.createPrivateChat(room + "/testbot");
            chat.sendMessage("Hello there");
            
            Message response = chat.nextMessage(2000);
            assertEquals("Sender of response is incorrect",room + "/testbot", response.getFrom());
            assertEquals("Body of response is incorrect","ACK", response.getBody());
        }
        catch (Exception e) {
            fail(e.getMessage());
        }
    }
    
    // TODO This test is commented because jabberd2 doesn't support discovering reserved nicknames
    /*public void testReservedNickname() {
        // User2 joins the new room
        MultiUserChat muc2 = new MultiUserChat(conn2, room);
        
        String reservedNickname = muc2.getReservedNickname();
        assertNull("Reserved nickname is not null", reservedNickname);
    }*/

    protected void setUp() throws Exception {
        super.setUp();
        try {
            // Connect to the server
            conn1 = new XMPPConnection(host);
            conn2 = new XMPPConnection(host);
            conn3 = new XMPPConnection(host);

            // Create the test accounts
            if (!conn1.getAccountManager().supportsAccountCreation())
                fail("Server does not support account creation");
            conn1.getAccountManager().createAccount("gato3", "gato3");
            conn2.getAccountManager().createAccount("gato4", "gato4");
            conn3.getAccountManager().createAccount("gato5", "gato5");

            // Login with the test accounts
            conn1.login("gato3", "gato3");
            conn2.login("gato4", "gato4");
            conn3.login("gato5", "gato5");

            user1 = "gato3@" + conn1.getHost();
            user2 = "gato4@" + conn2.getHost();
            user3 = "gato5@" + conn2.getHost();

            // User1 creates the room
            muc = new MultiUserChat(conn1, room);
            muc.create("testbot");

            // User1 sends an empty room configuration form which indicates that we want
            // an instant room
            muc.sendConfigurationForm(new Form(Form.TYPE_SUBMIT));

        }
        catch (Exception e) {
            fail(e.getMessage());
        }
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        // Destroy the new room
        muc.destroy("The room has almost no activity...", null);

        // Delete the created accounts for the test
        conn1.getAccountManager().deleteAccount();
        conn2.getAccountManager().deleteAccount();
        conn3.getAccountManager().deleteAccount();

        // Close all the connections
        conn1.close();
        conn2.close();
        conn3.close();
    }
}
