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

package org.jivesoftware.smack;

import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Packet;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Writes packets to a XMPP server. Packets are sent using a dedicated thread. Packet
 * interceptors can be registered to dynamically modify packets before they're actually
 * sent. Packet listeners can be registered to listen for all outgoing packets.
 *
 * @author Matt Tucker
 */
class PacketWriter {

    private Thread writerThread;
    private Writer writer;
    private XMPPConnection connection;
    final private LinkedList<Packet> queue;
    private boolean done = false;
    
    final private List<ListenerWrapper> listeners = new ArrayList<ListenerWrapper>();
    private boolean listenersDeleted = false;

    /**
     * Timestamp when the last stanza was sent to the server. This information is used
     * by the keep alive process to only send heartbeats when the connection has been idle.
     */
    private long lastActive = System.currentTimeMillis();

    /**
     * List of PacketInterceptor that will be notified when a new packet is about to be
     * sent to the server. These interceptors may modify the packet before it is being
     * actually sent to the server.
     */
    final private List interceptors = new ArrayList();
    /**
     * Flag that indicates if an interceptor was deleted. This is an optimization flag.
     */
    private boolean interceptorDeleted = false;

    /**
     * Creates a new packet writer with the specified connection.
     *
     * @param connection the connection.
     */
    protected PacketWriter(XMPPConnection connection) {
        this.connection = connection;
        this.writer = connection.writer;
        this.queue = new LinkedList<Packet>();

        writerThread = new Thread() {
            public void run() {
                writePackets();
            }
        };
        writerThread.setName("Smack Packet Writer");
        writerThread.setDaemon(true);
    }

    /**
     * Sends the specified packet to the server.
     *
     * @param packet the packet to send.
     */
    public void sendPacket(Packet packet) {
        if (!done) {
            // Invoke interceptors for the new packet that is about to be sent. Interceptors
            // may modify the content of the packet.
            processInterceptors(packet);

            synchronized(queue) {
                queue.addFirst(packet);
                queue.notifyAll();
            }

            // Process packet writer listeners. Note that we're using the sending
            // thread so it's expected that listeners are fast.
            processListeners(packet);
        }
    }

    /**
     * Registers a packet listener with this writer. The listener will be
     * notified immediately after every packet this writer sends. A packet filter
     * determines which packets will be delivered to the listener. Note that the thread
     * that writes packets will be used to invoke the listeners. Therefore, each
     * packet listener should complete all operations quickly or use a different
     * thread for processing.
     *
     * @param packetListener the packet listener to notify of sent packets.
     * @param packetFilter the packet filter to use.
     */
    public void addPacketListener(PacketListener packetListener, PacketFilter packetFilter) {
        synchronized (listeners) {
            listeners.add(new ListenerWrapper(packetListener, packetFilter));
        }
    }

    /**
     * Removes a packet listener.
     *
     * @param packetListener the packet listener to remove.
     */
    public void removePacketListener(PacketListener packetListener) {
        synchronized (listeners) {
            for (int i=0; i<listeners.size(); i++) {
                ListenerWrapper wrapper = listeners.get(i);
                if (wrapper != null && wrapper.packetListener.equals(packetListener)) {
                    listeners.set(i, null);
                    // Set the flag to indicate that the listener list needs
                    // to be cleaned up.
                    listenersDeleted = true;
                }
            }
        }
    }

    /**
     * Returns the number of registered packet listeners.
     *
     * @return the count of packet listeners.
     */
    public int getPacketListenerCount() {
        synchronized (listeners) {
            return listeners.size();
        }
    }

    /**
     * Registers a packet interceptor with this writer. The interceptor will be
     * notified of every packet that this writer is about to send. Interceptors
     * may modify the packet to be sent. A packet filter determines which packets
     * will be delivered to the interceptor.
     *
     * @param packetInterceptor the packet interceptor to notify of packets about to be sent.
     * @param packetFilter the packet filter to use.
     */
    public void addPacketInterceptor(PacketInterceptor packetInterceptor, PacketFilter packetFilter) {
        synchronized (interceptors) {
            interceptors.add(new InterceptorWrapper(packetInterceptor, packetFilter));
        }
    }

