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
import static org.junit.jupiter.api.Assertions.assertNotNull;
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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.net.ssl.SSLSession;

import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.XMPPException.FailedNonzaException;
import org.jivesoftware.smack.XmppInputOutputFilter;
import org.jivesoftware.smack.bind2.Bind2ModuleDescriptor;
import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnection;
import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnectionConfiguration;
import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnectionModule;
import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnectionModuleDescriptor;
import org.jivesoftware.smack.c2s.XmppClientToServerTransport;
import org.jivesoftware.smack.c2s.internal.ModularXmppClientToServerConnectionInternal;
import org.jivesoftware.smack.c2s.internal.WalkStateGraphContext;
import org.jivesoftware.smack.fast.FastModule;
import org.jivesoftware.smack.fast.FastModuleDescriptor;
import org.jivesoftware.smack.fast.FastToken;
import org.jivesoftware.smack.fast.FastTokenListener;
import org.jivesoftware.smack.fast.element.FastElements;
import org.jivesoftware.smack.fsm.ConnectionStateEvent;
import org.jivesoftware.smack.fsm.LoginContext;
import org.jivesoftware.smack.fsm.State;
import org.jivesoftware.smack.fsm.StateDescriptor;
import org.jivesoftware.smack.fsm.StateTransitionResult;
import org.jivesoftware.smack.internal.SmackTlsContext;
import org.jivesoftware.smack.packet.Nonza;
import org.jivesoftware.smack.packet.TopLevelStreamElement;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.sasl.SASLError;
import org.jivesoftware.smack.sasl.core.SASLAnonymous;
import org.jivesoftware.smack.sasl.ht.HtSha256NoneMechanism;
import org.jivesoftware.smack.sasl.packet.Sasl2Feature;
import org.jivesoftware.smack.sasl.packet.Sasl2Nonza;
import org.jivesoftware.smack.sasl.sasl2.Sasl2Authentication.Sasl2AuthenticationResult;
import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.jivesoftware.smack.util.stringencoder.Base64;
import org.jivesoftware.smack.xml.XmlPullParser;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.jxmpp.jid.EntityFullJid;
import org.jxmpp.jid.parts.Resourcepart;

public class Sasl2FastAuthenticationTest extends SmackTestSuite {

    @BeforeAll
    public static void setup() {
        SASLAuthentication.registerSASLMechanism(new HtSha256NoneMechanism());
        SASLAuthentication.registerSASLMechanism(new SASLAnonymous());
        SmackConfiguration.addModule(Bind2ModuleDescriptor.class);
        SmackConfiguration.addModule(Sasl2ModuleDescriptor.class);
        SmackConfiguration.addModule(FastModuleDescriptor.class);
    }

    private static final class MockConnectionInternal extends ModularXmppClientToServerConnectionInternal {
        private final List<Nonza> sentNonzas = new ArrayList<>();
        private final Queue<Function<Nonza, Nonza>> responseHandlers = new ArrayDeque<>();

        private MockConnectionInternal(ModularXmppClientToServerConnection connection) {
            super(connection, null, null, new ArrayDeque<>());
        }

