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
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;

/**
 * A test case for the StringUtils class.
 */
public class StringUtilsTest  {
	@Test
    public void testEscapeForXML() {
        String input = null;

        assertNull(StringUtils.escapeForXML(null));

        input = "<b>";
        assertCharSequenceEquals("&lt;b&gt;", StringUtils.escapeForXML(input));

        input = "\"";
        assertCharSequenceEquals("&quot;", StringUtils.escapeForXML(input));

        input = "&";
        assertCharSequenceEquals("&amp;", StringUtils.escapeForXML(input));

        input = "<b>\n\t\r</b>";
        assertCharSequenceEquals("&lt;b&gt;\n\t\r&lt;/b&gt;", StringUtils.escapeForXML(input));

        input = "   &   ";
        assertCharSequenceEquals("   &amp;   ", StringUtils.escapeForXML(input));

        input = "   \"   ";
        assertCharSequenceEquals("   &quot;   ", StringUtils.escapeForXML(input));

        input = "> of me <";
        assertCharSequenceEquals("&gt; of me &lt;", StringUtils.escapeForXML(input));

        input = "> of me & you<";
        assertCharSequenceEquals("&gt; of me &amp; you&lt;", StringUtils.escapeForXML(input));

        input = "& <";
        assertCharSequenceEquals("&amp; &lt;", StringUtils.escapeForXML(input));

        input = "&";
        assertCharSequenceEquals("&amp;", StringUtils.escapeForXML(input));
        
        input = "It's a good day today";
        assertCharSequenceEquals("It&apos;s a good day today", StringUtils.escapeForXML(input));
    }

	public static void assertCharSequenceEquals(CharSequence expected, CharSequence actual) {
	    assertEquals(expected.toString(), actual.toString());
	}

	@Test
    public void testHash() {
        // Test null
        // @TODO - should the StringUtils.hash(String) method be fixed to handle null input?
        try {
            StringUtils.hash(null);
            fail();
        }
        catch (NullPointerException npe) {
            assertTrue(true);
        }

        // Test empty String
        String result = StringUtils.hash("");
        assertEquals("da39a3ee5e6b4b0d3255bfef95601890afd80709", result);

        // Test a known hash
        String adminInHash = "d033e22ae348aeb5660fc2140aec35850c4da997";
        result = StringUtils.hash("admin");
        assertEquals(adminInHash, result);

        // Test a random String - make sure all resulting characters are valid hash characters
        // and that the returned string is 32 characters long.
        String random = "jive software blah and stuff this is pretty cool";
        result = StringUtils.hash(random);
        assertTrue(isValidHash(result));

        // Test junk input:
        String junk = "\n\n\t\b\r!@(!)^(#)@+_-\u2031\u09291\u00A9\u00BD\u0394\u00F8";
        result = StringUtils.hash(junk);
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

	@Test
    public void testEncodeHex() {
        String input = "";
        String output = "";
        assertEquals(new String(StringUtils.encodeHex(input.getBytes())),
                new String(output.getBytes()));

        input = "foo bar 123";
        output = "666f6f2062617220313233";
        assertEquals(new String(StringUtils.encodeHex(input.getBytes())),
                new String(output.getBytes()));
    }

	@Test
    public void testRandomString() {
        // Boundary test
        String result = StringUtils.randomString(-1);
        assertNull(result);

        // Zero length string test
        result = StringUtils.randomString(0);
        assertNull(result);

        // Test various lengths - make sure the same length is returned
        result = StringUtils.randomString(4);
        assertTrue(result.length() == 4);
        result = StringUtils.randomString(16);
        assertTrue(result.length() == 16);
        result = StringUtils.randomString(128);
        assertTrue(result.length() == 128);
    }
}
