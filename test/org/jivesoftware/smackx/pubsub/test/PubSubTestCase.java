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
 * Created on 2009-05-05
 */
package org.jivesoftware.smackx.pubsub.test;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.test.SmackTestCase;
import org.jivesoftware.smackx.pubsub.AccessModel;
import org.jivesoftware.smackx.pubsub.ConfigureForm;
import org.jivesoftware.smackx.pubsub.FormType;
import org.jivesoftware.smackx.pubsub.LeafNode;
import org.jivesoftware.smackx.pubsub.PubSubManager;

abstract public class PubSubTestCase extends SmackTestCase
{
	private PubSubManager[] manager;

	public PubSubTestCase(String arg0)
	{
		super(arg0);
	}

	public PubSubTestCase()
	{
		super("PubSub Test Case");
	}

	protected LeafNode getRandomPubnode(PubSubManager pubMgr, boolean persistItems, boolean deliverPayload) throws XMPPException
	{
		ConfigureForm form = new ConfigureForm(FormType.submit);
		form.setPersistentItems(persistItems);
		form.setDeliverPayloads(deliverPayload);
		form.setAccessModel(AccessModel.open);
		return (LeafNode)pubMgr.createNode("/test/Pubnode" + System.currentTimeMillis(), form);
	}

	protected LeafNode getPubnode(PubSubManager pubMgr, boolean persistItems, boolean deliverPayload, String nodeId) throws XMPPException
	{
		LeafNode node = null;
		
		try
		{
			node = (LeafNode)pubMgr.getNode(nodeId);
		}
		catch (XMPPException e)
		{
			ConfigureForm form = new ConfigureForm(FormType.submit);
			form.setPersistentItems(persistItems);
			form.setDeliverPayloads(deliverPayload);
			form.setAccessModel(AccessModel.open);
			node = (LeafNode)pubMgr.createNode(nodeId, form);
		}
		return node;
	}

	protected PubSubManager getManager(int idx)
	{
		if (manager == null)
		{
			manager = new PubSubManager[getMaxConnections()];
			
			for(int i=0; i<manager.length; i++)
			{
				manager[i] = new PubSubManager(getConnection(i), getService());
			}
		}
		return manager[idx];
	}
	
	protected String getService()
	{
		return "pubsub." + getServiceName();
	}
}
