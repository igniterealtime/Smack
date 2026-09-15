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
package org.jivesoftware.smack.bind2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.jivesoftware.smack.bind2.Bind2Module.Bind2SuccessResult;
import org.jivesoftware.smack.bind2.element.Bind2Elements;
import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnection;
import org.jivesoftware.smack.sasl.packet.Sasl2Feature;
import org.jivesoftware.smack.util.StringUtils;

import org.igniterealtime.smack.inttest.AbstractSmackSpecificLowLevelIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.TestNotPossibleException;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.annotations.SpecificationReference;

import org.jxmpp.jid.parts.Resourcepart;

@SpecificationReference(document = "XEP-0386", version = "1.1.0")
public class Bind2IntegrationTest extends AbstractSmackSpecificLowLevelIntegrationTest<ModularXmppClientToServerConnection> {

    @SuppressWarnings("this-escape")
    public Bind2IntegrationTest(SmackIntegrationTestEnvironment environment) throws Exception {
        super(environment, ModularXmppClientToServerConnection.class);
        ModularXmppClientToServerConnection connection = getSpecificUnconnectedConnection();
        try {
            connection.connect();
            var sasl2Feature = connection.getFeature(Sasl2Feature.class);
            if (sasl2Feature == null || !sasl2Feature.hasInlineFeature(Bind2Elements.Bind.class)) {
                throw new TestNotPossibleException("XEP-0386: Bind 2 (Bind2) not supported by service");
            }
        } finally {
            connection.disconnect();
        }
    }

    @SmackIntegrationTest
    public void testBind2ResourceBinding() throws Exception {
        ModularXmppClientToServerConnection connection = getSpecificUnconnectedConnection();
        try {
            connection.connect();
            Resourcepart requestedResource = Resourcepart.from("bind2-test-" + StringUtils.insecureRandomString(6));
            connection.login(connection.getConfiguration().getUsername(),
                            connection.getConfiguration().getPassword(),
                            requestedResource);

            assertTrue(connection.isAuthenticated(), "Expected connection to be authenticated");
            assertNotNull(connection.getUser(), "Expected connection to have a bound user JID");
            assertNotNull(connection.getUser().getResourcepart(), "Expected connection user JID to have a resourcepart");

            Bind2Module bind2Module = connection.getConnectionModuleFor(Bind2ModuleDescriptor.class);
            assertNotNull(bind2Module, "Bind2Module should be present on connection");

            Bind2SuccessResult bind2SuccessResult = bind2Module.getBind2SuccessResult();
            assertNotNull(bind2SuccessResult, "Bind2SuccessResult should not be null");
            assertNotNull(bind2SuccessResult.getBound(), "Bound element in Bind2SuccessResult should not be null");
            assertNotNull(bind2SuccessResult.getSuccessNonza(), "Success nonza in Bind2SuccessResult should not be null");
            assertEquals(connection.getUser().getResourcepart(), bind2SuccessResult.getBoundResource());
            assertEquals(requestedResource, bind2SuccessResult.getRequestedResource());
        } finally {
            connection.disconnect();
        }
    }

    @SmackIntegrationTest
    public void testBind2ServerGeneratedResource() throws Exception {
        ModularXmppClientToServerConnection connection = getSpecificUnconnectedConnection();
        try {
            connection.connect();
            connection.login(connection.getConfiguration().getUsername(),
                            connection.getConfiguration().getPassword(),
                            null);

            assertTrue(connection.isAuthenticated(), "Expected connection to be authenticated");
            assertNotNull(connection.getUser(), "Expected connection to have a bound user JID");
            assertNotNull(connection.getUser().getResourcepart(), "Expected bound resourcepart to be present");

            Bind2Module bind2Module = connection.getConnectionModuleFor(Bind2ModuleDescriptor.class);
            assertNotNull(bind2Module, "Bind2Module should be present on connection");

            Bind2SuccessResult bind2SuccessResult = bind2Module.getBind2SuccessResult();
            assertNotNull(bind2SuccessResult, "Bind2SuccessResult should not be null");
            assertNotNull(bind2SuccessResult.getBound(), "Bound element in Bind2SuccessResult should not be null");
            assertNotNull(bind2SuccessResult.getSuccessNonza(), "Success nonza in Bind2SuccessResult should not be null");
            assertEquals(connection.getUser().getResourcepart(), bind2SuccessResult.getBoundResource());
            assertNull(bind2SuccessResult.getRequestedResource(), "Expected requestedResource to be null for server-generated resource");
        } finally {
            connection.disconnect();
        }
    }

}
