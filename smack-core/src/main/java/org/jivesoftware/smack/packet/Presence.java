/**
 *
 * Copyright 2003-2007 Jive Software.
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

package org.jivesoftware.smack.packet;

import java.util.Locale;

import org.jivesoftware.smack.util.Objects;
import org.jivesoftware.smack.util.TypedCloneable;
import org.jivesoftware.smack.util.XmlStringBuilder;

/**
 * Represents XMPP presence packets. Every presence stanza(/packet) has a type, which is one of
 * the following values:
 * <ul>
 *      <li>{@link Presence.Type#available available} -- (Default) indicates the user is available to
 *          receive messages.
 *      <li>{@link Presence.Type#unavailable unavailable} -- the user is unavailable to receive messages.
 *      <li>{@link Presence.Type#subscribe subscribe} -- request subscription to recipient's presence.
 *      <li>{@link Presence.Type#subscribed subscribed} -- grant subscription to sender's presence.
 *      <li>{@link Presence.Type#unsubscribe unsubscribe} -- request removal of subscription to
 *          sender's presence.
 *      <li>{@link Presence.Type#unsubscribed unsubscribed} -- grant removal of subscription to
 *          sender's presence.
 *      <li>{@link Presence.Type#error error} -- the presence stanza(/packet) contains an error message.
 * </ul><p>
 *
 * A number of attributes are optional:
 * <ul>
 *      <li>Status -- free-form text describing a user's presence (i.e., gone to lunch).
 *      <li>Priority -- non-negative numerical priority of a sender's resource. The
 *          highest resource priority is the default recipient of packets not addressed
 *          to a particular resource.
 *      <li>Mode -- one of five presence modes: {@link Mode#available available} (the default),
 *          {@link Mode#chat chat}, {@link Mode#away away}, {@link Mode#xa xa} (extended away), and
 *          {@link Mode#dnd dnd} (do not disturb).
 * </ul><p>
 *
 * Presence packets are used for two purposes. First, to notify the server of
 * the user's current presence status. Second, they are used to subscribe and
 * unsubscribe users from the roster.
 *
 * @author Matt Tucker
 */
public final class Presence extends Stanza implements TypedCloneable<Presence> {

    public static final String ELEMENT = "presence";

    private Type type = Type.available;
    private String status = null;
    private int priority = Integer.MIN_VALUE;
    private Mode mode = null;

    /**
     * Creates a new presence update. Status, priority, and mode are left un-set.
     *
     * @param type the type.
     */
    public Presence(Type type) {
        setType(type);
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
        setType(type);
        setStatus(status);
        setPriority(priority);
        setMode(mode);
    }

    /**
     * Copy constructor.
     * <p>
     * This does not perform a deep clone, as extension elements are shared between the new and old
     * instance.
     * </p>
     *
     * @param other
     */
    public Presence(Presence other) {
        super(other);
        this.type = other.type;
        this.status = other.status;
        this.priority = other.priority;
        this.mode = other.mode;
    }

    /**
     * Returns true if the {@link Type presence type} is available (online) and
     * false if the user is unavailable (offline), or if this is a presence packet
     * involved in a subscription operation. This is a convenience method
     * equivalent to <tt>getType() == Presence.Type.available</tt>. Note that even
     * when the user is available, their presence mode may be {@link Mode#away away},
     * {@link Mode#xa extended away} or {@link Mode#dnd do not disturb}. Use
     * {@link #isAway()} to determine if the user is away.
     *
     * @return true if the presence type is available.
     */
    public boolean isAvailable() {
        return type == Type.available;    
    }

    /**
     * Returns true if the presence type is {@link Type#available available} and the presence
     * mode is {@link Mode#away away}, {@link Mode#xa extended away}, or
     * {@link Mode#dnd do not disturb}. False will be returned when the type or mode
     * is any other value, including when the presence type is unavailable (offline).
     * This is a convenience method equivalent to
     * <tt>type == Type.available && (mode == Mode.away || mode == Mode.xa || mode == Mode.dnd)</tt>.
     *
     * @return true if the presence type is available and the presence mode is away, xa, or dnd.
     */
    public boolean isAway() {
        return type == Type.available && (mode == Mode.away || mode == Mode.xa || mode == Mode.dnd); 
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
        this.type = Objects.requireNonNull(type, "Type cannot be null");
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
     * Returns the priority of the presence, or Integer.MIN_VALUE if no priority has been set.
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
        if (mode == null) {
            return Mode.available;
        }
        return mode;
    }

