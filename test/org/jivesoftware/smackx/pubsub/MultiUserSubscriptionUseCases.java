package org.jivesoftware.smackx.pubsub;

import java.util.Collection;

import org.jivesoftware.smack.XMPPException;
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
}
