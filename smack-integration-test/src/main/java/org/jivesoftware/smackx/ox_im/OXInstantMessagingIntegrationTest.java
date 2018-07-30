/**
 *
 * Copyright 2018 Paul Schaub.
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
package org.jivesoftware.smackx.ox_im;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;

import java.io.File;
import java.util.logging.Level;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.util.FileUtils;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.ox.AbstractOpenPgpIntegrationTest;
import org.jivesoftware.smackx.ox.OpenPgpContact;
import org.jivesoftware.smackx.ox.OpenPgpManager;
import org.jivesoftware.smackx.ox.crypto.PainlessOpenPgpProvider;
import org.jivesoftware.smackx.ox.element.SigncryptElement;
import org.jivesoftware.smackx.ox.store.filebased.FileBasedOpenPgpStore;
import org.jivesoftware.smackx.ox.util.OpenPgpPubSubUtil;

import org.igniterealtime.smack.inttest.SmackIntegrationTest;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.TestNotPossibleException;
import org.igniterealtime.smack.inttest.util.SimpleResultSyncPoint;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.pgpainless.decryption_verification.OpenPgpMetadata;
import org.pgpainless.key.OpenPgpV4Fingerprint;
import org.pgpainless.key.protection.UnprotectedKeysProtector;

public class OXInstantMessagingIntegrationTest extends AbstractOpenPgpIntegrationTest {

    private static final String sessionId = StringUtils.randomString(10);
    private static final File aliceStorePath = FileUtils.getTempDir("basic_ox_messaging_test_alice_" + sessionId);
    private static final File bobStorePath = FileUtils.getTempDir("basic_ox_messaging_test_bob_" + sessionId);

    private OpenPgpV4Fingerprint aliceFingerprint = null;
    private OpenPgpV4Fingerprint bobFingerprint = null;

    private OpenPgpManager aliceOpenPgp;
    private OpenPgpManager bobOpenPgp;

    /**
     * This integration test tests basic OX message exchange.
     * In this scenario, Alice and Bob are strangers, as they do not have subscribed to one another.
     *
     * Alice (conOne) creates keys and publishes them to the server.
     * Bob (conTwo) creates keys and publishes them to the server.
     *
     * Alice then manually fetches Bobs metadata node and all announced keys.
     *
     * Alice trusts Bobs keys and vice versa (even though Bob does not have copies of Alice' keys yet).
     *
     * She proceeds to create an OX encrypted message, which is encrypted to Bob and herself and signed by her.
     *
     * She sends the message.
     *
     * Bob receives the message, which - due to missing keys - triggers him to update Alice' keys.
     *
     * After the update Bob proceeds to decrypt and verify the message.
     *
     * After the test, the keys are deleted from local storage and from PubSub.
     *
     * @param environment test environment
     *
     * @throws XMPPException.XMPPErrorException
     * @throws InterruptedException
     * @throws SmackException.NotConnectedException
     * @throws TestNotPossibleException if the test is not possible due to lacking server support for PEP.
     * @throws SmackException.NoResponseException
     */
    public OXInstantMessagingIntegrationTest(SmackIntegrationTestEnvironment environment)
            throws XMPPException.XMPPErrorException, InterruptedException, SmackException.NotConnectedException,
            TestNotPossibleException, SmackException.NoResponseException {
        super(environment);
    }

    @BeforeClass
    @AfterClass
    public static void deleteStore() {
        LOGGER.log(Level.INFO, "Deleting storage directories...");
        FileUtils.deleteDirectory(aliceStorePath);
        FileUtils.deleteDirectory(bobStorePath);
    }

    @SmackIntegrationTest
    public void basicInstantMessagingTest()
            throws Exception {

        LOGGER.log(Level.INFO, aliceStorePath.getAbsolutePath() + " " + bobStorePath.getAbsolutePath());

        final SimpleResultSyncPoint bobReceivedMessage = new SimpleResultSyncPoint();
        final String body = "Writing integration tests is an annoying task, but it has to be done, so lets do it!!!";

        FileBasedOpenPgpStore aliceStore = new FileBasedOpenPgpStore(aliceStorePath);
        aliceStore.setKeyRingProtector(new UnprotectedKeysProtector());
        FileBasedOpenPgpStore bobStore = new FileBasedOpenPgpStore(bobStorePath);
        bobStore.setKeyRingProtector(new UnprotectedKeysProtector());

        PainlessOpenPgpProvider aliceProvider = new PainlessOpenPgpProvider(aliceConnection, aliceStore);
        PainlessOpenPgpProvider bobProvider = new PainlessOpenPgpProvider(bobConnection, bobStore);

        aliceOpenPgp = OpenPgpManager.getInstanceFor(aliceConnection);
        bobOpenPgp = OpenPgpManager.getInstanceFor(bobConnection);

        OXInstantMessagingManager aliceInstantMessaging = OXInstantMessagingManager.getInstanceFor(aliceConnection);
        OXInstantMessagingManager bobInstantMessaging = OXInstantMessagingManager.getInstanceFor(bobConnection);

        bobInstantMessaging.addOxMessageListener(new OxMessageListener() {
            @Override
            public void newIncomingOxMessage(OpenPgpContact contact, Message originalMessage, SigncryptElement decryptedPayload, OpenPgpMetadata metadata) {
                if (((Message.Body) decryptedPayload.getExtension(Message.Body.NAMESPACE)).getMessage().equals(body)) {
                    bobReceivedMessage.signal();
                } else {
                    bobReceivedMessage.signalFailure();
                }
            }
        });

        aliceOpenPgp.setOpenPgpProvider(aliceProvider);
        bobOpenPgp.setOpenPgpProvider(bobProvider);

        aliceFingerprint = aliceOpenPgp.generateAndImportKeyPair(alice);
        bobFingerprint = bobOpenPgp.generateAndImportKeyPair(bob);

        aliceOpenPgp.announceSupportAndPublish();
        bobOpenPgp.announceSupportAndPublish();

        OpenPgpContact bobForAlice = aliceOpenPgp.getOpenPgpContact(bob.asEntityBareJidIfPossible());
        OpenPgpContact aliceForBob = bobOpenPgp.getOpenPgpContact(alice.asEntityBareJidIfPossible());

        bobForAlice.updateKeys(aliceConnection);

        assertFalse(bobForAlice.isTrusted(bobFingerprint));
        assertFalse(aliceForBob.isTrusted(aliceFingerprint));

        bobForAlice.trust(bobFingerprint);
        aliceForBob.trust(aliceFingerprint);

        assertTrue(bobForAlice.isTrusted(bobFingerprint));
        assertTrue(aliceForBob.isTrusted(aliceFingerprint));

        aliceInstantMessaging.sendOxMessage(bobForAlice, body);

        bobReceivedMessage.waitForResult(timeout);
    }

    @After
    public void deleteKeyMetadata()
            throws XMPPException.XMPPErrorException, SmackException.NotConnectedException, InterruptedException,
            SmackException.NoResponseException {
        OpenPgpPubSubUtil.deletePubkeysListNode(aliceConnection);
        OpenPgpPubSubUtil.deletePubkeysListNode(bobConnection);

        if (aliceFingerprint != null) {
            OpenPgpPubSubUtil.deletePublicKeyNode(aliceConnection, aliceFingerprint);
        }
        if (bobFingerprint != null) {
            OpenPgpPubSubUtil.deletePublicKeyNode(bobConnection, bobFingerprint);
        }

        if (aliceOpenPgp != null) {
            aliceOpenPgp.stopMetadataListener();
        }

        if (bobOpenPgp != null) {
            bobOpenPgp.stopMetadataListener();
        }
    }
}
