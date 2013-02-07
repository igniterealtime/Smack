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
package org.jivesoftware.smackx.filetransfer;

import static org.junit.Assert.*;

import org.jivesoftware.smack.DummyConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class FileTransferNegotiatorTest {
    private DummyConnection connection;

    @Before
    public void setUp() throws Exception {
	// Uncomment this to enable debug output
	//Connection.DEBUG_ENABLED = true;

	connection = new DummyConnection();
	connection.connect();
	connection.login("me", "secret");
	new ServiceDiscoveryManager(connection);
    }
    
    @After
    public void tearDown() throws Exception {
	if (connection != null)
	    connection.disconnect();
    }

    @Test
    public void verifyForm() throws Exception
    {
	FileTransferNegotiator fileNeg = FileTransferNegotiator.getInstanceFor(connection);
	fileNeg.negotiateOutgoingTransfer("me", "streamid", "file", 1024, null, 10);
	Packet packet = connection.getSentPacket();
	assertTrue(packet.toXML().indexOf("\"stream-method\" type=\"list-single\"") != -1);
    }
}