        void queueHandler(Function<Nonza, Nonza> handler) {
            responseHandlers.add(handler);
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
                throw NoResponseException.newWith(connection, "No response queued for mock connection");
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
            throw new IllegalStateException("Unexpected response: " + response);
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
            public Builder(ModularXmppClientToServerConnectionConfiguration.Builder connectionConfigurationBuilder) {
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

    @Test
    public void testInitialLoginRequestsFastToken() throws Exception {
        var config = ModularXmppClientToServerConnectionConfiguration.builder()
            .setUsernameAndPassword("alice", "secret")
            .setXmppDomain("example.org")
            .addModule(DummyTransportModuleDescriptor.class)
            .with(FastModuleDescriptor.Builder.class)
                .setPreferredFastMechanism("HT-SHA-256-NONE")
                .setAutoRequestToken(true)
                .buildModule()
            .build();

        var connection = new ModularXmppClientToServerConnection(config);
        var mockInternal = new MockConnectionInternal(connection);

        // Server advertises SASL2 with ANONYMOUS and FAST inline
        FastElements.Fast fastFeature = new FastElements.Fast(Collections.singletonList("HT-SHA-256-NONE"), true);
        Sasl2Feature sasl2Feature = new Sasl2Feature(
            Arrays.asList("HT-SHA-256-NONE", "ANONYMOUS"),
            Collections.singletonList(fastFeature)
        );

        // Server responds to ANONYMOUS with a FAST token
        mockInternal.queueHandler(req -> {
            assertTrue(req instanceof Sasl2Nonza.Authenticate);
            Sasl2Nonza.Authenticate auth = (Sasl2Nonza.Authenticate) req;
            assertEquals("ANONYMOUS", auth.getMechanism());

            // Verify <request-token mechanism='HT-SHA-256-NONE'/> was included in <authenticate>
            FastElements.RequestToken reqToken = null;
            for (var ext : auth.getExtensionElements()) {
                if (ext instanceof FastElements.RequestToken) {
                    reqToken = (FastElements.RequestToken) ext;
                }
            }
            assertNotNull(reqToken);
            assertEquals("HT-SHA-256-NONE", reqToken.getMechanism());

            FastElements.Token issuedToken = new FastElements.Token("token-xyz-123", null);
            return new Sasl2Nonza.Success(null, "alice@example.org/res", Collections.singletonList(issuedToken));
        });

        LoginContext loginContext = new LoginContext("alice", "secret", Resourcepart.from("res"));
        List<org.jivesoftware.smack.packet.XmlElement> extensions = new ArrayList<>();
        extensions.add(new FastElements.RequestToken("HT-SHA-256-NONE"));

        Sasl2Authentication sasl2Auth = new Sasl2Authentication(mockInternal);
        Sasl2AuthenticationResult result = sasl2Auth.authenticate(loginContext, sasl2Feature, extensions);

        assertNotNull(result);
        assertEquals("ANONYMOUS", result.getUsedSaslMechanism().getName());
        assertTrue(result.hasSuccessExtension(FastElements.Token.class));
        assertEquals("token-xyz-123", result.getSuccessExtension(FastElements.Token.class).getToken());

        // Verify token was stored in FastModule
        FastModule fastModule = connection.getConnectionModuleFor(FastModuleDescriptor.class);
        FastToken savedToken = fastModule.getFastToken();
        assertNotNull(savedToken);
        assertEquals("token-xyz-123", savedToken.getToken());
        assertEquals("HT-SHA-256-NONE", savedToken.getMechanism());
    }

    @Test
    public void testFastTokenAuthenticationSuccess() throws Exception {
        String secretToken = "supersecretfasttoken";

        var config = ModularXmppClientToServerConnectionConfiguration.builder()
            .setUsernameAndPassword("alice", "password")
            .setXmppDomain("example.org")
            .addModule(DummyTransportModuleDescriptor.class)
            .with(FastModuleDescriptor.Builder.class)
                .setPreferredFastMechanism("HT-SHA-256-NONE")
                .buildModule()
            .build();

        var connection = new ModularXmppClientToServerConnection(config);
        var mockInternal = new MockConnectionInternal(connection);

        FastModule fastModule = connection.getConnectionModuleFor(FastModuleDescriptor.class);
        fastModule.setFastToken(new FastToken(secretToken, "HT-SHA-256-NONE"));

        FastElements.Fast fastFeature = new FastElements.Fast(Collections.singletonList("HT-SHA-256-NONE"), true);
        Sasl2Feature sasl2Feature = new Sasl2Feature(
            Arrays.asList("HT-SHA-256-NONE", "ANONYMOUS"),
            Collections.singletonList(fastFeature)
        );

        // Compute expected responder HMAC for mutual authentication
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secretToken.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] responderHmac = mac.doFinal("Responder".getBytes(StandardCharsets.US_ASCII));
        String responderAdditionalData = Base64.encodeToString(responderHmac);

        mockInternal.queueHandler(req -> {
            assertTrue(req instanceof Sasl2Nonza.Authenticate);
            Sasl2Nonza.Authenticate auth = (Sasl2Nonza.Authenticate) req;
            assertEquals("HT-SHA-256-NONE", auth.getMechanism());

            // Verify <fast count='1'/> extension
            FastElements.Fast fastExt = null;
            for (var ext : auth.getExtensionElements()) {
                if (ext instanceof FastElements.Fast) {
                    fastExt = (FastElements.Fast) ext;
                }
            }
            assertNotNull(fastExt);
            assertEquals(Long.valueOf(1), fastExt.getCount());

            return new Sasl2Nonza.Success(responderAdditionalData, "alice@example.org/res", Collections.emptyList());
        });

        LoginContext loginContext = new LoginContext("alice", "password", Resourcepart.from("res"));
        long count = fastModule.incrementTokenCount();
        List<org.jivesoftware.smack.packet.XmlElement> extensions = Collections.singletonList(new FastElements.Fast(count, false));

        Sasl2Authentication sasl2Auth = new Sasl2Authentication(mockInternal);
        Sasl2AuthenticationResult result = sasl2Auth.authenticate(loginContext, sasl2Feature, extensions);

        assertNotNull(result);
        assertEquals("HT-SHA-256-NONE", result.getUsedSaslMechanism().getName());
    }

    @Test
    public void testExpiredFastTokenGracefulFallback() throws Exception {
        var config = ModularXmppClientToServerConnectionConfiguration.builder()
            .setUsernameAndPassword("alice", "password")
            .setXmppDomain("example.org")
            .addModule(DummyTransportModuleDescriptor.class)
            .with(FastModuleDescriptor.Builder.class)
                .setPreferredFastMechanism("HT-SHA-256-NONE")
                .setAutoRequestToken(true)
                .buildModule()
            .build();

        var connection = new ModularXmppClientToServerConnection(config);
        var mockInternal = new MockConnectionInternal(connection);

        FastModule fastModule = connection.getConnectionModuleFor(FastModuleDescriptor.class);
        fastModule.setFastToken(new FastToken("expired-token", "HT-SHA-256-NONE"));

        FastElements.Fast fastFeature = new FastElements.Fast(Collections.singletonList("HT-SHA-256-NONE"), true);
        Sasl2Feature sasl2Feature = new Sasl2Feature(
            Arrays.asList("HT-SHA-256-NONE", "ANONYMOUS"),
            Collections.singletonList(fastFeature)
        );

        // 1st request with HT-SHA-256-NONE fails with credentials-expired
        mockInternal.queueHandler(req -> {
            assertTrue(req instanceof Sasl2Nonza.Authenticate);
            Sasl2Nonza.Authenticate auth = (Sasl2Nonza.Authenticate) req;
            assertEquals("HT-SHA-256-NONE", auth.getMechanism());

            return new Sasl2Nonza.Failure(SASLError.credentials_expired, null, null);
        });

        // 2nd request falls back to ANONYMOUS and requests a new token
        mockInternal.queueHandler(req -> {
            assertTrue(req instanceof Sasl2Nonza.Authenticate);
            Sasl2Nonza.Authenticate auth = (Sasl2Nonza.Authenticate) req;
            assertEquals("ANONYMOUS", auth.getMechanism());

            FastElements.Token newIssuedToken = new FastElements.Token("brand-new-token-456", null);
            return new Sasl2Nonza.Success(null, "alice@example.org/res", Collections.singletonList(newIssuedToken));
        });

        LoginContext loginContext = new LoginContext("alice", "password", Resourcepart.from("res"));
        long count = fastModule.incrementTokenCount();
        List<org.jivesoftware.smack.packet.XmlElement> extensions = new ArrayList<>();
        extensions.add(new FastElements.Fast(count, false));

        Sasl2Authentication sasl2Auth = new Sasl2Authentication(mockInternal);
        // Should NOT throw exception, but gracefully fall back!
        Sasl2AuthenticationResult result = sasl2Auth.authenticate(loginContext, sasl2Feature, extensions);

        assertNotNull(result);
        assertEquals("ANONYMOUS", result.getUsedSaslMechanism().getName());
        assertTrue(result.hasSuccessExtension(FastElements.Token.class));
        assertEquals("brand-new-token-456", result.getSuccessExtension(FastElements.Token.class).getToken());

        // Verify module now holds the new token
        FastToken savedToken = fastModule.getFastToken();
        assertNotNull(savedToken);
        assertEquals("brand-new-token-456", savedToken.getToken());
    }

    @Test
    public void testFastTokenRotation() throws Exception {
        String initialToken = "initial-token-111";

        var config = ModularXmppClientToServerConnectionConfiguration.builder()
            .setUsernameAndPassword("alice", "password")
            .setXmppDomain("example.org")
            .addModule(DummyTransportModuleDescriptor.class)
            .with(FastModuleDescriptor.Builder.class)
                .setPreferredFastMechanism("HT-SHA-256-NONE")
                .buildModule()
            .build();

        var connection = new ModularXmppClientToServerConnection(config);
        var mockInternal = new MockConnectionInternal(connection);

        FastModule fastModule = connection.getConnectionModuleFor(FastModuleDescriptor.class);
        fastModule.setFastToken(new FastToken(initialToken, "HT-SHA-256-NONE"));

        FastElements.Fast fastFeature = new FastElements.Fast(Collections.singletonList("HT-SHA-256-NONE"), true);
        Sasl2Feature sasl2Feature = new Sasl2Feature(
            Arrays.asList("HT-SHA-256-NONE", "ANONYMOUS"),
            Collections.singletonList(fastFeature)
        );

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(initialToken.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] responderHmac = mac.doFinal("Responder".getBytes(StandardCharsets.US_ASCII));
        String responderAdditionalData = Base64.encodeToString(responderHmac);

        mockInternal.queueHandler(req -> {
            assertTrue(req instanceof Sasl2Nonza.Authenticate);
            FastElements.Token rotatedToken = new FastElements.Token("rotated-token-222", null);
            return new Sasl2Nonza.Success(responderAdditionalData, "alice@example.org/res", Collections.singletonList(rotatedToken));
        });

        LoginContext loginContext = new LoginContext("alice", "password", Resourcepart.from("res"));
        long count = fastModule.incrementTokenCount();
        List<org.jivesoftware.smack.packet.XmlElement> extensions = Collections.singletonList(new FastElements.Fast(count, false));

        Sasl2Authentication sasl2Auth = new Sasl2Authentication(mockInternal);
        Sasl2AuthenticationResult result = sasl2Auth.authenticate(loginContext, sasl2Feature, extensions);

        assertNotNull(result);
        assertEquals("HT-SHA-256-NONE", result.getUsedSaslMechanism().getName());
        assertTrue(result.hasSuccessExtension(FastElements.Token.class));
        assertEquals("rotated-token-222", result.getSuccessExtension(FastElements.Token.class).getToken());

        // Verify module was updated with rotated token
        FastToken updatedToken = fastModule.getFastToken();
        assertNotNull(updatedToken);
        assertEquals("rotated-token-222", updatedToken.getToken());
    }

    public static final class ResumedElement implements org.jivesoftware.smack.packet.XmlElement {
        public static final String ELEMENT = "resumed";
        public static final String NAMESPACE = "urn:xmpp:sm:3";
        private final String previd;
        private final long h;

        public ResumedElement(String previd, long h) {
            this.previd = previd;
            this.h = h;
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
        public CharSequence toXML(org.jivesoftware.smack.packet.XmlEnvironment xmlEnvironment) {
            return "<resumed xmlns='urn:xmpp:sm:3' h='" + h + "' previd='" + previd + "'/>";
        }
    }

    @Test
    public void testFastTokenStreamResumption() throws Exception {
        String secretToken = "supersecretfasttoken";

        var config = ModularXmppClientToServerConnectionConfiguration.builder()
            .setUsernameAndPassword("alice", "password")
            .setXmppDomain("example.org")
            .addModule(DummyTransportModuleDescriptor.class)
            .with(FastModuleDescriptor.Builder.class)
                .setPreferredFastMechanism("HT-SHA-256-NONE")
                .buildModule()
            .build();

        var connection = new ModularXmppClientToServerConnection(config);
        var mockInternal = new MockConnectionInternal(connection);

        FastModule fastModule = connection.getConnectionModuleFor(FastModuleDescriptor.class);
        fastModule.setFastToken(new FastToken(secretToken, "HT-SHA-256-NONE"));

        FastElements.Fast fastFeature = new FastElements.Fast(Collections.singletonList("HT-SHA-256-NONE"), true);
        Sasl2Feature sasl2Feature = new Sasl2Feature(
            Arrays.asList("HT-SHA-256-NONE", "ANONYMOUS"),
            Collections.singletonList(fastFeature)
        );

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secretToken.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] responderHmac = mac.doFinal("Responder".getBytes(StandardCharsets.US_ASCII));
        String responderAdditionalData = Base64.encodeToString(responderHmac);

        mockInternal.queueHandler(req -> {
            assertTrue(req instanceof Sasl2Nonza.Authenticate);
            ResumedElement resumed = new ResumedElement("sm-session-id-123", 42);
            return new Sasl2Nonza.Success(responderAdditionalData, null, Collections.singletonList(resumed));
        });

        LoginContext loginContext = new LoginContext("alice", "password", Resourcepart.from("res"));
        long count = fastModule.incrementTokenCount();
        List<org.jivesoftware.smack.packet.XmlElement> extensions = Collections.singletonList(new FastElements.Fast(count, false));

        Sasl2Authentication sasl2Auth = new Sasl2Authentication(mockInternal);
        Sasl2AuthenticationResult result = sasl2Auth.authenticate(loginContext, sasl2Feature, extensions);

        assertNotNull(result);
        assertEquals("HT-SHA-256-NONE", result.getUsedSaslMechanism().getName());
        assertTrue(result.isStreamResumed());
        assertNotNull(result.getSuccessExtension("resumed", "urn:xmpp:sm:3"));
    }

    @Test
    public void testFastTokenSkipReasonsWhenNoMatchingMechanismAvailable() throws Exception {
        var config = ModularXmppClientToServerConnectionConfiguration.builder()
            .setUsernameAndPassword("alice", "password")
            .setXmppDomain("example.org")
            .addModule(DummyTransportModuleDescriptor.class)
            .with(FastModuleDescriptor.Builder.class)
                .setPreferredFastMechanism("HT-SHA-256-NONE")
                .buildModule()
            .build();

        var connection = new ModularXmppClientToServerConnection(config);
        var mockInternal = new MockConnectionInternal(connection);

        Sasl2Feature sasl2Feature = new Sasl2Feature(
            Collections.singletonList("HT-SHA-256-NONE"),
            Collections.emptyList()
        );

        LoginContext loginContext = new LoginContext("alice", "password", Resourcepart.from("res"));
        Sasl2Authentication sasl2Auth = new Sasl2Authentication(mockInternal);

        // Case 1: No token stored
        var e1 = org.junit.jupiter.api.Assertions.assertThrows(
            org.jivesoftware.smack.SmackException.SmackSaslException.class,
            () -> sasl2Auth.authenticate(loginContext, sasl2Feature, Collections.emptyList())
        );
        assertTrue(e1.getMessage().contains("no FAST token stored in FastModule"));

        // Case 2: Expired token stored
        FastModule fastModule = connection.getConnectionModuleFor(FastModuleDescriptor.class);
        FastToken expiredToken = new FastToken("tok", "HT-SHA-256-NONE", java.time.Instant.now().minusSeconds(60), 0);
        fastModule.setFastToken(expiredToken);

        var e2 = org.junit.jupiter.api.Assertions.assertThrows(
            org.jivesoftware.smack.SmackException.SmackSaslException.class,
            () -> sasl2Auth.authenticate(loginContext, sasl2Feature, Collections.emptyList())
        );
        assertTrue(e2.getMessage().contains("FAST token for HT-SHA-256-NONE is expired"));
    }

    @Test
    public void testFastTokenConfiguredViaBuilder() throws Exception {
        FastToken initialToken = new FastToken("builder-token-123", "HT-SHA-256-NONE");

        var config = ModularXmppClientToServerConnectionConfiguration.builder()
            .setUsernameAndPassword("alice", "password")
            .setXmppDomain("example.org")
            .addModule(DummyTransportModuleDescriptor.class)
            .with(FastModuleDescriptor.Builder.class)
                .setPreferredFastMechanism("HT-SHA-256-NONE")
                .setFastToken(initialToken)
                .buildModule()
            .build();

        var connection = new ModularXmppClientToServerConnection(config);
        FastModule fastModule = connection.getConnectionModuleFor(FastModuleDescriptor.class);
        assertNotNull(fastModule);
        assertEquals(initialToken, fastModule.getFastToken());
    }

    @Test
    public void testFastTokenListener() throws Exception {
        List<FastToken> receivedTokens = new ArrayList<>();
        List<Boolean> invalidations = new ArrayList<>();

        FastTokenListener listener = new org.jivesoftware.smack.fast.FastTokenListener() {
            @Override
            public void onFastTokenReceived(FastToken token) {
                receivedTokens.add(token);
            }

            @Override
            public void onFastTokenInvalidated() {
                invalidations.add(true);
            }
        };

        var config = ModularXmppClientToServerConnectionConfiguration.builder()
            .setUsernameAndPassword("alice", "password")
            .setXmppDomain("example.org")
            .addModule(DummyTransportModuleDescriptor.class)
            .with(FastModuleDescriptor.Builder.class)
                .setPreferredFastMechanism("HT-SHA-256-NONE")
                .addFastTokenListener(listener)
                .buildModule()
            .build();

        var connection = new ModularXmppClientToServerConnection(config);
        FastModule fastModule = connection.getConnectionModuleFor(FastModuleDescriptor.class);
        assertNotNull(fastModule);

        FastToken token1 = new FastToken("tok-1", "HT-SHA-256-NONE");
        fastModule.setFastToken(token1);
        assertEquals(1, receivedTokens.size());
        assertEquals("tok-1", receivedTokens.get(0).getToken());

        fastModule.deleteFastToken();
        assertEquals(1, invalidations.size());
    }

    @Test
    public void testFastTokenInvalidation() throws Exception {
        String secretToken = "supersecretfasttoken";

        var config = ModularXmppClientToServerConnectionConfiguration.builder()
            .setUsernameAndPassword("alice", "password")
            .setXmppDomain("example.org")
            .addModule(DummyTransportModuleDescriptor.class)
            .with(FastModuleDescriptor.Builder.class)
                .setPreferredFastMechanism("HT-SHA-256-NONE")
                .buildModule()
            .build();

        var connection = new ModularXmppClientToServerConnection(config);
        var mockInternal = new MockConnectionInternal(connection);

        FastModule fastModule = connection.getConnectionModuleFor(FastModuleDescriptor.class);
        fastModule.setFastToken(new FastToken(secretToken, "HT-SHA-256-NONE"));
        fastModule.setInvalidateToken(true);

        FastElements.Fast fastFeature = new FastElements.Fast(Collections.singletonList("HT-SHA-256-NONE"), true);
        Sasl2Feature sasl2Feature = new Sasl2Feature(
            Arrays.asList("HT-SHA-256-NONE", "ANONYMOUS"),
            Collections.singletonList(fastFeature)
        );

        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secretToken.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] responderHmac = mac.doFinal("Responder".getBytes(StandardCharsets.US_ASCII));
        String responderAdditionalData = Base64.encodeToString(responderHmac);

        mockInternal.queueHandler(req -> {
            assertTrue(req instanceof Sasl2Nonza.Authenticate);
            Sasl2Nonza.Authenticate auth = (Sasl2Nonza.Authenticate) req;
            assertEquals("HT-SHA-256-NONE", auth.getMechanism());

            FastElements.Fast fastExt = null;
            for (var ext : auth.getExtensionElements()) {
                if (ext instanceof FastElements.Fast) {
                    fastExt = (FastElements.Fast) ext;
                }
            }
            assertNotNull(fastExt);
            assertTrue(fastExt.isInvalidate());

            return new Sasl2Nonza.Success(responderAdditionalData, "alice@example.org/res", Collections.emptyList());
        });

        LoginContext loginContext = new LoginContext("alice", "password", Resourcepart.from("res"));
        long count = fastModule.incrementTokenCount();
        List<org.jivesoftware.smack.packet.XmlElement> extensions = Collections.singletonList(new FastElements.Fast(count, true));

        Sasl2Authentication sasl2Auth = new Sasl2Authentication(mockInternal);
        Sasl2AuthenticationResult result = sasl2Auth.authenticate(loginContext, sasl2Feature, extensions);

        assertNotNull(result);
        assertEquals("HT-SHA-256-NONE", result.getUsedSaslMechanism().getName());
        // Verify token was deleted after invalidation and flag was reset
        org.junit.jupiter.api.Assertions.assertNull(fastModule.getFastToken());
        org.junit.jupiter.api.Assertions.assertFalse(fastModule.isInvalidateToken());
    }

