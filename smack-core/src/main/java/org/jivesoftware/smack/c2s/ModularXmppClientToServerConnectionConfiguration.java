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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.fsm.StateDescriptor;
import org.jivesoftware.smack.fsm.StateDescriptorGraph;
import org.jivesoftware.smack.fsm.StateDescriptorGraph.GraphVertex;
import org.jivesoftware.smack.util.CollectionUtil;

public final class ModularXmppClientToServerConnectionConfiguration extends ConnectionConfiguration {

    final Set<ModularXmppClientToServerConnectionModuleDescriptor> moduleDescriptors;

    final GraphVertex<StateDescriptor> initialStateDescriptorVertex;

    private ModularXmppClientToServerConnectionConfiguration(Builder builder) {
        super(builder);

        moduleDescriptors = Collections.unmodifiableSet(CollectionUtil.newSetWith(builder.modulesDescriptors.values()));

        Set<Class<? extends StateDescriptor>> backwardEdgeStateDescriptors = new HashSet<>();
        // Add backward edges from configured connection modules. Note that all state descriptors from module
        // descriptors are backwards edges.
        for (ModularXmppClientToServerConnectionModuleDescriptor moduleDescriptor : moduleDescriptors) {
            Set<Class<? extends StateDescriptor>> moduleStateDescriptors = moduleDescriptor.getStateDescriptors();
            backwardEdgeStateDescriptors.addAll(moduleStateDescriptors);
        }

        try {
            initialStateDescriptorVertex = StateDescriptorGraph.constructStateDescriptorGraph(backwardEdgeStateDescriptors);
        }
        catch (InstantiationException | IllegalAccessException | IllegalArgumentException | InvocationTargetException
                        | NoSuchMethodException | SecurityException e) {
            // TODO: Depending on the exact exception thrown, this potentially indicates an invalid connection
            // configuration, e.g. there is no edge from disconnected to connected.
            throw new IllegalStateException(e);
        }
    }

    public void printStateGraphInDotFormat(PrintWriter pw, boolean breakStateName) {
        StateDescriptorGraph.stateDescriptorGraphToDot(Collections.singleton(initialStateDescriptorVertex), pw,
                        breakStateName);
    }

    public String getStateGraphInDotFormat() {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);

        printStateGraphInDotFormat(pw, true);

        return sw.toString();
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder
                    extends ConnectionConfiguration.Builder<Builder, ModularXmppClientToServerConnectionConfiguration> {

        private final Map<Class<? extends ModularXmppClientToServerConnectionModuleDescriptor>, ModularXmppClientToServerConnectionModuleDescriptor> modulesDescriptors = new HashMap<>();

        private Builder() {
            SmackConfiguration.addAllKnownModulesTo(this);
        }

        @Override
        public ModularXmppClientToServerConnectionConfiguration build() {
            return new ModularXmppClientToServerConnectionConfiguration(this);
        }

        public void addModule(ModularXmppClientToServerConnectionModuleDescriptor connectionModule) {
            Class<? extends ModularXmppClientToServerConnectionModuleDescriptor> moduleDescriptorClass = connectionModule.getClass();
            if (modulesDescriptors.containsKey(moduleDescriptorClass)) {
                throw new IllegalArgumentException("A connection module for " + moduleDescriptorClass + " is already configured");
            }
            modulesDescriptors.put(moduleDescriptorClass, connectionModule);
        }

        @SuppressWarnings("unchecked")
        public Builder addModule(Class<? extends ModularXmppClientToServerConnectionModuleDescriptor> moduleClass) {
            Class<?>[] declaredClasses = moduleClass.getDeclaredClasses();

            Class<? extends ModularXmppClientToServerConnectionModuleDescriptor.Builder> builderClass = null;
            for (Class<?> declaredClass : declaredClasses) {
                if (!ModularXmppClientToServerConnectionModuleDescriptor.Builder.class.isAssignableFrom(declaredClass)) {
                    continue;
                }

                builderClass = (Class<? extends ModularXmppClientToServerConnectionModuleDescriptor.Builder>) declaredClass;
                break;
            }

            if (builderClass == null) {
                throw new IllegalArgumentException(
                                "Found no builder for " + moduleClass + ". Delcared classes: " + Arrays.toString(declaredClasses));
            }

            return with(builderClass).buildModule();
        }

        public <B extends ModularXmppClientToServerConnectionModuleDescriptor.Builder> B with(
                        Class<? extends B> moduleDescriptorBuilderClass) {
            Constructor<? extends B> moduleDescriptorBuilderCosntructor;
            try {
                moduleDescriptorBuilderCosntructor = moduleDescriptorBuilderClass.getDeclaredConstructor(
                                ModularXmppClientToServerConnectionConfiguration.Builder.class);
            } catch (NoSuchMethodException | SecurityException e) {
                throw new IllegalArgumentException(e);
            }

            moduleDescriptorBuilderCosntructor.setAccessible(true);

            B moduleDescriptorBuilder;
            try {
                moduleDescriptorBuilder = moduleDescriptorBuilderCosntructor.newInstance(this);
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException
                            | InvocationTargetException e) {
                throw new IllegalArgumentException(e);
            }

            return moduleDescriptorBuilder;
        }

        public Builder removeModule(Class<? extends ModularXmppClientToServerConnectionModuleDescriptor> moduleClass) {
            modulesDescriptors.remove(moduleClass);
            return getThis();
        }

        public Builder removeAllModules() {
            modulesDescriptors.clear();
            return getThis();
        }

        @Override
        protected Builder getThis() {
            return this;
        }
    }
}
