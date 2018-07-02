/**
 *
 * Copyright Paul Schaub
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
import static junit.framework.TestCase.assertSame;
import static junit.framework.TestCase.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.TreeMap;

import org.jivesoftware.smackx.omemo.exceptions.CorruptedOmemoKeyException;
import org.jivesoftware.smackx.omemo.internal.OmemoCachedDeviceList;
import org.jivesoftware.smackx.omemo.internal.OmemoDevice;
import org.jivesoftware.smackx.omemo.trust.OmemoFingerprint;

import org.junit.AfterClass;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;


public abstract class OmemoStoreTest<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess, T_Addr, T_ECPub, T_Bundle, T_Ciph> {

    protected final OmemoStore<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess, T_Addr, T_ECPub, T_Bundle, T_Ciph> store;
    private final OmemoDevice alice, bob;

    private static final TemporaryFolder tmp = initStaticTemp();

    OmemoStoreTest(OmemoStore<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess, T_Addr, T_ECPub, T_Bundle, T_Ciph> store)
            throws XmppStringprepException {
        this.store = store;
        alice = new OmemoDevice(JidCreate.bareFrom("alice@wonderland.lit"), 123);
        bob = new OmemoDevice(JidCreate.bareFrom("bob@builder.tv"), 987);
    }

    // Tests

    @Test
    public void keyUtilNotNull() {
        assertNotNull(store.keyUtil());
    }

    @Test
    public void generateOmemoIdentityKeyPairDoesNotReturnNull() {
        assertNotNull(store.generateOmemoIdentityKeyPair());
    }

    @Test
    public void identityKeyFromIdentityKeyPairIsNotNull() {
        T_IdKeyPair pair = store.generateOmemoIdentityKeyPair();
        assertNotNull(store.keyUtil().identityKeyFromPair(pair));
    }

    @Test
    public void storeLoadRemoveOmemoIdentityKeyPair()
            throws IOException, CorruptedOmemoKeyException {

        T_IdKeyPair before = store.generateOmemoIdentityKeyPair();

        assertNull(store.loadOmemoIdentityKeyPair(alice));
        store.storeOmemoIdentityKeyPair(alice, before);

        T_IdKeyPair after = store.loadOmemoIdentityKeyPair(alice);
        assertNotNull(after);

        // Fingerprints equal
        assertEquals(store.keyUtil().getFingerprintOfIdentityKeyPair(before),
                store.keyUtil().getFingerprintOfIdentityKeyPair(after));

        // Byte-representation equals
        assertTrue(Arrays.equals(
                store.keyUtil().identityKeyPairToBytes(before),
                store.keyUtil().identityKeyPairToBytes(after)));

        // Non-existing keypair
        assertNull("Must return null for non-existing key pairs.", store.loadOmemoIdentityKeyPair(bob));

        // Deleting works
        store.removeOmemoIdentityKeyPair(alice);
        assertNull(store.loadOmemoIdentityKeyPair(alice));
    }

    @Test
    public void storeLoadRemoveOmemoIdentityKey()
            throws IOException, CorruptedOmemoKeyException {

        // Create IdentityKeys and get bytes
        T_IdKey keyA1 = store.keyUtil().identityKeyFromPair(store.generateOmemoIdentityKeyPair());
        T_IdKey keyB1 = store.keyUtil().identityKeyFromPair(store.generateOmemoIdentityKeyPair());
        byte[] bytesA1 = store.keyUtil().identityKeyToBytes(keyA1);
        byte[] bytesB = store.keyUtil().identityKeyToBytes(keyB1);

        // Not null and not of length 0
        assertNotNull("Serialized identityKey cannot be null.", bytesA1);
        assertNotNull("Serialized identityKey cannot be null.", bytesB);
        assertNotSame("Serialized identityKey must be of length > 0.", 0, bytesA1.length);
        assertNotSame("Serialized identityKey must be of length > 0.", 0, bytesB.length);

        // Keys do not equal
        assertFalse("Generated IdentityKeys must not be equal (ULTRA unlikely).",
                Arrays.equals(bytesA1, bytesB));

        // Loading must return null before and not null after saving
        assertNull("Must return null, the store could not have this key by now.",
                store.loadOmemoIdentityKey(alice, bob));
        store.storeOmemoIdentityKey(alice, bob, keyA1);
        T_IdKey keyA2 = store.loadOmemoIdentityKey(alice, bob);
        assertNotNull(keyA2);

        // Loaded key must equal stored one
        byte[] bytesA2 = store.keyUtil().identityKeyToBytes(keyA2);
        assertTrue("Serialized loaded key must equal serialized stored one.",
                Arrays.equals(bytesA1, bytesA2));

        // Non-existing keys must return null
        assertNull("Non-existing keys must be returned as null.", store.loadOmemoIdentityKey(bob, alice));

        // Key must vanish when deleted.
        store.removeOmemoIdentityKey(alice, bob);
        assertNull(store.loadOmemoIdentityKey(alice, bob));
    }

    @Test
    public void generateOmemoPreKeys() {
        TreeMap<Integer, T_PreKey> keys = store.generateOmemoPreKeys(31, 49);
        assertNotNull("Generated data structure must not be null.", keys);

        byte[] lastKey = null;

        for (int i = 31; i <= 79; i++) {
            assertEquals("Key ids must be ascending order, starting at 31.", Integer.valueOf(i), keys.firstKey());
            assertNotNull("Every id must match to a key.", keys.get(keys.firstKey()));
            byte[] bytes = store.keyUtil().preKeyToBytes(keys.get(keys.firstKey()));
            assertNotNull("Serialized preKey must not be null.", bytes);
            assertNotSame("Serialized preKey must not be of length 0.", 0, bytes.length);

            if (lastKey != null) {
                assertFalse("PreKeys MUST NOT be equal.", Arrays.equals(lastKey, bytes));
            }
            lastKey = bytes;

            keys.remove(keys.firstKey());

        }

        assertEquals("After deleting 49 keys, there must be no keys left.", 0, keys.size());
    }

    @Test
    public void storeLoadRemoveOmemoPreKeys()
            throws IOException, InterruptedException {

        TreeMap<Integer, T_PreKey> before = store.generateOmemoPreKeys(1, 10);
        assertEquals("The store must have no prekeys before this test.", 0, store.loadOmemoPreKeys(alice).size());

        store.storeOmemoPreKeys(alice, before);
        TreeMap<Integer, T_PreKey> after = store.loadOmemoPreKeys(alice);
        assertNotNull("Loaded preKeys must not be null.", after);
        assertEquals("Loaded preKey count must equal stored count.", before.size(), after.size());

        // Non-existing key must be returned as null
        assertNull("Non-existing preKey must be returned as null.", store.loadOmemoPreKey(alice, 10000));

        int last = after.size();
        for (int i = 1; i <= last; i++) {
            T_PreKey bKey = before.get(i);
            T_PreKey aKey = after.get(i);

            assertTrue("Loaded keys must equal stored ones.", Arrays.equals(
                    store.keyUtil().preKeyToBytes(bKey),
                    store.keyUtil().preKeyToBytes(aKey)));

            T_PreKey rKey = store.loadOmemoPreKey(alice, i);
            assertNotNull("Randomly accessed preKeys must not be null.", rKey);
            assertTrue("Randomly accessed preKeys must equal the stored ones.", Arrays.equals(
                    store.keyUtil().preKeyToBytes(aKey),
                    store.keyUtil().preKeyToBytes(rKey)));

            store.removeOmemoPreKey(alice, i);
            assertNull("PreKey must be null after deletion.", store.loadOmemoPreKey(alice, i));
        }

        TreeMap<Integer, T_PreKey> postDeletion = store.loadOmemoPreKeys(alice);
        assertSame("PreKey count must equal 0 after deletion of all keys.", 0, postDeletion.size());
    }

    @Test
    public void storeLoadRemoveOmemoSignedPreKeys()
            throws IOException, CorruptedOmemoKeyException {

        TreeMap<Integer, T_SigPreKey> before = store.loadOmemoSignedPreKeys(alice);
        assertEquals("At this stage, there must be no signed prekeys in the store.", 0, before.size());

        T_IdKeyPair idp = store.generateOmemoIdentityKeyPair();
        T_SigPreKey spk = store.generateOmemoSignedPreKey(idp, 125);

        assertNotNull("SignedPreKey must not be null.", spk);
        assertEquals("ID of signedPreKey must match.", 125, store.keyUtil().signedPreKeyIdFromKey(spk));
        byte[] bytes = store.keyUtil().signedPreKeyToBytes(spk);
        assertNotNull("Serialized signedPreKey must not be null", bytes);
        assertNotSame("Serialized signedPreKey must not be of length 0.", 0, bytes.length);

        // Stored key must equal loaded key
        store.storeOmemoSignedPreKey(alice, 125, spk);
        TreeMap<Integer, T_SigPreKey> after = store.loadOmemoSignedPreKeys(alice);
        assertEquals("We must have exactly 1 signedPreKey now.", 1, after.size());
        T_SigPreKey spk2 = after.get(after.firstKey());
        assertEquals("Id of the stored signedPreKey must match the one we stored.",
                125, store.keyUtil().signedPreKeyIdFromKey(spk2));
        assertTrue("Serialization of stored and loaded signed preKey must equal.", Arrays.equals(
                store.keyUtil().signedPreKeyToBytes(spk), store.keyUtil().signedPreKeyToBytes(spk2)));

        // Random access
        T_SigPreKey rspk = store.loadOmemoSignedPreKey(alice, 125);
        assertTrue("Serialization of stored and randomly accessed signed preKey must equal.", Arrays.equals(
                store.keyUtil().signedPreKeyToBytes(spk), store.keyUtil().signedPreKeyToBytes(rspk)));
        assertNull("Non-existing signedPreKey must be returned as null.",
                store.loadOmemoSignedPreKey(alice, 10000));

        // Deleting
        store.removeOmemoSignedPreKey(alice, 125);
        assertNull("Deleted key must be returned as null.", store.loadOmemoSignedPreKey(alice, 125));
        assertEquals(0, store.loadOmemoSignedPreKeys(alice).size());
    }

    @Test
    public void loadStoreDateOfLastSignedPreKeyRenewal() throws IOException {
        assertNull("The date of last signed preKey renewal must be null at this stage.",
                store.getDateOfLastSignedPreKeyRenewal(alice));
        Date before = new Date();
        store.setDateOfLastSignedPreKeyRenewal(alice, before);
        Date after = store.getDateOfLastSignedPreKeyRenewal(alice);
        assertEquals("Dates must equal.", after, before);
    }

    @Test
    public void loadStoreDateOfLastMessageReceived() throws IOException {
        assertNull("The date of last message received must be null at this stage.",
                store.getDateOfLastReceivedMessage(alice, bob));
        Date before = new Date();
        store.setDateOfLastReceivedMessage(alice, bob, before);
        Date after = store.getDateOfLastReceivedMessage(alice, bob);
        assertEquals("Dates must equal.", after, before);
    }

    @Test
    public void loadStoreCachedDeviceList() throws IOException {
        Integer[] active = new Integer[] {1,5,999,10};
        Integer[] inactive = new Integer[] {6,7,8};
        OmemoCachedDeviceList before = new OmemoCachedDeviceList(
                new HashSet<>(Arrays.asList(active)),
                new HashSet<>(Arrays.asList(inactive)));

        assertNotNull("Loading a non-existent cached deviceList must return an empty list.",
                store.loadCachedDeviceList(alice, bob.getJid()));

        store.storeCachedDeviceList(alice, bob.getJid(), before);
        OmemoCachedDeviceList after = store.loadCachedDeviceList(alice, bob.getJid());
        assertTrue("Loaded deviceList must not be empty", after.getAllDevices().size() != 0);

        assertEquals("Number of entries in active devices must match.", active.length, after.getActiveDevices().size());
        assertEquals("Number of entries in inactive devices must match.", inactive.length, after.getInactiveDevices().size());
        assertEquals("Number of total entries must match.", active.length + inactive.length, after.getAllDevices().size());

        for (Integer a : active) {
            assertTrue(after.getActiveDevices().contains(a));
            assertTrue(after.getAllDevices().contains(a));
        }

        for (Integer i : inactive) {
            assertTrue(after.getInactiveDevices().contains(i));
            assertTrue(after.getAllDevices().contains(i));
        }

        store.storeCachedDeviceList(alice, bob.getJid(), new OmemoCachedDeviceList());
        assertEquals("DeviceList must be empty after overwriting it with empty list.", 0,
                store.loadCachedDeviceList(alice, bob.getJid()).getAllDevices().size());
    }

    @Test
    public void loadAllRawSessionsReturnsEmptyMapTest() {
        HashMap<Integer, T_Sess> sessions = store.loadAllRawSessionsOf(alice, bob.getJid());
        assertNotNull(sessions);
        assertEquals(0, sessions.size());
    }

    @Test
    public void loadNonExistentRawSessionReturnsNullTest() {
        T_Sess session = store.loadRawSession(alice, bob);
        assertNull(session);
    }

    @Test
    public void getFingerprint() throws IOException, CorruptedOmemoKeyException {
        assertNull("Method must return null for a non-existent fingerprint.", store.getFingerprint(alice));
        store.storeOmemoIdentityKeyPair(alice, store.generateOmemoIdentityKeyPair());
        OmemoFingerprint fingerprint = store.getFingerprint(alice);
        assertNotNull("fingerprint must not be null", fingerprint);
        assertEquals("Fingerprint must be of length 64", 64, fingerprint.length());

        store.removeOmemoIdentityKeyPair(alice); //clean up
    }

    // ##############################################################
    // Workaround for https://github.com/junit-team/junit4/issues/671

    static TemporaryFolder initStaticTemp() {
        try {
            return new TemporaryFolder() { { before(); } };
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    @AfterClass
    public static void cleanup() throws Exception {
        FileBasedOmemoStore.deleteDirectory(tmp.getRoot());
    }

    // ##############################################################
}
