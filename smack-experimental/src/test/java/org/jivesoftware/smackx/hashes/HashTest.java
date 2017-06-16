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
package org.jivesoftware.smackx.hashes;

import static junit.framework.TestCase.assertEquals;

import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.junit.Test;

/**
 * Test HashManager functionality.
 * The test sums got calculated using 'echo "Hello World!" | { md5sum, sha1sum, sha224sum, sha256sum, sha384sum, sha512sum,
 * sha3-224sum -l, sha3-256sum -l, sha3-384sum -l, sha3-512sum -l, b2sum -l 160, b2sum -l 256, b2sum -l 384, b2sum -l 512 }
 */
public class HashTest extends SmackTestSuite {

    private static final String testString =     "Hello World!";
    private static final String md5sum =         "ed076287532e86365e841e92bfc50d8c";
    private static final String sha1sum =        "2ef7bde608ce5404e97d5f042f95f89f1c232871";
    private static final String sha224sum =      "4575bb4ec129df6380cedde6d71217fe0536f8ffc4e18bca530a7a1b";
    private static final String sha256sum =      "7f83b1657ff1fc53b92dc18148a1d65dfc2d4b1fa3d677284addd200126d9069";
    private static final String sha384sum =      "bfd76c0ebbd006fee583410547c1887b0292be76d582d96c242d2a792723e3fd6fd061f9d5cfd13b8f961358e6adba4a";
    private static final String sha512sum =      "861844d6704e8573fec34d967e20bcfef3d424cf48be04e6dc08f2bd58c729743371015ead891cc3cf1c9d34b49264b510751b1ff9e537937bc46b5d6ff4ecc8";
    private static final String sha3_224sum =    "716596afadfa17cd1cb35133829a02b03e4eed398ce029ce78a2161d";
    private static final String sha3_256sum =    "d0e47486bbf4c16acac26f8b653592973c1362909f90262877089f9c8a4536af";
    private static final String sha3_384sum =    "f324cbd421326a2abaedf6f395d1a51e189d4a71c755f531289e519f079b224664961e385afcc37da348bd859f34fd1c";
    private static final String sha3_512sum =    "32400b5e89822de254e8d5d94252c52bdcb27a3562ca593e980364d9848b8041b98eabe16c1a6797484941d2376864a1b0e248b0f7af8b1555a778c336a5bf48";
    private static final String b2_160sum =      "e7338d05e5aa2b5e4943389f9475fce2525b92f2";
    private static final String b2_256sum =      "bf56c0728fd4e9cf64bfaf6dabab81554103298cdee5cc4d580433aa25e98b00";
    private static final String b2_384sum =      "53fd759520545fe93270e61bac03b243b686af32ed39a4aa635555be47a89004851d6a13ece00d95b7bdf9910cb71071";
    private static final String b2_512sum =      "54b113f499799d2f3c0711da174e3bc724737ad18f63feb286184f0597e1466436705d6c8e8c7d3d3b88f5a22e83496e0043c44a3c2b1700e0e02259f8ac468e";

    private byte[] array() {
        if (testArray == null) {
            testArray = HashManager.utf8(testString);
        }
        return testArray;
    }

    private byte[] testArray;

