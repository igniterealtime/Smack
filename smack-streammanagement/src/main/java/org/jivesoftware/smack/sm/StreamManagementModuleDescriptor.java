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
package org.jivesoftware.smack.sm;

import java.util.HashSet;
import java.util.Set;

import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnectionConfiguration;
import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnectionModuleDescriptor;
import org.jivesoftware.smack.c2s.internal.ModularXmppClientToServerConnectionInternal;
import org.jivesoftware.smack.fsm.StateDescriptor;
import org.jivesoftware.smack.sm.StreamManagementModule.EnableStreamManagementStateDescriptor;
import org.jivesoftware.smack.sm.StreamManagementModule.ResumeStreamStateDescriptor;

public class StreamManagementModuleDescriptor extends ModularXmppClientToServerConnectionModuleDescriptor {

    private static final StreamManagementModuleDescriptor INSTANCE = new StreamManagementModuleDescriptor();

    @Override
    protected Set<Class<? extends StateDescriptor>> getStateDescriptors() {
        Set<Class<? extends StateDescriptor>> res = new HashSet<>();
        res.add(EnableStreamManagementStateDescriptor.class);
        res.add(ResumeStreamStateDescriptor.class);
        return res;
    }

    @Override
    protected StreamManagementModule constructXmppConnectionModule(
                    ModularXmppClientToServerConnectionInternal connectionInternal) {
        return new StreamManagementModule(this, connectionInternal);
    }

    public static class Builder extends ModularXmppClientToServerConnectionModuleDescriptor.Builder {

        protected Builder(ModularXmppClientToServerConnectionConfiguration.Builder connectionConfigurationBuilder) {
            super(connectionConfigurationBuilder);
        }

        @Override
        protected StreamManagementModuleDescriptor build() {
            return INSTANCE;
        }
    }
}
