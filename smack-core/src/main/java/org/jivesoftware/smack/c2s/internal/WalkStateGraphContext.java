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
package org.jivesoftware.smack.c2s.internal;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnection.AuthenticatedAndResourceBoundStateDescriptor;
import org.jivesoftware.smack.fsm.LoginContext;
import org.jivesoftware.smack.fsm.State;
import org.jivesoftware.smack.fsm.StateDescriptor;
import org.jivesoftware.smack.fsm.StateDescriptorGraph.GraphVertex;
import org.jivesoftware.smack.fsm.StateTransitionResult;
import org.jivesoftware.smack.util.CollectionUtil;
import org.jivesoftware.smack.util.Objects;

import org.jxmpp.jid.parts.Resourcepart;

public final class WalkStateGraphContext {
    private final Class<? extends StateDescriptor> initialStateClass;
    private final Class<? extends StateDescriptor> finalStateClass;
    private final Class<? extends StateDescriptor> mandatoryIntermediateState;
    private final LoginContext loginContext;

    private final List<State> walkedStateGraphPath = new ArrayList<>();

    /**
     * A linked Map of failed States with their reason as value.
     */
    final Map<State, StateTransitionResult> failedStates = new LinkedHashMap<>();

    boolean mandatoryIntermediateStateHandled;

    WalkStateGraphContext(Builder builder) {
        initialStateClass = builder.initialStateClass;
        finalStateClass = builder.finalStateClass;
        mandatoryIntermediateState = builder.mandatoryIntermediateState;
        loginContext = builder.loginContext;
    }

    public void recordWalkTo(State state) {
        walkedStateGraphPath.add(state);
    }

    public boolean isWalksFinalState(StateDescriptor stateDescriptor) {
        return stateDescriptor.getClass() == finalStateClass;
    }

    public boolean isFinalStateAuthenticatedAndResourceBound() {
        return finalStateClass == AuthenticatedAndResourceBoundStateDescriptor.class;
    }

    public GraphVertex<State> maybeReturnMandatoryImmediateState(List<GraphVertex<State>> outgoingStateEdges) {
        for (GraphVertex<State> outgoingStateVertex : outgoingStateEdges) {
            if (outgoingStateVertex.getElement().getStateDescriptor().getClass() == mandatoryIntermediateState) {
                mandatoryIntermediateStateHandled = true;
                return outgoingStateVertex;
            }
        }

        return null;
    }

    public List<State> getWalk() {
        return CollectionUtil.newListWith(walkedStateGraphPath);
    }

    public int getWalkLength() {
        return walkedStateGraphPath.size();
    }

    public void appendWalkTo(List<State> walk) {
        walk.addAll(walkedStateGraphPath);
    }

    public LoginContext getLoginContext() {
        return loginContext;
    }

    public boolean stateAlreadyVisited(State state) {
        return walkedStateGraphPath.contains(state);
    }

    public void recordFailedState(State state, StateTransitionResult stateTransitionResult) {
        failedStates.put(state, stateTransitionResult);
    }

    public Map<State, StateTransitionResult> getFailedStates() {
        return new HashMap<>(failedStates);
    }

    /**
     * Check if the way to the final state via the given successor state that would loop, i.e., lead over the initial state and
     * thus from a cycle.
     *
     * @param successorStateVertex the successor state to use on the way.
     * @return <code>true</code> if it would loop, <code>false</code> otherwise.
     */
    public boolean wouldCauseCycle(GraphVertex<State> successorStateVertex) {
        Set<Class<? extends StateDescriptor>> visited = new HashSet<>();
        return wouldCycleRecursive(successorStateVertex, visited);
    }

    private boolean wouldCycleRecursive(GraphVertex<State> stateVertex, Set<Class<? extends StateDescriptor>> visited) {
        Class<? extends StateDescriptor> stateVertexClass = stateVertex.getElement().getStateDescriptor().getClass();

        if (stateVertexClass == initialStateClass) {
            return true;
        }
        if (finalStateClass == stateVertexClass || visited.contains(stateVertexClass)) {
            return false;
        }

        visited.add(stateVertexClass);

        for (GraphVertex<State> successorStateVertex : stateVertex.getOutgoingEdges()) {
            boolean cycle = wouldCycleRecursive(successorStateVertex, visited);
            if (cycle) {
                return true;
            }
        }

        return false;
    }

    public static Builder builder(Class<? extends StateDescriptor> initialStateClass, Class<? extends StateDescriptor> finalStateClass) {
        return new Builder(initialStateClass, finalStateClass);
    }

    public static final class Builder {
        private final Class<? extends StateDescriptor> initialStateClass;
        private final Class<? extends StateDescriptor> finalStateClass;
        private Class<? extends StateDescriptor> mandatoryIntermediateState;
        private LoginContext loginContext;

        private Builder(Class<? extends StateDescriptor> initialStateClass, Class<? extends StateDescriptor> finalStateClass) {
            this.initialStateClass = Objects.requireNonNull(initialStateClass);
            this.finalStateClass = Objects.requireNonNull(finalStateClass);
        }

        public Builder withMandatoryIntermediateState(Class<? extends StateDescriptor> mandatoryIntermedidateState) {
            this.mandatoryIntermediateState = mandatoryIntermedidateState;
            return this;
        }

        public Builder withLoginContext(String username, String password, Resourcepart resource) {
            LoginContext loginContext = new LoginContext(username, password, resource);
            return withLoginContext(loginContext);
        }

        public Builder withLoginContext(LoginContext loginContext) {
            this.loginContext = loginContext;
            return this;
        }

        public WalkStateGraphContext build() {
            return new WalkStateGraphContext(this);
        }
    }
}
