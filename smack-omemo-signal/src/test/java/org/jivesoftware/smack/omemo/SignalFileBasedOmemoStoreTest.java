/**
 *
 * Copyright 2017 Paul Schaub
 *
 * This file is part of smack-omemo-signal.
 *
 * smack-omemo-signal is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
 */
package org.jivesoftware.smack.omemo;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNull;
import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertArrayEquals;
import static org.powermock.api.mockito.PowerMockito.when;

import java.io.File;
import java.util.Date;

import org.jivesoftware.smackx.omemo.FileBasedOmemoStore;
import org.jivesoftware.smackx.omemo.OmemoConfiguration;
import org.jivesoftware.smackx.omemo.OmemoManager;
import org.jivesoftware.smackx.omemo.exceptions.CorruptedOmemoKeyException;
import org.jivesoftware.smackx.omemo.internal.CachedDeviceList;
import org.jivesoftware.smackx.omemo.internal.OmemoDevice;
import org.jivesoftware.smackx.omemo.signal.SignalFileBasedOmemoStore;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.whispersystems.libsignal.IdentityKey;
import org.whispersystems.libsignal.IdentityKeyPair;
import org.whispersystems.libsignal.state.SignedPreKeyRecord;

/**
 * Test the file-based signalOmemoStore.
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest({OmemoManager.class})
public class SignalFileBasedOmemoStoreTest {

    private static File storePath;
    private static SignalFileBasedOmemoStore omemoStore;
    private static OmemoManager omemoManager;


    private void deleteStore() {
        FileBasedOmemoStore.deleteDirectory(storePath);
    }

    @BeforeClass
    public static void setup() throws XmppStringprepException {
        String userHome = System.getProperty("user.home");
        if (userHome != null) {
            File f = new File(userHome);
            storePath = new File(f, ".config/smack-integration-test/store");
        } else {
            storePath = new File("int_test_omemo_store");
        }

        OmemoConfiguration.setFileBasedOmemoStoreDefaultPath(storePath);
        omemoStore = new SignalFileBasedOmemoStore();

        OmemoDevice device = new OmemoDevice(JidCreate.bareFrom("storeTest@server.tld"), 55155);
        omemoManager = PowerMockito.mock(OmemoManager.class);
        when(omemoManager.getDeviceId()).thenReturn(device.getDeviceId());
        when(omemoManager.getOwnJid()).thenReturn(device.getJid());
        when(omemoManager.getOwnDevice()).thenReturn(device);
    }

    @Before
    public void before() {
        deleteStore();
    }

    @After
    public void after() {
        deleteStore();
    }

    @Test
    public void isFreshInstallationTest() {
        assertTrue(omemoStore.isFreshInstallation(omemoManager));
        omemoStore.storeOmemoIdentityKeyPair(omemoManager, omemoStore.generateOmemoIdentityKeyPair());
        assertFalse(omemoStore.isFreshInstallation(omemoManager));
        omemoStore.purgeOwnDeviceKeys(omemoManager);
        assertTrue(omemoStore.isFreshInstallation(omemoManager));
    }

    @Test
    public void defaultDeviceIdTest() throws XmppStringprepException {
        assertEquals(-1, omemoStore.getDefaultDeviceId(omemoManager.getOwnJid()));
        omemoStore.setDefaultDeviceId(omemoManager.getOwnJid(), 55);
        assertEquals(55, omemoStore.getDefaultDeviceId(omemoManager.getOwnJid()));
        assertEquals(-1, omemoStore.getDefaultDeviceId(JidCreate.bareFrom("randomGuy@server.tld")));
    }

    @Test
    public void cachedDeviceListTest() throws XmppStringprepException {
        OmemoDevice bob = new OmemoDevice(JidCreate.bareFrom("bob@builder.tv"), 666);
        OmemoDevice craig = new OmemoDevice(JidCreate.bareFrom("craig@southpark.tv"), 3333333);

        CachedDeviceList bobsList = new CachedDeviceList();
        assertEquals(0, bobsList.getAllDevices().size());
        bobsList.getActiveDevices().add(bob.getDeviceId());
        bobsList.getActiveDevices().add(777);
        bobsList.getInactiveDevices().add(888);

        CachedDeviceList craigsList = new CachedDeviceList();
        craigsList.addDevice(craig.getDeviceId());

        assertEquals(3, bobsList.getAllDevices().size());
        assertEquals(2, bobsList.getActiveDevices().size());
        assertTrue(bobsList.getInactiveDevices().contains(888));
        assertTrue(bobsList.getActiveDevices().contains(777));
        assertTrue(bobsList.getAllDevices().contains(888));

        assertEquals(0, craigsList.getInactiveDevices().size());
        assertEquals(1, craigsList.getActiveDevices().size());
        assertEquals(1, craigsList.getAllDevices().size());
        assertEquals(craig.getDeviceId(), craigsList.getActiveDevices().iterator().next().intValue());
    }

    @Test
    public void omemoIdentityKeyPairTest() throws CorruptedOmemoKeyException {
        assertNull(omemoStore.loadOmemoIdentityKeyPair(omemoManager));
        omemoStore.storeOmemoIdentityKeyPair(omemoManager, omemoStore.generateOmemoIdentityKeyPair());
        IdentityKeyPair ikp = omemoStore.loadOmemoIdentityKeyPair(omemoManager);
        assertNotNull(ikp);

        assertTrue(omemoStore.keyUtil().getFingerprint(ikp.getPublicKey()).equals(omemoStore.getFingerprint(omemoManager)));
    }

    @Test
    public void signedPreKeyTest() throws CorruptedOmemoKeyException {
        assertEquals(0, omemoStore.loadOmemoSignedPreKeys(omemoManager).size());
        IdentityKeyPair ikp = omemoStore.generateOmemoIdentityKeyPair();
        SignedPreKeyRecord spk = omemoStore.generateOmemoSignedPreKey(ikp, 14);
        omemoStore.storeOmemoSignedPreKey(omemoManager, 14, spk);
        assertEquals(1, omemoStore.loadOmemoSignedPreKeys(omemoManager).size());
        assertNotNull(omemoStore.loadOmemoSignedPreKey(omemoManager, 14));
        assertArrayEquals(spk.serialize(), omemoStore.loadOmemoSignedPreKey(omemoManager, 14).serialize());
        assertNull(omemoStore.loadOmemoSignedPreKey(omemoManager, 13));
        assertEquals(0, omemoStore.loadCurrentSignedPreKeyId(omemoManager));
        omemoStore.storeCurrentSignedPreKeyId(omemoManager, 15);
        assertEquals(15, omemoStore.loadCurrentSignedPreKeyId(omemoManager));
        omemoStore.removeOmemoSignedPreKey(omemoManager, 14);
        assertNull(omemoStore.loadOmemoSignedPreKey(omemoManager, 14));

        assertNull(omemoStore.getDateOfLastSignedPreKeyRenewal(omemoManager));
        Date now = new Date();
        omemoStore.setDateOfLastSignedPreKeyRenewal(omemoManager, now);
        assertEquals(now, omemoStore.getDateOfLastSignedPreKeyRenewal(omemoManager));
    }

    @Test
    public void preKeyTest() {
        assertEquals(0, omemoStore.loadOmemoPreKeys(omemoManager).size());
        assertNull(omemoStore.loadOmemoPreKey(omemoManager, 12));
        omemoStore.storeOmemoPreKeys(omemoManager,
                omemoStore.generateOmemoPreKeys(1, 20));
        assertNotNull(omemoStore.loadOmemoPreKey(omemoManager, 12));
        assertEquals(20, omemoStore.loadOmemoPreKeys(omemoManager).size());
        omemoStore.removeOmemoPreKey(omemoManager, 12);
        assertNull(omemoStore.loadOmemoPreKey(omemoManager, 12));
        assertEquals(19, omemoStore.loadOmemoPreKeys(omemoManager).size());

        assertEquals(0, omemoStore.loadLastPreKeyId(omemoManager));
        omemoStore.storeLastPreKeyId(omemoManager, 35);
        assertEquals(35, omemoStore.loadLastPreKeyId(omemoManager));
    }

    @Test
    public void trustingTest() throws XmppStringprepException, CorruptedOmemoKeyException {
        OmemoDevice bob = new OmemoDevice(JidCreate.bareFrom("bob@builder.tv"), 555);
        IdentityKey bobsKey = omemoStore.generateOmemoIdentityKeyPair().getPublicKey();
        assertFalse(omemoStore.isDecidedOmemoIdentity(omemoManager, bob, bobsKey));
        assertFalse(omemoStore.isTrustedOmemoIdentity(omemoManager, bob, bobsKey));
        omemoStore.trustOmemoIdentity(omemoManager, bob, bobsKey);
        assertTrue(omemoStore.isDecidedOmemoIdentity(omemoManager, bob, omemoStore.keyUtil().getFingerprint(bobsKey)));
        assertTrue(omemoStore.isTrustedOmemoIdentity(omemoManager, bob, omemoStore.keyUtil().getFingerprint(bobsKey)));
        assertNull(omemoStore.loadOmemoIdentityKey(omemoManager, bob));
        omemoStore.storeOmemoIdentityKey(omemoManager, bob, bobsKey);
        assertNotNull(omemoStore.loadOmemoIdentityKey(omemoManager, bob));
        IdentityKey bobsOtherKey = omemoStore.generateOmemoIdentityKeyPair().getPublicKey();
        assertFalse(omemoStore.isTrustedOmemoIdentity(omemoManager, bob, bobsOtherKey));
        assertFalse(omemoStore.isDecidedOmemoIdentity(omemoManager, bob, bobsOtherKey));
        omemoStore.distrustOmemoIdentity(omemoManager, bob, omemoStore.keyUtil().getFingerprint(bobsKey));
        assertTrue(omemoStore.isDecidedOmemoIdentity(omemoManager, bob, bobsKey));
        assertFalse(omemoStore.isTrustedOmemoIdentity(omemoManager, bob, bobsKey));

        assertNull(omemoStore.getDateOfLastReceivedMessage(omemoManager, bob));
        Date now = new Date();
        omemoStore.setDateOfLastReceivedMessage(omemoManager, bob, now);
        assertEquals(now, omemoStore.getDateOfLastReceivedMessage(omemoManager, bob));
    }
}
