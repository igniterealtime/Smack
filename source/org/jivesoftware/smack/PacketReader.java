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

import org.xmlpull.v1.*;

import java.util.*;
import java.io.ObjectInputStream;
import java.io.ByteArrayInputStream;

import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.util.StringUtils;

/**
 * Listens for XML traffic from the XMPP server, and parses it into packet objects.
 * The packet reader also manages all packet listeners and collectors.
 *
 * @see PacketCollector
 * @see PacketListener
 * @author Matt Tucker
 */
class PacketReader {

    /**
     * Namespace used to store packet properties.
     */
    private static final String PROPERTIES_NAMESPACE =
            "http://www.jivesoftware.com/xmlns/xmpp/properties";

    private Thread readerThread;
    private Thread listenerThread;

    private XMPPConnection connection;
    private XmlPullParser parser;
    private boolean done = false;
    protected List collectors = new ArrayList();
    private List listeners = new ArrayList();

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
            final String defaultProviderName = "org.xmlpull.mxp1.MXParserFactory";
            XmlPullParserFactory factory = null;
            try {
                // Attempt to load a factory implementation using a system property
                // and a classloader context.
                factory = XmlPullParserFactory.newInstance(
                        System.getProperty(XmlPullParserFactory.PROPERTY_NAME),
                        Thread.currentThread().getContextClassLoader().getClass());
            }
            catch (Exception e) {
                if (factory == null) {
                    // Loading failed. Therefore, use the hardcoded default.
                    factory = XmlPullParserFactory.newInstance(defaultProviderName, null);
                }
            }
            factory.setNamespaceAware(true);
            parser = factory.newPullParser();
            parser.setInput(connection.reader);

        }
        catch (XmlPullParserException xppe) {
            xppe.printStackTrace();
        }
    }

    /**
     * Creates a new packet collector for this reader. A packet filter determines
     * which packets will be accumulated by the collector.
     *
     * @param packetFilter the packet filter to use.
     * @return a new packet collector.
     */
    public PacketCollector createPacketCollector(PacketFilter packetFilter) {
        PacketCollector packetCollector = new PacketCollector(this, packetFilter);
        return packetCollector;
    }

    /**
     * Registers a packet listener with this reader. A packet filter determines
     * which packets will be delivered to the listener.
     *
     * @param packetListener the packet listener to notify of new packets.
     * @param packetFilter the packet filter to use.
     */
    public void addPacketListener(PacketListener packetListener, PacketFilter packetFilter) {
        ListenerWrapper wrapper = new ListenerWrapper(this, packetListener,
                packetFilter);
        synchronized (listeners) {
            listeners.add(wrapper);
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
        while (!done) {
            synchronized (listeners) {
                if (listeners.size() > 0) {
                    for (int i=listeners.size()-1; i>=0; i--) {
                        if (listeners.get(i) == null) {
                            listeners.remove(i);
                        }
                    }
                }
            }
            processedPacket = false;
            int size = listeners.size();
            for (int i=0; i<size; i++) {
                ListenerWrapper wrapper = (ListenerWrapper)listeners.get(i);
                if (wrapper != null) {
                    processedPacket = processedPacket || wrapper.notifyListener();
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
                if (eventType == XmlPullParser.START_TAG) {
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
                else if (eventType == XmlPullParser.END_TAG) {
                    if (parser.getName().equals("stream")) {
                        connection.close();
                    }
                }
                eventType = parser.next();
            } while (eventType != XmlPullParser.END_DOCUMENT && !done);
        }
        catch (Exception e) {
            if (!done) {
                // An exception occurred while parsing. Print the error an close the
                // connection.
                e.printStackTrace();
                connection.close();
            }
        }
    }

    /**
     * Processes a packet after it's been fully parsed by looping through the installed
     * packet collectors and listeners and letting them examine the packet to see if
     * they are a match with the filter.
     *
     * @param packet the packet to process.
     */
    private void processPacket(Packet packet) {
        if (packet == null) {
            return;
        }

        // Remove all null values from the collectors list.
        synchronized (collectors) {
            for (int i=collectors.size()-1; i>=0; i--) {
                    if (collectors.get(i) == null) {
                        collectors.remove(i);
                    }
                }
        }

        // Loop through all collectors and notify the appropriate ones.
        int size = collectors.size();
        for (int i=0; i<size; i++) {
            PacketCollector collector = (PacketCollector)collectors.get(i);
            if (collector != null) {
                // Have the collector process the packet to see if it wants to handle it.
                collector.processPacket(packet);
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
        XMPPError error = null;
        Map properties = null;

        boolean done = false;
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals("query")) {
                    String namespace = parser.getNamespace();
                    if (namespace.equals("jabber:iq:auth")) {
                        iqPacket = parseAuthentication(parser);
                    }
                    else if (namespace.equals("jabber:iq:roster")) {
                        iqPacket = parseRoster(parser);
                    }
                    else if (namespace.equals("jabber:iq:register")) {
                        iqPacket = parseRegistration(parser);
                    }
                }
                else if (parser.getName().equals("error")) {
                    error = parseError(parser);
                }
                else if (parser.getName().equals("x") &&
                        parser.getNamespace().equals(PROPERTIES_NAMESPACE))
                {
                    properties = parseProperties(parser);
                }
            }
            else if (eventType == XmlPullParser.END_TAG) {
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
        // Set packet properties.
        if (properties != null) {
            for (Iterator i=properties.keySet().iterator(); i.hasNext(); ) {
                String name = (String)i.next();
                iqPacket.setProperty(name, properties.get(name));
            }
        }
        // Return the packet.
        return iqPacket;
    }

    private static Authentication parseAuthentication(XmlPullParser parser) throws Exception {
        Authentication authentication = new Authentication();
        boolean done = false;
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
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
            else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals("query")) {
                    done = true;
                }
            }
        }
        return authentication;
    }

    private static RosterPacket parseRoster(XmlPullParser parser) throws Exception {
        RosterPacket roster = new RosterPacket();
        boolean done = false;
        RosterPacket.Item item = null;
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals("item")) {
                    String jid = parser.getAttributeValue("", "jid");
                    String name = parser.getAttributeValue("", "name");
                    String subscription = parser.getAttributeValue("", "subscription");
                    item = new RosterPacket.Item(jid, name);
                    item.setItemType(RosterPacket.ItemType.fromString(subscription));
                }
                if (parser.getName().equals("group")) {
                    String groupName = parser.nextText();
                    item.addGroupName(groupName);
                }
            }
            else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals("item")) {
                    roster.addRosterItem(item);
                }
                if (parser.getName().equals("query")) {
                    done = true;
                }
            }
        }
        return roster;
    }

     private static Registration parseRegistration(XmlPullParser parser) throws Exception {
        Registration registration = new Registration();
        Map fields = null;
        boolean done = false;
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals("username")) {
                    registration.setUsername(parser.nextText());
                }
                else if (parser.getName().equals("password")) {
                    registration.setPassword(parser.nextText());
                }
                else {
                    String name = parser.getName();
                    String value = parser.nextText();
                    // Ignore instructions, but anything else should be added to the map.
                    if (!name.equals("instructions")) {
                        if (fields == null) {
                            fields = new HashMap();
                        }
                        fields.put(name, value);
                    }
                }
            }
            else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals("query")) {
                    done = true;
                }
            }
        }
        registration.setAttributes(fields);
        return registration;
    }

    /**
     * Parses error sub-packets.
     *
     * @param parser the XML parser.
     * @return an error sub-packet.
     * @throws Exception if an exception occurs while parsing the packet.
     */
    private static XMPPError parseError(XmlPullParser parser) throws Exception {
        String errorCode = null;
        for (int i=0; i<parser.getAttributeCount(); i++) {
            if (parser.getAttributeName(i).equals("code")) {
                errorCode = parser.getAttributeValue("", "code");
            }
        }
        String message = parser.nextText();
        while (true) {
            if (parser.getEventType() == XmlPullParser.END_TAG && parser.getName().equals("error")) {
                break;
            }
        }
        return new XMPPError(Integer.parseInt(errorCode), message);
    }

    /**
     * Parses a message packet.
     *
     * @param parser the XML parser, positioned at the start of a message packet.
     * @return a Message object.
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
        Map properties = null;
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
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
                else if (parser.getName().equals("error")) {
                    message.setError(parseError(parser));
                }
                else if (parser.getName().equals("x") &&
                        parser.getNamespace().equals(PROPERTIES_NAMESPACE))
                {
                    properties = parseProperties(parser);
                }
            }
            else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals("message")) {
                    done = true;
                }
            }
        }
        message.setSubject(subject);
        message.setBody(body);
        message.setThread(thread);
        // Set packet properties.
        if (properties != null) {
            for (Iterator i=properties.keySet().iterator(); i.hasNext(); ) {
                String name = (String)i.next();
                message.setProperty(name, properties.get(name));
            }
        }
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
        Presence.Type type = Presence.Type.fromString(parser.getAttributeValue("", "type"));
        // We only handle "available" or "unavailable" packets for now.
        if (!(type == Presence.Type.AVAILABLE || type == Presence.Type.UNAVAILABLE)) {
            System.out.println("FOUND OTHER PRESENCE TYPE: " + type);
        }

        // Otherwise, it's a presence packet that has nothing to do with roster items.

        Presence presence = new Presence(type);
        presence.setTo(parser.getAttributeValue("", "to"));
        presence.setFrom(parser.getAttributeValue("", "from"));
        presence.setPacketID(parser.getAttributeValue("", "id"));

        // Parse sub-elements
        boolean done = false;
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
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
                else if (parser.getName().equals("error")) {
                    presence.setError(parseError(parser));
                }
                else if (parser.getName().equals("x") &&
                        parser.getNamespace().equals(PROPERTIES_NAMESPACE))
                {
                    Map properties = parseProperties(parser);
                    // Set packet properties.
                    for (Iterator i=properties.keySet().iterator(); i.hasNext(); ) {
                        String name = (String)i.next();
                        presence.setProperty(name, properties.get(name));
                    }
                }
            }
            else if (eventType == XmlPullParser.END_TAG) {
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
     * @return a map of the properties.
     * @throws Exception if an error occurs while parsing the properties.
     */
    private static Map parseProperties(XmlPullParser parser) throws Exception {
        Map properties = new HashMap();
        while (true) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG && parser.getName().equals("property")) {
                // Advance to name element.
                parser.next();
                String name = parser.nextText();
                parser.next();
                String type = parser.getAttributeValue("", "type");
                String valueText = parser.nextText();
                Object value = null;
                if ("integer".equals(type)) {
                    value = new Integer(valueText);
                }
                else if ("long".equals(type))  {
                    value = new Long(valueText);
                }
                else if ("float".equals(type)) {
                    value = new Float(valueText);
                }
                else if ("double".equals(type)) {
                    value = new Double(valueText);
                }
                else if ("boolean".equals(type)) {
                    value = new Boolean(valueText);
                }
                else if ("string".equals(type)) {
                    value = valueText;
                }
                else if ("java-object".equals(type)) {
                    try {
                        byte [] bytes = StringUtils.decodeBase64(valueText).getBytes("ISO-8859-1");
                        ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(bytes));
                        value = in.readObject();
                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
                if (name != null && value != null) {
                    properties.put(name, value);
                }
            }
            else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals("x")) {
                    break;
                }
            }
        }
        return properties;
    }

    /**
     * A wrapper class to associate a packet collector with a listener.
     */
    private static class ListenerWrapper {

        private PacketListener packetListener;
        private PacketCollector packetCollector;

        public ListenerWrapper(PacketReader packetReader, PacketListener packetListener,
                PacketFilter packetFilter)
        {
            this.packetListener = packetListener;
            this.packetCollector = new PacketCollector(packetReader, packetFilter);
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

        public boolean notifyListener() {
            Packet packet = packetCollector.pollResult();
            if (packet != null) {
                packetListener.processPacket(packet);
                return true;
            }
            else {
                return false;
            }
        }

        public void cancel() {
            packetCollector.cancel();
            packetCollector = null;
            packetListener = null;
        }
    }
}
