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

import java.util.*;

/**
 * A queue in a workgroup, which is a pool of agents that are routed a specific type of
 * chat request.
 *
 * @author Matt Tucker
 */
public class WorkgroupQueue {

    private String name;
    private Status status = Status.CLOSED;

    private int averageWaitTime = -1;
    private Date oldestEntry = null;
    private Set users = Collections.EMPTY_SET;

    private Set agents = Collections.EMPTY_SET;
    private int maxChats = 0;
    private int currentChats = 0;

    /**
     * Creates a new workgroup queue instance.
     *
     * @param name the name of the queue.
     */
    WorkgroupQueue(String name) {
        this.name = name;
    }

    /**
     * Returns the name of this queue.
     *
     * @return the name of this queue.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the status of this queue.
     *
     * @return the status of this queue.
     */
    public Status getStatus() {
        return status;
    }

    void setStatus(Status status) {
        this.status = status;
    }

    /**
     * Returns the number of users in this queue that are waiting to be routed to
     * an agent.
     *
     * @return the number of users waiting in the queue.
     */
    public int getUserCount() {
        if (users == null) {
            return 0;
        }
        return users.size();
    }

    /**
     * Returns an Iterator for the users in this queue that are waiting
     * to be routed to an agent (QueueUser instances).
     *
     * @return an Iterator for the users waiting in the queue.
     */
    public Iterator getUsers() {
        if (users == null) {
            return Collections.EMPTY_SET.iterator();
        }
        return Collections.unmodifiableSet(users).iterator();
    }

    void setUsers(Set users) {
        this.users = users;
    }

    /**
     * Returns the average amount of time users wait in this queue before being
     * routed to an agent. If average wait time info isn't available, -1 will
     * be returned.
     *
     * @return the average wait time in this queue.
     */
    public int getAverageWaitTime() {
        return averageWaitTime;
    }

    void setAverageWaitTime(int averageTime) {
        this.averageWaitTime = averageTime;
    }

    /**
     * Returns the date of the oldest request waiting in this queue. If there
     * are no requests waiting to be routed, this method will return <tt>null</tt>.
     *
     * @return the date of the oldest request in this queue.
     */
    public Date getOldestEntry() {
        return oldestEntry;
    }

    void setOldestEntry(Date oldestEntry) {
        this.oldestEntry = oldestEntry;
    }

    /**
     * Returns the count of the currently available agents in this queue.
     *
     * @return the number of active agents in this queue.
     */
    public int getAgentCount() {
        synchronized (agents)  {
            return agents.size();
        }
    }

    /**
     * Returns an Iterator the currently active agents (Agent instances)
     * in this queue.
     *
     * @return an Iterator for the active agents in this queue.
     */
    public Iterator getAgents() {
        return Collections.unmodifiableSet(agents).iterator();
    }

    void setAgents(Set agents) {
        this.agents = agents;
    }

    /**
     * Returns the maximum number of simultaneous chats this queue can handle.
     *
     * @return the max number of chats this queue can handle.
     */
    public int getMaxChats() {
        return maxChats;
    }

    void setMaxChats(int maxChats) {
        this.maxChats = maxChats;
    }

    /**
     * Returns the current number of active chat sessions in this queue.
     *
     * @return the current number of active chat sessions in this queue.
     */
    public int getCurrentChats() {
        return currentChats;
    }

    void setCurrentChats(int currentChats) {
        this.currentChats = currentChats;
    }

    /**
     * Represents the status of the queue. The possible values are:
     *
     * <ul>
     *      <li>WorkgroupQueue.Status.OPEN -- the queue is active and accepting
     *          new chat requests.
     *      <li>WorkgroupQueue.Status.ACTIVE -- the queue is active but NOT accepting
     *          new chat requests.
     *      <li>WorkgroupQueue.Status.CLOSED -- the queue is NOT active and NOT
     *          accepting new chat requests.
     * </ul>
     */
    public static class Status {

        /**
         * The queue is active and accepting new chat requests.
         */
        public static final Status OPEN = new Status("open");

        /**
         * The queue is active but NOT accepting new chat requests. This state might
         * occur when the workgroup has closed because regular support hours have closed,
         * but there are still several requests left in the queue.
         */
        public static final Status ACTIVE = new Status("active");

        /**
         * The queue is NOT active and NOT accepting new chat requests.
         */
        public static final Status CLOSED = new Status("closed");

        /**
         * Converts a String into the corresponding status. Valid String values
         * that can be converted to a status are: "open", "active", and "closed".
         *
         * @param type the String value to covert.
         * @return the corresponding Type.
         */
        public static Status fromString(String type) {
            if (type == null) {
                return null;
            }
            type = type.toLowerCase();
            if (OPEN.toString().equals(type)) {
                return OPEN;
            }
            else if (ACTIVE.toString().equals(type)) {
                return ACTIVE;
            }
            else if (CLOSED.toString().equals(type)) {
                return CLOSED;
            }
            else {
                return null;
            }
        }

        private String value;

        private Status(String value) {
            this.value = value;
        }

        public String toString() {
            return value;
        }
    }
}