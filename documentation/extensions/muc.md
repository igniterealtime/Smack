Multi User Chat
===============

Allows configuration of, participation in, and administration of individual
text-based conference rooms.

  * Create a new Room
  * Join a room
  * Manage room invitations
  * Discover MUC support
  * Discover joined rooms
  * Discover room information
  * Start a private chat
  * Manage changes on room subject
  * Manage role modifications
  * Manage affiliation modifications

**XEP related:** [XEP-45](http://www.xmpp.org/extensions/xep-0045.html)

Create a new Room
-----------------

**Description**

Allowed users may create new rooms. There are two types of rooms that you can
create. **Instant rooms** which are available for immediate access and are
automatically created based on some default configuration and **Reserved
rooms** which are manually configured by the room creator before anyone is
allowed to enter.

**Usage**

In order to create a room you will need to first create an instance of
_**MultiUserChat**_. The room name passed to the constructor will be the name
of the room to create. The next step is to send **create(String nickname)** to
the _**MultiUserChat**_ instance where nickname is the nickname to use when
joining the room.

Depending on the type of room that you want to create you will have to use
different configuration forms. In order to create an Instant room just send
**sendConfigurationForm(Form form)** where form is an empty form. But if you
want to create a Reserved room then you should first get the room's
configuration form, complete the form and finally send it back to the server.

**Examples**

In this example we can see how to create an instant room:

```
// Create a MultiUserChat using a XMPPConnection for a room
MultiUserChat muc = new MultiUserChat(conn1, "myroom@conference.jabber.org");

// Create the room
muc.create("testbot");

// Send an empty room configuration form which indicates that we want
// an instant room
muc.sendConfigurationForm(new Form(Form.TYPE_SUBMIT));
```

In this example we can see how to create a reserved room. The form is
completed with default values:


// Create a MultiUserChat using a XMPPConnection for a room
MultiUserChat muc = new MultiUserChat(conn1, "myroom@conference.jabber.org");

// Create the room
muc.create("testbot");

// Get the the room's configuration form
Form form = muc.getConfigurationForm();
// Create a new form to submit based on the original form
Form submitForm = form.createAnswerForm();
// Add default answers to the form to submit
for (Iterator fields = form.getFields(); fields.hasNext();) {
	FormField field = (FormField) fields.next();
	if (!FormField.TYPE_HIDDEN.equals(field.getType()) && field.getVariable() != null) {
		// Sets the default value as the answer
		submitForm.setDefaultAnswer(field.getVariable());
}
}
// Sets the new owner of the room
List owners = new ArrayList();
owners.add("johndoe@jabber.org");
submitForm.setAnswer("muc#roomconfig_roomowners", owners);
// Send the completed form (with default values) to the server to configure the room
muc.sendConfigurationForm(submitForm);
```

Join a room
-----------

**Description**

Your usual first step in order to send messages to a room is to join the room.
Multi User Chat allows to specify several parameter while joining a room.
Basically you can control the amount of history to receive after joining the
room as well as provide your nickname within the room and a password if the
room is password protected.

**Usage**

In order to join a room you will need to first create an instance of
_**MultiUserChat**_. The room name passed to the constructor will be the name
of the room to join. The next step is to send **join(...)** to the
_**MultiUserChat**_ instance. But first you will have to decide which join
message to send. If you want to just join the room without a password and
without specifying the amount of history to receive then you could use
**join(String nickname)** where nickname if your nickname in the room. In case
the room requires a password in order to join you could then use **join(String
nickname, String password)**. And finally, the most complete way to join a
room is to send **join(String nickname, String password, DiscussionHistory
history, long timeout)** where nickname is your nickname in the room, ,
password is your password to join the room, history is an object that
specifies the amount of history to receive and timeout is the milliseconds to
wait for a response from the server.

**Examples**

In this example we can see how to join a room with a given nickname:

```
// Create a MultiUserChat using a XMPPConnection for a room
MultiUserChat muc2 = new MultiUserChat(conn1, "myroom@conference.jabber.org");
// User2 joins the new room
// The room service will decide the amount of history to send
muc2.join("testbot2");
```

In this example we can see how to join a room with a given nickname and
password:

```
// Create a MultiUserChat using a XMPPConnection for a room
MultiUserChat muc2 = new MultiUserChat(conn1, "myroom@conference.jabber.org");

// User2 joins the new room using a password
// The room service will decide the amount of history to send
muc2.join("testbot2", "password");
```

In this example we can see how to join a room with a given nickname specifying
the amount of history to receive:

```
// Create a MultiUserChat using a XMPPConnection for a room
MultiUserChat muc2 = new MultiUserChat(conn1, "myroom@conference.jabber.org");

// User2 joins the new room using a password and specifying
// the amount of history to receive. In this example we are requesting the last 5 messages.
DiscussionHistory history = new DiscussionHistory();
history.setMaxStanzas(5);
muc2.join("testbot2", "password", history, conn1.getPacketReplyTimeout());
```

Manage room invitations
-----------------------

**Description**

It can be useful to invite another user to a room in which one is an occupant.
Depending on the room's type the invitee could receive a password to use to
join the room and/or be added to the member list if the room is of type
members-only. Smack allows to send room invitations and let potential invitees
to listening for room invitations and inviters to listen for invitees'
rejections.

**Usage**

In order to invite another user to a room you must be already joined to the
room. Once you are joined just send **invite(String participant, String
reason)** to the _**MultiUserChat**_ where participant is the user to invite
to the room (e.g. hecate@shakespeare.lit) and reason is the reason why the
user is being invited.

If potential invitees want to listen for room invitations then the invitee
must add an _**InvitationListener**_ to the _**MultiUserChat**_ class. Since
the _**InvitationListener**_ is an _interface_, it is necessary to create a
class that implements this _interface_. If an inviter wants to listen for room
invitation rejections, just add an _**InvitationRejectionListener**_ to the
_**MultiUserChat**_. _**InvitationRejectionListener**_ is also an interface so
you will need to create a class that implements this interface.

**Examples**

In this example we can see how to invite another user to the room and lister
for possible rejections:

```
// User2 joins the room
MultiUserChat muc2 = new MultiUserChat(conn2, room);
muc2.join("testbot2");
// User2 listens for invitation rejections
muc2.addInvitationRejectionListener(new InvitationRejectionListener() {
	public void invitationDeclined(String invitee, String reason) {
		// Do whatever you need here...
	}
});
// User2 invites user3 to join to the room
muc2.invite("user3@host.org/Smack", "Meet me in this excellent room");
```

In this example we can see how to listen for room invitations and decline
invitations:

```
// User3 listens for MUC invitations
MultiUserChat.addInvitationListener(conn3, new InvitationListener() {
	public void invitationReceived(XMPPConnection conn, String room, String inviter, String reason, String password) {
		// Reject the invitation
		MultiUserChat.decline(conn, room, inviter, "I'm busy right now");
	}
});
```

Discover MUC support
--------------------

**Description**

A user may want to discover if one of the user's contacts supports the Multi-
User Chat protocol.

**Usage**

In order to discover if one of the user's contacts supports MUC just send
**isServiceEnabled(XMPPConnection connection, String user)** to the
_**MultiUserChat**_ class where user is a fully qualified XMPP ID, e.g.
jdoe@example.com. You will receive a boolean indicating whether the user
supports MUC or not.

**Examples**

In this example we can see how to discover support of MUC:

```
// Discover whether user3@host.org supports MUC or not
boolean supports = MultiUserChat.isServiceEnabled(conn, "user3@host.org/Smack");
```

Discover joined rooms
---------------------

**Description**

A user may also want to query a contact regarding which rooms the contact is
in.

**Usage**

In order to get the rooms where a user is in just send
**getJoinedRooms(XMPPConnection connection, String user)** to the
_**MultiUserChat**_ class where user is a fully qualified XMPP ID, e.g.
jdoe@example.com. You will get an Iterator of Strings as an answer where each
String represents a room name.

**Examples**

In this example we can see how to get the rooms where a user is in:

```
// Get the rooms where user3@host.org has joined
Iterator joinedRooms = MultiUserChat.getJoinedRooms(conn, "user3@host.org/Smack");
```

Discover room information
-------------------------

**Description**

A user may need to discover information about a room without having to
actually join the room. The server will provide information only for public
rooms.

**Usage**

In order to discover information about a room just send
**getRoomInfo(XMPPConnection connection, String room)** to the
_**MultiUserChat**_ class where room is the XMPP ID of the room, e.g.
roomName@conference.myserver. You will get a RoomInfo object that contains the
discovered room information.

**Examples**

In this example we can see how to discover information about a room:

```
// Discover information about the room roomName@conference.myserver
RoomInfo info = MultiUserChat.getRoomInfo(conn, "roomName@conference.myserver");
System.out.println("Number of occupants:" + info.getOccupantsCount());
System.out.println("Room Subject:" + info.getSubject());
```

Start a private chat
--------------------

**Description**

A room occupant may want to start a private chat with another room occupant
even though they don't know the fully qualified XMPP ID (e.g.
jdoe@example.com) of each other.

**Usage**

To create a private chat with another room occupant just send
**createPrivateChat(String participant)** to the _**MultiUserChat**_ that you
used to join the room. The parameter participant is the occupant unique room
JID (e.g. 'darkcave@macbeth.shakespeare.lit/Paul'). You will receive a regular
_**Chat**_ object that you can use to chat with the other room occupant.

**Examples**

In this example we can see how to start a private chat with another room
occupant:

```
// Start a private chat with another participant
Chat chat = muc2.createPrivateChat("myroom@conference.jabber.org/johndoe");
chat.sendMessage("Hello there");
```

Manage changes on room subject
------------------------------

**Description**

A common feature of multi-user chat rooms is the ability to change the subject
within the room. As a default, only users with a role of "moderator" are
allowed to change the subject in a room. Although some rooms may be configured
to allow a mere participant or even a visitor to change the subject.

Every time the room's subject is changed you may want to be notified of the
modification. The new subject could be used to display an in-room message.

**Usage**

In order to modify the room's subject just send **changeSubject(String
subject)** to the _**MultiUserChat**_ that you used to join the room where
subject is the new room's subject. On the other hand, if you want to be
notified whenever the room's subject is modified you should add a
_**SubjectUpdatedListener**_ to the _**MultiUserChat**_ by sending
**addSubjectUpdatedListener(SubjectUpdatedListener listener)** to the
_**MultiUserChat**_. Since the _**SubjectUpdatedListener**_ is an _interface_,
it is necessary to create a class that implements this _interface_.

**Examples**

In this example we can see how to change the room's subject and react whenever
the room's subject is modified:

```
// An occupant wants to be notified every time the room's subject is changed
muc3.addSubjectUpdatedListener(new SubjectUpdatedListener() {
	public void subjectUpdated(String subject, String from) {
		....
	}
});
// A room's owner changes the room's subject
muc2.changeSubject("New Subject");
```

Manage role modifications
-------------------------

**Description**

There are four defined roles that an occupant can have:

  1. Moderator
  2. Participant
  3. Visitor
  4. None (the absence of a role)

These roles are temporary in that they do not persist across a user's visits
to the room and can change during the course of an occupant's visit to the
room.

A moderator is the most powerful occupant within the context of the room, and
can to some extent manage other occupants' roles in the room. A participant
has fewer privileges than a moderator, although he or she always has the right
to speak. A visitor is a more restricted role within the context of a
moderated room, since visitors are not allowed to send messages to all
occupants.

Roles are granted, revoked, and maintained based on the occupant's room
nickname or full JID. Whenever an occupant's role is changed Smack will
trigger specific events.

**Usage**

In order to grant voice (i.e. make someone a _participant_) just send the
message **grantVoice(String nickname)** to _**MultiUserChat**_. Use
**revokeVoice(String nickname)** to revoke the occupant's voice (i.e. make the
occupant a _visitor_).

In order to grant moderator privileges to a participant or visitor just send
the message **grantModerator(String nickname)** to _**MultiUserChat**_. Use
**revokeModerator(String nickname)** to revoke the moderator privilege from
the occupant thus making the occupant a participant.

Smack allows you to listen for role modification events. If you are interested
in listening role modification events of any occupant then use the listener
**_ParticipantStatusListener_**. But if you are interested in listening for
your own role modification events, use the listener **_UserStatusListener_**.
Both listeners should be added to the _**MultiUserChat**_ by using
**addParticipantStatusListener(ParticipantStatusListener listener)** or
**addUserStatusListener(UserStatusListener listener)** respectively. These
listeners include several notification events but you may be interested in
just a few of them. Smack provides default implementations for these listeners
avoiding you to implement all the interfaces' methods. The default
implementations are **_DefaultUserStatusListener_** and
**_DefaultParticipantStatusListener_**. Below you will find the sent messages
to the listeners whenever an occupant's role has changed.

These are the triggered events when the role has been upgraded:

| Old | New | Events |
|-----|-----|--------|
| None | Visitor | -- |
| Visitor | Participant | voiceGranted |
| Participant | Moderator | moderatorGranted |
| None | Participant | voiceGranted |
| None | Moderator | voiceGranted + moderatorGranted |
| Visitor | Moderator | voiceGranted + moderatorGranted |

These are the triggered events when the role has been downgraded:

| Old | New | Events |
|-----|-----|--------|
| Moderator | Participant | moderatorRevoked |
| Participant | Visitor | voiceRevoked |
| Visitor | None | kicked |
| Moderator | Visitor | voiceRevoked + moderatorRevoked |
| Moderator | None | kicked |
| Participant | None | kicked |

**Examples**

In this example we can see how to grant voice to a visitor and listen for the
notification events:

```
// User1 creates a room
muc = new MultiUserChat(conn1, "myroom@conference.jabber.org");
muc.create("testbot");
// User1 (which is the room owner) configures the room as a moderated room
Form form = muc.getConfigurationForm();
Form answerForm = form.createAnswerForm();
answerForm.setAnswer("muc#roomconfig_moderatedroom", "1");
muc.sendConfigurationForm(answerForm);
// User2 joins the new room (as a visitor)
MultiUserChat muc2 = new MultiUserChat(conn2, "myroom@conference.jabber.org");
muc2.join("testbot2");
// User2 will listen for his own "voice" notification events
muc2.addUserStatusListener(new DefaultUserStatusListener() {
	public void voiceGranted() {
		super.voiceGranted();
		...
	}
	public void voiceRevoked() {
		super.voiceRevoked();
		...
	}
});
// User3 joins the new room (as a visitor)
MultiUserChat muc3 = new MultiUserChat(conn3, "myroom@conference.jabber.org");
muc3.join("testbot3");
// User3 will lister for other occupants "voice" notification events
muc3.addParticipantStatusListener(new DefaultParticipantStatusListener() {
	public void voiceGranted(String participant) {
		super.voiceGranted(participant);
		...
	}
	public void voiceRevoked(String participant) {
		super.voiceRevoked(participant);
		...
	}
});

// The room's owner grants voice to user2
muc.grantVoice("testbot2");
```

Manage affiliation modifications
--------------------------------

**Description**

There are five defined affiliations that a user can have in relation to a
room:

  1. Owner
  2. Admin
  3. Member
  4. Outcast
  5. None (the absence of an affiliation)

These affiliations are semi-permanent in that they persist across a user's
visits to the room and are not affected by happenings in the room.
Affiliations are granted, revoked, and maintained based on the user's bare
JID.

If a user without a defined affiliation enters a room, the user's affiliation
is defined as "none"; however, this affiliation does not persist across
visits.

Owners and admins are by definition immune from certain actions. Specifically,
an owner or admin cannot be kicked from a room and cannot be banned from a
room. An admin must first lose his or her affiliation (i.e., have an
affiliation of "none" or "member") before such actions could be performed on
them.

The member affiliation provides a way for a room owner or admin to specify a
"whitelist" of users who are allowed to enter a members-only room. When a
member enters a members-only room, his or her affiliation does not change, no
matter what his or her role is. The member affiliation also provides a way for
users to effectively register with an open room and thus be permanently
associated with that room in some way (one result may be that the user's
nickname is reserved in the room).

An outcast is a user who has been banned from a room and who is not allowed to
enter the room. Whenever a user's affiliation is changed Smack will trigger
specific events.

**Usage**

In order to grant membership to a room, administrator privileges or owner
priveliges just send **grantMembership(String jid)**, **grantAdmin(String
jid)** or **grantOwnership(String jid)** to _**MultiUserChat**_ respectively.
Use **revokeMembership(String jid)**, **revokeAdmin(String jid)** or
**revokeOwnership(String jid)** to revoke the membership to a room,
administrator privileges or owner priveliges respectively.

In order to ban a user from the room just send the message **banUser(String
jid, String reason)** to _**MultiUserChat**_.

Smack allows you to listen for affiliation modification events. If you are
interested in listening affiliation modification events of any user then use
the listener **_ParticipantStatusListener_**. But if you are interested in
listening for your own affiliation modification events, use the listener
**_UserStatusListener_**. Both listeners should be added to the
_**MultiUserChat**_ by using
**addParticipantStatusListener(ParticipantStatusListener listener)** or
**addUserStatusListener(UserStatusListener listener)** respectively. These
listeners include several notification events but you may be interested in
just a few of them. Smack provides default implementations for these listeners
avoiding you to implement all the interfaces' methods. The default
implementations are **_DefaultUserStatusListener_** and
**_DefaultParticipantStatusListener_**. Below you will find the sent messages
to the listeners whenever a user's affiliation has changed.

These are the triggered events when the affiliation has been upgraded:

| Old | New | Events |
|-----|-----|--------|
| None | Member | membershipGranted |
| Member | Admin | membershipRevoked + adminGranted |
| Admin | Owner | adminRevoked + ownershipGranted |
| None | Admin | adminGranted |
| None | Owner | ownershipGranted |
| Member | Owner | membershipRevoked + ownershipGranted |

These are the triggered events when the affiliation has been downgraded:

| Old | New | Events |
|-----|-----|--------|
| Owner | Admin | ownershipRevoked + adminGranted |
| Admin | Member | adminRevoked + membershipGranted |
| Member | None | membershipRevoked |
| Owner | Member | ownershipRevoked + membershipGranted |
| Owner | None | ownershipRevoked |
| Admin | None | adminRevoked |
| _Anyone_ | Outcast | banned |

**Examples**

In this example we can see how to grant admin privileges to a user and listen
for the notification events:

```
// User1 creates a room
muc = new MultiUserChat(conn1, "myroom@conference.jabber.org");
muc.create("testbot");
// User1 (which is the room owner) configures the room as a moderated room
Form form = muc.getConfigurationForm();
Form answerForm = form.createAnswerForm();
answerForm.setAnswer("muc#roomconfig_moderatedroom", "1");
muc.sendConfigurationForm(answerForm);
// User2 joins the new room (as a visitor)
MultiUserChat muc2 = new MultiUserChat(conn2, "myroom@conference.jabber.org");
muc2.join("testbot2");
// User2 will listen for his own admin privileges
muc2.addUserStatusListener(new DefaultUserStatusListener() {
	public void membershipRevoked() {
		super.membershipRevoked();
		...
	}
	public void adminGranted() {
		super.adminGranted();
		...
	}
});
// User3 joins the new room (as a visitor)
MultiUserChat muc3 = new MultiUserChat(conn3, "myroom@conference.jabber.org");
muc3.join("testbot3");
// User3 will lister for other users admin privileges
muc3.addParticipantStatusListener(new DefaultParticipantStatusListener() {
	public void membershipRevoked(String participant) {
		super.membershipRevoked(participant);
		...
	}
	public void adminGranted(String participant) {
		super.adminGranted(participant);
		...
	}
});
// The room's owner grants admin privileges to user2
muc.grantAdmin("user2@jabber.org");
```
