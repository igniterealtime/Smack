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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.jivesoftware.smackx.InitExtensions;

import org.junit.jupiter.api.Test;

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
}
