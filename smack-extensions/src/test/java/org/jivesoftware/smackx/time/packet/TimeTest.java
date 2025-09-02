/**
 *
 * Copyright 2014-2025 Florian Schmaus
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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.TimeZone;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.test.util.SmackTestSuite;

import org.junit.jupiter.api.Test;

public class TimeTest extends SmackTestSuite {

    @Test
    public void parseCurrentTimeTest() {
        var zonedDateTime = ZonedDateTime.parse("2006-12-19T11:58:35-06:00");
        Time time = Time.builder("dummy")
                        .ofType(IQ.Type.result)
                        .set(zonedDateTime)
                        .build();

        var utc = time.getUtc();
        var tzo = time.getTzo();

        assertEquals("2006-12-19T17:58:35Z", utc);
        assertEquals("-06:00", tzo);
    }

    @Test
    public void negativeTimezoneTest() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(TimeZone.getTimeZone("GMT-830"));
        Time time = Time.builder("dummy")
                        .ofType(IQ.Type.result)
                        .setTime(calendar)
                        .build();

        assertEquals("-08:30", time.getTzo());
    }

    @Test
    public void positiveTimezoneTest() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(TimeZone.getTimeZone("GMT+830"));
        Time time = Time.builder("dummy")
                        .ofType(IQ.Type.result)
                        .setTime(calendar)
                        .build();

        assertEquals("+08:30", time.getTzo());
    }
}
