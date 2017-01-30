/**
 *
 * Copyright 2015-2016 Florian Schmaus
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
package org.igniterealtime.smack.inttest;

import java.util.Random;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

import org.jivesoftware.smack.StanzaCollector;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.filter.StanzaFilter;

public abstract class AbstractSmackIntTest {

    protected static final Logger LOGGER = Logger.getLogger(AbstractSmackIntTest.class.getName());

    protected static final Random INSECURE_RANDOM = new Random();

    protected final String testRunId;

    protected final long timeout;

    protected AbstractSmackIntTest(String testRunId, long timeout) {
        this.testRunId = testRunId;
        this.timeout = timeout;
    }

    protected void performActionAndWaitUntilStanzaReceived(Runnable action, XMPPConnection connection, StanzaFilter filter)
                    throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        StanzaCollector.Configuration configuration = StanzaCollector.newConfiguration().setStanzaFilter(
                        filter).setSize(1);
        StanzaCollector collector = connection.createStanzaCollector(configuration);

        try {
            action.run();
            collector.nextResultOrThrow(timeout);
        }
        finally {
            collector.cancel();
        }
    }

    protected void waitUntilTrue(Condition condition) throws TimeoutException, NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        final long deadline = System.currentTimeMillis() + timeout;
        do {
            if (condition.evaluate()) {
                return;
            }
            Thread.yield();
        } while (System.currentTimeMillis() <= deadline);
        throw new TimeoutException("Timeout waiting for condition to become true. Timeout was " + timeout + " ms.");
    }

    protected interface Condition {
        boolean evaluate() throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException;
    }
}
