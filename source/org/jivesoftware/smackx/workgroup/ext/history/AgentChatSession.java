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

import java.util.Date;

/**
 * Represents one chat session for an agent.
 */
public class AgentChatSession {
    public Date startDate;
    public long duration;
    public String visitorsName;
    public String visitorsEmail;
    public String sessionID;
    public String question;

    public AgentChatSession(Date date, long duration, String visitorsName, String visitorsEmail, String sessionID, String question) {
        this.startDate = date;
        this.duration = duration;
        this.visitorsName = visitorsName;
        this.visitorsEmail = visitorsEmail;
        this.sessionID = sessionID;
        this.question = question;
    }

    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public String getVisitorsName() {
        return visitorsName;
    }

    public void setVisitorsName(String visitorsName) {
        this.visitorsName = visitorsName;
    }

    public String getVisitorsEmail() {
        return visitorsEmail;
    }

    public void setVisitorsEmail(String visitorsEmail) {
        this.visitorsEmail = visitorsEmail;
    }

    public String getSessionID() {
        return sessionID;
    }

    public void setSessionID(String sessionID) {
        this.sessionID = sessionID;
    }

    public void setQuestion(String question){
        this.question = question;
    }

    public String getQuestion(){
        return question;
    }


}
