/**
 *
 * Copyright 2017 Florian Schmaus
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

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.packet.Stanza;

public abstract class SmackFuture<V> implements Future<V> {

    private boolean cancelled;

    private V result;

    protected Exception exception;

    private SuccessCallback<V> successCallback;

    private ExceptionCallback exceptionCallback;

    @Override
    public synchronized final boolean cancel(boolean mayInterruptIfRunning) {
        if (isDone()) {
            return false;
        }

        cancelled = true;
        return true;
    }

    @Override
    public synchronized final boolean isCancelled() {
        return cancelled;
    }

    @Override
    public synchronized final boolean isDone() {
        return result != null;
    }

    public void onSuccessOrError(SuccessCallback<V> successCallback, ExceptionCallback exceptionCallback) {
        this.successCallback = successCallback;
        this.exceptionCallback = exceptionCallback;

        maybeInvokeCallbacks();
    }

    public void onSuccess(SuccessCallback<V> successCallback) {
        onSuccessOrError(successCallback, null);
    }

    public void onError(ExceptionCallback exceptionCallback) {
        onSuccessOrError(null, exceptionCallback);
    }

    private final V getResultOrThrow() throws ExecutionException {
        assert (result != null || exception != null);
        if (result != null) {
            return result;
        }

        throw new ExecutionException(exception);
    }

    @Override
    public synchronized final V get() throws InterruptedException, ExecutionException {
        while (result == null && exception == null) {
            wait();
        }

        return getResultOrThrow();
    }

    @Override
    public synchronized final V get(long timeout, TimeUnit unit)
                    throws InterruptedException, ExecutionException, TimeoutException {
        final long deadline = System.currentTimeMillis() + unit.toMillis(timeout);
        while (result != null && exception != null) {
            final long waitTimeRemaining = deadline - System.currentTimeMillis();
            if (waitTimeRemaining > 0) {
                wait(waitTimeRemaining);
            }
        }

        if (result == null || exception == null) {
            throw new TimeoutException();
        }

        return getResultOrThrow();
    }

    protected final synchronized void maybeInvokeCallbacks() {
        if (result != null && successCallback != null) {
            successCallback.onSuccess(result);
        } else if (exception != null && exceptionCallback != null) {
            exceptionCallback.processException(exception);
        }
    }

    /**
     * This method checks if the given exception is <b>not</b> fatal. If this method returns <code>false</code>, then
     * the future will automatically set the given exception as failure reason and notify potential waiting threads.
     *
     * @param exception the exception to check.
     * @return <code>true</code> if the exception is not fatal, <code>false</code> otherwise.
     */
    protected abstract boolean isNonFatalException(Exception exception);

    protected abstract void handleStanza(Stanza stanza) throws NotConnectedException, InterruptedException;

    protected final void setResult(V result) {
        assert (Thread.holdsLock(this));

        this.result = result;
        this.notifyAll();

        maybeInvokeCallbacks();
    }

    public static abstract class InternalSmackFuture<V> extends SmackFuture<V> implements StanzaListener, ExceptionCallback {

        @Override
        public synchronized final void processException(Exception exception) {
            if (!isNonFatalException(exception)) {
                this.exception = exception;
                this.notifyAll();

                maybeInvokeCallbacks();
            }
        }

        /**
         * Wrapper method for {@link #handleStanza(Stanza)}. Note that this method is <code>synchronized</code>.
         */
        @Override
        public synchronized final void processStanza(Stanza stanza) throws NotConnectedException, InterruptedException {
            handleStanza(stanza);
        }
    }

    /**
     * A simple version of InternalSmackFuture which implements {@link #isNonFatalException(Exception)} as always returning <code>false</code> method.
     *
     * @param <V>
     */
    public static abstract class SimpleInternalSmackFuture<V> extends InternalSmackFuture<V> {
        @Override
        protected boolean isNonFatalException(Exception exception) {
            return false;
        }
    }
}
