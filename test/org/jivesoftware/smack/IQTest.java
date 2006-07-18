/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright 2003-2004 Jive Software.
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

package org.jivesoftware.smack;

import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketIDFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.test.SmackTestCase;

/**
 * Ensure that the server is handling IQ packets correctly.
 *
 * @author Gaston Dombiak
 */
public class IQTest extends SmackTestCase {

    public IQTest(String arg0) {
        super(arg0);
    }

    /**
     * Check that the server responds a 503 error code when the client sends an IQ packet with an
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

        PacketFilter filter = new AndFilter(new PacketIDFilter(iq.getPacketID()),
                new PacketTypeFilter(IQ.class));
        PacketCollector collector = getConnection(0).createPacketCollector(filter);
        // Send the iq packet with an invalid namespace
        getConnection(0).sendPacket(iq);

        IQ result = (IQ)collector.nextResult(SmackConfiguration.getPacketReplyTimeout());
        // Stop queuing results
        collector.cancel();
        if (result == null) {
            fail("No response from server");
        }
        else if (result.getType() != IQ.Type.ERROR) {
            fail("The server didn't reply with an error packet");
        }
        else {
            assertEquals("Server answered an incorrect error code", 503, result.getError().getCode());
        }
    }

    protected int getMaxConnections() {
        return 1;
    }
}
