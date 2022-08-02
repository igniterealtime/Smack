/**
 *
 * Copyright 2003-2007 Jive Software.
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
package org.jivesoftware.smack.tcp;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.SocketFactory;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.AlreadyConnectedException;
import org.jivesoftware.smack.SmackException.AlreadyLoggedInException;
import org.jivesoftware.smack.SmackException.ConnectionException;
import org.jivesoftware.smack.SmackException.EndpointConnectionException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.SmackException.NotLoggedInException;
import org.jivesoftware.smack.SmackException.SecurityNotPossibleException;
import org.jivesoftware.smack.SmackException.SecurityRequiredByServerException;
import org.jivesoftware.smack.SmackFuture;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.XMPPException.FailedNonzaException;
import org.jivesoftware.smack.XMPPException.StreamErrorException;
import org.jivesoftware.smack.compress.packet.Compress;
import org.jivesoftware.smack.compress.packet.Compressed;
import org.jivesoftware.smack.compression.XMPPInputOutputStream;
import org.jivesoftware.smack.datatypes.UInt16;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.internal.SmackTlsContext;
import org.jivesoftware.smack.packet.Element;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Nonza;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.packet.StartTls;
import org.jivesoftware.smack.packet.StreamError;
import org.jivesoftware.smack.proxy.ProxyInfo;
import org.jivesoftware.smack.sasl.packet.SaslNonza;
import org.jivesoftware.smack.sm.SMUtils;
import org.jivesoftware.smack.sm.StreamManagementException;
import org.jivesoftware.smack.sm.StreamManagementException.StreamIdDoesNotMatchException;
import org.jivesoftware.smack.sm.StreamManagementException.StreamManagementCounterError;
import org.jivesoftware.smack.sm.StreamManagementException.StreamManagementNotEnabledException;
import org.jivesoftware.smack.sm.packet.StreamManagement;
import org.jivesoftware.smack.sm.packet.StreamManagement.AckAnswer;
import org.jivesoftware.smack.sm.packet.StreamManagement.AckRequest;
import org.jivesoftware.smack.sm.packet.StreamManagement.Enable;
import org.jivesoftware.smack.sm.packet.StreamManagement.Enabled;
import org.jivesoftware.smack.sm.packet.StreamManagement.Failed;
import org.jivesoftware.smack.sm.packet.StreamManagement.Resume;
import org.jivesoftware.smack.sm.packet.StreamManagement.Resumed;
import org.jivesoftware.smack.sm.packet.StreamManagement.StreamManagementFeature;
import org.jivesoftware.smack.sm.predicates.Predicate;
import org.jivesoftware.smack.sm.provider.ParseStreamManagement;
import org.jivesoftware.smack.tcp.rce.RemoteXmppTcpConnectionEndpoints;
import org.jivesoftware.smack.tcp.rce.Rfc6120TcpRemoteConnectionEndpoint;
import org.jivesoftware.smack.util.ArrayBlockingQueueWithShutdown;
import org.jivesoftware.smack.util.Async;
import org.jivesoftware.smack.util.CloseableUtil;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.TLSUtils;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jivesoftware.smack.util.rce.RemoteConnectionException;
import org.jivesoftware.smack.xml.SmackXmlParser;
import org.jivesoftware.smack.xml.XmlPullParser;
import org.jivesoftware.smack.xml.XmlPullParserException;

import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Resourcepart;
import org.jxmpp.stringprep.XmppStringprepException;
import org.minidns.dnsname.DnsName;

/**
 * Creates a socket connection to an XMPP server. This is the default connection
 * to an XMPP server and is specified in the XMPP Core (RFC 6120).
 *
 * @see XMPPConnection
 * @author Matt Tucker
 */
public class XMPPTCPConnection extends AbstractXMPPConnection {

    private static final int QUEUE_SIZE = 500;
    private static final Logger LOGGER = Logger.getLogger(XMPPTCPConnection.class.getName());

    /**
     * The socket which is used for this connection.
     */
    private Socket socket;

    /**
     *
     */
    private boolean disconnectedButResumeable = false;

    private SSLSocket secureSocket;

    /**
     * Protected access level because of unit test purposes
     */
    protected final PacketWriter packetWriter = new PacketWriter();

    /**
     * Protected access level because of unit test purposes
     */
    protected final PacketReader packetReader = new PacketReader();

    /**
     *
     */
    private boolean streamFeaturesAfterAuthenticationReceived;

    /**
     *
     */
    private boolean compressSyncPoint;

    /**
     * The default bundle and defer callback, used for new connections.
     * @see bundleAndDeferCallback
     */
    private static BundleAndDeferCallback defaultBundleAndDeferCallback;

    /**
     * The used bundle and defer callback.
     * <p>
     * Although this field may be set concurrently, the 'volatile' keyword was deliberately not added, in order to avoid
     * having a 'volatile' read within the writer threads loop.
     * </p>
     */
    private BundleAndDeferCallback bundleAndDeferCallback = defaultBundleAndDeferCallback;

    private static boolean useSmDefault = true;

    private static boolean useSmResumptionDefault = true;

    /**
     * The stream ID of the stream that is currently resumable, ie. the stream we hold the state
     * for in {@link #clientHandledStanzasCount}, {@link #serverHandledStanzasCount} and
     * {@link #unFailedNonzaExceptionacknowledgedStanzas}.
     */
    private String smSessionId;

    /**
     * Represents the state of stream management resumption.
     * <p>
     * Unlike other sync points, this sync point is marked volatile because it is also read by the reader thread.
     * </p>
     */
    private volatile SyncPointState smResumedSyncPoint;
    private Failed smResumptionFailed;

    /**
     * Represents the state of stream magement.
     * <p>
     * This boolean is marked volatile as it is read by various threads, including the reader thread via {@link #isSmEnabled()}.
     * </p>
     */
    private volatile boolean smEnabledSyncPoint;

    /**
     * The client's preferred maximum resumption time in seconds.
     */
    private int smClientMaxResumptionTime = -1;

    /**
     * The server's preferred maximum resumption time in seconds.
     */
    private int smServerMaxResumptionTime = -1;

    /**
     * Indicates whether Stream Management (XEP-198) should be used if it's supported by the server.
     */
    private boolean useSm = useSmDefault;
    private boolean useSmResumption = useSmResumptionDefault;

    /**
     * The counter that the server sends the client about it's current height. For example, if the server sends
     * {@code <a h='42'/>}, then this will be set to 42 (while also handling the {@link #unacknowledgedStanzas} queue).
     */
    private long serverHandledStanzasCount = 0;

    /**
     * The counter for stanzas handled ("received") by the client.
     * <p>
     * Note that we don't need to synchronize this counter. Although JLS 17.7 states that reads and writes to longs are
     * not atomic, it guarantees that there are at most 2 separate writes, one to each 32-bit half. And since
     * {@link SMUtils#incrementHeight(long)} masks the lower 32 bit, we only operate on one half of the long and
     * therefore have no concurrency problem because the read/write operations on one half are guaranteed to be atomic.
     * </p>
     */
    private long clientHandledStanzasCount = 0;

    private BlockingQueue<Stanza> unacknowledgedStanzas;

    /**
     * Set to true if Stream Management was at least once enabled for this connection.
     */
    private boolean smWasEnabledAtLeastOnce = false;

    /**
     * This listeners are invoked for every stanza that got acknowledged.
     * <p>
     * We use a {@link ConcurrentLinkedQueue} here in order to allow the listeners to remove
     * themselves after they have been invoked.
     * </p>
     */
    private final Collection<StanzaListener> stanzaAcknowledgedListeners = new ConcurrentLinkedQueue<>();

    /**
     * These listeners are invoked for every stanza that got dropped.
     * <p>
     * We use a {@link ConcurrentLinkedQueue} here in order to allow the listeners to remove
     * themselves after they have been invoked.
     * </p>
     */
    private final Collection<StanzaListener> stanzaDroppedListeners = new ConcurrentLinkedQueue<>();

    /**
     * This listeners are invoked for a acknowledged stanza that has the given stanza ID. They will
     * only be invoked once and automatically removed after that.
     */
    private final Map<String, StanzaListener> stanzaIdAcknowledgedListeners = new ConcurrentHashMap<>();

    /**
     * Predicates that determine if an stream management ack should be requested from the server.
     * <p>
     * We use a linked hash set here, so that the order how the predicates are added matches the
     * order in which they are invoked in order to determine if an ack request should be send or not.
     * </p>
     */
    private final Set<StanzaFilter> requestAckPredicates = new LinkedHashSet<>();

