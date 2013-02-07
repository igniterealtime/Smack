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

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.pubsub.test.SingleUserTestCase;

public class OwnerUseCases extends SingleUserTestCase
{
	public void testCreateInstantNode() throws Exception
	{
		LeafNode node = getManager().createNode();
		assertNotNull(node);
		assertNotNull(node.getId());
	}

	public void testCreateNamedNode() throws Exception
	{
		String id = "TestNamedNode" + System.currentTimeMillis();
		LeafNode node = getManager().createNode(id);
		assertEquals(id, node.getId());
	}

	public void testCreateConfiguredNode() throws Exception
	{
		// Generate reasonably unique for multiple tests
		String id = "TestConfigNode" + System.currentTimeMillis();
		
		// Create and configure a node
		ConfigureForm form = new ConfigureForm(FormType.submit);
		form.setAccessModel(AccessModel.open);
		form.setDeliverPayloads(false);
		form.setNotifyRetract(true);
		form.setPersistentItems(true);
		form.setPublishModel(PublishModel.open);

		LeafNode node = (LeafNode)getManager().createNode(id, form);
		
		ConfigureForm currentForm = node.getNodeConfiguration();
		assertEquals(AccessModel.open, currentForm.getAccessModel());
		assertFalse(currentForm.isDeliverPayloads());
		assertTrue(currentForm.isNotifyRetract());
		assertTrue(currentForm.isPersistItems());
		assertEquals(PublishModel.open, currentForm.getPublishModel());
	}

	public void testCreateAndUpdateConfiguredNode() throws Exception
	{
		// Generate reasonably unique for multiple tests
		String id = "TestConfigNode2" + System.currentTimeMillis();
		
		// Create and configure a node
		ConfigureForm form = new ConfigureForm(FormType.submit);
		form.setAccessModel(AccessModel.open);
		form.setDeliverPayloads(false);
		form.setNotifyRetract(true);
		form.setPersistentItems(true);
		form.setPublishModel(PublishModel.open);
		
		LeafNode myNode = (LeafNode)getManager().createNode(id, form);
		ConfigureForm config = myNode.getNodeConfiguration();
		
		assertEquals(AccessModel.open, config.getAccessModel());
		assertFalse(config.isDeliverPayloads());
		assertTrue(config.isNotifyRetract());
		assertTrue(config.isPersistItems());
		assertEquals(PublishModel.open, config.getPublishModel());
			
		ConfigureForm submitForm = new ConfigureForm(config.createAnswerForm());
		submitForm.setAccessModel(AccessModel.whitelist);
		submitForm.setDeliverPayloads(true);
		submitForm.setNotifyRetract(false);
		submitForm.setPersistentItems(false);
		submitForm.setPublishModel(PublishModel.publishers);
		myNode.sendConfigurationForm(submitForm);

		ConfigureForm newConfig = myNode.getNodeConfiguration();
		assertEquals(AccessModel.whitelist, newConfig.getAccessModel());
		assertTrue(newConfig.isDeliverPayloads());
		assertFalse(newConfig.isNotifyRetract());
		assertFalse(newConfig.isPersistItems());
		assertEquals(PublishModel.publishers, newConfig.getPublishModel());
	}

	public void testGetDefaultConfig() throws Exception
	{
		ConfigureForm form = getManager().getDefaultConfiguration();
		assertNotNull(form);
	}
	
	public void testDeleteNode() throws Exception
	{
		LeafNode myNode = getManager().createNode();
		assertNotNull(getManager().getNode(myNode.getId()));
		
		getManager(0).deleteNode(myNode.getId());
		
		try
		{
			assertNull(getManager().getNode(myNode.getId()));
			fail("Node should not exist");
		}
		catch (XMPPException e)
		{
		}
	}
	
	public void testPurgeItems() throws XMPPException
	{
		LeafNode node = getRandomPubnode(getManager(), true, false);
		
		node.send(new Item());
		node.send(new Item());
		node.send(new Item());
		node.send(new Item());
		node.send(new Item());
		
		Collection<? extends Item> items = node.getItems();
		assertTrue(items.size() == 5);

		node.deleteAllItems();
		items = node.getItems();
		
		// Pubsub service may keep the last notification (in spec), so 0 or 1 may be returned on get items.
		assertTrue(items.size() < 2);
	}
}
