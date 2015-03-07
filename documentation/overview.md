Smack Overview
==============

[Back](index.md)

Smack is a library for communicating with XMPP servers to perform real-time
communications, including instant messaging and group chat.

Smack Key Advantages
--------------------

  * Extremely simple to use, yet powerful API. Sending a text message to a user can be accomplished in only a few lines of code: 

    ```java
    AbstractXMPPConnection connection = new XMPPTCPConnection("mtucker", "password", "jabber.org");
    connection.connect().login();

    Chat chat = ChatManager.getInstanceFor(connection)
        .createChat("jsmith@jivesoftware.com", new MessageListener() {

        public void processMessage(Chat chat, Message message) {
            System.out.println("Received message: " + message);
        }
    });
    chat.sendMessage("Howdy!");
    ```

* Doesn't force you to code at the packet level, as other libraries do. Smack provides intelligent higher level constructs such as the `Chat` and `Roster` classes, which let you program more efficiently. 
  * Does not require that you're familiar with the XMPP XML format, or even that you're familiar with XML. 
  * Provides easy machine to machine communication. Smack lets you set any number of properties on each message, including properties that are Java objects. 
  * Open Source under the Apache License, which means you can incorporate Smack into your commercial or non-commercial applications. 

About XMPP
----------

XMPP (eXtensible Messaging and Presence Protocol) is an open protocol
standardized by the IETF and supported and extended by the XMPP Standards
Foundation (([http://www.xmpp.org](http://www.xmpp.org)).

How To Use This Documentation
-----------------------------

This documentation assumes that you're already familiar with the main features
of XMPP instant messaging. It's also highly recommended that you open the
Javadoc API guide and use that as a reference while reading through this
documentation.

Copyright (C) Jive Software 2002-2008
