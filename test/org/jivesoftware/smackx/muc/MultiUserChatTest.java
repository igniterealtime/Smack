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

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;

import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManagerListener;
import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smack.test.SmackTestCase;
import org.jivesoftware.smackx.Form;
import org.jivesoftware.smackx.FormField;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.packet.DelayInformation;
import org.jivesoftware.smackx.packet.DiscoverInfo;
import org.jivesoftware.smackx.packet.XHTMLExtension;

/**
 * Tests the new MUC functionalities.
 * 
 * @author Gaston Dombiak
 */
public class MultiUserChatTest extends SmackTestCase {

    private String room;

    private MultiUserChat muc;

    public MultiUserChatTest(String arg0) {
        super(arg0);
    }

    /**
     * Test the compatibility of the MUC service with clients that still use the old groupchat
     * protocol.
     */
    public void testGroupchatCompatibility() {
        // TODO: this test needs to be re-written so that it manually emulates the old-style
        // TODO: group chat protocol. Tne GroupChat class was deleted for Smack 3.0. 
        /*try {
            Message message;

            GroupChat groupchat = new GroupChat(getConnection(1), room);
            groupchat.join("testbot2");
            Thread.sleep(400);

            // User1 checks the presence of user2 in the room
            Presence presence = muc.getOccupantPresence(room + "/testbot2");
            assertNotNull("Presence of user2 in room is missing", presence);
            assertEquals(
                "Presence mode of user2 is wrong",
                Presence.Mode.available,
                presence.getMode());

            // User using old client send a message
            groupchat.sendMessage("Hello");
            // Check that the rest of the occupants (that are support MUC) received the message
            message = muc.nextMessage(1000);
            assertNotNull("A MUC client didn't receive the message from an old client", message);
            // User that supports MUC send a message
            muc.sendMessage("Bye");
            // Check that the client the doesn't support MUC received the message
            message = groupchat.nextMessage(1000);
            assertNotNull("An old client didn't receive the message from a MUC client", message);
            // User that doesn't support MUC leaves the room
            groupchat.leave();
            Thread.sleep(300);
            // User1 checks the that user2 is not present in the room
            Occupant occupant = muc.getOccupant(room + "/testbot2");
            assertNull("Occupant testbot2 still exists", occupant);
        }
        catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }*/
    }

