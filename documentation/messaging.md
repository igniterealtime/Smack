Messaging using Chats
=====================

[Back](index.md)

Sending messages back and forth is at the core of instant messaging. Although
individual messages can be sent and received as packets, it's generally easier
to treat the string of messages as a chat using the
`org.jivesoftware.smack.chat2.Chat` class.

Chat
----

A chat creates a new thread of messages (using a thread ID) between two users.
The following code snippet demonstrates how to create a new Chat with a user
and then send them a text message:

```
// Assume we've created an XMPPConnection name "connection"._
ChatManager chatManager = ChatManager.getInstanceFor(connection);
chatManager.addListener(new IncomingChatMessageListener() {
  @Override
  void newIncomingMessage(EntityBareJid from, Message message, Chat chat) {
    System.out.println("New message from " + from + ": " + message.getBody());
  }
});
EntityBareJid jid = JidCreate.entityBareFrom("jsmith@jivesoftware.com");
Chat chat = chatManager.chatWith(jid);
chat.sendMessage("Howdy!");
}
```

The `Chat.sendMessage(String)` method is a convenience method that creates a
Message object, sets the body using the String parameter, then sends the
message. In the case that you wish to set additional values on a Message
before sending it, use
`Chat.send(Message)` method, as in the following code snippet:

```
Message newMessage = new Message();
newMessage.setBody("Howdy!");
// Additional modifications to the message Stanza.
JivePropertiesManager.addProperty(newMessage, "favoriteColor", "red");
chat.send(newMessage);
```

You'll also notice in the example above that we specified an IncomingChatMessageListener.
The listener is notified any time a new chat message arrives.
The following code snippet uses the listener
as a parrot-bot -- it echoes back everything the other user types.

```
// Assume a IncomingChatMessageListener we've setup with a ChatManager
public void newIncomingMessage(EntityBareJid from, Message message, Chat chat) {
    // Send back the same text the other user sent us.
    chat.send(message.getBody());
}
```

Copyright (C) Jive Software 2002-2008
