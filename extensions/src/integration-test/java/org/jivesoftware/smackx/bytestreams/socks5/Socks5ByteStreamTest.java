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
package org.jivesoftware.smackx.bytestreams.socks5;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.FutureTask;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.PacketIDFilter;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smack.test.SmackTestCase;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5BytestreamListener;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5BytestreamManager;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5BytestreamRequest;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5BytestreamSession;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5PacketUtils;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5Proxy;
import org.jivesoftware.smackx.bytestreams.socks5.packet.Bytestream;

/**
 * Test for Socks5 bytestreams with real XMPP servers.
 * 
 * @author Henning Staib
 */
public class Socks5ByteStreamTest extends SmackTestCase {

    /**
     * Constructor
     * 
     * @param arg0
     */
    public Socks5ByteStreamTest(String arg0) {
        super(arg0);
    }

    /**
     * Socks5 feature should be added to the service discovery on Smack startup.
     * 
     * @throws XMPPException should not happen
     */
    public void testInitializationSocks5FeaturesAndListenerOnStartup() throws XMPPException {
        XMPPConnection connection = getConnection(0);

        assertTrue(ServiceDiscoveryManager.getInstanceFor(connection).includesFeature(
                        Socks5BytestreamManager.NAMESPACE));

    }

    /**
     * Target should respond with not-acceptable error if no listeners for incoming Socks5
     * bytestream requests are registered.
     * 
     * @throws XMPPException should not happen
     */
    public void testRespondWithErrorOnSocks5BytestreamRequest() throws XMPPException {
        XMPPConnection targetConnection = getConnection(0);

        XMPPConnection initiatorConnection = getConnection(1);

        Bytestream bytestreamInitiation = Socks5PacketUtils.createBytestreamInitiation(
                        initiatorConnection.getUser(), targetConnection.getUser(), "session_id");
        bytestreamInitiation.addStreamHost("proxy.localhost", "127.0.0.1", 7777);

        PacketCollector collector = initiatorConnection.createPacketCollector(new PacketIDFilter(
                        bytestreamInitiation.getPacketID()));
        initiatorConnection.sendPacket(bytestreamInitiation);
        Packet result = collector.nextResult();

        assertNotNull(result.getError());
        assertEquals(XMPPError.Condition.no_acceptable.toString(), result.getError().getCondition());

    }

    /**
     * Socks5 bytestream should be successfully established using the local Socks5 proxy.
     * 
     * @throws Exception should not happen
     */
    public void testSocks5BytestreamWithLocalSocks5Proxy() throws Exception {

        // setup port for local socks5 proxy
        SmackConfiguration.setLocalSocks5ProxyEnabled(true);
        SmackConfiguration.setLocalSocks5ProxyPort(7778);
        Socks5Proxy socks5Proxy = Socks5Proxy.getSocks5Proxy();
        socks5Proxy.start();

        assertTrue(socks5Proxy.isRunning());

        XMPPConnection initiatorConnection = getConnection(0);
        XMPPConnection targetConnection = getConnection(1);

        // test data
        final byte[] data = new byte[] { 1, 2, 3 };
        final SynchronousQueue<byte[]> queue = new SynchronousQueue<byte[]>();

        Socks5BytestreamManager targetByteStreamManager = Socks5BytestreamManager.getBytestreamManager(targetConnection);

        Socks5BytestreamListener incomingByteStreamListener = new Socks5BytestreamListener() {

            public void incomingBytestreamRequest(Socks5BytestreamRequest request) {
                InputStream inputStream;
                try {
                    Socks5BytestreamSession session = request.accept();
                    inputStream = session.getInputStream();
                    byte[] receivedData = new byte[3];
                    inputStream.read(receivedData);
                    queue.put(receivedData);
                }
                catch (Exception e) {
                    fail(e.getMessage());
                }
            }

        };
        targetByteStreamManager.addIncomingBytestreamListener(incomingByteStreamListener);

        Socks5BytestreamManager initiatorByteStreamManager = Socks5BytestreamManager.getBytestreamManager(initiatorConnection);

        Socks5BytestreamSession session = initiatorByteStreamManager.establishSession(
                        targetConnection.getUser());
        OutputStream outputStream = session.getOutputStream();
        
        assertTrue(session.isDirect());

        // verify stream
        outputStream.write(data);
        outputStream.flush();
        outputStream.close();

        assertEquals("received data not equal to sent data", data, queue.take());

        // reset default configuration
        SmackConfiguration.setLocalSocks5ProxyPort(7777);

    }

