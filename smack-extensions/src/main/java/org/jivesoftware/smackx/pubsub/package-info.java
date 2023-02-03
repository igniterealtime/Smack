/**
 *
 * Copyright 2015-2023 Florian Schmaus
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Smack's API for XEP-0060: Publish-Subscribe. XMPP based
 * <a href="https://en.wikipedia.org/wiki/Publish/subscribe">publish and subscribe</a> is based around nodes to which
 * items can be published. Subscribers of those nodes will be notified about new items.
 * <h2>Node creation and configuration</h2>
 * <h3>Description</h3>
 * <p>
 * Allowed users may create and configure pubsub nodes. There are two types of nodes that can be created, leaf nodes and
 * collection nodes.
 * </p>
 * <ul>
 * <li>Leaf Nodes - contains only messages</li>
 * <li>Collection Nodes - contains only nodes (both Leaf and Collection are allowed), but no messages The current
 * version of this API only supports Leaf Nodes. There are many configuration options available for nodes, but the two
 * main options are whether the node is **persistent** or not and whether it will deliver payload or not.</li>
 * </ul>
 * <h3>Usage</h3>
 * <p>
 * In order to create a node you will need to first create an instance of _**PubSubManager**_. There are several options
 * for node creation which range from creating an instant node, default configuration, or a fully configured node.
 * </p>
 * <h3>Examples</h3>
 * <p>
 * Create an instant node:
 * </p>
 *
 * <pre>{@code
 * // Create a pubsub manager using an existing XMPPConnection
 * PubSubManager mgr = new PubSubManager(con);
 *
 * // Create the node
 * LeafNode leaf = mgr.createNode();
 * }</pre>
 * <p>
 * Create a node with default configuration and then configure it:
 * </p>
 *
 * <pre>{@code
 * // Create a pubsub manager using an existing XMPPConnection
 * PubSubManager mgr = PubSubManager.getInstanceFor(con);
 *
 * // Create the node
 * LeafNode leaf = mgr.createNode("testNode");
 * ConfigureForm form = new ConfigureForm(FormType.submit);
 * form.setAccessModel(AccessModel.open);
 * form.setDeliverPayloads(false);
 * form.setNotifyRetract(true);
 * form.setPersistentItems(true);
 * form.setPublishModel(PublishModel.open);
 *
 * leaf.sendConfigurationForm(form);
 * }</pre>
 * <p>
 * Create and configure a node:
 * </p>
 *
 * <pre>{@code
 * // Create a pubsub manager using an existing XMPPConnection
 * PubSubManager mgr = PubSubManager.getInstanceFor(con);
 *
 * // Create the node
 * ConfigureForm form = new ConfigureForm(FormType.submit);
 * form.setAccessModel(AccessModel.open);
 * form.setDeliverPayloads(false);
 * form.setNotifyRetract(true);
 * form.setPersistentItems(true);
 * form.setPublishModel(PublishModel.open);
 * LeafNode leaf = mgr.createNode("testNode", form);
 * }</pre>
 *
 * <h2>Publishing to a node</h2>
 * <h3>Description</h3>
 * <p>
 * This section deals with the **publish** portion of pubsub. Usage of a node typically involves either sending or
 * receiving data, referred to as items. Depending on the context of the nodes usage, the item being sent to it can have
 * different properties. It can contain application data known as payload, or the publisher may choose to supply
 * meaningful unique id's. Determination of an items acceptable properties is defined by a combination of node
 * configuration and its purpose.
 * </p>
 * <h3>Usage</h3>
 * <p>
 * To publish to a node, you will have to either create or retrieve an existing node and then create and send items to
 * that node. The ability for any given person to publish to the node will be dependent on its configuration.
 * </p>
 * <h3>Examples</h3>
 * <p>
 * In this example we publish an item to a node that does not take payload:
 * </p>
 *
 * <pre>{@code
 * // Create a pubsub manager using an existing XMPPConnection
 * PubSubManager mgr = PubSubManager.getInstanceFor(con);
 *
 * // Get the node
 * LeafNode node = mgr.getNode("testNode");
 *
 * // Publish an Item, let service set the id
 * node.send(new Item());
 *
 * // Publish an Item with the specified id
 * node.send(new Item("123abc"));
 * }</pre>
 * <p>
 * In this example we publish an item to a node that does take payload:
 * </p>
 *
 * <pre>{@code
 * // Create a pubsub manager using an existing XMPPConnection
 * PubSubManager mgr = PubSubManager.getInstanceFor(con);
 *
 * // Get the node
 * LeafNode node = mgr.getNode("testNode");
 *
 * // Publish an Item with payload
 * node.send(new PayloadItem("test" + System.currentTimeMillis(),
 *                 new SimplePayload("book", "pubsub:test:book", "Two Towers")));
 * }</pre>
 *
 * <h2>Receiving pubsub messages</h2>
 * <h3>Description</h3>
 * <p>
 * This section deals with the **subscribe** portion of pubsub. As mentioned in the last section, usage of a node
 * typically involves either sending or receiving items. Subscribers are interested in being notified when items are
 * published to the pubsub node. These items may or may not have application specific data (payload), as that is
 * dependent on the context in which the node is being used.
 * </p>
 * <h3>Usage</h3>
 * <p>
 * To get messages asynchronously when items are published to a node, you will have to
 * </p>
 * <ul>
 * <li>Get a node.</li>
 * <li>Create and register a listener.</li>
 * <li>Subscribe to the node.</li>
 * </ul>
 * <p>
 * Please note that you should register the listener before subscribing so that all messages sent after subscribing are
 * received. If done in the reverse order, messages that are sent after subscribing but before registering a listener
 * may not be processed as expected.
 * </p>
 * <h3>Examples</h3>
 * <p>
 * In this example we can see how to create a listener and register it and then subscribe for messages.
 * </p>
 *
 * <pre>{@code
 * // Create a pubsub manager using an existing XMPPConnection
 * PubSubManager mgr = PubSubManager.getInstanceFor(con);
 *
 * // Get the node
 * LeafNode node = mgr.getNode("testNode");
 *
 * node.addItemEventListener(new ItemEventCoordinator<Item>());
 * node.subscribe(myJid);
 * }</pre>
 * <p>
 * Where the listener is defined like so:
 * </p>
 *
 * <pre><code>
 * class ItemEventCoordinator implements ItemEventListener {
 *     {@literal @}@Override
 *     public void handlePublishedItems(ItemPublishEvent items) {
 *         System.out.println("Item count: " + System.out.println(items));
 *     }
 * }
 * </code></pre>
 * <p>
 * In addition to receiving published items, there are notifications for several other events that occur on a node as
 * well.
 * </p>
 * <ul>
 * <li>Deleting items or purging all items from a node</li>
 * <li>Changing the node configuration</li>
 * </ul>
 * <p>
 * In this example we can see how to create a listener, register it and then subscribe for item deletion messages.
 * </p>
 *
 * <pre>{@code
 * // Create a pubsub manager using an existing XMPPConnection
 * PubSubManager mgr = PubSubManager.getInstanceFor(con);
 *
 * // Get the node
 * LeafNode node = mgr.getNode("testNode");
 *
 * node.addItemDeleteListener(new ItemDeleteCoordinator<Item>());
 * node.subscribe(myJid);
 * node.deleteItem("id_one");
 * }</pre>
 * <p>
 * Where the handler is defined like so:
 * </p>
 *
 * <pre><code>
 * class ItemDeleteCoordinator implements ItemDeleteListener {
 *     {@literal @}Override
 *     public void handleDeletedItems(ItemDeleteEvent items) {
 *         System.out.println("Item count: " + items.getItemIds().size());
 *         System.out.println(items);
 *     }
 *
 *     {@literal @}Override
 *     public void handlePurge() {
 *         System.out.println("All items have been deleted from node");
 *     }
 * }
 * </code></pre>
 * <p>
 * In this example we can see how to create a listener, register it and then subscribe for node configuration messages.
 * </p>
 *
 * <pre>{@code
 * // Create a pubsub manager using an existing XMPPConnection
 * PubSubManager mgr = PubSubManager.getInstanceFor(con);
 *
 * // Get the node
 * Node node = mgr.getNode("testNode");
 *
 * node.addConfigurationListener(new NodeConfigCoordinator());
 * node.subscribe(myJid);
 *
 * ConfigureForm form = new ConfigureForm(FormType.submit);
 * form.setAccessModel(AccessModel.open);
 * form.setDeliverPayloads(false);
 * form.setNotifyRetract(true);
 * form.setPersistentItems(true);
 * form.setPublishModel(PublishModel.open);
 *
 * node.sendConfigurationForm(form);
 * }</pre>
 * <p>
 * In this example we can see how to create a listener, register it and then subscribe for node configuration messages.
 * </p>
 *
 * <pre><code>
 * class NodeConfigCoordinator implements NodeConfigListener {
 *     {@literal @}Override
 *     public void handleNodeConfiguration(ConfigurationEvent config) {
 *         System.out.println("New configuration");
 *         System.out.println(config.getConfiguration());
 *     }
 * </code></pre>
 *
 * <h2>Retrieving persisted pubsub messages</h2>
 * <h3>Description</h3>
 * <p>
 * When persistent nodes are used, the subscription and registration methods described in the last section will not
 * enable the retrieval of items that already exist in the node. This section deals with the specific methods for
 * retrieving these items. There are several means of retrieving existing items. You can retrieve all items at once, the
 * last N items, or the items specified by a collection of id's. Please note that the service may, according to the
 * pubsub specification, reply with a list of items that contains only the item id's (no payload) to save on bandwidth.
 * This will not occur when the id's are specified since this is the means of guaranteeing retrieval of payload.
 * </p>
 * <h3>Usage</h3>
 * <p>
 * To synchronously retrieve existing items from a persistent node, you will have to get an instance of a _**LeafNode**_
 * and call one of the retrieve methods.
 * </p>
 * <h3>Examples</h3>
 * <p>
 * In this example we can see how to retrieve the existing items from a node:
 * </p>
 *
 * <pre>{@code
 * // Create a pubsub manager using an existing XMPPConnection
 * PubSubManager mgr = PubSubManager.getInstanceFor(con);
 *
 * // Get the node
 * LeafNode node = mgr.getNode("testNode");
 *
 * Collection<? extends Item> items = node.getItems();
 * }</pre>
 * <p>
 * In this example we can see how to retrieve the last N existing items:
 * </p>
 *
 * <pre>{@code
 * // Create a pubsub manager using an existing XMPPConnection
 * PubSubManager mgr = PubSubManager.getInstanceFor(con);
 *
 * // Get the node
 * LeafNode node = mgr.getNode("testNode");
 *
 * List<? extends Item> items = node.getItems(100);
 * }</pre>
 * <p>
 * In this example we can see how to retrieve the specified existing items:
 * </p>
 *
 * <pre>{@code
 * // Create a pubsub manager using an existing XMPPConnection
 * PubSubManager mgr = PubSubManager.getInstanceFor(con);
 *
 * // Get the node
 * LeafNode node = mgr.getNode("testNode");
 * Collection<String> ids = new ArrayList<String>(3);
 * ids.add("1");
 * ids.add("3");
 * ids.add("4");
 *
 * List<? extends Item> items = node.getItems(ids);
 * }</pre>
 *
 * <h2>Discover pubsub information</h2>
 * <h3>Description</h3>
 * <p>
 * A user may want to query a server or node for a variety of pubsub related information.
 * </p>
 * <h3>Usage</h3>
 * <p>
 * To retrieve information, a user will simply use either the _**PubSubManager**_ or _**Node**_ classes depending on
 * what type of information is required.
 * </p>
 * <h3>Examples</h3>
 * <p>
 * In this example we can see how to get pubsub capabilities:
 * </p>
 *
 * <pre>{@code
 * // Create a pubsub manager using an existing XMPPConnection
 * PubSubManager mgr = PubSubManager.getInstanceFor(con);
 *
 * // Get the pubsub features that are supported
 * DiscoverInfo supportedFeatures = mgr.getSupportedFeatures();
 * }</pre>
 * <p>
 * In this example we can see how to get pubsub subscriptions for all nodes:
 * </p>
 *
 * <pre>{@code
 * // Create a pubsub manager using an existing XMPPConnection
 * PubSubManager mgr = PubSubManager.getInstanceFor(con);
 *
 * // Get all the subscriptions in the pubsub service
 * List<Subscription> subscriptions = mgr.getSubscriptions();
 * }</pre>
 * <p>
 * In this example we can see how to get all affiliations for the users bare JID on the pubsub service:
 * </p>
 *
 * <pre>{@code
 * // Create a pubsub manager using an existing XMPPConnection
 * PubSubManager mgr = PubSubManager.getInstanceFor(con);
 *
 * // Get the affiliations for the users bare JID
 * List<Affiliation> affiliations = mgr.getAffiliations();
 * }</pre>
 * <p>
 * In this example we can see how to get information about the node:
 * </p>
 *
 * <pre>{@code
 * // Create a pubsub manager using an existing XMPPConnection
 * PubSubManager mgr = PubSubManager.getInstanceFor(con);
 * Node node = mgr.getNode("testNode");
 *
 * // Get the node information
 * DiscoverInfo nodeInfo = node.discoverInfo();
 * }</pre>
 * <p>
 * In this example we can see how to discover the node items:
 * </p>
 *
 * <pre>{@code
 * // Create a pubsub manager using an existing XMPPConnection
 * PubSubManager mgr = PubSubManager.getInstanceFor(con);
 * Node node = mgr.getNode("testNode");
 *
 * // Discover the node items
 * DiscoverItems nodeItems = node.discoverItems();
 * }</pre>
 * <p>
 * In this example we can see how to get node subscriptions:
 * </p>
 *
 * <pre>{@code
 * // Create a pubsub manager using an existing XMPPConnection
 * PubSubManager mgr = PubSubManager.getInstanceFor(con);
 * Node node = mgr.getNode("testNode");
 *
 * // Discover the node subscriptions
 * List<Subscription> subscriptions = node.getSubscriptions();
 * }</pre>
 *
 * @see <a href="https://xmpp.org/extensions/xep-0060.html">XEP-0060: Publish-Subscribe</a>
 */
package org.jivesoftware.smackx.pubsub;
