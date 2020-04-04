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
package org.jivesoftware.smack.roster;

import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.concurrent.TimeoutException;

import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.roster.packet.RosterPacket.ItemType;
import org.jivesoftware.smack.util.StringUtils;

import org.igniterealtime.smack.inttest.AbstractSmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.util.IntegrationTestRosterUtil;
import org.igniterealtime.smack.inttest.util.SimpleResultSyncPoint;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.Jid;

public class RosterIntegrationTest extends AbstractSmackIntegrationTest {

    private final Roster rosterOne;
    private final Roster rosterTwo;

    public RosterIntegrationTest(SmackIntegrationTestEnvironment environment) {
        super(environment);
        rosterOne = Roster.getInstanceFor(conOne);
        rosterTwo = Roster.getInstanceFor(conTwo);
    }

    @SmackIntegrationTest
    public void subscribeRequestListenerTest() throws TimeoutException, Exception {
        IntegrationTestRosterUtil.ensureBothAccountsAreNotInEachOthersRoster(conOne, conTwo);

        final SubscribeListener subscribeListener = new SubscribeListener() {
            @Override
            public SubscribeAnswer processSubscribe(Jid from, Presence subscribeRequest) {
                if (from.equals(conOne.getUser().asBareJid())) {
                    return SubscribeAnswer.Approve;
                }
                return SubscribeAnswer.Deny;
            }
        };
        rosterTwo.addSubscribeListener(subscribeListener);

        final String conTwosRosterName = "ConTwo " + testRunId;
        final SimpleResultSyncPoint addedAndSubscribed = new SimpleResultSyncPoint();
        rosterOne.addRosterListener(new AbstractRosterListener() {
            @Override
            public void entriesAdded(Collection<Jid> addresses) {
                checkIfAddedAndSubscribed(addresses);
            }
            @Override
            public void entriesUpdated(Collection<Jid> addresses) {
                checkIfAddedAndSubscribed(addresses);
            }
            private void checkIfAddedAndSubscribed(Collection<Jid> addresses) {
                for (Jid jid : addresses) {
                    if (!jid.equals(conTwo.getUser().asBareJid())) {
                        continue;
                    }
                    BareJid bareJid = conTwo.getUser().asBareJid();
                    RosterEntry rosterEntry = rosterOne.getEntry(bareJid);
                    if (rosterEntry == null) {
                        addedAndSubscribed.signalFailure("No roster entry for " + bareJid);
                        return;
                    }
                    String name = rosterEntry.getName();
                    if (StringUtils.isNullOrEmpty(name)) {
                        addedAndSubscribed.signalFailure("Roster entry without name");
                        return;
                    }
                    if (!rosterEntry.getName().equals(conTwosRosterName)) {
                        addedAndSubscribed.signalFailure("Roster name does not match");
                        return;
                    }
                    if (!rosterEntry.getType().equals(ItemType.to)) {
                        return;
                    }
                    addedAndSubscribed.signal();
                }
            }
        });

        try {
            rosterOne.createItemAndRequestSubscription(conTwo.getUser().asBareJid(), conTwosRosterName, null);

            assertTrue(addedAndSubscribed.waitForResult(2 * connection.getReplyTimeout()));
        }
        finally {
            rosterTwo.removeSubscribeListener(subscribeListener);
        }
    }

}
