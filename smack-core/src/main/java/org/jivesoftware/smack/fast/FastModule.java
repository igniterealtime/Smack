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
import org.jivesoftware.smack.sasl.sasl2.Sasl2Fallback;
import org.jivesoftware.smack.sasl.sasl2.Sasl2Module.Sasl2AuthStateDescriptor;
import org.jivesoftware.smack.sasl.sasl2.internal.Sasl2AuthenticationContext;

public class FastModule extends ModularXmppClientToServerConnectionModule<FastModuleDescriptor> {

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

    public synchronized void setFastToken(FastToken fastToken) {
        this.fastToken = fastToken;
        this.invalidateToken = false;
        if (fastToken != null) {
            for (FastTokenListener listener : fastTokenListeners) {
                listener.onFastTokenReceived(fastToken);
            }
        }
    }

    public synchronized void deleteFastToken() {
        this.fastToken = null;
        this.invalidateToken = false;
        for (FastTokenListener listener : fastTokenListeners) {
            listener.onFastTokenInvalidated();
        }
    }

    public synchronized boolean hasToken() {
        return enabled && fastToken != null && !fastToken.isExpired();
    }

    // XXX: protected?
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

    Sasl2AuthenticationHook getSasl2AuthenticationHook() {
        return authenticationHook;
    }

    boolean hasUsableToken(Sasl2Feature sasl2Feature) {
        if (fastToken == null || fastToken.isExpired() || !sasl2Feature.isMechanismAvailable(fastToken.getMechanism())) {
            return false;
        }
        SASLMechanism mech = SASLAuthentication.getRegisteredSASLMechanism(fastToken.getMechanism());
        if (!(mech instanceof SaslHtMechanism)) {
            return false;
        }
        return getConfigurationSkipReason(mech) == null && getSkipReasonInternal(mech) == null;
    }

    /**
     * Get the reason why Smack will not select the given mechanism due to its blacklist or the connection
     * configuration. FAST must not use, or request tokens for, such mechanisms, as the SASL2 authentication would
     * otherwise end up without a selectable mechanism.
     *
     * @param mechanism the SASL mechanism.
     * @return the reason why the mechanism will not be selected, or <code>null</code>.
     */
    private String getConfigurationSkipReason(SASLMechanism mechanism) {
        ConnectionConfiguration configuration = connectionInternal.connection.getConfiguration();
        return SASLAuthentication.getSkipReason(mechanism, configuration.getAuthzid(), configuration);
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

    /**
     * Select the FAST mechanism to request a token for. Only mechanisms that are advertised by the service and usable
     * by Smack are considered. The preferred FAST mechanism is selected if possible. Otherwise, mechanisms with
     * channel binding are selected over the ones without, as required by XEP-0484 § 6, followed by the SASL
     * mechanism priority.
     *
     * @param sasl2Feature the announced SASL2 stream feature.
     * @return the name of the selected FAST mechanism, or <code>null</code> if there is none.
     */
    private String selectBestAdvertisedFastMechanism(Sasl2Feature sasl2Feature) {
        FastElements.Fast fastFeature = sasl2Feature.getInlineFeature(FastElements.Fast.class);
        if (fastFeature == null || fastFeature.getMechanisms() == null) {
            return null;
        }
        List<String> serverMechanisms = fastFeature.getMechanisms();

        String bestChannelBindingMechanism = null;
        String bestMechanism = null;
        // Iterate in SASL priority order over the registered mechanisms.
        for (SASLMechanism registeredMech : SASLAuthentication.getRegisteredSASLMechanisms()) {
            if (!(registeredMech instanceof SaslHtMechanism)) {
                continue;
            }
            SaslHtMechanism htMechanism = (SaslHtMechanism) registeredMech;
            String mechanismName = htMechanism.getName();
            if (!serverMechanisms.contains(mechanismName)
                            || getConfigurationSkipReason(htMechanism) != null
                            || htMechanism.getChannelBindingNotSupportedReason(connectionInternal.connection) != null) {
                continue;
            }

            if (mechanismName.equals(preferredFastMechanism)) {
                return mechanismName;
            }
            if (bestMechanism == null) {
                bestMechanism = mechanismName;
            }
            if (bestChannelBindingMechanism == null
                            && htMechanism.getChannelBindingType() != SaslHtMechanism.ChannelBindingType.NONE) {
                bestChannelBindingMechanism = mechanismName;
            }
        }

        if (bestChannelBindingMechanism != null) {
            return bestChannelBindingMechanism;
        }
        return bestMechanism;
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

        @Override
        public void addAuthenticateExtensions(Sasl2Feature sasl2Feature, LoginContext loginContext, List<XmlElement> extensions)
                        throws SmackException {
            if (!enabled) {
                return;
            }

            boolean isTokenUsable = hasUsableToken(sasl2Feature);

            if (isTokenUsable) {
                long count = incrementTokenCount();
                extensions.add(new FastElements.Fast(count > 0 ? count : null, isInvalidateToken()));
            } else if (autoRequestToken) {
                String mechanismToRequest = selectBestAdvertisedFastMechanism(sasl2Feature);
                if (mechanismToRequest != null) {
                    extensions.add(new FastElements.RequestToken(mechanismToRequest));
                }
            }
        }

        @Override
        public void onSasl2Success(Sasl2AuthenticationResult result, Collection<? extends XmlElement> authenticateExtensions)
                        throws SmackException {
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

        @Override
        public Sasl2Fallback onSasl2Failure(SASLErrorException failure, SASLMechanism failedMechanism,
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

                if (autoRequestToken) {
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

    private final class FastState extends State {

        private FastState(FastStateDescriptor fastStateDescriptor,
                        ModularXmppClientToServerConnectionInternal connectionInternal) {
            super(fastStateDescriptor, connectionInternal);
        }

        @Override
        public StateTransitionResult.TransitionImpossible isTransitionToPossible(WalkStateGraphContext walkStateGraphContext) {
            if (!isEnabled()) {
                return new StateTransitionResult.TransitionImpossibleReason("FastModule is disabled");
            }

            Sasl2Feature sasl2Feature = connectionInternal.connection.getFeature(Sasl2Feature.class);
            if (sasl2Feature == null) {
                return new StateTransitionResult.TransitionImpossibleReason("SASL 2 Feature not announced");
            }

            boolean hasUsableToken = hasUsableToken(sasl2Feature);
            boolean canRequestToken = isAutoRequestToken() && selectBestAdvertisedFastMechanism(sasl2Feature) != null;
            if (!hasUsableToken && !canRequestToken) {
                return new StateTransitionResult.TransitionImpossibleReason("No usable FAST token and no usable FAST mechanism to request a token for");
            }

            return null;
        }

        @Override
        public StateTransitionResult.AttemptResult transitionInto(WalkStateGraphContext walkStateGraphContext) {
            Sasl2AuthenticationContext sasl2AuthContext = walkStateGraphContext.get(Sasl2AuthenticationContext.class);
            if (sasl2AuthContext == null) {
                return new StateTransitionResult.Failure("SASL 2 authentication context not found in walk state graph context");
            }

            sasl2AuthContext.addHook(authenticationHook);

            return new StateTransitionResult.Success("FAST configured for SASL2 authentication");
        }
    }

    private FastState constructFastState(FastStateDescriptor fastStateDescriptor,
                    ModularXmppClientToServerConnectionInternal connectionInternal) {
        return new FastState(fastStateDescriptor, connectionInternal);
    }

}
