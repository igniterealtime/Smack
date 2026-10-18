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

import java.io.IOException;
import java.util.List;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnection.AuthenticatedAndResourceBoundStateDescriptor;
import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnection.AuthenticatedButUnboundStateDescriptor;
import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnection.ConnectedButUnauthenticatedStateDescriptor;
import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnection.SaslAuthenticationStateDescriptor;
import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnectionModule;
import org.jivesoftware.smack.c2s.internal.ModularXmppClientToServerConnectionInternal;
import org.jivesoftware.smack.c2s.internal.WalkStateGraphContext;
import org.jivesoftware.smack.fsm.LoginContext;
import org.jivesoftware.smack.fsm.State;
import org.jivesoftware.smack.fsm.StateDescriptor;
import org.jivesoftware.smack.fsm.StateTransitionResult;
import org.jivesoftware.smack.packet.XmlElement;
import org.jivesoftware.smack.sasl.packet.Sasl2Feature;
import org.jivesoftware.smack.sasl.packet.Sasl2Nonza;
import org.jivesoftware.smack.sasl.sasl2.Sasl2Authentication.Sasl2AuthenticationResult;
import org.jivesoftware.smack.sasl.sasl2.internal.Sasl2AuthenticationContext;

public class Sasl2Module extends ModularXmppClientToServerConnectionModule<Sasl2ModuleDescriptor> {

    private Sasl2AuthenticationResult sasl2AuthenticationResult;

    protected Sasl2Module(Sasl2ModuleDescriptor moduleDescriptor,
                    ModularXmppClientToServerConnectionInternal connectionInternal) {
        super(moduleDescriptor, connectionInternal);
    }

    public Sasl2Nonza.UserAgent getUserAgent() {
        return moduleDescriptor.getUserAgent();
    }

    public Sasl2AuthenticationResult getSasl2AuthenticationResult() {
        return sasl2AuthenticationResult;
    }

    public static final class Sasl2InitStateDescriptor extends StateDescriptor {
        private Sasl2InitStateDescriptor() {
            super(Sasl2InitState.class, 388);

            addPredeccessor(ConnectedButUnauthenticatedStateDescriptor.class);
            addSuccessor(Sasl2AuthStateDescriptor.class);
            declarePrecedenceOver(SaslAuthenticationStateDescriptor.class);
        }

        @Override
        protected Sasl2Module.Sasl2InitState constructState(ModularXmppClientToServerConnectionInternal connectionInternal) {
            Sasl2Module sasl2Module = connectionInternal.connection.getConnectionModuleFor(Sasl2ModuleDescriptor.class);
            return sasl2Module.constructSasl2InitState(this, connectionInternal);
        }
    }

    private final class Sasl2InitState extends State {

        private Sasl2InitState(Sasl2InitStateDescriptor sasl2InitStateDescriptor,
                        ModularXmppClientToServerConnectionInternal connectionInternal) {
            super(sasl2InitStateDescriptor, connectionInternal);
        }

        @Override
        public StateTransitionResult.TransitionImpossible isTransitionToPossible(WalkStateGraphContext walkStateGraphContext) {
            Sasl2Feature sasl2Feature = connectionInternal.connection.getFeature(Sasl2Feature.class);
            if (sasl2Feature == null) {
                return new StateTransitionResult.TransitionImpossibleReason("SASL 2 Feature not supported/announced by service");
            }

            return null;
        }

        @Override
        public StateTransitionResult.AttemptResult transitionInto(WalkStateGraphContext walkStateGraphContext) {
            Sasl2AuthenticationContext authContext = new Sasl2AuthenticationContext();
            walkStateGraphContext.set(Sasl2AuthenticationContext.class, authContext);

            Sasl2Nonza.UserAgent userAgent = getUserAgent();
            authContext.addExtension(userAgent);

            return StateTransitionResult.Success.EMPTY_INSTANCE;
        }

    }

