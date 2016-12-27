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
package org.igniterealtime.smack.inttest.util;

import java.util.concurrent.TimeoutException;

import org.jivesoftware.smack.util.Objects;

public class ResultSyncPoint<R, E extends Exception> {

    private R result;
    private E exception;

    public R waitForResult(long timeout) throws E, InterruptedException, TimeoutException {
        synchronized(this) {
            if (result != null) {
                return result;
            }
            if (exception != null) {
                throw exception;
            }
            final long deadline = System.currentTimeMillis() + timeout;
            while (result == null && exception == null) {
                final long now = System.currentTimeMillis();
                if (now >= deadline) break;
                wait(deadline - now);
            }
        }
        if (result != null) {
            return result;
        }
        if (exception != null) {
            throw exception;
        }
        throw new TimeoutException("Timeout expired");
    }


    public void signal(R result) {
        synchronized(this) {
            this.result = Objects.requireNonNull(result);
            notifyAll();
        }
    }

    public void signal(E exception) {
        synchronized(this) {
            this.exception = Objects.requireNonNull(exception);
            notifyAll();
        }
    }
}
