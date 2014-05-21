/**
 *
 * Copyright 2009 Jive Software.
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

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.security.sasl.SaslException;

import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.SmackException.ConnectionException;
import org.jivesoftware.smack.SmackException.ResourceBindingNotOfferedException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.compression.XMPPInputOutputStream;
import org.jivesoftware.smack.debugger.SmackDebugger;
import org.jivesoftware.smack.filter.IQReplyFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.packet.Bind;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Session;

/**
 * The abstract XMPPConnection class provides an interface for connections to a XMPP server and
 * implements shared methods which are used by the different types of connections (e.g.
 * {@link XMPPTCPConnection} or {@link XMPPBOSHConnection}). To create a connection to a XMPP server
 * a simple usage of this API might look like the following:
 * <p>
 * 
 * <pre>
 * // Create a connection to the igniterealtime.org XMPP server.
 * XMPPConnection con = new XMPPTCPConnection("igniterealtime.org");
 * // Connect to the server
 * con.connect();
 * // Most servers require you to login before performing other tasks.
 * con.login("jsmith", "mypass");
 * // Start a new conversation with John Doe and send him a message.
 * Chat chat = ChatManager.getInstanceFor(con).createChat(<font color="green">"jdoe@igniterealtime.org"</font>, new MessageListener() {
 *     public void processMessage(Chat chat, Message message) {
 *         // Print out any messages we get back to standard out.
 *         System.out.println(<font color="green">"Received message: "</font> + message);
 *     }
 * });
 * chat.sendMessage(<font color="green">"Howdy!"</font>);
 * // Disconnect from the server
 * con.disconnect();
 * </pre>
 * <p>
 * Connections can be reused between connections. This means that an Connection may be connected,
 * disconnected and then connected again. Listeners of the Connection will be retained accross
 * connections.
 * <p>
 * If a connected XMPPConnection gets disconnected abruptly and automatic reconnection is enabled (
 * {@link ConnectionConfiguration#isReconnectionAllowed()}, the default), then it will try to
 * reconnect again. To stop the reconnection process, use {@link #disconnect()}. Once stopped you
 * can use {@link #connect()} to manually connect to the server.
 * 
 * @author Matt Tucker
 * @author Guenther Niess
 */
@SuppressWarnings("javadoc")
public abstract class XMPPConnection {
    private static final Logger LOGGER = Logger.getLogger(XMPPConnection.class.getName());
    
    /** 
     * Counter to uniquely identify connections that are created.
     */
    private final static AtomicInteger connectionCounter = new AtomicInteger(0);

    /**
     * A set of listeners which will be invoked if a new connection is created.
     */
    private final static Set<ConnectionCreationListener> connectionEstablishedListeners =
            new CopyOnWriteArraySet<ConnectionCreationListener>();

    static {
        // Ensure the SmackConfiguration class is loaded by calling a method in it.
        SmackConfiguration.getVersion();
    }

    /**
     * A collection of ConnectionListeners which listen for connection closing
     * and reconnection events.
     */
    protected final Collection<ConnectionListener> connectionListeners =
            new CopyOnWriteArrayList<ConnectionListener>();

    /**
     * A collection of PacketCollectors which collects packets for a specified filter
     * and perform blocking and polling operations on the result queue.
     */
    protected final Collection<PacketCollector> collectors = new ConcurrentLinkedQueue<PacketCollector>();

    /**
     * List of PacketListeners that will be notified when a new packet was received.
     */
    protected final Map<PacketListener, ListenerWrapper> recvListeners =
            new ConcurrentHashMap<PacketListener, ListenerWrapper>();

    /**
     * List of PacketListeners that will be notified when a new packet was sent.
     */
    protected final Map<PacketListener, ListenerWrapper> sendListeners =
            new ConcurrentHashMap<PacketListener, ListenerWrapper>();

    /**
     * List of PacketInterceptors that will be notified when a new packet is about to be
     * sent to the server. These interceptors may modify the packet before it is being
     * actually sent to the server.
     */
    protected final Map<PacketInterceptor, InterceptorWrapper> interceptors =
            new ConcurrentHashMap<PacketInterceptor, InterceptorWrapper>();

    /**
     * 
     */
    private long packetReplyTimeout = SmackConfiguration.getDefaultPacketReplyTimeout();

    /**
     * The SmackDebugger allows to log and debug XML traffic.
     */
    protected SmackDebugger debugger = null;

    /**
     * The Reader which is used for the debugger.
     */
    protected Reader reader;

    /**
     * The Writer which is used for the debugger.
     */
    protected Writer writer;


    /**
     * The SASLAuthentication manager that is responsible for authenticating with the server.
     */
    protected SASLAuthentication saslAuthentication = new SASLAuthentication(this);

    /**
     * A number to uniquely identify connections that are created. This is distinct from the
     * connection ID, which is a value sent by the server once a connection is made.
     */
    protected final int connectionCounterValue = connectionCounter.getAndIncrement();

    /**
     * Holds the initial configuration used while creating the connection.
     */
    protected final ConnectionConfiguration config;

