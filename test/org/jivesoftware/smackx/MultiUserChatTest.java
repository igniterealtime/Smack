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
            e.printStackTrace();
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
            Thread.sleep(350);
            
            assertEquals(
                "Invitation was not received",
                "Meet me in this excellent room",
                answer[0]);
            // TODO This line was commented because jabberd2 is not accepting rejections 
            // from users that aren't participants of the room. Comment out this line when  
            // running the test against a server that correctly implements JEP-45  
            //assertEquals("Rejection was not received", "I'm busy right now", answer[1]);
            
            // User2 leaves the room
            muc2.leave();
        }
        catch (Exception e) {
            fail(e.getMessage());
            e.printStackTrace();
        }
    }

    public void testDiscoverJoinedRooms() {
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
            e.printStackTrace();
        }
    }
    
    public void testDiscoverMUCSupport() {
        // Discover user1 support of MUC
        boolean supports = MultiUserChat.isServiceEnabled(conn2, user1);
        assertTrue("Couldn't detect that user1 supports MUC", supports);
    }

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

            // User2 leaves the room
            muc2.leave();
        }
        catch (Exception e) {
            fail(e.getMessage());
            e.printStackTrace();
        }
    }
    
    // TODO This test is commented because jabberd2 doesn't support discovering reserved nicknames
    /*public void testReservedNickname() {
        // User2 joins the new room
        MultiUserChat muc2 = new MultiUserChat(conn2, room);
        
        String reservedNickname = muc2.getReservedNickname();
        assertNull("Reserved nickname is not null", reservedNickname);
    }*/

    public void testChangeSubject() {
        final String[] answer = new String[2];
        try {
            // User1 sets an initial subject
            muc.changeSubject("Initial Subject");

            // User2 joins the new room
            MultiUserChat muc2 = new MultiUserChat(conn2, room);
            muc2.join("testbot2",SmackConfiguration.getPacketReplyTimeout(),null,0,-1,-1,null);
            
            // User3 joins the new room
            MultiUserChat muc3 = new MultiUserChat(conn3, room);
            muc3.join("testbot3",SmackConfiguration.getPacketReplyTimeout(),null,0,-1,-1,null);

            // User3 wants to be notified every time the room's subject is changed.
            muc3.addSubjectUpdatedListener(new SubjectUpdatedListener() {
                public void subjectUpdated(String subject, String from) {
                    answer[0] = subject;
                    answer[1] = from;
                }
            });

            // Check that a 403 error is received when a not allowed user tries to change the 
            // subject in a room
            try {
                muc2.changeSubject("New Subject2");
                fail("User2 was allowed to change the room's subject");
            }
            catch (XMPPException e) {
                XMPPError xmppError = e.getXMPPError();
                assertNotNull(
                    "No XMPPError was received when changing the room's subject",
                    xmppError);
                assertEquals(
                    "Different error code was received while changing the room's subject",
                    403,
                    xmppError.getCode());
            }
            
            // Check that every MUC updates its subject when an allowed user changes the subject 
            // in a room
            muc.changeSubject("New Subject1");
            Thread.sleep(300);
            // Check that User2's MUC has updated its subject
            assertEquals(
                "User2 didn't receive the subject notification",
                "New Subject1",
                muc2.getSubject());
            // Check that SubjectUpdatedListener is working OK
            assertEquals(
                "User3 didn't receive the subject notification",
                "New Subject1",
                answer[0]);
            assertEquals(
                "User3 didn't receive the correct user that changed the subject",
                room + "/testbot",
                answer[1]);

            // User2 leaves the room
            muc2.leave();
            // User3 leaves the room
            muc3.leave();
        }
        catch (Exception e) {
            fail(e.getMessage());
            e.printStackTrace();
        }
    }

    public void testKickParticipant() {
        final String[] answer = new String[3];
        try {
            // User2 joins the new room
            MultiUserChat muc2 = new MultiUserChat(conn2, room);
            muc2.join("testbot2",SmackConfiguration.getPacketReplyTimeout(),null,0,-1,-1,null);
            // User2 will lister for his own "kicking"            
            muc2.addUserStatusListener(new DefaultUserStatusListener() {
                public void kicked(String actor, String reason) {
                    super.kicked(actor, reason);
                    answer[0] = actor;
                    answer[1] = reason;
                }
            });

            // User3 joins the new room
            MultiUserChat muc3 = new MultiUserChat(conn3, room);
            muc3.join("testbot3",SmackConfiguration.getPacketReplyTimeout(),null,0,-1,-1,null);
            // User3 will lister for user2's "kicking"            
            muc3.addParticipantStatusListener(new DefaultParticipantStatusListener() {
                public void kicked(String participant) {
                    super.kicked(participant);
                    answer[2] = participant;
                }
            });

            try {
                // Check whether a simple participant can kick a room owner or not
                muc2.kickParticipant("testbot", "Because I'm bad");
                fail("User2 was able to kick a room owner");
            }
            catch (XMPPException e) {
                XMPPError xmppError = e.getXMPPError();
                assertNotNull(
                    "No XMPPError was received when kicking a room owner",
                    xmppError);
                assertEquals(
                    "A simple participant was able to kick another participant from the room",
                    403,
                    xmppError.getCode());
            }
            
            // Check that the room's owner can kick a simple participant
            muc.kickParticipant("testbot2", "Because I'm the owner");
            Thread.sleep(300);

            assertNull("User2 wasn't kicked from the room", muc.getParticipantPresence(room + "/testbot2"));
            
            assertFalse("User2 thinks that he's still in the room", muc2.isJoined());

            // Check that UserStatusListener is working OK
            assertEquals(
                "User2 didn't receive the correct initiator of the kick",
                "gato3@" + conn1.getHost(),
                answer[0]);
            assertEquals(
                "User2 didn't receive the correct reason for the kick",
                "Because I'm the owner",
                answer[1]);

            // Check that ParticipantStatusListener is working OK
            assertEquals(
                "User3 didn't receive the correct kicked participant",
                room + "/testbot2",
                answer[2]);
            
            // User2 leaves the room
            muc2.leave();
            // User3 leaves the room
            muc3.leave();
        }
        catch (Exception e) {
            fail(e.getMessage());
            e.printStackTrace();
        }
    }

    public void testBanUser() {
        final String[] answer = new String[3];
        try {
            // User2 joins the new room
            MultiUserChat muc2 = new MultiUserChat(conn2, room);
            muc2.join("testbot2",SmackConfiguration.getPacketReplyTimeout(),null,0,-1,-1,null);
            // User2 will lister for his own "banning"            
            muc2.addUserStatusListener(new DefaultUserStatusListener() {
                public void banned(String actor, String reason) {
                    super.banned(actor, reason);
                    answer[0] = actor;
                    answer[1] = reason;
                }
            });

            // User3 joins the new room
            MultiUserChat muc3 = new MultiUserChat(conn3, room);
            muc3.join("testbot3",SmackConfiguration.getPacketReplyTimeout(),null,0,-1,-1,null);
            // User3 will lister for user2's "banning"            
            muc3.addParticipantStatusListener(new DefaultParticipantStatusListener() {
                public void banned(String participant) {
                    super.banned(participant);
                    answer[2] = participant;
                }
            });

            try {
                // Check whether a simple participant can ban a room owner or not
                muc2.banUser("gato3@" + conn2.getHost(), "Because I'm bad");
                fail("User2 was able to ban a room owner");
            }
            catch (XMPPException e) {
                XMPPError xmppError = e.getXMPPError();
                assertNotNull(
                    "No XMPPError was received when banning a room owner",
                    xmppError);
                assertEquals(
                    "A simple participant was able to ban another participant from the room",
                    403,
                    xmppError.getCode());
            }
            
            // Check that the room's owner can ban a simple participant
            muc.banUser("gato4@" + conn2.getHost(), "Because I'm the owner");
            Thread.sleep(300);

            assertNull("User2 wasn't banned from the room", muc.getParticipantPresence(room + "/testbot2"));
            
            assertFalse("User2 thinks that he's still in the room", muc2.isJoined());

            // Check that UserStatusListener is working OK
            assertEquals(
                "User2 didn't receive the correct initiator of the ban",
                "gato3@" + conn1.getHost(),
                answer[0]);
            assertEquals(
                "User2 didn't receive the correct reason for the banning",
                "Because I'm the owner",
                answer[1]);

            // Check that ParticipantStatusListener is working OK
            assertEquals(
                "User3 didn't receive the correct banned JID",
                room + "/testbot2",
                answer[2]);
            
            // User2 leaves the room
            muc2.leave();
            // User3 leaves the room
            muc3.leave();
        }
        catch (Exception e) {
            fail(e.getMessage());
            e.printStackTrace();
        }
    }

    public void testVoice() {
        final String[] answer = new String[4];
        try {

            makeRoomModerated();
            
            // User2 joins the new room (as a visitor)
            MultiUserChat muc2 = new MultiUserChat(conn2, room);
            muc2.join("testbot2",SmackConfiguration.getPacketReplyTimeout(),null,0,-1,-1,null);
            // User2 will listen for his own "voice"            
            muc2.addUserStatusListener(new DefaultUserStatusListener() {
                public void voiceGranted() {
                    super.voiceGranted();
                    answer[0] = "canSpeak";
                }
                public void voiceRevoked() {
                    super.voiceRevoked();
                    answer[1] = "cannot speak";
                }
            });

            // User3 joins the new room (as a visitor)
            MultiUserChat muc3 = new MultiUserChat(conn3, room);
            muc3.join("testbot3",SmackConfiguration.getPacketReplyTimeout(),null,0,-1,-1,null);
            // User3 will lister for user2's "voice"            
            muc3.addParticipantStatusListener(new DefaultParticipantStatusListener() {
                public void voiceGranted(String participant) {
                    super.voiceGranted(participant);
                    answer[2] = participant;
                }

                public void voiceRevoked(String participant) {
                    super.voiceRevoked(participant);
                    answer[3] = participant;
                }
            });

            try {
                // Check whether a visitor can grant voice to another visitor
                muc2.grantVoice("testbot3");
                fail("User2 was able to grant voice");
            }
            catch (XMPPException e) {
                XMPPError xmppError = e.getXMPPError();
                assertNotNull(
                    "No XMPPError was received granting voice",
                    xmppError);
                assertEquals(
                    "A visitor was able to grant voice to another visitor",
                    403,
                    xmppError.getCode());
            }
            
            // Check that the room's owner can grant voice to a participant
            muc.grantVoice("testbot2");
            Thread.sleep(300);

            // Check that UserStatusListener is working OK
            assertEquals(
                "User2 didn't receive the grant voice notification",
                "canSpeak",
                answer[0]);
            // Check that ParticipantStatusListener is working OK
            assertEquals(
                "User3 didn't receive user2's grant voice notification",
                room + "/testbot2",
                answer[2]);

            // Check that the room's owner can revoke voice from a participant
            muc.revokeVoice("testbot2");
            Thread.sleep(300);

            assertEquals(
                "User2 didn't receive the revoke voice notification",
                "cannot speak",
                answer[1]);
            assertEquals(
                "User3 didn't receive user2's revoke voice notification",
                room + "/testbot2",
                answer[3]);

            // User2 leaves the room
            muc2.leave();
            // User3 leaves the room
            muc3.leave();
        }
        catch (Exception e) {
            fail(e.getMessage());
            e.printStackTrace();
        }
    }

    public void testModerator() {
        final String[] answer = new String[8];
        try {

            makeRoomModerated();
            
            // User2 joins the new room (as a visitor)
            MultiUserChat muc2 = new MultiUserChat(conn2, room);
            muc2.join("testbot2",SmackConfiguration.getPacketReplyTimeout(),null,0,-1,-1,null);
            // User2 will listen for moderator privileges            
            muc2.addUserStatusListener(new DefaultUserStatusListener() {
                public void voiceGranted() {
                    super.voiceGranted();
                    answer[0] = "canSpeak";
                }
                public void voiceRevoked() {
                    super.voiceRevoked();
                    answer[1] = "cannot speak";
                }
                public void moderatorGranted() {
                    super.moderatorGranted();
                    answer[4] = "I'm a moderator";
                }
                public void moderatorRevoked() {
                    super.moderatorRevoked();
                    answer[5] = "I'm not a moderator";
                }
            });

            // User3 joins the new room (as a visitor)
            MultiUserChat muc3 = new MultiUserChat(conn3, room);
            muc3.join("testbot3",SmackConfiguration.getPacketReplyTimeout(),null,0,-1,-1,null);
            // User3 will lister for user2's moderator privileges            
            muc3.addParticipantStatusListener(new DefaultParticipantStatusListener() {
                public void voiceGranted(String participant) {
                    super.voiceGranted(participant);
                    answer[2] = participant;
                }
                public void voiceRevoked(String participant) {
                    super.voiceRevoked(participant);
                    answer[3] = participant;
                }
                public void moderatorGranted(String participant) {
                    super.moderatorGranted(participant);
                    answer[6] = participant;
                }
                public void moderatorRevoked(String participant) {
                    super.moderatorRevoked(participant);
                    answer[7] = participant;
                }
            });

            try {
                // Check whether a visitor can grant moderator privileges to another visitor
                muc2.grantModerator("testbot3");
                fail("User2 was able to grant moderator privileges");
            }
            catch (XMPPException e) {
                XMPPError xmppError = e.getXMPPError();
                assertNotNull(
                    "No XMPPError was received granting moderator privileges",
                    xmppError);
                assertEquals(
                    "A visitor was able to grant moderator privileges to another visitor",
                    403,
                    xmppError.getCode());
            }
            
            // Check that the room's owner can grant moderator privileges to a visitor
            muc.grantModerator("testbot2");
            Thread.sleep(300);

            // Check that UserStatusListener is working OK
            assertEquals(
                "User2 didn't receive the grant voice notification",
                "canSpeak",
                answer[0]);
            assertEquals(
                "User2 didn't receive the grant moderator privileges notification",
                "I'm a moderator",
                answer[4]);
            // Check that ParticipantStatusListener is working OK
            assertEquals(
                "User3 didn't receive user2's grant voice notification",
                room + "/testbot2",
                answer[2]);
            assertEquals(
                "User3 didn't receive user2's grant moderator privileges notification",
                room + "/testbot2",
                answer[6]);

            // Check that the room's owner can revoke moderator privileges from a moderator
            muc.revokeModerator("testbot2");
            Thread.sleep(300);

            assertNull(
                "User2 received a false revoke voice notification",
                answer[1]);
            assertNull(
                "User3 received a false user2's voice privileges notification",
                answer[3]);
            assertEquals(
                "User2 didn't receive the revoke moderator privileges notification",
                "I'm not a moderator",
                answer[5]);
            assertEquals(
                "User3 didn't receive user2's revoke moderator privileges notification",
                room + "/testbot2",
                answer[7]);

            
            // Check that the room's owner can grant moderator privileges to a participant
            clearAnswer(answer);
            muc.grantModerator("testbot2");
            Thread.sleep(300);

            // Check that UserStatusListener is working OK
            assertNull(
                "User2 received a false grant voice notification",
                answer[0]);
            assertEquals(
                "User2 didn't receive the grant moderator privileges notification",
                "I'm a moderator",
                answer[4]);
            // Check that ParticipantStatusListener is working OK
            assertNull(
                "User3 received a false user2's grant voice notification",
                answer[2]);
            assertEquals(
                "User3 didn't receive user2's grant moderator privileges notification",
                room + "/testbot2",
                answer[6]);

            // Check that the room's owner can revoke voice from a moderator
            clearAnswer(answer);
            muc.revokeVoice("testbot2");
            Thread.sleep(300);

            assertEquals(
                "User2 didn't receive the revoke voice notification",
                "cannot speak",
                answer[1]);
            assertEquals(
                "User3 didn't receive user2's revoke voice notification",
                room + "/testbot2",
                answer[3]);

            // User2 leaves the room
            muc2.leave();
            // User3 leaves the room
            muc3.leave();
        }
        catch (Exception e) {
            fail(e.getMessage());
            e.printStackTrace();
        }
    }

    public void testMembership() {
        final String[] answer = new String[4];
        try {

            makeRoomModerated();
            
            // User2 joins the new room (as a visitor)
            MultiUserChat muc2 = new MultiUserChat(conn2, room);
            muc2.join("testbot2",SmackConfiguration.getPacketReplyTimeout(),null,0,-1,-1,null);
            // User2 will listen for membership privileges            
            muc2.addUserStatusListener(new DefaultUserStatusListener() {
                public void membershipGranted() {
                    super.membershipGranted();
                    answer[0] = "I'm a member";
                }
                public void membershipRevoked() {
                    super.membershipRevoked();
                    answer[1] = "I'm not a member";
                }
            });

            // User3 joins the new room (as a visitor)
            MultiUserChat muc3 = new MultiUserChat(conn3, room);
            muc3.join("testbot3",SmackConfiguration.getPacketReplyTimeout(),null,0,-1,-1,null);
            // User3 will lister for user2's membership privileges            
            muc3.addParticipantStatusListener(new DefaultParticipantStatusListener() {
                public void membershipGranted(String participant) {
                    super.membershipGranted(participant);
                    answer[2] = participant;
                }
                public void membershipRevoked(String participant) {
                    super.membershipRevoked(participant);
                    answer[3] = participant;
                }
            });

            try {
                // Check whether a visitor can grant membership privileges to another visitor
                muc2.grantMembership("gato5@" + conn2.getHost());
                fail("User2 was able to grant membership privileges");
            }
            catch (XMPPException e) {
                XMPPError xmppError = e.getXMPPError();
                assertNotNull(
                    "No XMPPError was received granting membership privileges",
                    xmppError);
                assertEquals(
                    "A visitor was able to grant membership privileges to another visitor",
                    403,
                    xmppError.getCode());
            }
            
            // Check that the room's owner can grant membership privileges to a visitor
            muc.grantMembership("gato4@" + conn2.getHost());
            Thread.sleep(300);

            // Check that UserStatusListener is working OK
            assertEquals(
                "User2 didn't receive the grant membership notification",
                "I'm a member",
                answer[0]);
            // Check that ParticipantStatusListener is working OK
            assertEquals(
                "User3 didn't receive user2's grant membership notification",
                room + "/testbot2",
                answer[2]);

            // Check that the room's owner can revoke membership privileges from a member
            // and make the occupant a visitor
            muc.revokeMembership("gato4@" + conn2.getHost());
            Thread.sleep(300);

            // Check that UserStatusListener is working OK
            assertEquals(
                "User2 didn't receive the revoke membership notification",
                "I'm not a member",
                answer[1]);
            // Check that ParticipantStatusListener is working OK
            assertEquals(
                "User3 didn't receive user2's revoke membership notification",
                room + "/testbot2",
                answer[3]);
            
            // User2 leaves the room
            muc2.leave();
            // User3 leaves the room
            muc3.leave();
        }
        catch (Exception e) {
            fail(e.getMessage());
            e.printStackTrace();
        }
    }

    public void testAdmin() {
        final String[] answer = new String[8];
        try {

            makeRoomModerated();
            
            // User2 joins the new room (as a visitor)
            MultiUserChat muc2 = new MultiUserChat(conn2, room);
            muc2.join("testbot2",SmackConfiguration.getPacketReplyTimeout(),null,0,-1,-1,null);
            // User2 will listen for admin privileges            
            muc2.addUserStatusListener(new DefaultUserStatusListener() {
                public void membershipGranted() {
                    super.membershipGranted();
                    answer[0] = "I'm a member";
                }
                public void membershipRevoked() {
                    super.membershipRevoked();
                    answer[1] = "I'm not a member";
                }
                public void adminGranted() {
                    super.adminGranted();
                    answer[2] = "I'm an admin";
                }
                public void adminRevoked() {
                    super.adminRevoked();
                    answer[3] = "I'm not an admin";
                }
            });

            // User3 joins the new room (as a visitor)
            MultiUserChat muc3 = new MultiUserChat(conn3, room);
            muc3.join("testbot3",SmackConfiguration.getPacketReplyTimeout(),null,0,-1,-1,null);
            // User3 will lister for user2's admin privileges            
            muc3.addParticipantStatusListener(new DefaultParticipantStatusListener() {
                public void membershipGranted(String participant) {
                    super.membershipGranted(participant);
                    answer[4] = participant;
                }
                public void membershipRevoked(String participant) {
                    super.membershipRevoked(participant);
                    answer[5] = participant;
                }
                public void adminGranted(String participant) {
                    super.adminGranted(participant);
                    answer[6] = participant;
                }
                public void adminRevoked(String participant) {
                    super.adminRevoked(participant);
                    answer[7] = participant;
                }
            });

            try {
                // Check whether a visitor can grant admin privileges to another visitor
                muc2.grantAdmin("gato5@" + conn2.getHost());
                fail("User2 was able to grant admin privileges");
            }
            catch (XMPPException e) {
                XMPPError xmppError = e.getXMPPError();
                assertNotNull(
                    "No XMPPError was received granting admin privileges",
                    xmppError);
                assertEquals(
                    "A visitor was able to grant admin privileges to another visitor",
                    403,
                    xmppError.getCode());
            }
            
            // Check that the room's owner can grant admin privileges to a visitor
            muc.grantAdmin("gato4@" + conn2.getHost());
            Thread.sleep(300);

            // Check that UserStatusListener is working OK
            assertEquals(
                "User2 didn't receive the grant admin notification",
                "I'm an admin",
                answer[2]);
            // Check that ParticipantStatusListener is working OK
            assertEquals(
                "User3 didn't receive user2's grant admin notification",
                room + "/testbot2",
                answer[6]);

            // Check that the room's owner can revoke admin privileges from an admin
            // and make the occupant a visitor
            muc.revokeMembership("gato4@" + conn2.getHost());
            Thread.sleep(300);

            // Check that UserStatusListener is working OK
            assertEquals(
                "User2 didn't receive the revoke admin notification",
                "I'm not an admin",
                answer[3]);
            // Check that ParticipantStatusListener is working OK
            assertEquals(
                "User3 didn't receive user2's revoke admin notification",
                room + "/testbot2",
                answer[7]);
            
            // Check that the room's owner can grant admin privileges to a member
            clearAnswer(answer);
            muc.grantMembership("gato4@" + conn2.getHost());
            Thread.sleep(300);
            muc.grantAdmin("gato4@" + conn2.getHost());
            Thread.sleep(300);

            // Check that UserStatusListener is working OK
            assertEquals(
                "User2 didn't receive the revoke membership notification",
                "I'm not a member",
                answer[1]);
            assertEquals(
                "User2 didn't receive the grant admin notification",
                "I'm an admin",
                answer[2]);
            // Check that ParticipantStatusListener is working OK
            assertEquals(
                "User3 didn't receive user2's revoke membership notification",
                room + "/testbot2",
                answer[5]);
            assertEquals(
                "User3 didn't receive user2's grant admin notification",
                room + "/testbot2",
                answer[6]);

            // Check that the room's owner can revoke admin privileges from an admin
            // and make the occupant a member
            clearAnswer(answer);
            muc.revokeAdmin("gato4@" + conn2.getHost());
            Thread.sleep(300);

            // Check that UserStatusListener is working OK
            assertEquals(
                "User2 didn't receive the revoke admin notification",
                "I'm not an admin",
                answer[3]);
            assertEquals(
                "User2 didn't receive the grant membership notification",
                "I'm a member",
                answer[0]);
            // Check that ParticipantStatusListener is working OK
            assertEquals(
                "User3 didn't receive user2's revoke admin notification",
                room + "/testbot2",
                answer[7]);
            assertEquals(
                "User3 didn't receive user2's grant membership notification",
                room + "/testbot2",
                answer[4]);

            // User2 leaves the room
            muc2.leave();
            // User3 leaves the room
            muc3.leave();
        }
        catch (Exception e) {
            fail(e.getMessage());
            e.printStackTrace();
        }
    }

    public void testOwnership() {
        final String[] answer = new String[12];
        try {

            makeRoomModerated();
            
            // User2 joins the new room (as a visitor)
            MultiUserChat muc2 = new MultiUserChat(conn2, room);
            muc2.join("testbot2",SmackConfiguration.getPacketReplyTimeout(),null,0,-1,-1,null);
            // User2 will listen for ownership privileges            
            muc2.addUserStatusListener(new DefaultUserStatusListener() {
                public void membershipGranted() {
                    super.membershipGranted();
                    answer[0] = "I'm a member";
                }
                public void membershipRevoked() {
                    super.membershipRevoked();
                    answer[1] = "I'm not a member";
                }
                public void adminGranted() {
                    super.adminGranted();
                    answer[2] = "I'm an admin";
                }
                public void adminRevoked() {
                    super.adminRevoked();
                    answer[3] = "I'm not an admin";
                }
                public void ownershipGranted() {
                    super.ownershipGranted();
                    answer[4] = "I'm an owner";
                }
                public void ownershipRevoked() {
                    super.ownershipRevoked();
                    answer[5] = "I'm not an owner";
                }
            });

            // User3 joins the new room (as a visitor)
            MultiUserChat muc3 = new MultiUserChat(conn3, room);
            muc3.join("testbot3",SmackConfiguration.getPacketReplyTimeout(),null,0,-1,-1,null);
            // User3 will lister for user2's ownership privileges            
            muc3.addParticipantStatusListener(new DefaultParticipantStatusListener() {
                public void membershipGranted(String participant) {
                    super.membershipGranted(participant);
                    answer[6] = participant;
                }
                public void membershipRevoked(String participant) {
                    super.membershipRevoked(participant);
                    answer[7] = participant;
                }
                public void adminGranted(String participant) {
                    super.adminGranted(participant);
                    answer[8] = participant;
                }
                public void adminRevoked(String participant) {
                    super.adminRevoked(participant);
                    answer[9] = participant;
                }
                public void ownershipGranted(String participant) {
                    super.ownershipGranted(participant);
                    answer[10] = participant;
                }
                public void ownershipRevoked(String participant) {
                    super.ownershipRevoked(participant);
                    answer[11] = participant;
                }
            });

            try {
                // Check whether a visitor can grant ownership privileges to another visitor
                muc2.grantOwnership("gato5@" + conn2.getHost());
                fail("User2 was able to grant ownership privileges");
            }
            catch (XMPPException e) {
                XMPPError xmppError = e.getXMPPError();
                assertNotNull(
                    "No XMPPError was received granting ownership privileges",
                    xmppError);
                assertEquals(
                    "A visitor was able to grant ownership privileges to another visitor",
                    403,
                    xmppError.getCode());
            }
            
            // Check that the room's owner can grant ownership privileges to a visitor
            muc.grantOwnership("gato4@" + conn2.getHost());
            Thread.sleep(300);

            // Check that UserStatusListener is working OK
            assertEquals(
                "User2 didn't receive the grant ownership notification",
                "I'm an owner",
                answer[4]);
            // Check that ParticipantStatusListener is working OK
            assertEquals(
                "User3 didn't receive user2's grant ownership notification",
                room + "/testbot2",
                answer[10]);

            // Check that the room's owner can revoke ownership privileges from an owner
            // and make the occupant a visitor
            muc.revokeMembership("gato4@" + conn2.getHost());
            Thread.sleep(300);

            // Check that UserStatusListener is working OK
            assertEquals(
                "User2 didn't receive the revoke ownership notification",
                "I'm not an owner",
                answer[5]);
            // Check that ParticipantStatusListener is working OK
            assertEquals(
                "User3 didn't receive user2's revoke ownership notification",
                room + "/testbot2",
                answer[11]);
            
            // Check that the room's owner can grant ownership privileges to a member
            clearAnswer(answer);
            muc.grantMembership("gato4@" + conn2.getHost());
            Thread.sleep(300);
            muc.grantOwnership("gato4@" + conn2.getHost());
            Thread.sleep(300);

            // Check that UserStatusListener is working OK
            assertEquals(
                "User2 didn't receive the revoke membership notification",
                "I'm not a member",
                answer[1]);
            assertEquals(
                "User2 didn't receive the grant ownership notification",
                "I'm an owner",
                answer[4]);
            // Check that ParticipantStatusListener is working OK
            assertEquals(
                "User3 didn't receive user2's revoke membership notification",
                room + "/testbot2",
                answer[7]);
            assertEquals(
                "User3 didn't receive user2's grant ownership notification",
                room + "/testbot2",
                answer[10]);

            // Check that the room's owner can revoke ownership privileges from an owner
            // and make the occupant a member
            clearAnswer(answer);
            muc.revokeAdmin("gato4@" + conn2.getHost());
            Thread.sleep(300);

            // Check that UserStatusListener is working OK
            assertEquals(
                "User2 didn't receive the revoke ownership notification",
                "I'm not an owner",
                answer[5]);
            assertEquals(
                "User2 didn't receive the grant membership notification",
                "I'm a member",
                answer[0]);
            // Check that ParticipantStatusListener is working OK
            assertEquals(
                "User3 didn't receive user2's revoke ownership notification",
                room + "/testbot2",
                answer[11]);
            assertEquals(
                "User3 didn't receive user2's grant membership notification",
                room + "/testbot2",
                answer[6]);

            // Check that the room's owner can grant ownership privileges to an admin
            clearAnswer(answer);
            muc.grantAdmin("gato4@" + conn2.getHost());
            Thread.sleep(300);
            muc.grantOwnership("gato4@" + conn2.getHost());
            Thread.sleep(300);

            // Check that UserStatusListener is working OK
            assertEquals(
                "User2 didn't receive the revoke admin notification",
                "I'm not an admin",
                answer[3]);
            assertEquals(
                "User2 didn't receive the grant ownership notification",
                "I'm an owner",
                answer[4]);
            // Check that ParticipantStatusListener is working OK
            assertEquals(
                "User3 didn't receive user2's revoke admin notification",
                room + "/testbot2",
                answer[9]);
            assertEquals(
                "User3 didn't receive user2's grant ownership notification",
                room + "/testbot2",
                answer[10]);

            // Check that the room's owner can revoke ownership privileges from an owner
            // and make the occupant an admin
            clearAnswer(answer);
            muc.revokeOwnership("gato4@" + conn2.getHost());
            Thread.sleep(300);

            // Check that UserStatusListener is working OK
            assertEquals(
                "User2 didn't receive the revoke ownership notification",
                "I'm not an owner",
                answer[5]);
            assertEquals(
                "User2 didn't receive the grant admin notification",
                "I'm an admin",
                answer[2]);
            // Check that ParticipantStatusListener is working OK
            assertEquals(
                "User3 didn't receive user2's revoke ownership notification",
                room + "/testbot2",
                answer[11]);
            assertEquals(
                "User3 didn't receive user2's grant admin notification",
                room + "/testbot2",
                answer[8]);

            // User2 leaves the room
            muc2.leave();
            // User3 leaves the room
            muc3.leave();
        }
        catch (Exception e) {
            fail(e.getMessage());
            e.printStackTrace();
        }
    }

    private void makeRoomModerated() throws XMPPException {
        // User1 (which is the room owner) converts the instant room into a moderated room
        Form form = muc.getConfigurationForm();
        Form answerForm = form.createAnswerForm();
        answerForm.setAnswer("muc#owner_moderatedroom", "1");
        muc.sendConfigurationForm(answerForm);
    }
    
    private void clearAnswer(String[] answer) {
        for (int i=0; i < answer.length; i++) {
            answer[i] = null;
        }
    }

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

            user1 = "gato3@" + conn1.getHost() + "/Smack";
            user2 = "gato4@" + conn2.getHost() + "/Smack";
            user3 = "gato5@" + conn2.getHost() + "/Smack";

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
