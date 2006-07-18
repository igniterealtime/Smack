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
import org.jivesoftware.smack.packet.*;
import org.jivesoftware.smack.provider.IQProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.xmlpull.mxp1.MXParser;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.util.*;

/**
 * Listens for XML traffic from the XMPP server and parses it into packet objects.
 * The packet reader also manages all packet listeners and collectors.<p>
 *
 * @see PacketCollector
 * @see PacketListener
 * @author Matt Tucker
 */
class PacketReader {

    private final Thread readerThread;
    private final Thread listenerThread;

    private XMPPConnection connection;
    private XmlPullParser parser;
    private boolean done = false;
    private final VolatileMemberCollection<PacketCollector> collectors =
            new VolatileMemberCollection<PacketCollector>(50);
    private final VolatileMemberCollection listeners = new VolatileMemberCollection(50);
    protected final List<ConnectionListener> connectionListeners =
            new ArrayList<ConnectionListener>();

    private String connectionID = null;
    private final Object connectionIDLock = new Object();

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
                try {
                    processListeners();
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        listenerThread.setName("Smack Listener Processor");
        listenerThread.setDaemon(true);

        resetParser();
    }

    /**
     * Creates a new packet collector for this reader. A packet filter determines
     * which packets will be accumulated by the collector.
     *
     * @param packetFilter the packet filter to use.
     * @return a new packet collector.
     */
    public PacketCollector createPacketCollector(PacketFilter packetFilter) {
        PacketCollector collector = new PacketCollector(this, packetFilter);
        collectors.add(collector);
        // Add the collector to the list of active collector.
        return collector;
    }

    protected void cancelPacketCollector(PacketCollector packetCollector) {
        collectors.remove(packetCollector);
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
        listeners.remove(packetListener);
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
        // Wait for stream tag before returing. We'll wait a couple of seconds before
        // giving up and throwing an error.
        try {
            synchronized(connectionIDLock) {
                if (connectionID == null) {
                    // A waiting thread may be woken up before the wait time or a notify
                    // (although this is a rare thing). Therefore, we continue waiting
                    // until either a connectionID has been set (and hence a notify was
                    // made) or the total wait time has elapsed.
                    long waitTime = SmackConfiguration.getPacketReplyTimeout();
                    long start = System.currentTimeMillis();
                    while (connectionID == null && !done) {
                        if (waitTime <= 0) {
                            break;
                        }
                        // Wait 3 times the standard time since TLS may take a while
                        connectionIDLock.wait(waitTime * 3);
                        long now = System.currentTimeMillis();
                        waitTime -= now - start;
                        start = now;
                    }
                }
            }
        }
        catch (InterruptedException ie) {
            // Ignore.
        }
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
        // Notify connection listeners of the connection closing if done hasn't already been set.
        if (!done) {
            List<ConnectionListener> listenersCopy;
            synchronized (connectionListeners) {
                // Make a copy since it's possible that a listener will be removed from the list
                listenersCopy = new ArrayList<ConnectionListener>(connectionListeners);
                for (ConnectionListener listener : listenersCopy) {
                    listener.connectionClosed();
                }
            }
        }
        done = true;

        // Make sure that the listenerThread is awake to shutdown properly
        synchronized (listenerThread) {
            listenerThread.notify();
        }
    }

    /**
     * Sends out a notification that there was an error with the connection
     * and closes the connection.
     *
     * @param e the exception that causes the connection close event.
     */
    void notifyConnectionError(Exception e) {
        done = true;
        connection.close();
        // Print the stack trace to help catch the problem
        e.printStackTrace();
        // Notify connection listeners of the error.
        List<ConnectionListener> listenersCopy;
        synchronized (connectionListeners) {
            // Make a copy since it's possible that a listener will be removed from the list
            listenersCopy = new ArrayList<ConnectionListener>(connectionListeners);
            for (ConnectionListener listener : listenersCopy) {
                listener.connectionClosedOnError(e);
            }
        }
				
        // Make sure that the listenerThread is awake to shutdown properly
        synchronized (listenerThread) {
            listenerThread.notify();
        }
    }

