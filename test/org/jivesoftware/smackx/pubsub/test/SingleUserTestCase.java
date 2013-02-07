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
