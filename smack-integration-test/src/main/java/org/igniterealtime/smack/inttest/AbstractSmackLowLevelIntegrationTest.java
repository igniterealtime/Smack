/**
 *
 * Copyright 2015-2020 Florian Schmaus
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
package org.igniterealtime.smack.inttest;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;

import org.jxmpp.jid.DomainBareJid;

public abstract class AbstractSmackLowLevelIntegrationTest extends AbstractSmackIntTest {

    private final SmackIntegrationTestEnvironment environment;

    /**
     * The configuration
     */
    protected final Configuration configuration;

    protected final DomainBareJid service;

    protected AbstractSmackLowLevelIntegrationTest(SmackIntegrationTestEnvironment environment) {
        super(environment);
        this.environment = environment;
        this.configuration = environment.configuration;
        this.service = configuration.service;
    }

    protected AbstractXMPPConnection getConnectedConnection() throws InterruptedException, XMPPException, SmackException, IOException {
        AbstractXMPPConnection connection = getUnconnectedConnection();
        connection.connect().login();
        return connection;
    }

    protected AbstractXMPPConnection getUnconnectedConnection()
                    throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        return environment.connectionManager.constructConnection();
    }

    protected List<AbstractXMPPConnection> getUnconnectedConnections(int count)
                    throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        List<AbstractXMPPConnection> connections = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            AbstractXMPPConnection connection = getUnconnectedConnection();
            connections.add(connection);
        }
        return connections;
    }

    protected void recycle(AbstractXMPPConnection connection) {
        environment.connectionManager.recycle(connection);
    }

}
