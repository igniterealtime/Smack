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
