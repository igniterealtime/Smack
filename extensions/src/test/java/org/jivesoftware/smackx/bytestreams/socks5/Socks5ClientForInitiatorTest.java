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

import static org.junit.Assert.*;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smack.packet.IQ.Type;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5Client;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5ClientForInitiator;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5Proxy;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5Utils;
import org.jivesoftware.smackx.bytestreams.socks5.packet.Bytestream;
import org.jivesoftware.smackx.bytestreams.socks5.packet.Bytestream.StreamHost;
import org.jivesoftware.util.ConnectionUtils;
import org.jivesoftware.util.Protocol;
import org.jivesoftware.util.Verification;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test for Socks5ClientForInitiator class.
 * 
 * @author Henning Staib
 */
public class Socks5ClientForInitiatorTest {

    // settings
    String initiatorJID = "initiator@xmpp-server/Smack";
    String targetJID = "target@xmpp-server/Smack";
    String xmppServer = "xmpp-server";
    String proxyJID = "proxy.xmpp-server";
    String proxyAddress = "127.0.0.1";
    int proxyPort = 7890;
    String sessionID = "session_id";

    // protocol verifier
    Protocol protocol;

    // mocked XMPP connection
    Connection connection;

    /**
     * Initialize fields used in the tests.
     */
    @Before
    public void setup() {

        // build protocol verifier
        protocol = new Protocol();

        // create mocked XMPP connection
        connection = ConnectionUtils.createMockedConnection(protocol, initiatorJID, xmppServer);

    }

    /**
     * If the target is not connected to the local SOCKS5 proxy an exception should be thrown.
     * 
     * @throws Exception should not happen
     */
    @Test
    public void shouldFailIfTargetIsNotConnectedToLocalSocks5Proxy() throws Exception {

        // start a local SOCKS5 proxy
        SmackConfiguration.setLocalSocks5ProxyPort(proxyPort);
        Socks5Proxy socks5Proxy = Socks5Proxy.getSocks5Proxy();
        socks5Proxy.start();

        // build stream host information for local SOCKS5 proxy
        StreamHost streamHost = new StreamHost(connection.getUser(),
                        socks5Proxy.getLocalAddresses().get(0));
        streamHost.setPort(socks5Proxy.getPort());

        // create digest to get the socket opened by target
        String digest = Socks5Utils.createDigest(sessionID, initiatorJID, targetJID);

        Socks5ClientForInitiator socks5Client = new Socks5ClientForInitiator(streamHost, digest,
                        connection, sessionID, targetJID);

        try {
            socks5Client.getSocket(10000);

            fail("exception should be thrown");
        }
        catch (XMPPException e) {
            assertTrue(e.getMessage().contains("target is not connected to SOCKS5 proxy"));
            protocol.verifyAll(); // assert no XMPP messages were sent
        }

        socks5Proxy.stop();

    }

