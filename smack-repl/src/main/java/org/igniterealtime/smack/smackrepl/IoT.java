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
package org.igniterealtime.smack.smackrepl;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeoutException;

import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterUtil;
import org.jivesoftware.smack.tcp.XMPPTCPConnection;
import org.jivesoftware.smack.tcp.XMPPTCPConnectionConfiguration;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.iot.IoTDiscoveryIntegrationTest;
import org.jivesoftware.smackx.iot.Thing;
import org.jivesoftware.smackx.iot.data.IoTDataManager;
import org.jivesoftware.smackx.iot.data.ThingMomentaryReadOutRequest;
import org.jivesoftware.smackx.iot.data.ThingMomentaryReadOutResult;
import org.jivesoftware.smackx.iot.data.element.IoTDataField;
import org.jivesoftware.smackx.iot.data.element.IoTDataField.IntField;
import org.jivesoftware.smackx.iot.data.element.IoTFieldsExtension;
import org.jivesoftware.smackx.iot.discovery.AbstractThingStateChangeListener;
import org.jivesoftware.smackx.iot.discovery.IoTDiscoveryManager;
import org.jivesoftware.smackx.iot.discovery.ThingState;
import org.jivesoftware.smackx.iot.provisioning.BecameFriendListener;
import org.jivesoftware.smackx.iot.provisioning.IoTProvisioningManager;

import org.igniterealtime.smack.inttest.util.SimpleResultSyncPoint;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.impl.JidCreate;

public class IoT {

    // A 10 minute timeout.
    private static final long TIMEOUT = 10 * 60 * 1000;

    private interface IotScenario {
        void iotScenario(XMPPTCPConnection dataThingConnection, XMPPTCPConnection readingThingConnection) throws Exception;
    }

    public static void iotScenario(String dataThingJidString, String dataThingPassword, String readingThingJidString,
            String readingThingPassword, IotScenario scenario) throws Exception {
        final EntityBareJid dataThingJid = JidCreate.entityBareFrom(dataThingJidString);
        final EntityBareJid readingThingJid = JidCreate.entityBareFrom(readingThingJidString);

        final XMPPTCPConnectionConfiguration dataThingConnectionConfiguration = XMPPTCPConnectionConfiguration.builder()
                .setUsernameAndPassword(dataThingJid.getLocalpart(), dataThingPassword)
                .setXmppDomain(dataThingJid.asDomainBareJid()).setSecurityMode(SecurityMode.disabled)
                .enableDefaultDebugger().build();
        final XMPPTCPConnectionConfiguration readingThingConnectionConfiguration = XMPPTCPConnectionConfiguration
                .builder().setUsernameAndPassword(readingThingJid.getLocalpart(), readingThingPassword)
                .setXmppDomain(readingThingJid.asDomainBareJid()).setSecurityMode(SecurityMode.disabled)
                .enableDefaultDebugger().build();

        final XMPPTCPConnection dataThingConnection = new XMPPTCPConnection(dataThingConnectionConfiguration);
        final XMPPTCPConnection readingThingConnection = new XMPPTCPConnection(readingThingConnectionConfiguration);

        dataThingConnection.setReplyTimeout(TIMEOUT);
        readingThingConnection.setReplyTimeout(TIMEOUT);

        dataThingConnection.setUseStreamManagement(false);
        readingThingConnection.setUseStreamManagement(false);

        try {
            dataThingConnection.connect().login();
            readingThingConnection.connect().login();
            scenario.iotScenario(dataThingConnection, readingThingConnection);
        } finally {
            dataThingConnection.disconnect();
            readingThingConnection.disconnect();
        }
    }

    public static void iotReadOutScenario(String dataThingJidString, String dataThingPassword, String readingThingJidString,
                    String readingThingPassword)
                    throws Exception {
        iotScenario(dataThingJidString, dataThingPassword, readingThingJidString, readingThingPassword, READ_OUT_SCENARIO);
    }

    public static final IotScenario READ_OUT_SCENARIO = new IotScenario() {
        @Override
        public void iotScenario(XMPPTCPConnection dataThingConnection, XMPPTCPConnection readingThingConnection) throws TimeoutException, Exception {
            ThingState dataThingState = actAsDataThing(dataThingConnection);

            final SimpleResultSyncPoint syncPoint = new SimpleResultSyncPoint();
            dataThingState.setThingStateChangeListener(new AbstractThingStateChangeListener() {
                @Override
                public void owned(BareJid jid) {
                    syncPoint.signal();
                }
            });
            // Wait until the thing is owned.
            syncPoint.waitForResult(TIMEOUT);
            printStatus("OWNED - Thing now owned by " + dataThingState.getOwner());

            // Make sure things are befriended.
            IoTProvisioningManager readingThingProvisioningManager = IoTProvisioningManager.getInstanceFor(readingThingConnection);
            readingThingProvisioningManager.sendFriendshipRequestIfRequired(dataThingConnection.getUser().asBareJid());

            Roster dataThingRoster = Roster.getInstanceFor(dataThingConnection);
            RosterUtil.waitUntilOtherEntityIsSubscribed(dataThingRoster, readingThingConnection.getUser().asBareJid(), TIMEOUT);
            printStatus("FRIENDSHIP ACCEPTED - Trying to read out data");

            IoTDataManager readingThingDataManager = IoTDataManager.getInstanceFor(readingThingConnection);
            List<IoTFieldsExtension> values = readingThingDataManager.requestMomentaryValuesReadOut(dataThingConnection.getUser());
            if (values.size() != 1) {
                throw new IllegalStateException("Unexpected number of values returned: " + values.size());
            }
            IoTFieldsExtension field = values.get(0);
            printStatus("DATA READ-OUT SUCCESS: " + field.toXML(null));
            printStatus("IoT SCENARIO FINISHED SUCCESSFULLY");
        }
    };

