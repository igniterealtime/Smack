Processing Incoming Stanzas
===========================

[Back](index.md)

Smack provides a flexible framework for processing incoming stanzas using two
constructs:

  * `org.jivesoftware.smack.StanzaCollector` -- a class that lets you synchronously wait for new stanzas.
  * `org.jivesoftware.smack.StanzaListener` -- an interface for asynchronously notifying you of incoming stanzas.  A stanza listener is used for event style programming, while a stanza collector has a result queue of stanzas that you can do polling and blocking operations on. So, a stanza listener is useful when you want to take some action whenever a stanza happens to come in, while a stanza collector is useful when you want to wait for a specific stanza to arrive. Stanza collectors and listeners can be created using an `XMPPConnection` instance.

The `org.jivesoftware.smack.filter.StanzaFilter` interface determines which
specific stanzas will be delivered to a `StanzaCollector` or `StanzaListener`.
Many pre-defined filters can be found in the `org.jivesoftware.smack.filter`
package.

The following code snippet demonstrates registering both a stanza collector
and a stanza listener:

```
// Create a stanza filter to listen for new messages from a particular
// user. We use an AndFilter to combine two other filters._
StanzaFilter filter = new AndFilter(StanzaTypeFilter.MESSAGE, 
	FromMatchesFilter.create(JidCreate.entityBareFrom("mary@jivesoftware.com")));
// Assume we've created an XMPPConnection named "connection".

// First, register a stanza collector using the filter we created.
StanzaCollector myCollector = connection.createStanzaCollector(filter);
// Normally, you'd do something with the collector, like wait for new packets.

// Next, create a stanza listener. We use an anonymous inner class for brevity.
StanzaListener myListener = new StanzaListener() {
		public void processStanza(Stanza stanza) {
			// Do something with the incoming stanza here._
		}
	};
// Register the listener._
connection.addAsyncStanzaListener(myListener, filter);
// or for a synchronous stanza listener use
connection.addSyncStanzaListener(myListener, filter);
```

Standard Stanza Filters
-----------------------

A rich set of stanza filters are included with Smack, or you can create your
own filters by coding to the `StanzaFilter` interface. The default set of
filters includes:

  * `StanzaTypeFilter` -- filters for stanzas that are a particular Class type.
  * `StanzaIdFilter` -- filters for stanzas with a particular packet ID.
  * `ThreadFilter` -- filters for message stanzas with a particular thread ID.
  * `ToMatchesFilter` -- filters for stanzas that are sent to a particular address.
  * `FromMatchesFilter` -- filters for stanzas that are sent from a particular address.
  * `StanzaExtensionFilter` -- filters for stanzas that have a particular stanza extension.
  * `AndFilter` -- implements the logical AND operation over two filters.
  * `OrFilter` -- implements the logical OR operation over two filters.
  * `NotFilter` -- implements the logical NOT operation on a filter.

Copyright (C) Jive Software 2002-2008
