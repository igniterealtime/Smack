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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.net.ssl.SSLSession;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.ConnectionUnexpectedTerminatedException;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.XMPPException.FailedNonzaException;
import org.jivesoftware.smack.XMPPException.StreamErrorException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.XmppInputOutputFilter;
import org.jivesoftware.smack.compress.packet.Compress;
import org.jivesoftware.smack.compress.packet.Compressed;
import org.jivesoftware.smack.compress.packet.Failure;
import org.jivesoftware.smack.compression.XmppCompressionFactory;
import org.jivesoftware.smack.compression.XmppCompressionManager;
import org.jivesoftware.smack.fsm.StateDescriptorGraph.GraphVertex;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.StreamError;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.sasl.SASLErrorException;
import org.jivesoftware.smack.sasl.SASLMechanism;
import org.jivesoftware.smack.sasl.packet.SaslStreamElements.Challenge;
import org.jivesoftware.smack.sasl.packet.SaslStreamElements.Success;
import org.jivesoftware.smack.util.Objects;
import org.jivesoftware.smack.util.PacketParserUtils;

import org.jxmpp.jid.parts.Resourcepart;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

public abstract class AbstractXmppStateMachineConnection extends AbstractXMPPConnection {

    private final List<ConnectionStateMachineListener> connectionStateMachineListeners = new CopyOnWriteArrayList<>();

    private boolean featuresReceived;

    protected boolean streamResumed;

    private GraphVertex<State> currentStateVertex;

    private List<State> walkFromDisconnectToAuthenticated;

    private final List<XmppInputOutputFilter> inputOutputFilters = new CopyOnWriteArrayList<>();
    private List<XmppInputOutputFilter> previousInputOutputFilters;

    protected AbstractXmppStateMachineConnection(ConnectionConfiguration configuration, GraphVertex<StateDescriptor> initialStateDescriptorVertex) {
        super(configuration);
        currentStateVertex = StateDescriptorGraph.convertToStateGraph(initialStateDescriptorVertex, this);
    }

    @Override
    protected void loginInternal(String username, String password, Resourcepart resource)
                    throws XMPPException, SmackException, IOException, InterruptedException {
        WalkStateGraphContext walkStateGraphContext = buildNewWalkTo(AuthenticatedAndResourceBoundStateDescriptor.class)
                        .withLoginContext(username, password, resource)
                        .build();
        walkStateGraph(walkStateGraphContext);
    }

    protected static WalkStateGraphContextBuilder buildNewWalkTo(Class<? extends StateDescriptor> finalStateClass) {
        return new WalkStateGraphContextBuilder(finalStateClass);
    }

    protected static final class WalkStateGraphContext {
        private final Class<? extends StateDescriptor> finalStateClass;
        private final Class<? extends StateDescriptor> mandatoryIntermediateState;
        private final LoginContext loginContext;

        private final List<State> walkedStateGraphPath = new ArrayList<>();

        /**
         * A linked Map of failed States with their reason as value.
         */
        private final Map<State, TransitionReason> failedStates = new LinkedHashMap<>();

        private boolean mandatoryIntermediateStateHandled;

        private WalkStateGraphContext(Class<? extends StateDescriptor> finalStateClass, Class<? extends StateDescriptor> mandatoryIntermedidateState, LoginContext loginContext) {
            this.finalStateClass = Objects.requireNonNull(finalStateClass);
            this.mandatoryIntermediateState = mandatoryIntermedidateState;
            this.loginContext = loginContext;
        }

        public boolean isFinalStateAuthenticatedAndResourceBound() {
            return finalStateClass == AuthenticatedAndResourceBoundStateDescriptor.class;
        }
    }

    protected static final class WalkStateGraphContextBuilder {
        private final Class<? extends StateDescriptor> finalStateClass;
        private Class<? extends StateDescriptor> mandatoryIntermedidateState;
        private LoginContext loginContext;

        private WalkStateGraphContextBuilder(Class<? extends StateDescriptor> finalStateClass) {
            this.finalStateClass = finalStateClass;
        }

        public WalkStateGraphContextBuilder withMandatoryIntermediateState(Class<? extends StateDescriptor> mandatoryIntermedidateState) {
            this.mandatoryIntermedidateState = mandatoryIntermedidateState;
            return this;
        }

