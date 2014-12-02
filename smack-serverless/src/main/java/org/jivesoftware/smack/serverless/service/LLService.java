/**
 *
 * Copyright 2009 Jonas Ådahl.
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

package org.jivesoftware.smack.serverless.service;


import org.jivesoftware.smack.AbstractConnectionListener;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.ChatManagerListener;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.OrFilter;
import org.jivesoftware.smack.filter.PacketIDFilter;
import org.jivesoftware.smack.filter.IQTypeFilter;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smack.serverless.LLConnectionConfiguration;
import org.jivesoftware.smack.serverless.LLPresence;
import org.jivesoftware.smack.serverless.XMPPLLConnection;

import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ConcurrentHashMap;

/**
 * LLService acts as an abstract interface to a Link-local XMPP service
 * according to XEP-0174. XEP-0174 describes how this is achieved using
 * mDNS/DNS-SD, and this class creates an implementation unspecific 
 * interface for doing this.
 *
 * The mDNS/DNS-SD is for example implemented by JmDNSService (using JmDNS).
 *
 * There is only one instance of LLService possible at one time.
 *
 * Tasks taken care of here are:
 * <ul>
 *   <li>Connection Management
 *     <ul>
 *       <li>Keep track of active connections from and to the local client</li>
 *       <li>Listen for connections on a semi randomized port announced by the
 *           mDNS/DNS-SD daemon</li>
 *       <li>Establish new connections when there is none to use and packets are
 *           to be sent</li>
 *     <ul>
 *   <li>Chat Management - Keep track of messaging sessions between users</li>
 * </ul>
 *
 * @author Jonas Ådahl
 */
public abstract class LLService {
    private static LLService service = null;

    // Listeners for new services
    private static Set<LLServiceListener> serviceCreatedListeners =
        new CopyOnWriteArraySet<LLServiceListener>();

    static final int DEFAULT_MIN_PORT = 2300;
    static final int DEFAULT_MAX_PORT = 2400;
    protected LLPresence presence;
    private boolean done = false;
    private Thread listenerThread;

    private boolean initiated = false;

    private Map<String,Chat> chats =
        new ConcurrentHashMap<String,Chat>();

    private Map<String,XMPPLLConnection> ingoing =
        new ConcurrentHashMap<String,XMPPLLConnection>();
    private Map<String,XMPPLLConnection> outgoing =
        new ConcurrentHashMap<String,XMPPLLConnection>();

    // Listeners for state updates, such as LLService closed down
    private Set<LLServiceStateListener> stateListeners =
        new CopyOnWriteArraySet<LLServiceStateListener>();

    // Listeners for XMPPLLConnections associated with this service
    private Set<LLServiceConnectionListener> llServiceConnectionListeners =
        new CopyOnWriteArraySet<LLServiceConnectionListener>();

    // Listeners for packets coming from this Link-local service
    private final Map<PacketListener, ListenerWrapper> listeners =
            new ConcurrentHashMap<PacketListener, ListenerWrapper>();

    // Presence discoverer, notifies of changes in presences on the network.
    private LLPresenceDiscoverer presenceDiscoverer;

    // chat listeners gets notified when new chats are created
    private Set<ChatManagerListener> chatListeners = new CopyOnWriteArraySet<>();
    
    // Set of Packet collector wrappers
    private Set<CollectorWrapper> collectorWrappers =
        new CopyOnWriteArraySet<CollectorWrapper>();

    // Set of associated connections.
    private Set<XMPPLLConnection> associatedConnections =
        new HashSet<XMPPLLConnection>();

    private ServerSocket socket;

    static {
        SmackConfiguration.getVersion();
    }

    /**
     * Spam stdout with some debug information.
     */
    public void spam() {
        System.out.println("Number of ingoing connection in map: " + ingoing.size());
        System.out.println("Number of outgoing connection in map: " + outgoing.size());

        System.out.println("Active chats:");
//        for (LLChat chat : chats.values()) {
//            System.out.println(" * " + chat.getServiceName());
//        }

        System.out.println("Known presences:");
        for (LLPresence presence : presenceDiscoverer.getPresences()) {
            System.out.println(" * " + presence.getServiceName() + "(" + presence.getStatus() + ", " + presence.getHost() + ":" + presence.getPort() + ")");
        }
        Thread.currentThread().getThreadGroup().list();
    }

