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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.packet.StanzaError;
import org.jivesoftware.smack.util.NetworkUtil;

import org.jivesoftware.smackx.bytestreams.socks5.packet.Bytestream;

import org.jivesoftware.util.ConnectionUtils;
import org.jivesoftware.util.Protocol;
import org.junit.Test;
import org.jxmpp.jid.DomainBareJid;
import org.jxmpp.jid.EntityFullJid;
import org.jxmpp.jid.JidTestUtil;
import org.jxmpp.jid.impl.JidCreate;

/**
 * Tests for the Socks5BytestreamRequest class.
 *
 * @author Henning Staib
 */
public class Socks5ByteStreamRequestTest {

    // settings
    private static final EntityFullJid initiatorJID = JidTestUtil.DUMMY_AT_EXAMPLE_ORG_SLASH_DUMMYRESOURCE;
    private static final EntityFullJid targetJID = JidTestUtil.FULL_JID_1_RESOURCE_1;
    private static final DomainBareJid proxyJID = JidTestUtil.MUC_EXAMPLE_ORG;
    private static final String proxyAddress = "127.0.0.1";
    private static final String sessionID = "session_id";

    /**
     * Accepting a SOCKS5 Bytestream request should fail if the request doesn't contain any Socks5
     * proxies.
     *
     * @throws Exception should not happen
     */
    @Test
    public void shouldFailIfRequestHasNoStreamHosts() throws Exception {
        final Protocol protocol = new Protocol();
        final XMPPConnection connection = ConnectionUtils.createMockedConnection(protocol, targetJID);

        assertThrows(Socks5Exception.NoSocks5StreamHostsProvided.class, () -> {
            // build SOCKS5 Bytestream initialization request with no SOCKS5 proxies
            Bytestream bytestreamInitialization = Socks5PacketUtils.createBytestreamInitiation(
                            initiatorJID, targetJID, sessionID);

            // get SOCKS5 Bytestream manager for connection
            Socks5BytestreamManager byteStreamManager = Socks5BytestreamManager.getBytestreamManager(connection);

            // build SOCKS5 Bytestream request with the bytestream initialization
            Socks5BytestreamRequest byteStreamRequest = new Socks5BytestreamRequest(
                            byteStreamManager, bytestreamInitialization);

            // accept the stream (this is the call that is tested here)
            byteStreamRequest.accept();
        });

        // verify targets response
        assertEquals(1, protocol.getRequests().size());
        Stanza targetResponse = protocol.getRequests().remove(0);
        assertTrue(IQ.class.isInstance(targetResponse));
        assertEquals(initiatorJID, targetResponse.getTo());
        assertEquals(IQ.Type.error, ((IQ) targetResponse).getType());
        assertEquals(StanzaError.Condition.item_not_found,
                        targetResponse.getError().getCondition());
    }

    /**
     * Accepting a SOCKS5 Bytestream request should fail if target is not able to connect to any of
     * the provided SOCKS5 proxies.
     *
     * @throws Exception
     */
    @Test
    public void shouldFailIfRequestHasInvalidStreamHosts() throws Exception {
        final Protocol protocol = new Protocol();
        final XMPPConnection connection = ConnectionUtils.createMockedConnection(protocol, targetJID);

        assertThrows(Socks5Exception.CouldNotConnectToAnyProvidedSocks5Host.class, () -> {
            // build SOCKS5 Bytestream initialization request
            Bytestream bytestreamInitialization = Socks5PacketUtils.createBytestreamInitiation(
                            initiatorJID, targetJID, sessionID);
            // add proxy that is not running
            bytestreamInitialization.addStreamHost(proxyJID, proxyAddress, 7778);

            // get SOCKS5 Bytestream manager for connection
            Socks5BytestreamManager byteStreamManager = Socks5BytestreamManager.getBytestreamManager(connection);

            // build SOCKS5 Bytestream request with the bytestream initialization
            Socks5BytestreamRequest byteStreamRequest = new Socks5BytestreamRequest(
                            byteStreamManager, bytestreamInitialization);

            // accept the stream (this is the call that is tested here)
            byteStreamRequest.accept();
        });

        // verify targets response
        assertEquals(1, protocol.getRequests().size());
        Stanza targetResponse = protocol.getRequests().remove(0);
        assertTrue(IQ.class.isInstance(targetResponse));
        assertEquals(initiatorJID, targetResponse.getTo());
        assertEquals(IQ.Type.error, ((IQ) targetResponse).getType());
        assertEquals(StanzaError.Condition.item_not_found,
                        targetResponse.getError().getCondition());
    }

