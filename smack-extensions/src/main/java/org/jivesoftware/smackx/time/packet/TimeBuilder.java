/**
 *
 * Copyright 2021-2025 Florian Schmaus
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

import java.text.ParseException;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Calendar;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.IqBuilder;
import org.jivesoftware.smack.packet.IqData;

import org.jxmpp.util.XmppDateTime;

public class TimeBuilder extends IqBuilder<TimeBuilder, Time> implements TimeView {

    private ZonedDateTime zonedDateTime;

    TimeBuilder(IqData iqCommon) {
        super(iqCommon);
    }

    TimeBuilder(XMPPConnection connection) {
        super(connection);
    }

    TimeBuilder(String stanzaId) {
        super(stanzaId);
    }

    @Override
    public ZonedDateTime getZonedDateTime() {
        return zonedDateTime;
    }

    public TimeBuilder set(ZonedDateTime zonedDateTime) {
        this.zonedDateTime = zonedDateTime;
        return getThis();
    }

    /**
     * Sets the time using UTC formatted String, in the format CCYY-MM-DDThh:mm:ssZ, and the provided timezone
     * definition in the format (+|-)hh:mm.
     *
     * @param utc the time using a formatted String.
     * @param tzo the time zone definition.
     * @return a reference to this builder.
     * @throws ParseException if the provided string is not parsable (e.g. because it does not follow the expected
     *         format).
     */
    public TimeBuilder setUtcAndTzo(String utc, String tzo) throws ParseException {
        var instant = XmppDateTime.parseDate(utc).toInstant();
        var zoneId = ZoneId.of(tzo);

        zonedDateTime = instant.atZone(zoneId);

        return getThis();
    }

    public TimeBuilder setTime(Calendar calendar) {
        zonedDateTime = ZonedDateTime.ofInstant(calendar.toInstant(), calendar.getTimeZone().toZoneId());

        return getThis();
    }

    @Override
    public Time build() {
        return new Time(this);
    }

    @Override
    public TimeBuilder getThis() {
        return this;
    }
}