        public WalkStateGraphContextBuilder withLoginContext(String username, String password, Resourcepart resource) {
            LoginContext loginContext = new LoginContext(username, password, resource);
            return withLoginContext(loginContext);
        }

        public WalkStateGraphContextBuilder withLoginContext(LoginContext loginContext) {
            this.loginContext = loginContext;
            return this;
        }

        public WalkStateGraphContext build() {
            return new WalkStateGraphContext(finalStateClass, mandatoryIntermedidateState, loginContext);
        }
    }

    protected final void walkStateGraph(WalkStateGraphContext walkStateGraphContext) throws XMPPErrorException, SASLErrorException,
                    FailedNonzaException, IOException, SmackException, InterruptedException {
        // Save a copy of the current state
        GraphVertex<State> previousStateVertex = currentStateVertex;
        try {
            walkStateGraphInternal(walkStateGraphContext);
        }
        catch (XMPPErrorException | SASLErrorException | FailedNonzaException | IOException | SmackException
                        | InterruptedException e) {
            currentStateVertex = previousStateVertex;
            // Reset that state.
            State revertedState = currentStateVertex.getElement();
            invokeConnectionStateMachineListener(new ConnectionStateEvent.StateRevertBackwardsWalk(revertedState));
            revertedState.resetState();
            throw e;
        }
    }

    private void walkStateGraphInternal(WalkStateGraphContext walkStateGraphContext)
                    throws XMPPErrorException, SASLErrorException, IOException, SmackException, InterruptedException, FailedNonzaException {
        // Save a copy of the current state
        final GraphVertex<State> initialStateVertex = currentStateVertex;
        final State initialState = initialStateVertex.getElement();
        final StateDescriptor initialStateDescriptor = initialState.getStateDescriptor();

        walkStateGraphContext.walkedStateGraphPath.add(initialState);

        if (initialStateDescriptor.getClass() == walkStateGraphContext.finalStateClass) {
            // If this is used as final state, then it should be marked as such.
            assert (initialStateDescriptor.isFinalState());

            // We reached the final state.
            invokeConnectionStateMachineListener(new ConnectionStateEvent.FinalStateReached(initialState));
            return;
        }


        List<GraphVertex<State>> outgoingStateEdges = currentStateVertex.getOutgoingEdges();

        // See if we need to handle mandatory intermediate states.
        if (walkStateGraphContext.mandatoryIntermediateState != null && !walkStateGraphContext.mandatoryIntermediateStateHandled) {
            // Check if outgoingStateEdges contains the mandatory intermediate state.
            GraphVertex<State> mandatoryIntermediateStateVertex = null;
            for (GraphVertex<State> outgoingStateVertex : outgoingStateEdges) {
                if (outgoingStateVertex.getElement().getStateDescriptor().getClass() == walkStateGraphContext.mandatoryIntermediateState) {
                    mandatoryIntermediateStateVertex = outgoingStateVertex;
                    break;
                }
            }

            if (mandatoryIntermediateStateVertex != null) {
                walkStateGraphContext.mandatoryIntermediateStateHandled = true;
                TransitionReason reason = attemptEnterState(mandatoryIntermediateStateVertex, walkStateGraphContext);
                if (reason instanceof TransitionSuccessResult) {
                    walkStateGraph(walkStateGraphContext);
                    return;
                }

                // We could not enter a mandatory intermediate state. Throw here.
                throw new StateMachineException.SmackMandatoryStateFailedException(
                                mandatoryIntermediateStateVertex.getElement(), reason);
            }
        }

        for (Iterator<GraphVertex<State>> it = outgoingStateEdges.iterator(); it.hasNext();) {
            GraphVertex<State>  successorStateVertex = it.next();
            State successorState = successorStateVertex.getElement();
            TransitionReason reason = attemptEnterState(successorStateVertex, walkStateGraphContext);
            if (reason instanceof TransitionSuccessResult) {
                break;
            }

            // If attemptEnterState did not throw and did not return a value of type TransitionSuccessResult, then we
            // just record this value and go on from there. Note that reason may be null, which is returned by
            // attemptEnterState in case the state was already successfully handled. If this is the case, then we don't
            // record it.
            if (reason != null) {
                walkStateGraphContext.failedStates.put(successorState, reason);
            }

            if (!it.hasNext()) {
                throw new StateMachineException.SmackStateGraphDeadEndException(walkStateGraphContext.walkedStateGraphPath, walkStateGraphContext.failedStates);
            }
        }

        // Walk the state graph by recursion.
        walkStateGraph(walkStateGraphContext);
    }