    public void testDiscussionHistory() {
        try {
            // User1 sends some messages to the room
            muc.sendMessage("Message 1");
            muc.sendMessage("Message 2");
            // Wait 5 seconds before sending the last message
            Thread.sleep(5000);
            muc.sendMessage("Message 3");

            // User2 joins the room requesting to receive the messages of the last 2 seconds.
            MultiUserChat muc2 = new MultiUserChat(getConnection(1), room);
            DiscussionHistory history = new DiscussionHistory();
            history.setSeconds(2);
            muc2.join("testbot2", null, history, SmackConfiguration.getPacketReplyTimeout());

            Message msg;
            // Get first historic message
            msg = muc2.nextMessage(1000);
            assertNotNull("First message is null", msg);
            DelayInformation delay = (DelayInformation) msg.getExtension("x", "jabber:x:delay");
            assertNotNull("Message contains no delay information", delay);
            SimpleDateFormat UTC_FORMAT = new SimpleDateFormat("yyyyMMdd'T'HH:mm:ss");
            UTC_FORMAT.setTimeZone(TimeZone.getDefault());
            System.out.println(UTC_FORMAT.format(delay.getStamp()));

            assertEquals("Body of first message is incorrect", "Message 3", msg.getBody());
            // Try to get second historic message 
            msg = muc2.nextMessage(1000);
            assertNull("Second message is not null", msg);


            // User3 joins the room requesting to receive the last 2 messages.
            MultiUserChat muc3 = new MultiUserChat(getConnection(2), room);
            history = new DiscussionHistory();
            history.setMaxStanzas(2);
            muc3.join("testbot3", null, history, SmackConfiguration.getPacketReplyTimeout());

            // Get first historic message 
            msg = muc3.nextMessage(1000);
            assertNotNull("First message is null", msg);
            assertEquals("Body of first message is incorrect", "Message 2", msg.getBody());
            // Get second historic message 
            msg = muc3.nextMessage(1000);
            assertNotNull("Second message is null", msg);
            assertEquals("Body of second message is incorrect", "Message 3", msg.getBody());
            // Try to get third historic message 
            msg = muc3.nextMessage(1000);
            assertNull("Third message is not null", msg);

            // User2 leaves the room
            muc2.leave();
            // User3 leaves the room
            muc3.leave();

        }
        catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    public void testParticipantPresence() {
        try {
            // User2 joins the new room
            MultiUserChat muc2 = new MultiUserChat(getConnection(1), room);
            muc2.join("testbot2");
            Thread.sleep(400);

            // User1 checks the presence of user2 in the room
            Presence presence = muc.getOccupantPresence(room + "/testbot2");
            assertNotNull("Presence of user2 in room is missing", presence);
            assertTrue(
                "Presence mode of user2 is wrong",
                presence.getMode() == null || presence.getMode() == Presence.Mode.available);

            // User2 changes his availability to AWAY
            muc2.changeAvailabilityStatus("Gone to have lunch", Presence.Mode.away);
            Thread.sleep(200);
            // User1 checks the presence of user2 in the room
            presence = muc.getOccupantPresence(room + "/testbot2");
            assertNotNull("Presence of user2 in room is missing", presence);
            assertEquals("Presence mode of user2 is wrong", Presence.Mode.away, presence.getMode());
            assertEquals(
                "Presence status of user2 is wrong",
                "Gone to have lunch",
                presence.getStatus());

            // User2 changes his nickname
            muc2.changeNickname("testbotII");
            Thread.sleep(200);
            // User1 checks the presence of user2 in the room
            presence = muc.getOccupantPresence(room + "/testbot2");
            assertNull("Presence of participant testbot2 still exists", presence);
            presence = muc.getOccupantPresence(room + "/testbotII");
            assertNotNull("Presence of participant testbotII does not exist", presence);
            assertEquals(
                "Presence of participant testbotII has a wrong from",
                room + "/testbotII",
                presence.getFrom());

            // User2 leaves the room
            muc2.leave();
            Thread.sleep(250);
            // User1 checks the presence of user2 in the room
            presence = muc.getOccupantPresence(room + "/testbotII");
            assertNull("Presence of participant testbotII still exists", presence);

        }
        catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    public void testAnonymousParticipant() {
        try {
            // Anonymous user joins the new room
            ConnectionConfiguration connectionConfiguration =
                    new ConnectionConfiguration(getHost(), getPort(), getServiceName());
            XMPPConnection anonConnection = new XMPPConnection(connectionConfiguration);
            anonConnection.connect();
            anonConnection.loginAnonymously();
            MultiUserChat muc2 = new MultiUserChat(anonConnection, room);
            muc2.join("testbot2");
            Thread.sleep(400);

            // User1 checks the presence of Anonymous user in the room
            Presence presence = muc.getOccupantPresence(room + "/testbot2");
            assertNotNull("Presence of user2 in room is missing", presence);
            assertTrue(
                "Presence mode of user2 is wrong",
                presence.getMode() == null || presence.getMode() == Presence.Mode.available);

            // Anonymous user leaves the room
            muc2.leave();
            anonConnection.disconnect();
            Thread.sleep(250);
            // User1 checks the presence of Anonymous user in the room
            presence = muc.getOccupantPresence(room + "/testbot2");
            assertNull("Presence of participant testbotII still exists", presence);

        }
        catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    public void testInvitation() {
        final String[] answer = new String[2];
        try {
            // User2 joins the new room
            MultiUserChat muc2 = new MultiUserChat(getConnection(1), room);
            muc2.join("testbot2");

            // User3 is listening to MUC invitations
            MultiUserChat.addInvitationListener(getConnection(2), new InvitationListener() {
                public void invitationReceived(
                    Connection conn,
                    String room,
                    String inviter,
                    String reason,
                    String password,
                    Message message) {
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
            muc2.invite(getFullJID(2), "Meet me in this excellent room");
            Thread.sleep(350);

            assertEquals(
                "Invitation was not received",
                "Meet me in this excellent room",
                answer[0]);
            assertEquals("Rejection was not received", "I'm busy right now", answer[1]);

            // User2 leaves the room
            muc2.leave();
        }
        catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    public void testInvitationWithMessage() {
        final String[] answer = new String[2];
        try {
            // User2 joins the new room
            MultiUserChat muc2 = new MultiUserChat(getConnection(1), room);
            muc2.join("testbot2");

            // User3 is listening to MUC invitations
            MultiUserChat.addInvitationListener(getConnection(2), new InvitationListener() {
                public void invitationReceived(
                    Connection conn,
                    String room,
                    String inviter,
                    String reason,
                    String password,
                    Message message) {
                    // Indicate that the invitation was received
                    answer[0] = reason;
                    XHTMLExtension extension = (XHTMLExtension) message.getExtension("html",
                            "http://jabber.org/protocol/xhtml-im");
                    assertNotNull("An extension was not found in the invitation", extension);
                    answer[1] = (String) extension.getBodies().next();
                }
            });

            // User2 invites user3 to join to the room
            Message msg = new Message();
            XHTMLExtension xhtmlExtension = new XHTMLExtension();
            xhtmlExtension.addBody("<body>Meet me in this excellent room</body>");
            msg.addExtension(xhtmlExtension);
            muc2.invite(msg , getFullJID(2), "Meet me in this excellent room");
            Thread.sleep(350);

            assertEquals(
                "Invitation was not received",
                "Meet me in this excellent room",
                answer[0]);
            assertEquals("Rejection was not received",
                    "<body>Meet me in this excellent room</body>", answer[1]);

            // User2 leaves the room
            muc2.leave();
        }
        catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    public void testDiscoverJoinedRooms() {
        try {
            // Check that user1 has joined only to one room
            Iterator<String> joinedRooms = MultiUserChat.getJoinedRooms(getConnection(1), getFullJID(0));
            assertTrue("Joined rooms shouldn't be empty", joinedRooms.hasNext());
            assertEquals("Joined room is incorrect", joinedRooms.next(), room);
            assertFalse("User has joined more than one room", joinedRooms.hasNext());

            // Leave the new room
            muc.leave();

            // Check that user1 is not currently join any room
            joinedRooms = MultiUserChat.getJoinedRooms(getConnection(1), getFullJID(0));
            assertFalse("Joined rooms should be empty", joinedRooms.hasNext());

            muc.join("testbot");
        }
        catch (XMPPException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    public void testDiscoverMUCSupport() {
        // Discover user1 support of MUC
        boolean supports = MultiUserChat.isServiceEnabled(getConnection(1), getFullJID(0));
        assertTrue("Couldn't detect that user1 supports MUC", supports);
    }

    public void testDiscoverRoomInfo() {
        try {
            makeRoomModerated();

            RoomInfo info = MultiUserChat.getRoomInfo(getConnection(1), room);

            assertFalse("Room is members-only", info.isMembersOnly());
            assertTrue("Room is moderated", info.isModerated());
            assertFalse("Room is Nonanonymous", info.isNonanonymous());
            assertFalse("Room is PasswordProtected", info.isPasswordProtected());
            assertFalse("Room is Persistent", info.isPersistent());
            assertEquals("Room's description is incorrect", "fruta124", info.getDescription());
            assertEquals("Room's subject is incorrect", "", info.getSubject());
            assertEquals("Number of occupants is incorrect", 1, info.getOccupantsCount());
        }
        catch (XMPPException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    public void testDiscoverMUCService() {
        try {
            Collection<String> services = MultiUserChat.getServiceNames(getConnection(1));
            assertFalse("No MUC service was found", services.isEmpty());

            // Discover the hosted rooms by the chat service.
            Collection<HostedRoom> rooms = MultiUserChat.getHostedRooms(getConnection(1),
                   services.toArray(new String[0])[0]);
            // Check that we have discovered the room used by this test
            assertFalse("No room was found", rooms.isEmpty());
            // Check that we have discovered the room used by this test
            boolean found = false;
            for (Object room1 : rooms) {
                HostedRoom hostedRoom = (HostedRoom) room1;
                if (room.equals(hostedRoom.getJid())) {
                    found = true;
                    break;
                }
            }
            assertTrue("JID of room was not found", found);
        }
        catch (XMPPException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    public void testPrivateChat() {
        try {
            // User2 joins the new room
            MultiUserChat muc2 = new MultiUserChat(getConnection(1), room);
            muc2.join("testbot2");

            getConnection(0).getChatManager().addChatListener(new ChatManagerListener() {
                public void chatCreated(Chat chat2, boolean createdLocally) {
                    assertEquals(
                        "Sender of chat is incorrect",
                        room + "/testbot2",
                        chat2.getParticipant());
                    try {
                        chat2.sendMessage("ACK");
                    }
                    catch (XMPPException e) {
                        fail(e.getMessage());
                    }
                }
            });

            // Start a private chat with another participant            
            Chat chat = muc2.createPrivateChat(room + "/testbot", null);
            PacketCollector collector = chat.createCollector();
            chat.sendMessage("Hello there");

            Message response = (Message) collector.nextResult(2000);
            assertNotNull("No response", response);
            assertEquals("Sender of response is incorrect", room + "/testbot", response.getFrom());
            assertEquals("Body of response is incorrect", "ACK", response.getBody());

            // User2 leaves the room
            muc2.leave();
        }
        catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    /**
     * Tests that IQ packets can be sent to/from room occupants. This case will try to discover
     * information about other room occupants.
     */
    public void testPrivateIQ() {
        try {
            // User2 joins the new room
            MultiUserChat muc2 = new MultiUserChat(getConnection(1), room);
            muc2.join("testbot2");

            // User2 discovers information about User1
            DiscoverInfo info = ServiceDiscoveryManager.getInstanceFor(getConnection(1))
                    .discoverInfo(room + "/testbot", null);

            assertNotNull("No info was discovered from room occupant", info);
            assertEquals("Wrong IQ type", IQ.Type.RESULT, info.getType());
            assertEquals("Wrong IQ sender", room + "/testbot", info.getFrom());

            // User2 leaves the room
            muc2.leave();
        }
        catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    public void testReservedNickname() {
        try {
            MultiUserChat muc2 = new MultiUserChat(getConnection(1), room);
            // Check that user2 doesn't have a reserved nickname yet
            String reservedNickname = muc2.getReservedNickname();
            assertNull("Reserved nickname is not null", reservedNickname);

            // User2 registers with the room and reserves a nickname
            Form registrationForm = muc2.getRegistrationForm();
            Form answerForm = registrationForm.createAnswerForm();
            answerForm.setAnswer("muc#register_first", "MyFirstName");
            answerForm.setAnswer("muc#register_last", "MyLastName");
            answerForm.setAnswer("muc#register_roomnick", "MyNick");
            muc2.sendRegistrationForm(answerForm);

            // Check that user2 has a reserved nickname
            reservedNickname = muc2.getReservedNickname();
            assertEquals("Reserved nickname is wrong", "MyNick", reservedNickname);

            // Check that user2 can join the room using his reserved nickname
            muc2.join("MyNick");
            muc2.leave();

            // Check that other users cannot join the room with user2's reserved nickname
            MultiUserChat muc3 = new MultiUserChat(getConnection(2), room);
            try {
                muc3.join("MyNick");
                fail("Other user was able to join with other user's reserved nickname");
            }
            catch (XMPPException e) {
                XMPPError xmppError = e.getXMPPError();
                assertNotNull(
                    "No XMPPError was received when joining with other user's reserved nickname",
                    xmppError);
                assertEquals(
                    "Different error code was received while joining with other user's reserved nickname",
                    409,
                    xmppError.getCode());
            }

            // Check that user3 can join the room using his own nickname (not reserved)
            muc3.join("MyNotReservedNick");
            muc3.leave();

            // Check that another user cannot reserve an already reserved nickname
            registrationForm = muc3.getRegistrationForm();
            answerForm = registrationForm.createAnswerForm();
            answerForm.setAnswer("muc#register_first", "MyFirstName 2");
            answerForm.setAnswer("muc#register_last", "MyLastName 2");
            answerForm.setAnswer("muc#register_roomnick", "MyNick");
            try {
                muc3.sendRegistrationForm(answerForm);
            }
            catch (XMPPException e) {
                XMPPError xmppError = e.getXMPPError();
                assertNotNull(
                    "No XMPPError was received when reserving an already reserved nickname",
                    xmppError);
                assertEquals(
                    "Different error code was received while reserving an already reserved nickname",
                    409,
                    xmppError.getCode());
            }

            // Check that another user can reserve a new nickname
            registrationForm = muc3.getRegistrationForm();
            answerForm = registrationForm.createAnswerForm();
            answerForm.setAnswer("muc#register_first", "MyFirstName 2");
            answerForm.setAnswer("muc#register_last", "MyLastName 2");
            answerForm.setAnswer("muc#register_roomnick", "MyNick 2");
            muc3.sendRegistrationForm(answerForm);

        }
        catch (XMPPException e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    public void testChangeSubject() {
        final String[] answer = new String[2];
        try {
            // User1 sets an initial subject
            muc.changeSubject("Initial Subject");

            // User2 joins the new room
            MultiUserChat muc2 = new MultiUserChat(getConnection(1), room);
            muc2.join("testbot2");

            // User3 joins the new room
            MultiUserChat muc3 = new MultiUserChat(getConnection(2), room);
            muc3.join("testbot3");

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
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    public void testKickParticipant() {
        final String[] answer = new String[3];
        try {
            // User2 joins the new room
            MultiUserChat muc2 = new MultiUserChat(getConnection(1), room);
            muc2.join("testbot2");
            // User2 will lister for his own "kicking"            
            muc2.addUserStatusListener(new DefaultUserStatusListener() {
                public void kicked(String actor, String reason) {
                    super.kicked(actor, reason);
                    answer[0] = actor;
                    answer[1] = reason;
                }
            });

            // User3 joins the new room
            MultiUserChat muc3 = new MultiUserChat(getConnection(2), room);
            muc3.join("testbot3");
            // User3 will lister for user2's "kicking"            
            muc3.addParticipantStatusListener(new DefaultParticipantStatusListener() {
                public void kicked(String participant, String actor, String reason) {
                    super.kicked(participant, actor, reason);
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
                assertNotNull("No XMPPError was received when kicking a room owner", xmppError);
                assertEquals(
                    "A simple participant was able to kick another participant from the room",
                    403,
                    xmppError.getCode());
            }

            // Check that the room's owner can kick a simple participant
            muc.kickParticipant("testbot2", "Because I'm the owner");
            Thread.sleep(300);

            assertNull(
                "User2 wasn't kicked from the room",
                muc.getOccupant(room + "/testbot2"));

            assertFalse("User2 thinks that he's still in the room", muc2.isJoined());

            // Check that UserStatusListener is working OK
            assertEquals(
                "User2 didn't receive the correct initiator of the kick",
                getBareJID(0),
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
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    public void testBanUser() {
        final String[] answer = new String[3];
        try {
            // User2 joins the new room
            MultiUserChat muc2 = new MultiUserChat(getConnection(1), room);
            muc2.join("testbot2");
            // User2 will lister for his own "banning"            
            muc2.addUserStatusListener(new DefaultUserStatusListener() {
                public void banned(String actor, String reason) {
                    super.banned(actor, reason);
                    answer[0] = actor;
                    answer[1] = reason;
                }
            });

            // User3 joins the new room
            MultiUserChat muc3 = new MultiUserChat(getConnection(2), room);
            muc3.join("testbot3");
            // User3 will lister for user2's "banning"            
            muc3.addParticipantStatusListener(new DefaultParticipantStatusListener() {
                public void banned(String participant, String actor, String reason) {
                    super.banned(participant, actor, reason);
                    answer[2] = participant;
                }
            });

            try {
                // Check whether a simple participant can ban a room owner or not
                muc2.banUser(getBareJID(0), "Because I'm bad");
                fail("User2 was able to ban a room owner");
            }
            catch (XMPPException e) {
                XMPPError xmppError = e.getXMPPError();
                assertNotNull("No XMPPError was received when banning a room owner", xmppError);
                assertEquals(
                    "A simple participant was able to ban another participant from the room",
                    403,
                    xmppError.getCode());
            }

            // Check that the room's owner can ban a simple participant
            muc.banUser(getBareJID(1), "Because I'm the owner");
            Thread.sleep(300);

            assertNull(
                "User2 wasn't banned from the room",
                muc.getOccupant(room + "/testbot2"));

            assertFalse("User2 thinks that he's still in the room", muc2.isJoined());

            // Check that UserStatusListener is working OK
            assertEquals(
                "User2 didn't receive the correct initiator of the ban",
                getBareJID(0),
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
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    public void testVoice() {
        final String[] answer = new String[4];
        try {

            makeRoomModerated();

            // User2 joins the new room (as a visitor)
            MultiUserChat muc2 = new MultiUserChat(getConnection(1), room);
            muc2.join("testbot2");
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
            MultiUserChat muc3 = new MultiUserChat(getConnection(2), room);
            muc3.join("testbot3");
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
                assertNotNull("No XMPPError was received granting voice", xmppError);
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
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    public void testModerator() {
        final String[] answer = new String[8];
        try {

            makeRoomModerated();

            // User2 joins the new room (as a visitor)
            MultiUserChat muc2 = new MultiUserChat(getConnection(1), room);
            muc2.join("testbot2");
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
            MultiUserChat muc3 = new MultiUserChat(getConnection(2), room);
            muc3.join("testbot3");
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
                assertNotNull("No XMPPError was received granting moderator privileges", xmppError);
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

            assertNull("User2 received a false revoke voice notification", answer[1]);
            assertNull("User3 received a false user2's voice privileges notification", answer[3]);
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
            assertNull("User2 received a false grant voice notification", answer[0]);
            assertEquals(
                "User2 didn't receive the grant moderator privileges notification",
                "I'm a moderator",
                answer[4]);
            // Check that ParticipantStatusListener is working OK
            assertNull("User3 received a false user2's grant voice notification", answer[2]);
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
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    public void testMembership() {
        final String[] answer = new String[4];
        try {

            makeRoomModerated();

            // User2 joins the new room (as a visitor)
            MultiUserChat muc2 = new MultiUserChat(getConnection(1), room);
            muc2.join("testbot2");
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
            MultiUserChat muc3 = new MultiUserChat(getConnection(2), room);
            muc3.join("testbot3");
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
                muc2.grantMembership(getBareJID(2));
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
            muc.grantMembership(getBareJID(1));
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
            muc.revokeMembership(getBareJID(1));
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
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    public void testAdmin() {
        final String[] answer = new String[8];
        try {

            makeRoomModerated();

            // User2 joins the new room (as a visitor)
            MultiUserChat muc2 = new MultiUserChat(getConnection(1), room);
            muc2.join("testbot2");
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
            MultiUserChat muc3 = new MultiUserChat(getConnection(2), room);
            muc3.join("testbot3");
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
                muc2.grantAdmin(getBareJID(2));
                fail("User2 was able to grant admin privileges");
            }
            catch (XMPPException e) {
                XMPPError xmppError = e.getXMPPError();
                assertNotNull("No XMPPError was received granting admin privileges", xmppError);
                assertEquals(
                    "A visitor was able to grant admin privileges to another visitor",
                    403,
                    xmppError.getCode());
            }

            // Check that the room's owner can grant admin privileges to a visitor
            muc.grantAdmin(getBareJID(1));
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
            muc.revokeMembership(getBareJID(1));
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
            muc.grantMembership(getBareJID(1));
            Thread.sleep(300);
            muc.grantAdmin(getBareJID(1));
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
            muc.revokeAdmin(getBareJID(1));
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
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    public void testOwnership() {
        final String[] answer = new String[12];
        try {

            makeRoomModerated();

            // User2 joins the new room (as a visitor)
            MultiUserChat muc2 = new MultiUserChat(getConnection(1), room);
            muc2.join("testbot2");
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
            MultiUserChat muc3 = new MultiUserChat(getConnection(2), room);
            muc3.join("testbot3");
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
                muc2.grantOwnership(getBareJID(2));
                fail("User2 was able to grant ownership privileges");
            }
            catch (XMPPException e) {
                XMPPError xmppError = e.getXMPPError();
                assertNotNull("No XMPPError was received granting ownership privileges", xmppError);
                assertEquals(
                    "A visitor was able to grant ownership privileges to another visitor",
                    403,
                    xmppError.getCode());
            }

            // Check that the room's owner can grant ownership privileges to a visitor
            muc.grantOwnership(getBareJID(1));
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
            muc.revokeMembership(getBareJID(1));
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
            muc.grantMembership(getBareJID(1));
            Thread.sleep(300);
            muc.grantOwnership(getBareJID(1));
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
            muc.revokeAdmin(getBareJID(1));
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
            muc.grantAdmin(getBareJID(1));
            Thread.sleep(300);
            muc.grantOwnership(getBareJID(1));
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
            muc.revokeOwnership(getBareJID(1));
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
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    public void testGetAffiliationList() {
        try {
            // User2 joins the new room
            MultiUserChat muc2 = new MultiUserChat(getConnection(1), room);
            muc2.join("testbot2");

            // User3 joins the new room
            MultiUserChat muc3 = new MultiUserChat(getConnection(2), room);
            muc3.join("testbot3");

            // Grant ownership privileges to user2
            muc.grantOwnership(getBareJID(1));
            // Grant moderator privileges to user3
            muc.grantModerator("testbot3");

            // Check that the owner list is correct
            Collection<Affiliate> affiliates = muc.getOwners();
            assertEquals("Room does not have 2 owners", 2, affiliates.size());
            for (Affiliate affiliate1 : affiliates) {
                if (getBareJID(0).equals(affiliate1.getJid())) {
                    assertEquals("Wrong affiliation", "owner", affiliate1.getAffiliation());
                    assertEquals("Wrong role", "moderator", affiliate1.getRole());
                    assertEquals("Wrong nick", "testbot", affiliate1.getNick());
                }
                else if (getBareJID(1).equals(affiliate1.getJid())) {
                    assertEquals("Wrong affiliation", "owner", affiliate1.getAffiliation());
                    assertEquals("Wrong role", "moderator", affiliate1.getRole());
                    assertEquals("Wrong nick", "testbot2", affiliate1.getNick());
                }
                else {
                    fail("Unknown owner " + affiliate1.getJid());
                }
            }

            // Check that the admin list is correct
            affiliates = muc.getAdmins();
            assertEquals("Room has admins", 0, affiliates.size());

            // Check that the members list is correct
            affiliates = muc.getMembers();
            assertEquals("Room has admins", 0, affiliates.size());
            // Grant membership privileges to user2
            muc.grantMembership(getBareJID(1));
            // Check that the members list is correct
            affiliates = muc.getMembers();
            assertEquals("Room has admins", 1, affiliates.size());
            Affiliate affiliate = (Affiliate) affiliates.iterator().next();
            assertEquals("Wrong member jid", getBareJID(1), affiliate.getJid());

            // Check that the members list is correct
            affiliates = muc.getOutcasts();
            assertEquals("Room has outcasts", 0, affiliates.size());

            // Check that the moderator list is correct
            Collection<Occupant> occupants = muc.getModerators();
            assertEquals("Room does not have 2 moderators", 2, occupants.size());
            for (Occupant occupant1 : occupants) {
                if (getFullJID(0).equals(occupant1.getJid())) {
                    assertEquals("Wrong affiliation", "owner", occupant1.getAffiliation());
                    assertEquals("Wrong role", "moderator", occupant1.getRole());
                    assertEquals("Wrong nick", "testbot", occupant1.getNick());
                }
                else if (getFullJID(2).equals(occupant1.getJid())) {
                    assertEquals("Wrong affiliation", "none", occupant1.getAffiliation());
                    assertEquals("Wrong role", "moderator", occupant1.getRole());
                    assertEquals("Wrong nick", "testbot3", occupant1.getNick());
                }
                else {
                    fail("Unknown moderator " + occupant1.getJid());
                }
            }

            // Check that the participants list is correct
            occupants = muc.getParticipants();
            assertEquals("Room does not have 1 participant", 1, occupants.size());
            Occupant occupant = (Occupant) occupants.iterator().next();
            assertEquals("Wrong participant jid", getFullJID(1), occupant.getJid());

            Thread.sleep(500);

            // Check that we can retrieve Occupant information of a given user
            occupant = muc.getOccupant(room + "/testbot2");
            assertNotNull("Occupant was not found", occupant);
            assertEquals("Wrong occupant jid", getFullJID(1), occupant.getJid());
            assertEquals("Wrong occupant affiliation", "member", occupant.getAffiliation());
            assertEquals("Wrong occupant role", "participant", occupant.getRole());
            assertEquals("Wrong occupant nick", "testbot2", occupant.getNick());

            try {
                // Check whether a member can get the list of owners
                muc2.getOwners();
                fail("User2 was able to get the list of owners");
            }
            catch (XMPPException e) {
                XMPPError xmppError = e.getXMPPError();
                assertNotNull("No XMPPError was received getting the list of owners", xmppError);
                assertEquals(
                    "A member was able to get the list of owners",
                    403,
                    xmppError.getCode());
            }
        }
        catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    /**
     * Check that ParticipantStatusListener is receiving joining and leaving events correctly. 
     */
    public void testJoinLeftEvents() {
        final String[] answer = new String[8];
        try {
            // User1 will listen for occupants joining and leaving the room
            muc.addParticipantStatusListener(new DefaultParticipantStatusListener() {
                public void joined(String participant) {
                    super.joined(participant);
                    if ((room + "/testbot2").equals(participant)) {
                        answer[0] = participant;
                    }
                    else {
                        answer[1] = participant;
                    }
                }
                public void left(String participant) {
                    super.left(participant);
                    if ((room + "/testbot2").equals(participant)) {
                        answer[2] = participant;
                    }
                    // Skip unavailable presences of the same user
                    else if (!(room + "/testbot").equals(participant)) {
                        answer[3] = participant;
                    }
                }
            });

            // User2 joins the new room
            MultiUserChat muc2 = new MultiUserChat(getConnection(1), room);
            // User2 will listen for User3 joining and leaving the room
            muc2.addParticipantStatusListener(new DefaultParticipantStatusListener() {
                public void joined(String participant) {
                    super.joined(participant);
                    if ((room + "/testbot").equals(participant)) {
                        answer[4] = participant;
                    }
                    else {
                        answer[5] = participant;
                    }
                }
                public void left(String participant) {
                    super.left(participant);
                    if ((room + "/testbot").equals(participant)) {
                        answer[6] = participant;
                    }
                    // Skip unavailable presences of the same user
                    else if (!(room + "/testbot2").equals(participant)){
                        answer[7] = participant;
                    }
                }
            });
            muc2.join("testbot2");

            // User3 joins the new room
            MultiUserChat muc3 = new MultiUserChat(getConnection(2), room);
            muc3.join("testbot3");

            Thread.sleep(150);

            // User3 leaves the room
            muc3.leave();

            Thread.sleep(150);
            // User2 leaves the room
            muc2.leave();

            Thread.sleep(250);
            // Check that ParticipantStatusListener is working OK
            assertEquals(
                "User1 didn't receive the event of User2 joining the room",
                room + "/testbot2",
                answer[0]);
            assertEquals(
                "User1 didn't receive the event of User3 joining the room",
                room + "/testbot3",
                answer[1]);
            assertEquals(
                "User1 didn't receive the event of User2 leaving the room",
                room + "/testbot2",
                answer[2]);
            assertEquals(
                "User1 didn't receive the event of User3 leaving the room",
                room + "/testbot3",
                answer[3]);
            assertEquals(
                "User2 didn't receive the event of User1 joining the room",
                room + "/testbot",
                answer[4]);
            assertEquals(
                "User2 didn't receive the event of User3 joining the room",
                room + "/testbot3",
                answer[5]);
            assertNull(
                "User2 received the event of User1 leaving the room",
                answer[6]);
            assertEquals(
                "User2 didn't receive the event of User3 leaving the room",
                room + "/testbot3",
                answer[7]);
        }
        catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    public void testManyResources() throws Exception {
            // Create 5 more connections for user2
            XMPPConnection[] conns = new XMPPConnection[5];
            for (int i = 0; i < conns.length; i++) {
                ConnectionConfiguration connectionConfiguration =
                        new ConnectionConfiguration(getHost(), getPort(), getServiceName());
                connectionConfiguration.setSecurityMode(ConnectionConfiguration.SecurityMode.disabled);
                conns[i] = new XMPPConnection(connectionConfiguration);
                conns[i].connect();
                conns[i].login(getUsername(1), getPassword(1), "resource-" + i);
                Thread.sleep(20);
            }

            // Join the 5 connections to the same room
            MultiUserChat[] mucs = new MultiUserChat[5];
            for (int i = 0; i < mucs.length; i++) {
                mucs[i] = new MultiUserChat(conns[i], room);
                mucs[i].join("resource-" + i);
            }

            Thread.sleep(200);

            // Each connection has something to say
            for (int i = 0; i < mucs.length; i++) {
                mucs[i].sendMessage("I'm resource-" + i);
            }

            Thread.sleep(200);

            // Each connection leaves the room and closes the connection
            for (MultiUserChat muc1 : mucs) {
                muc1.leave();
            }

            Thread.sleep(200);

            for (int i = 0; i < mucs.length; i++) {
                conns[i].disconnect();
            }
    }

    /**
     * Test that deaf occupants do not get broadcasted messages. Deaf occupants is a Jive
     * extension to MUC so it may not work with other servers.
     */
    public void testDeafOccupants() {
        try {
            // User2 joins the room as a "normal" occupant
            MultiUserChat muc2 = new MultiUserChat(getConnection(1), room);
            muc2.join("testbot2");

            // User3 joins the room as a "deaf" occupant
            MultiUserChat muc3 = new MultiUserChat(getConnection(2), room);
            muc3.addPresenceInterceptor(new DeafOccupantInterceptor());
            muc3.join("testbot3");

            // User1 sends some messages to the room
            muc.sendMessage("Message 1");
            muc.sendMessage("Message 2");

            Thread.sleep(500);

            Message msg;
            // Normal occupant gets first message
            msg = muc2.nextMessage(1000);
            assertNotNull("First message is null", msg);
            // Get second message
            msg = muc2.nextMessage(1000);
            assertNotNull("Second message is null", msg);
            // Try to get a third message
            msg = muc2.nextMessage(1000);
            assertNull("Third message is not null", msg);

            // Deaf occupant tries to get a third message
            msg = muc3.nextMessage(1000);
            assertNull("Deaf occupant got a broadcast message", msg);

            // User2 leaves the room
            muc2.leave();
            // User3 leaves the room
            muc3.leave();
        }
        catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    private void makeRoomModerated() throws XMPPException {
        // User1 (which is the room owner) converts the instant room into a moderated room
        Form form = muc.getConfigurationForm();
        Form answerForm = form.createAnswerForm();
        answerForm.setAnswer("muc#roomconfig_moderatedroom", true);
        answerForm.setAnswer("muc#roomconfig_whois", Arrays.asList("moderators"));
        // Keep the room owner
        try {
            List<String> owners = new ArrayList<String>();
            owners.add(getBareJID(0));
            answerForm.setAnswer("muc#roomconfig_roomowners", owners);
        }
        catch (IllegalArgumentException e) {
            // Do nothing
        }
        muc.sendConfigurationForm(answerForm);
    }

    private void clearAnswer(String[] answer) {
        for (int i = 0; i < answer.length; i++) {
            answer[i] = null;
        }
    }

    protected void setUp() throws Exception {
        //XMPPConnection.DEBUG_ENABLED = false;
        super.setUp();
        room = "fruta124@" + getMUCDomain();
        try {
            // User1 creates the room
            muc = new MultiUserChat(getConnection(0), room);
            muc.create("testbot");

            // User1 sends an empty room configuration form which indicates that we want
            // an instant room
            Form form = new Form(Form.TYPE_SUBMIT);
            FormField field = new FormField("muc#roomconfig_whois");
            field.setType("list-single");
            form.addField(field);
            form.setAnswer("muc#roomconfig_whois", Arrays.asList("moderators"));
            muc.sendConfigurationForm(form);
        }
        catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
    }

    protected void tearDown() throws Exception {
        // Destroy the new room
        muc.destroy("The room has almost no activity...", null);

        super.tearDown();
    }

    protected int getMaxConnections() {
        return 3;
    }
}
