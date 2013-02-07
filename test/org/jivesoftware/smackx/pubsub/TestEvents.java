/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright 2003-2007 Jive Software.
 *
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
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
/*
 * Created on 2009-04-22
 */
package org.jivesoftware.smackx.pubsub;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.XMPPError.Type;
import org.jivesoftware.smack.test.SmackTestCase;
import org.jivesoftware.smackx.pubsub.listener.ItemDeleteListener;
import org.jivesoftware.smackx.pubsub.listener.ItemEventListener;
import org.jivesoftware.smackx.pubsub.listener.NodeConfigListener;

public class TestEvents extends SmackTestCase
{

	public TestEvents(String str)
	{
		super(str);
	}

	@Override
	protected int getMaxConnections()
	{
		return 2;
	}

	private String getService()
	{
		return "pubsub." + getServiceName();
	}

	public void testCreateAndGetNode() throws Exception
	{
		String nodeId = "MyTestNode";
		PubSubManager creatorMgr = new PubSubManager(getConnection(0), getService());
		
		LeafNode creatorNode = null;
		try
		{
			creatorNode = (LeafNode)creatorMgr.getNode(nodeId);
		}
		catch (XMPPException e)
		{
			if (e.getXMPPError().getType() == Type.CANCEL && e.getXMPPError().getCondition().equals("item-not-found"))
				creatorNode = creatorMgr.createNode(nodeId);
			else
				throw e;
		}
		
		PubSubManager subMgr = new PubSubManager(getConnection(1), getService());
		LeafNode subNode = (LeafNode)subMgr.getNode(nodeId);
		
		assertNotNull(subNode);
	}

	public void testConfigureAndNotify() throws Exception
	{
		// Setup event source
		String nodeId = "TestNode" + System.currentTimeMillis();
		PubSubManager creatorMgr = new PubSubManager(getConnection(0), getService());
		
		LeafNode creatorNode = getPubnode(creatorMgr, nodeId, false, true);

		BlockingQueue<NodeConfigCoordinator> queue = new ArrayBlockingQueue<NodeConfigCoordinator>(3);

		// Setup event receiver
		PubSubManager subMgr = new PubSubManager(getConnection(1), getService());
		LeafNode subNode = (LeafNode)subMgr.getNode(nodeId);

		NodeConfigListener sub1Handler = new NodeConfigCoordinator(queue, "sub1");
		subNode.subscribe(getConnection(1).getUser());
		subNode.addConfigurationListener(sub1Handler);
		
		ConfigureForm currentConfig = creatorNode.getNodeConfiguration(); 
		ConfigureForm form = new ConfigureForm(currentConfig.createAnswerForm());
		form.setPersistentItems(true);
		form.setDeliverPayloads(false);
		form.setNotifyConfig(true);
		creatorNode.sendConfigurationForm(form);

   		ConfigurationEvent event = queue.poll(5, TimeUnit.SECONDS).event;
   		assertEquals(nodeId, event.getNode());
   		assertNull(event.getConfiguration());
   		
		currentConfig = creatorNode.getNodeConfiguration(); 
		form = new ConfigureForm(currentConfig.createAnswerForm());
		form.setDeliverPayloads(true);
		creatorNode.sendConfigurationForm(form);

   		event = queue.poll(5, TimeUnit.SECONDS).event;
   		assertEquals(nodeId, event.getNode());
   		assertNotNull(event.getConfiguration());
   		assertTrue(event.getConfiguration().isPersistItems());
   		assertTrue(event.getConfiguration().isDeliverPayloads());
	}

