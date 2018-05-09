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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.jivesoftware.smack.util.Objects;
import org.jivesoftware.smack.util.TypedCloneable;
import org.jivesoftware.smack.util.XmlStringBuilder;

import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;

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
 * <caption>Message Types</caption>
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
public final class Message extends Stanza implements TypedCloneable<Message> {

    public static final String ELEMENT = "message";
    public static final String BODY = "body";

    private Type type;
    private String thread = null;

    private final Set<Subject> subjects = new HashSet<Subject>();

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
    public Message(Jid to) {
        setTo(to);
    }

    /**
     * Creates a new message of the specified type to a recipient.
     *
     * @param to the user to send the message to.
     * @param type the message type.
     */
    public Message(Jid to, Type type) {
        this(to);
        setType(type);
    }

    /**
     * Creates a new message to the specified recipient and with the specified body.
     *
     * @param to the user to send the message to.
     * @param body the body of the message.
     */
    public Message(Jid to, String body) {
        this(to);
        setBody(body);
    }

    /**
     * Creates a new message to the specified recipient and with the specified body.
     *
     * @param to the user to send the message to.
     * @param body the body of the message.
     * @throws XmppStringprepException if 'to' is not a valid XMPP address.
     */
    public Message(String to, String body) throws XmppStringprepException {
        this(JidCreate.from(to), body);
    }

