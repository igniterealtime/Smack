/**
 *
 * Copyright 2015-2023 Florian Schmaus
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

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;

import org.jxmpp.jid.DomainBareJid;

public abstract class AbstractSmackLowLevelIntegrationTest extends AbstractSmackIntTest {

    public interface UnconnectedConnectionSource {
        AbstractXMPPConnection getUnconnectedConnection();
    }

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

    /**
     * Get a connected connection. Note that this method will return a connection constructed via the default connection
     * descriptor. It is primarily meant for integration tests to discover if the XMPP service supports a certain
     * feature, that the integration test requires to run. This feature discovery is typically done in the constructor of the
     * integration tests. And if the discovery fails a {@link TestNotPossibleException} should be thrown by he constructor.
     *
     * <p> Please ensure that you invoke {@link #recycle(AbstractXMPPConnection connection)} once you are done with this connection.
     *
     * @return a connected connection.
     * @throws InterruptedException if the calling thread was interrupted.
     * @throws SmackException if Smack detected an exceptional situation.
     * @throws IOException if an I/O error occurred.
     * @throws XMPPException if an XMPP protocol error was received.
     */
    protected AbstractXMPPConnection getConnectedConnection()
                    throws InterruptedException, SmackException, IOException, XMPPException {
        return environment.connectionManager.constructConnectedConnection();
    }

    protected void recycle(AbstractXMPPConnection connection) {
        environment.connectionManager.recycle(connection);
    }

}
