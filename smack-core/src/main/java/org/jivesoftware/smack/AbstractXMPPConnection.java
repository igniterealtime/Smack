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
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.SmackException.AlreadyConnectedException;
import org.jivesoftware.smack.SmackException.AlreadyLoggedInException;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.SmackException.ConnectionException;
import org.jivesoftware.smack.SmackException.ResourceBindingNotOfferedException;
import org.jivesoftware.smack.SmackException.SecurityRequiredException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.compress.packet.Compress;
import org.jivesoftware.smack.compression.XMPPInputOutputStream;
import org.jivesoftware.smack.debugger.SmackDebugger;
import org.jivesoftware.smack.filter.IQReplyFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketIDFilter;
import org.jivesoftware.smack.packet.Bind;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Mechanisms;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.PacketExtension;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.RosterVer;
import org.jivesoftware.smack.packet.Session;
import org.jivesoftware.smack.packet.StartTls;
import org.jivesoftware.smack.packet.PlainStreamElement;
import org.jivesoftware.smack.parsing.ParsingExceptionCallback;
import org.jivesoftware.smack.parsing.UnparsablePacket;
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.rosterstore.RosterStore;
import org.jivesoftware.smack.util.DNSUtil;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smack.util.ParserUtils;
import org.jivesoftware.smack.util.SmackExecutorThreadFactory;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.dns.HostAddress;
import org.jxmpp.util.XmppStringUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;


public abstract class AbstractXMPPConnection implements XMPPConnection {
    private static final Logger LOGGER = Logger.getLogger(AbstractXMPPConnection.class.getName());

    /** 
     * Counter to uniquely identify connections that are created.
     */
    private final static AtomicInteger connectionCounter = new AtomicInteger(0);

    static {
        // Ensure the SmackConfiguration class is loaded by calling a method in it.
        SmackConfiguration.getVersion();
    }

    /**
     * Get the collection of listeners that are interested in connection creation events.
     * 
     * @return a collection of listeners interested on new connections.
     */
    protected static Collection<ConnectionCreationListener> getConnectionCreationListeners() {
        return XMPPConnectionRegistry.getConnectionCreationListeners();
    }
 
    /**
     * A collection of ConnectionListeners which listen for connection closing
     * and reconnection events.
     */
    protected final Set<ConnectionListener> connectionListeners =
            new CopyOnWriteArraySet<ConnectionListener>();

    /**
     * A collection of PacketCollectors which collects packets for a specified filter
     * and perform blocking and polling operations on the result queue.
     * <p>
     * We use a ConcurrentLinkedQueue here, because its Iterator is weakly
     * consistent and we want {@link #invokePacketCollectors(Packet)} for-each
     * loop to be lock free. As drawback, removing a PacketCollector is O(n).
     * The alternative would be a synchronized HashSet, but this would mean a
     * synchronized block around every usage of <code>collectors</code>.
     * </p>
     */
    private final Collection<PacketCollector> collectors = new ConcurrentLinkedQueue<PacketCollector>();

    /**
     * List of PacketListeners that will be notified synchronously when a new packet was received.
     */
    private final Map<PacketListener, ListenerWrapper> syncRecvListeners = new LinkedHashMap<>();

    /**
     * List of PacketListeners that will be notified asynchronously when a new packet was received.
     */
    private final Map<PacketListener, ListenerWrapper> asyncRecvListeners = new LinkedHashMap<>();

    /**
     * List of PacketListeners that will be notified when a new packet was sent.
     */
    private final Map<PacketListener, ListenerWrapper> sendListeners =
            new HashMap<PacketListener, ListenerWrapper>();

    /**
     * List of PacketListeners that will be notified when a new packet is about to be
     * sent to the server. These interceptors may modify the packet before it is being
     * actually sent to the server.
     */
    private final Map<PacketListener, InterceptorWrapper> interceptors =
            new HashMap<PacketListener, InterceptorWrapper>();

    protected final Lock connectionLock = new ReentrantLock();

    protected final Map<String, PacketExtension> streamFeatures = new HashMap<String, PacketExtension>();

    /**
     * The full JID of the authenticated user.
     */
    protected String user;

    protected boolean connected = false;

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
     * Set to success if the last features stanza from the server has been parsed. A XMPP connection
     * handshake can invoke multiple features stanzas, e.g. when TLS is activated a second feature
     * stanza is send by the server. This is set to true once the last feature stanza has been
     * parsed.
     */
    protected final SynchronizationPoint<Exception> lastFeaturesReceived = new SynchronizationPoint<Exception>(
                    AbstractXMPPConnection.this);

