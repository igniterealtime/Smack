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

package org.jivesoftware.smackx.workgroup.packet;

import org.jivesoftware.smack.packet.IQ;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Represents a list of conversation transcripts that a user had in all his history. Each
 * transcript summary includes the sessionID which may be used for getting more detailed
 * information about the conversation. {@link org.jivesoftware.smackx.workgroup.packet.Transcript}
 *
 * @author Gaston Dombiak
 */
public class Transcripts extends IQ {

    private static final SimpleDateFormat UTC_FORMAT = new SimpleDateFormat("yyyyMMdd'T'HH:mm:ss");
    static {
        UTC_FORMAT.setTimeZone(TimeZone.getTimeZone("GMT+0"));
    }

    private String userID;
    private List<Transcripts.TranscriptSummary> summaries;


    /**
     * Creates a transcripts request for the given userID.
     *
     * @param userID the id of the user to get his conversations transcripts.
     */
    public Transcripts(String userID) {
        this.userID = userID;
        this.summaries = new ArrayList<Transcripts.TranscriptSummary>();
    }

    /**
     * Creates a Transcripts which will contain the transcript summaries of the given user.
     *
     * @param userID the id of the user. Could be a real JID or a unique String that identifies
     *        anonymous users.
     * @param summaries the list of TranscriptSummaries.
     */
    public Transcripts(String userID, List<Transcripts.TranscriptSummary> summaries) {
        this.userID = userID;
        this.summaries = summaries;
    }

    /**
     * Returns the id of the user that was involved in the conversations. The userID could be a
     * real JID if the connected user was not anonymous. Otherwise, the userID will be a String
     * that was provided by the anonymous user as a way to idenitify the user across many user
     * sessions.
     *
     * @return the id of the user that was involved in the conversations.
     */
    public String getUserID() {
        return userID;
    }

    /**
     * Returns a list of TranscriptSummary. A TranscriptSummary does not contain the conversation
     * transcript but some summary information like the sessionID and the time when the
     * conversation started and finished. Once you have the sessionID it is possible to get the
     * full conversation transcript.
     *
     * @return a list of TranscriptSummary.
     */
    public List<Transcripts.TranscriptSummary> getSummaries() {
        return Collections.unmodifiableList(summaries);
    }

    public String getChildElementXML() {
        StringBuilder buf = new StringBuilder();

        buf.append("<transcripts xmlns=\"http://jivesoftware.com/protocol/workgroup\" userID=\"")
                .append(userID)
                .append("\">");

        for (TranscriptSummary transcriptSummary : summaries) {
            buf.append(transcriptSummary.toXML());
        }

        buf.append("</transcripts>");

        return buf.toString();
    }

    /**
     * A TranscriptSummary contains some information about a conversation such as the ID of the
     * session or the date when the conversation started and finished. You will need to use the
     * sessionID to get the full conversation transcript.
     */
    public static class TranscriptSummary {
        private String sessionID;
        private Date joinTime;
        private Date leftTime;
        private List<AgentDetail> agentDetails;

        public TranscriptSummary(String sessionID, Date joinTime, Date leftTime, List<AgentDetail> agentDetails) {
            this.sessionID = sessionID;
            this.joinTime = joinTime;
            this.leftTime = leftTime;
            this.agentDetails = agentDetails;
        }

        /**
         * Returns the ID of the session that is related to this conversation transcript. The
         * sessionID could be used for getting the full conversation transcript.
         *
         * @return the ID of the session that is related to this conversation transcript.
         */
        public String getSessionID() {
            return sessionID;
        }

        /**
         * Returns the Date when the conversation started.
         *
         * @return the Date when the conversation started.
         */
        public Date getJoinTime() {
            return joinTime;
        }

        /**
         * Returns the Date when the conversation finished.
         *
         * @return the Date when the conversation finished.
         */
        public Date getLeftTime() {
            return leftTime;
        }

        /**
         * Returns a list of AgentDetails. For each Agent that was involved in the conversation
         * the list will include an AgentDetail. An AgentDetail contains the JID of the agent
         * as well as the time when the Agent joined and left the conversation.
         *
         * @return a list of AgentDetails.
         */
        public List<AgentDetail> getAgentDetails() {
            return agentDetails;
        }

        public String toXML() {
            StringBuilder buf = new StringBuilder();

            buf.append("<transcript sessionID=\"")
                    .append(sessionID)
                    .append("\">");

            if (joinTime != null) {
                buf.append("<joinTime>").append(UTC_FORMAT.format(joinTime)).append("</joinTime>");
            }
            if (leftTime != null) {
                buf.append("<leftTime>").append(UTC_FORMAT.format(leftTime)).append("</leftTime>");
            }
            buf.append("<agents>");
            for (AgentDetail agentDetail : agentDetails) {
                buf.append(agentDetail.toXML());
            }
            buf.append("</agents></transcript>");

            return buf.toString();
        }
    }

    /**
     * An AgentDetail contains information of an Agent that was involved in a conversation. 
     */
    public static class AgentDetail {
        private String agentJID;
        private Date joinTime;
        private Date leftTime;

        public AgentDetail(String agentJID, Date joinTime, Date leftTime) {
            this.agentJID = agentJID;
            this.joinTime = joinTime;
            this.leftTime = leftTime;
        }

        /**
         * Returns the bare JID of the Agent that was involved in the conversation.
         *
         * @return the bared JID of the Agent that was involved in the conversation.
         */
        public String getAgentJID() {
            return agentJID;
        }

        /**
         * Returns the Date when the Agent joined the conversation.
         *
         * @return the Date when the Agent joined the conversation.
         */
        public Date getJoinTime() {
            return joinTime;
        }

        /**
         * Returns the Date when the Agent left the conversation.
         *
         * @return the Date when the Agent left the conversation.
         */
        public Date getLeftTime() {
            return leftTime;
        }

        public String toXML() {
            StringBuilder buf = new StringBuilder();

            buf.append("<agent>");

            if (agentJID != null) {
                buf.append("<agentJID>").append(agentJID).append("</agentJID>");
            }
            if (joinTime != null) {
                buf.append("<joinTime>").append(UTC_FORMAT.format(joinTime)).append("</joinTime>");
            }
            if (leftTime != null) {
                buf.append("<leftTime>").append(UTC_FORMAT.format(leftTime)).append("</leftTime>");
            }
            buf.append("</agent>");

            return buf.toString();
        }
    }
}