	public void testSendAndReceiveNoPayload() throws Exception
	{
		// Setup event source
		String nodeId = "TestNode" + System.currentTimeMillis();
		PubSubManager creatorMgr = new PubSubManager(getConnection(0), getService());
		LeafNode creatorNode = getPubnode(creatorMgr, nodeId, true, false);

		BlockingQueue<ItemEventCoordinator<Item>> queue = new ArrayBlockingQueue<ItemEventCoordinator<Item>>(3);
		ItemEventCoordinator<Item> creatorHandler = new ItemEventCoordinator<Item>(queue, "creator");
		creatorNode.addItemEventListener(creatorHandler);
		
		// Setup event receiver
		PubSubManager subMgr = new PubSubManager(getConnection(1), getService());
		LeafNode subNode = (LeafNode)subMgr.getNode(nodeId);

		ItemEventCoordinator<Item> sub1Handler = new ItemEventCoordinator<Item>(queue, "sub1");
		subNode.addItemEventListener(sub1Handler);
		Subscription sub1 = subNode.subscribe(getConnection(1).getUser());
		
        // Send event
        String itemId = String.valueOf(System.currentTimeMillis());
        creatorNode.send(new Item(itemId));
        
        for(int i=0; i<2; i++)
        {
    		ItemEventCoordinator<Item> coord = queue.take();
        	assertEquals(1, coord.events.getItems().size());
        	assertEquals(itemId, coord.events.getItems().iterator().next().getId());
        }
	}
	
	public void testPublishAndReceiveNoPayload() throws Exception
	{
		// Setup event source
		String nodeId = "TestNode" + System.currentTimeMillis();
		PubSubManager creatorMgr = new PubSubManager(getConnection(0), getService());
		LeafNode creatorNode = getPubnode(creatorMgr, nodeId, true, false);

		BlockingQueue<ItemEventCoordinator<Item>> queue = new ArrayBlockingQueue<ItemEventCoordinator<Item>>(3);
		
		// Setup event receiver
		PubSubManager subMgr = new PubSubManager(getConnection(1), getService());
		LeafNode subNode = (LeafNode)subMgr.getNode(nodeId);

		ItemEventCoordinator<Item> sub1Handler = new ItemEventCoordinator<Item>(queue, "sub1");
		subNode.addItemEventListener(sub1Handler);
		Subscription sub1 = subNode.subscribe(getConnection(1).getUser());
		
        // Send event
        String itemId = String.valueOf(System.currentTimeMillis());
        creatorNode.publish(new Item(itemId));
        
   		ItemEventCoordinator<Item> coord = queue.take();
       	assertEquals(1, coord.events.getItems().size());
       	assertEquals(itemId, coord.events.getItems().get(0).getId());
	}

	public void testSendAndReceiveSimplePayload() throws Exception
	{
		// Setup event source
		String nodeId = "TestNode" + System.currentTimeMillis();
		PubSubManager creatorMgr = new PubSubManager(getConnection(0), getService());
		LeafNode creatorNode = getPubnode(creatorMgr, nodeId, true, true);

		BlockingQueue<ItemEventCoordinator<PayloadItem<SimplePayload>>> queue = new ArrayBlockingQueue<ItemEventCoordinator<PayloadItem<SimplePayload>>>(3);
		
		// Setup event receiver
		PubSubManager subMgr = new PubSubManager(getConnection(1), getService());
		LeafNode subNode = (LeafNode)subMgr.getNode(nodeId);

		ItemEventCoordinator<PayloadItem<SimplePayload>> sub1Handler = new ItemEventCoordinator<PayloadItem<SimplePayload>>(queue, "sub1");
		subNode.addItemEventListener(sub1Handler);
		Subscription sub1 = subNode.subscribe(getConnection(1).getUser());
		
        // Send event
        String itemId = String.valueOf(System.currentTimeMillis());
        String payloadString = "<book xmlns=\"pubsub:test:book\"><author>Sir Arthur Conan Doyle</author></book>";
        creatorNode.send(new PayloadItem<SimplePayload>(itemId, new SimplePayload("book", "pubsub:test:book", payloadString)));
        
   		ItemEventCoordinator<PayloadItem<SimplePayload>> coord = queue.take();
       	assertEquals(1, coord.events.getItems().size());
       	PayloadItem<SimplePayload> item = coord.events.getItems().get(0);
       	assertEquals(itemId, item.getId());
       	assertTrue(item.getPayload() instanceof SimplePayload);
       	assertEquals(payloadString, item.getPayload().toXML());
       	assertEquals("book", item.getPayload().getElementName());
	}

