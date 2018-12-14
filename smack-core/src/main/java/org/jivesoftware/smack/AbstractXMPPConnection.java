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
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.SmackConfiguration.UnknownIqRequestReplyMode;
import org.jivesoftware.smack.SmackException.AlreadyConnectedException;
import org.jivesoftware.smack.SmackException.AlreadyLoggedInException;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.SmackException.NotLoggedInException;
import org.jivesoftware.smack.SmackException.ResourceBindingNotOfferedException;
import org.jivesoftware.smack.SmackException.SecurityRequiredByClientException;
import org.jivesoftware.smack.SmackException.SecurityRequiredException;
import org.jivesoftware.smack.SmackFuture.InternalSmackFuture;
import org.jivesoftware.smack.XMPPException.StreamErrorException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.compress.packet.Compress;
import org.jivesoftware.smack.compression.XMPPInputOutputStream;
import org.jivesoftware.smack.debugger.SmackDebugger;
import org.jivesoftware.smack.debugger.SmackDebuggerFactory;
import org.jivesoftware.smack.filter.IQReplyFilter;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.filter.StanzaIdFilter;
import org.jivesoftware.smack.iqrequest.IQRequestHandler;
import org.jivesoftware.smack.packet.Bind;
import org.jivesoftware.smack.packet.ErrorIQ;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Mechanisms;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Nonza;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Session;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.packet.StanzaError;
import org.jivesoftware.smack.packet.StartTls;
import org.jivesoftware.smack.packet.StreamError;
import org.jivesoftware.smack.parsing.ParsingExceptionCallback;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.sasl.core.SASLAnonymous;
import org.jivesoftware.smack.util.Async;
import org.jivesoftware.smack.util.DNSUtil;
import org.jivesoftware.smack.util.Objects;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smack.util.ParserUtils;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.dns.HostAddress;

import org.jxmpp.jid.DomainBareJid;
import org.jxmpp.jid.EntityFullJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.parts.Resourcepart;
import org.jxmpp.util.XmppStringUtils;
import org.minidns.dnsname.DnsName;
import org.xmlpull.v1.XmlPullParser;


/**
 * This abstract class is commonly used as super class for XMPP connection mechanisms like TCP and BOSH. Hence it
 * provides the methods for connection state management, like {@link #connect()}, {@link #login()} and
 * {@link #disconnect()} (which are deliberately not provided by the {@link XMPPConnection} interface).
 * <p>
 * <b>Note:</b> The default entry point to Smack's documentation is {@link XMPPConnection}. If you are getting started
 * with Smack, then head over to {@link XMPPConnection} and the come back here.
 * </p>
 * <h2>Parsing Exceptions</h2>
 * <p>
 * In case a Smack parser (Provider) throws those exceptions are handled over to the {@link ParsingExceptionCallback}. A
 * common cause for a provider throwing is illegal input, for example a non-numeric String where only Integers are
 * allowed. Smack's <em>default behavior</em> follows the <b>"fail-hard per default"</b> principle leading to a
 * termination of the connection on parsing exceptions. This default was chosen to make users eventually aware that they
 * should configure their own callback and handle those exceptions to prevent the disconnect. Handle a parsing exception
 * could be as simple as using a non-throwing no-op callback, which would cause the faulty stream element to be taken
 * out of the stream, i.e., Smack behaves like that element was never received.
 * </p>
 * <p>
 * If the parsing exception is because Smack received illegal input, then please consider informing the authors of the
 * originating entity about that. If it was thrown because of an bug in a Smack parser, then please consider filling a
 * bug with Smack.
 * </p>
 * <h3>Managing the parsing exception callback</h3>
 * <p>
 * The "fail-hard per default" behavior is achieved by using the
 * {@link org.jivesoftware.smack.parsing.ExceptionThrowingCallbackWithHint} as default parsing exception callback. You
 * can change the behavior using {@link #setParsingExceptionCallback(ParsingExceptionCallback)} to set a new callback.
 * Use {@link org.jivesoftware.smack.SmackConfiguration#setDefaultParsingExceptionCallback(ParsingExceptionCallback)} to
 * set the default callback.
 * </p>
 */
public abstract class AbstractXMPPConnection implements XMPPConnection {
    private static final Logger LOGGER = Logger.getLogger(AbstractXMPPConnection.class.getName());

    /**
     * Counter to uniquely identify connections that are created.
     */
    private static final AtomicInteger connectionCounter = new AtomicInteger(0);

    static {
        // Ensure the SmackConfiguration class is loaded by calling a method in it.
        SmackConfiguration.getVersion();
    }

    /**
     * A collection of ConnectionListeners which listen for connection closing
     * and reconnection events.
     */
    protected final Set<ConnectionListener> connectionListeners =
            new CopyOnWriteArraySet<>();

    /**
     * A collection of StanzaCollectors which collects packets for a specified filter
     * and perform blocking and polling operations on the result queue.
     * <p>
     * We use a ConcurrentLinkedQueue here, because its Iterator is weakly
     * consistent and we want {@link #invokeStanzaCollectorsAndNotifyRecvListeners(Stanza)} for-each
     * loop to be lock free. As drawback, removing a StanzaCollector is O(n).
     * The alternative would be a synchronized HashSet, but this would mean a
     * synchronized block around every usage of <code>collectors</code>.
     * </p>
     */
    private final Collection<StanzaCollector> collectors = new ConcurrentLinkedQueue<>();

    /**
     * List of PacketListeners that will be notified synchronously when a new stanza was received.
     */
    private final Map<StanzaListener, ListenerWrapper> syncRecvListeners = new LinkedHashMap<>();

    /**
     * List of PacketListeners that will be notified asynchronously when a new stanza was received.
     */
    private final Map<StanzaListener, ListenerWrapper> asyncRecvListeners = new LinkedHashMap<>();

    /**
     * List of PacketListeners that will be notified when a new stanza was sent.
     */
    private final Map<StanzaListener, ListenerWrapper> sendListeners =
            new HashMap<>();

    /**
     * List of PacketListeners that will be notified when a new stanza is about to be
     * sent to the server. These interceptors may modify the stanza before it is being
     * actually sent to the server.
     */
    private final Map<StanzaListener, InterceptorWrapper> interceptors =
            new HashMap<>();

    protected final Lock connectionLock = new ReentrantLock();

    protected final Map<String, ExtensionElement> streamFeatures = new HashMap<>();

