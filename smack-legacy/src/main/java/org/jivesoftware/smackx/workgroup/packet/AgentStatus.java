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
package org.jivesoftware.smackx.workgroup.packet;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.TimeZone;

import javax.xml.namespace.QName;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.util.ParserUtils;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.jxmpp.jid.EntityBareJid;

/**
 * Agent status packet.
 *
 * @author Matt Tucker
 */
public class AgentStatus implements ExtensionElement {

    @SuppressWarnings("DateFormatConstant")
    private static final SimpleDateFormat UTC_FORMAT = new SimpleDateFormat("yyyyMMdd'T'HH:mm:ss");

    static {
        UTC_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT+0"));
    }

    /**
     * Element name of the stanza extension.
     */
    public static final String ELEMENT_NAME = "agent-status";

    /**
     * Namespace of the stanza extension.
     */
    public static final String NAMESPACE = "http://jabber.org/protocol/workgroup";

    public static final QName QNAME = new QName(NAMESPACE, ELEMENT_NAME);

    private EntityBareJid workgroupJID;
    private final List<ChatInfo> currentChats = new ArrayList<>();
    private int maxChats = -1;

    AgentStatus() {
    }

    public EntityBareJid getWorkgroupJID() {
        return workgroupJID;
    }

    /**
     * Returns a collection of ChatInfo where each ChatInfo represents a Chat where this agent
     * is participating.
     *
     * @return a collection of ChatInfo where each ChatInfo represents a Chat where this agent
     *         is participating.
     */
    public List<ChatInfo> getCurrentChats() {
        return Collections.unmodifiableList(currentChats);
    }

    public int getMaxChats() {
        return maxChats;
    }

    @Override
    public String getElementName() {
        return ELEMENT_NAME;
    }

    @Override
    public String getNamespace() {
        return NAMESPACE;
    }

    @Override
    public String toXML(org.jivesoftware.smack.packet.XmlEnvironment enclosingNamespace) {
        StringBuilder buf = new StringBuilder();

        buf.append('<').append(ELEMENT_NAME).append(" xmlns=\"").append(NAMESPACE).append('"');
        if (workgroupJID != null) {
            buf.append(" jid=\"").append(workgroupJID).append('"');
        }
        buf.append('>');
        if (maxChats != -1) {
            buf.append("<max-chats>").append(maxChats).append("</max-chats>");
        }
        if (!currentChats.isEmpty()) {
            buf.append("<current-chats xmlns= \"http://jivesoftware.com/protocol/workgroup\">");
            for (Iterator<ChatInfo> it = currentChats.iterator(); it.hasNext();) {
                buf.append(it.next().toXML());
            }
            buf.append("</current-chats>");
        }
        buf.append("</").append(this.getElementName()).append("> ");

        return buf.toString();
    }

    /**
     * Represents information about a Chat where this Agent is participating.
     *
     * @author Gaston Dombiak
     */
    public static class ChatInfo {

        private final String sessionID;
        private final String userID;
        private final Date date;
        private final String email;
        private final String username;
        private final String question;

        public ChatInfo(String sessionID, String userID, Date date, String email, String username, String question) {
            this.sessionID = sessionID;
            this.userID = userID;
            this.date = date;
            this.email = email;
            this.username = username;
            this.question = question;
        }

        /**
         * Returns the sessionID associated to this chat. Each chat will have a unique sessionID
         * that could be used for retrieving the whole transcript of the conversation.
         *
         * @return the sessionID associated to this chat.
         */
        public String getSessionID() {
            return sessionID;
        }

        /**
         * Returns the user unique identification of the user that made the initial request and
         * for which this chat was generated. If the user joined using an anonymous connection
         * then the userID will be the value of the ID attribute of the USER element. Otherwise,
         * the userID will be the bare JID of the user that made the request.
         *
         * @return the user unique identification of the user that made the initial request.
         */
        public String getUserID() {
            return userID;
        }

        /**
         * Returns the date when this agent joined the chat.
         *
         * @return the date when this agent joined the chat.
         */
        public Date getDate() {
            return date;
        }

        /**
         * Returns the email address associated with the user.
         *
         * @return the email address associated with the user.
         */
        public String getEmail() {
            return email;
        }

        /**
         * Returns the username(nickname) associated with the user.
         *
         * @return the username associated with the user.
         */
        public String getUsername() {
            return username;
        }

        /**
         * Returns the question the user asked.
         *
         * @return the question the user asked, if any.
         */
        public String getQuestion() {
            return question;
        }

        public String toXML() {
            StringBuilder buf = new StringBuilder();

            buf.append("<chat ");
            if (sessionID != null) {
                buf.append(" sessionID=\"").append(sessionID).append('"');
            }
            if (userID != null) {
                buf.append(" userID=\"").append(userID).append('"');
            }
            if (date != null) {
                buf.append(" startTime=\"");
                synchronized (UTC_FORMAT) {
                    buf.append(UTC_FORMAT.format(date));
                }
                buf.append('"');
            }
            if (email != null) {
                buf.append(" email=\"").append(email).append('"');
            }
            if (username != null) {
                buf.append(" username=\"").append(username).append('"');
            }
            if (question != null) {
                buf.append(" question=\"").append(question).append('"');
            }
            buf.append("/>");

            return buf.toString();
        }
    }

    /**
     * Stanza extension provider for AgentStatus packets.
     */
    public static class Provider extends ExtensionElementProvider<AgentStatus> {

        @Override
        public AgentStatus parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment)
                        throws XmlPullParserException, IOException, ParseException {
            AgentStatus agentStatus = new AgentStatus();

            agentStatus.workgroupJID = ParserUtils.getBareJidAttribute(parser);

            boolean done = false;
            while (!done) {
                XmlPullParser.Event eventType = parser.next();

                if (eventType == XmlPullParser.Event.START_ELEMENT) {
                    if ("chat".equals(parser.getName())) {
                        ChatInfo chatInfo = parseChatInfo(parser);
                        agentStatus.currentChats.add(chatInfo);
                    }
                    else if ("max-chats".equals(parser.getName())) {
                        agentStatus.maxChats = Integer.parseInt(parser.nextText());
                    }
                }
                else if (eventType == XmlPullParser.Event.END_ELEMENT &&
                    ELEMENT_NAME.equals(parser.getName())) {
                    done = true;
                }
            }
            return agentStatus;
        }

        private static ChatInfo parseChatInfo(XmlPullParser parser) throws ParseException {

            String sessionID = parser.getAttributeValue("", "sessionID");
            String userID = parser.getAttributeValue("", "userID");
            Date date = null;
            synchronized (UTC_FORMAT) {
                date = UTC_FORMAT.parse(parser.getAttributeValue("", "startTime"));
            }

            String email = parser.getAttributeValue("", "email");
            String username = parser.getAttributeValue("", "username");
            String question = parser.getAttributeValue("", "question");

            return new ChatInfo(sessionID, userID, date, email, username, question);
        }
    }
}
