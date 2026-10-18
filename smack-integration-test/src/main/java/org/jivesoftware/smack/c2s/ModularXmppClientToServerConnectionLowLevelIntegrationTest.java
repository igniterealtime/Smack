/*
 *
 * Copyright 2018-2026 Florian Schmaus
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.bind2.Bind2ModuleDescriptor;
import org.jivesoftware.smack.bind2.element.Bind2Elements;
import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnection.AuthenticatedAndResourceBoundStateDescriptor;
import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnection.ResourceBindingStateDescriptor;
import org.jivesoftware.smack.fast.FastModuleDescriptor;
import org.jivesoftware.smack.fsm.ConnectionStateEvent;
import org.jivesoftware.smack.fsm.ConnectionStateMachineListener;
import org.jivesoftware.smack.fsm.StateDescriptor;
import org.jivesoftware.smack.sasl.packet.Sasl2Feature;
import org.jivesoftware.smack.sasl.sasl2.Sasl2ModuleDescriptor;

import org.igniterealtime.smack.inttest.AbstractSmackSpecificLowLevelIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.TestNotPossibleException;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;

public class ModularXmppClientToServerConnectionLowLevelIntegrationTest extends AbstractSmackSpecificLowLevelIntegrationTest<ModularXmppClientToServerConnection> {

    public ModularXmppClientToServerConnectionLowLevelIntegrationTest(SmackIntegrationTestEnvironment environment) {
        super(environment, ModularXmppClientToServerConnection.class);
    }

    @SmackIntegrationTest
    public void testDisconnectAfterConnect() throws SmackException, IOException, XMPPException, InterruptedException {
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

    @SmackIntegrationTest
    public void testReconnectWithLegacySasl() throws SmackException, IOException, XMPPException, InterruptedException,
                    ExecutionException, TimeoutException {
        ModularXmppClientToServerConnection connection = getSpecificUnconnectedConnection(builder -> {
            ((ModularXmppClientToServerConnectionConfiguration.Builder) builder)
                .removeModule(FastModuleDescriptor.class)
                .removeModule(Bind2ModuleDescriptor.class)
                .removeModule(Sasl2ModuleDescriptor.class);
        });

        assertLoginAfterReconnect(connection, true);
    }

    @SmackIntegrationTest
    public void testReconnectWithSasl2WithoutBind2() throws SmackException, IOException, XMPPException,
                    InterruptedException, TestNotPossibleException, ExecutionException, TimeoutException {
        ModularXmppClientToServerConnection connection = getSpecificUnconnectedConnection(builder -> {
            ((ModularXmppClientToServerConnectionConfiguration.Builder) builder)
                .removeModule(FastModuleDescriptor.class)
                .removeModule(Bind2ModuleDescriptor.class);
        });
        throwIfSasl2NotSupported(connection, false);

        assertLoginAfterReconnect(connection, true);
    }

    @SmackIntegrationTest
    public void testReconnectWithSasl2AndBind2() throws SmackException, IOException, XMPPException,
                    InterruptedException, TestNotPossibleException, ExecutionException, TimeoutException {
        ModularXmppClientToServerConnection connection = getSpecificUnconnectedConnection(builder -> {
            ((ModularXmppClientToServerConnectionConfiguration.Builder) builder)
                .removeModule(FastModuleDescriptor.class);
        });
        throwIfSasl2NotSupported(connection, true);

        assertLoginAfterReconnect(connection, false);
    }

    private static void throwIfSasl2NotSupported(ModularXmppClientToServerConnection connection, boolean requireBind2)
                    throws SmackException, IOException, XMPPException, InterruptedException, TestNotPossibleException {
        Sasl2Feature sasl2Feature;
        try {
            connection.connect();
            sasl2Feature = connection.getFeature(Sasl2Feature.class);
        } finally {
            connection.disconnect();
        }

        if (sasl2Feature == null) {
            throw new TestNotPossibleException("XEP-0388: Extensible SASL Profile (SASL2) not supported by service");
        }
        if (requireBind2 && !sasl2Feature.hasInlineFeature(Bind2Elements.Bind.class)) {
            throw new TestNotPossibleException("XEP-0386: Bind 2 (Bind2) not supported by service");
        }
    }

    /**
     * Connect and login twice using the same connection instance, disconnecting in between, and assert that both
     * logins result in an authenticated connection with a bound resource.
     *
     * @param connection the unconnected connection.
     * @param expectRfc6120ResourceBinding whether resource binding as per RFC 6120 § 7 is expected on every login.
     */
    private static void assertLoginAfterReconnect(ModularXmppClientToServerConnection connection,
                    boolean expectRfc6120ResourceBinding)
                    throws SmackException, IOException, XMPPException, InterruptedException, ExecutionException,
                    TimeoutException {
        // Connection state machine listeners are invoked asynchronously, but in order. Hence, once the final state of
        // a login was reached, all events of that login have been delivered.
        AtomicBoolean rfc6120ResourceBindingPerformed = new AtomicBoolean();
        AtomicReference<CompletableFuture<Boolean>> loginFinished = new AtomicReference<>();
        ConnectionStateMachineListener listener = (event, c) -> {
            if (!(event instanceof ConnectionStateEvent.SuccessfullyTransitionedInto)) {
                return;
            }
            StateDescriptor stateDescriptor = event.getStateDescriptor();
            if (stateDescriptor instanceof ResourceBindingStateDescriptor) {
                rfc6120ResourceBindingPerformed.set(true);
            } else if (stateDescriptor instanceof AuthenticatedAndResourceBoundStateDescriptor) {
                loginFinished.get().complete(rfc6120ResourceBindingPerformed.getAndSet(false));
            }
        };
        connection.addConnectionStateMachineListener(listener);

        try {
            for (int i = 1; i <= 2; i++) {
                CompletableFuture<Boolean> currentLoginFinished = new CompletableFuture<>();
                loginFinished.set(currentLoginFinished);
                try {
                    connection.connect();
                    connection.login();

                    assertTrue(connection.isAuthenticated(), "Expected connection to be authenticated after login #" + i);
                    assertNotNull(connection.getUser(), "Expected connection to have a user JID after login #" + i);
                    assertNotNull(connection.getUser().getResourcepart(),
                                    "Expected connection user JID to have a resourcepart after login #" + i);

                    boolean resourceBindingPerformed = currentLoginFinished.get(connection.getReplyTimeout(),
                                    TimeUnit.MILLISECONDS);
                    assertEquals(expectRfc6120ResourceBinding, resourceBindingPerformed,
                                    "Unexpected RFC 6120 resource binding behavior on login #" + i);
                } finally {
                    connection.disconnect();
                }
            }
        } finally {
            connection.removeConnectionStateMachineListener(listener);
        }
    }
}
