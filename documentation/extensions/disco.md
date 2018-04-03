Service Discovery
=================

[Back](index.md)

The service discovery extension allows one to discover items and information about
XMPP entities. Follow these links to learn how to use this extension.

  * Manage XMPP entity features
  * Provide node information
  * Discover items associated with an XMPP entity
  * Discover information about an XMPP entity
  * Publish publicly available items
  
**XEP related:** [XEP-30](http://www.xmpp.org/extensions/xep-0030.html)

Manage XMPP entity features
---------------------------

**Description**

Any XMPP entity may receive a discovery request and must answer with its
associated items or information. Therefore, your Smack client may receive a
discovery request that must respond to (i.e., if your client supports XHTML-
IM). This extension automatically responds to a discovery request with the
information that you previously configured.

**Usage**

In order to configure the supported features by your client you should first
obtain the ServiceDiscoveryManager associated with your XMPPConnection. To get
your ServiceDiscoveryManager send **getInstanceFor(connection)** to the class
_**ServiceDiscoveryManager**_ where connection is your XMPPConnection.

Once you have your ServiceDiscoveryManager you will be able to manage the
supported features. To register a new feature send **addFeature(feature)** to
your _**ServiceDiscoveryManager**_ where feature is a String that represents
the supported feature. To remove a supported feature send
**removeFeature(feature)** to your _**ServiceDiscoveryManager**_ where feature
is a String that represents the feature to remove.

**Examples**

In this example we can see how to add and remove supported features:

```
// Obtain the ServiceDiscoveryManager associated with my XMPP connection
ServiceDiscoveryManager discoManager = ServiceDiscoveryManager.getInstanceFor(connection);
// Register that a new feature is supported by this XMPP entity
discoManager.addFeature(namespace1);
// Remove the specified feature from the supported features
discoManager.removeFeature(namespace2);
```

Provide node information
------------------------

**Description**

Your XMPP entity may receive a discovery request for items non-addressable as
a JID such as the MUC rooms where you are joined. In order to answer the
correct information it is necessary to configure the information providers
associated to the items/nodes within the Smack client.

**Usage**

In order to configure the associated nodes within the Smack client you will
need to create a NodeInformationProvider and register it with the
_**ServiceDiscoveryManager**_. To get your ServiceDiscoveryManager send
**getInstanceFor(connection)** to the class _**ServiceDiscoveryManager**_
where connection is your XMPPConnection.

Once you have your ServiceDiscoveryManager you will be able to register
information providers for the XMPP entity's nodes. To register a new node
information provider send **setNodeInformationProvider(String node,
NodeInformationProvider listener)** to your _**ServiceDiscoveryManager**_
where node is the item non-addressable as a JID and listener is the
_**NodeInformationProvider**_ to register. To unregister a
_**NodeInformationProvider**_ send **removeNodeInformationProvider(String
node)** to your _**ServiceDiscoveryManager**_ where node is the item non-
addressable as a JID whose information provider we want to unregister.

**Examples**

In this example we can see how to register a NodeInformationProvider with a
ServiceDiscoveryManager that will provide information concerning a node named
"http://jabber.org/protocol/muc#rooms":

```
// Set the NodeInformationProvider that will provide information about the
// joined rooms whenever a disco request is received
ServiceDiscoveryManager.getInstanceFor(connection).setNodeInformationProvider(
	"http://jabber.org/protocol/muc#rooms",
	new NodeInformationProvider() {
		public List<DiscoverItems.Item> getNodeItems() {
			List<DiscoverItems.Item> answer = new ArrayList<>();
			Iterator rooms = MultiUserChat.getJoinedRooms(connection);
			while (rooms.hasNext()) {
				answer.add(new DiscoverItems.Item((String)rooms.next()));
			}
			return answer;
		}

		public List<String> getNodeFeatures() {...}
		public List<DiscoverInfo.Identity> getNodeIdentities() {...}
		public List<ExtensionElement> getNodePacketExtensions() {...}
});
```

Discover items associated with an XMPP entity
---------------------------------------------

**Description**

In order to obtain information about a specific item you have to first
discover the items available in an XMPP entity.

**Usage**

Once you have your ServiceDiscoveryManager you will be able to discover items
associated with an XMPP entity. To discover the items of a given XMPP entity
send **discoverItems(entityID)** to your _**ServiceDiscoveryManager**_ where
entityID is the ID of the entity. The message **discoverItems(entityID)** will
answer an instance of _**DiscoverItems**_ that contains the discovered items.

**Examples**

In this example we can see how to discover the items associated with an online
catalog service:

```
// Obtain the ServiceDiscoveryManager associated with my XMPPConnection
ServiceDiscoveryManager discoManager = ServiceDiscoveryManager.getInstanceFor(connection);
// Get the items of a given XMPP entity
// This example gets the items associated with online catalog service
DiscoverItems discoItems = discoManager.discoverItems("plays.shakespeare.lit");
// Get the discovered items of the queried XMPP entity
Iterator it = discoItems.getItems().iterator();
// Display the items of the remote XMPP entity
while (it.hasNext()) {
	DiscoverItems.Item item = (DiscoverItems.Item) it.next();
	System.out.println(item.getEntityID());
	System.out.println(item.getNode());
	System.out.println(item.getName());
}
```

Discover information about an XMPP entity
-----------------------------------------

**Description**

Once you have discovered the entity ID and name of an item, you may want to
find out more about the item. The information desired generally is of two
kinds: 1) The item's identity and 2) The features offered by the item.

