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

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Function;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.SASLAuthentication;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.bind2.Bind2Module.Bind2StateDescriptor;
import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnectionModule;
import org.jivesoftware.smack.c2s.internal.ModularXmppClientToServerConnectionInternal;
import org.jivesoftware.smack.c2s.internal.WalkStateGraphContext;
import org.jivesoftware.smack.fast.element.FastElements;
import org.jivesoftware.smack.fsm.LoginContext;
import org.jivesoftware.smack.fsm.State;
import org.jivesoftware.smack.fsm.StateDescriptor;
import org.jivesoftware.smack.fsm.StateTransitionResult;
import org.jivesoftware.smack.packet.XmlElement;
import org.jivesoftware.smack.sasl.SASLErrorException;
import org.jivesoftware.smack.sasl.SASLMechanism;
import org.jivesoftware.smack.sasl.SaslTokenMechanism;
import org.jivesoftware.smack.sasl.ht.SaslHtMechanism;
import org.jivesoftware.smack.sasl.packet.Sasl2Feature;
import org.jivesoftware.smack.sasl.sasl2.Sasl2Authentication.Sasl2AuthenticationResult;
import org.jivesoftware.smack.sasl.sasl2.Sasl2AuthenticationHook;
import org.jivesoftware.smack.sasl.sasl2.Sasl2AuthenticationHookSupplier;
import org.jivesoftware.smack.sasl.sasl2.Sasl2Fallback;
import org.jivesoftware.smack.sasl.sasl2.Sasl2Module;
import org.jivesoftware.smack.sasl.sasl2.Sasl2Module.Sasl2AuthStateDescriptor;
import org.jivesoftware.smack.sasl.sasl2.Sasl2ModuleDescriptor;

