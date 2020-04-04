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
package org.jivesoftware.smack.bind2;

import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnection.AuthenticatedAndResourceBoundStateDescriptor;
import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnection.ConnectedButUnauthenticatedStateDescriptor;
import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnection.SaslAuthenticationStateDescriptor;
import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnectionModule;
import org.jivesoftware.smack.c2s.internal.ModularXmppClientToServerConnectionInternal;
import org.jivesoftware.smack.c2s.internal.WalkStateGraphContext;
import org.jivesoftware.smack.fsm.State;
import org.jivesoftware.smack.fsm.StateDescriptor;
import org.jivesoftware.smack.fsm.StateTransitionResult;

public class Bind2Module extends ModularXmppClientToServerConnectionModule<Bind2ModuleDescriptor> {

    protected Bind2Module(Bind2ModuleDescriptor moduleDescriptor,
                    ModularXmppClientToServerConnectionInternal connectionInternal) {
        super(moduleDescriptor, connectionInternal);
    }

    public static final class Bind2StateDescriptor extends StateDescriptor {
        private Bind2StateDescriptor() {
            super(Bind2State.class, 386, StateDescriptor.Property.notImplemented);

            addPredeccessor(ConnectedButUnauthenticatedStateDescriptor.class);
            addSuccessor(AuthenticatedAndResourceBoundStateDescriptor.class);
            declarePrecedenceOver(SaslAuthenticationStateDescriptor.class);
        }

        @Override
        protected Bind2Module.Bind2State constructState(ModularXmppClientToServerConnectionInternal connectionInternal) {
            // This is the trick: the module is constructed prior the states, so we get the actual state out of the module by fetching the module from the connection.
            Bind2Module bind2Module = connectionInternal.connection.getConnectionModuleFor(Bind2ModuleDescriptor.class);
            return bind2Module.constructBind2State(this, connectionInternal);
        }
    }

    private static final class Bind2State extends State {

        private Bind2State(Bind2StateDescriptor bind2StateDescriptor,
                        ModularXmppClientToServerConnectionInternal connectionInternal) {
            super(bind2StateDescriptor, connectionInternal);
        }

        @Override
        public StateTransitionResult.TransitionImpossible isTransitionToPossible(WalkStateGraphContext walkStateGraphContext) {
            return new StateTransitionResult.TransitionImpossibleBecauseNotImplemented(stateDescriptor);
        }

        @Override
        public StateTransitionResult.AttemptResult transitionInto(WalkStateGraphContext walkStateGraphContext) {
            throw new IllegalStateException("Bind2 not implemented");
        }

    }

    public Bind2State constructBind2State(Bind2StateDescriptor bind2StateDescriptor,
                    ModularXmppClientToServerConnectionInternal connectionInternal) {
        return new Bind2State(bind2StateDescriptor, connectionInternal);
    }

}
