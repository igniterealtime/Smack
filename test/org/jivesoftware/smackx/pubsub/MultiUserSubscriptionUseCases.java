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
