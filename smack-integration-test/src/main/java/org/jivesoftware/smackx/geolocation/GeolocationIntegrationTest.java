/**
 *
 * Copyright 2020 Aditya Borikar.
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
package org.jivesoftware.smackx.geolocation;

import java.net.URI;
import java.util.concurrent.TimeoutException;

import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.SmackException.NotLoggedInException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;

import org.jivesoftware.smackx.disco.EntityCapabilitiesChangedListener;
import org.jivesoftware.smackx.disco.ServiceDiscoveryManager;
import org.jivesoftware.smackx.geoloc.GeoLocationManager;
import org.jivesoftware.smackx.geoloc.packet.GeoLocation;
import org.jivesoftware.smackx.pep.PepEventListener;

import org.igniterealtime.smack.inttest.AbstractSmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.annotations.AfterClass;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.util.IntegrationTestRosterUtil;
import org.igniterealtime.smack.inttest.util.SimpleResultSyncPoint;
import org.junit.jupiter.api.Assertions;
import org.jxmpp.util.XmppDateTime;

public class GeolocationIntegrationTest extends AbstractSmackIntegrationTest {

    private final GeoLocationManager glm1;
    private final GeoLocationManager glm2;

    public GeolocationIntegrationTest(SmackIntegrationTestEnvironment environment) {
        super(environment);
        glm1 = GeoLocationManager.getInstanceFor(conOne);
        glm2 = GeoLocationManager.getInstanceFor(conTwo);
    }

    @AfterClass
    public void unsubscribe() throws NotLoggedInException, NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
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
        GeoLocation.Builder builder = GeoLocation.builder();
        GeoLocation data = builder.setAccuracy(23d)
                                            .setAlt(1000d)
                                            .setAltAccuracy(10d)
                                            .setArea("Delhi")
                                            .setBearing(10d)
                                            .setBuilding("Small Building")
                                            .setCountry("India")
                                            .setCountryCode("IN")
                                            .setDescription("My Description")
                                            .setFloor("top")
                                            .setLat(25.098345d)
                                            .setLocality("awesome")
                                            .setLon(77.992034)
                                            .setPostalcode("110085")
                                            .setRegion("North")
                                            .setRoom("small")
                                            .setSpeed(250.0d)
                                            .setStreet("Wall Street")
                                            .setText("Unit Testing GeoLocation")
                                            .setTimestamp(XmppDateTime.parseDate("2004-02-19"))
                                            .setTzo("+5:30")
                                            .setUri(new URI("http://xmpp.org"))
                                            .build();

        IntegrationTestRosterUtil.ensureBothAccountsAreSubscribedToEachOther(conOne, conTwo, timeout);

        final SimpleResultSyncPoint geoLocationReceived = new SimpleResultSyncPoint();

        final PepEventListener<GeoLocation> geoLocationListener = (jid, geoLocation, id, message) -> {
            if (geoLocation.equals(data)) {
                geoLocationReceived.signal();
            }
        };

        try {
            // Register ConTwo's interest in receiving geolocation notifications, and wait for that interest to have been propagated.
            registerListenerAndWait(glm2, ServiceDiscoveryManager.getInstanceFor(conTwo), geoLocationListener);

            // Publish the data.
            glm1.publishGeoLocation(data); // for the purpose of this test, this needs not be blocking/use publishAndWait();

            // Wait for the data to be received.
            try {
                Object result = geoLocationReceived.waitForResult(timeout);

                // Explicitly assert the success case.
                Assertions.assertNotNull(result, "Expected to receive a PEP notification, but did not.");
            } catch (TimeoutException e) {
                Assertions.fail("Expected to receive a PEP notification, but did not.");
            }
        } finally {
            unregisterListener(glm2, geoLocationListener);
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
        GeoLocation.Builder builder = GeoLocation.builder();
        GeoLocation data = builder.setAccuracy(12d)
                .setAlt(999d)
                .setAltAccuracy(9d)
                .setArea("Amsterdam")
                .setBearing(9d)
                .setBuilding("Test Building")
                .setCountry("Netherlands")
                .setCountryCode("NL")
                .setDescription("My Description")
                .setFloor("middle")
                .setLat(25.098345d)
                .setLocality("brilliant")
                .setLon(77.992034)
                .setPostalcode("110085")
                .setRegion("North")
                .setRoom("small")
                .setSpeed(250.0d)
                .setStreet("Wall Street")
                .setText("Unit Testing GeoLocation 2")
                .setTimestamp(XmppDateTime.parseDate("2007-02-19"))
                .setTzo("+5:30")
                .setUri(new URI("http://xmpp.org"))
                .build();

        IntegrationTestRosterUtil.ensureBothAccountsAreSubscribedToEachOther(conOne, conTwo, timeout);

        final SimpleResultSyncPoint geoLocationReceived = new SimpleResultSyncPoint();

        final PepEventListener<GeoLocation> geoLocationListener = (jid, geoLocation, id, message) -> {
            if (geoLocation.equals(data)) {
                geoLocationReceived.signal();
            }
        };

        // TODO Ensure that pre-existing filtering notification excludes geolocation.
        try {
            // Publish the data
            publishAndWait(glm1, ServiceDiscoveryManager.getInstanceFor(conOne), data);

            // Adds listener, which implicitly publishes a disco/info filter for geolocation notification.
            registerListenerAndWait(glm2, ServiceDiscoveryManager.getInstanceFor(conTwo), geoLocationListener);

            // Wait for the data to be received.
            try {
                Object result = geoLocationReceived.waitForResult(timeout);

                // Explicitly assert the success case.
                Assertions.assertNotNull(result, "Expected to receive a PEP notification, but did not.");
            } catch (TimeoutException e) {
                Assertions.fail("Expected to receive a PEP notification, but did not.");
            }
        } finally {
            unregisterListener(glm2, geoLocationListener);
        }
    }

    /**
     * Registers a listener for GeoLocation data. This implicitly publishes a CAPS update to include a notification
     * filter for the geolocation node. This method blocks until the server has indicated that this update has been
     * received.
     *
     * @param geoManager The GeoLocationManager instance for the connection that is expected to receive data.
     * @param discoManager The ServiceDiscoveryManager instance for the connection that is expected to publish data.
     * @param listener A listener instance for GeoLocation data that is to be registered.
     *
     * @throws Exception if the test fails
     */
    public void registerListenerAndWait(GeoLocationManager geoManager, ServiceDiscoveryManager discoManager, PepEventListener<GeoLocation> listener) throws Exception {
        final SimpleResultSyncPoint notificationFilterReceived = new SimpleResultSyncPoint();
        final EntityCapabilitiesChangedListener notificationFilterReceivedListener = info -> {
            if (info.containsFeature(GeoLocationManager.GEOLOCATION_NODE + "+notify")) {
                notificationFilterReceived.signal();
            }
        };

        discoManager.addEntityCapabilitiesChangedListener(notificationFilterReceivedListener);
        try {
            geoManager.addGeoLocationListener(listener);
            notificationFilterReceived.waitForResult(timeout);
        } finally {
            discoManager.removeEntityCapabilitiesChangedListener(notificationFilterReceivedListener);
        }
    }

    /**
     * The functionally reverse of {@link #registerListenerAndWait(GeoLocationManager, ServiceDiscoveryManager, PepEventListener)}
     * with the difference of not being a blocking operation.
     *
     * @param geoManager The GeoLocationManager instance for the connection that was expected to receive data.
     * @param listener A listener instance for GeoLocation data that is to be removed.
     */
    public void unregisterListener(GeoLocationManager geoManager, PepEventListener<GeoLocation> listener) {
        // Does it make sense to have a method implementation that's one line? This is provided to allow for symmetry in the API.
        geoManager.removeGeoLocationListener(listener);
    }

    /**
     * Publish data using PEP, and block until the server has echoed the publication back to the publishing user.
     *
     * @param geoManager The GeoLocationManager instance for the connection that is expected to publish data.
     * @param discoManager The ServiceDiscoveryManager instance for the connection that is expected to publish data.
     * @param data The data to be published.
     *
     * @throws Exception if the test fails
     */
    public void publishAndWait(GeoLocationManager geoManager, ServiceDiscoveryManager discoManager, GeoLocation data) throws Exception {
        final SimpleResultSyncPoint publicationEchoReceived = new SimpleResultSyncPoint();
        final PepEventListener<GeoLocation> publicationEchoListener = (jid, geoLocation, id, message) -> {
            if (geoLocation.equals(data)) {
                publicationEchoReceived.signal();
            }
        };
        try {
            registerListenerAndWait(geoManager, discoManager, publicationEchoListener);
            geoManager.addGeoLocationListener(publicationEchoListener);
            geoManager.publishGeoLocation(data);
        } finally {
            geoManager.removeGeoLocationListener(publicationEchoListener);
        }
    }
}
