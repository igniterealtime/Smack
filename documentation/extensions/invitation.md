Group Chat Invitations
======================

The group chat invitation extension is used to invite other users to a
group chat room.

  * Inviting Other Users
  * Listen for Invitations

**XEP related:** N/A -- this protocol is outdated now that the Multi-User Chat (MUC) XEP is available ([XEP-45](http://www.xmpp.org/extensions/xep-0045.html)). However, most existing clients still use this older protocol. Once MUC support becomes more widespread, this API may be deprecated. 

Inviting Other Users
--------------------

To use the GroupChatInvitation packet extension to invite another user to a
group chat room, address a new message to the user and set the room name
appropriately, as in the following code example:

```
Message message = new Message("user@chat.example.com");
message.setBody("Join me for a group chat!");
message.addExtension(new GroupChatInvitation("room@chat.example.com"));
con.sendStanza(message);
```

The XML generated for the invitation portion of the code above would be:

```
<x xmlns="jabber:x:conference" jid="room@chat.example.com"/>
```

Listening for Invitations
-------------------------

To listen for group chat invitations, use a StanzaExtensionFilter for the `x`
element name and `jabber:x:conference` namespace, as in the following code
example:

```
StanzaFilter filter = new StanzaExtensionFilter("x", "jabber:x:conference");
// Create a packet collector or packet listeners using the filter...
```
