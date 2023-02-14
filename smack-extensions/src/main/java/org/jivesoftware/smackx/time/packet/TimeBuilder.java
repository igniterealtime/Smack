/**
 *
 * Copyright 2021 Florian Schmaus
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
import java.util.Calendar;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.IqBuilder;
import org.jivesoftware.smack.packet.IqData;
import org.jivesoftware.smack.util.StringUtils;

import org.jxmpp.util.XmppDateTime;

// TODO: Use java.time.ZonedDataTime once Smack's minimum Android SDK API level is 26 or higher.
public class TimeBuilder extends IqBuilder<TimeBuilder, Time> implements TimeView {

    private String utc;
    private String tzo;

    TimeBuilder(IqData iqCommon) {
        super(iqCommon);
    }

    TimeBuilder(XMPPConnection connection) {
        super(connection);
    }

    TimeBuilder(String stanzaId) {
        super(stanzaId);
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
        this.utc = StringUtils.requireNotNullNorEmpty(utc, "Must provide utc argument");
        // Sanity check the provided string.
        XmppDateTime.parseDate(utc);

        this.tzo = StringUtils.requireNotNullNorEmpty(tzo, "Must provide tzo argument");

        return getThis();
    }

    public TimeBuilder setTime(Calendar calendar) {
        // Convert local time to the UTC time.
        utc = XmppDateTime.formatXEP0082Date(calendar.getTime());
        tzo = XmppDateTime.asString(calendar.getTimeZone());

        return getThis();
    }

    @Override
    public String getUtc() {
        return utc;
    }

    @Override
    public String getTzo() {
        return tzo;
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
