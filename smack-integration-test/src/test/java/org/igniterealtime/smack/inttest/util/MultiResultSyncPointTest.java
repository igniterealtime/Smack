/**
 *
 * Copyright 2024 Guus der Kinderen
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
package org.igniterealtime.smack.inttest.util;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeoutException;

import org.jivesoftware.smack.util.Async;

import org.junit.jupiter.api.Test;

public class MultiResultSyncPointTest {
    @Test
    public void testResultSyncPoint() throws Exception {
        final String result1 = "r1";
        final String result2 = "r2";
        final CyclicBarrier barrier = new CyclicBarrier(2);
        final MultiResultSyncPoint<String, Exception> rsp = new MultiResultSyncPoint<>(2);
        Async.go(new Async.ThrowingRunnable() {
            @Override
            public void runOrThrow() throws InterruptedException, BrokenBarrierException {
                barrier.await();
                rsp.signal(result1);
                rsp.signal(result2);
            }
        });
        barrier.await();
        List<String> receivedResult = rsp.waitForResults(60 * 1000);
        assertTrue(receivedResult.contains(result1));
        assertTrue(receivedResult.contains(result2));
    }

    @Test
    public void exceptionTestResultSyncPoint() throws Exception {
        final CyclicBarrier barrier = new CyclicBarrier(2);
        final ResultSyncPoint<String, MultiResultSyncPointTest.TestException> rsp = new ResultSyncPoint<>();
        Async.go(new Async.ThrowingRunnable() {
            @Override
            public void runOrThrow() throws InterruptedException, BrokenBarrierException {
                barrier.await();
                rsp.signal(new MultiResultSyncPointTest.TestException());
            }
        });
        barrier.await();
        assertThrows(MultiResultSyncPointTest.TestException.class, () -> rsp.waitForResult(60 * 1000));
    }

    @Test
    public void testTimeout() throws Exception {
        final MultiResultSyncPoint<String, Exception> rsp = new MultiResultSyncPoint<>(2);
        try {
            rsp.waitForResults(100);
            fail("A timeout exception should have been thrown.");
        } catch (TimeoutException e) {
            // Expected
        }
    }

    @Test
    public void testTimeoutWithOneResult() throws Exception {
        final String result1 = "partial";
        final CyclicBarrier barrier = new CyclicBarrier(2);
        final MultiResultSyncPoint<String, Exception> rsp = new MultiResultSyncPoint<>(2);
        Async.go(new Async.ThrowingRunnable() {
            @Override
            public void runOrThrow() throws InterruptedException, BrokenBarrierException {
                barrier.await();
                rsp.signal(result1);
            }
        });
        barrier.await();
        try {
            rsp.waitForResults(100);
            fail("A timeout exception should have been thrown.");
        } catch (TimeoutException e) {
            // Expected
        }
    }

    private static class TestException extends Exception {

        /**
         *
         */
        private static final long serialVersionUID = 1L;

    }
}
