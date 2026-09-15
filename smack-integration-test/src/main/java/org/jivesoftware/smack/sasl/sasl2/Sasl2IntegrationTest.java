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
package org.jivesoftware.smack.sasl.sasl2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.bind2.Bind2ModuleDescriptor;
import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnection;
import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnectionConfiguration;
import org.jivesoftware.smack.sasl.SASLError;
import org.jivesoftware.smack.sasl.SASLErrorException;
import org.jivesoftware.smack.sasl.packet.Sasl2Feature;
import org.jivesoftware.smack.sasl.sasl2.Sasl2Authentication.Sasl2AuthenticationResult;
import org.jivesoftware.smack.util.StringUtils;

import org.igniterealtime.smack.inttest.AbstractSmackSpecificLowLevelIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.TestNotPossibleException;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.annotations.SpecificationReference;

@SpecificationReference(document = "XEP-0388", version = "1.0.4")
public class Sasl2IntegrationTest extends AbstractSmackSpecificLowLevelIntegrationTest<ModularXmppClientToServerConnection> {

    @SuppressWarnings("this-escape")
    public Sasl2IntegrationTest(SmackIntegrationTestEnvironment environment) throws Exception {
        super(environment, ModularXmppClientToServerConnection.class);
        ModularXmppClientToServerConnection connection = getSpecificUnconnectedConnection();
        try {
            connection.connect();
            var sasl2Feature = connection.getFeature(Sasl2Feature.class);
            if (sasl2Feature == null) {
                throw new TestNotPossibleException("XEP-0388: Extensible SASL Profile (SASL2) not supported by service");
            }
        } finally {
            connection.disconnect();
        }
    }

    private ModularXmppClientToServerConnection getUnconnectedSasl2OnlyConnection()
                    throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        return getSpecificUnconnectedConnection(
                        builder -> ((ModularXmppClientToServerConnectionConfiguration.Builder) builder).removeModule(
                                        Bind2ModuleDescriptor.class));
    }

    @SmackIntegrationTest
    public void testSasl2Authentication() throws SmackException, IOException, XMPPException, InterruptedException {
        ModularXmppClientToServerConnection connection = getUnconnectedSasl2OnlyConnection();
        try {
            connection.connect().login();

            assertTrue(connection.isAuthenticated(), "Expected connection to be authenticated");
            assertNotNull(connection.getUser(), "Expected connection to have a bound user JID");
            assertNotNull(connection.getUser().getResourcepart(), "Expected connection user JID to have a resourcepart");

            Sasl2Module sasl2Module = connection.getConnectionModuleFor(Sasl2ModuleDescriptor.class);
            assertNotNull(sasl2Module, "Sasl2Module should be present on connection");

            Sasl2AuthenticationResult result = sasl2Module.getSasl2AuthenticationResult();
            assertNotNull(result, "Sasl2AuthenticationResult should be set after successful SASL2 authentication");
            assertNotNull(result.getSuccessNonza(), "Sasl2 Success nonza should not be null");
            assertNotNull(result.getUsedSaslMechanism(), "Used SASL mechanism should not be null");
            assertFalse(result.isResourceBound(), "Result should not be resource bound via SASL2 when Bind2 is disabled");
        } finally {
            connection.disconnect();
        }
    }

    @SmackIntegrationTest
    public void testSasl2AuthenticationFailure() throws SmackException, IOException, XMPPException, InterruptedException {
        ModularXmppClientToServerConnection connection = getUnconnectedSasl2OnlyConnection();
        try {
            connection.connect();
            var username = connection.getConfiguration().getUsername();
            var invalidPassword = "invalid-password-" + StringUtils.insecureRandomString(16);

            SASLErrorException errorException = assertThrows(SASLErrorException.class,
                () -> connection.login(username, invalidPassword));

            assertNotNull(errorException.getSasl2Failure(), "SASL2 Failure element should be present");
            assertEquals(SASLError.not_authorized, errorException.getSasl2Failure().getSASLError());
            assertFalse(connection.isAuthenticated(), "Connection should not be authenticated after failed login");
        } finally {
            connection.disconnect();
        }
    }
}