    @SuppressWarnings("HidingField")
    private final XMPPTCPConnectionConfiguration config;

    /**
     * Creates a new XMPP connection over TCP (optionally using proxies).
     * <p>
     * Note that XMPPTCPConnection constructors do not establish a connection to the server
     * and you must call {@link #connect()}.
     * </p>
     *
     * @param config the connection configuration.
     */
    public XMPPTCPConnection(XMPPTCPConnectionConfiguration config) {
        super(config);
        this.config = config;
        addConnectionListener(new ConnectionListener() {
            @Override
            public void connectionClosedOnError(Exception e) {
                if (e instanceof XMPPException.StreamErrorException || e instanceof StreamManagementException) {
                    dropSmState();
                }
            }
        });

        // Re-init the reader and writer in case of SASL <success/>. This is done to reset the parser since a new stream
        // is initiated.
        buildNonzaCallback().listenFor(SaslNonza.Success.class, s -> resetParser()).install();
    }

    /**
     * Creates a new XMPP connection over TCP.
     * <p>
     * Note that {@code jid} must be the bare JID, e.g. "user@example.org". More fine-grained control over the
     * connection settings is available using the {@link #XMPPTCPConnection(XMPPTCPConnectionConfiguration)}
     * constructor.
     * </p>
     *
     * @param jid the bare JID used by the client.
     * @param password the password or authentication token.
     * @throws XmppStringprepException if the provided string is invalid.
     */
    public XMPPTCPConnection(CharSequence jid, String password) throws XmppStringprepException {
        this(XMPPTCPConnectionConfiguration.builder().setXmppAddressAndPassword(jid, password).build());
    }

    /**
     * Creates a new XMPP connection over TCP.
     * <p>
     * This is the simplest constructor for connecting to an XMPP server. Alternatively,
     * you can get fine-grained control over connection settings using the
     * {@link #XMPPTCPConnection(XMPPTCPConnectionConfiguration)} constructor.
     * </p>
     * @param username TODO javadoc me please
     * @param password TODO javadoc me please
     * @param serviceName TODO javadoc me please
     * @throws XmppStringprepException if the provided string is invalid.
     */
    public XMPPTCPConnection(CharSequence username, String password, String serviceName) throws XmppStringprepException {
        this(XMPPTCPConnectionConfiguration.builder().setUsernameAndPassword(username, password).setXmppDomain(
                                        JidCreate.domainBareFrom(serviceName)).build());
    }

    @Override
    protected void throwNotConnectedExceptionIfAppropriate() throws NotConnectedException {
        if (packetWriter == null) {
            throw new NotConnectedException();
        }
        packetWriter.throwNotConnectedExceptionIfDoneAndResumptionNotPossible();
    }

    @Override
    protected void throwAlreadyConnectedExceptionIfAppropriate() throws AlreadyConnectedException {
        if (isConnected() && !disconnectedButResumeable) {
            throw new AlreadyConnectedException();
        }
    }

    @Override
    protected void throwAlreadyLoggedInExceptionIfAppropriate() throws AlreadyLoggedInException {
        if (isAuthenticated() && !disconnectedButResumeable) {
            throw new AlreadyLoggedInException();
        }
    }

    @Override
    protected void afterSuccessfulLogin(final boolean resumed) throws NotConnectedException, InterruptedException {
        // Reset the flag in case it was set
        disconnectedButResumeable = false;
        super.afterSuccessfulLogin(resumed);
    }

    @Override
    protected synchronized void loginInternal(String username, String password, Resourcepart resource) throws XMPPException,
                    SmackException, IOException, InterruptedException {
        // Authenticate using SASL
        SSLSession sslSession = secureSocket != null ? secureSocket.getSession() : null;

        streamFeaturesAfterAuthenticationReceived = false;
        authenticate(username, password, config.getAuthzid(), sslSession);

        // Wait for stream features after the authentication.
        // TODO: The name of this synchronization point "maybeCompressFeaturesReceived" is not perfect. It should be
        // renamed to "streamFeaturesAfterAuthenticationReceived".
        waitForConditionOrThrowConnectionException(() -> streamFeaturesAfterAuthenticationReceived, "compress features from server");

        // If compression is enabled then request the server to use stream compression. XEP-170
        // recommends to perform stream compression before resource binding.
        maybeEnableCompression();

        smResumedSyncPoint = SyncPointState.initial;
        smResumptionFailed = null;
        if (isSmResumptionPossible()) {
            smResumedSyncPoint = SyncPointState.request_sent;
            sendNonza(new Resume(clientHandledStanzasCount, smSessionId));
            waitForConditionOrThrowConnectionException(() -> smResumedSyncPoint == SyncPointState.successful || smResumptionFailed != null, "resume previous stream");
            if (smResumedSyncPoint == SyncPointState.successful) {
                // We successfully resumed the stream, be done here
                afterSuccessfulLogin(true);
                return;
            }
            // SM resumption failed, what Smack does here is to report success of
            // lastFeaturesReceived in case of sm resumption was answered with 'failed' so that
            // normal resource binding can be tried.
            assert smResumptionFailed != null;
            LOGGER.fine("Stream resumption failed, continuing with normal stream establishment process: " + smResumptionFailed);
        }

        // We either failed to resume a previous stream management (SM) session, or we did not even try. In any case,
        // mark SM as not enabled. Most importantly, we do this prior calling bindResourceAndEstablishSession(), as the
        // bind IQ may trigger a SM ack request, which would be invalid in the pre resource bound state.
        smEnabledSyncPoint = false;

        List<Stanza> previouslyUnackedStanzas = new LinkedList<Stanza>();
        if (unacknowledgedStanzas != null) {
            // There was a previous connection with SM enabled but that was either not resumable or
            // failed to resume. Make sure that we (re-)send the unacknowledged stanzas.
            unacknowledgedStanzas.drainTo(previouslyUnackedStanzas);
            // Reset unacknowledged stanzas to 'null' to signal that we never send 'enable' in this
            // XMPP session (There maybe was an enabled in a previous XMPP session of this
            // connection instance though). This is used in writePackets to decide if stanzas should
            // be added to the unacknowledged stanzas queue, because they have to be added right
            // after the 'enable' stream element has been sent.
            dropSmState();
        }

        // Now bind the resource. It is important to do this *after* we dropped an eventually
        // existing Stream Management state. As otherwise <bind/> and <session/> may end up in
        // unacknowledgedStanzas and become duplicated on reconnect. See SMACK-706.
        bindResourceAndEstablishSession(resource);

        if (isSmAvailable() && useSm) {
            // Remove what is maybe left from previously stream managed sessions
            serverHandledStanzasCount = 0;
            sendNonza(new Enable(useSmResumption, smClientMaxResumptionTime));
            // XEP-198 3. Enabling Stream Management. If the server response to 'Enable' is 'Failed'
            // then this is a non recoverable error and we therefore throw an exception.
            waitForConditionOrThrowConnectionException(() -> smEnabledSyncPoint, "enabling stream mangement");
            synchronized (requestAckPredicates) {
                if (requestAckPredicates.isEmpty()) {
                    // Assure that we have at lest one predicate set up that so that we request acks
                    // for the server and eventually flush some stanzas from the unacknowledged
                    // stanza queue
                    requestAckPredicates.add(Predicate.forMessagesOrAfter5Stanzas());
                }
            }
        }
        // Inform client about failed resumption if possible, resend stanzas otherwise
        // Process the stanzas synchronously so a client can re-queue them for transmission
        // before it is informed about connection success
        if (!stanzaDroppedListeners.isEmpty()) {
            for (Stanza stanza : previouslyUnackedStanzas) {
                for (StanzaListener listener : stanzaDroppedListeners) {
                    try {
                        listener.processStanza(stanza);
                    }
                    catch (InterruptedException | NotConnectedException | NotLoggedInException e) {
                        LOGGER.log(Level.FINER, "StanzaDroppedListener received exception", e);
                    }
                }
            }
        } else {
            for (Stanza stanza : previouslyUnackedStanzas) {
                sendStanzaInternal(stanza);
            }
        }

        afterSuccessfulLogin(false);
    }

    @Override
    public boolean isSecureConnection() {
        return secureSocket != null;
    }

    /**
     * Shuts the current connection down. After this method returns, the connection must be ready
     * for re-use by connect.
     */
    @Override
    protected void shutdown() {
        if (isSmEnabled()) {
            try {
                // Try to send a last SM Acknowledgement. Most servers won't find this information helpful, as the SM
                // state is dropped after a clean disconnect anyways. OTOH it doesn't hurt much either.
                sendSmAcknowledgementInternal();
            } catch (InterruptedException | NotConnectedException e) {
                LOGGER.log(Level.FINE, "Can not send final SM ack as connection is not connected", e);
            }
        }
        shutdown(false);
    }

