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

import java.util.Set;
import java.util.UUID;

import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnectionConfiguration;
import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnectionModuleDescriptor;
import org.jivesoftware.smack.c2s.internal.ModularXmppClientToServerConnectionInternal;
import org.jivesoftware.smack.fsm.StateDescriptor;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.sasl.packet.Sasl2Feature;
import org.jivesoftware.smack.sasl.packet.Sasl2Nonza;
import org.jivesoftware.smack.sasl.packet.Sasl2Provider;

public final class Sasl2ModuleDescriptor extends ModularXmppClientToServerConnectionModuleDescriptor {

    static {
        ProviderManager.addStreamFeatureProvider(Sasl2Feature.QNAME, Sasl2Provider.Sasl2FeatureProvider.INSTANCE);
        ProviderManager.addExtensionProvider(Sasl2Feature.Inline.ELEMENT, Sasl2Feature.Inline.NAMESPACE, Sasl2Provider.InlineProvider.INSTANCE);
        ProviderManager.addNonzaProvider(Sasl2Provider.AuthenticateProvider.INSTANCE);
        ProviderManager.addNonzaProvider(Sasl2Provider.ChallengeProvider.INSTANCE);
        ProviderManager.addNonzaProvider(Sasl2Provider.ResponseProvider.INSTANCE);
        ProviderManager.addNonzaProvider(Sasl2Provider.SuccessProvider.INSTANCE);
        ProviderManager.addNonzaProvider(Sasl2Provider.FailureProvider.INSTANCE);
        ProviderManager.addNonzaProvider(Sasl2Provider.AbortProvider.INSTANCE);
        ProviderManager.addNonzaProvider(Sasl2Provider.ContinueProvider.INSTANCE);
        ProviderManager.addNonzaProvider(Sasl2Provider.NextProvider.INSTANCE);
        ProviderManager.addNonzaProvider(Sasl2Provider.TaskDataProvider.INSTANCE);
    }

    private final Sasl2Nonza.UserAgent userAgent;

    private Sasl2ModuleDescriptor() {
        this(new Sasl2Nonza.UserAgent(Sasl2Nonza.UserAgent.DEFAULT_SOFTWARE, null));
    }

    private Sasl2ModuleDescriptor(Sasl2Nonza.UserAgent userAgent) {
        this.userAgent = userAgent != null ? userAgent : new Sasl2Nonza.UserAgent(Sasl2Nonza.UserAgent.DEFAULT_SOFTWARE, null);
    }

    public Sasl2Nonza.UserAgent getUserAgent() {
        return userAgent;
    }

    @Override
    protected Set<Class<? extends StateDescriptor>> getStateDescriptors() {
        return org.jivesoftware.smack.util.CollectionUtil.setOf(
            Sasl2Module.Sasl2InitStateDescriptor.class,
            Sasl2Module.Sasl2AuthStateDescriptor.class
        );
    }

    @Override
    protected Sasl2Module constructXmppConnectionModule(
                    ModularXmppClientToServerConnectionInternal connectionInternal) {
        return new Sasl2Module(this, connectionInternal);
    }

    public static class Builder extends ModularXmppClientToServerConnectionModuleDescriptor.Builder {

        private Sasl2Nonza.UserAgent userAgent;

        protected Builder(ModularXmppClientToServerConnectionConfiguration.Builder connectionConfigurationBuilder) {
            super(connectionConfigurationBuilder);
        }

        public Builder setUserAgent(Sasl2Nonza.UserAgent userAgent) {
            this.userAgent = userAgent;
            return this;
        }

        public Builder setUserAgent(String software, String device) {
            this.userAgent = new Sasl2Nonza.UserAgent(software, device);
            return this;
        }

        public Builder setUserAgent(java.util.UUID id, String software, String device) {
            this.userAgent = new Sasl2Nonza.UserAgent(id, software, device);
            return this;
        }

        public Builder setUserAgent(String id, String software, String device) {
            var uuid = UUID.fromString(id);
            return setUserAgent(uuid, software, device);
        }

        @Override
        protected Sasl2ModuleDescriptor build() {
            return new Sasl2ModuleDescriptor(userAgent);
        }
    }
}
