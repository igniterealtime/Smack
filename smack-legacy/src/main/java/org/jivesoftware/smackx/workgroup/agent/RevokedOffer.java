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

package org.jivesoftware.smackx.workgroup.agent;

import java.util.Date;

import org.jxmpp.jid.Jid;

/**
 * An immutable simple class to embody the information concerning a revoked offer, this is namely
 *  the reason, the workgroup, the userJID, and the timestamp which the message was received.<br>
 *
 * @author loki der quaeler
 */
public class RevokedOffer {

    private final Jid userJID;
    private final Jid userID;
    private final Jid workgroupName;
    private final String sessionID;
    private final String reason;
    private final Date timestamp;

    /**
     *
     * @param userJID the JID of the user for which this revocation was issued.
     * @param userID the user ID of the user for which this revocation was issued.
     * @param workgroupName the fully qualified name of the workgroup
     * @param sessionID the session id attributed to this chain of packets
     * @param reason the server issued message as to why this revocation was issued.
     * @param timestamp the timestamp at which the revocation was issued
     */
    RevokedOffer(Jid userJID, Jid userID, Jid workgroupName, String sessionID,
            String reason, Date timestamp) {
        super();

        this.userJID = userJID;
        this.userID = userID;
        this.workgroupName = workgroupName;
        this.sessionID = sessionID;
        this.reason = reason;
        this.timestamp = timestamp;
    }

    public Jid getUserJID() {
        return userJID;
    }

    /**
     * Get user id.
     * @return the jid of the user for which this revocation was issued
     */
    public Jid getUserID() {
        return this.userID;
    }

    /**
     * Get workgroup name.
     * @return the fully qualified name of the workgroup
     */
    public Jid getWorkgroupName() {
        return this.workgroupName;
    }

    /**
     * Get the session id.
     * @return the session id which will associate all packets for the pending chat
     */
    public String getSessionID() {
        return this.sessionID;
    }

    /**
     * Get reason.
     * @return the server issued message as to why this revocation was issued
     */
    public String getReason() {
        return this.reason;
    }

    /**
     * Get the timestamp.
     * @return the timestamp at which the revocation was issued
     */
    public Date getTimestamp() {
        return this.timestamp;
    }
}
