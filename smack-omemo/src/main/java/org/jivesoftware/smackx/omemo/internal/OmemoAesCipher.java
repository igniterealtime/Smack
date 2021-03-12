/**
 *
 * Copyright 2017 Paul Schaub, 2019-2021 Florian Schmaus
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
package org.jivesoftware.smackx.omemo.internal;

import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.jivesoftware.smack.util.RandomUtil;
import org.jivesoftware.smackx.omemo.util.OmemoConstants;
import org.jivesoftware.smackx.omemo.util.OmemoMessageBuilder;

public class OmemoAesCipher {

    static {
        byte[] iv = OmemoMessageBuilder.generateIv();
        byte[] key = new byte[16];
        RandomUtil.fillWithSecureRandom(key);

        try {
            encryptAesGcmNoPadding("This is just a test", key, iv);
        } catch (InvalidKeyException | NoSuchAlgorithmException | NoSuchPaddingException
                        | InvalidAlgorithmParameterException | IllegalBlockSizeException | BadPaddingException e) {
            String message = "Unable to perform " + OmemoConstants.Crypto.CIPHERMODE
                            + " operation requires by OMEMO. Ensure that a suitable crypto provider for is available."
                            + " For example Bouncycastle on Android (BouncyCastleProvider)";
            throw new AssertionError(message);
        }
    }

    private enum CipherOpmode {
        encrypt(Cipher.ENCRYPT_MODE),
        decrypt(Cipher.DECRYPT_MODE),
        ;

        public final int opmodeInt;

        CipherOpmode(int opmodeInt) {
            this.opmodeInt = opmodeInt;
        }
    }

    private static byte[] performCipherOperation(CipherOpmode opmode, byte[] input, byte[] key,
                    byte[] initializationVector)
                    throws IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException,
                    NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException {
        SecretKey secretKey = new SecretKeySpec(key, OmemoConstants.Crypto.KEYTYPE);
        IvParameterSpec ivSpec = new IvParameterSpec(initializationVector);

        Cipher cipher = Cipher.getInstance(OmemoConstants.Crypto.CIPHERMODE);
        cipher.init(opmode.opmodeInt, secretKey, ivSpec);

        byte[] ciphertext = cipher.doFinal(input);

        return ciphertext;
    }

    public static byte[] decryptAesGcmNoPadding(byte[] ciphertext, byte[] key, byte[] initializationVector)
                    throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException,
                    NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException {
        return performCipherOperation(CipherOpmode.decrypt, ciphertext, key, initializationVector);
    }

    public static byte[] encryptAesGcmNoPadding(byte[] plaintext, byte[] key, byte[] initializationVector)
                    throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
                    InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
        return performCipherOperation(CipherOpmode.encrypt, plaintext, key, initializationVector);
    }

    public static byte[] encryptAesGcmNoPadding(String plaintext, byte[] key, byte[] initializationVector)
                    throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException,
                    InvalidAlgorithmParameterException, IllegalBlockSizeException, BadPaddingException {
        byte[] plaintextBytes = plaintext.getBytes(StandardCharsets.UTF_8);
        return encryptAesGcmNoPadding(plaintextBytes, key, initializationVector);
    }
}
