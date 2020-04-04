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

public abstract class StateTransitionResult {

    private final String message;

    protected StateTransitionResult(String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return message;
    }

    public abstract static class AttemptResult extends StateTransitionResult {
        protected AttemptResult(String message) {
            super(message);
        }
    }

    public static class Success extends AttemptResult {

        public static final Success EMPTY_INSTANCE = new Success();

        private Success() {
            super("");
        }

        public Success(String successMessage) {
            super(successMessage);
        }
    }

    public static class Failure extends AttemptResult {
        public Failure(String failureMessage) {
            super(failureMessage);
        }
    }

    public static final class FailureCausedByException<E extends Exception> extends Failure {
        private final E exception;

        public FailureCausedByException(E exception) {
            super(exception.getMessage());
            this.exception = exception;
        }

        public E getException() {
            return exception;
        }
    }

    public abstract static class TransitionImpossible extends StateTransitionResult {
        protected TransitionImpossible(String message) {
            super(message);
        }
    }

    public static class TransitionImpossibleReason extends TransitionImpossible {
        public TransitionImpossibleReason(String reason) {
            super(reason);
        }
    }

    public static class TransitionImpossibleBecauseNotImplemented extends TransitionImpossibleReason {
        public TransitionImpossibleBecauseNotImplemented(StateDescriptor stateDescriptor) {
            super(stateDescriptor.getFullStateName(false) + " is not implemented (yet)");
        }
    }
}
