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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnectionConfiguration;
import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnectionModuleDescriptor;
import org.jivesoftware.smack.c2s.internal.ModularXmppClientToServerConnectionInternal;
import org.jivesoftware.smack.fast.element.FastElements;
import org.jivesoftware.smack.fast.provider.FastProvider;
import org.jivesoftware.smack.fsm.StateDescriptor;
import org.jivesoftware.smack.provider.ProviderManager;

public final class FastModuleDescriptor extends ModularXmppClientToServerConnectionModuleDescriptor {

    public static final String DEFAULT_PREFERRED_MECHANISM = "HT-SHA-256-ENDP";

    static {
        ProviderManager.addExtensionProvider(FastElements.Fast.ELEMENT, FastElements.NAMESPACE, FastProvider.FastElementProvider.INSTANCE);
        ProviderManager.addExtensionProvider(FastElements.RequestToken.ELEMENT, FastElements.NAMESPACE, FastProvider.RequestTokenProvider.INSTANCE);
        ProviderManager.addExtensionProvider(FastElements.Token.ELEMENT, FastElements.NAMESPACE, FastProvider.TokenProvider.INSTANCE);
    }

    private final String preferredFastMechanism;
    private final boolean autoRequestToken;
    private final boolean enabled;
    private final FastToken fastToken;
    private final Set<FastTokenListener> fastTokenListeners;

    private FastModuleDescriptor(String preferredFastMechanism,
            boolean autoRequestToken, boolean enabled, FastToken fastToken,
            Set<FastTokenListener> fastTokenListeners) {
        this.preferredFastMechanism = preferredFastMechanism;
        this.autoRequestToken = autoRequestToken;
        this.enabled = enabled;
        this.fastToken = fastToken;
        this.fastTokenListeners = Collections.unmodifiableSet(new HashSet<>(fastTokenListeners));
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

    public FastToken getFastToken() {
        return fastToken;
    }

    public Set<FastTokenListener> getFastTokenListeners() {
        return fastTokenListeners;
    }

    @Override
    protected Set<Class<? extends StateDescriptor>> getStateDescriptors() {
        return Collections.singleton(FastModule.FastStateDescriptor.class);
    }

    @Override
    protected FastModule constructXmppConnectionModule(
                    ModularXmppClientToServerConnectionInternal connectionInternal) {
        return new FastModule(this, connectionInternal);
    }

    public static Builder getBuilder(ModularXmppClientToServerConnectionConfiguration.Builder connectionConfigurationBuilder) {
        return new Builder(connectionConfigurationBuilder);
    }

    public static final class Builder extends ModularXmppClientToServerConnectionModuleDescriptor.Builder {

        private String preferredFastMechanism = DEFAULT_PREFERRED_MECHANISM;
        private boolean autoRequestToken = true;
        private boolean enabled = true;
        private FastToken fastToken;
        private final Set<FastTokenListener> fastTokenListeners = new HashSet<>();

        public Builder(ModularXmppClientToServerConnectionConfiguration.Builder connectionConfigurationBuilder) {
            super(connectionConfigurationBuilder);
        }

        public Builder setPreferredFastMechanism(String preferredFastMechanism) {
            this.preferredFastMechanism = preferredFastMechanism;
            return this;
        }

        public Builder setAutoRequestToken(boolean autoRequestToken) {
            this.autoRequestToken = autoRequestToken;
            return this;
        }

        public Builder setEnabled(boolean enabled) {
            this.enabled = enabled;
            return this;
        }

        public Builder setFastToken(FastToken fastToken) {
            this.fastToken = fastToken;
            return this;
        }

        public Builder addFastTokenListener(FastTokenListener listener) {
            this.fastTokenListeners.add(listener);
            return this;
        }

        @Override
        protected FastModuleDescriptor build() {
            return new FastModuleDescriptor(preferredFastMechanism, autoRequestToken, enabled, fastToken, fastTokenListeners);
        }
    }
}
