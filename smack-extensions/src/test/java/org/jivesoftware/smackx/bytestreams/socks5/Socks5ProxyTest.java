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
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

import org.junit.Test;

/**
 * Test for Socks5Proxy class.
 *
 * @author Henning Staib
 */
public class Socks5ProxyTest {

    private static final String loopbackAddress = InetAddress.getLoopbackAddress().getHostAddress();

    /**
     * The SOCKS5 proxy should be a quasi singleton used by all XMPP connections.
     */
    @Test
    public void shouldBeAQuasiSingleton() {
        Socks5Proxy proxy1 = Socks5Proxy.getSocks5Proxy();
        Socks5Proxy proxy2 = Socks5Proxy.getSocks5Proxy();

        assertNotNull(proxy1);
        assertNotNull(proxy2);
        assertSame(proxy1, proxy2);
    }

    /**
     * The SOCKS5 proxy should use a free port above the one configured.
     *
     * @throws Exception should not happen
     */
    @Test
    public void shouldUseFreePortOnNegativeValues() throws Exception {
        Socks5Proxy proxy = new Socks5Proxy();
        assertFalse(proxy.isRunning());

        try (ServerSocket serverSocket = new ServerSocket(0)) {
            proxy.setLocalSocks5ProxyPort(-serverSocket.getLocalPort());

            proxy.start();

            assertTrue(proxy.isRunning());

            assertTrue(proxy.getPort() > serverSocket.getLocalPort());
        } finally {
            proxy.stop();
        }
    }

    /**
     * When inserting new network addresses to the proxy the order should remain in the order they
     * were inserted.
     *
     * @throws UnknownHostException
     */
    @Test
    public void shouldPreserveAddressOrderOnInsertions() throws UnknownHostException {
        Socks5Proxy proxy = Socks5Proxy.getSocks5Proxy();

        LinkedHashSet<InetAddress> addresses = new LinkedHashSet<>(proxy.getLocalAddresses());

        for (int i = 1 ; i <= 3; i++) {
            addresses.add(InetAddress.getByName(Integer.toString(i)));
        }

        for (InetAddress address : addresses) {
            proxy.addLocalAddress(address);
        }

        List<InetAddress> localAddresses = proxy.getLocalAddresses();

        Iterator<InetAddress> iterator = addresses.iterator();
        for (int i = 0; i < addresses.size(); i++) {
            assertEquals(iterator.next(), localAddresses.get(i));
        }
    }

    /**
     * When replacing network addresses of the proxy the order should remain in the order if the
     * given list.
     *
     * @throws UnknownHostException
     */
    @Test
    public void shouldPreserveAddressOrderOnReplace() throws UnknownHostException {
        Socks5Proxy proxy = Socks5Proxy.getSocks5Proxy();
        List<InetAddress> addresses = new ArrayList<>(proxy.getLocalAddresses());
        addresses.add(InetAddress.getByName("1"));
        addresses.add(InetAddress.getByName("2"));
        addresses.add(InetAddress.getByName("3"));

        proxy.replaceLocalAddresses(addresses);

        List<InetAddress> localAddresses = proxy.getLocalAddresses();
        for (int i = 0; i < addresses.size(); i++) {
            assertEquals(addresses.get(i), localAddresses.get(i));
        }
    }

    /**
     * If the SOCKS5 proxy accepts a connection that is not a SOCKS5 connection it should close the
     * corresponding socket.
     *
     * @throws Exception should not happen
     */
    @Test
    public void shouldCloseSocketIfNoSocks5Request() throws Exception {
        Socks5Proxy proxy = new Socks5Proxy();
        proxy.start();

        try (Socket socket = new Socket(loopbackAddress, proxy.getPort())) {
            OutputStream out = socket.getOutputStream();
            out.write(new byte[] { 1, 2, 3 });

            int res;
            try {
                res = socket.getInputStream().read();
            } catch (SocketException e) {
                res = -1;
            }

            assertEquals(-1, res);
        } finally {
            proxy.stop();
        }
    }

