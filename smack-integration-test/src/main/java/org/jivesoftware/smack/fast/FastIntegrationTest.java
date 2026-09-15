/*
 *
 * Copyright 2026 Florian Schmaus
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
package org.jivesoftware.smack.fast;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnection;
import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnectionConfiguration;
import org.jivesoftware.smack.fast.element.FastElements;
import org.jivesoftware.smack.sasl.ht.SaslHtMechanism;
import org.jivesoftware.smack.sasl.packet.Sasl2Feature;
import org.jivesoftware.smack.sasl.sasl2.Sasl2Module;
import org.jivesoftware.smack.sasl.sasl2.Sasl2ModuleDescriptor;

import org.igniterealtime.smack.inttest.AbstractSmackSpecificLowLevelIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.TestNotPossibleException;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.annotations.SpecificationReference;

@SpecificationReference(document = "XEP-0484", version = "0.2.0")
public class FastIntegrationTest extends AbstractSmackSpecificLowLevelIntegrationTest<ModularXmppClientToServerConnection> {

    @SuppressWarnings("this-escape")
    public FastIntegrationTest(SmackIntegrationTestEnvironment environment) throws Exception {
        super(environment, ModularXmppClientToServerConnection.class);
        ModularXmppClientToServerConnection connection = getSpecificUnconnectedConnection();
        try {
            connection.connect();
            Sasl2Feature sasl2Feature = connection.getFeature(Sasl2Feature.class);
            if (sasl2Feature == null || !sasl2Feature.hasInlineFeature(FastElements.Fast.class)) {
                throw new TestNotPossibleException("XEP-0484: FAST not supported by service");
            }
        } finally {
            connection.disconnect();
        }
    }

    @SmackIntegrationTest
    public void testFastTokenRequestAndSubsequentAuthentication() throws Exception {
        ModularXmppClientToServerConnection testConn1 = getSpecificUnconnectedConnection(builder -> {
            ModularXmppClientToServerConnectionConfiguration.Builder modularBuilder =
                (ModularXmppClientToServerConnectionConfiguration.Builder) builder;
            modularBuilder.with(FastModuleDescriptor.Builder.class)
                .setPreferredFastMechanism("HT-SHA-256-NONE")
                .setAutoRequestToken(true)
                .buildModule();
            modularBuilder.addEnabledSaslMechanism("SCRAM-SHA-256-PLUS");
            modularBuilder.addEnabledSaslMechanism("SCRAM-SHA-256");
            modularBuilder.addEnabledSaslMechanism("HT-SHA-256-NONE");
            modularBuilder.addEnabledSaslMechanism("HT-SHA-256-ENDP");
        });

        FastToken token;
        try {
            testConn1.connect();
            testConn1.login();

            assertTrue(testConn1.isAuthenticated(), "Connection 1 should be authenticated");
            FastModule fastModule1 = testConn1.getConnectionModuleFor(FastModuleDescriptor.class);
            assertNotNull(fastModule1);
            token = fastModule1.getFastToken();
            assertNotNull(token, "FAST token should have been received and stored");
        } finally {
            testConn1.disconnect();
        }

        // Reconnect testConn1 using the stored FAST token for fast re-authentication
        try {
            testConn1.connect();
            testConn1.login();

            assertTrue(testConn1.isAuthenticated(), "Connection should be authenticated using FAST");
            Sasl2Module sasl2Module = testConn1.getConnectionModuleFor(Sasl2ModuleDescriptor.class);
            assertNotNull(sasl2Module);
            assertNotNull(sasl2Module.getSasl2AuthenticationResult());
            assertTrue(sasl2Module.getSasl2AuthenticationResult().getUsedSaslMechanism() instanceof SaslHtMechanism,
                    "Expected FAST authentication to use SaslHtMechanism");
        } finally {
            testConn1.disconnect();
        }
    }

    @SmackIntegrationTest
    public void testInvalidFastTokenDegradesGracefully() throws Exception {
        ModularXmppClientToServerConnection testConn = getSpecificUnconnectedConnection(builder -> {
            ModularXmppClientToServerConnectionConfiguration.Builder modularBuilder =
                (ModularXmppClientToServerConnectionConfiguration.Builder) builder;
            modularBuilder.with(FastModuleDescriptor.Builder.class)
                .setFastToken(new FastToken("invalid-fast-token-xyz", "HT-SHA-256-NONE"))
                .setAutoRequestToken(true)
                .buildModule();
        });

        try {
            testConn.connect();
            // Should not fail, but fall back gracefully
            testConn.login();

            assertTrue(testConn.isAuthenticated(), "Connection should be authenticated after graceful fallback");
        } finally {
            testConn.disconnect();
        }
    }
}
