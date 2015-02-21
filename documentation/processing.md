Processing Incoming Packets
===========================

[Back](index.html)

Smack provides a flexible framework for processing incoming packets using two
constructs:

  * `org.jivesoftware.smack.PacketCollector` -- a class that lets you synchronously wait for new packets.
  * `org.jivesoftware.smack.PacketListener` -- an interface for asynchronously notifying you of incoming packets.  A packet listener is used for event style programming, while a packet collector has a result queue of packets that you can do polling and blocking operations on. So, a packet listener is useful when you want to take some action whenever a packet happens to come in, while a packet collector is useful when you want to wait for a specific packet to arrive. Packet collectors and listeners can be created using an `XMPPConnection` instance.

The `org.jivesoftware.smack.filter.PacketFilter` interface determines which
specific packets will be delivered to a `PacketCollector` or `PacketListener`.
Many pre-defined filters can be found in the `org.jivesoftware.smack.filter`
package.

The following code snippet demonstrates registering both a packet collector
and a packet listener:

```
// Create a packet filter to listen for new messages from a particular
// user. We use an AndFilter to combine two other filters._
PacketFilter filter = new AndFilter(new PacketTypeFilter(Message.class),
		new FromContainsFilter("mary@jivesoftware.com"));
// Assume we've created an XMPPConnection name "connection".

// First, register a packet collector using the filter we created.
PacketCollector myCollector = connection.createPacketCollector(filter);
// Normally, you'd do something with the collector, like wait for new packets.

// Next, create a packet listener. We use an anonymous inner class for brevity.
PacketListener myListener = new PacketListener() {
		**public** **void** processPacket(Packet packet) {
			// Do something with the incoming packet here._
		}
	};
// Register the listener._
connection.addPacketListener(myListener, filter);
```

Standard Packet Filters
-----------------------

A rich set of packet filters are included with Smack, or you can create your
own filters by coding to the `PacketFilter` interface. The default set of
filters includes:

  * `PacketTypeFilter` -- filters for packets that are a particular Class type.
  * `StanzaIdFilter` -- filters for packets with a particular packet ID.
  * `ThreadFilter` -- filters for message packets with a particular thread ID.
  * `ToContainsFilter` -- filters for packets that are sent to a particular address.
  * `FromContainsFilter` -- filters for packets that are sent to a particular address.
  * `PacketExtensionFilter` -- filters for packets that have a particular packet extension.
  * `AndFilter` -- implements the logical AND operation over two filters.
  * `OrFilter` -- implements the logical OR operation over two filters.
  * `NotFilter` -- implements the logical NOT operation on a filter.

Copyright (C) Jive Software 2002-2008
