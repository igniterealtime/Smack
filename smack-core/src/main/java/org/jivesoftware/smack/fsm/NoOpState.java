/**
 *
 * Copyright 2018-2020 Florian Schmaus
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
package org.jivesoftware.smack.fsm;

import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnection;
import org.jivesoftware.smack.c2s.internal.ModularXmppClientToServerConnectionInternal;
import org.jivesoftware.smack.c2s.internal.WalkStateGraphContext;

public class NoOpState extends State {

    /**
     * Constructs a NoOpState. Note that the signature of this constructor is designed so that it mimics States which
     * are non-static inner classes of ModularXmppClientToServerConnection. That is why the first argument is not used.
     *
     * @param connection the connection.
     * @param stateDescriptor the related state descriptor
     * @param connectionInternal the internal connection API.
     */
    @SuppressWarnings("UnusedVariable")
    protected NoOpState(ModularXmppClientToServerConnection connection, StateDescriptor stateDescriptor, ModularXmppClientToServerConnectionInternal connectionInternal) {
        super(stateDescriptor, connectionInternal);
    }

    @Override
    public StateTransitionResult.Success transitionInto(WalkStateGraphContext walkStateGraphContext) {
        // Transition into a NoOpState always succeeds.
        return StateTransitionResult.Success.EMPTY_INSTANCE;
    }
}