    /**
     * Removes a packet interceptor.
     *
     * @param packetInterceptor the packet interceptor to remove.
     */
    public void removePacketInterceptor(PacketInterceptor packetInterceptor) {
        synchronized (interceptors) {
            for (int i=0; i<interceptors.size(); i++) {
                InterceptorWrapper wrapper = (InterceptorWrapper)interceptors.get(i);
                if (wrapper != null && wrapper.packetInterceptor.equals(packetInterceptor)) {
                    interceptors.set(i, null);
                    // Set the flag to indicate that the interceptor list needs
                    // to be cleaned up.
                    interceptorDeleted = true;
                }
            }
        }
    }

    /**
     * Starts the packet writer thread and opens a connection to the server. The
     * packet writer will continue writing packets until {@link #shutdown} or an
     * error occurs.
     */
    public void startup() {
        writerThread.start();
    }

    /**
     * Starts the keep alive process. A white space (aka heartbeat) is going to be
     * sent to the server every 30 seconds (by default) since the last stanza was sent
     * to the server.
     */
    void startKeepAliveProcess() {
        // Schedule a keep-alive task to run if the feature is enabled. will write
        // out a space character each time it runs to keep the TCP/IP connection open.
        int keepAliveInterval = SmackConfiguration.getKeepAliveInterval();
        if (keepAliveInterval > 0) {
            Thread keepAliveThread = new Thread(new KeepAliveTask(keepAliveInterval));
            keepAliveThread.setDaemon(true);
            keepAliveThread.start();
        }
    }

    void setWriter(Writer writer) {
        this.writer = writer;
    }

    /**
     * Shuts down the packet writer. Once this method has been called, no further
     * packets will be written to the server.
     */
    public void shutdown() {
        done = true;
    }

    /**
     * Returns the next available packet from the queue for writing.
     *
     * @return the next packet for writing.
     */
    private Packet nextPacket() {
        synchronized(queue) {
            while (!done && queue.size() == 0) {
                try {
                    queue.wait(2000);
                }
                catch (InterruptedException ie) {
                    // Do nothing
                }
            }
            if (queue.size() > 0) {
                return queue.removeLast();
            }
            else {
                return null;
            }
        }
    }

    private void writePackets() {
        try {
            // Open the stream.
            openStream();
            // Write out packets from the queue.
            while (!done) {
                Packet packet = nextPacket();
                if (packet != null) {
                    synchronized (writer) {
                        writer.write(packet.toXML());
                        writer.flush();
                        // Keep track of the last time a stanza was sent to the server
                        lastActive = System.currentTimeMillis();
                    }
                }
            }
            // Close the stream.
            try {
                writer.write("</stream:stream>");
                writer.flush();
            }
            catch (Exception e) {
                // Do nothing
            }
            finally {
                try {
                    writer.close();
                }
                catch (Exception e) {
                    // Do nothing
                }
            }
        }
        catch (IOException ioe){
            if (!done) {
                done = true;
                connection.packetReader.notifyConnectionError(ioe);
            }
        }
    }

    /**
     * Process listeners.
     */
    private void processListeners(Packet packet) {
        // Clean up null entries in the listeners list if the flag is set. List
        // removes are done seperately so that the main notification process doesn't
        // need to synchronize on the list.
        synchronized (listeners) {
            if (listenersDeleted) {
                for (int i=listeners.size()-1; i>=0; i--) {
                    if (listeners.get(i) == null) {
                        listeners.remove(i);
                    }
                }
                listenersDeleted = false;
            }
        }
        // Notify the listeners of the new sent packet
        int size = listeners.size();
        for (int i=0; i<size; i++) {
            ListenerWrapper listenerWrapper = listeners.get(i);
            if (listenerWrapper != null) {
                listenerWrapper.notifyListener(packet);
            }
        }
    }

