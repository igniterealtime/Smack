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

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.util.CallbackRecipient;
import org.jivesoftware.smack.util.ExceptionCallback;
import org.jivesoftware.smack.util.SuccessCallback;

public abstract class SmackFuture<V, E extends Exception> implements Future<V>, CallbackRecipient<V, E> {

    private boolean cancelled;

    protected V result;

    protected E exception;

    private SuccessCallback<V> successCallback;

    private ExceptionCallback<E> exceptionCallback;

    @Override
    public synchronized final boolean cancel(boolean mayInterruptIfRunning) {
        if (isDone()) {
            return false;
        }

        cancelled = true;

        if (mayInterruptIfRunning) {
            notifyAll();
        }

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

    @Override
    public CallbackRecipient<V, E> onSuccess(SuccessCallback<V> successCallback) {
        this.successCallback = successCallback;
        maybeInvokeCallbacks();
        return this;
    }

    @Override
    public CallbackRecipient<V, E> onError(ExceptionCallback<E> exceptionCallback) {
        this.exceptionCallback = exceptionCallback;
        maybeInvokeCallbacks();
        return this;
    }

    private V getOrThrowExecutionException() throws ExecutionException {
        assert (result != null || exception != null || cancelled);
        if (result != null) {
            return result;
        }
        if (exception != null) {
            throw new ExecutionException(exception);
        }

        assert (cancelled);
        throw new CancellationException();
    }

    @Override
    public synchronized final V get() throws InterruptedException, ExecutionException {
        while (result == null && exception == null && !cancelled) {
            wait();
        }

        return getOrThrowExecutionException();
    }

    public synchronized final V getOrThrow() throws E, InterruptedException {
        while (result == null && exception == null && !cancelled) {
            wait();
        }

        if (exception != null) {
            throw exception;
        }

        if (cancelled) {
            throw new CancellationException();
        }

        assert result != null;
        return result;
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

        if (cancelled) {
            throw new CancellationException();
        }

        if (result == null || exception == null) {
            throw new TimeoutException();
        }

        return getOrThrowExecutionException();
    }

    private static final ExecutorService EXECUTOR_SERVICE;

    static {
        ThreadFactory threadFactory = new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread thread = new Thread(r);
                thread.setDaemon(true);
                thread.setName("SmackFuture Thread");
                return thread;
            }
        };
        BlockingQueue<Runnable> blockingQueue = new ArrayBlockingQueue<>(128);
        RejectedExecutionHandler rejectedExecutionHandler = new RejectedExecutionHandler() {
            @Override
            public void rejectedExecution(Runnable r, ThreadPoolExecutor executor) {
                r.run();
            }
        };
        int cores = Runtime.getRuntime().availableProcessors();
        int maximumPoolSize = cores <= 4 ? 2 : cores;
        ExecutorService executorService = new ThreadPoolExecutor(0, maximumPoolSize, 60L, TimeUnit.SECONDS,
                        blockingQueue, threadFactory, rejectedExecutionHandler);

        EXECUTOR_SERVICE = executorService;
    }

    @SuppressWarnings("FutureReturnValueIgnored")
    protected final synchronized void maybeInvokeCallbacks() {
        if (cancelled) {
            return;
        }

        if (result != null && successCallback != null) {
            EXECUTOR_SERVICE.submit(new Runnable() {
                @Override
                public void run() {
                    successCallback.onSuccess(result);
                }
            });
        }
        else if (exception != null && exceptionCallback != null) {
            EXECUTOR_SERVICE.submit(new Runnable() {
                @Override
                public void run() {
                    exceptionCallback.processException(exception);
                }
            });
        }
    }

    public static class InternalSmackFuture<V, E extends Exception> extends SmackFuture<V, E> {
        public final synchronized void setResult(V result) {
            this.result = result;
            this.notifyAll();

            maybeInvokeCallbacks();
        }

        public final synchronized void setException(E exception) {
            this.exception = exception;
            this.notifyAll();

            maybeInvokeCallbacks();
        }
    }

    public static abstract class InternalProcessStanzaSmackFuture<V, E extends Exception> extends InternalSmackFuture<V, E>
                    implements StanzaListener, ExceptionCallback<E> {

        /**
         * This method checks if the given exception is <b>not</b> fatal. If this method returns <code>false</code>,
         * then the future will automatically set the given exception as failure reason and notify potential waiting
         * threads.
         *
         * @param exception the exception to check.
         * @return <code>true</code> if the exception is not fatal, <code>false</code> otherwise.
         */
        protected abstract boolean isNonFatalException(E exception);

        protected abstract void handleStanza(Stanza stanza);

        @Override
        public synchronized final void processException(E exception) {
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
        public synchronized final void processStanza(Stanza stanza) {
            handleStanza(stanza);
        }
    }

    /**
     * A simple version of InternalSmackFuture which implements isNonFatalException(E) as always returning
     * <code>false</code> method.
     *
     * @param <V>
     */
    public static abstract class SimpleInternalProcessStanzaSmackFuture<V, E extends Exception>
                    extends InternalProcessStanzaSmackFuture<V, E> {
        @Override
        protected boolean isNonFatalException(E exception) {
            return false;
        }
    }

    public static <V, E extends Exception> SmackFuture<V, E> from(V result) {
        InternalSmackFuture<V, E> future = new InternalSmackFuture<>();
        future.setResult(result);
        return future;
    }

}
