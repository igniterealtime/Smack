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

import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.jivesoftware.smack.test.util.TestUtils;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.hashes.element.HashElement;
import org.jivesoftware.smackx.hashes.provider.HashElementProvider;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static org.jivesoftware.smackx.hashes.HashManager.ALGORITHM.SHA_256;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test toXML and parse of HashElement and HashElementProvider.
 */
public class HashElementTest extends SmackTestSuite {

    @Test
    public void stanzaTest() throws Exception {
        String message = "Hello World!";
        HashElement element = HashManager.calculateHashElement(SHA_256, message.getBytes(StringUtils.UTF8));
        String expected = "<hash xmlns='urn:xmpp:hashes:2' algo='sha-256'>f4OxZX/x/FO5LcGBSKHWXfwtSx+j1ncoSt3SABJtkGk=</hash>";
        assertEquals(expected, element.toXML().toString());

        HashElement parsed = new HashElementProvider().parse(TestUtils.getParser(expected));
        assertEquals(expected, parsed.toXML().toString());
        assertEquals(SHA_256, parsed.getAlgorithm());
        assertEquals("f4OxZX/x/FO5LcGBSKHWXfwtSx+j1ncoSt3SABJtkGk=", parsed.getHashB64());
        assertArrayEquals(HashManager.sha_256(message.getBytes(StringUtils.UTF8)), parsed.getHash());

        assertFalse(parsed.equals(expected));
        assertFalse(parsed.equals(null));
        assertEquals(element, parsed);
        assertTrue(element.equals(parsed));

        HashElement other = new HashElement(HashManager.ALGORITHM.SHA_512,
                "861844d6704e8573fec34d967e20bcfef3d424cf48be04e6dc08f2bd58c729743371015ead891cc3cf1c9d34b49264b510751b1ff9e537937bc46b5d6ff4ecc8".getBytes(StringUtils.UTF8));
        assertFalse(element.equals(other));
    }

}
