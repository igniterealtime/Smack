XHTML Messages
==============

Provides the ability to send and receive formatted messages using XHTML.

Follow these links to learn how to compose, send, receive and discover support
for XHTML messages:

  * Compose an XHTML Message
  * Send an XHTML Message
  * Receive an XHTML Message
  * Discover support for XHTML Messages

**XEP related:** [XEP-71](http://www.xmpp.org/extensions/xep-0071.html)

Compose an XHTML Message
------------------------

**Description**

The first step in order to send an XHTML message is to compose it. Smack
provides a special class that helps to build valid XHTML messages hiding any
low level complexity. For special situations, advanced users may decide not to
use the helper class and generate the XHTML by themselves. Even for these
situations Smack provides a well defined entry point in order to add the
generated XHTML content to a given message.

Note: not all clients are able to view XHTML formatted messages. Therefore,
it's recommended that you include a normal body in that message that is either
an unformatted version of the text or a note that XHTML support is required to
view the message contents.

**Usage**

Create an instance of _**XHTMLText**_ specifying the style and language of the
body. You can add several XHTML bodies to the message but each body should be
for a different language. Once you have an XHTMLText you can start to append
tags and text to it. In order to append tags there are several messages that
you can use. For each XHTML defined tag there is a message that you can send.
In order to add text you can send the message **#append(String
textToAppend)**.

After you have configured the XHTML text, the last step you have to do is to
add the XHTML text to the message you want to send. If you decided to create
the XHTML text by yourself, you will have to follow this last step too. In
order to add the XHTML text to the message send the message **#addBody(Message
message, String body)** to the _**XHTMLManager**_ class where _message_ is the
message that will receive the XHTML body and _body_ is the string to add as an
XHTML body to the message.**

**Example**

In this example we can see how to compose the following XHTML message:

```
<body>
	<p style='font-size:large'>Hey John, this is my new
		<span style='color:green'>green</span>
		<em>!!!!</em>
	</p>
</body>
```

```
// Create a message to send
Message msg = chat.createMessage();
msg.setSubject("Any subject you want");
msg.setBody("Hey John, this is my new green!!!!");

// Create an XHTMLText to send with the message
XHTMLText xhtmlText = new XHTMLText(null, null);
xhtmlText.appendOpenParagraphTag("font-size:large");
xhtmlText.append("Hey John, this is my new ");
xhtmlText.appendOpenSpanTag("color:green");
xhtmlText.append("green");
xhtmlText.appendCloseSpanTag();
xhtmlText.appendOpenEmTag();
xhtmlText.append("!!!!");
xhtmlText.appendCloseEmTag();
xhtmlText.appendCloseParagraphTag();
xhtmlText.appendCloseBodyTag();

// Add the XHTML text to the message
XHTMLManager.addBody(msg, xhtmlText);
```

Send an XHTML Message
---------------------

**Description**

After you have composed an XHTML message you will want to send it. Once you
have added the XHTML content to the message you want to send you are almost
done. The last step is to send the message as you do with any other message.

**Usage**

An XHTML message is like any regular message, therefore to send the message
you can follow the usual steps you do in order to send a message. For example,
to send a message as part of a chat just use the message **#sendMessage(Message)** of
_**Chat**_ or you can use the message **#sendStanza(Stanza)** of
_**XMPPConnection**_.

**Example**

In this example we can see how to send a message with XHTML content as part of
a chat.

```
// Create a message to send
Message msg = chat.createMessage();
// Obtain the XHTML text to send from somewhere
XHTMLText xhtmlBody = getXHTMLTextToSend();

// Add the XHTML text to the message
XHTMLManager.addBody(msg, xhtmlBody);

// Send the message that contains the XHTML
chat.sendMessage(msg);
```

Receive an XHTML Message
------------------------

**Description**

It is also possible to obtain the XHTML content from a received message.
Remember that the specification defines that a message may contain several
XHTML bodies where each body should be for a different language.

**Usage**

To get the XHTML bodies of a given message just send the message
**#getBodies(Message)** to the class _**XHTMLManager**_. The answer of this
message will be an _**List**_ with the different XHTML bodies of the
message or null if none.

**Example**

In this example we can see how to create a PacketListener that obtains the
XHTML bodies of any received message.

```
// Create a listener for the chat and display any XHTML content
IncomingChatMessageListener listener = new IncomingChatMessageListener() {
public void newIncomingMessage(EntityBareJid from, Message message, Chat chat) {
    // Obtain the XHTML bodies of the message
    List<CharSequence> bodies = XHTMLManager.getBodies(message);
    if (bodies == null) {
        return;

    // Display the bodies on the console
    for (CharSequence body : bodies) {
        System.out.println(body);
    }
}
};
chatManager.addListener(listener);
```

Discover support for XHTML Messages
-----------------------------------

**Description**

Before you start to send XHTML messages to a user you should discover if the
user supports XHTML messages. There are two ways to achieve the discovery,
explicitly and implicitly. Explicit is when you first try to discover if the
user supports XHTML before sending any XHTML message. Implicit is when you
send XHTML messages without first discovering if the conversation partner's
client supports XHTML and depenging on the answer (normal message or XHTML
message) you find out if the user supports XHTML messages or not. This section
explains how to explicitly discover for XHTML support.

**Usage**

In order to discover if a remote user supports XHTML messages send
**#isServiceEnabled(XMPPConnection connection, String userID)** to the class
_**XHTMLManager**_ where connection is the connection to use to perform the
service discovery and userID is the user to check (A fully qualified xmpp ID,
e.g. jdoe@example.com). This message will return true if the specified user
handles XHTML messages.

**Example**

In this example we can see how to discover if a remote user supports XHTML
Messages.

```
Message msg = chat.createMessage();
// Include a normal body in the message
msg.setBody(getTextToSend());
// Check if the other user supports XHTML messages
if (XHTMLManager.isServiceEnabled(connection, chat.getParticipant())) {
	// Obtain the XHTML text to send from somewhere
	String xhtmlBody = getXHTMLTextToSend();
	// Include an XHTML body in the message
	qHTMLManager.addBody(msg, xhtmlBody);
}

// Send the message
chat.sendMessage(msg);
```
