/**
 *
 * Copyright 2018 Florian Schmaus
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

import org.jivesoftware.smack.fsm.AbstractXmppStateMachineConnection.State;
import org.jivesoftware.smack.fsm.AbstractXmppStateMachineConnection.TransitionFailureResult;
import org.jivesoftware.smack.fsm.AbstractXmppStateMachineConnection.TransitionImpossibleReason;
import org.jivesoftware.smack.fsm.AbstractXmppStateMachineConnection.TransitionSuccessResult;

public class ConnectionStateEvent {

    private final StateDescriptor stateDescriptor;

    private final long timestamp;

    protected ConnectionStateEvent(StateDescriptor stateDescriptor) {
        this.stateDescriptor = stateDescriptor;
        this.timestamp = System.currentTimeMillis();
    }

    public StateDescriptor getStateDescriptor() {
        return stateDescriptor;
    }

    @Override
    public String toString() {
        return  stateDescriptor.getStateName() + ' ' + getClass().getSimpleName();
    }

    public long getTimestamp() {
        return timestamp;
    }

    public static class StateRevertBackwardsWalk extends ConnectionStateEvent {
        StateRevertBackwardsWalk(State state) {
            super(state.getStateDescriptor());
        }
    }

    public static class FinalStateReached extends ConnectionStateEvent {
        FinalStateReached(State state) {
            super(state.getStateDescriptor());
        }
    }

    public static class TransitionNotPossible extends ConnectionStateEvent {
        private final TransitionImpossibleReason transitionImpossibleReason;

        TransitionNotPossible(State state, TransitionImpossibleReason reason) {
            super(state.getStateDescriptor());
            this.transitionImpossibleReason = reason;
        }

        @Override
        public String toString() {
            return super.toString() + ": " + transitionImpossibleReason;
        }
    }

    public static class AboutToTransitionInto extends ConnectionStateEvent {
        AboutToTransitionInto(State state) {
            super(state.getStateDescriptor());
        }
    }

    public static class TransitionFailed extends ConnectionStateEvent {
        private final TransitionFailureResult transitionFailedReason;

        TransitionFailed(State state, TransitionFailureResult transitionFailedReason) {
            super(state.getStateDescriptor());
            this.transitionFailedReason = transitionFailedReason;
        }

        @Override
        public String toString() {
            return super.toString() + ": " + transitionFailedReason;
        }
    }

    public static class SuccessfullyTransitionedInto extends ConnectionStateEvent {
        private final TransitionSuccessResult transitionSuccessResult;

        SuccessfullyTransitionedInto(State state, TransitionSuccessResult transitionSuccessResult) {
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
