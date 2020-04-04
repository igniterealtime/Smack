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

import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnection.AuthenticatedAndResourceBoundStateDescriptor;
import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnection.ConnectedButUnauthenticatedStateDescriptor;
import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnection.SaslAuthenticationStateDescriptor;
import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnectionModule;
import org.jivesoftware.smack.c2s.internal.ModularXmppClientToServerConnectionInternal;
import org.jivesoftware.smack.c2s.internal.WalkStateGraphContext;
import org.jivesoftware.smack.fsm.State;
import org.jivesoftware.smack.fsm.StateDescriptor;
import org.jivesoftware.smack.fsm.StateTransitionResult;

public class InstantStreamResumptionModule extends ModularXmppClientToServerConnectionModule<InstantStreamResumptionModuleDescriptor> {

    protected InstantStreamResumptionModule(InstantStreamResumptionModuleDescriptor instantStreamResumptionModuleDescriptor,
                    ModularXmppClientToServerConnectionInternal connectionInternal) {
        super(instantStreamResumptionModuleDescriptor, connectionInternal);
    }

    public static final class InstantStreamResumptionStateDescriptor extends StateDescriptor {
        private InstantStreamResumptionStateDescriptor() {
            super(InstantStreamResumptionState.class, 397, StateDescriptor.Property.notImplemented);

            addSuccessor(AuthenticatedAndResourceBoundStateDescriptor.class);
            addPredeccessor(ConnectedButUnauthenticatedStateDescriptor.class);
            declarePrecedenceOver(SaslAuthenticationStateDescriptor.class);
        }

        @Override
        protected InstantStreamResumptionModule.InstantStreamResumptionState constructState(ModularXmppClientToServerConnectionInternal connectionInternal) {
            // This is the trick: the module is constructed prior the states, so we get the actual state out of the module by fetching the module from the connection.
            InstantStreamResumptionModule isrModule = connectionInternal.connection.getConnectionModuleFor(InstantStreamResumptionModuleDescriptor.class);
            return isrModule.constructInstantStreamResumptionState(this, connectionInternal);
        }
    }

    private boolean useIsr = true;

    private final class InstantStreamResumptionState extends State {
        private InstantStreamResumptionState(InstantStreamResumptionStateDescriptor instantStreamResumptionStateDescriptor,
                        ModularXmppClientToServerConnectionInternal connectionInternal) {
            super(instantStreamResumptionStateDescriptor, connectionInternal);
        }

        @Override
        public StateTransitionResult.TransitionImpossible isTransitionToPossible(WalkStateGraphContext walkStateGraphContext) {
            if (!useIsr) {
                return new StateTransitionResult.TransitionImpossibleReason("Instant stream resumption not enabled nor implemented");
            }

            return new StateTransitionResult.TransitionImpossibleBecauseNotImplemented(stateDescriptor);
        }

        @Override
        public StateTransitionResult.AttemptResult transitionInto(WalkStateGraphContext walkStateGraphContext) {
            throw new IllegalStateException("Instant stream resumption not implemented");
        }
    }

    public void setInstantStreamResumptionEnabled(boolean useIsr) {
        this.useIsr = useIsr;
    }

    public InstantStreamResumptionState constructInstantStreamResumptionState(
                    InstantStreamResumptionStateDescriptor instantStreamResumptionStateDescriptor,
                    ModularXmppClientToServerConnectionInternal connectionInternal) {
        return new InstantStreamResumptionState(instantStreamResumptionStateDescriptor, connectionInternal);
    }
}