    protected LLService(LLPresence presence, LLPresenceDiscoverer discoverer) {
        this.presence = presence;
        presenceDiscoverer = discoverer;
        service = this;

//        XMPPLLConnection.addLLConnectionListener(new AbstractConnectionListener() {
//
//            @Override
//            public void connected(XMPPConnection xmppConnection) {
//                if (! (xmppConnection instanceof XMPPLLConnection)) {
//                    return;
//                }
//                XMPPLLConnection connection = (XMPPLLConnection) xmppConnection;
//                // We only care about this connection if we were the one
//                // creating it
//                if (isAssociatedConnection(connection)) {
//                    if (connection.isInitiator()) {
//                        addOutgoingConnection(connection);
//                    }
//                    else {
//                        addIngoingConnection(connection);
//                    }
//
//                    connection.addConnectionListener(new ConnectionActivityListener(connection));
//
//                    // Notify listeners that a new connection associated with this
//                    // service has been created.
//                    notifyNewServiceConnection(connection);
//
//
//                    // add other existing packet filters associated with this service
//                    for (ListenerWrapper wrapper : listeners.values()) {
//                        connection.addPacketListener(wrapper.getPacketListener(),
//                                wrapper.getPacketFilter());
//                    }
//
//                    // add packet collectors
//                    for (CollectorWrapper cw : collectorWrappers) {
//                        cw.createPacketCollector(connection);
//                    }
//                }
//            }
//        });

        notifyServiceListeners(this);
    }

    /**
     * Add a LLServiceListener. The LLServiceListener is notified when a new
     * Link-local service is created.
     *
     * @param listener the LLServiceListener
     */
    public static void addLLServiceListener(LLServiceListener listener) {
        serviceCreatedListeners.add(listener);
    }

    /**
     * Remove a LLServiceListener.
     */
    public static void removeLLServiceListener(LLServiceListener listener) {
        serviceCreatedListeners.remove(listener);
    }

    /**
     * Notify LLServiceListeners about a new Link-local service.
     *
     * @param service the new service.
     */
    public static void notifyServiceListeners(LLService service) {
        for (LLServiceListener listener : serviceCreatedListeners) {
            listener.serviceCreated(service);
        }
    }

    /**
     * Returns the running mDNS/DNS-SD XMPP instance. There can only be one
     * instance at a time.
     *
     * @return the active LLService instance.
     * @throws XMPPException if the LLService hasn't been instantiated.
     */
    public synchronized static LLService getServiceInstance() throws XMPPException {
        if (service == null)
            throw new XMPPException.XMPPErrorException("Link-local service not initiated.",
                    new XMPPError(XMPPError.Condition.undefined_condition));
        return service;
    }

    /**
     * Registers the service to the mDNS/DNS-SD daemon.
     * Should be implemented by the class extending this, for mDNS/DNS-SD library specific calls.
     */
    protected abstract void registerService() throws XMPPException;

    /**
     * Re-announce the presence information by using the mDNS/DNS-SD daemon.
     */
    protected abstract void reannounceService() throws XMPPException;

    /**
     * Make the client unavailabe. Equivalent to sending unavailable-presence.
     */
    public abstract void makeUnavailable();

    /**
     * Update the text field information. Used for setting new presence information.
     */
    protected abstract void updateText();

