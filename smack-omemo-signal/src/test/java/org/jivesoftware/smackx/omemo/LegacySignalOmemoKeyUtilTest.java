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
package org.jivesoftware.smackx.omemo;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertNotSame;
import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;

import org.jivesoftware.smack.test.util.SmackTestSuite;

import org.jivesoftware.smackx.omemo.exceptions.CorruptedOmemoKeyException;
import org.jivesoftware.smackx.omemo.signal.SignalOmemoKeyUtil;

import org.junit.Test;
import org.whispersystems.libsignal.IdentityKey;
import org.whispersystems.libsignal.IdentityKeyPair;
import org.whispersystems.libsignal.state.SignedPreKeyRecord;

/**
 * Test SignalOmemoKeyUtil methods.
 *
 * @author Paul Schaub
 */
public class LegacySignalOmemoKeyUtilTest extends SmackTestSuite {

    private final SignalOmemoKeyUtil keyUtil = new SignalOmemoKeyUtil();

    @Test
    public void omemoIdentityKeyPairSerializationTest() throws CorruptedOmemoKeyException {
        IdentityKeyPair ikp = keyUtil.generateOmemoIdentityKeyPair();
        byte[] bytes = keyUtil.identityKeyPairToBytes(ikp);
        assertNotNull("serialized identityKeyPair must not be null.",
                bytes);
        assertNotSame("serialized identityKeyPair must not be of length 0.",
                0, bytes.length);

        IdentityKeyPair ikp2 = keyUtil.identityKeyPairFromBytes(bytes);
        assertTrue("Deserialized IdentityKeyPairs PublicKey must equal the originals one.",
                ikp.getPublicKey().equals(ikp2.getPublicKey()));
    }

    @Test
    public void omemoIdentityKeySerializationTest() {
        IdentityKey k = keyUtil.generateOmemoIdentityKeyPair().getPublicKey();

        try {
            assertEquals("Deserialized IdentityKey must equal the original one.",
                    k, keyUtil.identityKeyFromBytes(keyUtil.identityKeyToBytes(k)));
        } catch (CorruptedOmemoKeyException e) {
            fail("Caught exception while serializing and deserializing identityKey (" + e + "): " + e.getMessage());
        }
    }

    @Test
    public void generateOmemoSignedPreKeyTest() {
        IdentityKeyPair ikp = keyUtil.generateOmemoIdentityKeyPair();
        try {
            SignedPreKeyRecord spk = keyUtil.generateOmemoSignedPreKey(ikp, 1);
            assertNotNull("SignedPreKey must not be null.", spk);
            assertEquals("SignedPreKeyId must match.", 1, spk.getId());
            assertEquals("singedPreKeyId must match here also.", 1, keyUtil.signedPreKeyIdFromKey(spk));
        } catch (CorruptedOmemoKeyException e) {
            fail("Caught an exception while generating signedPreKey (" + e + "): " + e.getMessage());
        }
    }

    @Test
    public void getFingerprintTest() {
        IdentityKeyPair ikp = keyUtil.generateOmemoIdentityKeyPair();
        IdentityKey ik = ikp.getPublicKey();
        assertTrue("Length of fingerprint must be 64.",
                keyUtil.getFingerprintOfIdentityKey(ik).length() == 64);
    }
}
