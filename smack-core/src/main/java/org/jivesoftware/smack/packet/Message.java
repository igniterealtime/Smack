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

import java.util.List;
import java.util.Locale;

import javax.xml.namespace.QName;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.util.EqualsUtil;
import org.jivesoftware.smack.util.HashCode;
import org.jivesoftware.smack.util.Objects;
import org.jivesoftware.smack.util.StringUtils;
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
public final class Message extends MessageOrPresence<MessageBuilder>
                implements MessageView {

    public static final String ELEMENT = "message";
    public static final String BODY = "body";

    private Type type;

    /**
     * Creates a new, "normal" message.
     * @deprecated use {@link StanzaBuilder}, preferable via {@link StanzaFactory}, instead.
     */
    @Deprecated
    // TODO: Remove in Smack 4.5.
    public Message() {
    }

    /**
     * Creates a new "normal" message to the specified recipient.
     *
     * @param to the recipient of the message.
     * @deprecated use {@link StanzaBuilder}, preferable via {@link StanzaFactory}, instead.
     */
    @Deprecated
    // TODO: Remove in Smack 4.5.
    public Message(Jid to) {
        setTo(to);
    }

    /**
     * Creates a new message of the specified type to a recipient.
     *
     * @param to the user to send the message to.
     * @param type the message type.
     * @deprecated use {@link StanzaBuilder}, preferable via {@link StanzaFactory}, instead.
     */
    @Deprecated
    // TODO: Remove in Smack 4.5.
    public Message(Jid to, Type type) {
        this(to);
        setType(type);
    }

    /**
     * Creates a new message to the specified recipient and with the specified body.
     *
     * @param to the user to send the message to.
     * @param body the body of the message.
     * @deprecated use {@link StanzaBuilder}, preferable via {@link StanzaFactory}, instead.
     */
    @Deprecated
    // TODO: Remove in Smack 4.5.
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
     * @deprecated use {@link StanzaBuilder}, preferable via {@link StanzaFactory}, instead.
     */
    @Deprecated
    // TODO: Remove in Smack 4.5.
    public Message(String to, String body) throws XmppStringprepException {
        this(JidCreate.from(to), body);
    }

    /**
     * Creates a new message with the specified recipient and extension element.
     *
     * @param to TODO javadoc me please
     * @param extensionElement TODO javadoc me please
     * @since 4.2
     * @deprecated use {@link StanzaBuilder}, preferable via {@link StanzaFactory}, instead.
     */
    @Deprecated
    // TODO: Remove in Smack 4.5.
    public Message(Jid to, ExtensionElement extensionElement) {
        this(to);
        addExtension(extensionElement);
    }

    Message(MessageBuilder messageBuilder) {
        super(messageBuilder);
        type = messageBuilder.type;
    }

    /**
     * Copy constructor.
     * <p>
     * This does not perform a deep clone, as extension elements are shared between the new and old
     * instance.
     * </p>
     *
     * @param other TODO javadoc me please
     */
    public Message(Message other) {
        super(other);
        this.type = other.type;
    }

    @Override
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
     * @deprecated use {@link StanzaBuilder} instead.
     */
    @Deprecated
    // TODO: Remove in Smack 4.5.
    public void setType(Type type) {
        this.type = type;
    }

    /**
     * Sets the subject of the message. The subject is a short description of
     * message contents.
     *
     * @param subject the subject of the message.
     * @deprecated use {@link StanzaBuilder} instead.
     */
    @Deprecated
    // TODO: Remove when stanza builder is ready.
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
    @Deprecated
    // TODO: Remove when stanza builder is ready.
    public Subject addSubject(String language, String subject) {
        language = Stanza.determineLanguage(this, language);

        List<Subject> currentSubjects = getExtensions(Subject.class);
        for (Subject currentSubject : currentSubjects) {
            if (language.equals(currentSubject.getLanguage())) {
                throw new IllegalArgumentException("Subject with the language " + language + " already exists");
            }
        }

        Subject messageSubject = new Subject(language, subject);
        addExtension(messageSubject);
        return messageSubject;
    }

    /**
     * Removes the subject with the given language from the message.
     *
     * @param language the language of the subject which is to be removed
     * @return true if a subject was removed and false if it was not.
     */
    @Deprecated
    // TODO: Remove when stanza builder is ready.
    public boolean removeSubject(String language) {
        language = Stanza.determineLanguage(this, language);
        for (Subject subject : getExtensions(Subject.class)) {
            if (language.equals(subject.language)) {
                return removeSubject(subject);
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
    @Deprecated
    // TODO: Remove when stanza builder is ready.
    public boolean removeSubject(Subject subject) {
        return removeExtension(subject) != null;
    }

    /**
     * Sets the body of the message.
     *
     * @param body the body of the message.
     * @see #setBody(String)
     * @since 4.2
     * @deprecated use {@link StanzaBuilder} instead.
     */
    @Deprecated
    // TODO: Remove when stanza builder is ready.
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
     * @deprecated use {@link StanzaBuilder} instead.
     */
    @Deprecated
    // TODO: Remove when stanza builder is ready.
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
     * @deprecated use {@link StanzaBuilder} instead.
     */
    @Deprecated
    // TODO: Remove when stanza builder is ready.
    public Body addBody(String language, String body) {
        language = Stanza.determineLanguage(this, language);

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
     * @deprecated use {@link StanzaBuilder} instead.
     */
    @Deprecated
    // TODO: Remove when stanza builder is ready.
    public boolean removeBody(String language) {
        language = Stanza.determineLanguage(this, language);
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
     * @deprecated use {@link StanzaBuilder} instead.
     */
    @Deprecated
    // TODO: Remove when stanza builder is ready.
    public boolean removeBody(Body body) {
        XmlElement removedElement = removeExtension(body);
        return removedElement != null;
    }

    /**
     * Sets the thread id of the message, which is a unique identifier for a sequence
     * of "chat" messages.
     *
     * @param thread the thread id of the message.
     * @deprecated use {@link StanzaBuilder} instead.
     */
    @Deprecated
    // TODO: Remove when stanza builder is ready.
    public void setThread(String thread) {
        addExtension(new Message.Thread(thread));
    }

    @Override
    public String getElementName() {
        return ELEMENT;
    }

    @Override
    public MessageBuilder asBuilder() {
        return StanzaBuilder.buildMessageFrom(this, getStanzaId());
    }

    @Override
    public MessageBuilder asBuilder(String id) {
        return StanzaBuilder.buildMessageFrom(this, id);
    }

    @Override
    public MessageBuilder asBuilder(XMPPConnection connection) {
        return connection.getStanzaFactory().buildMessageStanzaFrom(this);
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
    public XmlStringBuilder toXML(XmlEnvironment enclosingXmlEnvironment) {
        XmlStringBuilder buf = new XmlStringBuilder(this, enclosingXmlEnvironment);
        addCommonAttributes(buf);
        buf.optAttribute("type", type);
        buf.rightAngleBracket();

        // Append the error subpacket if the message type is an error.
        if (type == Type.error) {
            appendErrorIfExists(buf);
        }

        // Add extension elements, if any are defined.
        buf.append(getExtensions());

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
     * @deprecated use {@link #asBuilder()} instead.
     */
    // TODO: Remove in Smack 4.5.
    @Deprecated
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

        public static final QName QNAME = new QName(NAMESPACE, ELEMENT);

        private final String subject;
        private final String language;

        public Subject(String language, String subject) {
            if (subject == null) {
                throw new NullPointerException("Subject cannot be null.");
            }
            this.language = language;
            this.subject = subject;
        }

        @Override
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

        private final HashCode.Cache hashCodeCache = new HashCode.Cache();

        @Override
        public int hashCode() {
            return hashCodeCache.getHashCode(c ->
                c.append(language)
                 .append(subject)
            );
        }

        @Override
        public boolean equals(Object obj) {
            return EqualsUtil.equals(this, obj, (e, o) ->
                e.append(language, o.language)
                 .append(subject, o.subject)
            );
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
        public XmlStringBuilder toXML(org.jivesoftware.smack.packet.XmlEnvironment enclosingNamespace) {
            XmlStringBuilder xml = new XmlStringBuilder(this, enclosingNamespace);
            xml.rightAngleBracket();
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
        public static final QName QNAME = new QName(NAMESPACE, ELEMENT);

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

        @Override
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

        private final HashCode.Cache hashCodeCache = new HashCode.Cache();

        @Override
        public int hashCode() {
            return hashCodeCache.getHashCode(c ->
                c.append(language)
                .append(message)
            );
        }

        @Override
        public boolean equals(Object obj) {
            return EqualsUtil.equals(this, obj, (e, o) ->
                e.append(language, o.language)
                 .append(message, o.message)
            );
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
        public XmlStringBuilder toXML(XmlEnvironment enclosingXmlEnvironment) {
            XmlStringBuilder xml = new XmlStringBuilder(this, enclosingXmlEnvironment);
            xml.rightAngleBracket();
            xml.text(message);
            xml.closeElement(getElementName());
            return xml;
        }

    }

    @SuppressWarnings("JavaLangClash")
    public static class Thread implements ExtensionElement {
        public static final String ELEMENT = "thread";
        public static final String NAMESPACE = StreamOpen.CLIENT_NAMESPACE;
        public static final QName QNAME = new QName(NAMESPACE, ELEMENT);

        public static final String PARENT_ATTRIBUTE_NAME = "parent";

        private final String thread;
        private final String parent;

        public Thread(String thread) {
            this(thread, null);
        }

        public Thread(String thread, String parent) {
            this.thread = StringUtils.requireNotNullNorEmpty(thread, "thread must not be null nor empty");
            this.parent = StringUtils.requireNullOrNotEmpty(parent, "parent must be null or not empty");
        }

        public String getThread() {
            return thread;
        }

        public String getParent() {
            return parent;
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
        public QName getQName() {
            return QNAME;
        }

        @Override
        public XmlStringBuilder toXML(XmlEnvironment xmlEnvironment) {
            XmlStringBuilder xml = new XmlStringBuilder(this, xmlEnvironment);
            xml.optAttribute(PARENT_ATTRIBUTE_NAME, parent);
            xml.rightAngleBracket();
            xml.escape(thread);
            xml.closeElement(this);
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
