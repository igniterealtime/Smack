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
package org.jivesoftware.smackx.ciphers;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Arrays;
import java.util.Random;
import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.jivesoftware.smack.test.util.SmackTestSuite;

import org.junit.Test;

public class AesGcmNoPaddingTest extends SmackTestSuite {

    @Test
    public void Aes128Test() throws NoSuchAlgorithmException, NoSuchPaddingException, NoSuchProviderException, InvalidKeyException, InvalidAlgorithmParameterException {
        AesGcmNoPadding aes128 = AesGcmNoPadding.createEncryptionKey(Aes128GcmNoPadding.NAMESPACE);
        assertNotNull(aes128);
        assertEquals(16, aes128.getKey().length);
        assertEquals(12, aes128.getIv().length);
        assertEquals(28, aes128.getKeyAndIv().length);
        assertNotNull(aes128.getCipher());
        assertEquals(128, aes128.getLength());
    }

    @Test
    public void Aes256Test() throws NoSuchAlgorithmException, NoSuchPaddingException, NoSuchProviderException, InvalidKeyException, InvalidAlgorithmParameterException {
        AesGcmNoPadding aes256 = AesGcmNoPadding.createEncryptionKey(Aes256GcmNoPadding.NAMESPACE);
        assertNotNull(aes256);
        assertEquals(32, aes256.getKey().length);
        assertEquals(12, aes256.getIv().length);
        assertEquals(44, aes256.getKeyAndIv().length);
        assertNotNull(aes256.getCipher());
        assertEquals(256, aes256.getLength());
    }

    @Test(expected = NoSuchAlgorithmException.class)
    public void invalidEncryptionCipher() throws NoSuchAlgorithmException, NoSuchPaddingException, NoSuchProviderException, InvalidKeyException, InvalidAlgorithmParameterException {
        AesGcmNoPadding.createEncryptionKey("invalid");
    }

    @Test(expected = NoSuchAlgorithmException.class)
    public void invalidDecryptionCipher() throws NoSuchAlgorithmException, NoSuchPaddingException, NoSuchProviderException, InvalidKeyException, InvalidAlgorithmParameterException {
        AesGcmNoPadding.createDecryptionKey("invalid", null);
    }

    @Test
    public void encryption128Test() throws NoSuchAlgorithmException, NoSuchPaddingException, NoSuchProviderException, InvalidKeyException, InvalidAlgorithmParameterException, BadPaddingException, IllegalBlockSizeException {
        AesGcmNoPadding aes1 = AesGcmNoPadding.createEncryptionKey(Aes128GcmNoPadding.NAMESPACE);
        AesGcmNoPadding aes2 = AesGcmNoPadding.createDecryptionKey(Aes128GcmNoPadding.NAMESPACE, aes1.getKeyAndIv());

        byte[] data = new byte[4096];
        new Random().nextBytes(data);

        byte[] enc = aes1.getCipher().doFinal(data);
        assertFalse(Arrays.equals(data, enc));

        byte[] dec = aes2.getCipher().doFinal(enc);
        assertTrue(Arrays.equals(dec, data));
    }

    @Test
    public void encryption256Test() throws NoSuchAlgorithmException, NoSuchPaddingException, NoSuchProviderException, InvalidKeyException, InvalidAlgorithmParameterException, BadPaddingException, IllegalBlockSizeException {
        AesGcmNoPadding aes1 = AesGcmNoPadding.createEncryptionKey(Aes256GcmNoPadding.NAMESPACE);
        AesGcmNoPadding aes2 = AesGcmNoPadding.createDecryptionKey(Aes256GcmNoPadding.NAMESPACE, aes1.getKeyAndIv());

        byte[] data = new byte[4096];
        new Random().nextBytes(data);

        byte[] enc = aes1.getCipher().doFinal(data);
        assertFalse(Arrays.equals(data, enc));

        byte[] dec = aes2.getCipher().doFinal(enc);
        assertTrue(Arrays.equals(dec, data));
    }
}
