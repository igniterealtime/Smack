/**
 *
 * Copyright 2015 Florian Schmaus
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
package org.jivesoftware.smack.util;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The event manager class is used to perform actions and wait for an event, which is usually caused by the action (or maybe never occurs).
 * <p>
 * Events are distinguished by an unique event key. They can produce an event result, which can simply be null.
 * </p>
 * <p>
 * The action is able to throw an exception.
 * </p>
 *
 * @param <K> the event key.
 * @param <R> the event result.
 * @param <E> the exception which could be thrown by the action.
 */
public class EventManger<K, R, E extends Exception> {

    private final Map<K, Reference<R>> events = new ConcurrentHashMap<>();

    /**
     * Perform an action and wait for an event.
     * <p>
     * The event is signaled with {@link #signalEvent(Object, Object)}.
     * </p>
     *
     * @param eventKey the event key, must not be null.
     * @param timeout the timeout to wait for the event in milliseconds.
     * @param action the action to perform prior waiting for the event, must not be null.
     * @return the event value, may be null.
     * @throws InterruptedException if interrupted while waiting for the event.
     * @throws E depending on the concrete use case.
     */
    public R performActionAndWaitForEvent(K eventKey, long timeout, Callback<E> action) throws InterruptedException, E {
        final Reference<R> reference = new Reference<>();
        events.put(eventKey, reference);
        try {
            synchronized (reference) {
                action.action();
                reference.wait(timeout);
            }
            return reference.eventResult;
        }
        finally {
            events.remove(eventKey);
        }
    }

    /**
     * Signal an event and the event result.
     * <p>
     * This method will return <code>false</code> if the event was not created with
     * {@link #performActionAndWaitForEvent(Object, long, Callback)}.
     * </p>
     *
     * @param eventKey the event key, must not be null.
     * @param eventResult the event result, may be null.
     * @return true if the event was found and signaled, false otherwise.
     */
    public boolean signalEvent(K eventKey, R eventResult) {
        final Reference<R> reference = events.get(eventKey);
        if (reference == null) {
            return false;
        }
        reference.eventResult = eventResult;
        synchronized (reference) {
            reference.notifyAll();
        }
        return true;
    }

    private static class Reference<V> {
        volatile V eventResult;
    }

    public interface Callback<E extends Exception> {
        void action() throws E;
    }
}
