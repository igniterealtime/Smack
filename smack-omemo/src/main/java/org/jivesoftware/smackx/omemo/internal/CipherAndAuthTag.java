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
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

/**
 * Encapsulate Cipher and AuthTag.
 *
 * @author Paul Schaub
 */
public class CipherAndAuthTag {
    private final byte[] key, iv, authTag;
    private final boolean wasPreKey;

    public CipherAndAuthTag(byte[] key, byte[] iv, byte[] authTag, boolean wasPreKey) {
        this.authTag = authTag;
        this.key = key;
        this.iv = iv;
        this.wasPreKey = wasPreKey;
    }

    public String decrypt(byte[] ciphertext) throws InvalidKeyException, IllegalBlockSizeException, BadPaddingException,
                    NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException {
        byte[] plaintext = OmemoAesCipher.decryptAesGcmNoPadding(ciphertext, key, iv);
        return new String(plaintext, StandardCharsets.UTF_8);
    }

    public byte[] getAuthTag() {
        if (authTag != null) {
            return authTag.clone();
        }
        return null;
    }

    public byte[] getKey() {
        if (key != null) {
            return key.clone();
        }
        return null;
    }

    public byte[] getIv() {
        if (iv != null) {
            return iv.clone();
        }
        return null;
    }

    public boolean wasPreKeyEncrypted() {
        return wasPreKey;
    }
}