    /**
     * Target should not try to connect to SOCKS5 proxies that already failed twice.
     *
     * @throws Exception should not happen
     */
    @Test
    public void shouldBlacklistInvalidProxyAfter2Failures() throws Exception {
        final Protocol protocol = new Protocol();
        final XMPPConnection connection = ConnectionUtils.createMockedConnection(protocol, targetJID);

        // build SOCKS5 Bytestream initialization request
        Bytestream bytestreamInitialization = Socks5PacketUtils.createBytestreamInitiation(
                        initiatorJID, targetJID, sessionID);
        // Add an unreachable stream host.
        bytestreamInitialization.addStreamHost(JidCreate.from("invalid." + proxyJID), "127.0.0.2", 7778);

        // get SOCKS5 Bytestream manager for connection
        Socks5BytestreamManager byteStreamManager = Socks5BytestreamManager.getBytestreamManager(connection);

        // try to connect several times
        for (int i = 0; i < 2; i++) {
            assertThrows(Socks5Exception.CouldNotConnectToAnyProvidedSocks5Host.class, () -> {
                // build SOCKS5 Bytestream request with the bytestream initialization
                Socks5BytestreamRequest byteStreamRequest = new Socks5BytestreamRequest(
                                byteStreamManager, bytestreamInitialization);

                // set timeouts
                byteStreamRequest.setTotalConnectTimeout(600);
                byteStreamRequest.setMinimumConnectTimeout(300);

                // accept the stream (this is the call that is tested here)
                byteStreamRequest.accept();
            });

            // verify targets response
            assertEquals(1, protocol.getRequests().size());
            Stanza targetResponse = protocol.getRequests().remove(0);
            assertTrue(IQ.class.isInstance(targetResponse));
            assertEquals(initiatorJID, targetResponse.getTo());
            assertEquals(IQ.Type.error, ((IQ) targetResponse).getType());
            assertEquals(StanzaError.Condition.item_not_found,
                            targetResponse.getError().getCondition());
        }

        // create test data for stream
        byte[] data = new byte[] { 1, 2, 3 };

        try (Socks5TestProxy socks5Proxy = new Socks5TestProxy()) {
            assertTrue(socks5Proxy.isRunning());

            // add a valid SOCKS5 proxy
            bytestreamInitialization.addStreamHost(proxyJID, proxyAddress, socks5Proxy.getPort());

            // build SOCKS5 Bytestream request with the bytestream initialization
            Socks5BytestreamRequest byteStreamRequest = new Socks5BytestreamRequest(byteStreamManager,
                            bytestreamInitialization);

            // set timeouts
            byteStreamRequest.setTotalConnectTimeout(600);
            byteStreamRequest.setMinimumConnectTimeout(300);

            // accept the stream (this is the call that is tested here)
            InputStream inputStream = byteStreamRequest.accept().getInputStream();

            // create digest to get the socket opened by target
            String digest = Socks5Utils.createDigest(sessionID, initiatorJID, targetJID);

            // test stream by sending some data
            OutputStream outputStream = socks5Proxy.getSocket(digest).getOutputStream();
            outputStream.write(data);

            // verify that data is transferred correctly
            byte[] result = new byte[3];
            inputStream.read(result);
            assertArrayEquals(data, result);

            // verify targets response
            assertEquals(1, protocol.getRequests().size());
            Stanza targetResponse = protocol.getRequests().remove(0);
            assertEquals(Bytestream.class, targetResponse.getClass());
            assertEquals(initiatorJID, targetResponse.getTo());
            assertEquals(IQ.Type.result, ((Bytestream) targetResponse).getType());
            assertEquals(proxyJID, ((Bytestream) targetResponse).getUsedHost().getJID());
        }
    }

