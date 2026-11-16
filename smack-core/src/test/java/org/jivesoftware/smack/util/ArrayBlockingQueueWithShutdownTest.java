/*
 *
 * Copyright 2026 Florian Schmaus
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

// A broken wake up would otherwise block a test forever.
@Timeout(30)
public class ArrayBlockingQueueWithShutdownTest {

    private final ArrayBlockingQueueWithShutdown<String> queue = new ArrayBlockingQueueWithShutdown<>(2);

    @Test
    public void wakeUpBeforeTakeTest() throws InterruptedException {
        queue.wakeUp();
        assertNull(queue.take());
    }

    @Test
    public void wakeUpWaitingTakeTest() throws InterruptedException {
        final AtomicReference<Object> result = new AtomicReference<>();
        Thread consumer = new Thread(() -> {
            try {
                result.set(queue.take());
            }
            catch (InterruptedException e) {
                result.set(e);
            }
        });
        consumer.start();
        awaitWaiting(consumer);

        queue.wakeUp();
        consumer.join(5000);
        assertFalse(consumer.isAlive(), "The consumer was not woken up");
        assertNull(result.get());
    }

    @Test
    public void wakeUpTakesPrecedenceOverElementsTest() throws InterruptedException {
        queue.put("a");
        queue.wakeUp();
        assertNull(queue.take());
        assertEquals("a", queue.take());
    }

    @Test
    public void wakeUpsAreCoalescedTest() throws InterruptedException {
        queue.wakeUp();
        queue.wakeUp();
        queue.put("a");
        assertNull(queue.take());
        assertEquals("a", queue.take());
    }

    @Test
    public void pollIgnoresWakeUpTest() throws InterruptedException {
        queue.put("a");
        queue.wakeUp();
        assertEquals("a", queue.poll(5, TimeUnit.SECONDS));
        // The wake up is still pending.
        assertNull(queue.take());
    }

    @Test
    public void shutdownTest() {
        queue.wakeUp();
        queue.shutdown();
        assertThrows(InterruptedException.class, () -> queue.take());
    }

    private static void awaitWaiting(Thread thread) throws InterruptedException {
        final long deadline = System.currentTimeMillis() + 5000;
        while (thread.getState() != Thread.State.WAITING) {
            if (System.currentTimeMillis() > deadline) {
                fail("Thread " + thread + " did not start waiting");
            }
            Thread.sleep(10);
        }
    }
}