    public void init() throws XMPPException {
        // allocate a new port for remote clients to connect to
        socket = bindRange(DEFAULT_MIN_PORT, DEFAULT_MAX_PORT);
//        presence.setPort(socket.getLocalPort());

        // register service on the allocated port
        registerService();

        // start to listen for new connections
        listenerThread = new Thread() {
            public void run() {
                try {
                    // Listen for connections
                    listenForConnections();

                    // If listen for connections returns with no exception,
                    // the service has closed down
                    for (LLServiceStateListener listener : stateListeners)
                        listener.serviceClosed();
                } catch (XMPPException e) {
                    for (LLServiceStateListener listener : stateListeners)
                        listener.serviceClosedOnError(e);
                }
            }
        };
        listenerThread.setName("Smack Link-local Service Listener");
        listenerThread.setDaemon(true);
        listenerThread.start();

        initiated = true;
    }

    public void close() throws IOException {
        done = true;

//        // close incoming connections
//        for (XMPPLLConnection connection : ingoing.values()) {
//            try {
//                connection.shutdown();
//            } catch (Exception e) {
//                // ignore
//            }
//        }

//        // close outgoing connections
//        for (XMPPLLConnection connection : outgoing.values()) {
//            try {
//                connection.shutdown();
//            } catch (Exception e) {
//                // ignore
//            }
//        }
        try {
            socket.close();
        } catch (IOException ioe) {
            // ignore
        }
    }

    /**
     * Listen for new connections on socket, and spawn XMPPLLConnections
     * when new connections are established.
     *
     * @throws XMPPException whenever an exception occurs
     */
    private void listenForConnections() throws XMPPException {
        while (!done) {
            try {
                // wait for new connection
                Socket s = socket.accept();

                LLConnectionConfiguration config =
                    new LLConnectionConfiguration(presence, s);
//                XMPPLLConnection connection = new XMPPLLConnection(this, config);

                // Associate the new connection with this service
//                addAssociatedConnection(connection);

                // Spawn new thread to handle the connecting.
                // The reason for spawning a new thread is to let two remote clients
//                // be able to connect at the same time.
//                Thread connectionInitiatorThread =
//                    new ConnectionInitiatorThread(connection);
//                connectionInitiatorThread.setName("Smack Link-local Connection Initiator");
//                connectionInitiatorThread.setDaemon(true);
//                connectionInitiatorThread.start();
            }
            catch (SocketException se) {
                // If we are closing down, it's probably closed socket exception.
                if (!done) {
                    throw new XMPPException.XMPPErrorException("Link-local service unexpectedly closed down.",
                            new XMPPError(XMPPError.Condition.undefined_condition), se);
                }
            }
            catch (IOException ioe) {
                throw new XMPPException.XMPPErrorException("Link-local service unexpectedly closed down.",
                        new XMPPError(XMPPError.Condition.undefined_condition), ioe);
            }
        }
    }

    /**
     * Bind one socket to any port within a given range.
     *
     * @param min the minimum port number allowed
     * @param max hte maximum port number allowed
     * @throws XMPPException if binding failed on all allowed ports.
     */
    private static ServerSocket bindRange(int min, int max) throws XMPPException {
        // TODO this method exists also for the local socks5 proxy code and should be factored out into a util
        int port = 0;
        for (int try_port = min; try_port <= max; try_port++) {
            try {
                ServerSocket socket = new ServerSocket(try_port);
                return socket;
            }
            catch (IOException e) {
                // failed to bind, try next
            }
        }
        throw new XMPPException.XMPPErrorException("Unable to bind port, no ports available.",
                new XMPPError(XMPPError.Condition.resource_constraint));
    }

    protected void unknownOriginMessage(Message message) {
        for (LLServiceStateListener listener : stateListeners) {
            listener.unknownOriginMessage(message);
        }
    }

    protected void serviceNameChanged(String newName, String oldName) {
        // update our own presence with the new name, for future connections
        presence.setServiceName(newName);

        // clean up connections
        XMPPLLConnection c;
        c = getConnectionTo(oldName);
        if (c != null)
            c.disconnect();
        c = getConnectionTo(newName);
        if (c != null)
            c.disconnect();

        // notify listeners
        for (LLServiceStateListener listener : stateListeners) {
            listener.serviceNameChanged(newName, oldName);
        }
    }

    /**
     * Adds a listener that are notified when a new link-local connection
     * has been established.
     *
     * @param listener A class implementing the LLConnectionListener interface.
     */
    public void addLLServiceConnectionListener(LLServiceConnectionListener listener) {
        llServiceConnectionListeners.add(listener);
    }

