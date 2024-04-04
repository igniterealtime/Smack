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

import org.junit.jupiter.api.Assertions;

public class SimpleResultSyncPoint extends ResultSyncPoint<Void, Exception> {

    public void signal() {
        signal((Void) null);
    }

    public void signalFailure() {
        signalFailure("Unspecified failure");
    }

    public void signalFailure(String failureMessage) {
        signal(new Exception(failureMessage));
    }

    public static void assertSuccess(SimpleResultSyncPoint resultSyncPoint, long timeout, String message) throws InterruptedException {
        try {
            resultSyncPoint.waitForResult(timeout);
        } catch (InterruptedException e) {
            throw e;
        } catch (Exception e) {
            Assertions.fail(message, e);
        }
    }
}