    /**
     * Resets the parser using the latest connection's reader. Reseting the parser is necessary
     * when the plain connection has been secured or when a new opening stream element is going
     * to be sent by the server.
     */
    private void resetParser() {
        try {
            parser = new MXParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
            parser.setInput(connection.reader);
        }
        catch (XmlPullParserException xppe) {
            xppe.printStackTrace();
        }
    }

    /**
     * Process listeners.
     */
    private void processListeners() {
        while (!done) {
            boolean processedPacket = false;
            Iterator it = listeners.getIterator();
            while (it.hasNext()) {
                ListenerWrapper wrapper = (ListenerWrapper) it.next();
                processedPacket = processedPacket || wrapper.notifyListener();
            }
            if (!processedPacket) {
                try {
                    // Wait until more packets are ready to be processed.
                    synchronized (listenerThread) {
                        listenerThread.wait();
                    }
                }
                catch (InterruptedException ie) {
                    // Ignore.
                }
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
                        processPacket(PacketParserUtils.parseMessage(parser));
                    }
                    else if (parser.getName().equals("iq")) {
                        processPacket(parseIQ(parser));
                    }
                    else if (parser.getName().equals("presence")) {
                        processPacket(PacketParserUtils.parsePresence(parser));
                    }
                    // We found an opening stream. Record information about it, then notify
                    // the connectionID lock so that the packet reader startup can finish.
                    else if (parser.getName().equals("stream")) {
                        // Ensure the correct jabber:client namespace is being used.
                        if ("jabber:client".equals(parser.getNamespace(null))) {
                            // Get the connection id.
                            for (int i=0; i<parser.getAttributeCount(); i++) {
                                if (parser.getAttributeName(i).equals("id")) {
                                    // Save the connectionID
                                    connectionID = parser.getAttributeValue(i);
                                    if (!"1.0".equals(parser.getAttributeValue("", "version"))) {
                                        // Notify that a stream has been opened if the
                                        // server is not XMPP 1.0 compliant otherwise make the
                                        // notification after TLS has been negotiated or if TLS
                                        // is not supported
                                        releaseConnectionIDLock();
                                    }
                                }
                                else if (parser.getAttributeName(i).equals("from")) {
                                    // Use the server name that the server says that it is.
                                    connection.serviceName = parser.getAttributeValue(i);
                                }
                            }
                        }
                    }
                    else if (parser.getName().equals("error")) {
                        throw new XMPPException(parseStreamError(parser));
                    }
                    else if (parser.getName().equals("features")) {
                    	parseFeatures(parser);
                    }
                    else if (parser.getName().equals("proceed")) {
                        // Secure the connection by negotiating TLS
                        connection.proceedTLSReceived();
                        // Reset the state of the parser since a new stream element is going
                        // to be sent by the server
                        resetParser();
                    }
                    else if (parser.getName().equals("failure")) {
                        String namespace = parser.getNamespace(null);
                        if ("urn:ietf:params:xml:ns:xmpp-tls".equals(namespace)) {
                            // TLS negotiation has failed. The server will close the connection
                            throw new Exception("TLS negotiation has failed");
                        }
                        else if ("http://jabber.org/protocol/compress".equals(namespace)) {
                            // Stream compression has been denied. This is a recoverable
                            // situation. It is still possible to authenticate and
                            // use the connection but using an uncompressed connection
                            connection.streamCompressionDenied();
                        }
                        else {
                            // SASL authentication has failed. The server may close the connection
                            // depending on the number of retries
                            connection.getSASLAuthentication().authenticationFailed();
                        }
                    }
                    else if (parser.getName().equals("challenge")) {
                        // The server is challenging the SASL authentication made by the client
                        connection.getSASLAuthentication().challengeReceived(parser.nextText());
                    }
                    else if (parser.getName().equals("success")) {
                        // We now need to bind a resource for the connection
                        // Open a new stream and wait for the response
                        connection.packetWriter.openStream();

                        // Reset the state of the parser since a new stream element is going
                        // to be sent by the server
                        resetParser();

                        // The SASL authentication with the server was successful. The next step
                        // will be to bind the resource
                        connection.getSASLAuthentication().authenticated();
                    }
                    else if (parser.getName().equals("compressed")) {
                        // Server confirmed that it's possible to use stream compression. Start
                        // stream compression
                        connection.startStreamCompression();
                        // Reset the state of the parser since a new stream element is going
                        // to be sent by the server
                        resetParser();
                    }
                }
                else if (eventType == XmlPullParser.END_TAG) {
                    if (parser.getName().equals("stream")) {
                        // Close the connection.
                        connection.close();
                    }
                }
                eventType = parser.next();
            } while (!done && eventType != XmlPullParser.END_DOCUMENT);
        }
        catch (Exception e) {
            if (!done) {
                // Close the connection and notify connection listeners of the
                // error.
                notifyConnectionError(e);
            }
        }
    }

    /**
     * Releases the connection ID lock so that the thread that was waiting can resume. The
     * lock will be released when one of the following three conditions is met:<p>
     *
     * 1) An opening stream was sent from a non XMPP 1.0 compliant server
     * 2) Stream features were received from an XMPP 1.0 compliant server that does not support TLS
     * 3) TLS negotiation was successful
     *
     */
    private void releaseConnectionIDLock() {
        synchronized(connectionIDLock) {
            connectionIDLock.notifyAll();
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

        // Loop through all collectors and notify the appropriate ones.
        Iterator it = collectors.getIterator();
        while (it.hasNext()) {
            PacketCollector collector = (PacketCollector) it.next();
            collector.processPacket(packet);
        }

        // Notify the listener thread that packets are waiting.
        synchronized (listenerThread) {
            listenerThread.notifyAll();
        }
    }

    private StreamError parseStreamError(XmlPullParser parser) throws IOException,
            XmlPullParserException {
        StreamError streamError = null;
        boolean done = false;
        while (!done) {
            int eventType = parser.next();

            if (eventType == XmlPullParser.START_TAG) {
                streamError = new StreamError(parser.getName());
            }
            else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals("error")) {
                    done = true;
                }
            }
        }
        return streamError;
    }

    private void parseFeatures(XmlPullParser parser) throws Exception {
        boolean startTLSReceived = false;
        boolean done = false;
        while (!done) {
            int eventType = parser.next();

            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals("starttls")) {
                    startTLSReceived = true;
                    // Confirm the server that we want to use TLS
                    connection.startTLSReceived();
                }
                else if (parser.getName().equals("mechanisms")) {
                    // The server is reporting available SASL mechanisms. Store this information
                    // which will be used later while logging (i.e. authenticating) into
                    // the server
                    connection.getSASLAuthentication()
                            .setAvailableSASLMethods(parseMechanisms(parser));
                }
                else if (parser.getName().equals("bind")) {
                    // The server requires the client to bind a resource to the stream
                    connection.getSASLAuthentication().bindingRequired();
                }
                else if (parser.getName().equals("session")) {
                    // The server supports sessions
                    connection.getSASLAuthentication().sessionsSupported();
                }
                else if (parser.getName().equals("compression")) {
                    // The server supports stream compression
                    connection.setAvailableCompressionMethods(parseCompressionMethods(parser));
                }
            }
            else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals("features")) {
                    done = true;
                }
            }
        }
        if (!startTLSReceived) {
            releaseConnectionIDLock();
        }
    }

    /**
     * Returns a collection of Stings with the mechanisms included in the mechanisms stanza.
     *
     * @param parser the XML parser, positioned at the start of an IQ packet.
     * @return a collection of Stings with the mechanisms included in the mechanisms stanza.
     * @throws Exception if an exception occurs while parsing the stanza.
     */
    private Collection<String> parseMechanisms(XmlPullParser parser) throws Exception {
        List<String> mechanisms = new ArrayList<String>();
        boolean done = false;
        while (!done) {
            int eventType = parser.next();

            if (eventType == XmlPullParser.START_TAG) {
                String elementName = parser.getName();
                if (elementName.equals("mechanism")) {
                    mechanisms.add(parser.nextText());
                }
            }
            else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals("mechanisms")) {
                    done = true;
                }
            }
        }
        return mechanisms;
    }

    private Collection<String> parseCompressionMethods(XmlPullParser parser)
            throws IOException, XmlPullParserException {
        List<String> methods = new ArrayList<String>();
        boolean done = false;
        while (!done) {
            int eventType = parser.next();

            if (eventType == XmlPullParser.START_TAG) {
                String elementName = parser.getName();
                if (elementName.equals("method")) {
                    methods.add(parser.nextText());
                }
            }
            else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals("compression")) {
                    done = true;
                }
            }
        }
        return methods;
    }

    /**
     * Parses an IQ packet.
     *
     * @param parser the XML parser, positioned at the start of an IQ packet.
     * @return an IQ object.
     * @throws Exception if an exception occurs while parsing the packet.
     */
    private IQ parseIQ(XmlPullParser parser) throws Exception {
        IQ iqPacket = null;

        String id = parser.getAttributeValue("", "id");
        String to = parser.getAttributeValue("", "to");
        String from = parser.getAttributeValue("", "from");
        IQ.Type type = IQ.Type.fromString(parser.getAttributeValue("", "type"));
        XMPPError error = null;

        boolean done = false;
        while (!done) {
            int eventType = parser.next();

            if (eventType == XmlPullParser.START_TAG) {
                String elementName = parser.getName();
                String namespace = parser.getNamespace();
                if (elementName.equals("error")) {
                    error = PacketParserUtils.parseError(parser);
                }
                else if (elementName.equals("query") && namespace.equals("jabber:iq:auth")) {
                    iqPacket = parseAuthentication(parser);
                }
                else if (elementName.equals("query") && namespace.equals("jabber:iq:roster")) {
                    iqPacket = parseRoster(parser);
                }
                else if (elementName.equals("query") && namespace.equals("jabber:iq:register")) {
                    iqPacket = parseRegistration(parser);
                }
                else if (elementName.equals("bind") &&
                        namespace.equals("urn:ietf:params:xml:ns:xmpp-bind")) {
                    iqPacket = parseResourceBinding(parser);
                }
                // Otherwise, see if there is a registered provider for
                // this element name and namespace.
                else {
                    Object provider = ProviderManager.getIQProvider(elementName, namespace);
                    if (provider != null) {
                        if (provider instanceof IQProvider) {
                            iqPacket = ((IQProvider)provider).parseIQ(parser);
                        }
                        else if (provider instanceof Class) {
                            iqPacket = (IQ)PacketParserUtils.parseWithIntrospection(elementName,
                                    (Class)provider, parser);
                        }
                    }
                }
            }
            else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals("iq")) {
                    done = true;
                }
            }
        }
        // Decide what to do when an IQ packet was not understood
        if (iqPacket == null) {
            if (IQ.Type.GET == type || IQ.Type.SET == type ) {
                // If the IQ stanza is of type "get" or "set" containing a child element
                // qualified by a namespace it does not understand, then answer an IQ of
                // type "error" with code 501 ("feature-not-implemented")
                iqPacket = new IQ() {
                    public String getChildElementXML() {
                        return null;
                    }
                };
                iqPacket.setPacketID(id);
                iqPacket.setTo(from);
                iqPacket.setFrom(to);
                iqPacket.setType(IQ.Type.ERROR);
                iqPacket.setError(new XMPPError(501, "feature-not-implemented"));
                connection.sendPacket(iqPacket);
                return null;
            }
            else {
                // If an IQ packet wasn't created above, create an empty IQ packet.
                iqPacket = new IQ() {
                    public String getChildElementXML() {
                        return null;
                    }
                };
            }
        }

        // Set basic values on the iq packet.
        iqPacket.setPacketID(id);
        iqPacket.setTo(to);
        iqPacket.setFrom(from);
        iqPacket.setType(type);
        iqPacket.setError(error);

        return iqPacket;
    }

    private Authentication parseAuthentication(XmlPullParser parser) throws Exception {
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

    private RosterPacket parseRoster(XmlPullParser parser) throws Exception {
        RosterPacket roster = new RosterPacket();
        boolean done = false;
        RosterPacket.Item item = null;
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals("item")) {
                    String jid = parser.getAttributeValue("", "jid");
                    String name = parser.getAttributeValue("", "name");
                    // Create packet.
                    item = new RosterPacket.Item(jid, name);
                    // Set status.
                    String ask = parser.getAttributeValue("", "ask");
                    RosterPacket.ItemStatus status = RosterPacket.ItemStatus.fromString(ask);
                    item.setItemStatus(status);
                    // Set type.
                    String subscription = parser.getAttributeValue("", "subscription");
                    RosterPacket.ItemType type = RosterPacket.ItemType.fromString(subscription);
                    item.setItemType(type);
                }
                if (parser.getName().equals("group") && item!= null) {
                    item.addGroupName(parser.nextText());
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

     private Registration parseRegistration(XmlPullParser parser) throws Exception {
        Registration registration = new Registration();
        Map<String, String> fields = null;
        boolean done = false;
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                // Any element that's in the jabber:iq:register namespace,
                // attempt to parse it if it's in the form <name>value</name>.
                if (parser.getNamespace().equals("jabber:iq:register")) {
                    String name = parser.getName();
                    String value = "";
                    if (fields == null) {
                        fields = new HashMap<String, String>();
                    }

                    if (parser.next() == XmlPullParser.TEXT) {
                        value = parser.getText();
                    }
                    // Ignore instructions, but anything else should be added to the map.
                    if (!name.equals("instructions")) {
                        fields.put(name, value);
                    }
                    else {
                        registration.setInstructions(value);
                    }
}
                // Otherwise, it must be a packet extension.
                else {
                    registration.addExtension(
                        PacketParserUtils.parsePacketExtension(
                            parser.getName(),
                            parser.getNamespace(),
                            parser));
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

    private Bind parseResourceBinding(XmlPullParser parser) throws IOException,
            XmlPullParserException {
        Bind bind = new Bind();
        boolean done = false;
        while (!done) {
            int eventType = parser.next();
            if (eventType == XmlPullParser.START_TAG) {
                if (parser.getName().equals("resource")) {
                    bind.setResource(parser.nextText());
                }
                else if (parser.getName().equals("jid")) {
                    bind.setJid(parser.nextText());
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                if (parser.getName().equals("bind")) {
                    done = true;
                }
            }
        }

        return bind;
    }

    /**
     * When an object is added it the first attempt is to add it to a 'null' space and when it is
     * removed it is not removed from the list but instead the position is nulled so as not to
     * interfere with list iteration as the Collection memebres are thought to be extermely
     * volatile. In other words, many are added and deleted and 'null' values are skipped by the
     * returned iterator.
     */
    static class VolatileMemberCollection<E> {

        private final Object mutex = new Object();
        private final ArrayList<E> collectors;
        private int nullIndex = -1;
        private int[] nullArray;

        VolatileMemberCollection(int initialCapacity) {
            collectors = new ArrayList<E>(initialCapacity);
            nullArray = new int[initialCapacity];
        }

        public void add(E member) {
            synchronized (mutex) {
                if (nullIndex < 0) {
                    ensureCapacity();
                    collectors.add(member);
                }
                else {
                    collectors.set(nullArray[nullIndex--], member);
                }
            }
        }

        private void ensureCapacity() {
            int current = nullArray.length;
            if (collectors.size() + 1 >= current) {
                int newCapacity = current * 2;
                int oldData[] = nullArray;

                collectors.ensureCapacity(newCapacity);
                nullArray = new int[newCapacity];
                System.arraycopy(oldData, 0, nullArray, 0, nullIndex + 1);
            }
        }

        public void remove(E member) {
            synchronized (mutex) {
                int index = collectors.lastIndexOf(member);
                if (index >= 0) {
                    collectors.set(index, null);
                    nullArray[++nullIndex] = index;
                }
            }
        }

        /**
         * One thread should be using an iterator at a time.
         *
         * @return Iterator over PacketCollector.
         */
        public Iterator getIterator() {
            return new Iterator() {
                private int index = 0;
                private Object next;
                private int size = collectors.size();

                public void remove() {
                }

                public boolean hasNext() {
                    return next != null || grabNext() != null;
                }

                private Object grabNext() {
                    Object next;
                    while (index < size) {
                        next = collectors.get(index++);
                        if (next != null) {
                            this.next = next;
                            return next;
                        }
                    }
                    this.next = null;
                    return null;
                }

                public Object next() {
                    Object toReturn = (this.next != null ? this.next : grabNext());
                    this.next = null;
                    return toReturn;
                }
            };
        }
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
            this.packetCollector = packetReader.createPacketCollector(packetFilter);
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