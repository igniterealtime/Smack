/**
 *
 * Copyright 2015 Florian Schmaus
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.igniterealtime.smack.inttest.AbstractSmackLowLevelIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.TestNotPossibleException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.FromMatchesFilter;
import org.jivesoftware.smack.filter.MessageWithBodiesFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;

public class StreamManagementTest extends AbstractSmackLowLevelIntegrationTest {

    public StreamManagementTest(SmackIntegrationTestEnvironment environment) throws Exception {
        super(environment);
        performCheck(new ConnectionCallback() {
            @Override
            public void connectionCallback(XMPPTCPConnection connection) throws Exception {
                if (!connection.isSmAvailable()) {
                    throw new TestNotPossibleException("XEP-198: Stream Mangement not supported by service");
                }
            }
        });
    }

    @SmackIntegrationTest
    public void testStreamManagement(XMPPTCPConnection conOne, XMPPTCPConnection conTwo) throws InterruptedException,
                    SmackException, IOException, XMPPException {
        final String body1 = "Hi, what's up? " + testRunId;
        final String body2 = "Hi, what's up? I've been just instantly shutdown" + testRunId;
        final String body3 = "Hi, what's up? I've been just resumed" + testRunId;

        final PacketCollector collector = conTwo.createPacketCollector(new AndFilter(
                        MessageWithBodiesFilter.INSTANCE,
                        FromMatchesFilter.createFull(conOne.getUser())));

        try {
            send(body1, conOne, conTwo);
            assertMessageWithBodyReceived(body1, collector);

            conOne.instantShutdown();

            send(body2, conOne, conTwo);

            // Reconnect with xep198
            conOne.connect().login();
            assertMessageWithBodyReceived(body2, collector);

            send(body3, conOne, conTwo);
            assertMessageWithBodyReceived(body3, collector);
        }
        finally {
            collector.cancel();
        }
    }

    private static void send(String messageString, XMPPConnection from, XMPPConnection to)
                    throws NotConnectedException, InterruptedException {
        Message message = new Message(to.getUser());
        message.setBody(messageString);
        from.sendStanza(message);
    }

    private static void assertMessageWithBodyReceived(String body, PacketCollector collector) throws InterruptedException {
        Message message = collector.nextResult();
        assertNotNull(message);
        assertEquals(body, message.getBody());
    }
}
