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
 * Represents XMPP message packets. A message can be one of several types:
 *
 * <ul>
 *      <li>Message.Type.NORMAL -- (Default) a normal text message used in email like interface.
 *      <li>Message.Type.CHAT -- a typically short text message used in line-by-line chat interfaces.
 *      <li>Message.Type.GROUP_CHAT -- a chat message sent to a groupchat server for group chats.
 *      <li>Message.Type.HEADLINE -- a text message to be displayed in scrolling marquee displays.
 *      <li>Message.Type.ERROR -- indicates a messaging error.
 * </ul>
 *
 * For each message type, different message fields are typically used as follows:
 * <p>
 * <table border="1">
 * <tr><td>&nbsp;</td><td colspan="5"><b>Message type</b></td></tr>
 * <tr><td><i>Field</i></td><td><b>Normal</b></td><td><b>Chat</b></td><td><b>Group Chat</b></td><td><b>Headline</b></td><td><b>XMPPError</b></td></tr>
 * <tr><td><i>subject</i></td> <td>SHOULD</td><td>SHOULD NOT</td><td>SHOULD NOT</td><td>SHOULD NOT</td><td>SHOULD NOT</td></tr>
 * <tr><td><i>thread</i></td>  <td>OPTIONAL</td><td>SHOULD</td><td>OPTIONAL</td><td>OPTIONAL</td><td>SHOULD NOT</td></tr>
 * <tr><td><i>body</i></td>    <td>SHOULD</td><td>SHOULD</td><td>SHOULD</td><td>SHOULD</td><td>SHOULD NOT</td></tr>
 * <tr><td><i>error</i></td>   <td>MUST NOT</td><td>MUST NOT</td><td>MUST NOT</td><td>MUST NOT</td><td>MUST</td></tr>
 * </table>
 *
 * @author Matt Tucker
 */
public class Message extends Packet {

    private Type type = Type.NORMAL;
    private String subject = null;
    private String body = null;
    private String thread = null;

    /**
     * Creates a new, "normal" message.
     */
    public Message() {
    }

    /**
     * Creates a new "normal" message to the specified recipient.
     *
     * @param to the recipient of the message.
     */
    public Message(String to) {
        if (to == null) {
            throw new IllegalArgumentException("Parameter cannot be null");
        }
        setTo(to);
    }

    /**
     * Creates a new message of the specified type to a recipient.
     *
     * @param to the user to send the message to.
     * @param type the message type.
     */
    public Message(String to, Type type) {
        if (to == null || type == null) {
            throw new IllegalArgumentException("Parameters cannot be null.");
        }
        setTo(to);
        this.type = type;
    }

    /**
     * Returns the type of the message.
     *
     * @return the type of the message.
     */
    public Type getType() {
        return type;
    }

    /**
     * Sets the type of the message.
     *
     * @param type the type of the message.
     */
    public void setType(Type type) {
        if (type == null) {
            throw new IllegalArgumentException("Type cannot be null.");
        }
        this.type = type;
    }

    /**
     * Returns the subject of the message, or null if the subject has not been set.
     * The subject is a short description of message contents.
     *
     * @return the subject of the message.
     */
    public String getSubject() {
        return subject;
    }

    /**
     * Sets the subject of the message. The subject is a short description of
     * message contents.
     *
     * @param subject the subject of the message.
     */
    public void setSubject(String subject) {
        this.subject = subject;
    }

    /**
     * Returns the body of the message, or null if the body has not been set. The body
     * is the main message contents.
     *
     * @return the body of the message.
     */
    public String getBody() {
        return body;
    }

    /**
     * Sets the body of the message. The body is the main message contents.
     * @param body
     */
    public void setBody(String body) {
        this.body = body;
    }

    /**
     * Returns the thread id of the message, which is a unique identifier for a sequence
     * of "chat" messages. If no thread id is set, <tt>null</tt> will be returned.
     *
     * @return the thread id of the message, or <tt>null</tt> if it doesn't exist.
     */
    public String getThread() {
        return thread;
    }

    /**
     * Sets the thread id of the message, which is a unique identifier for a sequence
     * of "chat" messages.
     *
     * @param thread the thread id of the message.
     */
    public void setThread(String thread) {
        this.thread = thread;
    }

    public String toXML() {
        StringBuilder buf = new StringBuilder();
        buf.append("<message");
        if (getPacketID() != null) {
            buf.append(" id=\"").append(getPacketID()).append("\"");
        }
        if (getTo() != null) {
            buf.append(" to=\"").append(StringUtils.escapeForXML(getTo())).append("\"");
        }
        if (getFrom() != null) {
            buf.append(" from=\"").append(StringUtils.escapeForXML(getFrom())).append("\"");
        }
        if (type != Type.NORMAL) {
            buf.append(" type=\"").append(type).append("\"");
        }
        buf.append(">");
        if (subject != null) {
            buf.append("<subject>").append(StringUtils.escapeForXML(subject)).append("</subject>");
        }
        if (body != null) {
            buf.append("<body>").append(StringUtils.escapeForXML(body)).append("</body>");
        }
        if (thread != null) {
            buf.append("<thread>").append(thread).append("</thread>");
        }
        // Append the error subpacket if the message type is an error.
        if (type == Type.ERROR) {
            XMPPError error = getError();
            if (error != null) {
                buf.append(error.toXML());
            }
        }
        // Add packet extensions, if any are defined.
        buf.append(getExtensionsXML());
        buf.append("</message>");
        return buf.toString();
    }

    /**
     * Represents the type of a message.
     */
    public static class Type {

        /**
         * (Default) a normal text message used in email like interface.
         */
        public static final Type NORMAL = new Type("normal");

        /**
         * Typically short text message used in line-by-line chat interfaces.
         */
        public static final Type CHAT = new Type("chat");

        /**
         * Chat message sent to a groupchat server for group chats.
         */
        public static final Type GROUP_CHAT = new Type("groupchat");

        /**
         * Text message to be displayed in scrolling marquee displays.
         */
        public static final Type HEADLINE = new Type("headline");

        /**
         * indicates a messaging error.
         */
        public static final Type ERROR = new Type("error");

        /**
         * Converts a String value into its Type representation.
         *
         * @param type the String value.
         * @return the Type corresponding to the String.
         */
        public static Type fromString(String type) {
            if (type == null) {
                return NORMAL;
            }
            type = type.toLowerCase();
            if (CHAT.toString().equals(type)) {
                return CHAT;
            }
            else if (GROUP_CHAT.toString().equals(type)) {
                return GROUP_CHAT;
            }
            else if (HEADLINE.toString().equals(type)) {
                return HEADLINE;
            }
            else if (ERROR.toString().equals(type)) {
                return ERROR;
            }
            else {
                return NORMAL;
            }
        }

        private String value;

        private Type(String value) {
            this.value = value;
        }

        public String toString() {
            return value;
        }
    }
}