	/*
	 * For this test, the following extension needs to be added to the meta-inf/smack.providers file
	 * 
	 * 	 <extensionProvider>
	 *     	<elementName>car</elementName>
	 *      <namespace>pubsub:test:vehicle</namespace>
	 *      <className>org.jivesoftware.smackx.pubsub.CarExtensionProvider</className>
	 *   </extensionProvider>
	 */
	/*
	public void testSendAndReceiveCarPayload() throws Exception
	{
		// Setup event source
		String nodeId = "TestNode" + System.currentTimeMillis();
		PubSubManager creatorMgr = new PubSubManager(getConnection(0), getService());
		LeafNode creatorNode = getPubnode(creatorMgr, nodeId, true, true);

		BlockingQueue<ItemEventCoordinator<PayloadItem<CarExtension>>> queue = new ArrayBlockingQueue<ItemEventCoordinator<PayloadItem<CarExtension>>>(3);
		
		// Setup event receiver
		PubSubManager subMgr = new PubSubManager(getConnection(1), getService());
		Node subNode = subMgr.getNode(nodeId);

		ItemEventCoordinator<PayloadItem<CarExtension>> sub1Handler = new ItemEventCoordinator<PayloadItem<CarExtension>>(queue, "sub1");
		subNode.addItemEventListener(sub1Handler);
		Subscription sub1 = subNode.subscribe(getConnection(1).getUser());
		
        // Send event
        String itemId = String.valueOf(System.currentTimeMillis());
        String payloadString = "<car xmlns='pubsub:test:vehicle'><paint color='green'/><tires num='4'/></car>";
        creatorNode.send(new PayloadItem(itemId, new SimplePayload("car", "pubsub:test:vehicle", payloadString)));
        
   		ItemEventCoordinator<PayloadItem<CarExtension>> coord = queue.take();
       	assertEquals(1, coord.events.getItems().size());
       	PayloadItem item = coord.events.getItems().get(0);
       	assertEquals(itemId, item.getId());
       	assertTrue(item.getPayload() instanceof CarExtension);

       	CarExtension car = (CarExtension)item.getPayload();
       	assertEquals("green", car.getColor());
       	assertEquals(4, car.getNumTires());
	}
*/
	
	public void testSendAndReceiveMultipleSubs() throws Exception
	{
		// Setup event source
		String nodeId = "TestNode" + System.currentTimeMillis();
		PubSubManager creatorMgr = new PubSubManager(getConnection(0), getService());
		LeafNode creatorNode = getPubnode(creatorMgr, nodeId, true, false);

		BlockingQueue<ItemEventCoordinator<Item>> queue = new ArrayBlockingQueue<ItemEventCoordinator<Item>>(3);
		ItemEventCoordinator<Item> creatorHandler = new ItemEventCoordinator<Item>(queue, "creator");
		creatorNode.addItemEventListener(creatorHandler);
		
		// Setup event receiver
		PubSubManager subMgr = new PubSubManager(getConnection(1), getService());
		LeafNode subNode = (LeafNode)subMgr.getNode(nodeId);

		ItemEventCoordinator<Item> sub1Handler = new ItemEventCoordinator<Item>(queue, "sub1");
		subNode.addItemEventListener(sub1Handler);
		Subscription sub1 = subNode.subscribe(getConnection(1).getUser());
		
		ItemEventCoordinator<Item> sub2Handler = new ItemEventCoordinator<Item>(queue, "sub2");
		subNode.addItemEventListener(sub2Handler);
		Subscription sub2 = subNode.subscribe(getConnection(1).getUser());

		// Send event
        String itemId = String.valueOf(System.currentTimeMillis());
        creatorNode.send(new Item(itemId));
        
        for(int i=0; i<3; i++)
        {
    		ItemEventCoordinator<Item> coord = queue.take();
        	assertEquals(1, coord.events.getItems().size());
        	assertEquals(itemId, coord.events.getItems().iterator().next().getId());
        	
        	if (coord.id.equals("sub1") || coord.id.equals("sub2"))
        	{
        		assertEquals(2, coord.events.getSubscriptions().size());
        	}
        }
	}

