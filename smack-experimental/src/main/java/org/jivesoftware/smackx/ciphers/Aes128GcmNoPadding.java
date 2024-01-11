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
import javax.crypto.NoSuchPaddingException;

public class Aes128GcmNoPadding extends AesGcmNoPadding {
    public static final String NAMESPACE = "urn:xmpp:ciphers:aes-128-gcm-nopadding:0";

    public Aes128GcmNoPadding(int MODE) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException,
            NoSuchProviderException, InvalidAlgorithmParameterException {
        super(128, MODE);
    }

    public Aes128GcmNoPadding(byte[] keyAndIv, int MODE) throws NoSuchProviderException, InvalidAlgorithmParameterException,
            NoSuchAlgorithmException, InvalidKeyException, NoSuchPaddingException {
        super(AesGcmNoPadding.copyOfRange(keyAndIv, 0, 16), // 16 byte key
                AesGcmNoPadding.copyOfRange(keyAndIv, 16, keyAndIv.length), MODE); // rest (12 byte) IV
    }

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }
}
