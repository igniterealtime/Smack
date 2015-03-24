/**
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

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;
import java.util.concurrent.SynchronousQueue;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketIDFilter;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smack.test.SmackTestCase;
import org.jivesoftware.smackx.bytestreams.ibb.InBandBytestreamListener;
import org.jivesoftware.smackx.bytestreams.ibb.InBandBytestreamManager;
import org.jivesoftware.smackx.bytestreams.ibb.InBandBytestreamRequest;
import org.jivesoftware.smackx.bytestreams.ibb.InBandBytestreamSession;
import org.jivesoftware.smackx.bytestreams.ibb.InBandBytestreamManager.StanzaType;
import org.jivesoftware.smackx.bytestreams.ibb.packet.Open;

/**
 * Test for In-Band Bytestreams with real XMPP servers.
 * 
 * @author Henning Staib
 */
public class InBandBytestreamTest extends SmackTestCase {

    /* the amount of data transmitted in each test */
    int dataSize = 1024000;

    public InBandBytestreamTest(String arg0) {
        super(arg0);
    }

    /**
     * Target should respond with not-acceptable error if no listeners for incoming In-Band
     * Bytestream requests are registered.
     * 
     * @throws XMPPException should not happen
     */
    public void testRespondWithErrorOnInBandBytestreamRequest() throws XMPPException {
        XMPPConnection targetConnection = getConnection(0);

        XMPPConnection initiatorConnection = getConnection(1);

        Open open = new Open("sessionID", 1024);
        open.setFrom(initiatorConnection.getUser());
        open.setTo(targetConnection.getUser());

        PacketCollector collector = initiatorConnection.createPacketCollector(new PacketIDFilter(
                        open.getStanzaId()));
        initiatorConnection.sendStanza(open);
        Packet result = collector.nextResult();

        assertNotNull(result.getError());
        assertEquals(XMPPError.Condition.no_acceptable.toString(), result.getError().getCondition());

    }

    /**
     * An In-Band Bytestream should be successfully established using IQ stanzas.
     * 
     * @throws Exception should not happen
     */
    public void testInBandBytestreamWithIQStanzas() throws Exception {

        XMPPConnection initiatorConnection = getConnection(0);
        XMPPConnection targetConnection = getConnection(1);

        // test data
        Random rand = new Random();
        final byte[] data = new byte[dataSize];
        rand.nextBytes(data);
        final SynchronousQueue<byte[]> queue = new SynchronousQueue<byte[]>();

        InBandBytestreamManager targetByteStreamManager = InBandBytestreamManager.getByteStreamManager(targetConnection);

        InBandBytestreamListener incomingByteStreamListener = new InBandBytestreamListener() {

            public void incomingBytestreamRequest(InBandBytestreamRequest request) {
                InputStream inputStream;
                try {
                    inputStream = request.accept().getInputStream();
                    byte[] receivedData = new byte[dataSize];
                    int totalRead = 0;
                    while (totalRead < dataSize) {
                        int read = inputStream.read(receivedData, totalRead, dataSize - totalRead);
                        totalRead += read;
                    }
                    queue.put(receivedData);
                }
                catch (Exception e) {
                    fail(e.getMessage());
                }
            }

        };
        targetByteStreamManager.addIncomingBytestreamListener(incomingByteStreamListener);

        InBandBytestreamManager initiatorByteStreamManager = InBandBytestreamManager.getByteStreamManager(initiatorConnection);

        OutputStream outputStream = initiatorByteStreamManager.establishSession(
                        targetConnection.getUser()).getOutputStream();

        // verify stream
        outputStream.write(data);
        outputStream.flush();
        outputStream.close();

        assertEquals("received data not equal to sent data", data, queue.take());

    }

