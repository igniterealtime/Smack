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
 
import java.util.Collection;
import java.util.List;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smack.packet.XMPPError.Condition;
import org.jivesoftware.smackx.pubsub.packet.PubSubNamespace;
import org.jivesoftware.smackx.pubsub.test.SingleUserTestCase;

public class PublisherUseCases extends SingleUserTestCase
{
	public void testSendNodeTrNot() throws Exception
	{
		getPubnode(false, false).send();
	}

	public void testSendNodeTrPay_WithOutPayload() throws XMPPException
	{
		LeafNode node = getPubnode(false, true);

		try
		{
			node.send(new Item());
			fail("Exception should be thrown when there is no payload");
		}
		catch (XMPPException e) {
			XMPPError err = e.getXMPPError();
			assertTrue(err.getType().equals(XMPPError.Type.MODIFY));
			assertTrue(err.getCondition().equals(Condition.bad_request.toString()));
			assertNotNull(err.getExtension("payload-required", PubSubNamespace.ERROR.getXmlns()));
		}

		try
		{
			node.send(new Item("test" + System.currentTimeMillis()));
			fail("Exception should be thrown when there is no payload");
		}
		catch (XMPPException e) {
			XMPPError err = e.getXMPPError();
			assertTrue(err.getType().equals(XMPPError.Type.MODIFY));
			assertTrue(err.getCondition().equals(Condition.bad_request.toString()));
			assertNotNull(err.getExtension("payload-required", PubSubNamespace.ERROR.getXmlns()));
		}
	}

	public void testSendNodeTrPay_WithPayload() throws XMPPException
	{
		LeafNode node = getPubnode(false, true);
		node.send(new PayloadItem<SimplePayload>(null, 
						new SimplePayload("book", "pubsub:test:book", "<book xmlns='pubsub:test:book'><title>Lord of the Rings</title></book>")));
		node.send(new PayloadItem<SimplePayload>("test" + System.currentTimeMillis(), 
						new SimplePayload("book", "pubsub:test:book", "<book xmlns='pubsub:test:book'><title>Two Towers</title></book>")));
	}

	public void testSendNodePerNot() throws Exception
	{
		LeafNode node = getPubnode(true, false);
		node.send(new Item());
		node.send(new Item("test" + System.currentTimeMillis()));
		node.send(new PayloadItem<SimplePayload>(null, 
						new SimplePayload("book", "pubsub:test:book", "<book xmlns='pubsub:test:book'><title>Lord of the Rings</title></book>")));
		node.send(new PayloadItem<SimplePayload>("test" + System.currentTimeMillis(), 
						new SimplePayload("book", "pubsub:test:book", "<book xmlns='pubsub:test:book'><title>Two Towers</title></book>")));
	}

	public void testSendPerPay_WithPayload() throws Exception
	{
		LeafNode node = getPubnode(true, true);
		node.send(new PayloadItem<SimplePayload>(null, 
						new SimplePayload("book", "pubsub:test:book", "<book xmlns='pubsub:test:book'><title>Lord of the Rings</title></book>")));
		node.send(new PayloadItem<SimplePayload>("test" + System.currentTimeMillis(), 
						new SimplePayload("book", "pubsub:test:book", "<book xmlns='pubsub:test:book'><title>Two Towers</title></book>")));
	}

	public void testSendPerPay_NoPayload() throws Exception
	{
		LeafNode node = getPubnode(true, true);
		try
		{
			node.send(new Item());
			fail("Exception should be thrown when there is no payload");
		}
		catch (XMPPException e) {
			XMPPError err = e.getXMPPError();
			assertTrue(err.getType().equals(XMPPError.Type.MODIFY));
			assertTrue(err.getCondition().equals(Condition.bad_request.toString()));
			assertNotNull(err.getExtension("payload-required", PubSubNamespace.ERROR.getXmlns()));
		}

		try
		{
			node.send(new Item("test" + System.currentTimeMillis()));
			fail("Exception should be thrown when there is no payload");
		}
		catch (XMPPException e) {
			XMPPError err = e.getXMPPError();
			assertTrue(err.getType().equals(XMPPError.Type.MODIFY));
			assertTrue(err.getCondition().equals(Condition.bad_request.toString()));
			assertNotNull(err.getExtension("payload-required", PubSubNamespace.ERROR.getXmlns()));
		}
	}

	public void testDeleteItems() throws XMPPException
	{
		LeafNode node = getPubnode(true, false);
		
		node.send(new Item("1"));
		node.send(new Item("2"));
		node.send(new Item("3"));
		node.send(new Item("4"));
		
		node.deleteItem("1");
		Collection<? extends Item> items = node.getItems();
		
		assertEquals(3, items.size());
	}

	public void testPersistItems() throws XMPPException
	{
		LeafNode node = getPubnode(true, false);
		
		node.send(new Item("1"));
		node.send(new Item("2"));
		node.send(new Item("3"));
		node.send(new Item("4"));
		
		Collection<? extends Item> items = node.getItems();
		
		assertTrue(items.size() == 4);
	}
	
	public void testItemOverwritten() throws XMPPException
	{
		LeafNode node = getPubnode(true, false);
		
		node.send(new PayloadItem<SimplePayload>("1", new SimplePayload("test", null, "<test/>")));
		node.send(new PayloadItem<SimplePayload>("1", new SimplePayload("test2", null, "<test2/>")));
		
		List<? extends Item> items = node.getItems();
		assertEquals(1, items.size());
		assertEquals("1", items.get(0).getId());
	}
}
