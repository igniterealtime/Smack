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
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;

import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.util.stringencoder.Base64;
import org.jivesoftware.smackx.InitExtensions;
import org.jivesoftware.smackx.bytestreams.ibb.InBandBytestreamManager.StanzaType;
import org.jivesoftware.smackx.bytestreams.ibb.packet.DataPacketExtension;
import org.jivesoftware.smackx.bytestreams.ibb.packet.Open;
import org.jivesoftware.util.ConnectionUtils;
import org.jivesoftware.util.Protocol;
import org.jivesoftware.util.Verification;
import org.junit.Before;
import org.junit.Test;
import org.jxmpp.jid.DomainBareJid;
import org.jxmpp.jid.EntityFullJid;
import org.jxmpp.jid.JidTestUtil;
import org.powermock.reflect.Whitebox;

/**
 * Test for InBandBytestreamSession.
 * <p>
 * Tests sending data encapsulated in message stanzas.
 * 
 * @author Henning Staib
 */
public class InBandBytestreamSessionMessageTest extends InitExtensions {

    // settings
    static final EntityFullJid initiatorJID = JidTestUtil.DUMMY_AT_EXAMPLE_ORG_SLASH_DUMMYRESOURCE;
    static final EntityFullJid targetJID = JidTestUtil.FULL_JID_1_RESOURCE_1;
    static final DomainBareJid xmppServer = JidTestUtil.DOMAIN_BARE_JID_1;
    String sessionID = "session_id";

    int blockSize = 10;

    // protocol verifier
    Protocol protocol;

    // mocked XMPP connection
    XMPPConnection connection;

    InBandBytestreamManager byteStreamManager;

    Open initBytestream;

    Verification<Message, IQ> incrementingSequence;

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
        connection = ConnectionUtils.createMockedConnection(protocol, initiatorJID, xmppServer);

        // initialize InBandBytestreamManager to get the InitiationListener
        byteStreamManager = InBandBytestreamManager.getByteStreamManager(connection);

        // create a In-Band Bytestream open packet with message stanza
        initBytestream = new Open(sessionID, blockSize, StanzaType.MESSAGE);
        initBytestream.setFrom(initiatorJID);
        initBytestream.setTo(targetJID);

