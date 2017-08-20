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

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public abstract class AesGcmNoPadding {

    public static final String keyType = "AES";
    public static final String cipherMode = "AES/GCM/NoPadding";

    private final int length;
    protected final Cipher cipher;
    private final byte[] key, iv, keyAndIv;

    AesGcmNoPadding(int bits, int MODE) throws NoSuchAlgorithmException, NoSuchProviderException,
            NoSuchPaddingException, InvalidAlgorithmParameterException, InvalidKeyException {
        this.length = bits;
        int bytes = bits / 8;

        KeyGenerator keyGenerator = KeyGenerator.getInstance(keyType);
        keyGenerator.init(bits);
        key = keyGenerator.generateKey().getEncoded();

        SecureRandom secureRandom = new SecureRandom();
        iv = new byte[12];
        secureRandom.nextBytes(iv);

        keyAndIv = new byte[bytes + 12];
        System.arraycopy(key, 0, keyAndIv, 0, bytes);
        System.arraycopy(iv, 0, keyAndIv, bytes, 12);

        cipher = Cipher.getInstance(cipherMode, "BC");
        SecretKey keySpec = new SecretKeySpec(key, keyType);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        cipher.init(MODE, keySpec, ivSpec);
    }

    public static AesGcmNoPadding createEncryptionKey(String cipherName)
            throws NoSuchProviderException, InvalidKeyException, NoSuchAlgorithmException, NoSuchPaddingException,
            InvalidAlgorithmParameterException {

        switch (cipherName) {
            case Aes128GcmNoPadding.NAMESPACE:
                return new Aes128GcmNoPadding(Cipher.ENCRYPT_MODE);
            case Aes256GcmNoPadding.NAMESPACE:
                return new Aes256GcmNoPadding(Cipher.ENCRYPT_MODE);
            default: throw new NoSuchAlgorithmException("Invalid cipher.");
        }
    }

    /**
     * Create a new AES key.
     * @param key key
     * @param iv iv
     * @param MODE cipher mode (Cipher.ENCRYPT_MODE / Cipher.DECRYPT_MODE)
     * @throws NoSuchPaddingException
     * @throws NoSuchAlgorithmException
     * @throws NoSuchProviderException
     * @throws InvalidAlgorithmParameterException
     * @throws InvalidKeyException
     */
    public AesGcmNoPadding(byte[] key, byte[] iv, int MODE) throws NoSuchPaddingException, NoSuchAlgorithmException,
            NoSuchProviderException, InvalidAlgorithmParameterException, InvalidKeyException {
        assert iv.length == 12;
        this.length = key.length * 8;
        this.key = key;
        this.iv = iv;

        keyAndIv = new byte[key.length + iv.length];
        System.arraycopy(key, 0, keyAndIv, 0, key.length);
        System.arraycopy(iv, 0, keyAndIv, key.length, iv.length);

        cipher = Cipher.getInstance(cipherMode, "BC");
        SecretKeySpec keySpec = new SecretKeySpec(key, keyType);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        cipher.init(MODE, keySpec, ivSpec);
    }

    public static AesGcmNoPadding createDecryptionKey(String namespace, byte[] serialized)
            throws NoSuchAlgorithmException, InvalidAlgorithmParameterException, NoSuchProviderException,
            InvalidKeyException, NoSuchPaddingException {

        switch (namespace) {
            case Aes128GcmNoPadding.NAMESPACE:
                return new Aes128GcmNoPadding(serialized, Cipher.DECRYPT_MODE);
            case Aes256GcmNoPadding.NAMESPACE:
                return new Aes256GcmNoPadding(serialized, Cipher.DECRYPT_MODE);
            default: throw new NoSuchAlgorithmException("Invalid cipher.");
        }
    }

    public byte[] getKeyAndIv() {
        return keyAndIv.clone();
    }

    public byte[] getKey() {
        return key.clone();
    }

    public byte[] getIv() {
        return iv.clone();
    }

    public int getLength() {
        return length;
    }

    public Cipher getCipher() {
        return cipher;
    }

    public abstract String getNamespace();

    static byte[] copyOfRange(byte[] source, int start, int end) {
        byte[] copy = new byte[end - start];
        System.arraycopy(source, start, copy, 0, end - start);
        return copy;
    }
}
