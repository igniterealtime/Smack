/*
 *
 * Copyright 2021-2024 Guus der Kinderen
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

import java.util.ArrayList;
import java.util.List;

import org.jivesoftware.smack.util.Objects;

import org.igniterealtime.smack.inttest.util.ResultSyncPoint.ResultSyncPointTimeoutException;

public class MultiResultSyncPoint<R, E extends Exception> {

    private final List<R> results;
    private E exception;
    private final int expectedResultCount;

    public MultiResultSyncPoint(int expectedResultCount) {
        this.expectedResultCount = expectedResultCount;
        this.results = new ArrayList<>(expectedResultCount);
    }

    public synchronized List<R> waitForResults(long timeout) throws E, InterruptedException, ResultSyncPointTimeoutException {
        return waitForResults(timeout, null);
    }

    public synchronized List<R> waitForResults(long timeout, String timeoutMessage) throws E, InterruptedException, ResultSyncPointTimeoutException {
        long now = System.currentTimeMillis();
        final long deadline = now + timeout;
        while (results.size() < expectedResultCount && exception == null && now < deadline) {
            wait(deadline - now);
            now = System.currentTimeMillis();
        }
        if (now >= deadline) {
            StringBuilder sb = new StringBuilder();
            if (timeoutMessage != null) {
                sb.append(timeoutMessage).append(". ");
            }
            sb.append("MultiResultSyncPoint timeout waiting " + timeout + " ms. Got " + results.size() + " results of " + expectedResultCount + " results");

            throw new ResultSyncPointTimeoutException(sb.toString());
        }
        if (exception != null) throw exception;
        return new ArrayList<>(results);
    }

    public synchronized void signal(R result) {
        this.results.add(Objects.requireNonNull(result));
        if (expectedResultCount <= results.size()) {
            notifyAll();
        }
    }

    public synchronized void signal(E exception) {
        this.exception = Objects.requireNonNull(exception);
        notifyAll();
    }
}