    /**
     * Removes a listener from the new connection listener list.
     *
     * @param listener The class implementing the LLConnectionListener interface that
     * is to be removed.
     */
    public void removeLLServiceConnectionListener(LLServiceConnectionListener listener) {
        llServiceConnectionListeners.remove(listener);
    }

    private void notifyNewServiceConnection(XMPPLLConnection connection) {
        for (LLServiceConnectionListener listener : llServiceConnectionListeners) {
            listener.connectionCreated(connection);
        }
    }

    /**
     * Add the given connection to the list of associated connections.
     * An associated connection means it's a Link-Local connection managed
     * by this service.
     *
     * @param connection the connection to be associated
     */
    private void addAssociatedConnection(XMPPLLConnection connection) {
        synchronized (associatedConnections) {
            associatedConnections.add(connection);
        }
    }

    /**
     * Remove the given connection from the list of associated connections.
     *
     * @param connection the connection to be removed.
     */
    private void removeAssociatedConnection(XMPPLLConnection connection) {
        synchronized (associatedConnections) {
            associatedConnections.remove(connection);
        }
    }

    /**
     * Return true if the given connection is associated / managed by this
     * service.
     *
     * @param connection the connection to be checked
     * @return true if the connection is associated with this service or false
     * if it is not associated with this service.
     */
    private boolean isAssociatedConnection(XMPPLLConnection connection) {
        synchronized (associatedConnections) {
            return associatedConnections.contains(connection);
        }
    } 

    /**
     * Add a packet listener.
     *
     * @param listener the PacketListener
     * @param filter the Filter
     */
    public void addPacketListener(PacketListener listener, PacketFilter filter) {
        ListenerWrapper wrapper = new ListenerWrapper(listener, filter);
        listeners.put(listener, wrapper);

        // Also add to existing connections
        synchronized (ingoing) {
            synchronized (outgoing) {
                for (XMPPLLConnection c : getConnections()) {
                    c.addPacketListener(listener, filter);
                }
            }
        }
    }

    /** 
     * Remove a packet listener.
     */
    public void removePacketListener(PacketListener listener) {
        listeners.remove(listener);

        // Also add to existing connections
        synchronized (ingoing) {
            synchronized (outgoing) {
                for (XMPPLLConnection c : getConnections()) {
                    c.removePacketListener(listener);
                }
            }
        }
    }

    /**
     * Add service state listener.
     *
     * @param listener the service state listener to be added.
     */
    public void addServiceStateListener(LLServiceStateListener listener) {
        stateListeners.add(listener);
    }

    /**
     * Remove service state listener.
     *
     * @param listener the service state listener to be removed.
     */
    public void removeServiceStateListener(LLServiceStateListener listener) {
        stateListeners.remove(listener);
    }

//    /**
//     * Add Link-local chat session listener. The chat session listener will
//     * be notified when new link-local chat sessions are created.
//     *
//     * @param listener the listener to be added.
//     */
//    public void addLLChatListener(ChatManagerListener<LLChat> listener) {
//        chatListeners.add(listener);
//    }
//
//    /**
//     * Remove Link-local chat session listener. 
//     *
//     * @param listener the listener to be removed.
//     */
//    public void removeLLChatListener(ChatManagerListener<LLChat> listener) {
//        chatListeners.remove(listener);
//    }

    /**
     * Add presence listener. A presence listener will be notified of new
     * presences, presences going offline, and changes in presences.
     *
     * @param listener the listener to be added.
     */
    public void addPresenceListener(LLPresenceListener listener) {
        presenceDiscoverer.addPresenceListener(listener);
    }

    /**
     * Remove presence listener.
     *
     * @param listener presence listener to be removed.
     */
    public void removePresenceListener(LLPresenceListener listener) {
        presenceDiscoverer.removePresenceListener(listener);
    }

