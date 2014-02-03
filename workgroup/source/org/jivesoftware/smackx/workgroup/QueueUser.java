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

package org.jivesoftware.smackx.workgroup;

import java.util.Date;

/**
 * An immutable class which wraps up customer-in-queue data return from the server; depending on
 * the type of information dispatched from the server, not all information will be available in
 * any given instance.
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
