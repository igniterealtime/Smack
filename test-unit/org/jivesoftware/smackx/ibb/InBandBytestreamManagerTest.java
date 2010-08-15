/**
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
package org.jivesoftware.smackx.ibb;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smackx.ibb.InBandBytestreamManager.StanzaType;
import org.jivesoftware.smackx.ibb.packet.Open;
import org.jivesoftware.util.ConnectionUtils;
import org.jivesoftware.util.Protocol;
import org.jivesoftware.util.Verification;
import org.junit.Before;
import org.junit.Test;

/**
 * Test for InBandBytestreamManager.
 * 
 * @author Henning Staib
 */
public class InBandBytestreamManagerTest {

    // settings
    String initiatorJID = "initiator@xmpp-server/Smack";
    String targetJID = "target@xmpp-server/Smack";
    String xmppServer = "xmpp-server";
    String sessionID = "session_id";

    // protocol verifier
    Protocol protocol;

    // mocked XMPP connection
    Connection connection;

    /**
     * Initialize fields used in the tests.
     */
    @Before
    public void setup() {

        // build protocol verifier
        protocol = new Protocol();

        // create mocked XMPP connection
        connection = ConnectionUtils.createMockedConnection(protocol, initiatorJID,
                        xmppServer);

    }

    /**
     * Test that
     * {@link InBandBytestreamManager#getByteStreamManager(Connection)} returns
     * one bytestream manager for every connection
     */
    @Test
    public void shouldHaveOneManagerForEveryConnection() {

        // mock two connections
        Connection connection1 = mock(Connection.class);
        Connection connection2 = mock(Connection.class);

        // get bytestream manager for the first connection twice
        InBandBytestreamManager conn1ByteStreamManager1 = InBandBytestreamManager.getByteStreamManager(connection1);
        InBandBytestreamManager conn1ByteStreamManager2 = InBandBytestreamManager.getByteStreamManager(connection1);

        // get bytestream manager for second connection
        InBandBytestreamManager conn2ByteStreamManager1 = InBandBytestreamManager.getByteStreamManager(connection2);

        // assertions
        assertEquals(conn1ByteStreamManager1, conn1ByteStreamManager2);
        assertNotSame(conn1ByteStreamManager1, conn2ByteStreamManager1);

    }

    /**
     * Invoking {@link InBandBytestreamManager#establishSession(String)} should
     * throw an exception if the given target does not support in-band
     * bytestream.
     */
    @Test
    public void shouldFailIfTargetDoesNotSupportIBB() {
        InBandBytestreamManager byteStreamManager = InBandBytestreamManager.getByteStreamManager(connection);

        try {
            XMPPError xmppError = new XMPPError(
                            XMPPError.Condition.feature_not_implemented);
            IQ errorIQ = IBBPacketUtils.createErrorIQ(targetJID, initiatorJID, xmppError);
            protocol.addResponse(errorIQ);

            // start In-Band Bytestream
            byteStreamManager.establishSession(targetJID);

            fail("exception should be thrown");
        }
        catch (XMPPException e) {
            assertEquals(XMPPError.Condition.feature_not_implemented.toString(),
                            e.getXMPPError().getCondition());
        }

    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotAllowTooBigDefaultBlockSize() {
        InBandBytestreamManager byteStreamManager = InBandBytestreamManager.getByteStreamManager(connection);
        byteStreamManager.setDefaultBlockSize(1000000);
    }

    @Test
    public void shouldCorrectlySetDefaultBlockSize() {
        InBandBytestreamManager byteStreamManager = InBandBytestreamManager.getByteStreamManager(connection);
        byteStreamManager.setDefaultBlockSize(1024);
        assertEquals(1024, byteStreamManager.getDefaultBlockSize());
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldNotAllowTooBigMaximumBlockSize() {
        InBandBytestreamManager byteStreamManager = InBandBytestreamManager.getByteStreamManager(connection);
        byteStreamManager.setMaximumBlockSize(1000000);
    }

    @Test
    public void shouldCorrectlySetMaximumBlockSize() {
        InBandBytestreamManager byteStreamManager = InBandBytestreamManager.getByteStreamManager(connection);
        byteStreamManager.setMaximumBlockSize(1024);
        assertEquals(1024, byteStreamManager.getMaximumBlockSize());
    }

    @Test
    public void shouldUseConfiguredStanzaType() {
        InBandBytestreamManager byteStreamManager = InBandBytestreamManager.getByteStreamManager(connection);
        byteStreamManager.setStanza(StanzaType.MESSAGE);

        protocol.addResponse(null, new Verification<Open, IQ>() {

            public void verify(Open request, IQ response) {
                assertEquals(StanzaType.MESSAGE, request.getStanza());
            }

        });

        try {
            // start In-Band Bytestream
            byteStreamManager.establishSession(targetJID);
        }
        catch (XMPPException e) {
            protocol.verifyAll();
        }

    }

    @Test
    public void shouldReturnSession() throws Exception {
        InBandBytestreamManager byteStreamManager = InBandBytestreamManager.getByteStreamManager(connection);

        IQ success = IBBPacketUtils.createResultIQ(targetJID, initiatorJID);
        protocol.addResponse(success, Verification.correspondingSenderReceiver,
                        Verification.requestTypeSET);

        // start In-Band Bytestream
        InBandBytestreamSession session = byteStreamManager.establishSession(targetJID);

        assertNotNull(session);
        assertNotNull(session.getInputStream());
        assertNotNull(session.getOutputStream());

        protocol.verifyAll();

    }

}
