/**
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

    private static Mac HMAC_SHA1;

    static {
        try {
            HMAC_SHA1 = Mac.getInstance(HMACSHA1);
        }
        catch (NoSuchAlgorithmException e) {
            // Smack wont be able to function normally if this exception is thrown, wrap it into
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


}
