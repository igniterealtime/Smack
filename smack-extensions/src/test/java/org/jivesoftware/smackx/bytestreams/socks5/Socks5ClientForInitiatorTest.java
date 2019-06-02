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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.packet.EmptyResultIQ;
import org.jivesoftware.smack.packet.ErrorIQ;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.StanzaError;
import org.jivesoftware.smack.util.CloseableUtil;

import org.jivesoftware.smackx.bytestreams.socks5.packet.Bytestream;
import org.jivesoftware.smackx.bytestreams.socks5.packet.Bytestream.StreamHost;

import org.jivesoftware.util.ConnectionUtils;
import org.jivesoftware.util.Protocol;
import org.jivesoftware.util.Verification;
import org.junit.Test;
import org.jxmpp.jid.DomainBareJid;
import org.jxmpp.jid.EntityFullJid;
import org.jxmpp.jid.JidTestUtil;

/**
 * Test for Socks5ClientForInitiator class.
 *
 * @author Henning Staib
 */
public class Socks5ClientForInitiatorTest {

    // settings
    private static final EntityFullJid initiatorJID = JidTestUtil.DUMMY_AT_EXAMPLE_ORG_SLASH_DUMMYRESOURCE;
    private static final EntityFullJid targetJID = JidTestUtil.FULL_JID_1_RESOURCE_1;
    private static final DomainBareJid proxyJID = JidTestUtil.MUC_EXAMPLE_ORG;
    private static final String loopbackAddress = InetAddress.getLoopbackAddress().getHostAddress();

    private static final int GET_SOCKET_TIMEOUT = 90 * 1000;

    private static final String sessionID = "session_id";

    /**
     * If the target is not connected to the local SOCKS5 proxy an exception should be thrown.
     *
     * @throws Exception should not happen
     */
    @Test
    public void shouldFailIfTargetIsNotConnectedToLocalSocks5Proxy() throws Exception {
        Protocol protocol = new Protocol();
        XMPPConnection connection = ConnectionUtils.createMockedConnection(protocol, initiatorJID);

        // start a local SOCKS5 proxy
        Socks5Proxy socks5Proxy = new Socks5Proxy();
        socks5Proxy.start();
        try {
            // build stream host information for local SOCKS5 proxy
            StreamHost streamHost = new StreamHost(connection.getUser(),
                            loopbackAddress,
                            socks5Proxy.getPort());

            // create digest to get the socket opened by target
            String digest = Socks5Utils.createDigest(sessionID, initiatorJID, targetJID);

            Socks5ClientForInitiator socks5Client = new Socks5ClientForInitiator(streamHost, digest,
                            connection, sessionID, targetJID);

            try {
                socks5Client.getSocket(GET_SOCKET_TIMEOUT);

                fail("exception should be thrown");
            }
            catch (SmackException e) {
                assertTrue(e.getMessage().contains("target is not connected to SOCKS5 proxy"));
                protocol.verifyAll(); // assert no XMPP messages were sent
            }
        } finally {
            socks5Proxy.stop();
        }
    }

    /**
     * Initiator and target should successfully connect to the local SOCKS5 proxy.
     *
     * @throws Exception should not happen
     */
    @Test
    public void shouldSuccessfullyConnectThroughLocalSocks5Proxy() throws Exception {
        Protocol protocol = new Protocol();
        XMPPConnection connection = ConnectionUtils.createMockedConnection(protocol, initiatorJID);

        // start a local SOCKS5 proxy
        Socks5Proxy socks5Proxy = new Socks5Proxy();
        socks5Proxy.start();
        try {
            // test data
            final byte[] data = new byte[] { 1, 2, 3 };

            // create digest
            final String digest = Socks5Utils.createDigest(sessionID, initiatorJID, targetJID);

            // allow connection of target with this digest
            socks5Proxy.addTransfer(digest);

            // build stream host information
            final StreamHost streamHost = new StreamHost(connection.getUser(),
                            loopbackAddress,
                            socks5Proxy.getPort());

            // target connects to local SOCKS5 proxy
            Thread targetThread = new Thread() {
                @Override
                public void run() {
                    try {
                        Socks5Client targetClient = new Socks5Client(streamHost, digest);
                        Socket socket = targetClient.getSocket(10000);
                        socket.getOutputStream().write(data);
                    }
                    catch (Exception e) {
                        // TODO: This does not work.
                        fail(e.getMessage());
                    }
                }
            };
            targetThread.start();

            // TODO: Replace this Thread.sleep().
            Thread.sleep(200);

            // initiator connects
            Socks5ClientForInitiator socks5Client = new Socks5ClientForInitiator(streamHost, digest,
                            connection, sessionID, targetJID);

            Socket socket = socks5Client.getSocket(GET_SOCKET_TIMEOUT);

            // verify test data
            InputStream in = socket.getInputStream();
            for (int i = 0; i < data.length; i++) {
                assertEquals(data[i], in.read());
            }

            targetThread.join();

            protocol.verifyAll(); // assert no XMPP messages were sent

            socks5Proxy.removeTransfer(digest);
        } finally {
            socks5Proxy.stop();
        }
    }