        incrementingSequence = new Verification<Message, IQ>() {

            long lastSeq = 0;

            @Override
            public void verify(Message request, IQ response) {
                DataPacketExtension dpe = (DataPacketExtension) request.getExtension(
                                DataPacketExtension.ELEMENT, DataPacketExtension.NAMESPACE);
                assertEquals(lastSeq++, dpe.getSeq());
            }

        };

    }

    /**
     * Test the output stream write(byte[]) method.
     * 
     * @throws Exception should not happen
     */
    @Test
    public void shouldSendThreeDataPackets1() throws Exception {
        InBandBytestreamSession session = new InBandBytestreamSession(connection, initBytestream,
                        initiatorJID);

        // verify the data packets
        protocol.addResponse(null, incrementingSequence);
        protocol.addResponse(null, incrementingSequence);
        protocol.addResponse(null, incrementingSequence);

        byte[] controlData = new byte[blockSize * 3];

        OutputStream outputStream = session.getOutputStream();
        outputStream.write(controlData);
        outputStream.flush();

        protocol.verifyAll();

    }

    /**
     * Test the output stream write(byte) method.
     * 
     * @throws Exception should not happen
     */
    @Test
    public void shouldSendThreeDataPackets2() throws Exception {
        InBandBytestreamSession session = new InBandBytestreamSession(connection, initBytestream,
                        initiatorJID);

        // verify the data packets
        protocol.addResponse(null, incrementingSequence);
        protocol.addResponse(null, incrementingSequence);
        protocol.addResponse(null, incrementingSequence);

        byte[] controlData = new byte[blockSize * 3];

        OutputStream outputStream = session.getOutputStream();
        for (byte b : controlData) {
            outputStream.write(b);
        }
        outputStream.flush();

        protocol.verifyAll();

    }

    /**
     * Test the output stream write(byte[], int, int) method.
     * 
     * @throws Exception should not happen
     */
    @Test
    public void shouldSendThreeDataPackets3() throws Exception {
        InBandBytestreamSession session = new InBandBytestreamSession(connection, initBytestream,
                        initiatorJID);

        // verify the data packets
        protocol.addResponse(null, incrementingSequence);
        protocol.addResponse(null, incrementingSequence);
        protocol.addResponse(null, incrementingSequence);

        byte[] controlData = new byte[(blockSize * 3) - 2];

        OutputStream outputStream = session.getOutputStream();
        int off = 0;
        for (int i = 1; i <= 7; i++) {
            outputStream.write(controlData, off, i);
            off += i;
        }
        outputStream.flush();

        protocol.verifyAll();

    }

    /**
     * Test the output stream flush() method.
     * 
     * @throws Exception should not happen
     */
    @Test
    public void shouldSendThirtyDataPackets() throws Exception {
        byte[] controlData = new byte[blockSize * 3];

        InBandBytestreamSession session = new InBandBytestreamSession(connection, initBytestream,
                        initiatorJID);

        // verify the data packets
        for (int i = 0; i < controlData.length; i++) {
            protocol.addResponse(null, incrementingSequence);
        }

        OutputStream outputStream = session.getOutputStream();
        for (byte b : controlData) {
            outputStream.write(b);
            outputStream.flush();
        }

        protocol.verifyAll();

    }

    /**
     * Test successive calls to the output stream flush() method.
     * 
     * @throws Exception should not happen
     */
    @Test
    public void shouldSendNothingOnSuccessiveCallsToFlush() throws Exception {
        byte[] controlData = new byte[blockSize * 3];

        InBandBytestreamSession session = new InBandBytestreamSession(connection, initBytestream,
                        initiatorJID);

        // verify the data packets
        protocol.addResponse(null, incrementingSequence);
        protocol.addResponse(null, incrementingSequence);
        protocol.addResponse(null, incrementingSequence);

        OutputStream outputStream = session.getOutputStream();
        outputStream.write(controlData);

        outputStream.flush();
        outputStream.flush();
        outputStream.flush();

        protocol.verifyAll();

    }

    /**
     * If a data stanza(/packet) is received out of order the session should be closed. See XEP-0047 Section
     * 2.2.
     * 
     * @throws Exception should not happen
     */
    @Test
    public void shouldSendCloseRequestIfInvalidSequenceReceived() throws Exception {
        // confirm close request
        IQ resultIQ = IBBPacketUtils.createResultIQ(initiatorJID, targetJID);
        protocol.addResponse(resultIQ, Verification.requestTypeSET,
                        Verification.correspondingSenderReceiver);

        // get IBB sessions data packet listener
        InBandBytestreamSession session = new InBandBytestreamSession(connection, initBytestream,
                        initiatorJID);
        InputStream inputStream = session.getInputStream();
        StanzaListener listener = Whitebox.getInternalState(inputStream, StanzaListener.class);

        // build invalid packet with out of order sequence
        String base64Data = Base64.encode("Data");
        DataPacketExtension dpe = new DataPacketExtension(sessionID, 123, base64Data);
        Message dataMessage = new Message();
        dataMessage.addExtension(dpe);

        // add data packets
        listener.processStanza(dataMessage);

        // read until exception is thrown
        try {
            inputStream.read();
            fail("exception should be thrown");
        }
        catch (IOException e) {
            assertTrue(e.getMessage().contains("Packets out of sequence"));
        }

        protocol.verifyAll();

    }

    /**
     * Test the input stream read(byte[], int, int) method.
     * 
     * @throws Exception should not happen
     */
    @Test
    public void shouldReadAllReceivedData1() throws Exception {
        // create random data
        Random rand = new Random();
        byte[] controlData = new byte[3 * blockSize];
        rand.nextBytes(controlData);

        // get IBB sessions data packet listener
        InBandBytestreamSession session = new InBandBytestreamSession(connection, initBytestream,
                        initiatorJID);
        InputStream inputStream = session.getInputStream();
        StanzaListener listener = Whitebox.getInternalState(inputStream, StanzaListener.class);

        // verify data packet and notify listener
        for (int i = 0; i < controlData.length / blockSize; i++) {
            String base64Data = Base64.encodeToString(controlData, i * blockSize, blockSize);
            DataPacketExtension dpe = new DataPacketExtension(sessionID, i, base64Data);
            Message dataMessage = new Message();
            dataMessage.addExtension(dpe);
            listener.processStanza(dataMessage);
        }

        byte[] bytes = new byte[3 * blockSize];
        int read = 0;
        read = inputStream.read(bytes, 0, blockSize);
        assertEquals(blockSize, read);
        read = inputStream.read(bytes, 10, blockSize);
        assertEquals(blockSize, read);
        read = inputStream.read(bytes, 20, blockSize);
        assertEquals(blockSize, read);

        // verify data
        for (int i = 0; i < bytes.length; i++) {
            assertEquals(controlData[i], bytes[i]);
        }

        protocol.verifyAll();

    }

    /**
     * Test the input stream read() method.
     * 
     * @throws Exception should not happen
     */
    @Test
    public void shouldReadAllReceivedData2() throws Exception {
        // create random data
        Random rand = new Random();
        byte[] controlData = new byte[3 * blockSize];
        rand.nextBytes(controlData);

        // get IBB sessions data packet listener
        InBandBytestreamSession session = new InBandBytestreamSession(connection, initBytestream,
                        initiatorJID);
        InputStream inputStream = session.getInputStream();
        StanzaListener listener = Whitebox.getInternalState(inputStream, StanzaListener.class);

        // verify data packet and notify listener
        for (int i = 0; i < controlData.length / blockSize; i++) {
            String base64Data = Base64.encodeToString(controlData, i * blockSize, blockSize);
            DataPacketExtension dpe = new DataPacketExtension(sessionID, i, base64Data);
            Message dataMessage = new Message();
            dataMessage.addExtension(dpe);
            listener.processStanza(dataMessage);
        }

        // read data
        byte[] bytes = new byte[3 * blockSize];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) inputStream.read();
        }

        // verify data
        for (int i = 0; i < bytes.length; i++) {
            assertEquals(controlData[i], bytes[i]);
        }

        protocol.verifyAll();

    }

}