    /**
     * Get the presence information associated with the given service name.
     *
     * @param serviceName the service name which information should be returned.
     * @return the service information.
     */
    public LLPresence getPresenceByServiceName(String serviceName) {
        return presenceDiscoverer.getPresence(serviceName);
    }

    public CollectorWrapper createPacketCollector(PacketFilter filter) {
        CollectorWrapper wrapper = new CollectorWrapper(filter);
        collectorWrappers.add(wrapper);
        return wrapper;
    }

    /**
     * Return a collection of all active connections. This may be used if the
     * user wants to change a property on all connections, such as add a service
     * discovery feature or other.
     *
     * @return a colllection of all active connections.
     */
    public Collection<XMPPLLConnection> getConnections() {
        Collection<XMPPLLConnection> connections =
            new ArrayList<XMPPLLConnection>(outgoing.values());
        connections.addAll(ingoing.values());
        return connections;
    }

    /**
     * Returns a connection to a given service name.
     * First checks for an outgoing connection, if noone exists,
     * try ingoing.
     *
     * @param serviceName the service name
     * @return a connection associated with the service name or null if no
     * connection is available.
     */
    XMPPLLConnection getConnectionTo(String serviceName) {
        XMPPLLConnection connection = outgoing.get(serviceName);
        if (connection != null)
            return connection;
        return ingoing.get(serviceName);
    }

    void addIngoingConnection(XMPPLLConnection connection) {
        ingoing.put(connection.getServiceName(), connection);
    }

    void removeIngoingConnection(XMPPLLConnection connection) {
        ingoing.remove(connection.getServiceName());
    }

    void addOutgoingConnection(XMPPLLConnection connection) {
        outgoing.put(connection.getServiceName(), connection);
    }

    void removeOutgoingConnection(XMPPLLConnection connection) {
        outgoing.remove(connection.getServiceName());
    }

//    LLChat removeLLChat(String serviceName) {
//        return chats.remove(serviceName);
//    }
//
//    /**
//     * Returns a new {@link org.jivesoftware.smack.serverless.LLChat}
//     * at the request of the local client.
//     * This method should not be used to create Chat sessions
//     * in response to messages received from remote peers.
//     */
//    void newLLChat(LLChat chat) {
//        chats.put(chat.getServiceName(), chat);
//        for (ChatManagerListener<LLChat> listener : chatListeners) {
//            listener.chatCreated(chat, true);
//        }
//    }

    /**
     * Get a LLChat associated with a given service name.
     * If no LLChat session is available, a new one is created.
     *
     * This method should not be used to create Chat sessions
     * in response to messages received from remote peers.
     *
     * @param serviceName the service name
     * @return a chat session instance associated with the given service name.
     */
    public Chat getChat(String serviceName) throws XMPPException, IOException, SmackException {
        Chat chat = chats.get(serviceName);
        if (chat == null) {
            LLPresence presence = getPresenceByServiceName(serviceName);
            if (presence == null)
                throw new XMPPException.XMPPErrorException("Can't initiate new chat to '" +
                        serviceName + "': mDNS presence unknown.", new XMPPError(XMPPError.Condition.undefined_condition));
            chat =ChatManager.getInstanceFor(service.getConnection(presence.getServiceName())).createChat(
                    presence.getServiceName(),
                    UUID.randomUUID().toString(), null);
            //newLLChat(chat);
        }
        return chat;
    }

    /**
     * Returns a XMPPLLConnection to the serviceName.
     * If no established connection exists, a new connection is created.
     * 
     * @param serviceName Service name of the remote client.
     * @return A connection to the given service name.
     */
    public XMPPLLConnection getConnection(String serviceName) throws XMPPException.XMPPErrorException, IOException, SmackException {
        // If a connection exists, return it.
        XMPPLLConnection connection = getConnectionTo(serviceName);
        if (connection != null)
            return connection;

        // If no connection exists, look up the presence and connect according to.
        LLPresence remotePresence = getPresenceByServiceName(serviceName);

        if (remotePresence == null) {
            throw new XMPPException.XMPPErrorException("Can't initiate connection, remote peer is not available.",
                    new XMPPError(XMPPError.Condition.recipient_unavailable));
        }

//        LLConnectionConfiguration config =
//            new LLConnectionConfiguration(presence, remotePresence);
//        connection = new XMPPLLConnection(this, config);
        // Associate the new connection with this service
        addAssociatedConnection(connection);
        connection.connect();
        addOutgoingConnection(connection);

        return connection;
    }

