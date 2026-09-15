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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Consumer;

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

public class Sasl2Module extends ModularXmppClientToServerConnectionModule<Sasl2ModuleDescriptor> {

    public static final class Sasl2AuthenticationContext {
        private final List<XmlElement> extensions = new ArrayList<>();
        private final List<Sasl2AuthenticationHook> hooks = new ArrayList<>();
        private final List<Consumer<Sasl2AuthenticationResult>> successCallbacks = new ArrayList<>();

        public void addExtension(XmlElement extension) {
            if (extension != null) {
                extensions.add(extension);
            }
        }

        public void addExtensions(Collection<? extends XmlElement> extensions) {
            if (extensions != null) {
                this.extensions.addAll(extensions);
            }
        }

        public List<XmlElement> getExtensions() {
            return Collections.unmodifiableList(extensions);
        }

        public void addHook(Sasl2AuthenticationHook hook) {
            if (hook != null) {
                hooks.add(hook);
            }
        }

        public List<Sasl2AuthenticationHook> getHooks() {
            return Collections.unmodifiableList(hooks);
        }

        public void addSuccessCallback(Consumer<Sasl2AuthenticationResult> callback) {
            if (callback != null) {
                successCallbacks.add(callback);
            }
        }

        public List<Consumer<Sasl2AuthenticationResult>> getSuccessCallbacks() {
            return Collections.unmodifiableList(successCallbacks);
        }

        public void reset() {
            extensions.clear();
            hooks.clear();
            successCallbacks.clear();
        }
    }

    private final Sasl2AuthenticationContext authenticationContext = new Sasl2AuthenticationContext();
    private final Set<Sasl2AuthenticationHook> customHooks = new CopyOnWriteArraySet<>();
    private Sasl2AuthenticationResult sasl2AuthenticationResult;

    protected Sasl2Module(Sasl2ModuleDescriptor moduleDescriptor,
                    ModularXmppClientToServerConnectionInternal connectionInternal) {
        super(moduleDescriptor, connectionInternal);
    }

    public Sasl2AuthenticationContext getAuthenticationContext() {
        return authenticationContext;
    }

    public void resetContext() {
        authenticationContext.reset();
    }

    public Sasl2AuthenticationResult getSasl2AuthenticationResult() {
        return sasl2AuthenticationResult;
    }

    public void addSasl2AuthenticationHook(Sasl2AuthenticationHook hook) {
        customHooks.add(hook);
    }

    public boolean removeSasl2AuthenticationHook(Sasl2AuthenticationHook hook) {
        return customHooks.remove(hook);
    }

    public Set<Sasl2AuthenticationHook> getCustomHooks() {
        return Collections.unmodifiableSet(customHooks);
    }

    public List<Sasl2AuthenticationHook> getHooks() {
        List<Sasl2AuthenticationHook> allHooks = new ArrayList<>();
        if (connectionInternal.connection != null) {
            allHooks.addAll(connectionInternal.connection
                            .getConnectionModulesImplementing(Sasl2AuthenticationHook.class));
            for (var supplier : connectionInternal.connection.getConnectionModulesImplementing(Sasl2AuthenticationHookSupplier.class)) {
                Sasl2AuthenticationHook hook = supplier.getSasl2AuthenticationHook();
                if (hook != null) {
                    allHooks.add(hook);
                }
            }
        }
        allHooks.addAll(customHooks);
        return Collections.unmodifiableList(allHooks);
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

    private static final class Sasl2InitState extends State {

        private Sasl2Feature sasl2Feature;

        private Sasl2InitState(Sasl2InitStateDescriptor sasl2InitStateDescriptor,
                        ModularXmppClientToServerConnectionInternal connectionInternal) {
            super(sasl2InitStateDescriptor, connectionInternal);
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
        public StateTransitionResult.AttemptResult transitionInto(WalkStateGraphContext walkStateGraphContext) {
            Sasl2Module sasl2Module = connectionInternal.connection.getConnectionModuleFor(Sasl2ModuleDescriptor.class);
            sasl2Module.resetContext();

            Sasl2Nonza.UserAgent userAgent = sasl2Module.getModuleDescriptor().getUserAgent();
            sasl2Module.getAuthenticationContext().addExtension(userAgent);

            for (Sasl2AuthenticationHook hook : sasl2Module.getCustomHooks()) {
                sasl2Module.getAuthenticationContext().addHook(hook);
            }

            return StateTransitionResult.Success.EMPTY_INSTANCE;
        }

        @Override
        public void resetState() {
            sasl2Feature = null;
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

    private static final class Sasl2AuthState extends State {

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
            Sasl2Module sasl2Module = connectionInternal.connection.getConnectionModuleFor(Sasl2ModuleDescriptor.class);
            Sasl2AuthenticationContext authContext = sasl2Module.getAuthenticationContext();
            LoginContext loginContext = walkStateGraphContext.getLoginContext();

            List<XmlElement> sasl2Extensions = new ArrayList<>(authContext.getExtensions());
            List<Sasl2AuthenticationHook> hooks = new ArrayList<>(authContext.getHooks());

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

            sasl2Module.sasl2AuthenticationResult = result;

            for (Consumer<Sasl2AuthenticationResult> callback : authContext.getSuccessCallbacks()) {
                callback.accept(result);
            }

            if (!result.isResourceBound() && !result.isStreamResumed()) {
                connectionInternal.waitForFeaturesReceived("server stream features after SASL2 authentication");
            }

            return new Sasl2SuccessResult(result);
        }

        @Override
        public void resetState() {
            sasl2Feature = null;
            Sasl2Module sasl2Module = connectionInternal.connection.getConnectionModuleFor(Sasl2ModuleDescriptor.class);
            if (sasl2Module != null) {
                sasl2Module.sasl2AuthenticationResult = null;
                sasl2Module.resetContext();
            }
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

    public Sasl2InitState constructSasl2InitState(Sasl2InitStateDescriptor sasl2InitStateDescriptor,
                    ModularXmppClientToServerConnectionInternal connectionInternal) {
        return new Sasl2InitState(sasl2InitStateDescriptor, connectionInternal);
    }

    public Sasl2AuthState constructSasl2AuthState(Sasl2AuthStateDescriptor sasl2AuthStateDescriptor,
                    ModularXmppClientToServerConnectionInternal connectionInternal) {
        return new Sasl2AuthState(sasl2AuthStateDescriptor, connectionInternal);
    }

}