public class FastModule extends ModularXmppClientToServerConnectionModule<FastModuleDescriptor>
                implements Sasl2AuthenticationHookSupplier {

    private final String preferredFastMechanism;
    private final boolean autoRequestToken;
    private final boolean enabled;
    private final Set<FastTokenListener> fastTokenListeners = new CopyOnWriteArraySet<>();

    private FastToken fastToken;
    private boolean invalidateToken;
    private final Sasl2AuthenticationHook authenticationHook = new FastAuthenticationHook();

    protected FastModule(FastModuleDescriptor moduleDescriptor,
                    ModularXmppClientToServerConnectionInternal connectionInternal) {
        super(moduleDescriptor, connectionInternal);
        this.preferredFastMechanism = moduleDescriptor.getPreferredFastMechanism();
        this.autoRequestToken = moduleDescriptor.isAutoRequestToken();
        this.enabled = moduleDescriptor.isEnabled();
        this.fastToken = moduleDescriptor.getFastToken();
        this.fastTokenListeners.addAll(moduleDescriptor.getFastTokenListeners());
    }

    public String getPreferredFastMechanism() {
        return preferredFastMechanism;
    }

    public boolean isAutoRequestToken() {
        return autoRequestToken;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public synchronized FastToken getFastToken() {
        if (!enabled) {
            return null;
        }
        return fastToken;
    }

    public synchronized FastToken getToken() {
        return getFastToken();
    }

    public synchronized void setFastToken(FastToken fastToken) {
        this.fastToken = fastToken;
        this.invalidateToken = false;
        if (fastToken != null) {
            for (FastTokenListener listener : fastTokenListeners) {
                listener.onFastTokenReceived(fastToken);
            }
        }
    }

    public synchronized void setToken(FastToken token) {
        setFastToken(token);
    }

    public synchronized void deleteFastToken() {
        this.fastToken = null;
        this.invalidateToken = false;
        for (FastTokenListener listener : fastTokenListeners) {
            listener.onFastTokenInvalidated();
        }
    }

    public synchronized void deleteToken() {
        deleteFastToken();
    }

    public synchronized boolean hasToken() {
        return enabled && fastToken != null && !fastToken.isExpired();
    }

    public synchronized long incrementTokenCount() {
        if (fastToken != null) {
            fastToken = fastToken.withIncrementedCount();
            return fastToken.getCount();
        }
        return 0L;
    }

    public boolean isInvalidateToken() {
        return invalidateToken;
    }

    public void setInvalidateToken(boolean invalidateToken) {
        this.invalidateToken = invalidateToken;
    }

    public void addFastTokenListener(FastTokenListener listener) {
        fastTokenListeners.add(listener);
    }

    public boolean removeFastTokenListener(FastTokenListener listener) {
        return fastTokenListeners.remove(listener);
    }

    @Override
    public Sasl2AuthenticationHook getSasl2AuthenticationHook() {
        return authenticationHook;
    }

    boolean hasUsableToken(Sasl2Feature sasl2Feature) {
        if (fastToken == null || fastToken.isExpired() || !sasl2Feature.isMechanismAvailable(fastToken.getMechanism())) {
            return false;
        }
        SASLMechanism mech = SASLAuthentication.getRegisteredSASLMechanism(fastToken.getMechanism());
        if (mech == null) {
            return false;
        }
        return getSkipReasonInternal(mech) == null;
    }

    private String getSkipReasonInternal(SASLMechanism mechanism) {
        if (!(mechanism instanceof SaslHtMechanism)) {
            return null;
        }

        if (!enabled) {
            return "FastModule is disabled";
        }

        SaslHtMechanism htMechanism = (SaslHtMechanism) mechanism;
        String tokenStr = htMechanism.getToken();
        FastToken tokenToUse = this.fastToken;

        if (tokenToUse == null && tokenStr == null) {
            return "no FAST token stored in FastModule";
        }
        if (tokenToUse != null) {
            if (tokenToUse.isExpired()) {
                return "FAST token for " + htMechanism.getName() + " is expired";
            }
            if (!htMechanism.getName().equals(tokenToUse.getMechanism())) {
                return "stored FAST token mechanism " + tokenToUse.getMechanism() + " does not match " + htMechanism.getName();
            }
        }

        return htMechanism.getChannelBindingNotSupportedReason(connectionInternal.connection);
    }

    private void prepareSelectedMechanismInternal(SASLMechanism mechanism) {
        if (!enabled) {
            return;
        }
        if (mechanism instanceof SaslTokenMechanism) {
            SaslTokenMechanism tokenMech = (SaslTokenMechanism) mechanism;
            if (tokenMech.getToken() == null && fastToken != null) {
                tokenMech.setToken(fastToken.getToken());
            }
        }
    }

    private void addAuthenticateExtensionsInternal(Sasl2Feature sasl2Feature, List<XmlElement> extensions) {
        if (!enabled) {
            return;
        }

        boolean isTokenUsable = hasUsableToken(sasl2Feature);

        if (isTokenUsable) {
            long count = incrementTokenCount();
            extensions.add(new FastElements.Fast(count > 0 ? count : null, isInvalidateToken()));
        } else if (autoRequestToken && sasl2Feature.hasInlineFeature(FastElements.Fast.class)) {
            String mechanismToRequest = selectBestAdvertisedFastMechanism(sasl2Feature);
            if (mechanismToRequest != null) {
                extensions.add(new FastElements.RequestToken(mechanismToRequest));
            }
        }
    }

    public String selectBestAdvertisedFastMechanism(Sasl2Feature sasl2Feature) {
        FastElements.Fast fastFeature = sasl2Feature.getInlineFeature(FastElements.Fast.class);
        if (fastFeature == null || fastFeature.getMechanisms() == null || fastFeature.getMechanisms().isEmpty()) {
            return null;
        }
        List<String> serverMechanisms = fastFeature.getMechanisms();
        if (preferredFastMechanism != null && serverMechanisms.contains(preferredFastMechanism)) {
            return preferredFastMechanism;
        }
        for (SASLMechanism registeredMech : SASLAuthentication.getRegisteredSASLMechanisms()) {
            if (registeredMech instanceof SaslHtMechanism && serverMechanisms.contains(registeredMech.getName())) {
                return registeredMech.getName();
            }
        }
        return serverMechanisms.get(0);
    }

    private void onSasl2SuccessInternal(Sasl2AuthenticationResult result, Collection<? extends XmlElement> authenticateExtensions) {
        if (!enabled) {
            return;
        }

        FastElements.Token fastTokenExt = result.getSuccessExtension(FastElements.Token.class);
        if (fastTokenExt != null) {
            String tokenMechanism = null;
            if (authenticateExtensions != null) {
                for (XmlElement ext : authenticateExtensions) {
                    if (ext instanceof FastElements.RequestToken) {
                        tokenMechanism = ((FastElements.RequestToken) ext).getMechanism();
                        break;
                    }
                }
            }
            if (tokenMechanism == null) {
                if (result.getUsedSaslMechanism() instanceof SaslHtMechanism) {
                    tokenMechanism = result.getUsedSaslMechanism().getName();
                } else {
                    tokenMechanism = preferredFastMechanism;
                }
            }
            setFastToken(new FastToken(fastTokenExt.getToken(), tokenMechanism, fastTokenExt.getExpiry()));
        } else if (authenticateExtensions != null) {
            for (XmlElement ext : authenticateExtensions) {
                if (ext instanceof FastElements.Fast) {
                    FastElements.Fast fastElem = (FastElements.Fast) ext;
                    if (Boolean.TRUE.equals(fastElem.isInvalidate())) {
                        deleteFastToken();
                        break;
                    }
                }
            }
        }
    }

    private Sasl2Fallback onSasl2FailureInternal(SASLMechanism failedMechanism,
                                                 Sasl2Feature sasl2Feature, Collection<? extends XmlElement> attemptedExtensions) {
        if (!enabled) {
            return null;
        }

        if (failedMechanism instanceof SaslHtMechanism) {
            // The FAST token was rejected / expired by the server. Degrade gracefully.
            deleteFastToken();

            List<XmlElement> fallbackExtensions = new ArrayList<>();
            if (attemptedExtensions != null) {
                for (XmlElement ext : attemptedExtensions) {
                    if (ext instanceof FastElements.Fast) {
                        continue;
                    }
                    fallbackExtensions.add(ext);
                }
            }

            if (autoRequestToken && sasl2Feature.hasInlineFeature(FastElements.Fast.class)) {
                String mechanismToRequest = selectBestAdvertisedFastMechanism(sasl2Feature);
                if (mechanismToRequest != null) {
                    fallbackExtensions.add(new FastElements.RequestToken(mechanismToRequest));
                }
            }

            Function<SASLMechanism, String> mechanismFilter = m ->
                m instanceof SaslHtMechanism ? "FAST authentication failed; degrading to non-FAST authentication" : null;

            return new Sasl2Fallback(fallbackExtensions, mechanismFilter);
        }

        return null;
    }

    private final class FastAuthenticationHook implements Sasl2AuthenticationHook {
        @Override
        public String getSkipReason(SASLMechanism mechanism, Sasl2Feature sasl2Feature, ConnectionConfiguration configuration) {
            if (hasUsableToken(sasl2Feature)) {
                if (!mechanism.getName().equals(fastToken.getMechanism())) {
                    return "FAST token is present for " + fastToken.getMechanism() + "; skipping " + mechanism.getName();
                }
            }
            return getSkipReasonInternal(mechanism);
        }

        @Override
        public void prepareSelectedMechanism(SASLMechanism mechanism, Sasl2Feature sasl2Feature) throws SmackException {
            prepareSelectedMechanismInternal(mechanism);
        }

        @Override
        public void addAuthenticateExtensions(Sasl2Feature sasl2Feature, LoginContext loginContext, List<XmlElement> extensions)
                        throws SmackException {
            addAuthenticateExtensionsInternal(sasl2Feature, extensions);
        }

        @Override
        public void onSasl2Success(Sasl2AuthenticationResult result, Collection<? extends XmlElement> authenticateExtensions)
                        throws SmackException {
            onSasl2SuccessInternal(result, authenticateExtensions);
        }

        @Override
        public Sasl2Fallback onSasl2Failure(SASLErrorException failure, SASLMechanism failedMechanism,
                                             Sasl2Feature sasl2Feature, Collection<? extends XmlElement> attemptedExtensions) {
            return onSasl2FailureInternal(failedMechanism, sasl2Feature, attemptedExtensions);
        }
    }

    public static final class FastStateDescriptor extends StateDescriptor {
        private FastStateDescriptor() {
            super(FastState.class, 484);

            addPredeccessor(Bind2StateDescriptor.class);
            addSuccessor(Sasl2AuthStateDescriptor.class);
            declarePrecedenceOver(Sasl2AuthStateDescriptor.class);
        }

        @Override
        protected FastModule.FastState constructState(ModularXmppClientToServerConnectionInternal connectionInternal) {
            FastModule fastModule = connectionInternal.connection.getConnectionModuleFor(FastModuleDescriptor.class);
            return fastModule.constructFastState(this, connectionInternal);
        }
    }

    private static final class FastState extends State {

        private FastState(FastStateDescriptor fastStateDescriptor,
                        ModularXmppClientToServerConnectionInternal connectionInternal) {
            super(fastStateDescriptor, connectionInternal);
        }

        @Override
        public StateTransitionResult.TransitionImpossible isTransitionToPossible(WalkStateGraphContext walkStateGraphContext) {
            FastModule fastModule = connectionInternal.connection.getConnectionModuleFor(FastModuleDescriptor.class);
            if (fastModule == null || !fastModule.isEnabled()) {
                return new StateTransitionResult.TransitionImpossibleReason("FastModule is disabled or not present");
            }

            Sasl2Feature sasl2Feature = connectionInternal.connection.getFeature(Sasl2Feature.class);
            if (sasl2Feature == null) {
                return new StateTransitionResult.TransitionImpossibleReason("SASL 2 Feature not announced");
            }

            boolean hasUsableToken = fastModule.hasUsableToken(sasl2Feature);
            boolean canRequestToken = fastModule.isAutoRequestToken() && sasl2Feature.hasInlineFeature(FastElements.Fast.class);
            if (!hasUsableToken && !canRequestToken) {
                return new StateTransitionResult.TransitionImpossibleReason("No usable FAST token nor server FAST token request support");
            }

            return null;
        }

        @Override
        public StateTransitionResult.AttemptResult transitionInto(WalkStateGraphContext walkStateGraphContext) {
            Sasl2Module sasl2Module = connectionInternal.connection.getConnectionModuleFor(Sasl2ModuleDescriptor.class);
            if (sasl2Module == null) {
                return new StateTransitionResult.Failure("SASL 2 module not found on connection");
            }

            FastModule fastModule = connectionInternal.connection.getConnectionModuleFor(FastModuleDescriptor.class);
            if (fastModule == null) {
                return new StateTransitionResult.Failure("FastModule not found on connection");
            }

            sasl2Module.getAuthenticationContext().addHook(fastModule.getSasl2AuthenticationHook());

            return new StateTransitionResult.Success("FAST configured for SASL2 authentication");
        }
    }

    public FastState constructFastState(FastStateDescriptor fastStateDescriptor,
                    ModularXmppClientToServerConnectionInternal connectionInternal) {
        return new FastState(fastStateDescriptor, connectionInternal);
    }

}
