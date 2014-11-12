/**
 *
 * Copyright © 2014 Florian Schmaus
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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.packet.TopLevelStreamElement;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.PlainStreamElement;

public class SynchronizationPoint<E extends Exception> {

    private static final Logger LOGGER = Logger.getLogger(SynchronizationPoint.class.getName());

    private final AbstractXMPPConnection connection;
    private final Lock connectionLock;
    private final Condition condition;

    private State state;
    private E failureException;

    public SynchronizationPoint(AbstractXMPPConnection connection) {
        this.connection = connection;
        this.connectionLock = connection.getConnectionLock();
        this.condition = connection.getConnectionLock().newCondition();
        init();
    }

    public void init() {
        state = State.Initial;
        failureException = null;
    }

    public void sendAndWaitForResponse(TopLevelStreamElement request) throws NoResponseException,
                    NotConnectedException {
        assert (state == State.Initial);
        connectionLock.lock();
        try {
            if (request != null) {
                if (request instanceof Packet) {
                    connection.sendPacket((Packet) request);
                }
                else if (request instanceof PlainStreamElement){
                    connection.send((PlainStreamElement) request);
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
        checkForResponse();
    }

    public void sendAndWaitForResponseOrThrow(PlainStreamElement request) throws E, NoResponseException,
                    NotConnectedException {
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

    public void checkIfSuccessOrWaitOrThrow() throws NoResponseException, E {
        checkIfSuccessOrWait();
        if (state == State.Failure) {
            throw failureException;
        }
    }

    public void checkIfSuccessOrWait() throws NoResponseException {
        connectionLock.lock();
        try {
            if (state == State.Success) {
                // Return immediately
                return;
            }
            waitForConditionOrTimeout();
        } finally {
            connectionLock.unlock();
        }
        checkForResponse();
    }

    public void reportSuccess() {
        connectionLock.lock();
        try {
            state = State.Success;
            condition.signal();
        }
        finally {
            connectionLock.unlock();
        }
    }

    public void reportFailure() {
        reportFailure(null);
    }

    public void reportFailure(E failureException) {
        connectionLock.lock();
        try {
            state = State.Failure;
            this.failureException = failureException;
            condition.signal();
        }
        finally {
            connectionLock.unlock();
        }
    }

    public boolean wasSuccessful() {
        return state == State.Success;
    }

    public boolean requestSent() {
        return state == State.RequestSent;
    }

    private void waitForConditionOrTimeout() {
        long remainingWait = TimeUnit.MILLISECONDS.toNanos(connection.getPacketReplyTimeout());
        while (state == State.RequestSent || state == State.Initial) {
            try {
                remainingWait = condition.awaitNanos(
                                remainingWait);
                if (remainingWait <= 0) {
                    state = State.NoResponse;
                    break;
                }
            } catch (InterruptedException e) {
                LOGGER.log(Level.FINE, "was interrupted while waiting, this should not happen", e);
            }
        }
    }

    /**
     * Check for a response and throw a {@link NoResponseException} if there was none.
     * <p>
     * The exception is thrown, if state is one of 'Initial', 'NoResponse' or 'RequestSent'
     * </p>
     * @throws NoResponseException
     */
    private void checkForResponse() throws NoResponseException {
        switch (state) {
        case Initial:
        case NoResponse:
        case RequestSent:
            throw new NoResponseException(connection);
        default:
            // Do nothing
            break;
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
