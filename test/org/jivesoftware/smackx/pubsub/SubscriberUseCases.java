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
 * Created on 2009-04-09
 */
package org.jivesoftware.smackx.pubsub;
 
import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.FormField;
import org.jivesoftware.smackx.pubsub.test.SingleUserTestCase;

public class SubscriberUseCases extends SingleUserTestCase
{
	public void testSubscribe() throws Exception
	{
		LeafNode node = getPubnode(false, false);
		Subscription sub = node.subscribe(getJid());
		
		assertEquals(getJid(), sub.getJid());
		assertNotNull(sub.getId());
		assertEquals(node.getId(), sub.getNode());
		assertEquals(Subscription.State.subscribed, sub.getState());
	}

	public void testSubscribeBadJid() throws Exception
	{
		LeafNode node = getPubnode(false, false);
		
		try
		{
			node.subscribe("this@over.here");
			fail();
		}
		catch (XMPPException e)
		{
		}
	}

	public void testSubscribeWithOptions() throws Exception
	{
		SubscribeForm form = new SubscribeForm(FormType.submit);
		form.setDeliverOn(true);
		Calendar expire = Calendar.getInstance();
		expire.set(2020, 1, 1);
		form.setExpiry(expire.getTime());
		LeafNode node = getPubnode(false, false);
		node.subscribe(getJid(), form);
	}
	
	public void testSubscribeConfigRequired() throws Exception
	{
		ConfigureForm form = new ConfigureForm(FormType.submit);
		form.setAccessModel(AccessModel.open);
		
		// Openfire specific field - nothing in the spec yet
		FormField required = new FormField("pubsub#subscription_required");
		required.setType(FormField.TYPE_BOOLEAN);
		form.addField(required);
		form.setAnswer("pubsub#subscription_required", true);
		LeafNode node = (LeafNode)getManager().createNode("Pubnode" + System.currentTimeMillis(), form);

		Subscription sub = node.subscribe(getJid());
		
		assertEquals(getJid(), sub.getJid());
		assertNotNull(sub.getId());
		assertEquals(node.getId(), sub.getNode());
		assertEquals(true, sub.isConfigRequired());
	}
	
	public void testUnsubscribe() throws Exception
	{
		LeafNode node = getPubnode(false, false);
		node.subscribe(getJid());
		Collection<Subscription> subs = node.getSubscriptions();
		
		node.unsubscribe(getJid());
		Collection<Subscription> afterSubs = node.getSubscriptions();
		assertEquals(subs.size()-1, afterSubs.size());
	}
	
	public void testUnsubscribeWithMultipleNoSubId() throws Exception
	{
		LeafNode node = getPubnode(false, false);
		node.subscribe(getBareJID(0));
		node.subscribe(getBareJID(0));
		node.subscribe(getBareJID(0));
		
		try
		{
			node.unsubscribe(getBareJID(0));
			fail("Unsubscribe with no subid should fail");
		}
		catch (XMPPException e)
		{
		}
	}
	
	public void testUnsubscribeWithMultipleWithSubId() throws Exception
	{
		LeafNode node = getPubnode(false, false);
		node.subscribe(getJid());
		Subscription sub = node.subscribe(getJid());
		node.subscribe(getJid());
		node.unsubscribe(getJid(), sub.getId());
	}
	
	public void testGetOptions() throws Exception
	{
		LeafNode node = getPubnode(false, false);
		Subscription sub = node.subscribe(getJid());
		SubscribeForm form = node.getSubscriptionOptions(getJid(), sub.getId());
		assertNotNull(form);
	}
	
//	public void testSubscribeWithConfig() throws Exception
//	{		
//		LeafNode node = getPubnode(false, false);
//
//		Subscription sub = node.subscribe(getBareJID(0));
//		
//		assertEquals(getBareJID(0), sub.getJid());
//		assertNotNull(sub.getId());
//		assertEquals(node.getId(), sub.getNode());
//		assertEquals(true, sub.isConfigRequired());
//	}
//	
	public void testGetItems() throws Exception
	{
		LeafNode node = getPubnode(true, false);
		runNodeTests(node);
	}
	
