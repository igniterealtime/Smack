/**
 *
 * Copyright 2018-2021 Florian Schmaus
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

import java.io.PrintWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jivesoftware.smack.c2s.ModularXmppClientToServerConnection.DisconnectedStateDescriptor;
import org.jivesoftware.smack.c2s.internal.ModularXmppClientToServerConnectionInternal;
import org.jivesoftware.smack.util.Consumer;
import org.jivesoftware.smack.util.MultiMap;

/**
 * Smack's utility API for Finite State Machines (FSM).
 *
 * <p>
 * Thanks to Andreas Fried for the fun and successful bug hunting session.
 * </p>
 *
 * @author Florian Schmaus
 *
 */
public class StateDescriptorGraph {

    private static GraphVertex<StateDescriptor> addNewStateDescriptorGraphVertex(
                    Class<? extends StateDescriptor> stateDescriptorClass,
                    Map<Class<? extends StateDescriptor>, GraphVertex<StateDescriptor>> graphVertexes)
                    throws InstantiationException, IllegalAccessException, IllegalArgumentException,
                    InvocationTargetException, NoSuchMethodException, SecurityException {
        Constructor<? extends StateDescriptor> stateDescriptorConstructor = stateDescriptorClass.getDeclaredConstructor();
        stateDescriptorConstructor.setAccessible(true);
        StateDescriptor stateDescriptor = stateDescriptorConstructor.newInstance();
        GraphVertex<StateDescriptor> graphVertexStateDescriptor = new GraphVertex<>(stateDescriptor);

        GraphVertex<StateDescriptor> previous = graphVertexes.put(stateDescriptorClass, graphVertexStateDescriptor);
        assert previous == null;

        return graphVertexStateDescriptor;
    }

    private static final class HandleStateDescriptorGraphVertexContext {
        private final Set<Class<? extends StateDescriptor>> handledStateDescriptors = new HashSet<>();
        Map<Class<? extends StateDescriptor>, GraphVertex<StateDescriptor>> graphVertexes;
        MultiMap<Class<? extends StateDescriptor>, Class<? extends StateDescriptor>> inferredForwardEdges;

        private HandleStateDescriptorGraphVertexContext(
                        Map<Class<? extends StateDescriptor>, GraphVertex<StateDescriptor>> graphVertexes,
                        MultiMap<Class<? extends StateDescriptor>, Class<? extends StateDescriptor>> inferredForwardEdges) {
            this.graphVertexes = graphVertexes;
            this.inferredForwardEdges = inferredForwardEdges;
        }

        private boolean recurseInto(Class<? extends StateDescriptor> stateDescriptorClass) {
            boolean wasAdded = handledStateDescriptors.add(stateDescriptorClass);
            boolean alreadyHandled = !wasAdded;
            return alreadyHandled;
        }

        private GraphVertex<StateDescriptor> getOrConstruct(Class<? extends StateDescriptor> stateDescriptorClass)
                        throws InstantiationException, IllegalAccessException, IllegalArgumentException,
                        InvocationTargetException, NoSuchMethodException, SecurityException {
            GraphVertex<StateDescriptor> graphVertexStateDescriptor = graphVertexes.get(stateDescriptorClass);

            if (graphVertexStateDescriptor == null) {
                graphVertexStateDescriptor = addNewStateDescriptorGraphVertex(stateDescriptorClass, graphVertexes);

                for (Class<? extends StateDescriptor> inferredSuccessor : inferredForwardEdges.getAll(
                                stateDescriptorClass)) {
                    graphVertexStateDescriptor.getElement().addSuccessor(inferredSuccessor);
                }
            }

            return graphVertexStateDescriptor;
        }
    }

