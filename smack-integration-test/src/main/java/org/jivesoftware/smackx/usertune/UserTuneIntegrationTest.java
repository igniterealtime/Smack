/**
 *
 * Copyright 2019 Aditya Borikar.
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
package org.jivesoftware.smackx.usertune;

import java.net.URI;
import java.util.concurrent.TimeoutException;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NotLoggedInException;
import org.jivesoftware.smack.XMPPException;

import org.jivesoftware.smackx.disco.EntityCapabilitiesChangedListener;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.pep.PepEventListener;
import org.jivesoftware.smackx.usertune.element.UserTuneElement;

import org.igniterealtime.smack.inttest.AbstractSmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.annotations.AfterClass;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.util.IntegrationTestRosterUtil;
import org.igniterealtime.smack.inttest.util.SimpleResultSyncPoint;
import org.junit.jupiter.api.Assertions;

public class UserTuneIntegrationTest extends AbstractSmackIntegrationTest {

    private final UserTuneManager utm1;
    private final UserTuneManager utm2;

    public UserTuneIntegrationTest(SmackIntegrationTestEnvironment environment) throws NotLoggedInException {
        super(environment);
        utm1 = UserTuneManager.getInstanceFor(conOne);
        utm2 = UserTuneManager.getInstanceFor(conTwo);
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
        URI uri = new URI("http://www.yesworld.com/lyrics/Fragile.html#9");
        UserTuneElement.Builder builder = UserTuneElement.getBuilder();
        UserTuneElement data = builder.setArtist("Yes")
                .setLength(686)
                .setRating(8)
                .setSource("Yessongs")
                .setTitle("Heart of the Sunrise")
                .setTrack("3")
                .setUri(uri)
                .build();

        IntegrationTestRosterUtil.ensureBothAccountsAreSubscribedToEachOther(conOne, conTwo, timeout);

        final SimpleResultSyncPoint userTuneReceived = new SimpleResultSyncPoint();

        final PepEventListener<UserTuneElement> userTuneListener = (jid, userTune, id, message) -> {
            if (userTune.equals(data)) {
                userTuneReceived.signal();
            }
        };

        try {
            // Register ConTwo's interest in receiving user tune notifications, and wait for that interest to have been propagated.
            registerListenerAndWait(utm2, ServiceDiscoveryManager.getInstanceFor(conTwo), userTuneListener);

            // Publish the data.
            utm1.publishUserTune(data); // for the purpose of this test, this needs not be blocking/use publishAndWait();

            // Wait for the data to be received.
            Object result = userTuneReceived.waitForResult(timeout);

            // Explicitly assert the success case.
            Assertions.assertNotNull(result, "Expected to receive a PEP notification, but did not.");
        } finally {
            unregisterListener(utm2, userTuneListener);
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
        URI uri = new URI("http://www.yesworld.com/lyrics/Fragile.html#8");
        UserTuneElement.Builder builder = UserTuneElement.getBuilder();
        UserTuneElement data = builder.setArtist("No")
                .setLength(306)
                .setRating(3)
                .setSource("NoSongs")
                .setTitle("Sunrise of the Heart")
                .setTrack("2")
                .setUri(uri)
                .build();

        IntegrationTestRosterUtil.ensureBothAccountsAreSubscribedToEachOther(conOne, conTwo, timeout);

        final SimpleResultSyncPoint userTuneReceived = new SimpleResultSyncPoint();

        final PepEventListener<UserTuneElement> userTuneListener = (jid, userTune, id, message) -> {
            if (userTune.equals(data)) {
                userTuneReceived.signal();
            }
        };

        // TODO Ensure that pre-existing filtering notification excludes userTune.
        try {
            // Publish the data
            publishAndWait(utm1, ServiceDiscoveryManager.getInstanceFor(conOne), data);

            // Adds listener, which implicitly publishes a disco/info filter for userTune notification.
            registerListenerAndWait(utm2, ServiceDiscoveryManager.getInstanceFor(conTwo), userTuneListener);

            // Wait for the data to be received.
            try {
                Object result = userTuneReceived.waitForResult(timeout);

                // Explicitly assert the success case.
                Assertions.assertNotNull(result, "Expected to receive a PEP notification, but did not.");
            } catch (TimeoutException e) {
                Assertions.fail("Expected to receive a PEP notification, but did not.");
            }
        } finally {
            unregisterListener(utm2, userTuneListener);
        }
    }

    /**
     * Registers a listener for User Tune data. This implicitly publishes a CAPS update to include a notification
     * filter for the usertune node. This method blocks until the server has indicated that this update has been
     * received.
     *
     * @param userTuneManager The UserTuneManager instance for the connection that is expected to receive data.
     * @param discoManager The ServiceDiscoveryManager instance for the connection that is expected to publish data.
     * @param listener A listener instance for UserTune data that is to be registered.
     *
     * @throws Exception if the test fails
     */
    public void registerListenerAndWait(UserTuneManager userTuneManager, ServiceDiscoveryManager discoManager, PepEventListener<UserTuneElement> listener) throws Exception {
        final SimpleResultSyncPoint notificationFilterReceived = new SimpleResultSyncPoint();
        final EntityCapabilitiesChangedListener notificationFilterReceivedListener = info -> {
            if (info.containsFeature(UserTuneManager.USERTUNE_NODE + "+notify")) {
                notificationFilterReceived.signal();
            }
        };

        discoManager.addEntityCapabilitiesChangedListener(notificationFilterReceivedListener);
        try {
            userTuneManager.addUserTuneListener(listener);
            notificationFilterReceived.waitForResult(timeout);
        } finally {
            discoManager.removeEntityCapabilitiesChangedListener(notificationFilterReceivedListener);
        }
    }

    /**
     * The functionally reverse of {@link #registerListenerAndWait(UserTuneManager, ServiceDiscoveryManager, PepEventListener)}
     * with the difference of not being a blocking operation.
     *
     * @param userTuneManager The UserTuneManager instance for the connection that was expected to receive data.
     * @param listener A listener instance for UserTune data that is to be removed.
     */
    public void unregisterListener(UserTuneManager userTuneManager, PepEventListener<UserTuneElement> listener) {
        // Does it make sense to have a method implementation that's one line? This is provided to allow for symmetry in the API.
        userTuneManager.removeUserTuneListener(listener);
    }

    /**
     * Publish data using PEP, and block until the server has echoed the publication back to the publishing user.
     *
     * @param userTuneManager The UserTuneManager instance for the connection that is expected to publish data.
     * @param discoManager The ServiceDiscoveryManager instance for the connection that is expected to publish data.
     * @param data The data to be published.
     *
     * @throws Exception if the test fails
     */
    public void publishAndWait(UserTuneManager userTuneManager, ServiceDiscoveryManager discoManager, UserTuneElement data) throws Exception {
        final SimpleResultSyncPoint publicationEchoReceived = new SimpleResultSyncPoint();
        final PepEventListener<UserTuneElement> publicationEchoListener = (jid, userTune, id, message) -> {
            if (userTune.equals(data)) {
                publicationEchoReceived.signal();
            }
        };
        try {
            registerListenerAndWait(userTuneManager, discoManager, publicationEchoListener);
            userTuneManager.addUserTuneListener(publicationEchoListener);
            userTuneManager.publishUserTune(data);
        } finally {
            userTuneManager.removeUserTuneListener(publicationEchoListener);
        }
    }
}
