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
