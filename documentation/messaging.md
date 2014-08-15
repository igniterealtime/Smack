Messaging using Chats
=====================

[Back](index.html)

Sending messages back and forth is at the core of instant messaging. Although
individual messages can be sent and received as packets, it's generally easier
to treat the string of messages as a chat using the
`org.jivesoftware.smack.Chat` class.

Chat
----

A chat creates a new thread of messages (using a thread ID) between two users.
The following code snippet demonstrates how to create a new Chat with a user
and then send them a text message:

```
// Assume we've created a XMPPConnection name "connection"._
ChatManager chatmanager = connection.getChatManager();
Chat newChat = chatmanager.createChat("jsmith@jivesoftware.com", new MessageListener() {
	public void processMessage(Chat chat, Message message) {
		System.out.println("Received message: " + message);
	}
});

try {
	newChat.sendMessage("Howdy!");
}
catch (XMPPException e) {
	System.out.println("Error Delivering block");
}
```

The `Chat.sendMessage(String)` method is a convenience method that creates a
Message object, sets the body using the String parameter, then sends the
message. In the case that you wish to set additional values on a Message
before sending it, use the `Chat.createMessage()` and
`Chat.sendMessage(Message)` methods, as in the following code snippet:

```
Message newMessage = new Message();
newMessage.setBody("Howdy!");
message.setProperty("favoriteColor", "red");
newChat.sendMessage(newMessage);
```

You'll also notice in the example above that we specified a MessageListener
when creating a chat. The listener is notified any time a new message arrives
from the other user in the chat. The following code snippet uses the listener
as a parrot-bot -- it echoes back everything the other user types.

```
// Assume a MessageListener we've setup with a chat._

public void processMessage(Chat chat, Message message) {
		// Send back the same text the other user sent us._
		chat.sendMessage(message.getBody());
}
```

Incoming Chat
-------------

When chats are prompted by another user, the setup is slightly different since
you are receiving a chat message first. Instead of explicitly creating a chat
to send messages, you need to register to handle newly created Chat instances
when the ChatManager creates them.  The ChatManager will already find a
matching chat (by thread id) and if none exists, then it will create a new one
that does match. To get this new chat, you have to register to be notified
when it happens. You can register a message listener to receive all future
messages as part of this handler.

```
_// Assume we've created a XMPPConnection name "connection"._
ChatManager chatmanager = connection.getChatManager().addChatListener(
	new ChatManagerListener() {
		@Override
		public void chatCreated(Chat chat, boolean createdLocally)
		{
			if (!createdLocally)
				chat.addMessageListener(new MyNewMessageListener());;
		}
	});
```

In addition to thread based chat messages, there are some clients that do not
send a thread id as part of the chat. To handle this scenario, Smack will
attempt match the incoming messages to the best fit existing chat, based on
the JID. It will attempt to find a chat with the same full JID, failing that,
it will try the base JID. If no existing chat to the user can found, then a
new one is created.

Copyright (C) Jive Software 2002-2008
