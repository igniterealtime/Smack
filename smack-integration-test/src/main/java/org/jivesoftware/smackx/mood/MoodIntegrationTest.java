/*
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

import java.util.concurrent.TimeoutException;

import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.SmackException.NotLoggedInException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;

import org.jivesoftware.smackx.mood.element.MoodElement;
import org.jivesoftware.smackx.pep.AbstractPepIntegrationTest;
import org.jivesoftware.smackx.pep.PepEventListener;
import org.jivesoftware.smackx.pubsub.PubSubException.NotALeafNodeException;

import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.annotations.SpecificationReference;
import org.igniterealtime.smack.inttest.util.IntegrationTestRosterUtil;
import org.igniterealtime.smack.inttest.util.ResultSyncPoint;

@SpecificationReference(document = "XEP-0107", version = "1.2.1")
public class MoodIntegrationTest extends AbstractPepIntegrationTest {

    private final MoodManager mm1;
    private final MoodManager mm2;

    public MoodIntegrationTest(SmackIntegrationTestEnvironment environment) {
        super(environment);
        mm1 = MoodManager.getInstanceFor(conOne);
        mm2 = MoodManager.getInstanceFor(conTwo);
    }

    /**
     * Verifies that a notification is sent when a publication is received, assuming that notification filtering
     * has been adjusted to allow for the notification to be delivered.
     *
     * @throws NotLoggedInException if the connection is not logged in.
     * @throws NotALeafNodeException if the PubSub node is not a leaf node.
     * @throws NoResponseException if there was no response from the remote entity or server.
     * @throws NotConnectedException if the connection is not connected.
     * @throws XMPPErrorException if an XMPP error occurred.
     * @throws InterruptedException if the calling thread was interrupted.
     * @throws TimeoutException if a timeout occurred.
     */
    @SmackIntegrationTest
    public void testNotification() throws NotLoggedInException, NotALeafNodeException, NoResponseException,
            NotConnectedException, XMPPErrorException, InterruptedException, TimeoutException {
        Mood data = Mood.satisfied;

        IntegrationTestRosterUtil.ensureBothAccountsAreSubscribedToEachOther(conOne, conTwo, timeout);

        final ResultSyncPoint<MoodElement, ?> moodReceived = new ResultSyncPoint<>();

        final PepEventListener<MoodElement> moodListener = (jid, moodElement, id, message) -> {
            if (moodElement.getMood().equals(data)) {
                moodReceived.signal(moodElement);
            }
        };

        try {
            // Register ConTwo's interest in receiving mood notifications, and wait for that interest to have been propagated.
            registerListenerAndWait(mm2::addMoodListener, moodListener);

            // Publish the data.
            mm1.setMood(data); // for the purpose of this test, this needs not be blocking/use publishAndWait();

            // Wait for the data to be received.
            assertResult(moodReceived, "Expected " + conTwo.getUser() + " to receive a PEP notification, but did not.");
        } finally {
            mm2.removeMoodListener(moodListener);
            IntegrationTestRosterUtil.ensureBothAccountsAreNotInEachOthersRoster(conOne, conTwo);
        }
    }

    /**
     * Verifies that a notification for a previously sent publication is received as soon as notification filtering
     * has been adjusted to allow for the notification to be delivered.
     *
     * @throws NotLoggedInException if the connection is not logged in.
     * @throws NotALeafNodeException if the PubSub node is not a leaf node.
     * @throws NoResponseException if there was no response from the remote entity or server.
     * @throws NotConnectedException if the connection is not connected.
     * @throws XMPPErrorException if an XMPP error occurred.
     * @throws InterruptedException if the calling thread was interrupted.
     * @throws TimeoutException if a timeout occurred.
     */
    @SmackIntegrationTest
    public void testNotificationAfterFilterChange() throws NotLoggedInException, NotALeafNodeException,
            NoResponseException, NotConnectedException, XMPPErrorException, InterruptedException, TimeoutException {
        Mood data = Mood.cautious;

        IntegrationTestRosterUtil.ensureBothAccountsAreSubscribedToEachOther(conOne, conTwo, timeout);

        final ResultSyncPoint<MoodElement, ?> moodReceived = new ResultSyncPoint<>();

        final PepEventListener<MoodElement> moodListener = (jid, moodElement, id, message) -> {
            if (moodElement.getMood().equals(data)) {
                moodReceived.signal(moodElement);
            }
        };

        // TODO Ensure that pre-existing filtering notification excludes mood.
        try {
            // Publish the data
            publishAndWait(mm1::addMoodListener, mm1::removeMoodListener, () -> mm1.setMood(data), moodElement -> moodElement.getMood().equals(data));

            // Adds listener, which implicitly publishes a disco/info filter for mood notification.
            registerListenerAndWait(mm2::addMoodListener, moodListener);

            // Wait for the data to be received.
            assertResult(moodReceived, "Expected " + conTwo.getUser() + " to receive a PEP notification, but did not.");
        } finally {
            mm2.removeMoodListener(moodListener);
            IntegrationTestRosterUtil.ensureBothAccountsAreNotInEachOthersRoster(conOne, conTwo);
        }
    }
}
