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
package org.jivesoftware.smackx.omemo.internal;

import org.jivesoftware.smackx.omemo.exceptions.CryptoFailedException;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

import static org.jivesoftware.smackx.omemo.util.OmemoConstants.Crypto.CIPHERMODE;
import static org.jivesoftware.smackx.omemo.util.OmemoConstants.Crypto.KEYTYPE;
import static org.jivesoftware.smackx.omemo.util.OmemoConstants.Crypto.PROVIDER;

/**
 * Encapsulate Cipher and AuthTag.
 *
 * @author Paul Schaub
 */
public class CipherAndAuthTag {
    private final byte[] key, iv, authTag;

    public CipherAndAuthTag(byte[] key, byte[] iv, byte[] authTag) throws CryptoFailedException {
        this.authTag = authTag;
        this.key = key;
        this.iv = iv;
    }

    public Cipher getCipher() throws CryptoFailedException {

        Cipher cipher;
        try {
            cipher = Cipher.getInstance(CIPHERMODE, PROVIDER);
            SecretKeySpec keySpec = new SecretKeySpec(key, KEYTYPE);
            IvParameterSpec ivSpec = new IvParameterSpec(iv);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
        } catch (NoSuchAlgorithmException | java.security.InvalidKeyException |
                InvalidAlgorithmParameterException |
                NoSuchPaddingException | NoSuchProviderException e) {
            throw new CryptoFailedException(e);
        }

        return cipher;
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
}