    @Test
    public void testUserAgentUuidValidationAndBuilderCustomization() throws Exception {
        java.util.UUID testUuid = java.util.UUID.randomUUID();
        ModularXmppClientToServerConnectionConfiguration config = ModularXmppClientToServerConnectionConfiguration.builder()
            .setXmppAddressAndPassword("alice@example.org", "password")
            .setHost("example.org")
            .addModule(DummyTransportModuleDescriptor.class)
            .with(Sasl2ModuleDescriptor.Builder.class)
                .setUserAgent(testUuid, "CustomClient", "Desktop")
                .buildModule()
            .build();

        var connection = new ModularXmppClientToServerConnection(config);
        Sasl2Module sasl2Module = connection.getConnectionModuleFor(Sasl2ModuleDescriptor.class);
        assertNotNull(sasl2Module);
        Sasl2Nonza.UserAgent userAgent = sasl2Module.getModuleDescriptor().getUserAgent();
        assertNotNull(userAgent);
        assertEquals(testUuid, userAgent.getId());
        assertEquals("CustomClient", userAgent.getSoftware());
        assertEquals("Desktop", userAgent.getDevice());

        String xml = userAgent.toXML(org.jivesoftware.smack.packet.XmlEnvironment.EMPTY).toString();
        org.jivesoftware.smack.test.util.XmlAssertUtil.assertXmlSimilar(
            "<user-agent xmlns='urn:xmpp:sasl:2' id='" + testUuid + "'><software>CustomClient</software><device>Desktop</device></user-agent>",
            xml
        );
    }

