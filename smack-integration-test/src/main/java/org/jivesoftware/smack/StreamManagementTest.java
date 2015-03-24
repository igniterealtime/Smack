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

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import org.igniterealtime.smack.inttest.AbstractSmackLowLevelIntegrationTest;
import org.igniterealtime.smack.inttest.Configuration;
import org.igniterealtime.smack.inttest.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.TestNotPossibleException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.junit.AfterClass;
import org.junit.BeforeClass;

public class StreamManagementTest extends AbstractSmackLowLevelIntegrationTest {

    public StreamManagementTest(Configuration configuration, String testRunId)
                    throws Exception {
        super(configuration, testRunId);
        performCheck(new ConnectionCallback() {
            @Override
            public void connectionCallback(XMPPTCPConnection connection) throws Exception {
                if (!connection.isSmAvailable()) {
                    throw new TestNotPossibleException("XEP-198: Stream Mangement not supported by service");
                }
            }
        });
    }

    @BeforeClass
    public static void before() {
        // TODO remove this once stream mangement is enabled per default
        XMPPTCPConnection.setUseStreamManagementDefault(true);
    }

    @AfterClass
    public static void after() {
        XMPPTCPConnection.setUseStreamManagementDefault(false);
    }

    @SmackIntegrationTest
    public void testStreamManagement(XMPPTCPConnection conOne, XMPPTCPConnection conTwo) throws InterruptedException, KeyManagementException,
                    NoSuchAlgorithmException, SmackException, IOException, XMPPException,
                    TestNotPossibleException {
        send("Hi, what's up?", conOne, conTwo);

        conOne.instantShutdown();

        send("Hi, what's up? I've been just instantly shutdown", conOne, conTwo);

        // Reconnect with xep198
        conOne.connect();

        send("Hi, what's up? I've been just resumed", conOne, conTwo);
        // TODO check that all messages where received
    }

    private static void send(String messageString, XMPPConnection from, XMPPConnection to)
                    throws NotConnectedException, InterruptedException {
        Message message = new Message(to.getUser());
        message.setBody(messageString);
        from.sendStanza(message);
    }
}
