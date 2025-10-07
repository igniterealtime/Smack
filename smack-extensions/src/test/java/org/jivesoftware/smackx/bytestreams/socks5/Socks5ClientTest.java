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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.SmackException.SmackMessageException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.test.util.NetworkUtil;

import org.jivesoftware.smackx.bytestreams.socks5.packet.Bytestream.StreamHost;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jxmpp.jid.DomainBareJid;
import org.jxmpp.jid.JidTestUtil;

/**
 * Test for Socks5Client class.
 *
 * @author Henning Staib
 */
public class Socks5ClientTest {

    // settings
    private int serverPort;
    private String serverAddress;
    private DomainBareJid proxyJID = JidTestUtil.MUC_EXAMPLE_ORG;
    private String digest = "digest";
    private ServerSocket serverSocket;

    /**
     * Initialize fields used in the tests.
     *
     * @throws Exception should not happen
     */
    @BeforeEach
    public void setup() throws Exception {
        // create SOCKS5 proxy server socket
        serverSocket = NetworkUtil.getSocketOnLoopback();
        serverAddress = serverSocket.getInetAddress().getHostAddress();
        serverPort = serverSocket.getLocalPort();
    }

    /**
     * A SOCKS5 client MUST close connection if server doesn't accept any of the given
     * authentication methods. (See RFC1928 Section 3)
     *
     * @throws Exception should not happen
     */
    @Test
    public void shouldCloseSocketIfServerDoesNotAcceptAuthenticationMethod() throws Exception {

        var exception = new AtomicReference<Exception>();
        // start thread to connect to SOCKS5 proxy
        Thread serverThread = new Thread() {

            @Override
            public void run() {
                StreamHost streamHost = new StreamHost(proxyJID, serverAddress, serverPort);

                Socks5Client socks5Client = new Socks5Client(streamHost, digest);

                try {
                    socks5Client.getSocket(10000);
                    // should not reach this, exception should be thrown
                }
                catch (Exception e) {
                    exception.set(e);
                }

            }

        };
        serverThread.start();

        // accept connection form client
        Socket socket = serverSocket.accept();
        DataInputStream in = new DataInputStream(socket.getInputStream());
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());

        // validate authentication request
        assertEquals((byte) 0x05, (byte) in.read()); // version
        assertEquals((byte) 0x01, (byte) in.read()); // number of supported auth methods
        assertEquals((byte) 0x00, (byte) in.read()); // no-authentication method

        // respond that no authentication method is accepted
        out.write(new byte[] { (byte) 0x05, (byte) 0xFF });
        out.flush();

        // wait for client to shutdown
        serverThread.join();

        var ioException = assertInstanceOf(IOException.class, exception.get());
        assertEquals("Remote SOCKS5 server responded with unexpected version: 5 -1. Should be 0x05 0x00.", ioException.getMessage());

