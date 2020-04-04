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

import org.jivesoftware.smack.fsm.StateDescriptorGraph.GraphVertex;

public class ConnectionStateEvent {

    private final StateDescriptor currentStateDescriptor;
    private final StateDescriptor successorStateDescriptor;

    private final long timestamp;

    public ConnectionStateEvent(StateDescriptor currentStateDescriptor) {
        this(currentStateDescriptor, null);
    }

    public ConnectionStateEvent(StateDescriptor currentStateDescriptor, StateDescriptor successorStateDescriptor) {
        this.currentStateDescriptor = currentStateDescriptor;
        this.successorStateDescriptor = successorStateDescriptor;
        this.timestamp = System.currentTimeMillis();
    }

    public StateDescriptor getStateDescriptor() {
        return currentStateDescriptor;
    }

    @Override
    public String toString() {
        if (successorStateDescriptor == null) {
            return getClass().getSimpleName() + ": " + currentStateDescriptor.getStateName();
        } else {
            return currentStateDescriptor.getStateName() + ' ' + getClass().getSimpleName() + ' '
                            + successorStateDescriptor.getStateName();
        }
    }

    public long getTimestamp() {
        return timestamp;
    }

    public static class StateRevertBackwardsWalk extends ConnectionStateEvent {
        public StateRevertBackwardsWalk(State state) {
            super(state.getStateDescriptor());
        }
    }

    public static class FinalStateReached extends ConnectionStateEvent {
        public FinalStateReached(State state) {
            super(state.getStateDescriptor());
        }
    }

    public static class TransitionNotPossible extends ConnectionStateEvent {
        private final StateTransitionResult.TransitionImpossible transitionImpossibleReason;

        public TransitionNotPossible(State currentState, State successorState, StateTransitionResult.TransitionImpossible reason) {
            super(currentState.getStateDescriptor(), successorState.getStateDescriptor());
            this.transitionImpossibleReason = reason;
        }

        @Override
        public String toString() {
            return super.toString() + ": " + transitionImpossibleReason;
        }
    }

    public static class AboutToTransitionInto extends ConnectionStateEvent {
        public AboutToTransitionInto(State currentState, State successorState) {
            super(currentState.getStateDescriptor(), successorState.getStateDescriptor());
        }
    }

    public static class TransitionFailed extends ConnectionStateEvent {
        private final StateTransitionResult.Failure transitionFailedReason;

        public TransitionFailed(State currentState, State failedSuccessorState, StateTransitionResult.Failure transitionFailedReason) {
            super(currentState.getStateDescriptor(), failedSuccessorState.getStateDescriptor());
            this.transitionFailedReason = transitionFailedReason;
        }

        @Override
        public String toString() {
            return super.toString() + ": " + transitionFailedReason;
        }
    }

    public static class TransitionIgnoredDueCycle extends ConnectionStateEvent {
        public TransitionIgnoredDueCycle(GraphVertex<State> currentStateVertex, GraphVertex<State> successorStateVertexCausingCycle) {
            super(currentStateVertex.getElement().getStateDescriptor(), successorStateVertexCausingCycle.getElement().getStateDescriptor());
        }
    }

    public static class SuccessfullyTransitionedInto extends ConnectionStateEvent {
        private final StateTransitionResult.Success transitionSuccessResult;

        public SuccessfullyTransitionedInto(State state, StateTransitionResult.Success transitionSuccessResult) {
            super(state.getStateDescriptor());
            this.transitionSuccessResult = transitionSuccessResult;
        }

        @Override
        public String toString() {
            return super.toString() + ": " + transitionSuccessResult;
        }
    }

    public abstract static class DetailedTransitionIntoInformation extends ConnectionStateEvent {
        protected DetailedTransitionIntoInformation(State state) {
            super(state.getStateDescriptor());
        }
    }
}
