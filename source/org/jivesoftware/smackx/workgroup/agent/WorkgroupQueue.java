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

import java.util.*;

/**
 * A queue in a workgroup, which is a pool of agents that are routed  a specific type of
 * chat request.
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
     * Returns the name of the queue.
     *
     * @return the name of the queue.
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the status of the queue.
     *
     * @return the status of the queue.
     */
    public Status getStatus() {
        return status;
    }

    void setStatus(Status status) {
        this.status = status;
    }

    /**
     * Returns the number of users waiting in the queue waiting to be routed to
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
     * Returns an Iterator for the users in the queue waiting to be routed to
     * an agent (QueueUser instances).
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
     * Returns the average amount of time users wait in the queue before being
     * routed to an agent. If average wait time info isn't available, -1 will
     * be returned.
     *
     * @return the average wait time
     */
    public int getAverageWaitTime() {
        return averageWaitTime;
    }

    void setAverageWaitTime(int averageTime) {
        this.averageWaitTime = averageTime;
    }

    /**
     * Returns the date of the oldest request waiting in the queue. If there
     * are no requests waiting to be routed, this method will return <tt>null</tt>.
     *
     * @return the date of the oldest request in the queue.
     */
    public Date getOldestEntry() {
        return oldestEntry;
    }

    void setOldestEntry(Date oldestEntry) {
        this.oldestEntry = oldestEntry;
    }

    /**
     * Returns the count of the currently available agents in the queue.
     *
     * @return the number of active agents in the queue.
     */
    public int getAgentCount() {
        synchronized (agents)  {
            return agents.size();
        }
    }

    /**
     * Returns an Iterator the currently active agents (Agent instances).
     *
     * @return an Iterator for the active agents.
     */
    public Iterator getAgents() {
        return Collections.unmodifiableSet(agents).iterator();
    }

    void setAgents(Set agents) {
        this.agents = agents;
    }

    /**
     * Returns the maximum number of simultaneous chats the queue can handle.
     *
     * @return the max number of chats the queue can handle.
     */
    public int getMaxChats() {
        return maxChats;
    }

    void setMaxChats(int maxChats) {
        this.maxChats = maxChats;
    }

    /**
     * Returns the current number of active chat sessions in the queue.
     *
     * @return the current number of active chat sessions in the queue.
     */
    public int getCurrentChats() {
        return currentChats;
    }

    void setCurrentChats(int currentChats) {
        this.currentChats = currentChats;
    }

    /**
     * A class to represent the status of the workgroup. The possible values are:
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