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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.ListIterator;
import java.util.Queue;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.net.ssl.SSLSession;
import javax.security.auth.callback.CallbackHandler;
import javax.xml.namespace.QName;

import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.SmackSaslException;
import org.jivesoftware.smack.XMPPException.FailedNonzaException;
import org.jivesoftware.smack.XmppInputOutputFilter;
import org.jivesoftware.smack.bind2.element.Bind2Elements;
import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnection;
import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnectionConfiguration;
import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnectionModule;
import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnectionModuleDescriptor;
import org.jivesoftware.smack.c2s.XmppClientToServerTransport;
import org.jivesoftware.smack.c2s.internal.ModularXmppClientToServerConnectionInternal;
import org.jivesoftware.smack.c2s.internal.WalkStateGraphContext;
import org.jivesoftware.smack.fsm.ConnectionStateEvent;
import org.jivesoftware.smack.fsm.LoginContext;
import org.jivesoftware.smack.fsm.State;
import org.jivesoftware.smack.fsm.StateDescriptor;
import org.jivesoftware.smack.fsm.StateTransitionResult;
import org.jivesoftware.smack.internal.SmackTlsContext;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.Nonza;
import org.jivesoftware.smack.packet.TopLevelStreamElement;
import org.jivesoftware.smack.packet.XmlElement;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.sasl.SASLError;
import org.jivesoftware.smack.sasl.SASLErrorException;
import org.jivesoftware.smack.sasl.SASLMechanism;
import org.jivesoftware.smack.sasl.core.SASLAnonymous;
import org.jivesoftware.smack.sasl.packet.Sasl2Feature;
import org.jivesoftware.smack.sasl.packet.Sasl2Nonza;
import org.jivesoftware.smack.sasl.sasl2.Sasl2Authentication.Sasl2AuthenticationResult;
import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jivesoftware.smack.util.stringencoder.Base64;
import org.jivesoftware.smack.xml.XmlPullParser;

import org.junit.jupiter.api.Test;
import org.jxmpp.jid.EntityFullJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Resourcepart;
import org.jxmpp.stringprep.XmppStringprepException;

public class Sasl2AuthenticationTest extends SmackTestSuite {

    private static final class MockConnectionInternal extends ModularXmppClientToServerConnectionInternal {
        private final List<Nonza> sentNonzas = new ArrayList<>();
        private final Queue<Function<Nonza, Nonza>> responseHandlers = new ArrayDeque<>();

        private MockConnectionInternal(ModularXmppClientToServerConnection connection) {
            super(connection, null, null, new ArrayDeque<>());
        }

        void queueResponse(Nonza response) {
            responseHandlers.add(req -> response);
        }

        void queueHandler(Function<Nonza, Nonza> handler) {
            responseHandlers.add(handler);
        }

        List<Nonza> getSentNonzas() {
            return sentNonzas;
        }

        @Override
        public XmlEnvironment getOutgoingStreamXmlEnvironment() {
            return null;
        }

        @Override
        public void parseAndProcessElement(String wrappedCompleteIncomingElement) {
        }

        @Override
        public void notifyConnectionError(Exception e) {
        }

        @Override
        public String onStreamOpen(XmlPullParser parser) {
            return null;
        }

        @Override
        public void onStreamClosed() {
        }

        @Override
        public void fireFirstLevelElementSendListeners(TopLevelStreamElement element) {
        }

        @Override
        public void invokeConnectionStateMachineListener(ConnectionStateEvent connectionStateEvent) {
        }

        @Override
        public void addXmppInputOutputFilter(XmppInputOutputFilter xmppInputOutputFilter) {
        }

        @Override
        public ListIterator<XmppInputOutputFilter> getXmppInputOutputFilterBeginIterator() {
            return Collections.emptyListIterator();
        }

        @Override
        public ListIterator<XmppInputOutputFilter> getXmppInputOutputFilterEndIterator() {
            return Collections.emptyListIterator();
        }

        @Override
        public void prepareToWaitForFeaturesReceived() {
        }

