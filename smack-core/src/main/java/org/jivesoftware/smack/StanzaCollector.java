/**
 *
 * Copyright 2003-2007 Jive Software, 2016-2017 Florian Schmaus.
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

import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.packet.Stanza;

/**
 * Provides a mechanism to collect Stanzas into a result queue that pass a
 * specified filter/matcher. The collector lets you perform blocking and polling
 * operations on the result queue. So, a StanzaCollector is more suitable to
 * use than a {@link StanzaListener} when you need to wait for a specific
 * result.<p>
 *
 * Each stanza(/packet) collector will queue up a configured number of packets for processing before
 * older packets are automatically dropped.  The default number is retrieved by 
 * {@link SmackConfiguration#getStanzaCollectorSize()}.
 *
 * @see XMPPConnection#createStanzaCollector(StanzaFilter)
 * @author Matt Tucker
 */
public class StanzaCollector {

    private final StanzaFilter packetFilter;

    private final ArrayBlockingQueue<Stanza> resultQueue;

    /**
     * The stanza(/packet) collector which timeout for the next result will get reset once this collector collects a stanza.
     */
    private final StanzaCollector collectorToReset;

    private final XMPPConnection connection;

    private boolean cancelled = false;

    /**
     * Creates a new stanza(/packet) collector. If the stanza(/packet) filter is <tt>null</tt>, then
     * all packets will match this collector.
     *
     * @param connection the connection the collector is tied to.
     * @param configuration the configuration used to construct this collector
     */
    protected StanzaCollector(XMPPConnection connection, Configuration configuration) {
        this.connection = connection;
        this.packetFilter = configuration.packetFilter;
        this.resultQueue = new ArrayBlockingQueue<>(configuration.size);
        this.collectorToReset = configuration.collectorToReset;
    }

    /**
     * Explicitly cancels the stanza(/packet) collector so that no more results are
     * queued up. Once a stanza(/packet) collector has been cancelled, it cannot be
     * re-enabled. Instead, a new stanza(/packet) collector must be created.
     */
    public void cancel() {
        // If the packet collector has already been cancelled, do nothing.
        if (!cancelled) {
            cancelled = true;
            connection.removeStanzaCollector(this);
        }
    }

    /**
     * Returns the stanza(/packet) filter associated with this stanza(/packet) collector. The packet
     * filter is used to determine what packets are queued as results.
     *
     * @return the stanza(/packet) filter.
     * @deprecated use {@link #getStanzaFilter()} instead.
     */
    @Deprecated
    public StanzaFilter getPacketFilter() {
        return getStanzaFilter();
    }

    /**
     * Returns the stanza filter associated with this stanza collector. The stanza
     * filter is used to determine what stanzas are queued as results.
     *
     * @return the stanza filter.
     */
    public StanzaFilter getStanzaFilter() {
        return packetFilter;
    }

    /**
     * Polls to see if a stanza(/packet) is currently available and returns it, or
     * immediately returns <tt>null</tt> if no packets are currently in the
     * result queue.
     *
     * @return the next stanza(/packet) result, or <tt>null</tt> if there are no more
     *      results.
     */
    @SuppressWarnings("unchecked")
    public <P extends Stanza> P pollResult() {
        return (P) resultQueue.poll();
    }

    /**
     * Polls to see if a stanza(/packet) is currently available and returns it, or
     * immediately returns <tt>null</tt> if no packets are currently in the
     * result queue.
     * <p>
     * Throws an XMPPErrorException in case the polled stanzas did contain an XMPPError.
     * </p>
     * 
     * @return the next available packet.
     * @throws XMPPErrorException in case an error response.
     */
    public <P extends Stanza> P pollResultOrThrow() throws XMPPErrorException {
        P result = pollResult();
        if (result != null) {
            XMPPErrorException.ifHasErrorThenThrow(result);
        }
        return result;
    }

    /**
     * Returns the next available packet. The method call will block (not return) until a stanza(/packet) is
     * available.
     * 
     * @return the next available packet.
     * @throws InterruptedException 
     */
    @SuppressWarnings("unchecked")
    public <P extends Stanza> P nextResultBlockForever() throws InterruptedException {
        throwIfCancelled();
        P res = null;
        while (res == null) {
            res = (P) resultQueue.take();
        }
        return res;
    }

    /**
     * Returns the next available packet. The method call will block until the connection's default
     * timeout has elapsed.
     * 
     * @return the next available packet.
     * @throws InterruptedException 
     */
    public <P extends Stanza> P nextResult() throws InterruptedException {
        return nextResult(connection.getReplyTimeout());
    }

    private volatile long waitStart;

    /**
     * Returns the next available packet. The method call will block (not return)
     * until a stanza(/packet) is available or the <tt>timeout</tt> has elapsed. If the
     * timeout elapses without a result, <tt>null</tt> will be returned.
     *
     * @param timeout the timeout in milliseconds.
     * @return the next available packet.
     * @throws InterruptedException 
     */
    @SuppressWarnings("unchecked")
    public <P extends Stanza> P nextResult(long timeout) throws InterruptedException {
        throwIfCancelled();
        P res = null;
        long remainingWait = timeout;
        waitStart = System.currentTimeMillis();
        do {
            res = (P) resultQueue.poll(remainingWait, TimeUnit.MILLISECONDS);
            if (res != null) {
                return res;
            }
            remainingWait = timeout - (System.currentTimeMillis() - waitStart);
        } while (remainingWait > 0);
        return null;
    }

