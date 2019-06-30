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
package org.jivesoftware.smack.util;

import java.security.SecureRandom;

public class RandomUtils {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    /**
     * Generate a securely random byte array.
     *
     * @param len length of the byte array
     * @return byte array
     */
    public static byte[] secureRandomBytes(int len) {
        byte[] bytes = new byte[len];
        SECURE_RANDOM.nextBytes(bytes);
        return bytes;
    }
}