This information helps you determine what actions are possible with regard to
this item (registration, search, join, etc.) as well as specific feature types
of interest, if any (e.g., for the purpose of feature negotiation).

**Usage**

Once you have your ServiceDiscoveryManager you will be able to discover
information associated with an XMPP entity. To discover the information of a
given XMPP entity send **discoverInfo(entityID)** to your
_**ServiceDiscoveryManager**_ where entityID is the ID of the entity. The
message **discoverInfo(entityID)** will answer an instance of
_**DiscoverInfo**_ that contains the discovered information.

**Examples**

In this example we can see how to discover the information of a conference
room:

```
// Obtain the ServiceDiscoveryManager associated with my XMPPConnection
ServiceDiscoveryManager discoManager = ServiceDiscoveryManager.getInstanceFor(connection);
// Get the information of a given XMPP entity
// This example gets the information of a conference room
DiscoverInfo discoInfo = discoManager.discoverInfo("balconyscene@plays.shakespeare.lit");
// Get the discovered identities of the remote XMPP entity
Iterator it = discoInfo.getIdentities().iterator();
// Display the identities of the remote XMPP entity
while (it.hasNext()) {
	DiscoverInfo.Identity identity = (DiscoverInfo.Identity) it.next();
	System.out.println(identity.getName());
	System.out.println(identity.getType());
	System.out.println(identity.getCategory());
}
// Check if room is password protected
discoInfo.containsFeature("muc_passwordprotected");
```

Publish publicly available items
--------------------------------

**Description**

Publish your entity items to some kind of persistent storage. This enables
other entities to query that entity using the disco#items namespace and
receive a result even when the entity being queried is not online (or
available).

**Usage**

Once you have your ServiceDiscoveryManager you will be able to publish items
to some kind of persistent storage. To publish the items of a given XMPP
entity you have to first create an instance of _**DiscoverItems**_ and
configure it with the items to publish. Then you will have to send
**publishItems(Jid entityID, DiscoverItems discoverItems)** to your
_**ServiceDiscoveryManager**_ where entityID is the address of the XMPP entity
that will persist the items and discoverItems contains the items to publish.

**Examples**

In this example we can see how to publish new items:

```
// Obtain the ServiceDiscoveryManager associated with my XMPPConnection
ServiceDiscoveryManager discoManager = ServiceDiscoveryManager.getInstanceFor(connection);

// Create a DiscoverItems with the items to publish
DiscoverItems itemsToPublish = new DiscoverItems();
Jid jid = JidCreate.from("pubsub.shakespeare.lit");
DiscoverItems.Item itemToPublish = new DiscoverItems.Item(jid);
itemToPublish.setName("Avatar");
itemToPublish.setNode("romeo/avatar");
itemToPublish.setAction(DiscoverItems.Item.UPDATE_ACTION);
itemsToPublish.addItem(itemToPublish);

// Publish the new items by sending them to the server
Jid jid2 = JidCreate.from("host");
discoManager.publishItems(jid2, itemsToPublish);
```