    /**
     * Send a message to the remote peer.
     *
     * @param message the message to be sent.
     * @throws XMPPException if the message cannot be sent.
     */
    void sendMessage(Message message) throws XMPPException, IOException, SmackException {
        sendPacket(message);
    }


    /**
     * Send a packet to the remote peer.
     *
     * @param packet the packet to be sent.
     * @throws XMPPException if the packet cannot be sent.
     */
    public void sendPacket(Packet packet) throws XMPPException, IOException, SmackException {
        getConnection(packet.getTo()).sendPacket(packet);
    }

    /**
     * Send an IQ set or get and wait for the response. This function works
     * different from a normal one-connection IQ request where a packet
     * collector is created and added to the connection. This function
     * takes care of (at least) two cases when this doesn't work:
     * <ul>
     *  <li>Consider client A requests something from B. This is done by
     *      A connecting to B (no existing connection is available), then
     *      sending an IQ request to B using the new connection and starts
     *      waiting for a reply. However the connection between them may be
     *      terminated due to inactivity, and for B to reply, it have to 
     *      establish a new connection. This function takes care of this
     *      by listening for the packets on all new connections.</li>
     *  <li>Consider client A and client B concurrently establishes
     *      connections between them. This will result in two parallell
     *      connections between the two entities and the two clients may
     *      choose whatever connection to use when communicating. This
     *      function takes care of the possibility that if A requests
     *      something from B using connection #1 and B replies using
     *      connection #2, the packet will still be collected.</li>
     * </ul>
     */
    public IQ getIQResponse(IQ request) throws XMPPException, IOException, SmackException {
        XMPPLLConnection connection = getConnection(request.getTo());

        // Create a packet collector to listen for a response.
        // Filter: req.id == rpl.id ^ (rp.iqtype in (result, error))
        CollectorWrapper collector = createPacketCollector(
                new AndFilter(
                    new PacketIDFilter(request.getPacketID()),
                    new OrFilter(
                        IQTypeFilter.RESULT,
                        IQTypeFilter.ERROR)));

        connection.sendPacket(request);

        // Wait up to 5 seconds for a result.
        IQ result = (IQ) collector.nextResult(
                SmackConfiguration.getDefaultPacketReplyTimeout());

        // Stop queuing results
        collector.cancel();
        if (result == null) {
            throw new XMPPException.XMPPErrorException("No response from the remote host.",
                    new XMPPError(XMPPError.Condition.undefined_condition));
        }

        return result;
    }

    /**
     * Update the presence information announced by the mDNS/DNS-SD daemon.
     * The presence object stored in the LLService class will be updated
     * with the new information and the daemon will reannounce the changes.
     *
     * @param presence the new presence information
     * @throws XMPPException if an error occurs
     */
    public void updateLocalPresence(LLPresence presence) throws XMPPException {
//        this.presence.update(presence);

        if (initiated) {
            updateText();
            reannounceService();
        }
    }

    /**
     * Get current Link-local presence.
     */
    public LLPresence getLocalPresence() {
        return presence;
    }

    /**
     * ConnectionActivityListener listens for link-local connection activity
     * such as closed connection and broken connection, and keeps record of
     * what active connections exist up to date.
     */
    private class ConnectionActivityListener implements ConnectionListener {
        private XMPPLLConnection connection;

        ConnectionActivityListener(XMPPLLConnection connection) {
            this.connection = connection;
        }

        @Override
        public void connected(XMPPConnection connection) {

        }

        @Override
        public void authenticated(XMPPConnection connection) {

        }

        public void connectionClosed() {
            removeConnectionRecord();
        }