    private static void handleStateDescriptorGraphVertex(GraphVertex<StateDescriptor> node,
                    HandleStateDescriptorGraphVertexContext context,
                    boolean failOnUnknownStates)
                    throws InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchMethodException, SecurityException {
        Class<? extends StateDescriptor> stateDescriptorClass = node.element.getClass();
        boolean alreadyHandled = context.recurseInto(stateDescriptorClass);
        if (alreadyHandled) {
            return;
        }

        Set<Class<? extends StateDescriptor>> successorClasses = node.element.getSuccessors();
        int numSuccessors = successorClasses.size();

        Map<Class<? extends StateDescriptor>, GraphVertex<StateDescriptor>> successorStateDescriptors = new HashMap<>(
                        numSuccessors);
        for (Class<? extends StateDescriptor> successorClass : successorClasses) {
            GraphVertex<StateDescriptor> successorGraphNode = context.getOrConstruct(successorClass);
            successorStateDescriptors.put(successorClass, successorGraphNode);
        }

        switch (numSuccessors) {
        case 0:
            throw new IllegalStateException("State " + stateDescriptorClass + " has no successor");
        case 1:
            GraphVertex<StateDescriptor> soleSuccessorNode = successorStateDescriptors.values().iterator().next();
            node.addOutgoingEdge(soleSuccessorNode);
            handleStateDescriptorGraphVertex(soleSuccessorNode, context, failOnUnknownStates);
            return;
        }

        // We hit a state with multiple successors, perform a topological sort on the successors first.
        // Process the information regarding subordinates and superiors states.

        // The preference graph is the graph where the precedence information of all successors is stored, which we will
        // topologically sort to find out which successor we should try first. It is a further new graph we use solely in
        // this step for every node. The graph is represented as map. There is no special marker for the initial node
        // as it is not required for the topological sort performed later.
        Map<Class<? extends StateDescriptor>, GraphVertex<Class<? extends StateDescriptor>>> preferenceGraph = new HashMap<>(numSuccessors);

        // Iterate over all successor states of the current state.
        for (GraphVertex<StateDescriptor> successorStateDescriptorGraphNode : successorStateDescriptors.values()) {
            StateDescriptor successorStateDescriptor = successorStateDescriptorGraphNode.element;
            Class<? extends StateDescriptor> successorStateDescriptorClass = successorStateDescriptor.getClass();
            for (Class<? extends StateDescriptor> subordinateClass : successorStateDescriptor.getSubordinates()) {
                if (failOnUnknownStates && !successorClasses.contains(subordinateClass)) {
                    throw new IllegalStateException(successorStateDescriptor + " points to a subordinate '" + subordinateClass + "' which is not part of the successor set");
                }

                GraphVertex<Class<? extends StateDescriptor>> superiorClassNode = lookupAndCreateIfRequired(
                                preferenceGraph, successorStateDescriptorClass);
                GraphVertex<Class<? extends StateDescriptor>> subordinateClassNode = lookupAndCreateIfRequired(
                                preferenceGraph, subordinateClass);

                superiorClassNode.addOutgoingEdge(subordinateClassNode);
            }
            for (Class<? extends StateDescriptor> superiorClass : successorStateDescriptor.getSuperiors()) {
                if (failOnUnknownStates && !successorClasses.contains(superiorClass)) {
                    throw new IllegalStateException(successorStateDescriptor + " points to a superior '" + superiorClass
                                    + "' which is not part of the successor set");
                }

                GraphVertex<Class<? extends StateDescriptor>> subordinateClassNode = lookupAndCreateIfRequired(
                                preferenceGraph, successorStateDescriptorClass);
                GraphVertex<Class<? extends StateDescriptor>> superiorClassNode = lookupAndCreateIfRequired(
                                preferenceGraph, superiorClass);

                superiorClassNode.addOutgoingEdge(subordinateClassNode);
            }
        }

        // Perform a topological sort which returns the state descriptor classes sorted by their priority. Highest
        // priority state descriptors first.
        List<GraphVertex<Class<? extends StateDescriptor>>> sortedSuccessors = topologicalSort(preferenceGraph.values());

        // Handle the successor nodes which have not preference information available. Simply append them to the end of
        // the sorted successor list.
        outerloop: for (Class<? extends StateDescriptor> successorStateDescriptor : successorClasses) {
            for (GraphVertex<Class<? extends StateDescriptor>> sortedSuccessor : sortedSuccessors) {
                if (sortedSuccessor.getElement() == successorStateDescriptor) {
                    continue outerloop;
                }
            }

            sortedSuccessors.add(new GraphVertex<>(successorStateDescriptor));
        }

        for (GraphVertex<Class<? extends StateDescriptor>> successor : sortedSuccessors) {
            GraphVertex<StateDescriptor> successorVertex = successorStateDescriptors.get(successor.element);
            node.addOutgoingEdge(successorVertex);

            // Recurse further.
            handleStateDescriptorGraphVertex(successorVertex, context, failOnUnknownStates);
        }
    }