    /**
     * Socks5 bytestream should be successfully established using a Socks5 proxy provided by the
     * XMPP server.
     * <p>
     * This test will fail if the XMPP server doesn't provide any Socks5 proxies or the Socks5 proxy
     * only allows Socks5 bytestreams in the context of a file transfer (like Openfire in default
     * configuration, see xmpp.proxy.transfer.required flag).
     * 
     * @throws Exception if no Socks5 proxies found or proxy is unwilling to activate Socks5
     *         bytestream
     */
    public void testSocks5BytestreamWithRemoteSocks5Proxy() throws Exception {

        // disable local socks5 proxy
        SmackConfiguration.setLocalSocks5ProxyEnabled(false);
        Socks5Proxy.getSocks5Proxy().stop();

        assertFalse(Socks5Proxy.getSocks5Proxy().isRunning());

        XMPPConnection initiatorConnection = getConnection(0);
        XMPPConnection targetConnection = getConnection(1);

        // test data
        final byte[] data = new byte[] { 1, 2, 3 };
        final SynchronousQueue<byte[]> queue = new SynchronousQueue<byte[]>();

        Socks5BytestreamManager targetByteStreamManager = Socks5BytestreamManager.getBytestreamManager(targetConnection);

        Socks5BytestreamListener incomingByteStreamListener = new Socks5BytestreamListener() {

            public void incomingBytestreamRequest(Socks5BytestreamRequest request) {
                InputStream inputStream;
                try {
                    Socks5BytestreamSession session = request.accept();
                    inputStream = session.getInputStream();
                    byte[] receivedData = new byte[3];
                    inputStream.read(receivedData);
                    queue.put(receivedData);
                }
                catch (Exception e) {
                    fail(e.getMessage());
                }
            }

        };
        targetByteStreamManager.addIncomingBytestreamListener(incomingByteStreamListener);

        Socks5BytestreamManager initiatorByteStreamManager = Socks5BytestreamManager.getBytestreamManager(initiatorConnection);

        Socks5BytestreamSession session = initiatorByteStreamManager.establishSession(
                        targetConnection.getUser());
        OutputStream outputStream = session.getOutputStream();

        assertTrue(session.isMediated());

        // verify stream
        outputStream.write(data);
        outputStream.flush();
        outputStream.close();

        assertEquals("received data not equal to sent data", data, queue.take());

        // reset default configuration
        SmackConfiguration.setLocalSocks5ProxyEnabled(true);
        Socks5Proxy.getSocks5Proxy().start();

    }

    /**
     * Socks5 bytestream should be successfully established using a Socks5 proxy provided by the
     * XMPP server. The established connection should transfer data bidirectional if the Socks5
     * proxy supports it.
     * <p>
     * Support for bidirectional Socks5 bytestream:
     * <ul>
     * <li>Openfire (3.6.4 and below) - no</li>
     * <li>ejabberd (2.0.5 and higher) - yes</li>
     * </ul>
     * <p>
     * This test will fail if the XMPP server doesn't provide any Socks5 proxies or the Socks5 proxy
     * only allows Socks5 bytestreams in the context of a file transfer (like Openfire in default
     * configuration, see xmpp.proxy.transfer.required flag).
     * 
     * @throws Exception if no Socks5 proxies found or proxy is unwilling to activate Socks5
     *         bytestream
     */
    public void testBiDirectionalSocks5BytestreamWithRemoteSocks5Proxy() throws Exception {

        XMPPConnection initiatorConnection = getConnection(0);

        // disable local socks5 proxy
        SmackConfiguration.setLocalSocks5ProxyEnabled(false);
        Socks5Proxy.getSocks5Proxy().stop();

        assertFalse(Socks5Proxy.getSocks5Proxy().isRunning());

        XMPPConnection targetConnection = getConnection(1);

        // test data
        final byte[] data = new byte[] { 1, 2, 3 };
        final SynchronousQueue<byte[]> queue = new SynchronousQueue<byte[]>();

        Socks5BytestreamManager targetByteStreamManager = Socks5BytestreamManager.getBytestreamManager(targetConnection);

        Socks5BytestreamListener incomingByteStreamListener = new Socks5BytestreamListener() {

            public void incomingBytestreamRequest(Socks5BytestreamRequest request) {
                try {
                    Socks5BytestreamSession session = request.accept();
                    OutputStream outputStream = session.getOutputStream();
                    outputStream.write(data);
                    outputStream.flush();
                    InputStream inputStream = session.getInputStream();
                    byte[] receivedData = new byte[3];
                    inputStream.read(receivedData);
                    queue.put(receivedData);
                    session.close();
                }
                catch (Exception e) {
                    fail(e.getMessage());
                }
            }

        };
        targetByteStreamManager.addIncomingBytestreamListener(incomingByteStreamListener);

        Socks5BytestreamManager initiatorByteStreamManager = Socks5BytestreamManager.getBytestreamManager(initiatorConnection);

        Socks5BytestreamSession session = initiatorByteStreamManager.establishSession(targetConnection.getUser());
        
        assertTrue(session.isMediated());
        
        // verify stream
        final byte[] receivedData = new byte[3];
        final InputStream inputStream = session.getInputStream();

        FutureTask<Integer> futureTask = new FutureTask<Integer>(new Callable<Integer>() {

            public Integer call() throws Exception {
                return inputStream.read(receivedData);
            }
        });
        Thread executor = new Thread(futureTask);
        executor.start();

        try {
            futureTask.get(2000, TimeUnit.MILLISECONDS);
        }
        catch (TimeoutException e) {
            // reset default configuration
            SmackConfiguration.setLocalSocks5ProxyEnabled(true);
            Socks5Proxy.getSocks5Proxy().start();

            fail("Couldn't send data from target to inititator");
        }

        assertEquals("sent data not equal to received data", data, receivedData);

        OutputStream outputStream = session.getOutputStream();

        outputStream.write(data);
        outputStream.flush();
        outputStream.close();

        assertEquals("received data not equal to sent data", data, queue.take());

        session.close();

        // reset default configuration
        SmackConfiguration.setLocalSocks5ProxyEnabled(true);
        Socks5Proxy.getSocks5Proxy().start();

    }

    @Override
    protected int getMaxConnections() {
        return 2;
    }

}