    @Override
    public synchronized void instantShutdown() {
        shutdown(true);
    }

    private void shutdown(boolean instant) {
        // The writer thread may already been finished at this point, for example when the connection is in the
        // disconnected-but-resumable state. There is no need to wait for the closing stream tag from the server in this
        // case.
        if (!packetWriter.done()) {
            // First shutdown the writer, this will result in a closing stream element getting send to
            // the server
            LOGGER.finer(packetWriter.threadName + " shutdown()");
            packetWriter.shutdown(instant);
            LOGGER.finer(packetWriter.threadName + " shutdown() returned");

            if (!instant) {
                waitForClosingStreamTagFromServer();
            }
        }

        LOGGER.finer(packetReader.threadName + " shutdown()");
        packetReader.shutdown();
        LOGGER.finer(packetReader.threadName + " shutdown() returned");

        CloseableUtil.maybeClose(socket, LOGGER);

        setWasAuthenticated();

        try {
            boolean readerAndWriterThreadsTermianted = waitFor(() -> !packetWriter.running && !packetReader.running);
            if (!readerAndWriterThreadsTermianted) {
                LOGGER.severe("Reader and/or writer threads did not terminate timely. Writer running: "
                                + packetWriter.running + ", Reader running: " + packetReader.running);
            } else {
                LOGGER.fine("Reader and writer threads terminated");
            }
        } catch (InterruptedException e) {
            LOGGER.log(Level.FINE, "Interrupted while waiting for reader and writer threads to terminate", e);
        }

        if (disconnectedButResumeable) {
            return;
        }

        // If we are able to resume the stream, then don't set
        // connected/authenticated/usingTLS to false since we like behave like we are still
        // connected (e.g. sendStanza should not throw a NotConnectedException).
        if (instant) {
            disconnectedButResumeable = isSmResumptionPossible();
            if (!disconnectedButResumeable) {
                // Reset the stream management session id to null, since the stream is no longer resumable. Note that we
                // keep the unacknowledgedStanzas queue, because we want to resend them when we are reconnected.
                smSessionId = null;
            }
        } else {
            disconnectedButResumeable = false;

            // Drop the stream management state if this is not an instant shutdown. We send
            // a </stream> close tag and now the stream management state is no longer valid.
            // This also prevents that we will potentially (re-)send any unavailable presence we
            // may have send, because it got put into the unacknowledged queue and was not acknowledged before the
            // connection terminated.
            dropSmState();
            // Note that we deliberately do not reset authenticatedConnectionInitiallyEstablishedTimestamp here, so that the
            // information is available in the connectionClosedOnError() listeners.
        }
        authenticated = false;
        connected = false;
        secureSocket = null;
        reader = null;
        writer = null;

        initState();
    }

    @Override
    public void sendNonza(Nonza element) throws NotConnectedException, InterruptedException {
        packetWriter.sendStreamElement(element);
    }

    @Override
    protected void sendStanzaInternal(Stanza packet) throws NotConnectedException, InterruptedException {
        packetWriter.sendStreamElement(packet);
        if (isSmEnabled()) {
            for (StanzaFilter requestAckPredicate : requestAckPredicates) {
                if (requestAckPredicate.accept(packet)) {
                    requestSmAcknowledgementInternal();
                    break;
                }
            }
        }
    }

    private void connectUsingConfiguration() throws ConnectionException, IOException, InterruptedException {
        RemoteXmppTcpConnectionEndpoints.Result<Rfc6120TcpRemoteConnectionEndpoint> result = RemoteXmppTcpConnectionEndpoints.lookup(config);

        List<RemoteConnectionException<Rfc6120TcpRemoteConnectionEndpoint>> connectionExceptions = new ArrayList<>();

        SocketFactory socketFactory = config.getSocketFactory();
        ProxyInfo proxyInfo = config.getProxyInfo();
        int timeout = config.getConnectTimeout();
        if (socketFactory == null) {
            socketFactory = SocketFactory.getDefault();
        }
        for (Rfc6120TcpRemoteConnectionEndpoint endpoint : result.discoveredRemoteConnectionEndpoints) {
            Iterator<? extends InetAddress> inetAddresses;
            String host = endpoint.getHost().toString();
            UInt16 portUint16 = endpoint.getPort();
            int port = portUint16.intValue();
            if (proxyInfo == null) {
                inetAddresses = endpoint.getInetAddresses().iterator();
                assert inetAddresses.hasNext();

                innerloop: while (inetAddresses.hasNext()) {
                    // Create a *new* Socket before every connection attempt, i.e. connect() call, since Sockets are not
                    // re-usable after a failed connection attempt. See also SMACK-724.
                    SmackFuture.SocketFuture socketFuture = new SmackFuture.SocketFuture(socketFactory);

                    final InetAddress inetAddress = inetAddresses.next();
                    final InetSocketAddress inetSocketAddress = new InetSocketAddress(inetAddress, port);
                    LOGGER.finer("Trying to establish TCP connection to " + inetSocketAddress);
                    socketFuture.connectAsync(inetSocketAddress, timeout);

                    try {
                        socket = socketFuture.getOrThrow();
                    } catch (IOException e) {
                        RemoteConnectionException<Rfc6120TcpRemoteConnectionEndpoint> rce = new RemoteConnectionException<>(
                                        endpoint, inetAddress, e);
                        connectionExceptions.add(rce);
                        if (inetAddresses.hasNext()) {
                            continue innerloop;
                        } else {
                            break innerloop;
                        }
                    }
                    LOGGER.finer("Established TCP connection to " + inetSocketAddress);
                    // We found a host to connect to, return here
                    this.host = host;
                    this.port = portUint16;
                    return;
                }
            } else {
                // TODO: Move this into the inner-loop above. There appears no reason why we should not try a proxy
                // connection to every inet address of each connection endpoint.
                socket = socketFactory.createSocket();
                StringUtils.requireNotNullNorEmpty(host, "Host of endpoint " + endpoint + " must not be null when using a Proxy");
                final String hostAndPort = host + " at port " + port;
                LOGGER.finer("Trying to establish TCP connection via Proxy to " + hostAndPort);
                try {
                    proxyInfo.getProxySocketConnection().connect(socket, host, port, timeout);
                } catch (IOException e) {
                    CloseableUtil.maybeClose(socket, LOGGER);
                    RemoteConnectionException<Rfc6120TcpRemoteConnectionEndpoint> rce = new RemoteConnectionException<>(endpoint, null, e);
                    connectionExceptions.add(rce);
                    continue;
                }
                LOGGER.finer("Established TCP connection to " + hostAndPort);
                // We found a host to connect to, return here
                this.host = host;
                this.port = portUint16;
                return;
            }
        }

        // There are no more host addresses to try
        // throw an exception and report all tried
        // HostAddresses in the exception
        throw EndpointConnectionException.from(result.lookupFailures, connectionExceptions);
    }

