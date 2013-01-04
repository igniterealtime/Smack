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
package org.jivesoftware.smackx.bytestreams.socks5;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.smack.SmackConfiguration;
import org.junit.After;
import org.junit.Test;

/**
 * Test for Socks5Proxy class.
 * 
 * @author Henning Staib
 */
public class Socks5ProxyTest {

    /**
     * The SOCKS5 proxy should be a singleton used by all XMPP connections
     */
    @Test
    public void shouldBeASingleton() {
        SmackConfiguration.setLocalSocks5ProxyEnabled(false);

        Socks5Proxy proxy1 = Socks5Proxy.getSocks5Proxy();
        Socks5Proxy proxy2 = Socks5Proxy.getSocks5Proxy();

        assertNotNull(proxy1);
        assertNotNull(proxy2);
        assertSame(proxy1, proxy2);
    }

    /**
     * The SOCKS5 proxy should not be started if disabled by configuration.
     */
    @Test
    public void shouldNotBeRunningIfDisabled() {
        SmackConfiguration.setLocalSocks5ProxyEnabled(false);
        Socks5Proxy proxy = Socks5Proxy.getSocks5Proxy();
        assertFalse(proxy.isRunning());
    }

    /**
     * The SOCKS5 proxy should use a free port above the one configured.
     * 
     * @throws Exception should not happen
     */
    @Test
    public void shouldUseFreePortOnNegativeValues() throws Exception {
        SmackConfiguration.setLocalSocks5ProxyEnabled(false);
        Socks5Proxy proxy = Socks5Proxy.getSocks5Proxy();
        assertFalse(proxy.isRunning());

        ServerSocket serverSocket = new ServerSocket(0);
        SmackConfiguration.setLocalSocks5ProxyPort(-serverSocket.getLocalPort());

        proxy.start();

        assertTrue(proxy.isRunning());

        serverSocket.close();

        assertTrue(proxy.getPort() > serverSocket.getLocalPort());

    }

    /**
     * When inserting new network addresses to the proxy the order should remain in the order they
     * were inserted.
     */
    @Test
    public void shouldPreserveAddressOrderOnInsertions() {
        Socks5Proxy proxy = Socks5Proxy.getSocks5Proxy();
        List<String> addresses = new ArrayList<String>(proxy.getLocalAddresses());
        addresses.add("1");
        addresses.add("2");
        addresses.add("3");
        for (String address : addresses) {
            proxy.addLocalAddress(address);
        }

        List<String> localAddresses = proxy.getLocalAddresses();
        for (int i = 0; i < addresses.size(); i++) {
            assertEquals(addresses.get(i), localAddresses.get(i));
        }
    }

    /**
     * When replacing network addresses of the proxy the order should remain in the order if the
     * given list.
     */
    @Test
    public void shouldPreserveAddressOrderOnReplace() {
        Socks5Proxy proxy = Socks5Proxy.getSocks5Proxy();
        List<String> addresses = new ArrayList<String>(proxy.getLocalAddresses());
        addresses.add("1");
        addresses.add("2");
        addresses.add("3");

        proxy.replaceLocalAddresses(addresses);

        List<String> localAddresses = proxy.getLocalAddresses();
        for (int i = 0; i < addresses.size(); i++) {
            assertEquals(addresses.get(i), localAddresses.get(i));
        }
    }

    /**
     * Inserting the same address multiple times should not cause the proxy to return this address
     * multiple times.
     */
    @Test
    public void shouldNotReturnMultipleSameAddress() {
        Socks5Proxy proxy = Socks5Proxy.getSocks5Proxy();

        proxy.addLocalAddress("same");
        proxy.addLocalAddress("same");
        proxy.addLocalAddress("same");

        assertEquals(2, proxy.getLocalAddresses().size());
    }

    /**
     * There should be only one thread executing the SOCKS5 proxy process.
     */
    @Test
    public void shouldOnlyStartOneServerThread() {
        int threadCount = Thread.activeCount();

        SmackConfiguration.setLocalSocks5ProxyPort(7890);
        Socks5Proxy proxy = Socks5Proxy.getSocks5Proxy();
        proxy.start();

        assertTrue(proxy.isRunning());
        assertEquals(threadCount + 1, Thread.activeCount());

        proxy.start();

        assertTrue(proxy.isRunning());
        assertEquals(threadCount + 1, Thread.activeCount());

        proxy.stop();

        assertFalse(proxy.isRunning());
        assertEquals(threadCount, Thread.activeCount());

        proxy.start();

        assertTrue(proxy.isRunning());
        assertEquals(threadCount + 1, Thread.activeCount());

        proxy.stop();

    }

    /**
     * If the SOCKS5 proxy accepts a connection that is not a SOCKS5 connection it should close the
     * corresponding socket.
     * 
     * @throws Exception should not happen
     */
    @Test
    public void shouldCloseSocketIfNoSocks5Request() throws Exception {
        SmackConfiguration.setLocalSocks5ProxyPort(7890);
        Socks5Proxy proxy = Socks5Proxy.getSocks5Proxy();
        proxy.start();

        Socket socket = new Socket(proxy.getLocalAddresses().get(0), proxy.getPort());

        OutputStream out = socket.getOutputStream();
        out.write(new byte[] { 1, 2, 3 });

        int res;
        try {
            res = socket.getInputStream().read();
        } catch (SocketException e) {
            res = -1;
        }

        assertEquals(-1, res);

        proxy.stop();

    }

