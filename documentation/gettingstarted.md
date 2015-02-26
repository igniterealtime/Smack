Smack: Getting Started
======================

[Back](index.html)

This document will introduce you to the Smack API and provide an overview of
important classes and concepts.

JAR Files and Requirements
--------------------------

Smack is meant to be easily embedded into any existing Java application. The
library ships as several JAR files to provide more flexibility over which
features applications require:

  * `smack-core.jar` -- provides core XMPP functionality. All XMPP features that are part of the XMPP RFCs are included.
  * `smack-tcp.jar` -- support for XMPP over TCP. Includes XMPPTCPConnection class, which you usually want to use
  * `smack-extensions.jar` -- support for many of the extensions (XEPs) defined by the XMPP Standards Foundation, including multi-user chat, file transfer, user search, etc. The extensions are documented in the [extensions manual](extensions/index.html).
  * `smack-experimental.jar` -- support for experimental extensions (XEPs) defined by the XMPP Standards Foundation. The API and functionality of those extensions should be considered as unstable.
  * `smack-legacy.jar` -- support for legacy extensions (XEPs) defined by the XMPP Standards Foundation.
  * `smack-bosh.jar` -- support for BOSH (XEP-0124). This code should be considered as beta.
  * `smack-jingle.jar` -- support for Jingle. This code is old and currenlty unmaintained.
  * `smack-resolver-dnsjava.jar` -- support for resolving DNS SRV records with the help of dnsjava. Ideal for platforms that do not support the javax.naming API.
  * `smack-resolver-javax.jar` -- support for resolving DNS SRV records with the javax namespace API.
  * `smack-debug.jar` -- an enhanced GUI debugger for protocol traffic. It will automatically be used when found in the classpath and when [debugging](debugging.html) is enabled.

Configuration
-------------

Smack has an initialization process that involves 2 phases.

  * Initializing system properties - Initializing all the system properties accessible through the class **SmackConfiguration**. These properties are retrieve by the _getXXX_ methods on that class.
  * Initializing startup classes - Initializing any classes meant to be active at startup by instantiating the class, and then calling the _initialize_ method on that class if it extends **SmackInitializer**. If it does not extend this interface, then initialization will have to take place in a static block of code which is automatically executed when the class is loaded.

Initialization is accomplished via a configuration file. By default, Smack
will load the one embedded in the Smack jar at _org.jivesoftware.smack/smack-
config.xml_. This particular configuration contains a list of initializer
classes to load. All manager type classes that need to be initialized are
contained in this list of initializers.

Establishing a Connection
-------------------------

The `XMPPTCPConnection` class is used to create a connection to an XMPP
server. Below are code examples for making a connection:

```
// Create a connection to the jabber.org server.
AbstractXMPPConnection conn1 = **new** XMPPTCPConnection("username", "password" "jabber.org");
conn1.connect();

// Create a connection to the jabber.org server on a specific port.
XMPPTCPConnectionConfiguration config = XMPPTCPConnectionConfiguration.builder()
  .setUsernameAndPassword("username", "password")
  .setServiceName("jabber.org")
  .setHost("earl.jabber.org")
  .setPort("8222")
  .build();

AbstractXMPPConnection conn2 = **new** XMPPTCPConnection(config);
conn2.connect();
```

Note that maximum security will be used when connecting to the server by
default (and when possible), including use of TLS encryption. The
ConnectionConfiguration class provides advanced control over the connection
created, such as the ability to disable or require encryption. See
[XMPPConnection Management](connections.html) for full details.

Once you've created a connection, you should login with the
`XMPPConnection.login()` method. Once you've logged in, you can being
chatting with other users by creating new `Chat` or `GroupChat`
objects.

Working with the Roster
----------------------

The roster lets you keep track of the availability (presence) of other users.
Users can be organized into groups such as "Friends" and "Co-workers", and
then you discover whether each user is online or offline.

Retrieve the roster using the `Roster.getInstanceFor(XMPPConnection)` method. The roster
class allows you to find all the roster entries, the groups they belong to,
and the current presence status of each entry.

Reading and Writing Stanzas

Each message to the XMPP server from a client is called a packet and is sent
as XML. The `org.jivesoftware.smack.packet` package contains classes that
encapsulate the three different basic packet types allowed by XMPP (message,
presence, and IQ). Classes such as `Chat` and `GroupChat` provide higher-level
constructs that manage creating and sending packets automatically, but you can
also create and send packets directly. Below is a code example for changing
your presence to let people know you're unavailable and "out fishing":

```
// Create a new presence. Pass in false to indicate we're unavailable._
Presence presence = new Presence(Presence.Type.unavailable);
presence.setStatus("Gone fishing");
// Send the packet (assume we have an XMPPConnection instance called "con").
con.sendPacket(presence);
```

Smack provides two ways to read incoming packets: `PacketListener`, and
`PacketCollector`. Both use `StanzaFilter` instances to determine which
packets should be processed. A packet listener is used for event style
programming, while a packet collector has a result queue of packets that you
can do polling and blocking operations on. So, a packet listener is useful
when you want to take some action whenever a packet happens to come in, while
a packet collector is useful when you want to wait for a specific packet to
arrive. Packet collectors and listeners can be created using an Connection
instance.

Copyright (C) Jive Software 2002-2008
