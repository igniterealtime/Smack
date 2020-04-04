/**
 *
 * Copyright 2019-2020 Florian Schmaus
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

import java.util.Set;

import org.jivesoftware.smack.c2s.internal.ModularXmppClientToServerConnectionInternal;
import org.jivesoftware.smack.fsm.StateDescriptor;

public abstract class ModularXmppClientToServerConnectionModuleDescriptor {

    protected abstract Set<Class<? extends StateDescriptor>> getStateDescriptors();

    protected abstract ModularXmppClientToServerConnectionModule<? extends ModularXmppClientToServerConnectionModuleDescriptor> constructXmppConnectionModule(
                    ModularXmppClientToServerConnectionInternal connectionInternal);

    public abstract static class Builder {
        private final ModularXmppClientToServerConnectionConfiguration.Builder connectionConfigurationBuilder;

        protected Builder(ModularXmppClientToServerConnectionConfiguration.Builder connectionConfigurationBuilder) {
            this.connectionConfigurationBuilder = connectionConfigurationBuilder;
        }

        protected abstract ModularXmppClientToServerConnectionModuleDescriptor build();

        public ModularXmppClientToServerConnectionConfiguration.Builder buildModule() {
            ModularXmppClientToServerConnectionModuleDescriptor moduleDescriptor = build();
            connectionConfigurationBuilder.addModule(moduleDescriptor);
            return connectionConfigurationBuilder;
        }

    }

}