    /**
     * Set to success if the sasl feature has been received.
     */
    protected final SynchronizationPoint<SmackException> saslFeatureReceived = new SynchronizationPoint<SmackException>(
                    AbstractXMPPConnection.this);
 
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
     * Defines how the from attribute of outgoing stanzas should be handled.
     */
    private FromMode fromMode = FromMode.OMITTED;

    protected XMPPInputOutputStream compressionHandler;

    private ParsingExceptionCallback parsingExceptionCallback = SmackConfiguration.getDefaultParsingExceptionCallback();

    /**
     * ExecutorService used to invoke the PacketListeners on newly arrived and parsed stanzas. It is
     * important that we use a <b>single threaded ExecutorService</b> in order to guarantee that the
     * PacketListeners are invoked in the same order the stanzas arrived.
     */
    private final ThreadPoolExecutor executorService = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS,
                    new ArrayBlockingQueue<Runnable>(100), new SmackExecutorThreadFactory(connectionCounterValue, "IncomingNotifier"));

    /**
     * Creates an executor service just as {@link Executors#newCachedThreadPool()} would do, but with a keep alive time
     * of 2 minutes instead of 60 seconds. And a custom thread factory to set meaningful names on the threads and set
     * them 'daemon'.
     */
    private final ExecutorService cachedExecutorService = new ThreadPoolExecutor(
                    // @formatter:off
                    0,                                 // corePoolSize
                    Integer.MAX_VALUE,                 // maximumPoolSize
                    60L * 2,                           // keepAliveTime
                    TimeUnit.SECONDS,                  // keepAliveTime unit, note that MINUTES is Android API 9
                    new SynchronousQueue<Runnable>(),  // workQueue
                    new SmackExecutorThreadFactory(    // threadFactory
                                    connectionCounterValue,
                                    "CachedExecutor"
                                    )
                    // @formatter:on
                    );

    /**
     * A executor service used to invoke the callbacks of synchronous packet listeners. We use a executor service to
     * decouple incoming stanza processing from callback invocation. It is important that order of callback invocation
     * is the same as the order of the incoming stanzas. Therefore we use a <i>single</i> threaded executor service.
     */
    private final ExecutorService singleThreadedExecutorService = Executors.newSingleThreadExecutor(new SmackExecutorThreadFactory(
                    getConnectionCounter(), "Sync PacketListener Callback"));

    private Roster roster;

    /**
     * The used host to establish the connection to
     */
    protected String host;

    /**
     * The used port to establish the connection to
     */
    protected int port;

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
    protected AbstractXMPPConnection(ConnectionConfiguration configuration) {
        config = configuration;
    }

    protected ConnectionConfiguration getConfiguration() {
        return config;
    }

    @Override
    public String getServiceName() {
        if (serviceName != null) {
            return serviceName;
        }
        return config.getServiceName();
    }

    @Override
    public String getHost() {
        return host;
    }

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public abstract String getConnectionID();

    @Override
    public abstract boolean isSecureConnection();

    protected abstract void sendPacketInternal(Packet packet) throws NotConnectedException;

    @Override
    public abstract void send(PlainStreamElement element) throws NotConnectedException;

    @Override
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
        throwAlreadyConnectedExceptionIfAppropriate();
        saslAuthentication.init();
        saslFeatureReceived.init();
        lastFeaturesReceived.init();
        connectInternal();
    }

    /**
     * Abstract method that concrete subclasses of XMPPConnection need to implement to perform their
     * way of XMPP connection establishment. Implementations are required to perform an automatic
     * login if the previous connection state was logged (authenticated).
     * 
     * @throws SmackException
     * @throws IOException
     * @throws XMPPException
     */
    protected abstract void connectInternal() throws SmackException, IOException, XMPPException;

    private String usedUsername, usedPassword, usedResource;

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
     * {@link ConnectionConfiguration.Builder#setSendPresence(boolean)}. If this connection is
     * not interested in loading its roster upon login then use
     * {@link ConnectionConfiguration.Builder#setRosterLoadedAtLogin(boolean)}.
     * Finally, if you want to not pass a password and instead use a more advanced mechanism
     * while using SASL then you may be interested in using
     * {@link ConnectionConfiguration.Builder#setCallbackHandler(javax.security.auth.callback.CallbackHandler)}.
     * For more advanced login settings see {@link ConnectionConfiguration}.
     * 
     * @throws XMPPException if an error occurs on the XMPP protocol level.
     * @throws SmackException if an error occurs somehwere else besides XMPP protocol level.
     * @throws IOException 
     */
    public void login() throws XMPPException, SmackException, IOException {
        throwNotConnectedExceptionIfAppropriate();
        throwAlreadyLoggedInExceptionIfAppropriate();
        if (isAnonymous()) {
            loginAnonymously();
        } else {
            // The previously used username, password and resource take over precedence over the
            // ones from the connection configuration
            String username = usedUsername != null ? usedUsername : config.getUsername();
            String password = usedPassword != null ? usedPassword : config.getPassword();
            String resource = usedResource != null ? usedResource : config.getResource();
            login(username, password, resource);
        }
    }

    /**
     * Same as {@link #login(String, String, String)}, but takes the resource from the connection
     * configuration.
     * 
     * @param username
     * @param password
     * @throws XMPPException
     * @throws SmackException
     * @throws IOException
     * @see #login
     */
    public void login(String username, String password) throws XMPPException, SmackException,
                    IOException {
        login(username, password, config.getResource());
    }

    /**
     * Login with the given username. You may omit the password if a callback handler is used. If
     * resource is null, then the server will generate one.
     * 
     * @param username
     * @param password
     * @param resource
     * @throws XMPPException
     * @throws SmackException
     * @throws IOException
     * @see #login
     */
    public void login(String username, String password, String resource) throws XMPPException,
                    SmackException, IOException {
        if (StringUtils.isNullOrEmpty(username)) {
            throw new IllegalArgumentException("Username must not be null or empty");
        }
        usedUsername = username;
        usedPassword = password;
        usedResource = resource;
        loginNonAnonymously(username, password, resource);
    }

    protected abstract void loginNonAnonymously(String username, String password, String resource)
                    throws XMPPException, SmackException, IOException;

    protected abstract void loginAnonymously() throws XMPPException, SmackException, IOException;

    @Override
    public final boolean isConnected() {
        return connected;
    }

    @Override
    public final boolean isAuthenticated() {
        return authenticated;
    }

    @Override
    public final String getUser() {
        return user;
    }

    protected void bindResourceAndEstablishSession(String resource) throws XMPPErrorException,
                    IOException, SmackException {

        // Wait until either:
        // - the servers last features stanza has been parsed
        // - the timeout occurs
        LOGGER.finer("Waiting for last features to be received before continuing with resource binding");
        lastFeaturesReceived.checkIfSuccessOrWait();


        if (!hasFeature(Bind.ELEMENT, Bind.NAMESPACE)) {
            // Server never offered resource binding, which is REQURIED in XMPP client and
            // server implementations as per RFC6120 7.2
            throw new ResourceBindingNotOfferedException();
        }

        // Resource binding, see RFC6120 7.
        // Note that we can not use IQReplyFilter here, since the users full JID is not yet
        // available. It will become available right after the resource has been successfully bound.
        Bind bindResource = Bind.newSet(resource);
        PacketCollector packetCollector = createPacketCollectorAndSend(new PacketIDFilter(bindResource), bindResource);
        Bind response = packetCollector.nextResultOrThrow();
        user = response.getJid();
        serviceName = XmppStringUtils.parseDomain(user);

        Session.Feature sessionFeature = getFeature(Session.ELEMENT, Session.NAMESPACE);
        // Only bind the session if it's announced as stream feature by the server, is not optional and not disabled
        // For more information see http://tools.ietf.org/html/draft-cridland-xmpp-session-01
        if (sessionFeature != null && !sessionFeature.isOptional() && !getConfiguration().isLegacySessionDisabled()) {
            Session session = new Session();
            packetCollector = createPacketCollectorAndSend(new PacketIDFilter(session), session);
            packetCollector.nextResultOrThrow();
        }
    }

    protected void afterSuccessfulLogin(final boolean resumed) throws NotConnectedException {
        // Indicate that we're now authenticated.
        this.authenticated = true;

        // If debugging is enabled, change the the debug window title to include the
        // name we are now logged-in as.
        // If DEBUG_ENABLED was set to true AFTER the connection was created the debugger
        // will be null
        if (config.isDebuggerEnabled() && debugger != null) {
            debugger.userHasLogged(user);
        }
        callConnectionAuthenticatedListener();

        // Set presence to online. It is important that this is done after
        // callConnectionAuthenticatedListener(), as this call will also
        // eventually load the roster. And we should load the roster before we
        // send the initial presence.
        if (config.isSendPresence() && !resumed) {
            if (!isAnonymous()) {
                // Make sure that the roster has setup its listeners prior sending the initial presence by calling
                // getRoster()
                getRoster();
            }
            sendPacket(new Presence(Presence.Type.available));
        }
    }

    @Override
    public final boolean isAnonymous() {
        return config.getUsername() == null && usedUsername == null;
    }

    private String serviceName;

    protected List<HostAddress> hostAddresses;

    protected void populateHostAddresses() throws Exception {
        // N.B.: Important to use config.serviceName and not AbstractXMPPConnection.serviceName
        if (config.host != null) {
            hostAddresses = new ArrayList<HostAddress>(1);
            HostAddress hostAddress;
            hostAddress = new HostAddress(config.host, config.port);
            hostAddresses.add(hostAddress);
        } else {
            hostAddresses = DNSUtil.resolveXMPPDomain(config.serviceName);
        }
    }

    protected Lock getConnectionLock() {
        return connectionLock;
    }

    protected void throwNotConnectedExceptionIfAppropriate() throws NotConnectedException {
        if (!isConnected()) {
            throw new NotConnectedException();
        }
    }

    protected void throwAlreadyConnectedExceptionIfAppropriate() throws AlreadyConnectedException {
        if (isConnected()) {
            throw new AlreadyConnectedException();
        }
    }

    protected void throwAlreadyLoggedInExceptionIfAppropriate() throws AlreadyLoggedInException {
        if (isAuthenticated()) {
            throw new AlreadyLoggedInException();
        }
    }

    @Override
    public void sendPacket(Packet packet) throws NotConnectedException {
        if (packet == null) {
            throw new IllegalArgumentException("Packet must not be null");
        }
        throwNotConnectedExceptionIfAppropriate();
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
    }

    @Override
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
        // Also only check for rosterIsLoaded is isRosterLoadedAtLogin is set, otherwise the user
        // has to manually call Roster.reload() before he can expect a initialized roster.
        if (!roster.isLoaded() && config.isRosterLoadedAtLogin()) {
            roster.waitUntilLoaded();
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
            throw new NotConnectedException();
        }

        sendPacket(unavailablePresence);
        shutdown();
        callConnectionClosedListener();
    }

    /**
     * Shuts the current connection down.
     */
    protected abstract void shutdown();

    @Override
    public void addConnectionListener(ConnectionListener connectionListener) {
        if (connectionListener == null) {
            return;
        }
        connectionListeners.add(connectionListener);
    }

    @Override
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

    @Override
    public PacketCollector createPacketCollectorAndSend(IQ packet) throws NotConnectedException {
        PacketFilter packetFilter = new IQReplyFilter(packet, this);
        // Create the packet collector before sending the packet
        PacketCollector packetCollector = createPacketCollectorAndSend(packetFilter, packet);
        return packetCollector;
    }

    @Override
    public PacketCollector createPacketCollectorAndSend(PacketFilter packetFilter, Packet packet)
                    throws NotConnectedException {
        // Create the packet collector before sending the packet
        PacketCollector packetCollector = createPacketCollector(packetFilter);
        try {
            // Now we can send the packet as the collector has been created
            sendPacket(packet);
        }
        catch (NotConnectedException | RuntimeException e) {
            packetCollector.cancel();
            throw e;
        }
        return packetCollector;
    }

    @Override
    public PacketCollector createPacketCollector(PacketFilter packetFilter) {
        PacketCollector collector = new PacketCollector(this, packetFilter);
        // Add the collector to the list of active collectors.
        collectors.add(collector);
        return collector;
    }

    @Override
    public void removePacketCollector(PacketCollector collector) {
        collectors.remove(collector);
    }

    @Override
    public void addPacketListener(PacketListener packetListener, PacketFilter packetFilter) {
        addAsyncPacketListener(packetListener, packetFilter);
    }

    @Override
    public boolean removePacketListener(PacketListener packetListener) {
        return removeAsyncPacketListener(packetListener);
    }

    @Override
    public void addSyncPacketListener(PacketListener packetListener, PacketFilter packetFilter) {
        if (packetListener == null) {
            throw new NullPointerException("Packet listener is null.");
        }
        ListenerWrapper wrapper = new ListenerWrapper(packetListener, packetFilter);
        synchronized (syncRecvListeners) {
            syncRecvListeners.put(packetListener, wrapper);
        }
    }

    @Override
    public boolean removeSyncPacketListener(PacketListener packetListener) {
        synchronized (syncRecvListeners) {
            return syncRecvListeners.remove(packetListener) != null;
        }
    }

    @Override
    public void addAsyncPacketListener(PacketListener packetListener, PacketFilter packetFilter) {
        if (packetListener == null) {
            throw new NullPointerException("Packet listener is null.");
        }
        ListenerWrapper wrapper = new ListenerWrapper(packetListener, packetFilter);
        synchronized (asyncRecvListeners) {
            asyncRecvListeners.put(packetListener, wrapper);
        }
    }

    @Override
    public boolean removeAsyncPacketListener(PacketListener packetListener) {
        synchronized (asyncRecvListeners) {
            return asyncRecvListeners.remove(packetListener) != null;
        }
    }

    @Override
    public void addPacketSendingListener(PacketListener packetListener, PacketFilter packetFilter) {
        if (packetListener == null) {
            throw new NullPointerException("Packet listener is null.");
        }
        ListenerWrapper wrapper = new ListenerWrapper(packetListener, packetFilter);
        synchronized (sendListeners) {
            sendListeners.put(packetListener, wrapper);
        }
    }

    @Override
    public void removePacketSendingListener(PacketListener packetListener) {
        synchronized (sendListeners) {
            sendListeners.remove(packetListener);
        }
    }

    /**
     * Process all packet listeners for sending packets.
     * <p>
     * Compared to {@link #firePacketInterceptors(Packet)}, the listeners will be invoked in a new thread.
     * </p>
     * 
     * @param packet the packet to process.
     */
    @SuppressWarnings("javadoc")
    protected void firePacketSendingListeners(final Packet packet) {
        final List<PacketListener> listenersToNotify = new LinkedList<PacketListener>();
        synchronized (sendListeners) {
            for (ListenerWrapper listenerWrapper : sendListeners.values()) {
                if (listenerWrapper.filterMatches(packet)) {
                    listenersToNotify.add(listenerWrapper.getListener());
                }
            }
        }
        if (listenersToNotify.isEmpty()) {
            return;
        }
        // Notify in a new thread, because we can
        asyncGo(new Runnable() {
            @Override
            public void run() {
                for (PacketListener listener : listenersToNotify) {
                    try {
                        listener.processPacket(packet);
                    }
                    catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Sending listener threw exception", e);
                        continue;
                    }
                }
            }});
    }

    @Override
    public void addPacketInterceptor(PacketListener packetInterceptor,
            PacketFilter packetFilter) {
        if (packetInterceptor == null) {
            throw new NullPointerException("Packet interceptor is null.");
        }
        InterceptorWrapper interceptorWrapper = new InterceptorWrapper(packetInterceptor, packetFilter);
        synchronized (interceptors) {
            interceptors.put(packetInterceptor, interceptorWrapper);
        }
    }

    @Override
    public void removePacketInterceptor(PacketListener packetInterceptor) {
        synchronized (interceptors) {
            interceptors.remove(packetInterceptor);
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
    private void firePacketInterceptors(Packet packet) {
        List<PacketListener> interceptorsToInvoke = new LinkedList<PacketListener>();
        synchronized (interceptors) {
            for (InterceptorWrapper interceptorWrapper : interceptors.values()) {
                if (interceptorWrapper.filterMatches(packet)) {
                    interceptorsToInvoke.add(interceptorWrapper.getInterceptor());
                }
            }
        }
        for (PacketListener interceptor : interceptorsToInvoke) {
            try {
                interceptor.processPacket(packet);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Packet interceptor threw exception", e);
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
                debugger = SmackConfiguration.createDebugger(this, writer, reader);
            }

            if (debugger == null) {
                LOGGER.severe("Debugging enabled but could not find debugger class");
            } else {
                // Obtain new reader and writer from the existing debugger
                reader = debugger.newConnectionReader(reader);
                writer = debugger.newConnectionWriter(writer);
            }
        }
    }

    @Override
    public long getPacketReplyTimeout() {
        return packetReplyTimeout;
    }

    @Override
    public void setPacketReplyTimeout(long timeout) {
        packetReplyTimeout = timeout;
    }

    protected void parseAndProcessStanza(XmlPullParser parser) throws Exception {
        ParserUtils.assertAtStartTag(parser);
        int parserDepth = parser.getDepth();
        Packet stanza = null;
        try {
            stanza = PacketParserUtils.parseStanza(parser, this);
        }
        catch (Exception e) {
            CharSequence content = PacketParserUtils.parseContentDepth(parser,
                            parserDepth);
            UnparsablePacket message = new UnparsablePacket(content, e);
            ParsingExceptionCallback callback = getParsingExceptionCallback();
            if (callback != null) {
                callback.handleUnparsablePacket(message);
            }
        }
        ParserUtils.assertAtEndTag(parser);
        if (stanza != null) {
            processPacket(stanza);
        }
    }

    /**
     * Processes a packet after it's been fully parsed by looping through the installed
     * packet collectors and listeners and letting them examine the packet to see if
     * they are a match with the filter.
     *
     * @param packet the packet to process.
     */
    protected void processPacket(Packet packet) {
        assert(packet != null);
        lastStanzaReceived = System.currentTimeMillis();
        // Deliver the incoming packet to listeners.
        executorService.submit(new ListenerNotification(packet));
    }

    /**
     * A runnable to notify all listeners and packet collectors of a packet.
     */
    private class ListenerNotification implements Runnable {

        private final Packet packet;

        public ListenerNotification(Packet packet) {
            this.packet = packet;
        }

        public void run() {
            invokePacketCollectorsAndNotifyRecvListeners(packet);
        }
    }


    /**
     * Invoke {@link PacketCollector#processPacket(Packet)} for every
     * PacketCollector with the given packet. Also notify the receive listeners with a matching packet filter about the packet.
     *
     * @param packet the packet to notify the PacketCollectors and receive listeners about.
     */
    protected void invokePacketCollectorsAndNotifyRecvListeners(final Packet packet) {
        // First handle the async recv listeners. Note that this code is very similar to what follows a few lines below,
        // the only difference is that asyncRecvListeners is used here and that the packet listeners are started in
        // their own thread.
        final Collection<PacketListener> listenersToNotify = new LinkedList<PacketListener>();
        synchronized (asyncRecvListeners) {
            for (ListenerWrapper listenerWrapper : asyncRecvListeners.values()) {
                if (listenerWrapper.filterMatches(packet)) {
                    listenersToNotify.add(listenerWrapper.getListener());
                }
            }
        }

        for (final PacketListener listener : listenersToNotify) {
            asyncGo(new Runnable() {
                @Override
                public void run() {
                    try {
                        listener.processPacket(packet);
                    } catch (Exception e) {
                        LOGGER.log(Level.SEVERE, "Exception in async packet listener", e);
                    }
                }
            });
        }

        // Loop through all collectors and notify the appropriate ones.
        for (PacketCollector collector: collectors) {
            collector.processPacket(packet);
        }

        // Notify the receive listeners interested in the packet
        listenersToNotify.clear();
        synchronized (syncRecvListeners) {
            for (ListenerWrapper listenerWrapper : syncRecvListeners.values()) {
                if (listenerWrapper.filterMatches(packet)) {
                    listenersToNotify.add(listenerWrapper.getListener());
                }
            }
        }

        // Decouple incoming stanza processing from listener invocation. Unlike async listeners, this uses a single
        // threaded executor service and therefore keeps the order.
        singleThreadedExecutorService.execute(new Runnable() {
            @Override
            public void run() {
                for (PacketListener listener : listenersToNotify) {
                    try {
                        listener.processPacket(packet);
                    } catch(NotConnectedException e) {
                        LOGGER.log(Level.WARNING, "Got not connected exception, aborting", e);
                        break;
                    } catch (Exception e) {
                        LOGGER.log(Level.SEVERE, "Exception in packet listener", e);
                    }
                }
            }
        });

    }

    /**
     * Sets whether the connection has already logged in the server. This method assures that the
     * {@link #wasAuthenticated} flag is never reset once it has ever been set.
     * 
     */
    protected void setWasAuthenticated() {
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

        private final PacketListener packetListener;
        private final PacketFilter packetFilter;

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

        public boolean filterMatches(Packet packet) {
            return packetFilter == null || packetFilter.accept(packet);
        }

        public PacketListener getListener() {
            return packetListener;
        }
    }

    /**
     * A wrapper class to associate a packet filter with an interceptor.
     */
    protected static class InterceptorWrapper {

        private final PacketListener packetInterceptor;
        private final PacketFilter packetFilter;

        /**
         * Create a class which associates a packet filter with an interceptor.
         * 
         * @param packetInterceptor the interceptor.
         * @param packetFilter the associated filter or null if it intercepts all packets.
         */
        public InterceptorWrapper(PacketListener packetInterceptor, PacketFilter packetFilter) {
            this.packetInterceptor = packetInterceptor;
            this.packetFilter = packetFilter;
        }

        public boolean filterMatches(Packet packet) {
            return packetFilter == null || packetFilter.accept(packet);
        }

        public PacketListener getInterceptor() {
            return packetInterceptor;
        }
    }

    @Override
    public int getConnectionCounter() {
        return connectionCounterValue;
    }

    @Override
    public void setFromMode(FromMode fromMode) {
        this.fromMode = fromMode;
    }

    @Override
    public FromMode getFromMode() {
        return this.fromMode;
    }

    @Override
    protected void finalize() throws Throwable {
        LOGGER.fine("finalizing XMPPConnection ( " + getConnectionCounter()
                        + "): Shutting down executor services");
        try {
            // It's usually not a good idea to rely on finalize. But this is the easiest way to
            // avoid the "Smack Listener Processor" leaking. The thread(s) of the executor have a
            // reference to their ExecutorService which prevents the ExecutorService from being
            // gc'ed. It is possible that the XMPPConnection instance is gc'ed while the
            // listenerExecutor ExecutorService call not be gc'ed until it got shut down.
            executorService.shutdownNow();
            cachedExecutorService.shutdown();
            removeCallbacksService.shutdownNow();
            singleThreadedExecutorService.shutdownNow();
        } catch (Throwable t) {
            LOGGER.log(Level.WARNING, "finalize() threw trhowable", t);
        }
        finally {
            super.finalize();
        }
    }

    @Override
    public RosterStore getRosterStore() {
        return config.getRosterStore();
    }

    @Override
    public boolean isRosterLoadedAtLogin() {
        return config.isRosterLoadedAtLogin();
    }

    protected final void parseFeatures(XmlPullParser parser) throws XmlPullParserException,
                    IOException, SmackException {
        streamFeatures.clear();
        final int initialDepth = parser.getDepth();
        while (true) {
            int eventType = parser.next();

            if (eventType == XmlPullParser.START_TAG && parser.getDepth() == initialDepth + 1) {
                PacketExtension streamFeature = null;
                String name = parser.getName();
                String namespace = parser.getNamespace();
                switch (name) {
                case StartTls.ELEMENT:
                    streamFeature = PacketParserUtils.parseStartTlsFeature(parser);
                    break;
                case Mechanisms.ELEMENT:
                    streamFeature = new Mechanisms(PacketParserUtils.parseMechanisms(parser));
                    break;
                case Bind.ELEMENT:
                    streamFeature = Bind.Feature.INSTANCE;
                    break;
                case Session.ELEMENT:
                    streamFeature = PacketParserUtils.parseSessionFeature(parser);
                    break;
                case RosterVer.ELEMENT:
                    if(namespace.equals(RosterVer.NAMESPACE)) {
                        streamFeature = RosterVer.INSTANCE;
                    }
                    else {
                        LOGGER.severe("Unknown Roster Versioning Namespace: "
                                        + namespace
                                        + ". Roster versioning not enabled");
                    }
                    break;
                case Compress.Feature.ELEMENT:
                    streamFeature = PacketParserUtils.parseCompressionFeature(parser);
                    break;
                default:
                    PacketExtensionProvider<PacketExtension> provider = ProviderManager.getStreamFeatureProvider(name, namespace);
                    if (provider != null) {
                        streamFeature = provider.parse(parser);
                    }
                    break;
                }
                if (streamFeature != null) {
                    addStreamFeature(streamFeature);
                }
            }
            else if (eventType == XmlPullParser.END_TAG && parser.getDepth() == initialDepth) {
                break;
            }
        }

        if (hasFeature(Mechanisms.ELEMENT, Mechanisms.NAMESPACE)) {
            // Only proceed with SASL auth if TLS is disabled or if the server doesn't announce it
            if (!hasFeature(StartTls.ELEMENT, StartTls.NAMESPACE)
                            || config.getSecurityMode() == SecurityMode.disabled) {
                saslFeatureReceived.reportSuccess();
            }
        }

        // If the server reported the bind feature then we are that that we did SASL and maybe
        // STARTTLS. We can then report that the last 'stream:features' have been parsed
        if (hasFeature(Bind.ELEMENT, Bind.NAMESPACE)) {
            if (!hasFeature(Compress.Feature.ELEMENT, Compress.NAMESPACE)
                            || !config.isCompressionEnabled()) {
                // This was was last features from the server is either it did not contain
                // compression or if we disabled it
                lastFeaturesReceived.reportSuccess();
            }
        }
        afterFeaturesReceived();
    }

    protected void afterFeaturesReceived() throws SecurityRequiredException, NotConnectedException {
        // Default implementation does nothing
    }

    @SuppressWarnings("unchecked")
    @Override
    public <F extends PacketExtension> F getFeature(String element, String namespace) {
        return (F) streamFeatures.get(XmppStringUtils.generateKey(element, namespace));
    }

    @Override
    public boolean hasFeature(String element, String namespace) {
        return getFeature(element, namespace) != null;
    }

    private void addStreamFeature(PacketExtension feature) {
        String key = XmppStringUtils.generateKey(feature.getElementName(), feature.getNamespace());
        streamFeatures.put(key, feature);
    }

    private final ScheduledExecutorService removeCallbacksService = new ScheduledThreadPoolExecutor(1,
                    new SmackExecutorThreadFactory(connectionCounterValue, "RemoveCallbacks"));

    @Override
    public void sendStanzaWithResponseCallback(Packet stanza, PacketFilter replyFilter,
                    PacketListener callback) throws NotConnectedException {
        sendStanzaWithResponseCallback(stanza, replyFilter, callback, null);
    }

    @Override
    public void sendStanzaWithResponseCallback(Packet stanza, PacketFilter replyFilter,
                    PacketListener callback, ExceptionCallback exceptionCallback)
                    throws NotConnectedException {
        sendStanzaWithResponseCallback(stanza, replyFilter, callback, exceptionCallback,
                        getPacketReplyTimeout());
    }

    @Override
    public void sendStanzaWithResponseCallback(Packet stanza, PacketFilter replyFilter,
                    final PacketListener callback, final ExceptionCallback exceptionCallback,
                    long timeout) throws NotConnectedException {
        if (stanza == null) {
            throw new IllegalArgumentException("stanza must not be null");
        }
        if (replyFilter == null) {
            // While Smack allows to add PacketListeners with a PacketFilter value of 'null', we
            // disallow it here in the async API as it makes no sense
            throw new IllegalArgumentException("replyFilter must not be null");
        }
        if (callback == null) {
            throw new IllegalArgumentException("callback must not be null");
        }
        final PacketListener packetListener = new PacketListener() {
            @Override
            public void processPacket(Packet packet) throws NotConnectedException {
                try {
                    XMPPErrorException.ifHasErrorThenThrow(packet);
                    callback.processPacket(packet);
                }
                catch (XMPPErrorException e) {
                    if (exceptionCallback != null) {
                        exceptionCallback.processException(e);
                    }
                }
                finally {
                    removeAsyncPacketListener(this);
                }
            }
        };
        removeCallbacksService.schedule(new Runnable() {
            @Override
            public void run() {
                boolean removed = removeAsyncPacketListener(packetListener);
                // If the packetListener got removed, then it was never run and
                // we never received a response, inform the exception callback
                if (removed && exceptionCallback != null) {
                    exceptionCallback.processException(new NoResponseException(AbstractXMPPConnection.this));
                }
            }
        }, timeout, TimeUnit.MILLISECONDS);
        addAsyncPacketListener(packetListener, replyFilter);
        sendPacket(stanza);
    }

    @Override
    public void sendIqWithResponseCallback(IQ iqRequest, PacketListener callback)
                    throws NotConnectedException {
        sendIqWithResponseCallback(iqRequest, callback, null);
    }

    @Override
    public void sendIqWithResponseCallback(IQ iqRequest, PacketListener callback,
                    ExceptionCallback exceptionCallback) throws NotConnectedException {
        sendIqWithResponseCallback(iqRequest, callback, exceptionCallback, getPacketReplyTimeout());
    }

    @Override
    public void sendIqWithResponseCallback(IQ iqRequest, final PacketListener callback,
                    final ExceptionCallback exceptionCallback, long timeout)
                    throws NotConnectedException {
        PacketFilter replyFilter = new IQReplyFilter(iqRequest, this);
        sendStanzaWithResponseCallback(iqRequest, replyFilter, callback, exceptionCallback, timeout);
    }

    private long lastStanzaReceived;

    public long getLastStanzaReceived() {
        return lastStanzaReceived;
    }

    /**
     * Install a parsing exception callback, which will be invoked once an exception is encountered while parsing a
     * stanza
     * 
     * @param callback the callback to install
     */
    public void setParsingExceptionCallback(ParsingExceptionCallback callback) {
        parsingExceptionCallback = callback;
    }

    /**
     * Get the current active parsing exception callback.
     *  
     * @return the active exception callback or null if there is none
     */
    public ParsingExceptionCallback getParsingExceptionCallback() {
        return parsingExceptionCallback;
    }

    protected final void asyncGo(Runnable runnable) {
        cachedExecutorService.execute(runnable);
    }

}
