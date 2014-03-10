/**
 *
 * Copyright 2003-2007 Jive Software, 2014 Florian Schmaus
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

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.util.XmppDateTime;

import java.util.Calendar;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A Time IQ packet, which is used by XMPP clients to exchange their respective local
 * times. Clients that wish to fully support the entity time protocol should register
 * a PacketListener for incoming time requests that then respond with the local time.
 *
 * @see <a href="http://www.xmpp.org/extensions/xep-0202.html">XEP-202</a>
 * @author Florian Schmaus
 */
public class Time extends IQ {
    public static final String NAMESPACE = "urn:xmpp:time";
    public static final String ELEMENT = "time";

    private static final Logger LOGGER = Logger.getLogger(Time.class.getName());

    private String utc;
    private String tzo;

    public Time() {
        setType(Type.GET);
    }

    /**
     * Creates a new Time instance using the specified calendar instance as
     * the time value to send.
     *
     * @param cal the time value.
     */
    public Time(Calendar cal) {
        tzo = XmppDateTime.asString(cal.getTimeZone());
        // Convert local time to the UTC time.
        utc = XmppDateTime.formatXEP0082Date(cal.getTime());
    }

    /**
     * Returns the local time or <tt>null</tt> if the time hasn't been set.
     *
     * @return the local time.
     */
    public Date getTime() {
        if (utc == null) {
            return null;
        }
        Date date = null;
        try {
            date = XmppDateTime.parseDate(utc);
        }
        catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting local time", e);
        }
        return date;
    }

    /**
     * Sets the time using the local time.
     *
     * @param time the current local time.
     */
    public void setTime(Date time) {
    }

    /**
     * Returns the time as a UTC formatted String using the format CCYY-MM-DDThh:mm:ssZ.
     *
     * @return the time as a UTC formatted String.
     */
    public String getUtc() {
        return utc;
    }

    /**
     * Sets the time using UTC formatted String in the format CCYY-MM-DDThh:mm:ssZ.
     *
     * @param utc the time using a formatted String.
     */
    public void setUtc(String utc) {
        this.utc = utc;
    }

    /**
     * Returns the time zone.
     *
     * @return the time zone.
     */
    public String getTzo() {
        return tzo;
    }

    /**
     * Sets the time zone offset.
     *
     * @param tzo the time zone offset.
     */
    public void setTzo(String tzo) {
        this.tzo = tzo;
    }

    public static Time createResponse(Packet request) {
        Time time = new Time(Calendar.getInstance());
        time.setType(Type.RESULT);
        time.setTo(request.getFrom());
        return time;
    }

    public String getChildElementXML() {
        StringBuilder buf = new StringBuilder();
        buf.append("<" + ELEMENT +  " xmlns='" + NAMESPACE + "'>");
        if (utc != null) {
            buf.append("<utc>").append(utc).append("</utc>");
            buf.append("<tzo>").append(tzo).append("</tzo>");
        }
        buf.append("</" + ELEMENT + ">");
        return buf.toString();
    }
}
