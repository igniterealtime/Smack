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

import java.util.LinkedList;
import java.io.*;

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
            while (queue.size() == 0) {
                try {
                    queue.wait();
                }
                catch (InterruptedException ie) { }
            }
            return (Packet)queue.removeLast();
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
//            stream.append("xmlns:sasl=\"http://www.iana.org/assignments/sasl-mechanisms\" ");
            writer.write(stream.toString());
            writer.flush();
            stream = null;
            // Write out packets from the queue.
            while (!done) {
                Packet packet = nextPacket();
                writer.write(packet.toXML());
                writer.flush();
            }
            // Close the stream.
            try {
                writer.write("</stream>");
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
                ioe.printStackTrace();
                connection.close();
            }
        }
    }
}


