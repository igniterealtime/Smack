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

package org.jivesoftware.smackx.workgroup.user;

/**
 * Listener interface for those that wish to be notified of workgroup queue events.
 *
 * @see Workgroup#addQueueListener(QueueListener)
 * @author loki der quaeler
 */
public interface QueueListener {

    /**
     * The user joined the workgroup queue.
     */
    public void joinedQueue();

    /**
     * The user departed the workgroup queue.
     */
    public void departedQueue();

    /**
     * The user's queue position has been updated to a new value.
     *
     * @param currentPosition the user's current position in the queue.
     */
    public void queuePositionUpdated(int currentPosition);

    /**
     * The user's estimated remaining wait time in the queue has been updated.
     *
     * @param secondsRemaining the estimated number of seconds remaining until the
     *      the user is routed to the agent.
     */
    public void queueWaitTimeUpdated(int secondsRemaining);

}
