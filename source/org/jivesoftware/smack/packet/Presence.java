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
 *    contact webmaster@coolservlets.com.
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

package org.jivesoftware.smack.packet;

import org.jivesoftware.smack.*;

/**
 * Represents XMPP presence packets. Each packet will either be an "available" or
 * "unavailable" message. A number of attributes are optional:
 * <ul>
 *      <li>Status -- free-form text describing a user's presence (i.e., gone to lunch).
 *      <li>Priority -- non-negative numerical priority of a sender's resource. The
 *          highest resource priority is the default recipient of packets not addressed
 *          to a particular resource.
 *      <li>Mode -- one of four presence modes: chat, away, xa (extended away, and
 *          dnd (do not disturb).
 * </ul>
 *
 * Note: XMPP presence packets are also used to subscribe and unsubscribe users from
 * rosters. However, that functionality is controlled by the Roster object rather
 * than Presence objects.
 *
 * @author Matt Tucker
 */
public class Presence extends Packet {

    public static final Mode CHAT = new Mode("chat");
    public static final Mode AWAY =  new Mode("away");
    public static final Mode EXTENDED_AWAY = new Mode("xa");
    public static final Mode DO_NOT_DISTURB = new Mode("dnd");
    public static final Mode INVISIBLE = new Mode("invisible");

    private boolean available = true;
    private String status = null;
    private int priority = -1;
    private Mode mode = null;

    /**
     * Creates a new presence update. Status, priority, and mode are left un-set.
     *
     * @param available true if this is an "available" presence packet, or false for
     *      "unavailable".
     */
    public Presence(boolean available) {
        this.available = available;
    }

    /**
     * Creates a new presence update with a specified status, priority, and mode.
     *
     * @param available true if this is an "available" presence update, or false for
     *      "unavailable".
     * @param status a text message describing the presence update.
     * @param priority the priority of this presence update.
     * @param mode the mode type for this presence update.
     */
    public Presence(boolean available, String status, int priority, Mode mode) {
        this.available = available;
        this.status = status;
        this.priority = priority;
        this.mode = mode;
    }

    /**
     * Returns true if this is an "available" presence update and false if "unavailable".
     *
     * @return true if an "available" presence update, false otherwise.
     */
    public boolean isAvailable() {
        return available;
    }

    /**
     * Toggles the presence update between "available" and "unavailable".
     *
     * @param available true to make this an "available" presence update, or false
     *      for "unavailable".
     */
    public void setAvailable(boolean available) {
        this.available = available;
    }

    /**
     * Returns the status message of the presence update, or <tt>null</tt> if there
     * is not status. The status is free-form text describing a user's presence
     * (i.e., "gone to lunch").
     *
     * @return the status message.
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the status message of the presence update. The status is free-form text
     * describing a user's presence (i.e., gone to lunch).
     *
     * @param status the status message.
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * Returns the priority of the presence, or -1 if no priority has been set.
     *
     * @return the priority.
     */
    public int getPriority() {
        return priority;
    }

    /**
     * Sets the priority of the presence.
     *
     * @param priority the priority of the presence.
     */
    public void setPriority(int priority) {
        this.priority = priority;
    }

    /**
     * Returns the mode of the presence update, or <tt>null</tt> if no mode has been set.
     *
     * @return the mode.
     */
    public Mode getMode() {
        return mode;
    }

    /**
     * Sets the mode of the presence update.
     *
     * @param mode the mode.
     */
    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public String toXML() {
        StringBuffer buf = new StringBuffer();
        buf.append("<presence ");
        if (getPacketID() != null) {
            buf.append("id=\"").append(getPacketID()).append("\" ");
        }
        if (getTo() != null) {
            buf.append("to=\"").append(getTo()).append("\" ");
        }
        if (getFrom() != null) {
            buf.append("from=\"").append(getFrom()).append("\" ");
        }
        if (available) {
            buf.append("type=\"available\">");
        }
        else {
            buf.append("type=\"unavailable\">");
        }
        if (status != null) {
            buf.append("<status>").append(status).append("</status>");
        }
        if (priority != -1) {
            buf.append("<priority>").append(priority).append("</priority>");
        }
        if (mode != null) {
            buf.append("<show>").append(mode).append("</show>");
        }
        buf.append("</presence>");
        return buf.toString();
    }

    /**
     * A typsafe enum class to represent the presecence mode.
     */
    public static class Mode {

        private String value;

        private Mode(String value) {
            this.value = value;
        }

        public String toString() {
            return value;
        }

        /**
         * Returns the Mode constant
         */
        public static Mode fromString(String value) {
            if (value == null) {
                return null;
            }
            else if (value.equals("chat")) {
                return CHAT;
            }
            else if (value.equals("away")) {
                return AWAY;
            }
            else if (value.equals("xa")) {
                return EXTENDED_AWAY;
            }
            else if (value.equals("dnd")) {
                return DO_NOT_DISTURB;
            }
            else if (value.equals("invisible")) {
                return INVISIBLE;
            }
            else {
                return null;
            }
        }
    }
}