    /**
     * Holds the Caps Node information for the used XMPP service (i.e. the XMPP server)
     */
    private String serviceCapsNode;

    /**
     * Defines how the from attribute of outgoing stanzas should be handled.
     */
    private FromMode fromMode = FromMode.OMITTED;

    /**
     * Stores whether the server supports rosterVersioning
     */
    private boolean rosterVersioningSupported = false;

    protected XMPPInputOutputStream compressionHandler;

    private final ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(2,
                    new SmackExecutorThreadFactory(connectionCounterValue));

    /**
     * SmackExecutorThreadFactory is a *static* inner class of XMPPConnection. Note that we must not
     * use anonymous classes in order to prevent threads from leaking.
     */
    private static final class SmackExecutorThreadFactory implements ThreadFactory {
        private final int connectionCounterValue;
        private int count = 0;

        private SmackExecutorThreadFactory(int connectionCounterValue) {
            this.connectionCounterValue = connectionCounterValue;
        }

        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable, "Smack Executor Service " + count++ + " ("
                            + connectionCounterValue + ")");
            thread.setDaemon(true);
            return thread;
        }
    }

    private Roster roster;

    /**
     * The used host to establish the connection to
     */
    private String host;

    /**
     * The used port to establish the connection to
     */
    private int port;

    /**
     * Set to true if the server requires the connection to be binded in order to continue.
     * <p>
     * Note that we use AtomicBoolean here because it allows us to set the Boolean *object*, which
     * we also use as synchronization object. A plain non-atomic Boolean object would be newly created
     * for every change of the boolean value, which makes it useless as object for wait()/notify().
     */
    private AtomicBoolean bindingRequired = new AtomicBoolean(false);

    private boolean sessionSupported;

    /**
     * 
     */
    private IOException connectionException;

    /**
     * Flag that indicates if the user is currently authenticated with the server.
     */
    protected boolean authenticated = false;

    /**
     * Flag that indicates if the user was authenticated with the server when the connection
     * to the server was closed (abruptly or not).
     */
    protected boolean wasAuthenticated = false;

    /**
     * Create a new XMPPConnection to a XMPP server.
     * 
     * @param configuration The configuration which is used to establish the connection.
     */
    protected XMPPConnection(ConnectionConfiguration configuration) {
        config = configuration;
    }

    /**
     * Returns the configuration used to connect to the server.
     * 
     * @return the configuration used to connect to the server.
     */
    protected ConnectionConfiguration getConfiguration() {
        return config;
    }

    /**
     * Returns the name of the service provided by the XMPP server for this connection.
     * This is also called XMPP domain of the connected server. After
     * authenticating with the server the returned value may be different.
     * 
     * @return the name of the service provided by the XMPP server.
     */
    public String getServiceName() {
        return config.getServiceName();
    }

    /**
     * Returns the host name of the server where the XMPP server is running. This would be the
     * IP address of the server or a name that may be resolved by a DNS server.
     * 
     * @return the host name of the server where the XMPP server is running or null if not yet connected.
     */
    public String getHost() {
        return host;
    }

    /**
     * Returns the port number of the XMPP server for this connection. The default port
     * for normal connections is 5222.
     * 
     * @return the port number of the XMPP server or 0 if not yet connected.
     */
    public int getPort() {
        return port;
    }

    /**
     * Returns the full XMPP address of the user that is logged in to the connection or
     * <tt>null</tt> if not logged in yet. An XMPP address is in the form
     * username@server/resource.
     * 
     * @return the full XMPP address of the user logged in.
     */
    public abstract String getUser();

    /**
     * Returns the connection ID for this connection, which is the value set by the server
     * when opening a XMPP stream. If the server does not set a connection ID, this value
     * will be null. This value will be <tt>null</tt> if not connected to the server.
     * 
     * @return the ID of this connection returned from the XMPP server or <tt>null</tt> if
     *      not connected to the server.
     */
    public abstract String getConnectionID();

    /**
     * Returns true if currently connected to the XMPP server.
     * 
     * @return true if connected.
     */
    public abstract boolean isConnected();

    /**
     * Returns true if currently authenticated by successfully calling the login method.
     * 
     * @return true if authenticated.
     */
    public abstract boolean isAuthenticated();

    /**
     * Returns true if currently authenticated anonymously.
     * 
     * @return true if authenticated anonymously.
     */
    public abstract boolean isAnonymous();

    /**
     * Returns true if the connection to the server has successfully negotiated encryption. 
     * 
     * @return true if a secure connection to the server.
     */
    public abstract boolean isSecureConnection();

    protected abstract void sendPacketInternal(Packet packet) throws NotConnectedException;

    /**
     * Returns true if network traffic is being compressed. When using stream compression network
     * traffic can be reduced up to 90%. Therefore, stream compression is ideal when using a slow
     * speed network connection. However, the server will need to use more CPU time in order to
     * un/compress network data so under high load the server performance might be affected.
     * 
     * @return true if network traffic is being compressed.
     */
    public abstract boolean isUsingCompression();

    /**
     * Establishes a connection to the XMPP server and performs an automatic login
     * only if the previous connection state was logged (authenticated). It basically
     * creates and maintains a connection to the server.
     * <p>
     * Listeners will be preserved from a previous connection.
     * 
     * @throws XMPPException if an error occurs on the XMPP protocol level.
     * @throws SmackException if an error occurs somewhere else besides XMPP protocol level.
     * @throws IOException 
     * @throws ConnectionException with detailed information about the failed connection.
     */
    public void connect() throws SmackException, IOException, XMPPException {
        saslAuthentication.init();
        bindingRequired.set(false);
        sessionSupported = false;
        connectionException = null;
        connectInternal();
    }

    /**
     * Abstract method that concrete subclasses of XMPPConnection need to implement to perform their
     * way of XMPP connection establishment. Implementations must guarantee that this method will
     * block until the last features stanzas has been parsed and the features have been reported
     * back to XMPPConnection (e.g. by calling @{link {@link XMPPConnection#serverRequiresBinding()}
     * and such).
     * <p>
     * Also implementations are required to perform an automatic login if the previous connection
     * state was logged (authenticated).
     *
     * @throws SmackException
     * @throws IOException
     * @throws XMPPException
     */
    protected abstract void connectInternal() throws SmackException, IOException, XMPPException;

    /**
     * Logs in to the server using the strongest authentication mode supported by
     * the server, then sets presence to available. If the server supports SASL authentication 
     * then the user will be authenticated using SASL if not Non-SASL authentication will 
     * be tried. If more than five seconds (default timeout) elapses in each step of the 
     * authentication process without a response from the server, or if an error occurs, a 
     * XMPPException will be thrown.<p>
     * 
     * Before logging in (i.e. authenticate) to the server the connection must be connected.
     * 
     * It is possible to log in without sending an initial available presence by using
     * {@link ConnectionConfiguration#setSendPresence(boolean)}. If this connection is
     * not interested in loading its roster upon login then use
     * {@link ConnectionConfiguration#setRosterLoadedAtLogin(boolean)}.
     * Finally, if you want to not pass a password and instead use a more advanced mechanism
     * while using SASL then you may be interested in using
     * {@link ConnectionConfiguration#setCallbackHandler(javax.security.auth.callback.CallbackHandler)}.
     * For more advanced login settings see {@link ConnectionConfiguration}.
     * 
     * @param username the username.
     * @param password the password or <tt>null</tt> if using a CallbackHandler.
     * @throws XMPPException if an error occurs on the XMPP protocol level.
     * @throws SmackException if an error occurs somehwere else besides XMPP protocol level.
     * @throws IOException 
     * @throws SaslException 
     */
    public void login(String username, String password) throws XMPPException, SmackException, SaslException, IOException {
        login(username, password, "Smack");
    }

    /**
     * Logs in to the server using the strongest authentication mode supported by
     * the server, then sets presence to available. If the server supports SASL authentication 
     * then the user will be authenticated using SASL if not Non-SASL authentication will 
     * be tried. If more than five seconds (default timeout) elapses in each step of the 
     * authentication process without a response from the server, or if an error occurs, a 
     * XMPPException will be thrown.<p>
     * 
     * Before logging in (i.e. authenticate) to the server the connection must be connected.
     * 
     * It is possible to log in without sending an initial available presence by using
     * {@link ConnectionConfiguration#setSendPresence(boolean)}. If this connection is
     * not interested in loading its roster upon login then use
     * {@link ConnectionConfiguration#setRosterLoadedAtLogin(boolean)}.
     * Finally, if you want to not pass a password and instead use a more advanced mechanism
     * while using SASL then you may be interested in using
     * {@link ConnectionConfiguration#setCallbackHandler(javax.security.auth.callback.CallbackHandler)}.
     * For more advanced login settings see {@link ConnectionConfiguration}.
     * 
     * @param username the username.
     * @param password the password or <tt>null</tt> if using a CallbackHandler.
     * @param resource the resource.
     * @throws XMPPException if an error occurs on the XMPP protocol level.
     * @throws SmackException if an error occurs somehwere else besides XMPP protocol level.
     * @throws IOException 
     * @throws SaslException 
     */
    public abstract void login(String username, String password, String resource) throws XMPPException, SmackException, SaslException, IOException;

    /**
     * Logs in to the server anonymously. Very few servers are configured to support anonymous
     * authentication, so it's fairly likely logging in anonymously will fail. If anonymous login
     * does succeed, your XMPP address will likely be in the form "123ABC@server/789XYZ" or
     * "server/123ABC" (where "123ABC" and "789XYZ" is a random value generated by the server).
     * 
     * @throws XMPPException if an error occurs on the XMPP protocol level.
     * @throws SmackException if an error occurs somehwere else besides XMPP protocol level.
     * @throws IOException 
     * @throws SaslException 
     */
    public abstract void loginAnonymously() throws XMPPException, SmackException, SaslException, IOException;

    /**
     * Notification message saying that the server requires the client to bind a
     * resource to the stream.
     */
    protected void serverRequiresBinding() {
        synchronized (bindingRequired) {
            bindingRequired.set(true);
            bindingRequired.notify();
        }
    }

    /**
     * Notification message saying that the server supports sessions. When a server supports
     * sessions the client needs to send a Session packet after successfully binding a resource
     * for the session.
     */
    protected void serverSupportsSession() {
        sessionSupported = true;
    }

    protected String bindResourceAndEstablishSession(String resource) throws XMPPErrorException,
                    ResourceBindingNotOfferedException, NoResponseException, NotConnectedException {

        synchronized (bindingRequired) {
            if (!bindingRequired.get()) {
                try {
                    bindingRequired.wait(getPacketReplyTimeout());
                }
                catch (InterruptedException e) {
                    // Ignore
                }
                if (!bindingRequired.get()) {
                    // Server never offered resource binding, which is REQURIED in XMPP client and
                    // server
                    // implementations as per RFC6120 7.2
                    throw new ResourceBindingNotOfferedException();
                }
            }
        }

        Bind bindResource = new Bind();
        bindResource.setResource(resource);

        Bind response = (Bind) createPacketCollectorAndSend(bindResource).nextResultOrThrow();
        String userJID = response.getJid();

        if (sessionSupported && !getConfiguration().isLegacySessionDisabled()) {
            Session session = new Session();
            createPacketCollectorAndSend(session).nextResultOrThrow();
        }
        return userJID;
    }

    protected void setConnectionException(IOException exception) {
        connectionException = exception;
    }

    protected void throwConnectionExceptionOrNoResponse() throws IOException, NoResponseException {
        if (connectionException != null) {
            throw connectionException;
        } else {
            throw new NoResponseException();
        }
    }

    protected Reader getReader() {
        return reader;
    }

    protected Writer getWriter() {
        return writer;
    }

    protected void setServiceName(String serviceName) {
        config.setServiceName(serviceName);
    }
    
    protected void setLoginInfo(String username, String password, String resource) {
        config.setLoginInfo(username, password, resource);
    }
    
    protected void serverSupportsAccountCreation() {
        AccountManager.getInstance(this).setSupportsAccountCreation(true);
    }

    protected void maybeResolveDns() throws Exception {
        config.maybeResolveDns();
    }

    /**
     * Sends the specified packet to the server.
     * 
     * @param packet the packet to send.
     * @throws NotConnectedException 
     */
    public void sendPacket(Packet packet) throws NotConnectedException {
        if (!isConnected()) {
            throw new NotConnectedException();
        }
        if (packet == null) {
            throw new NullPointerException("Packet is null.");
        }
        switch (fromMode) {
        case OMITTED:
            packet.setFrom(null);
            break;
        case USER:
            packet.setFrom(getUser());
            break;
        case UNCHANGED:
        default:
            break;
        }
        // Invoke interceptors for the new packet that is about to be sent. Interceptors may modify
        // the content of the packet.
        firePacketInterceptors(packet);
        sendPacketInternal(packet);
        // Process packet writer listeners. Note that we're using the sending thread so it's
        // expected that listeners are fast.
        firePacketSendingListeners(packet);
    }

    /**
     * Returns the roster for the user.
     * <p>
     * This method will never return <code>null</code>, instead if the user has not yet logged into
     * the server or is logged in anonymously all modifying methods of the returned roster object
     * like {@link Roster#createEntry(String, String, String[])},
     * {@link Roster#removeEntry(RosterEntry)} , etc. except adding or removing
     * {@link RosterListener}s will throw an IllegalStateException.
     * 
     * @return the user's roster.
     * @throws IllegalStateException if the connection is anonymous
     */
    public Roster getRoster() {
        if (isAnonymous()) {
            throw new IllegalStateException("Anonymous users can't have a roster");
        }
        // synchronize against login()
        synchronized(this) {
            if (roster == null) {
                roster = new Roster(this);
            }
            if (!isAuthenticated()) {
                return roster;
            }
        }

        // If this is the first time the user has asked for the roster after calling
        // login, we want to wait for the server to send back the user's roster. This
        // behavior shields API users from having to worry about the fact that roster
        // operations are asynchronous, although they'll still have to listen for
        // changes to the roster. Note: because of this waiting logic, internal
        // Smack code should be wary about calling the getRoster method, and may need to
        // access the roster object directly.
        // Also only check for rosterInitalized is isRosterLoadedAtLogin is set, otherwise the user
        // has to manually call Roster.reload() before he can expect a initialized roster.
        if (!roster.rosterInitialized && config.isRosterLoadedAtLogin()) {
            try {
                synchronized (roster) {
                    long waitTime = getPacketReplyTimeout();
                    long start = System.currentTimeMillis();
                    while (!roster.rosterInitialized) {
                        if (waitTime <= 0) {
                            break;
                        }
                        roster.wait(waitTime);
                        long now = System.currentTimeMillis();
                        waitTime -= now - start;
                        start = now;
                    }
                }
            }
            catch (InterruptedException ie) {
                // Ignore.
            }
        }
        return roster;
    }

    /**
     * Returns the SASLAuthentication manager that is responsible for authenticating with
     * the server.
     * 
     * @return the SASLAuthentication manager that is responsible for authenticating with
     *         the server.
     */
    protected SASLAuthentication getSASLAuthentication() {
        return saslAuthentication;
    }

    /**
     * Closes the connection by setting presence to unavailable then closing the connection to
     * the XMPP server. The XMPPConnection can still be used for connecting to the server
     * again.
     *
     * @throws NotConnectedException 
     */
    public void disconnect() throws NotConnectedException {
        disconnect(new Presence(Presence.Type.unavailable));
    }

    /**
     * Closes the connection. A custom unavailable presence is sent to the server, followed
     * by closing the stream. The XMPPConnection can still be used for connecting to the server
     * again. A custom unavailable presence is useful for communicating offline presence
     * information such as "On vacation". Typically, just the status text of the presence
     * packet is set with online information, but most XMPP servers will deliver the full
     * presence packet with whatever data is set.
     * 
     * @param unavailablePresence the presence packet to send during shutdown.
     * @throws NotConnectedException 
     */
    public synchronized void disconnect(Presence unavailablePresence) throws NotConnectedException {
        if (!isConnected()) {
            return;
        }

        sendPacket(unavailablePresence);
        shutdown();
        callConnectionClosedListener();
    };

    /**
     * Shuts the current connection down.
     */
    protected abstract void shutdown();

    /**
     * Adds a new listener that will be notified when new Connections are created. Note
     * that newly created connections will not be actually connected to the server.
     * 
     * @param connectionCreationListener a listener interested on new connections.
     */
    public static void addConnectionCreationListener(
            ConnectionCreationListener connectionCreationListener) {
        connectionEstablishedListeners.add(connectionCreationListener);
    }

    /**
     * Removes a listener that was interested in connection creation events.
     * 
     * @param connectionCreationListener a listener interested on new connections.
     */
    public static void removeConnectionCreationListener(
            ConnectionCreationListener connectionCreationListener) {
        connectionEstablishedListeners.remove(connectionCreationListener);
    }

    /**
     * Get the collection of listeners that are interested in connection creation events.
     * 
     * @return a collection of listeners interested on new connections.
     */
    protected static Collection<ConnectionCreationListener> getConnectionCreationListeners() {
        return Collections.unmodifiableCollection(connectionEstablishedListeners);
    }

    /**
     * Adds a connection listener to this connection that will be notified when
     * the connection closes or fails.
     * 
     * @param connectionListener a connection listener.
     */
    public void addConnectionListener(ConnectionListener connectionListener) {
        if (connectionListener == null) {
            return;
        }
        if (!connectionListeners.contains(connectionListener)) {
            connectionListeners.add(connectionListener);
        }
    }

    /**
     * Removes a connection listener from this connection.
     * 
     * @param connectionListener a connection listener.
     */
    public void removeConnectionListener(ConnectionListener connectionListener) {
        connectionListeners.remove(connectionListener);
    }

    /**
     * Get the collection of listeners that are interested in connection events.
     * 
     * @return a collection of listeners interested on connection events.
     */
    protected Collection<ConnectionListener> getConnectionListeners() {
        return connectionListeners;
    }

    /**
     * Creates a new packet collector collecting packets that are replies to <code>packet</code>.
     * Does also send <code>packet</code>. The packet filter for the collector is an
     * {@link IQReplyFilter}, guaranteeing that packet id and JID in the 'from' address have
     * expected values.
     *
     * @param packet the packet to filter responses from
     * @return a new packet collector.
     * @throws NotConnectedException 
     */
    public PacketCollector createPacketCollectorAndSend(IQ packet) throws NotConnectedException {
        PacketFilter packetFilter = new IQReplyFilter(packet, this);
        // Create the packet collector before sending the packet
        PacketCollector packetCollector = createPacketCollector(packetFilter);
        // Now we can send the packet as the collector has been created
        sendPacket(packet);
        return packetCollector;
    }

    /**
     * Creates a new packet collector for this connection. A packet filter determines
     * which packets will be accumulated by the collector. A PacketCollector is
     * more suitable to use than a {@link PacketListener} when you need to wait for
     * a specific result.
     * 
     * @param packetFilter the packet filter to use.
     * @return a new packet collector.
     */
    public PacketCollector createPacketCollector(PacketFilter packetFilter) {
        PacketCollector collector = new PacketCollector(this, packetFilter);
        // Add the collector to the list of active collectors.
        collectors.add(collector);
        return collector;
    }

    /**
     * Remove a packet collector of this connection.
     * 
     * @param collector a packet collectors which was created for this connection.
     */
    protected void removePacketCollector(PacketCollector collector) {
        collectors.remove(collector);
    }

    /**
     * Get the collection of all packet collectors for this connection.
     * 
     * @return a collection of packet collectors for this connection.
     */
    protected Collection<PacketCollector> getPacketCollectors() {
        return collectors;
    }

    /**
     * Registers a packet listener with this connection. A packet listener will be invoked only
     * when an incoming packet is received. A packet filter determines
     * which packets will be delivered to the listener. If the same packet listener
     * is added again with a different filter, only the new filter will be used.
     * 
     * <p>
     * NOTE: If you want get a similar callback for outgoing packets, see {@link #addPacketInterceptor(PacketInterceptor, PacketFilter)}.
     * 
     * @param packetListener the packet listener to notify of new received packets.
     * @param packetFilter   the packet filter to use.
     */
    public void addPacketListener(PacketListener packetListener, PacketFilter packetFilter) {
        if (packetListener == null) {
            throw new NullPointerException("Packet listener is null.");
        }
        ListenerWrapper wrapper = new ListenerWrapper(packetListener, packetFilter);
        recvListeners.put(packetListener, wrapper);
    }

    /**
     * Removes a packet listener for received packets from this connection.
     * 
     * @param packetListener the packet listener to remove.
     */
    public void removePacketListener(PacketListener packetListener) {
        recvListeners.remove(packetListener);
    }

    /**
     * Get a map of all packet listeners for received packets of this connection.
     * 
     * @return a map of all packet listeners for received packets.
     */
    protected Map<PacketListener, ListenerWrapper> getPacketListeners() {
        return recvListeners;
    }

    /**
     * Registers a packet listener with this connection. The listener will be
     * notified of every packet that this connection sends. A packet filter determines
     * which packets will be delivered to the listener. Note that the thread
     * that writes packets will be used to invoke the listeners. Therefore, each
     * packet listener should complete all operations quickly or use a different
     * thread for processing.
     * 
     * @param packetListener the packet listener to notify of sent packets.
     * @param packetFilter   the packet filter to use.
     */
    public void addPacketSendingListener(PacketListener packetListener, PacketFilter packetFilter) {
        if (packetListener == null) {
            throw new NullPointerException("Packet listener is null.");
        }
        ListenerWrapper wrapper = new ListenerWrapper(packetListener, packetFilter);
        sendListeners.put(packetListener, wrapper);
    }

    /**
     * Removes a packet listener for sending packets from this connection.
     * 
     * @param packetListener the packet listener to remove.
     */
    public void removePacketSendingListener(PacketListener packetListener) {
        sendListeners.remove(packetListener);
    }

    /**
     * Get a map of all packet listeners for sending packets of this connection.
     * 
     * @return a map of all packet listeners for sent packets.
     */
    protected Map<PacketListener, ListenerWrapper> getPacketSendingListeners() {
        return sendListeners;
    }


    /**
     * Process all packet listeners for sending packets.
     * 
     * @param packet the packet to process.
     */
    private void firePacketSendingListeners(Packet packet) {
        // Notify the listeners of the new sent packet
        for (ListenerWrapper listenerWrapper : sendListeners.values()) {
            try {
                listenerWrapper.notifyListener(packet);
            }
            catch (NotConnectedException e) {
                LOGGER.log(Level.WARNING, "Got not connected exception, aborting");
                break;
            }
        }
    }

    /**
     * Registers a packet interceptor with this connection. The interceptor will be
     * invoked every time a packet is about to be sent by this connection. Interceptors
     * may modify the packet to be sent. A packet filter determines which packets
     * will be delivered to the interceptor.
     * 
     * <p>
     * NOTE: For a similar functionality on incoming packets, see {@link #addPacketListener(PacketListener, PacketFilter)}.
     *
     * @param packetInterceptor the packet interceptor to notify of packets about to be sent.
     * @param packetFilter      the packet filter to use.
     */
    public void addPacketInterceptor(PacketInterceptor packetInterceptor,
            PacketFilter packetFilter) {
        if (packetInterceptor == null) {
            throw new NullPointerException("Packet interceptor is null.");
        }
        interceptors.put(packetInterceptor, new InterceptorWrapper(packetInterceptor, packetFilter));
    }

    /**
     * Removes a packet interceptor.
     *
     * @param packetInterceptor the packet interceptor to remove.
     */
    public void removePacketInterceptor(PacketInterceptor packetInterceptor) {
        interceptors.remove(packetInterceptor);
    }

    /**
     * Get a map of all packet interceptors for sending packets of this connection.
     * 
     * @return a map of all packet interceptors for sending packets.
     */
    protected Map<PacketInterceptor, InterceptorWrapper> getPacketInterceptors() {
        return interceptors;
    }

    /**
     * Process interceptors. Interceptors may modify the packet that is about to be sent.
     * Since the thread that requested to send the packet will invoke all interceptors, it
     * is important that interceptors perform their work as soon as possible so that the
     * thread does not remain blocked for a long period.
     * 
     * @param packet the packet that is going to be sent to the server
     */
    private void firePacketInterceptors(Packet packet) {
        if (packet != null) {
            for (InterceptorWrapper interceptorWrapper : interceptors.values()) {
                interceptorWrapper.notifyListener(packet);
            }
        }
    }

    /**
     * Initialize the {@link #debugger}. You can specify a customized {@link SmackDebugger}
     * by setup the system property <code>smack.debuggerClass</code> to the implementation.
     * 
     * @throws IllegalStateException if the reader or writer isn't yet initialized.
     * @throws IllegalArgumentException if the SmackDebugger can't be loaded.
     */
    protected void initDebugger() {
        if (reader == null || writer == null) {
            throw new NullPointerException("Reader or writer isn't initialized.");
        }
        // If debugging is enabled, we open a window and write out all network traffic.
        if (config.isDebuggerEnabled()) {
            if (debugger == null) {
                // Detect the debugger class to use.
                String className = null;
                // Use try block since we may not have permission to get a system
                // property (for example, when an applet).
                try {
                    className = System.getProperty("smack.debuggerClass");
                }
                catch (Throwable t) {
                    // Ignore.
                }
                Class<?> debuggerClass = null;
                if (className != null) {
                    try {
                        debuggerClass = Class.forName(className);
                    }
                    catch (Exception e) {
                        LOGGER.warning("Unabled to instantiate debugger class " + className);
                    }
                }
                if (debuggerClass == null) {
                    try {
                        debuggerClass =
                                Class.forName("org.jivesoftware.smackx.debugger.EnhancedDebugger");
                    }
                    catch (Exception ex) {
                        try {
                            debuggerClass =
                                    Class.forName("org.jivesoftware.smack.debugger.LiteDebugger");
                        }
                        catch (Exception ex2) {
                            LOGGER.warning("Unabled to instantiate either Smack debugger class");
                        }
                    }
                }
                // Create a new debugger instance. If an exception occurs then disable the debugging
                // option
                try {
                    Constructor<?> constructor = debuggerClass
                            .getConstructor(XMPPConnection.class, Writer.class, Reader.class);
                    debugger = (SmackDebugger) constructor.newInstance(this, writer, reader);
                    reader = debugger.getReader();
                    writer = debugger.getWriter();
                }
                catch (Exception e) {
                    throw new IllegalArgumentException("Can't initialize the configured debugger!", e);
                }
            }
            else {
                // Obtain new reader and writer from the existing debugger
                reader = debugger.newConnectionReader(reader);
                writer = debugger.newConnectionWriter(writer);
            }
        }
    }

    /**
     * Set the servers Entity Caps node
     * 
     * XMPPConnection holds this information in order to avoid a dependency to
     * smackx where EntityCapsManager lives from smack.
     * 
     * @param node
     */
    protected void setServiceCapsNode(String node) {
        serviceCapsNode = node;
    }

    /**
     * Retrieve the servers Entity Caps node
     * 
     * XMPPConnection holds this information in order to avoid a dependency to
     * smackx where EntityCapsManager lives from smack.
     * 
     * @return the servers entity caps node
     */
    public String getServiceCapsNode() {
        return serviceCapsNode;
    }

    /**
     * Returns true if the server supports roster versioning as defined in XEP-0237.
     *
     * @return true if the server supports roster versioning
     */
    public boolean isRosterVersioningSupported() {
        return rosterVersioningSupported;
    }

    /**
     * Indicates that the server supports roster versioning as defined in XEP-0237.
     */
    protected void setRosterVersioningSupported() {
        rosterVersioningSupported = true;
    }

    /**
     * Returns the current value of the reply timeout in milliseconds for request for this
     * XMPPConnection instance.
     *
     * @return the packet reply timeout in milliseconds
     */
    public long getPacketReplyTimeout() {
        return packetReplyTimeout;
    }

    /**
     * Set the packet reply timeout in milliseconds. In most cases, Smack will throw a
     * {@link NoResponseException} if no reply to a request was received within the timeout period.
     *
     * @param timeout the packet reply timeout in milliseconds
     */
    public void setPacketReplyTimeout(long timeout) {
        packetReplyTimeout = timeout;
    }

    /**
     * Processes a packet after it's been fully parsed by looping through the installed
     * packet collectors and listeners and letting them examine the packet to see if
     * they are a match with the filter.
     *
     * @param packet the packet to process.
     */
    protected void processPacket(Packet packet) {
        if (packet == null) {
            return;
        }

        // Loop through all collectors and notify the appropriate ones.
        for (PacketCollector collector: getPacketCollectors()) {
            collector.processPacket(packet);
        }

        // Deliver the incoming packet to listeners.
        executorService.submit(new ListenerNotification(packet));
    }

    /**
     * A runnable to notify all listeners of a packet.
     */
    private class ListenerNotification implements Runnable {

        private Packet packet;

        public ListenerNotification(Packet packet) {
            this.packet = packet;
        }

        public void run() {
            for (ListenerWrapper listenerWrapper : recvListeners.values()) {
                try {
                    listenerWrapper.notifyListener(packet);
                } catch(NotConnectedException e) {
                    LOGGER.log(Level.WARNING, "Got not connected exception, aborting", e);
                    break;
                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Exception in packet listener", e);
                }
            }
        }
    }

    /**
     * Sets whether the connection has already logged in the server. This method assures that the
     * {@link #wasAuthenticated} flag is never reset once it has ever been set.
     * 
     * @param authenticated true if the connection has already been authenticated.
     */
    protected void setWasAuthenticated(boolean authenticated) {
        // Never reset the flag if the connection has ever been authenticated
        if (!wasAuthenticated) {
            wasAuthenticated = authenticated;
        }
    }

    protected void callConnectionConnectedListener() {
        for (ConnectionListener listener : getConnectionListeners()) {
            listener.connected(this);
        }
    }

    protected void callConnectionAuthenticatedListener() {
        for (ConnectionListener listener : getConnectionListeners()) {
            listener.authenticated(this);
        }
    }

    void callConnectionClosedListener() {
        for (ConnectionListener listener : getConnectionListeners()) {
            try {
                listener.connectionClosed();
            }
            catch (Exception e) {
                // Catch and print any exception so we can recover
                // from a faulty listener and finish the shutdown process
                LOGGER.log(Level.SEVERE, "Error in listener while closing connection", e);
            }
        }
    }

    protected void callConnectionClosedOnErrorListener(Exception e) {
        LOGGER.log(Level.WARNING, "Connection closed with error", e);
        for (ConnectionListener listener : getConnectionListeners()) {
            try {
                listener.connectionClosedOnError(e);
            }
            catch (Exception e2) {
                // Catch and print any exception so we can recover
                // from a faulty listener
                LOGGER.log(Level.SEVERE, "Error in listener while closing connection", e2);
            }
        }
    }

    /**
     * A wrapper class to associate a packet filter with a listener.
     */
    protected static class ListenerWrapper {

        private PacketListener packetListener;
        private PacketFilter packetFilter;

        /**
         * Create a class which associates a packet filter with a listener.
         * 
         * @param packetListener the packet listener.
         * @param packetFilter the associated filter or null if it listen for all packets.
         */
        public ListenerWrapper(PacketListener packetListener, PacketFilter packetFilter) {
            this.packetListener = packetListener;
            this.packetFilter = packetFilter;
        }

        /**
         * Notify and process the packet listener if the filter matches the packet.
         * 
         * @param packet the packet which was sent or received.
         * @throws NotConnectedException 
         */
        public void notifyListener(Packet packet) throws NotConnectedException {
            if (packetFilter == null || packetFilter.accept(packet)) {
                packetListener.processPacket(packet);
            }
        }
    }

    /**
     * A wrapper class to associate a packet filter with an interceptor.
     */
    protected static class InterceptorWrapper {

        private PacketInterceptor packetInterceptor;
        private PacketFilter packetFilter;

        /**
         * Create a class which associates a packet filter with an interceptor.
         * 
         * @param packetInterceptor the interceptor.
         * @param packetFilter the associated filter or null if it intercepts all packets.
         */
        public InterceptorWrapper(PacketInterceptor packetInterceptor, PacketFilter packetFilter) {
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

        /**
         * Notify and process the packet interceptor if the filter matches the packet.
         * 
         * @param packet the packet which will be sent.
         */
        public void notifyListener(Packet packet) {
            if (packetFilter == null || packetFilter.accept(packet)) {
                packetInterceptor.interceptPacket(packet);
            }
        }
    }

    protected ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        return executorService.schedule(command, delay, unit);
    }

    /**
     * Get the connection counter of this XMPPConnection instance. Those can be used as ID to
     * identify the connection, but beware that the ID may not be unique if you create more then
     * <tt>2*Integer.MAX_VALUE</tt> instances as the counter could wrap.
     *
     * @return the connection counter of this XMPPConnection
     */
    public int getConnectionCounter() {
        return connectionCounterValue;
    }

    public static enum FromMode {
        /**
         * Leave the 'from' attribute unchanged. This is the behavior of Smack < 4.0
         */
        UNCHANGED,
        /**
         * Omit the 'from' attribute. According to RFC 6120 8.1.2.1 1. XMPP servers "MUST (...)
         * override the 'from' attribute specified by the client". It is therefore safe to specify
         * FromMode.OMITTED here.
         */
        OMITTED,
        /**
         * Set the from to the clients full JID. This is usually not required.
         */
        USER
    }

    /**
     * Set the FromMode for this connection instance. Defines how the 'from' attribute of outgoing
     * stanzas should be populated by Smack.
     * 
     * @param fromMode
     */
    public void setFromMode(FromMode fromMode) {
        this.fromMode = fromMode;
    }

    /**
     * Get the currently active FromMode.
     *
     * @return the currently active {@link FromMode}
     */
    public FromMode getFromMode() {
        return this.fromMode;
    }

    @Override
    protected void finalize() throws Throwable {
        try {
            // It's usually not a good idea to rely on finalize. But this is the easiest way to
            // avoid the "Smack Listener Processor" leaking. The thread(s) of the executor have a
            // reference to their ExecutorService which prevents the ExecutorService from being
            // gc'ed. It is possible that the XMPPConnection instance is gc'ed while the
            // listenerExecutor ExecutorService call not be gc'ed until it got shut down.
            executorService.shutdownNow();
        }
        finally {
            super.finalize();
        }
    }
}
