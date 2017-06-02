/**
 *
 * Copyright 2017 Florian Schmaus, Paul Schaub
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

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterEntry;
import org.jivesoftware.smackx.omemo.element.OmemoBundleElement;
import org.jivesoftware.smackx.omemo.exceptions.CannotEstablishOmemoSessionException;
import org.jivesoftware.smackx.omemo.exceptions.CorruptedOmemoKeyException;
import org.jivesoftware.smackx.omemo.internal.CachedDeviceList;
import org.jivesoftware.smackx.omemo.util.OmemoConstants;
import org.jivesoftware.smackx.pubsub.PubSubAssertionError;
import org.jivesoftware.smackx.pubsub.PubSubException;
import org.jivesoftware.smackx.pubsub.PubSubManager;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;

/**
 * Class containing some helper methods for OmemoIntegrationTests.
 */
final class OmemoIntegrationTestHelper {

    private static final Logger LOGGER = Logger.getLogger(OmemoIntegrationTestHelper.class.getSimpleName());

    static void cleanServerSideTraces(OmemoManager omemoManager) {
        cleanUpPubSub(omemoManager);
        cleanUpRoster(omemoManager);
    }

    static void deletePath(File storePath) {
        FileBasedOmemoStore.deleteDirectory(storePath);
    }

    static void deletePath(OmemoManager omemoManager) {
        OmemoService.getInstance().getOmemoStoreBackend().purgeOwnDeviceKeys(omemoManager);
    }

    static void cleanUpPubSub(OmemoManager omemoManager) {
        PubSubManager pm = PubSubManager.getInstance(omemoManager.getConnection(),omemoManager.getOwnJid());
        try {
            omemoManager.requestDeviceListUpdateFor(omemoManager.getOwnJid());
        } catch (SmackException.NotConnectedException | InterruptedException | SmackException.NoResponseException e) {
            //ignore
        }

        CachedDeviceList deviceList = OmemoService.getInstance().getOmemoStoreBackend().loadCachedDeviceList(omemoManager, omemoManager.getOwnJid());
        for (int id : deviceList.getAllDevices()) {
            try {
                pm.getLeafNode(OmemoConstants.PEP_NODE_BUNDLE_FROM_DEVICE_ID(id)).deleteAllItems();
            } catch (InterruptedException | SmackException.NoResponseException | SmackException.NotConnectedException | PubSubException.NotALeafNodeException | XMPPException.XMPPErrorException | PubSubAssertionError.DiscoInfoNodeAssertionError e) {
                //Silent
            }

            try {
                pm.deleteNode(OmemoConstants.PEP_NODE_BUNDLE_FROM_DEVICE_ID(id));
            } catch (SmackException.NoResponseException | InterruptedException | SmackException.NotConnectedException | XMPPException.XMPPErrorException | PubSubAssertionError e) {
                //Silent
            }
        }

        try {
            pm.getLeafNode(OmemoConstants.PEP_NODE_DEVICE_LIST).deleteAllItems();
        } catch (InterruptedException | SmackException.NoResponseException | SmackException.NotConnectedException | PubSubException.NotALeafNodeException | XMPPException.XMPPErrorException | PubSubAssertionError.DiscoInfoNodeAssertionError e) {
            //Silent
        }

        try {
            pm.deleteNode(OmemoConstants.PEP_NODE_DEVICE_LIST);
        } catch (SmackException.NoResponseException | InterruptedException | SmackException.NotConnectedException | XMPPException.XMPPErrorException | PubSubAssertionError e) {
            //Silent
        }
    }

    static void cleanUpRoster(OmemoManager omemoManager) {
        Roster roster = Roster.getInstanceFor(omemoManager.getConnection());
        for (RosterEntry r : roster.getEntries()) {
            try {
                roster.removeEntry(r);
            } catch (InterruptedException | SmackException.NoResponseException | SmackException.NotConnectedException | XMPPException.XMPPErrorException | SmackException.NotLoggedInException e) {
                //Silent
            }
        }
    }

    /**
     * Let Alice subscribe to Bob.
     * @param alice
     * @param bob
     * @throws SmackException.NotLoggedInException
     * @throws XMPPException.XMPPErrorException
     * @throws SmackException.NotConnectedException
     * @throws InterruptedException
     * @throws SmackException.NoResponseException
     */
    static void subscribe(OmemoManager alice, OmemoManager bob, String nick)
            throws SmackException.NotLoggedInException, XMPPException.XMPPErrorException,
            SmackException.NotConnectedException, InterruptedException,
            SmackException.NoResponseException {

        Roster aliceRoster = Roster.getInstanceFor(alice.getConnection());
        Roster bobsRoster = Roster.getInstanceFor(bob.getConnection());
        bobsRoster.setSubscriptionMode(Roster.SubscriptionMode.accept_all);
        aliceRoster.createEntry(bob.getOwnJid(), nick, null);
    }


    static void unidirectionalTrust(OmemoManager alice, OmemoManager bob) throws SmackException.NotConnectedException, InterruptedException, SmackException.NoResponseException, CannotEstablishOmemoSessionException {
        //Fetch deviceList
        alice.requestDeviceListUpdateFor(bob.getOwnJid());
        LOGGER.log(Level.INFO, "Current deviceList state: " + alice.getOwnDevice() + " knows " + bob.getOwnDevice() + ": "
                + OmemoService.getInstance().getOmemoStoreBackend().loadCachedDeviceList(alice, bob.getOwnJid()));
        assertTrue("Trusting party must know the others device at this point.",
                alice.getOmemoService().getOmemoStoreBackend().loadCachedDeviceList(alice, bob.getOwnJid())
                        .getActiveDevices().contains(bob.getDeviceId()));

        //Create sessions
        alice.buildSessionsWith(bob.getOwnJid());
        assertTrue("Trusting party must have a session with the other end at this point.",
                !alice.getOmemoService().getOmemoStoreBackend().loadAllRawSessionsOf(alice, bob.getOwnJid()).isEmpty());

        //Trust the other party
        alice.getOmemoService().getOmemoStoreBackend().trustOmemoIdentity(alice, bob.getOwnDevice(),
                alice.getOmemoService().getOmemoStoreBackend().getFingerprint(alice, bob.getOwnDevice()));

    }

    static void setUpOmemoManager(OmemoManager omemoManager) throws CorruptedOmemoKeyException, InterruptedException, SmackException.NoResponseException, SmackException.NotConnectedException, XMPPException.XMPPErrorException, SmackException.NotLoggedInException, PubSubException.NotALeafNodeException {
        omemoManager.initialize();
        OmemoBundleElement bundle = OmemoService.fetchBundle(omemoManager, omemoManager.getOwnDevice());
        assertNotNull("Bundle must not be null.", bundle);
        assertEquals("Published Bundle must equal our local bundle.", bundle, omemoManager.getOmemoService().getOmemoStoreBackend().packOmemoBundle(omemoManager));
    }
}
