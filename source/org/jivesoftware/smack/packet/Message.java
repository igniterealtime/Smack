/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright 2003-2007 Jive Software.
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

import java.util.*;

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

    private Type type = Type.normal;
    private String subject = null;
    private String thread = null;
    private String language;

    private final Set<Body> bodies = new HashSet<Body>();

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
        setTo(to);
    }

    /**
     * Creates a new message of the specified type to a recipient.
     *
     * @param to the user to send the message to.
     * @param type the message type.
     */
    public Message(String to, Type type) {
        setTo(to);
        this.type = type;
    }

    /**
     * Returns the type of the message. If no type has been set this method will return {@link
     * org.jivesoftware.smack.packet.Message.Type#normal}.
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
     * @throws IllegalArgumentException if null is passed in as the type
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
        return getBody(null);
    }

    /**
     * Returns the body corresponding to the language. If the language is null, the method result
     * will be the same as {@link #getBody()}. Null will be returned if the language does not have
     * a corresponding body.
     *
     * @param language the language of the body to return.
     * @return the body related to the passed in language.
     * @since 3.0.2
     */
    public String getBody(String language) {
        language = parseXMLLang(language);
        for (Body body : bodies) {
            if ((body.langauge == null && language == null)
                    || (body != null && body.langauge.equals(language))) {
                return body.message;
            }
        }
        return null;
    }

    /**
     * Returns a set of all bodies in this Message, including the default message body accessible
     * from {@link #getBody()}.
     *
     * @return a collection of all bodies in this Message.
     * @since 3.0.2
     */
    public Collection<Body> getBodies() {
        return Collections.unmodifiableCollection(bodies);
    }

    /**
     * Sets the body of the message. The body is the main message contents.
     *
     * @param body the body of the message.
     */
    public void setBody(String body) {
        if (body == null) {
            removeBody("");
            return;
        }
        addBody(null, body);
    }

    /**
     * Adds a body with a corresponding language.
     *
     * @param language the language of the body being added.
     * @param body the body being added to the message.
     * @return the new {@link org.jivesoftware.smack.packet.Message.Body}
     * @throws NullPointerException if the body is null, a null pointer exception is thrown
     * @since 3.0.2
     */
    public Body addBody(String language, String body) {
        if (body == null) {
            throw new NullPointerException("Body must be specified");
        }
        language = parseXMLLang(language);

        Body messageBody = new Body(language, body);
        bodies.add(messageBody);
        return messageBody;
    }

    /**
     * Removes the body with the given language from the message.
     *
     * @param language the language of the body which is to be removed
     * @return true if a body was removed and false if it was not.
     */
    public boolean removeBody(String language) {
        language = parseXMLLang(language);

        for (Body body : bodies) {
            if (language.equals(body.langauge)) {
                return bodies.remove(body);
            }
        }
        return false;
    }

    /**
     * Removes the body from the message and returns true if the body was removed.
     *
     * @param body the body being removed from the message.
     * @return true if the body was successfully removed and false if it was not.
     * @since 3.0.2
     */
    public boolean removeBody(Body body) {
        return bodies.remove(body);
    }

    /**
     * Returns all the languages being used for the bodies, not including the default body.
     *
     * @return the languages being used for the bodies.
     * @since 3.0.2
     */
    public Collection<String> getBodyLanguages() {
        List<String> languages = new ArrayList<String>(bodies.size());
        for (Body body : bodies) {
            if (!parseXMLLang(body.langauge).equals(getDefaultLanguage())) {
                languages.add(body.langauge);
            }
        }
        return Collections.unmodifiableCollection(languages);
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

    /**
     * Returns the xml:lang of this Message.
     *
     * @return the xml:lang of this Message.
     * @since 3.0.2
     */
    private String getLanguage() {
        return language;
    }

    /**
     * Sets the xml:lang of this Message.
     *
     * @param language the xml:lang of this Message.
     * @since 3.0.2
     */
    public void setLanguage(String language) {
        this.language = language;
    }

    public String toXML() {
        StringBuilder buf = new StringBuilder();
        buf.append("<message");
        if (getXmlns() != null) {
            buf.append(" xmlns=\"").append(getXmlns()).append("\"");
        }
        if (language != null) {
            buf.append(" xml:lang=\"").append(getLanguage()).append("\"");
        }
        if (getPacketID() != null) {
            buf.append(" id=\"").append(getPacketID()).append("\"");
        }
        if (getTo() != null) {
            buf.append(" to=\"").append(StringUtils.escapeForXML(getTo())).append("\"");
        }
        if (getFrom() != null) {
            buf.append(" from=\"").append(StringUtils.escapeForXML(getFrom())).append("\"");
        }
        if (type != Type.normal) {
            buf.append(" type=\"").append(type).append("\"");
        }
        buf.append(">");
        if (subject != null) {
            buf.append("<subject>").append(StringUtils.escapeForXML(subject)).append("</subject>");
        }
        // Add the body in the default language
        if (getBody() != null) {
            buf.append("<body>").append(StringUtils.escapeForXML(getBody())).append("</body>");
        }
        // Add the bodies in other languages
        for (Body body : getBodies()) {
            // Skip the default language
            if (DEFAULT_LANGUAGE.equals(body.getLanguage()) || body.getLanguage() == null) {
                continue;
            }
            buf.append("<body xml:lang=\"").append(body.getLanguage()).append("\">");
            buf.append(StringUtils.escapeForXML(body.getMessage()));
            buf.append("</body>");
        }
        if (thread != null) {
            buf.append("<thread>").append(thread).append("</thread>");
        }
        // Append the error subpacket if the message type is an error.
        if (type == Type.error) {
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


    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Message message = (Message) o;

        if(!super.equals(message)) { return false; }
        if (bodies.size() != message.bodies.size() || !bodies.containsAll(message.bodies)) {
            return false;
        }
        if (language != null ? !language.equals(message.language) : message.language != null) {
            return false;
        }
        if (subject != null ? !subject.equals(message.subject) : message.subject != null) {
            return false;
        }
        if (thread != null ? !thread.equals(message.thread) : message.thread != null) {
            return false;
        }
        return type == message.type;

    }

    public int hashCode() {
        int result;
        result = (type != null ? type.hashCode() : 0);
        result = 31 * result + (subject != null ? subject.hashCode() : 0);
        result = 31 * result + (thread != null ? thread.hashCode() : 0);
        result = 31 * result + (language != null ? language.hashCode() : 0);
        result = 31 * result + bodies.hashCode();
        return result;
    }

    /**
     * Represents a message body, its language and the content of the message.
     */
    public static class Body {

        private String message;
        private String langauge;

        private Body(String language, String message) {
            if (message == null) {
                throw new NullPointerException("Message cannot be null.");
            }
            this.langauge = language;
            this.message = message;
        }

        /**
         * Returns the language of this message body. If the language is null, then, no language
         * was specified.
         *
         * @return the language of this message body.
         */
        public String getLanguage() {
            if (DEFAULT_LANGUAGE.equals(langauge)) {
                return null;
            }
            else {
                return langauge;
            }
        }

        /**
         * Returns the message content.
         *
         * @return the content of the message.
         */
        public String getMessage() {
            return message;
        }


        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) { return false; }

            Body body = (Body) o;

            if (langauge != null ? !langauge.equals(body.langauge) : body.langauge != null) {
                return false;
            }
            return message.equals(body.message);

        }

        public int hashCode() {
            int result;
            result = message.hashCode();
            result = 31 * result + (langauge != null ? langauge.hashCode() : 0);
            return result;
        }
    }

    /**
     * Represents the type of a message.
     */
    public enum Type {

        /**
         * (Default) a normal text message used in email like interface.
         */
        normal,

        /**
         * Typically short text message used in line-by-line chat interfaces.
         */
        chat,

        /**
         * Chat message sent to a groupchat server for group chats.
         */
        groupchat,

        /**
         * Text message to be displayed in scrolling marquee displays.
         */
        headline,

        /**
         * indicates a messaging error.
         */
        error;

        public static Type fromString(String name) {
            try {
                return Type.valueOf(name);
            }
            catch (Exception e) {
                return normal;
            }
        }

    }
}