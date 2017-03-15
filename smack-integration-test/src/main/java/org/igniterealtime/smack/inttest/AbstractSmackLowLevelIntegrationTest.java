/**
 *
 * Copyright 2015-2017 Florian Schmaus
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

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jxmpp.jid.DomainBareJid;

public abstract class AbstractSmackLowLevelIntegrationTest extends AbstractSmackIntTest {

    private final SmackIntegrationTestEnvironment environment;

    /**
     * The configuration
     */
    protected final Configuration configuration;

    protected final DomainBareJid service;

    public AbstractSmackLowLevelIntegrationTest(SmackIntegrationTestEnvironment environment) {
        super(environment.testRunId, environment.configuration);
        this.environment = environment;
        this.configuration = environment.configuration;
        this.service = configuration.service;
    }

    public final XMPPTCPConnectionConfiguration.Builder getConnectionConfiguration() throws KeyManagementException, NoSuchAlgorithmException {
        XMPPTCPConnectionConfiguration.Builder builder = XMPPTCPConnectionConfiguration.builder();
        if (configuration.tlsContext != null) {
            builder.setCustomSSLContext(configuration.tlsContext);
        }
        builder.setSecurityMode(configuration.securityMode);
        builder.setXmppDomain(service);
        return builder;
    }

    protected void performCheck(ConnectionCallback callback) throws Exception {
        XMPPTCPConnection connection = SmackIntegrationTestFramework.getConnectedConnection(environment, -1);
        try {
            callback.connectionCallback(connection);
        } finally {
            IntTestUtil.disconnectAndMaybeDelete(connection, configuration);
        }
    }

    public interface ConnectionCallback {
        public void connectionCallback(XMPPTCPConnection connection) throws Exception;
    }
}
