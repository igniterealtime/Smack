/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright (C) 1999-2003 Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software.
 * Use is subject to license terms.
 */

package org.jivesoftware.smackx.workgroup.agent;

import java.util.Date;

/**
 * An immutable simple class to embody the information concerning a revoked offer, this is namely
 *  the reason, the workgroup, the userJID, and the timestamp which the message was received.<br>
 *
 * @author loki der quaeler
 */
public class RevokedOffer {

    protected String userID;
    protected String workgroupName;
    protected String sessionID;
    protected String reason;
    protected Date timestamp;

    /**
     * @param uid the jid of the user for which this revocation was issued
     * @param wg the fully qualified name of the workgroup
     * @param sid the session id attributed to this chain of packets
     * @param cause the server issued message as to why this revocation was issued
     * @param ts the timestamp at which the revocation was issued
     */
    public RevokedOffer (String uid, String wg, String sid, String cause, Date ts) {
        super();

        this.userID = uid;
        this.workgroupName = wg;
        this.sessionID = sid;
        this.reason = cause;
        this.timestamp = ts;
    }

    /**
     * @return the jid of the user for which this revocation was issued
     */
    public String getUserID () {
        return this.userID;
    }

    /**
     * @return the fully qualified name of the workgroup
     */
    public String getWorkgroupName () {
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