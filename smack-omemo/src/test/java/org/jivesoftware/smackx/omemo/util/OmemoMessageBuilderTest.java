/**
 *
 * Copyright the original author or authors
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
package org.jivesoftware.smackx.omemo.util;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Random;

import org.jivesoftware.smack.test.util.SmackTestSuite;

import org.junit.Test;

public class OmemoMessageBuilderTest extends SmackTestSuite {

    private static final byte[] messageKey = new byte[16];
    private static final byte[] cipherTextWithAuthTag = new byte[35 + 16];

    public OmemoMessageBuilderTest() {
        Random random = new Random();
        random.nextBytes(messageKey);
        random.nextBytes(cipherTextWithAuthTag);
    }

    @Test
    public void testMoveAuthTag() {
        // Extract authTag for testing purposes
        byte[] authTag = new byte[16];
        System.arraycopy(cipherTextWithAuthTag, 35, authTag, 0, 16);

        byte[] messageKeyWithAuthTag = new byte[16 + 16];
        byte[] cipherTextWithoutAuthTag = new byte[35];

        OmemoMessageBuilder.moveAuthTag(messageKey, cipherTextWithAuthTag, messageKeyWithAuthTag, cipherTextWithoutAuthTag);

        // Check if first n - 16 bytes of cipherText got copied over to cipherTextWithoutAuthTag correctly
        byte[] checkCipherText = new byte[35];
        System.arraycopy(cipherTextWithAuthTag, 0, checkCipherText, 0, 35);
        assertTrue(Arrays.equals(checkCipherText, cipherTextWithoutAuthTag));

        byte[] checkMessageKey = new byte[16];
        System.arraycopy(messageKeyWithAuthTag, 0, checkMessageKey, 0, 16);
        assertTrue(Arrays.equals(checkMessageKey, messageKey));

        byte[] checkAuthTag = new byte[16];
        System.arraycopy(messageKeyWithAuthTag, 16, checkAuthTag, 0, 16);
        assertTrue(Arrays.equals(checkAuthTag, authTag));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCheckIllegalMessageKeyWithAuthTagLength() {
        byte[] illegalMessageKey = new byte[16 + 15]; // too short
        byte[] cipherTextWithoutAuthTag = new byte[35]; // ok

        OmemoMessageBuilder.moveAuthTag(messageKey, cipherTextWithAuthTag, illegalMessageKey, cipherTextWithoutAuthTag);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testCheckIllegalCipherTextWithoutAuthTagLength() {
        byte[] messageKeyWithAuthTag = new byte[16 + 16]; // ok
        byte[] illegalCipherTextWithoutAuthTag = new byte[39]; // too long

        OmemoMessageBuilder.moveAuthTag(messageKey, cipherTextWithAuthTag, messageKeyWithAuthTag, illegalCipherTextWithoutAuthTag);
    }
}
