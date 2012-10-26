/**
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

package org.jivesoftware.smackx.workgroup.ext.history;

import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.provider.IQProvider;
import org.xmlpull.v1.XmlPullParser;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * IQ provider used to retrieve individual agent information. Each chat session can be mapped
 * to one or more jids and therefore retrievable.
 */
public class AgentChatHistory extends IQ {
    private String agentJID;
    private int maxSessions;
    private long startDate;

    private List<AgentChatSession> agentChatSessions = new ArrayList<AgentChatSession>();

    public AgentChatHistory(String agentJID, int maxSessions, Date startDate) {
        this.agentJID = agentJID;
        this.maxSessions = maxSessions;
        this.startDate = startDate.getTime();
    }

    public AgentChatHistory(String agentJID, int maxSessions) {
        this.agentJID = agentJID;
        this.maxSessions = maxSessions;
        this.startDate = 0;
    }

    public AgentChatHistory() {
    }

    public void addChatSession(AgentChatSession chatSession) {
        agentChatSessions.add(chatSession);
    }

    public Collection<AgentChatSession> getAgentChatSessions() {
        return agentChatSessions;
    }

    /**
     * Element name of the packet extension.
     */
    public static final String ELEMENT_NAME = "chat-sessions";

    /**
     * Namespace of the packet extension.
     */
    public static final String NAMESPACE = "http://jivesoftware.com/protocol/workgroup";

    public String getChildElementXML() {
        StringBuilder buf = new StringBuilder();

        buf.append("<").append(ELEMENT_NAME).append(" xmlns=");
        buf.append('"');
        buf.append(NAMESPACE);
        buf.append('"');
        buf.append(" agentJID=\"" + agentJID + "\"");
        buf.append(" maxSessions=\"" + maxSessions + "\"");
        buf.append(" startDate=\"" + startDate + "\"");

        buf.append("></").append(ELEMENT_NAME).append("> ");
        return buf.toString();
    }

    /**
     * Packet extension provider for AgentHistory packets.
     */
    public static class InternalProvider implements IQProvider {

        public IQ parseIQ(XmlPullParser parser) throws Exception {
            if (parser.getEventType() != XmlPullParser.START_TAG) {
                throw new IllegalStateException("Parser not in proper position, or bad XML.");
            }

            AgentChatHistory agentChatHistory = new AgentChatHistory();

            boolean done = false;
            while (!done) {
                int eventType = parser.next();
                if ((eventType == XmlPullParser.START_TAG) && ("chat-session".equals(parser.getName()))) {
                    agentChatHistory.addChatSession(parseChatSetting(parser));

                }
                else if (eventType == XmlPullParser.END_TAG && ELEMENT_NAME.equals(parser.getName())) {
                    done = true;
                }
            }
            return agentChatHistory;
        }

        private AgentChatSession parseChatSetting(XmlPullParser parser) throws Exception {

            boolean done = false;
            Date date = null;
            long duration = 0;
            String visitorsName = null;
            String visitorsEmail = null;
            String sessionID = null;
            String question = null;

            while (!done) {
                int eventType = parser.next();
                if ((eventType == XmlPullParser.START_TAG) && ("date".equals(parser.getName()))) {
                    String dateStr = parser.nextText();
                    long l = Long.valueOf(dateStr).longValue();
                    date = new Date(l);
                }
                else if ((eventType == XmlPullParser.START_TAG) && ("duration".equals(parser.getName()))) {
                    duration = Long.valueOf(parser.nextText()).longValue();
                }
                else if ((eventType == XmlPullParser.START_TAG) && ("visitorsName".equals(parser.getName()))) {
                    visitorsName = parser.nextText();
                }
                else if ((eventType == XmlPullParser.START_TAG) && ("visitorsEmail".equals(parser.getName()))) {
                    visitorsEmail = parser.nextText();
                }
                else if ((eventType == XmlPullParser.START_TAG) && ("sessionID".equals(parser.getName()))) {
                    sessionID = parser.nextText();
                }
                else if ((eventType == XmlPullParser.START_TAG) && ("question".equals(parser.getName()))) {
                    question = parser.nextText();
                }
                else if (eventType == XmlPullParser.END_TAG && "chat-session".equals(parser.getName())) {
                    done = true;
                }
            }
            return new AgentChatSession(date, duration, visitorsName, visitorsEmail, sessionID, question);
        }
    }
}
