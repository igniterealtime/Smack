/**
 *
 * Copyright 2003-2007 Jive Software.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

/**
 * A test case for the SHA1 class.
 */
public class SHA1Test  {

    @Test
    public void testHash() {
        // Test null
        // @TODO - should the StringUtils.hash(String) method be fixed to handle null input?
        try {
            SHA1.hex((String) null);
            fail();
        }
        catch (NullPointerException npe) {
            assertTrue(true);
        }

        // Test empty String
        String result = SHA1.hex("");
        assertEquals("da39a3ee5e6b4b0d3255bfef95601890afd80709", result);

        // Test a known hash
        String adminInHash = "d033e22ae348aeb5660fc2140aec35850c4da997";
        result = SHA1.hex("admin");
        assertEquals(adminInHash, result);

        // Test a random String - make sure all resulting characters are valid hash characters
        // and that the returned string is 32 characters long.
        String random = "jive software blah and stuff this is pretty cool";
        result = SHA1.hex(random);
        assertTrue(isValidHash(result));

        // Test junk input:
        String junk = "\n\n\t\b\r!@(!)^(#)@+_-\u2031\u09291\u00A9\u00BD\u0394\u00F8";
        result = SHA1.hex(junk);
        assertTrue(isValidHash(result));
    }

    /* ----- Utility methods and vars ----- */

    private final String HASH_CHARS = "0123456789abcdef";

    /**
     * Returns true if the input string is valid md5 hash, false otherwise.
     */
    private boolean isValidHash(String result) {
        boolean valid = true;
        for (int i=0; i<result.length(); i++) {
            char c = result.charAt(i);
            if (HASH_CHARS.indexOf(c) < 0) {
                valid = false;
            }
        }
        return valid;
    }

}
