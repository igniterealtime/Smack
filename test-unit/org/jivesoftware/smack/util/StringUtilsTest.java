/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright (C) 2002-2003 Jive Software. All rights reserved.
 * ====================================================================
 * The Jive Software License (based on Apache Software License, Version 1.1)
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by
 *        Jive Software (http://www.jivesoftware.com)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Smack" and "Jive Software" must not be used to
 *    endorse or promote products derived from this software without
 *    prior written permission. For written permission, please
 *    contact webmaster@jivesoftware.com.
 *
 * 5. Products derived from this software may not be called "Smack",
 *    nor may "Smack" appear in their name, without prior written
 *    permission of Jive Software.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL JIVE SOFTWARE OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 */

package org.jivesoftware.smack.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

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
        assertEquals("&lt;b&gt;", StringUtils.escapeForXML(input));

        input = "\"";
        assertEquals("&quot;", StringUtils.escapeForXML(input));

        input = "&";
        assertEquals("&amp;", StringUtils.escapeForXML(input));

        input = "<b>\n\t\r</b>";
        assertEquals("&lt;b&gt;\n\t\r&lt;/b&gt;", StringUtils.escapeForXML(input));

        input = "   &   ";
        assertEquals("   &amp;   ", StringUtils.escapeForXML(input));

        input = "   \"   ";
        assertEquals("   &quot;   ", StringUtils.escapeForXML(input));

        input = "> of me <";
        assertEquals("&gt; of me &lt;", StringUtils.escapeForXML(input));

        input = "> of me & you<";
        assertEquals("&gt; of me &amp; you&lt;", StringUtils.escapeForXML(input));

        input = "& <";
        assertEquals("&amp; &lt;", StringUtils.escapeForXML(input));

        input = "&";
        assertEquals("&amp;", StringUtils.escapeForXML(input));
        
        input = "It's a good day today";
        assertEquals("It&apos;s a good day today", StringUtils.escapeForXML(input));
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

    /**
     * This method tests 2 StringUtil methods - encodeBase64(String) and encodeBase64(byte[]).
     */
	@Test
    public void testEncodeBase64() {
        String input = "";
        String output = "";
        assertEquals(StringUtils.encodeBase64(input), output);

        input = "foo bar 123";
        output = "Zm9vIGJhciAxMjM=";
        assertEquals(StringUtils.encodeBase64(input), output);

        input = "=";
        output = "PQ==";
        assertEquals(StringUtils.encodeBase64(input), output);

        input = "abcdefghijklmnopqrstuvwxyz0123456789\n\t\"?!.@{}[]();',./<>#$%^&*";
        output = "YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXowMTIzNDU2Nzg5CgkiPyEuQHt9W10oKTsnLC4vPD4jJCVeJio=";
        assertEquals(StringUtils.encodeBase64(input), output);
    }

    /***
     * This method tests 2 StringUtil methods - decodeBase64(String) and decodeBase64(byte[]).
     */
    /*
    public void testDecodeBase64() {
        String input = "";
        String output = "";
        assertEquals(StringUtils.decodeBase64(input), output);

        input = "Zm9vIGJhciAxMjM=";
        output = "foo bar 123";
        assertEquals(StringUtils.decodeBase64(input), output);

        input = "PQ==";
        output = "=";
        assertEquals(StringUtils.decodeBase64(input), output);

        input = "YWJjZGVmZ2hpamtsbW5vcHFyc3R1dnd4eXowMTIzNDU2Nzg5CgkiPyEuQHt9W10oKTsnLC4vPD4jJCVeJio=";
        output = "abcdefghijklmnopqrstuvwxyz0123456789\n\t\"?!.@{}[]();',./<>#$%^&*";
        assertEquals(StringUtils.decodeBase64(input), output);
    }
    */

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

	@Test
    public void testParsing() {
        String error = "Error parsing node name";
        assertEquals(error, "", StringUtils.parseName("yahoo.myjabber.net"));
        assertEquals(error, "", StringUtils.parseName("yahoo.myjabber.net/registred"));
        assertEquals(error, "user", StringUtils.parseName("user@yahoo.myjabber.net/registred"));
        assertEquals(error, "user", StringUtils.parseName("user@yahoo.myjabber.net"));

        error = "Error parsing server name";
        String result = "yahoo.myjabber.net";
        assertEquals(error, result, StringUtils.parseServer("yahoo.myjabber.net"));
        assertEquals(error, result, StringUtils.parseServer("yahoo.myjabber.net/registred"));
        assertEquals(error, result, StringUtils.parseServer("user@yahoo.myjabber.net/registred"));
        assertEquals(error, result, StringUtils.parseServer("user@yahoo.myjabber.net"));
    }

    @Test
	public void parseXep0082DateProfile() throws Exception
	{
		Date date = StringUtils.parseDate("1971-07-21");
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		cal.setTimeZone(TimeZone.getTimeZone("GMT"));
		assertEquals(1971, cal.get(Calendar.YEAR));
		assertEquals(6, cal.get(Calendar.MONTH));
		assertEquals(21, cal.get(Calendar.DAY_OF_MONTH));
	}
    
    @Test
	public void parseXep0082TimeProfile() throws Exception
	{
		Date date = StringUtils.parseDate("02:56:15");
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		cal.setTimeZone(TimeZone.getTimeZone("GMT"));
		assertEquals(2, cal.get(Calendar.HOUR_OF_DAY));
		assertEquals(56, cal.get(Calendar.MINUTE));
		assertEquals(15, cal.get(Calendar.SECOND));
	}

    @Test
	public void parseXep0082TimeWithMillisProfile() throws Exception
	{
		Date date = StringUtils.parseDate("02:56:15.123");
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		cal.setTimeZone(TimeZone.getTimeZone("GMT"));
		assertEquals(2, cal.get(Calendar.HOUR_OF_DAY));
		assertEquals(56, cal.get(Calendar.MINUTE));
		assertEquals(15, cal.get(Calendar.SECOND));
		assertEquals(123, cal.get(Calendar.MILLISECOND));
	}

    @Test
	public void parseXep0082DateTimeProfile() throws Exception
	{
		Date date = StringUtils.parseDate("1971-07-21T02:56:15Z");
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		cal.setTimeZone(TimeZone.getTimeZone("GMT"));
		assertEquals(1971, cal.get(Calendar.YEAR));
		assertEquals(6, cal.get(Calendar.MONTH));
		assertEquals(21, cal.get(Calendar.DAY_OF_MONTH));
		assertEquals(2, cal.get(Calendar.HOUR_OF_DAY));
		assertEquals(56, cal.get(Calendar.MINUTE));
		assertEquals(15, cal.get(Calendar.SECOND));
	}
    
    @Test
	public void parseXep0082DateTimeProfileWithMillis() throws Exception
	{
		Date date = StringUtils.parseDate("1971-07-21T02:56:15.123Z");
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		cal.setTimeZone(TimeZone.getTimeZone("GMT"));
		assertEquals(1971, cal.get(Calendar.YEAR));
		assertEquals(6, cal.get(Calendar.MONTH));
		assertEquals(21, cal.get(Calendar.DAY_OF_MONTH));
		assertEquals(2, cal.get(Calendar.HOUR_OF_DAY));
		assertEquals(56, cal.get(Calendar.MINUTE));
		assertEquals(15, cal.get(Calendar.SECOND));
		assertEquals(123, cal.get(Calendar.MILLISECOND));
	}
    
    @Test
	public void parseXep0091() throws Exception
	{
		Date date = StringUtils.parseDate("20020910T23:08:25");
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		cal.setTimeZone(TimeZone.getTimeZone("GMT"));
		assertEquals(2002, cal.get(Calendar.YEAR));
		assertEquals(8, cal.get(Calendar.MONTH));
		assertEquals(10, cal.get(Calendar.DAY_OF_MONTH));
		assertEquals(23, cal.get(Calendar.HOUR_OF_DAY));
		assertEquals(8, cal.get(Calendar.MINUTE));
		assertEquals(25, cal.get(Calendar.SECOND));
	}

    @Test
	public void parseXep0091NoLeading0() throws Exception
	{
		Date date = StringUtils.parseDate("200291T23:08:25");
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		cal.setTimeZone(TimeZone.getTimeZone("GMT"));
		assertEquals(2002, cal.get(Calendar.YEAR));
		assertEquals(8, cal.get(Calendar.MONTH));
		assertEquals(1, cal.get(Calendar.DAY_OF_MONTH));
		assertEquals(23, cal.get(Calendar.HOUR_OF_DAY));
		assertEquals(8, cal.get(Calendar.MINUTE));
		assertEquals(25, cal.get(Calendar.SECOND));
	}

    @Test
	public void parseXep0091AmbiguousMonthDay() throws Exception
	{
		Date date = StringUtils.parseDate("2002101T23:08:25");
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		cal.setTimeZone(TimeZone.getTimeZone("GMT"));
		assertEquals(2002, cal.get(Calendar.YEAR));
		assertEquals(9, cal.get(Calendar.MONTH));
		assertEquals(1, cal.get(Calendar.DAY_OF_MONTH));
		assertEquals(23, cal.get(Calendar.HOUR_OF_DAY));
		assertEquals(8, cal.get(Calendar.MINUTE));
		assertEquals(25, cal.get(Calendar.SECOND));
	}

    @Test
	public void parseXep0091SingleDigitMonth() throws Exception
	{
		Date date = StringUtils.parseDate("2002130T23:08:25");
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		cal.setTimeZone(TimeZone.getTimeZone("GMT"));
		assertEquals(2002, cal.get(Calendar.YEAR));
		assertEquals(0, cal.get(Calendar.MONTH));
		assertEquals(30, cal.get(Calendar.DAY_OF_MONTH));
		assertEquals(23, cal.get(Calendar.HOUR_OF_DAY));
		assertEquals(8, cal.get(Calendar.MINUTE));
		assertEquals(25, cal.get(Calendar.SECOND));
	}

    @Test (expected=ParseException.class)
	public void parseNoMonthDay() throws Exception
	{
		StringUtils.parseDate("2002T23:08:25");
	}
    
    @Test (expected=ParseException.class)
	public void parseNoYear() throws Exception
	{
		StringUtils.parseDate("130T23:08:25");
	}
}
