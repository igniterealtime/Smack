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
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
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
import org.jivesoftware.smack.provider.PacketExtensionProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.rosterstore.RosterStore;
import org.jivesoftware.smack.util.PacketParserUtils;
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
     * List of PacketListeners that will be notified when a new packet was received.
     */
    private final Map<PacketListener, ListenerWrapper> recvListeners =
            new HashMap<PacketListener, ListenerWrapper>();

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

    /**
     * ExecutorService used to invoke the PacketListeners on newly arrived and parsed stanzas. It is
     * important that we use a <b>single threaded ExecutorService</b> in order to guarantee that the
     * PacketListeners are invoked in the same order the stanzas arrived.
     */
    private final ScheduledExecutorService executorService = new ScheduledThreadPoolExecutor(1,
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

    private boolean anonymous = false;

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
    public abstract String getUser();

    @Override
    public abstract String getConnectionID();

    @Override
    public abstract boolean isConnected();

    @Override
    public abstract boolean isAuthenticated();

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
     */
    public void login(String username, String password) throws XMPPException, SmackException, IOException {
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
     */
    public abstract void login(String username, String password, String resource) throws XMPPException, SmackException, IOException;

    /**
     * Logs in to the server anonymously. Very few servers are configured to support anonymous
     * authentication, so it's fairly likely logging in anonymously will fail. If anonymous login
     * does succeed, your XMPP address will likely be in the form "123ABC@server/789XYZ" or
     * "server/123ABC" (where "123ABC" and "789XYZ" is a random value generated by the server).
     * 
     * @throws XMPPException if an error occurs on the XMPP protocol level.
     * @throws SmackException if an error occurs somehwere else besides XMPP protocol level.
     * @throws IOException 
     */
    public abstract void loginAnonymously() throws XMPPException, SmackException, IOException;

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
        PacketCollector packetCollector = createPacketCollector(new PacketIDFilter(bindResource));
        try {
            sendPacket(bindResource);
        } catch (NotConnectedException e) {
            packetCollector.cancel();
            throw e;
        }
        Bind response = packetCollector.nextResultOrThrow();
        user = response.getJid();
        setServiceName(XmppStringUtils.parseDomain(user));

        if (hasFeature(Session.ELEMENT, Session.NAMESPACE) && !getConfiguration().isLegacySessionDisabled()) {
            Session session = new Session();
            packetCollector = createPacketCollector(new PacketIDFilter(session));
            try {
                sendPacket(session);
            } catch (NotConnectedException e) {
                packetCollector.cancel();
                throw e;
            }
            packetCollector.nextResultOrThrow();
        }
    }

    protected void afterSuccessfulLogin(final boolean anonymous, final boolean resumed) throws NotConnectedException {
        // Indicate that we're now authenticated.
        this.authenticated = true;
        this.anonymous = anonymous;

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
            sendPacket(new Presence(Presence.Type.available));
        }
    }

    @Override
    public boolean isAnonymous() {
        return anonymous;
    }

    protected void setServiceName(String serviceName) {
        config.setServiceName(serviceName);
    }

    protected void setLoginInfo(String username, String password, String resource) {
        config.setLoginInfo(username, password, resource);
    }

    protected void maybeResolveDns() throws Exception {
        config.maybeResolveDns();
    }

    protected Lock getConnectionLock() {
        return connectionLock;
    }

    @Override
    public void sendPacket(Packet packet) throws NotConnectedException {
        if (!isConnected()) {
            throw new NotConnectedException();
        }
        if (packet == null) {
            throw new IllegalArgumentException("Packet must not be null");
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
        PacketCollector packetCollector = createPacketCollector(packetFilter);
        try {
            // Now we can send the packet as the collector has been created
            sendPacket(packet);
        }
        catch (NotConnectedException e) {
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
        if (packetListener == null) {
            throw new NullPointerException("Packet listener is null.");
        }
        ListenerWrapper wrapper = new ListenerWrapper(packetListener, packetFilter);
        synchronized (recvListeners) {
            recvListeners.put(packetListener, wrapper);
        }
    }

    @Override
    public boolean removePacketListener(PacketListener packetListener) {
        synchronized (recvListeners) {
            return recvListeners.remove(packetListener) != null;
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
     * 
     * @param packet the packet to process.
     */
    private void firePacketSendingListeners(Packet packet) {
        List<PacketListener> listenersToNotify = new LinkedList<PacketListener>();
        synchronized (sendListeners) {
            for (ListenerWrapper listenerWrapper : sendListeners.values()) {
                if (listenerWrapper.filterMatches(packet)) {
                    listenersToNotify.add(listenerWrapper.getListener());
                }
            }
        }
        for (PacketListener listener : listenersToNotify) {
            try {
                listener.processPacket(packet);
            }
            catch (NotConnectedException e) {
                LOGGER.log(Level.WARNING, "Got not connected exception, aborting");
                break;
            }
        }
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

    /**
     * Invoke {@link PacketCollector#processPacket(Packet)} for every
     * PacketCollector with the given packet.
     *
     * @param packet the packet to notify the PacketCollectors about.
     */
    protected void invokePacketCollectors(Packet packet) {
        // Loop through all collectors and notify the appropriate ones.
        for (PacketCollector collector: collectors) {
            collector.processPacket(packet);
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
        if (packet == null) {
            return;
        }

        invokePacketCollectors(packet);

        // Deliver the incoming packet to listeners.
        executorService.submit(new ListenerNotification(packet));
    }

    protected void notifiyReceivedListeners(Packet packet) {
        List<PacketListener> listenersToNotify = new LinkedList<PacketListener>();
        synchronized (recvListeners) {
            for (ListenerWrapper listenerWrapper : recvListeners.values()) {
                if (listenerWrapper.filterMatches(packet)) {
                    listenersToNotify.add(listenerWrapper.getListener());
                }
            }
        }

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

    /**
     * A runnable to notify all listeners of a packet.
     */
    private class ListenerNotification implements Runnable {

        private final Packet packet;

        public ListenerNotification(Packet packet) {
            this.packet = packet;
        }

        public void run() {
            notifiyReceivedListeners(packet);
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
        try {
            // It's usually not a good idea to rely on finalize. But this is the easiest way to
            // avoid the "Smack Listener Processor" leaking. The thread(s) of the executor have a
            // reference to their ExecutorService which prevents the ExecutorService from being
            // gc'ed. It is possible that the XMPPConnection instance is gc'ed while the
            // listenerExecutor ExecutorService call not be gc'ed until it got shut down.
            executorService.shutdownNow();
            removeCallbacksService.shutdownNow();
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
                    streamFeature = Session.Feature.INSTANCE;
                    break;
                case RosterVer.ELEMENT:
                    if(namespace.equals(RosterVer.NAMESPACE)) {
                        streamFeature = RosterVer.INSTANCE;
                    }
                    else {
                        LOGGER.severe("Unkown Roster Versioning Namespace: "
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
                    new SmackExecutorThreadFactory(connectionCounterValue));

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
                    removePacketListener(this);
                }
            }
        };
        removeCallbacksService.schedule(new Runnable() {
            @Override
            public void run() {
                boolean removed = removePacketListener(packetListener);
                // If the packetListener got removed, then it was never run and
                // we never received a response, inform the exception callback
                if (removed && exceptionCallback != null) {
                    exceptionCallback.processException(new NoResponseException());
                }
            }
        }, timeout, TimeUnit.MILLISECONDS);
        addPacketListener(packetListener, replyFilter);
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

    protected void reportStanzaReceived() {
        this.lastStanzaReceived = System.currentTimeMillis();
    }
}