    /**
     * The SOCKS5 proxy should reply with an error message if no supported authentication methods
     * are given in the SOCKS5 request.
     * 
     * @throws Exception should not happen
     */
    @Test
    public void shouldRespondWithErrorIfNoSupportedAuthenticationMethod() throws Exception {
        SmackConfiguration.setLocalSocks5ProxyPort(7890);
        Socks5Proxy proxy = Socks5Proxy.getSocks5Proxy();
        proxy.start();

        Socket socket = new Socket(proxy.getLocalAddresses().get(0), proxy.getPort());

        OutputStream out = socket.getOutputStream();

        // request username/password-authentication
        out.write(new byte[] { (byte) 0x05, (byte) 0x01, (byte) 0x02 });

        InputStream in = socket.getInputStream();

        assertEquals((byte) 0x05, (byte) in.read());
        assertEquals((byte) 0xFF, (byte) in.read());

        assertEquals(-1, in.read());

        proxy.stop();

    }

    /**
     * The SOCKS5 proxy should respond with an error message if the client is not allowed to connect
     * with the proxy.
     * 
     * @throws Exception should not happen
     */
    @Test
    public void shouldRespondWithErrorIfConnectionIsNotAllowed() throws Exception {
        SmackConfiguration.setLocalSocks5ProxyPort(7890);
        Socks5Proxy proxy = Socks5Proxy.getSocks5Proxy();
        proxy.start();

        Socket socket = new Socket(proxy.getLocalAddresses().get(0), proxy.getPort());

        OutputStream out = socket.getOutputStream();
        out.write(new byte[] { (byte) 0x05, (byte) 0x01, (byte) 0x00 });

        InputStream in = socket.getInputStream();

        assertEquals((byte) 0x05, (byte) in.read());
        assertEquals((byte) 0x00, (byte) in.read());

        // send valid SOCKS5 message
        out.write(new byte[] { (byte) 0x05, (byte) 0x00, (byte) 0x00, (byte) 0x03, (byte) 0x01,
                        (byte) 0xAA, (byte) 0x00, (byte) 0x00 });

        // verify error message
        assertEquals((byte) 0x05, (byte) in.read());
        assertFalse((byte) 0x00 == (byte) in.read()); // something other than 0 == success
        assertEquals((byte) 0x00, (byte) in.read());
        assertEquals((byte) 0x03, (byte) in.read());
        assertEquals((byte) 0x01, (byte) in.read());
        assertEquals((byte) 0xAA, (byte) in.read());
        assertEquals((byte) 0x00, (byte) in.read());
        assertEquals((byte) 0x00, (byte) in.read());

        assertEquals(-1, in.read());

        proxy.stop();

    }

    /**
     * A Client should successfully establish a connection to the SOCKS5 proxy.
     * 
     * @throws Exception should not happen
     */
    @Test
    public void shouldSuccessfullyEstablishConnection() throws Exception {
        SmackConfiguration.setLocalSocks5ProxyPort(7890);
        Socks5Proxy proxy = Socks5Proxy.getSocks5Proxy();
        proxy.start();

        assertTrue(proxy.isRunning());

        String digest = new String(new byte[] { (byte) 0xAA });

        // add digest to allow connection
        proxy.addTransfer(digest);

        Socket socket = new Socket(proxy.getLocalAddresses().get(0), proxy.getPort());

        OutputStream out = socket.getOutputStream();
        out.write(new byte[] { (byte) 0x05, (byte) 0x01, (byte) 0x00 });

        InputStream in = socket.getInputStream();

        assertEquals((byte) 0x05, (byte) in.read());
        assertEquals((byte) 0x00, (byte) in.read());

        // send valid SOCKS5 message
        out.write(new byte[] { (byte) 0x05, (byte) 0x00, (byte) 0x00, (byte) 0x03, (byte) 0x01,
                        (byte) 0xAA, (byte) 0x00, (byte) 0x00 });

        // verify response
        assertEquals((byte) 0x05, (byte) in.read());
        assertEquals((byte) 0x00, (byte) in.read()); // success
        assertEquals((byte) 0x00, (byte) in.read());
        assertEquals((byte) 0x03, (byte) in.read());
        assertEquals((byte) 0x01, (byte) in.read());
        assertEquals((byte) 0xAA, (byte) in.read());
        assertEquals((byte) 0x00, (byte) in.read());
        assertEquals((byte) 0x00, (byte) in.read());

        Thread.sleep(200);

        Socket remoteSocket = proxy.getSocket(digest);

        // remove digest
        proxy.removeTransfer(digest);

        // test stream
        OutputStream remoteOut = remoteSocket.getOutputStream();
        byte[] data = new byte[] { 1, 2, 3, 4, 5 };
        remoteOut.write(data);
        remoteOut.flush();

        for (int i = 0; i < data.length; i++) {
            assertEquals(data[i], in.read());
        }

        remoteSocket.close();

        assertEquals(-1, in.read());

        proxy.stop();

    }

    /**
     * Reset SOCKS5 proxy settings.
     */
    @After
    public void cleanup() {
        SmackConfiguration.setLocalSocks5ProxyEnabled(true);
        SmackConfiguration.setLocalSocks5ProxyPort(7777);
        Socks5Proxy socks5Proxy = Socks5Proxy.getSocks5Proxy();
        try {
            String address = InetAddress.getLocalHost().getHostAddress();
            List<String> addresses = new ArrayList<String>();
            addresses.add(address);
            socks5Proxy.replaceLocalAddresses(addresses);
        }
        catch (UnknownHostException e) {
            // ignore
        }

        socks5Proxy.stop();
    }

}
