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

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;

import java.io.File;
import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.jivesoftware.smack.test.util.FileTestUtil;
import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.jivesoftware.smackx.ox.callback.SecretKeyPassphraseCallback;
import org.jivesoftware.smackx.ox.exception.MissingUserIdOnKeyException;
import org.jivesoftware.smackx.ox.store.definition.OpenPgpStore;
import org.jivesoftware.smackx.ox.store.definition.OpenPgpTrustStore;
import org.jivesoftware.smackx.ox.store.filebased.FileBasedOpenPgpStore;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.junit.After;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.junit.runners.Parameterized;
import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.JidTestUtil;
import org.pgpainless.key.OpenPgpV4Fingerprint;
import org.pgpainless.key.collection.PGPKeyRing;
import org.pgpainless.key.protection.UnprotectedKeysProtector;
import org.pgpainless.util.Passphrase;

@RunWith(Parameterized.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class OpenPgpStoreTest extends SmackTestSuite {

    private static final File storagePath;

    private static final BareJid alice = JidTestUtil.BARE_JID_1;
    private static final BareJid bob = JidTestUtil.BARE_JID_2;

    private static final OpenPgpV4Fingerprint finger1 = new OpenPgpV4Fingerprint("DEADBEEFDEADBEEFDEADBEEFDEADBEEFDEADBEEF");
    private static final OpenPgpV4Fingerprint finger2 = new OpenPgpV4Fingerprint("C0FFEEC0FFEEC0FFEEC0FFEEC0FFEEC0FFEE1234");
    private static final OpenPgpV4Fingerprint finger3 = new OpenPgpV4Fingerprint("0123012301230123012301230123012301230123");

    private final OpenPgpStore openPgpStoreInstance1;
    private final OpenPgpStore openPgpStoreInstance2;

    static {
        storagePath = FileTestUtil.getTempDir("storeTest");
        Security.addProvider(new BouncyCastleProvider());
    }

    @Parameterized.Parameters
    public static Collection<OpenPgpStore[]> data() {
        return Arrays.asList(
                new OpenPgpStore[][] {
                        {new FileBasedOpenPgpStore(storagePath), new FileBasedOpenPgpStore(storagePath)}
                });
    }

    public OpenPgpStoreTest(OpenPgpStore firstInstance, OpenPgpStore secondInstance) {
        if (firstInstance == secondInstance || !firstInstance.getClass().equals(secondInstance.getClass())) {
            throw new IllegalArgumentException("firstInstance must be another instance of the same class as secondInstance.");
        }
        this.openPgpStoreInstance1 = firstInstance;
        this.openPgpStoreInstance2 = secondInstance;
    }

    @Before
    @After
    public void deletePath() {
        FileTestUtil.deleteDirectory(storagePath);
    }

    /*
    Generic
     */

    @Test
    public void t00_store_protectorGetSet() {
        openPgpStoreInstance1.setKeyRingProtector(new UnprotectedKeysProtector());
        assertNotNull(openPgpStoreInstance1.getKeyRingProtector());
        // TODO: Test method below
        openPgpStoreInstance1.setSecretKeyPassphraseCallback(new SecretKeyPassphraseCallback() {
            @Override
            public Passphrase onPassphraseNeeded(OpenPgpV4Fingerprint fingerprint) {
                return null;
            }
        });
    }

    /*
    OpenPgpKeyStore
     */

    @Test
    public void t00_deleteTest() throws IOException, PGPException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException, MissingUserIdOnKeyException {
        assertNull(openPgpStoreInstance1.getSecretKeysOf(alice));
        assertNull(openPgpStoreInstance1.getPublicKeysOf(alice));

        PGPKeyRing keys = openPgpStoreInstance1.generateKeyRing(alice);
        openPgpStoreInstance1.importSecretKey(alice, keys.getSecretKeys());
        openPgpStoreInstance1.importPublicKey(alice, keys.getPublicKeys());

        assertNotNull(openPgpStoreInstance1.getSecretKeysOf(alice));
        assertNotNull(openPgpStoreInstance1.getPublicKeysOf(alice));

        openPgpStoreInstance1.deleteSecretKeyRing(alice, new OpenPgpV4Fingerprint(keys.getSecretKeys()));
        openPgpStoreInstance1.deletePublicKeyRing(alice, new OpenPgpV4Fingerprint(keys.getSecretKeys()));

        assertNull(openPgpStoreInstance1.getPublicKeysOf(alice));
        assertNull(openPgpStoreInstance1.getSecretKeysOf(alice));
    }

    @Test
    public void t01_key_emptyStoreTest() throws IOException, PGPException {
        assertNull(openPgpStoreInstance1.getPublicKeysOf(alice));
        assertNull(openPgpStoreInstance1.getSecretKeysOf(alice));
        assertNull(openPgpStoreInstance1.getPublicKeyRing(alice, finger1));
        assertNull(openPgpStoreInstance1.getSecretKeyRing(alice, finger1));
    }

    @Test
    public void t02_key_importKeysTest() throws IOException, PGPException, InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException, MissingUserIdOnKeyException {
        // Test for nullity of all possible values.

        PGPKeyRing keys = openPgpStoreInstance1.generateKeyRing(alice);

        PGPSecretKeyRing secretKeys = keys.getSecretKeys();
        PGPPublicKeyRing publicKeys = keys.getPublicKeys();
        assertNotNull(secretKeys);
        assertNotNull(publicKeys);

        OpenPgpContact cAlice = openPgpStoreInstance1.getOpenPgpContact(alice);
        assertNull(cAlice.getAnyPublicKeys());

        OpenPgpV4Fingerprint fingerprint = new OpenPgpV4Fingerprint(publicKeys);
        assertEquals(fingerprint, new OpenPgpV4Fingerprint(secretKeys));

        assertNull(openPgpStoreInstance1.getPublicKeysOf(alice));
        assertNull(openPgpStoreInstance1.getSecretKeysOf(alice));

        openPgpStoreInstance1.importPublicKey(alice, publicKeys);
        assertTrue(Arrays.equals(publicKeys.getEncoded(), openPgpStoreInstance1.getPublicKeysOf(alice).getEncoded()));
        assertNotNull(openPgpStoreInstance1.getPublicKeyRing(alice, fingerprint));
        assertNull(openPgpStoreInstance1.getSecretKeysOf(alice));

        cAlice = openPgpStoreInstance1.getOpenPgpContact(alice);
        assertNotNull(cAlice.getAnyPublicKeys());

        // Import keys a second time -> No change expected.
        openPgpStoreInstance1.importPublicKey(alice, publicKeys);
        assertTrue(Arrays.equals(publicKeys.getEncoded(), openPgpStoreInstance1.getPublicKeysOf(alice).getEncoded()));
        openPgpStoreInstance1.importSecretKey(alice, secretKeys);
        assertTrue(Arrays.equals(secretKeys.getEncoded(), openPgpStoreInstance1.getSecretKeysOf(alice).getEncoded()));

        openPgpStoreInstance1.importSecretKey(alice, secretKeys);
        assertNotNull(openPgpStoreInstance1.getSecretKeysOf(alice));
        assertTrue(Arrays.equals(secretKeys.getEncoded(), openPgpStoreInstance1.getSecretKeysOf(alice).getEncoded()));
        assertNotNull(openPgpStoreInstance1.getSecretKeyRing(alice, fingerprint));

        assertTrue(Arrays.equals(secretKeys.getEncoded(), openPgpStoreInstance1.getSecretKeyRing(alice, fingerprint).getEncoded()));
        assertTrue(Arrays.equals(publicKeys.getEncoded(), openPgpStoreInstance1.getPublicKeyRing(alice, fingerprint).getEncoded()));

        // Clean up
        openPgpStoreInstance1.deletePublicKeyRing(alice, fingerprint);
        openPgpStoreInstance1.deleteSecretKeyRing(alice, fingerprint);
    }

    @Test(expected = MissingUserIdOnKeyException.class)
    public void t04_key_wrongBareJidOnSecretKeyImportTest() throws PGPException, NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException, IOException, MissingUserIdOnKeyException {
        PGPSecretKeyRing secretKeys = openPgpStoreInstance1.generateKeyRing(alice).getSecretKeys();

        openPgpStoreInstance1.importSecretKey(bob, secretKeys);
    }

    @Test(expected = MissingUserIdOnKeyException.class)
    public void t05_key_wrongBareJidOnPublicKeyImportTest() throws PGPException, NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException, IOException, MissingUserIdOnKeyException {
        PGPPublicKeyRing publicKeys = openPgpStoreInstance1.generateKeyRing(alice).getPublicKeys();

        openPgpStoreInstance1.importPublicKey(bob, publicKeys);
    }

    @Test
    public void t06_key_keyReloadTest() throws PGPException, NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException, IOException, MissingUserIdOnKeyException {
        PGPKeyRing keys = openPgpStoreInstance1.generateKeyRing(alice);
        PGPSecretKeyRing secretKeys = keys.getSecretKeys();
        OpenPgpV4Fingerprint fingerprint = new OpenPgpV4Fingerprint(secretKeys);
        PGPPublicKeyRing publicKeys = keys.getPublicKeys();

        openPgpStoreInstance1.importSecretKey(alice, secretKeys);
        openPgpStoreInstance1.importPublicKey(alice, publicKeys);

        assertNotNull(openPgpStoreInstance2.getSecretKeysOf(alice));
        assertNotNull(openPgpStoreInstance2.getPublicKeysOf(alice));

        // Clean up
        openPgpStoreInstance1.deletePublicKeyRing(alice, fingerprint);
        openPgpStoreInstance1.deleteSecretKeyRing(alice, fingerprint);
        openPgpStoreInstance2.deletePublicKeyRing(alice, fingerprint);
        openPgpStoreInstance2.deleteSecretKeyRing(alice, fingerprint);
    }

    @Test
    public void t07_multipleKeysTest() throws PGPException, NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException, IOException, MissingUserIdOnKeyException {
        PGPKeyRing one = openPgpStoreInstance1.generateKeyRing(alice);
        PGPKeyRing two = openPgpStoreInstance1.generateKeyRing(alice);

        OpenPgpV4Fingerprint fingerprint1 = new OpenPgpV4Fingerprint(one.getSecretKeys());
        OpenPgpV4Fingerprint fingerprint2 = new OpenPgpV4Fingerprint(two.getSecretKeys());

        openPgpStoreInstance1.importSecretKey(alice, one.getSecretKeys());
        openPgpStoreInstance1.importSecretKey(alice, two.getSecretKeys());
        openPgpStoreInstance1.importPublicKey(alice, one.getPublicKeys());
        openPgpStoreInstance1.importPublicKey(alice, two.getPublicKeys());

        assertTrue(Arrays.equals(one.getSecretKeys().getEncoded(), openPgpStoreInstance1.getSecretKeyRing(alice, fingerprint1).getEncoded()));
        assertTrue(Arrays.equals(two.getSecretKeys().getEncoded(), openPgpStoreInstance1.getSecretKeyRing(alice, fingerprint2).getEncoded()));

        assertTrue(Arrays.equals(one.getSecretKeys().getEncoded(), openPgpStoreInstance1.getSecretKeysOf(alice).getSecretKeyRing(fingerprint1.getKeyId()).getEncoded()));

        assertTrue(Arrays.equals(one.getPublicKeys().getEncoded(),
                openPgpStoreInstance1.getPublicKeyRing(alice, fingerprint1).getEncoded()));

        // Cleanup
        openPgpStoreInstance1.deletePublicKeyRing(alice, fingerprint1);
        openPgpStoreInstance1.deletePublicKeyRing(alice, fingerprint2);
        openPgpStoreInstance1.deleteSecretKeyRing(alice, fingerprint1);
        openPgpStoreInstance1.deleteSecretKeyRing(alice, fingerprint2);
    }

    /*
    OpenPgpTrustStore
     */

    @Test
    public void t08_trust_emptyStoreTest() throws IOException {

        assertEquals(OpenPgpTrustStore.Trust.undecided, openPgpStoreInstance1.getTrust(alice, finger2));
        openPgpStoreInstance1.setTrust(alice, finger2, OpenPgpTrustStore.Trust.trusted);
        assertEquals(OpenPgpTrustStore.Trust.trusted, openPgpStoreInstance1.getTrust(alice, finger2));
        // Set trust a second time -> no change
        openPgpStoreInstance1.setTrust(alice, finger2, OpenPgpTrustStore.Trust.trusted);
        assertEquals(OpenPgpTrustStore.Trust.trusted, openPgpStoreInstance1.getTrust(alice, finger2));

        assertEquals(OpenPgpTrustStore.Trust.undecided, openPgpStoreInstance1.getTrust(alice, finger3));

        openPgpStoreInstance1.setTrust(bob, finger2, OpenPgpTrustStore.Trust.untrusted);
        assertEquals(OpenPgpTrustStore.Trust.untrusted, openPgpStoreInstance1.getTrust(bob, finger2));
        assertEquals(OpenPgpTrustStore.Trust.trusted, openPgpStoreInstance1.getTrust(alice, finger2));

        // clean up
        openPgpStoreInstance1.setTrust(alice, finger2, OpenPgpTrustStore.Trust.undecided);
        openPgpStoreInstance1.setTrust(bob, finger2, OpenPgpTrustStore.Trust.undecided);
    }

    @Test
    public void t09_trust_reloadTest() throws IOException {
        openPgpStoreInstance1.setTrust(alice, finger1, OpenPgpTrustStore.Trust.trusted);
        assertEquals(OpenPgpTrustStore.Trust.trusted, openPgpStoreInstance2.getTrust(alice, finger1));

        // cleanup
        openPgpStoreInstance1.setTrust(alice, finger1, OpenPgpTrustStore.Trust.undecided);
        openPgpStoreInstance2.setTrust(alice, finger1, OpenPgpTrustStore.Trust.undecided);
    }

    /*
    OpenPgpMetadataStore
     */

    @Test
    public void t10_meta_emptyStoreTest() throws IOException {
        assertNotNull(openPgpStoreInstance1.getAnnouncedFingerprintsOf(alice));
        assertTrue(openPgpStoreInstance1.getAnnouncedFingerprintsOf(alice).isEmpty());

        Map<OpenPgpV4Fingerprint, Date> map = new HashMap<>();
        Date date1 = new Date(12354563423L);
        Date date2 = new Date(8274729879812L);
        map.put(finger1, date1);
        map.put(finger2, date2);

        openPgpStoreInstance1.setAnnouncedFingerprintsOf(alice, map);
        assertFalse(openPgpStoreInstance1.getAnnouncedFingerprintsOf(alice).isEmpty());
        assertEquals(map, openPgpStoreInstance1.getAnnouncedFingerprintsOf(alice));

        assertTrue(openPgpStoreInstance1.getAnnouncedFingerprintsOf(bob).isEmpty());

        assertFalse(openPgpStoreInstance2.getAnnouncedFingerprintsOf(alice).isEmpty());
        assertEquals(map, openPgpStoreInstance2.getAnnouncedFingerprintsOf(alice));

        openPgpStoreInstance1.setAnnouncedFingerprintsOf(alice, Collections.<OpenPgpV4Fingerprint, Date>emptyMap());
        openPgpStoreInstance2.setAnnouncedFingerprintsOf(alice, Collections.<OpenPgpV4Fingerprint, Date>emptyMap());
    }

    @Test
    public void t11_key_fetchDateTest() throws IOException {

        Map<OpenPgpV4Fingerprint, Date> fetchDates1 = openPgpStoreInstance1.getPublicKeyFetchDates(alice);
        assertNotNull(fetchDates1);
        assertTrue(fetchDates1.isEmpty());

        Date date1 = new Date(85092830954L);
        fetchDates1.put(finger1, date1);
        openPgpStoreInstance1.setPublicKeyFetchDates(alice, fetchDates1);

        Map<OpenPgpV4Fingerprint, Date> fetchDates2 = openPgpStoreInstance1.getPublicKeyFetchDates(alice);
        assertNotNull(fetchDates2);
        assertFalse(fetchDates2.isEmpty());
        assertEquals(fetchDates1, fetchDates2);

        Map<OpenPgpV4Fingerprint, Date> fetchDates3 = openPgpStoreInstance2.getPublicKeyFetchDates(alice);
        assertNotNull(fetchDates3);
        assertEquals(fetchDates1, fetchDates3);

        openPgpStoreInstance1.setPublicKeyFetchDates(alice, null);
        openPgpStoreInstance2.setPublicKeyFetchDates(alice, null);

        assertNotNull(openPgpStoreInstance1.getPublicKeyFetchDates(alice));
        assertTrue(openPgpStoreInstance1.getPublicKeyFetchDates(alice).isEmpty());
    }
}