    /**
     * The full JID of the authenticated user, as returned by the resource binding response of the server.
     * <p>
     * It is important that we don't infer the user from the login() arguments and the configurations service name, as,
     * for example, when SASL External is used, the username is not given to login but taken from the 'external'
     * certificate.
     * </p>
     */
    protected EntityFullJid user;

    protected boolean connected = false;

    /**
     * The stream ID, see RFC 6120 § 4.7.3
     */
    protected String streamId;

    /**
     * The timeout to wait for a reply in milliseconds.
     */
    private long replyTimeout = SmackConfiguration.getDefaultReplyTimeout();

    /**
     * The SmackDebugger allows to log and debug XML traffic.
     */
    protected final SmackDebugger debugger;

    /**
     * The Reader which is used for the debugger.
     */
    protected Reader reader;

    /**
     * The Writer which is used for the debugger.
     */
    protected Writer writer;

    protected final SynchronizationPoint<SmackException> tlsHandled = new SynchronizationPoint<>(this, "establishing TLS");

    /**
     * Set to success if the last features stanza from the server has been parsed. A XMPP connection
     * handshake can invoke multiple features stanzas, e.g. when TLS is activated a second feature
     * stanza is send by the server. This is set to true once the last feature stanza has been
     * parsed.
     */
    protected final SynchronizationPoint<Exception> lastFeaturesReceived = new SynchronizationPoint<Exception>(
                    AbstractXMPPConnection.this, "last stream features received from server");

    /**
     * Set to success if the SASL feature has been received.
     */
    protected final SynchronizationPoint<XMPPException> saslFeatureReceived = new SynchronizationPoint<>(
                    AbstractXMPPConnection.this, "SASL mechanisms stream feature from server");

    /**
     * The SASLAuthentication manager that is responsible for authenticating with the server.
     */
    protected final SASLAuthentication saslAuthentication;

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
     * This scheduled thread pool executor is used to remove pending callbacks.
     */
    protected static final ScheduledExecutorService SCHEDULED_EXECUTOR_SERVICE = Executors.newSingleThreadScheduledExecutor(
            new ThreadFactory() {
                @Override
                public Thread newThread(Runnable runnable) {
                    Thread thread = new Thread(runnable);
                    thread.setName("Smack Scheduled Executor Service");
                    thread.setDaemon(true);
                    return thread;
                }
            });