    /**
     * The SOCKS5 proxy should reply with an error message if no supported authentication methods
     * are given in the SOCKS5 request.
     *
     * @throws Exception should not happen
     */
    @Test
    public void shouldRespondWithErrorIfNoSupportedAuthenticationMethod() throws Exception {
        Socks5Proxy proxy = new Socks5Proxy();
        proxy.start();

        try (Socket socket = new Socket(loopbackAddress, proxy.getPort())) {
            OutputStream out = socket.getOutputStream();

            // request username/password-authentication
            out.write(new byte[] { (byte) 0x05, (byte) 0x01, (byte) 0x02 });

            InputStream in = socket.getInputStream();

            assertEquals((byte) 0x05, (byte) in.read());
            assertEquals((byte) 0xFF, (byte) in.read());

            assertEquals(-1, in.read());
        } finally {
            proxy.stop();
        }
    }

    /**
     * The SOCKS5 proxy should respond with an error message if the client is not allowed to connect
     * with the proxy.
     *
     * @throws Exception should not happen
     */
    @Test
    public void shouldRespondWithErrorIfConnectionIsNotAllowed() throws Exception {
        Socks5Proxy proxy = new Socks5Proxy();
        proxy.start();

        try (Socket socket = new Socket(loopbackAddress, proxy.getPort())) {
            OutputStream out = socket.getOutputStream();
            out.write(new byte[] { (byte) 0x05, (byte) 0x01, (byte) 0x00 });

            InputStream in = socket.getInputStream();

            assertEquals((byte) 0x05, (byte) in.read());
            assertEquals((byte) 0x00, (byte) in.read());

            // send valid SOCKS5 message
            out.write(new byte[] { (byte) 0x05, (byte) 0x00, (byte) 0x00, (byte) 0x03, (byte) 0x01, (byte) 0xAA,
                            (byte) 0x00, (byte) 0x00 });

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
        } finally {
            proxy.stop();
        }
    }

    /**
     * A Client should successfully establish a connection to the SOCKS5 proxy.
     *
     * @throws Exception should not happen
     */
    @Test
    public void shouldSuccessfullyEstablishConnection() throws Exception {
        Socks5Proxy proxy = new Socks5Proxy();
        proxy.start();

        try {
            assertTrue(proxy.isRunning());
            String digest = new String(new byte[] { (byte) 0xAA }, StandardCharsets.UTF_8);

            // add digest to allow connection
            proxy.addTransfer(digest);

            @SuppressWarnings("resource")
            Socket socket = new Socket(loopbackAddress, proxy.getPort());

            OutputStream out = socket.getOutputStream();
            out.write(new byte[] { (byte) 0x05, (byte) 0x01, (byte) 0x00 });

            InputStream in = socket.getInputStream();

            assertEquals((byte) 0x05, (byte) in.read());
            assertEquals((byte) 0x00, (byte) in.read());

            // send valid SOCKS5 message
            out.write(new byte[] { (byte) 0x05, (byte) 0x00, (byte) 0x00, (byte) 0x03, (byte) 0x01, (byte) 0xAA,
                            (byte) 0x00, (byte) 0x00 });

            // verify response
            assertEquals((byte) 0x05, (byte) in.read());
            assertEquals((byte) 0x00, (byte) in.read()); // success
            assertEquals((byte) 0x00, (byte) in.read());
            assertEquals((byte) 0x03, (byte) in.read());
            assertEquals((byte) 0x01, (byte) in.read());
            assertEquals((byte) 0xAA, (byte) in.read());
            assertEquals((byte) 0x00, (byte) in.read());
            assertEquals((byte) 0x00, (byte) in.read());

            Socket remoteSocket = proxy.getSocket(digest);

            try {
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
            } finally {
                remoteSocket.close();
            }

            assertEquals(-1, in.read());
        } finally {
            proxy.stop();
        }
    }
}
