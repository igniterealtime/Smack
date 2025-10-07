/*
 *
 * Copyright 2015-2020 Florian Schmaus
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeoutException;

import org.jivesoftware.smack.util.Async;

import org.junit.jupiter.api.Test;

public class ResultSyncPointTest {

    @Test
    public void testResultSyncPoint() throws Exception {
        final String result = "Hip Hip Hurrary!!111!";
        final CyclicBarrier barrier = new CyclicBarrier(2);
        final ResultSyncPoint<String, Exception> rsp = new ResultSyncPoint<>();
        Async.go(new Async.ThrowingRunnable() {
            @Override
            public void runOrThrow() throws InterruptedException, BrokenBarrierException {
                barrier.await();
                rsp.signal(result);
            }
        });
        barrier.await();
        String receivedResult = rsp.waitForResult(60 * 1000);
        assertEquals(result, receivedResult);
    }

    @Test
    public void exceptionTestResultSyncPoint() throws Exception {
        final CyclicBarrier barrier = new CyclicBarrier(2);
        final ResultSyncPoint<String, TestException> rsp = new ResultSyncPoint<>();
        Async.go(new Async.ThrowingRunnable() {
            @Override
            public void runOrThrow() throws InterruptedException, BrokenBarrierException {
                barrier.await();
                rsp.signal(new TestException());
            }
        });
        barrier.await();
        assertThrows(TestException.class, () -> rsp.waitForResult(60 * 1000));
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

    private static final class TestException extends Exception {

        /**
         *
         */
        private static final long serialVersionUID = 1L;

    }
}
