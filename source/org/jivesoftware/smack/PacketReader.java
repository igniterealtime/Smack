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
 *    contact webmaster@coolservlets.com.
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

import org.xmlpull.v1.*;
import java.util.*;
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.packet.Error;
import org.jivesoftware.smack.filter.PacketFilter;

/**
 * Listens for XML traffic from the XMPP server, and parses it into packet objects.
 *
 * @see XMPPConnection#getPacketReader()
 * @see PacketCollector
 * @author Matt Tucker
 */
public class PacketReader {

    private Thread readerThread;
    private Thread listenerThread;

    private XMPPConnection connection;
    private XmlPullParser parser;
    private boolean done = false;
    protected List collectors = new ArrayList();
    private List listeners = Collections.synchronizedList(new ArrayList());

    private String connectionID = null;
    private Object connectionIDLock = new Object();

    protected PacketReader(XMPPConnection connection) {
        this.connection = connection;

        readerThread = new Thread() {
            public void run() {
                parsePackets();
            }
        };
        readerThread.setName("Smack Packet Reader");
        readerThread.setDaemon(true);

        listenerThread = new Thread() {
            public void run() {
                processListeners();
            }
        };
        listenerThread.setName("Smack Listener Processor");
        listenerThread.setDaemon(true);

        try {
            XmlPullParserFactory factory = XmlPullParserFactory.newInstance(
                    System.getProperty(XmlPullParserFactory.PROPERTY_NAME), null);
            factory.setNamespaceAware(true);
            parser = factory.newPullParser();
            parser.setInput(connection.reader);

        }
        catch (XmlPullParserException xppe) {
            xppe.printStackTrace();
        }
    }

    public PacketCollector createPacketCollector(PacketFilter packetFilter) {
        return new PacketCollector(this, packetFilter);
    }

    public void addPacketListener(PacketListener packetListener, PacketFilter packetFilter) {
        // TODO: implement
    }

    public void removePacketListener(PacketListener packetListener) {
        // TODO: implement
    }

    /**
     * Starts the packet reader thread and returns once a connection to the server
     * has been established. A connection will be attempted for a maximum of five
     * seconds. An XMPPException will be thrown if the connection fails.
     *
     * @throws XMPPException if the server fails to send an opening stream back
     *      for more than five seconds.
     */
    public void startup() throws XMPPException {
        readerThread.start();
        listenerThread.start();
        // Wait for stream tag before returing. We'll wait a maximum of five seconds before
        // giving up and throwing an error.
        try {
            synchronized(connectionIDLock) {
                connectionIDLock.wait(5000);
            }
        }
        catch (InterruptedException ie) { }
        if (connectionID == null) {
            throw new XMPPException("Connection failed. No response from server.");
        }
        else {
            connection.connectionID = connectionID;
        }
    }

    /**
     * Shuts the packet reader down.
     */
    public void shutdown() {
        done = true;
    }

