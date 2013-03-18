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
package org.jivesoftware.smackx.pubsub;

import static org.junit.Assert.assertEquals;

import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.ThreadedDummyConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smack.packet.XMPPError.Condition;
import org.jivesoftware.smackx.packet.DiscoverInfo;
import org.jivesoftware.smackx.packet.DiscoverInfo.Identity;
import org.jivesoftware.smackx.pubsub.packet.PubSub;
import org.junit.Assert;
import org.junit.Test;

public class ConfigureFormTest
{
	@Test
	public void checkChildrenAssocPolicy()
	{
		ConfigureForm form = new ConfigureForm(FormType.submit);
		form.setChildrenAssociationPolicy(ChildrenAssociationPolicy.owners);
		assertEquals(ChildrenAssociationPolicy.owners, form.getChildrenAssociationPolicy());
	}
	
	@Test
	public void getConfigFormWithInsufficientPriviliges() throws XMPPException
	{
		ThreadedDummyConnection con = new ThreadedDummyConnection();
		PubSubManager mgr = new PubSubManager(con);
		DiscoverInfo info = new DiscoverInfo();
		Identity ident = new Identity("pubsub", null, "leaf");
		info.addIdentity(ident);
		con.addIQReply(info);
		
		Node node = mgr.getNode("princely_musings");
		
		PubSub errorIq = new PubSub();
		XMPPError error = new XMPPError(Condition.forbidden);
		errorIq.setError(error);
		con.addIQReply(errorIq);
		
		try
		{
			node.getNodeConfiguration();
		}
		catch (XMPPException e)
		{
			Assert.assertEquals(XMPPError.Type.AUTH, e.getXMPPError().getType());
		}
	}

	@Test (expected=XMPPException.class)
	public void getConfigFormWithTimeout() throws XMPPException
	{
		ThreadedDummyConnection con = new ThreadedDummyConnection();
		PubSubManager mgr = new PubSubManager(con);
		DiscoverInfo info = new DiscoverInfo();
		Identity ident = new Identity("pubsub", null, "leaf");
		info.addIdentity(ident);
		con.addIQReply(info);
		
		Node node = mgr.getNode("princely_musings");
		
		SmackConfiguration.setPacketReplyTimeout(100);
		node.getNodeConfiguration();
	}
}