    /**
     * If the initiator can connect to a SOCKS5 proxy but activating the stream fails an exception
     * should be thrown.
     *
     * @throws Exception should not happen
     */
    @Test
    public void shouldFailIfActivateSocks5ProxyFails() throws Exception {
        Protocol protocol = new Protocol();
        XMPPConnection connection = ConnectionUtils.createMockedConnection(protocol, initiatorJID);

        // build error response as reply to the stream activation
        IQ error = new ErrorIQ(StanzaError.getBuilder(StanzaError.Condition.internal_server_error));
        error.setFrom(proxyJID);
        error.setTo(initiatorJID);

        protocol.addResponse(error, Verification.correspondingSenderReceiver,
                        Verification.requestTypeSET);

        // start a local SOCKS5 proxy
        try (Socks5TestProxy socks5Proxy = new Socks5TestProxy()) {
            StreamHost streamHost = new StreamHost(proxyJID, loopbackAddress, socks5Proxy.getPort());

            // create digest to get the socket opened by target
            String digest = Socks5Utils.createDigest(sessionID, initiatorJID, targetJID);

            Socks5ClientForInitiator socks5Client = new Socks5ClientForInitiator(streamHost, digest, connection,
                            sessionID, targetJID);

            try {

                socks5Client.getSocket(GET_SOCKET_TIMEOUT);

                fail("exception should be thrown");
            } catch (XMPPErrorException e) {
                assertTrue(StanzaError.Condition.internal_server_error.equals(e.getStanzaError().getCondition()));
                protocol.verifyAll();
            }
        }
    }

    /**
     * Target and initiator should successfully connect to a "remote" SOCKS5 proxy and the initiator
     * activates the bytestream.
     *
     * @throws Exception should not happen
     */
    @Test
    public void shouldSuccessfullyEstablishConnectionAndActivateSocks5Proxy() throws Exception {
        Protocol protocol = new Protocol();
        XMPPConnection connection = ConnectionUtils.createMockedConnection(protocol, initiatorJID);

        // build activation confirmation response
        IQ activationResponse = new EmptyResultIQ();

        activationResponse.setFrom(proxyJID);
        activationResponse.setTo(initiatorJID);

        protocol.addResponse(activationResponse, Verification.correspondingSenderReceiver,
                        Verification.requestTypeSET, new Verification<Bytestream, IQ>() {

                            @Override
                            public void verify(Bytestream request, IQ response) {
                                // verify that the correct stream should be activated
                                assertNotNull(request.getToActivate());
                                assertEquals(targetJID, request.getToActivate().getTarget());
                            }

                        });

        Socket initiatorSocket = null, targetSocket = null;
        // start a local SOCKS5 proxy
        try (Socks5TestProxy socks5Proxy = new Socks5TestProxy()) {
            StreamHost streamHost = new StreamHost(proxyJID, loopbackAddress, socks5Proxy.getPort());

            // create digest to get the socket opened by target
            String digest = Socks5Utils.createDigest(sessionID, initiatorJID, targetJID);

            Socks5ClientForInitiator socks5Client = new Socks5ClientForInitiator(streamHost, digest, connection,
                            sessionID, targetJID);

            initiatorSocket = socks5Client.getSocket(10000);
            InputStream in = initiatorSocket.getInputStream();

            targetSocket = socks5Proxy.getSocket(digest);
            OutputStream out = targetSocket.getOutputStream();

            // verify test data
            for (int i = 0; i < 10; i++) {
                out.write(i);
                assertEquals(i, in.read());
            }

            protocol.verifyAll();
        } finally {
            CloseableUtil.maybeClose(initiatorSocket);
            CloseableUtil.maybeClose(targetSocket);
        }
    }

}
