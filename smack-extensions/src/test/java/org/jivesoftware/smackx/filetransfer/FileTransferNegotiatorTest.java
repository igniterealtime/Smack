/**
 *
 * Copyright 2011 Robin Collier
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
package org.jivesoftware.smackx.filetransfer;

import static org.junit.Assert.assertTrue;

import org.jivesoftware.smack.DummyConnection;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.jxmpp.jid.JidTestUtil;

public class FileTransferNegotiatorTest {
    private DummyConnection connection;

    @Before
    public void setUp() throws Exception {
        // Uncomment this to enable debug output
        // SmackConfiguration.DEBUG = true;

        connection = new DummyConnection();
        connection.connect();
        connection.login();
        ServiceDiscoveryManager.getInstanceFor(connection);
    }

    @After
    public void tearDown() throws Exception {
        if (connection != null)
            connection.disconnect();
    }

    @Test
    public void verifyForm() throws Exception {
        FileTransferNegotiator fileNeg = FileTransferNegotiator.getInstanceFor(connection);
        try {
            fileNeg.negotiateOutgoingTransfer(JidTestUtil.DUMMY_AT_EXAMPLE_ORG, "streamid", "file", 1024, null, 10);
        } catch (NoResponseException e) {
            // Ignore
        }
        Stanza packet = connection.getSentPacket();
        String xml = packet.toXML().toString();
        assertTrue(xml.indexOf("var='stream-method' type='list-single'") != -1);
    }
}
