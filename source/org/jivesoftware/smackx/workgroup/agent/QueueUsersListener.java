package org.jivesoftware.smackx.workgroup.agent;

import java.util.Date;
import java.util.Set;

public interface QueueUsersListener {

    /**
     * The status of the queue was updated.
     *
     * @param queue the workgroup queue.
     * @param status the status of queue.
     */
    public void statusUpdated(WorkgroupQueue queue, WorkgroupQueue.Status status);

    /**
     * The average wait time of the queue was updated.
     *
     * @param queue the workgroup queue.
     * @param averageWaitTime the average wait time of the queue.
     */
    public void averageWaitTimeUpdated(WorkgroupQueue queue, int averageWaitTime);

    /**
     * The date of oldest entry waiting in the queue was updated.
     *
     * @param queue the workgroup queue.
     * @param oldestEntry the date of the oldest entry waiting in the queue.
     */
    public void oldestEntryUpdated(WorkgroupQueue queue, Date oldestEntry);

    /**
     * The list of users waiting in the queue was updated.
     *
     * @param queue the workgroup queue.
     * @param users the list of users waiting in the queue.
     */
    public void usersUpdated(WorkgroupQueue queue, Set users);
}