	private void runNodeTests(LeafNode node) throws Exception
	{
		node.send((Item)null);
		node.send((Item)null);
		node.send((Item)null);
		node.send((Item)null);
		node.send((Item)null);
		
		Collection<? extends Item> items = node.getItems();
		assertTrue(items.size() == 5);
		
		long curTime = System.currentTimeMillis();
		node.send(new Item("1-" + curTime));
		node.send(new Item("2-" + curTime));
		node.send(new Item("3-" + curTime));
		node.send(new Item("4-" + curTime));
		node.send(new Item("5-" + curTime));
		
		items = node.getItems();
		assertTrue(items.size() == 10);

		LeafNode payloadNode = getPubnode(true, true);

		Map<String , String> idPayload = new HashMap<String, String>();
		idPayload.put("6-" + curTime, "<a/>");
		idPayload.put("7-" + curTime, "<a href=\"/up/here\"/>");
		idPayload.put("8-" + curTime, "<entity>text<inner></inner></entity>");
		idPayload.put("9-" + curTime, "<entity><inner><text></text></inner></entity>");
		
		for (Map.Entry<String, String> payload : idPayload.entrySet())
		{
			payloadNode.send(new PayloadItem<SimplePayload>(payload.getKey(), new SimplePayload("a", "pubsub:test", payload.getValue())));
		}
		
		payloadNode.send(new PayloadItem<SimplePayload>("6-" + curTime, new SimplePayload("a", "pubsub:test", "<a xmlns='pubsub:test'/>")));
		payloadNode.send(new PayloadItem<SimplePayload>("7-" + curTime, new SimplePayload("a", "pubsub:test", "<a xmlns='pubsub:test' href=\'/up/here\'/>")));
		payloadNode.send(new PayloadItem<SimplePayload>("8-" + curTime, new SimplePayload("entity", "pubsub:test", "<entity xmlns='pubsub:test'>text<inner>a</inner></entity>")));
		payloadNode.send(new PayloadItem<SimplePayload>("9-" + curTime, new SimplePayload("entity", "pubsub:test", "<entity xmlns='pubsub:test'><inner><text>b</text></inner></entity>")));
		
		List<PayloadItem<SimplePayload>> payloadItems = payloadNode.getItems();
		Map<String, PayloadItem<SimplePayload>> idMap = new HashMap<String, PayloadItem<SimplePayload>>();
		
		for (PayloadItem<SimplePayload> payloadItem : payloadItems) 
		{
			idMap.put(payloadItem.getId(), payloadItem);
		}
		
		assertEquals(4, payloadItems.size());

		PayloadItem<SimplePayload> testItem = idMap.get("6-" + curTime);
		assertNotNull(testItem);
		assertXMLEqual("<a xmlns='pubsub:test'/>", testItem.getPayload().toXML());
		
		testItem = idMap.get("7-" + curTime);
		assertNotNull(testItem);
		assertXMLEqual("<a xmlns='pubsub:test' href=\'/up/here\'/>", testItem.getPayload().toXML());
		
		testItem = idMap.get("8-" + curTime);
		assertNotNull(testItem);
		assertXMLEqual("<entity xmlns='pubsub:test'>text<inner>a</inner></entity>", testItem.getPayload().toXML());

		testItem = idMap.get("9-" + curTime);
		assertNotNull(testItem);
		assertXMLEqual("<entity xmlns='pubsub:test'><inner><text>b</text></inner></entity>", testItem.getPayload().toXML());
	}

	public void testGetSpecifiedItems() throws Exception
	{
		LeafNode node = getPubnode(true, true);
		
		node.send(new PayloadItem<SimplePayload>("1", new SimplePayload("a", "pubsub:test", "<a xmlns='pubsub:test' href='1'/>")));
		node.send(new PayloadItem<SimplePayload>("2", new SimplePayload("a", "pubsub:test", "<a xmlns='pubsub:test' href='2'/>")));
		node.send(new PayloadItem<SimplePayload>("3", new SimplePayload("a", "pubsub:test", "<a xmlns='pubsub:test' href='3'/>")));
		node.send(new PayloadItem<SimplePayload>("4", new SimplePayload("a", "pubsub:test", "<a xmlns='pubsub:test' href='4'/>")));
		node.send(new PayloadItem<SimplePayload>("5", new SimplePayload("a", "pubsub:test", "<a xmlns='pubsub:test' href='5'/>")));
		
		Collection<String> ids = new ArrayList<String>(3);
		ids.add("1");
		ids.add("3");
		ids.add("4");

		List<PayloadItem<SimplePayload>> items = node.getItems(ids);
		assertEquals(3, items.size());
		assertEquals("1", items.get(0).getId());
		assertXMLEqual("<a xmlns='pubsub:test' href='1'/>", items.get(0).getPayload().toXML());
		assertEquals( "3", items.get(1).getId());
		assertXMLEqual("<a xmlns='pubsub:test' href='3'/>", items.get(1).getPayload().toXML());
		assertEquals("4", items.get(2).getId());
		assertXMLEqual("<a xmlns='pubsub:test' href='4'/>", items.get(2).getPayload().toXML());
	}

	public void testGetLastNItems() throws XMPPException
	{
		LeafNode node = getPubnode(true, false);
		
		node.send(new Item("1"));
		node.send(new Item("2"));
		node.send(new Item("3"));
		node.send(new Item("4"));
		node.send(new Item("5"));
		
		List<Item> items = node.getItems(2);
		assertEquals(2, items.size());
		assertTrue(listContainsId("4", items));
		assertTrue(listContainsId("5", items));
	}

	private static boolean listContainsId(String id, List<Item> items) 
	{
		for (Item item : items) 
		{
			if (item.getId().equals(id))
				return true;
		}
		return false;
	}

	private String getJid()
	{
		return getConnection(0).getUser();
	}

}
