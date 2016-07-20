/**
 *
 * Copyright 2016 Florian Schmaus
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
package org.jivesoftware.smackx.iot;

import static org.junit.Assert.assertEquals;

import org.igniterealtime.smack.inttest.AbstractSmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.TestNotPossibleException;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.iot.discovery.IoTClaimedException;
import org.jivesoftware.smackx.iot.discovery.IoTDiscoveryManager;
import org.jivesoftware.smackx.iot.discovery.ThingState;
import org.jivesoftware.smackx.iot.discovery.element.IoTClaimed;
import org.jxmpp.jid.Jid;

public class IoTDiscoveryIntegrationTest extends AbstractSmackIntegrationTest {

    private final IoTDiscoveryManager discoveryManagerOne;
    private final IoTDiscoveryManager discoveryManagerTwo;

    public IoTDiscoveryIntegrationTest(SmackIntegrationTestEnvironment environment) throws NoResponseException,
                    XMPPErrorException, NotConnectedException, InterruptedException, TestNotPossibleException {
        super(environment);
        discoveryManagerOne = IoTDiscoveryManager.getInstanceFor(conOne);
        discoveryManagerTwo = IoTDiscoveryManager.getInstanceFor(conTwo);
        checkPrerequisites(conOne);
    }

    @SmackIntegrationTest
    public void registerClaimAndUnregisterThing()
                    throws XMPPErrorException, InterruptedException, SmackException {
        final String key = StringUtils.randomString(12);
        final String sn = StringUtils.randomString(12);
        final Thing thing = Thing.builder().setKey(key).setSerialNumber(sn).setManufacturer("Ignite Realtime").setModel(
                        "Smack").setVersion("0.1").build();

        registerThing(discoveryManagerOne, thing);

        IoTClaimed iotClaimed = discoveryManagerTwo.claimThing(thing.getMetaTags());
        assertEquals(conOne.getUser().asBareJid(), iotClaimed.getJid());

        discoveryManagerTwo.disownThing(iotClaimed.getJid());

        discoveryManagerOne.unregister();
    }

    static void checkPrerequisites(XMPPConnection connection) throws NoResponseException, XMPPErrorException,
                    NotConnectedException, InterruptedException, TestNotPossibleException {
        IoTDiscoveryManager discoveryManager = IoTDiscoveryManager.getInstanceFor(connection);
        Jid registry = discoveryManager.findRegistry();
        if (registry == null) {
            throw new TestNotPossibleException("Could not find IoT Registry");
        }
    }

    public static ThingState registerThing(IoTDiscoveryManager iotDiscoveryManager, Thing thing) throws XMPPErrorException, InterruptedException, SmackException {
        int attempts = 0;
        while (true) {
            try {
                return iotDiscoveryManager.registerThing(thing);
            }
            catch (IoTClaimedException e) {
                iotDiscoveryManager.unregister();
            }
            if (attempts++ > 3) {
                throw new SmackException("Could no register thing");
            }
        }
    }

}