    /**
     * Initializes the connection by creating a stanza reader and writer and opening a
     * XMPP stream to the server.
     *
     * @throws XMPPException if establishing a connection to the server fails.
     * @throws SmackException if the server fails to respond back or if there is anther error.
     * @throws IOException if an I/O error occurred.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    private void initConnection() throws IOException, InterruptedException {
        compressionHandler = null;

        // Set the reader and writer instance variables
        initReaderAndWriter();

        // Start the writer thread. This will open an XMPP stream to the server
        packetWriter.init();
        // Start the reader thread. The startup() method will block until we
        // get an opening stream packet back from server
        packetReader.init();
    }

    private void initReaderAndWriter() throws IOException {
        InputStream is = socket.getInputStream();
        OutputStream os = socket.getOutputStream();
        if (compressionHandler != null) {
            is = compressionHandler.getInputStream(is);
            os = compressionHandler.getOutputStream(os);
        }
        // OutputStreamWriter is already buffered, no need to wrap it into a BufferedWriter
        writer = new OutputStreamWriter(os, "UTF-8");
        reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));

        // If debugging is enabled, we open a window and write out all network traffic.
        initDebugger();
    }

    /**
     * The server has indicated that TLS negotiation can start. We now need to secure the
     * existing plain connection and perform a handshake. This method won't return until the
     * connection has finished the handshake or an error occurred while securing the connection.
     * @throws IOException if an I/O error occurred.
     * @throws SecurityNotPossibleException if TLS is not possible.
     * @throws CertificateException if there is an issue with the certificate.
     */
    @SuppressWarnings("LiteralClassName")
    private void proceedTLSReceived() throws IOException, SecurityNotPossibleException, CertificateException {
        SmackTlsContext smackTlsContext = getSmackTlsContext();

        Socket plain = socket;
        int port = plain.getPort();
        String xmppServiceDomainString = config.getXMPPServiceDomain().toString();
        SSLSocketFactory sslSocketFactory = smackTlsContext.sslContext.getSocketFactory();
        // Secure the plain connection
        socket = sslSocketFactory.createSocket(plain, xmppServiceDomainString, port, true);

        final SSLSocket sslSocket = (SSLSocket) socket;
        // Immediately set the enabled SSL protocols and ciphers. See SMACK-712 why this is
        // important (at least on certain platforms) and it seems to be a good idea anyways to
        // prevent an accidental implicit handshake.
        TLSUtils.setEnabledProtocolsAndCiphers(sslSocket, config.getEnabledSSLProtocols(), config.getEnabledSSLCiphers());

        // Initialize the reader and writer with the new secured version
        initReaderAndWriter();

        // Proceed to do the handshake
        sslSocket.startHandshake();

        if (smackTlsContext.daneVerifier != null) {
            smackTlsContext.daneVerifier.finish(sslSocket.getSession());
        }

        final HostnameVerifier verifier = getConfiguration().getHostnameVerifier();
        if (verifier == null) {
                throw new IllegalStateException("No HostnameVerifier set. Use connectionConfiguration.setHostnameVerifier() to configure.");
        }

        final String verifierHostname;
        {
            DnsName xmppServiceDomainDnsName = getConfiguration().getXmppServiceDomainAsDnsNameIfPossible();
            // Try to convert the XMPP service domain, which potentially includes Unicode characters, into ASCII
            // Compatible Encoding (ACE) to match RFC3280 dNSname IA5String constraint.
            // See also: https://bugzilla.mozilla.org/show_bug.cgi?id=280839#c1
            if (xmppServiceDomainDnsName != null) {
                verifierHostname = xmppServiceDomainDnsName.ace;
            }
            else {
                LOGGER.log(Level.WARNING, "XMPP service domain name '" + getXMPPServiceDomain()
                                + "' can not be represented as DNS name. TLS X.509 certificate validiation may fail.");
                verifierHostname = getXMPPServiceDomain().toString();
            }
        }

        final boolean verificationSuccessful;
        // Verify the TLS session.
        verificationSuccessful = verifier.verify(verifierHostname, sslSocket.getSession());
        if (!verificationSuccessful) {
            throw new CertificateException(
                            "Hostname verification of certificate failed. Certificate does not authenticate "
                                            + getXMPPServiceDomain());
        }

        // Set that TLS was successful
        secureSocket = sslSocket;
    }

    /**
     * Returns the compression handler that can be used for one compression methods offered by the server.
     *
     * @return a instance of XMPPInputOutputStream or null if no suitable instance was found
     *
     */
    private static XMPPInputOutputStream maybeGetCompressionHandler(Compress.Feature compression) {
        for (XMPPInputOutputStream handler : SmackConfiguration.getCompressionHandlers()) {
                String method = handler.getCompressionMethod();
                if (compression.getMethods().contains(method))
                    return handler;
        }
        return null;
    }

    @Override
    public boolean isUsingCompression() {
        return compressionHandler != null && compressSyncPoint;
    }

    /**
     * <p>
     * Starts using stream compression that will compress network traffic. Traffic can be
     * reduced up to 90%. Therefore, stream compression is ideal when using a slow speed network
     * connection. However, the server and the client will need to use more CPU time in order to
     * un/compress network data so under high load the server performance might be affected.
     * </p>
     * <p>
     * Stream compression has to have been previously offered by the server. Currently only the
     * zlib method is supported by the client. Stream compression negotiation has to be done
     * before authentication took place.
     * </p>
     *
     * @throws NotConnectedException if the XMPP connection is not connected.
     * @throws SmackException if Smack detected an exceptional situation.
     * @throws InterruptedException if the calling thread was interrupted.
     * @throws XMPPException if an XMPP protocol error was received.
     */
    private void maybeEnableCompression() throws SmackException, InterruptedException, XMPPException {
        if (!config.isCompressionEnabled()) {
            return;
        }

        Compress.Feature compression = getFeature(Compress.Feature.class);
        if (compression == null) {
            // Server does not support compression
            return;
        }
        // If stream compression was offered by the server and we want to use
        // compression then send compression request to the server
        if ((compressionHandler = maybeGetCompressionHandler(compression)) != null) {
            compressSyncPoint = false;
            sendNonza(new Compress(compressionHandler.getCompressionMethod()));
            waitForConditionOrThrowConnectionException(() -> compressSyncPoint, "establishing stream compression");
        } else {
            LOGGER.warning("Could not enable compression because no matching handler/method pair was found");
        }
    }

    /**
     * Establishes a connection to the XMPP server. It basically
     * creates and maintains a socket connection to the server.
     * <p>
     * Listeners will be preserved from a previous connection if the reconnection
     * occurs after an abrupt termination.
     * </p>
     *
     * @throws XMPPException if an error occurs while trying to establish the connection.
     * @throws SmackException if Smack detected an exceptional situation.
     * @throws IOException if an I/O error occurred.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    @Override
    protected void connectInternal() throws SmackException, IOException, XMPPException, InterruptedException {
        // Establishes the TCP connection to the server and does setup the reader and writer. Throws an exception if
        // there is an error establishing the connection
        connectUsingConfiguration();

        connected = true;

        // We connected successfully to the servers TCP port
        initConnection();

        // TLS handled will be true either if TLS was established, or if it was not mandatory.
        waitForConditionOrThrowConnectionException(() -> tlsHandled, "establishing TLS");

        // Wait with SASL auth until the SASL mechanisms have been received
        waitForConditionOrThrowConnectionException(() -> saslFeatureReceived, "SASL mechanisms stream feature from server");
    }

    /**
     * For unit testing purposes
     *
     * @param writer TODO javadoc me please
     */
    protected void setWriter(Writer writer) {
        this.writer = writer;
    }

    @Override
    protected void afterFeaturesReceived() throws NotConnectedException, InterruptedException, SecurityRequiredByServerException {
        StartTls startTlsFeature = getFeature(StartTls.class);
        if (startTlsFeature != null) {
            if (startTlsFeature.required() && config.getSecurityMode() == SecurityMode.disabled) {
                SecurityRequiredByServerException smackException = new SecurityRequiredByServerException();
                currentSmackException = smackException;
                notifyWaitingThreads();
                throw smackException;
            }

            if (config.getSecurityMode() != ConnectionConfiguration.SecurityMode.disabled) {
                sendNonza(new StartTls());
            } else {
                tlsHandled = true;
                notifyWaitingThreads();
            }
        } else {
            tlsHandled = true;
            notifyWaitingThreads();
        }

        if (isSaslAuthenticated()) {
            // If we have received features after the SASL has been successfully completed, then we
            // have also *maybe* received, as it is an optional feature, the compression feature
            // from the server.
            streamFeaturesAfterAuthenticationReceived = true;
            notifyWaitingThreads();
        }
    }

    private void resetParser() throws IOException {
        try {
            packetReader.parser = SmackXmlParser.newXmlParser(reader);
        } catch (XmlPullParserException e) {
            throw new IOException(e);
        }
   }

    private void openStreamAndResetParser() throws IOException, NotConnectedException, InterruptedException {
        sendStreamOpen();
        resetParser();
    }

    protected class PacketReader {

        private final String threadName = "Smack Reader (" + getConnectionCounter() + ')';

        XmlPullParser parser;

        private volatile boolean done;

        private boolean running;

        /**
         * Initializes the reader in order to be used. The reader is initialized during the
         * first connection and when reconnecting due to an abruptly disconnection.
         */
        void init() {
            done = false;

            running = true;
            Async.go(new Runnable() {
                @Override
                public void run() {
                    LOGGER.finer(threadName + " start");
                    try {
                        parsePackets();
                    } finally {
                        LOGGER.finer(threadName + " exit");
                        running = false;
                        notifyWaitingThreads();
                    }
                }
            }, threadName);
         }

        /**
         * Shuts the stanza reader down. This method simply sets the 'done' flag to true.
         */
        void shutdown() {
            done = true;
        }