    @Test
    public void hashTest() {
        assertEquals(md5sum, HashManager.hex(HashManager.hash(HashManager.ALGORITHM.MD5, array())));
        assertEquals(sha1sum, HashManager.hex(HashManager.hash(HashManager.ALGORITHM.SHA_1, array())));
        assertEquals(sha224sum, HashManager.hex(HashManager.hash(HashManager.ALGORITHM.SHA_224, array())));
        assertEquals(sha256sum, HashManager.hex(HashManager.hash(HashManager.ALGORITHM.SHA_256, array())));
        assertEquals(sha384sum, HashManager.hex(HashManager.hash(HashManager.ALGORITHM.SHA_384, array())));
        assertEquals(sha512sum, HashManager.hex(HashManager.hash(HashManager.ALGORITHM.SHA_512, array())));
        assertEquals(sha3_224sum, HashManager.hex(HashManager.hash(HashManager.ALGORITHM.SHA3_224, array())));
        assertEquals(sha3_256sum, HashManager.hex(HashManager.hash(HashManager.ALGORITHM.SHA3_256, array())));
        assertEquals(sha3_384sum, HashManager.hex(HashManager.hash(HashManager.ALGORITHM.SHA3_384, array())));
        assertEquals(sha3_512sum, HashManager.hex(HashManager.hash(HashManager.ALGORITHM.SHA3_512, array())));
        assertEquals(b2_160sum, HashManager.hex(HashManager.hash(HashManager.ALGORITHM.BLAKE2B160, array())));
        assertEquals(b2_256sum, HashManager.hex(HashManager.hash(HashManager.ALGORITHM.BLAKE2B256, array())));
        assertEquals(b2_384sum, HashManager.hex(HashManager.hash(HashManager.ALGORITHM.BLAKE2B384, array())));
        assertEquals(b2_512sum, HashManager.hex(HashManager.hash(HashManager.ALGORITHM.BLAKE2B512, array())));
    }

    @Test
    public void md5Test() {
        String actual = HashManager.hex(HashManager.md5(array()));
        assertEquals(md5sum, actual);
    }

    @Test
    public void sha1Test() {
        String actual = HashManager.hex(HashManager.sha_1(array()));
        assertEquals(sha1sum, actual);
    }

    @Test
    public void sha224Test() {
        String actual = HashManager.hex(HashManager.sha_224(array()));
        assertEquals(sha224sum, actual);
    }

    @Test
    public void sha256Test() {
        String actual = HashManager.hex(HashManager.sha_256(array()));
        assertEquals(sha256sum, actual);
    }

    @Test
    public void sha384Test() {
        String actual = HashManager.hex(HashManager.sha_384(array()));
        assertEquals(sha384sum, actual);
    }

    @Test
    public void sha512Test() {
        String actual = HashManager.hex(HashManager.sha_512(array()));
        assertEquals(sha512sum, actual);
    }

    @Test
    public void sha3_224Test() {
        String actual = HashManager.hex(HashManager.sha3_224(array()));
        assertEquals(sha3_224sum, actual);
    }

    @Test
    public void sha3_256Test() {
        String actual = HashManager.hex(HashManager.sha3_256(array()));
        assertEquals(sha3_256sum, actual);
    }

    @Test
    public void sha3_384Test() {
        String actual = HashManager.hex(HashManager.sha3_384(array()));
        assertEquals(sha3_384sum, actual);
    }

    @Test
    public void sha3_512Test() {
        String actual = HashManager.hex(HashManager.sha3_512(array()));
        assertEquals(sha3_512sum, actual);
    }

    @Test
    public void blake2b160Test() {
        String actual = HashManager.hex(HashManager.blake2b160(array()));
        assertEquals(b2_160sum, actual);
    }

    @Test
    public void blake2b256Test() {
        String actual = HashManager.hex(HashManager.blake2b256(array()));
        assertEquals(b2_256sum, actual);
    }

    @Test
    public void blake2b384Test() {
        String actual = HashManager.hex(HashManager.blake2b384(array()));
        assertEquals(b2_384sum, actual);
    }

    @Test
    public void blake2b512Test() {
        String actual = HashManager.hex(HashManager.blake2b512(array()));
        assertEquals(b2_512sum, actual);
    }

    @Test
    public void asFeatureTest() {
        assertEquals("urn:xmpp:hash-function-text-names:id-blake2b384", HashManager.asFeature(HashManager.ALGORITHM.BLAKE2B384));
        assertEquals("urn:xmpp:hash-function-text-names:md5", HashManager.asFeature(HashManager.ALGORITHM.MD5));
        assertEquals("urn:xmpp:hash-function-text-names:sha3-512", HashManager.asFeature(HashManager.ALGORITHM.SHA3_512));
        assertEquals("urn:xmpp:hash-function-text-names:sha-512", HashManager.asFeature(HashManager.ALGORITHM.SHA_512));
    }
}
