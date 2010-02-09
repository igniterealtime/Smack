/**
 * $RCSfile$
 * $Revision: 2779 $
 * $Date: 2005-09-05 17:00:45 -0300 (Mon, 05 Sep 2005) $
 *
 * Copyright 2003-2006 Jive Software.
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

package org.jivesoftware.smackx.muc;

import org.jivesoftware.smack.packet.Packet;

import java.util.LinkedList;

/**
 * A variant of the {@link org.jivesoftware.smack.PacketCollector} class
 * that does not force attachment to a <code>Connection</code>
 * on creation and no filter is required. Used to collect message
 * packets targeted to a group chat room.
 *
 * @author Larry Kirschner
 */
class ConnectionDetachedPacketCollector {
    /**
     * Max number of packets that any one collector can hold. After the max is
     * reached, older packets will be automatically dropped from the queue as
     * new packets are added.
     */
    private static final int MAX_PACKETS = 65536;

    private LinkedList<Packet> resultQueue;

    /**
     * Creates a new packet collector. If the packet filter is <tt>null</tt>, then
     * all packets will match this collector.
     */
    public ConnectionDetachedPacketCollector() {
        this.resultQueue = new LinkedList<Packet>();
    }

    /**
     * Polls to see if a packet is currently available and returns it, or
     * immediately returns <tt>null</tt> if no packets are currently in the
     * result queue.
     *
     * @return the next packet result, or <tt>null</tt> if there are no more
     *      results.
     */
    public synchronized Packet pollResult() {
        if (resultQueue.isEmpty()) {
            return null;
        }
        else {
            return resultQueue.removeLast();
        }
    }

    /**
     * Returns the next available packet. The method call will block (not return)
     * until a packet is available.
     *
     * @return the next available packet.
     */
    public synchronized Packet nextResult() {
        // Wait indefinitely until there is a result to return.
        while (resultQueue.isEmpty()) {
            try {
                wait();
            }
            catch (InterruptedException ie) {
                // Ignore.
            }
        }
        return resultQueue.removeLast();
    }

    /**
     * Returns the next available packet. The method call will block (not return)
     * until a packet is available or the <tt>timeout</tt> has elapased. If the
     * timeout elapses without a result, <tt>null</tt> will be returned.
     *
     * @param timeout the amount of time to wait for the next packet (in milleseconds).
     * @return the next available packet.
     */
    public synchronized Packet nextResult(long timeout) {
        // Wait up to the specified amount of time for a result.
        if (resultQueue.isEmpty()) {
            try {
                wait(timeout);
            }
            catch (InterruptedException ie) {
                // Ignore.
            }
        }
        // If still no result, return null.
        if (resultQueue.isEmpty()) {
            return null;
        }
        else {
            return resultQueue.removeLast();
        }
    }

    /**
     * Processes a packet to see if it meets the criteria for this packet collector.
     * If so, the packet is added to the result queue.
     *
     * @param packet the packet to process.
     */
    protected synchronized void processPacket(Packet packet) {
        if (packet == null) {
            return;
        }
        // If the max number of packets has been reached, remove the oldest one.
        if (resultQueue.size() == MAX_PACKETS) {
            resultQueue.removeLast();
        }
        // Add the new packet.
        resultQueue.addFirst(packet);
        // Notify waiting threads a result is available.
        notifyAll();
    }
}
