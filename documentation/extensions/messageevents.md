Message Events
==============

This extension is used to request and respond to events relating to the
delivery, display, and composition of messages. There are three stages in this
extension:

  1. Request for event notifications, 
  2. Receive the event notification requests and send event notifications, and 
  3. Receive the event notifications.

**XEP related:** [XEP-22](http://www.xmpp.org/extensions/xep-0022.html)

Requesting Event Notifications
------------------------------

**Description**

In order to receive event notifications for a given message you first have to
specify which events are you interested in. Each message that you send has to
request its own event notifications. Therefore, every message that you send as
part of a chat should request its own event notifications.

**Usage**

The class _MessageEventManager_ provides an easy way for requesting event
notifications. All you have to do is specify the message that requires the
event notifications and the events that you are interested in.

Use the static method _**MessageEventManager.addNotificationsRequests(Message
message, boolean offline, boolean delivered, boolean displayed, boolean
composing)**_ for requesting event notifications.

**Example**

Below you can find an example that logs in a user to the server, creates a
message, adds the requests for notifications and sends the message.

```
// Connect to the server and log in
conn1 = new XMPPConnection(host);
conn1.login(server_user1, pass1);
// Create a chat with user2
Chat chat1 = conn1.createChat(user2);
// Create a message to send
Message msg = chat1.createMessage();
msg.setSubject("Any subject you want");
msg.setBody("An interesting body comes here...");
// Add to the message all the notifications requests (offline,
// composing)
MessageEventManager.addNotificationsRequests(msg, **true**, **true**, **true**, **true**);
// Send the message that contains the notifications request
chat1.sendMessage(msg);
```

Reacting to Event Notification Requests
---------------------------------------

**Description**

You can receive notification requests for the following events: delivered,
displayed, composing and offline. You **must** listen for these requests and
react accordingly.

**Usage**

The general idea is to create a new _DefaultMessageEventRequestListener_ that
will listen to the event notifications requests and react with custom logic.
Then you will have to add the listener to the _MessageEventManager_ that works
on the desired _XMPPConnection_.

Note that _DefaultMessageEventRequestListener_ is a default implementation of
the _MessageEventRequestListener_ interface. The class
_DefaultMessageEventRequestListener_ automatically sends a delivered
notification to the sender of the message if the sender has requested to be
notified when the message is delivered. If you decide to create a new class
that implements the _MessageEventRequestListener_ interface, please remember
to send the delivered notification.

  * To create a new _MessageEventManager_ use the _**MessageEventManager(XMPPConnection)**_ constructor. 
  * To create an event notification requests listener create a subclass of _**DefaultMessageEventRequestListener**_ or create a class that implements the _**MessageEventRequestListener**_ interface. 
  * To add a listener to the messageEventManager use the MessageEventManager's message _**addMessageEventRequestListener(MessageEventRequestListener)**_.

**Example**

Below you can find an example that connects two users to the server. One user
will create a message, add the requests for notifications and will send the
message to the other user. The other user will add a
_DefaultMessageEventRequestListener_ to a _MessageEventManager_ that will
listen and react to the event notification requested by the other user.

```
// Connect to the server and log in the users
conn1 = new XMPPConnection(host);
conn1.login(server_user1, pass1);
conn2 = new XMPPConnection(host);
conn2.login(server_user2, pass2);
// User2 creates a MessageEventManager
MessageEventManager messageEventManager = new MessageEventManager(conn2);
// User2 adds the listener that will react to the event notifications requests
messageEventManager.addMessageEventRequestListener(new DefaultMessageEventRequestListener() {
public void deliveredNotificationRequested(
String from,
String packetID,
MessageEventManager messageEventManager) {
super.deliveredNotificationRequested(from, packetID, messageEventManager);
// DefaultMessageEventRequestListener automatically responds that the message was delivered when receives this r
System.out.println("Delivered Notification Requested (" + from + ", " + packetID + ")");
}
public void displayedNotificationRequested(String from, String packetID, MessageEventManager messageEventManager) {
super.displayedNotificationRequested(from, packetID,
// Send to the message's sender that the message was
messageEventManager.sendDisplayedNotification(from, packetID);
}
public void composingNotificationRequested(String from, String packetID, MessageEventManager messageEventManager) {
super.composingNotificationRequested(from, packetID, messageEventManager);
// Send to the message's sender that the message's receiver is composing a reply
messageEventManager.sendComposingNotification(from, packetID);
}
public void offlineNotificationRequested(String from, String packetID, MessageEventManager messageEventManager) {
super.offlineNotificationRequested(from, packetID, messageEventManager);
// The XMPP server should take care of this request. Do nothing.
System.out.println("Offline Notification Requested (" + from + ", " + packetID + ")");
}
});
// User1 creates a chat with user2
Chat chat1 = conn1.createChat(user2);
// User1 creates a message to send to user2
Message msg = chat1.createMessage();
msg.setSubject("Any subject you want");
msg.setBody("An interesting body comes here...");
// User1 adds to the message all the notifications requests (offline, delivered, displayed,
// composing)
MessageEventManager.addNotificationsRequests(msg, true, true, true, true);
// User1 sends the message that contains the notifications request
chat1.sendMessage(msg);
Thread.sleep(500);
// User2 sends to the message's sender that the message's receiver cancelled composing a reply
messageEventManager.sendCancelledNotification(user1, msg.getPacketID());
```

Reacting to Event Notifications
-------------------------------

**Description**

Once you have requested for event notifications you will start to receive
notifications of events. You can receive notifications of the following
events: delivered, displayed, composing, offline and cancelled. You will
probably want to react to some or all of these events.

**Usage**

The general idea is to create a new _MessageEventNotificationListener_ that
will listen to the event notifications and react with custom logic. Then you
will have to add the listener to the _MessageEventManager_ that works on the
desired _XMPPConnection_.

  * To create a new _MessageEventManager_ use the _**MessageEventManager(XMPPConnection)**_ constructor. 
  * To create an event notifications listener create a class that implements the _**MessageEventNotificationListener**_ interface. 
  * To add a listener to the messageEventManager use the MessageEventManager's message _**addMessageEventNotificationListener(MessageEventNotificationListener)**_.

**Example**

Below you can find an example that logs in a user to the server, adds a
_MessageEventNotificationListener_ to a _MessageEventManager_ that will listen
and react to the event notifications, creates a message, adds the requests for
notifications and sends the message.

```
// Connect to the server and log in
conn1 = new XMPPConnection(host);
conn1.login(server_user1, pass1);
// Create a MessageEventManager
MessageEventManager messageEventManager = new MessageEventManager(conn1);
// Add the listener that will react to the event notifications
messageEventManager.addMessageEventNotificationListener(new MessageEventNotificationListener() {
	public void deliveredNotification(String from, String packetID) {
	System.out.println("The message has been delivered (" + from + ", " + packetID + ")");
}
public void displayedNotification(String from, String packetID) {
	System.out.println("The message has been displayed (" + from + ", " + packetID + ")");
}
public void composingNotification(String from, String packetID) {
	System.out.println("The message's receiver is composing a reply (" + from + ", " + packetID + ")");
}
public void offlineNotification(String from, String packetID) {
	System.out.println("The message's receiver is offline (" + from + ", " + packetID + ")");
}
public void cancelledNotification(String from, String packetID) {
	System.out.println("The message's receiver cancelled composing a reply (" + from + ", " + packetID + ")");
}
});
// Create a chat with user2
Chat chat1 = conn1.createChat(user2);
// Create a message to send
Message msg = chat1.createMessage();
msg.setSubject("Any subject you want");
msg.setBody("An interesting body comes here...");
// Add to the message all the notifications requests (offline, delivered, displayed,
// composing)
MessageEventManager.addNotificationsRequests(msg, **true**, **true**, **true**, **true**);
// Send the message that contains the notifications request
chat1.sendMessage(msg);
```
