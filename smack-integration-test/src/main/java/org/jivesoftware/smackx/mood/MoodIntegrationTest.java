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

import java.util.concurrent.TimeoutException;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;

import org.jivesoftware.smackx.disco.EntityCapabilitiesChangedListener;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.mood.element.MoodElement;
import org.jivesoftware.smackx.pep.PepEventListener;

import org.igniterealtime.smack.inttest.AbstractSmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.annotations.AfterClass;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.util.IntegrationTestRosterUtil;
import org.igniterealtime.smack.inttest.util.SimpleResultSyncPoint;
import org.junit.jupiter.api.Assertions;

public class MoodIntegrationTest extends AbstractSmackIntegrationTest {

    private final MoodManager mm1;
    private final MoodManager mm2;

    public MoodIntegrationTest(SmackIntegrationTestEnvironment environment) {
        super(environment);
        mm1 = MoodManager.getInstanceFor(conOne);
        mm2 = MoodManager.getInstanceFor(conTwo);
    }

    @AfterClass
    public void unsubscribe()
            throws SmackException.NotLoggedInException, XMPPException.XMPPErrorException,
            SmackException.NotConnectedException, InterruptedException, SmackException.NoResponseException {
        IntegrationTestRosterUtil.ensureBothAccountsAreNotInEachOthersRoster(conOne, conTwo);
    }

    /**
     * Verifies that a notification is sent when a publication is received, assuming that notification filtering
     * has been adjusted to allow for the notification to be delivered.
     *
     * @throws Exception if the test fails
     */
    @SmackIntegrationTest
    public void testNotification() throws Exception {
        Mood data = Mood.satisfied;

        IntegrationTestRosterUtil.ensureBothAccountsAreSubscribedToEachOther(conOne, conTwo, timeout);

        final SimpleResultSyncPoint moodReceived = new SimpleResultSyncPoint();

        final PepEventListener<MoodElement> moodListener = (jid, moodElement, id, message) -> {
            if (moodElement.getMood().equals(data)) {
                moodReceived.signal();
            }
        };

        try {
            // Register ConTwo's interest in receiving mood notifications, and wait for that interest to have been propagated.
            registerListenerAndWait(mm2, ServiceDiscoveryManager.getInstanceFor(conTwo), moodListener);

            // Publish the data.
            mm1.setMood(data); // for the purpose of this test, this needs not be blocking/use publishAndWait();

            // Wait for the data to be received.
            try {
                moodReceived.waitForResult(timeout);
            } catch (TimeoutException e) {
                Assertions.fail("Expected to receive a PEP notification, but did not.");
            }
        } finally {
            unregisterListener(mm2, moodListener);
        }
    }

    /**
     * Verifies that a notification for a previously sent publication is received as soon as notification filtering
     * has been adjusted to allow for the notification to be delivered.
     *
     * @throws Exception if the test fails
     */
    @SmackIntegrationTest
    public void testNotificationAfterFilterChange() throws Exception {
        Mood data = Mood.cautious;

        IntegrationTestRosterUtil.ensureBothAccountsAreSubscribedToEachOther(conOne, conTwo, timeout);

        final SimpleResultSyncPoint moodReceived = new SimpleResultSyncPoint();

        final PepEventListener<MoodElement> moodListener = (jid, moodElement, id, message) -> {
            if (moodElement.getMood().equals(data)) {
                moodReceived.signal();
            }
        };

        // TODO Ensure that pre-existing filtering notification excludes mood.
        try {
            // Publish the data
            publishAndWait(mm1, ServiceDiscoveryManager.getInstanceFor(conOne), data);

            // Adds listener, which implicitly publishes a disco/info filter for mood notification.
            registerListenerAndWait(mm2, ServiceDiscoveryManager.getInstanceFor(conTwo), moodListener);

            // Wait for the data to be received.
            try {
                Object result = moodReceived.waitForResult(timeout);

                // Explicitly assert the success case.
                Assertions.assertNotNull(result, "Expected to receive a PEP notification, but did not.");
            } catch (TimeoutException e) {
                Assertions.fail("Expected to receive a PEP notification, but did not.");
            }
        } finally {
            unregisterListener(mm2, moodListener);
        }
    }

    /**
     * Registers a listener for User Tune data. This implicitly publishes a CAPS update to include a notification
     * filter for the mood node. This method blocks until the server has indicated that this update has been
     * received.
     *
     * @param moodManager The MoodManager instance for the connection that is expected to receive data.
     * @param discoManager The ServiceDiscoveryManager instance for the connection that is expected to publish data.
     * @param listener A listener instance for Mood data that is to be registered.
     *
     * @throws Exception if the test fails
     */
    public void registerListenerAndWait(MoodManager moodManager, ServiceDiscoveryManager discoManager, PepEventListener<MoodElement> listener) throws Exception {
        final SimpleResultSyncPoint notificationFilterReceived = new SimpleResultSyncPoint();
        final EntityCapabilitiesChangedListener notificationFilterReceivedListener = info -> {
            if (info.containsFeature(MoodManager.MOOD_NODE + "+notify")) {
                notificationFilterReceived.signal();
            }
        };

        discoManager.addEntityCapabilitiesChangedListener(notificationFilterReceivedListener);
        try {
            moodManager.addMoodListener(listener);
            notificationFilterReceived.waitForResult(timeout);
        } finally {
            discoManager.removeEntityCapabilitiesChangedListener(notificationFilterReceivedListener);
        }
    }

    /**
     * The functionally reverse of {@link #registerListenerAndWait(MoodManager, ServiceDiscoveryManager, PepEventListener)}
     * with the difference of not being a blocking operation.
     *
     * @param moodManager The MoodManager instance for the connection that was expected to receive data.
     * @param listener A listener instance for Mood data that is to be removed.
     */
    public void unregisterListener(MoodManager moodManager, PepEventListener<MoodElement> listener) {
        // Does it make sense to have a method implementation that's one line? This is provided to allow for symmetry in the API.
        moodManager.removeMoodListener(listener);
    }

    /**
     * Publish data using PEP, and block until the server has echoed the publication back to the publishing user.
     *
     * @param moodManager The MoodManager instance for the connection that is expected to publish data.
     * @param discoManager The ServiceDiscoveryManager instance for the connection that is expected to publish data.
     * @param data The data to be published.
     *
     * @throws Exception if the test fails
     */
    public void publishAndWait(MoodManager moodManager, ServiceDiscoveryManager discoManager, Mood data) throws Exception {
        final SimpleResultSyncPoint publicationEchoReceived = new SimpleResultSyncPoint();
        final PepEventListener<MoodElement> publicationEchoListener = (jid, moodElement, id, message) -> {
            if (moodElement.getMood().equals(data)) {
                publicationEchoReceived.signal();
            }
        };
        try {
            registerListenerAndWait(moodManager, discoManager, publicationEchoListener);
            moodManager.addMoodListener(publicationEchoListener);
            moodManager.setMood(data);
        } finally {
            moodManager.removeMoodListener(publicationEchoListener);
        }
    }
}
