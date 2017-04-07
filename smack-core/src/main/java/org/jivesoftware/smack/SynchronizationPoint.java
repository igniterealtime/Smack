/**
 *
 * Copyright © 2014-2015 Florian Schmaus
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
package org.jivesoftware.smack;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;

import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.packet.TopLevelStreamElement;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.packet.Nonza;

public class SynchronizationPoint<E extends Exception> {

    private final AbstractXMPPConnection connection;
    private final Lock connectionLock;
    private final Condition condition;
    private final String waitFor;

    // Note that there is no need to make 'state' and 'failureException' volatile. Since 'lock' and 'unlock' have the
    // same memory synchronization effects as synchronization block enter and leave.
    private State state;
    private E failureException;

    /**
     * Construct a new synchronization point for the given connection.
     *
     * @param connection the connection of this synchronization point.
     * @param waitFor a description of the event this synchronization point handles.
     */
    public SynchronizationPoint(AbstractXMPPConnection connection, String waitFor) {
        this.connection = connection;
        this.connectionLock = connection.getConnectionLock();
        this.condition = connection.getConnectionLock().newCondition();
        this.waitFor = waitFor;
        init();
    }

    /**
     * Initialize (or reset) this synchronization point.
     */
    public void init() {
        connectionLock.lock();
        state = State.Initial;
        failureException = null;
        connectionLock.unlock();
    }

    /**
     * Send the given top level stream element and wait for a response.
     *
     * @param request the plain stream element to send.
     * @throws NoResponseException if no response was received.
     * @throws NotConnectedException if the connection is not connected.
     * @return <code>null</code> if synchronization point was successful, or the failure Exception.
     */
    public E sendAndWaitForResponse(TopLevelStreamElement request) throws NoResponseException,
                    NotConnectedException, InterruptedException {
        assert (state == State.Initial);
        connectionLock.lock();
        try {
            if (request != null) {
                if (request instanceof Stanza) {
                    connection.sendStanza((Stanza) request);
                }
                else if (request instanceof Nonza){
                    connection.sendNonza((Nonza) request);
                } else {
                    throw new IllegalStateException("Unsupported element type");
                }
                state = State.RequestSent;
            }
            waitForConditionOrTimeout();
        }
        finally {
            connectionLock.unlock();
        }
        return checkForResponse();
    }

    /**
     * Send the given plain stream element and wait for a response.
     *
     * @param request the plain stream element to send.
     * @throws E if an failure was reported.
     * @throws NoResponseException if no response was received.
     * @throws NotConnectedException if the connection is not connected.
     */
    public void sendAndWaitForResponseOrThrow(Nonza request) throws E, NoResponseException,
                    NotConnectedException, InterruptedException {
        sendAndWaitForResponse(request);
        switch (state) {
        case Failure:
            if (failureException != null) {
                throw failureException;
            }
            break;
        default:
            // Success, do nothing
        }
    }

    /**
     * Check if this synchronization point is successful or wait the connections reply timeout.
     * @throws NoResponseException if there was no response marking the synchronization point as success or failed.
     * @throws E if there was a failure
     * @throws InterruptedException 
     */
    public void checkIfSuccessOrWaitOrThrow() throws NoResponseException, E, InterruptedException {
        checkIfSuccessOrWait();
        if (state == State.Failure) {
            throw failureException;
        }
    }

    /**
     * Check if this synchronization point is successful or wait the connections reply timeout.
     * @throws NoResponseException if there was no response marking the synchronization point as success or failed.
     * @throws InterruptedException
     * @return <code>null</code> if synchronization point was successful, or the failure Exception.
     */
    public E checkIfSuccessOrWait() throws NoResponseException, InterruptedException {
        connectionLock.lock();
        try {
            switch (state) {
            // Return immediately on success or failure
            case Success:
                return null;
            case Failure:
                return failureException;
            default:
                // Do nothing
                break;
            }
            waitForConditionOrTimeout();
        } finally {
            connectionLock.unlock();
        }
        return checkForResponse();
    }

    /**
     * Report this synchronization point as successful.
     */
    public void reportSuccess() {
        connectionLock.lock();
        try {
            state = State.Success;
            condition.signalAll();
        }
        finally {
            connectionLock.unlock();
        }
    }

    /**
     * Deprecated.
     * @deprecated use {@link #reportFailure(Exception)} instead.
     */
    @Deprecated
    public void reportFailure() {
        reportFailure(null);
    }

    /**
     * Report this synchronization point as failed because of the given exception. The {@code failureException} must be set.
     *
     * @param failureException the exception causing this synchronization point to fail.
     */
    public void reportFailure(E failureException) {
        assert failureException != null;
        connectionLock.lock();
        try {
            state = State.Failure;
            this.failureException = failureException;
            condition.signalAll();
        }
        finally {
            connectionLock.unlock();
        }
    }

    /**
     * Check if this synchronization point was successful.
     *
     * @return true if the synchronization point was successful, false otherwise.
     */
    public boolean wasSuccessful() {
        connectionLock.lock();
        try {
            return state == State.Success;
        }
        finally {
            connectionLock.unlock();
        }
    }

    /**
     * Check if this synchronization point has its request already sent.
     *
     * @return true if the request was already sent, false otherwise.
     */
    public boolean requestSent() {
        connectionLock.lock();
        try {
            return state == State.RequestSent;
        }
        finally {
            connectionLock.unlock();
        }
    }

    public E getFailureException() {
        connectionLock.lock();
        try {
            return failureException;
        }
        finally {
            connectionLock.unlock();
        }
    }

    /**
     * Wait for the condition to become something else as {@link State#RequestSent} or {@link State#Initial}.
     * {@link #reportSuccess()}, {@link #reportFailure()} and {@link #reportFailure(Exception)} will either set this
     * synchronization point to {@link State#Success} or {@link State#Failure}. If none of them is set after the
     * connections reply timeout, this method will set the state of {@link State#NoResponse}.
     * @throws InterruptedException 
     */
    private void waitForConditionOrTimeout() throws InterruptedException {
        long remainingWait = TimeUnit.MILLISECONDS.toNanos(connection.getReplyTimeout());
        while (state == State.RequestSent || state == State.Initial) {
            if (remainingWait <= 0) {
                state = State.NoResponse;
                break;
            }
            remainingWait = condition.awaitNanos(remainingWait);
        }
    }

    /**
     * Check for a response and throw a {@link NoResponseException} if there was none.
     * <p>
     * The exception is thrown, if state is one of 'Initial', 'NoResponse' or 'RequestSent'
     * </p>
     * @return <code>true</code> if synchronization point was successful, <code>false</code> on failure.
     * @throws NoResponseException
     */
    private E checkForResponse() throws NoResponseException {
        switch (state) {
        case Initial:
        case NoResponse:
        case RequestSent:
            throw NoResponseException.newWith(connection, waitFor);
        case Success:
            return null;
        case Failure:
            return failureException;
        default:
            throw new AssertionError("Unknown state " + state);
        }
    }

    private enum State {
        Initial,
        RequestSent,
        NoResponse,
        Success,
        Failure,
    }
}
