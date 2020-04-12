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
import org.jivesoftware.smack.packet.Message;

import org.jivesoftware.smackx.geoloc.GeoLocationListener;
import org.jivesoftware.smackx.geoloc.GeoLocationManager;
import org.jivesoftware.smackx.geoloc.packet.GeoLocation;

import org.igniterealtime.smack.inttest.AbstractSmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.annotations.AfterClass;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.util.IntegrationTestRosterUtil;
import org.igniterealtime.smack.inttest.util.SimpleResultSyncPoint;
import org.jxmpp.jid.BareJid;
import org.jxmpp.util.XmppDateTime;

public class GeolocationIntegrationTest extends AbstractSmackIntegrationTest {

    private final GeoLocationManager glm1;
    private final GeoLocationManager glm2;

    public GeolocationIntegrationTest(SmackIntegrationTestEnvironment environment) {
        super(environment);
        glm1 = GeoLocationManager.getInstanceFor(conOne);
        glm2 = GeoLocationManager.getInstanceFor(conTwo);
    }

    @SmackIntegrationTest
    public void test() throws TimeoutException, Exception {
        GeoLocation.Builder builder = GeoLocation.builder();
        GeoLocation geoLocation1 = builder.setAccuracy(23d)
                                            .setAlt(1000d)
                                            .setAltAccuracy(10d)
                                            .setArea("Delhi")
                                            .setBearing(10d)
                                            .setBuilding("Small Building")
                                            .setCountry("India")
                                            .setCountryCode("IN")
                                            .setDescription("My Description")
                                            .setError(90d)
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
        final GeoLocationListener geoLocationListener = new GeoLocationListener() {

            @Override
            public void onGeoLocationUpdated(BareJid jid, GeoLocation geoLocation, Message message) {
                if (geoLocation.equals(geoLocation1)) {
                    geoLocationReceived.signal();
                }
            }
        };

        glm2.addGeoLocationListener(geoLocationListener);

        try {
            glm1.sendGeolocation(geoLocation1);
            geoLocationReceived.waitForResult(timeout);
        } finally {
            glm2.removeGeoLocationListener(geoLocationListener);
        }
    }

    @AfterClass
    public void unsubscribe() throws NotLoggedInException, NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        IntegrationTestRosterUtil.ensureBothAccountsAreNotInEachOthersRoster(conOne, conTwo);
    }
}