    public static final class Sasl2AuthStateDescriptor extends StateDescriptor {
        private Sasl2AuthStateDescriptor() {
            super(Sasl2AuthState.class, 388);

            addSuccessor(AuthenticatedAndResourceBoundStateDescriptor.class);
            addSuccessor(AuthenticatedButUnboundStateDescriptor.class);
        }

        @Override
        protected Sasl2Module.Sasl2AuthState constructState(ModularXmppClientToServerConnectionInternal connectionInternal) {
            Sasl2Module sasl2Module = connectionInternal.connection.getConnectionModuleFor(Sasl2ModuleDescriptor.class);
            return sasl2Module.constructSasl2AuthState(this, connectionInternal);
        }
    }

    private final class Sasl2AuthState extends State {

        private Sasl2Feature sasl2Feature;

        private Sasl2AuthState(Sasl2AuthStateDescriptor sasl2AuthStateDescriptor,
                        ModularXmppClientToServerConnectionInternal connectionInternal) {
            super(sasl2AuthStateDescriptor, connectionInternal);
        }

        @Override
        public StateTransitionResult.TransitionImpossible isTransitionToPossible(WalkStateGraphContext walkStateGraphContext) {
            sasl2Feature = connectionInternal.connection.getFeature(Sasl2Feature.class);
            if (sasl2Feature == null) {
                return new StateTransitionResult.TransitionImpossibleReason("SASL 2 Feature not announced");
            }

            return null;
        }

        @Override
        public StateTransitionResult.AttemptResult transitionInto(WalkStateGraphContext walkStateGraphContext)
                        throws SmackException, XMPPException, IOException, InterruptedException {
            LoginContext loginContext = walkStateGraphContext.getLoginContext();

            Sasl2AuthenticationContext authContext = walkStateGraphContext.get(Sasl2AuthenticationContext.class);
            assert authContext != null;

            List<XmlElement> sasl2Extensions = authContext.getExtensions();
            List<Sasl2AuthenticationHook> hooks = authContext.getHooks();

            for (Sasl2AuthenticationHook hook : hooks) {
                hook.addAuthenticateExtensions(sasl2Feature, loginContext, sasl2Extensions);
            }

            connectionInternal.prepareToWaitForFeaturesReceived();

            Sasl2Authentication sasl2Authentication = new Sasl2Authentication(connectionInternal, hooks);
            Sasl2AuthenticationResult result = sasl2Authentication.authenticate(
                loginContext,
                sasl2Feature,
                sasl2Extensions
            );

            sasl2AuthenticationResult = result;

            authContext.forEachSuccessCallback(result);

            // XXX: is this possible? is this required?
            if (!result.isResourceBound() && !result.isStreamResumed()) {
                connectionInternal.waitForFeaturesReceived("server stream features after SASL2 authentication");
            }

            return new Sasl2SuccessResult(result);
        }

        @Override
        public void resetState() {
            sasl2Feature = null;
            sasl2AuthenticationResult = null;
        }
    }

    public static final class Sasl2SuccessResult extends StateTransitionResult.Success {
        private final Sasl2AuthenticationResult sasl2AuthenticationResult;

        public Sasl2SuccessResult(Sasl2AuthenticationResult sasl2AuthenticationResult) {
            super("SASL2 authentication successful using " + sasl2AuthenticationResult.getUsedSaslMechanism().getName());
            this.sasl2AuthenticationResult = sasl2AuthenticationResult;
        }

        public Sasl2AuthenticationResult getSasl2AuthenticationResult() {
            return sasl2AuthenticationResult;
        }
    }

    private Sasl2InitState constructSasl2InitState(Sasl2InitStateDescriptor sasl2InitStateDescriptor,
                    ModularXmppClientToServerConnectionInternal connectionInternal) {
        return new Sasl2InitState(sasl2InitStateDescriptor, connectionInternal);
    }

    private Sasl2AuthState constructSasl2AuthState(Sasl2AuthStateDescriptor sasl2AuthStateDescriptor,
                    ModularXmppClientToServerConnectionInternal connectionInternal) {
        return new Sasl2AuthState(sasl2AuthStateDescriptor, connectionInternal);
    }

}
