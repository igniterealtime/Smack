/*
 *
 * Copyright © 2014 Florian Schmaus
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
package org.jivesoftware.smack.util;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public class MAC {

    public static final String HMACSHA1 = "HmacSHA1";
    public static final String HMACSHA256 = "HmacSHA256";
    public static final String HMACSHA512 = "HmacSHA512";

    private static Mac HMAC_SHA1;
    private static Mac HMAC_SHA256;
    private static Mac HMAC_SHA512;

    static {
        try {
            HMAC_SHA1 = Mac.getInstance(HMACSHA1);
            HMAC_SHA256 = Mac.getInstance(HMACSHA256);
            HMAC_SHA512 = Mac.getInstance(HMACSHA512);
        }
        catch (NoSuchAlgorithmException e) {
            // Smack won't be able to function normally if this exception is thrown, wrap it into
            // an ISE and make the user aware of the problem.
            throw new IllegalStateException(e);
        }
    }


    public static synchronized byte[] hmacsha1(SecretKeySpec key, byte[] input) throws InvalidKeyException {
        HMAC_SHA1.init(key);
        return HMAC_SHA1.doFinal(input);
    }

    public static byte[] hmacsha1(byte[] keyBytes, byte[] input) throws InvalidKeyException {
        SecretKeySpec key = new SecretKeySpec(keyBytes, HMACSHA1);
        return hmacsha1(key, input);
    }

    public static synchronized byte[] hmacsha256(SecretKeySpec key, byte[] input) throws InvalidKeyException {
        HMAC_SHA256.init(key);
        return HMAC_SHA256.doFinal(input);
    }

    public static byte[] hmacsha256(byte[] keyBytes, byte[] input) throws InvalidKeyException {
        SecretKeySpec key = new SecretKeySpec(keyBytes, HMACSHA256);
        return hmacsha256(key, input);
    }

    public static synchronized byte[] hmacsha512(SecretKeySpec key, byte[] input) throws InvalidKeyException {
        HMAC_SHA512.init(key);
        return HMAC_SHA512.doFinal(input);
    }

    public static byte[] hmacsha512(byte[] keyBytes, byte[] input) throws InvalidKeyException {
        SecretKeySpec key = new SecretKeySpec(keyBytes, HMACSHA512);
        return hmacsha512(key, input);
    }


}