        @Override
        public void waitForFeaturesReceived(String waitFor) {
        }

        @Override
        public void newStreamOpenWaitForFeaturesSequence(String waitFor) {
        }

        @Override
        public SmackTlsContext getSmackTlsContext() {
            return null;
        }

        @Override
        public SSLSession getSslSession() {
            return null;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <SN extends Nonza, FN extends Nonza> SN sendAndWaitForResponse(Nonza nonza,
                        Class<SN> successNonzaClass, Class<FN> failedNonzaClass)
                        throws NoResponseException, FailedNonzaException {
            return sendAndWaitForResponse(nonza, Collections.singleton(successNonzaClass), failedNonzaClass);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <SN extends Nonza, FN extends Nonza> SN sendAndWaitForResponse(Nonza nonza,
                        Collection<Class<? extends SN>> successNonzaClasses, Class<FN> failedNonzaClass)
                        throws NoResponseException, FailedNonzaException {
            sentNonzas.add(nonza);
            Function<Nonza, Nonza> handler = responseHandlers.poll();
            if (handler == null) {
                throw NoResponseException.newWith(connection, "No response queued for mock");
            }
            Nonza response = handler.apply(nonza);
            if (failedNonzaClass != null && failedNonzaClass.isInstance(response)) {
                throw new FailedNonzaException(response);
            }
            for (Class<? extends SN> clazz : successNonzaClasses) {
                if (clazz.isInstance(response)) {
                    return (SN) response;
                }
            }
            throw new IllegalStateException("Unexpected response nonza: " + response);
        }

        @Override
        public void asyncGo(Runnable runnable) {
            runnable.run();
        }

        @Override
        public void waitForConditionOrThrowConnectionException(Supplier<Boolean> condition, String waitFor) {
        }

        @Override
        public void notifyWaitingThreads() {
        }

        @Override
        public void notifyDataReceived() {
        }

        @Override
        public void setCompressionEnabled(boolean compressionEnabled) {
        }

        @Override
        public void setTransport(XmppClientToServerTransport xmppTransport) {
        }

        @Override
        public void setUser(EntityFullJid user) {
        }
    }

    public static final class DummyTransportModuleDescriptor extends ModularXmppClientToServerConnectionModuleDescriptor {
        DummyTransportModuleDescriptor(Builder builder) {
        }

        @Override
        protected Set<Class<? extends StateDescriptor>> getStateDescriptors() {
            return Collections.singleton(DummyTransportStateDescriptor.class);
        }

        @Override
        protected ModularXmppClientToServerConnectionModule<DummyTransportModuleDescriptor> constructXmppConnectionModule(
                        ModularXmppClientToServerConnectionInternal connectionInternal) {
            return new ModularXmppClientToServerConnectionModule<DummyTransportModuleDescriptor>(this, connectionInternal) {};
        }

        public static Builder builder(ModularXmppClientToServerConnectionConfiguration.Builder connectionConfigurationBuilder) {
            return new Builder(connectionConfigurationBuilder);
        }

        public static final class Builder extends ModularXmppClientToServerConnectionModuleDescriptor.Builder {
            Builder(ModularXmppClientToServerConnectionConfiguration.Builder connectionConfigurationBuilder) {
                super(connectionConfigurationBuilder);
            }

            @Override
            public DummyTransportModuleDescriptor build() {
                return new DummyTransportModuleDescriptor(this);
            }
        }
    }

    public static final class DummyTransportStateDescriptor extends StateDescriptor {
        public DummyTransportStateDescriptor() {
            super(DummyTransportState.class);
            addPredeccessor(ModularXmppClientToServerConnection.LookupRemoteConnectionEndpointsStateDescriptor.class);
            addSuccessor(ModularXmppClientToServerConnection.ConnectedButUnauthenticatedStateDescriptor.class);
        }

        @Override
        protected DummyTransportState constructState(ModularXmppClientToServerConnectionInternal connectionInternal) {
            return new DummyTransportState(this, connectionInternal);
        }
    }

    private static final class DummyTransportState extends State {
        private DummyTransportState(StateDescriptor stateDescriptor, ModularXmppClientToServerConnectionInternal connectionInternal) {
            super(stateDescriptor, connectionInternal);
        }

        @Override
        public StateTransitionResult.TransitionImpossible isTransitionToPossible(WalkStateGraphContext walkStateGraphContext) {
            return null;
        }

        @Override
        public StateTransitionResult.AttemptResult transitionInto(WalkStateGraphContext walkStateGraphContext) {
            return null;
        }
    }

    private static MockConnectionInternal createMockConnectionInternal() throws XmppStringprepException {
        ModularXmppClientToServerConnectionConfiguration config = ModularXmppClientToServerConnectionConfiguration.builder()
                        .setUsernameAndPassword("testuser", "testpassword")
                        .setXmppDomain("example.org")
                        .addModule(DummyTransportModuleDescriptor.class)
                        .build();
        ModularXmppClientToServerConnection connection = new ModularXmppClientToServerConnection(config);
        return new MockConnectionInternal(connection);
    }

    public static final class TestFastElement implements ExtensionElement {
        public static final String ELEMENT = "fast";
        public static final String NAMESPACE = "urn:xmpp:fast:0";
        public static final QName QNAME = new QName(NAMESPACE, ELEMENT);

        private final String token;

        public TestFastElement(String token) {
            this.token = token;
        }

        public String getToken() {
            return token;
        }

        @Override
        public String getElementName() {
            return ELEMENT;
        }

        @Override
        public String getNamespace() {
            return NAMESPACE;
        }

        @Override
        public XmlStringBuilder toXML(XmlEnvironment xmlEnvironment) {
            XmlStringBuilder xml = new XmlStringBuilder(this, xmlEnvironment);
            xml.optAttribute("token", token);
            xml.closeEmptyElement();
            return xml;
        }
    }

    @Test
    public void testAnonymousAuthenticationWithoutBind2() throws Exception {
        MockConnectionInternal connectionInternal = createMockConnectionInternal();
        Sasl2Authentication sasl2Authentication = new Sasl2Authentication(connectionInternal);

        Sasl2Feature sasl2Feature = new Sasl2Feature(Collections.singletonList(SASLAnonymous.NAME), Collections.emptyList());
        LoginContext loginContext = new LoginContext(null, null, Resourcepart.from("smack-resource"));

        Sasl2Nonza.Success serverSuccess = new Sasl2Nonza.Success(null, "anon@example.org", null);
        connectionInternal.queueResponse(serverSuccess);

        Sasl2AuthenticationResult result = sasl2Authentication.authenticate(loginContext, sasl2Feature, (Collection<? extends XmlElement>) null);

        assertEquals(SASLAnonymous.NAME, result.getUsedSaslMechanism().getName());
        assertEquals("anon@example.org", result.getAuthorizationIdentifier().toString());
        assertFalse(result.isResourceBound());

        assertEquals(1, connectionInternal.getSentNonzas().size());
        assertTrue(connectionInternal.getSentNonzas().get(0) instanceof Sasl2Nonza.Authenticate);
        Sasl2Nonza.Authenticate sentAuth = (Sasl2Nonza.Authenticate) connectionInternal.getSentNonzas().get(0);
        assertEquals(SASLAnonymous.NAME, sentAuth.getMechanism());
    }

    @Test
    public void testAnonymousAuthenticationWithBind2() throws Exception {
        MockConnectionInternal connectionInternal = createMockConnectionInternal();
        Sasl2Authentication sasl2Authentication = new Sasl2Authentication(connectionInternal);

        Bind2Elements.Bind bindFeature = new Bind2Elements.Bind(Collections.emptySet());
        Sasl2Feature sasl2Feature = new Sasl2Feature(Collections.singletonList(SASLAnonymous.NAME), Collections.singletonList(bindFeature));
        LoginContext loginContext = new LoginContext(null, null, Resourcepart.from("smack-resource"));
        Bind2Elements.Bind bind2Request = new Bind2Elements.Bind("smack-resource", null);

        Bind2Elements.Bound boundElement = new Bind2Elements.Bound(null);
        Sasl2Nonza.Success serverSuccess = new Sasl2Nonza.Success(null, "anon@example.org/smack-resource", Collections.singletonList(boundElement));
        connectionInternal.queueResponse(serverSuccess);

        Sasl2AuthenticationResult result = sasl2Authentication.authenticate(loginContext, sasl2Feature, Collections.singletonList(bind2Request));

        assertEquals(SASLAnonymous.NAME, result.getUsedSaslMechanism().getName());
        assertEquals("anon@example.org/smack-resource", result.getAuthorizationIdentifier().toString());
        assertTrue(result.isResourceBound());
        assertEquals(JidCreate.entityFullFrom("anon@example.org/smack-resource"), result.getBoundFullJid());
        assertEquals(Resourcepart.from("smack-resource"), result.getBoundResource());
        assertNotNull(result.getSuccessExtension(Bind2Elements.Bound.class));

        assertEquals(1, connectionInternal.getSentNonzas().size());
        Sasl2Nonza.Authenticate sentAuth = (Sasl2Nonza.Authenticate) connectionInternal.getSentNonzas().get(0);
        assertEquals(SASLAnonymous.NAME, sentAuth.getMechanism());
        assertEquals(1, sentAuth.getExtensionElements().size());
        assertTrue(sentAuth.getExtensionElements().get(0) instanceof Bind2Elements.Bind);
    }

    public static final class TestMultiStepMechanism extends SASLMechanism {
        public static final String NAME = "TEST-MULTI-STEP";
        private boolean authenticated;

        @Override
        public String getName() {
            return NAME;
        }

        @Override
        public int getPriority() {
            return 1000;
        }

        @Override
        public TestMultiStepMechanism newInstance() {
            return new TestMultiStepMechanism();
        }

        @Override
        protected byte[] getAuthenticationText() {
            return "initial-data".getBytes(StandardCharsets.UTF_8);
        }

        @Override
        protected void authenticateInternal(CallbackHandler cbh) {
        }

        @Override
        protected byte[] evaluateChallenge(byte[] challenge) {
            return "step2-data".getBytes(StandardCharsets.UTF_8);
        }

        @Override
        public void checkIfSuccessfulOrThrow() {
            authenticated = true;
        }

        public boolean isAuthenticated() {
            return authenticated;
        }
    }

    @Test
    public void testMultiStepAuthentication() throws Exception {
        SASLAuthentication.registerSASLMechanism(new TestMultiStepMechanism());
        try {
            MockConnectionInternal connectionInternal = createMockConnectionInternal();
            Sasl2Authentication sasl2Authentication = new Sasl2Authentication(connectionInternal);

            Sasl2Feature sasl2Feature = new Sasl2Feature(Collections.singletonList(TestMultiStepMechanism.NAME), Collections.emptyList());
            LoginContext loginContext = new LoginContext("user", "pencil", Resourcepart.from("smack-res"));

            // Step 1: Server receives Authenticate and replies with Challenge
            connectionInternal.queueHandler(nonza -> {
                assertTrue(nonza instanceof Sasl2Nonza.Authenticate);
                Sasl2Nonza.Authenticate auth = (Sasl2Nonza.Authenticate) nonza;
                assertEquals("initial-data", Base64.decodeToString(auth.getInitialResponse()));
                return new Sasl2Nonza.Challenge(Base64.encode("server-challenge-1"));
            });

            // Step 2: Server receives Response and sends Success with additional-data
            connectionInternal.queueHandler(nonza -> {
                assertTrue(nonza instanceof Sasl2Nonza.Response);
                Sasl2Nonza.Response resp = (Sasl2Nonza.Response) nonza;
                assertEquals("step2-data", Base64.decodeToString(resp.getData()));
                return new Sasl2Nonza.Success(Base64.encode("server-success-data"), "user@example.org", null);
            });

            Sasl2AuthenticationResult result = sasl2Authentication.authenticate(loginContext, sasl2Feature, (Collection<? extends XmlElement>) null);

            assertEquals(TestMultiStepMechanism.NAME, result.getUsedSaslMechanism().getName());
            assertTrue(((TestMultiStepMechanism) result.getUsedSaslMechanism()).isAuthenticated());
            assertEquals(2, connectionInternal.getSentNonzas().size());
            assertTrue(connectionInternal.getSentNonzas().get(0) instanceof Sasl2Nonza.Authenticate);
            assertTrue(connectionInternal.getSentNonzas().get(1) instanceof Sasl2Nonza.Response);
        } finally {
            SASLAuthentication.unregisterSASLMechanism(TestMultiStepMechanism.class.getName());
        }
    }

    @Test
    public void testMechanismFilter() throws Exception {
        SASLAuthentication.registerSASLMechanism(new TestMultiStepMechanism());
        try {
            MockConnectionInternal connectionInternal = createMockConnectionInternal();
            Sasl2Authentication sasl2Authentication = new Sasl2Authentication(connectionInternal);

            // Server announces both TestMultiStepMechanism (prio 1000) and SASLAnonymous (prio 0)
            Sasl2Feature sasl2Feature = new Sasl2Feature(
                Arrays.asList(TestMultiStepMechanism.NAME, SASLAnonymous.NAME),
                Collections.emptyList()
            );
            LoginContext loginContext = new LoginContext("user", "pencil", Resourcepart.from("smack-res"));

            // Filter out TestMultiStepMechanism (allow only mechanisms other than TestMultiStepMechanism)
            Function<SASLMechanism, String> mechanismFilter = mech -> mech.getName().equals(TestMultiStepMechanism.NAME) ? "excluded by test filter" : null;

            Sasl2Nonza.Success serverSuccess = new Sasl2Nonza.Success(null, "anon@example.org", null);
            connectionInternal.queueResponse(serverSuccess);

            Sasl2AuthenticationResult result = sasl2Authentication.authenticate(
                loginContext,
                sasl2Feature,
                null,
                mechanismFilter
            );

            // SASLAnonymous should have been selected because TestMultiStepMechanism was filtered out
            assertEquals(SASLAnonymous.NAME, result.getUsedSaslMechanism().getName());
        } finally {
            SASLAuthentication.unregisterSASLMechanism(TestMultiStepMechanism.class.getName());
        }
    }

    @Test
    public void testExplicitMechanismAuthentication() throws Exception {
        MockConnectionInternal connectionInternal = createMockConnectionInternal();
        Sasl2Authentication sasl2Authentication = new Sasl2Authentication(connectionInternal);

        TestMultiStepMechanism explicitMech = new TestMultiStepMechanism();
        LoginContext loginContext = new LoginContext("user", "pencil", Resourcepart.from("smack-res"));

        connectionInternal.queueResponse(new Sasl2Nonza.Success(null, "user@example.org", null));

        Sasl2AuthenticationResult result = sasl2Authentication.authenticate(
            explicitMech,
            loginContext,
            null
        );

        assertEquals(TestMultiStepMechanism.NAME, result.getUsedSaslMechanism().getName());
        assertEquals(explicitMech, result.getUsedSaslMechanism());
    }

    @Test
    public void testMultipleRequestExtensionsAndSuccessExtensionLookup() throws Exception {
        MockConnectionInternal connectionInternal = createMockConnectionInternal();
        Sasl2Authentication sasl2Authentication = new Sasl2Authentication(connectionInternal);

        Sasl2Feature sasl2Feature = new Sasl2Feature(Collections.singletonList(SASLAnonymous.NAME), Collections.emptyList());
        LoginContext loginContext = new LoginContext(null, null, Resourcepart.from("smack-resource"));

        Bind2Elements.Bind bind2Request = new Bind2Elements.Bind("smack-resource", null);
        TestFastElement fastRequest = new TestFastElement("client-fast-token-request");
        List<XmlElement> requestExtensions = Arrays.asList(bind2Request, fastRequest);

        Bind2Elements.Bound boundElement = new Bind2Elements.Bound(null);
        TestFastElement fastResponse = new TestFastElement("server-fast-token-issued");
        List<XmlElement> responseExtensions = Arrays.asList(boundElement, fastResponse);

        Sasl2Nonza.Success serverSuccess = new Sasl2Nonza.Success(
            null,
            "anon@example.org/smack-resource",
            responseExtensions
        );
        connectionInternal.queueResponse(serverSuccess);

        Sasl2AuthenticationResult result = sasl2Authentication.authenticate(
            loginContext,
            sasl2Feature,
            requestExtensions
        );

        assertTrue(result.isResourceBound());
        assertEquals(2, result.getSuccessExtensions().size());

        // Test querying extensions
        TestFastElement extractedFastByClass = result.getSuccessExtension(TestFastElement.class);
        assertNotNull(extractedFastByClass);
        assertEquals("server-fast-token-issued", extractedFastByClass.getToken());

        TestFastElement extractedFastByQName = (TestFastElement) result.getSuccessExtension(TestFastElement.QNAME);
        assertNotNull(extractedFastByQName);
        assertEquals("server-fast-token-issued", extractedFastByQName.getToken());

        TestFastElement extractedFastByName = (TestFastElement) result.getSuccessExtension(TestFastElement.ELEMENT, TestFastElement.NAMESPACE);
        assertNotNull(extractedFastByName);
        assertEquals("server-fast-token-issued", extractedFastByName.getToken());

        assertNull(result.getSuccessExtension("non-existent", "urn:xmpp:non-existent"));

        // Verify sent nonza contained both extensions
        assertEquals(1, connectionInternal.getSentNonzas().size());
        Sasl2Nonza.Authenticate sentAuth = (Sasl2Nonza.Authenticate) connectionInternal.getSentNonzas().get(0);
        assertEquals(2, sentAuth.getExtensionElements().size());
    }

    @Test
    public void testSasl2AuthenticationFailure() throws Exception {
        MockConnectionInternal connectionInternal = createMockConnectionInternal();
        Sasl2Authentication sasl2Authentication = new Sasl2Authentication(connectionInternal);

        Sasl2Feature sasl2Feature = new Sasl2Feature(Collections.singletonList(SASLAnonymous.NAME), Collections.emptyList());
        LoginContext loginContext = new LoginContext(null, null, Resourcepart.from("smack-res"));

        Sasl2Nonza.Failure failureNonza = new Sasl2Nonza.Failure(SASLError.not_authorized, Collections.singletonMap("en", "Invalid credentials"), null);
        connectionInternal.queueResponse(failureNonza);

        SASLErrorException thrown = assertThrows(SASLErrorException.class, () ->
            sasl2Authentication.authenticate(loginContext, sasl2Feature, (Collection<? extends XmlElement>) null)
        );

        assertEquals(SASLAnonymous.NAME, thrown.getMechanism());
        assertNotNull(thrown.getSasl2Failure());
        assertEquals(SASLError.not_authorized, thrown.getSasl2Failure().getSASLError());
        assertEquals("Invalid credentials", thrown.getSasl2Failure().getDescriptiveText());
    }

    @Test
    public void testSasl2ContinueUnsupported() throws Exception {
        MockConnectionInternal connectionInternal = createMockConnectionInternal();
        Sasl2Authentication sasl2Authentication = new Sasl2Authentication(connectionInternal);

        Sasl2Feature sasl2Feature = new Sasl2Feature(Collections.singletonList(SASLAnonymous.NAME), Collections.emptyList());
        LoginContext loginContext = new LoginContext(null, null, Resourcepart.from("smack-res"));

        Sasl2Nonza.Continue continueNonza = new Sasl2Nonza.Continue(null, Collections.singletonList("some-task"), null);
        connectionInternal.queueResponse(continueNonza);

        SmackSaslException thrown = assertThrows(SmackSaslException.class, () ->
            sasl2Authentication.authenticate(loginContext, sasl2Feature, (Collection<? extends XmlElement>) null)
        );

        assertTrue(thrown.getMessage().contains("continue"));
    }
}
