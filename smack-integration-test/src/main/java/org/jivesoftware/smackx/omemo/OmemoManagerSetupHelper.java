/**
 *
 * Copyright 2017 Paul Schaub
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
package org.jivesoftware.smackx.omemo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.roster.PresenceEventListener;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterEntry;
import org.jivesoftware.smackx.omemo.exceptions.CannotEstablishOmemoSessionException;
import org.jivesoftware.smackx.omemo.exceptions.CorruptedOmemoKeyException;
import org.jivesoftware.smackx.omemo.internal.OmemoCachedDeviceList;
import org.jivesoftware.smackx.omemo.internal.OmemoDevice;
import org.jivesoftware.smackx.omemo.trust.OmemoFingerprint;
import org.jivesoftware.smackx.omemo.util.EphemeralTrustCallback;
import org.jivesoftware.smackx.omemo.util.OmemoConstants;
import org.jivesoftware.smackx.pubsub.PubSubException;
import org.jivesoftware.smackx.pubsub.PubSubManager;

import com.google.common.collect.Maps;
import org.igniterealtime.smack.inttest.util.SimpleResultSyncPoint;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.FullJid;
import org.jxmpp.jid.Jid;


public class OmemoManagerSetupHelper {

    /**
     * Synchronously subscribes presence.
     * @param subscriber connection of user which subscribes.
     * @param target connection of user which gets subscribed.
     * @param targetNick nick of the subscribed user.
     * @param targetGroups groups of the user.
     * @throws Exception
     */
    public static void syncSubscribePresence(final XMPPConnection subscriber,
                                             final XMPPConnection target,
                                             String targetNick,
                                             String[] targetGroups)
            throws Exception {
        final SimpleResultSyncPoint subscribed = new SimpleResultSyncPoint();

        Roster subscriberRoster = Roster.getInstanceFor(subscriber);
        Roster targetRoster = Roster.getInstanceFor(target);

        targetRoster.setSubscriptionMode(Roster.SubscriptionMode.accept_all);
        subscriberRoster.addPresenceEventListener(new PresenceEventListener() {
            @Override
            public void presenceAvailable(FullJid address, Presence availablePresence) {
            }

            @Override
            public void presenceUnavailable(FullJid address, Presence presence) {
            }

            @Override
            public void presenceError(Jid address, Presence errorPresence) {
                subscribed.signalFailure();
            }

            @Override
            public void presenceSubscribed(BareJid address, Presence subscribedPresence) {
                subscribed.signal();
            }

            @Override
            public void presenceUnsubscribed(BareJid address, Presence unsubscribedPresence) {
            }
        });

        subscriberRoster.createEntry(target.getUser().asBareJid(), targetNick, targetGroups);

        subscribed.waitForResult(10 * 1000);
    }

    public static void trustAllIdentities(OmemoManager alice, OmemoManager bob)
            throws InterruptedException, SmackException.NotConnectedException, SmackException.NotLoggedInException,
            SmackException.NoResponseException, CannotEstablishOmemoSessionException, CorruptedOmemoKeyException,
            XMPPException.XMPPErrorException, PubSubException.NotALeafNodeException {
        Roster roster = Roster.getInstanceFor(alice.getConnection());

        if (alice.getOwnJid() != bob.getOwnJid() &&
                (!roster.iAmSubscribedTo(bob.getOwnJid()) || !roster.isSubscribedToMyPresence(bob.getOwnJid()))) {
            throw new IllegalStateException("Before trusting identities of a user, we must be subscribed to one another.");
        }

        alice.requestDeviceListUpdateFor(bob.getOwnJid());
        HashMap<OmemoDevice, OmemoFingerprint> fingerprints = alice.getActiveFingerprints(bob.getOwnJid());

        for (OmemoDevice device : fingerprints.keySet()) {
            OmemoFingerprint fingerprint = fingerprints.get(device);
            alice.trustOmemoIdentity(device, fingerprint);
        }
    }

    public static void trustAllIdentitiesWithTests(OmemoManager alice, OmemoManager bob)
            throws InterruptedException, SmackException.NotConnectedException, SmackException.NotLoggedInException,
            SmackException.NoResponseException, CannotEstablishOmemoSessionException, CorruptedOmemoKeyException,
            XMPPException.XMPPErrorException, PubSubException.NotALeafNodeException {
        alice.requestDeviceListUpdateFor(bob.getOwnJid());
        HashMap<OmemoDevice, OmemoFingerprint> fps1 = alice.getActiveFingerprints(bob.getOwnJid());

        assertFalse(fps1.isEmpty());
        assertAllDevicesAreUndecided(alice, fps1);
        assertAllDevicesAreUntrusted(alice, fps1);

        trustAllIdentities(alice, bob);

        HashMap<OmemoDevice, OmemoFingerprint> fps2 = alice.getActiveFingerprints(bob.getOwnJid());
        assertEquals(fps1.size(), fps2.size());
        assertTrue(Maps.difference(fps1, fps2).areEqual());

        assertAllDevicesAreDecided(alice, fps2);
        assertAllDevicesAreTrusted(alice, fps2);
    }

    public static OmemoManager prepareOmemoManager(XMPPConnection connection) throws Exception {
        final OmemoManager manager = OmemoManager.getInstanceFor(connection, OmemoManager.randomDeviceId());
        manager.setTrustCallback(new EphemeralTrustCallback());

        if (connection.isAuthenticated()) {
            manager.initialize();
        } else {
            throw new AssertionError("Connection must be authenticated.");
        }
        return manager;
    }

    public static void assertAllDevicesAreUndecided(OmemoManager manager, HashMap<OmemoDevice, OmemoFingerprint> devices) {
        for (OmemoDevice device : devices.keySet()) {
            // All fingerprints MUST be neither decided, nor trusted.
            assertFalse(manager.isDecidedOmemoIdentity(device, devices.get(device)));
        }
    }

    public static void assertAllDevicesAreUntrusted(OmemoManager manager, HashMap<OmemoDevice, OmemoFingerprint> devices) {
        for (OmemoDevice device : devices.keySet()) {
            // All fingerprints MUST be neither decided, nor trusted.
            assertFalse(manager.isTrustedOmemoIdentity(device, devices.get(device)));
        }
    }

    public static void assertAllDevicesAreDecided(OmemoManager manager, HashMap<OmemoDevice, OmemoFingerprint> devices) {
        for (OmemoDevice device : devices.keySet()) {
            // All fingerprints MUST be neither decided, nor trusted.
            assertTrue(manager.isDecidedOmemoIdentity(device, devices.get(device)));
        }
    }

    public static void assertAllDevicesAreTrusted(OmemoManager manager, HashMap<OmemoDevice, OmemoFingerprint> devices) {
        for (OmemoDevice device : devices.keySet()) {
            // All fingerprints MUST be neither decided, nor trusted.
            assertTrue(manager.isTrustedOmemoIdentity(device, devices.get(device)));
        }
    }

    public static void cleanUpPubSub(OmemoManager omemoManager) {
        PubSubManager pm = PubSubManager.getInstance(omemoManager.getConnection(),omemoManager.getOwnJid());
        try {
            omemoManager.requestDeviceListUpdateFor(omemoManager.getOwnJid());
        } catch (SmackException.NotConnectedException | InterruptedException | SmackException.NoResponseException | PubSubException.NotALeafNodeException | XMPPException.XMPPErrorException e) {
            // ignore
        }

        OmemoCachedDeviceList deviceList = OmemoService.getInstance().getOmemoStoreBackend()
                .loadCachedDeviceList(omemoManager.getOwnDevice(), omemoManager.getOwnJid());

        for (int id : deviceList.getAllDevices()) {
            try {
                pm.getLeafNode(OmemoConstants.PEP_NODE_BUNDLE_FROM_DEVICE_ID(id)).deleteAllItems();
            } catch (InterruptedException | SmackException.NoResponseException | SmackException.NotConnectedException |
                    PubSubException.NotALeafNodeException | XMPPException.XMPPErrorException |
                    PubSubException.NotAPubSubNodeException e) {
                // Silent
            }

            try {
                pm.deleteNode(OmemoConstants.PEP_NODE_BUNDLE_FROM_DEVICE_ID(id));
            } catch (SmackException.NoResponseException | InterruptedException | SmackException.NotConnectedException
                    | XMPPException.XMPPErrorException e) {
                // Silent
            }
        }

        try {
            pm.getLeafNode(OmemoConstants.PEP_NODE_DEVICE_LIST).deleteAllItems();
        } catch (InterruptedException | SmackException.NoResponseException | SmackException.NotConnectedException |
                PubSubException.NotALeafNodeException | XMPPException.XMPPErrorException |
                PubSubException.NotAPubSubNodeException e) {
            // Silent
        }

        try {
            pm.deleteNode(OmemoConstants.PEP_NODE_DEVICE_LIST);
        } catch (SmackException.NoResponseException | InterruptedException | SmackException.NotConnectedException |
                XMPPException.XMPPErrorException e) {
            // Silent
        }
    }

    public static void cleanUpRoster(OmemoManager omemoManager) {
        Roster roster = Roster.getInstanceFor(omemoManager.getConnection());
        for (RosterEntry r : roster.getEntries()) {
            try {
                roster.removeEntry(r);
            } catch (InterruptedException | SmackException.NoResponseException | SmackException.NotConnectedException |
                    XMPPException.XMPPErrorException | SmackException.NotLoggedInException e) {
                // Silent
            }
        }
    }
}