    /**
     * Target should not not blacklist any SOCKS5 proxies regardless of failing connections.
     *
     * @throws Exception should not happen
     */
    @Test
    public void shouldNotBlacklistInvalidProxy() throws Exception {
        final Protocol protocol = new Protocol();
        final XMPPConnection connection = ConnectionUtils.createMockedConnection(protocol, targetJID);

        // build SOCKS5 Bytestream initialization request
        Bytestream bytestreamInitialization = Socks5PacketUtils.createBytestreamInitiation(
                        initiatorJID, targetJID, sessionID);
        bytestreamInitialization.addStreamHost(JidCreate.from("invalid." + proxyJID), "127.0.0.2", 7778);

        // get SOCKS5 Bytestream manager for connection
        Socks5BytestreamManager byteStreamManager = Socks5BytestreamManager.getBytestreamManager(connection);

        // try to connect several times
        for (int i = 0; i < 10; i++) {
            assertThrows(Socks5Exception.CouldNotConnectToAnyProvidedSocks5Host.class, () -> {
                // build SOCKS5 Bytestream request with the bytestream initialization
                Socks5BytestreamRequest byteStreamRequest = new Socks5BytestreamRequest(
                                byteStreamManager, bytestreamInitialization);

                // set timeouts
                byteStreamRequest.setTotalConnectTimeout(600);
                byteStreamRequest.setMinimumConnectTimeout(300);
                byteStreamRequest.setConnectFailureThreshold(0);

                // accept the stream (this is the call that is tested here)
                byteStreamRequest.accept();
            });

            // verify targets response
            assertEquals(1, protocol.getRequests().size());
            Stanza targetResponse = protocol.getRequests().remove(0);
            assertTrue(IQ.class.isInstance(targetResponse));
            assertEquals(initiatorJID, targetResponse.getTo());
            assertEquals(IQ.Type.error, ((IQ) targetResponse).getType());
            assertEquals(StanzaError.Condition.item_not_found,
                            targetResponse.getError().getCondition());
        }
    }

