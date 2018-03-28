Roster Item Exchange
====================

[Back](index.md)

This extension is used to send rosters, roster groups and roster entries from
one XMPP Entity to another. It also provides an easy way to hook up custom
logic when entries are received from other XMPP clients.

Follow these links to learn how to send and receive roster items:

  * Send a complete roster
  * Send a roster's group
  * Send a roster's entry
  * Receive roster entries

**XEP related:** [XEP-93](http://www.xmpp.org/extensions/xep-0093.html)

Send a entire roster
-------------------

**Description**

Sometimes it is useful to send a whole roster to another user. Smack provides
a very easy way to send a complete roster to another XMPP client.

**Usage**

Create an instance of _**RosterExchangeManager**_ and use the **#send(Roster,
String)** message to send a roster to a given user. The first parameter is the
roster to send and the second parameter is the id of the user that will
receive the roster entries.

**Example**

In this example we can see how user1 sends his roster to user2.

```
XMPPConnection conn1 = …

// Create a new roster exchange manager on conn1
RosterExchangeManager rosterExchangeManager = new RosterExchangeManager(conn1);
// Send user1's roster to user2
rosterExchangeManager.send(Roster.getInstanceFor(conn1), user2);
```

Send a roster group
-------------------

**Description**

It is also possible to send a roster group to another XMPP client. A roster
group groups a set of roster entries under a name.

**Usage**

Create an instance of _**RosterExchangeManager**_ and use the
**#send(RosterGroup, String)** message to send a roster group to a given user.
The first parameter is the roster group to send and the second parameter is
the id of the user that will receive the roster entries.

**Example**

In this example we can see how user1 sends his roster groups to user2.

```
XMPPConnection conn1 = …

// Create a new roster exchange manager on conn1
RosterExchangeManager rosterExchangeManager = new RosterExchangeManager(conn1);
// Send user1's RosterGroups to user2
for (Iterator it = Roster.getInstanceFor(conn1).getGroups(); it.hasNext(); )
rosterExchangeManager.send((RosterGroup)it.next(), user2);
```

Send a roster entry
-------------------

**Description**

Sometimes you may need to send a single roster entry to another XMPP client.
Smack also lets you send items at this granularity level.

**Usage**

Create an instance of _**RosterExchangeManager**_ and use the
**#send(RosterEntry, String)** message to send a roster entry to a given user.
The first parameter is the roster entry to send and the second parameter is
the id of the user that will receive the roster entries.

**Example**

In this example we can see how user1 sends a roster entry to user2.

```
XMPPConnection conn1 = …

// Create a new roster exchange manager on conn1
RosterExchangeManager rosterExchangeManager = new RosterExchangeManager(conn1);
// Send a roster entry (any) to user2
rosterExchangeManager1.send((RosterEntry)Roster.getInstanceFor(conn1).getEntries().next(), user2);
```

Receive roster entries
----------------------

**Description**

Since roster items are sent between XMPP clients, it is necessary to listen to
possible roster entries receptions. Smack provides a mechanism that you can
use to execute custom logic when roster entries are received.

**Usage**

  1. Create a class that implements the _**RosterExchangeListener**_ interface.
  2. Implement the method **entriesReceived(String, Iterator)** that will be called when new entries are received with custom logic.
  3. Add the listener to the _RosterExchangeManager_ that works on the desired _XMPPConnection_.

**Example**

In this example we can see how user1 sends a roster entry to user2 and user2
adds the received entries to his roster.

```
// Connect to the server and log in the users
XMPPConnection conn1 = …
XMPPConnection conn2 = …

final Roster user2_roster = Roster.getInstanceFor(conn2);

// Create a RosterExchangeManager that will help user2 to listen and accept
the entries received
RosterExchangeManager rosterExchangeManager2 = new RosterExchangeManager(conn2);
// Create a RosterExchangeListener that will iterate over the received roster entries
RosterExchangeListener rosterExchangeListener = new RosterExchangeListener() {
public void entriesReceived(String from, Iterator remoteRosterEntries) {
while (remoteRosterEntries.hasNext()) {
try {
// Get the received entry
RemoteRosterEntry remoteRosterEntry = (RemoteRosterEntry) remoteRosterEntries.next();
// Display the remote entry on the console
System.out.println(remoteRosterEntry);
// Add the entry to the user2's roster
user2_roster.createEntry(
remoteRosterEntry.getUser(),
remoteRosterEntry.getName(),
remoteRosterEntry.getGroupArrayNames());
}
catch (XMPPException e) {
e.printStackTrace();
}
}
}
};
// Add the RosterExchangeListener to the RosterExchangeManager that user2 is using
rosterExchangeManager2.addRosterListener(rosterExchangeListener);

// Create a RosterExchangeManager that will help user1 to send his roster
RosterExchangeManager rosterExchangeManager1 = new RosterExchangeManager(conn1);
// Send user1's roster to user2
rosterExchangeManager1.send(Roster.getInstanceFor(conn1), user2);
```
