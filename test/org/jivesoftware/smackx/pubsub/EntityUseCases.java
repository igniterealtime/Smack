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
 
import java.util.Iterator;
import java.util.List;

import org.jivesoftware.smackx.packet.DiscoverInfo;
import org.jivesoftware.smackx.packet.DiscoverItems;
import org.jivesoftware.smackx.packet.DiscoverInfo.Identity;
import org.jivesoftware.smackx.pubsub.test.SingleUserTestCase;

public class EntityUseCases extends SingleUserTestCase
{
	public void testDiscoverPubsubInfo() throws Exception
	{
		DiscoverInfo supportedFeatures = getManager().getSupportedFeatures();
		assertNotNull(supportedFeatures);
	}

	public void testDiscoverNodeInfo() throws Exception
	{
		LeafNode myNode = getManager().createNode("DiscoNode" + System.currentTimeMillis());
		DiscoverInfo info = myNode.discoverInfo();
		assertTrue(info.getIdentities().hasNext());
		Identity ident = info.getIdentities().next();
		
		assertEquals("leaf", ident.getType());
	}
	
	public void testDiscoverNodeItems() throws Exception
	{
		LeafNode myNode = getRandomPubnode(getManager(), true, false);
		myNode.send(new Item());
		myNode.send(new Item());
		myNode.send(new Item());
		myNode.send(new Item());
		DiscoverItems items = myNode.discoverItems();
		
		int count = 0;
		
		for(Iterator<DiscoverItems.Item> it = items.getItems(); it.hasNext(); it.next(),count++);
		
		assertEquals(4, count);
	}
	
	public void testDiscoverSubscriptions() throws Exception
	{
		getManager().getSubscriptions();
	}
	
	public void testDiscoverNodeSubscriptions() throws Exception
	{
		LeafNode myNode = getRandomPubnode(getManager(), true, true);
		myNode.subscribe(getConnection(0).getUser());
		List<Subscription> subscriptions = myNode.getSubscriptions();
		
		assertTrue(subscriptions.size() < 3);
		
		for (Subscription subscription : subscriptions) 
		{
			assertNull(subscription.getNode());
		}
	}
	
	public void testRetrieveAffiliation() throws Exception
	{
		getManager().getAffiliations();
	}
}
