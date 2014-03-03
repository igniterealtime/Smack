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

import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.junit.Test;

public class XmppDateTimeTest {
    @Test
    public void parseXep0082Date() throws Exception {
        Date date = XmppDateTime.parseDate("1971-07-21");
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.setTimeZone(TimeZone.getTimeZone("GMT"));
        assertEquals(1971, cal.get(Calendar.YEAR));
        assertEquals(6, cal.get(Calendar.MONTH));
        assertEquals(21, cal.get(Calendar.DAY_OF_MONTH));
    }

    @Test
    public void parseXep0082Time() throws Exception {
        Date date = XmppDateTime.parseDate("02:56:15");
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.setTimeZone(TimeZone.getTimeZone("GMT"));
        assertEquals(2, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(56, cal.get(Calendar.MINUTE));
        assertEquals(15, cal.get(Calendar.SECOND));
    }

    @Test
    public void parseXep0082TimeUTC() throws Exception {
        Date date = XmppDateTime.parseDate("02:56:15Z");
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.setTimeZone(TimeZone.getTimeZone("GMT"));
        assertEquals(2, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(56, cal.get(Calendar.MINUTE));
        assertEquals(15, cal.get(Calendar.SECOND));
    }

    @Test
    public void parseXep0082TimeWithZone() throws Exception {
        Date date = XmppDateTime.parseDate("04:40:15+02:30");
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.setTimeZone(TimeZone.getTimeZone("GMT"));
        assertEquals(2, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(10, cal.get(Calendar.MINUTE));
        assertEquals(15, cal.get(Calendar.SECOND));
    }

    @Test
    public void parseXep0082TimeWithMillis() throws Exception {
        Date date = XmppDateTime.parseDate("02:56:15.123");
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.setTimeZone(TimeZone.getTimeZone("GMT"));
        assertEquals(2, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(56, cal.get(Calendar.MINUTE));
        assertEquals(15, cal.get(Calendar.SECOND));
        assertEquals(123, cal.get(Calendar.MILLISECOND));
    }

    @Test
    public void parseXep0082TimeWithMillisUTC() throws Exception {
        Date date = XmppDateTime.parseDate("02:56:15.123Z");
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.setTimeZone(TimeZone.getTimeZone("GMT"));
        assertEquals(2, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(56, cal.get(Calendar.MINUTE));
        assertEquals(15, cal.get(Calendar.SECOND));
        assertEquals(123, cal.get(Calendar.MILLISECOND));
    }

    @Test
    public void parseXep0082TimeWithMillisZone() throws Exception {
        Date date = XmppDateTime.parseDate("02:56:15.123+01:00");
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.setTimeZone(TimeZone.getTimeZone("GMT"));
        assertEquals(1, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(56, cal.get(Calendar.MINUTE));
        assertEquals(15, cal.get(Calendar.SECOND));
        assertEquals(123, cal.get(Calendar.MILLISECOND));
    }

    @Test
    public void parseXep0082DateTimeUTC() throws Exception {
        Date date = XmppDateTime.parseDate("1971-07-21T02:56:15Z");
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
    public void parseXep0082DateTimeZone() throws Exception {
        Date date = XmppDateTime.parseDate("1971-07-21T02:56:15-01:00");
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.setTimeZone(TimeZone.getTimeZone("GMT"));
        assertEquals(1971, cal.get(Calendar.YEAR));
        assertEquals(6, cal.get(Calendar.MONTH));
        assertEquals(21, cal.get(Calendar.DAY_OF_MONTH));
        assertEquals(3, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(56, cal.get(Calendar.MINUTE));
        assertEquals(15, cal.get(Calendar.SECOND));
    }

    @Test
    public void parseXep0082DateTimeWithMillisUTC() throws Exception {
        Date date = XmppDateTime.parseDate("1971-07-21T02:56:15.123Z");
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
    public void parseXep0082DateTimeWithMillisZone() throws Exception {
        Date date = XmppDateTime.parseDate("1971-07-21T02:56:15.123-01:00");
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.setTimeZone(TimeZone.getTimeZone("GMT"));
        assertEquals(1971, cal.get(Calendar.YEAR));
        assertEquals(6, cal.get(Calendar.MONTH));
        assertEquals(21, cal.get(Calendar.DAY_OF_MONTH));
        assertEquals(3, cal.get(Calendar.HOUR_OF_DAY));
        assertEquals(56, cal.get(Calendar.MINUTE));
        assertEquals(15, cal.get(Calendar.SECOND));
        assertEquals(123, cal.get(Calendar.MILLISECOND));
    }

    @Test
    public void parseXep0091() throws Exception {
        Date date = XmppDateTime.parseDate("20020910T23:08:25");
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
    public void parseXep0091NoLeading0() throws Exception {
        Date date = XmppDateTime.parseDate("200291T23:08:25");
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
    public void parseXep0091AmbiguousMonthDay() throws Exception {
        Date date = XmppDateTime.parseDate("2002101T23:08:25");
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
    public void parseXep0091SingleDigitMonth() throws Exception {
        Date date = XmppDateTime.parseDate("2002130T23:08:25");
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

    @Test(expected = ParseException.class)
    public void parseNoMonthDay() throws Exception {
        XmppDateTime.parseDate("2002T23:08:25");
    }

    @Test(expected = ParseException.class)
    public void parseNoYear() throws Exception {
        XmppDateTime.parseDate("130T23:08:25");
    }
}