    /**
     * Initiator and target should successfully connect to the local SOCKS5 proxy.
     * 
     * @throws Exception should not happen
     */
    @Test
    public void shouldSuccessfullyConnectThroughLocalSocks5Proxy() throws Exception {

        // start a local SOCKS5 proxy
        SmackConfiguration.setLocalSocks5ProxyPort(proxyPort);
        Socks5Proxy socks5Proxy = Socks5Proxy.getSocks5Proxy();
        socks5Proxy.start();

        // test data
        final byte[] data = new byte[] { 1, 2, 3 };

        // create digest
        final String digest = Socks5Utils.createDigest(sessionID, initiatorJID, targetJID);

        // allow connection of target with this digest
        socks5Proxy.addTransfer(digest);

        // build stream host information
        final StreamHost streamHost = new StreamHost(connection.getUser(),
                        socks5Proxy.getLocalAddresses().get(0));
        streamHost.setPort(socks5Proxy.getPort());

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
                    fail(e.getMessage());
                }
            }

        };
        targetThread.start();

        Thread.sleep(200);

        // initiator connects
        Socks5ClientForInitiator socks5Client = new Socks5ClientForInitiator(streamHost, digest,
                        connection, sessionID, targetJID);

        Socket socket = socks5Client.getSocket(10000);

        // verify test data
        InputStream in = socket.getInputStream();
        for (int i = 0; i < data.length; i++) {
            assertEquals(data[i], in.read());
        }

        targetThread.join();

        protocol.verifyAll(); // assert no XMPP messages were sent

        socks5Proxy.removeTransfer(digest);
        socks5Proxy.stop();

    }

    /**
     * If the initiator can connect to a SOCKS5 proxy but activating the stream fails an exception
     * should be thrown.
     * 
     * @throws Exception should not happen
     */
    @Test
    public void shouldFailIfActivateSocks5ProxyFails() throws Exception {

        // build error response as reply to the stream activation
        XMPPError xmppError = new XMPPError(XMPPError.Condition.interna_server_error);
        IQ error = new IQ() {

            public String getChildElementXML() {
                return null;
            }

        };
        error.setType(Type.ERROR);
        error.setFrom(proxyJID);
        error.setTo(initiatorJID);
        error.setError(xmppError);

        protocol.addResponse(error, Verification.correspondingSenderReceiver,
                        Verification.requestTypeSET);

        // start a local SOCKS5 proxy
        Socks5TestProxy socks5Proxy = Socks5TestProxy.getProxy(proxyPort);
        socks5Proxy.start();

        StreamHost streamHost = new StreamHost(proxyJID, socks5Proxy.getAddress());
        streamHost.setPort(socks5Proxy.getPort());

        // create digest to get the socket opened by target
        String digest = Socks5Utils.createDigest(sessionID, initiatorJID, targetJID);

        Socks5ClientForInitiator socks5Client = new Socks5ClientForInitiator(streamHost, digest,
                        connection, sessionID, targetJID);

        try {

            socks5Client.getSocket(10000);

            fail("exception should be thrown");
        }
        catch (XMPPException e) {
            assertTrue(e.getMessage().contains("activating SOCKS5 Bytestream failed"));
            protocol.verifyAll();
        }

        socks5Proxy.stop();
    }

    /**
     * Target and initiator should successfully connect to a "remote" SOCKS5 proxy and the initiator
     * activates the bytestream.
     * 
     * @throws Exception should not happen
     */
    @Test
    public void shouldSuccessfullyEstablishConnectionAndActivateSocks5Proxy() throws Exception {

        // build activation confirmation response
        IQ activationResponse = new IQ() {

            @Override
            public String getChildElementXML() {
                return null;
            }

        };
        activationResponse.setFrom(proxyJID);
        activationResponse.setTo(initiatorJID);
        activationResponse.setType(IQ.Type.RESULT);

        protocol.addResponse(activationResponse, Verification.correspondingSenderReceiver,
                        Verification.requestTypeSET, new Verification<Bytestream, IQ>() {

                            public void verify(Bytestream request, IQ response) {
                                // verify that the correct stream should be activated
                                assertNotNull(request.getToActivate());
                                assertEquals(targetJID, request.getToActivate().getTarget());
                            }

                        });

        // start a local SOCKS5 proxy
        Socks5TestProxy socks5Proxy = Socks5TestProxy.getProxy(proxyPort);
        socks5Proxy.start();

        StreamHost streamHost = new StreamHost(proxyJID, socks5Proxy.getAddress());
        streamHost.setPort(socks5Proxy.getPort());

        // create digest to get the socket opened by target
        String digest = Socks5Utils.createDigest(sessionID, initiatorJID, targetJID);

        Socks5ClientForInitiator socks5Client = new Socks5ClientForInitiator(streamHost, digest,
                        connection, sessionID, targetJID);

        Socket initiatorSocket = socks5Client.getSocket(10000);
        InputStream in = initiatorSocket.getInputStream();

        Socket targetSocket = socks5Proxy.getSocket(digest);
        OutputStream out = targetSocket.getOutputStream();

        // verify test data
        for (int i = 0; i < 10; i++) {
            out.write(i);
            assertEquals(i, in.read());
        }

        protocol.verifyAll();

        initiatorSocket.close();
        targetSocket.close();
        socks5Proxy.stop();

    }

    /**
     * Reset default port for local SOCKS5 proxy.
     */
    @After
    public void cleanup() {
        SmackConfiguration.setLocalSocks5ProxyPort(7777);
    }

}