    public static GraphVertex<StateDescriptor> constructStateDescriptorGraph(
                    Set<Class<? extends StateDescriptor>> backwardEdgeStateDescriptors,
                    boolean failOnUnknownStates)
                    throws InstantiationException, IllegalAccessException, IllegalArgumentException,
                    InvocationTargetException, NoSuchMethodException, SecurityException {
        Map<Class<? extends StateDescriptor>, GraphVertex<StateDescriptor>> graphVertexes = new HashMap<>();

        final Class<? extends StateDescriptor> initialStatedescriptorClass = DisconnectedStateDescriptor.class;
        GraphVertex<StateDescriptor> initialNode = addNewStateDescriptorGraphVertex(initialStatedescriptorClass, graphVertexes);

        MultiMap<Class<? extends StateDescriptor>, Class<? extends StateDescriptor>> inferredForwardEdges = new MultiMap<>();
        for (Class<? extends StateDescriptor> backwardsEdge : backwardEdgeStateDescriptors) {
            GraphVertex<StateDescriptor> graphVertexStateDescriptor = addNewStateDescriptorGraphVertex(backwardsEdge, graphVertexes);

            for (Class<? extends StateDescriptor> predecessor : graphVertexStateDescriptor.getElement().getPredeccessors()) {
                inferredForwardEdges.put(predecessor, backwardsEdge);
            }
        }
        // Ensure that the intial node has their successors inferred.
        for (Class<? extends StateDescriptor> inferredSuccessorOfInitialStateDescriptor : inferredForwardEdges.getAll(initialStatedescriptorClass)) {
            initialNode.getElement().addSuccessor(inferredSuccessorOfInitialStateDescriptor);
        }

        HandleStateDescriptorGraphVertexContext context = new HandleStateDescriptorGraphVertexContext(graphVertexes, inferredForwardEdges);
        handleStateDescriptorGraphVertex(initialNode, context, failOnUnknownStates);

        return initialNode;
    }

    private static GraphVertex<State> convertToStateGraph(GraphVertex<StateDescriptor> stateDescriptorVertex,
                    ModularXmppClientToServerConnectionInternal connectionInternal, Map<StateDescriptor, GraphVertex<State>> handledStateDescriptors) {
        StateDescriptor stateDescriptor = stateDescriptorVertex.getElement();
        GraphVertex<State> stateVertex = handledStateDescriptors.get(stateDescriptor);
        if (stateVertex != null) {
            return stateVertex;
        }

        State state = stateDescriptor.constructState(connectionInternal);
        stateVertex = new GraphVertex<>(state);
        handledStateDescriptors.put(stateDescriptor, stateVertex);
        for (GraphVertex<StateDescriptor> successorStateDescriptorVertex : stateDescriptorVertex.getOutgoingEdges()) {
            GraphVertex<State> successorStateVertex = convertToStateGraph(successorStateDescriptorVertex, connectionInternal, handledStateDescriptors);
            // It is important that we keep the order of the edges. This should do it.
            stateVertex.addOutgoingEdge(successorStateVertex);
        }

        return stateVertex;
    }

    public static GraphVertex<State> convertToStateGraph(GraphVertex<StateDescriptor> initialStateDescriptor,
                    ModularXmppClientToServerConnectionInternal connectionInternal) {
        Map<StateDescriptor, GraphVertex<State>> handledStateDescriptors = new HashMap<>();
        GraphVertex<State> initialState = convertToStateGraph(initialStateDescriptor, connectionInternal,
                        handledStateDescriptors);
        return initialState;
    }

    // Graph API after here.
    // This API could possibly factored out into an extra package/class, but then we will probably need a builder for
    // the graph vertex in order to keep it immutable.
    public static final class GraphVertex<E> {
        private final E element;
        private final List<GraphVertex<E>> outgoingEdges = new ArrayList<>();

        private VertexColor color = VertexColor.white;

        private GraphVertex(E element) {
            this.element = element;
        }

        private void addOutgoingEdge(GraphVertex<E> vertex) {
            assert vertex != null;
            if (outgoingEdges.contains(vertex)) {
                throw new IllegalArgumentException("This " + this + " already has an outgoing edge to " + vertex);
            }
            outgoingEdges.add(vertex);
        }

        public E getElement() {
            return element;
        }

        public List<GraphVertex<E>> getOutgoingEdges() {
            return Collections.unmodifiableList(outgoingEdges);
        }

        private enum VertexColor {
            white,
            grey,
            black,
        }

        @Override
        public String toString() {
            return toString(true);
        }

