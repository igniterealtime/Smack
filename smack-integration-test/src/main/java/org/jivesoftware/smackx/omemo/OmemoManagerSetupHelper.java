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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterEntry;

import org.jivesoftware.smackx.omemo.exceptions.CannotEstablishOmemoSessionException;
import org.jivesoftware.smackx.omemo.exceptions.CorruptedOmemoKeyException;
import org.jivesoftware.smackx.omemo.internal.OmemoDevice;
import org.jivesoftware.smackx.omemo.trust.OmemoFingerprint;
import org.jivesoftware.smackx.pubsub.PubSubException;

import com.google.common.collect.Maps;

public class OmemoManagerSetupHelper {


    public static void trustAllIdentities(OmemoManager alice, OmemoManager bob)
            throws InterruptedException, SmackException.NotConnectedException, SmackException.NotLoggedInException,
            SmackException.NoResponseException, CannotEstablishOmemoSessionException, CorruptedOmemoKeyException,
            XMPPException.XMPPErrorException, PubSubException.NotALeafNodeException, IOException {
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
            XMPPException.XMPPErrorException, PubSubException.NotALeafNodeException, IOException {
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

    public static void cleanUpPubSub(OmemoManager omemoManager)
                    throws IOException, NotConnectedException, InterruptedException {
        List<Exception> exceptions = omemoManager.purgeEverything();
        assertTrue(exceptions.isEmpty(), "There where exceptions while purging OMEMO: " + exceptions);
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
