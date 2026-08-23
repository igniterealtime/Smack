/*
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

import org.jivesoftware.smack.SmackException.NotLoggedInException;

import org.jivesoftware.smackx.pep.AbstractPepIntegrationTest;
import org.jivesoftware.smackx.pep.PepEventListener;
import org.jivesoftware.smackx.usertune.element.UserTuneElement;

import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.annotations.SpecificationReference;
import org.igniterealtime.smack.inttest.util.IntegrationTestRosterUtil;
import org.igniterealtime.smack.inttest.util.SimpleResultSyncPoint;

import org.junit.jupiter.api.Assertions;

@SpecificationReference(document = "XEP-0118", version = "1.3.0")
public class UserTuneIntegrationTest extends AbstractPepIntegrationTest {

    private final UserTuneManager utm1;
    private final UserTuneManager utm2;

    public UserTuneIntegrationTest(SmackIntegrationTestEnvironment environment) throws NotLoggedInException {
        super(environment);
        utm1 = UserTuneManager.getInstanceFor(conOne);
        utm2 = UserTuneManager.getInstanceFor(conTwo);
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
            registerListenerAndWait(utm2::addUserTuneListener, userTuneListener);

            // Publish the data.
            utm1.publishUserTune(data); // for the purpose of this test, this needs not be blocking/use publishAndWait();

            // Wait for the data to be received.
            Object result = userTuneReceived.waitForResult(timeout);

            // Explicitly assert the success case.
            Assertions.assertNotNull(result, "Expected to receive a PEP notification, but did not.");
        } finally {
            utm2.removeUserTuneListener(userTuneListener);
            IntegrationTestRosterUtil.ensureBothAccountsAreNotInEachOthersRoster(conOne, conTwo);
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
            publishAndWait(utm1::addUserTuneListener, utm1::removeUserTuneListener, () -> utm1.publishUserTune(data), userTune -> userTune.equals(data));

            // Adds listener, which implicitly publishes a disco/info filter for userTune notification.
            registerListenerAndWait(utm2::addUserTuneListener, userTuneListener);

            // Wait for the data to be received.
            assertResult(userTuneReceived, "Expected " + conTwo.getUser() + " to receive a PEP notification from " + conOne.getUser() + ", but did not.");
        } finally {
            utm2.removeUserTuneListener(userTuneListener);
            IntegrationTestRosterUtil.ensureBothAccountsAreNotInEachOthersRoster(conOne, conTwo);
        }
    }
}