        public String toString(boolean includeOutgoingEdges) {
            StringBuilder sb = new StringBuilder();
            sb.append("GraphVertex " + element + " [color=" + color
                            + ", identityHashCode=" + System.identityHashCode(this)
                            + ", outgoingEdgeCount=" + outgoingEdges.size());

            if (includeOutgoingEdges) {
                sb.append(", outgoingEdges={");

                for (Iterator<GraphVertex<E>> it = outgoingEdges.iterator(); it.hasNext();) {
                    GraphVertex<E> outgoingEdgeVertex = it.next();
                    sb.append(outgoingEdgeVertex.toString(false));
                    if (it.hasNext()) {
                        sb.append(", ");
                    }
                }
                sb.append('}');
            }

            sb.append(']');
            return sb.toString();
        }
    }

    private static GraphVertex<Class<? extends StateDescriptor>> lookupAndCreateIfRequired(
                    Map<Class<? extends StateDescriptor>, GraphVertex<Class<? extends StateDescriptor>>> map,
                    Class<? extends StateDescriptor> clazz) {
        GraphVertex<Class<? extends StateDescriptor>> vertex = map.get(clazz);
        if (vertex == null) {
            vertex = new GraphVertex<>(clazz);
            map.put(clazz, vertex);
        }
        return vertex;
    }

    private static <E> List<GraphVertex<E>> topologicalSort(Collection<GraphVertex<E>> vertexes) {
        List<GraphVertex<E>> res = new ArrayList<>();
        dfs(vertexes, vertex -> res.add(0, vertex), null);
        return res;
    }

    private static <E> void dfsVisit(GraphVertex<E> vertex, Consumer<GraphVertex<E>> dfsFinishedVertex,
                    DfsEdgeFound<E> dfsEdgeFound) {
        vertex.color = GraphVertex.VertexColor.grey;

        final int totalEdgeCount = vertex.getOutgoingEdges().size();

        int edgeCount = 0;

        for (GraphVertex<E> successorVertex : vertex.getOutgoingEdges()) {
            edgeCount++;
            if (dfsEdgeFound != null) {
                dfsEdgeFound.onEdgeFound(vertex, successorVertex, edgeCount, totalEdgeCount);
            }
            if (successorVertex.color == GraphVertex.VertexColor.white) {
                dfsVisit(successorVertex, dfsFinishedVertex, dfsEdgeFound);
            }
        }

        vertex.color = GraphVertex.VertexColor.black;
        if (dfsFinishedVertex != null) {
            dfsFinishedVertex.accept(vertex);
        }
    }

    private static <E> void dfs(Collection<GraphVertex<E>> vertexes, Consumer<GraphVertex<E>> dfsFinishedVertex,
                    DfsEdgeFound<E> dfsEdgeFound) {
        for (GraphVertex<E> vertex : vertexes) {
            if (vertex.color == GraphVertex.VertexColor.white) {
                dfsVisit(vertex, dfsFinishedVertex, dfsEdgeFound);
            }
        }
    }

    public static <E> void stateDescriptorGraphToDot(Collection<GraphVertex<StateDescriptor>> vertexes,
                    PrintWriter dotOut, boolean breakStateName) {
        dotOut.append("digraph {\n");
        dfs(vertexes,
                finishedVertex -> {
                   boolean isMultiVisitState = finishedVertex.element.isMultiVisitState();
                   boolean isFinalState = finishedVertex.element.isFinalState();
                   boolean isNotImplemented = finishedVertex.element.isNotImplemented();

                   String style = null;
                   if (isMultiVisitState) {
                       style = "bold";
                   } else if (isFinalState) {
                       style = "filled";
                   } else if (isNotImplemented) {
                       style = "dashed";
                   }

                   if (style == null) {
                       return;
                   }

                   dotOut.append('"')
                       .append(finishedVertex.element.getFullStateName(breakStateName))
                       .append("\" [ ")
                       .append("style=")
                       .append(style)
                       .append(" ]\n");
               },
               (from, to, edgeId, totalEdgeCount) -> {
                   dotOut.append("  \"")
                       .append(from.element.getFullStateName(breakStateName))
                       .append("\" -> \"")
                       .append(to.element.getFullStateName(breakStateName))
                       .append('"');
                   if (totalEdgeCount > 1) {
                       // Note that 'dot' requires *double* quotes to enclose the value.
                       dotOut.append(" [xlabel=\"")
                       .append(Integer.toString(edgeId))
                       .append("\"]");
                   }
                   dotOut.append(";\n");
               });
        dotOut.append("}\n");
    }

    private interface DfsEdgeFound<E> {
        void onEdgeFound(GraphVertex<E> from, GraphVertex<E> to, int edgeId, int totalEdgeCount);
    }
}
