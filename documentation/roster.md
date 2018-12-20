Roster and Presence
===================

[Back](index.md)

The roster lets you keep track of the availability ("presence") of other
users. A roster also allows you to organize users into groups such as
"Friends" and "Co-workers". Other IM systems refer to the roster as the buddy
list, contact list, etc.

A `Roster` instance is obtained using the `Roster.getInstanceFor(XMPPConnection)` method.

A detailed descriptrion of the protcol behind the Roster and Presence
semantics can be found in [RFC
6120](https://tools.ietf.org/html/rfc6121).

Roster Entries
--------------

Every user in a roster is represented by a RosterEntry, which consists of:

  * An XMPP address, aka. JID (e.g. jsmith@example.com).
  * A name you've assigned to the user (e.g. "Joe").
  * The list of groups in the roster that the entry belongs to. If the roster entry belongs to no groups, it's called an "unfiled entry".  The following code snippet prints all entries in the roster: 

```
Roster roster = Roster.getInstanceFor(connection);
Collection<RosterEntry> entries = roster.getEntries();
for (RosterEntry entry : entries) {
	System.out.println(entry);
}
```

Methods also exist to get individual entries, the list of unfiled entries, or
to get one or all roster groups.

Presence

![Roster](images/roster.png)

Every entry in the roster has presence associated with it. The
`Roster.getPresence(BareJid user)` method will return a Presence object with
the user's presence or `null` if the user is not online or you are not
subscribed to the user's presence. _Note:_ Presence subscription is
nnot tied to the user being on the roster, and vice versa: You could
be subscriped to a remote users presence without the user in your roster, and
a remote user can be in your roster without any presence subscription relation.

A user either has a presence of online or offline. When a user is online,
their presence may contain extended information such as what they are
currently doing, whether they wish to be disturbed, etc. See the Presence
class for further details.

Listening for Roster and Presence Changes
-----------------------------------------

The typical use of the roster class is to display a tree view of groups and
entries along with the current presence value of each entry. As an example,
see the image showing a Roster in the Exodus XMPP client to the right.

The presence information will likely change often, and it's also possible for
the roster entries to change or be deleted. To listen for changing roster and
presence data, a RosterListener should be used. To be informed about all
changes to the roster the RosterListener should be registered before logging
into the XMPP server. The following code snippet registers a RosterListener
with the Roster that prints any presence changes in the roster to standard
out. A normal client would use similar code to update the roster UI with the
changing information.

```
Roster roster = Roster.getInstanceFor(con);
roster.addRosterListener(new RosterListener() {
	// Ignored events public void entriesAdded(Collection<String> addresses) {}
	public void entriesDeleted(Collection<String> addresses) {}
	public void entriesUpdated(Collection<String> addresses) {}
	public void presenceChanged(Presence presence) {
		System.out.println("Presence changed: " + presence.getFrom() + " " + presence);
	}
});
```

Note that in order to receive presence changed events you need to be subscribed
to the users presence. See the following section.

Adding Entries to the Roster
----------------------------

Rosters and presence use a permissions-based model where users must give
permission before someone else can see their presence. This protects a
user's privacy by making sure that only approved users are able to view their
presence information. Therefore, when you add a new roster entry, you will not
see the presence information until the other user accepts your request.

If another user requests a presence subscription, you must accept or reject
that request. Smack handles presence subscription requests in one of three ways:

  * Automatically accept all presence subscription requests.
  * Automatically reject all presence subscription requests.
  * Process presence subscription requests manually.  The mode can be set using the `Roster.setSubscriptionMode(Roster.SubscriptionMode)` method. Simple clients normally use one of the automated subscription modes, while full-featured clients should manually process subscription requests and let the end-user accept or reject each request. If using the manual mode, a PacketListener should be registered that listens for Presence packets that have a type of `Presence.Type.subscribe`.   

Copyright (C) Jive Software 2002-2008