    /**
     * Returns the next available stanza. The method in equivalent to
     * {@link #nextResultOrThrow(long)} where the timeout argument is the default reply timeout of
     * the connection associated with this collector.
     * 
     * @return the next available stanza.
     * @throws XMPPErrorException in case an error response was received.
     * @throws NoResponseException if there was no response from the server.
     * @throws InterruptedException
     * @throws NotConnectedException
     * @see #nextResultOrThrow(long)
     */
    public <P extends Stanza> P nextResultOrThrow() throws NoResponseException, XMPPErrorException,
                    InterruptedException, NotConnectedException {
        return nextResultOrThrow(connection.getReplyTimeout());
    }

    /**
     * Returns the next available stanza. The method call will block until a stanza is
     * available or the <tt>timeout</tt> has elapsed. This method does also cancel the
     * collector in every case.
     * <p>
     * Three things can happen when waiting for an response:
     * </p>
     * <ol>
     * <li>A result response arrives.</li>
     * <li>An error response arrives.</li>
     * <li>An timeout occurs.</li>
     * <li>The thread is interrupted</li>
     * </ol>
     * <p>
     * in which this method will
     * </p>
     * <ol>
     * <li>return with the result.</li>
     * <li>throw an {@link XMPPErrorException}.</li>
     * <li>throw an {@link NoResponseException}.</li>
     * <li>throw an {@link InterruptedException}.</li>
     * </ol>
     * <p>
     * Additionally the method will throw a {@link NotConnectedException} if no response was
     * received and the connection got disconnected.
     * </p>
     *
     * @param timeout the amount of time to wait for the next stanza in milliseconds.
     * @return the next available stanza.
     * @throws NoResponseException if there was no response from the server.
     * @throws XMPPErrorException in case an error response was received.
     * @throws InterruptedException if the calling thread was interrupted.
     * @throws NotConnectedException if there was no response and the connection got disconnected.
     */
    public <P extends Stanza> P nextResultOrThrow(long timeout) throws NoResponseException,
                    XMPPErrorException, InterruptedException, NotConnectedException {
        P result = nextResult(timeout);
        cancel();
        if (result == null) {
            if (!connection.isConnected()) {
                throw new NotConnectedException(connection, packetFilter);
            }
            throw NoResponseException.newWith(connection, this);
        }

        XMPPErrorException.ifHasErrorThenThrow(result);

        return result;
    }

    /**
     * Get the number of collected stanzas this stanza(/packet) collector has collected so far.
     * 
     * @return the count of collected stanzas.
     * @since 4.1
     */
    public int getCollectedCount() {
        return resultQueue.size();
    }

    /**
     * Processes a stanza(/packet) to see if it meets the criteria for this stanza(/packet) collector.
     * If so, the stanza(/packet) is added to the result queue.
     *
     * @param packet the stanza(/packet) to process.
     */
    protected void processStanza(Stanza packet) {
        if (packetFilter == null || packetFilter.accept(packet)) {
            // CHECKSTYLE:OFF
        	while (!resultQueue.offer(packet)) {
        		// Since we know the queue is full, this poll should never actually block.
        		resultQueue.poll();
        	}
            // CHECKSTYLE:ON
            if (collectorToReset != null) {
                collectorToReset.waitStart = System.currentTimeMillis();
            }
        }
    }

    private void throwIfCancelled() {
        if (cancelled) {
            throw new IllegalStateException("Packet collector already cancelled");
        }
    }

    /**
     * Get a new stanza(/packet) collector configuration instance.
     * 
     * @return a new stanza(/packet) collector configuration.
     */
    public static Configuration newConfiguration() {
        return new Configuration();
    }

    public static final class Configuration {
        private StanzaFilter packetFilter;
        private int size = SmackConfiguration.getStanzaCollectorSize();
        private StanzaCollector collectorToReset;

        private Configuration() {
        }

        /**
         * Set the stanza(/packet) filter used by this collector. If <code>null</code>, then all packets will
         * get collected by this collector.
         * 
         * @param packetFilter
         * @return a reference to this configuration.
         * @deprecated use {@link #setStanzaFilter(StanzaFilter)} instead.
         */
        @Deprecated
        public Configuration setPacketFilter(StanzaFilter packetFilter) {
            return setStanzaFilter(packetFilter);
        }

        /**
         * Set the stanza filter used by this collector. If <code>null</code>, then all stanzas will
         * get collected by this collector.
         * 
         * @param stanzaFilter
         * @return a reference to this configuration.
         */
        public Configuration setStanzaFilter(StanzaFilter stanzaFilter) {
            this.packetFilter = stanzaFilter;
            return this;
        }

        /**
         * Set the maximum size of this collector, i.e. how many stanzas this collector will collect
         * before dropping old ones.
         * 
         * @param size
         * @return a reference to this configuration.
         */
        public Configuration setSize(int size) {
            this.size = size;
            return this;
        }

        /**
         * Set the collector which timeout for the next result is reset once this collector collects
         * a packet.
         * 
         * @param collector
         * @return a reference to this configuration.
         */
        public Configuration setCollectorToReset(StanzaCollector collector) {
            this.collectorToReset = collector;
            return this;
        }
    }
}
