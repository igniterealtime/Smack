/**
* $RCSfile$
* $Revision$
* $Date$
*
* Copyright (C) 2002-2004 Jive Software. All rights reserved.
* ====================================================================
* The Jive Software License (based on Apache Software License, Version 1.1)
*
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions
* are met:
*
* 1. Redistributions of source code must retain the above copyright
*    notice, this list of conditions and the following disclaimer.
*
* 2. Redistributions in binary form must reproduce the above copyright
*    notice, this list of conditions and the following disclaimer in
*    the documentation and/or other materials provided with the
*    distribution.
*
* 3. The end-user documentation included with the redistribution,
*    if any, must include the following acknowledgment:
*       "This product includes software developed by
*        Jive Software (http://www.jivesoftware.com)."
*    Alternately, this acknowledgment may appear in the software itself,
*    if and wherever such third-party acknowledgments normally appear.
*
* 4. The names "Smack" and "Jive Software" must not be used to
*    endorse or promote products derived from this software without
*    prior written permission. For written permission, please
*    contact webmaster@jivesoftware.com.
*
* 5. Products derived from this software may not be called "Smack",
*    nor may "Smack" appear in their name, without prior written
*    permission of Jive Software.
*
* THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
* WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
* OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
* DISCLAIMED.  IN NO EVENT SHALL JIVE SOFTWARE OR
* ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
* SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
* LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
* USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
* ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
* OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
* OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
* SUCH DAMAGE.
* ====================================================================
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