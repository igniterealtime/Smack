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

package org.jivesoftware.smackx.workgroup.packet;

import java.util.Date;

/**
 * An immutable class which wraps up customer-in-queue data return from the server; depending on
 *  the type of information dispatched from the server, not all information will be available in
 *  any given instance.
 *
 * @author loki der quaeler
 */
public class QueueUser {

    private String userID;

    private int queuePosition;
    private int estimatedTime;
    private Date joinDate;

    /**
     * @param uid the user jid of the customer in the queue
     * @param position the position customer sits in the queue
     * @param time the estimate of how much longer the customer will be in the queue in seconds
     * @param joinedAt the timestamp of when the customer entered the queue
     */
    public QueueUser (String uid, int position, int time, Date joinedAt) {
        super();

        this.userID = uid;
        this.queuePosition = position;
        this.estimatedTime = time;
        this.joinDate = joinedAt;
    }

    /**
     * @return the user jid of the customer in the queue
     */
    public String getUserID () {
        return this.userID;
    }

    /**
     * @return the position in the queue at which the customer sits, or -1 if the update which
     *          this instance embodies is only a time update instead
     */
    public int getQueuePosition () {
        return this.queuePosition;
    }

    /**
     * @return the estimated time remaining of the customer in the queue in seconds, or -1 if
     *          if the update which this instance embodies is only a position update instead
     */
    public int getEstimatedRemainingTime () {
        return this.estimatedTime;
    }

    /**
     * @return the timestamp of when this customer entered the queue, or null if the server did not
     *          provide this information
     */
    public Date getQueueJoinTimestamp () {
        return this.joinDate;
    }

}
