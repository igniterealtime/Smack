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
package org.jivesoftware.smack.isr;

import java.util.Collections;
import java.util.Set;

import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnectionConfiguration;
import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnectionModuleDescriptor;
import org.jivesoftware.smack.c2s.internal.ModularXmppClientToServerConnectionInternal;
import org.jivesoftware.smack.fsm.StateDescriptor;

public class InstantStreamResumptionModuleDescriptor extends ModularXmppClientToServerConnectionModuleDescriptor {

    private static final InstantStreamResumptionModuleDescriptor INSTANCE = new InstantStreamResumptionModuleDescriptor();

    @Override
    protected Set<Class<? extends StateDescriptor>> getStateDescriptors() {
        return Collections.singleton(InstantStreamResumptionModule.InstantStreamResumptionStateDescriptor.class);
    }

    @Override
    protected InstantStreamResumptionModule constructXmppConnectionModule(
                    ModularXmppClientToServerConnectionInternal connectionInternal) {
        return new InstantStreamResumptionModule(this, connectionInternal);
    }

    public static final class Builder extends ModularXmppClientToServerConnectionModuleDescriptor.Builder {

        private Builder(ModularXmppClientToServerConnectionConfiguration.Builder connectionConfigurationBuilder) {
            super(connectionConfigurationBuilder);
        }

        @Override
        protected ModularXmppClientToServerConnectionModuleDescriptor build() {
            return INSTANCE;
        }

    }
}
