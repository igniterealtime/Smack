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
package org.jivesoftware.smackx.ox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Arrays;
import java.util.Set;
import java.util.logging.Level;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.util.StringUtils;

import org.jivesoftware.smackx.ox.callback.backup.AskForBackupCodeCallback;
import org.jivesoftware.smackx.ox.callback.backup.DisplayBackupCodeCallback;
import org.jivesoftware.smackx.ox.callback.backup.SecretKeyBackupSelectionCallback;
import org.jivesoftware.smackx.ox.crypto.PainlessOpenPgpProvider;
import org.jivesoftware.smackx.ox.exception.InvalidBackupCodeException;
import org.jivesoftware.smackx.ox.exception.MissingOpenPgpKeyException;
import org.jivesoftware.smackx.ox.exception.MissingUserIdOnKeyException;
import org.jivesoftware.smackx.ox.exception.NoBackupFoundException;
import org.jivesoftware.smackx.ox.store.definition.OpenPgpStore;
import org.jivesoftware.smackx.ox.store.filebased.FileBasedOpenPgpStore;
import org.jivesoftware.smackx.pubsub.PubSubException;

import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.igniterealtime.smack.inttest.SmackIntegrationTestEnvironment;
import org.igniterealtime.smack.inttest.TestNotPossibleException;
import org.igniterealtime.smack.inttest.annotations.AfterClass;
import org.igniterealtime.smack.inttest.annotations.BeforeClass;
import org.igniterealtime.smack.inttest.annotations.SmackIntegrationTest;
import org.pgpainless.key.OpenPgpV4Fingerprint;
import org.pgpainless.key.protection.UnprotectedKeysProtector;

public class OXSecretKeyBackupIntegrationTest extends AbstractOpenPgpIntegrationTest {

    private static final String sessionId = StringUtils.randomString(10);
    private static final File tempDir = org.apache.commons.io.FileUtils.getTempDirectory();
    private static final File beforePath = new File(tempDir, "ox_backup_" + sessionId);
    private static final File afterPath = new File(tempDir, "ox_restore_" + sessionId);

    private String backupCode = null;

    private OpenPgpManager openPgpManager;

    /**
     * This integration test tests the basic secret key backup and restore functionality as described
     * in XEP-0373 §5.
     *
     * In order to simulate two different devices, we are using two {@link FileBasedOpenPgpStore} implementations
     * which point to different directories.
     *
     * First, Alice generates a fresh OpenPGP key pair.
     *
     * She then creates a backup of the key in her private PEP node.
     *
     * Now the {@link OpenPgpStore} implementation is replaced by another instance to simulate a different device.
     *
     * Then the secret key backup is restored from PubSub and the imported secret key is compared to the one in
     * the original store.
     *
     * Afterwards the private PEP node is deleted from PubSub and the storage directories are emptied.
     *
     * @see <a href="https://xmpp.org/extensions/xep-0373.html#synchro-pep">
     *     XEP-0373 §5: Synchronizing the Secret Key with a Private PEP Node</a>
     * @param environment TODO javadoc me please
     * @throws XMPPException.XMPPErrorException if there was an XMPP error returned.
     * @throws TestNotPossibleException if the test is not possible.
     * @throws SmackException.NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     * @throws SmackException.NoResponseException if there was no response from the remote entity.
     */
    public OXSecretKeyBackupIntegrationTest(SmackIntegrationTestEnvironment environment)
            throws XMPPException.XMPPErrorException, TestNotPossibleException, SmackException.NotConnectedException,
            InterruptedException, SmackException.NoResponseException {
        super(environment);
        if (!OpenPgpManager.serverSupportsSecretKeyBackups(aliceConnection)) {
            throw new TestNotPossibleException("Server does not support the 'whitelist' PubSub access model.");
        }
    }

    @AfterClass
    @BeforeClass
    public static void cleanStore() throws IOException {
        LOGGER.log(Level.INFO, "Delete store directories...");
        org.apache.commons.io.FileUtils.deleteDirectory(afterPath);
        org.apache.commons.io.FileUtils.deleteDirectory(beforePath);
    }

    @SmackIntegrationTest
    public void test() throws InvalidAlgorithmParameterException, NoSuchAlgorithmException,
            NoSuchProviderException, IOException, InterruptedException, PubSubException.NotALeafNodeException,
            SmackException.NoResponseException, SmackException.NotConnectedException, XMPPException.XMPPErrorException,
            SmackException.NotLoggedInException, SmackException.FeatureNotSupportedException,
            MissingUserIdOnKeyException, NoBackupFoundException, InvalidBackupCodeException, PGPException,
            MissingOpenPgpKeyException {

        OpenPgpStore beforeStore = new FileBasedOpenPgpStore(beforePath);
        beforeStore.setKeyRingProtector(new UnprotectedKeysProtector());
        PainlessOpenPgpProvider beforeProvider = new PainlessOpenPgpProvider(aliceConnection, beforeStore);
        openPgpManager = OpenPgpManager.getInstanceFor(aliceConnection);
        openPgpManager.setOpenPgpProvider(beforeProvider);

        OpenPgpSelf self = openPgpManager.getOpenPgpSelf();

        assertNull(self.getSigningKeyFingerprint());

        OpenPgpV4Fingerprint keyFingerprint = openPgpManager.generateAndImportKeyPair(alice);
        assertEquals(keyFingerprint, self.getSigningKeyFingerprint());

        assertTrue(self.getSecretKeys().contains(keyFingerprint.getKeyId()));

        PGPSecretKeyRing beforeSec = beforeStore.getSecretKeyRing(alice, keyFingerprint);
        assertNotNull(beforeSec);

        PGPPublicKeyRing beforePub = beforeStore.getPublicKeyRing(alice, keyFingerprint);
        assertNotNull(beforePub);

        openPgpManager.backupSecretKeyToServer(new DisplayBackupCodeCallback() {
            @Override
            public void displayBackupCode(String backupCode) {
                OXSecretKeyBackupIntegrationTest.this.backupCode = backupCode;
            }
        }, new SecretKeyBackupSelectionCallback() {
            @Override
            public Set<OpenPgpV4Fingerprint> selectKeysToBackup(Set<OpenPgpV4Fingerprint> availableSecretKeys) {
                return availableSecretKeys;
            }
        });

        FileBasedOpenPgpStore afterStore = new FileBasedOpenPgpStore(afterPath);
        afterStore.setKeyRingProtector(new UnprotectedKeysProtector());
        PainlessOpenPgpProvider afterProvider = new PainlessOpenPgpProvider(aliceConnection, afterStore);
        openPgpManager.setOpenPgpProvider(afterProvider);

        OpenPgpV4Fingerprint fingerprint = openPgpManager.restoreSecretKeyServerBackup(new AskForBackupCodeCallback() {
            @Override
            public String askForBackupCode() {
                return backupCode;
            }
        });

        assertEquals(keyFingerprint, fingerprint);

        assertTrue(self.getSecretKeys().contains(keyFingerprint.getKeyId()));

        assertEquals(keyFingerprint, self.getSigningKeyFingerprint());

        PGPSecretKeyRing afterSec = afterStore.getSecretKeyRing(alice, keyFingerprint);
        assertNotNull(afterSec);
        assertTrue(Arrays.equals(beforeSec.getEncoded(), afterSec.getEncoded()));

        PGPPublicKeyRing afterPub = afterStore.getPublicKeyRing(alice, keyFingerprint);
        assertNotNull(afterPub);
        assertTrue(Arrays.equals(beforePub.getEncoded(), afterPub.getEncoded()));
    }
}
