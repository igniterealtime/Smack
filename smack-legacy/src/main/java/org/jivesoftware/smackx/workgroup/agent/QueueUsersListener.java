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
import java.util.Set;

import org.jivesoftware.smackx.workgroup.QueueUser;

public interface QueueUsersListener {

    /**
     * The status of the queue was updated.
     *
     * @param queue the workgroup queue.
     * @param status the status of queue.
     */
    void statusUpdated(WorkgroupQueue queue, WorkgroupQueue.Status status);

    /**
     * The average wait time of the queue was updated.
     *
     * @param queue the workgroup queue.
     * @param averageWaitTime the average wait time of the queue.
     */
    void averageWaitTimeUpdated(WorkgroupQueue queue, int averageWaitTime);

    /**
     * The date of oldest entry waiting in the queue was updated.
     *
     * @param queue the workgroup queue.
     * @param oldestEntry the date of the oldest entry waiting in the queue.
     */
    void oldestEntryUpdated(WorkgroupQueue queue, Date oldestEntry);

    /**
     * The list of users waiting in the queue was updated.
     *
     * @param queue the workgroup queue.
     * @param users the list of users waiting in the queue.
     */
    void usersUpdated(WorkgroupQueue queue, Set<QueueUser> users);
}