    /**
     * Creates a new message with the specified recipient and extension element.
     *
     * @param to
     * @param extensionElement
     * @since 4.2
     */
    public Message(Jid to, ExtensionElement extensionElement) {
        this(to);
        addExtension(extensionElement);
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
    public Message(Message other) {
        super(other);
        this.type = other.type;
        this.thread = other.thread;
        this.subjects.addAll(other.subjects);
    }

    /**
     * Returns the type of the message. If no type has been set this method will return {@link
     * org.jivesoftware.smack.packet.Message.Type#normal}.
     *
     * @return the type of the message.
     */
    public Type getType() {
        if (type == null) {
            return Type.normal;
        }
        return type;
    }

    /**
     * Sets the type of the message.
     *
     * @param type the type of the message.
     */
    public void setType(Type type) {
        this.type = type;
    }

    /**
     * Returns the default subject of the message, or null if the subject has not been set.
     * The subject is a short description of message contents.
     * <p>
     * The default subject of a message is the subject that corresponds to the message's language.
     * (see {@link #getLanguage()}) or if no language is set to the applications default
     * language (see {@link Stanza#getDefaultLanguage()}).
     *
     * @return the subject of the message.
     */
    public String getSubject() {
        return getSubject(null);
    }

    /**
     * Returns the subject corresponding to the language. If the language is null, the method result
     * will be the same as {@link #getSubject()}. Null will be returned if the language does not have
     * a corresponding subject.
     *
     * @param language the language of the subject to return.
     * @return the subject related to the passed in language.
     */
    public String getSubject(String language) {
        Subject subject = getMessageSubject(language);
        return subject == null ? null : subject.subject;
    }

    private Subject getMessageSubject(String language) {
        language = determineLanguage(language);
        for (Subject subject : subjects) {
            if (Objects.equals(language, subject.language)) {
                return subject;
            }
        }
        return null;
    }

    /**
     * Returns a set of all subjects in this Message, including the default message subject accessible
     * from {@link #getSubject()}.
     *
     * @return a collection of all subjects in this message.
     */
    public Set<Subject> getSubjects() {
        return Collections.unmodifiableSet(subjects);
    }

    /**
     * Sets the subject of the message. The subject is a short description of
     * message contents.
     *
     * @param subject the subject of the message.
     */
    public void setSubject(String subject) {
        if (subject == null) {
            removeSubject(""); // use empty string because #removeSubject(null) is ambiguous 
            return;
        }
        addSubject(null, subject);
    }

    /**
     * Adds a subject with a corresponding language.
     *
     * @param language the language of the subject being added.
     * @param subject the subject being added to the message.
     * @return the new {@link org.jivesoftware.smack.packet.Message.Subject}
     * @throws NullPointerException if the subject is null, a null pointer exception is thrown
     */
    public Subject addSubject(String language, String subject) {
        language = determineLanguage(language);
        Subject messageSubject = new Subject(language, subject);
        subjects.add(messageSubject);
        return messageSubject;
    }

    /**
     * Removes the subject with the given language from the message.
     *
     * @param language the language of the subject which is to be removed
     * @return true if a subject was removed and false if it was not.
     */
    public boolean removeSubject(String language) {
        language = determineLanguage(language);
        for (Subject subject : subjects) {
            if (language.equals(subject.language)) {
                return subjects.remove(subject);
            }
        }
        return false;
    }

    /**
     * Removes the subject from the message and returns true if the subject was removed.
     *
     * @param subject the subject being removed from the message.
     * @return true if the subject was successfully removed and false if it was not.
     */
    public boolean removeSubject(Subject subject) {
        return subjects.remove(subject);
    }

    /**
     * Returns all the languages being used for the subjects, not including the default subject.
     *
     * @return the languages being used for the subjects.
     */
    public List<String> getSubjectLanguages() {
        Subject defaultSubject = getMessageSubject(null);
        List<String> languages = new ArrayList<String>();
        for (Subject subject : subjects) {
            if (!subject.equals(defaultSubject)) {
                languages.add(subject.language);
            }
        }
        return Collections.unmodifiableList(languages);
    }

    /**
     * Returns the default body of the message, or null if the body has not been set. The body
     * is the main message contents.
     * <p>
     * The default body of a message is the body that corresponds to the message's language.
     * (see {@link #getLanguage()}) or if no language is set to the applications default
     * language (see {@link Stanza#getDefaultLanguage()}).
     *
     * @return the body of the message.
     */
    public String getBody() {
        return getBody(language);
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
        Body body = getMessageBody(language);
        return body == null ? null : body.message;
    }

    private Body getMessageBody(String language) {
        language = determineLanguage(language);
        for (Body body : getBodies()) {
            if (Objects.equals(language, body.language) || (language != null && language.equals(this.language) && body.language == null)) {
                return body;
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
    public Set<Body> getBodies() {
        List<ExtensionElement> bodiesList = getExtensions(Body.ELEMENT, Body.NAMESPACE);
        Set<Body> resultSet = new HashSet<>(bodiesList.size());
        for (ExtensionElement extensionElement : bodiesList) {
            Body body = (Body) extensionElement;
            resultSet.add(body);
        }
        return resultSet;
    }

    /**
     * Sets the body of the message.
     *
     * @param body the body of the message.
     * @see #setBody(String)
     * @since 4.2
     */
    public void setBody(CharSequence body) {
        String bodyString;
        if (body != null) {
            bodyString = body.toString();
        } else {
            bodyString = null;
        }
        setBody(bodyString);
    }

    /**
     * Sets the body of the message. The body is the main message contents.
     *
     * @param body the body of the message.
     */
    public void setBody(String body) {
        if (body == null) {
            removeBody(""); // use empty string because #removeBody(null) is ambiguous
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
        language = determineLanguage(language);

        removeBody(language);

        Body messageBody = new Body(language, body);
        addExtension(messageBody);
        return messageBody;
    }

    /**
     * Removes the body with the given language from the message.
     *
     * @param language the language of the body which is to be removed
     * @return true if a body was removed and false if it was not.
     */
    public boolean removeBody(String language) {
        language = determineLanguage(language);
        for (Body body : getBodies()) {
            String bodyLanguage = body.getLanguage();
            if (Objects.equals(bodyLanguage, language)) {
                removeExtension(body);
                return true;
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
        ExtensionElement removedElement = removeExtension(body);
        return removedElement != null;
    }

    /**
     * Returns all the languages being used for the bodies, not including the default body.
     *
     * @return the languages being used for the bodies.
     * @since 3.0.2
     */
    public List<String> getBodyLanguages() {
        Body defaultBody = getMessageBody(null);
        List<String> languages = new ArrayList<String>();
        for (Body body : getBodies()) {
            if (!body.equals(defaultBody)) {
                languages.add(body.language);
            }
        }
        return Collections.unmodifiableList(languages);
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

    private String determineLanguage(String language) {

        // empty string is passed by #setSubject() and #setBody() and is the same as null
        language = "".equals(language) ? null : language;

        // if given language is null check if message language is set
        if (language == null && this.language != null) {
            return this.language;
        }
        return language;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Message Stanza [");
        logCommonAttributes(sb);
        if (type != null) {
            sb.append("type=").append(type).append(',');
        }
        sb.append(']');
        return sb.toString();
    }

    @Override
    public XmlStringBuilder toXML(String enclosingNamespace) {
        XmlStringBuilder buf = new XmlStringBuilder(enclosingNamespace);
        buf.halfOpenElement(ELEMENT);
        enclosingNamespace = addCommonAttributes(buf, enclosingNamespace);
        buf.optAttribute("type", type);
        buf.rightAngleBracket();

        // Add the subject in the default language
        Subject defaultSubject = getMessageSubject(null);
        if (defaultSubject != null) {
            buf.element("subject", defaultSubject.subject);
        }
        // Add the subject in other languages
        for (Subject subject : getSubjects()) {
            // Skip the default language
            if (subject.equals(defaultSubject))
                continue;
            buf.append(subject.toXML(null));
        }
        buf.optElement("thread", thread);
        // Append the error subpacket if the message type is an error.
        if (type == Type.error) {
            appendErrorIfExists(buf);
        }

        // Add extension elements, if any are defined.
        buf.append(getExtensions(), enclosingNamespace);

        buf.closeElement(ELEMENT);
        return buf;
    }

    /**
     * Creates and returns a copy of this message stanza.
     * <p>
     * This does not perform a deep clone, as extension elements are shared between the new and old
     * instance.
     * </p>
     * @return a clone of this message.
     */
    @Override
    public Message clone() {
        return new Message(this);
    }

    /**
     * Represents a message subject, its language and the content of the subject.
     */
    public static final class Subject implements ExtensionElement {

        public static final String ELEMENT = "subject";
        public static final String NAMESPACE = StreamOpen.CLIENT_NAMESPACE;

        private final String subject;
        private final String language;

        private Subject(String language, String subject) {
            if (subject == null) {
                throw new NullPointerException("Subject cannot be null.");
            }
            this.language = language;
            this.subject = subject;
        }

        /**
         * Returns the language of this message subject.
         *
         * @return the language of this message subject.
         */
        public String getLanguage() {
            return language;
        }

        /**
         * Returns the subject content.
         *
         * @return the content of the subject.
         */
        public String getSubject() {
            return subject;
        }


        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            if (language != null) {
                result = prime * result + this.language.hashCode();
            }
            result = prime * result + this.subject.hashCode();
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            Subject other = (Subject) obj;
            // simplified comparison because language and subject are always set
            return this.language.equals(other.language) && this.subject.equals(other.subject);
        }

        @Override
        public String getElementName() {
            return ELEMENT;
        }

        @Override
        public String getNamespace() {
            return NAMESPACE;
        }

        @Override
        public XmlStringBuilder toXML(String enclosingNamespace) {
            XmlStringBuilder xml = new XmlStringBuilder();
            xml.halfOpenElement(getElementName()).optXmlLangAttribute(getLanguage()).rightAngleBracket();
            xml.escape(subject);
            xml.closeElement(getElementName());
            return xml;
        }

    }

    /**
     * Represents a message body, its language and the content of the message.
     */
    public static final class Body implements ExtensionElement {

        public static final String ELEMENT = "body";
        public static final String NAMESPACE = StreamOpen.CLIENT_NAMESPACE;

        enum BodyElementNamespace {
            client(StreamOpen.CLIENT_NAMESPACE),
            server(StreamOpen.SERVER_NAMESPACE),
            ;

            private final String xmlNamespace;

            BodyElementNamespace(String xmlNamespace) {
                this.xmlNamespace = xmlNamespace;
            }

            public String getNamespace() {
                return xmlNamespace;
            }
        }

        private final String message;
        private final String language;
        private final BodyElementNamespace namespace;

        public Body(String language, String message) {
            this(language, message, BodyElementNamespace.client);
        }

        public Body(String language, String message, BodyElementNamespace namespace) {
            if (message == null) {
                throw new NullPointerException("Message cannot be null.");
            }
            this.language = language;
            this.message = message;
            this.namespace = Objects.requireNonNull(namespace);
        }

        /**
         * Returns the language of this message body or {@code null} if the body extension element does not explicitly
         * set a language, but instead inherits it from the outer element (usually a {@link Message} stanza).
         *
         * @return the language of this message body or {@code null}.
         */
        public String getLanguage() {
            return language;
        }

        /**
         * Returns the message content.
         *
         * @return the content of the message.
         */
        public String getMessage() {
            return message;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            if (language != null) {
                result = prime * result + this.language.hashCode();
            }
            result = prime * result + this.message.hashCode();
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            Body other = (Body) obj;
            // simplified comparison because language and message are always set
            return Objects.equals(this.language, other.language) && this.message.equals(other.message);
        }

        @Override
        public String getElementName() {
            return ELEMENT;
        }

        @Override
        public String getNamespace() {
            return namespace.xmlNamespace;
        }

        @Override
        public XmlStringBuilder toXML(String enclosingNamespace) {
            XmlStringBuilder xml = new XmlStringBuilder(this, enclosingNamespace);
            xml.optXmlLangAttribute(getLanguage()).rightAngleBracket();
            xml.escape(message);
            xml.closeElement(getElementName());
            return xml;
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

        /**
         * Converts a String into the corresponding types. Valid String values that can be converted
         * to types are: "normal", "chat", "groupchat", "headline" and "error".
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
}
