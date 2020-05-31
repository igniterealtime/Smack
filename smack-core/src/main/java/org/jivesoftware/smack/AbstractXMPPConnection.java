/**
 *
 * Copyright 2009 Jive Software, 2018-2020 Florian Schmaus.
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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLSession;
import javax.xml.namespace.QName;

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
import org.jivesoftware.smack.SmackException.SmackSaslException;
import org.jivesoftware.smack.SmackException.SmackWrappedException;
import org.jivesoftware.smack.SmackFuture.InternalSmackFuture;
import org.jivesoftware.smack.XMPPException.FailedNonzaException;
import org.jivesoftware.smack.XMPPException.StreamErrorException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smack.compress.packet.Compress;
import org.jivesoftware.smack.compression.XMPPInputOutputStream;
import org.jivesoftware.smack.datatypes.UInt16;
import org.jivesoftware.smack.debugger.SmackDebugger;
import org.jivesoftware.smack.debugger.SmackDebuggerFactory;
import org.jivesoftware.smack.filter.IQReplyFilter;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.filter.StanzaIdFilter;
import org.jivesoftware.smack.internal.SmackTlsContext;
import org.jivesoftware.smack.iqrequest.IQRequestHandler;
import org.jivesoftware.smack.packet.Bind;
import org.jivesoftware.smack.packet.ErrorIQ;
import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.FullyQualifiedElement;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Mechanisms;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.MessageBuilder;
import org.jivesoftware.smack.packet.MessageOrPresence;
import org.jivesoftware.smack.packet.MessageOrPresenceBuilder;
import org.jivesoftware.smack.packet.Nonza;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.PresenceBuilder;
import org.jivesoftware.smack.packet.Session;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.packet.StanzaError;
import org.jivesoftware.smack.packet.StanzaFactory;
import org.jivesoftware.smack.packet.StartTls;
import org.jivesoftware.smack.packet.StreamError;
import org.jivesoftware.smack.packet.StreamOpen;
import org.jivesoftware.smack.packet.TopLevelStreamElement;
import org.jivesoftware.smack.packet.XmlEnvironment;
import org.jivesoftware.smack.packet.id.StanzaIdSource;
import org.jivesoftware.smack.parsing.ParsingExceptionCallback;
import org.jivesoftware.smack.parsing.SmackParsingException;
import org.jivesoftware.smack.provider.ExtensionElementProvider;
import org.jivesoftware.smack.provider.NonzaProvider;
import org.jivesoftware.smack.provider.ProviderManager;
import org.jivesoftware.smack.sasl.SASLErrorException;
import org.jivesoftware.smack.sasl.SASLMechanism;
import org.jivesoftware.smack.sasl.core.SASLAnonymous;
import org.jivesoftware.smack.sasl.packet.SaslNonza;
import org.jivesoftware.smack.util.Async;
import org.jivesoftware.smack.util.CollectionUtil;
import org.jivesoftware.smack.util.Consumer;
import org.jivesoftware.smack.util.MultiMap;
import org.jivesoftware.smack.util.Objects;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smack.util.ParserUtils;
import org.jivesoftware.smack.util.Predicate;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.Supplier;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.jxmpp.jid.DomainBareJid;
import org.jxmpp.jid.EntityBareJid;
import org.jxmpp.jid.EntityFullJid;
import org.jxmpp.jid.Jid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Resourcepart;
import org.jxmpp.stringprep.XmppStringprepException;
import org.jxmpp.util.XmppStringUtils;

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

    protected static final SmackReactor SMACK_REACTOR;

    static {
        SMACK_REACTOR = SmackReactor.getInstance();
    }

    /**
     * Counter to uniquely identify connections that are created.
     */
    private static final AtomicInteger connectionCounter = new AtomicInteger(0);

    static {
        // Ensure the SmackConfiguration class is loaded by calling a method in it.
        SmackConfiguration.getVersion();
    }

    protected enum SyncPointState {
        initial,
        request_sent,
        successful,
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

    private final Map<StanzaListener, ListenerWrapper> recvListeners = new LinkedHashMap<>();

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

    private final Map<Consumer<MessageBuilder>, GenericInterceptorWrapper<MessageBuilder, Message>> messageInterceptors = new HashMap<>();

    private final Map<Consumer<PresenceBuilder>, GenericInterceptorWrapper<PresenceBuilder, Presence>> presenceInterceptors = new HashMap<>();

    private XmlEnvironment incomingStreamXmlEnvironment;

    protected XmlEnvironment outgoingStreamXmlEnvironment;

    final MultiMap<QName, NonzaCallback> nonzaCallbacksMap = new MultiMap<>();

    protected final Lock connectionLock = new ReentrantLock();

    protected final Map<QName, FullyQualifiedElement> streamFeatures = new HashMap<>();

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

    protected SmackException currentSmackException;
    protected XMPPException currentXmppException;

    protected boolean tlsHandled;

    /**
     * Set to <code>true</code> if the last features stanza from the server has been parsed. A XMPP connection
     * handshake can invoke multiple features stanzas, e.g. when TLS is activated a second feature
     * stanza is send by the server. This is set to true once the last feature stanza has been
     * parsed.
     */
    protected boolean lastFeaturesReceived;

    /**
     * Set to <code>true</code> if the SASL feature has been received.
     */
    protected boolean saslFeatureReceived;

    /**
     * A synchronization point which is successful if this connection has received the closing
     * stream element from the remote end-point, i.e. the server.
     */
    protected boolean closingStreamReceived;

    /**
     * The SASLAuthentication manager that is responsible for authenticating with the server.
     */
    private final SASLAuthentication saslAuthentication;

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
     * A cached thread pool executor service with custom thread factory to set meaningful names on the threads and set
     * them 'daemon'.
     */
    private static final ExecutorService CACHED_EXECUTOR_SERVICE = Executors.newCachedThreadPool(new ThreadFactory() {
        @Override
        public Thread newThread(Runnable runnable) {
            Thread thread = new Thread(runnable);
            thread.setName("Smack Cached Executor");
            thread.setDaemon(true);
            thread.setUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                @Override
                public void uncaughtException(Thread t, Throwable e) {
                    LOGGER.log(Level.WARNING, t + " encountered uncaught exception", e);
                }
            });
            return thread;
        }
    });

    protected static final AsyncButOrdered<AbstractXMPPConnection> ASYNC_BUT_ORDERED = new AsyncButOrdered<>();

    protected final AsyncButOrdered<StanzaListener> inOrderListeners = new AsyncButOrdered<>();

    /**
     * The used host to establish the connection to
     */
    protected String host;

    /**
     * The used port to establish the connection to
     */
    protected UInt16 port;

    /**
     * Flag that indicates if the user is currently authenticated with the server.
     */
    protected boolean authenticated = false;

    // TODO: Migrate to ZonedDateTime once Smack's minimum required Android SDK level is 26 (8.0, Oreo) or higher.
    protected long authenticatedConnectionInitiallyEstablishedTimestamp;

    /**
     * Flag that indicates if the user was authenticated with the server when the connection
     * to the server was closed (abruptly or not).
     */
    protected boolean wasAuthenticated = false;

    private final Map<QName, IQRequestHandler> setIqRequestHandler = new HashMap<>();
    private final Map<QName, IQRequestHandler> getIqRequestHandler = new HashMap<>();

    private final StanzaFactory stanzaFactory;

    /**
     * Create a new XMPPConnection to an XMPP server.
     *
     * @param configuration The configuration which is used to establish the connection.
     */
    protected AbstractXMPPConnection(ConnectionConfiguration configuration) {
        saslAuthentication = new SASLAuthentication(this, configuration);
        config = configuration;

        // Install the SASL Nonza callbacks.
        buildNonzaCallback()
            .listenFor(SaslNonza.Challenge.class, c -> {
                try {
                    saslAuthentication.challengeReceived(c);
                } catch (SmackException | InterruptedException e) {
                    saslAuthentication.authenticationFailed(e);
                }
            })
            .listenFor(SaslNonza.Success.class, s -> {
                try {
                    saslAuthentication.authenticated(s);
                } catch (SmackSaslException | NotConnectedException | InterruptedException e) {
                    saslAuthentication.authenticationFailed(e);
                }
            })
            .listenFor(SaslNonza.SASLFailure.class, f -> saslAuthentication.authenticationFailed(f))
            .install();

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

        StanzaIdSource stanzaIdSource = configuration.constructStanzaIdSource();
        stanzaFactory = new StanzaFactory(stanzaIdSource);
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
        final UInt16 port = this.port;
        if (port == null) {
            return -1;
        }

        return port.intValue();
    }

    @Override
    public abstract boolean isSecureConnection();

    protected abstract void sendStanzaInternal(Stanza packet) throws NotConnectedException, InterruptedException;

    @Override
    public boolean trySendStanza(Stanza stanza) throws NotConnectedException {
        // Default implementation which falls back to sendStanza() as mentioned in the methods javadoc. May be
        // overwritten by subclasses.
        try {
            sendStanza(stanza);
        } catch (InterruptedException e) {
            LOGGER.log(Level.FINER,
                            "Thread blocked in fallback implementation of trySendStanza(Stanza) was interrupted", e);
            return false;
        }
        return true;
    }

    @Override
    public boolean trySendStanza(Stanza stanza, long timeout, TimeUnit unit)
                    throws NotConnectedException, InterruptedException {
        // Default implementation which falls back to sendStanza() as mentioned in the methods javadoc. May be
        // overwritten by subclasses.
        sendStanza(stanza);
        return true;
    }

    @Override
    public abstract void sendNonza(Nonza element) throws NotConnectedException, InterruptedException;

    @Override
    public abstract boolean isUsingCompression();

    protected void initState() {
        currentSmackException = null;
        currentXmppException = null;
        saslFeatureReceived = lastFeaturesReceived = tlsHandled = false;
        // TODO: We do not init closingStreamReceived here, as the integration tests use it to check if we waited for
        // it.
    }

    /**
     * Establishes a connection to the XMPP server. It basically
     * creates and maintains a connection to the server.
     * <p>
     * Listeners will be preserved from a previous connection.
     * </p>
     *
     * @throws XMPPException if an error occurs on the XMPP protocol level.
     * @throws SmackException if an error occurs somewhere else besides XMPP protocol level.
     * @throws IOException if an I/O error occurred.
     * @return a reference to this object, to chain <code>connect()</code> with <code>login()</code>.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public synchronized AbstractXMPPConnection connect() throws SmackException, IOException, XMPPException, InterruptedException {
        // Check if not already connected
        throwAlreadyConnectedExceptionIfAppropriate();

        // Reset the connection state
        initState();
        closingStreamReceived = false;
        streamId = null;

        try {
            // Perform the actual connection to the XMPP service
            connectInternal();

            // If TLS is required but the server doesn't offer it, disconnect
            // from the server and throw an error. First check if we've already negotiated TLS
            // and are secure, however (features get parsed a second time after TLS is established).
            if (!isSecureConnection() && getConfiguration().getSecurityMode() == SecurityMode.required) {
                throw new SecurityRequiredByClientException();
            }
        } catch (SmackException | IOException | XMPPException | InterruptedException e) {
            instantShutdown();
            throw e;
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
     * @throws SmackException if Smack detected an exceptional situation.
     * @throws IOException if an I/O error occurred.
     * @throws XMPPException if an XMPP protocol error was received.
     * @throws InterruptedException if the calling thread was interrupted.
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
     * @throws InterruptedException if the calling thread was interrupted.
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
     * @param username TODO javadoc me please
     * @param password TODO javadoc me please
     * @throws XMPPException if an XMPP protocol error was received.
     * @throws SmackException if Smack detected an exceptional situation.
     * @throws IOException if an I/O error occurred.
     * @throws InterruptedException if the calling thread was interrupted.
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
     * @param username TODO javadoc me please
     * @param password TODO javadoc me please
     * @param resource TODO javadoc me please
     * @throws XMPPException if an XMPP protocol error was received.
     * @throws SmackException if Smack detected an exceptional situation.
     * @throws IOException if an I/O error occurred.
     * @throws InterruptedException if the calling thread was interrupted.
     * @see #login
     */
    public synchronized void login(CharSequence username, String password, Resourcepart resource) throws XMPPException,
                    SmackException, IOException, InterruptedException {
        if (!config.allowNullOrEmptyUsername) {
            StringUtils.requireNotNullNorEmpty(username, "Username must not be null nor empty");
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

    protected final void throwCurrentConnectionException() throws SmackException, XMPPException {
        if (currentSmackException != null) {
            throw currentSmackException;
        } else if (currentXmppException != null) {
            throw currentXmppException;
        }

        throw new AssertionError("No current connection exception set, although throwCurrentException() was called");
    }

    protected final boolean hasCurrentConnectionException() {
        return currentSmackException != null || currentXmppException != null;
    }

    protected final void setCurrentConnectionExceptionAndNotify(Exception exception) {
        if (exception instanceof SmackException) {
            currentSmackException = (SmackException) exception;
        } else if (exception instanceof XMPPException) {
            currentXmppException = (XMPPException) exception;
        } else {
            currentSmackException = new SmackException.SmackWrappedException(exception);
        }

        notifyWaitingThreads();
    }

    /**
     * We use an extra object for {@link #notifyWaitingThreads()} and {@link #waitForCondition(Supplier)}, because all state
     * changing methods of the connection are synchronized using the connection instance as monitor. If we now would
     * also use the connection instance for the internal process to wait for a condition, the {@link Object#wait()}
     * would leave the monitor when it waites, which would allow for another potential call to a state changing function
     * to proceed.
     */
    private final Object internalMonitor = new Object();

    protected final void notifyWaitingThreads() {
        synchronized (internalMonitor) {
            internalMonitor.notifyAll();
        }
    }

    protected final boolean waitForCondition(Supplier<Boolean> condition) throws InterruptedException {
        final long deadline = System.currentTimeMillis() + getReplyTimeout();
        synchronized (internalMonitor) {
            while (!condition.get().booleanValue() && !hasCurrentConnectionException()) {
                final long now = System.currentTimeMillis();
                if (now >= deadline) {
                    return false;
                }
                internalMonitor.wait(deadline - now);
            }
        }
        return true;
    }

    protected final void waitForCondition(Supplier<Boolean> condition, String waitFor) throws InterruptedException, NoResponseException {
        boolean success = waitForCondition(condition);
        if (!success) {
            throw NoResponseException.newWith(this, waitFor);
        }
    }

    protected final void waitForConditionOrThrowConnectionException(Supplier<Boolean> condition, String waitFor) throws InterruptedException, SmackException, XMPPException {
        waitForCondition(condition, waitFor);
        if (hasCurrentConnectionException()) {
            throwCurrentConnectionException();
        }
    }

    protected Resourcepart bindResourceAndEstablishSession(Resourcepart resource)
                    throws SmackException, InterruptedException, XMPPException {
        // Wait until either:
        // - the servers last features stanza has been parsed
        // - the timeout occurs
        LOGGER.finer("Waiting for last features to be received before continuing with resource binding");
        waitForConditionOrThrowConnectionException(() -> lastFeaturesReceived, "last stream features received from server");

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

        return response.getJid().getResourcepart();
    }

    protected void afterSuccessfulLogin(final boolean resumed) throws NotConnectedException, InterruptedException {
        if (!resumed) {
            authenticatedConnectionInitiallyEstablishedTimestamp = System.currentTimeMillis();
        }
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
            Presence availablePresence = getStanzaFactory()
                            .buildPresenceStanza()
                            .ofType(Presence.Type.available)
                            .build();
            sendStanza(availablePresence);
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
    public final StanzaFactory getStanzaFactory() {
        return stanzaFactory;
    }

    @Override
    public final void sendStanza(Stanza stanza) throws NotConnectedException, InterruptedException {
        Objects.requireNonNull(stanza, "Stanza must not be null");
        assert stanza instanceof Message || stanza instanceof Presence || stanza instanceof IQ;

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
        Stanza stanzaAfterInterceptors = firePacketInterceptors(stanza);
        sendStanzaInternal(stanzaAfterInterceptors);
    }

    /**
     * Authenticate a connection.
     *
     * @param username the username that is authenticating with the server.
     * @param password the password to send to the server.
     * @param authzid the authorization identifier (typically null).
     * @param sslSession the optional SSL/TLS session (if one was established)
     * @return the used SASLMechanism.
     * @throws XMPPErrorException if there was an XMPP error returned.
     * @throws SASLErrorException if a SASL protocol error was returned.
     * @throws IOException if an I/O error occurred.
     * @throws InterruptedException if the calling thread was interrupted.
     * @throws SmackSaslException if a SASL specific error occurred.
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws NoResponseException if there was no response from the remote entity.
     * @throws SmackWrappedException in case of an exception.
     * @see SASLAuthentication#authenticate(String, String, EntityBareJid, SSLSession)
     */
    protected final SASLMechanism authenticate(String username, String password, EntityBareJid authzid,
                    SSLSession sslSession) throws XMPPErrorException, SASLErrorException, SmackSaslException,
                    NotConnectedException, NoResponseException, IOException, InterruptedException, SmackWrappedException {
        SASLMechanism saslMechanism = saslAuthentication.authenticate(username, password, authzid, sslSession);
        afterSaslAuthenticationSuccess();
        return saslMechanism;
    }

    /**
     * Hook for subclasses right after successful SASL authentication. RFC 6120 § 6.4.6. specifies a that the initiating
     * entity, needs to initiate a new stream in this case. But some transports, like BOSH, requires a special handling.
     * <p>
     * Note that we can not reset XMPPTCPConnection's parser here, because this method is invoked by the thread calling
     * {@link #login()}, but the parser reset has to be done within the reader thread.
     * </p>
     *
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     * @throws SmackWrappedException in case of an exception.
     */
    protected void afterSaslAuthenticationSuccess()
                    throws NotConnectedException, InterruptedException, SmackWrappedException {
        sendStreamOpen();
    }

    protected final boolean isSaslAuthenticated() {
        return saslAuthentication.authenticationSuccessful();
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
            unavailablePresence = getStanzaFactory().buildPresenceStanza()
                            .ofType(Presence.Type.unavailable)
                            .build();
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
     * @throws NotConnectedException if the XMPP connection is not connected.
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

    private final Object notifyConnectionErrorMonitor = new Object();

    /**
     * Sends out a notification that there was an error with the connection
     * and closes the connection.
     *
     * @param exception the exception that causes the connection close event.
     */
    protected final void notifyConnectionError(final Exception exception) {
        synchronized (notifyConnectionErrorMonitor) {
            if (!isConnected()) {
                LOGGER.log(Level.INFO, "Connection was already disconnected when attempting to handle " + exception,
                                exception);
                return;
            }

            // Note that we first have to set the current connection exception and notify waiting threads, as one of them
            // could hold the instance lock, which we also need later when calling instantShutdown().
            setCurrentConnectionExceptionAndNotify(exception);

            // Closes the connection temporary. A if the connection supports stream management, then a reconnection is
            // possible. Note that a connection listener of e.g. XMPPTCPConnection will drop the SM state in
            // case the Exception is a StreamErrorException.
            instantShutdown();

            for (StanzaCollector collector : collectors) {
                collector.notifyConnectionError(exception);
            }

            Async.go(() -> {
                // Notify connection listeners of the error.
                callConnectionClosedOnErrorListener(exception);
            }, AbstractXMPPConnection.this + " callConnectionClosedOnErrorListener()");
        }
    }

    /**
     * Performs an unclean disconnect and shutdown of the connection. Does not send a closing stream stanza.
     */
    public abstract void instantShutdown();

    /**
     * Shuts the current connection down.
     */
    protected abstract void shutdown();

    protected final boolean waitForClosingStreamTagFromServer() {
        try {
            waitForConditionOrThrowConnectionException(() -> closingStreamReceived, "closing stream tag from the server");
        } catch (InterruptedException | SmackException | XMPPException e) {
            LOGGER.log(Level.INFO, "Exception while waiting for closing stream element from the server " + this, e);
            return false;
        }
        return true;
    }

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
    public final void addStanzaListener(StanzaListener stanzaListener, StanzaFilter stanzaFilter) {
        if (stanzaListener == null) {
            throw new NullPointerException("Given stanza listener must not be null");
        }
        ListenerWrapper wrapper = new ListenerWrapper(stanzaListener, stanzaFilter);
        synchronized (recvListeners) {
            recvListeners.put(stanzaListener, wrapper);
        }
    }

    @Override
    public final boolean removeStanzaListener(StanzaListener stanzaListener) {
        synchronized (recvListeners) {
            return recvListeners.remove(stanzaListener) != null;
        }
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

    @Override
    public void removeStanzaSendingListener(StanzaListener packetListener) {
        synchronized (sendListeners) {
            sendListeners.remove(packetListener);
        }
    }

    /**
     * Process all stanza listeners for sending stanzas.
     * <p>
     * Compared to {@link #firePacketInterceptors(Stanza)}, the listeners will be invoked in a new thread.
     * </p>
     *
     * @param sendTopLevelStreamElement the top level stream element which just got send.
     */
    // TODO: Rename to fireElementSendingListeners().
    @SuppressWarnings("javadoc")
    protected void firePacketSendingListeners(final TopLevelStreamElement sendTopLevelStreamElement) {
        if (debugger != null) {
            debugger.onOutgoingStreamElement(sendTopLevelStreamElement);
        }

        if (!(sendTopLevelStreamElement instanceof Stanza)) {
            return;
        }
        Stanza packet = (Stanza) sendTopLevelStreamElement;

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
    public void removeStanzaInterceptor(StanzaListener packetInterceptor) {
        synchronized (interceptors) {
            interceptors.remove(packetInterceptor);
        }
    }

    private static <MPB extends MessageOrPresenceBuilder<MP, MPB>, MP extends MessageOrPresence<MPB>> void addInterceptor(
                    Map<Consumer<MPB>, GenericInterceptorWrapper<MPB, MP>> interceptors, Consumer<MPB> interceptor,
                    Predicate<MP> filter) {
        Objects.requireNonNull(interceptor, "Interceptor must not be null");

        GenericInterceptorWrapper<MPB, MP> interceptorWrapper = new GenericInterceptorWrapper<>(interceptor, filter);

        synchronized (interceptors) {
            interceptors.put(interceptor, interceptorWrapper);
        }
    }

    private static <MPB extends MessageOrPresenceBuilder<MP, MPB>, MP extends MessageOrPresence<MPB>> void removeInterceptor(
                    Map<Consumer<MPB>, GenericInterceptorWrapper<MPB, MP>> interceptors, Consumer<MPB> interceptor) {
        synchronized (interceptors) {
            interceptors.remove(interceptor);
        }
    }

    @Override
    public void addMessageInterceptor(Consumer<MessageBuilder> messageInterceptor, Predicate<Message> messageFilter) {
        addInterceptor(messageInterceptors, messageInterceptor, messageFilter);
    }

    @Override
    public void removeMessageInterceptor(Consumer<MessageBuilder> messageInterceptor) {
        removeInterceptor(messageInterceptors, messageInterceptor);
    }

    @Override
    public void addPresenceInterceptor(Consumer<PresenceBuilder> presenceInterceptor,
                    Predicate<Presence> presenceFilter) {
        addInterceptor(presenceInterceptors, presenceInterceptor, presenceFilter);
    }

    @Override
    public void removePresenceInterceptor(Consumer<PresenceBuilder> presenceInterceptor) {
        removeInterceptor(presenceInterceptors, presenceInterceptor);
    }

    private static <MPB extends MessageOrPresenceBuilder<MP, MPB>, MP extends MessageOrPresence<MPB>> MP fireMessageOrPresenceInterceptors(
                    MP messageOrPresence, Map<Consumer<MPB>, GenericInterceptorWrapper<MPB, MP>> interceptors) {
        List<Consumer<MPB>> interceptorsToInvoke = new LinkedList<>();
        synchronized (interceptors) {
            for (GenericInterceptorWrapper<MPB, MP> interceptorWrapper : interceptors.values()) {
                if (interceptorWrapper.filterMatches(messageOrPresence)) {
                    Consumer<MPB> interceptor = interceptorWrapper.getInterceptor();
                    interceptorsToInvoke.add(interceptor);
                }
            }
        }

        // Avoid transforming the stanza to a builder if there is no interceptor.
        if (interceptorsToInvoke.isEmpty()) {
            return messageOrPresence;
        }

        MPB builder = messageOrPresence.asBuilder();
        for (Consumer<MPB> interceptor : interceptorsToInvoke) {
            interceptor.accept(builder);
        }

        // Now that the interceptors have (probably) modified the stanza in its builder form, we need to re-assemble it.
        messageOrPresence = builder.build();
        return messageOrPresence;
    }

    /**
     * Process interceptors. Interceptors may modify the stanza that is about to be sent.
     * Since the thread that requested to send the stanza will invoke all interceptors, it
     * is important that interceptors perform their work as soon as possible so that the
     * thread does not remain blocked for a long period.
     *
     * @param packet the stanza that is going to be sent to the server.
     * @return the, potentially modified stanza, after the interceptors are run.
     */
    private Stanza firePacketInterceptors(Stanza packet) {
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

        final Stanza stanzaAfterInterceptors;
        if (packet instanceof Message) {
            Message message = (Message) packet;
            stanzaAfterInterceptors = fireMessageOrPresenceInterceptors(message, messageInterceptors);
        }
        else if (packet instanceof Presence) {
            Presence presence = (Presence) packet;
            stanzaAfterInterceptors = fireMessageOrPresenceInterceptors(presence, presenceInterceptors);
        } else {
            // We do not (yet) support interceptors for IQ stanzas.
            assert packet instanceof IQ;
            stanzaAfterInterceptors = packet;
        }

        return stanzaAfterInterceptors;
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
        if (Long.MAX_VALUE - System.currentTimeMillis() < timeout) {
            throw new IllegalArgumentException("Extremely long reply timeout");
        }
        else {
            replyTimeout = timeout;
        }
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

    protected final NonzaCallback.Builder buildNonzaCallback() {
        return new NonzaCallback.Builder(this);
    }

    protected <SN extends Nonza, FN extends Nonza> SN sendAndWaitForResponse(Nonza nonza, Class<SN> successNonzaClass,
                    Class<FN> failedNonzaClass)
                    throws NoResponseException, NotConnectedException, InterruptedException, FailedNonzaException {
        NonzaCallback.Builder builder = buildNonzaCallback();
        SN successNonza = NonzaCallback.sendAndWaitForResponse(builder, nonza, successNonzaClass, failedNonzaClass);
        return successNonza;
    }

    protected final void parseAndProcessNonza(XmlPullParser parser) throws IOException, XmlPullParserException, SmackParsingException {
        ParserUtils.assertAtStartTag(parser);

        final int initialDepth = parser.getDepth();
        final String element = parser.getName();
        final String namespace = parser.getNamespace();
        final QName key = new QName(namespace, element);

        NonzaProvider<? extends Nonza> nonzaProvider = ProviderManager.getNonzaProvider(key);
        if (nonzaProvider == null) {
            LOGGER.severe("Unknown nonza: " + key);
            ParserUtils.forwardToEndTagOfDepth(parser, initialDepth);
            return;
        }

        List<NonzaCallback> nonzaCallbacks;
        synchronized (nonzaCallbacksMap) {
            nonzaCallbacks = nonzaCallbacksMap.getAll(key);
            nonzaCallbacks = CollectionUtil.newListWith(nonzaCallbacks);
        }
        if (nonzaCallbacks == null) {
            LOGGER.info("No nonza callback for " + key);
            ParserUtils.forwardToEndTagOfDepth(parser, initialDepth);
            return;
        }

        Nonza nonza = nonzaProvider.parse(parser, incomingStreamXmlEnvironment);

        for (NonzaCallback nonzaCallback : nonzaCallbacks) {
            nonzaCallback.onNonzaReceived(nonza);
        }
    }

    protected void parseAndProcessStanza(XmlPullParser parser)
                    throws XmlPullParserException, IOException, InterruptedException {
        ParserUtils.assertAtStartTag(parser);
        int parserDepth = parser.getDepth();
        Stanza stanza = null;
        try {
            stanza = PacketParserUtils.parseStanza(parser, incomingStreamXmlEnvironment);
        }
        catch (XmlPullParserException | SmackParsingException | IOException | IllegalArgumentException e) {
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
     * @throws InterruptedException if the calling thread was interrupted.
     */
    protected void processStanza(final Stanza stanza) throws InterruptedException {
        assert stanza != null;

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
                final QName key = iqRequest.getChildElementQName();
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
                    final ErrorIQ errorIQ = IQ.createErrorResponse(iq, StanzaError.getBuilder(
                                    replyCondition).build());
                    // Use async sendStanza() here, since if sendStanza() would block, then some connections, e.g.
                    // XmppNioTcpConnection, would deadlock, as this operation is performed in the same thread that is
                    asyncGo(() -> {
                        try {
                            sendStanza(errorIQ);
                        }
                        catch (InterruptedException | NotConnectedException e) {
                            LOGGER.log(Level.WARNING, "Exception while sending error IQ to unkown IQ request", e);
                        }
                    });
                } else {
                    Executor executorService = null;
                    switch (iqRequestHandler.getMode()) {
                    case sync:
                        executorService = ASYNC_BUT_ORDERED.asExecutorFor(this);
                        break;
                    case async:
                        executorService = this::asyncGoLimited;
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

                            assert response.isResponseIQ();

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
        extractMatchingListeners(packet, asyncRecvListeners, listenersToNotify);
        for (final StanzaListener listener : listenersToNotify) {
            asyncGoLimited(new Runnable() {
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

        listenersToNotify.clear();
        extractMatchingListeners(packet, recvListeners, listenersToNotify);
        for (StanzaListener stanzaListener : listenersToNotify) {
            inOrderListeners.performAsyncButOrdered(stanzaListener, () -> {
                try {
                    stanzaListener.processStanza(packet);
                }
                catch (NotConnectedException e) {
                    LOGGER.log(Level.WARNING, "Got not connected exception, aborting", e);
                }
                catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "Exception in packet listener", e);
                }
            });
        }

        // Notify the receive listeners interested in the packet
        listenersToNotify.clear();
        extractMatchingListeners(packet, syncRecvListeners, listenersToNotify);
        // Decouple incoming stanza processing from listener invocation. Unlike async listeners, this uses a single
        // threaded executor service and therefore keeps the order.
        ASYNC_BUT_ORDERED.performAsyncButOrdered(this, new Runnable() {
            @Override
            public void run() {
                // As listeners are able to remove themselves and because the timepoint where it is decided to invoke a
                // listener is a different timepoint where the listener is actually invoked (here), we have to check
                // again if the listener is still active.
                Iterator<StanzaListener> it = listenersToNotify.iterator();
                synchronized (syncRecvListeners) {
                    while (it.hasNext()) {
                        StanzaListener stanzaListener = it.next();
                        if (!syncRecvListeners.containsKey(stanzaListener)) {
                            // The listener was removed from syncRecvListener, also remove him from listenersToNotify.
                            it.remove();
                        }
                    }
                }
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

    private static void extractMatchingListeners(Stanza stanza, Map<StanzaListener, ListenerWrapper> listeners,
                    Collection<StanzaListener> listenersToNotify) {
        synchronized (listeners) {
            for (ListenerWrapper listenerWrapper : listeners.values()) {
                if (listenerWrapper.filterMatches(stanza)) {
                    listenersToNotify.add(listenerWrapper.getListener());
                }
            }
        }
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

    private void callConnectionClosedOnErrorListener(Exception e) {
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
    @Deprecated
    // TODO: Remove once addStanzaInterceptor is gone.
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

    private static final class GenericInterceptorWrapper<MPB extends MessageOrPresenceBuilder<MP, MPB>, MP extends MessageOrPresence<MPB>> {
        private final Consumer<MPB> stanzaInterceptor;
        private final Predicate<MP> stanzaFilter;

        private GenericInterceptorWrapper(Consumer<MPB> stanzaInterceptor, Predicate<MP> stanzaFilter) {
            this.stanzaInterceptor = stanzaInterceptor;
            this.stanzaFilter = stanzaFilter;
        }

        private boolean filterMatches(MP stanza) {
            return stanzaFilter == null || stanzaFilter.test(stanza);
        }

        public Consumer<MPB> getInterceptor() {
            return stanzaInterceptor;
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

    protected final void parseFeatures(XmlPullParser parser) throws XmlPullParserException, IOException, SmackParsingException {
        streamFeatures.clear();
        final int initialDepth = parser.getDepth();
        while (true) {
            XmlPullParser.Event eventType = parser.next();

            if (eventType == XmlPullParser.Event.START_ELEMENT && parser.getDepth() == initialDepth + 1) {
                FullyQualifiedElement streamFeature = null;
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
                        streamFeature = provider.parse(parser, incomingStreamXmlEnvironment);
                    }
                    break;
                }
                if (streamFeature != null) {
                    addStreamFeature(streamFeature);
                }
            }
            else if (eventType == XmlPullParser.Event.END_ELEMENT && parser.getDepth() == initialDepth) {
                break;
            }
        }
    }

    protected final void parseFeaturesAndNotify(XmlPullParser parser) throws Exception {
        parseFeatures(parser);

        if (hasFeature(Mechanisms.ELEMENT, Mechanisms.NAMESPACE)) {
            // Only proceed with SASL auth if TLS is disabled or if the server doesn't announce it
            if (!hasFeature(StartTls.ELEMENT, StartTls.NAMESPACE)
                            || config.getSecurityMode() == SecurityMode.disabled) {
                tlsHandled = saslFeatureReceived = true;
                notifyWaitingThreads();
            }
        }

        // If the server reported the bind feature then we are that that we did SASL and maybe
        // STARTTLS. We can then report that the last 'stream:features' have been parsed
        if (hasFeature(Bind.ELEMENT, Bind.NAMESPACE)) {
            if (!hasFeature(Compress.Feature.ELEMENT, Compress.NAMESPACE)
                            || !config.isCompressionEnabled()) {
                // This was was last features from the server is either it did not contain
                // compression or if we disabled it
                lastFeaturesReceived = true;
                notifyWaitingThreads();
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
    public <F extends FullyQualifiedElement> F getFeature(String element, String namespace) {
        QName qname = new QName(namespace, element);
        return (F) streamFeatures.get(qname);
    }

    @Override
    public boolean hasFeature(String element, String namespace) {
        return getFeature(element, namespace) != null;
    }

    protected void addStreamFeature(FullyQualifiedElement feature) {
        QName key = feature.getQName();
        streamFeatures.put(key, feature);
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
        final QName key = iqRequestHandler.getQName();
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
        final QName key = new QName(namespace, element);
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
     * Get the timestamp when the connection was the first time authenticated, i.e., when the first successful login was
     * performed. Note that this value is not reset on disconnect, so it represents the timestamp from the last
     * authenticated connection. The value is also not reset on stream resumption.
     *
     * @return the timestamp or {@code null}.
     * @since 4.3.3
     */
    public final long getAuthenticatedConnectionInitiallyEstablishedTimestamp() {
        return authenticatedConnectionInitiallyEstablishedTimestamp;
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
        String localEndpointString = localEndpoint == null ?  "not-authenticated" : localEndpoint.toString();
        return getClass().getSimpleName() + '[' + localEndpointString + "] (" + getConnectionCounter() + ')';
    }

    /**
     * A queue of deferred runnables that where not executed immediately because {@link #currentAsyncRunnables} reached
     * {@link #maxAsyncRunnables}. Note that we use a {@code LinkedList} in order to avoid space blowups in case the
     * list ever becomes very big and shrinks again.
     */
    private final Queue<Runnable> deferredAsyncRunnables = new LinkedList<>();

    private int deferredAsyncRunnablesCount;

    private int deferredAsyncRunnablesCountPrevious;

    private int maxAsyncRunnables = SmackConfiguration.getDefaultConcurrencyLevelLimit();

    private int currentAsyncRunnables;

    protected void asyncGoLimited(final Runnable runnable) {
        Runnable wrappedRunnable = new Runnable() {
            @Override
            public void run() {
                runnable.run();

                synchronized (deferredAsyncRunnables) {
                    Runnable defferredRunnable = deferredAsyncRunnables.poll();
                    if (defferredRunnable == null) {
                        currentAsyncRunnables--;
                    } else {
                        deferredAsyncRunnablesCount--;
                        asyncGo(defferredRunnable);
                    }
                }
            }
        };

        synchronized (deferredAsyncRunnables) {
            if (currentAsyncRunnables < maxAsyncRunnables) {
                currentAsyncRunnables++;
                asyncGo(wrappedRunnable);
            } else {
                deferredAsyncRunnablesCount++;
                deferredAsyncRunnables.add(wrappedRunnable);
            }

            final int HIGH_WATERMARK = 100;
            final int INFORM_WATERMARK = 20;

            final int deferredAsyncRunnablesCount = this.deferredAsyncRunnablesCount;

            if (deferredAsyncRunnablesCount >= HIGH_WATERMARK
                    && deferredAsyncRunnablesCountPrevious < HIGH_WATERMARK) {
                LOGGER.log(Level.WARNING, "High watermark of " + HIGH_WATERMARK + " simultaneous executing runnables reached");
            } else if (deferredAsyncRunnablesCount >= INFORM_WATERMARK
                    && deferredAsyncRunnablesCountPrevious < INFORM_WATERMARK) {
                LOGGER.log(Level.INFO, INFORM_WATERMARK + " simultaneous executing runnables reached");
            }

            deferredAsyncRunnablesCountPrevious = deferredAsyncRunnablesCount;
        }
    }

    public void setMaxAsyncOperations(int maxAsyncOperations) {
        if (maxAsyncOperations < 1) {
            throw new IllegalArgumentException("Max async operations must be greater than 0");
        }

        synchronized (deferredAsyncRunnables) {
            maxAsyncRunnables = maxAsyncOperations;
        }
    }

    protected static void asyncGo(Runnable runnable) {
        CACHED_EXECUTOR_SERVICE.execute(runnable);
    }

    @SuppressWarnings("static-method")
    protected final SmackReactor getReactor() {
        return SMACK_REACTOR;
    }

    protected static ScheduledAction schedule(Runnable runnable, long delay, TimeUnit unit) {
        return SMACK_REACTOR.schedule(runnable, delay, unit, ScheduledAction.Kind.NonBlocking);
    }

    protected void onStreamOpen(XmlPullParser parser) {
        // We found an opening stream.
        if ("jabber:client".equals(parser.getNamespace(null))) {
            streamId = parser.getAttributeValue("", "id");
            incomingStreamXmlEnvironment = XmlEnvironment.from(parser);

            String reportedServerDomainString = parser.getAttributeValue("", "from");
            if (reportedServerDomainString == null) {
                // RFC 6120 § 4.7.1. makes no explicit statement whether or not 'from' in the stream open from the server
                // in c2s connections is required or not.
                return;
            }
            DomainBareJid reportedServerDomain;
            try {
                reportedServerDomain = JidCreate.domainBareFrom(reportedServerDomainString);
                DomainBareJid configuredXmppServiceDomain = config.getXMPPServiceDomain();
                if (!configuredXmppServiceDomain.equals(reportedServerDomain)) {
                    LOGGER.warning("Domain reported by server '" + reportedServerDomain
                            + "' does not match configured domain '" + configuredXmppServiceDomain + "'");
                }
            } catch (XmppStringprepException e) {
                LOGGER.log(Level.WARNING, "XMPP service domain '" + reportedServerDomainString
                        + "' as reported by server could not be transformed to a valid JID", e);
            }
        }
    }

    protected void sendStreamOpen() throws NotConnectedException, InterruptedException {
        // If possible, provide the receiving entity of the stream open tag, i.e. the server, as much information as
        // possible. The 'to' attribute is *always* available. The 'from' attribute if set by the user and no external
        // mechanism is used to determine the local entity (user). And the 'id' attribute is available after the first
        // response from the server (see e.g. RFC 6120 § 9.1.1 Step 2.)
        CharSequence to = getXMPPServiceDomain();
        CharSequence from = null;
        CharSequence localpart = config.getUsername();
        if (localpart != null) {
            from = XmppStringUtils.completeJidFrom(localpart, to);
        }
        String id = getStreamId();

        StreamOpen streamOpen = new StreamOpen(to, from, id, config.getXmlLang(), StreamOpen.StreamContentNamespace.client);
        sendNonza(streamOpen);

        XmlEnvironment.Builder xmlEnvironmentBuilder = XmlEnvironment.builder();
        xmlEnvironmentBuilder.with(streamOpen);
        outgoingStreamXmlEnvironment = xmlEnvironmentBuilder.build();
    }

    protected final SmackTlsContext getSmackTlsContext() {
        return config.smackTlsContext;
    }
}
