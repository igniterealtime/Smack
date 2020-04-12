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

import java.io.IOException;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;

import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.TestNotPossibleException;
import org.igniterealtime.smack.inttest.annotations.AfterClass;
import org.igniterealtime.smack.inttest.annotations.BeforeClass;
import org.igniterealtime.smack.inttest.util.IntegrationTestRosterUtil;

/**
 * Abstract OMEMO integration test framing, which creates and initializes two OmemoManagers (for conOne and conTwo).
 * Both users subscribe to one another and trust their identities.
 * After the test traces in PubSub and in the users Rosters are removed.
 */
public abstract class AbstractTwoUsersOmemoIntegrationTest extends AbstractOmemoIntegrationTest {

    protected OmemoManager alice, bob;

    public AbstractTwoUsersOmemoIntegrationTest(SmackIntegrationTestEnvironment environment)
            throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException,
            SmackException.NoResponseException, TestNotPossibleException {
        super(environment);
    }

    @BeforeClass
    public void setup() throws Exception {
        alice = OmemoManagerSetupHelper.prepareOmemoManager(conOne);
        bob = OmemoManagerSetupHelper.prepareOmemoManager(conTwo);

        assertFalse(alice.getDeviceId().equals(bob.getDeviceId()));

        // Subscribe presences
        IntegrationTestRosterUtil.ensureBothAccountsAreSubscribedToEachOther(alice.getConnection(), bob.getConnection(), timeout);

        OmemoManagerSetupHelper.trustAllIdentitiesWithTests(alice, bob);    // Alice trusts Bob's devices
        OmemoManagerSetupHelper.trustAllIdentitiesWithTests(bob, alice);    // Bob trusts Alice' and Mallory's devices

        assertEquals(bob.getOwnFingerprint(), alice.getActiveFingerprints(bob.getOwnJid()).get(bob.getOwnDevice()));
        assertEquals(alice.getOwnFingerprint(), bob.getActiveFingerprints(alice.getOwnJid()).get(alice.getOwnDevice()));
    }

    @AfterClass
    public void cleanUp() throws IOException {
        alice.stopStanzaAndPEPListeners();
        bob.stopStanzaAndPEPListeners();
        OmemoManagerSetupHelper.cleanUpPubSub(alice);
        OmemoManagerSetupHelper.cleanUpRoster(alice);
        OmemoManagerSetupHelper.cleanUpPubSub(bob);
        OmemoManagerSetupHelper.cleanUpRoster(bob);
    }
}
