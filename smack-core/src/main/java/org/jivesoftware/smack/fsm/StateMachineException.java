/**
 *
 * Copyright 2018-2019 Florian Schmaus
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
import org.jivesoftware.smack.fsm.AbstractXmppStateMachineConnection.State;
import org.jivesoftware.smack.fsm.AbstractXmppStateMachineConnection.TransitionReason;

public abstract class StateMachineException extends SmackException {

    private static final long serialVersionUID = 1L;

    public static class SmackMandatoryStateFailedException extends StateMachineException {

        private static final long serialVersionUID = 1L;

        SmackMandatoryStateFailedException(State state, TransitionReason failureReason) {
        }
    }

    public static final class SmackStateGraphDeadEndException extends StateMachineException {

        private final List<State> walkedStateGraphPath;

        private final Map<State, TransitionReason> failedStates;

        private static final long serialVersionUID = 1L;

        SmackStateGraphDeadEndException(List<State> walkedStateGraphPath, Map<State, TransitionReason> failedStates) {
            this.walkedStateGraphPath = Collections.unmodifiableList(walkedStateGraphPath);
            this.failedStates = Collections.unmodifiableMap(failedStates);
        }

        public List<State> getWalkedStateGraph() {
            return walkedStateGraphPath;
        }

        public Map<State, TransitionReason> getFailedStates() {
            return failedStates;
        }
    }
}
