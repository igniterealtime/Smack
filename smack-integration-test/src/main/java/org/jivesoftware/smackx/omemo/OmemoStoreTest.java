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

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNotSame;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;
import static org.jivesoftware.smackx.omemo.OmemoIntegrationTestHelper.deletePath;
import static org.jivesoftware.smackx.omemo.OmemoIntegrationTestHelper.setUpOmemoManager;

import java.util.Date;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;

import org.igniterealtime.smack.inttest.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.TestNotPossibleException;

/**
 * Test the OmemoStore.
 */
public class OmemoStoreTest extends AbstractOmemoIntegrationTest {

    private OmemoManager alice;
    private OmemoManager bob;

    public OmemoStoreTest(SmackIntegrationTestEnvironment environment) throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException, SmackException.NoResponseException, TestNotPossibleException {
        super(environment);
    }

    @Override
    public void before() {
        alice = OmemoManager.getInstanceFor(conOne);
        bob = OmemoManager.getInstanceFor(conOne);
    }

    @SmackIntegrationTest
    public void storeTest() throws Exception {

        //########### PRE-INITIALIZATION ############

        assertEquals("Creating an OmemoManager without MUST have set the default deviceId.", alice.getDeviceId(), OmemoService.getInstance().getOmemoStoreBackend().getDefaultDeviceId(alice.getOwnJid()));
        assertEquals("OmemoManager must be equal, since both got created without giving a deviceId.", alice, bob);
        OmemoService.getInstance().getOmemoStoreBackend().setDefaultDeviceId(alice.getOwnJid(), -1); //Reset default deviceId

        alice.shutdown();

        alice = OmemoManager.getInstanceFor(conOne);
        assertNotSame("Instantiating OmemoManager without deviceId MUST assign random deviceId.", alice.getDeviceId(), bob.getDeviceId());

        OmemoStore<?,?,?,?,?,?,?,?,?> store = OmemoService.getInstance().getOmemoStoreBackend();
        OmemoFingerprint finger = new OmemoFingerprint("FINGER");
        //DefaultDeviceId
        store.setDefaultDeviceId(alice.getOwnJid(), 777);
        assertEquals("defaultDeviceId setting/getting must equal.", 777, store.getDefaultDeviceId(alice.getOwnJid()));

        //Trust/Distrust/Decide
        bob.shutdown();
        bob = OmemoManager.getInstanceFor(conTwo, 998);
        assertFalse("Bobs device MUST be undecided at this point",
                store.isDecidedOmemoIdentity(alice, bob.getOwnDevice(), finger));
        assertFalse("Bobs device MUST not be trusted at this point",
                store.isTrustedOmemoIdentity(alice, bob.getOwnDevice(), finger));
        store.trustOmemoIdentity(alice, bob.getOwnDevice(), finger);
        assertTrue("Bobs device MUST be trusted at this point.",
                store.isTrustedOmemoIdentity(alice, bob.getOwnDevice(), finger));
        assertTrue("Bobs device MUST be decided at this point.",
                store.isDecidedOmemoIdentity(alice, bob.getOwnDevice(), finger));
        store.distrustOmemoIdentity(alice, bob.getOwnDevice(), finger);
        assertFalse("Bobs device MUST be untrusted at this point.",
                store.isTrustedOmemoIdentity(alice, bob.getOwnDevice(), finger));

        //Dates
        assertNull("Date of last received message must be null when no message was received ever.",
                store.getDateOfLastReceivedMessage(alice, bob.getOwnDevice()));
        Date now = new Date();
        store.setDateOfLastReceivedMessage(alice, bob.getOwnDevice(), now);
        assertEquals("Date of last reveived message must match the one we set.",
                now, store.getDateOfLastReceivedMessage(alice, bob.getOwnDevice()));
        assertNull("Date of last signed prekey renewal must be null.",
                store.getDateOfLastSignedPreKeyRenewal(alice));
        store.setDateOfLastSignedPreKeyRenewal(alice, now);
        assertEquals("Date of last signed prekey renewal must match our date.",
                now, store.getDateOfLastSignedPreKeyRenewal(alice));

        //Keys
        assertNull("IdentityKeyPair must be null at this point.",
                store.loadOmemoIdentityKeyPair(alice));
        assertNull("IdentityKey of contact must be null at this point.",
                store.loadOmemoIdentityKey(alice, bob.getOwnDevice()));
        assertEquals("PreKeys list must be of length 0 at this point.",
                0, store.loadOmemoPreKeys(alice).size());
        assertEquals("SignedPreKeys list must be of length 0 at this point.",
                0, store.loadOmemoSignedPreKeys(alice).size());

        assertNotNull("Generated IdentityKeyPair must not be null.",
                store.generateOmemoIdentityKeyPair());
        assertEquals("Generated PreKey list must be of correct length.",
                100, store.generateOmemoPreKeys(1, 100).size());


        //LastPreKeyId
        assertEquals("LastPreKeyId must be 0 at this point.",
                0, store.loadLastPreKeyId(alice));
        store.storeLastPreKeyId(alice, 1234);
        Thread.sleep(100);
        assertEquals("LastPreKeyId set/get must equal.", 1234, store.loadLastPreKeyId(alice));
        store.storeLastPreKeyId(alice, 0);

        //CurrentSignedPreKeyId
        assertEquals("CurrentSignedPreKeyId must be 0 at this point.",
                0, store.loadCurrentSignedPreKeyId(alice));
        store.storeCurrentSignedPreKeyId(alice, 554);
        Thread.sleep(100);
        assertEquals("CurrentSignedPreKeyId must match the value we set.",
                554, store.loadCurrentSignedPreKeyId(alice));
        store.storeCurrentSignedPreKeyId(alice, 0);

        deletePath(alice);

        //################# POST-INITIALIZATION #################
        setUpOmemoManager(alice);

        //Keys
        assertNotNull("IdentityKeyPair must not be null after initialization",
                store.loadOmemoIdentityKeyPair(alice));
        assertNotSame("LastPreKeyId must not be 0 after initialization.",
                0, store.loadLastPreKeyId(alice));
        assertNotSame("currentSignedPreKeyId must not be 0 after initialization.",
                0, store.loadCurrentSignedPreKeyId(alice));
        assertNotNull("The last PreKey must not be null.",
                store.loadOmemoPreKey(alice, store.loadLastPreKeyId(alice) - 1));
        assertNotNull("The current signedPreKey must not be null.",
                store.loadOmemoSignedPreKey(alice, store.loadCurrentSignedPreKeyId(alice)));
    }

    @Override
    public void after() {
        alice.shutdown();
        bob.shutdown();
    }
}