    /**
     * An In-Band Bytestream should be successfully established using message stanzas.
     * 
     * @throws Exception should not happen
     */
    public void testInBandBytestreamWithMessageStanzas() throws Exception {

        XMPPConnection initiatorConnection = getConnection(0);
        XMPPConnection targetConnection = getConnection(1);

        // test data
        Random rand = new Random();
        final byte[] data = new byte[dataSize];
        rand.nextBytes(data);
        final SynchronousQueue<byte[]> queue = new SynchronousQueue<byte[]>();

        InBandBytestreamManager targetByteStreamManager = InBandBytestreamManager.getByteStreamManager(targetConnection);

        InBandBytestreamListener incomingByteStreamListener = new InBandBytestreamListener() {

            public void incomingBytestreamRequest(InBandBytestreamRequest request) {
                InputStream inputStream;
                try {
                    inputStream = request.accept().getInputStream();
                    byte[] receivedData = new byte[dataSize];
                    int totalRead = 0;
                    while (totalRead < dataSize) {
                        int read = inputStream.read(receivedData, totalRead, dataSize - totalRead);
                        totalRead += read;
                    }
                    queue.put(receivedData);
                }
                catch (Exception e) {
                    fail(e.getMessage());
                }
            }

        };
        targetByteStreamManager.addIncomingBytestreamListener(incomingByteStreamListener);

        InBandBytestreamManager initiatorByteStreamManager = InBandBytestreamManager.getByteStreamManager(initiatorConnection);
        initiatorByteStreamManager.setStanza(StanzaType.MESSAGE);

        OutputStream outputStream = initiatorByteStreamManager.establishSession(
                        targetConnection.getUser()).getOutputStream();

        // verify stream
        outputStream.write(data);
        outputStream.flush();
        outputStream.close();

        assertEquals("received data not equal to sent data", data, queue.take());

    }

    /**
     * An In-Band Bytestream should be successfully established using IQ stanzas. The established
     * session should transfer data bidirectional.
     * 
     * @throws Exception should not happen
     */
    public void testBiDirectionalInBandBytestream() throws Exception {

        XMPPConnection initiatorConnection = getConnection(0);

        XMPPConnection targetConnection = getConnection(1);

        // test data
        Random rand = new Random();
        final byte[] data = new byte[dataSize];
        rand.nextBytes(data);

        final SynchronousQueue<byte[]> queue = new SynchronousQueue<byte[]>();

        InBandBytestreamManager targetByteStreamManager = InBandBytestreamManager.getByteStreamManager(targetConnection);

        InBandBytestreamListener incomingByteStreamListener = new InBandBytestreamListener() {

            public void incomingBytestreamRequest(InBandBytestreamRequest request) {
                try {
                    InBandBytestreamSession session = request.accept();
                    OutputStream outputStream = session.getOutputStream();
                    outputStream.write(data);
                    outputStream.flush();
                    InputStream inputStream = session.getInputStream();
                    byte[] receivedData = new byte[dataSize];
                    int totalRead = 0;
                    while (totalRead < dataSize) {
                        int read = inputStream.read(receivedData, totalRead, dataSize - totalRead);
                        totalRead += read;
                    }
                    queue.put(receivedData);
                }
                catch (Exception e) {
                    fail(e.getMessage());
                }
            }

        };
        targetByteStreamManager.addIncomingBytestreamListener(incomingByteStreamListener);

        InBandBytestreamManager initiatorByteStreamManager = InBandBytestreamManager.getByteStreamManager(initiatorConnection);

        InBandBytestreamSession session = initiatorByteStreamManager.establishSession(targetConnection.getUser());

        // verify stream
        byte[] receivedData = new byte[dataSize];
        InputStream inputStream = session.getInputStream();
        int totalRead = 0;
        while (totalRead < dataSize) {
            int read = inputStream.read(receivedData, totalRead, dataSize - totalRead);
            totalRead += read;
        }

        assertEquals("sent data not equal to received data", data, receivedData);

        OutputStream outputStream = session.getOutputStream();

        outputStream.write(data);
        outputStream.flush();
        outputStream.close();

        assertEquals("received data not equal to sent data", data, queue.take());

    }

    @Override
    protected int getMaxConnections() {
        return 2;
    }

}
