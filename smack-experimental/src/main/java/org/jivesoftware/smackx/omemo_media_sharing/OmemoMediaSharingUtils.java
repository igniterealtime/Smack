/**
 *
 * Copyright © 2019 Paul Schaub
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
package org.jivesoftware.smackx.omemo_media_sharing;

import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.jivesoftware.smack.util.RandomUtils;

/**
 * Utility code for XEP-XXXX: OMEMO Media Sharing.
 *
 * @see <a href="https://xmpp.org/extensions/inbox/omemo-media-sharing.html">XEP-XXXX: OMEMO Media Sharing</a>
 */
public class OmemoMediaSharingUtils {

    private static final String KEYTYPE = "AES";
    private static final String CIPHERMODE = "AES/GCM/NoPadding";
    // 256 bit = 32 byte
    private static final int LEN_KEY = 32;
    private static final int LEN_KEY_BITS = LEN_KEY * 8;

    @SuppressWarnings("unused")
    private static final int LEN_IV_12 = 12;
    private static final int LEN_IV_16 = 16;
    // Note: Contrary to what the ProtoXEP states, 16 byte IV length is used in the wild instead of 12.
    // At some point we should switch to 12 bytes though.
    private static final int LEN_IV = LEN_IV_16;

    public static byte[] generateRandomIV() {
        return generateRandomIV(LEN_IV);
    }

    public static byte[] generateRandomIV(int len) {
        return RandomUtils.secureRandomBytes(len);
    }

    /**
     * Generate a random 256 bit AES key.
     *
     * @return encoded AES key
     * @throws NoSuchAlgorithmException if the JVM doesn't provide the given key type.
     */
    public static byte[] generateRandomKey() throws NoSuchAlgorithmException {
        KeyGenerator generator = KeyGenerator.getInstance(KEYTYPE);
        generator.init(LEN_KEY_BITS);
        return generator.generateKey().getEncoded();
    }

    /**
     * Create a {@link Cipher} from a given key and iv which is in encryption mode.
     *
     * @param key aes encryption key
     * @param iv initialization vector
     *
     * @return cipher in encryption mode
     *
     * @throws NoSuchPaddingException if the JVM doesn't provide the padding specified in the ciphermode.
     * @throws NoSuchAlgorithmException if the JVM doesn't provide the encryption method specified in the ciphermode.
     * @throws InvalidAlgorithmParameterException if the cipher cannot be initiated.
     * @throws InvalidKeyException if the key is invalid.
     */
    public static Cipher encryptionCipherFrom(byte[] key, byte[] iv)
            throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException,
            InvalidKeyException {
        SecretKey secretKey = new SecretKeySpec(key, KEYTYPE);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        Cipher cipher = Cipher.getInstance(CIPHERMODE);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
        return cipher;
    }

    /**
     * Create a {@link Cipher} from a given key and iv which is in decryption mode.
     *
     * @param key aes encryption key
     * @param iv initialization vector
     *
     * @return cipher in decryption mode
     *
     * @throws NoSuchPaddingException if the JVM doesn't provide the padding specified in the ciphermode.
     * @throws NoSuchAlgorithmException if the JVM doesn't provide the encryption method specified in the ciphermode.
     * @throws InvalidAlgorithmParameterException if the cipher cannot be initiated.
     * @throws InvalidKeyException if the key is invalid.
     */
    public static Cipher decryptionCipherFrom(byte[] key, byte[] iv)
            throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException,
            InvalidKeyException {
        SecretKey secretKey = new SecretKeySpec(key, KEYTYPE);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        Cipher cipher = Cipher.getInstance(CIPHERMODE);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);
        return cipher;
    }
}
