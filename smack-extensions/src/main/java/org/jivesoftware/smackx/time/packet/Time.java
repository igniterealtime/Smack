/**
 *
 * Copyright 2003-2007 Jive Software, 2014-2025 Florian Schmaus
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

import java.time.ZonedDateTime;
import java.util.Calendar;
import java.util.Date;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.IqData;
import org.jivesoftware.smack.util.Objects;

/**
 * A Time IQ packet, which is used by XMPP clients to exchange their respective local
 * times. Clients that wish to fully support the entity time protocol should register
 * a PacketListener for incoming time requests that then respond with the local time.
 *
 * @see <a href="http://www.xmpp.org/extensions/xep-0202.html">XEP-202</a>
 * @author Florian Schmaus
 */
public class Time extends IQ implements TimeView {

    public static final String NAMESPACE = "urn:xmpp:time";
    public static final String ELEMENT = "time";

    private final ZonedDateTime zonedDateTime;

    @SuppressWarnings("this-escape")
    public Time(TimeBuilder timeBuilder) {
        super(timeBuilder, ELEMENT, NAMESPACE);
        zonedDateTime = timeBuilder.getZonedDateTime();

        Type type = getType();
        switch (type) {
        case get:
            if (zonedDateTime != null) {
                throw new IllegalArgumentException("Time requests must not have time set");
            }
            break;
        case result:
            Objects.requireNonNull(zonedDateTime, "Must have set a time value");
            break;
        case error:
            // Nothing to check.
            break;
        case set:
            throw new IllegalArgumentException("Invalid IQ type");
        }
    }

    @Override
    public ZonedDateTime getZonedDateTime() {
        return zonedDateTime;
    }

    /**
     * Returns the local time or <code>null</code> if the time hasn't been set.
     *
     * @return the local time.
     * @deprecated use {@link #getZonedDateTime()} instead.
     */
    // TODO: Remove in Smack 4.6
    @Deprecated
    public Date getTime() {
        if (zonedDateTime == null) return null;

        return Date.from(zonedDateTime.toInstant());
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder buf) {
        if (zonedDateTime != null) {
            buf.rightAngleBracket();
            buf.element("utc", getUtc());
            buf.element("tzo", getTzo());
        } else {
            buf.setEmptyElement();
        }

        return buf;
    }

    public static TimeBuilder builder(XMPPConnection connection) {
        return new TimeBuilder(connection);
    }

    public static TimeBuilder builder(IqData iqData) {
        return new TimeBuilder(iqData);
    }

    public static TimeBuilder builder(String stanzaId) {
        return new TimeBuilder(stanzaId);
    }

    public static TimeBuilder builder(Time timeRequest, Calendar calendar) {
        IqData iqData = IqData.createResponseData(timeRequest);
        return builder(iqData).setTime(calendar);
    }

    public static TimeBuilder builder(Time timeRequest) {
        return builder(timeRequest, Calendar.getInstance());
    }
}
