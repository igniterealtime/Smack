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

package org.jivesoftware.smackx.workgroup.ext.history;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.jxmpp.jid.EntityBareJid;

/**
 * IQ provider used to retrieve individual agent information. Each chat session can be mapped
 * to one or more jids and therefore retrievable.
 */
public class AgentChatHistory extends IQ {

    /**
     * Element name of the stanza extension.
     */
    public static final String ELEMENT_NAME = "chat-sessions";

    /**
     * Namespace of the stanza extension.
     */
    public static final String NAMESPACE = "http://jivesoftware.com/protocol/workgroup";

    private EntityBareJid agentJID;
    private int maxSessions;
    private long startDate;

    private final List<AgentChatSession> agentChatSessions = new ArrayList<>();

    public AgentChatHistory(EntityBareJid agentJID, int maxSessions, Date startDate) {
        this();
        this.agentJID = agentJID;
        this.maxSessions = maxSessions;
        this.startDate = startDate.getTime();
    }

    public AgentChatHistory(EntityBareJid agentJID, int maxSessions) {
        this();
        this.agentJID = agentJID;
        this.maxSessions = maxSessions;
        this.startDate = 0;
    }

    public AgentChatHistory() {
        super(ELEMENT_NAME, NAMESPACE);
    }

    public void addChatSession(AgentChatSession chatSession) {
        agentChatSessions.add(chatSession);
    }

    public Collection<AgentChatSession> getAgentChatSessions() {
        return agentChatSessions;
    }

    @Override
    protected IQChildElementXmlStringBuilder getIQChildElementBuilder(IQChildElementXmlStringBuilder buf) {
        buf.append(" agentJID=\"" + agentJID + "\"");
        buf.append(" maxSessions=\"" + maxSessions + "\"");
        buf.append(" startDate=\"" + startDate + "\"");
        buf.setEmptyElement();
        return buf;
    }

    /**
     * Stanza extension provider for AgentHistory packets.
     */
    public static class InternalProvider extends IQProvider<AgentChatHistory> {

        @Override
        public AgentChatHistory parse(XmlPullParser parser, int initialDepth, XmlEnvironment xmlEnvironment) throws XmlPullParserException, IOException {
            if (parser.getEventType() != XmlPullParser.Event.START_ELEMENT) {
                throw new IllegalStateException("Parser not in proper position, or bad XML.");
            }

            AgentChatHistory agentChatHistory = new AgentChatHistory();

            boolean done = false;
            while (!done) {
                XmlPullParser.Event eventType = parser.next();
                if (eventType == XmlPullParser.Event.START_ELEMENT && "chat-session".equals(parser.getName())) {
                    agentChatHistory.addChatSession(parseChatSetting(parser));

                }
                else if (eventType == XmlPullParser.Event.END_ELEMENT && ELEMENT_NAME.equals(parser.getName())) {
                    done = true;
                }
            }
            return agentChatHistory;
        }

        private static AgentChatSession parseChatSetting(XmlPullParser parser)
                        throws XmlPullParserException, IOException {
            boolean done = false;
            Date date = null;
            long duration = 0;
            String visitorsName = null;
            String visitorsEmail = null;
            String sessionID = null;
            String question = null;

            while (!done) {
                XmlPullParser.Event eventType = parser.next();
                if (eventType == XmlPullParser.Event.START_ELEMENT && "date".equals(parser.getName())) {
                    String dateStr = parser.nextText();
                    long l = Long.valueOf(dateStr).longValue();
                    date = new Date(l);
                }
                else if (eventType == XmlPullParser.Event.START_ELEMENT && "duration".equals(parser.getName())) {
                    duration = Long.valueOf(parser.nextText()).longValue();
                }
                else if (eventType == XmlPullParser.Event.START_ELEMENT && "visitorsName".equals(parser.getName())) {
                    visitorsName = parser.nextText();
                }
                else if (eventType == XmlPullParser.Event.START_ELEMENT && "visitorsEmail".equals(parser.getName())) {
                    visitorsEmail = parser.nextText();
                }
                else if (eventType == XmlPullParser.Event.START_ELEMENT && "sessionID".equals(parser.getName())) {
                    sessionID = parser.nextText();
                }
                else if (eventType == XmlPullParser.Event.START_ELEMENT && "question".equals(parser.getName())) {
                    question = parser.nextText();
                }
                else if (eventType == XmlPullParser.Event.END_ELEMENT && "chat-session".equals(parser.getName())) {
                    done = true;
                }
            }
            return new AgentChatSession(date, duration, visitorsName, visitorsEmail, sessionID, question);
        }
    }
}
