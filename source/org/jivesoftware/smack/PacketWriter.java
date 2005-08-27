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

import java.util.*;
import java.io.*;

import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Packet;

/**
 * Writes packets to a XMPP server.
 *
 * @author Matt Tucker
 */
class PacketWriter {

    private Thread writerThread;
    private Writer writer;
    private XMPPConnection connection;
    private LinkedList queue;
    private boolean done = false;
    
    private List listeners = new ArrayList();
    private boolean listenersDeleted = false;
    private Thread listenerThread;
    private LinkedList sentPackets = new LinkedList();

    /**
     * Creates a new packet writer with the specified connection.
     *
     * @param connection the connection.
     */
    protected PacketWriter(XMPPConnection connection) {
        this.connection = connection;
        this.writer = connection.writer;
        this.queue = new LinkedList();

        writerThread = new Thread() {
            public void run() {
                writePackets();
            }
        };
        writerThread.setName("Smack Packet Writer");
        writerThread.setDaemon(true);

        listenerThread = new Thread() {
            public void run() {
                processListeners();
            }
        };
        listenerThread.setName("Smack Writer Listener Processor");
        listenerThread.setDaemon(true);

        // Schedule a keep-alive task to run if the feature is enabled. will write
        // out a space character each time it runs to keep the TCP/IP connection open.
        int keepAliveInterval = SmackConfiguration.getKeepAliveInterval();
        if (keepAliveInterval > 0) {
            Thread keepAliveThread = new Thread(new KeepAliveTask(keepAliveInterval));
            keepAliveThread.setDaemon(true);
            keepAliveThread.start();
        }
    }

    /**
     * Sends the specified packet to the server.
     *
     * @param packet the packet to send.
     */
    public void sendPacket(Packet packet) {
        if (!done) {
            synchronized(queue) {
                queue.addFirst(packet);
                queue.notifyAll();
            }
            // Add the sent packet to the list of sent packets. The
            // PacketWriterListeners will be notified of the new packet.
            synchronized(sentPackets) {
                sentPackets.addFirst(packet);
                sentPackets.notifyAll();
            }
        }
    }

    /**
     * Registers a packet listener with this writer. The listener will be
     * notified of every packet that this writer sends. A packet filter determines
     * which packets will be delivered to the listener.
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
                ListenerWrapper wrapper = (ListenerWrapper)listeners.get(i);
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
     * Starts the packet writer thread and opens a connection to the server. The
     * packet writer will continue writing packets until {@link #shutdown} or an
     * error occurs.
     */
    public void startup() {
        writerThread.start();
        listenerThread.start();
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
                catch (InterruptedException ie) { }
            }
            if (queue.size() > 0) {
                return (Packet)queue.removeLast();
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
                    }
                }
            }
            // Close the stream.
            try {
                writer.write("</stream:stream>");
                writer.flush();
            }
            catch (Exception e) { }
            finally {
                try {
                    writer.close();
                }
                catch (Exception e) { }
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
    private void processListeners() {
        while (!done) {
            Packet sentPacket;
            // Wait until a new packet has been sent
            synchronized (sentPackets) {
                while (!done && sentPackets.size() == 0) {
                    try {
                        sentPackets.wait(2000);
                    }
                    catch (InterruptedException ie) { }
                }
                if (sentPackets.size() > 0) {
                    sentPacket = (Packet)sentPackets.removeLast();
                }
                else {
                    sentPacket = null;
                }
            }
            if (sentPacket != null) {
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
                    ListenerWrapper listenerWrapper = (ListenerWrapper)listeners.get(i);
                    if (listenerWrapper != null) {
                        listenerWrapper.notifyListener(sentPacket);
                    }
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
        StringBuffer stream = new StringBuffer();
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
     * A TimerTask that keeps connections to the server alive by sending a space
     * character on an interval.
     */
    private class KeepAliveTask implements Runnable {

        private int delay;

        public KeepAliveTask(int delay) {
            this.delay = delay;
        }

        public void run() {
            while (!done) {
                synchronized (writer) {
                    try {
                        writer.write(" ");
                        writer.flush();
                    }
                    catch (Exception e) { }
                }
                try {
                    // Sleep until we should write the next keep-alive.
                    Thread.sleep(delay);
                }
                catch (InterruptedException ie) { }
            }
        }
    }
}