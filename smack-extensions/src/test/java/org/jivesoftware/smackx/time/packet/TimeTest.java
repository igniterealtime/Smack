/**
 *
 * Copyright 2014 Florian Schmaus
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
package org.jivesoftware.smackx.time.packet;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.jivesoftware.smack.DummyConnection;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.test.util.TestUtils;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smackx.InitExtensions;
import org.junit.Test;

public class TimeTest extends InitExtensions {

    @Test
    public void parseCurrentTimeTest() {
        Calendar calendar = Calendar.getInstance();
        Time time = new Time(calendar);

        Date date = time.getTime();
        Date calendarDate = calendar.getTime();

        assertEquals(calendarDate, date);
    }

    @Test
    public void negativeTimezoneTest() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(TimeZone.getTimeZone("GMT-830"));
        Time time = new Time(calendar);

        assertEquals("-8:30", time.getTzo());
    }

    @Test
    public void positiveTimezoneTest() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(TimeZone.getTimeZone("GMT+830"));
        Time time = new Time(calendar);

        assertEquals("+8:30", time.getTzo());
    }

    @Test
    public void parseTimeWithIntrospectionTest() throws Exception {
        DummyConnection connection = new DummyConnection();

        // @formatter:off
        final String request =
        "<iq type='get'"
          + "from='romeo@montague.net/orchard'"
          + "to='juliet@capulet.com/balcony'"
          + "id='time_1'>"
          + "<time xmlns='urn:xmpp:time'/>"
          + "</iq>";
        // @formatter:on
        IQ iqRequest = PacketParserUtils.parseIQ(TestUtils.getIQParser(request), connection);
        assertTrue(iqRequest instanceof Time);

        // @formatter:off
        final String response =
        "<iq type='result'"
          + "from='juliet@capulet.com/balcony'"
          + "to='romeo@montague.net/orchard'"
          + "id='time_1'>"
          + "<time xmlns='urn:xmpp:time'>"
          + "<tzo>-06:00</tzo>"
          + "<utc>2006-12-19T17:58:35Z</utc>"
          + "</time>"
          + "</iq>";
        // @formatter:on
        IQ iqResponse = PacketParserUtils.parseIQ(TestUtils.getIQParser(response), connection);
        assertTrue(iqResponse instanceof Time);
        Time time = (Time) iqResponse;
        assertEquals("-06:00", time.getTzo());
        assertEquals("2006-12-19T17:58:35Z", time.getUtc());
    }
}
