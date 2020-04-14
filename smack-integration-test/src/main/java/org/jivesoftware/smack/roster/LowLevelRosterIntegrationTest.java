/**
 *
 * Copyright 2016-2020 Florian Schmaus
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

import java.util.concurrent.TimeoutException;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.packet.Presence;

import org.igniterealtime.smack.inttest.AbstractSmackLowLevelIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.util.IntegrationTestRosterUtil;
import org.igniterealtime.smack.inttest.util.SimpleResultSyncPoint;
import org.jxmpp.jid.FullJid;

public class LowLevelRosterIntegrationTest extends AbstractSmackLowLevelIntegrationTest {

    public LowLevelRosterIntegrationTest(SmackIntegrationTestEnvironment environment) {
        super(environment);
    }

    @SmackIntegrationTest
    public void testPresenceEventListenersOffline(final AbstractXMPPConnection conOne,
            final AbstractXMPPConnection conTwo) throws TimeoutException, Exception {
        IntegrationTestRosterUtil.ensureBothAccountsAreNotInEachOthersRoster(conOne, conTwo);

        final Roster rosterOne = Roster.getInstanceFor(conOne);
        final Roster rosterTwo = Roster.getInstanceFor(conTwo);

        rosterOne.createItem(conTwo.getUser().asBareJid(), "Con Two", null);
        rosterTwo.createItem(conOne.getUser().asBareJid(), "Con One", null);

        IntegrationTestRosterUtil.ensureBothAccountsAreSubscribedToEachOther(conOne, conTwo, timeout);

        final SimpleResultSyncPoint offlineTriggered = new SimpleResultSyncPoint();

        rosterOne.addPresenceEventListener(new AbstractPresenceEventListener() {
            @Override
            public void presenceUnavailable(FullJid jid, Presence presence) {
                if (!jid.equals(conTwo.getUser())) {
                    return;
                }
                offlineTriggered.signal();
            }
        });

        // Disconnect conTwo, this should cause an 'unavailable' presence to be send from conTwo to
        // conOne.
        conTwo.disconnect();

        Boolean result = offlineTriggered.waitForResult(timeout);
        if (!result) {
            throw new Exception("presenceUnavailable() was not called");
        }
    }

}
