/**
 *
 * Copyright the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
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
package org.jivesoftware.smackx.bytestreams.ibb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smackx.InitExtensions;
import org.jivesoftware.smackx.bytestreams.ibb.InBandBytestreamManager.StanzaType;
import org.jivesoftware.smackx.bytestreams.ibb.packet.Open;
import org.jivesoftware.util.ConnectionUtils;
import org.jivesoftware.util.Protocol;
import org.jivesoftware.util.Verification;
import org.junit.Before;
import org.junit.Test;
import org.jxmpp.jid.DomainBareJid;
import org.jxmpp.jid.EntityFullJid;
import org.jxmpp.jid.JidTestUtil;

/**
 * Test for InBandBytestreamManager.
 * 
 * @author Henning Staib
 */
public class InBandBytestreamManagerTest extends InitExtensions {

    // settings
    static final EntityFullJid initiatorJID = JidTestUtil.DUMMY_AT_EXAMPLE_ORG_SLASH_DUMMYRESOURCE;
    static final EntityFullJid targetJID = JidTestUtil.FULL_JID_1_RESOURCE_1;
    static final DomainBareJid xmppServer = JidTestUtil.DOMAIN_BARE_JID_1;
    String sessionID = "session_id";

    // protocol verifier
    Protocol protocol;

    // mocked XMPP connection
    XMPPConnection connection;

    /**
     * Initialize fields used in the tests.
     * @throws XMPPException 
     * @throws SmackException 
     * @throws InterruptedException 
     */
    @Before
    public void setup() throws XMPPException, SmackException, InterruptedException {

        // build protocol verifier
        protocol = new Protocol();

        // create mocked XMPP connection
        connection = ConnectionUtils.createMockedConnection(protocol, initiatorJID,
                        xmppServer);

    }

    /**
     * Test that
     * {@link InBandBytestreamManager#getByteStreamManager(XMPPConnection)} returns
     * one bytestream manager for every connection.
     */
    @Test
    public void shouldHaveOneManagerForEveryConnection() {

        // mock two connections
        XMPPConnection connection1 = mock(XMPPConnection.class);
        XMPPConnection connection2 = mock(XMPPConnection.class);

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
     * Invoking {@link InBandBytestreamManager#establishSession(org.jxmpp.jid.Jid)} should
     * throw an exception if the given target does not support in-band
     * bytestream.
     * @throws SmackException 
     * @throws XMPPException 
     * @throws InterruptedException 
     */
    @Test
    public void shouldFailIfTargetDoesNotSupportIBB() throws SmackException, XMPPException, InterruptedException {
        InBandBytestreamManager byteStreamManager = InBandBytestreamManager.getByteStreamManager(connection);

        try {
            IQ errorIQ = IBBPacketUtils.createErrorIQ(targetJID, initiatorJID,
                            XMPPError.Condition.feature_not_implemented);
            protocol.addResponse(errorIQ);

            // start In-Band Bytestream
            byteStreamManager.establishSession(targetJID);

            fail("exception should be thrown");
        }
        catch (XMPPErrorException e) {
            assertEquals(XMPPError.Condition.feature_not_implemented,
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
    public void shouldUseConfiguredStanzaType() throws SmackException, InterruptedException {
        InBandBytestreamManager byteStreamManager = InBandBytestreamManager.getByteStreamManager(connection);
        byteStreamManager.setStanza(StanzaType.MESSAGE);

        protocol.addResponse(null, new Verification<Open, IQ>() {

            @Override
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
