/*
 *
 * Copyright 2019-2026 Florian Schmaus
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

import java.util.Collections;
import java.util.Set;

import org.jivesoftware.smack.bind2.element.Bind2Elements;
import org.jivesoftware.smack.bind2.provider.Bind2Provider;
import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnectionConfiguration;
import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnectionModuleDescriptor;
import org.jivesoftware.smack.c2s.internal.ModularXmppClientToServerConnectionInternal;
import org.jivesoftware.smack.fsm.StateDescriptor;
import org.jivesoftware.smack.provider.ProviderManager;

public class Bind2ModuleDescriptor extends ModularXmppClientToServerConnectionModuleDescriptor {

    static {
        ProviderManager.addExtensionProvider(Bind2Elements.Bind.ELEMENT, Bind2Elements.NAMESPACE, Bind2Provider.BindProvider.INSTANCE);
        ProviderManager.addExtensionProvider(Bind2Elements.Bound.ELEMENT, Bind2Elements.NAMESPACE, Bind2Provider.BoundProvider.INSTANCE);
    }

    private static final Bind2ModuleDescriptor INSTANCE = new Bind2ModuleDescriptor();

    @Override
    protected Set<Class<? extends StateDescriptor>> getStateDescriptors() {
        return Collections.singleton(Bind2Module.Bind2StateDescriptor.class);
    }

    @Override
    protected Bind2Module constructXmppConnectionModule(
                    ModularXmppClientToServerConnectionInternal connectionInternal) {
        return new Bind2Module(this, connectionInternal);
    }

    public static class Builder extends ModularXmppClientToServerConnectionModuleDescriptor.Builder {

        protected Builder(ModularXmppClientToServerConnectionConfiguration.Builder connectionConfigurationBuilder) {
            super(connectionConfigurationBuilder);
        }

        @Override
        protected Bind2ModuleDescriptor build() {
            return INSTANCE;
        }
    }
}
