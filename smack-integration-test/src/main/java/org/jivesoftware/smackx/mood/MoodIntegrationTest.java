/**
 *
 * Copyright 2018 Paul Schaub, 2019-2020 Florian Schmaus.
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
package org.jivesoftware.smackx.mood;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;

import org.igniterealtime.smack.inttest.AbstractSmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.util.IntegrationTestRosterUtil;
import org.igniterealtime.smack.inttest.util.SimpleResultSyncPoint;
import org.junit.AfterClass;

public class MoodIntegrationTest extends AbstractSmackIntegrationTest {

    private final MoodManager mm1;
    private final MoodManager mm2;

    public MoodIntegrationTest(SmackIntegrationTestEnvironment environment) {
        super(environment);
        mm1 = MoodManager.getInstanceFor(conOne);
        mm2 = MoodManager.getInstanceFor(conTwo);
    }

    @SmackIntegrationTest
    public void test() throws Exception {
        IntegrationTestRosterUtil.ensureBothAccountsAreSubscribedToEachOther(conOne, conTwo, timeout);

        final SimpleResultSyncPoint moodReceived = new SimpleResultSyncPoint();

        final MoodListener moodListener = (jid, message, moodElement) -> {
            if (moodElement.getMood() == Mood.satisfied) {
                moodReceived.signal();
            }
        };
        mm2.addMoodListener(moodListener);

        try {
            mm1.setMood(Mood.satisfied);

            moodReceived.waitForResult(timeout);
        } finally {
            mm2.removeMoodListener(moodListener);
        }
    }

    @AfterClass
    public void unsubscribe()
            throws SmackException.NotLoggedInException, XMPPException.XMPPErrorException,
            SmackException.NotConnectedException, InterruptedException, SmackException.NoResponseException {
        IntegrationTestRosterUtil.ensureBothAccountsAreNotInEachOthersRoster(conOne, conTwo);
    }
}
