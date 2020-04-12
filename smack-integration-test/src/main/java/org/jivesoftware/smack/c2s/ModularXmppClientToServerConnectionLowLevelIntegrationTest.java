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
package org.jivesoftware.smack.c2s;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;

import org.igniterealtime.smack.inttest.AbstractSmackSpecificLowLevelIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;

public class ModularXmppClientToServerConnectionLowLevelIntegrationTest extends AbstractSmackSpecificLowLevelIntegrationTest<ModularXmppClientToServerConnection> {

    public ModularXmppClientToServerConnectionLowLevelIntegrationTest(SmackIntegrationTestEnvironment environment) {
        super(environment, ModularXmppClientToServerConnection.class);
    }

    @SmackIntegrationTest
    public void testDisconnectAfterConnect() throws KeyManagementException, NoSuchAlgorithmException, SmackException,
            IOException, XMPPException, InterruptedException {
        ModularXmppClientToServerConnection connection = getSpecificUnconnectedConnection();

        connection.connect();

        connection.disconnect();
    }

    @SmackIntegrationTest
    public void testDisconnectNeverConnected()
                    throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        ModularXmppClientToServerConnection connection = getSpecificUnconnectedConnection();

        connection.disconnect();
    }
}
