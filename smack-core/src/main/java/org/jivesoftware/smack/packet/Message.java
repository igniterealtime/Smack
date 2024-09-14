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

import javax.xml.namespace.QName;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.util.EqualsUtil;
import org.jivesoftware.smack.util.HashCode;
import org.jivesoftware.smack.util.Objects;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.XmlStringBuilder;

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

    private final Type type;

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