        /**
         * Parse top-level packets in order to process them further.
         */
        private void parsePackets() {
            try {
                openStreamAndResetParser();
                XmlPullParser.Event eventType = parser.getEventType();
                while (!done) {
                    switch (eventType) {
                    case START_ELEMENT:
                        final String name = parser.getName();
                        switch (name) {
                        case Message.ELEMENT:
                        case IQ.IQ_ELEMENT:
                        case Presence.ELEMENT:
                            try {
                                parseAndProcessStanza(parser);
                            } finally {
                                clientHandledStanzasCount = SMUtils.incrementHeight(clientHandledStanzasCount);
                            }
                            break;
                        case "stream":
                            onStreamOpen(parser);
                            break;
                        case "error":
                            StreamError streamError = PacketParserUtils.parseStreamError(parser);
                            // Stream errors are non recoverable, throw this exceptions. Also note that this will set
                            // this exception as current connection exceptions and notify any waiting threads.
                            throw new StreamErrorException(streamError);
                        case "features":
                            parseFeaturesAndNotify(parser);
                            break;
                        case "proceed":
                            // Secure the connection by negotiating TLS
                            proceedTLSReceived();
                            // Send a new opening stream to the server
                            openStreamAndResetParser();
                            break;
                        case "failure":
                            String namespace = parser.getNamespace(null);
                            switch (namespace) {
                            case "urn:ietf:params:xml:ns:xmpp-tls":
                                // TLS negotiation has failed. The server will close the connection
                                // TODO Parse failure stanza
                                throw new SmackException.SmackMessageException("TLS negotiation has failed");
                            case "http://jabber.org/protocol/compress":
                                // Stream compression has been denied. This is a recoverable
                                // situation. It is still possible to authenticate and
                                // use the connection but using an uncompressed connection
                                // TODO Parse failure stanza
                                currentSmackException = new SmackException.SmackMessageException("Could not establish compression");
                                notifyWaitingThreads();
                                break;
                            default:
                                parseAndProcessNonza(parser);
                            }
                            break;
                        case Compressed.ELEMENT:
                            // Server confirmed that it's possible to use stream compression. Start
                            // stream compression
                            // Initialize the reader and writer with the new compressed version
                            initReaderAndWriter();
                            // Send a new opening stream to the server
                            openStreamAndResetParser();
                            // Notify that compression is being used
                            compressSyncPoint = true;
                            notifyWaitingThreads();
                            break;
                        case Enabled.ELEMENT:
                            Enabled enabled = ParseStreamManagement.enabled(parser);
                            if (enabled.isResumeSet()) {
                                smSessionId = enabled.getId();
                                if (StringUtils.isNullOrEmpty(smSessionId)) {
                                    SmackException xmppException = new SmackException.SmackMessageException("Stream Management 'enabled' element with resume attribute but without session id received");
                                    setCurrentConnectionExceptionAndNotify(xmppException);
                                    throw xmppException;
                                }
                                smServerMaxResumptionTime = enabled.getMaxResumptionTime();
                            } else {
                                // Mark this a non-resumable stream by setting smSessionId to null
                                smSessionId = null;
                            }
                            clientHandledStanzasCount = 0;
                            smWasEnabledAtLeastOnce = true;
                            smEnabledSyncPoint = true;
                            notifyWaitingThreads();
                            break;
                        case Failed.ELEMENT:
                            Failed failed = ParseStreamManagement.failed(parser);
                            if (smResumedSyncPoint == SyncPointState.request_sent) {
                                // This is a <failed/> nonza in a response to resuming a previous stream, failure to do
                                // so is non-fatal as we can simply continue with resource binding in this case.
                                smResumptionFailed = failed;
                                notifyWaitingThreads();
                            } else {
                                FailedNonzaException xmppException = new FailedNonzaException(failed, failed.getStanzaErrorCondition());
                                setCurrentConnectionExceptionAndNotify(xmppException);
                            }
                            break;
                        case Resumed.ELEMENT:
                            Resumed resumed = ParseStreamManagement.resumed(parser);
                            if (!smSessionId.equals(resumed.getPrevId())) {
                                throw new StreamIdDoesNotMatchException(smSessionId, resumed.getPrevId());
                            }
                            // Mark SM as enabled
                            smEnabledSyncPoint = true;
                            // First, drop the stanzas already handled by the server
                            processHandledCount(resumed.getHandledCount());
                            // Then re-send what is left in the unacknowledged queue
                            List<Stanza> stanzasToResend = new ArrayList<>(unacknowledgedStanzas.size());
                            unacknowledgedStanzas.drainTo(stanzasToResend);
                            for (Stanza stanza : stanzasToResend) {
                                sendStanzaInternal(stanza);
                            }
                            // If there where stanzas resent, then request a SM ack for them.
                            // Writer's sendStreamElement() won't do it automatically based on
                            // predicates.
                            if (!stanzasToResend.isEmpty()) {
                                requestSmAcknowledgementInternal();
                            }
                            // Mark SM resumption as successful
                            smResumedSyncPoint = SyncPointState.successful;
                            notifyWaitingThreads();
                            break;
                        case AckAnswer.ELEMENT:
                            AckAnswer ackAnswer = ParseStreamManagement.ackAnswer(parser);
                            processHandledCount(ackAnswer.getHandledCount());
                            break;
                        case AckRequest.ELEMENT:
                            ParseStreamManagement.ackRequest(parser);
                            if (smEnabledSyncPoint) {
                                sendSmAcknowledgementInternal();
                            } else {
                                LOGGER.warning("SM Ack Request received while SM is not enabled");
                            }
                            break;
                         default:
                             parseAndProcessNonza(parser);
                             break;
                        }
                        break;
                    case END_ELEMENT:
                        final String endTagName = parser.getName();
                        if ("stream".equals(endTagName)) {
                            if (!parser.getNamespace().equals("http://etherx.jabber.org/streams")) {
                                LOGGER.warning(XMPPTCPConnection.this +  " </stream> but different namespace " + parser.getNamespace());
                                break;
                            }

                            // Check if the queue was already shut down before reporting success on closing stream tag
                            // received. This avoids a race if there is a disconnect(), followed by a connect(), which
                            // did re-start the queue again, causing this writer to assume that the queue is not
                            // shutdown, which results in a call to disconnect().
                            final boolean queueWasShutdown = packetWriter.queue.isShutdown();
                            closingStreamReceived = true;
                            notifyWaitingThreads();

                            if (queueWasShutdown) {
                                // We received a closing stream element *after* we initiated the
                                // termination of the session by sending a closing stream element to
                                // the server first
                                return;
                            } else {
                                // We received a closing stream element from the server without us
                                // sending a closing stream element first. This means that the
                                // server wants to terminate the session, therefore disconnect
                                // the connection
                                LOGGER.info(XMPPTCPConnection.this
                                                + " received closing </stream> element."
                                                + " Server wants to terminate the connection, calling disconnect()");
                                ASYNC_BUT_ORDERED.performAsyncButOrdered(XMPPTCPConnection.this, new Runnable() {
                                    @Override
                                    public void run() {
                                        disconnect();
                                    }});
                            }
                        }
                        break;
                    case END_DOCUMENT:
                        // END_DOCUMENT only happens in an error case, as otherwise we would see a
                        // closing stream element before.
                        throw new SmackException.SmackMessageException(
                                        "Parser got END_DOCUMENT event. This could happen e.g. if the server closed the connection without sending a closing stream element");
                    default:
                        // Catch all for incomplete switch (MissingCasesInEnumSwitch) statement.
                        break;
                    }
                    eventType = parser.next();
                }
            }
            catch (Exception e) {
                // Set running to false since this thread will exit here and notifyConnectionError() will wait until
                // the reader and writer thread's 'running' value is false. Hence we need to set it to false before calling
                // notifyConnetctionError() below, even though run() also sets it to false. Therefore, do not remove this.
                running = false;

                String ignoreReasonThread = null;

                boolean writerThreadWasShutDown = packetWriter.queue.isShutdown();
                if (writerThreadWasShutDown) {
                    ignoreReasonThread = "writer";
                } else if (done) {
                    ignoreReasonThread = "reader";
                }

                if (ignoreReasonThread != null) {
                    LOGGER.log(Level.FINER, "Ignoring " + e + " as " + ignoreReasonThread + " was already shut down");
                    return;
                }

                // Close the connection and notify connection listeners of the error.
                notifyConnectionError(e);
            }
        }
    }

    protected class PacketWriter {
        public static final int QUEUE_SIZE = XMPPTCPConnection.QUEUE_SIZE;
        public static final int UNACKKNOWLEDGED_STANZAS_QUEUE_SIZE = 1024;
        public static final int UNACKKNOWLEDGED_STANZAS_QUEUE_SIZE_HIGH_WATER_MARK = (int) (0.3 * UNACKKNOWLEDGED_STANZAS_QUEUE_SIZE);

