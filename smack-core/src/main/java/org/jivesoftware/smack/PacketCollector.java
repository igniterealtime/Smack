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

package org.jivesoftware.smack;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Packet;

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
 * @see XMPPConnection#createPacketCollector(PacketFilter)
 * @author Matt Tucker
 */
public class PacketCollector {

    private static final Logger LOGGER = Logger.getLogger(PacketCollector.class.getName());

    private final PacketFilter packetFilter;
    private final ArrayBlockingQueue<Packet> resultQueue;
    private final XMPPConnection connection;

    private boolean cancelled = false;

    /**
     * Creates a new packet collector. If the packet filter is <tt>null</tt>, then
     * all packets will match this collector.
     *
     * @param connection the connection the collector is tied to.
     * @param packetFilter determines which packets will be returned by this collector.
     */
    protected PacketCollector(XMPPConnection connection, PacketFilter packetFilter) {
        this(connection, packetFilter, SmackConfiguration.getPacketCollectorSize());
    }

    /**
     * Creates a new packet collector. If the packet filter is <tt>null</tt>, then
     * all packets will match this collector.
     *
     * @param connection the connection the collector is tied to.
     * @param packetFilter determines which packets will be returned by this collector.
     * @param maxSize the maximum number of packets that will be stored in the collector.
     */
    protected PacketCollector(XMPPConnection connection, PacketFilter packetFilter, int maxSize) {
        this.connection = connection;
        this.packetFilter = packetFilter;
        this.resultQueue = new ArrayBlockingQueue<Packet>(maxSize);
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
            connection.removePacketCollector(this);
        }
    }

    public boolean isCanceled() {
        return cancelled;
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
    @SuppressWarnings("unchecked")
    public <P extends Packet> P pollResult() {
        return (P) resultQueue.poll();
    }

    /**
     * Polls to see if a packet is currently available and returns it, or
     * immediately returns <tt>null</tt> if no packets are currently in the
     * result queue.
     * <p>
     * Throws an XMPPErrorException in case the polled stanzas did contain an XMPPError.
     * </p>
     * 
     * @return the next available packet.
     * @throws XMPPErrorException in case an error response.
     */
    public <P extends Packet> P pollResultOrThrow() throws XMPPErrorException {
        P result = pollResult();
        if (result != null) {
            XMPPErrorException.ifHasErrorThenThrow(result);
        }
        return result;
    }

    /**
     * Returns the next available packet. The method call will block (not return) until a packet is
     * available.
     * 
     * @return the next available packet.
     */
    @SuppressWarnings("unchecked")
    public <P extends Packet> P nextResultBlockForever() {
        P res = null;
        while (res == null) {
            try {
                res = (P) resultQueue.take();
            } catch (InterruptedException e) {
                LOGGER.log(Level.FINE,
                                "nextResultBlockForever was interrupted", e);
            }
        }
        return res;
    }

    /**
     * Returns the next available packet. The method call will block until the connection's default
     * timeout has elapsed.
     * 
     * @return the next availabe packet.
     */
    public <P extends Packet> P nextResult() {
        return nextResult(connection.getPacketReplyTimeout());
    }

    /**
     * Returns the next available packet. The method call will block (not return)
     * until a packet is available or the <tt>timeout</tt> has elapsed. If the
     * timeout elapses without a result, <tt>null</tt> will be returned.
     *
     * @param timeout the timeout in milliseconds.
     * @return the next available packet.
     */
    @SuppressWarnings("unchecked")
    public <P extends Packet> P nextResult(long timeout) {
        P res = null;
        long remainingWait = timeout;
        final long waitStart = System.currentTimeMillis();
        while (res == null && remainingWait > 0) {
            try {
                res = (P) resultQueue.poll(remainingWait, TimeUnit.MILLISECONDS);
                remainingWait = timeout - (System.currentTimeMillis() - waitStart);
            } catch (InterruptedException e) {
                LOGGER.log(Level.FINE, "nextResult was interrupted", e);
            }
        }
        return res;
    }

    /**
     * Returns the next available packet. The method call will block until a packet is available or
     * the connections reply timeout has elapsed. If the timeout elapses without a result,
     * <tt>null</tt> will be returned. This method does also cancel the PacketCollector.
     * 
     * @return the next available packet.
     * @throws XMPPErrorException in case an error response.
     * @throws NoResponseException if there was no response from the server.
     */
    public <P extends Packet> P nextResultOrThrow() throws NoResponseException, XMPPErrorException {
        return nextResultOrThrow(connection.getPacketReplyTimeout());
    }

    /**
     * Returns the next available packet. The method call will block until a packet is available or
     * the <tt>timeout</tt> has elapsed. This method does also cancel the PacketCollector.
     * 
     * @param timeout the amount of time to wait for the next packet (in milleseconds).
     * @return the next available packet.
     * @throws NoResponseException if there was no response from the server.
     * @throws XMPPErrorException in case an error response.
     */
    public <P extends Packet> P nextResultOrThrow(long timeout) throws NoResponseException, XMPPErrorException {
        P result = nextResult(timeout);
        cancel();
        if (result == null) {
            throw new NoResponseException(connection);
        }

        XMPPErrorException.ifHasErrorThenThrow(result);

        return result;
    }

    /**
     * Get the number of collected stanzas this packet collector has collected so far.
     * 
     * @return the count of collected stanzas.
     * @since 4.1
     */
    public int getCollectedCount() {
        return resultQueue.size();
    }

    /**
     * Processes a packet to see if it meets the criteria for this packet collector.
     * If so, the packet is added to the result queue.
     *
     * @param packet the packet to process.
     */
    protected void processPacket(Packet packet) {
        if (packetFilter == null || packetFilter.accept(packet)) {
        	while (!resultQueue.offer(packet)) {
        		// Since we know the queue is full, this poll should never actually block.
        		resultQueue.poll();
        	}
        }
    }
}
