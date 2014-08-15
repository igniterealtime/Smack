Privacy Lists
============

[XEP-0016: Privacy Lists](http://xmpp.org/extensions/xep-0016.html)

What is?
--------

`Privacy` is a method for users to block communications from particular other
users. In XMPP this is done by managing one's privacy lists.

Server-side privacy lists enable successful completion of the following use
cases:

  * Retrieving one's privacy lists.
  * Adding, removing, and editing one's privacy lists.
  * Setting, changing, or declining active lists.
  * Setting, changing, or declining the default list (i.e., the list that is active by default).
  * Allowing or blocking messages based on JID, group, or subscription type (or globally).
  * Allowing or blocking inbound presence notifications based on JID, group, or subscription type (or globally).
  * Allowing or blocking outbound presence notifications based on JID, group, or subscription type (or globally).
  * Allowing or blocking IQ stanzas based on JID, group, or subscription type (or globally).
  * Allowing or blocking all communications based on JID, group, or subscription type (or globally).

How can I use it?
-----------------

The API implementation releases three main public classes:

  * `PrivacyListManager`: this is the main API class to retrieve and handle server privacy lists.
  * `PrivacyList`: witch represents one privacy list, with a name, a set of privacy items. For example, the list with visible or invisible.
  * `PrivacyItem`: block or allow one aspect of privacy. For example, to allow my friend to see my presence.

1. Right from the start, a client MAY **get his/her privacy list** that is stored in the server:

```
// Create a privacy manager for the current connection._
PrivacyListManager privacyManager = PrivacyListManager.getInstanceFor(myConnection);
// Retrieve server privacy lists_
PrivacyList[] lists = privacyManager.getPrivacyLists();
```

Now the client is able to show every `PrivacyItem` of the server and also for
every list if it is active, default or none of them. The client is a listener
of privacy changes.



2. In order to **add a new list in the server**, the client MAY implement something like:

```
// Set the name of the list_
String listName = "newList";

// Create the list of PrivacyItem that will allow or deny some privacy aspect_
String user = "tybalt@example.com";
String groupName = "enemies";
ArrayList privacyItems = new ArrayList();

PrivacyItem item = new PrivacyItem(PrivacyItem.Type.jid, user, true, 1);
privacyItems.add(item);

item = new PrivacyItem(PrivacyItem.Type.subscription, PrivacyItem.SUBSCRIPTION_BOTH, true, 2);
privacyItems.add(item);

item = new PrivacyItem(PrivacyItem.Type.group, groupName, false, 3);
item.setFilterMessage(true);
privacyItems.add(item);

// Get the privacy manager for the current connection._
PrivacyListManager privacyManager = PrivacyListManager.getInstanceFor(myConnection);
// Create the new list._
privacyManager.createPrivacyList(listName, privacyItems);
```

3. To **modify an existent list**, the client code MAY be like:

```
// Set the name of the list_
String listName = "existingList";
// Get the privacy manager for the current connection._
PrivacyListManager privacyManager = PrivacyListManager.getInstanceFor(myConnection);
// Sent the new list to the server._
privacyManager.updatePrivacyList(listName, items);
```

Notice `items` was defined at the example 2 and MUST contain all the elements
in the list (not the "delta").

4. In order to **delete an existing list**, the client MAY perform something like:

```
// Set the name of the list_
String listName = "existingList";
// Get the privacy manager for the current connection._
PrivacyListManager privacyManager = PrivacyListManager.getInstanceFor(myConnection);
// Remove the list._
privacyManager.deletePrivacyList(listName);
```

5. In order to **decline the use of an active list**, the client MAY perform something like:

```
// Get the privacy manager for the current connection._
PrivacyListManager privacyManager = PrivacyListManager.getInstanceFor(myConnection);
// Decline the use of the active list._
privacyManager.declineActiveList();
```

6. In order to **decline the use of a default list**, the client MAY perform something like:

```
// Get the privacy manager for the current connection._
PrivacyListManager privacyManager = PrivacyListManager.getInstanceFor(myConnection);
// Decline the use of the default list._
privacyManager.declineDefaultList();
```

Listening for Privacy Changes

In order to handle privacy changes, clients SHOULD listen manager's updates.
When a list is changed the manager notifies every added listener. Listeners
MUST implement the `PrivacyListListener` interface. Clients may need to react
when a privacy list is modified. The `PrivacyListManager` lets you add
listerners that will be notified when a list has been changed. Listeners
should implement the `PrivacyListListener` interface.

The most important notification is `updatedPrivacyList` that is performed when
a privacy list changes its privacy items.

The listener becomes notified after performing:

```
// Get the privacy manager for the current connection._
PrivacyListManager privacyManager = PrivacyListManager.getInstanceFor(myConnection);
// Add the listener (this) to get notified_
privacyManager.addListener(this);
```
Copyright (C) Jive Software 2002-2008