    /**
     * Process interceptors. Interceptors may modify the packet that is about to be sent.
     * Since the thread that requested to send the packet will invoke all interceptors, it
     * is important that interceptors perform their work as soon as possible so that the
     * thread does not remain blocked for a long period.
     *
     * @param packet the packet that is going to be sent to the server
     */
    private void processInterceptors(Packet packet) {
        if (packet != null) {
            // Clean up null entries in the interceptors list if the flag is set. List
            // removes are done seperately so that the main notification process doesn't
            // need to synchronize on the list.
            synchronized (interceptors) {
                if (interceptorDeleted) {
                    for (int i=interceptors.size()-1; i>=0; i--) {
                        if (interceptors.get(i) == null) {
                            interceptors.remove(i);
                        }
                    }
                    interceptorDeleted = false;
                }
            }
            // Notify the interceptors of the new packet to be sent
            int size = interceptors.size();
            for (int i=0; i<size; i++) {
                InterceptorWrapper interceptorWrapper = (InterceptorWrapper)interceptors.get(i);
                if (interceptorWrapper != null) {
                    interceptorWrapper.notifyListener(packet);
                }
            }
        }
    }

    /**
     * Sends to the server a new stream element. This operation may be requested several times
     * so we need to encapsulate the logic in one place. This message will be sent while doing
     * TLS, SASL and resource binding.
     *
     * @throws IOException If an error occurs while sending the stanza to the server.
     */
    void openStream() throws IOException {
        StringBuilder stream = new StringBuilder();
        stream.append("<stream:stream");
        stream.append(" to=\"").append(connection.serviceName).append("\"");
        stream.append(" xmlns=\"jabber:client\"");
        stream.append(" xmlns:stream=\"http://etherx.jabber.org/streams\"");
        stream.append(" version=\"1.0\">");
        writer.write(stream.toString());
        writer.flush();
    }

    /**
     * A wrapper class to associate a packet filter with a listener.
     */
    private static class ListenerWrapper {

        private PacketListener packetListener;
        private PacketFilter packetFilter;

        public ListenerWrapper(PacketListener packetListener,
                               PacketFilter packetFilter)
        {
            this.packetListener = packetListener;
            this.packetFilter = packetFilter;
        }

        public boolean equals(Object object) {
            if (object == null) {
                return false;
            }
            if (object instanceof ListenerWrapper) {
                return ((ListenerWrapper)object).packetListener.equals(this.packetListener);
            }
            else if (object instanceof PacketListener) {
                return object.equals(this.packetListener);
            }
            return false;
        }

        public void notifyListener(Packet packet) {
            if (packetFilter == null || packetFilter.accept(packet)) {
                packetListener.processPacket(packet);
            }
        }
    }

    /**
     * A wrapper class to associate a packet filter with an interceptor.
     */
    private static class InterceptorWrapper {

        private PacketInterceptor packetInterceptor;
        private PacketFilter packetFilter;

        public InterceptorWrapper(PacketInterceptor packetInterceptor, PacketFilter packetFilter)
        {
            this.packetInterceptor = packetInterceptor;
            this.packetFilter = packetFilter;
        }

        public boolean equals(Object object) {
            if (object == null) {
                return false;
            }
            if (object instanceof InterceptorWrapper) {
                return ((InterceptorWrapper) object).packetInterceptor
                        .equals(this.packetInterceptor);
            }
            else if (object instanceof PacketInterceptor) {
                return object.equals(this.packetInterceptor);
            }
            return false;
        }

        public void notifyListener(Packet packet) {
            if (packetFilter == null || packetFilter.accept(packet)) {
                packetInterceptor.interceptPacket(packet);
            }
        }
    }

    /**
     * A TimerTask that keeps connections to the server alive by sending a space
     * character on an interval.
     */
    private class KeepAliveTask implements Runnable {

        private int delay;

        public KeepAliveTask(int delay) {
            this.delay = delay;
        }

        public void run() {
            try {
                // Sleep 15 seconds before sending first heartbeat. This will give time to
                // properly finish TLS negotiation and then start sending heartbeats.
                Thread.sleep(15000);
            }
            catch (InterruptedException ie) {
                // Do nothing
            }
            while (!done) {
                synchronized (writer) {
                    // Send heartbeat if no packet has been sent to the server for a given time
                    if (System.currentTimeMillis() - lastActive >= delay) {
                        try {
                            writer.write(" ");
                            writer.flush();
                        }
                        catch (Exception e) {
                            // Do nothing
                        }
                    }
                }
                try {
                    // Sleep until we should write the next keep-alive.
                    Thread.sleep(delay);
                }
                catch (InterruptedException ie) {
                    // Do nothing
                }
            }
        }
    }
}