/*
 * Created on 2009-05-05
 */
package org.jivesoftware.smackx.pubsub.test;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.pubsub.LeafNode;
import org.jivesoftware.smackx.pubsub.PubSubManager;

public class SingleUserTestCase extends PubSubTestCase
{
	protected PubSubManager getManager()
	{
		return getManager(0);
	}
	
	protected LeafNode getPubnode(boolean persistItems, boolean deliverPayload) throws XMPPException
	{
		return getRandomPubnode(getManager(), persistItems, deliverPayload);
	}

	@Override
	protected int getMaxConnections()
	{
		return 1;
	}

}
