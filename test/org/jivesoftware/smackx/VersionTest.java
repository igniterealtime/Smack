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

package org.jivesoftware.smackx;

import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.filter.PacketIDFilter;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.test.SmackTestCase;
import org.jivesoftware.smackx.packet.Version;

/**
 * Test case to ensure that Smack is able to get and parse correctly iq:version packets.
 *
 * @author Gaston Dombiak
 */
public class VersionTest extends SmackTestCase {

    public VersionTest(String arg0) {
        super(arg0);
    }

    /**
     * Get the version of the server and make sure that all the required data is present
     *
     * Note: This test expects the server to answer an iq:version packet.
     */
    public void testGetServerVersion() {
        Version version = new Version();
        version.setType(IQ.Type.GET);
        version.setTo(getServiceName());

        // Create a packet collector to listen for a response.
        PacketCollector collector = getConnection(0).createPacketCollector(new PacketIDFilter(version.getPacketID()));

        getConnection(0).sendPacket(version);

        // Wait up to 5 seconds for a result.
        IQ result = (IQ)collector.nextResult(5000);
        // Close the collector
        collector.cancel();

        assertNotNull("No result from the server", result);

        assertEquals("Incorrect result type", IQ.Type.RESULT, result.getType());
        assertNotNull("No name specified in the result", ((Version)result).getName());
        assertNotNull("No version specified in the result", ((Version)result).getVersion());
    }

    protected int getMaxConnections() {
        return 1;
    }
}