    /**
     * Process listeners.
     */
    private void processListeners() {
        boolean processedPacket = false;
        while (true) {
            synchronized(listeners) {
                int size = listeners.size();
                for (int i=0; i<size; i++) {

                }
            }
            if (!processedPacket) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException ie) { }
            }
        }
    }

    /**
     * Parse top-level packets in order to process them further.
     */
    private void parsePackets() {
        try {
            int eventType = parser.getEventType();
            do {
                if (eventType == parser.START_TAG) {
                    if (parser.getName().equals("message")) {
                        processPacket(parseMessage(parser));
                    }
                    else if (parser.getName().equals("iq")) {
                        processPacket(parseIQ(parser));
                    }
                    else if (parser.getName().equals("presence")) {
                        processPacket(parsePresence(parser));
                    }
                    // We found an opening stream. Record information about it, then notify
                    // the connectionID lock so that the packet reader startup can finish.
                    else if (parser.getName().equals("stream")) {
                        // Ensure the correct jabber:client namespace is being used.
                        if ("jabber:client".equals(parser.getNamespace(null))) {
                            // Check to see if the server supports SASL. We don't actually
                            // do anything with this information at the moment.
                            boolean supportsSASL = parser.getNamespace("sasl") != null;
                            // Get the connection id.
                            for (int i=0; i<parser.getAttributeCount(); i++) {
                                if (parser.getAttributeName(i).equals("id")) {
                                    // Save the connectionID and notify that we've gotten it.
                                    connectionID = parser.getAttributeValue(i);
                                    synchronized(connectionIDLock) {
                                        connectionIDLock.notifyAll();
                                    }
                                }
                            }
                        }
                    }
                }
                else if (eventType == parser.END_TAG) {
                    if (parser.getName().equals("stream")) {
                        connection.close();
                    }
                }
                eventType = parser.next();
            } while (eventType != parser.END_DOCUMENT && !done);
        }
        catch (Exception e) {
            // An exception occurred while parsing. Print the error an close the
            // connection.
            e.printStackTrace();
            if (!done) {
                connection.close();
            }
        }
    }

    /**
     * Processes a packet after it's been fully parsed by looping through the installed
     * packet collectors and letting them examine the packet to see if they are a match.
     *
     * @param packet the packet to process.
     */
    private void processPacket(Packet packet) {
        if (packet == null) {
            return;
        }
        // Loop through all collectors and notify the appropriate ones.
        synchronized (collectors) {
            // Loop through packet collectors backwards.
            int size = collectors.size();
            for (int i=0; i<size; i++) {
                PacketCollector collector = (PacketCollector)collectors.get(i);
                if (collector != null) {
                    // Have the collector process the packet to see if it wants to handle it.
                    collector.processPacket(packet);
                }
            }
        }
    }

    /**
     * Parses an IQ packet.
     *
     * @param parser the XML parser, positioned at the start of an IQ packet.
     * @return an IQ object.
     * @throws Exception if an exception occurs while parsing the packet.
     */
    private static Packet parseIQ(XmlPullParser parser) throws Exception {
        IQ iqPacket = null;

        String id = parser.getAttributeValue("", "id");
        String to = parser.getAttributeValue("", "to");
        String from = parser.getAttributeValue("", "from");
        IQ.Type type = IQ.Type.fromString(parser.getAttributeValue("", "type"));
        Error error = null;

        boolean done = false;
        while (!done) {
            int eventType = parser.next();
            if (eventType == parser.START_TAG) {
                if (parser.getName().equals("query")) {
                    String namespace = parser.getNamespace();
                    if (namespace.equals("jabber:iq:auth")) {
                        iqPacket = parseAuthentication(parser);
                    }
                }
                if (parser.getName().equals("error")) {
                    error = parseError(parser);
                }
            }
            else if (eventType == parser.END_TAG) {
                if (parser.getName().equals("iq")) {
                    done = true;
                }
            }
        }
        // Set basic values on the iq packet.
        if (iqPacket == null) {
            iqPacket = new IQ();
        }
        iqPacket.setPacketID(id);
        iqPacket.setTo(to);
        iqPacket.setFrom(from);
        iqPacket.setType(type);
        iqPacket.setError(error);
        // Return the packet.
        return iqPacket;
    }

    private static Authentication parseAuthentication(XmlPullParser parser) throws Exception {
        Authentication authentication = new Authentication();
        boolean done = false;
        while (!done) {
            int eventType = parser.next();
            if (eventType == parser.START_TAG) {
                if (parser.getName().equals("username")) {
                    authentication.setUsername(parser.nextText());
                }
                else if (parser.getName().equals("password")) {
                    authentication.setPassword(parser.nextText());
                }
                else if (parser.getName().equals("digest")) {
                    authentication.setDigest(parser.nextText());
                }
                else if (parser.getName().equals("resource")) {
                    authentication.setResource(parser.nextText());
                }
            }
            else if (eventType == parser.END_TAG) {
                if (parser.getName().equals("query")) {
                    done = true;
                }
            }
        }
        return authentication;
    }

    private static Error parseError(XmlPullParser parser) throws Exception {
        String errorCode = null;
        for (int i=0; i<parser.getAttributeCount(); i++) {
            if (parser.getAttributeName(i).equals("code")) {
                errorCode = parser.getAttributeValue("", "code");
            }
        }
        String message = parser.nextText();
        while (true) {
            if (parser.getEventType() == parser.END_TAG && parser.getName().equals("error")) {
                break;
            }
            parser.next();
        }
        return new Error(Integer.parseInt(errorCode), message);
    }

    /**
     * Parses a message packet.
     *
     * @param parser the XML parser, positioned at the start of a message packet.
     * @return an Message object.
     * @throws Exception if an exception occurs while parsing the packet.
     */
    private static Packet parseMessage(XmlPullParser parser) throws Exception {
        Message message = new Message();
        message.setTo(parser.getAttributeValue("", "to"));
        message.setFrom(parser.getAttributeValue("", "from"));
        message.setType(Message.Type.fromString(parser.getAttributeValue("", "type")));

        // Parse sub-elements. We include extra logic to make sure the values
        // are only read once. This is because it's possible for the names to appear
        // in arbitrary sub-elements.
        boolean done = false;
        String subject = null;
        String body = null;
        String thread = null;
        while (!done) {
            int eventType = parser.next();
            if (eventType == parser.START_TAG) {
                if (parser.getName().equals("subject")) {
                    if (subject == null) {
                        subject = parser.nextText();
                    }
                }
                else if (parser.getName().equals("body")) {
                    if (body == null) {
                        body = parser.nextText();
                    }
                }
                else if (parser.getName().equals("thread")) {
                    if (thread == null) {
                        thread = parser.nextText();
                    }
                }
            }
            else if (eventType == parser.END_TAG) {
                if (parser.getName().equals("message")) {
                    done = true;
                }
            }
        }
        message.setSubject(subject);
        message.setBody(body);
        message.setThread(thread);
        return message;
    }

    /**
     * Parses a presence packet.
     *
     * @param parser the XML parser, positioned at the start of a presence packet.
     * @return an Presence object.
     * @throws Exception if an exception occurs while parsing the packet.
     */
    private static Packet parsePresence(XmlPullParser parser) throws Exception {
        String type = parser.getAttributeValue("", "type");
        // If the type value isn't set, it should default to available.
        if (type == null) {
            type = "available";
        }
        // We only handle "available" or "unavailable" packets for now.
        if (!(type.equals("available") || type.equals("unavailable"))) {
            System.out.println("FOUND OTHER PRESENCE TYPE: " + type);
        }
        Presence presence = new Presence("available".equals(type));
        presence.setTo(parser.getAttributeValue("", "to"));
        presence.setFrom(parser.getAttributeValue("", "from"));
        presence.setPacketID(parser.getAttributeValue("", "id"));

        // Parse sub-elements
        boolean done = false;
        while (!done) {
            int eventType = parser.next();
            if (eventType == parser.START_TAG) {
                if (parser.getName().equals("status")) {
                    presence.setStatus(parser.nextText());
                }
                else if (parser.getName().equals("priority")) {
                    try {
                        int priority = Integer.parseInt(parser.nextText());
                        presence.setPriority(priority);
                    }
                    catch (NumberFormatException nfe) { }
                }
                else if (parser.getName().equals("show")) {
                    presence.setMode(Presence.Mode.fromString(parser.nextText()));
                }
            }
            else if (eventType == parser.END_TAG) {
                if (parser.getName().equals("presence")) {
                    done = true;
                }
            }
        }
        return presence;
    }

    /**
     * Parse a properties sub-packet. If any errors occur while de-serializing Java object
     * properties, an exception will be printed and not thrown since a thrown
     * exception will shut down the entire connection. ClassCastExceptions will occur
     * when both the sender and receiver of the packet don't have identical versions
     * of the same class.
     *
     * @param parser the XML parser, positioned at the start of a properties sub-packet.
     * @param packet the packet being parsed.
     * @throws Exception if an error occurs while parsing the properties.
     */
    private static void parseProperties(XmlPullParser parser, Packet packet) throws Exception {
        boolean done = false;
        while (!done) {
            int eventType = parser.next();
            if (eventType == parser.START_TAG) {


            }
        }
    }

    /**
     * A wrapper class to associate a packet collector with a listener.
     */
    private static class PacketListenerWrapper {

        private PacketListener packetListener;
        private PacketCollector packetCollector;

        public PacketListenerWrapper(PacketReader packetReader, PacketListener packetListener,
                PacketFilter packetFilter)
        {
            this.packetListener = packetListener;
            this.packetCollector = new PacketCollector(packetReader, packetFilter);
        }
    }
}
