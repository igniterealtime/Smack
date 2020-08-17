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

import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnection.AuthenticatedAndResourceBoundStateDescriptor;
import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnection.AuthenticatedButUnboundStateDescriptor;
import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnection.ResourceBindingStateDescriptor;
import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnectionModule;
import org.jivesoftware.smack.c2s.internal.ModularXmppClientToServerConnectionInternal;
import org.jivesoftware.smack.c2s.internal.WalkStateGraphContext;
import org.jivesoftware.smack.compression.CompressionModule.CompressionStateDescriptor;
import org.jivesoftware.smack.fsm.State;
import org.jivesoftware.smack.fsm.StateDescriptor;
import org.jivesoftware.smack.fsm.StateTransitionResult;

public class StreamManagementModule extends ModularXmppClientToServerConnectionModule<StreamManagementModuleDescriptor> {

    protected StreamManagementModule(StreamManagementModuleDescriptor moduleDescriptor,
                    ModularXmppClientToServerConnectionInternal connectionInternal) {
        super(moduleDescriptor, connectionInternal);
    }

    private boolean useSm = true;

    private boolean useSmResumption = true;

    public static final class EnableStreamManagementStateDescriptor extends StateDescriptor {

        private EnableStreamManagementStateDescriptor() {
            super(StreamManagementModule.EnableStreamManagementState.class, 198, StateDescriptor.Property.notImplemented);

            addPredeccessor(ResourceBindingStateDescriptor.class);
            addSuccessor(AuthenticatedAndResourceBoundStateDescriptor.class);
            declarePrecedenceOver(AuthenticatedAndResourceBoundStateDescriptor.class);
        }

        @Override
        protected StreamManagementModule.EnableStreamManagementState constructState(ModularXmppClientToServerConnectionInternal connectionInternal) {
            // This is the trick: the module is constructed prior the states, so we get the actual state out of the module by fetching the module from the connection.
            StreamManagementModule smModule = connectionInternal.connection.getConnectionModuleFor(StreamManagementModuleDescriptor.class);
            return smModule.constructEnableStreamMangementState(this, connectionInternal);
        }

    }

    private EnableStreamManagementState constructEnableStreamMangementState(
                    EnableStreamManagementStateDescriptor enableStreamManagementStateDescriptor,
                    ModularXmppClientToServerConnectionInternal connectionInternal) {
        return new EnableStreamManagementState(enableStreamManagementStateDescriptor, connectionInternal);
    }

    private final class EnableStreamManagementState extends State {
        private EnableStreamManagementState(EnableStreamManagementStateDescriptor stateDescriptor,
                        ModularXmppClientToServerConnectionInternal connectionInternal) {
            super(stateDescriptor, connectionInternal);
        }

        @Override
        public StateTransitionResult.TransitionImpossible isTransitionToPossible(WalkStateGraphContext walkStateGraphContext) {
            if (!useSm) {
                return new StateTransitionResult.TransitionImpossibleReason("Stream management not enabled");
            }

            return new StateTransitionResult.TransitionImpossibleBecauseNotImplemented(stateDescriptor);
        }

        @Override
        public StateTransitionResult.AttemptResult transitionInto(WalkStateGraphContext walkStateGraphContext) {
            throw new IllegalStateException("SM not implemented");
        }
    }

    public static final class ResumeStreamStateDescriptor extends StateDescriptor {
        private ResumeStreamStateDescriptor() {
            super(StreamManagementModule.ResumeStreamState.class, 198, StateDescriptor.Property.notImplemented);

            addPredeccessor(AuthenticatedButUnboundStateDescriptor.class);
            addSuccessor(AuthenticatedAndResourceBoundStateDescriptor.class);
            declarePrecedenceOver(ResourceBindingStateDescriptor.class);
            declareInferiorityTo(CompressionStateDescriptor.class);
        }

        @Override
        protected StreamManagementModule.ResumeStreamState constructState(ModularXmppClientToServerConnectionInternal connectionInternal) {
            StreamManagementModule smModule = connectionInternal.connection.getConnectionModuleFor(StreamManagementModuleDescriptor.class);
            return smModule.constructResumeStreamState(this, connectionInternal);
        }

    }

    private ResumeStreamState constructResumeStreamState(
                    ResumeStreamStateDescriptor resumeStreamStateDescriptor,
                    ModularXmppClientToServerConnectionInternal connectionInternal) {
        return new ResumeStreamState(resumeStreamStateDescriptor, connectionInternal);
    }

    private final class ResumeStreamState extends State {
        private ResumeStreamState(ResumeStreamStateDescriptor stateDescriptor,
                        ModularXmppClientToServerConnectionInternal connectionInternal) {
            super(stateDescriptor, connectionInternal);
        }


        @Override
        public StateTransitionResult.TransitionImpossible isTransitionToPossible(WalkStateGraphContext walkStateGraphContext) {
            if (!useSmResumption) {
                return new StateTransitionResult.TransitionImpossibleReason("Stream resumption not enabled");
            }

            return new StateTransitionResult.TransitionImpossibleBecauseNotImplemented(stateDescriptor);
        }

        @Override
        public StateTransitionResult.AttemptResult transitionInto(WalkStateGraphContext walkStateGraphContext) {
            throw new IllegalStateException("Stream resumption not implemented");
        }
    }

    public void setStreamManagementEnabled(boolean useSm) {
        this.useSm = useSm;
    }

    public void setStreamResumptionEnabled(boolean useSmResumption) {
        this.useSmResumption = useSmResumption;
    }

}
