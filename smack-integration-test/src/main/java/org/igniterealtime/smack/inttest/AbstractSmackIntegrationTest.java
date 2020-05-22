/**
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
package org.igniterealtime.smack.inttest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.FromMatchesFilter;
import org.jivesoftware.smack.filter.PresenceTypeFilter;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.util.Async.ThrowingRunnable;

import org.igniterealtime.smack.inttest.util.SimpleResultSyncPoint;

public abstract class AbstractSmackIntegrationTest extends AbstractSmackIntTest {

    /**
     * The first connection.
     */
    protected final XMPPConnection conOne;

    /**
     * The second connection.
     */
    protected final XMPPConnection conTwo;

    /**
     * The third connection.
     */
    protected final XMPPConnection conThree;

    /**
     * An alias for the first connection {@link #conOne}.
     */
    protected final XMPPConnection connection;

    protected final List<XMPPConnection> connections;

    public AbstractSmackIntegrationTest(SmackIntegrationTestEnvironment environment) {
        super(environment);
        this.connection = this.conOne = environment.conOne;
        this.conTwo = environment.conTwo;
        this.conThree = environment.conThree;

        final List<XMPPConnection> connectionsLocal = new ArrayList<>(3);
        connectionsLocal.add(conOne);
        connectionsLocal.add(conTwo);
        connectionsLocal.add(conThree);
        this.connections = Collections.unmodifiableList(connectionsLocal);
    }

    /**
     * Perform action and wait until conA observes a presence form conB.
     * <p>
     * This method is usually used so that 'action' performs an operation that changes one entities
     * features/nodes/capabilities, and we want to check that another connection is able to observe this change, and use
     * that new "thing" that was added to the connection.
     * </p>
     * <p>
     * Note that this method is a workaround at best and not reliable. Because it is not guaranteed that any XEP-0030
     * related manager, e.g. EntityCapsManager, already processed the presence when this method returns.
     * </p>
     * TODO: Come up with a better solution.
     *
     * @param conA the connection to observe the presence on.
     * @param conB the connection sending the presence
     * @param action the action to perform.
     * @throws Exception in case of an exception.
     */
    protected void performActionAndWaitForPresence(XMPPConnection conA, XMPPConnection conB, ThrowingRunnable action)
                    throws Exception {
        final SimpleResultSyncPoint presenceReceivedSyncPoint = new SimpleResultSyncPoint();
        final StanzaListener presenceListener = new StanzaListener() {
            @Override
            public void processStanza(Stanza packet) {
                presenceReceivedSyncPoint.signal();
            }
        };

        // Add a stanzaListener to listen for incoming presence
        conA.addAsyncStanzaListener(presenceListener, new AndFilter(
                        PresenceTypeFilter.AVAILABLE,
                        FromMatchesFilter.create(conB.getUser())
                        ));

        action.runOrThrow();

        try {
            // wait for the dummy feature to get sent via presence
            presenceReceivedSyncPoint.waitForResult(timeout);
        } finally {
            conA.removeAsyncStanzaListener(presenceListener);
        }
    }
}
