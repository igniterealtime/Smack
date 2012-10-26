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