    private TransitionReason attemptEnterState(GraphVertex<State> successorStateVertex,
                    WalkStateGraphContext walkStateGraphContext)
                    throws SmackException, XMPPErrorException, SASLErrorException, IOException, InterruptedException, FailedNonzaException {
        final State successorState = successorStateVertex.getElement();
        final StateDescriptor successorStateDescriptor = successorState.getStateDescriptor();

        if (!successorStateDescriptor.isMultiVisitState() && walkStateGraphContext.walkedStateGraphPath.contains(successorState)) {
            // This can happen if a state leads back to the state where it originated from. See for example the
            // 'Compression' state. We return 'null' here to signal that the state can safely be ignored.
            return null;
        }

        if (successorStateDescriptor.isNotImplemented()) {
            TransitionImpossibleBecauseNotImplemented transtionImpossibleBecauseNotImplemented = new TransitionImpossibleBecauseNotImplemented(
                            successorStateDescriptor);
            invokeConnectionStateMachineListener(new ConnectionStateEvent.TransitionNotPossible(successorState,
                            transtionImpossibleBecauseNotImplemented));
            return transtionImpossibleBecauseNotImplemented;
        }

        final TransitionIntoResult transitionIntoResult;
        try {
            TransitionImpossibleReason transitionImpossibleReason = successorState.isTransitionToPossible(walkStateGraphContext);
            if (transitionImpossibleReason != null) {
                invokeConnectionStateMachineListener(new ConnectionStateEvent.TransitionNotPossible(successorState,
                                transitionImpossibleReason));
                return transitionImpossibleReason;
            }

           invokeConnectionStateMachineListener(new ConnectionStateEvent.AboutToTransitionInto(successorState));
           transitionIntoResult = successorState.transitionInto(walkStateGraphContext);
        } catch (SmackException | XMPPErrorException | SASLErrorException | IOException | InterruptedException
                    | FailedNonzaException e) {
            // TODO Document why this is required given that there is another call site of resetState().
            invokeConnectionStateMachineListener(new ConnectionStateEvent.StateRevertBackwardsWalk(successorState));
            successorState.resetState();
            throw e;
        }
        if (transitionIntoResult instanceof TransitionFailureResult) {
            TransitionFailureResult transitionFailureResult = (TransitionFailureResult) transitionIntoResult;
            invokeConnectionStateMachineListener(new ConnectionStateEvent.TransitionFailed(successorState, transitionFailureResult));
            return transitionIntoResult;
        }

        // If transitionIntoResult is not an instance of TransitionFailureResult, then it has to be of type
        // TransitionSuccessResult.
        TransitionSuccessResult transitionSuccessResult = (TransitionSuccessResult) transitionIntoResult;

        currentStateVertex = successorStateVertex;
        invokeConnectionStateMachineListener(new ConnectionStateEvent.SuccessfullyTransitionedInto(successorState,
                        transitionSuccessResult));

        return transitionSuccessResult;
    }

    protected abstract SSLSession getSSLSession();

    @Override
    protected void afterFeaturesReceived() {
        featuresReceived = true;
        synchronized (this) {
            notifyAll();
        }
    }

