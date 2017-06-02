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

import org.igniterealtime.smack.inttest.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.TestNotPossibleException;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smackx.omemo.exceptions.CorruptedOmemoKeyException;
import org.jivesoftware.smackx.omemo.util.OmemoConstants;
import org.jivesoftware.smackx.pubsub.PubSubException;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static org.jivesoftware.smackx.omemo.OmemoIntegrationTestHelper.cleanServerSideTraces;
import static org.jivesoftware.smackx.omemo.OmemoIntegrationTestHelper.setUpOmemoManager;

public class OmemoInitializationTest extends AbstractOmemoIntegrationTest {

    private OmemoManager alice;
    private OmemoStore<?,?,?,?,?,?,?,?,?> store;

    @Override
    public void before() {
        alice = OmemoManager.getInstanceFor(conOne, 666);
        store = OmemoService.getInstance().getOmemoStoreBackend();
    }

    public OmemoInitializationTest(SmackIntegrationTestEnvironment environment) throws TestNotPossibleException, XMPPErrorException, NotConnectedException, NoResponseException, InterruptedException {
        super(environment);
    }

    /**
     * Tests, if the initialization is done properly.
     */
    @SmackIntegrationTest
    public void initializationTest() throws XMPPException.XMPPErrorException, PubSubException.NotALeafNodeException, SmackException.NotConnectedException, InterruptedException, SmackException.NoResponseException, SmackException.NotLoggedInException, CorruptedOmemoKeyException {
        //test keys.
        setUpOmemoManager(alice);
        assertNotNull("IdentityKey must not be null after initialization.", store.loadOmemoIdentityKeyPair(alice));
        assertTrue("We must have " + OmemoConstants.TARGET_PRE_KEY_COUNT + " preKeys.",
                store.loadOmemoPreKeys(alice).size() == OmemoConstants.TARGET_PRE_KEY_COUNT);
        assertNotNull("Our signedPreKey must not be null.", store.loadCurrentSignedPreKeyId(alice));

        //Is deviceId published?
        assertTrue("Published deviceList must contain our deviceId.",
                OmemoService.fetchDeviceList(alice, alice.getOwnJid())
                        .getDeviceIds().contains(alice.getDeviceId()));

        assertTrue("Our fingerprint must be of correct length.",
                OmemoService.getInstance().getOmemoStoreBackend().getFingerprint(alice).length() == 64);
    }

    @Override
    public void after() {
        alice.shutdown();
        cleanServerSideTraces(alice);
    }
}
