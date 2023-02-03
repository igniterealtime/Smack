/**
 *
 * Copyright 2015-2023 Florian Schmaus
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

/**
 * Smack API for Multi-User Chat (MUC, XEP-0045). This API allows configuration of, participation in, and administration
 * of individual text-based conference rooms. The two key API classes are {@link MultiUserChatManager} and
 * {@link MultiUserChat}.
 * <h2>Create a new Room</h2>
 * <h3>Description</h3>
 * <p>
 * Allowed users may create new rooms. There are two types of rooms that you can create. **Instant rooms** which are
 * available for immediate access and are automatically created based on some default configuration and **Reserved
 * rooms** which are manually configured by the room creator before anyone is allowed to enter.
 * </p>
 * <h3>Usage</h3>
 * <p>
 * In order to create a room you will need to first create an instance of _* *MultiUserChat**_. In order to do so, get a
 * instance of `MultiUserChatManager` and call `getMultiUserChat(String)` to retrieve a `MultiUserChat` instance. The
 * next step is to send **create(String nickname)** to the _**MultiUserChat**_ instance where nickname is the nickname
 * to use when joining the room. Depending on the type of room that you want to create you will have to use different
 * configuration forms. In order to create an Instant room just use `MucCreateConfigFormHandle.makeInstant()`. But if
 * you want to create a Reserved room then you should first get the room's configuration form, complete the form and
 * finally send it back to the server.
 * </p>
 * <h3>Examples</h3>
 * <p>
 * In this example we can see how to create an instant room:
 * </p>
 *
 * <pre>{@code
 * // Get the MultiUserChatManager
 * MultiUserChatManager manager = MultiUserChatManager.getInstanceFor(connection);
 * // Get a MultiUserChat using MultiUserChatManager
 * MultiUserChat muc = manager.getMultiUserChat(mucJid);
 * // Create the room and send an empty configuration form to make this an instant room
 * muc.create(nickname).makeInstant();
 * }</pre>
 * <p>
 * In this example we can see how to create a reserved room. The form is completed with default values:
 * </p>
 *
 * <pre>{@code
 * // Get the MultiUserChatManager
 * MultiUserChatManager manager = MultiUserChatManager.getInstanceFor(connection);
 * // Create a MultiUserChat using an XMPPConnection for a room
 * MultiUserChat muc = manager.getMultiUserChat(mucJid);
 * // Prepare a list of owners of the new room
 * Set<Jid> owners = JidUtil.jidSetFrom(new String[] { "me@example.org", "juliet@example.org" });
 * // Create the room
 * muc.create(nickname).getConfigFormManger().setRoomOwners(owners).submitConfigurationForm();
 * }</pre>
 *
 * <h2>Join a room</h2>
 * <h3>Description</h3>
 * <p>
 * Your usual first step in order to send messages to a room is to join the room. Multi User Chat allows to specify
 * several parameter while joining a room. Basically you can control the amount of history to receive after joining the
 * room as well as provide your nickname within the room and a password if the room is password protected.
 * </p>
 * <h3>Usage</h3>
 * <p>
 * In order to join a room you will need to first get an instance of _**MultiUserChat**_. In order to do so, get a
 * instance of `MultiUserChatManager` and call `getMultiUserChat(String)` to retrieve a `MultiUserChat` instance. The
 * next step is to send **join(...)** to the _**MultiUserChat**_ instance. But first you will have to decide which join
 * message to send. If you want to just join the room without a password and without specifying the amount of history to
 * receive then you could use join(String nickname)** where nickname if your nickname in the room. In case the room
 * requires a password in order to join you could then use **join(String nickname, String password)**. And finally, the
 * most complete way to join a room is to send **join(String nickname, String password, DiscussionHistory history, long
 * timeout)** where nickname is your nickname in the room, , password is your password to join the room, history is an
 * object that specifies the amount of history to receive and timeout is the milliseconds to wait for a response from
 * the server.
 * </p>
 * <h3>Examples</h3>
 * <p>
 * In this example we can see how to join a room with a given nickname:
 * </p>
 *
 * <pre>{@code
 * // Get the MultiUserChatManager
 * MultiUserChatManager manager = MultiUserChatManager.getInstanceFor(connection);
 * // Create a MultiUserChat using an XMPPConnection for a room
 * MultiUserChat muc2 = manager.getMultiUserChat(mucJid);
 * // User2 joins the new room
 * // The room service will decide the amount of history to send
 * muc2.join(nickname);
 * }</pre>
 * <p>
 * In this example we can see how to join a room with a given nickname and password:
 * </p>
 *
 * <pre>{@code
 * // Get the MultiUserChatManager
 * MultiUserChatManager manager = MultiUserChatManager.getInstanceFor(connection);
 * // Create a MultiUserChat using an XMPPConnection for a room
 * MultiUserChat muc2 = manager.getMultiUserChat(mucJid);
 * // User2 joins the new room using a password
 * // The room service will decide the amount of history to send
 * muc2.join(nickname, "password");
 * }</pre>
 * <p>
 * In this example we can see how to join a room with a given nickname specifying the amount of history to receive:
 * </p>
 *
 * <pre>{@code
 * // Get the MultiUserChatManager
 * MultiUserChatManager manager = MultiUserChatManager.getInstanceFor(connection);
 *
 * // Create a MultiUserChat using an XMPPConnection for a room
 * MultiUserChat muc2 = manager.getMultiUserChat(mucJid);
 *
 * // User2 joins the new room using a password and specifying
 * // the amount of history to receive. In this example we are requesting the last 5 messages.
 * DiscussionHistory history = new DiscussionHistory();
 * history.setMaxStanzas(5);
 * muc2.join(nickname, "password", history, conn1.getPacketReplyTimeout());
 * }</pre>
 *
 * <h2>Manage room invitations</h2>
 * <h3>Description</h3>
 * <p>
 * It can be useful to invite another user to a room in which one is an occupant. Depending on the room's type the
 * invitee could receive a password to use to join the room and/or be added to the member list if the room is of type
 * members-only. Smack allows to send room invitations and let potential invitees to listening for room invitations and
 * inviters to listen for invitees' rejections.
 * </p>
 * <h3>Usage</h3>
 * <p>
 * In order to invite another user to a room you must be already joined to the room. Once you are joined just send
 * **invite(String participant, String reason)** to the _**MultiUserChat**_ where participant is the user to invite to
 * the room (e.g. hecate@shakespeare.lit) and reason is the reason why the user is being invited.
 * </p>
 * <p>
 * If potential invitees want to listen for room invitations then the invitee must add an _**InvitationListener**_ to
 * the _**MultiUserChatManager**_ class. Since the _**InvitationListener**_ is an _interface_, it is necessary to create
 * a class that implements this _interface_. If an inviter wants to listen for room invitation rejections, just add an
 * _**InvitationRejectionListener**_ to the _**MultiUserChat**_. _**InvitationRejectionListener**_ is also an interface
 * so you will need to create a class that implements this interface.
 * </p>
 * <h3>Examples</h3>
 * <p>
 * In this example we can see how to invite another user to the room and lister for possible rejections:
 * </p>
 *
 * <pre>{@code
 * // Get the MultiUserChatManager
 * MultiUserChatManager manager = MultiUserChatManager.getInstanceFor(connection);
 *
 * // Create a MultiUserChat using an XMPPConnection for a room
 * MultiUserChat muc2 = manager.getMultiUserChat(mucJid);
 *
 * muc2.join(nickname);
 * // User2 listens for invitation rejections
 * muc2.addInvitationRejectionListener(new InvitationRejectionListener() {
 *     public void invitationDeclined(String invitee, String reason) {
 *         // Do whatever you need here...
 *     }
 * });
 * // User2 invites user3 to join to the room
 * muc2.invite(otherJid, "Meet me in this excellent room");
 * }</pre>
 * <p>
 * In this example we can see how to listen for room invitations and decline invitations:
 * </p>
 *
 * <pre>{@code
 * // User3 listens for MUC invitations
 * MultiUserChatManager.getInstanceFor(connection).addInvitationListener(new InvitationListener() {
 *     public void invitationReceived(XMPPConnection conn, String room, EntityFullJid inviter, String reason,
 *                     String password) {
 *         // Reject the invitation
 *         MultiUserChat.decline(conn, room, inviter.asBareJid(), "I'm busy right now");
 *     }
 * });
 * }</pre>
 *
 * <h2>Discover MUC support</h2>
 * <h3>Description</h3>
 * <p>
 * A user may want to discover if one of the user's contacts supports the Multi-User Chat protocol.
 * </p>
 * <h3>Usage</h3>
 * <p>
 * In order to discover if one of the user's contacts supports MUC just send isServiceEnabled(String user)** to the
 * _**MultiUserChatManager**_ class where user is a fully qualified XMPP ID, e.g. jdoe@example.com. You will receive a
 * boolean indicating whether the user supports MUC or not.
 * </p>
 * <h3>Examples</h3>
 * <p>
 * In this example we can see how to discover support of MUC:
 * </p>
 *
 * <pre>{@code
 * // Discover whether user3@host.org supports MUC or not
 * boolean supportsMuc = MultiUserChatManager.getInstanceFor(connection).isServiceEnabled(otherJid);
 * }</pre>
 *
 * <h2>Discover joined rooms</h2>
 * <h3>Description</h3>
 * <p>
 * A user may also want to query a contact regarding which rooms the contact is in.
 * </p>
 * <h3>Usage</h3>
 * <p>
 * In order to get the rooms where a user is in just send getJoinedRooms(String user)** to the
 * _**MultiUserChatManager**_ class where user is a fully qualified XMPP ID, e.g. jdoe@example.com. You will get an
 * Iterator of Strings as an answer where each String represents a room name.
 * </p>
 * <h3>Examples</h3>
 * <p>
 * In this example we can see how to get the rooms where a user is in:
 * </p>
 *
 * <pre>{@code
 * // Get the MultiUserChatManager
 * MultiUserChatManager manager = MultiUserChatManager.getInstanceFor(connection);
 *
 * // Get the rooms where user3@host.org has joined
 * List<String> joinedRooms = manager.getJoinedRooms("user3@host.org/Smack");
 * }</pre>
 *
 * <h2>Discover room information</h2>
 * <h3>Description</h3>
 * <p>
 * A user may need to discover information about a room without having to actually join the room. The server will
 * provide information only for public rooms.
 * </p>
 * <h3>Usage</h3>
 * <p>
 * In order to discover information about a room just send getRoomInfo(String room)** to the _**MultiUserChatManager**_
 * class where room is the XMPP ID of the room, e.g. roomName@conference.myserver. You will get a RoomInfo object that
 * contains the discovered room information.
 * </p>
 * <h3>Examples</h3>
 * <p>
 * In this example we can see how to discover information about a room:
 * </p>
 *
 * <pre>{@code
 * // Get the MultiUserChatManager
 * MultiUserChatManager manager = MultiUserChatManager.getInstanceFor(connection);
 *
 * // Discover information about the room roomName@conference.myserver
 * RoomInfo info = manager.getRoomInfo("roomName@conference.myserver");
 * System.out.println("Number of occupants:" + info.getOccupantsCount());
 * System.out.println("Room Subject:" + info.getSubject());
 * }</pre>
 *
 * <h2>Start a private chat</h2>
 * <h3>Description</h3>
 * <p>
 * A room occupant may want to start a private chat with another room occupant even though they don't know the fully
 * qualified XMPP address (e.g. jdoe@example.com) of each other.
 * </p>
 * <h3>Usage</h3>
 * <p>
 * To create a private chat with another room occupant just send createPrivateChat(String participant)** to the
 * _**MultiUserChat**_ that you used to join the room. The parameter participant is the occupant unique room JID (e.g.
 * 'darkcave@macbeth.shakespeare.lit/Paul'). You will receive a regular _**Chat**_ object that you can use to chat with
 * the other room occupant.
 * </p>
 * <h3>Examples</h3>
 * <p>
 * In this example we can see how to start a private chat with another room occupant:
 * </p>
 *
 * <pre>{@code
 * // Start a private chat with another participant
 * Chat chat = muc2.createPrivateChat("myroom@conference.jabber.org/johndoe");
 * chat.sendMessage("Hello there");
 * }</pre>
 *
 * <h2>Manage changes on room subject</h2>
 * <h3>Description</h3>
 * <p>
 * A common feature of multi-user chat rooms is the ability to change the subject within the room. As a default, only
 * users with a role of "moderator" are allowed to change the subject in a room. Although some rooms may be configured
 * to allow a mere participant or even a visitor to change the subject.
 * </p>
 * <p>
 * Every time the room's subject is changed you may want to be notified of the modification. The new subject could be
 * used to display an in-room message.
 * </p>
 * <h3>Usage</h3>
 * <p>
 * In order to modify the room's subject just send **changeSubject(String subject)** to the _**MultiUserChat**_ that you
 * used to join the room where subject is the new room's subject. On the other hand, if you want to be notified whenever
 * the room's subject is modified you should add a _**SubjectUpdatedListener**_ to the _**MultiUserChat**_ by sending
 ** addSubjectUpdatedListener(SubjectUpdatedListener listener)** to the _**MultiUserChat**_. Since the
 * _**SubjectUpdatedListener**_ is an _interface_, it is necessary to create a class that implements this _interface_.
 * </p>
 * <h3>Examples</h3>
 * <p>
 * In this example we can see how to change the room's subject and react whenever the room's subject is modified:
 * </p>
 *
 * <pre>{@code
 * // An occupant wants to be notified every time the room's subject is changed
 * muc3.addSubjectUpdatedListener(new SubjectUpdatedListener() {
 *      public void subjectUpdated(String subject, String from) {
 *          ....
 *      }
 * });
 * // A room's owner changes the room's subject
 * muc2.changeSubject("New Subject");
 * }</pre>
 *
 * <h2></h2>
 * <h3>Description</h3>
 * <p>
 * There are four defined roles that an occupant can have:
 * </p>
 * <ol>
 * <li>Moderator</li>
 * <li>Participant</li>
 * <li>Visitor</li>
 * <li>None (the absence of a role)</li>
 * </ol>
 * <p>
 * These roles are temporary in that they do not persist across a user's visits to the room and can change during the
 * course of an occupant's visit to the room.
 * </p>
 * <p>
 * A moderator is the most powerful occupant within the context of the room, and can to some extent manage other
 * occupants' roles in the room. A participant has fewer privileges than a moderator, although he or she always has the
 * right to speak. A visitor is a more restricted role within the context of a moderated room, since visitors are not
 * allowed to send messages to all occupants.
 * </p>
 * <p>
 * Roles are granted, revoked, and maintained based on the occupant's room nickname or full JID. Whenever an occupant's
 * role is changed Smack will trigger specific events.
 * </p>
 * <h3>Usage</h3>
 * <p>
 * In order to grant voice (i.e. make someone a _participant_) just send the message **grantVoice(String nickname)** to
 * _**MultiUserChat**_. Use revokeVoice(String nickname)** to revoke the occupant's voice (i.e. make the occupant a
 * _visitor_).
 * </p>
 * <p>
 * In order to grant moderator privileges to a participant or visitor just send the message **grantModerator(String
 * nickname)** to _**MultiUserChat**_. Use revokeModerator(String nickname)** to revoke the moderator privilege from the
 * occupant thus making the occupant a participant.
 * </p>
 * <p>
 * Smack allows you to listen for role modification events. If you are interested in listening role modification events
 * of any occupant then use the listener _ParticipantStatusListener_**. But if you are interested in listening for your
 * own role modification events, use the listener **_UserStatusListener_**. Both listeners should be added to the
 * _**MultiUserChat**_ by using addParticipantStatusListener(ParticipantStatusListener listener)** or
 ** addUserStatusListener(UserStatusListener listener)** respectively. These listeners include several notification
 * events but you may be interested in just a few of them. Smack provides default implementations for these listeners
 * avoiding you to implement all the interfaces' methods. The default implementations are
 * **_DefaultUserStatusListener_** and _DefaultParticipantStatusListener_**. Below you will find the sent messages to
 * the listeners whenever an occupant's role has changed.
 * </p>
 * <p>
 * These are the triggered events when the role has been upgraded:
 * </p>
 * <table border="1">
 * <caption>Role Upgrade paths</caption>
 * <tr>
 * <th>Old</th>
 * <th>New</th>
 * <th>Events</th>
 * </tr>
 * <tr>
 * <td>None</td>
 * <td>Visitor</td>
 * <td>--</td>
 * </tr>
 * <tr>
 * <td>Visitor</td>
 * <td>Participant</td>
 * <td>voiceGranted</td>
 * </tr>
 * <tr>
 * <td>Participant</td>
 * <td>Moderator</td>
 * <td>moderatorGranted</td>
 * </tr>
 * <tr>
 * <td>None</td>
 * <td>Participant</td>
 * <td>voiceGranted</td>
 * </tr>
 * <tr>
 * <td>None</td>
 * <td>Moderator</td>
 * <td>voiceGranted + moderatorGranted</td>
 * </tr>
 * <tr>
 * <td>Visitor</td>
 * <td>Moderator</td>
 * <td>voiceGranted + moderatorGranted</td>
 * </tr>
 * </table>
 * <p>
 * These are the triggered events when the role has been downgraded:
 * </p>
 * <table border="1">
 * <caption>Role Downgrade paths</caption>
 * <tr>
 * <th>Old</th>
 * <th>New</th>
 * <th>Events</th>
 * </tr>
 * <tr>
 * <td>Moderator</td>
 * <td>Participant</td>
 * <td>moderatorRevoked</td>
 * </tr>
 * <tr>
 * <td>Participant</td>
 * <td>Vistor</td>
 * <td>voiceRevoked</td>
 * </tr>
 * <tr>
 * <td>Visitor</td>
 * <td>None</td>
 * <td>kicked</td>
 * </tr>
 * <tr>
 * <td>Moderator</td>
 * <td>Visitor</td>
 * <td>voiceRevoked + moderatorRevoked</td>
 * </tr>
 * <tr>
 * <td>Moderator</td>
 * <td>None</td>
 * <td>kicked</td>
 * </tr>
 * <tr>
 * <td>Participant</td>
 * <td>None</td>
 * <td>kicked</td>
 * </tr>
 * </table>
 * <h3>Examples</h3>
 * <p>
 * In this example we can see how to grant voice to a visitor and listen for the notification events:
 * </p>
 *
 * <pre>{@code
 * // User1 creates a room
 * muc = manager.getMultiUserChat("myroom@conference.jabber.org");
 * muc.create("testbot");
 * // User1 (which is the room owner) configures the room as a moderated room
 * Form form = muc.getConfigurationForm();
 * FillableForm answerForm = configForm.getFillableForm();
 * answerForm.setAnswer("muc#roomconfig_moderatedroom", "1");
 * muc.sendConfigurationForm(answerForm);
 *
 * // User2 joins the new room (as a visitor)
 * MultiUserChat muc2 = manager2.getMultiUserChat("myroom@conference.jabber.org");
 * muc2.join("testbot2");
 * // User2 will listen for his own "voice" notification events
 * muc2.addUserStatusListener(new DefaultUserStatusListener() {
 *      public void voiceGranted() {
 *          super.voiceGranted();
 *          ...
 *      }
 *      public void voiceRevoked() {
 *          super.voiceRevoked();
 *          ...
 *      }
 * });
 *
 * // User3 joins the new room (as a visitor)
 * MultiUserChat muc3 = manager3.getMultiUserChat("myroom@conference.jabber.org");
 * muc3.join("testbot3");
 * // User3 will lister for other occupants "voice" notification events
 * muc3.addParticipantStatusListener(new DefaultParticipantStatusListener() {
 *      public void voiceGranted(String participant) {
 *          super.voiceGranted(participant);
 *          ...
 *      }
 *      public void voiceRevoked(String participant) {
 *          super.voiceRevoked(participant);
 *          ...
 *      }
 * });
 *
 * // The room's owner grants voice to user2
 * muc.grantVoice("testbot2");
 * }</pre>
 *
 * <h2>Manage affiliation modifications</h2>
 * <h3>Description</h3>
 * <p>
 * There are five defined affiliations that a user can have in relation to a room:
 * </p>
 * <ol>
 * <li>Owner</li>
 * <li>Admin</li>
 * <li>Member</li>
 * <li>Outcast</li>
 * <li>None (the absence of an affiliation)</li>
 * </ol>
 * <p>
 * These affiliations are semi-permanent in that they persist across a user's visits to the room and are not affected by
 * happenings in the room. Affiliations are granted, revoked, and maintained based on the user's bare JID.
 * </p>
 * <p>
 * If a user without a defined affiliation enters a room, the user's affiliation is defined as "none"; however, this
 * affiliation does not persist across visits.
 * </p>
 * <p>
 * Owners and admins are by definition immune from certain actions. Specifically, an owner or admin cannot be kicked
 * from a room and cannot be banned from a room. An admin must first lose his or her affiliation (i.e., have an
 * affiliation of "none" or "member") before such actions could be performed on them.
 * </p>
 * <p>
 * The member affiliation provides a way for a room owner or admin to specify a "whitelist" of users who are allowed to
 * enter a members-only room. When a member enters a members-only room, his or her affiliation does not change, no
 * matter what his or her role is. The member affiliation also provides a way for users to effectively register with an
 * open room and thus be permanently associated with that room in some way (one result may be that the user's nickname
 * is reserved in the room).
 * </p>
 * <p>
 * An outcast is a user who has been banned from a room and who is not allowed to enter the room. Whenever a user's
 * affiliation is changed Smack will trigger specific events.
 * </p>
 * <h3>Usage</h3>
 * <p>
 * In order to grant membership to a room, administrator privileges or owner priveliges just send
 * **grantMembership(String jid)**, **grantAdmin(String jid)** or **grantOwnership(String jid)** to _**MultiUserChat**_
 * respectively. Use **revokeMembership(String jid)**, **revokeAdmin(String jid)** or revokeOwnership(String jid)** to
 * revoke the membership to a room, administrator privileges or owner priveliges respectively.
 * </p>
 * <p>
 * In order to ban a user from the room just send the message **banUser(String jid, String reason)** to
 * _**MultiUserChat**_.
 * </p>
 * <p>
 * Smack allows you to listen for affiliation modification events. If you are interested in listening affiliation
 * modification events of any user then use the listener **_ParticipantStatusListener_**. But if you are interested in
 * listening for your own affiliation modification events, use the listener _UserStatusListener_**. Both listeners
 * should be added to the _**MultiUserChat**_ by using addParticipantStatusListener(ParticipantStatusListener
 * listener)** or addUserStatusListener(UserStatusListener listener)** respectively. These listeners include several
 * notification events but you may be interested in just a few of them. Smack provides default implementations for these
 * listeners avoiding you to implement all the interfaces' methods. The default implementations are
 * **_DefaultUserStatusListener_** and _DefaultParticipantStatusListener_**. Below you will find the sent messages to
 * the listeners whenever a user's affiliation has changed.
 * </p>
 * <p>
 * These are the triggered events when the affiliation has been upgraded:
 * </p>
 * <table border="1">
 * <caption>Affiliation Upgrade paths</caption>
 * <tr>
 * <th>Old</th>
 * <th>New</th>
 * <th>Events</th>
 * </tr>
 * <tr>
 * <td>None</td>
 * <td>Member</td>
 * <td>membershipGranted</td>
 * </tr>
 * <tr>
 * <td>Member</td>
 * <td>Admin</td>
 * <td>membershipRevoked + adminGranted</td>
 * </tr>
 * <tr>
 * <td>Admin</td>
 * <td>Owner</td>
 * <td>adminRevoked + ownershipGranted</td>
 * </tr>
 * <tr>
 * <td>None</td>
 * <td>Admin</td>
 * <td>adminGranted</td>
 * </tr>
 * <tr>
 * <td>None</td>
 * <td>Owner</td>
 * <td>ownershipGranted</td>
 * </tr>
 * <tr>
 * <td>Member</td>
 * <td>Owner</td>
 * <td>membershipRevoked + ownershipGranted</td>
 * </tr>
 * </table>
 * <p>
 * These are the triggered events when the affiliation has been downgraded:
 * </p>
 * <table border="1">
 * <caption>Affiliation Downgrade paths</caption>
 * <tr>
 * <th>Owner</th>
 * <th>Admin</th>
 * <th>ownershipRevoked + adminGranted</th>
 * </tr>
 * <tr>
 * <td>Admin</td>
 * <td>Member</td>
 * <td>adminRevoked + membershipGranted</td>
 * </tr>
 * <tr>
 * <td>Member</td>
 * <td>None</td>
 * <td>membershipRevoked</td>
 * </tr>
 * <tr>
 * <td>Owner</td>
 * <td>Member</td>
 * <td>ownershipRevoked + membershipGranted</td>
 * </tr>
 * <tr>
 * <td>Owner</td>
 * <td>None</td>
 * <td>ownershipRevoked</td>
 * </tr>
 * <tr>
 * <td>Admin</td>
 * <td>None</td>
 * <td>adminRevoked</td>
 * </tr>
 * <tr>
 * <td>Anyone</td>
 * <td>Outcast</td>
 * <td>banned</td>
 * </tr>
 * </table>
 * <h3>Examples</h3>
 * <p>
 * In this example we can see how to grant admin privileges to a user and listen for the notification events:
 * </p>
 *
 * <pre>{@code
 * // User1 creates a room
 * muc = manager.getMultiUserChat("myroom@conference.jabber.org");
 * muc.create("testbot");
 * // User1 (which is the room owner) configures the room as a moderated room
 * Form form = muc.getConfigurationForm();
 * FillableForm answerForm = configForm.getFillableForm();
 * answerForm.setAnswer("muc#roomconfig_moderatedroom", "1");
 * muc.sendConfigurationForm(answerForm);
 *
 * // User2 joins the new room (as a visitor)
 * MultiUserChat muc2 = manager2.getMultiUserChat("myroom@conference.jabber.org");
 * muc2.join("testbot2");
 * // User2 will listen for his own admin privileges
 * muc2.addUserStatusListener(new DefaultUserStatusListener() {
 *      public void membershipRevoked() {
 *          super.membershipRevoked();
 *          ...
 *      }
 *      public void adminGranted() {
 *          super.adminGranted();
 *          ...
 *      }
 * });
 *
 * // User3 joins the new room (as a visitor)
 * MultiUserChat muc3 = manager3.getMultiUserChat("myroom@conference.jabber.org");
 * muc3.join("testbot3");
 * // User3 will lister for other users admin privileges
 * muc3.addParticipantStatusListener(new DefaultParticipantStatusListener() {
 *      public void membershipRevoked(String participant) {
 *          super.membershipRevoked(participant);
 *          ...
 *      }
 *      public void adminGranted(String participant) {
 *          super.adminGranted(participant);
 *          ...
 *      }
 * });
 * // The room's owner grants admin privileges to user2
 * muc.grantAdmin("user2@jabber.org");
 * }</pre>
 *
 * @see <a href="https://xmpp.org/extensions/xep-0045.html">XEP-0045: Multi-User Chat</a>
 */
package org.jivesoftware.smackx.muc;