    @Test
    public void testFastTokenIneligibleChannelBindingFallbackToRequestToken() throws Exception {
        var config = ModularXmppClientToServerConnectionConfiguration.builder()
            .setUsernameAndPassword("alice", "secret")
            .setXmppDomain("example.org")
            .addModule(DummyTransportModuleDescriptor.class)
            .with(FastModuleDescriptor.Builder.class)
                .setFastToken(new FastToken("token-for-endp", "HT-SHA-256-ENDP"))
                .setPreferredFastMechanism("HT-SHA-256-NONE")
                .setAutoRequestToken(true)
                .buildModule()
            .build();

        var connection = new ModularXmppClientToServerConnection(config);
        var mockInternal = new MockConnectionInternal(connection);

        // Server advertises SASL2 with ANONYMOUS and FAST inline with HT-SHA-256-NONE
        FastElements.Fast fastFeature = new FastElements.Fast(Collections.singletonList("HT-SHA-256-NONE"), true);
        Sasl2Feature sasl2Feature = new Sasl2Feature(
            Arrays.asList("HT-SHA-256-NONE", "ANONYMOUS"),
            Collections.singletonList(fastFeature)
        );

        // Because HT-SHA-256-ENDP requires a secure TLS connection and our mock connection is not secure,
        // it should NOT use HT-SHA-256-ENDP and should instead request a token for HT-SHA-256-NONE
        mockInternal.queueHandler(req -> {
            assertTrue(req instanceof Sasl2Nonza.Authenticate);
            Sasl2Nonza.Authenticate auth = (Sasl2Nonza.Authenticate) req;
            assertEquals("ANONYMOUS", auth.getMechanism());

            FastElements.RequestToken requestTokenExt = null;
            for (var ext : auth.getExtensionElements()) {
                if (ext instanceof FastElements.RequestToken) {
                    requestTokenExt = (FastElements.RequestToken) ext;
                }
                if (ext instanceof FastElements.Fast) {
                    org.junit.jupiter.api.Assertions.fail("Should not include <fast> element when stored token mechanism is ineligible");
                }
            }
            assertNotNull(requestTokenExt);
            assertEquals("HT-SHA-256-NONE", requestTokenExt.getMechanism());

            return new Sasl2Nonza.Success(null, "alice@example.org/res",
                Collections.singletonList(new FastElements.Token("new-token-none", null)));
        });

        LoginContext loginContext = new LoginContext("alice", "secret", Resourcepart.from("res"));
        Sasl2Module sasl2Module = connection.getConnectionModuleFor(Sasl2ModuleDescriptor.class);
        assertNotNull(sasl2Module);

        Sasl2Authentication sasl2Auth = new Sasl2Authentication(mockInternal);
        // Invoke selectBestAdvertisedFastMechanism flow via extensions check
        FastModule fastModule = connection.getConnectionModuleFor(FastModuleDescriptor.class);
        assertNotNull(fastModule);
        FastToken token = fastModule.getFastToken();
        assertNotNull(token);

        // Transition with autoRequestToken enabled and ineligible token
        List<org.jivesoftware.smack.packet.XmlElement> extensions = new ArrayList<>();
        extensions.add(new FastElements.RequestToken("HT-SHA-256-NONE"));

        Sasl2AuthenticationResult result = sasl2Auth.authenticate(loginContext, sasl2Feature, extensions);
        assertNotNull(result);
        assertEquals("ANONYMOUS", result.getUsedSaslMechanism().getName());
        assertEquals("new-token-none", fastModule.getFastToken().getToken());
    }

