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

package org.jivesoftware.smackx.workgroup.agent;

import java.util.Date;

/**
 * A revoked offer which contains the reason the offer was revoked, the
 * workgroup, the user JID, and the timestamp of the revocation.
 *
 * @author Matt Tucker
 */
public class RevokedOffer {

    private String userID;
    private String workgroupName;
    private String sessionID;
    private String reason;
    private Date timestamp;

    /**
     * @param userID the jid of the user for which this revocation was issued.
     * @param workgroupName the fully qualified name of the workgroup.
     * @param sessionID the session id attributed to this chain of packets.
     * @param reason the server issued message as to why this revocation was issued.
     * @param timestamp the timestamp at which the revocation was issued.
     */
    public RevokedOffer(String userID, String workgroupName, String sessionID,
            String reason, Date timestamp)
    {
        this.userID = userID;
        this.workgroupName = workgroupName;
        this.sessionID = sessionID;
        this.reason = reason;
        this.timestamp = timestamp;
    }

    /**
     * @return the jid of the user for which this rejection was issued.
     */
    public String getUserID() {
        return this.userID;
    }

    /**
     * @return the fully qualified name of the workgroup
     */
    public String getWorkgroupName() {
        return this.workgroupName;
    }

    /**
     * @return the session id which will associate all packets for the pending chat
     */
    public String getSessionID() {
        return this.sessionID;
    }

    /**
     * @return the server issued message as to why this revocation was issued
     */
    public String getReason () {
        return this.reason;
    }

    /**
     * @return the timestamp at which the revocation was issued
     */
    public Date getTimestamp () {
        return this.timestamp;
    }
}