    /**
     * Sets the mode of the presence update. A null presence mode value is interpreted
     * to be the same thing as {@link Presence.Mode#available}.
     *
     * @param mode the mode.
     */
    public void setMode(Mode mode) {
        this.mode = mode;
    }

    @Override
    public XmlStringBuilder toXML() {
        XmlStringBuilder buf = new XmlStringBuilder();
        buf.halfOpenElement(ELEMENT);
        addCommonAttributes(buf);
        if (type != Type.available) {
            buf.attribute("type", type);
        }
        buf.rightAngleBracket();

        buf.optElement("status", status);
        if (priority != Integer.MIN_VALUE) {
            buf.element("priority", Integer.toString(priority));
        }
        if (mode != null && mode != Mode.available) {
            buf.element("show", mode);
        }
        buf.append(getExtensionsXML());

        // Add the error sub-packet, if there is one.
        appendErrorIfExists(buf);

        buf.closeElement(ELEMENT);

        return buf;
    }

    /**
     * Creates and returns a copy of this presence stanza.
     * <p>
     * This does not perform a deep clone, as extension elements are shared between the new and old
     * instance.
     * </p>
     * @return a clone of this presence.
     */
    @Override
    public Presence clone() {
        return new Presence(this);
    }

    /**
     * An enum to represent the presence type. Note that presence type is often confused
     * with presence mode. Generally, if a user is signed in to a server, they have a presence
     * type of {@link #available available}, even if the mode is {@link Mode#away away},
     * {@link Mode#dnd dnd}, etc. The presence type is only {@link #unavailable unavailable} when
     * the user is signing out of the server.
     */
    public enum Type {

       /**
        * The user is available to receive messages (default).
        */
        available,

        /**
         * The user is unavailable to receive messages.
         */
        unavailable,

        /**
         * Request subscription to recipient's presence.
         */
        subscribe,

        /**
         * Grant subscription to sender's presence.
         */
        subscribed,

        /**
         * Request removal of subscription to sender's presence.
         */
        unsubscribe,

        /**
         * Grant removal of subscription to sender's presence.
         */
        unsubscribed,

        /**
         * The presence stanza(/packet) contains an error message.
         */
        error,

        /**
         * A presence probe as defined in section 4.3 of RFC 6121
         */
        probe,
        ;

        /**
         * Converts a String into the corresponding types. Valid String values that can be converted
         * to types are: "available", "unavailable", "subscribe", "subscribed", "unsubscribe",
         * "unsubscribed" and "error".
         * 
         * @param string the String value to covert.
         * @return the corresponding Type.
         * @throws IllegalArgumentException when not able to parse the string parameter
         * @throws NullPointerException if the string is null
         */
        public static Type fromString(String string) {
            return Type.valueOf(string.toLowerCase(Locale.US));
        }
    }

    /**
     * An enum to represent the presence mode.
     */
    public enum Mode {

        /**
         * Free to chat.
         */
        chat,

        /**
         * Available (the default).
         */
        available,

        /**
         * Away.
         */
        away,

        /**
         * Away for an extended period of time.
         */
        xa,

        /**
         * Do not disturb.
         */
        dnd;

        /**
         * Converts a String into the corresponding types. Valid String values that can be converted
         * to types are: "chat", "available", "away", "xa", and "dnd".
         * 
         * @param string the String value to covert.
         * @return the corresponding Type.
         * @throws IllegalArgumentException when not able to parse the string parameter
         * @throws NullPointerException if the string is null
         */
        public static Mode fromString(String string) {
            return Mode.valueOf(string.toLowerCase(Locale.US));
        }
    }
}