    /**
     * If the SOCKS5 Bytestream request contains multiple SOCKS5 proxies and the first one doesn't
     * respond, the connection attempt to this proxy should not consume the whole timeout for
     * connecting to the proxies.
     *
     * @throws Exception should not happen
     */
    @Test
    public void shouldNotTimeoutIfFirstSocks5ProxyDoesNotRespond() throws Exception {
        final Protocol protocol = new Protocol();
        final XMPPConnection connection = ConnectionUtils.createMockedConnection(protocol, targetJID);

        // start a local SOCKS5 proxy
        try (Socks5TestProxy socks5Proxy = new Socks5TestProxy()) {
            // create a fake SOCKS5 proxy that doesn't respond to a request
            ServerSocket unresponsiveSocks5Socket = NetworkUtil.getSocketOnLoopback();

            try {
                // build SOCKS5 Bytestream initialization request
                Bytestream bytestreamInitialization = Socks5PacketUtils.createBytestreamInitiation(
                                initiatorJID, targetJID, sessionID);
                bytestreamInitialization.addStreamHost(proxyJID, proxyAddress, unresponsiveSocks5Socket.getLocalPort());
                bytestreamInitialization.addStreamHost(proxyJID, proxyAddress, socks5Proxy.getPort());

                // create test data for stream
                byte[] data = new byte[] { 1, 2, 3 };

                // get SOCKS5 Bytestream manager for connection
                Socks5BytestreamManager byteStreamManager = Socks5BytestreamManager.getBytestreamManager(connection);

                // build SOCKS5 Bytestream request with the bytestream initialization
                Socks5BytestreamRequest byteStreamRequest = new Socks5BytestreamRequest(byteStreamManager,
                                bytestreamInitialization);

                // set timeouts
                byteStreamRequest.setTotalConnectTimeout(2000);
                byteStreamRequest.setMinimumConnectTimeout(1000);

                // accept the stream (this is the call that is tested here)
                InputStream inputStream = byteStreamRequest.accept().getInputStream();

                // assert that client tries to connect to dumb SOCKS5 proxy
                Socket socket = unresponsiveSocks5Socket.accept();
                assertNotNull(socket);

                // create digest to get the socket opened by target
                String digest = Socks5Utils.createDigest(sessionID, initiatorJID, targetJID);

                // test stream by sending some data
                OutputStream outputStream = socks5Proxy.getSocket(digest).getOutputStream();
                outputStream.write(data);

                // verify that data is transferred correctly
                byte[] result = new byte[3];
                inputStream.read(result);
                assertArrayEquals(data, result);

                // verify targets response
                assertEquals(1, protocol.getRequests().size());
                Stanza targetResponse = protocol.getRequests().remove(0);
                assertEquals(Bytestream.class, targetResponse.getClass());
                assertEquals(initiatorJID, targetResponse.getTo());
                assertEquals(IQ.Type.result, ((Bytestream) targetResponse).getType());
                assertEquals(proxyJID, ((Bytestream) targetResponse).getUsedHost().getJID());
            } finally {
                unresponsiveSocks5Socket.close();
            }
        }
    }

    /**
     * Accepting the SOCKS5 Bytestream request should be successfully.
     *
     * @throws Exception should not happen
     */
    @Test
    public void shouldAcceptSocks5BytestreamRequestAndReceiveData() throws Exception {
        final Protocol protocol = new Protocol();
        final XMPPConnection connection = ConnectionUtils.createMockedConnection(protocol, targetJID);

        // start a local SOCKS5 proxy
        try (Socks5TestProxy socks5Proxy = new Socks5TestProxy()) {
            // build SOCKS5 Bytestream initialization request
            Bytestream bytestreamInitialization = Socks5PacketUtils.createBytestreamInitiation(
                            initiatorJID, targetJID, sessionID);
            bytestreamInitialization.addStreamHost(proxyJID, proxyAddress, socks5Proxy.getPort());

            // create test data for stream
            byte[] data = new byte[] { 1, 2, 3 };

            // get SOCKS5 Bytestream manager for connection
            Socks5BytestreamManager byteStreamManager = Socks5BytestreamManager.getBytestreamManager(connection);

            // build SOCKS5 Bytestream request with the bytestream initialization
            Socks5BytestreamRequest byteStreamRequest = new Socks5BytestreamRequest(byteStreamManager,
                            bytestreamInitialization);

            // accept the stream (this is the call that is tested here)
            InputStream inputStream = byteStreamRequest.accept().getInputStream();

            // create digest to get the socket opened by target
            String digest = Socks5Utils.createDigest(sessionID, initiatorJID, targetJID);

            // test stream by sending some data
            OutputStream outputStream = socks5Proxy.getSocket(digest).getOutputStream();
            outputStream.write(data);

            // verify that data is transferred correctly
            byte[] result = new byte[3];
            inputStream.read(result);
            assertArrayEquals(data, result);

            // verify targets response
            assertEquals(1, protocol.getRequests().size());
            Stanza targetResponse = protocol.getRequests().remove(0);
            assertEquals(Bytestream.class, targetResponse.getClass());
            assertEquals(initiatorJID, targetResponse.getTo());
            assertEquals(IQ.Type.result, ((Bytestream) targetResponse).getType());
            assertEquals(proxyJID, ((Bytestream) targetResponse).getUsedHost().getJID());
        }
    }
}
