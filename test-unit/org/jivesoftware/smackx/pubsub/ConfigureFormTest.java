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
		Identity ident = new Identity("pubsub", null);
		ident.setType("leaf");
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
		Identity ident = new Identity("pubsub", null);
		ident.setType("leaf");
		info.addIdentity(ident);
		con.addIQReply(info);
		
		Node node = mgr.getNode("princely_musings");
		
		SmackConfiguration.setPacketReplyTimeout(100);
		node.getNodeConfiguration();
	}
}
