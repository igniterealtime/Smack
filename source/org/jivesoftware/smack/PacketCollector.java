/**
 * $RCSfile$
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

package org.jivesoftware.smack;

import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Packet;

import java.util.LinkedList;

/**
 * Provides a mechanism to collect packets into a result queue that pass a
 * specified filter. The collector lets you perform blocking and polling
 * operations on the result queue. So, a PacketCollector is more suitable to
 * use than a {@link PacketListener} when you need to wait for a specific
 * result.<p>
 *
 * Each packet collector will queue up a configured number of packets for processing before
 * older packets are automatically dropped.  The default number is retrieved by 
 * {@link SmackConfiguration#getPacketCollectorSize()}.
 *
 * @see Connection#createPacketCollector(PacketFilter)
 * @author Matt Tucker
 */
public class PacketCollector {

    /**
     * Max number of packets that any one collector can hold. After the max is
     * reached, older packets will be automatically dropped from the queue as
     * new packets are added.
     */
    private int maxPackets = SmackConfiguration.getPacketCollectorSize();

    private PacketFilter packetFilter;
    private LinkedList<Packet> resultQueue;
    private Connection conection;
    private boolean cancelled = false;

    /**
     * Creates a new packet collector. If the packet filter is <tt>null</tt>, then
     * all packets will match this collector.
     *
     * @param conection the connection the collector is tied to.
     * @param packetFilter determines which packets will be returned by this collector.
     */
    protected PacketCollector(Connection conection, PacketFilter packetFilter) {
        this.conection = conection;
        this.packetFilter = packetFilter;
        this.resultQueue = new LinkedList<Packet>();
    }

    /**
     * Creates a new packet collector. If the packet filter is <tt>null</tt>, then
     * all packets will match this collector.
     *
     * @param conection the connection the collector is tied to.
     * @param packetFilter determines which packets will be returned by this collector.
     * @param maxSize the maximum number of packets that will be stored in the collector.
     */
    protected PacketCollector(Connection conection, PacketFilter packetFilter, int maxSize) {
        this(conection, packetFilter);
        maxPackets = maxSize;
    }

    /**
     * Explicitly cancels the packet collector so that no more results are
     * queued up. Once a packet collector has been cancelled, it cannot be
     * re-enabled. Instead, a new packet collector must be created.
     */
    public void cancel() {
        // If the packet collector has already been cancelled, do nothing.
        if (!cancelled) {
            cancelled = true;
            conection.removePacketCollector(this);
        }
    }

    /**
     * Returns the packet filter associated with this packet collector. The packet
     * filter is used to determine what packets are queued as results.
     *
     * @return the packet filter.
     */
    public PacketFilter getPacketFilter() {
        return packetFilter;
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
            long waitTime = timeout;
            long start = System.currentTimeMillis();
            try {
                // Keep waiting until the specified amount of time has elapsed, or
                // a packet is available to return.
                while (resultQueue.isEmpty()) {
                    if (waitTime <= 0) {
                        break;
                    }
                    wait(waitTime);
                    long now = System.currentTimeMillis();
                    waitTime -= (now - start);
                    start = now;
                }
            }
            catch (InterruptedException ie) {
                // Ignore.
            }
            // Still haven't found a result, so return null.
            if (resultQueue.isEmpty()) {
                return null;
            }
            // Return the packet that was found.
            else {
                return resultQueue.removeLast();
            }
        }
        // There's already a packet waiting, so return it.
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
        if (packetFilter == null || packetFilter.accept(packet)) {
            // If the max number of packets has been reached, remove the oldest one.
            if (resultQueue.size() == maxPackets) {
                resultQueue.removeLast();
            }
            // Add the new packet.
            resultQueue.addFirst(packet);
            // Notify waiting threads a result is available.
            notifyAll();
        }
    }
}
