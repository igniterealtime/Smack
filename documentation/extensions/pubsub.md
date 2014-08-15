Pubsub
======

This section details the usage of an API designed for accessing an XMPP based
implementation of a [publish and
subscribe](http://en.wikipedia.org/wiki/Publish/subscribe) based messaging
system. It has functionality for creation, configuration of, subscription and
publishing to pubsub nodes.

  * Node creation and configuration
  * Publishing to a node
  * Receiving pubsub messages
  * Retrieving persisted pubsub messages
  * Discover pubsub information

**XEP related:** [XEP-0060](http://xmpp.org/extensions/xep-0060.html)

Node creation and configuration
-------------------------------

### Description

Allowed users may create and configure pubsub nodes. There are two types of
nodes that can be created, leaf nodes and collection nodes.

* Leaf Nodes - contains only messages
* Collection Nodes - contains only nodes (both Leaf and Collection are allowed), but no messages
The current version of this API only supports Leaf Nodes. There are many
configuration options available for nodes, but the two main options are
whether the node is **persistent** or not and whether it will deliver payload
or not.

### Usage

In order to create a node you will need to first create an instance of
_**PubSubManager**_. There are several options for node creation which range
from creating an instant node, default configuration, or a fully configured
node.

### Examples

Create an instant node:

```
// Create a pubsub manager using an existing XMPPConnection
PubSubManager mgr = new PubSubManager(con);

// Create the node
LeafNode leaf = mgr.createNode();
```

Create a node with default configuration and then configure it:

```
// Create a pubsub manager using an existing XMPPConnection
PubSubManager mgr = new PubSubManager(con);

// Create the node
LeafNode leaf = mgr.createNode("testNode");
ConfigureForm form = new ConfigureForm(FormType.submit);
form.setAccessModel(AccessModel.open);
form.setDeliverPayloads(false);
form.setNotifyRetract(true);
form.setPersistentItems(true);
form.setPublishModel(PublishModel.open);

leaf.sendConfigurationForm(form);
```

Create and configure a node:

```
// Create a pubsub manager using an existing XMPPConnection
PubSubManager mgr = new PubSubManager(con);

// Create the node
ConfigureForm form = new ConfigureForm(FormType.submit);
form.setAccessModel(AccessModel.open);
form.setDeliverPayloads(false);
form.setNotifyRetract(true);
form.setPersistentItems(true);
form.setPublishModel(PublishModel.open);
LeafNode leaf = mgr.createNode("testNode", form);
```

Publishing to a node
--------------------

**Description**

This section deals with the **publish** portion of pubsub. Usage of a node
typically involves either sending or receiving data, referred to as items.
Depending on the context of the nodes usage, the item being sent to it can
have different properties. It can contain application data known as payload,
or the publisher may choose to supply meaningful unique id's. Determination of
an items acceptable properties is defined by a combination of node
configuration and its purpose.

**Usage**

To publish to a node, you will have to either create or retrieve an existing
node and then create and send items to that node. The ability for any given
person to publish to the node will be dependent on its configuration.

**Examples**

In this example we publish an item to a node that does not take payload:

```
// Create a pubsub manager using an existing XMPPConnection
PubSubManager mgr = new PubSubManager(con);

// Get the node
LeafNode node = mgr.getNode("testNode");

// Publish an Item, let service set the id
node.send(new Item());

// Publish an Item with the specified id
node.send(new Item("123abc"));
```

In this example we publish an item to a node that does take payload:

```
// Create a pubsub manager using an existing XMPPConnection
PubSubManager mgr = new PubSubManager(con);

// Get the node
LeafNode node = mgr.getNode("testNode");

// Publish an Item with payload
node.send(new PayloadItem("test" + System.currentTimeMillis(),
new SimplePayload("book", "pubsub:test:book", "Two Towers")));
```

Receiving pubsub messages
-------------------------

**Description**

This section deals with the **subscribe** portion of pubsub. As mentioned in
the last section, usage of a node typically involves either sending or
receiving items. Subscribers are interested in being notified when items are
published to the pubsub node. These items may or may not have application
specific data (payload), as that is dependent on the context in which the node
is being used.

**Usage**

To get messages asynchronously when items are published to a node, you will
have to

* Get a node.
* Create and register a listener.
* Subscribe to the node.

Please note that you should register the listener before subscribing so that
all messages sent after subscribing are received. If done in the reverse
order, messages that are sent after subscribing but before registering a
listener may not be processed as expected.

**Examples**

In this example we can see how to create a listener and register it and then
subscribe for messages.

```
// Create a pubsub manager using an existing XMPPConnection
PubSubManager mgr = new PubSubManager(con);

// Get the node
LeafNode node = mgr.getNode("testNode");

node.addItemEventListener(new ItemEventCoordinator&ltItem;>());
node.subscribe(myJid);
```

Where the listener is defined like so:

```
class ItemEventCoordinator  implements ItemEventListener {
	@Override
	public void handlePublishedItems(ItemPublishEvent items) {
		System.out.println("Item count: " + System.out.println(items);
	}
}
```

In addition to receiving published items, there are notifications for several
other events that occur on a node as well.

* Deleting items or purging all items from a node
* Changing the node configuration

In this example we can see how to create a listener, register it and then
subscribe for item deletion messages.

```
// Create a pubsub manager using an existing XMPPConnection
PubSubManager mgr = new PubSubManager(con);

// Get the node
LeafNode node = mgr.getNode("testNode");

node.addItemDeleteListener(new ItemDeleteCoordinator&ltItem;>());
node.subscribe(myJid);
node.deleteItem("id_one");
```

Where the handler is defined like so:

```
class ItemDeleteCoordinator implements ItemDeleteListener {
	@Override
	public void handleDeletedItems(ItemDeleteEvent items) {
		System.out.println("Item count: " + items.getItemIds().size());
		System.out.println(items);
	}

	@Override
	public void handlePurge() {
		System.out.println("All items have been deleted from node");
	}
}
```

In this example we can see how to create a listener, register it and then
subscribe for node configuration messages.

```
// Create a pubsub manager using an existing XMPPConnection
PubSubManager mgr = new PubSubManager(con);

// Get the node
Node node = mgr.getNode("testNode");

node.addConfigurationListener(new NodeConfigCoordinator());
node.subscribe(myJid);

ConfigureForm form = new ConfigureForm(FormType.submit);
form.setAccessModel(AccessModel.open);
form.setDeliverPayloads(false);
form.setNotifyRetract(true);
form.setPersistentItems(true);
form.setPublishModel(PublishModel.open);

node.sendConfigurationForm(form);
```

Where the handler is defined like so:

```
class NodeConfigCoordinator implements NodeConfigListener {
	@Override
	public void handleNodeConfiguration(ConfigurationEvent config) {
		System.out.println("New configuration");
		System.out.println(config.getConfiguration());
	}
}
```

Retrieving persisted pubsub messages
------------------------------------

**Description**

When persistent nodes are used, the subscription and registration methods
described in the last section will not enable the retrieval of items that
already exist in the node. This section deals with the specific methods for
retrieving these items. There are several means of retrieving existing items.
You can retrieve all items at once, the last N items, or the items specified
by a collection of id's. Please note that the service may, according to the
pubsub specification, reply with a list of items that contains only the item
id's (no payload) to save on bandwidth. This will not occur when the id's are
specified since this is the means of guaranteeing retrieval of payload.

**Usage**

To synchronously retrieve existing items from a persistent node, you will have
to get an instance of a _**LeafNode**_ and call one of the retrieve methods.

**Examples**

In this example we can see how to retrieve the existing items from a node:

```
// Create a pubsub manager using an existing XMPPConnection
PubSubManager mgr = new PubSubManager(con);

// Get the node
LeafNode node = mgr.getNode("testNode");

Collection<? extends Item> items = node.getItems();
```

In this example we can see how to retrieve the last N existing items:

```
// Create a pubsub manager using an existing XMPPConnection
PubSubManager mgr = new PubSubManager(con);

// Get the node
LeafNode node = mgr.getNode("testNode");

List<? extends Item> items = node.getItems(100);
```

In this example we can see how to retrieve the specified existing items:

```
// Create a pubsub manager using an existing XMPPConnection
PubSubManager mgr = new PubSubManager(con);

// Get the node
LeafNode node = mgr.getNode("testNode");
Collection&ltString;> ids = new ArrayList&ltString;>(3);
ids.add("1");
ids.add("3");
ids.add("4");

List<? extends Item> items = node.getItems(ids);
```

Discover pubsub information
---------------------------

**Description**

A user may want to query a server or node for a variety of pubsub related
information.

**Usage**

To retrieve information, a user will simply use either the _**PubSubManager**_
or _**Node**_ classes depending on what type of information is required.

**Examples**

In this example we can see how to get pubsub capabilities:

```
// Create a pubsub manager using an existing XMPPConnection
PubSubManager mgr = new PubSubManager(con);

// Get the pubsub features that are supported
DiscoverInfo supportedFeatures = mgr.getSupportedFeatures();
```

In this example we can see how to get pubsub subscriptions for all nodes:

```
// Create a pubsub manager using an existing XMPPConnection
PubSubManager mgr = new PubSubManager(con);

// Get all the subscriptions in the pubsub service
List&ltSubscription;> subscriptions = mgr.getSubscriptions();
```

In this example we can see how to get all affiliations for the users bare JID
on the pubsub service:

```
// Create a pubsub manager using an existing XMPPConnection
PubSubManager mgr = new PubSubManager(con);

// Get the affiliations for the users bare JID
List&ltAffiliation;> affiliations = mgr.getAffiliations();
```

In this example we can see how to get information about the node:

```
// Create a pubsub manager using an existing XMPPConnection
PubSubManager mgr = new PubSubManager(con);
Node node = mgr.getNode("testNode");

// Get the node information
DiscoverInfo nodeInfo = node.discoverInfo();
```

In this example we can see how to discover the node items:

```
// Create a pubsub manager using an existing XMPPConnection
PubSubManager mgr = new PubSubManager(con);
Node node = mgr.getNode("testNode");

// Discover the node items
DiscoverItems nodeItems = node.discoverItems();
```

In this example we can see how to get node subscriptions:

```
// Create a pubsub manager using an existing XMPPConnection
PubSubManager mgr = new PubSubManager(con);
Node node = mgr.getNode("testNode");

// Discover the node subscriptions
List&ltSubscription;> subscriptions = node.getSubscriptions();
```
