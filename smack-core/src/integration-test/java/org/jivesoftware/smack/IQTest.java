/**
 *
 * Copyright 2003-2007 Jive Software.
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

package org.jivesoftware.smack;

import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketIDFilter;
import org.jivesoftware.smack.filter.StanzaTypeFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.test.SmackTestCase;
import org.jivesoftware.smackx.packet.Version;

/**
 * Ensure that the server is handling IQ packets correctly.
 *
 * @author Gaston Dombiak
 */
// sinttest candidate
public class IQTest extends SmackTestCase {

    public IQTest(String arg0) {
        super(arg0);
    }

    /**
     * Check that the server responds a 503 error code when the client sends an IQ stanza with an
     * invalid namespace.
     */
    public void testInvalidNamespace() {
        IQ iq = new IQ() {
            public String getChildElementXML() {
                StringBuilder buf = new StringBuilder();
                buf.append("<query xmlns=\"jabber:iq:anything\">");
                buf.append("</query>");
                return buf.toString();
            }
        };

        PacketFilter filter = new AndFilter(new PacketIDFilter(iq.getStanzaId()),
                new StanzaTypeFilter(IQ.class));
        StanzaCollector collector = getConnection(0).createStanzaCollector(filter);
        // Send the iq packet with an invalid namespace
        getConnection(0).sendStanza(iq);

        IQ result = (IQ)collector.nextResult(SmackConfiguration.getPacketReplyTimeout());
        // Stop queuing results
        collector.cancel();
        if (result == null) {
            fail("No response from server");
        }
        else if (result.getType() != IQ.Type.error) {
            fail("The server didn't reply with an error packet");
        }
        else {
            assertEquals("Server answered an incorrect error code", 503, result.getError().getCode());
        }
    }

    /**
     * Check that sending an IQ to a full JID that is offline returns an IQ ERROR instead
     * of being route to some other resource of the same user.
     */
    public void testFullJIDToOfflineUser() {
        // Request the version from the server.
        Version versionRequest = new Version();
        versionRequest.setType(IQ.Type.get);
        versionRequest.setFrom(getFullJID(0));
        versionRequest.setTo(getBareJID(0) + "/Something");

        // Create a packet collector to listen for a response.
        StanzaCollector collector = getConnection(0).createStanzaCollector(
                       new PacketIDFilter(versionRequest.getStanzaId()));

        getConnection(0).sendStanza(versionRequest);

        // Wait up to 5 seconds for a result.
        IQ result = (IQ)collector.nextResult(SmackConfiguration.getPacketReplyTimeout());
        // Stop queuing results
        collector.cancel();
        assertNotNull("No response from server", result);
        assertEquals("The server didn't reply with an error packet", IQ.Type.error, result.getType());
        assertEquals("Server answered an incorrect error code", 503, result.getError().getCode());
    }

    protected int getMaxConnections() {
        return 1;
    }
}