        // assert socket is closed
        assertEquals(-1, in.read());

    }

    /**
     * The SOCKS5 client should close connection if server replies in an unsupported way.
     *
     * @throws Exception should not happen
     */
    @Test
    public void shouldCloseSocketIfServerRepliesInUnsupportedWay() throws Exception {

        var exception = new AtomicReference<Exception>();
        // start thread to connect to SOCKS5 proxy
        Thread serverThread = new Thread() {

            @Override
            public void run() {
                StreamHost streamHost = new StreamHost(proxyJID, serverAddress, serverPort);

                Socks5Client socks5Client = new Socks5Client(streamHost, digest);
                try {
                    socks5Client.getSocket(10000);
                    // should not reach this, exception should be thrown
                }
                catch (Exception e) {
                    exception.set(e);
                }

            }

        };
        serverThread.start();

        // accept connection from client
        Socket socket = serverSocket.accept();
        DataInputStream in = new DataInputStream(socket.getInputStream());
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());

        // validate authentication request
        assertEquals((byte) 0x05, (byte) in.read()); // version
        assertEquals((byte) 0x01, (byte) in.read()); // number of supported auth methods
        assertEquals((byte) 0x00, (byte) in.read()); // no-authentication method

        // respond that no no-authentication method is used
        out.write(new byte[] { (byte) 0x05, (byte) 0x00 });
        out.flush();

        Socks5Utils.receiveSocks5Message(in);

        // reply with unsupported address type
        out.write(new byte[] { (byte) 0x05, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x00 });
        out.flush();

        // wait for client to shutdown
        serverThread.join();

        var ioException = assertInstanceOf(IOException.class, exception.get());
        assertEquals("Unsupported SOCKS5 address type: 1 (expected: 0x03)", ioException.getMessage());

        // assert socket is closed
        assertEquals(-1, in.read());

    }

    /**
     * The SOCKS5 client should close connection if server replies with an error.
     *
     * @throws Exception should not happen
     */
    @Test
    public void shouldCloseSocketIfServerRepliesWithError() throws Exception {

        var exception = new AtomicReference<Exception>();
        // start thread to connect to SOCKS5 proxy
        Thread serverThread = new Thread() {

            @Override
            public void run() {
                StreamHost streamHost = new StreamHost(proxyJID, serverAddress, serverPort);

                Socks5Client socks5Client = new Socks5Client(streamHost, digest);
                try {
                    socks5Client.getSocket(10000);
                    // should not reach this, exception should be thrown
                }
                catch (Exception e) {
                    exception.set(e);
                }

            }

        };
        serverThread.start();

        Socket socket = serverSocket.accept();
        DataInputStream in = new DataInputStream(socket.getInputStream());
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());

        // validate authentication request
        assertEquals((byte) 0x05, (byte) in.read()); // version
        assertEquals((byte) 0x01, (byte) in.read()); // number of supported auth methods
        assertEquals((byte) 0x00, (byte) in.read()); // no-authentication method

        // respond that no no-authentication method is used
        out.write(new byte[] { (byte) 0x05, (byte) 0x00 });
        out.flush();

        Socks5Utils.receiveSocks5Message(in);

        // reply with full SOCKS5 message with an error code (01 = general SOCKS server
        // failure)
        out.write(new byte[] { (byte) 0x05, (byte) 0x01, (byte) 0x00, (byte) 0x03 });
        byte[] address = digest.getBytes(StandardCharsets.UTF_8);
        out.write(address.length);
        out.write(address);
        out.write(new byte[] { (byte) 0x00, (byte) 0x00 });
        out.flush();

        // wait for client to shutdown
        serverThread.join();

        var ioException = assertInstanceOf(IOException.class, exception.get());
        assertEquals("Connection request does not equal connection response. Response: [5, 1, 0, 3, 6, 100, 105, 103, 101, 115, 116, 0, 0]. Request: [5, 0, 0, 3, 6, 100, 105, 103, 101, 115, 116, 0, 0]", ioException.getMessage());

        // assert socket is closed
        assertEquals(-1, in.read());

    }

    /**
     * The SOCKS5 client should successfully connect to the SOCKS5 server.
     *
     * @throws Exception should not happen
     */
    @Test
    public void shouldSuccessfullyConnectToSocks5Server() throws Exception {

        var clientFinished = new AtomicBoolean();
        // start thread to connect to SOCKS5 proxy
        Thread serverThread = new Thread() {

            @Override
            public void run() {
                StreamHost streamHost = new StreamHost(proxyJID, serverAddress, serverPort);

                Socks5Client socks5Client = new Socks5Client(streamHost, digest);

                try {
                    Socket socket = socks5Client.getSocket(10000);
                    assertNotNull(socket);
                    socket.getOutputStream().write(123);
                    socket.close();
                }
                catch (IOException | SmackMessageException | NotConnectedException | NoResponseException
                                | InterruptedException | TimeoutException | XMPPException e) {
                    throw new AssertionError(e);
                }
                clientFinished.set(true);
            }

        };
        serverThread.start();

        Socket socket = serverSocket.accept();
        DataInputStream in = new DataInputStream(socket.getInputStream());
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());

        // validate authentication request
        assertEquals((byte) 0x05, (byte) in.read()); // version
        assertEquals((byte) 0x01, (byte) in.read()); // number of supported auth methods
        assertEquals((byte) 0x00, (byte) in.read()); // no-authentication method

        // respond that no no-authentication method is used
        out.write(new byte[] { (byte) 0x05, (byte) 0x00 });
        out.flush();

        byte[] address = digest.getBytes(StandardCharsets.UTF_8);

        assertEquals((byte) 0x05, (byte) in.read()); // version
        assertEquals((byte) 0x01, (byte) in.read()); // connect request
        assertEquals((byte) 0x00, (byte) in.read()); // reserved byte (always 0)
        assertEquals((byte) 0x03, (byte) in.read()); // address type (domain)
        assertEquals(address.length, (byte) in.read()); // address length
        for (int i = 0; i < address.length; i++) {
            assertEquals(address[i], (byte) in.read()); // address
        }
        assertEquals((byte) 0x00, (byte) in.read()); // port
        assertEquals((byte) 0x00, (byte) in.read());

        // reply with success SOCKS5 message
        out.write(new byte[] { (byte) 0x05, (byte) 0x00, (byte) 0x00, (byte) 0x03 });
        out.write(address.length);
        out.write(address);
        out.write(new byte[] { (byte) 0x00, (byte) 0x00 });
        out.flush();

        // wait for client to shutdown
        serverThread.join();

        assertTrue(clientFinished.get());

        // verify data sent from client
        assertEquals(123, in.read());

        // assert socket is closed
        assertEquals(-1, in.read());

    }

    /**
     * Close fake SOCKS5 proxy.
     *
     * @throws Exception should not happen
     */
    @AfterEach
    public void cleanup() throws Exception {
        // Avoid NPE if serverSocket could not get created for whateve reason.
        if (serverSocket != null) {
            serverSocket.close();
        }
    }
}