    public static void iotOwnerApprovesFriendScenario(String dataThingJidString, String dataThingPassword,
            String readingThingJidString, String readingThingPassword) throws Exception {
        iotScenario(dataThingJidString, dataThingPassword, readingThingJidString, readingThingPassword,
                OWNER_APPROVES_FRIEND_SCENARIO);
    }

    public static final IotScenario OWNER_APPROVES_FRIEND_SCENARIO = new IotScenario() {
        @Override
        public void iotScenario(XMPPTCPConnection dataThingConnection, XMPPTCPConnection readingThingConnection) throws TimeoutException, Exception {
            // First ensure that the two XMPP entities are not already subscribed to each other presences.
            RosterUtil.ensureNotSubscribedToEachOther(dataThingConnection, readingThingConnection);

            final BareJid dataThingBareJid = dataThingConnection.getUser().asBareJid();
            final BareJid readingThingBareJid = readingThingConnection.getUser().asBareJid();
            final ThingState dataThingState = actAsDataThing(dataThingConnection);

            printStatus("WAITING for 'claimed' notification. Please claim thing now");
            final SimpleResultSyncPoint syncPoint = new SimpleResultSyncPoint();
            dataThingState.setThingStateChangeListener(new AbstractThingStateChangeListener() {
                @Override
                public void owned(BareJid jid) {
                    syncPoint.signal();
                }
            });
            // Wait until the thing is owned.
            syncPoint.waitForResult(TIMEOUT);
            printStatus("OWNED - Thing now owned by " + dataThingState.getOwner());

            // Now, ReadingThing sends a friendship request to data thing, which
            // will proxy the request to its provisioning service, which will
            // likely return that both a not friends since the owner did not
            // authorize the friendship yet.
            final SimpleResultSyncPoint friendshipApprovedSyncPoint = new SimpleResultSyncPoint();
            final IoTProvisioningManager readingThingProvisioningManager = IoTProvisioningManager.getInstanceFor(readingThingConnection);
            final BecameFriendListener becameFriendListener = new BecameFriendListener() {
                @Override
                public void becameFriend(BareJid jid, Presence presence) {
                    if (jid.equals(dataThingBareJid)) {
                        friendshipApprovedSyncPoint.signal();
                    }
                }
            };
            readingThingProvisioningManager.addBecameFriendListener(becameFriendListener);

            try {
                readingThingProvisioningManager
                        .sendFriendshipRequestIfRequired(dataThingConnection.getUser().asBareJid());
                friendshipApprovedSyncPoint.waitForResult(TIMEOUT);
            } finally {
                readingThingProvisioningManager.removeBecameFriendListener(becameFriendListener);
            }

            printStatus("FRIENDSHIP APPROVED - ReadingThing " + readingThingBareJid + " is now a friend of DataThing " + dataThingBareJid);
        }
    };

    private static ThingState actAsDataThing(XMPPTCPConnection connection) throws XMPPException, SmackException, InterruptedException {
        final String key = StringUtils.randomString(12);
        final String sn = StringUtils.randomString(12);
        Thing dataThing = Thing.builder()
                        .setKey(key)
                        .setSerialNumber(sn)
                        .setManufacturer("IgniteRealtime")
                        .setModel("Smack")
                        .setVersion("0.1")
                        .setMomentaryReadOutRequestHandler(new ThingMomentaryReadOutRequest() {
            @Override
            public void momentaryReadOutRequest(ThingMomentaryReadOutResult callback) {
                IoTDataField.IntField field = new IntField("timestamp", (int) (System.currentTimeMillis() / 1000));
                callback.momentaryReadOut(Collections.singletonList(field));
            }
        })
                        .build();
        IoTDiscoveryManager iotDiscoveryManager = IoTDiscoveryManager.getInstanceFor(connection);
        ThingState state = IoTDiscoveryIntegrationTest.registerThing(iotDiscoveryManager, dataThing);
        printStatus("SUCCESS: Thing registered:" + dataThing);
        return state;
    }

    private static void printStatus(CharSequence status) {
        // CHECKSTYLE:OFF
        System.out.println(status);
        // CHECKSTYLE:ON
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 4) {
            throw new IllegalArgumentException();
        }
        iotOwnerApprovesFriendScenario(args[0], args[1], args[2], args[3]);
    }

}
