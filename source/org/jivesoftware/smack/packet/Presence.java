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

package org.jivesoftware.smack.packet;

import org.jivesoftware.smack.util.StringUtils;

/**
 * Represents XMPP presence packets. Every presence packet has a type, which is one of
 * the following values:
 * <ul>
 *      <li><tt>Presence.Type.AVAILABLE</tt> -- (Default) indicates the user is available to
 *          receive messages.
 *      <li><tt>Presence.Type.UNAVAILABLE</tt> -- the user is unavailable to receive messages.
 *      <li><tt>Presence.Type.SUBSCRIBE</tt> -- request subscription to recipient's presence.
 *      <li><tt>Presence.Type.SUBSCRIBED</tt> -- grant subscription to sender's presence.
 *      <li><tt>Presence.Type.UNSUBSCRIBE</tt> -- request removal of subscription to sender's
 *          presence.
 *      <li><tt>Presence.Type.UNSUBSCRIBED</tt> -- grant removal of subscription to sender's
 *          presence.
 *      <li><tt>Presence.Type.ERROR</tt> -- the presence packet contains an error message.
 * </ul><p>
 *
 * A number of attributes are optional:
 * <ul>
 *      <li>Status -- free-form text describing a user's presence (i.e., gone to lunch).
 *      <li>Priority -- non-negative numerical priority of a sender's resource. The
 *          highest resource priority is the default recipient of packets not addressed
 *          to a particular resource.
 *      <li>Mode -- one of five presence modes: available (the default), chat, away,
 *          xa (extended away, and dnd (do not disturb).
 * </ul><p>
 *
 * Presence packets are used for two purposes. First, to notify the server of our
 * the clients current presence status. Second, they are used to subscribe and
 * unsubscribe users from the roster.
 *
 * @see RosterPacket
 * @author Matt Tucker
 */
public class Presence extends Packet {

    private Type type = Type.AVAILABLE;
    private String status = null;
    private int priority = -1;
    private Mode mode = Mode.AVAILABLE;

    /**
     * Creates a new presence update. Status, priority, and mode are left un-set.
     *
     * @param type the type.
     */
    public Presence(Type type) {
        this.type = type;
    }

    /**
     * Creates a new presence update with a specified status, priority, and mode.
     *
     * @param type the type.
     * @param status a text message describing the presence update.
     * @param priority the priority of this presence update.
     * @param mode the mode type for this presence update.
     */
    public Presence(Type type, String status, int priority, Mode mode) {
        this.type = type;
        this.status = status;
        this.priority = priority;
        this.mode = mode;
    }

    /**
     * Returns the type of this presence packet.
     *
     * @return the type of the presence packet.
     */
    public Type getType() {
        return type;
    }

    /**
     * Sets the type of the presence packet.
     *
     * @param type the type of the presence packet.
     */
    public void setType(Type type) {
        this.type = type;
    }

    /**
     * Returns the status message of the presence update, or <tt>null</tt> if there
     * is not a status. The status is free-form text describing a user's presence
     * (i.e., "gone to lunch").
     *
     * @return the status message.
     */
    public String getStatus() {
        return status;
    }

    /**
     * Sets the status message of the presence update. The status is free-form text
     * describing a user's presence (i.e., "gone to lunch").
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
     * Sets the priority of the presence. The valid range is -128 through 128.
     *
     * @param priority the priority of the presence.
     * @throws IllegalArgumentException if the priority is outside the valid range.
     */
    public void setPriority(int priority) {
        if (priority < -128 || priority > 128) {
            throw new IllegalArgumentException("Priority value " + priority +
                    " is not valid. Valid range is -128 through 128.");
        }
        this.priority = priority;
    }

    /**
     * Returns the mode of the presence update.
     *
     * @return the mode.
     */
    public Mode getMode() {
        return mode;
    }

    /**
     * Sets the mode of the presence update. For the standard "available" state, set
     * the mode to <tt>null</tt>.
     *
     * @param mode the mode.
     */
    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public String toXML() {
        StringBuffer buf = new StringBuffer();
        buf.append("<presence");
        if (getPacketID() != null) {
            buf.append(" id=\"").append(getPacketID()).append("\"");
        }
        if (getTo() != null) {
            buf.append(" to=\"").append(StringUtils.escapeForXML(getTo())).append("\"");
        }
        if (getFrom() != null) {
            buf.append(" from=\"").append(StringUtils.escapeForXML(getFrom())).append("\"");
        }
        if (type != Type.AVAILABLE) {
            buf.append(" type=\"").append(type).append("\"");
        }
        buf.append(">");
        if (status != null) {
            buf.append("<status>").append(status).append("</status>");
        }
        if (priority != -1) {
            buf.append("<priority>").append(priority).append("</priority>");
        }
        if (mode != null && mode != Mode.AVAILABLE) {
            buf.append("<show>").append(mode).append("</show>");
        }

        buf.append(this.getExtensionsXML());

        buf.append("</presence>");
        
        return buf.toString();
    }

    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append(type);
        if (mode != null) {
            buf.append(": ").append(mode);
        }
        if (status != null) {
            buf.append(" (").append(status).append(")");
        }
        return buf.toString();
    }

    /**
     * A typsafe enum class to represent the presecence type.
     */
    public static class Type {

        public static final Type AVAILABLE = new Type("available");
        public static final Type UNAVAILABLE = new Type("unavailable");
        public static final Type SUBSCRIBE = new Type("subscribe");
        public static final Type SUBSCRIBED = new Type("subscribed");
        public static final Type UNSUBSCRIBE = new Type("unsubscribe");
        public static final Type UNSUBSCRIBED = new Type("unsubscribed");
        public static final Type ERROR = new Type("error");

        private String value;

        private Type(String value) {
            this.value = value;
        }

        public String toString() {
            return value;
        }

        /**
         * Returns the type constant associated with the String value.
         */
        public static Type fromString(String value) {
            if (value == null) {
                return AVAILABLE;
            }
            value = value.toLowerCase();
            if ("unavailable".equals(value)) {
                return UNAVAILABLE;
            }
            else if ("subscribe".equals(value)) {
                return SUBSCRIBE;
            }
            else if ("subscribed".equals(value)) {
                return SUBSCRIBED;
            }
            else if ("unsubscribe".equals(value)) {
                return UNSUBSCRIBE;
            }
            else if ("unsubscribed".equals(value)) {
                return UNSUBSCRIBED;
            }
            else if ("error".equals(value)) {
                return ERROR;
            }
            // Default to available.
            else {
                return AVAILABLE;
            }
        }
    }

    /**
     * A typsafe enum class to represent the presecence mode.
     */
    public static class Mode {

        public static final Mode AVAILABLE = new Mode("available");
        public static final Mode CHAT = new Mode("chat");
        public static final Mode AWAY =  new Mode("away");
        public static final Mode EXTENDED_AWAY = new Mode("xa");
        public static final Mode DO_NOT_DISTURB = new Mode("dnd");
        public static final Mode INVISIBLE = new Mode("invisible");

        private String value;

        private Mode(String value) {
            this.value = value;
        }

        public String toString() {
            return value;
        }

        /**
         * Returns the mode constant associated with the String value.
         */
        public static Mode fromString(String value) {
            if (value == null) {
                return AVAILABLE;
            }
            value = value.toLowerCase();
            if (value.equals("chat")) {
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
                return AVAILABLE;
            }
        }
    }
}