	public void testSendAndReceiveMultipleItems() throws Exception
	{
		// Setup event source
		String nodeId = "TestNode" + System.currentTimeMillis();
		PubSubManager creatorMgr = new PubSubManager(getConnection(0), getService());
		
		LeafNode creatorNode = getPubnode(creatorMgr, nodeId, true, true);

		BlockingQueue<ItemEventCoordinator<PayloadItem<SimplePayload>>> queue = new ArrayBlockingQueue<ItemEventCoordinator<PayloadItem<SimplePayload>>>(3);
		ItemEventCoordinator<PayloadItem<SimplePayload>> creatorHandler = new ItemEventCoordinator<PayloadItem<SimplePayload>>(queue, "creator");
		creatorNode.addItemEventListener(creatorHandler);
		creatorNode.subscribe(getConnection(0).getUser());
		
		// Setup event receiver
		PubSubManager subMgr = new PubSubManager(getConnection(1), getService());
		LeafNode subNode = (LeafNode)subMgr.getNode(nodeId);

		ItemEventCoordinator<PayloadItem<SimplePayload>> sub1Handler = new ItemEventCoordinator<PayloadItem<SimplePayload>>(queue, "sub1");
		subNode.addItemEventListener(sub1Handler);
		Subscription sub1 = subNode.subscribe(getConnection(1).getUser());
		
		ItemEventCoordinator<PayloadItem<SimplePayload>> sub2Handler = new ItemEventCoordinator<PayloadItem<SimplePayload>>(queue, "sub2");
		subNode.addItemEventListener(sub2Handler);
		Subscription sub2 = subNode.subscribe(getConnection(1).getUser());
		
		assertEquals(Subscription.State.subscribed, sub1.getState());
		assertEquals(Subscription.State.subscribed, sub2.getState());

        // Send event
        String itemId = String.valueOf(System.currentTimeMillis());
        
        Collection<PayloadItem<SimplePayload>> items = new ArrayList<PayloadItem<SimplePayload>>(3);
        String ids[] = {"First-" + itemId, "Second-" + itemId, "Third-" + itemId};
        items.add(new PayloadItem<SimplePayload>(ids[0], new SimplePayload("a", "pubsub:test", "<a xmlns='pubsub:test' href='1'/>")));
        items.add(new PayloadItem<SimplePayload>(ids[1], new SimplePayload("a", "pubsub:test", "<a xmlns='pubsub:test' href='1'/>")));
        items.add(new PayloadItem<SimplePayload>(ids[2], new SimplePayload("a", "pubsub:test", "<a xmlns='pubsub:test' href='1'/>")));
        creatorNode.send(items);
        
        for(int i=0; i<3; i++)
        {
    		ItemEventCoordinator<PayloadItem<SimplePayload>> coord = queue.poll(5, TimeUnit.SECONDS);
        	if (coord == creatorHandler)
        		assertEquals(1, coord.events.getSubscriptions().size());
        	else
        		assertEquals(2, coord.events.getSubscriptions().size());
        	List<PayloadItem<SimplePayload>> itemResults = coord.events.getItems();
        	assertEquals(3, itemResults.size());

//        	assertEquals(ids[0], itemResults.get(0).getId());
//    		assertEquals("<a xmlns='pubsub:test' href='1'/>", itemResults.get(0).getPayload().toXML().replace('\"', '\''));
//    		assertEquals(ids[1], itemResults.get(1).getId());
//    		assertEquals("<a xmlns='pubsub:test' href='2'/>", itemResults.get(1).getPayload().toXML().replace('\"', '\''));
//    		assertEquals(ids[21], itemResults.get(2).getId());
//    		assertEquals("<a xmlns='pubsub:test' href='3'/>", itemResults.get(3).getPayload().toXML().replace('\"', '\''));
        }
	}

