package org.jivesoftware.smackx.workgroup.agent;

import java.util.Set;

public interface QueueAgentsListener {

    /**
     * The current number of chats the agents are handling was updated.
     *
     * @param queue the workgroup queue.
     * @param currentChats the current number of chats the agents are handling.
     */
    public void currentChatsUpdated(WorkgroupQueue queue, int currentChats);

    /**
     * The maximum number of chats the agents can handle was updated.
     *
     * @param queue the workgroup queue.
     * @param maxChats the maximum number of chats the agents can handle.
     */
    public void maxChatsUpdated(WorkgroupQueue queue, int maxChats);

    /**
     * The list of available agents servicing the queue was updated.
     *
     * @param queue the workgroup queue.
     * @param agents the available agents servicing the queue.
     */
    public void agentsUpdated(WorkgroupQueue queue, Set agents);
}