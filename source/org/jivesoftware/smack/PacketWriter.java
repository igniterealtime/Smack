/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright (C) 2002-2003 Jive Software. All rights reserved.
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
                queue.notify();
            }
            // Add the sent packet to the list of sent packets
            // The PacketWriterListeners will be notified of the new packet
            synchronized(sentPackets) {
                sentPackets.addFirst(packet);
                sentPackets.notify();
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
        listenerThread.start();
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
            StringBuffer stream = new StringBuffer();
            stream.append("<stream:stream");
            stream.append(" to=\"" + connection.getHost() + "\"");
            stream.append(" xmlns=\"jabber:client\"");
            stream.append(" xmlns:stream=\"http://etherx.jabber.org/streams\">");
            writer.write(stream.toString());
            writer.flush();
            stream = null;
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
            synchronized(sentPackets) {
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
                // Clean up null entries in the listeners list
                synchronized (listeners) {
                    if (listeners.size() > 0) {
                        for (int i=listeners.size()-1; i>=0; i--) {
                            if (listeners.get(i) == null) {
                                listeners.remove(i);
                            }
                        }
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
     * character. The
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
                    }
                    catch (Exception e) { }
                }
                try {
                    // Sleep 30 seconds.
                    Thread.sleep(delay);
                }
                catch (InterruptedException ie) { }
            }
        }
    }
}