/**
 * Copyright 2012 Florian Schmaus
 *
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
package org.jivesoftware.smackx.ping;

import org.jivesoftware.smack.DummyConnection;
import org.jivesoftware.smack.TestUtils;
import org.jivesoftware.smack.ThreadedDummyConnection;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.ping.packet.Ping;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smackx.packet.DiscoverInfo;
import org.junit.Test;

import static org.junit.Assert.*;

public class PingPongTest {

    @Test
    public void checkSendingPing() throws Exception {
        DummyConnection con = new DummyConnection();
        PingManager pinger = new PingManager(con);
        pinger.ping("test@myserver.com");

        Packet sentPacket = con.getSentPacket();
        
        assertTrue(sentPacket instanceof Ping);
        
    }

    @Test
    public void checkSuccessfulPing() throws Exception {
        ThreadedDummyConnection con = new ThreadedDummyConnection();
        
        PingManager pinger = new PingManager(con);

        boolean pingSuccess = pinger.ping("test@myserver.com");
        
        assertTrue(pingSuccess);
        
    }

    /**
     * DummyConnection will not reply so it will timeout.
     * @throws Exception
     */
    @Test
    public void checkFailedPingOnTimeout() throws Exception {
        DummyConnection con = new DummyConnection();
        PingManager pinger = new PingManager(con);

        boolean pingSuccess = pinger.ping("test@myserver.com");
        
        assertFalse(pingSuccess);
        
    }
    
    /**
     * DummyConnection will not reply so it will timeout.
     * @throws Exception
     */
    @Test
    public void checkFailedPingError() throws Exception {
        ThreadedDummyConnection con = new ThreadedDummyConnection();
        //@formatter:off
        String reply = 
                "<iq type='error' id='qrzSp-16' to='test@myserver.com'>" +
                        "<ping xmlns='urn:xmpp:ping'/>" +
                        "<error type='cancel'>" +
                            "<service-unavailable xmlns='urn:ietf:params:xml:ns:xmpp-stanzas'/>" +
                        "</error>" + 
                 "</iq>";
        //@formatter:on
        IQ serviceUnavailable = PacketParserUtils.parseIQ(TestUtils.getIQParser(reply), con);
        con.addIQReply(serviceUnavailable);

        PingManager pinger = new PingManager(con);

        boolean pingSuccess = pinger.ping("test@myserver.com");
        
        assertFalse(pingSuccess);
        
    }
    
    @Test
    public void checkSuccessfulDiscoRequest() throws Exception {
        ThreadedDummyConnection con = new ThreadedDummyConnection();
        DiscoverInfo info = new DiscoverInfo();
        info.addFeature(Ping.NAMESPACE);
        
        //@formatter:off
        String reply = 
                "<iq type='result' id='qrzSp-16' to='test@myserver.com'>" +
                        "<query xmlns='http://jabber.org/protocol/disco#info'><identity category='client' type='pc' name='Pidgin'/>" +
                            "<feature var='urn:xmpp:ping'/>" +
                        "</query></iq>";
        //@formatter:on
        IQ discoReply = PacketParserUtils.parseIQ(TestUtils.getIQParser(reply), con);
        con.addIQReply(discoReply);

        PingManager pinger = new PingManager(con);
        boolean pingSupported = pinger.isPingSupported("test@myserver.com");
        
        assertTrue(pingSupported);
    }

    @Test
    public void checkUnuccessfulDiscoRequest() throws Exception {
        ThreadedDummyConnection con = new ThreadedDummyConnection();
        DiscoverInfo info = new DiscoverInfo();
        info.addFeature(Ping.NAMESPACE);
        
        //@formatter:off
        String reply = 
                "<iq type='result' id='qrzSp-16' to='test@myserver.com'>" +
                        "<query xmlns='http://jabber.org/protocol/disco#info'><identity category='client' type='pc' name='Pidgin'/>" +
                            "<feature var='urn:xmpp:noping'/>" +
                        "</query></iq>";
        //@formatter:on
        IQ discoReply = PacketParserUtils.parseIQ(TestUtils.getIQParser(reply), con);
        con.addIQReply(discoReply);

        PingManager pinger = new PingManager(con);
        boolean pingSupported = pinger.isPingSupported("test@myserver.com");
        
        assertFalse(pingSupported);
    }
}