    protected final void parseAndProcessElement(String element) throws XmlPullParserException, IOException,
                    InterruptedException, StreamErrorException, SmackException, SmackParsingException {
        XmlPullParser parser = PacketParserUtils.getParserFor(element);

        // Skip the enclosing stream open what is guaranteed to be there.
        parser.next();

        int event = parser.getEventType();
        outerloop: while (true) {
            switch (event) {
            case XmlPullParser.START_TAG:
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
                    saslFeatureReceived.reportFailure(new StreamErrorException(streamError));
                    throw new StreamErrorException(streamError);
                case "features":
                    parseFeatures(parser);
                    afterFeaturesReceived();
                    break;
                // SASL related top level stream elements
                case Challenge.ELEMENT:
                    // The server is challenging the SASL authentication made by the client
                    String challengeData = parser.nextText();
                    getSASLAuthentication().challengeReceived(challengeData);
                    break;
                case Success.ELEMENT:
                    Success success = new Success(parser.nextText());
                    // The SASL authentication with the server was successful. The next step
                    // will be to bind the resource
                    getSASLAuthentication().authenticated(success);
                    sendStreamOpen();
                    break;
                default:
                    parseAndProcessNonza(parser);
                    break;
                }
                break;
            case XmlPullParser.END_DOCUMENT:
                break outerloop;
            }
            event = parser.next();
        }
    }

    protected synchronized void prepareToWaitForFeaturesReceived() {
        featuresReceived = false;
    }

    protected void waitForFeaturesReceived(String waitFor)
                    throws InterruptedException, ConnectionUnexpectedTerminatedException, NoResponseException {
        long waitStartMs = System.currentTimeMillis();
        long timeoutMs = getReplyTimeout();
        synchronized (this) {
            while (!featuresReceived && currentConnectionException == null) {
                long remainingWaitMs = timeoutMs - (System.currentTimeMillis() - waitStartMs);
                if (remainingWaitMs <= 0) {
                    throw NoResponseException.newWith(this, waitFor);
                }
                wait(remainingWaitMs);
            }
            if (currentConnectionException != null) {
                throw new SmackException.ConnectionUnexpectedTerminatedException(currentConnectionException);
            }
        }
    }

    protected void newStreamOpenWaitForFeaturesSequence(String waitFor) throws InterruptedException,
                    ConnectionUnexpectedTerminatedException, NoResponseException, NotConnectedException {
        prepareToWaitForFeaturesReceived();
        sendStreamOpen();
        waitForFeaturesReceived(waitFor);
    }

    protected final void addXmppInputOutputFilter(XmppInputOutputFilter xmppInputOutputFilter) {
        inputOutputFilters.add(0, xmppInputOutputFilter);
    }

    protected final ListIterator<XmppInputOutputFilter> getXmppInputOutputFilterBeginIterator() {
        return inputOutputFilters.listIterator();
    }

    protected final ListIterator<XmppInputOutputFilter> getXmppInputOutputFilterEndIterator() {
        return inputOutputFilters.listIterator(inputOutputFilters.size());
    }

    protected final synchronized List<Object> getFilterStats() {
        Collection<XmppInputOutputFilter> filters;
        if (inputOutputFilters.isEmpty() && previousInputOutputFilters != null) {
            filters = previousInputOutputFilters;
        } else {
            filters = inputOutputFilters;
        }

        List<Object> filterStats = new ArrayList<>(filters.size());
        for (XmppInputOutputFilter xmppInputOutputFilter : filters) {
            Object stats = xmppInputOutputFilter.getStats();
            if (stats != null) {
                filterStats.add(stats);
            }
        }

        return Collections.unmodifiableList(filterStats);
    }

    protected abstract class State {
        private final StateDescriptor stateDescriptor;

        protected State(StateDescriptor stateDescriptor) {
            this.stateDescriptor = stateDescriptor;
        }

        /**
         * Check if the state should be activated.
         *
         * @return <code>null</code> if the state should be activated.
         * @throws SmackException in case a Smack exception occurs.
         */
        protected TransitionImpossibleReason isTransitionToPossible(WalkStateGraphContext walkStateGraphContext) throws SmackException {
            return null;
        }

        protected abstract TransitionIntoResult transitionInto(WalkStateGraphContext walkStateGraphContext)
                        throws XMPPErrorException, SASLErrorException, IOException, SmackException, InterruptedException, FailedNonzaException;

        StateDescriptor getStateDescriptor() {
            return stateDescriptor;
        }

        protected void resetState() {
        }

        @Override
        public String toString() {
            return "State " + stateDescriptor + ' ' + AbstractXmppStateMachineConnection.this;
        }

        protected final void ensureNotOnOurWayToAuthenticatedAndResourceBound(WalkStateGraphContext walkStateGraphContext) {
            if (walkStateGraphContext.isFinalStateAuthenticatedAndResourceBound()) {
                throw new IllegalStateException(
                        "Smack should never attempt to reach the authenticated and resource bound state over " + this
                                + ". This is probably a programming error within Smack, please report it to the develoeprs.");
            }
        }
    }

    abstract static class TransitionReason {
        public final String reason;
        private TransitionReason(String reason) {
            this.reason = reason;
        }

        @Override
        public final String toString() {
            return reason;
        }
    }

    protected static class TransitionImpossibleReason extends TransitionReason {
        public TransitionImpossibleReason(String reason) {
            super(reason);
        }
    }

    protected static class TransitionImpossibleBecauseNotImplemented extends TransitionImpossibleReason {
        public TransitionImpossibleBecauseNotImplemented(StateDescriptor stateDescriptor) {
            super(stateDescriptor.getFullStateName(false) + " is not implemented (yet)");
        }
    }

    protected abstract static class TransitionIntoResult extends TransitionReason {
        public TransitionIntoResult(String reason) {
            super(reason);
        }
    }

    public static class TransitionSuccessResult extends TransitionIntoResult {

        public static final TransitionSuccessResult EMPTY_INSTANCE = new TransitionSuccessResult();

        private TransitionSuccessResult() {
            super("");
        }

        public TransitionSuccessResult(String reason) {
            super(reason);
        }
    }

    public static final class TransitionFailureResult extends TransitionIntoResult {
        private TransitionFailureResult(String reason) {
            super(reason);
        }
    }

    protected final class NoOpState extends State {

        private NoOpState(StateDescriptor stateDescriptor) {
            super(stateDescriptor);
        }

        @Override
        protected TransitionImpossibleReason isTransitionToPossible(WalkStateGraphContext walkStateGraphContext) {
            // Transition into a NoOpState is always possible.
            return null;
        }

        @Override
        protected TransitionIntoResult transitionInto(WalkStateGraphContext walkStateGraphContext) {
            // Transition into a NoOpState always succeeds.
            return TransitionSuccessResult.EMPTY_INSTANCE;
        }
    }

    protected static class DisconnectedStateDescriptor extends StateDescriptor {
        protected DisconnectedStateDescriptor() {
            super(DisconnectedState.class, StateDescriptor.Property.finalState);
        }
    }

    private final class DisconnectedState extends State {

        private DisconnectedState(StateDescriptor stateDescriptor) {
            super(stateDescriptor);
        }

        @Override
        protected TransitionIntoResult transitionInto(WalkStateGraphContext walkStateGraphContext) {
            if (inputOutputFilters.isEmpty()) {
                previousInputOutputFilters = null;
            } else {
                previousInputOutputFilters = new ArrayList<>(inputOutputFilters.size());
                previousInputOutputFilters.addAll(inputOutputFilters);
                inputOutputFilters.clear();
            }

            ListIterator<State> it = walkFromDisconnectToAuthenticated.listIterator(
                            walkFromDisconnectToAuthenticated.size());
            while (it.hasPrevious()) {
                State stateToReset = it.previous();
                stateToReset.resetState();
            }
            walkFromDisconnectToAuthenticated = null;

            return TransitionSuccessResult.EMPTY_INSTANCE;
        }
    }

    protected static final class ConnectedButUnauthenticatedStateDescriptor extends StateDescriptor {
        private ConnectedButUnauthenticatedStateDescriptor() {
            super(ConnectedButUnauthenticatedState.class, StateDescriptor.Property.finalState);
            addSuccessor(SaslAuthenticationStateDescriptor.class);
        }
    }

    private final class ConnectedButUnauthenticatedState extends State {
        private ConnectedButUnauthenticatedState(StateDescriptor stateDescriptor) {
            super(stateDescriptor);
        }

        @Override
        protected TransitionIntoResult transitionInto(WalkStateGraphContext walkStateGraphContext) {
            assert (walkFromDisconnectToAuthenticated == null);
            if (getStateDescriptor().getClass() == walkStateGraphContext.finalStateClass) {
                // If this is the final state, then record the walk so far.
                walkFromDisconnectToAuthenticated = new ArrayList<>(walkStateGraphContext.walkedStateGraphPath);
            }

            connected = true;
            return TransitionSuccessResult.EMPTY_INSTANCE;
        }

        @Override
        protected void resetState() {
            connected = false;
        }
    }

    protected static final class SaslAuthenticationStateDescriptor extends StateDescriptor {
        private SaslAuthenticationStateDescriptor() {
            super(SaslAuthenticationState.class, "RFC 6120 § 6");
            addSuccessor(AuthenticatedButUnboundStateDescriptor.class);
        }
    }

    private final class SaslAuthenticationState extends State {
        private SaslAuthenticationState(StateDescriptor stateDescriptor) {
            super(stateDescriptor);
        }

        @Override
        protected TransitionIntoResult transitionInto(WalkStateGraphContext walkStateGraphContext) throws XMPPErrorException,
                        SASLErrorException, IOException, SmackException, InterruptedException {
            prepareToWaitForFeaturesReceived();

            LoginContext loginContext = walkStateGraphContext.loginContext;
            SASLMechanism usedSaslMechanism = saslAuthentication.authenticate(loginContext.username, loginContext.password, config.getAuthzid(), getSSLSession());
            // authenticate() will only return if the SASL authentication was successful, but we also need to wait for the next round of stream features.

            waitForFeaturesReceived("server stream features after SASL authentication");

            return new SaslAuthenticationSuccessResult(usedSaslMechanism);
        }
    }

    public static final class SaslAuthenticationSuccessResult extends TransitionSuccessResult {
        private final String saslMechanismName;

        private SaslAuthenticationSuccessResult(SASLMechanism usedSaslMechanism) {
            super("SASL authentication successfull using " + usedSaslMechanism.getName());
            this.saslMechanismName = usedSaslMechanism.getName();
        }

        public String getSaslMechanismName() {
            return saslMechanismName;
        }
    }

    protected static final class AuthenticatedButUnboundStateDescriptor extends StateDescriptor {
        private AuthenticatedButUnboundStateDescriptor() {
            super(StateDescriptor.Property.multiVisitState);
            addSuccessor(ResourceBindingStateDescriptor.class);
            addSuccessor(CompressionStateDescriptor.class);
        }
    }

    protected static final class ResourceBindingStateDescriptor extends StateDescriptor {
        private ResourceBindingStateDescriptor() {
            super(ResourceBindingState.class, "RFC 6120 § 7");
            addSuccessor(AuthenticatedAndResourceBoundStateDescriptor.class);
        }
    }

    private final class ResourceBindingState extends State {
        private ResourceBindingState(StateDescriptor stateDescriptor) {
            super(stateDescriptor);
        }

        @Override
        protected TransitionIntoResult transitionInto(WalkStateGraphContext walkStateGraphContext) throws XMPPErrorException,
                        SASLErrorException, IOException, SmackException, InterruptedException {
            // TODO: The reportSuccess() is just a quick fix until there is a variant of the
            // bindResourceAndEstablishSession() method which does not require this.
            lastFeaturesReceived.reportSuccess();

            LoginContext loginContext = walkStateGraphContext.loginContext;
            Resourcepart resource = bindResourceAndEstablishSession(loginContext.resource);
            streamResumed = false;

            return new ResourceBoundResult(resource, loginContext.resource);
        }
    }

    public static final class ResourceBoundResult extends TransitionSuccessResult {
        private final Resourcepart resource;

        private ResourceBoundResult(Resourcepart boundResource, Resourcepart requestedResource) {
            super("Resource '" + boundResource  + "' bound (requested: '" + requestedResource + "'");
            this.resource = boundResource;
        }

        public Resourcepart getResource() {
            return resource;
        }
    }

    protected static final class CompressionStateDescriptor extends StateDescriptor {
        private CompressionStateDescriptor() {
            super(CompressionState.class, 138);
            addSuccessor(AuthenticatedButUnboundStateDescriptor.class);
            declarePrecedenceOver(ResourceBindingStateDescriptor.class);
        }
    }

    private boolean compressionEnabled;

    private class CompressionState extends State {
        private XmppCompressionFactory selectedCompressionFactory;
        private XmppInputOutputFilter usedXmppInputOutputCompressionFitler;

        protected CompressionState(StateDescriptor stateDescriptor) {
            super(stateDescriptor);
        }

        @Override
        protected TransitionImpossibleReason isTransitionToPossible(WalkStateGraphContext walkStateGraphContext) {
            if (!config.isCompressionEnabled()) {
                return new TransitionImpossibleReason("Stream compression disabled");
            }

            Compress.Feature compressFeature = getFeature(Compress.Feature.ELEMENT, Compress.NAMESPACE);
            if (compressFeature == null) {
                return new TransitionImpossibleReason("Stream compression not supported");
            }

            selectedCompressionFactory = XmppCompressionManager.getBestFactory(compressFeature);
            if (selectedCompressionFactory == null) {
                return new TransitionImpossibleReason("No matching compression factory");
            }

            usedXmppInputOutputCompressionFitler = selectedCompressionFactory.fabricate(config);

            return null;
        }

        @Override
        protected TransitionIntoResult transitionInto(WalkStateGraphContext walkStateGraphContext)
                        throws NoResponseException, NotConnectedException, FailedNonzaException, InterruptedException,
                        ConnectionUnexpectedTerminatedException {
            final String compressionMethod = selectedCompressionFactory.getCompressionMethod();
            sendAndWaitForResponse(new Compress(compressionMethod), Compressed.class, Failure.class);

            addXmppInputOutputFilter(usedXmppInputOutputCompressionFitler);

            newStreamOpenWaitForFeaturesSequence("server stream features after compression enabled");

            compressionEnabled = true;

            return new CompressionTransitionSuccessResult(compressionMethod);
        }

        @Override
        protected void resetState() {
            selectedCompressionFactory = null;
            usedXmppInputOutputCompressionFitler = null;
            compressionEnabled = false;
        }
    }

    public static final class CompressionTransitionSuccessResult extends TransitionSuccessResult {
        private final String compressionMethod;

        private CompressionTransitionSuccessResult(String compressionMethod) {
            super(compressionMethod + " compression enabled");
            this.compressionMethod = compressionMethod;
        }

        public String getCompressionMethod() {
            return compressionMethod;
        }
    }

    @Override
    public final boolean isUsingCompression() {
        return compressionEnabled;
    }

    protected static final class AuthenticatedAndResourceBoundStateDescriptor extends StateDescriptor {
        private AuthenticatedAndResourceBoundStateDescriptor() {
            super(AuthenticatedAndResourceBoundState.class, StateDescriptor.Property.finalState);
        }
    }

    private final class AuthenticatedAndResourceBoundState extends State {
        private AuthenticatedAndResourceBoundState(StateDescriptor stateDescriptor) {
            super(stateDescriptor);
        }

        @Override
        protected TransitionIntoResult transitionInto(WalkStateGraphContext walkStateGraphContext)
                        throws NotConnectedException, InterruptedException {
            if (walkFromDisconnectToAuthenticated != null) {
                // If there was already a previous walk to ConnectedButUnauthenticated, then the context of the current
                // walk must not start from the 'Disconnected' state.
                assert (walkStateGraphContext.walkedStateGraphPath.get(0).stateDescriptor.getClass() != DisconnectedStateDescriptor.class);
                walkFromDisconnectToAuthenticated.addAll(walkStateGraphContext.walkedStateGraphPath);
            } else {
                walkFromDisconnectToAuthenticated = new ArrayList<>(walkStateGraphContext.walkedStateGraphPath.size() + 1);
                walkFromDisconnectToAuthenticated.addAll(walkStateGraphContext.walkedStateGraphPath);
            }
            walkFromDisconnectToAuthenticated.add(this);

            afterSuccessfulLogin(streamResumed);
            return TransitionSuccessResult.EMPTY_INSTANCE;
        }

        @Override
        protected void resetState() {
            authenticated = false;
        }
    }

    public void addConnectionStateMachineListener(ConnectionStateMachineListener connectionStateMachineListener) {
        connectionStateMachineListeners.add(connectionStateMachineListener);
    }

    public boolean removeConnectionStateMachineListener(ConnectionStateMachineListener connectionStateMachineListener) {
        return connectionStateMachineListeners.remove(connectionStateMachineListener);
    }

    protected void invokeConnectionStateMachineListener(ConnectionStateEvent connectionStateEvent) {
        if (connectionStateMachineListeners.isEmpty()) {
            return;
        }

        ASYNC_BUT_ORDERED.performAsyncButOrdered(this, () -> {
            for (ConnectionStateMachineListener connectionStateMachineListener : connectionStateMachineListeners) {
                connectionStateMachineListener.onConnectionStateEvent(connectionStateEvent, this);
            }
        });
    }
}