	public void testSendAndReceiveDelayed() throws Exception
	{
		// Setup event source
		String nodeId = "TestNode" + System.currentTimeMillis();
		PubSubManager creatorMgr = new PubSubManager(getConnection(0), getService());
		
		LeafNode creatorNode = getPubnode(creatorMgr, nodeId, true, false);

		// Send event
        String itemId = String.valueOf("DelayId-" + System.currentTimeMillis());
        String payloadString = "<book xmlns='pubsub:test:book'><author>Sir Arthur Conan Doyle</author></book>";
        creatorNode.send(new PayloadItem<SimplePayload>(itemId, new SimplePayload("book", "pubsub:test:book", payloadString)));

        Thread.sleep(1000);

        BlockingQueue<ItemEventCoordinator<PayloadItem<SimplePayload>>> queue = new ArrayBlockingQueue<ItemEventCoordinator<PayloadItem<SimplePayload>>>(3);

		// Setup event receiver
		PubSubManager subMgr = new PubSubManager(getConnection(1), getService());
		LeafNode subNode = (LeafNode)subMgr.getNode(nodeId);

		ItemEventCoordinator<PayloadItem<SimplePayload>> sub1Handler = new ItemEventCoordinator<PayloadItem<SimplePayload>>(queue, "sub1");
		subNode.addItemEventListener(sub1Handler);
		Subscription sub1 = subNode.subscribe(getConnection(1).getUser());

		ItemEventCoordinator<PayloadItem<SimplePayload>> coord = queue.take();
   		assertTrue(coord.events.isDelayed());
   		assertNotNull(coord.events.getPublishedDate());
	}

	public void testDeleteItemAndNotify() throws Exception
	{
		// Setup event source
		String nodeId = "TestNode" + System.currentTimeMillis();
		PubSubManager creatorMgr = new PubSubManager(getConnection(0), getService());
		
		LeafNode creatorNode = getPubnode(creatorMgr, nodeId, true, false);

		BlockingQueue<ItemDeleteCoordinator> queue = new ArrayBlockingQueue<ItemDeleteCoordinator>(3);
		
		// Setup event receiver
		PubSubManager subMgr = new PubSubManager(getConnection(1), getService());
		LeafNode subNode = (LeafNode)subMgr.getNode(nodeId);

		ItemDeleteCoordinator sub1Handler = new ItemDeleteCoordinator(queue, "sub1");
		subNode.addItemDeleteListener(sub1Handler);
		subNode.subscribe(getConnection(1).getUser());

		// Send event
        String itemId = String.valueOf(System.currentTimeMillis());
        
        Collection<Item> items = new ArrayList<Item>(3);
        String id1 = "First-" + itemId;
        String id2 = "Second-" + itemId;
        String id3 = "Third-" + itemId;
        items.add(new Item(id1));
        items.add(new Item(id2));
        items.add(new Item(id3));
        creatorNode.send(items);
        
        creatorNode.deleteItem(id1);
        
   		ItemDeleteCoordinator coord = queue.poll(5, TimeUnit.SECONDS);
   		assertEquals(1, coord.event.getItemIds().size());
   		assertEquals(id1, coord.event.getItemIds().get(0));

   		creatorNode.deleteItem(Arrays.asList(id2, id3));

   		coord = queue.poll(5, TimeUnit.SECONDS);
   		assertEquals(2, coord.event.getItemIds().size());
   		assertTrue(coord.event.getItemIds().contains(id2));
   		assertTrue(coord.event.getItemIds().contains(id3));
	}

	public void testPurgeAndNotify() throws Exception
	{
		// Setup event source
		String nodeId = "TestNode" + System.currentTimeMillis();
		PubSubManager creatorMgr = new PubSubManager(getConnection(0), getService());
		
		LeafNode creatorNode = getPubnode(creatorMgr, nodeId, true, false);

		BlockingQueue<ItemDeleteCoordinator> queue = new ArrayBlockingQueue<ItemDeleteCoordinator>(3);
		
		// Setup event receiver
		PubSubManager subMgr = new PubSubManager(getConnection(1), getService());
		LeafNode subNode = (LeafNode)subMgr.getNode(nodeId);

		ItemDeleteCoordinator sub1Handler = new ItemDeleteCoordinator(queue, "sub1");
		subNode.addItemDeleteListener(sub1Handler);
		subNode.subscribe(getConnection(1).getUser());

		// Send event
        String itemId = String.valueOf(System.currentTimeMillis());
        
        Collection<Item> items = new ArrayList<Item>(3);
        String id1 = "First-" + itemId;
        String id2 = "Second-" + itemId;
        String id3 = "Third-" + itemId;
        items.add(new Item(id1));
        items.add(new Item(id2));
        items.add(new Item(id3));
        creatorNode.send(items);
        
        creatorNode.deleteAllItems();
        
   		ItemDeleteCoordinator coord = queue.poll(5, TimeUnit.SECONDS);
   		assertNull(nodeId, coord.event);
	}

