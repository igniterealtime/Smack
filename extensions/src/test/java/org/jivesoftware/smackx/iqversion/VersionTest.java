/**
 *
 * Copyright 2014 Georg Lukas.
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
package org.jivesoftware.smackx.iqversion;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.jivesoftware.smack.DummyConnection;
import org.jivesoftware.smack.ThreadedDummyConnection;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.test.util.TestUtils;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smackx.iqversion.packet.Version;
import org.junit.Before;
import org.junit.Test;

public class VersionTest {
    private DummyConnection dummyCon;
    private ThreadedDummyConnection threadedCon;

    @Before
    public void setup() {
        dummyCon = new DummyConnection();
        threadedCon = new ThreadedDummyConnection();
    }

    @Test
    public void checkProvider() throws Exception {
        // @formatter:off
        String control = "<iq from='capulet.lit' to='juliet@capulet.lit/balcony' id='s2c1' type='get'>"
                + "<query xmlns='jabber:iq:version'/>"
                + "</iq>";
        // @formatter:on
        DummyConnection con = new DummyConnection();

        // Enable version replys for this connection
        VersionManager.getInstanceFor(con).setVersion(new Version("Test", "0.23", "DummyOS"));
        IQ versionRequest = PacketParserUtils.parseIQ(TestUtils.getIQParser(control), con);

        assertTrue(versionRequest instanceof Version);

        con.processPacket(versionRequest);

        Packet replyPacket = con.getSentPacket();
        assertTrue(replyPacket instanceof Version);

        Version reply = (Version) replyPacket;
        //getFrom check is pending for SMACK-547
        //assertEquals("juliet@capulet.lit/balcony", reply.getFrom());
        assertEquals("capulet.lit", reply.getTo());
        assertEquals("s2c1", reply.getPacketID());
        assertEquals(IQ.Type.RESULT, reply.getType());
        assertEquals("Test", reply.getName());
        assertEquals("0.23", reply.getVersion());
        assertEquals("DummyOS", reply.getOs());
    }
}
