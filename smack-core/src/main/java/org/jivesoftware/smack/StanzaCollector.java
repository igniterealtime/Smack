/**
 *
 * Copyright 2003-2007 Jive Software, 2016-2019 Florian Schmaus.
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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

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
 * Each stanza collector will queue up a configured number of packets for processing before
 * older packets are automatically dropped.  The default number is retrieved by
 * {@link SmackConfiguration#getStanzaCollectorSize()}.
 *
 * @see XMPPConnection#createStanzaCollector(StanzaFilter)
 * @author Matt Tucker
 */
public final class StanzaCollector implements AutoCloseable {

    private final StanzaFilter packetFilter;

    private final ArrayDeque<Stanza> resultQueue;

    private final int maxQueueSize;

    /**
     * The stanza collector which timeout for the next result will get reset once this collector collects a stanza.
     */
    private final StanzaCollector collectorToReset;

    private final XMPPConnection connection;

    private final Stanza request;

    private volatile boolean cancelled;

    private Exception connectionException;

    /**
     * Creates a new stanza collector. If the stanza filter is <code>null</code>, then
     * all packets will match this collector.
     *
     * @param connection the connection the collector is tied to.
     * @param configuration the configuration used to construct this collector
     */
    StanzaCollector(XMPPConnection connection, Configuration configuration) {
        this.connection = connection;
        this.packetFilter = configuration.packetFilter;
        this.resultQueue = new ArrayDeque<>(configuration.size);
        this.maxQueueSize = configuration.size;
        this.collectorToReset = configuration.collectorToReset;
        this.request = configuration.request;
    }