        private final String threadName = "Smack Writer (" + getConnectionCounter() + ')';

        private final ArrayBlockingQueueWithShutdown<Element> queue = new ArrayBlockingQueueWithShutdown<>(
                        QUEUE_SIZE, true);

        /**
         * If set, the stanza writer is shut down
         */
        protected volatile Long shutdownTimestamp = null;

        private volatile boolean instantShutdown;

        /**
         * True if some preconditions are given to start the bundle and defer mechanism.
         * <p>
         * This will likely get set to true right after the start of the writer thread, because
         * {@link #nextStreamElement()} will check if {@link queue} is empty, which is probably the case, and then set
         * this field to true.
         * </p>
         */
        private boolean shouldBundleAndDefer;

        private boolean running;

        /**
        * Initializes the writer in order to be used. It is called at the first connection and also
        * is invoked if the connection is disconnected by an error.
        */
        void init() {
            shutdownTimestamp = null;

            if (unacknowledgedStanzas != null) {
                // It's possible that there are new stanzas in the writer queue that
                // came in while we were disconnected but resumable, drain those into
                // the unacknowledged queue so that they get resent now
                drainWriterQueueToUnacknowledgedStanzas();
            }

            queue.start();
            running = true;
            Async.go(new Runnable() {
                @Override
                public void run() {
                    LOGGER.finer(threadName + " start");
                    try {
                        writePackets();
                    } finally {
                        LOGGER.finer(threadName + " exit");
                        running = false;
                        notifyWaitingThreads();
                    }
                }
            }, threadName);
        }

        private boolean done() {
            return shutdownTimestamp != null;
        }

        protected void throwNotConnectedExceptionIfDoneAndResumptionNotPossible() throws NotConnectedException {
            final boolean done = done();
            if (done) {
                final boolean smResumptionPossible = isSmResumptionPossible();
                // Don't throw a NotConnectedException is there is an resumable stream available
                if (!smResumptionPossible) {
                    throw new NotConnectedException(XMPPTCPConnection.this, "done=" + done
                                    + " smResumptionPossible=" + smResumptionPossible);
                }
            }
        }

        /**
         * Sends the specified element to the server.
         *
         * @param element the element to send.
         * @throws NotConnectedException if the XMPP connection is not connected.
         * @throws InterruptedException if the calling thread was interrupted.
         */
        protected void sendStreamElement(Element element) throws NotConnectedException, InterruptedException {
            throwNotConnectedExceptionIfDoneAndResumptionNotPossible();
            try {
                queue.put(element);
            }
            catch (InterruptedException e) {
                // put() may throw an InterruptedException for two reasons:
                // 1. If the queue was shut down
                // 2. If the thread was interrupted
                // so we have to check which is the case
                throwNotConnectedExceptionIfDoneAndResumptionNotPossible();
                // If the method above did not throw, then the sending thread was interrupted
                throw e;
            }
        }

        /**
         * Shuts down the stanza writer. Once this method has been called, no further
         * packets will be written to the server.
         */
        void shutdown(boolean instant) {
            instantShutdown = instant;
            queue.shutdown();
            shutdownTimestamp = System.currentTimeMillis();
        }

        /**
         * Maybe return the next available element from the queue for writing. If the queue is shut down <b>or</b> a
         * spurious interrupt occurs, <code>null</code> is returned. So it is important to check the 'done' condition in
         * that case.
         *
         * @return the next element for writing or null.
         */
        private Element nextStreamElement() {
            // It is important the we check if the queue is empty before removing an element from it
            if (queue.isEmpty()) {
                shouldBundleAndDefer = true;
            }
            Element packet = null;
            try {
                packet = queue.take();
            }
            catch (InterruptedException e) {
                if (!queue.isShutdown()) {
                    // Users shouldn't try to interrupt the packet writer thread
                    LOGGER.log(Level.WARNING, "Writer thread was interrupted. Don't do that. Use disconnect() instead.", e);
                }
            }
            return packet;
        }

        private void writePackets() {
            try {
                // Write out packets from the queue.
                while (!done()) {
                    Element element = nextStreamElement();
                    if (element == null) {
                        continue;
                    }

                    // Get a local version of the bundle and defer callback, in case it's unset
                    // between the null check and the method invocation
                    final BundleAndDeferCallback localBundleAndDeferCallback = bundleAndDeferCallback;
                    // If the preconditions are given (e.g. bundleAndDefer callback is set, queue is
                    // empty), then we could wait a bit for further stanzas attempting to decrease
                    // our energy consumption
                    if (localBundleAndDeferCallback != null && isAuthenticated() && shouldBundleAndDefer) {
                        // Reset shouldBundleAndDefer to false, nextStreamElement() will set it to true once the
                        // queue is empty again.
                        shouldBundleAndDefer = false;
                        final AtomicBoolean bundlingAndDeferringStopped = new AtomicBoolean();
                        final int bundleAndDeferMillis = localBundleAndDeferCallback.getBundleAndDeferMillis(new BundleAndDefer(
                                        bundlingAndDeferringStopped));
                        if (bundleAndDeferMillis > 0) {
                            long remainingWait = bundleAndDeferMillis;
                            final long waitStart = System.currentTimeMillis();
                            synchronized (bundlingAndDeferringStopped) {
                                while (!bundlingAndDeferringStopped.get() && remainingWait > 0) {
                                    bundlingAndDeferringStopped.wait(remainingWait);
                                    remainingWait = bundleAndDeferMillis
                                                    - (System.currentTimeMillis() - waitStart);
                                }
                            }
                        }
                    }

                    Stanza packet = null;
                    if (element instanceof Stanza) {
                        packet = (Stanza) element;
                    }
                    else if (element instanceof Enable) {
                        // The client needs to add messages to the unacknowledged stanzas queue
                        // right after it sent 'enabled'. Stanza will be added once
                        // unacknowledgedStanzas is not null.
                        unacknowledgedStanzas = new ArrayBlockingQueue<>(UNACKKNOWLEDGED_STANZAS_QUEUE_SIZE);
                    }
                    maybeAddToUnacknowledgedStanzas(packet);

                    CharSequence elementXml = element.toXML(outgoingStreamXmlEnvironment);
                    if (elementXml instanceof XmlStringBuilder) {
                        try {
                            ((XmlStringBuilder) elementXml).write(writer, outgoingStreamXmlEnvironment);
                        } catch (NullPointerException npe) {
                            LOGGER.log(Level.FINE, "NPE in XmlStringBuilder of " + element.getClass() + ": " + element, npe);
                            throw npe;
                        }
                    }
                    else {
                        writer.write(elementXml.toString());
                    }

                    if (queue.isEmpty()) {
                        writer.flush();
                    }
                    if (packet != null) {
                        firePacketSendingListeners(packet);
                    }
                }
                if (!instantShutdown) {
                    // Flush out the rest of the queue.
                    try {
                        while (!queue.isEmpty()) {
                            Element packet = queue.remove();
                            if (packet instanceof Stanza) {
                                Stanza stanza = (Stanza) packet;
                                maybeAddToUnacknowledgedStanzas(stanza);
                            }
                            writer.write(packet.toXML().toString());
                        }
                    }
                    catch (Exception e) {
                        LOGGER.log(Level.WARNING,
                                        "Exception flushing queue during shutdown, ignore and continue",
                                        e);
                    }

                    // Close the stream.
                    try {
                        writer.write("</stream:stream>");
                        writer.flush();
                    }
                    catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Exception writing closing stream element", e);
                    }

                    // Delete the queue contents (hopefully nothing is left).
                    queue.clear();
                } else if (instantShutdown && isSmEnabled()) {
                    // This was an instantShutdown and SM is enabled, drain all remaining stanzas
                    // into the unacknowledgedStanzas queue
                    drainWriterQueueToUnacknowledgedStanzas();
                }
                // Do *not* close the writer here, as it will cause the socket
                // to get closed. But we may want to receive further stanzas
                // until the closing stream tag is received. The socket will be
                // closed in shutdown().
            }
            catch (Exception e) {
                // The exception can be ignored if the the connection is 'done'
                // or if the it was caused because the socket got closed
                if (!(done() || queue.isShutdown())) {
                    // Set running to false since this thread will exit here and notifyConnectionError() will wait until
                    // the reader and writer thread's 'running' value is false.
                    running = false;
                    notifyConnectionError(e);
                } else {
                    LOGGER.log(Level.FINE, "Ignoring Exception in writePackets()", e);
                }
            }
        }

