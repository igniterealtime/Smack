/**
 *
 * Copyright 2015-2019 Florian Schmaus
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
package org.jivesoftware.smackx.ping;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;

import org.igniterealtime.smack.inttest.AbstractSmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.jxmpp.jid.Jid;

public class PingIntegrationTest extends AbstractSmackIntegrationTest {

    public PingIntegrationTest(SmackIntegrationTestEnvironment<?> environment) {
        super(environment);
    }

    @SmackIntegrationTest
    public void pingServer() throws NotConnectedException, InterruptedException {
        PingManager pingManager = PingManager.getInstanceFor(connection);
        assertTrue(pingManager.pingMyServer());
    }

    private static final class Pinger implements Runnable {
        private final List<Jid> toPing;
        private final Collection<Future<Boolean>> pongFutures;

        private final PingManager pingManager;

        private Pinger(XMPPConnection connection, Collection<Future<Boolean>> pongFutures, Jid... toPing) {
            this(connection, pongFutures, Arrays.asList(toPing));
        }

        private Pinger(XMPPConnection connection, Collection<Future<Boolean>> pongFutures, List<Jid> toPing) {
            this.toPing = toPing;
            this.pongFutures = pongFutures;

            this.pingManager = PingManager.getInstanceFor(connection);
        }

        @Override
        public void run() {
            List<Future<Boolean>> futures = new ArrayList<>();
            for (Jid jid : toPing) {
                Future<Boolean> future = pingManager.pingAsync(jid);
                futures.add(future);
            }
            pongFutures.addAll(futures);
        }
    }

    @SmackIntegrationTest
    public void pingAsync() throws InterruptedException, ExecutionException {
        List<Future<Boolean>> pongFutures = Collections.synchronizedList(new ArrayList<Future<Boolean>>());
        Runnable[] pinger = new Runnable[3];
        pinger[0] = new Pinger(conOne, pongFutures, conTwo.getUser(), conThree.getUser());
        pinger[1] = new Pinger(conTwo, pongFutures, conOne.getUser(), conThree.getUser());
        pinger[2] = new Pinger(conThree, pongFutures, conOne.getUser(), conTwo.getUser());

        ExecutorService executorService = Executors.newFixedThreadPool(pinger.length);
        for (Runnable runnable : pinger) {
            executorService.execute(runnable);
        }

        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.MINUTES);

        for (Future<Boolean> pongFuture : pongFutures) {
            assertTrue(pongFuture.get());
        }
    }
}
