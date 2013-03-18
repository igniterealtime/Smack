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
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smack.packet.IQ.Type;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5BytestreamManager;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5Client;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5Proxy;
import org.jivesoftware.smackx.bytestreams.socks5.Socks5Utils;
import org.jivesoftware.smackx.bytestreams.socks5.packet.Bytestream;
import org.jivesoftware.smackx.bytestreams.socks5.packet.Bytestream.StreamHost;
import org.jivesoftware.smackx.packet.DiscoverInfo;
import org.jivesoftware.smackx.packet.DiscoverItems;
import org.jivesoftware.smackx.packet.DiscoverInfo.Identity;
import org.jivesoftware.smackx.packet.DiscoverItems.Item;
import org.jivesoftware.util.ConnectionUtils;
import org.jivesoftware.util.Protocol;
import org.jivesoftware.util.Verification;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Test for Socks5BytestreamManager.
 * 
 * @author Henning Staib
 */
public class Socks5ByteStreamManagerTest {

    // settings
    String initiatorJID = "initiator@xmpp-server/Smack";
    String targetJID = "target@xmpp-server/Smack";
    String xmppServer = "xmpp-server";
    String proxyJID = "proxy.xmpp-server";
    String proxyAddress = "127.0.0.1";
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
     * Test that {@link Socks5BytestreamManager#getBytestreamManager(Connection)} returns one
     * bytestream manager for every connection
     */
    @Test
    public void shouldHaveOneManagerForEveryConnection() {

        // mock two connections
        Connection connection1 = mock(Connection.class);
        Connection connection2 = mock(Connection.class);

        /*
         * create service discovery managers for the connections because the
         * ConnectionCreationListener is not called when creating mocked connections
         */
        new ServiceDiscoveryManager(connection1);
        new ServiceDiscoveryManager(connection2);

        // get bytestream manager for the first connection twice
        Socks5BytestreamManager conn1ByteStreamManager1 = Socks5BytestreamManager.getBytestreamManager(connection1);
        Socks5BytestreamManager conn1ByteStreamManager2 = Socks5BytestreamManager.getBytestreamManager(connection1);

        // get bytestream manager for second connection
        Socks5BytestreamManager conn2ByteStreamManager1 = Socks5BytestreamManager.getBytestreamManager(connection2);

        // assertions
        assertEquals(conn1ByteStreamManager1, conn1ByteStreamManager2);
        assertNotSame(conn1ByteStreamManager1, conn2ByteStreamManager1);

    }

    /**
     * The SOCKS5 Bytestream feature should be removed form the service discovery manager if Socks5
     * bytestream feature is disabled.
     */
    @Test
    public void shouldDisableService() {
        Socks5BytestreamManager byteStreamManager = Socks5BytestreamManager.getBytestreamManager(connection);
        ServiceDiscoveryManager discoveryManager = ServiceDiscoveryManager.getInstanceFor(connection);

        assertTrue(discoveryManager.includesFeature(Socks5BytestreamManager.NAMESPACE));

        byteStreamManager.disableService();

        assertFalse(discoveryManager.includesFeature(Socks5BytestreamManager.NAMESPACE));
    }

    /**
     * Invoking {@link Socks5BytestreamManager#establishSession(String)} should throw an exception
     * if the given target does not support SOCKS5 Bytestream.
     */
    @Test
    public void shouldFailIfTargetDoesNotSupportSocks5() {
        Socks5BytestreamManager byteStreamManager = Socks5BytestreamManager.getBytestreamManager(connection);

        try {
            // build empty discover info as reply if targets features are queried
            DiscoverInfo discoverInfo = new DiscoverInfo();
            protocol.addResponse(discoverInfo);

            // start SOCKS5 Bytestream
            byteStreamManager.establishSession(targetJID);

            fail("exception should be thrown");
        }
        catch (XMPPException e) {
            assertTrue(e.getMessage().contains("doesn't support SOCKS5 Bytestream"));
        }
        catch (IOException e) {
            fail(e.getMessage());
        }
        catch (InterruptedException e) {
            fail(e.getMessage());
        }

    }

    /**
     * Invoking {@link Socks5BytestreamManager#establishSession(String, String)} should fail if XMPP
     * server doesn't return any proxies.
     */
    @Test
    public void shouldFailIfNoSocks5ProxyFound1() {

        // disable clients local SOCKS5 proxy
        SmackConfiguration.setLocalSocks5ProxyEnabled(false);

        // get Socks5ByteStreamManager for connection
        Socks5BytestreamManager byteStreamManager = Socks5BytestreamManager.getBytestreamManager(connection);

        /**
         * create responses in the order they should be queried specified by the XEP-0065
         * specification
         */

        // build discover info that supports the SOCKS5 feature
        DiscoverInfo discoverInfo = Socks5PacketUtils.createDiscoverInfo(targetJID, initiatorJID);
        discoverInfo.addFeature(Socks5BytestreamManager.NAMESPACE);

        // return that SOCKS5 is supported if target is queried
        protocol.addResponse(discoverInfo, Verification.correspondingSenderReceiver,
                        Verification.requestTypeGET);

        // build discover items with no proxy items
        DiscoverItems discoverItems = Socks5PacketUtils.createDiscoverItems(xmppServer,
                        initiatorJID);

        // return the item with no proxy if XMPP server is queried
        protocol.addResponse(discoverItems, Verification.correspondingSenderReceiver,
                        Verification.requestTypeGET);

        try {

            // start SOCKS5 Bytestream
            byteStreamManager.establishSession(targetJID, sessionID);

            fail("exception should be thrown");
        }
        catch (XMPPException e) {
            protocol.verifyAll();
            assertTrue(e.getMessage().contains("no SOCKS5 proxies available"));
        }
        catch (Exception e) {
            fail(e.getMessage());
        }

    }

    /**
     * Invoking {@link Socks5BytestreamManager#establishSession(String, String)} should fail if no
     * proxy is a SOCKS5 proxy.
     */
    @Test
    public void shouldFailIfNoSocks5ProxyFound2() {

        // disable clients local SOCKS5 proxy
        SmackConfiguration.setLocalSocks5ProxyEnabled(false);

        // get Socks5ByteStreamManager for connection
        Socks5BytestreamManager byteStreamManager = Socks5BytestreamManager.getBytestreamManager(connection);

        /**
         * create responses in the order they should be queried specified by the XEP-0065
         * specification
         */

        // build discover info that supports the SOCKS5 feature
        DiscoverInfo discoverInfo = Socks5PacketUtils.createDiscoverInfo(targetJID, initiatorJID);
        discoverInfo.addFeature(Socks5BytestreamManager.NAMESPACE);

        // return that SOCKS5 is supported if target is queried
        protocol.addResponse(discoverInfo, Verification.correspondingSenderReceiver,
                        Verification.requestTypeGET);

        // build discover items containing a proxy item
        DiscoverItems discoverItems = Socks5PacketUtils.createDiscoverItems(xmppServer,
                        initiatorJID);
        Item item = new Item(proxyJID);
        discoverItems.addItem(item);

        // return the proxy item if XMPP server is queried
        protocol.addResponse(discoverItems, Verification.correspondingSenderReceiver,
                        Verification.requestTypeGET);

        // build discover info for proxy containing information about NOT being a Socks5
        // proxy
        DiscoverInfo proxyInfo = Socks5PacketUtils.createDiscoverInfo(proxyJID, initiatorJID);
        Identity identity = new Identity("noproxy", proxyJID, "bytestreams");
        proxyInfo.addIdentity(identity);

        // return the proxy identity if proxy is queried
        protocol.addResponse(proxyInfo, Verification.correspondingSenderReceiver,
                        Verification.requestTypeGET);

        try {

            // start SOCKS5 Bytestream
            byteStreamManager.establishSession(targetJID, sessionID);

            fail("exception should be thrown");
        }
        catch (XMPPException e) {
            protocol.verifyAll();
            assertTrue(e.getMessage().contains("no SOCKS5 proxies available"));
        }
        catch (Exception e) {
            fail(e.getMessage());
        }

    }

    /**
     * Invoking {@link Socks5BytestreamManager#establishSession(String, String)} should fail if no
     * SOCKS5 proxy can be found. If it turns out that a proxy is not a SOCKS5 proxy it should not
     * be queried again.
     */
    @Test
    public void shouldBlacklistNonSocks5Proxies() {

        // disable clients local SOCKS5 proxy
        SmackConfiguration.setLocalSocks5ProxyEnabled(false);

        // get Socks5ByteStreamManager for connection
        Socks5BytestreamManager byteStreamManager = Socks5BytestreamManager.getBytestreamManager(connection);

        /**
         * create responses in the order they should be queried specified by the XEP-0065
         * specification
         */

        // build discover info that supports the SOCKS5 feature
        DiscoverInfo discoverInfo = Socks5PacketUtils.createDiscoverInfo(targetJID, initiatorJID);
        discoverInfo.addFeature(Socks5BytestreamManager.NAMESPACE);

        // return that SOCKS5 is supported if target is queried
        protocol.addResponse(discoverInfo, Verification.correspondingSenderReceiver,
                        Verification.requestTypeGET);

        // build discover items containing a proxy item
        DiscoverItems discoverItems = Socks5PacketUtils.createDiscoverItems(xmppServer,
                        initiatorJID);
        Item item = new Item(proxyJID);
        discoverItems.addItem(item);

        // return the proxy item if XMPP server is queried
        protocol.addResponse(discoverItems, Verification.correspondingSenderReceiver,
                        Verification.requestTypeGET);

        // build discover info for proxy containing information about NOT being a Socks5
        // proxy
        DiscoverInfo proxyInfo = Socks5PacketUtils.createDiscoverInfo(proxyJID, initiatorJID);
        Identity identity = new Identity("noproxy", proxyJID, "bytestreams");
        proxyInfo.addIdentity(identity);

        // return the proxy identity if proxy is queried
        protocol.addResponse(proxyInfo, Verification.correspondingSenderReceiver,
                        Verification.requestTypeGET);

        try {

            // start SOCKS5 Bytestream
            byteStreamManager.establishSession(targetJID, sessionID);

            fail("exception should be thrown");
        }
        catch (XMPPException e) {
            protocol.verifyAll();
            assertTrue(e.getMessage().contains("no SOCKS5 proxies available"));
        }
        catch (Exception e) {
            fail(e.getMessage());
        }

        /* retry to establish SOCKS5 Bytestream */

        // add responses for service discovery again
        protocol.addResponse(discoverInfo, Verification.correspondingSenderReceiver,
                        Verification.requestTypeGET);
        protocol.addResponse(discoverItems, Verification.correspondingSenderReceiver,
                        Verification.requestTypeGET);

        try {

            // start SOCKS5 Bytestream
            byteStreamManager.establishSession(targetJID, sessionID);

            fail("exception should be thrown");
        }
        catch (XMPPException e) {
            /*
             * #verifyAll() tests if the number of requests and responses corresponds and should
             * fail if the invalid proxy is queried again
             */
            protocol.verifyAll();
            assertTrue(e.getMessage().contains("no SOCKS5 proxies available"));
        }
        catch (Exception e) {
            fail(e.getMessage());
        }

    }

    /**
     * Invoking {@link Socks5BytestreamManager#establishSession(String, String)} should fail if the
     * target does not accept a SOCKS5 Bytestream. See <a
     * href="http://xmpp.org/extensions/xep-0065.html#usecase-alternate">XEP-0065 Section 5.2 A2</a>
     */
    @Test
    public void shouldFailIfTargetDoesNotAcceptSocks5Bytestream() {

        // disable clients local SOCKS5 proxy
        SmackConfiguration.setLocalSocks5ProxyEnabled(false);

        // get Socks5ByteStreamManager for connection
        Socks5BytestreamManager byteStreamManager = Socks5BytestreamManager.getBytestreamManager(connection);

        /**
         * create responses in the order they should be queried specified by the XEP-0065
         * specification
         */

        // build discover info that supports the SOCKS5 feature
        DiscoverInfo discoverInfo = Socks5PacketUtils.createDiscoverInfo(targetJID, initiatorJID);
        discoverInfo.addFeature(Socks5BytestreamManager.NAMESPACE);

        // return that SOCKS5 is supported if target is queried
        protocol.addResponse(discoverInfo, Verification.correspondingSenderReceiver,
                        Verification.requestTypeGET);

        // build discover items containing a proxy item
        DiscoverItems discoverItems = Socks5PacketUtils.createDiscoverItems(xmppServer,
                        initiatorJID);
        Item item = new Item(proxyJID);
        discoverItems.addItem(item);

        // return the proxy item if XMPP server is queried
        protocol.addResponse(discoverItems, Verification.correspondingSenderReceiver,
                        Verification.requestTypeGET);

        // build discover info for proxy containing information about being a SOCKS5 proxy
        DiscoverInfo proxyInfo = Socks5PacketUtils.createDiscoverInfo(proxyJID, initiatorJID);
        Identity identity = new Identity("proxy", proxyJID, "bytestreams");
        proxyInfo.addIdentity(identity);

        // return the socks5 bytestream proxy identity if proxy is queried
        protocol.addResponse(proxyInfo, Verification.correspondingSenderReceiver,
                        Verification.requestTypeGET);

        // build a socks5 stream host info containing the address and the port of the
        // proxy
        Bytestream streamHostInfo = Socks5PacketUtils.createBytestreamResponse(proxyJID,
                        initiatorJID);
        streamHostInfo.addStreamHost(proxyJID, proxyAddress, 7778);

        // return stream host info if it is queried
        protocol.addResponse(streamHostInfo, Verification.correspondingSenderReceiver,
                        Verification.requestTypeGET);

        // build error packet to reject SOCKS5 Bytestream
        XMPPError xmppError = new XMPPError(XMPPError.Condition.no_acceptable);
        IQ rejectPacket = new IQ() {

            public String getChildElementXML() {
                return null;
            }

        };
        rejectPacket.setType(Type.ERROR);
        rejectPacket.setFrom(targetJID);
        rejectPacket.setTo(initiatorJID);
        rejectPacket.setError(xmppError);

        // return error packet as response to the bytestream initiation
        protocol.addResponse(rejectPacket, Verification.correspondingSenderReceiver,
                        Verification.requestTypeSET);

        try {

            // start SOCKS5 Bytestream
            byteStreamManager.establishSession(targetJID, sessionID);

            fail("exception should be thrown");
        }
        catch (XMPPException e) {
            protocol.verifyAll();
            assertEquals(xmppError, e.getXMPPError());
        }
        catch (Exception e) {
            fail(e.getMessage());
        }

    }

    /**
     * Invoking {@link Socks5BytestreamManager#establishSession(String, String)} should fail if the
     * proxy used by target is invalid.
     */
    @Test
    public void shouldFailIfTargetUsesInvalidSocks5Proxy() {

        // disable clients local SOCKS5 proxy
        SmackConfiguration.setLocalSocks5ProxyEnabled(false);

        // get Socks5ByteStreamManager for connection
        Socks5BytestreamManager byteStreamManager = Socks5BytestreamManager.getBytestreamManager(connection);

        /**
         * create responses in the order they should be queried specified by the XEP-0065
         * specification
         */

        // build discover info that supports the SOCKS5 feature
        DiscoverInfo discoverInfo = Socks5PacketUtils.createDiscoverInfo(targetJID, initiatorJID);
        discoverInfo.addFeature(Socks5BytestreamManager.NAMESPACE);

        // return that SOCKS5 is supported if target is queried
        protocol.addResponse(discoverInfo, Verification.correspondingSenderReceiver,
                        Verification.requestTypeGET);

        // build discover items containing a proxy item
        DiscoverItems discoverItems = Socks5PacketUtils.createDiscoverItems(xmppServer,
                        initiatorJID);
        Item item = new Item(proxyJID);
        discoverItems.addItem(item);

        // return the proxy item if XMPP server is queried
        protocol.addResponse(discoverItems, Verification.correspondingSenderReceiver,
                        Verification.requestTypeGET);

        // build discover info for proxy containing information about being a SOCKS5 proxy
        DiscoverInfo proxyInfo = Socks5PacketUtils.createDiscoverInfo(proxyJID, initiatorJID);
        Identity identity = new Identity("proxy", proxyJID, "bytestreams");
        proxyInfo.addIdentity(identity);

        // return the socks5 bytestream proxy identity if proxy is queried
        protocol.addResponse(proxyInfo, Verification.correspondingSenderReceiver,
                        Verification.requestTypeGET);

        // build a socks5 stream host info containing the address and the port of the
        // proxy
        Bytestream streamHostInfo = Socks5PacketUtils.createBytestreamResponse(proxyJID,
                        initiatorJID);
        streamHostInfo.addStreamHost(proxyJID, proxyAddress, 7778);

        // return stream host info if it is queried
        protocol.addResponse(streamHostInfo, Verification.correspondingSenderReceiver,
                        Verification.requestTypeGET);

        // build used stream host response with unknown proxy
        Bytestream streamHostUsedPacket = Socks5PacketUtils.createBytestreamResponse(targetJID,
                        initiatorJID);
        streamHostUsedPacket.setSessionID(sessionID);
        streamHostUsedPacket.setUsedHost("invalid.proxy");

        // return used stream host info as response to the bytestream initiation
        protocol.addResponse(streamHostUsedPacket, Verification.correspondingSenderReceiver,
                        Verification.requestTypeSET);

        try {

            // start SOCKS5 Bytestream
            byteStreamManager.establishSession(targetJID, sessionID);

            fail("exception should be thrown");
        }
        catch (XMPPException e) {
            protocol.verifyAll();
            assertTrue(e.getMessage().contains("Remote user responded with unknown host"));
        }
        catch (Exception e) {
            fail(e.getMessage());
        }

    }

    /**
     * Invoking {@link Socks5BytestreamManager#establishSession(String, String)} should fail if
     * initiator can not connect to the SOCKS5 proxy used by target.
     */
    @Test
    public void shouldFailIfInitiatorCannotConnectToSocks5Proxy() {

        // disable clients local SOCKS5 proxy
        SmackConfiguration.setLocalSocks5ProxyEnabled(false);

        // get Socks5ByteStreamManager for connection
        Socks5BytestreamManager byteStreamManager = Socks5BytestreamManager.getBytestreamManager(connection);

        /**
         * create responses in the order they should be queried specified by the XEP-0065
         * specification
         */

        // build discover info that supports the SOCKS5 feature
        DiscoverInfo discoverInfo = Socks5PacketUtils.createDiscoverInfo(targetJID, initiatorJID);
        discoverInfo.addFeature(Socks5BytestreamManager.NAMESPACE);

        // return that SOCKS5 is supported if target is queried
        protocol.addResponse(discoverInfo, Verification.correspondingSenderReceiver,
                        Verification.requestTypeGET);

        // build discover items containing a proxy item
        DiscoverItems discoverItems = Socks5PacketUtils.createDiscoverItems(xmppServer,
                        initiatorJID);
        Item item = new Item(proxyJID);
        discoverItems.addItem(item);

        // return the proxy item if XMPP server is queried
        protocol.addResponse(discoverItems, Verification.correspondingSenderReceiver,
                        Verification.requestTypeGET);

        // build discover info for proxy containing information about being a SOCKS5 proxy
        DiscoverInfo proxyInfo = Socks5PacketUtils.createDiscoverInfo(proxyJID, initiatorJID);
        Identity identity = new Identity("proxy", proxyJID, "bytestreams");
        proxyInfo.addIdentity(identity);

        // return the socks5 bytestream proxy identity if proxy is queried
        protocol.addResponse(proxyInfo, Verification.correspondingSenderReceiver,
                        Verification.requestTypeGET);

        // build a socks5 stream host info containing the address and the port of the
        // proxy
        Bytestream streamHostInfo = Socks5PacketUtils.createBytestreamResponse(proxyJID,
                        initiatorJID);
        streamHostInfo.addStreamHost(proxyJID, proxyAddress, 7778);

        // return stream host info if it is queried
        protocol.addResponse(streamHostInfo, Verification.correspondingSenderReceiver,
                        Verification.requestTypeGET);

        // build used stream host response
        Bytestream streamHostUsedPacket = Socks5PacketUtils.createBytestreamResponse(targetJID,
                        initiatorJID);
        streamHostUsedPacket.setSessionID(sessionID);
        streamHostUsedPacket.setUsedHost(proxyJID);

        // return used stream host info as response to the bytestream initiation
        protocol.addResponse(streamHostUsedPacket, new Verification<Bytestream, Bytestream>() {

            public void verify(Bytestream request, Bytestream response) {
                // verify SOCKS5 Bytestream request
                assertEquals(response.getSessionID(), request.getSessionID());
                assertEquals(1, request.getStreamHosts().size());
                StreamHost streamHost = (StreamHost) request.getStreamHosts().toArray()[0];
                assertEquals(response.getUsedHost().getJID(), streamHost.getJID());
            }

        }, Verification.correspondingSenderReceiver, Verification.requestTypeSET);

        try {

            // start SOCKS5 Bytestream
            byteStreamManager.establishSession(targetJID, sessionID);

            fail("exception should be thrown");
        }
        catch (IOException e) {
            // initiator can't connect to proxy because it is not running
            protocol.verifyAll();
            assertEquals(ConnectException.class, e.getClass());
        }
        catch (Exception e) {
            fail(e.getMessage());
        }

    }

    /**
     * Invoking {@link Socks5BytestreamManager#establishSession(String, String)} should successfully
     * negotiate and return a SOCKS5 Bytestream connection.
     * 
     * @throws Exception should not happen
     */
    @Test
    public void shouldNegotiateSocks5BytestreamAndTransferData() throws Exception {

        // disable clients local SOCKS5 proxy
        SmackConfiguration.setLocalSocks5ProxyEnabled(false);

        // get Socks5ByteStreamManager for connection
        Socks5BytestreamManager byteStreamManager = Socks5BytestreamManager.getBytestreamManager(connection);

        /**
         * create responses in the order they should be queried specified by the XEP-0065
         * specification
         */

        // build discover info that supports the SOCKS5 feature
        DiscoverInfo discoverInfo = Socks5PacketUtils.createDiscoverInfo(targetJID, initiatorJID);
        discoverInfo.addFeature(Socks5BytestreamManager.NAMESPACE);

        // return that SOCKS5 is supported if target is queried
        protocol.addResponse(discoverInfo, Verification.correspondingSenderReceiver,
                        Verification.requestTypeGET);

        // build discover items containing a proxy item
        DiscoverItems discoverItems = Socks5PacketUtils.createDiscoverItems(xmppServer,
                        initiatorJID);
        Item item = new Item(proxyJID);
        discoverItems.addItem(item);

        // return the proxy item if XMPP server is queried
        protocol.addResponse(discoverItems, Verification.correspondingSenderReceiver,
                        Verification.requestTypeGET);

        // build discover info for proxy containing information about being a SOCKS5 proxy
        DiscoverInfo proxyInfo = Socks5PacketUtils.createDiscoverInfo(proxyJID, initiatorJID);
        Identity identity = new Identity("proxy", proxyJID, "bytestreams");
        proxyInfo.addIdentity(identity);

        // return the socks5 bytestream proxy identity if proxy is queried
        protocol.addResponse(proxyInfo, Verification.correspondingSenderReceiver,
                        Verification.requestTypeGET);

        // build a socks5 stream host info containing the address and the port of the
        // proxy
        Bytestream streamHostInfo = Socks5PacketUtils.createBytestreamResponse(proxyJID,
                        initiatorJID);
        streamHostInfo.addStreamHost(proxyJID, proxyAddress, 7778);

        // return stream host info if it is queried
        protocol.addResponse(streamHostInfo, Verification.correspondingSenderReceiver,
                        Verification.requestTypeGET);

        // build used stream host response
        Bytestream streamHostUsedPacket = Socks5PacketUtils.createBytestreamResponse(targetJID,
                        initiatorJID);
        streamHostUsedPacket.setSessionID(sessionID);
        streamHostUsedPacket.setUsedHost(proxyJID);

        // return used stream host info as response to the bytestream initiation
        protocol.addResponse(streamHostUsedPacket, new Verification<Bytestream, Bytestream>() {

            public void verify(Bytestream request, Bytestream response) {
                assertEquals(response.getSessionID(), request.getSessionID());
                assertEquals(1, request.getStreamHosts().size());
                StreamHost streamHost = (StreamHost) request.getStreamHosts().toArray()[0];
                assertEquals(response.getUsedHost().getJID(), streamHost.getJID());
            }

        }, Verification.correspondingSenderReceiver, Verification.requestTypeSET);

        // build response to proxy activation
        IQ activationResponse = Socks5PacketUtils.createActivationConfirmation(proxyJID,
                        initiatorJID);

        // return proxy activation response if proxy should be activated
        protocol.addResponse(activationResponse, new Verification<Bytestream, IQ>() {

            public void verify(Bytestream request, IQ response) {
                assertEquals(targetJID, request.getToActivate().getTarget());
            }

        }, Verification.correspondingSenderReceiver, Verification.requestTypeSET);

        // start a local SOCKS5 proxy
        Socks5TestProxy socks5Proxy = Socks5TestProxy.getProxy(7778);
        socks5Proxy.start();

        // create digest to get the socket opened by target
        String digest = Socks5Utils.createDigest(sessionID, initiatorJID, targetJID);

        // finally call the method that should be tested
        OutputStream outputStream = byteStreamManager.establishSession(targetJID, sessionID).getOutputStream();

        // test the established bytestream
        InputStream inputStream = socks5Proxy.getSocket(digest).getInputStream();

        byte[] data = new byte[] { 1, 2, 3 };
        outputStream.write(data);

        byte[] result = new byte[3];
        inputStream.read(result);

        assertArrayEquals(data, result);

        protocol.verifyAll();

    }

    /**
     * If multiple network addresses are added to the local SOCKS5 proxy, all of them should be
     * contained in the SOCKS5 Bytestream request.
     * 
     * @throws Exception should not happen
     */
    @Test
    public void shouldUseMultipleAddressesForLocalSocks5Proxy() throws Exception {

        // enable clients local SOCKS5 proxy on port 7778
        SmackConfiguration.setLocalSocks5ProxyEnabled(true);
        SmackConfiguration.setLocalSocks5ProxyPort(7778);

        // start a local SOCKS5 proxy
        Socks5Proxy socks5Proxy = Socks5Proxy.getSocks5Proxy();
        socks5Proxy.start();
        assertTrue(socks5Proxy.isRunning());

        // get Socks5ByteStreamManager for connection
        Socks5BytestreamManager byteStreamManager = Socks5BytestreamManager.getBytestreamManager(connection);

        /**
         * create responses in the order they should be queried specified by the XEP-0065
         * specification
         */

        // build discover info that supports the SOCKS5 feature
        DiscoverInfo discoverInfo = Socks5PacketUtils.createDiscoverInfo(targetJID, initiatorJID);
        discoverInfo.addFeature(Socks5BytestreamManager.NAMESPACE);

        // return that SOCKS5 is supported if target is queried
        protocol.addResponse(discoverInfo, Verification.correspondingSenderReceiver,
                        Verification.requestTypeGET);

        // build discover items containing no proxy item
        DiscoverItems discoverItems = Socks5PacketUtils.createDiscoverItems(xmppServer,
                        initiatorJID);

        // return the discover item if XMPP server is queried
        protocol.addResponse(discoverItems, Verification.correspondingSenderReceiver,
                        Verification.requestTypeGET);

        // build used stream host response
        Bytestream streamHostUsedPacket = Socks5PacketUtils.createBytestreamResponse(targetJID,
                        initiatorJID);
        streamHostUsedPacket.setSessionID(sessionID);
        streamHostUsedPacket.setUsedHost(initiatorJID); // local proxy used

        // return used stream host info as response to the bytestream initiation
        protocol.addResponse(streamHostUsedPacket, new Verification<Bytestream, Bytestream>() {

            public void verify(Bytestream request, Bytestream response) {
                assertEquals(response.getSessionID(), request.getSessionID());
                assertEquals(2, request.getStreamHosts().size());
                StreamHost streamHost1 = (StreamHost) request.getStreamHosts().toArray()[0];
                assertEquals(response.getUsedHost().getJID(), streamHost1.getJID());
                StreamHost streamHost2 = (StreamHost) request.getStreamHosts().toArray()[1];
                assertEquals(response.getUsedHost().getJID(), streamHost2.getJID());
                assertEquals("localAddress", streamHost2.getAddress());
            }

        }, Verification.correspondingSenderReceiver, Verification.requestTypeSET);

        // create digest to get the socket opened by target
        String digest = Socks5Utils.createDigest(sessionID, initiatorJID, targetJID);

        // connect to proxy as target
        socks5Proxy.addTransfer(digest);
        StreamHost streamHost = new StreamHost(targetJID, socks5Proxy.getLocalAddresses().get(0));
        streamHost.setPort(socks5Proxy.getPort());
        Socks5Client socks5Client = new Socks5Client(streamHost, digest);
        InputStream inputStream = socks5Client.getSocket(2000).getInputStream();

        // add another network address before establishing SOCKS5 Bytestream
        socks5Proxy.addLocalAddress("localAddress");

        // finally call the method that should be tested
        OutputStream outputStream = byteStreamManager.establishSession(targetJID, sessionID).getOutputStream();

        // test the established bytestream
        byte[] data = new byte[] { 1, 2, 3 };
        outputStream.write(data);

        byte[] result = new byte[3];
        inputStream.read(result);

        assertArrayEquals(data, result);

        protocol.verifyAll();

        // reset proxy settings
        socks5Proxy.stop();
        socks5Proxy.removeLocalAddress("localAddress");
        SmackConfiguration.setLocalSocks5ProxyPort(7777);

    }

    /**
     * Invoking {@link Socks5BytestreamManager#establishSession(String, String)} the first time
     * should successfully negotiate a SOCKS5 Bytestream via the second SOCKS5 proxy and should
     * prioritize this proxy for a second SOCKS5 Bytestream negotiation.
     * 
     * @throws Exception should not happen
     */
    @Test
    public void shouldPrioritizeSecondSocks5ProxyOnSecondAttempt() throws Exception {

        // disable clients local SOCKS5 proxy
        SmackConfiguration.setLocalSocks5ProxyEnabled(false);

        // get Socks5ByteStreamManager for connection
        Socks5BytestreamManager byteStreamManager = Socks5BytestreamManager.getBytestreamManager(connection);

        assertTrue(byteStreamManager.isProxyPrioritizationEnabled());

        Verification<Bytestream, Bytestream> streamHostUsedVerification1 = new Verification<Bytestream, Bytestream>() {

            public void verify(Bytestream request, Bytestream response) {
                assertEquals(response.getSessionID(), request.getSessionID());
                assertEquals(2, request.getStreamHosts().size());
                // verify that the used stream host is the second in list
                StreamHost streamHost = (StreamHost) request.getStreamHosts().toArray()[1];
                assertEquals(response.getUsedHost().getJID(), streamHost.getJID());
            }

        };
        createResponses(streamHostUsedVerification1);

        // start a local SOCKS5 proxy
        Socks5TestProxy socks5Proxy = Socks5TestProxy.getProxy(7778);
        socks5Proxy.start();

        // create digest to get the socket opened by target
        String digest = Socks5Utils.createDigest(sessionID, initiatorJID, targetJID);

        // call the method that should be tested
        OutputStream outputStream = byteStreamManager.establishSession(targetJID, sessionID).getOutputStream();

        // test the established bytestream
        InputStream inputStream = socks5Proxy.getSocket(digest).getInputStream();

        byte[] data = new byte[] { 1, 2, 3 };
        outputStream.write(data);

        byte[] result = new byte[3];
        inputStream.read(result);

        assertArrayEquals(data, result);

        protocol.verifyAll();

        Verification<Bytestream, Bytestream> streamHostUsedVerification2 = new Verification<Bytestream, Bytestream>() {

            public void verify(Bytestream request, Bytestream response) {
                assertEquals(response.getSessionID(), request.getSessionID());
                assertEquals(2, request.getStreamHosts().size());
                // verify that the used stream host is the first in list
                StreamHost streamHost = (StreamHost) request.getStreamHosts().toArray()[0];
                assertEquals(response.getUsedHost().getJID(), streamHost.getJID());
            }

        };
        createResponses(streamHostUsedVerification2);

        // call the method that should be tested again
        outputStream = byteStreamManager.establishSession(targetJID, sessionID).getOutputStream();

        // test the established bytestream
        inputStream = socks5Proxy.getSocket(digest).getInputStream();

        outputStream.write(data);

        inputStream.read(result);

        assertArrayEquals(data, result);

        protocol.verifyAll();

    }

    /**
     * Invoking {@link Socks5BytestreamManager#establishSession(String, String)} the first time
     * should successfully negotiate a SOCKS5 Bytestream via the second SOCKS5 proxy. The second
     * negotiation should run in the same manner if prioritization is disabled.
     * 
     * @throws Exception should not happen
     */
    @Test
    public void shouldNotPrioritizeSocks5ProxyIfPrioritizationDisabled() throws Exception {

        // disable clients local SOCKS5 proxy
        SmackConfiguration.setLocalSocks5ProxyEnabled(false);

        // get Socks5ByteStreamManager for connection
        Socks5BytestreamManager byteStreamManager = Socks5BytestreamManager.getBytestreamManager(connection);
        byteStreamManager.setProxyPrioritizationEnabled(false);

        assertFalse(byteStreamManager.isProxyPrioritizationEnabled());

        Verification<Bytestream, Bytestream> streamHostUsedVerification = new Verification<Bytestream, Bytestream>() {

            public void verify(Bytestream request, Bytestream response) {
                assertEquals(response.getSessionID(), request.getSessionID());
                assertEquals(2, request.getStreamHosts().size());
                // verify that the used stream host is the second in list
                StreamHost streamHost = (StreamHost) request.getStreamHosts().toArray()[1];
                assertEquals(response.getUsedHost().getJID(), streamHost.getJID());
            }

        };
        createResponses(streamHostUsedVerification);

        // start a local SOCKS5 proxy
        Socks5TestProxy socks5Proxy = Socks5TestProxy.getProxy(7778);
        socks5Proxy.start();

        // create digest to get the socket opened by target
        String digest = Socks5Utils.createDigest(sessionID, initiatorJID, targetJID);

        // call the method that should be tested
        OutputStream outputStream = byteStreamManager.establishSession(targetJID, sessionID).getOutputStream();

        // test the established bytestream
        InputStream inputStream = socks5Proxy.getSocket(digest).getInputStream();

        byte[] data = new byte[] { 1, 2, 3 };
        outputStream.write(data);

        byte[] result = new byte[3];
        inputStream.read(result);

        assertArrayEquals(data, result);

        protocol.verifyAll();

        createResponses(streamHostUsedVerification);

        // call the method that should be tested again
        outputStream = byteStreamManager.establishSession(targetJID, sessionID).getOutputStream();

        // test the established bytestream
        inputStream = socks5Proxy.getSocket(digest).getInputStream();

        outputStream.write(data);

        inputStream.read(result);

        assertArrayEquals(data, result);

        protocol.verifyAll();

        byteStreamManager.setProxyPrioritizationEnabled(true);

    }

    private void createResponses(Verification<Bytestream, Bytestream> streamHostUsedVerification) {
        // build discover info that supports the SOCKS5 feature
        DiscoverInfo discoverInfo = Socks5PacketUtils.createDiscoverInfo(targetJID, initiatorJID);
        discoverInfo.addFeature(Socks5BytestreamManager.NAMESPACE);

        // return that SOCKS5 is supported if target is queried
        protocol.addResponse(discoverInfo, Verification.correspondingSenderReceiver,
                        Verification.requestTypeGET);

        // build discover items containing a proxy item
        DiscoverItems discoverItems = Socks5PacketUtils.createDiscoverItems(xmppServer,
                        initiatorJID);
        discoverItems.addItem(new Item("proxy2.xmpp-server"));
        discoverItems.addItem(new Item(proxyJID));

        // return the proxy item if XMPP server is queried
        protocol.addResponse(discoverItems, Verification.correspondingSenderReceiver,
                        Verification.requestTypeGET);

        /*
         * build discover info for proxy "proxy2.xmpp-server" containing information about being a
         * SOCKS5 proxy
         */
        DiscoverInfo proxyInfo1 = Socks5PacketUtils.createDiscoverInfo("proxy2.xmpp-server",
                        initiatorJID);
        Identity identity1 = new Identity("proxy", "proxy2.xmpp-server", "bytestreams");
        identity1.setType("bytestreams");
        proxyInfo1.addIdentity(identity1);

        // return the SOCKS5 bytestream proxy identity if proxy is queried
        protocol.addResponse(proxyInfo1, Verification.correspondingSenderReceiver,
                        Verification.requestTypeGET);

        // build discover info for proxy containing information about being a SOCKS5 proxy
        DiscoverInfo proxyInfo2 = Socks5PacketUtils.createDiscoverInfo(proxyJID, initiatorJID);
        Identity identity2 = new Identity("proxy", proxyJID, "bytestreams");
        proxyInfo2.addIdentity(identity2);

        // return the SOCKS5 bytestream proxy identity if proxy is queried
        protocol.addResponse(proxyInfo2, Verification.correspondingSenderReceiver,
                        Verification.requestTypeGET);

        /*
         * build a SOCKS5 stream host info for "proxy2.xmpp-server" containing the address and the
         * port of the proxy
         */
        Bytestream streamHostInfo1 = Socks5PacketUtils.createBytestreamResponse(
                        "proxy2.xmpp-server", initiatorJID);
        streamHostInfo1.addStreamHost("proxy2.xmpp-server", proxyAddress, 7778);

        // return stream host info if it is queried
        protocol.addResponse(streamHostInfo1, Verification.correspondingSenderReceiver,
                        Verification.requestTypeGET);

        // build a SOCKS5 stream host info containing the address and the port of the proxy
        Bytestream streamHostInfo2 = Socks5PacketUtils.createBytestreamResponse(proxyJID,
                        initiatorJID);
        streamHostInfo2.addStreamHost(proxyJID, proxyAddress, 7778);

        // return stream host info if it is queried
        protocol.addResponse(streamHostInfo2, Verification.correspondingSenderReceiver,
                        Verification.requestTypeGET);

        // build used stream host response
        Bytestream streamHostUsedPacket = Socks5PacketUtils.createBytestreamResponse(targetJID,
                        initiatorJID);
        streamHostUsedPacket.setSessionID(sessionID);
        streamHostUsedPacket.setUsedHost(proxyJID);

        // return used stream host info as response to the bytestream initiation
        protocol.addResponse(streamHostUsedPacket, streamHostUsedVerification,
                        Verification.correspondingSenderReceiver, Verification.requestTypeSET);

        // build response to proxy activation
        IQ activationResponse = Socks5PacketUtils.createActivationConfirmation(proxyJID,
                        initiatorJID);

        // return proxy activation response if proxy should be activated
        protocol.addResponse(activationResponse, new Verification<Bytestream, IQ>() {

            public void verify(Bytestream request, IQ response) {
                assertEquals(targetJID, request.getToActivate().getTarget());
            }

        }, Verification.correspondingSenderReceiver, Verification.requestTypeSET);

    }

    /**
     * Stop eventually started local SOCKS5 test proxy.
     */
    @After
    public void cleanUp() {
        Socks5TestProxy.stopProxy();
        SmackConfiguration.setLocalSocks5ProxyEnabled(true);
    }

}