        private void drainWriterQueueToUnacknowledgedStanzas() {
            List<Element> elements = new ArrayList<>(queue.size());
            queue.drainTo(elements);
            for (int i = 0; i < elements.size(); i++) {
                Element element = elements.get(i);
                // If the unacknowledgedStanza queue is full, then bail out with a warning message. See SMACK-844.
                if (unacknowledgedStanzas.remainingCapacity() == 0) {
                    StreamManagementException.UnacknowledgedQueueFullException exception = StreamManagementException.UnacknowledgedQueueFullException
                            .newWith(i, elements, unacknowledgedStanzas);
                    LOGGER.log(Level.WARNING,
                            "Some stanzas may be lost as not all could be drained to the unacknowledged stanzas queue", exception);
                    return;
                }
                if (element instanceof Stanza) {
                    unacknowledgedStanzas.add((Stanza) element);
                }
            }
        }

        private void maybeAddToUnacknowledgedStanzas(Stanza stanza) throws IOException {
            // Check if the stream element should be put to the unacknowledgedStanza
            // queue. Note that we can not do the put() in sendStanzaInternal() and the
            // packet order is not stable at this point (sendStanzaInternal() can be
            // called concurrently).
            if (unacknowledgedStanzas != null && stanza != null) {
                // If the unacknowledgedStanza queue reaching its high water mark, request an new ack
                // from the server in order to drain it
                if (unacknowledgedStanzas.size() == UNACKKNOWLEDGED_STANZAS_QUEUE_SIZE_HIGH_WATER_MARK) {
                    writer.write(AckRequest.INSTANCE.toXML().toString());
                }

                try {
                    // It is important the we put the stanza in the unacknowledged stanza
                    // queue before we put it on the wire
                    unacknowledgedStanzas.put(stanza);
                }
                catch (InterruptedException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
    }

    /**
     * Set if Stream Management should be used by default for new connections.
     *
     * @param useSmDefault true to use Stream Management for new connections.
     */
    public static void setUseStreamManagementDefault(boolean useSmDefault) {
        XMPPTCPConnection.useSmDefault = useSmDefault;
    }

    /**
     * Set if Stream Management resumption should be used by default for new connections.
     *
     * @param useSmResumptionDefault true to use Stream Management resumption for new connections.
     * @deprecated use {@link #setUseStreamManagementResumptionDefault(boolean)} instead.
     */
    @Deprecated
    public static void setUseStreamManagementResumptiodDefault(boolean useSmResumptionDefault) {
        setUseStreamManagementResumptionDefault(useSmResumptionDefault);
    }

    /**
     * Set if Stream Management resumption should be used by default for new connections.
     *
     * @param useSmResumptionDefault true to use Stream Management resumption for new connections.
     */
    public static void setUseStreamManagementResumptionDefault(boolean useSmResumptionDefault) {
        if (useSmResumptionDefault) {
            // Also enable SM is resumption is enabled
            setUseStreamManagementDefault(useSmResumptionDefault);
        }
        XMPPTCPConnection.useSmResumptionDefault = useSmResumptionDefault;
    }

    /**
     * Set if Stream Management should be used if supported by the server.
     *
     * @param useSm true to use Stream Management.
     */
    public void setUseStreamManagement(boolean useSm) {
        this.useSm = useSm;
    }

    /**
     * Set if Stream Management resumption should be used if supported by the server.
     *
     * @param useSmResumption true to use Stream Management resumption.
     */
    public void setUseStreamManagementResumption(boolean useSmResumption) {
        if (useSmResumption) {
            // Also enable SM is resumption is enabled
            setUseStreamManagement(useSmResumption);
        }
        this.useSmResumption = useSmResumption;
    }

    /**
     * Set the preferred resumption time in seconds.
     * @param resumptionTime the preferred resumption time in seconds
     */
    public void setPreferredResumptionTime(int resumptionTime) {
        smClientMaxResumptionTime = resumptionTime;
    }

    /**
     * Add a predicate for Stream Management acknowledgment requests.
     * <p>
     * Those predicates are used to determine when a Stream Management acknowledgement request is send to the server.
     * Some pre-defined predicates are found in the <code>org.jivesoftware.smack.sm.predicates</code> package.
     * </p>
     * <p>
     * If not predicate is configured, the {@link Predicate#forMessagesOrAfter5Stanzas()} will be used.
     * </p>
     *
     * @param predicate the predicate to add.
     * @return if the predicate was not already active.
     */
    public boolean addRequestAckPredicate(StanzaFilter predicate) {
        synchronized (requestAckPredicates) {
            return requestAckPredicates.add(predicate);
        }
    }

    /**
     * Remove the given predicate for Stream Management acknowledgment request.
     * @param predicate the predicate to remove.
     * @return true if the predicate was removed.
     */
    public boolean removeRequestAckPredicate(StanzaFilter predicate) {
        synchronized (requestAckPredicates) {
            return requestAckPredicates.remove(predicate);
        }
    }

    /**
     * Remove all predicates for Stream Management acknowledgment requests.
     */
    public void removeAllRequestAckPredicates() {
        synchronized (requestAckPredicates) {
            requestAckPredicates.clear();
        }
    }

    /**
     * Send an unconditional Stream Management acknowledgement request to the server.
     *
     * @throws StreamManagementNotEnabledException if Stream Management is not enabled.
     * @throws NotConnectedException if the connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public void requestSmAcknowledgement() throws StreamManagementNotEnabledException, NotConnectedException, InterruptedException {
        if (!isSmEnabled()) {
            throw new StreamManagementException.StreamManagementNotEnabledException();
        }
        requestSmAcknowledgementInternal();
    }

    private void requestSmAcknowledgementInternal() throws NotConnectedException, InterruptedException {
        packetWriter.sendStreamElement(AckRequest.INSTANCE);
    }

    /**
     * Send a unconditional Stream Management acknowledgment to the server.
     * <p>
     * See <a href="http://xmpp.org/extensions/xep-0198.html#acking">XEP-198: Stream Management § 4. Acks</a>:
     * "Either party MAY send an &lt;a/&gt; element at any time (e.g., after it has received a certain number of stanzas,
     * or after a certain period of time), even if it has not received an &lt;r/&gt; element from the other party."
     * </p>
     *
     * @throws StreamManagementNotEnabledException if Stream Management is not enabled.
     * @throws NotConnectedException if the connection is not connected.
     * @throws InterruptedException if the calling thread was interrupted.
     */
    public void sendSmAcknowledgement() throws StreamManagementNotEnabledException, NotConnectedException, InterruptedException {
        if (!isSmEnabled()) {
            throw new StreamManagementException.StreamManagementNotEnabledException();
        }
        sendSmAcknowledgementInternal();
    }

    private void sendSmAcknowledgementInternal() throws NotConnectedException, InterruptedException {
        AckAnswer ackAnswer = new AckAnswer(clientHandledStanzasCount);
        // Do net put an ack to the queue if it has already been shutdown. Some servers, like ejabberd, like to request
        // an ack even after we have send a stream close (and hance the queue was shutdown). If we would not check here,
        // then the ack would dangle around in the queue, and be send on the next re-connection attempt even before the
        // stream open.
        packetWriter.queue.putIfNotShutdown(ackAnswer);
    }

    /**
     * Add a Stanza acknowledged listener.
     * <p>
     * Those listeners will be invoked every time a Stanza has been acknowledged by the server. The will not get
     * automatically removed. Consider using {@link #addStanzaIdAcknowledgedListener(String, StanzaListener)} when
     * possible.
     * </p>
     *
     * @param listener the listener to add.
     */
    public void addStanzaAcknowledgedListener(StanzaListener listener) {
        stanzaAcknowledgedListeners.add(listener);
    }

    /**
     * Remove the given Stanza acknowledged listener.
     *
     * @param listener the listener.
     * @return true if the listener was removed.
     */
    public boolean removeStanzaAcknowledgedListener(StanzaListener listener) {
        return stanzaAcknowledgedListeners.remove(listener);
    }

    /**
     * Remove all stanza acknowledged listeners.
     */
    public void removeAllStanzaAcknowledgedListeners() {
        stanzaAcknowledgedListeners.clear();
    }

    /**
     * Add a Stanza dropped listener.
     * <p>
     * Those listeners will be invoked every time a Stanza has been dropped due to a failed SM resume. They will not get
     * automatically removed. If at least one StanzaDroppedListener is configured, no attempt will be made to retransmit
     * the Stanzas.
     * </p>
     *
     * @param listener the listener to add.
     * @since 4.3.3
     */
    public void addStanzaDroppedListener(StanzaListener listener) {
        stanzaDroppedListeners.add(listener);
    }

    /**
     * Remove the given Stanza dropped listener.
     *
     * @param listener the listener.
     * @return true if the listener was removed.
     * @since 4.3.3
     */
    public boolean removeStanzaDroppedListener(StanzaListener listener) {
        return stanzaDroppedListeners.remove(listener);
    }

    /**
     * Add a new Stanza ID acknowledged listener for the given ID.
     * <p>
     * The listener will be invoked if the stanza with the given ID was acknowledged by the server. It will
     * automatically be removed after the listener was run.
     * </p>
     *
     * @param id the stanza ID.
     * @param listener the listener to invoke.
     * @return the previous listener for this stanza ID or null.
     * @throws StreamManagementNotEnabledException if Stream Management is not enabled.
     */
    @SuppressWarnings("FutureReturnValueIgnored")
    public StanzaListener addStanzaIdAcknowledgedListener(final String id, StanzaListener listener) throws StreamManagementNotEnabledException {
        // Prevent users from adding callbacks that will never get removed
        if (!smWasEnabledAtLeastOnce) {
            throw new StreamManagementException.StreamManagementNotEnabledException();
        }
        // Remove the listener after max. 3 hours
        final int removeAfterSeconds = Math.min(getMaxSmResumptionTime(), 3 * 60 * 60);
        schedule(new Runnable() {
            @Override
            public void run() {
                stanzaIdAcknowledgedListeners.remove(id);
            }
        }, removeAfterSeconds, TimeUnit.SECONDS);
        return stanzaIdAcknowledgedListeners.put(id, listener);
    }

    /**
     * Remove the Stanza ID acknowledged listener for the given ID.
     *
     * @param id the stanza ID.
     * @return true if the listener was found and removed, false otherwise.
     */
    public StanzaListener removeStanzaIdAcknowledgedListener(String id) {
        return stanzaIdAcknowledgedListeners.remove(id);
    }

    /**
     * Removes all Stanza ID acknowledged listeners.
     */
    public void removeAllStanzaIdAcknowledgedListeners() {
        stanzaIdAcknowledgedListeners.clear();
    }

    /**
     * Returns true if Stream Management is supported by the server.
     *
     * @return true if Stream Management is supported by the server.
     */
    public boolean isSmAvailable() {
        return hasFeature(StreamManagementFeature.ELEMENT, StreamManagement.NAMESPACE);
    }

    /**
     * Returns true if Stream Management was successfully negotiated with the server.
     *
     * @return true if Stream Management was negotiated.
     */
    public boolean isSmEnabled() {
        return smEnabledSyncPoint;
    }

    /**
     * Returns true if the stream was successfully resumed with help of Stream Management.
     *
     * @return true if the stream was resumed.
     */
    public boolean streamWasResumed() {
        return smResumedSyncPoint == SyncPointState.successful;
    }

    /**
     * Returns true if the connection is disconnected by a Stream resumption via Stream Management is possible.
     *
     * @return true if disconnected but resumption possible.
     */
    public boolean isDisconnectedButSmResumptionPossible() {
        return disconnectedButResumeable && isSmResumptionPossible();
    }

    /**
     * Returns true if the stream is resumable.
     *
     * @return true if the stream is resumable.
     */
    public boolean isSmResumptionPossible() {
        // There is no resumable stream available
        if (smSessionId == null)
            return false;

        final Long shutdownTimestamp = packetWriter.shutdownTimestamp;
        // Seems like we are already reconnected, report true
        if (shutdownTimestamp == null) {
            return true;
        }

        // See if resumption time is over
        long current = System.currentTimeMillis();
        long maxResumptionMillies = ((long) getMaxSmResumptionTime()) * 1000;
        if (current > shutdownTimestamp + maxResumptionMillies) {
            // Stream resumption is *not* possible if the current timestamp is greater then the greatest timestamp where
            // resumption is possible
            return false;
        } else {
            return true;
        }
    }

    /**
     * Drop the stream management state. Sets {@link #smSessionId} and
     * {@link #unacknowledgedStanzas} to <code>null</code>.
     */
    private void dropSmState() {
        // clientHandledCount and serverHandledCount will be reset on <enable/> and <enabled/>
        // respective. No need to reset them here.
        smSessionId = null;
        unacknowledgedStanzas = null;
    }

    /**
     * Get the maximum resumption time in seconds after which a managed stream can be resumed.
     * <p>
     * This method will return {@link Integer#MAX_VALUE} if neither the client nor the server specify a maximum
     * resumption time. Be aware of integer overflows when using this value, e.g. do not add arbitrary values to it
     * without checking for overflows before.
     * </p>
     *
     * @return the maximum resumption time in seconds or {@link Integer#MAX_VALUE} if none set.
     */
    public int getMaxSmResumptionTime() {
        int clientResumptionTime = smClientMaxResumptionTime > 0 ? smClientMaxResumptionTime : Integer.MAX_VALUE;
        int serverResumptionTime = smServerMaxResumptionTime > 0 ? smServerMaxResumptionTime : Integer.MAX_VALUE;
        return Math.min(clientResumptionTime, serverResumptionTime);
    }

    private void processHandledCount(long handledCount) throws StreamManagementCounterError {
        long ackedStanzasCount = SMUtils.calculateDelta(handledCount, serverHandledStanzasCount);
        final List<Stanza> ackedStanzas = new ArrayList<>(
                        ackedStanzasCount <= Integer.MAX_VALUE ? (int) ackedStanzasCount
                                        : Integer.MAX_VALUE);
        for (long i = 0; i < ackedStanzasCount; i++) {
            Stanza ackedStanza = unacknowledgedStanzas.poll();
            // If the server ack'ed a stanza, then it must be in the
            // unacknowledged stanza queue. There can be no exception.
            if (ackedStanza == null) {
                throw new StreamManagementCounterError(handledCount, serverHandledStanzasCount,
                                ackedStanzasCount, ackedStanzas);
            }
            ackedStanzas.add(ackedStanza);
        }

        boolean atLeastOneStanzaAcknowledgedListener = false;
        if (!stanzaAcknowledgedListeners.isEmpty()) {
            // If stanzaAcknowledgedListeners is not empty, the we have at least one
            atLeastOneStanzaAcknowledgedListener = true;
        }
        else {
            // Otherwise we look for a matching id in the stanza *id* acknowledged listeners
            for (Stanza ackedStanza : ackedStanzas) {
                String id = ackedStanza.getStanzaId();
                if (id != null && stanzaIdAcknowledgedListeners.containsKey(id)) {
                    atLeastOneStanzaAcknowledgedListener = true;
                    break;
                }
            }
        }

        // Only spawn a new thread if there is a chance that some listener is invoked
        if (atLeastOneStanzaAcknowledgedListener) {
            asyncGo(new Runnable() {
                @Override
                public void run() {
                    for (Stanza ackedStanza : ackedStanzas) {
                        for (StanzaListener listener : stanzaAcknowledgedListeners) {
                            try {
                                listener.processStanza(ackedStanza);
                            }
                            catch (InterruptedException | NotConnectedException | NotLoggedInException e) {
                                LOGGER.log(Level.FINER, "Received exception", e);
                            }
                        }
                        String id = ackedStanza.getStanzaId();
                        if (StringUtils.isNullOrEmpty(id)) {
                            continue;
                        }
                        StanzaListener listener = stanzaIdAcknowledgedListeners.remove(id);
                        if (listener != null) {
                            try {
                                listener.processStanza(ackedStanza);
                            }
                            catch (InterruptedException | NotConnectedException | NotLoggedInException e) {
                                LOGGER.log(Level.FINER, "Received exception", e);
                            }
                        }
                    }
                }
            });
        }

        serverHandledStanzasCount = handledCount;
    }

    /**
     * Set the default bundle and defer callback used for new connections.
     *
     * @param defaultBundleAndDeferCallback TODO javadoc me please
     * @see BundleAndDeferCallback
     * @since 4.1
     */
    public static void setDefaultBundleAndDeferCallback(BundleAndDeferCallback defaultBundleAndDeferCallback) {
        XMPPTCPConnection.defaultBundleAndDeferCallback = defaultBundleAndDeferCallback;
    }

    /**
     * Set the bundle and defer callback used for this connection.
     * <p>
     * You can use <code>null</code> as argument to reset the callback. Outgoing stanzas will then
     * no longer get deferred.
     * </p>
     *
     * @param bundleAndDeferCallback the callback or <code>null</code>.
     * @see BundleAndDeferCallback
     * @since 4.1
     */
    public void setBundleandDeferCallback(BundleAndDeferCallback bundleAndDeferCallback) {
        this.bundleAndDeferCallback = bundleAndDeferCallback;
    }

}
