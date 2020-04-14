/**
 *
 * Copyright 2018-2020 Florian Schmaus
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

import java.util.List;
import java.util.logging.Level;

import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnection;

import org.igniterealtime.smack.XmppConnectionStressTest;
import org.igniterealtime.smack.XmppConnectionStressTest.StressTestFailedException.ErrorsWhileSendingOrReceivingException;
import org.igniterealtime.smack.XmppConnectionStressTest.StressTestFailedException.NotAllMessagesReceivedException;
import org.igniterealtime.smack.inttest.AbstractSmackLowLevelIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;

public class XmppConnectionIntegrationTest extends AbstractSmackLowLevelIntegrationTest {

    public XmppConnectionIntegrationTest(SmackIntegrationTestEnvironment environment) {
        super(environment);
    }

    @SmackIntegrationTest(connectionCount = 4)
    public void allToAllMessageSendTest(List<AbstractXMPPConnection> connections)
            throws InterruptedException, NotAllMessagesReceivedException, ErrorsWhileSendingOrReceivingException {
        final long seed = 42;
        final int messagesPerConnection = 3; // 100
        final int maxPayloadChunkSize = 16; // 512
        final int maxPayloadChunks = 4; // 32
        final boolean intermixMessages = false; // true

        XmppConnectionStressTest.Configuration stressTestConfiguration = new XmppConnectionStressTest.Configuration(
                seed, messagesPerConnection, maxPayloadChunkSize, maxPayloadChunks, intermixMessages);

        XmppConnectionStressTest stressTest = new XmppConnectionStressTest(stressTestConfiguration);

        stressTest.run(connections, timeout);

        final Level connectionStatsLogLevel = Level.FINE;
        if (LOGGER.isLoggable(connectionStatsLogLevel)) {
            if (connections.get(0) instanceof ModularXmppClientToServerConnection) {
                for (XMPPConnection connection : connections) {
                    ModularXmppClientToServerConnection xmppC2sConnection = (ModularXmppClientToServerConnection) connection;
                    ModularXmppClientToServerConnection.Stats stats = xmppC2sConnection.getStats();
                    LOGGER.log(connectionStatsLogLevel,
                            "Connections stats for " + xmppC2sConnection + ":\n{}",
                            stats);
                }
            }
        }
    }

}
