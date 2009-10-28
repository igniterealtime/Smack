/*
 * Created on 2009-07-13
 */
package org.jivesoftware.smackx.pubsub;

import org.jivesoftware.smack.XMPPConnection;

public class CollectionNode extends Node
{
	CollectionNode(XMPPConnection connection, String nodeId)
	{
		super(connection, nodeId);
	}

}
