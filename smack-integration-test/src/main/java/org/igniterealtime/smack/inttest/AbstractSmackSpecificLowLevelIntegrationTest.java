/**
 *
 * Copyright 2018-2021 Florian Schmaus
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

import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;

public abstract class AbstractSmackSpecificLowLevelIntegrationTest<C extends AbstractXMPPConnection>
        extends AbstractSmackLowLevelIntegrationTest {

    private final SmackIntegrationTestEnvironment environment;

    private final XmppConnectionDescriptor<C, ? extends ConnectionConfiguration, ? extends ConnectionConfiguration.Builder<?, ?>> connectionDescriptor;

    public AbstractSmackSpecificLowLevelIntegrationTest(SmackIntegrationTestEnvironment environment,
            Class<C> connectionClass) {
        super(environment);
        this.environment = environment;

        connectionDescriptor = environment.connectionManager.getConnectionDescriptorFor(connectionClass);
        if (connectionDescriptor == null) {
            throw new IllegalStateException("No connection descriptor for " + connectionClass + " known");
        }
    }

    public XmppConnectionDescriptor<C, ? extends ConnectionConfiguration, ? extends ConnectionConfiguration.Builder<?, ?>> getConnectionDescriptor() {
        return connectionDescriptor;
    }

    protected C getSpecificUnconnectedConnection() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        return environment.connectionManager.constructConnection(connectionDescriptor);
    }

    protected List<C> getSpecificUnconnectedConnections(int count)
                    throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        List<C> connections = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            C connection = getSpecificUnconnectedConnection();
            connections.add(connection);
        }
        return connections;
    }
}