    /**
     * Explicitly cancels the stanza collector so that no more results are
     * queued up. Once a stanza collector has been cancelled, it cannot be
     * re-enabled. Instead, a new stanza collector must be created.
     */
    public synchronized void cancel() {
        // If the packet collector has already been cancelled, do nothing.
        if (cancelled) {
            return;
        }

        cancelled = true;
        connection.removeStanzaCollector(this);
        notifyAll();

        if (collectorToReset != null) {
            collectorToReset.cancel();
        }
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
     * Polls to see if a stanza is currently available and returns it, or
     * immediately returns <code>null</code> if no packets are currently in the
     * result queue.
     *
     * @param <P> type of the result stanza.
     * @return the next stanza result, or <code>null</code> if there are no more
     *      results.
     */
    @SuppressWarnings("unchecked")
    public synchronized <P extends Stanza> P pollResult() {
        return (P) resultQueue.poll();
    }

    /**
     * Polls to see if a stanza is currently available and returns it, or
     * immediately returns <code>null</code> if no packets are currently in the
     * result queue.
     * <p>
     * Throws an XMPPErrorException in case the polled stanzas did contain an XMPPError.
     * </p>
     *
     * @param <P> type of the result stanza.
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
     * Returns the next available packet. The method call will block (not return) until a stanza is
     * available.
     *
     * @param <P> type of the result stanza.
     * @return the next available packet.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    @SuppressWarnings("unchecked")
    // TODO: Consider removing this method as it is hardly ever useful.
    public synchronized <P extends Stanza> P nextResultBlockForever() throws InterruptedException {
        throwIfCancelled();

        while (true) {
            P res = (P) resultQueue.poll();
            if (res != null) {
                return res;
            }
            if (cancelled) {
                return null;
            }
            wait();
        }
    }

    /**
     * Returns the next available packet. The method call will block until the connection's default
     * timeout has elapsed.
     *
     * @param <P> type of the result stanza.
     * @return the next available packet.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public <P extends Stanza> P nextResult() throws InterruptedException {
        return nextResult(connection.getReplyTimeout());
    }

    private volatile long waitStart;

    /**
     * Returns the next available stanza. The method call will block (not return) until a stanza is available or the
     * <code>timeout</code> has elapsed or if the connection was terminated because of an error. If the timeout elapses without a
     * result or if there was an connection error, <code>null</code> will be returned.
     *
     * @param <P> type of the result stanza.
     * @param timeout the timeout in milliseconds.
     * @return the next available stanza or <code>null</code> on timeout or connection error.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    @SuppressWarnings("unchecked")
    public <P extends Stanza> P nextResult(long timeout) throws InterruptedException {
        throwIfCancelled();
        P res = null;
        long remainingWait = timeout;
        waitStart = System.currentTimeMillis();
        while (remainingWait > 0 && connectionException == null && !cancelled) {
            synchronized (this) {
                res = (P) resultQueue.poll();
                if (res != null) {
                    return res;
                }
                wait(remainingWait);
            }
            remainingWait = timeout - (System.currentTimeMillis() - waitStart);
        }
        return res;
    }

    /**
     * Returns the next available stanza. The method in equivalent to
     * {@link #nextResultOrThrow(long)} where the timeout argument is the default reply timeout of
     * the connection associated with this collector.
     *
     * @param <P> type of the result stanza.
     * @return the next available stanza.
     * @throws XMPPErrorException in case an error response was received.
     * @throws NoResponseException if there was no response from the server.
     * @throws InterruptedException if the calling thread was interrupted.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @see #nextResultOrThrow(long)
     */
    public <P extends Stanza> P nextResultOrThrow() throws NoResponseException, XMPPErrorException,
                    InterruptedException, NotConnectedException {
        return nextResultOrThrow(connection.getReplyTimeout());
    }

    /**
     * Returns the next available stanza. The method call will block until a stanza is
     * available or the <code>timeout</code> has elapsed. This method does also cancel the
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
     * @param <P> type of the result stanza.
     * @return the next available stanza.
     * @throws NoResponseException if there was no response from the server.
     * @throws XMPPErrorException in case an error response was received.
     * @throws InterruptedException if the calling thread was interrupted.
     * @throws NotConnectedException if there was no response and the connection got disconnected.
     */
    public <P extends Stanza> P nextResultOrThrow(long timeout) throws NoResponseException,
                    XMPPErrorException, InterruptedException, NotConnectedException {
        P result;
        try {
            result = nextResult(timeout);
        } finally {
            cancel();
        }
        if (result == null) {
            if (connectionException != null) {
                throw new NotConnectedException(connection, packetFilter, connectionException);
            }
            if (!connection.isConnected()) {
                throw new NotConnectedException(connection, packetFilter);
            }
            throw NoResponseException.newWith(timeout, this, cancelled);
        }

        XMPPErrorException.ifHasErrorThenThrow(result);

        return result;
    }

    private List<Stanza> collectedCache;

    /**
     * Return a list of all collected stanzas. This method must be invoked after the collector has been cancelled.
     *
     * @return a list of collected stanzas.
     * @since 4.3.0
     */
    public List<Stanza> getCollectedStanzasAfterCancelled() {
        if (!cancelled) {
            throw new IllegalStateException("Stanza collector was not yet cancelled");
        }

        if (collectedCache == null) {
            collectedCache = new ArrayList<>(getCollectedCount());
            collectedCache.addAll(resultQueue);
        }

        return collectedCache;
    }

    /**
     * Get the number of collected stanzas this stanza collector has collected so far.
     *
     * @return the count of collected stanzas.
     * @since 4.1
     */
    public synchronized int getCollectedCount() {
        return resultQueue.size();
    }

    private String stringCache;

    @Override
    public String toString() {
        if (stringCache == null) {
            StringBuilder sb = new StringBuilder(128);
            sb.append("Stanza Collector filter='").append(packetFilter).append('\'');
            if (request != null) {
                sb.append(" request='").append(request).append('\'');
            }
            stringCache = sb.toString();
        }
        return stringCache;
    }

    synchronized void notifyConnectionError(Exception exception) {
        connectionException = exception;
        notifyAll();
    }

    /**
     * Processes a stanza to see if it meets the criteria for this stanza collector.
     * If so, the stanza is added to the result queue.
     *
     * @param packet the stanza to process.
     */
    void processStanza(Stanza packet) {
        if (packetFilter == null || packetFilter.accept(packet)) {
            synchronized (this) {
                if (resultQueue.size() == maxQueueSize) {
                    Stanza rolledOverStanza = resultQueue.poll();
                    assert rolledOverStanza != null;
                }
                resultQueue.add(packet);
                notifyAll();
            }
            if (collectorToReset != null) {
                collectorToReset.waitStart = System.currentTimeMillis();
            }
        }
    }

    private void throwIfCancelled() {
        if (cancelled) {
            throw new IllegalStateException("Stanza collector already cancelled");
        }
    }

    /**
     * Get a new stanza collector configuration instance.
     *
     * @return a new stanza collector configuration.
     */
    public static Configuration newConfiguration() {
        return new Configuration();
    }

    public static final class Configuration {
        private StanzaFilter packetFilter;
        private int size = SmackConfiguration.getStanzaCollectorSize();
        private StanzaCollector collectorToReset;
        private Stanza request;

        private Configuration() {
        }

        /**
         * Set the stanza filter used by this collector. If <code>null</code>, then all stanzas will
         * get collected by this collector.
         *
         * @param stanzaFilter TODO javadoc me please
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
         * @param size TODO javadoc me please
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
         * @param collector TODO javadoc me please
         * @return a reference to this configuration.
         */
        public Configuration setCollectorToReset(StanzaCollector collector) {
            this.collectorToReset = collector;
            return this;
        }

        public Configuration setRequest(Stanza request) {
            this.request = request;
            return this;
        }
    }

    @Override
    public void close() {
        cancel();
    }

}