        public void connectionClosedOnError(Exception e) {
            removeConnectionRecord();
        }

        public void reconnectingIn(int seconds) {
        }

        public void reconnectionSuccessful() {
        }

        public void reconnectionFailed(Exception e) {
        }

        private void removeConnectionRecord() {
            if (connection.isInitiator())
                removeOutgoingConnection(connection);
            else
                removeIngoingConnection(connection);

            removeAssociatedConnection(connection);
        }
    }

    /**
     * Initiates a connection in a seperate thread, controlling
     * it was established correctly and stream was initiated.
     */
    private class ConnectionInitiatorThread extends Thread {
        XMPPLLConnection connection;

        ConnectionInitiatorThread(XMPPLLConnection connection) {
            this.connection = connection;
        }

        public void run() {
//            try {
//                connection.initListen();
//            }
//            catch (XMPPException | SmackException | IOException e) {
//                // ignore, since its an incoming connection
//                // there is nothing to save
//            }
        }
    }

    /**
     * A wrapper class to associate a packet filter with a listener.
     */
    private static class ListenerWrapper {

        private PacketListener packetListener;
        private PacketFilter packetFilter;

        public ListenerWrapper(PacketListener packetListener, PacketFilter packetFilter) {
            this.packetListener = packetListener;
            this.packetFilter = packetFilter;
        }
       
        public void notifyListener(Packet packet) throws SmackException.NotConnectedException {
            if (packetFilter == null || packetFilter.accept(packet)) {
                packetListener.processPacket(packet);
            }
        }

        public PacketListener getPacketListener() {
            return packetListener;
        }

        public PacketFilter getPacketFilter() {
            return packetFilter;
        }
    }

    /**
     * Packet Collector Wrapper which is used for collecting packages
     * from multiple connections as well as newly established connections (works
     * together with LLService constructor.
     */
    public class CollectorWrapper {
        // Existing collectors.
        private Set<PacketCollector> collectors =
            new CopyOnWriteArraySet<PacketCollector>();

        // Packet filter for all the collectors.
        private PacketFilter packetFilter;

        // A common object used for shared locking between
        // the collectors.
        private Object lock = new Object();

        private CollectorWrapper(PacketFilter packetFilter) {
            this.packetFilter = packetFilter;

            // Apply to all active connections
            for (XMPPLLConnection connection : getConnections()) {
                createPacketCollector(connection);
            }
        }

        /**
         * Create a new per-connection packet collector.
         *
         * @param connection the connection the collector should be added to.
         */
        private void createPacketCollector(XMPPLLConnection connection) {
            synchronized (connection) {
                PacketCollector collector =
                    connection.createPacketCollector(packetFilter);
                //collector.setLock(lock);
                collectors.add(collector);
            }
        }

        /**
         * Returns the next available packet. The method call will block (not return)
         * until a packet is available or the <tt>timeout</tt> has elapsed. If the
         * timeout elapses without a result, <tt>null</tt> will be returned.
         *
         * @param timeout the amount of time to wait for the next packet
         *                (in milleseconds).
         * @return the next available packet.
         */
        public synchronized Packet nextResult(long timeout) {
            Packet packet;
            long waitTime = timeout;
            long start = System.currentTimeMillis();

            try {
                while (true) {
                    for (PacketCollector c : collectors) {
                        if (c.isCanceled())
                            collectors.remove(c);
                        else {
                            packet = c.pollResult();
                            if (packet != null)
                                return packet;
                        }
                    }

                    if (waitTime <= 0) {
                        break;
                    }

                    // TODO: lock won't be notified bc it's no longer managed by PacketCollector
                    // Perhaps we need a different mechanism here
                    // wait
                    synchronized (lock) {
                        lock.wait(waitTime);
                    }
                    long now = System.currentTimeMillis();
                    waitTime -= (now - start);
                }
            }
            catch (InterruptedException ie) {
                // ignore
            }

            return null;
        }

        public void cancel() {
            for (PacketCollector c : collectors) {
                c.cancel();
            }
            collectorWrappers.remove(this);
        }
    }
}

