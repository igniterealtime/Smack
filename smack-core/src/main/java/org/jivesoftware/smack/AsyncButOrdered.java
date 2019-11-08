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
package org.jivesoftware.smack;

import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;

/**
 * Helper class to perform an operation asynchronous but keeping the order in respect to a given key.
 * <p>
 * A typical use pattern for this helper class consists of callbacks for an abstract entity where the order of callbacks
 * matters, which eventually call user code in form of listeners. Since the order the callbacks matters, you need to use
 * synchronous connection listeners. But if those listeners would invoke the user provided listeners, and if those user
 * provided listeners would take a long time to complete, or even worse, block, then Smack's total progress is stalled,
 * since synchronous connection listeners are invoked from the main event loop.
 * </p>
 * <p>
 * It is common for those situations that the order of callbacks is not globally important, but only important in
 * respect to an particular entity. Take chat state notifications (CSN) for example: Assume there are two contacts which
 * send you CSNs. If a contact sends you first 'active' and then 'inactive, it is crucial that first the listener is
 * called with 'active' and afterwards with 'inactive'. But if there is another contact is sending 'composing' followed
 * by 'paused', then it is also important that the listeners are invoked in the correct order, but the order in which
 * the listeners for those two contacts are invoked does not matter.
 * </p>
 * <p>
 * Using this helper class, one would call {@link #performAsyncButOrdered(Object, Runnable)} which the remote contacts
 * JID as first argument and a {@link Runnable} invoking the user listeners as second. This class guarantees that
 * runnables of subsequent invocations are always executed after the runnables of previous invocations using the same
 * key.
 * </p>
 *
 * @param <K> the type of the key
 * @since 4.3
 */
public class AsyncButOrdered<K> {

    /**
     * A map with the currently pending runnables for a given key. Note that this is a weak hash map so we do not have
     * to take care of removing the keys ourselfs from the map.
     */
    private final Map<K, Queue<Runnable>> pendingRunnables = new WeakHashMap<>();

    /**
     * A marker map if there is an active thread for the given key. Holds the responsible handler thread if one is
     * active, otherwise the key is non-existend in the map.
     */
    private final Map<K, Handler> threadActiveMap = new HashMap<>();

    private final Executor executor;

    public AsyncButOrdered() {
        this(null);
    }

    public AsyncButOrdered(Executor executor) {
        this.executor = executor;
    }

    private void scheduleHandler(Handler handler) {
        if (executor == null) {
            AbstractXMPPConnection.asyncGo(handler);
        } else {
            executor.execute(handler);
        }
    }

    /**
     * Invoke the given {@link Runnable} asynchronous but ordered in respect to the given key.
     *
     * @param key the key deriving the order
     * @param runnable the {@link Runnable} to run
     * @return true if a new thread was created
     */
    public boolean performAsyncButOrdered(K key, Runnable runnable) {
        // First check if a key queue already exists, create one if not.
        Queue<Runnable> keyQueue;
        synchronized (pendingRunnables) {
            keyQueue = pendingRunnables.get(key);
            if (keyQueue == null) {
                keyQueue = new ConcurrentLinkedQueue<>();
                pendingRunnables.put(key, keyQueue);
            }
        }

        // Then add the task to the queue.
        keyQueue.add(runnable);

        // Finally check if there is already a handler working on that queue, create one if not.
        Handler newlyCreatedHandler = null;
        synchronized (threadActiveMap) {
            if (!threadActiveMap.containsKey(key)) {
                newlyCreatedHandler = new Handler(keyQueue, key);

                // Mark that there is thread active for the given key. Note that this has to be done before scheduling
                // the handler thread.
                threadActiveMap.put(key, newlyCreatedHandler);
            }
        }

        if (newlyCreatedHandler != null) {
            scheduleHandler(newlyCreatedHandler);
            return true;
        }

        return false;
    }

    public Executor asExecutorFor(final K key) {
        return new Executor() {
            @Override
            public void execute(Runnable runnable) {
                performAsyncButOrdered(key, runnable);
            }
        };
    }

    private class Handler implements Runnable {
        private final Queue<Runnable> keyQueue;
        private final K key;

        Handler(Queue<Runnable> keyQueue, K key) {
            this.keyQueue = keyQueue;
            this.key = key;
        }

        @Override
        public void run() {
            mainloop:
            while (true) {
                Runnable runnable = null;
                while ((runnable = keyQueue.poll()) != null) {
                    try {
                        runnable.run();
                    } catch (Throwable t) {
                        // The run() method threw, this handler thread is going to terminate because of that. We create
                        // a new handler to continue working on the queue while throwing the throwable so that the
                        // executor can handle it.
                        Handler newlyCreatedHandler = new Handler(keyQueue, key);
                        synchronized (threadActiveMap) {
                            threadActiveMap.put(key, newlyCreatedHandler);
                        }
                        scheduleHandler(newlyCreatedHandler);
                        throw t;
                    }
                }

                synchronized (threadActiveMap) {
                    // If the queue is empty, stop this handler, otherwise continue looping.
                    if (keyQueue.isEmpty()) {
                        threadActiveMap.remove(key);
                        break mainloop;
                    }
                }
            }
        }
    }
}