    /**
     * A cached thread pool executor service with custom thread factory to set meaningful names on the threads and set
     * them 'daemon'.
     */
    private static final ExecutorService CACHED_EXECUTOR_SERVICE = Executors.newCachedThreadPool(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable);
            thread.setName("Smack Cached Executor");
            thread.setDaemon(true);
            return thread;
        }
    });

    private static final AsyncButOrdered<AbstractXMPPConnection> ASYNC_BUT_ORDERED = new AsyncButOrdered<>();

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

    private final Map<String, IQRequestHandler> setIqRequestHandler = new HashMap<>();
    private final Map<String, IQRequestHandler> getIqRequestHandler = new HashMap<>();

    /**
     * Create a new XMPPConnection to an XMPP server.
     *
     * @param configuration The configuration which is used to establish the connection.
     */
    protected AbstractXMPPConnection(ConnectionConfiguration configuration) {
        saslAuthentication = new SASLAuthentication(this, configuration);
        config = configuration;
        SmackDebuggerFactory debuggerFactory = configuration.getDebuggerFactory();
        if (debuggerFactory != null) {
            debugger = debuggerFactory.create(this);
        } else {
            debugger = null;
        }
        // Notify listeners that a new connection has been established
        for (ConnectionCreationListener listener : XMPPConnectionRegistry.getConnectionCreationListeners()) {
            listener.connectionCreated(this);
        }
    }

    /**
     * Get the connection configuration used by this connection.
     *
     * @return the connection configuration.
     */
    public ConnectionConfiguration getConfiguration() {
        return config;
    }

    @Override
    public DomainBareJid getXMPPServiceDomain() {
        if (xmppServiceDomain != null) {
            return xmppServiceDomain;
        }
        return config.getXMPPServiceDomain();
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
    public abstract boolean isSecureConnection();

    protected abstract void sendStanzaInternal(Stanza packet) throws NotConnectedException, InterruptedException;

    @Override
    public abstract void sendNonza(Nonza element) throws NotConnectedException, InterruptedException;

    @Override
    public abstract boolean isUsingCompression();

    /**
     * Establishes a connection to the XMPP server. It basically
     * creates and maintains a connection to the server.
     * <p>
     * Listeners will be preserved from a previous connection.
     * </p>
     *
     * @throws XMPPException if an error occurs on the XMPP protocol level.
     * @throws SmackException if an error occurs somewhere else besides XMPP protocol level.
     * @throws IOException
     * @return a reference to this object, to chain <code>connect()</code> with <code>login()</code>.
     * @throws InterruptedException
     */
    public synchronized AbstractXMPPConnection connect() throws SmackException, IOException, XMPPException, InterruptedException {
        // Check if not already connected
        throwAlreadyConnectedExceptionIfAppropriate();

        // Reset the connection state
        saslAuthentication.init();
        saslFeatureReceived.init();
        lastFeaturesReceived.init();
        tlsHandled.init();
        streamId = null;

        // Perform the actual connection to the XMPP service
        connectInternal();

        // If TLS is required but the server doesn't offer it, disconnect
        // from the server and throw an error. First check if we've already negotiated TLS
        // and are secure, however (features get parsed a second time after TLS is established).
        if (!isSecureConnection() && getConfiguration().getSecurityMode() == SecurityMode.required) {
            shutdown();
            throw new SecurityRequiredByClientException();
        }

        // Make note of the fact that we're now connected.
        connected = true;
        callConnectionConnectedListener();

        return this;
    }

    /**
     * Abstract method that concrete subclasses of XMPPConnection need to implement to perform their
     * way of XMPP connection establishment. Implementations are required to perform an automatic
     * login if the previous connection state was logged (authenticated).
     *
     * @throws SmackException
     * @throws IOException
     * @throws XMPPException
     * @throws InterruptedException
     */
    protected abstract void connectInternal() throws SmackException, IOException, XMPPException, InterruptedException;

    private String usedUsername, usedPassword;

    /**
     * The resourcepart used for this connection. May not be the resulting resourcepart if it's null or overridden by the XMPP service.
     */
    private Resourcepart usedResource;

    /**
     * Logs in to the server using the strongest SASL mechanism supported by
     * the server. If more than the connection's default stanza timeout elapses in each step of the
     * authentication process without a response from the server, a
     * {@link SmackException.NoResponseException} will be thrown.
     * <p>
     * Before logging in (i.e. authenticate) to the server the connection must be connected
     * by calling {@link #connect}.
     * </p>
     * <p>
     * It is possible to log in without sending an initial available presence by using
     * {@link ConnectionConfiguration.Builder#setSendPresence(boolean)}.
     * Finally, if you want to not pass a password and instead use a more advanced mechanism
     * while using SASL then you may be interested in using
     * {@link ConnectionConfiguration.Builder#setCallbackHandler(javax.security.auth.callback.CallbackHandler)}.
     * For more advanced login settings see {@link ConnectionConfiguration}.
     * </p>
     *
     * @throws XMPPException if an error occurs on the XMPP protocol level.
     * @throws SmackException if an error occurs somewhere else besides XMPP protocol level.
     * @throws IOException if an I/O error occurs during login.
     * @throws InterruptedException
     */
    public synchronized void login() throws XMPPException, SmackException, IOException, InterruptedException {
        // The previously used username, password and resource take over precedence over the
        // ones from the connection configuration
        CharSequence username = usedUsername != null ? usedUsername : config.getUsername();
        String password = usedPassword != null ? usedPassword : config.getPassword();
        Resourcepart resource = usedResource != null ? usedResource : config.getResource();
        login(username, password, resource);
    }

    /**
     * Same as {@link #login(CharSequence, String, Resourcepart)}, but takes the resource from the connection
     * configuration.
     *
     * @param username
     * @param password
     * @throws XMPPException
     * @throws SmackException
     * @throws IOException
     * @throws InterruptedException
     * @see #login
     */
    public synchronized void login(CharSequence username, String password) throws XMPPException, SmackException,
                    IOException, InterruptedException {
        login(username, password, config.getResource());
    }

    /**
     * Login with the given username (authorization identity). You may omit the password if a callback handler is used.
     * If resource is null, then the server will generate one.
     *
     * @param username
     * @param password
     * @param resource
     * @throws XMPPException
     * @throws SmackException
     * @throws IOException
     * @throws InterruptedException
     * @see #login
     */
    public synchronized void login(CharSequence username, String password, Resourcepart resource) throws XMPPException,
                    SmackException, IOException, InterruptedException {
        if (!config.allowNullOrEmptyUsername) {
            StringUtils.requireNotNullOrEmpty(username, "Username must not be null or empty");
        }
        throwNotConnectedExceptionIfAppropriate("Did you call connect() before login()?");
        throwAlreadyLoggedInExceptionIfAppropriate();
        usedUsername = username != null ? username.toString() : null;
        usedPassword = password;
        usedResource = resource;
        loginInternal(usedUsername, usedPassword, usedResource);
    }

    protected abstract void loginInternal(String username, String password, Resourcepart resource)
                    throws XMPPException, SmackException, IOException, InterruptedException;

    @Override
    public final boolean isConnected() {
        return connected;
    }

    @Override
    public final boolean isAuthenticated() {
        return authenticated;
    }

    @Override
    public final EntityFullJid getUser() {
        return user;
    }

    @Override
    public String getStreamId() {
        if (!isConnected()) {
            return null;
        }
        return streamId;
    }

    protected void bindResourceAndEstablishSession(Resourcepart resource) throws XMPPErrorException,
                    SmackException, InterruptedException {

        // Wait until either:
        // - the servers last features stanza has been parsed
        // - the timeout occurs
        LOGGER.finer("Waiting for last features to be received before continuing with resource binding");
        lastFeaturesReceived.checkIfSuccessOrWait();


        if (!hasFeature(Bind.ELEMENT, Bind.NAMESPACE)) {
            // Server never offered resource binding, which is REQUIRED in XMPP client and
            // server implementations as per RFC6120 7.2
            throw new ResourceBindingNotOfferedException();
        }

        // Resource binding, see RFC6120 7.
        // Note that we can not use IQReplyFilter here, since the users full JID is not yet
        // available. It will become available right after the resource has been successfully bound.
        Bind bindResource = Bind.newSet(resource);
        StanzaCollector packetCollector = createStanzaCollectorAndSend(new StanzaIdFilter(bindResource), bindResource);
        Bind response = packetCollector.nextResultOrThrow();
        // Set the connections user to the result of resource binding. It is important that we don't infer the user
        // from the login() arguments and the configurations service name, as, for example, when SASL External is used,
        // the username is not given to login but taken from the 'external' certificate.
        user = response.getJid();
        xmppServiceDomain = user.asDomainBareJid();

        Session.Feature sessionFeature = getFeature(Session.ELEMENT, Session.NAMESPACE);
        // Only bind the session if it's announced as stream feature by the server, is not optional and not disabled
        // For more information see http://tools.ietf.org/html/draft-cridland-xmpp-session-01
        if (sessionFeature != null && !sessionFeature.isOptional()) {
            Session session = new Session();
            packetCollector = createStanzaCollectorAndSend(new StanzaIdFilter(session), session);
            packetCollector.nextResultOrThrow();
        }
    }

    protected void afterSuccessfulLogin(final boolean resumed) throws NotConnectedException, InterruptedException {
        // Indicate that we're now authenticated.
        this.authenticated = true;

        // If debugging is enabled, change the the debug window title to include the
        // name we are now logged-in as.
        // If DEBUG was set to true AFTER the connection was created the debugger
        // will be null
        if (debugger != null) {
            debugger.userHasLogged(user);
        }
        callConnectionAuthenticatedListener(resumed);

        // Set presence to online. It is important that this is done after
        // callConnectionAuthenticatedListener(), as this call will also
        // eventually load the roster. And we should load the roster before we
        // send the initial presence.
        if (config.isSendPresence() && !resumed) {
            sendStanza(new Presence(Presence.Type.available));
        }
    }

    @Override
    public final boolean isAnonymous() {
        return isAuthenticated() && SASLAnonymous.NAME.equals(getUsedSaslMechansism());
    }

    /**
     * Get the name of the SASL mechanism that was used to authenticate this connection. This returns the name of
     * mechanism which was used the last time this connection was authenticated, and will return <code>null</code> if
     * this connection was not authenticated before.
     *
     * @return the name of the used SASL mechanism.
     * @since 4.2
     */
    public final String getUsedSaslMechansism() {
        return saslAuthentication.getNameOfLastUsedSaslMechansism();
    }

    private DomainBareJid xmppServiceDomain;

    protected List<HostAddress> hostAddresses;

    /**
     * Populates {@link #hostAddresses} with the resolved addresses or with the configured host address. If no host
     * address was configured and all lookups failed, for example with NX_DOMAIN, then {@link #hostAddresses} will be
     * populated with the empty list.
     *
     * @return a list of host addresses where DNS (SRV) RR resolution failed.
     */
    protected List<HostAddress> populateHostAddresses() {
        List<HostAddress> failedAddresses = new LinkedList<>();
        if (config.hostAddress != null) {
            hostAddresses = new ArrayList<>(1);
            HostAddress hostAddress = new HostAddress(config.port, config.hostAddress);
            hostAddresses.add(hostAddress);
        }
        else if (config.host != null) {
            hostAddresses = new ArrayList<>(1);
            HostAddress hostAddress = DNSUtil.getDNSResolver().lookupHostAddress(config.host, config.port, failedAddresses, config.getDnssecMode());
            if (hostAddress != null) {
                hostAddresses.add(hostAddress);
            }
        } else {
            // N.B.: Important to use config.serviceName and not AbstractXMPPConnection.serviceName
            DnsName dnsName = DnsName.from(config.getXMPPServiceDomain());
            hostAddresses = DNSUtil.resolveXMPPServiceDomain(dnsName, failedAddresses, config.getDnssecMode());
        }
        // Either the populated host addresses are not empty *or* there must be at least one failed address.
        assert (!hostAddresses.isEmpty() || !failedAddresses.isEmpty());
        return failedAddresses;
    }

    protected Lock getConnectionLock() {
        return connectionLock;
    }

    protected void throwNotConnectedExceptionIfAppropriate() throws NotConnectedException {
        throwNotConnectedExceptionIfAppropriate(null);
    }

    protected void throwNotConnectedExceptionIfAppropriate(String optionalHint) throws NotConnectedException {
        if (!isConnected()) {
            throw new NotConnectedException(optionalHint);
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
    public void sendStanza(Stanza stanza) throws NotConnectedException, InterruptedException {
        Objects.requireNonNull(stanza, "Stanza must not be null");
        assert (stanza instanceof Message || stanza instanceof Presence || stanza instanceof IQ);

        throwNotConnectedExceptionIfAppropriate();
        switch (fromMode) {
        case OMITTED:
            stanza.setFrom((Jid) null);
            break;
        case USER:
            stanza.setFrom(getUser());
            break;
        case UNCHANGED:
        default:
            break;
        }
        // Invoke interceptors for the new stanza that is about to be sent. Interceptors may modify
        // the content of the stanza.
        firePacketInterceptors(stanza);
        sendStanzaInternal(stanza);
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
     */
    public void disconnect() {
        Presence unavailablePresence = null;
        if (isAuthenticated()) {
            unavailablePresence = new Presence(Presence.Type.unavailable);
        }
        try {
            disconnect(unavailablePresence);
        }
        catch (NotConnectedException e) {
            LOGGER.log(Level.FINEST, "Connection is already disconnected", e);
        }
    }

    /**
     * Closes the connection. A custom unavailable presence is sent to the server, followed
     * by closing the stream. The XMPPConnection can still be used for connecting to the server
     * again. A custom unavailable presence is useful for communicating offline presence
     * information such as "On vacation". Typically, just the status text of the presence
     * stanza is set with online information, but most XMPP servers will deliver the full
     * presence stanza with whatever data is set.
     *
     * @param unavailablePresence the optional presence stanza to send during shutdown.
     * @throws NotConnectedException
     */
    public synchronized void disconnect(Presence unavailablePresence) throws NotConnectedException {
        if (unavailablePresence != null) {
            try {
                sendStanza(unavailablePresence);
            } catch (InterruptedException e) {
                LOGGER.log(Level.FINE,
                        "Was interrupted while sending unavailable presence. Continuing to disconnect the connection",
                        e);
            }
        }
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

    @Override
    public <I extends IQ> I sendIqRequestAndWaitForResponse(IQ request)
            throws NoResponseException, XMPPErrorException, NotConnectedException, InterruptedException {
        StanzaCollector collector = createStanzaCollectorAndSend(request);
        IQ resultResponse = collector.nextResultOrThrow();
        @SuppressWarnings("unchecked")
        I concreteResultResponse = (I) resultResponse;
        return concreteResultResponse;
    }

    @Override
    public StanzaCollector createStanzaCollectorAndSend(IQ packet) throws NotConnectedException, InterruptedException {
        StanzaFilter packetFilter = new IQReplyFilter(packet, this);
        // Create the packet collector before sending the packet
        StanzaCollector packetCollector = createStanzaCollectorAndSend(packetFilter, packet);
        return packetCollector;
    }

    @Override
    public StanzaCollector createStanzaCollectorAndSend(StanzaFilter packetFilter, Stanza packet)
                    throws NotConnectedException, InterruptedException {
        StanzaCollector.Configuration configuration = StanzaCollector.newConfiguration()
                        .setStanzaFilter(packetFilter)
                        .setRequest(packet);
        // Create the packet collector before sending the packet
        StanzaCollector packetCollector = createStanzaCollector(configuration);
        try {
            // Now we can send the packet as the collector has been created
            sendStanza(packet);
        }
        catch (InterruptedException | NotConnectedException | RuntimeException e) {
            packetCollector.cancel();
            throw e;
        }
        return packetCollector;
    }

    @Override
    public StanzaCollector createStanzaCollector(StanzaFilter packetFilter) {
        StanzaCollector.Configuration configuration = StanzaCollector.newConfiguration().setStanzaFilter(packetFilter);
        return createStanzaCollector(configuration);
    }

    @Override
    public StanzaCollector createStanzaCollector(StanzaCollector.Configuration configuration) {
        StanzaCollector collector = new StanzaCollector(this, configuration);
        // Add the collector to the list of active collectors.
        collectors.add(collector);
        return collector;
    }

    @Override
    public void removeStanzaCollector(StanzaCollector collector) {
        collectors.remove(collector);
    }

    @Override
    public void addSyncStanzaListener(StanzaListener packetListener, StanzaFilter packetFilter) {
        if (packetListener == null) {
            throw new NullPointerException("Packet listener is null.");
        }
        ListenerWrapper wrapper = new ListenerWrapper(packetListener, packetFilter);
        synchronized (syncRecvListeners) {
            syncRecvListeners.put(packetListener, wrapper);
        }
    }

    @Override
    public boolean removeSyncStanzaListener(StanzaListener packetListener) {
        synchronized (syncRecvListeners) {
            return syncRecvListeners.remove(packetListener) != null;
        }
    }

    @Override
    public void addAsyncStanzaListener(StanzaListener packetListener, StanzaFilter packetFilter) {
        if (packetListener == null) {
            throw new NullPointerException("Packet listener is null.");
        }
        ListenerWrapper wrapper = new ListenerWrapper(packetListener, packetFilter);
        synchronized (asyncRecvListeners) {
            asyncRecvListeners.put(packetListener, wrapper);
        }
    }

    @Override
    public boolean removeAsyncStanzaListener(StanzaListener packetListener) {
        synchronized (asyncRecvListeners) {
            return asyncRecvListeners.remove(packetListener) != null;
        }
    }

    @Deprecated
    @Override
    public void addPacketSendingListener(StanzaListener packetListener, StanzaFilter packetFilter) {
        addStanzaSendingListener(packetListener, packetFilter);
    }

    @Override
    public void addStanzaSendingListener(StanzaListener packetListener, StanzaFilter packetFilter) {
        if (packetListener == null) {
            throw new NullPointerException("Packet listener is null.");
        }
        ListenerWrapper wrapper = new ListenerWrapper(packetListener, packetFilter);
        synchronized (sendListeners) {
            sendListeners.put(packetListener, wrapper);
        }
    }

    @Deprecated
    @Override
    public void removePacketSendingListener(StanzaListener packetListener) {
        removeStanzaSendingListener(packetListener);
    }

    @Override
    public void removeStanzaSendingListener(StanzaListener packetListener) {
        synchronized (sendListeners) {
            sendListeners.remove(packetListener);
        }
    }

    /**
     * Process all stanza listeners for sending packets.
     * <p>
     * Compared to {@link #firePacketInterceptors(Stanza)}, the listeners will be invoked in a new thread.
     * </p>
     *
     * @param packet the stanza to process.
     */
    @SuppressWarnings("javadoc")
    protected void firePacketSendingListeners(final Stanza packet) {
        final SmackDebugger debugger = this.debugger;
        if (debugger != null) {
            debugger.onOutgoingStreamElement(packet);
        }

        final List<StanzaListener> listenersToNotify = new LinkedList<>();
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
                for (StanzaListener listener : listenersToNotify) {
                    try {
                        listener.processStanza(packet);
                    }
                    catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Sending listener threw exception", e);
                        continue;
                    }
                }
            }
        });
    }

    @Deprecated
    @Override
    public void addPacketInterceptor(StanzaListener packetInterceptor,
                                     StanzaFilter packetFilter) {
        addStanzaInterceptor(packetInterceptor, packetFilter);
    }

    @Override
    public void addStanzaInterceptor(StanzaListener packetInterceptor,
            StanzaFilter packetFilter) {
        if (packetInterceptor == null) {
            throw new NullPointerException("Packet interceptor is null.");
        }
        InterceptorWrapper interceptorWrapper = new InterceptorWrapper(packetInterceptor, packetFilter);
        synchronized (interceptors) {
            interceptors.put(packetInterceptor, interceptorWrapper);
        }
    }

    @Deprecated
    @Override
    public void removePacketInterceptor(StanzaListener packetInterceptor) {
        removeStanzaInterceptor(packetInterceptor);
    }

    @Override
    public void removeStanzaInterceptor(StanzaListener packetInterceptor) {
        synchronized (interceptors) {
            interceptors.remove(packetInterceptor);
        }
    }

    /**
     * Process interceptors. Interceptors may modify the stanza that is about to be sent.
     * Since the thread that requested to send the stanza will invoke all interceptors, it
     * is important that interceptors perform their work as soon as possible so that the
     * thread does not remain blocked for a long period.
     *
     * @param packet the stanza that is going to be sent to the server
     */
    private void firePacketInterceptors(Stanza packet) {
        List<StanzaListener> interceptorsToInvoke = new LinkedList<>();
        synchronized (interceptors) {
            for (InterceptorWrapper interceptorWrapper : interceptors.values()) {
                if (interceptorWrapper.filterMatches(packet)) {
                    interceptorsToInvoke.add(interceptorWrapper.getInterceptor());
                }
            }
        }
        for (StanzaListener interceptor : interceptorsToInvoke) {
            try {
                interceptor.processStanza(packet);
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
        if (debugger != null) {
            // Obtain new reader and writer from the existing debugger
            reader = debugger.newConnectionReader(reader);
            writer = debugger.newConnectionWriter(writer);
        }
    }

    @Override
    public long getReplyTimeout() {
        return replyTimeout;
    }

    @Override
    public void setReplyTimeout(long timeout) {
        replyTimeout = timeout;
    }

    private SmackConfiguration.UnknownIqRequestReplyMode unknownIqRequestReplyMode = SmackConfiguration.getUnknownIqRequestReplyMode();

    /**
     * Set how Smack behaves when an unknown IQ request has been received.
     *
     * @param unknownIqRequestReplyMode reply mode.
     */
    public void setUnknownIqRequestReplyMode(UnknownIqRequestReplyMode unknownIqRequestReplyMode) {
        this.unknownIqRequestReplyMode = Objects.requireNonNull(unknownIqRequestReplyMode, "Mode must not be null");
    }

    protected void parseAndProcessStanza(XmlPullParser parser) throws Exception {
        ParserUtils.assertAtStartTag(parser);
        int parserDepth = parser.getDepth();
        Stanza stanza = null;
        try {
            stanza = PacketParserUtils.parseStanza(parser);
        }
        catch (Exception e) {
            CharSequence content = PacketParserUtils.parseContentDepth(parser,
                            parserDepth);
            UnparseableStanza message = new UnparseableStanza(content, e);
            ParsingExceptionCallback callback = getParsingExceptionCallback();
            if (callback != null) {
                callback.handleUnparsableStanza(message);
            }
        }
        ParserUtils.assertAtEndTag(parser);
        if (stanza != null) {
            processStanza(stanza);
        }
    }

    /**
     * Processes a stanza after it's been fully parsed by looping through the installed
     * stanza collectors and listeners and letting them examine the stanza to see if
     * they are a match with the filter.
     *
     * @param stanza the stanza to process.
     * @throws InterruptedException
     */
    protected void processStanza(final Stanza stanza) throws InterruptedException {
        assert (stanza != null);

        final SmackDebugger debugger = this.debugger;
        if (debugger != null) {
            debugger.onIncomingStreamElement(stanza);
        }

        lastStanzaReceived = System.currentTimeMillis();
        // Deliver the incoming packet to listeners.
        invokeStanzaCollectorsAndNotifyRecvListeners(stanza);
    }

    /**
     * Invoke {@link StanzaCollector#processStanza(Stanza)} for every
     * StanzaCollector with the given packet. Also notify the receive listeners with a matching stanza filter about the packet.
     * <p>
     * This method will be invoked by the connections incoming processing thread which may be shared across multiple connections and
     * thus it is important that no user code, e.g. in form of a callback, is invoked by this method. For the same reason,
     * this method must not block for an extended period of time.
     * </p>
     *
     * @param packet the stanza to notify the StanzaCollectors and receive listeners about.
     */
    protected void invokeStanzaCollectorsAndNotifyRecvListeners(final Stanza packet) {
        if (packet instanceof IQ) {
            final IQ iq = (IQ) packet;
            if (iq.isRequestIQ()) {
                final IQ iqRequest = iq;
                final String key = XmppStringUtils.generateKey(iq.getChildElementName(), iq.getChildElementNamespace());
                IQRequestHandler iqRequestHandler;
                final IQ.Type type = iq.getType();
                switch (type) {
                case set:
                    synchronized (setIqRequestHandler) {
                        iqRequestHandler = setIqRequestHandler.get(key);
                    }
                    break;
                case get:
                    synchronized (getIqRequestHandler) {
                        iqRequestHandler = getIqRequestHandler.get(key);
                    }
                    break;
                default:
                    throw new IllegalStateException("Should only encounter IQ type 'get' or 'set'");
                }
                if (iqRequestHandler == null) {
                    StanzaError.Condition replyCondition;
                    switch (unknownIqRequestReplyMode) {
                    case doNotReply:
                        return;
                    case replyFeatureNotImplemented:
                        replyCondition = StanzaError.Condition.feature_not_implemented;
                        break;
                    case replyServiceUnavailable:
                        replyCondition = StanzaError.Condition.service_unavailable;
                        break;
                    default:
                        throw new AssertionError();
                    }

                    // If the IQ stanza is of type "get" or "set" with no registered IQ request handler, then answer an
                    // IQ of type 'error' with condition 'service-unavailable'.
                    ErrorIQ errorIQ = IQ.createErrorResponse(iq, StanzaError.getBuilder((
                                    replyCondition)));
                    try {
                        sendStanza(errorIQ);
                    }
                    catch (InterruptedException | NotConnectedException e) {
                        LOGGER.log(Level.WARNING, "Exception while sending error IQ to unkown IQ request", e);
                    }
                } else {
                    Executor executorService = null;
                    switch (iqRequestHandler.getMode()) {
                    case sync:
                        executorService = ASYNC_BUT_ORDERED.asExecutorFor(this);
                        break;
                    case async:
                        executorService = CACHED_EXECUTOR_SERVICE;
                        break;
                    }
                    final IQRequestHandler finalIqRequestHandler = iqRequestHandler;
                    executorService.execute(new Runnable() {
                        @Override
                        public void run() {
                            IQ response = finalIqRequestHandler.handleIQRequest(iq);
                            if (response == null) {
                                // It is not ideal if the IQ request handler does not return an IQ response, because RFC
                                // 6120 § 8.1.2 does specify that a response is mandatory. But some APIs, mostly the
                                // file transfer one, does not always return a result, so we need to handle this case.
                                // Also sometimes a request handler may decide that it's better to not send a response,
                                // e.g. to avoid presence leaks.
                                return;
                            }

                            assert (response.getType() == IQ.Type.result || response.getType() == IQ.Type.error);

                            response.setTo(iqRequest.getFrom());
                            response.setStanzaId(iqRequest.getStanzaId());
                            try {
                                sendStanza(response);
                            }
                            catch (InterruptedException | NotConnectedException e) {
                                LOGGER.log(Level.WARNING, "Exception while sending response to IQ request", e);
                            }
                        }
                    });
                }
                // The following returns makes it impossible for packet listeners and collectors to
                // filter for IQ request stanzas, i.e. IQs of type 'set' or 'get'. This is the
                // desired behavior.
                return;
            }
        }

        // First handle the async recv listeners. Note that this code is very similar to what follows a few lines below,
        // the only difference is that asyncRecvListeners is used here and that the packet listeners are started in
        // their own thread.
        final Collection<StanzaListener> listenersToNotify = new LinkedList<>();
        synchronized (asyncRecvListeners) {
            for (ListenerWrapper listenerWrapper : asyncRecvListeners.values()) {
                if (listenerWrapper.filterMatches(packet)) {
                    listenersToNotify.add(listenerWrapper.getListener());
                }
            }
        }

        for (final StanzaListener listener : listenersToNotify) {
            asyncGo(new Runnable() {
                @Override
                public void run() {
                    try {
                        listener.processStanza(packet);
                    } catch (Exception e) {
                        LOGGER.log(Level.SEVERE, "Exception in async packet listener", e);
                    }
                }
            });
        }

        // Loop through all collectors and notify the appropriate ones.
        for (StanzaCollector collector : collectors) {
            collector.processStanza(packet);
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
        ASYNC_BUT_ORDERED.performAsyncButOrdered(this, new Runnable() {
            @Override
            public void run() {
                for (StanzaListener listener : listenersToNotify) {
                    try {
                        listener.processStanza(packet);
                    } catch (NotConnectedException e) {
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
        for (ConnectionListener listener : connectionListeners) {
            listener.connected(this);
        }
    }

    protected void callConnectionAuthenticatedListener(boolean resumed) {
        for (ConnectionListener listener : connectionListeners) {
            try {
                listener.authenticated(this, resumed);
            } catch (Exception e) {
                // Catch and print any exception so we can recover
                // from a faulty listener and finish the shutdown process
                LOGGER.log(Level.SEVERE, "Exception in authenticated listener", e);
            }
        }
    }

    void callConnectionClosedListener() {
        for (ConnectionListener listener : connectionListeners) {
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
        boolean logWarning = true;
        if (e instanceof StreamErrorException) {
            StreamErrorException see = (StreamErrorException) e;
            if (see.getStreamError().getCondition() == StreamError.Condition.not_authorized
                            && wasAuthenticated) {
                logWarning = false;
                LOGGER.log(Level.FINE,
                                "Connection closed with not-authorized stream error after it was already authenticated. The account was likely deleted/unregistered on the server");
            }
        }
        if (logWarning) {
            LOGGER.log(Level.WARNING, "Connection " + this + " closed with error", e);
        }
        for (ConnectionListener listener : connectionListeners) {
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
     * A wrapper class to associate a stanza filter with a listener.
     */
    protected static class ListenerWrapper {

        private final StanzaListener packetListener;
        private final StanzaFilter packetFilter;

        /**
         * Create a class which associates a stanza filter with a listener.
         *
         * @param packetListener the stanza listener.
         * @param packetFilter the associated filter or null if it listen for all packets.
         */
        public ListenerWrapper(StanzaListener packetListener, StanzaFilter packetFilter) {
            this.packetListener = packetListener;
            this.packetFilter = packetFilter;
        }

        public boolean filterMatches(Stanza packet) {
            return packetFilter == null || packetFilter.accept(packet);
        }

        public StanzaListener getListener() {
            return packetListener;
        }
    }

    /**
     * A wrapper class to associate a stanza filter with an interceptor.
     */
    protected static class InterceptorWrapper {

        private final StanzaListener packetInterceptor;
        private final StanzaFilter packetFilter;

        /**
         * Create a class which associates a stanza filter with an interceptor.
         *
         * @param packetInterceptor the interceptor.
         * @param packetFilter the associated filter or null if it intercepts all packets.
         */
        public InterceptorWrapper(StanzaListener packetInterceptor, StanzaFilter packetFilter) {
            this.packetInterceptor = packetInterceptor;
            this.packetFilter = packetFilter;
        }

        public boolean filterMatches(Stanza packet) {
            return packetFilter == null || packetFilter.accept(packet);
        }

        public StanzaListener getInterceptor() {
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

    protected final void parseFeatures(XmlPullParser parser) throws Exception {
        streamFeatures.clear();
        final int initialDepth = parser.getDepth();
        while (true) {
            int eventType = parser.next();

            if (eventType == XmlPullParser.START_TAG && parser.getDepth() == initialDepth + 1) {
                ExtensionElement streamFeature = null;
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
                case Compress.Feature.ELEMENT:
                    streamFeature = PacketParserUtils.parseCompressionFeature(parser);
                    break;
                default:
                    ExtensionElementProvider<ExtensionElement> provider = ProviderManager.getStreamFeatureProvider(name, namespace);
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
                tlsHandled.reportSuccess();
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

    @SuppressWarnings("unused")
    protected void afterFeaturesReceived() throws SecurityRequiredException, NotConnectedException, InterruptedException {
        // Default implementation does nothing
    }

    @SuppressWarnings("unchecked")
    @Override
    public <F extends ExtensionElement> F getFeature(String element, String namespace) {
        return (F) streamFeatures.get(XmppStringUtils.generateKey(element, namespace));
    }

    @Override
    public boolean hasFeature(String element, String namespace) {
        return getFeature(element, namespace) != null;
    }

    protected void addStreamFeature(ExtensionElement feature) {
        String key = XmppStringUtils.generateKey(feature.getElementName(), feature.getNamespace());
        streamFeatures.put(key, feature);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void sendStanzaWithResponseCallback(Stanza stanza, StanzaFilter replyFilter,
                    StanzaListener callback) throws NotConnectedException, InterruptedException {
        sendStanzaWithResponseCallback(stanza, replyFilter, callback, null);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void sendStanzaWithResponseCallback(Stanza stanza, StanzaFilter replyFilter,
                    StanzaListener callback, ExceptionCallback exceptionCallback)
                    throws NotConnectedException, InterruptedException {
        sendStanzaWithResponseCallback(stanza, replyFilter, callback, exceptionCallback,
                        getReplyTimeout());
    }

    @Override
    public SmackFuture<IQ, Exception> sendIqRequestAsync(IQ request) {
        return sendIqRequestAsync(request, getReplyTimeout());
    }

    @Override
    public SmackFuture<IQ, Exception> sendIqRequestAsync(IQ request, long timeout) {
        StanzaFilter replyFilter = new IQReplyFilter(request, this);
        return sendAsync(request, replyFilter, timeout);
    }

    @Override
    public <S extends Stanza> SmackFuture<S, Exception> sendAsync(S stanza, final StanzaFilter replyFilter) {
        return sendAsync(stanza, replyFilter, getReplyTimeout());
    }

    @SuppressWarnings("FutureReturnValueIgnored")
    @Override
    public <S extends Stanza> SmackFuture<S, Exception> sendAsync(S stanza, final StanzaFilter replyFilter, long timeout) {
        Objects.requireNonNull(stanza, "stanza must not be null");
        // While Smack allows to add PacketListeners with a PacketFilter value of 'null', we
        // disallow it here in the async API as it makes no sense
        Objects.requireNonNull(replyFilter, "replyFilter must not be null");

        final InternalSmackFuture<S, Exception> future = new InternalSmackFuture<>();

        final StanzaListener stanzaListener = new StanzaListener() {
            @Override
            public void processStanza(Stanza stanza) throws NotConnectedException, InterruptedException {
                boolean removed = removeAsyncStanzaListener(this);
                if (!removed) {
                    // We lost a race against the "no response" handling runnable. Avoid calling the callback, as the
                    // exception callback will be invoked (if any).
                    return;
                }
                try {
                    XMPPErrorException.ifHasErrorThenThrow(stanza);
                    @SuppressWarnings("unchecked")
                    S s = (S) stanza;
                    future.setResult(s);
                }
                catch (XMPPErrorException exception) {
                    future.setException(exception);
                }
            }
        };
        schedule(new Runnable() {
            @Override
            public void run() {
                boolean removed = removeAsyncStanzaListener(stanzaListener);
                if (!removed) {
                    // We lost a race against the stanza listener, he already removed itself because he received a
                    // reply. There is nothing more to do here.
                    return;
                }

                // If the packetListener got removed, then it was never run and
                // we never received a response, inform the exception callback
                Exception exception;
                if (!isConnected()) {
                    // If the connection is no longer connected, throw a not connected exception.
                    exception = new NotConnectedException(AbstractXMPPConnection.this, replyFilter);
                }
                else {
                    exception = NoResponseException.newWith(AbstractXMPPConnection.this, replyFilter);
                }
                future.setException(exception);
            }
        }, timeout, TimeUnit.MILLISECONDS);

        addAsyncStanzaListener(stanzaListener, replyFilter);
        try {
            sendStanza(stanza);
        }
        catch (NotConnectedException | InterruptedException exception) {
            future.setException(exception);
        }

        return future;
    }

    @SuppressWarnings({ "FutureReturnValueIgnored", "deprecation" })
    @Override
    public void sendStanzaWithResponseCallback(Stanza stanza, final StanzaFilter replyFilter,
                    final StanzaListener callback, final ExceptionCallback exceptionCallback,
                    long timeout) throws NotConnectedException, InterruptedException {
        Objects.requireNonNull(stanza, "stanza must not be null");
        // While Smack allows to add PacketListeners with a PacketFilter value of 'null', we
        // disallow it here in the async API as it makes no sense
        Objects.requireNonNull(replyFilter, "replyFilter must not be null");
        Objects.requireNonNull(callback, "callback must not be null");

        final StanzaListener packetListener = new StanzaListener() {
            @Override
            public void processStanza(Stanza packet) throws NotConnectedException, InterruptedException, NotLoggedInException {
                boolean removed = removeAsyncStanzaListener(this);
                if (!removed) {
                    // We lost a race against the "no response" handling runnable. Avoid calling the callback, as the
                    // exception callback will be invoked (if any).
                    return;
                }
                try {
                    XMPPErrorException.ifHasErrorThenThrow(packet);
                    callback.processStanza(packet);
                }
                catch (XMPPErrorException e) {
                    if (exceptionCallback != null) {
                        exceptionCallback.processException(e);
                    }
                }
            }
        };
        schedule(new Runnable() {
            @Override
            public void run() {
                boolean removed = removeAsyncStanzaListener(packetListener);
                // If the packetListener got removed, then it was never run and
                // we never received a response, inform the exception callback
                if (removed && exceptionCallback != null) {
                    Exception exception;
                    if (!isConnected()) {
                        // If the connection is no longer connected, throw a not connected exception.
                        exception = new NotConnectedException(AbstractXMPPConnection.this, replyFilter);
                    } else {
                        exception = NoResponseException.newWith(AbstractXMPPConnection.this, replyFilter);
                    }
                    final Exception exceptionToProcess = exception;
                    Async.go(new Runnable() {
                        @Override
                        public void run() {
                            exceptionCallback.processException(exceptionToProcess);
                        }
                    });
                }
            }
        }, timeout, TimeUnit.MILLISECONDS);
        addAsyncStanzaListener(packetListener, replyFilter);
        sendStanza(stanza);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void sendIqWithResponseCallback(IQ iqRequest, StanzaListener callback)
                    throws NotConnectedException, InterruptedException {
        sendIqWithResponseCallback(iqRequest, callback, null);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void sendIqWithResponseCallback(IQ iqRequest, StanzaListener callback,
                    ExceptionCallback exceptionCallback) throws NotConnectedException, InterruptedException {
        sendIqWithResponseCallback(iqRequest, callback, exceptionCallback, getReplyTimeout());
    }

    @SuppressWarnings("deprecation")
    @Override
    public void sendIqWithResponseCallback(IQ iqRequest, final StanzaListener callback,
                    final ExceptionCallback exceptionCallback, long timeout)
                    throws NotConnectedException, InterruptedException {
        StanzaFilter replyFilter = new IQReplyFilter(iqRequest, this);
        sendStanzaWithResponseCallback(iqRequest, replyFilter, callback, exceptionCallback, timeout);
    }

    @SuppressWarnings("FutureReturnValueIgnored")
    @Override
    public void addOneTimeSyncCallback(final StanzaListener callback, final StanzaFilter packetFilter) {
        final StanzaListener packetListener = new StanzaListener() {
            @Override
            public void processStanza(Stanza packet) throws NotConnectedException, InterruptedException, NotLoggedInException {
                try {
                    callback.processStanza(packet);
                } finally {
                    removeSyncStanzaListener(this);
                }
            }
        };
        addSyncStanzaListener(packetListener, packetFilter);
        schedule(new Runnable() {
            @Override
            public void run() {
                removeSyncStanzaListener(packetListener);
            }
        }, getReplyTimeout(), TimeUnit.MILLISECONDS);
    }

    @Override
    public IQRequestHandler registerIQRequestHandler(final IQRequestHandler iqRequestHandler) {
        final String key = XmppStringUtils.generateKey(iqRequestHandler.getElement(), iqRequestHandler.getNamespace());
        switch (iqRequestHandler.getType()) {
        case set:
            synchronized (setIqRequestHandler) {
                return setIqRequestHandler.put(key, iqRequestHandler);
            }
        case get:
            synchronized (getIqRequestHandler) {
                return getIqRequestHandler.put(key, iqRequestHandler);
            }
        default:
            throw new IllegalArgumentException("Only IQ type of 'get' and 'set' allowed");
        }
    }

    @Override
    public final IQRequestHandler unregisterIQRequestHandler(IQRequestHandler iqRequestHandler) {
        return unregisterIQRequestHandler(iqRequestHandler.getElement(), iqRequestHandler.getNamespace(),
                        iqRequestHandler.getType());
    }

    @Override
    public IQRequestHandler unregisterIQRequestHandler(String element, String namespace, IQ.Type type) {
        final String key = XmppStringUtils.generateKey(element, namespace);
        switch (type) {
        case set:
            synchronized (setIqRequestHandler) {
                return setIqRequestHandler.remove(key);
            }
        case get:
            synchronized (getIqRequestHandler) {
                return getIqRequestHandler.remove(key);
            }
        default:
            throw new IllegalArgumentException("Only IQ type of 'get' and 'set' allowed");
        }
    }

    private long lastStanzaReceived;

    @Override
    public long getLastStanzaReceived() {
        return lastStanzaReceived;
    }

    /**
     * Install a parsing exception callback, which will be invoked once an exception is encountered while parsing a
     * stanza.
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

    @Override
    public final String toString() {
        EntityFullJid localEndpoint = getUser();
        String localEndpointString = (localEndpoint == null ?  "not-authenticated" : localEndpoint.toString());
        return getClass().getSimpleName() + '[' + localEndpointString + "] (" + getConnectionCounter() + ')';
    }

    protected static void asyncGo(Runnable runnable) {
        CACHED_EXECUTOR_SERVICE.execute(runnable);
    }

    protected static ScheduledFuture<?> schedule(Runnable runnable, long delay, TimeUnit unit) {
        return SCHEDULED_EXECUTOR_SERVICE.schedule(runnable, delay, unit);
    }
}
