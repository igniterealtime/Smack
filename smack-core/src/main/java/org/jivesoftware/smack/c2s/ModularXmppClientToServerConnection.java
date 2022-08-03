/**
 *
 * Copyright 2018-2022 Florian Schmaus
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
package org.jivesoftware.smack.c2s;

import java.io.IOException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLSession;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.SmackException.OutgoingQueueFullException;
import org.jivesoftware.smack.SmackFuture;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.XMPPException.FailedNonzaException;
import org.jivesoftware.smack.XMPPException.StreamErrorException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.XmppInputOutputFilter;
import org.jivesoftware.smack.c2s.XmppClientToServerTransport.LookupConnectionEndpointsFailed;
import org.jivesoftware.smack.c2s.XmppClientToServerTransport.LookupConnectionEndpointsResult;
import org.jivesoftware.smack.c2s.XmppClientToServerTransport.LookupConnectionEndpointsSuccess;
import org.jivesoftware.smack.c2s.internal.ModularXmppClientToServerConnectionInternal;
import org.jivesoftware.smack.c2s.internal.WalkStateGraphContext;
import org.jivesoftware.smack.fsm.ConnectionStateEvent;
import org.jivesoftware.smack.fsm.ConnectionStateMachineListener;
import org.jivesoftware.smack.fsm.LoginContext;
import org.jivesoftware.smack.fsm.NoOpState;
import org.jivesoftware.smack.fsm.State;
import org.jivesoftware.smack.fsm.StateDescriptor;
import org.jivesoftware.smack.fsm.StateDescriptorGraph;
import org.jivesoftware.smack.fsm.StateDescriptorGraph.GraphVertex;
import org.jivesoftware.smack.fsm.StateMachineException;
import org.jivesoftware.smack.fsm.StateTransitionResult;
import org.jivesoftware.smack.fsm.StateTransitionResult.AttemptResult;
import org.jivesoftware.smack.internal.AbstractStats;
import org.jivesoftware.smack.internal.SmackTlsContext;
import org.jivesoftware.smack.packet.AbstractStreamClose;
import org.jivesoftware.smack.packet.AbstractStreamOpen;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Nonza;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.StreamError;
import org.jivesoftware.smack.packet.TopLevelStreamElement;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.sasl.SASLErrorException;
import org.jivesoftware.smack.sasl.SASLMechanism;
import org.jivesoftware.smack.util.ArrayBlockingQueueWithShutdown;
import org.jivesoftware.smack.util.ExtendedAppendable;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.Supplier;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.jxmpp.jid.DomainBareJid;
import org.jxmpp.jid.parts.Resourcepart;
import org.jxmpp.util.XmppStringUtils;

public final class ModularXmppClientToServerConnection extends AbstractXMPPConnection {

    private static final Logger LOGGER = Logger.getLogger(ModularXmppClientToServerConnectionConfiguration.class.getName());

    private final ArrayBlockingQueueWithShutdown<TopLevelStreamElement> outgoingElementsQueue = new ArrayBlockingQueueWithShutdown<>(
                    100, true);

    private XmppClientToServerTransport activeTransport;

    private final List<ConnectionStateMachineListener> connectionStateMachineListeners = new CopyOnWriteArrayList<>();

    private boolean featuresReceived;

    private boolean streamResumed;

    private GraphVertex<State> currentStateVertex;

    private List<State> walkFromDisconnectToAuthenticated;

    private final ModularXmppClientToServerConnectionConfiguration configuration;

    private final ModularXmppClientToServerConnectionInternal connectionInternal;

    private final Map<Class<? extends ModularXmppClientToServerConnectionModuleDescriptor>, ModularXmppClientToServerConnectionModule<? extends ModularXmppClientToServerConnectionModuleDescriptor>> connectionModules = new HashMap<>();

    private final Map<Class<? extends ModularXmppClientToServerConnectionModuleDescriptor>, XmppClientToServerTransport> transports = new HashMap<>();
    /**
     * This is one of those cases where the field is modified by one thread and read by another. We currently use
     * CopyOnWriteArrayList but should potentially use a VarHandle once Smack supports them.
     */
    private final List<XmppInputOutputFilter> inputOutputFilters = new CopyOnWriteArrayList<>();

    private List<XmppInputOutputFilter> previousInputOutputFilters;

    public ModularXmppClientToServerConnection(ModularXmppClientToServerConnectionConfiguration configuration) {
        super(configuration);

        this.configuration = configuration;

        // Construct the internal connection API.
        connectionInternal = new ModularXmppClientToServerConnectionInternal(this, getReactor(), debugger, outgoingElementsQueue) {

            @Override
            public void parseAndProcessElement(String wrappedCompleteElement) {
                ModularXmppClientToServerConnection.this.parseAndProcessElement(wrappedCompleteElement);
            }

            @Override
            public void notifyConnectionError(Exception e) {
                ModularXmppClientToServerConnection.this.notifyConnectionError(e);
            }

            @Override
            public String onStreamOpen(XmlPullParser parser) {
                return ModularXmppClientToServerConnection.this.onStreamOpen(parser);
            }

            @Override
            public void onStreamClosed() {
                ModularXmppClientToServerConnection.this.closingStreamReceived = true;
                notifyWaitingThreads();
            }

            @Override
            public void fireFirstLevelElementSendListeners(TopLevelStreamElement element) {
                ModularXmppClientToServerConnection.this.firePacketSendingListeners(element);
            }

            @Override
            public void invokeConnectionStateMachineListener(ConnectionStateEvent connectionStateEvent) {
                ModularXmppClientToServerConnection.this.invokeConnectionStateMachineListener(connectionStateEvent);
            }

            @Override
            public XmlEnvironment getOutgoingStreamXmlEnvironment() {
                return outgoingStreamXmlEnvironment;
            }

            @Override
            public void addXmppInputOutputFilter(XmppInputOutputFilter xmppInputOutputFilter) {
                inputOutputFilters.add(0, xmppInputOutputFilter);
            }

            @Override
            public ListIterator<XmppInputOutputFilter> getXmppInputOutputFilterBeginIterator() {
                return inputOutputFilters.listIterator();
            }

            @Override
            public ListIterator<XmppInputOutputFilter> getXmppInputOutputFilterEndIterator() {
                return inputOutputFilters.listIterator(inputOutputFilters.size());
            }

            @Override
            public void waitForFeaturesReceived(String waitFor) throws InterruptedException, SmackException, XMPPException {
                ModularXmppClientToServerConnection.this.waitForFeaturesReceived(waitFor);
            }

            @Override
            public void newStreamOpenWaitForFeaturesSequence(String waitFor) throws InterruptedException,
                            SmackException, XMPPException {
                ModularXmppClientToServerConnection.this.newStreamOpenWaitForFeaturesSequence(waitFor);
            }

            @Override
            public SmackTlsContext getSmackTlsContext() {
                return ModularXmppClientToServerConnection.this.getSmackTlsContext();
            }

            @Override
            public <SN extends Nonza, FN extends Nonza> SN sendAndWaitForResponse(Nonza nonza, Class<SN> successNonzaClass,
                            Class<FN> failedNonzaClass) throws NoResponseException, NotConnectedException, FailedNonzaException, InterruptedException {
                return ModularXmppClientToServerConnection.this.sendAndWaitForResponse(nonza, successNonzaClass, failedNonzaClass);
            }

            @Override
            public void asyncGo(Runnable runnable) {
                AbstractXMPPConnection.asyncGo(runnable);
            }

            @Override
            public void waitForConditionOrThrowConnectionException(Supplier<Boolean> condition, String waitFor)
                            throws InterruptedException, SmackException, XMPPException {
                ModularXmppClientToServerConnection.this.waitForConditionOrThrowConnectionException(condition, waitFor);
            }

            @Override
            public void notifyWaitingThreads() {
                ModularXmppClientToServerConnection.this.notifyWaitingThreads();
            }

            @Override
            public void setCompressionEnabled(boolean compressionEnabled) {
                ModularXmppClientToServerConnection.this.compressionEnabled = compressionEnabled;
            }

            @Override
            public void setTransport(XmppClientToServerTransport xmppTransport) {
                ModularXmppClientToServerConnection.this.activeTransport = xmppTransport;
                ModularXmppClientToServerConnection.this.connected = true;
            }

        };

        // Construct the modules from the module descriptor. We do this before constructing the state graph, as the
        // modules are sometimes used to construct the states.
        for (ModularXmppClientToServerConnectionModuleDescriptor moduleDescriptor : configuration.moduleDescriptors) {
            Class<? extends ModularXmppClientToServerConnectionModuleDescriptor> moduleDescriptorClass = moduleDescriptor.getClass();
            ModularXmppClientToServerConnectionModule<? extends ModularXmppClientToServerConnectionModuleDescriptor> connectionModule = moduleDescriptor.constructXmppConnectionModule(connectionInternal);
            connectionModules.put(moduleDescriptorClass, connectionModule);

            XmppClientToServerTransport transport = connectionModule.getTransport();
            // Not every module may provide a transport.
            if (transport != null) {
                transports.put(moduleDescriptorClass, transport);
            }
        }

        GraphVertex<StateDescriptor> initialStateDescriptorVertex = configuration.initialStateDescriptorVertex;
        // Convert the graph of state descriptors to a graph of states, bound to this very connection.
        currentStateVertex = StateDescriptorGraph.convertToStateGraph(initialStateDescriptorVertex, connectionInternal);
    }

    @SuppressWarnings("unchecked")
    public <CM extends ModularXmppClientToServerConnectionModule<? extends ModularXmppClientToServerConnectionModuleDescriptor>> CM getConnectionModuleFor(
                    Class<? extends ModularXmppClientToServerConnectionModuleDescriptor> descriptorClass) {
        return (CM) connectionModules.get(descriptorClass);
    }

    @Override
    protected void loginInternal(String username, String password, Resourcepart resource)
                    throws XMPPException, SmackException, IOException, InterruptedException {
        WalkStateGraphContext walkStateGraphContext = buildNewWalkTo(
                        AuthenticatedAndResourceBoundStateDescriptor.class).withLoginContext(username, password,
                                        resource).build();
        walkStateGraph(walkStateGraphContext);
    }

    private WalkStateGraphContext.Builder buildNewWalkTo(Class<? extends StateDescriptor> finalStateClass) {
        return WalkStateGraphContext.builder(currentStateVertex.getElement().getStateDescriptor().getClass(), finalStateClass);
    }

    /**
     * Unwind the state. This will revert the effects of the state by calling {@link State#resetState()} prior issuing a
     * connection state event of {@link ConnectionStateEvent#StateRevertBackwardsWalk}.
     *
     * @param revertedState the state which is going to get reverted.
     */
    private void unwindState(State revertedState) {
        invokeConnectionStateMachineListener(new ConnectionStateEvent.StateRevertBackwardsWalk(revertedState));
        revertedState.resetState();
    }

    private void walkStateGraph(WalkStateGraphContext walkStateGraphContext)
                    throws XMPPException, IOException, SmackException, InterruptedException {
        // Save a copy of the current state
        GraphVertex<State> previousStateVertex = currentStateVertex;
        try {
            walkStateGraphInternal(walkStateGraphContext);
        } catch (IOException | SmackException | InterruptedException | XMPPException e) {
            currentStateVertex = previousStateVertex;
            // Unwind the state.
            State revertedState = currentStateVertex.getElement();
            unwindState(revertedState);
            throw e;
        }
    }

    private void walkStateGraphInternal(WalkStateGraphContext walkStateGraphContext)
                    throws IOException, SmackException, InterruptedException, XMPPException {
        // Save a copy of the current state
        final GraphVertex<State> initialStateVertex = currentStateVertex;
        final State initialState = initialStateVertex.getElement();
        final StateDescriptor initialStateDescriptor = initialState.getStateDescriptor();

        walkStateGraphContext.recordWalkTo(initialState);

        // Check if this is the walk's final state.
        if (walkStateGraphContext.isWalksFinalState(initialStateDescriptor)) {
            // If this is used as final state, then it should be marked as such.
            assert initialStateDescriptor.isFinalState();

            // We reached the final state.
            invokeConnectionStateMachineListener(new ConnectionStateEvent.FinalStateReached(initialState));
            return;
        }

        List<GraphVertex<State>> outgoingStateEdges = initialStateVertex.getOutgoingEdges();

        // See if we need to handle mandatory intermediate states.
        GraphVertex<State> mandatoryIntermediateStateVertex = walkStateGraphContext.maybeReturnMandatoryImmediateState(outgoingStateEdges);
        if (mandatoryIntermediateStateVertex != null) {
            StateTransitionResult reason = attemptEnterState(mandatoryIntermediateStateVertex, walkStateGraphContext);

            if (reason instanceof StateTransitionResult.Success) {
                walkStateGraph(walkStateGraphContext);
                return;
            }

            // We could not enter a mandatory intermediate state. Throw here.
            throw new StateMachineException.SmackMandatoryStateFailedException(
                            mandatoryIntermediateStateVertex.getElement(), reason);
        }

        for (Iterator<GraphVertex<State>> it = outgoingStateEdges.iterator(); it.hasNext();) {
            GraphVertex<State> successorStateVertex = it.next();
            State successorState = successorStateVertex.getElement();

            // Ignore successorStateVertex if the only way to the final state is via the initial state. This happens
            // typically if we are in the ConnectedButUnauthenticated state on the way to ResourceboundAndAuthenticated,
            // where we do not want to walk via InstantShutdown/Shtudown in a cycle over the initial state towards this
            // state.
            if (walkStateGraphContext.wouldCauseCycle(successorStateVertex)) {
                // Ignore this successor.
                invokeConnectionStateMachineListener(new ConnectionStateEvent.TransitionIgnoredDueCycle(initialStateVertex, successorStateVertex));
            } else {
                StateTransitionResult result = attemptEnterState(successorStateVertex, walkStateGraphContext);

                if (result instanceof StateTransitionResult.Success) {
                    break;
                }

                // If attemptEnterState did not throw and did not return a value of type TransitionSuccessResult, then we
                // just record this value and go on from there. Note that reason may be null, which is returned by
                // attemptEnterState in case the state was already successfully handled. If this is the case, then we don't
                // record it.
                if (result != null) {
                    walkStateGraphContext.recordFailedState(successorState, result);
                }
            }

            if (!it.hasNext()) {
                throw StateMachineException.SmackStateGraphDeadEndException.from(walkStateGraphContext,
                                initialStateVertex);
            }
        }

        // Walk the state graph by recursion.
        walkStateGraph(walkStateGraphContext);
    }

    /**
     * Attempt to enter a state. Note that this method may return <code>null</code> if this state can be safely ignored.
     *
     * @param successorStateVertex the successor state vertex.
     * @param walkStateGraphContext the "walk state graph" context.
     * @return A state transition result or <code>null</code> if this state can be ignored.
     * @throws SmackException if Smack detected an exceptional situation.
     * @throws XMPPException if an XMPP protocol error was received.
     * @throws IOException if an I/O error occurred.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    private StateTransitionResult attemptEnterState(GraphVertex<State> successorStateVertex,
                    WalkStateGraphContext walkStateGraphContext) throws SmackException, XMPPException,
                    IOException, InterruptedException {
        final GraphVertex<State> initialStateVertex = currentStateVertex;
        final State initialState = initialStateVertex.getElement();
        final State successorState = successorStateVertex.getElement();
        final StateDescriptor successorStateDescriptor = successorState.getStateDescriptor();

        if (!successorStateDescriptor.isMultiVisitState()
                        && walkStateGraphContext.stateAlreadyVisited(successorState)) {
            // This can happen if a state leads back to the state where it originated from. See for example the
            // 'Compression' state. We return 'null' here to signal that the state can safely be ignored.
            return null;
        }

        if (successorStateDescriptor.isNotImplemented()) {
            StateTransitionResult.TransitionImpossibleBecauseNotImplemented transtionImpossibleBecauseNotImplemented = new StateTransitionResult.TransitionImpossibleBecauseNotImplemented(
                            successorStateDescriptor);
            invokeConnectionStateMachineListener(new ConnectionStateEvent.TransitionNotPossible(initialState, successorState,
                            transtionImpossibleBecauseNotImplemented));
            return transtionImpossibleBecauseNotImplemented;
        }

        final StateTransitionResult.AttemptResult transitionAttemptResult;
        try {
            StateTransitionResult.TransitionImpossible transitionImpossible = successorState.isTransitionToPossible(
                            walkStateGraphContext);
            if (transitionImpossible != null) {
                invokeConnectionStateMachineListener(new ConnectionStateEvent.TransitionNotPossible(initialState, successorState,
                                transitionImpossible));
                return transitionImpossible;
            }

            invokeConnectionStateMachineListener(new ConnectionStateEvent.AboutToTransitionInto(initialState, successorState));
            transitionAttemptResult = successorState.transitionInto(walkStateGraphContext);
        } catch (SmackException | IOException | InterruptedException | XMPPException e) {
            // Unwind the state here too, since this state will not be unwound by walkStateGraph(), as it will not
            // become a predecessor state in the walk.
            unwindState(successorState);
            throw e;
        }
        if (transitionAttemptResult instanceof StateTransitionResult.Failure) {
            StateTransitionResult.Failure transitionFailureResult = (StateTransitionResult.Failure) transitionAttemptResult;
            invokeConnectionStateMachineListener(
                            new ConnectionStateEvent.TransitionFailed(initialState, successorState, transitionFailureResult));
            return transitionAttemptResult;
        }

        // If transitionAttemptResult is not an instance of TransitionFailureResult, then it has to be of type
        // TransitionSuccessResult.
        StateTransitionResult.Success transitionSuccessResult = (StateTransitionResult.Success) transitionAttemptResult;

        currentStateVertex = successorStateVertex;
        invokeConnectionStateMachineListener(
                        new ConnectionStateEvent.SuccessfullyTransitionedInto(successorState, transitionSuccessResult));

        return transitionSuccessResult;
    }

    @Override
    protected void sendInternal(TopLevelStreamElement element) throws NotConnectedException, InterruptedException {
        final XmppClientToServerTransport transport = activeTransport;
        if (transport == null) {
            throw new NotConnectedException();
        }

        outgoingElementsQueue.put(element);
        transport.notifyAboutNewOutgoingElements();
    }

    @Override
    protected void sendNonBlockingInternal(TopLevelStreamElement element) throws NotConnectedException, OutgoingQueueFullException {
        final XmppClientToServerTransport transport = activeTransport;
        if (transport == null) {
            throw new NotConnectedException();
        }

        boolean enqueued = outgoingElementsQueue.offer(element);
        if (!enqueued) {
            throw new OutgoingQueueFullException();
        }

        transport.notifyAboutNewOutgoingElements();
    }

    @Override
    protected void shutdown() {
        shutdown(false);
    }

    @Override
    public synchronized void instantShutdown() {
        shutdown(true);
    }

    @Override
    public ModularXmppClientToServerConnectionConfiguration getConfiguration() {
        return configuration;
    }

    private void shutdown(boolean instant) {
        Class<? extends StateDescriptor> mandatoryIntermediateState;
        if (instant) {
            mandatoryIntermediateState = InstantShutdownStateDescriptor.class;
        } else {
            mandatoryIntermediateState = ShutdownStateDescriptor.class;
        }

        WalkStateGraphContext context = buildNewWalkTo(
                        DisconnectedStateDescriptor.class).withMandatoryIntermediateState(
                                        mandatoryIntermediateState).build();

        try {
            walkStateGraph(context);
        } catch (IOException | SmackException | InterruptedException | XMPPException e) {
            throw new IllegalStateException("A walk to disconnected state should never throw", e);
        }
    }

    private SSLSession getSSLSession() {
        final XmppClientToServerTransport transport = activeTransport;
        if (transport == null) {
            return null;
        }
        return transport.getSslSession();
    }

    @Override
    protected void afterFeaturesReceived() {
        featuresReceived = true;
        notifyWaitingThreads();
    }

    private void parseAndProcessElement(String element) {
        try {
            XmlPullParser parser = PacketParserUtils.getParserFor(element);

            // Skip the enclosing stream open what is guaranteed to be there.
            parser.next();

            XmlPullParser.Event event = parser.getEventType();
            outerloop: while (true) {
                switch (event) {
                case START_ELEMENT:
                    final String name = parser.getName();
                    // Note that we don't handle "stream" here as it's done in the splitter.
                    switch (name) {
                    case Message.ELEMENT:
                    case IQ.IQ_ELEMENT:
                    case Presence.ELEMENT:
                        try {
                            parseAndProcessStanza(parser);
                        } finally {
                            // TODO: Here would be the following stream management code.
                            // clientHandledStanzasCount = SMUtils.incrementHeight(clientHandledStanzasCount);
                        }
                        break;
                    case "error":
                        StreamError streamError = PacketParserUtils.parseStreamError(parser, null);
                        StreamErrorException streamErrorException = new StreamErrorException(streamError);
                        currentXmppException = streamErrorException;
                        notifyWaitingThreads();
                        throw streamErrorException;
                    case "features":
                        parseFeatures(parser);
                        afterFeaturesReceived();
                        break;
                    default:
                        parseAndProcessNonza(parser);
                        break;
                    }
                    break;
                case END_DOCUMENT:
                    break outerloop;
                default: // fall out
                }
                event = parser.next();
            }
        } catch (XmlPullParserException | IOException | InterruptedException | StreamErrorException
                        | SmackParsingException e) {
            notifyConnectionError(e);
        }
    }

    private synchronized void prepareToWaitForFeaturesReceived() {
        featuresReceived = false;
    }

    private void waitForFeaturesReceived(String waitFor)
                    throws InterruptedException, SmackException, XMPPException {
        waitForConditionOrThrowConnectionException(() -> featuresReceived, waitFor);
    }

    @Override
    protected AbstractStreamOpen getStreamOpen(DomainBareJid to, CharSequence from, String id, String lang) {
        StreamOpenAndCloseFactory streamOpenAndCloseFactory = activeTransport.getStreamOpenAndCloseFactory();
        return streamOpenAndCloseFactory.createStreamOpen(to, from, id, lang);
    }

    private void newStreamOpenWaitForFeaturesSequence(String waitFor) throws InterruptedException,
                    SmackException, XMPPException {
        prepareToWaitForFeaturesReceived();

        // Create StreamOpen from StreamOpenAndCloseFactory via underlying transport.
        StreamOpenAndCloseFactory streamOpenAndCloseFactory = activeTransport.getStreamOpenAndCloseFactory();
        CharSequence from = null;
        CharSequence localpart = connectionInternal.connection.getConfiguration().getUsername();
        DomainBareJid xmppServiceDomain = getXMPPServiceDomain();
        if (localpart != null) {
            from = XmppStringUtils.completeJidFrom(localpart, xmppServiceDomain);
        }
        AbstractStreamOpen streamOpen = streamOpenAndCloseFactory.createStreamOpen(xmppServiceDomain, from, getStreamId(), getConfiguration().getXmlLang());
        sendStreamOpen(streamOpen);

        waitForFeaturesReceived(waitFor);
    }

    private void sendStreamOpen(AbstractStreamOpen streamOpen) throws NotConnectedException, InterruptedException {
        sendNonza(streamOpen);
        updateOutgoingStreamXmlEnvironmentOnStreamOpen(streamOpen);
    }

    public static class DisconnectedStateDescriptor extends StateDescriptor {
        protected DisconnectedStateDescriptor() {
            super(DisconnectedState.class, StateDescriptor.Property.finalState);
            addSuccessor(LookupRemoteConnectionEndpointsStateDescriptor.class);
        }
    }

    private final class DisconnectedState extends State {

        private DisconnectedState(StateDescriptor stateDescriptor, ModularXmppClientToServerConnectionInternal connectionInternal) {
            super(stateDescriptor, connectionInternal);
        }

        @Override
        public StateTransitionResult.AttemptResult transitionInto(WalkStateGraphContext walkStateGraphContext) {
            synchronized (ModularXmppClientToServerConnection.this) {
                if (inputOutputFilters.isEmpty()) {
                    previousInputOutputFilters = null;
                } else {
                    previousInputOutputFilters = new ArrayList<>(inputOutputFilters.size());
                    previousInputOutputFilters.addAll(inputOutputFilters);
                    inputOutputFilters.clear();
                }
            }

            // Reset all states we have visited when transitioning from disconnected to authenticated. This assumes that
            // every state after authenticated does not need to be reset.
            ListIterator<State> it = walkFromDisconnectToAuthenticated.listIterator(
                            walkFromDisconnectToAuthenticated.size());
            while (it.hasPrevious()) {
                State stateToReset = it.previous();
                stateToReset.resetState();
            }
            walkFromDisconnectToAuthenticated = null;

            return StateTransitionResult.Success.EMPTY_INSTANCE;
        }
    }

    public static final class LookupRemoteConnectionEndpointsStateDescriptor extends StateDescriptor {
        private LookupRemoteConnectionEndpointsStateDescriptor() {
            super(LookupRemoteConnectionEndpointsState.class);
        }
    }

    private final class LookupRemoteConnectionEndpointsState extends State {
        boolean outgoingElementsQueueWasShutdown;

        private LookupRemoteConnectionEndpointsState(StateDescriptor stateDescriptor, ModularXmppClientToServerConnectionInternal connectionInternal) {
            super(stateDescriptor, connectionInternal);
        }

        @Override
        public AttemptResult transitionInto(WalkStateGraphContext walkStateGraphContext) throws XMPPErrorException,
                        SASLErrorException, IOException, SmackException, InterruptedException, FailedNonzaException {
            // There is a challenge here: We are going to trigger the discovery of endpoints which will run
            // asynchronously. After a timeout, all discovered endpoints are collected. To prevent stale results from
            // previous discover runs, the results are communicated via SmackFuture, so that we always handle the most
            // up-to-date results.

            Map<XmppClientToServerTransport, List<SmackFuture<LookupConnectionEndpointsResult, Exception>>> lookupFutures = new HashMap<>(
                            transports.size());

            final int numberOfFutures;
            {
                List<SmackFuture<?, ?>> allFutures = new ArrayList<>();
                for (XmppClientToServerTransport transport : transports.values()) {
                    // First we clear the transport of any potentially previously discovered connection endpoints.
                    transport.resetDiscoveredConnectionEndpoints();

                    // Ask the transport to start the discovery of remote connection endpoints asynchronously.
                    List<SmackFuture<LookupConnectionEndpointsResult, Exception>> transportFutures = transport.lookupConnectionEndpoints();

                    lookupFutures.put(transport, transportFutures);
                    allFutures.addAll(transportFutures);
                }

                numberOfFutures = allFutures.size();

                // Wait until all features are ready or if the timeout occurs. Note that we do not inspect and react the
                // return value of SmackFuture.await() as we want to collect the LookupConnectionEndpointsFailed later.
                SmackFuture.await(allFutures, getReplyTimeout(), TimeUnit.MILLISECONDS);
            }

            // Note that we do not pass the lookupFailures in case there is at least one successful transport. The
            // lookup failures are also recording in LookupConnectionEndpointsSuccess, e.g. as part of
            // RemoteXmppTcpConnectionEndpoints.Result.
            List<LookupConnectionEndpointsFailed> lookupFailures = new ArrayList<>(numberOfFutures);

            boolean atLeastOneConnectionEndpointDiscovered = false;
            for (Map.Entry<XmppClientToServerTransport, List<SmackFuture<LookupConnectionEndpointsResult, Exception>>> entry : lookupFutures.entrySet()) {
                XmppClientToServerTransport transport = entry.getKey();

                for (SmackFuture<LookupConnectionEndpointsResult, Exception> future : entry.getValue()) {
                    LookupConnectionEndpointsResult result = future.getIfAvailable();

                    if (result == null) {
                        continue;
                    }

                    if (result instanceof LookupConnectionEndpointsFailed) {
                        LookupConnectionEndpointsFailed lookupFailure = (LookupConnectionEndpointsFailed) result;
                        lookupFailures.add(lookupFailure);
                        continue;
                    }

                    LookupConnectionEndpointsSuccess successResult = (LookupConnectionEndpointsSuccess) result;

                    // Arm the transport with the success result, so that its information can be used by the transport
                    // to establish the connection.
                    transport.loadConnectionEndpoints(successResult);

                    // Mark that the connection attempt can continue.
                    atLeastOneConnectionEndpointDiscovered = true;
                }
            }

            if (!atLeastOneConnectionEndpointDiscovered) {
                throw SmackException.NoEndpointsDiscoveredException.from(lookupFailures);
            }

            if (!lookupFailures.isEmpty()) {
                // TODO: Put those non-fatal lookup failures into a sink of the connection so that the user is able to
                // be aware of them.
            }

            // Even though the outgoing elements queue is unrelated to the lookup remote connection endpoints state, we
            // do start the queue at this point. The transports will need it available, and we use the state's reset()
            // function to close the queue again on failure.
            outgoingElementsQueueWasShutdown = outgoingElementsQueue.start();

            return StateTransitionResult.Success.EMPTY_INSTANCE;
        }

        @Override
        public void resetState() {
            for (XmppClientToServerTransport transport : transports.values()) {
                transport.resetDiscoveredConnectionEndpoints();
            }

            if (outgoingElementsQueueWasShutdown) {
                // Reset the outgoing elements queue in this state, since we also start it in this state.
                outgoingElementsQueue.shutdown();
            }
        }
    }

    public static final class ConnectedButUnauthenticatedStateDescriptor extends StateDescriptor {
        private ConnectedButUnauthenticatedStateDescriptor() {
            super(ConnectedButUnauthenticatedState.class, StateDescriptor.Property.finalState);
            addSuccessor(SaslAuthenticationStateDescriptor.class);
            addSuccessor(InstantShutdownStateDescriptor.class);
            addSuccessor(ShutdownStateDescriptor.class);
        }
    }

    private final class ConnectedButUnauthenticatedState extends State {
        private ConnectedButUnauthenticatedState(StateDescriptor stateDescriptor, ModularXmppClientToServerConnectionInternal connectionInternal) {
            super(stateDescriptor, connectionInternal);
        }

        @Override
        public StateTransitionResult.AttemptResult transitionInto(WalkStateGraphContext walkStateGraphContext) {
            assert walkFromDisconnectToAuthenticated == null;

            if (walkStateGraphContext.isWalksFinalState(getStateDescriptor())) {
                // If this is the final state, then record the walk so far.
                walkFromDisconnectToAuthenticated = walkStateGraphContext.getWalk();
            }

            connected = true;
            return StateTransitionResult.Success.EMPTY_INSTANCE;
        }

        @Override
        public void resetState() {
            connected = false;
        }
    }

    public static final class SaslAuthenticationStateDescriptor extends StateDescriptor {
        private SaslAuthenticationStateDescriptor() {
            super(SaslAuthenticationState.class, "RFC 6120 § 6");
            addSuccessor(AuthenticatedButUnboundStateDescriptor.class);
        }
    }

    private final class SaslAuthenticationState extends State {
        private SaslAuthenticationState(StateDescriptor stateDescriptor, ModularXmppClientToServerConnectionInternal connectionInternal) {
            super(stateDescriptor, connectionInternal);
        }

        @Override
        public StateTransitionResult.AttemptResult transitionInto(WalkStateGraphContext walkStateGraphContext)
                        throws IOException, SmackException, InterruptedException, XMPPException {
            prepareToWaitForFeaturesReceived();

            LoginContext loginContext = walkStateGraphContext.getLoginContext();
            SASLMechanism usedSaslMechanism = authenticate(loginContext.username, loginContext.password,
                            config.getAuthzid(), getSSLSession());
            // authenticate() will only return if the SASL authentication was successful, but we also need to wait for
            // the next round of stream features.

            waitForFeaturesReceived("server stream features after SASL authentication");

            return new SaslAuthenticationSuccessResult(usedSaslMechanism);
        }
    }

    public static final class SaslAuthenticationSuccessResult extends StateTransitionResult.Success {
        private final String saslMechanismName;

        private SaslAuthenticationSuccessResult(SASLMechanism usedSaslMechanism) {
            super("SASL authentication successfull using " + usedSaslMechanism.getName());
            this.saslMechanismName = usedSaslMechanism.getName();
        }

        public String getSaslMechanismName() {
            return saslMechanismName;
        }
    }

    public static final class AuthenticatedButUnboundStateDescriptor extends StateDescriptor {
        private AuthenticatedButUnboundStateDescriptor() {
            super(StateDescriptor.Property.multiVisitState);
            addSuccessor(ResourceBindingStateDescriptor.class);
        }
    }

    public static final class ResourceBindingStateDescriptor extends StateDescriptor {
        private ResourceBindingStateDescriptor() {
            super(ResourceBindingState.class, "RFC 6120 § 7");
            addSuccessor(AuthenticatedAndResourceBoundStateDescriptor.class);
        }
    }

    private final class ResourceBindingState extends State {
        private ResourceBindingState(StateDescriptor stateDescriptor, ModularXmppClientToServerConnectionInternal connectionInternal) {
            super(stateDescriptor, connectionInternal);
        }

        @Override
        public StateTransitionResult.AttemptResult transitionInto(WalkStateGraphContext walkStateGraphContext)
                        throws IOException, SmackException, InterruptedException, XMPPException {
            // Calling bindResourceAndEstablishSession() below requires the lastFeaturesReceived sync point to be signaled.
            // Since we entered this state, the FSM has decided that the last features have been received, hence signal
            // the sync point.
            lastFeaturesReceived = true;
            notifyWaitingThreads();

            LoginContext loginContext = walkStateGraphContext.getLoginContext();
            Resourcepart resource = bindResourceAndEstablishSession(loginContext.resource);

            // TODO: This should be a field in the Stream Management (SM) module. Here should be hook which the SM
            // module can use to set the field instead.
            streamResumed = false;

            return new ResourceBoundResult(resource, loginContext.resource);
        }
    }

    public static final class ResourceBoundResult extends StateTransitionResult.Success {
        private final Resourcepart resource;

        private ResourceBoundResult(Resourcepart boundResource, Resourcepart requestedResource) {
            super("Resource '" + boundResource + "' bound (requested: '" + requestedResource + "')");
            this.resource = boundResource;
        }

        public Resourcepart getResource() {
            return resource;
        }
    }

    private boolean compressionEnabled;

    @Override
    public boolean isUsingCompression() {
        return compressionEnabled;
    }

    public static final class AuthenticatedAndResourceBoundStateDescriptor extends StateDescriptor {
        private AuthenticatedAndResourceBoundStateDescriptor() {
            super(AuthenticatedAndResourceBoundState.class, StateDescriptor.Property.finalState);
            addSuccessor(InstantShutdownStateDescriptor.class);
            addSuccessor(ShutdownStateDescriptor.class);
        }
    }

    private final class AuthenticatedAndResourceBoundState extends State {
        private AuthenticatedAndResourceBoundState(StateDescriptor stateDescriptor,
                        ModularXmppClientToServerConnectionInternal connectionInternal) {
            super(stateDescriptor, connectionInternal);
        }

        @Override
        public StateTransitionResult.AttemptResult transitionInto(WalkStateGraphContext walkStateGraphContext)
                        throws NotConnectedException, InterruptedException {
            if (walkFromDisconnectToAuthenticated != null) {
                // If there was already a previous walk to ConnectedButUnauthenticated, then the context of the current
                // walk must not start from the 'Disconnected' state.
                assert walkStateGraphContext.getWalk().get(0).getStateDescriptor().getClass()
                    != DisconnectedStateDescriptor.class;
                // Append the current walk to the previous one.
                walkStateGraphContext.appendWalkTo(walkFromDisconnectToAuthenticated);
            } else {
                walkFromDisconnectToAuthenticated = new ArrayList<>(
                                walkStateGraphContext.getWalkLength() + 1);
                walkStateGraphContext.appendWalkTo(walkFromDisconnectToAuthenticated);
            }
            walkFromDisconnectToAuthenticated.add(this);

            afterSuccessfulLogin(streamResumed);

            return StateTransitionResult.Success.EMPTY_INSTANCE;
        }

        @Override
        public void resetState() {
            authenticated = false;
        }
    }

    static final class ShutdownStateDescriptor extends StateDescriptor {
        private ShutdownStateDescriptor() {
            super(ShutdownState.class);
            addSuccessor(CloseConnectionStateDescriptor.class);
        }
    }

    private final class ShutdownState extends State {
        private ShutdownState(StateDescriptor stateDescriptor,
                        ModularXmppClientToServerConnectionInternal connectionInternal) {
            super(stateDescriptor, connectionInternal);
        }

        @Override
        public StateTransitionResult.TransitionImpossible isTransitionToPossible(WalkStateGraphContext walkStateGraphContext) {
            ensureNotOnOurWayToAuthenticatedAndResourceBound(walkStateGraphContext);
            return null;
        }

        @Override
        public StateTransitionResult.AttemptResult transitionInto(WalkStateGraphContext walkStateGraphContext) {
            closingStreamReceived = false;

            StreamOpenAndCloseFactory openAndCloseFactory = activeTransport.getStreamOpenAndCloseFactory();
            AbstractStreamClose closeStreamElement = openAndCloseFactory.createStreamClose();
            boolean streamCloseIssued = outgoingElementsQueue.offerAndShutdown(closeStreamElement);

            if (streamCloseIssued) {
                activeTransport.notifyAboutNewOutgoingElements();

                boolean successfullyReceivedStreamClose = waitForClosingStreamTagFromServer();

                if (successfullyReceivedStreamClose) {
                    for (Iterator<XmppInputOutputFilter> it = connectionInternal.getXmppInputOutputFilterBeginIterator(); it.hasNext();) {
                        XmppInputOutputFilter filter = it.next();
                        filter.closeInputOutput();
                    }

                    // Closing the filters may produced new outgoing data. Notify the transport about it.
                    activeTransport.afterFiltersClosed();

                    for (Iterator<XmppInputOutputFilter> it = connectionInternal.getXmppInputOutputFilterBeginIterator(); it.hasNext();) {
                        XmppInputOutputFilter filter = it.next();
                        try {
                            filter.waitUntilInputOutputClosed();
                        } catch (IOException | CertificateException | InterruptedException | SmackException | XMPPException e) {
                            LOGGER.log(Level.WARNING, "waitUntilInputOutputClosed() threw", e);
                        }
                    }

                    // For correctness we set authenticated to false here, even though we will later again set it to
                    // false in the disconnected state.
                    authenticated = false;
                }
            }

            return StateTransitionResult.Success.EMPTY_INSTANCE;
        }
    }

    static final class InstantShutdownStateDescriptor extends StateDescriptor {
        private InstantShutdownStateDescriptor() {
            super(InstantShutdownState.class);
            addSuccessor(CloseConnectionStateDescriptor.class);
        }
    }

    private static final class InstantShutdownState extends NoOpState {
        private InstantShutdownState(ModularXmppClientToServerConnection connection, StateDescriptor stateDescriptor, ModularXmppClientToServerConnectionInternal connectionInternal) {
            super(connection, stateDescriptor, connectionInternal);
        }

        @Override
        public StateTransitionResult.TransitionImpossible isTransitionToPossible(WalkStateGraphContext walkStateGraphContext) {
            ensureNotOnOurWayToAuthenticatedAndResourceBound(walkStateGraphContext);
            return null;
        }
    }

    private static final class CloseConnectionStateDescriptor extends StateDescriptor {
        private CloseConnectionStateDescriptor() {
            super(CloseConnectionState.class);
            addSuccessor(DisconnectedStateDescriptor.class);
        }
    }

    private final class CloseConnectionState extends State {
        private CloseConnectionState(StateDescriptor stateDescriptor,
                        ModularXmppClientToServerConnectionInternal connectionInternal) {
            super(stateDescriptor, connectionInternal);
        }

        @Override
        public StateTransitionResult.AttemptResult transitionInto(WalkStateGraphContext walkStateGraphContext) {
            activeTransport.disconnect();
            activeTransport = null;

            authenticated = connected = false;

            return StateTransitionResult.Success.EMPTY_INSTANCE;
        }
    }

    public void addConnectionStateMachineListener(ConnectionStateMachineListener connectionStateMachineListener) {
        connectionStateMachineListeners.add(connectionStateMachineListener);
    }

    public boolean removeConnectionStateMachineListener(ConnectionStateMachineListener connectionStateMachineListener) {
        return connectionStateMachineListeners.remove(connectionStateMachineListener);
    }

    private void invokeConnectionStateMachineListener(ConnectionStateEvent connectionStateEvent) {
        if (connectionStateMachineListeners.isEmpty()) {
            return;
        }

        ASYNC_BUT_ORDERED.performAsyncButOrdered(this, () -> {
            for (ConnectionStateMachineListener connectionStateMachineListener : connectionStateMachineListeners) {
                connectionStateMachineListener.onConnectionStateEvent(connectionStateEvent, this);
            }
        });
    }

    @Override
    public boolean isSecureConnection() {
        final XmppClientToServerTransport transport = activeTransport;
        if (transport == null) {
            return false;
        }
        return transport.isTransportSecured();
    }

    @Override
    protected void connectInternal() throws SmackException, IOException, XMPPException, InterruptedException {
        WalkStateGraphContext walkStateGraphContext = buildNewWalkTo(ConnectedButUnauthenticatedStateDescriptor.class)
                        .build();
        walkStateGraph(walkStateGraphContext);
    }

    private Map<String, Object> getFilterStats() {
        Collection<XmppInputOutputFilter> filters;
        synchronized (this) {
            if (inputOutputFilters.isEmpty() && previousInputOutputFilters != null) {
                filters = previousInputOutputFilters;
            } else {
                filters = inputOutputFilters;
            }
        }

        Map<String, Object> filterStats = new HashMap<>(filters.size());
        for (XmppInputOutputFilter xmppInputOutputFilter : filters) {
            Object stats = xmppInputOutputFilter.getStats();
            String filterName = xmppInputOutputFilter.getFilterName();

            filterStats.put(filterName, stats);
        }

        return filterStats;
    }

    public Stats getStats() {
        Map<Class<? extends ModularXmppClientToServerConnectionModuleDescriptor>, XmppClientToServerTransport.Stats> transportsStats = new HashMap<>(
                        transports.size());
        for (Map.Entry<Class<? extends ModularXmppClientToServerConnectionModuleDescriptor>, XmppClientToServerTransport> entry : transports.entrySet()) {
            XmppClientToServerTransport.Stats transportStats = entry.getValue().getStats();

            transportsStats.put(entry.getKey(), transportStats);
        }

        Map<String, Object> filterStats = getFilterStats();

        return new Stats(transportsStats, filterStats);
    }

    public static final class Stats extends AbstractStats {
        public final Map<Class<? extends ModularXmppClientToServerConnectionModuleDescriptor>, XmppClientToServerTransport.Stats> transportsStats;
        public final Map<String, Object> filtersStats;

        private Stats(Map<Class<? extends ModularXmppClientToServerConnectionModuleDescriptor>, XmppClientToServerTransport.Stats> transportsStats,
                        Map<String, Object> filtersStats) {
            this.transportsStats = Collections.unmodifiableMap(transportsStats);
            this.filtersStats = Collections.unmodifiableMap(filtersStats);
        }

        @Override
        public void appendStatsTo(ExtendedAppendable appendable) throws IOException {
            StringUtils.appendHeading(appendable, "Connection stats", '#').append('\n');

            for (Map.Entry<Class<? extends ModularXmppClientToServerConnectionModuleDescriptor>, XmppClientToServerTransport.Stats> entry : transportsStats.entrySet()) {
                Class<? extends ModularXmppClientToServerConnectionModuleDescriptor> transportClass = entry.getKey();
                XmppClientToServerTransport.Stats stats = entry.getValue();

                StringUtils.appendHeading(appendable, transportClass.getName());
                if (stats != null) {
                    appendable.append(stats.toString());
                } else {
                    appendable.append("No stats available.");
                }
                appendable.append('\n');
            }

            for (Map.Entry<String, Object> entry : filtersStats.entrySet()) {
                String filterName = entry.getKey();
                Object filterStats = entry.getValue();

                StringUtils.appendHeading(appendable, filterName);
                appendable.append(filterStats.toString()).append('\n');
            }
        }

    }
}
