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
// TODO this should become PingManagerTest

//package org.jivesoftware.smackx.ping;
//
//import static org.custommonkey.xmlunit.XMLAssert.assertXMLEqual;
//import static org.junit.Assert.assertTrue;
//
//import java.util.Properties;
//import java.util.concurrent.CountDownLatch;
//import java.util.concurrent.TimeUnit;
//
//import org.jivesoftware.smack.Connection;
//import org.jivesoftware.smack.DummyConnection;
//import org.jivesoftware.smack.PacketInterceptor;
//import org.jivesoftware.smack.PacketListener;
//import org.jivesoftware.smack.SmackConfiguration;
//import org.jivesoftware.smack.ThreadedDummyConnection;
//import org.jivesoftware.smack.filter.IQTypeFilter;
//import org.jivesoftware.smack.filter.PacketTypeFilter;
//import org.jivesoftware.smack.packet.IQ;
//import org.jivesoftware.smack.packet.Packet;
//import org.jivesoftware.smack.pingx.packet.Ping;
//import org.jivesoftware.smackx.ping.PingFailedListener;
//import org.junit.After;
//import org.junit.Before;
//import org.junit.Test;
//
//public class KeepaliveTest {
//    private static final long PING_MINIMUM = 1000;
//    private static String TO = "juliet@capulet.lit/balcony";
//    private static String ID = "s2c1";
//
//    private static Properties outputProperties = new Properties();
//    {
//        outputProperties.put(javax.xml.transform.OutputKeys.OMIT_XML_DECLARATION, "yes");
//    }
//    
//    private int originalTimeout;
//    
//    @Before
//    public void resetProperties()
//    {
//        SmackConfiguration.setKeepAliveInterval(-1);
//        originalTimeout = SmackConfiguration.getPacketReplyTimeout();
//        SmackConfiguration.setPacketReplyTimeout(1000);
//    }
//    
//    @After
//    public void restoreProperties()
//    {
//        SmackConfiguration.setPacketReplyTimeout(originalTimeout);
//    }
//    /*
//     * Stanza copied from spec
//     */
//    @Test
//    public void validatePingStanzaXML() throws Exception {
//        // @formatter:off
//        String control = "<iq to='juliet@capulet.lit/balcony' id='s2c1' type='get'>"
//                + "<ping xmlns='urn:xmpp:ping'/></iq>";
//        // @formatter:on
//
//        Ping ping = new Ping(TO);
//        ping.setPacketID(ID);
//
//        assertXMLEqual(control, ping.toXML());
//    }
//
//    @Test
//    public void serverPingFailSingleConnection() throws Exception {
//        DummyConnection connection = getConnection();
//        CountDownLatch latch = new CountDownLatch(2);
//        addInterceptor(connection, latch);
//        addPingFailedListener(connection, latch);
//
//        // Time based testing kind of sucks, but this should be reliable on a DummyConnection since there 
//        // is no actual server involved.  This will provide enough time to ping and wait for the lack of response. 
//        assertTrue(latch.await(getWaitTime(), TimeUnit.MILLISECONDS));
//    }
//
//    @Test
//    public void serverPingSuccessfulSingleConnection() throws Exception {
//        ThreadedDummyConnection connection = getThreadedConnection();
//        final CountDownLatch latch = new CountDownLatch(1);
//
//        connection.addPacketListener(new PacketListener() {
//            @Override
//            public void processPacket(Packet packet) {
//                latch.countDown();
//            }
//        }, new IQTypeFilter(IQ.Type.RESULT));
//
//        // Time based testing kind of sucks, but this should be reliable on a DummyConnection since there 
//        // is no actual server involved.  This will provide enough time to ping and wait for the lack of response. 
//        assertTrue(latch.await(getWaitTime(), TimeUnit.MILLISECONDS));
//    }
//
//    @Test
//    public void serverPingFailMultipleConnection() throws Exception {
//        CountDownLatch latch = new CountDownLatch(6);
//        SmackConfiguration.setPacketReplyTimeout(15000);
//        
//        DummyConnection con1 = getConnection();
//        addInterceptor(con1, latch);
//        addPingFailedListener(con1, latch);
//
//        DummyConnection con2 = getConnection();
//        addInterceptor(con2, latch);
//        addPingFailedListener(con2, latch);
//        
//        DummyConnection con3 = getConnection();
//        addInterceptor(con3, latch);
//        addPingFailedListener(con2, latch);
//
//        assertTrue(latch.await(getWaitTime(), TimeUnit.MILLISECONDS));
//    }
//
//    private void addPingFailedListener(DummyConnection con, final CountDownLatch latch) {
//        KeepAliveManager manager = KeepAliveManager.getInstanceFor(con);
//        manager.addPingFailedListener(new PingFailedListener() {
//            @Override
//            public void pingFailed() {
//                latch.countDown();
//            }
//        });
//    }
//
//    private DummyConnection getConnection() {
//        DummyConnection con = new DummyConnection();
//        KeepAliveManager mgr = KeepAliveManager.getInstanceFor(con);
//        mgr.setPingInterval(PING_MINIMUM);
//
//        return con;
//    }
//    
//    private ThreadedDummyConnection getThreadedConnection() {
//        ThreadedDummyConnection con = new ThreadedDummyConnection();
//        KeepAliveManager mgr = KeepAliveManager.getInstanceFor(con);
//        mgr.setPingInterval(PING_MINIMUM);
//
//        return con;
//    }
//
//    private void addInterceptor(final Connection con, final CountDownLatch latch) {
//        con.addPacketInterceptor(new PacketInterceptor() {
//            @Override
//            public void interceptPacket(Packet packet) {
//                con.removePacketInterceptor(this);
//                latch.countDown();
//            }
//        }, new PacketTypeFilter(Ping.class));
//    }
//
//    private long getWaitTime() {
//        return PING_MINIMUM + SmackConfiguration.getPacketReplyTimeout() + 3000;
//    }
//}
