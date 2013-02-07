/**
 * $RCSfile$
 * $Revision: $
 * $Date: $
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
package org.jivesoftware.smackx.pubsub;

import java.util.Collection;
import java.util.List;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smackx.pubsub.test.PubSubTestCase;

public class MultiUserSubscriptionUseCases extends PubSubTestCase
{

	@Override
	protected int getMaxConnections()
	{
		return 2;
	}

	public void testGetItemsWithSingleSubscription() throws XMPPException
	{
		LeafNode node = getRandomPubnode(getManager(0), true, false);
		node.send((Item)null);
		node.send((Item)null);
		node.send((Item)null);
		node.send((Item)null);
		node.send((Item)null);
		
		LeafNode user2Node = (LeafNode) getManager(1).getNode(node.getId());
		user2Node.subscribe(getBareJID(1));

		Collection<? extends Item> items = user2Node.getItems();
		assertTrue(items.size() == 5);
	}

	public void testGetItemsWithMultiSubscription() throws XMPPException
	{
		LeafNode node = getRandomPubnode(getManager(0), true, false);
		node.send((Item)null);
		node.send((Item)null);
		node.send((Item)null);
		node.send((Item)null);
		node.send((Item)null);
		
		LeafNode user2Node = (LeafNode) getManager(1).getNode(node.getId());
		Subscription sub1 = user2Node.subscribe(getBareJID(1));

		Subscription sub2 = user2Node.subscribe(getBareJID(1));

		try
		{
			user2Node.getItems();
		}
		catch (XMPPException exc)
		{
			assertEquals("bad-request", exc.getXMPPError().getCondition());
			assertEquals(XMPPError.Type.MODIFY, exc.getXMPPError().getType());
		}
		List<Item> items = user2Node.getItems(sub1.getId());
		assertTrue(items.size() == 5);
	}
}
