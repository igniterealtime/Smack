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
