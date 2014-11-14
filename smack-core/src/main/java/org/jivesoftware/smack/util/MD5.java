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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class MD5 {

    /**
     * Used by the hash method.
     */
    private static MessageDigest MD5_DIGEST;

    static {
        try {
            MD5_DIGEST = MessageDigest.getInstance(StringUtils.MD5);
        }
        catch (NoSuchAlgorithmException e) {
            // Smack wont be able to function normally if this exception is thrown, wrap it into
            // an ISE and make the user aware of the problem.
            throw new IllegalStateException(e);
        }
    }

    public static synchronized byte[] bytes(byte[] bytes) {
        return MD5_DIGEST.digest(bytes);
    }

    public static byte[] bytes(String string) {
        return bytes(StringUtils.toBytes(string));
    }

    public static String hex(byte[] bytes) {
        return StringUtils.encodeHex(bytes(bytes));
    }

    public static String hex(String string) {
        return hex(StringUtils.toBytes(string));
    }

}