    @Test
    public void testIntelligentSelectionOfAdvertisedFastMechanism() throws Exception {
        var config = ModularXmppClientToServerConnectionConfiguration.builder()
            .setUsernameAndPassword("alice", "secret")
            .setXmppDomain("example.org")
            .addModule(DummyTransportModuleDescriptor.class)
            .with(FastModuleDescriptor.Builder.class)
                .setPreferredFastMechanism("HT-SHA3-512-ENDP") // preferred not in server list
                .setAutoRequestToken(true)
                .buildModule()
            .build();

        var connection = new ModularXmppClientToServerConnection(config);
        var mockInternal = new MockConnectionInternal(connection);

        // Server advertises NONE (prio 66) and ENDP (prio 61)
        FastElements.Fast fastFeature = new FastElements.Fast(
            Arrays.asList("HT-SHA-256-NONE", "HT-SHA-256-ENDP"),
            true
        );
        Sasl2Feature sasl2Feature = new Sasl2Feature(
            Arrays.asList("HT-SHA-256-NONE", "HT-SHA-256-ENDP", "ANONYMOUS"),
            Collections.singletonList(fastFeature)
        );

        mockInternal.queueHandler(req -> {
            assertTrue(req instanceof Sasl2Nonza.Authenticate);
            Sasl2Nonza.Authenticate auth = (Sasl2Nonza.Authenticate) req;
            assertEquals("ANONYMOUS", auth.getMechanism());

            FastElements.RequestToken requestTokenExt = null;
            for (var ext : auth.getExtensionElements()) {
                if (ext instanceof FastElements.RequestToken) {
                    requestTokenExt = (FastElements.RequestToken) ext;
                }
            }
            assertNotNull(requestTokenExt);
            // Verify that HT-SHA-256-ENDP (higher priority) is requested over HT-SHA-256-NONE
            assertEquals("HT-SHA-256-ENDP", requestTokenExt.getMechanism());

            return new Sasl2Nonza.Success(null, "alice@example.org/res",
                Collections.singletonList(new FastElements.Token("token-for-endp", null)));
        });

        FastModule fastModule = connection.getConnectionModuleFor(FastModuleDescriptor.class);
        assertNotNull(fastModule);

        // Verify selectBestAdvertisedFastMechanism picks ENDP over NONE based on priority
        org.jivesoftware.smack.sasl.SASLMechanism bestMech = null;
        for (org.jivesoftware.smack.sasl.SASLMechanism reg : SASLAuthentication.getRegisteredSASLMechanisms()) {
            if (reg instanceof org.jivesoftware.smack.sasl.ht.SaslHtMechanism && fastFeature.getMechanisms().contains(reg.getName())) {
                bestMech = reg;
                break;
            }
        }
        assertNotNull(bestMech);
        assertEquals("HT-SHA-256-ENDP", bestMech.getName());

        LoginContext loginContext = new LoginContext("alice", "secret", Resourcepart.from("res"));
        Sasl2Authentication sasl2Auth = new Sasl2Authentication(mockInternal);
        List<org.jivesoftware.smack.packet.XmlElement> extensions = Collections.singletonList(
            new FastElements.RequestToken("HT-SHA-256-ENDP")
        );
        Sasl2AuthenticationResult result = sasl2Auth.authenticate(loginContext, sasl2Feature, extensions);
        assertNotNull(result);
        assertEquals("ANONYMOUS", result.getUsedSaslMechanism().getName());
        assertEquals("token-for-endp", fastModule.getFastToken().getToken());
    }
}
