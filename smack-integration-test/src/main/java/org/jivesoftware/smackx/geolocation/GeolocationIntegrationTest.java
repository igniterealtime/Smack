/*
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
import java.text.ParseException;
import java.util.concurrent.TimeoutException;

import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.SmackException.NotLoggedInException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;

import org.jivesoftware.smackx.geoloc.GeoLocationManager;
import org.jivesoftware.smackx.geoloc.packet.GeoLocation;
import org.jivesoftware.smackx.pep.AbstractPepIntegrationTest;
import org.jivesoftware.smackx.pep.PepEventListener;
import org.jivesoftware.smackx.pubsub.PubSubException.NotALeafNodeException;

import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.annotations.SpecificationReference;
import org.igniterealtime.smack.inttest.util.IntegrationTestRosterUtil;
import org.igniterealtime.smack.inttest.util.ResultSyncPoint;

import org.jxmpp.util.XmppDateTime;

@SpecificationReference(document = "XEP-0080", version = "1.9")
public class GeolocationIntegrationTest extends AbstractPepIntegrationTest {

    private final GeoLocationManager glm1;
    private final GeoLocationManager glm2;

    public GeolocationIntegrationTest(SmackIntegrationTestEnvironment environment) {
        super(environment);
        glm1 = GeoLocationManager.getInstanceFor(conOne);
        glm2 = GeoLocationManager.getInstanceFor(conTwo);
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
     * @throws ParseException if date parsing fails.
     */
    @SmackIntegrationTest
    public void testNotification() throws NotLoggedInException, NotALeafNodeException, NoResponseException,
            NotConnectedException, XMPPErrorException, InterruptedException, TimeoutException, ParseException {
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
                                            .setUri(URI.create("http://xmpp.org"))
                                            .build();

        IntegrationTestRosterUtil.ensureBothAccountsAreSubscribedToEachOther(conOne, conTwo, timeout);

        final ResultSyncPoint<GeoLocation, ?> geoLocationReceived = new ResultSyncPoint<>();

        final PepEventListener<GeoLocation> geoLocationListener = (jid, geoLocation, id, message) -> {
            if (geoLocation.equals(data)) {
                geoLocationReceived.signal(geoLocation);
            }
        };

        try {
            // Register ConTwo's interest in receiving geolocation notifications, and wait for that interest to have been propagated.
            registerListenerAndWait(glm2::addGeoLocationListener, geoLocationListener);

            // Publish the data.
            glm1.publishGeoLocation(data); // for the purpose of this test, this needs not be blocking/use publishAndWait();

            // Wait for the data to be received.
            assertResult(geoLocationReceived,
        "Expected " + conTwo.getUser() + " to receive a PEP notification from " + conOne.getUser() +
                " that contained '" + data.toXML() + "', but did not.");
        } finally {
            glm2.removeGeoLocationListener(geoLocationListener);
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
     * @throws ParseException if date parsing fails.
     */
    @SmackIntegrationTest
    public void testNotificationAfterFilterChange() throws NotLoggedInException, NotALeafNodeException,
            NoResponseException, NotConnectedException, XMPPErrorException, InterruptedException, TimeoutException, ParseException {
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
                .setUri(URI.create("http://xmpp.org"))
                .build();

        IntegrationTestRosterUtil.ensureBothAccountsAreSubscribedToEachOther(conOne, conTwo, timeout);

        final ResultSyncPoint<GeoLocation, ?> geoLocationReceived = new ResultSyncPoint<>();

        final PepEventListener<GeoLocation> geoLocationListener = (jid, geoLocation, id, message) -> {
            if (geoLocation.equals(data)) {
                geoLocationReceived.signal(geoLocation);
            }
        };

        // TODO Ensure that pre-existing filtering notification excludes geolocation.
        try {
            // Publish the data
            publishAndWait(glm1::addGeoLocationListener, glm1::removeGeoLocationListener, () -> glm1.publishGeoLocation(data), geoLocation -> geoLocation.equals(data));

            // Adds listener, which implicitly publishes a disco/info filter for geolocation notification.
            registerListenerAndWait(glm2::addGeoLocationListener, geoLocationListener);

            // Wait for the data to be received.
            assertResult(geoLocationReceived,
        "Expected " + conTwo.getUser() + " to receive a PEP notification from " + conOne.getUser() +
                " that contained '" + data.toXML() + "', but did not.");
        } finally {
            glm2.removeGeoLocationListener(geoLocationListener);
            IntegrationTestRosterUtil.ensureBothAccountsAreNotInEachOthersRoster(conOne, conTwo);
        }
    }
}
