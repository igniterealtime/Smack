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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.c2s.internal.WalkStateGraphContext;
import org.jivesoftware.smack.fsm.StateDescriptorGraph.GraphVertex;

public abstract class StateMachineException extends SmackException {

    private static final long serialVersionUID = 1L;

    protected StateMachineException(String message) {
        super(message);
    }

    protected StateMachineException() {
        super();
    }

    public static class SmackMandatoryStateFailedException extends StateMachineException {

        private static final long serialVersionUID = 1L;

        public SmackMandatoryStateFailedException(State state, StateTransitionResult failureReason) {
        }
    }

    public static final class SmackStateGraphDeadEndException extends StateMachineException {

        private final List<State> walkedStateGraphPath;

        private final Map<State, StateTransitionResult> failedStates;

        private final StateDescriptor deadEndState;

        private static final long serialVersionUID = 1L;

        private SmackStateGraphDeadEndException(String message, WalkStateGraphContext walkStateGraphContext, GraphVertex<State> stateVertex) {
            super(message);
            this.walkedStateGraphPath = Collections.unmodifiableList(walkStateGraphContext.getWalk());
            this.failedStates = Collections.unmodifiableMap(walkStateGraphContext.getFailedStates());

            deadEndState = stateVertex.getElement().getStateDescriptor();
        }

        public List<State> getWalkedStateGraph() {
            return walkedStateGraphPath;
        }

        public Map<State, StateTransitionResult> getFailedStates() {
            return failedStates;
        }

        public StateDescriptor getDeadEndState() {
            return deadEndState;
        }

        public static SmackStateGraphDeadEndException from(WalkStateGraphContext walkStateGraphContext, GraphVertex<State> stateVertex) {
            String message = stateVertex + " has no successor vertexes";

            return new SmackStateGraphDeadEndException(message, walkStateGraphContext, stateVertex);
        }
    }
}
