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
package org.jivesoftware.smackx.bytestreams.ibb;

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.bytestreams.ibb.InBandBytestreamManager;
import org.jivesoftware.smackx.bytestreams.ibb.InBandBytestreamSession;
import org.jivesoftware.smackx.bytestreams.ibb.packet.Data;
import org.jivesoftware.smackx.bytestreams.ibb.packet.DataPacketExtension;
import org.jivesoftware.smackx.bytestreams.ibb.packet.Open;
import org.jivesoftware.util.ConnectionUtils;
import org.jivesoftware.util.Protocol;
import org.jivesoftware.util.Verification;
import org.junit.Before;
import org.junit.Test;
import org.powermock.reflect.Whitebox;

/**
 * Test for InBandBytestreamSession.
 * <p>
 * Tests the basic behavior of an In-Band Bytestream session along with sending data encapsulated in
 * IQ stanzas.
 * 
 * @author Henning Staib
 */
public class InBandBytestreamSessionTest {

    // settings
    String initiatorJID = "initiator@xmpp-server/Smack";
    String targetJID = "target@xmpp-server/Smack";
    String xmppServer = "xmpp-server";
    String sessionID = "session_id";

    int blockSize = 20;
    int dataSize = blockSize/4 * 3;

    // protocol verifier
    Protocol protocol;

    // mocked XMPP connection
    Connection connection;

    InBandBytestreamManager byteStreamManager;

    Open initBytestream;

    Verification<Data, IQ> incrementingSequence;

