/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright 2003-2004 Jive Software.
 *
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
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

package org.jivesoftware.smackx.packet;

import org.jivesoftware.smack.packet.PacketExtension;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Represents timestamp information about data stored for later delivery. A DelayInformation will 
 * always includes the timestamp when the packet was originally sent and may include more 
 * information such as the JID of the entity that originally sent the packet as well as the reason
 * for the dealy.<p>
 * 
 * For more information see <a href="http://www.jabber.org/jeps/jep-0091.html">JEP-91</a>.
 * 
 * @author Gaston Dombiak
 */
public class DelayInformation implements PacketExtension {

    public static SimpleDateFormat UTC_FORMAT = new SimpleDateFormat("yyyyMMdd'T'HH:mm:ss");
    /**
     * New date format based on JEP-82 that some clients may use when sending delayed dates.
     * JEP-91 is using a SHOULD other servers or clients may be using this format instead of the
     * old UTC format.
     */
    public static SimpleDateFormat NEW_UTC_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");

    static {
        UTC_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT+0"));
        NEW_UTC_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    private Date stamp;
    private String from;
    private String reason;

    /**
     * Creates a new instance with the specified timestamp. 
     */
    public DelayInformation(Date stamp) {
        super();
        this.stamp = stamp;
    }

    /**
     * Returns the JID of the entity that originally sent the packet or that delayed the 
     * delivery of the packet or <tt>null</tt> if this information is not available.
     * 
     * @return the JID of the entity that originally sent the packet or that delayed the 
     *         delivery of the packet.
     */
    public String getFrom() {
        return from;
    }

    /**
     * Sets the JID of the entity that originally sent the packet or that delayed the 
     * delivery of the packet or <tt>null</tt> if this information is not available.
     * 
     * @param from the JID of the entity that originally sent the packet.
     */
    public void setFrom(String from) {
        this.from = from;
    }

    /**
     * Returns the timstamp when the packet was originally sent. The returned Date is 
     * be understood as UTC.
     * 
     * @return the timstamp when the packet was originally sent.
     */
    public Date getStamp() {
        return stamp;
    }

    /**
     * Returns a natural-language description of the reason for the delay or <tt>null</tt> if 
     * this information is not available.
     * 
     * @return a natural-language description of the reason for the delay or <tt>null</tt>.
     */
    public String getReason() {
        return reason;
    }

    /**
     * Sets a natural-language description of the reason for the delay or <tt>null</tt> if 
     * this information is not available.
     * 
     * @param reason a natural-language description of the reason for the delay or <tt>null</tt>.
     */
    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getElementName() {
        return "x";
    }

    public String getNamespace() {
        return "jabber:x:delay";
    }

    public String toXML() {
        StringBuilder buf = new StringBuilder();
        buf.append("<").append(getElementName()).append(" xmlns=\"").append(getNamespace()).append(
                "\"");
        buf.append(" stamp=\"");
        synchronized (UTC_FORMAT) {
            buf.append(UTC_FORMAT.format(stamp));
        }
        buf.append("\"");
        if (from != null && from.length() > 0) {
            buf.append(" from=\"").append(from).append("\"");
        }
        buf.append(">");
        if (reason != null && reason.length() > 0) {
            buf.append(reason);
        }
        buf.append("</").append(getElementName()).append(">");
        return buf.toString();
    }

}