	public void testListenerMultipleNodes() throws Exception
	{
		// Setup event source
		String nodeId1 = "Node-1-" + System.currentTimeMillis();
		PubSubManager creatorMgr = new PubSubManager(getConnection(0), getService());
		String nodeId2 = "Node-2-" + System.currentTimeMillis();
		
		LeafNode creatorNode1 = getPubnode(creatorMgr, nodeId1, true, false);
		LeafNode creatorNode2 = getPubnode(creatorMgr, nodeId2, true, false);

		BlockingQueue<ItemEventCoordinator<Item>> queue = new ArrayBlockingQueue<ItemEventCoordinator<Item>>(3);
		
		PubSubManager subMgr = new PubSubManager(getConnection(1), getService());
		LeafNode subNode1 = (LeafNode)subMgr.getNode(nodeId1);
		LeafNode subNode2 = (LeafNode)subMgr.getNode(nodeId2);
		
		subNode1.addItemEventListener(new ItemEventCoordinator<Item>(queue, "sub1"));
		subNode2.addItemEventListener(new ItemEventCoordinator<Item>(queue, "sub2"));
		
		subNode1.subscribe(getConnection(1).getUser());
		subNode2.subscribe(getConnection(1).getUser());
		
		creatorNode1.send(new Item("item1"));
		creatorNode2.send(new Item("item2"));
		boolean check1 = false;
		boolean check2 = false;
		
		for (int i=0; i<2; i++)
		{
			ItemEventCoordinator<Item> event = queue.take();
			
			if (event.id.equals("sub1"))
			{
				assertEquals(event.events.getNodeId(), nodeId1);
				check1 = true;
			}
			else
			{
				assertEquals(event.events.getNodeId(), nodeId2);
				check2 = true;
			}
		}
		assertTrue(check1);
		assertTrue(check2);
	}

	class ItemEventCoordinator <T extends Item> implements ItemEventListener<T>
	{
		private BlockingQueue<ItemEventCoordinator<T>> theQueue;
		private ItemPublishEvent<T> events;
		private String id;
		
		ItemEventCoordinator(BlockingQueue<ItemEventCoordinator<T>> queue, String id)
		{
			theQueue = queue;
			this.id = id;
		}

		public void handlePublishedItems(ItemPublishEvent<T> items)
		{
			events = items;
			theQueue.add(this);
		}

		@Override
		public String toString()
		{
			return "ItemEventCoordinator: " + id;
		}
		
	}
	
	class NodeConfigCoordinator implements NodeConfigListener
	{
		private BlockingQueue<NodeConfigCoordinator> theQueue;
		private String id;
		private ConfigurationEvent event;
		
		NodeConfigCoordinator(BlockingQueue<NodeConfigCoordinator> queue, String id)
		{
			theQueue = queue;
			this.id = id;
		}

		public void handleNodeConfiguration(ConfigurationEvent config)
		{
			event = config;
			theQueue.add(this);
		}

		@Override
		public String toString()
		{
			return "NodeConfigCoordinator: " + id;
		}

	}

	class ItemDeleteCoordinator implements ItemDeleteListener
	{
		private BlockingQueue<ItemDeleteCoordinator> theQueue;
		private String id;
		private ItemDeleteEvent event;
		
		ItemDeleteCoordinator(BlockingQueue<ItemDeleteCoordinator> queue, String id)
		{
			theQueue = queue;
			this.id = id;
		}

		public void handleDeletedItems(ItemDeleteEvent delEvent)
		{
			event = delEvent;
			theQueue.add(this);
		}


		public void handlePurge()
		{
			event = null;
			theQueue.add(this);
		}

		@Override
		public String toString()
		{
			return "ItemDeleteCoordinator: " + id;
		}
	}

	static private LeafNode getPubnode(PubSubManager manager, String id, boolean persistItems, boolean deliverPayload) 
		throws XMPPException
	{
		ConfigureForm form = new ConfigureForm(FormType.submit);
		form.setPersistentItems(persistItems);
		form.setDeliverPayloads(deliverPayload);
		form.setAccessModel(AccessModel.open);
		return (LeafNode)manager.createNode(id, form);
	}

}
