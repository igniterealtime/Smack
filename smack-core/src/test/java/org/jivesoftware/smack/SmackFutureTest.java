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

import static org.junit.Assert.assertTrue;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.SmackFuture.InternalSmackFuture;
import org.jivesoftware.smack.SmackFuture.SimpleInternalSmackFuture;
import org.jivesoftware.smack.packet.Stanza;
import org.junit.Test;

public class SmackFutureTest {

    @Test
    public void simpleSmackFutureSuccessTest() throws NotConnectedException, InterruptedException, ExecutionException {
        InternalSmackFuture<Boolean> future = new SimpleInternalSmackFuture<Boolean>() {
            @Override
            protected void handleStanza(Stanza stanza) throws NotConnectedException, InterruptedException {
                setResult(true);
            }
        };

        future.processStanza(null);

        assertTrue(future.get());
    }

    @Test(expected = TimeoutException.class)
    public void simpleSmackFutureTimeoutTest() throws InterruptedException, ExecutionException, TimeoutException {
        InternalSmackFuture<Boolean> future = new SimpleInternalSmackFuture<Boolean>() {
            @Override
            protected void handleStanza(Stanza stanza) throws NotConnectedException, InterruptedException {
            }
        };

        future.get(5, TimeUnit.SECONDS);
    }
}