    /**
     * Initialize fields used in the tests.
     */
    @Before
    public void setup() {

        // build protocol verifier
        protocol = new Protocol();

        // create mocked XMPP connection
        connection = ConnectionUtils.createMockedConnection(protocol, initiatorJID, xmppServer);

        // initialize InBandBytestreamManager to get the InitiationListener
        byteStreamManager = InBandBytestreamManager.getByteStreamManager(connection);

        // create a In-Band Bytestream open packet
        initBytestream = new Open(sessionID, blockSize);
        initBytestream.setFrom(initiatorJID);
        initBytestream.setTo(targetJID);

        incrementingSequence = new Verification<Data, IQ>() {

            long lastSeq = 0;

            public void verify(Data request, IQ response) {
                assertEquals(lastSeq++, request.getDataPacketExtension().getSeq());
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

        // set acknowledgments for the data packets
        IQ resultIQ = IBBPacketUtils.createResultIQ(initiatorJID, targetJID);
        protocol.addResponse(resultIQ, incrementingSequence);
        protocol.addResponse(resultIQ, incrementingSequence);
        protocol.addResponse(resultIQ, incrementingSequence);

        byte[] controlData = new byte[dataSize * 3];

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

        // set acknowledgments for the data packets
        IQ resultIQ = IBBPacketUtils.createResultIQ(initiatorJID, targetJID);
        protocol.addResponse(resultIQ, incrementingSequence);
        protocol.addResponse(resultIQ, incrementingSequence);
        protocol.addResponse(resultIQ, incrementingSequence);

        byte[] controlData = new byte[dataSize * 3];

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

        // set acknowledgments for the data packets
        IQ resultIQ = IBBPacketUtils.createResultIQ(initiatorJID, targetJID);
        protocol.addResponse(resultIQ, incrementingSequence);
        protocol.addResponse(resultIQ, incrementingSequence);
        protocol.addResponse(resultIQ, incrementingSequence);

        byte[] controlData = new byte[(dataSize * 3) - 2];

        OutputStream outputStream = session.getOutputStream();
        int off = 0;
        for (int i = 1; i+off <= controlData.length; i++) {
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
        byte[] controlData = new byte[dataSize * 3];

        InBandBytestreamSession session = new InBandBytestreamSession(connection, initBytestream,
                        initiatorJID);

        // set acknowledgments for the data packets
        IQ resultIQ = IBBPacketUtils.createResultIQ(initiatorJID, targetJID);
        for (int i = 0; i < controlData.length; i++) {
            protocol.addResponse(resultIQ, incrementingSequence);
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
        byte[] controlData = new byte[dataSize * 3];

        InBandBytestreamSession session = new InBandBytestreamSession(connection, initBytestream,
                        initiatorJID);

        // set acknowledgments for the data packets
        IQ resultIQ = IBBPacketUtils.createResultIQ(initiatorJID, targetJID);
        protocol.addResponse(resultIQ, incrementingSequence);
        protocol.addResponse(resultIQ, incrementingSequence);
        protocol.addResponse(resultIQ, incrementingSequence);

        OutputStream outputStream = session.getOutputStream();
        outputStream.write(controlData);

        outputStream.flush();
        outputStream.flush();
        outputStream.flush();

        protocol.verifyAll();

    }

    /**
     * Test that the data is correctly chunked.
     * 
     * @throws Exception should not happen
     */
    @Test
    public void shouldSendDataCorrectly() throws Exception {
        // create random data
        Random rand = new Random();
        final byte[] controlData = new byte[256 * dataSize];
        rand.nextBytes(controlData);

        // compares the data of each packet with the control data
        Verification<Data, IQ> dataVerification = new Verification<Data, IQ>() {

            public void verify(Data request, IQ response) {
                byte[] decodedData = request.getDataPacketExtension().getDecodedData();
                int seq = (int) request.getDataPacketExtension().getSeq();
                for (int i = 0; i < decodedData.length; i++) {
                    assertEquals(controlData[(seq * dataSize) + i], decodedData[i]);
                }
            }

        };

        // set acknowledgments for the data packets
        IQ resultIQ = IBBPacketUtils.createResultIQ(initiatorJID, targetJID);
        for (int i = 0; i < controlData.length / dataSize; i++) {
            protocol.addResponse(resultIQ, incrementingSequence, dataVerification);
        }

        InBandBytestreamSession session = new InBandBytestreamSession(connection, initBytestream,
                        initiatorJID);

        OutputStream outputStream = session.getOutputStream();
        outputStream.write(controlData);
        outputStream.flush();

        protocol.verifyAll();

    }

    /**
     * If the input stream is closed the output stream should not be closed as well.
     * 
     * @throws Exception should not happen
     */
    @Test
    public void shouldNotCloseBothStreamsIfOutputStreamIsClosed() throws Exception {

        InBandBytestreamSession session = new InBandBytestreamSession(connection, initBytestream,
                        initiatorJID);
        OutputStream outputStream = session.getOutputStream();
        outputStream.close();

        // verify data packet confirmation is of type RESULT
        protocol.addResponse(null, Verification.requestTypeRESULT);

        // insert data to read
        InputStream inputStream = session.getInputStream();
        PacketListener listener = Whitebox.getInternalState(inputStream, PacketListener.class);
        String base64Data = StringUtils.encodeBase64("Data");
        DataPacketExtension dpe = new DataPacketExtension(sessionID, 0, base64Data);
        Data data = new Data(dpe);
        listener.processPacket(data);

        // verify no packet send
        protocol.verifyAll();

        try {
            outputStream.flush();
            fail("should throw an exception");
        }
        catch (IOException e) {
            assertTrue(e.getMessage().contains("closed"));
        }

        assertTrue(inputStream.read() != 0);

    }

    /**
     * Valid data packets should be confirmed.
     * 
     * @throws Exception should not happen
     */
    @Test
    public void shouldConfirmReceivedDataPacket() throws Exception {
        // verify data packet confirmation is of type RESULT
        protocol.addResponse(null, Verification.requestTypeRESULT);

        InBandBytestreamSession session = new InBandBytestreamSession(connection, initBytestream,
                        initiatorJID);
        InputStream inputStream = session.getInputStream();
        PacketListener listener = Whitebox.getInternalState(inputStream, PacketListener.class);

        String base64Data = StringUtils.encodeBase64("Data");
        DataPacketExtension dpe = new DataPacketExtension(sessionID, 0, base64Data);
        Data data = new Data(dpe);

        listener.processPacket(data);

        protocol.verifyAll();

    }

    /**
     * If the data packet has a sequence that is already used an 'unexpected-request' error should
     * be returned. See XEP-0047 Section 2.2.
     * 
     * @throws Exception should not happen
     */
    @Test
    public void shouldReplyWithErrorIfAlreadyUsedSequenceIsReceived() throws Exception {
        // verify reply to first valid data packet is of type RESULT
        protocol.addResponse(null, Verification.requestTypeRESULT);

        // verify reply to invalid data packet is an error
        protocol.addResponse(null, Verification.requestTypeERROR, new Verification<IQ, IQ>() {

            public void verify(IQ request, IQ response) {
                assertEquals(XMPPError.Condition.unexpected_request.toString(),
                                request.getError().getCondition());
            }

        });

        // get IBB sessions data packet listener
        InBandBytestreamSession session = new InBandBytestreamSession(connection, initBytestream,
                        initiatorJID);
        InputStream inputStream = session.getInputStream();
        PacketListener listener = Whitebox.getInternalState(inputStream, PacketListener.class);

        // build data packets
        String base64Data = StringUtils.encodeBase64("Data");
        DataPacketExtension dpe = new DataPacketExtension(sessionID, 0, base64Data);
        Data data1 = new Data(dpe);
        Data data2 = new Data(dpe);

        // notify listener
        listener.processPacket(data1);
        listener.processPacket(data2);

        protocol.verifyAll();

    }

    /**
     * If the data packet contains invalid Base64 encoding an 'bad-request' error should be
     * returned. See XEP-0047 Section 2.2.
     * 
     * @throws Exception should not happen
     */
    @Test
    public void shouldReplyWithErrorIfDataIsInvalid() throws Exception {
        // verify reply to invalid data packet is an error
        protocol.addResponse(null, Verification.requestTypeERROR, new Verification<IQ, IQ>() {

            public void verify(IQ request, IQ response) {
                assertEquals(XMPPError.Condition.bad_request.toString(),
                                request.getError().getCondition());
            }

        });

        // get IBB sessions data packet listener
        InBandBytestreamSession session = new InBandBytestreamSession(connection, initBytestream,
                        initiatorJID);
        InputStream inputStream = session.getInputStream();
        PacketListener listener = Whitebox.getInternalState(inputStream, PacketListener.class);

        // build data packets
        DataPacketExtension dpe = new DataPacketExtension(sessionID, 0, "AA=BB");
        Data data = new Data(dpe);

        // notify listener
        listener.processPacket(data);

        protocol.verifyAll();

    }

    /**
     * If a data packet is received out of order the session should be closed. See XEP-0047 Section
     * 2.2.
     * 
     * @throws Exception should not happen
     */
    @Test
    public void shouldSendCloseRequestIfInvalidSequenceReceived() throws Exception {
        IQ resultIQ = IBBPacketUtils.createResultIQ(initiatorJID, targetJID);

        // confirm data packet with invalid sequence
        protocol.addResponse(resultIQ);

        // confirm close request
        protocol.addResponse(resultIQ, Verification.requestTypeSET,
                        Verification.correspondingSenderReceiver);

        // get IBB sessions data packet listener
        InBandBytestreamSession session = new InBandBytestreamSession(connection, initBytestream,
                        initiatorJID);
        InputStream inputStream = session.getInputStream();
        PacketListener listener = Whitebox.getInternalState(inputStream, PacketListener.class);

        // build invalid packet with out of order sequence
        String base64Data = StringUtils.encodeBase64("Data");
        DataPacketExtension dpe = new DataPacketExtension(sessionID, 123, base64Data);
        Data data = new Data(dpe);

        // add data packets
        listener.processPacket(data);

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
        byte[] controlData = new byte[3 * dataSize];
        rand.nextBytes(controlData);

        IQ resultIQ = IBBPacketUtils.createResultIQ(initiatorJID, targetJID);

        // get IBB sessions data packet listener
        InBandBytestreamSession session = new InBandBytestreamSession(connection, initBytestream,
                        initiatorJID);
        InputStream inputStream = session.getInputStream();
        PacketListener listener = Whitebox.getInternalState(inputStream, PacketListener.class);

        // set data packet acknowledgement and notify listener
        for (int i = 0; i < controlData.length / dataSize; i++) {
            protocol.addResponse(resultIQ);
            String base64Data = StringUtils.encodeBase64(controlData, i * dataSize, dataSize,
                            false);
            DataPacketExtension dpe = new DataPacketExtension(sessionID, i, base64Data);
            Data data = new Data(dpe);
            listener.processPacket(data);
        }

        byte[] bytes = new byte[3 * dataSize];
        int read = 0;
        read = inputStream.read(bytes, 0, dataSize);
        assertEquals(dataSize, read);
        read = inputStream.read(bytes, dataSize, dataSize);
        assertEquals(dataSize, read);
        read = inputStream.read(bytes, dataSize*2, dataSize);
        assertEquals(dataSize, read);

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
        byte[] controlData = new byte[3 * dataSize];
        rand.nextBytes(controlData);

        IQ resultIQ = IBBPacketUtils.createResultIQ(initiatorJID, targetJID);

        // get IBB sessions data packet listener
        InBandBytestreamSession session = new InBandBytestreamSession(connection, initBytestream,
                        initiatorJID);
        InputStream inputStream = session.getInputStream();
        PacketListener listener = Whitebox.getInternalState(inputStream, PacketListener.class);

        // set data packet acknowledgment and notify listener
        for (int i = 0; i < controlData.length / dataSize; i++) {
            protocol.addResponse(resultIQ);
            String base64Data = StringUtils.encodeBase64(controlData, i * dataSize, dataSize,
                            false);
            DataPacketExtension dpe = new DataPacketExtension(sessionID, i, base64Data);
            Data data = new Data(dpe);
            listener.processPacket(data);
        }

        // read data
        byte[] bytes = new byte[3 * dataSize];
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) inputStream.read();
        }

        // verify data
        for (int i = 0; i < bytes.length; i++) {
            assertEquals(controlData[i], bytes[i]);
        }

        protocol.verifyAll();

    }

    /**
     * If the output stream is closed the input stream should not be closed as well.
     * 
     * @throws Exception should not happen
     */
    @Test
    public void shouldNotCloseBothStreamsIfInputStreamIsClosed() throws Exception {
        // acknowledgment for data packet
        IQ resultIQ = IBBPacketUtils.createResultIQ(initiatorJID, targetJID);
        protocol.addResponse(resultIQ);

        // get IBB sessions data packet listener
        InBandBytestreamSession session = new InBandBytestreamSession(connection, initBytestream,
                        initiatorJID);
        InputStream inputStream = session.getInputStream();
        PacketListener listener = Whitebox.getInternalState(inputStream, PacketListener.class);

        // build data packet
        String base64Data = StringUtils.encodeBase64("Data");
        DataPacketExtension dpe = new DataPacketExtension(sessionID, 0, base64Data);
        Data data = new Data(dpe);

        // add data packets
        listener.processPacket(data);

        inputStream.close();

        protocol.verifyAll();

        try {
            while (inputStream.read() != -1) {
            }
            inputStream.read();
            fail("should throw an exception");
        }
        catch (IOException e) {
            assertTrue(e.getMessage().contains("closed"));
        }

        session.getOutputStream().flush();

    }

    /**
     * If the session is closed the input stream and output stream should be closed as well.
     * 
     * @throws Exception should not happen
     */
    @Test
    public void shouldCloseBothStreamsIfSessionIsClosed() throws Exception {
        // acknowledgment for data packet
        IQ resultIQ = IBBPacketUtils.createResultIQ(initiatorJID, targetJID);
        protocol.addResponse(resultIQ);

        // acknowledgment for close request
        protocol.addResponse(resultIQ, Verification.correspondingSenderReceiver,
                        Verification.requestTypeSET);

        // get IBB sessions data packet listener
        InBandBytestreamSession session = new InBandBytestreamSession(connection, initBytestream,
                        initiatorJID);
        InputStream inputStream = session.getInputStream();
        PacketListener listener = Whitebox.getInternalState(inputStream, PacketListener.class);

        // build data packet
        String base64Data = StringUtils.encodeBase64("Data");
        DataPacketExtension dpe = new DataPacketExtension(sessionID, 0, base64Data);
        Data data = new Data(dpe);

        // add data packets
        listener.processPacket(data);

        session.close();

        protocol.verifyAll();

        try {
            while (inputStream.read() != -1) {
            }
            inputStream.read();
            fail("should throw an exception");
        }
        catch (IOException e) {
            assertTrue(e.getMessage().contains("closed"));
        }

        try {
            session.getOutputStream().flush();
            fail("should throw an exception");
        }
        catch (IOException e) {
            assertTrue(e.getMessage().contains("closed"));
        }

    }

    /**
     * If the input stream is closed concurrently there should be no deadlock.
     * 
     * @throws Exception should not happen
     */
    @Test
    public void shouldNotDeadlockIfInputStreamIsClosed() throws Exception {
        // acknowledgment for data packet
        IQ resultIQ = IBBPacketUtils.createResultIQ(initiatorJID, targetJID);
        protocol.addResponse(resultIQ);

        // get IBB sessions data packet listener
        InBandBytestreamSession session = new InBandBytestreamSession(connection, initBytestream,
                        initiatorJID);
        final InputStream inputStream = session.getInputStream();
        PacketListener listener = Whitebox.getInternalState(inputStream, PacketListener.class);

        // build data packet
        String base64Data = StringUtils.encodeBase64("Data");
        DataPacketExtension dpe = new DataPacketExtension(sessionID, 0, base64Data);
        Data data = new Data(dpe);

        // add data packets
        listener.processPacket(data);

        Thread closer = new Thread(new Runnable() {

            public void run() {
                try {
                    Thread.sleep(200);
                    inputStream.close();
                }
                catch (Exception e) {
                    fail(e.getMessage());
                }
            }

        });
        closer.start();

        try {
            byte[] bytes = new byte[20];
            while (inputStream.read(bytes) != -1) {
            }
            inputStream.read();
            fail("should throw an exception");
        }
        catch (IOException e) {
            assertTrue(e.getMessage().contains("closed"));
        }

        protocol.verifyAll();

    }
    
}
