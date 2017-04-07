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

import org.jivesoftware.smack.AbstractConnectionListener;
import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionConfiguration.DnssecMode;
import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.AlreadyConnectedException;
import org.jivesoftware.smack.SmackException.AlreadyLoggedInException;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.SmackException.ConnectionException;
import org.jivesoftware.smack.SmackException.SecurityRequiredByServerException;
import org.jivesoftware.smack.SynchronizationPoint;
import org.jivesoftware.smack.XMPPException.FailedNonzaException;
import org.jivesoftware.smack.XMPPException.StreamErrorException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.compress.packet.Compressed;
import org.jivesoftware.smack.compression.XMPPInputOutputStream;
import org.jivesoftware.smack.filter.StanzaFilter;
import org.jivesoftware.smack.compress.packet.Compress;
import org.jivesoftware.smack.packet.Element;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.StreamOpen;
import org.jivesoftware.smack.packet.Stanza;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.StartTls;
import org.jivesoftware.smack.packet.StreamError;
import org.jivesoftware.smack.sasl.packet.SaslStreamElements;
import org.jivesoftware.smack.sasl.packet.SaslStreamElements.Challenge;
import org.jivesoftware.smack.sasl.packet.SaslStreamElements.SASLFailure;
import org.jivesoftware.smack.sasl.packet.SaslStreamElements.Success;
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
import org.jivesoftware.smack.packet.Nonza;
import org.jivesoftware.smack.proxy.ProxyInfo;
import org.jivesoftware.smack.util.ArrayBlockingQueueWithShutdown;
import org.jivesoftware.smack.util.Async;
import org.jivesoftware.smack.util.DNSUtil;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.TLSUtils;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jivesoftware.smack.util.dns.HostAddress;
import org.jivesoftware.smack.util.dns.SmackDaneProvider;
import org.jivesoftware.smack.util.dns.SmackDaneVerifier;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.jid.parts.Resourcepart;
import org.jxmpp.stringprep.XmppStringprepException;
import org.jxmpp.util.XmppStringUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import javax.net.SocketFactory;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.PasswordCallback;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.SecureRandom;
import java.security.Security;
import java.security.UnrecoverableKeyException;
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
    protected PacketWriter packetWriter;

    /**
     * Protected access level because of unit test purposes
     */
    protected PacketReader packetReader;

    private final SynchronizationPoint<Exception> initalOpenStreamSend = new SynchronizationPoint<>(
                    this, "initial open stream element send to server");

    /**
     * 
     */
    private final SynchronizationPoint<XMPPException> maybeCompressFeaturesReceived = new SynchronizationPoint<XMPPException>(
                    this, "stream compression feature");

    /**
     * 
     */
    private final SynchronizationPoint<SmackException> compressSyncPoint = new SynchronizationPoint<>(
                    this, "stream compression");

    /**
     * A synchronization point which is successful if this connection has received the closing
     * stream element from the remote end-point, i.e. the server.
     */
    private final SynchronizationPoint<Exception> closingStreamReceived = new SynchronizationPoint<>(
                    this, "stream closing element received");

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
     * {@link #unacknowledgedStanzas}.
     */
    private String smSessionId;

    private final SynchronizationPoint<FailedNonzaException> smResumedSyncPoint = new SynchronizationPoint<>(
                    this, "stream resumed element");

    private final SynchronizationPoint<SmackException> smEnabledSyncPoint = new SynchronizationPoint<>(
                    this, "stream enabled element");

    /**
     * The client's preferred maximum resumption time in seconds.
     */
    private int smClientMaxResumptionTime = -1;

    /**
     * The server's preferred maximum resumption time in seconds.
     */
    private int smServerMaxResumptimTime = -1;

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
     * We use a {@link ConccurrentLinkedQueue} here in order to allow the listeners to remove
     * themselves after they have been invoked.
     * </p>
     */
    private final Collection<StanzaListener> stanzaAcknowledgedListeners = new ConcurrentLinkedQueue<StanzaListener>();

    /**
     * This listeners are invoked for a acknowledged stanza that has the given stanza ID. They will
     * only be invoked once and automatically removed after that.
     */
    private final Map<String, StanzaListener> stanzaIdAcknowledgedListeners = new ConcurrentHashMap<String, StanzaListener>();

    /**
     * Predicates that determine if an stream management ack should be requested from the server.
     * <p>
     * We use a linked hash set here, so that the order how the predicates are added matches the
     * order in which they are invoked in order to determine if an ack request should be send or not.
     * </p>
     */
    private final Set<StanzaFilter> requestAckPredicates = new LinkedHashSet<StanzaFilter>();

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
        addConnectionListener(new AbstractConnectionListener() {
            @Override
            public void connectionClosedOnError(Exception e) {
                if (e instanceof XMPPException.StreamErrorException) {
                    dropSmState();
                }
            }
        });
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
     * @throws XmppStringprepException 
     */
    public XMPPTCPConnection(CharSequence jid, String password) throws XmppStringprepException {
        this(XmppStringUtils.parseLocalpart(jid.toString()), password, XmppStringUtils.parseDomain(jid.toString()));
    }

    /**
     * Creates a new XMPP connection over TCP.
     * <p>
     * This is the simplest constructor for connecting to an XMPP server. Alternatively,
     * you can get fine-grained control over connection settings using the
     * {@link #XMPPTCPConnection(XMPPTCPConnectionConfiguration)} constructor.
     * </p>
     * @param username
     * @param password
     * @param serviceName
     * @throws XmppStringprepException 
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
        saslAuthentication.authenticate(username, password, config.getAuthzid(), sslSession);

        // If compression is enabled then request the server to use stream compression. XEP-170
        // recommends to perform stream compression before resource binding.
        maybeEnableCompression();

        if (isSmResumptionPossible()) {
            smResumedSyncPoint.sendAndWaitForResponse(new Resume(clientHandledStanzasCount, smSessionId));
            if (smResumedSyncPoint.wasSuccessful()) {
                // We successfully resumed the stream, be done here
                afterSuccessfulLogin(true);
                return;
            }
            // SM resumption failed, what Smack does here is to report success of
            // lastFeaturesReceived in case of sm resumption was answered with 'failed' so that
            // normal resource binding can be tried.
            LOGGER.fine("Stream resumption failed, continuing with normal stream establishment process");
        }

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
            // XEP-198 3. Enabling Stream Management. If the server response to 'Enable' is 'Failed'
            // then this is a non recoverable error and we therefore throw an exception.
            smEnabledSyncPoint.sendAndWaitForResponseOrThrow(new Enable(useSmResumption, smClientMaxResumptionTime));
            synchronized (requestAckPredicates) {
                if (requestAckPredicates.isEmpty()) {
                    // Assure that we have at lest one predicate set up that so that we request acks
                    // for the server and eventually flush some stanzas from the unacknowledged
                    // stanza queue
                    requestAckPredicates.add(Predicate.forMessagesOrAfter5Stanzas());
                }
            }
        }
        // (Re-)send the stanzas *after* we tried to enable SM
        for (Stanza stanza : previouslyUnackedStanzas) {
            sendStanzaInternal(stanza);
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

    /**
     * Performs an unclean disconnect and shutdown of the connection. Does not send a closing stream stanza.
     */
    public synchronized void instantShutdown() {
        shutdown(true);
    }

    private void shutdown(boolean instant) {
        if (disconnectedButResumeable) {
            return;
        }

        // First shutdown the writer, this will result in a closing stream element getting send to
        // the server
        if (packetWriter != null) {
            LOGGER.finer("PacketWriter shutdown()");
            packetWriter.shutdown(instant);
        }
        LOGGER.finer("PacketWriter has been shut down");

        if (!instant) {
            try {
                // After we send the closing stream element, check if there was already a
                // closing stream element sent by the server or wait with a timeout for a
                // closing stream element to be received from the server.
                @SuppressWarnings("unused")
                Exception res = closingStreamReceived.checkIfSuccessOrWait();
            } catch (InterruptedException | NoResponseException e) {
                LOGGER.log(Level.INFO, "Exception while waiting for closing stream element from the server " + this, e);
            }
        }

        if (packetReader != null) {
            LOGGER.finer("PacketReader shutdown()");
                packetReader.shutdown();
        }
        LOGGER.finer("PacketReader has been shut down");

        try {
                socket.close();
        } catch (Exception e) {
                LOGGER.log(Level.WARNING, "shutdown", e);
        }

        setWasAuthenticated();
        // If we are able to resume the stream, then don't set
        // connected/authenticated/usingTLS to false since we like behave like we are still
        // connected (e.g. sendStanza should not throw a NotConnectedException).
        if (isSmResumptionPossible() && instant) {
            disconnectedButResumeable = true;
        } else {
            disconnectedButResumeable = false;
            // Reset the stream management session id to null, since if the stream is cleanly closed, i.e. sending a closing
            // stream tag, there is no longer a stream to resume.
            smSessionId = null;
        }
        authenticated = false;
        connected = false;
        secureSocket = null;
        reader = null;
        writer = null;

        maybeCompressFeaturesReceived.init();
        compressSyncPoint.init();
        smResumedSyncPoint.init();
        smEnabledSyncPoint.init();
        initalOpenStreamSend.init();
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

    private void connectUsingConfiguration() throws ConnectionException, IOException {
        List<HostAddress> failedAddresses = populateHostAddresses();
        SocketFactory socketFactory = config.getSocketFactory();
        ProxyInfo proxyInfo = config.getProxyInfo();
        int timeout = config.getConnectTimeout();
        if (socketFactory == null) {
            socketFactory = SocketFactory.getDefault();
        }
        for (HostAddress hostAddress : hostAddresses) {
            Iterator<InetAddress> inetAddresses = null;
            String host = hostAddress.getFQDN();
            int port = hostAddress.getPort();
            if (proxyInfo == null) {
                inetAddresses = hostAddress.getInetAddresses().iterator();
                assert(inetAddresses.hasNext());

                innerloop: while (inetAddresses.hasNext()) {
                    // Create a *new* Socket before every connection attempt, i.e. connect() call, since Sockets are not
                    // re-usable after a failed connection attempt. See also SMACK-724.
                    socket = socketFactory.createSocket();

                    final InetAddress inetAddress = inetAddresses.next();
                    final String inetAddressAndPort = inetAddress + " at port " + port;
                    LOGGER.finer("Trying to establish TCP connection to " + inetAddressAndPort);
                    try {
                        socket.connect(new InetSocketAddress(inetAddress, port), timeout);
                    } catch (Exception e) {
                        hostAddress.setException(inetAddress, e);
                        if (inetAddresses.hasNext()) {
                            continue innerloop;
                        } else {
                            break innerloop;
                        }
                    }
                    LOGGER.finer("Established TCP connection to " + inetAddressAndPort);
                    // We found a host to connect to, return here
                    this.host = host;
                    this.port = port;
                    return;
                }
                failedAddresses.add(hostAddress);
            } else {
                socket = socketFactory.createSocket();
                StringUtils.requireNotNullOrEmpty(host, "Host of HostAddress " + hostAddress + " must not be null when using a Proxy");
                final String hostAndPort = host + " at port " + port;
                LOGGER.finer("Trying to establish TCP connection via Proxy to " + hostAndPort);
                try {
                    proxyInfo.getProxySocketConnection().connect(socket, host, port, timeout);
                } catch (IOException e) {
                    hostAddress.setException(e);
                    continue;
                }
                LOGGER.finer("Established TCP connection to " + hostAndPort);
                // We found a host to connect to, return here
                this.host = host;
                this.port = port;
                return;
            }
        }
        // There are no more host addresses to try
        // throw an exception and report all tried
        // HostAddresses in the exception
        throw ConnectionException.from(failedAddresses);
    }

    /**
     * Initializes the connection by creating a stanza(/packet) reader and writer and opening a
     * XMPP stream to the server.
     *
     * @throws XMPPException if establishing a connection to the server fails.
     * @throws SmackException if the server failes to respond back or if there is anther error.
     * @throws IOException 
     */
    private void initConnection() throws IOException {
        boolean isFirstInitialization = packetReader == null || packetWriter == null;
        compressionHandler = null;

        // Set the reader and writer instance variables
        initReaderAndWriter();

        if (isFirstInitialization) {
            packetWriter = new PacketWriter();
            packetReader = new PacketReader();

            // If debugging is enabled, we should start the thread that will listen for
            // all packets and then log them.
            if (config.isDebuggerEnabled()) {
                addAsyncStanzaListener(debugger.getReaderListener(), null);
                if (debugger.getWriterListener() != null) {
                    addPacketSendingListener(debugger.getWriterListener(), null);
                }
            }
        }
        // Start the packet writer. This will open an XMPP stream to the server
        packetWriter.init();
        // Start the packet reader. The startup() method will block until we
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
     * @throws IOException 
     * @throws CertificateException 
     * @throws NoSuchAlgorithmException 
     * @throws NoSuchProviderException 
     * @throws KeyStoreException 
     * @throws UnrecoverableKeyException 
     * @throws KeyManagementException 
     * @throws SmackException 
     * @throws Exception if an exception occurs.
     */
    @SuppressWarnings("LiteralClassName")
    private void proceedTLSReceived() throws NoSuchAlgorithmException, CertificateException, IOException, KeyStoreException, NoSuchProviderException, UnrecoverableKeyException, KeyManagementException, SmackException {
        SSLContext context = this.config.getCustomSSLContext();
        KeyStore ks = null;
        KeyManager[] kms = null;
        PasswordCallback pcb = null;
        SmackDaneVerifier daneVerifier = null;

        if (config.getDnssecMode() == DnssecMode.needsDnssecAndDane) {
            SmackDaneProvider daneProvider = DNSUtil.getDaneProvider();
            if (daneProvider == null) {
                throw new UnsupportedOperationException("DANE enabled but no SmackDaneProvider configured");
            }
            daneVerifier = daneProvider.newInstance();
            if (daneVerifier == null) {
                throw new IllegalStateException("DANE requested but DANE provider did not return a DANE verifier");
            }
        }

        if (context == null) {
            final String keyStoreType = config.getKeystoreType();
            final CallbackHandler callbackHandler = config.getCallbackHandler();
            final String keystorePath = config.getKeystorePath();
            if ("PKCS11".equals(keyStoreType)) {
                try {
                    Constructor<?> c = Class.forName("sun.security.pkcs11.SunPKCS11").getConstructor(InputStream.class);
                    String pkcs11Config = "name = SmartCard\nlibrary = "+config.getPKCS11Library();
                    ByteArrayInputStream config = new ByteArrayInputStream(pkcs11Config.getBytes(StringUtils.UTF8));
                    Provider p = (Provider)c.newInstance(config);
                    Security.addProvider(p);
                    ks = KeyStore.getInstance("PKCS11",p);
                    pcb = new PasswordCallback("PKCS11 Password: ",false);
                    callbackHandler.handle(new Callback[]{pcb});
                    ks.load(null,pcb.getPassword());
                }
                catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Exception", e);
                    ks = null;
                }
            }
            else if ("Apple".equals(keyStoreType)) {
                ks = KeyStore.getInstance("KeychainStore","Apple");
                ks.load(null,null);
                //pcb = new PasswordCallback("Apple Keychain",false);
                //pcb.setPassword(null);
            }
            else if (keyStoreType != null){
                ks = KeyStore.getInstance(keyStoreType);
                if (callbackHandler != null && StringUtils.isNotEmpty(keystorePath)) {
                    try {
                        pcb = new PasswordCallback("Keystore Password: ", false);
                        callbackHandler.handle(new Callback[] { pcb });
                        ks.load(new FileInputStream(keystorePath), pcb.getPassword());
                    }
                    catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Exception", e);
                        ks = null;
                    }
                } else {
                    ks.load(null, null);
                }
            }

            if (ks != null) {
                KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
                try {
                    if (pcb == null) {
                        kmf.init(ks, null);
                    }
                    else {
                        kmf.init(ks, pcb.getPassword());
                        pcb.clearPassword();
                    }
                    kms = kmf.getKeyManagers();
                }
                catch (NullPointerException npe) {
                    LOGGER.log(Level.WARNING, "NullPointerException", npe);
                }
            }

            // If the user didn't specify a SSLContext, use the default one
            context = SSLContext.getInstance("TLS");

            final SecureRandom secureRandom = new java.security.SecureRandom();
            X509TrustManager customTrustManager = config.getCustomX509TrustManager();

            if (daneVerifier != null) {
                // User requested DANE verification.
                daneVerifier.init(context, kms, customTrustManager, secureRandom);
            } else {
                TrustManager[] customTrustManagers = null;
                if (customTrustManager != null) {
                    customTrustManagers = new TrustManager[] { customTrustManager };
                }
                context.init(kms, customTrustManagers, secureRandom);
            }
        }

        Socket plain = socket;
        // Secure the plain connection
        socket = context.getSocketFactory().createSocket(plain,
                host, plain.getPort(), true);

        final SSLSocket sslSocket = (SSLSocket) socket;
        // Immediately set the enabled SSL protocols and ciphers. See SMACK-712 why this is
        // important (at least on certain platforms) and it seems to be a good idea anyways to
        // prevent an accidental implicit handshake.
        TLSUtils.setEnabledProtocolsAndCiphers(sslSocket, config.getEnabledSSLProtocols(), config.getEnabledSSLCiphers());

        // Initialize the reader and writer with the new secured version
        initReaderAndWriter();

        // Proceed to do the handshake
        sslSocket.startHandshake();

        if (daneVerifier != null) {
            daneVerifier.finish(sslSocket);
        }

        final HostnameVerifier verifier = getConfiguration().getHostnameVerifier();
        if (verifier == null) {
                throw new IllegalStateException("No HostnameVerifier set. Use connectionConfiguration.setHostnameVerifier() to configure.");
        } else if (!verifier.verify(getXMPPServiceDomain().toString(), sslSocket.getSession())) {
            throw new CertificateException("Hostname verification of certificate failed. Certificate does not authenticate " + getXMPPServiceDomain());
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
        for (XMPPInputOutputStream handler : SmackConfiguration.getCompresionHandlers()) {
                String method = handler.getCompressionMethod();
                if (compression.getMethods().contains(method))
                    return handler;
        }
        return null;
    }

    @Override
    public boolean isUsingCompression() {
        return compressionHandler != null && compressSyncPoint.wasSuccessful();
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
     * @throws NotConnectedException 
     * @throws SmackException
     * @throws NoResponseException 
     * @throws InterruptedException 
     */
    private void maybeEnableCompression() throws NotConnectedException, NoResponseException, SmackException, InterruptedException {
        if (!config.isCompressionEnabled()) {
            return;
        }
        maybeCompressFeaturesReceived.checkIfSuccessOrWait();
        Compress.Feature compression = getFeature(Compress.Feature.ELEMENT, Compress.NAMESPACE);
        if (compression == null) {
            // Server does not support compression
            return;
        }
        // If stream compression was offered by the server and we want to use
        // compression then send compression request to the server
        if ((compressionHandler = maybeGetCompressionHandler(compression)) != null) {
            compressSyncPoint.sendAndWaitForResponseOrThrow(new Compress(compressionHandler.getCompressionMethod()));
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
     * @throws SmackException 
     * @throws IOException 
     * @throws InterruptedException 
     */
    @Override
    protected void connectInternal() throws SmackException, IOException, XMPPException, InterruptedException {
        closingStreamReceived.init();
        // Establishes the TCP connection to the server and does setup the reader and writer. Throws an exception if
        // there is an error establishing the connection
        connectUsingConfiguration();

        // We connected successfully to the servers TCP port
        initConnection();
    }

    /**
     * Sends out a notification that there was an error with the connection
     * and closes the connection. Also prints the stack trace of the given exception
     *
     * @param e the exception that causes the connection close event.
     */
    private synchronized void notifyConnectionError(Exception e) {
        // Listeners were already notified of the exception, return right here.
        if ((packetReader == null || packetReader.done) &&
                (packetWriter == null || packetWriter.done())) return;

        // Closes the connection temporary. A reconnection is possible
        // Note that a connection listener of XMPPTCPConnection will drop the SM state in
        // case the Exception is a StreamErrorException.
        instantShutdown();

        // Notify connection listeners of the error.
        callConnectionClosedOnErrorListener(e);
    }

    /**
     * For unit testing purposes
     *
     * @param writer
     */
    protected void setWriter(Writer writer) {
        this.writer = writer;
    }

    @Override
    protected void afterFeaturesReceived() throws NotConnectedException, InterruptedException {
        StartTls startTlsFeature = getFeature(StartTls.ELEMENT, StartTls.NAMESPACE);
        if (startTlsFeature != null) {
            if (startTlsFeature.required() && config.getSecurityMode() == SecurityMode.disabled) {
                SmackException smackException = new SecurityRequiredByServerException();
                tlsHandled.reportFailure(smackException);
                notifyConnectionError(smackException);
                return;
            }

            if (config.getSecurityMode() != ConnectionConfiguration.SecurityMode.disabled) {
                sendNonza(new StartTls());
            } else {
                tlsHandled.reportSuccess();
            }
        } else {
            tlsHandled.reportSuccess();
        }

        if (getSASLAuthentication().authenticationSuccessful()) {
            // If we have received features after the SASL has been successfully completed, then we
            // have also *maybe* received, as it is an optional feature, the compression feature
            // from the server.
            maybeCompressFeaturesReceived.reportSuccess();
        }
    }

    /**
     * Resets the parser using the latest connection's reader. Reseting the parser is necessary
     * when the plain connection has been secured or when a new opening stream element is going
     * to be sent by the server.
     *
     * @throws SmackException if the parser could not be reset.
     * @throws InterruptedException 
     */
    void openStream() throws SmackException, InterruptedException {
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
        sendNonza(new StreamOpen(to, from, id));
        try {
            packetReader.parser = PacketParserUtils.newXmppParser(reader);
        }
        catch (XmlPullParserException e) {
            throw new SmackException(e);
        }
    }

    protected class PacketReader {

        XmlPullParser parser;

        private volatile boolean done;

        /**
         * Initializes the reader in order to be used. The reader is initialized during the
         * first connection and when reconnecting due to an abruptly disconnection.
         */
        void init() {
            done = false;

            Async.go(new Runnable() {
                @Override
                public void run() {
                    parsePackets();
                }
            }, "Smack Packet Reader (" + getConnectionCounter() + ")");
         }

        /**
         * Shuts the stanza(/packet) reader down. This method simply sets the 'done' flag to true.
         */
        void shutdown() {
            done = true;
        }

        /**
         * Parse top-level packets in order to process them further.
         *
         * @param thread the thread that is being used by the reader to parse incoming packets.
         */
        private void parsePackets() {
            try {
                initalOpenStreamSend.checkIfSuccessOrWait();
                int eventType = parser.getEventType();
                while (!done) {
                    switch (eventType) {
                    case XmlPullParser.START_TAG:
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
                            // We found an opening stream.
                            if ("jabber:client".equals(parser.getNamespace(null))) {
                                streamId = parser.getAttributeValue("", "id");
                                String reportedServerDomain = parser.getAttributeValue("", "from");
                                assert(config.getXMPPServiceDomain().equals(reportedServerDomain));
                            }
                            break;
                        case "error":
                            StreamError streamError = PacketParserUtils.parseStreamError(parser);
                            saslFeatureReceived.reportFailure(new StreamErrorException(streamError));
                            // Mark the tlsHandled sync point as success, we will use the saslFeatureReceived sync
                            // point to report the error, which is checked immediately after tlsHandled in
                            // connectInternal().
                            tlsHandled.reportSuccess();
                            throw new StreamErrorException(streamError);
                        case "features":
                            parseFeatures(parser);
                            break;
                        case "proceed":
                            try {
                                // Secure the connection by negotiating TLS
                                proceedTLSReceived();
                                // Send a new opening stream to the server
                                openStream();
                            }
                            catch (Exception e) {
                                SmackException smackException = new SmackException(e);
                                tlsHandled.reportFailure(smackException);
                                throw e;
                            }
                            break;
                        case "failure":
                            String namespace = parser.getNamespace(null);
                            switch (namespace) {
                            case "urn:ietf:params:xml:ns:xmpp-tls":
                                // TLS negotiation has failed. The server will close the connection
                                // TODO Parse failure stanza
                                throw new SmackException("TLS negotiation has failed");
                            case "http://jabber.org/protocol/compress":
                                // Stream compression has been denied. This is a recoverable
                                // situation. It is still possible to authenticate and
                                // use the connection but using an uncompressed connection
                                // TODO Parse failure stanza
                                compressSyncPoint.reportFailure(new SmackException(
                                                "Could not establish compression"));
                                break;
                            case SaslStreamElements.NAMESPACE:
                                // SASL authentication has failed. The server may close the connection
                                // depending on the number of retries
                                final SASLFailure failure = PacketParserUtils.parseSASLFailure(parser);
                                getSASLAuthentication().authenticationFailed(failure);
                                break;
                            }
                            break;
                        case Challenge.ELEMENT:
                            // The server is challenging the SASL authentication made by the client
                            String challengeData = parser.nextText();
                            getSASLAuthentication().challengeReceived(challengeData);
                            break;
                        case Success.ELEMENT:
                            Success success = new Success(parser.nextText());
                            // We now need to bind a resource for the connection
                            // Open a new stream and wait for the response
                            openStream();
                            // The SASL authentication with the server was successful. The next step
                            // will be to bind the resource
                            getSASLAuthentication().authenticated(success);
                            break;
                        case Compressed.ELEMENT:
                            // Server confirmed that it's possible to use stream compression. Start
                            // stream compression
                            // Initialize the reader and writer with the new compressed version
                            initReaderAndWriter();
                            // Send a new opening stream to the server
                            openStream();
                            // Notify that compression is being used
                            compressSyncPoint.reportSuccess();
                            break;
                        case Enabled.ELEMENT:
                            Enabled enabled = ParseStreamManagement.enabled(parser);
                            if (enabled.isResumeSet()) {
                                smSessionId = enabled.getId();
                                if (StringUtils.isNullOrEmpty(smSessionId)) {
                                    SmackException xmppException = new SmackException("Stream Management 'enabled' element with resume attribute but without session id received");
                                    smEnabledSyncPoint.reportFailure(xmppException);
                                    throw xmppException;
                                }
                                smServerMaxResumptimTime = enabled.getMaxResumptionTime();
                            } else {
                                // Mark this a non-resumable stream by setting smSessionId to null
                                smSessionId = null;
                            }
                            clientHandledStanzasCount = 0;
                            smWasEnabledAtLeastOnce = true;
                            smEnabledSyncPoint.reportSuccess();
                            LOGGER.fine("Stream Management (XEP-198): succesfully enabled");
                            break;
                        case Failed.ELEMENT:
                            Failed failed = ParseStreamManagement.failed(parser);
                            FailedNonzaException xmppException = new FailedNonzaException(failed, failed.getXMPPErrorCondition());
                            // If only XEP-198 would specify different failure elements for the SM
                            // enable and SM resume failure case. But this is not the case, so we
                            // need to determine if this is a 'Failed' response for either 'Enable'
                            // or 'Resume'.
                            if (smResumedSyncPoint.requestSent()) {
                                smResumedSyncPoint.reportFailure(xmppException);
                            }
                            else {
                                if (!smEnabledSyncPoint.requestSent()) {
                                    throw new IllegalStateException("Failed element received but SM was not previously enabled");
                                }
                                smEnabledSyncPoint.reportFailure(new SmackException(xmppException));
                                // Report success for last lastFeaturesReceived so that in case a
                                // failed resumption, we can continue with normal resource binding.
                                // See text of XEP-198 5. below Example 11.
                                lastFeaturesReceived.reportSuccess();
                            }
                            break;
                        case Resumed.ELEMENT:
                            Resumed resumed = ParseStreamManagement.resumed(parser);
                            if (!smSessionId.equals(resumed.getPrevId())) {
                                throw new StreamIdDoesNotMatchException(smSessionId, resumed.getPrevId());
                            }
                            // Mark SM as enabled and resumption as successful.
                            smResumedSyncPoint.reportSuccess();
                            smEnabledSyncPoint.reportSuccess();
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
                            LOGGER.fine("Stream Management (XEP-198): Stream resumed");
                            break;
                        case AckAnswer.ELEMENT:
                            AckAnswer ackAnswer = ParseStreamManagement.ackAnswer(parser);
                            processHandledCount(ackAnswer.getHandledCount());
                            break;
                        case AckRequest.ELEMENT:
                            ParseStreamManagement.ackRequest(parser);
                            if (smEnabledSyncPoint.wasSuccessful()) {
                                sendSmAcknowledgementInternal();
                            } else {
                                LOGGER.warning("SM Ack Request received while SM is not enabled");
                            }
                            break;
                         default:
                             LOGGER.warning("Unknown top level stream element: " + name);
                             break;
                        }
                        break;
                    case XmlPullParser.END_TAG:
                        if (parser.getName().equals("stream")) {
                            if (!parser.getNamespace().equals("http://etherx.jabber.org/streams")) {
                                LOGGER.warning(XMPPTCPConnection.this +  " </stream> but different namespace " + parser.getNamespace());
                                break;
                            }

                            // Check if the queue was already shut down before reporting success on closing stream tag
                            // received. This avoids a race if there is a disconnect(), followed by a connect(), which
                            // did re-start the queue again, causing this writer to assume that the queue is not
                            // shutdown, which results in a call to disconnect().
                            final boolean queueWasShutdown = packetWriter.queue.isShutdown();
                            closingStreamReceived.reportSuccess();

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
                                disconnect();
                            }
                        }
                        break;
                    case XmlPullParser.END_DOCUMENT:
                        // END_DOCUMENT only happens in an error case, as otherwise we would see a
                        // closing stream element before.
                        throw new SmackException(
                                        "Parser got END_DOCUMENT event. This could happen e.g. if the server closed the connection without sending a closing stream element");
                    }
                    eventType = parser.next();
                }
            }
            catch (Exception e) {
                closingStreamReceived.reportFailure(e);
                // The exception can be ignored if the the connection is 'done'
                // or if the it was caused because the socket got closed
                if (!(done || packetWriter.queue.isShutdown())) {
                    // Close the connection and notify connection listeners of the
                    // error.
                    notifyConnectionError(e);
                }
            }
        }
    }

    protected class PacketWriter {
        public static final int QUEUE_SIZE = XMPPTCPConnection.QUEUE_SIZE;

        private final ArrayBlockingQueueWithShutdown<Element> queue = new ArrayBlockingQueueWithShutdown<Element>(
                        QUEUE_SIZE, true);

        /**
         * Needs to be protected for unit testing purposes.
         */
        protected SynchronizationPoint<NoResponseException> shutdownDone = new SynchronizationPoint<NoResponseException>(
                        XMPPTCPConnection.this, "shutdown completed");

        /**
         * If set, the stanza(/packet) writer is shut down
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

        /** 
        * Initializes the writer in order to be used. It is called at the first connection and also 
        * is invoked if the connection is disconnected by an error.
        */ 
        void init() {
            shutdownDone.init();
            shutdownTimestamp = null;

            if (unacknowledgedStanzas != null) {
                // It's possible that there are new stanzas in the writer queue that
                // came in while we were disconnected but resumable, drain those into
                // the unacknowledged queue so that they get resent now
                drainWriterQueueToUnacknowledgedStanzas();
            }

            queue.start();
            Async.go(new Runnable() {
                @Override
                public void run() {
                    writePackets();
                }
            }, "Smack Packet Writer (" + getConnectionCounter() + ")");
        }

        private boolean done() {
            return shutdownTimestamp != null;
        }

        protected void throwNotConnectedExceptionIfDoneAndResumptionNotPossible() throws NotConnectedException {
            final boolean done = done();
            if (done) {
                final boolean smResumptionPossbile = isSmResumptionPossible();
                // Don't throw a NotConnectedException is there is an resumable stream available
                if (!smResumptionPossbile) {
                    throw new NotConnectedException(XMPPTCPConnection.this, "done=" + done
                                    + " smResumptionPossible=" + smResumptionPossbile);
                }
            }
        }

        /**
         * Sends the specified element to the server.
         *
         * @param element the element to send.
         * @throws NotConnectedException 
         * @throws InterruptedException 
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
         * Shuts down the stanza(/packet) writer. Once this method has been called, no further
         * packets will be written to the server.
         * @throws InterruptedException 
         */
        void shutdown(boolean instant) {
            instantShutdown = instant;
            queue.shutdown();
            shutdownTimestamp = System.currentTimeMillis();
            try {
                shutdownDone.checkIfSuccessOrWait();
            }
            catch (NoResponseException | InterruptedException e) {
                LOGGER.log(Level.WARNING, "shutdownDone was not marked as successful by the writer thread", e);
            }
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
                    LOGGER.log(Level.WARNING, "Packet writer thread was interrupted. Don't do that. Use disconnect() instead.", e);
                }
            }
            return packet;
        }

        private void writePackets() {
            Exception writerException = null;
            try {
                openStream();
                initalOpenStreamSend.reportSuccess();
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
                        unacknowledgedStanzas = new ArrayBlockingQueue<>(QUEUE_SIZE);
                    }
                    maybeAddToUnacknowledgedStanzas(packet);

                    CharSequence elementXml = element.toXML();
                    if (elementXml instanceof XmlStringBuilder) {
                        ((XmlStringBuilder) elementXml).write(writer);
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
                        writer.flush();
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
                    writerException = e;
                } else {
                    LOGGER.log(Level.FINE, "Ignoring Exception in writePackets()", e);
                }
            } finally {
                LOGGER.fine("Reporting shutdownDone success in writer thread");
                shutdownDone.reportSuccess();
            }
            // Delay notifyConnectionError after shutdownDone has been reported in the finally block.
            if (writerException != null) {
                notifyConnectionError(writerException);
            }
        }

        private void drainWriterQueueToUnacknowledgedStanzas() {
            List<Element> elements = new ArrayList<Element>(queue.size());
            queue.drainTo(elements);
            for (Element element : elements) {
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
                // If the unacknowledgedStanza queue is nearly full, request an new ack
                // from the server in order to drain it
                if (unacknowledgedStanzas.size() == 0.8 * XMPPTCPConnection.QUEUE_SIZE) {
                    writer.write(AckRequest.INSTANCE.toXML().toString());
                    writer.flush();
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
     * @throws StreamManagementNotEnabledException if Stream Mangement is not enabled.
     * @throws NotConnectedException if the connection is not connected.
     * @throws InterruptedException 
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
     * "Either party MAY send an <a/> element at any time (e.g., after it has received a certain number of stanzas,
     * or after a certain period of time), even if it has not received an <r/> element from the other party."
     * </p>
     * 
     * @throws StreamManagementNotEnabledException if Stream Management is not enabled.
     * @throws NotConnectedException if the connection is not connected.
     * @throws InterruptedException 
     */
    public void sendSmAcknowledgement() throws StreamManagementNotEnabledException, NotConnectedException, InterruptedException {
        if (!isSmEnabled()) {
            throw new StreamManagementException.StreamManagementNotEnabledException();
        }
        sendSmAcknowledgementInternal();
    }

    private void sendSmAcknowledgementInternal() throws NotConnectedException, InterruptedException {
        packetWriter.sendStreamElement(new AckAnswer(clientHandledStanzasCount));
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
        // Remove the listener after max. 12 hours
        final int removeAfterSeconds = Math.min(getMaxSmResumptionTime(), 12 * 60 * 60);
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
        return smEnabledSyncPoint.wasSuccessful();
    }

    /**
     * Returns true if the stream was successfully resumed with help of Stream Management.
     * 
     * @return true if the stream was resumed.
     */
    public boolean streamWasResumed() {
        return smResumedSyncPoint.wasSuccessful();
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
        int serverResumptionTime = smServerMaxResumptimTime > 0 ? smServerMaxResumptimTime : Integer.MAX_VALUE;
        return Math.min(clientResumptionTime, serverResumptionTime);
    }

    private void processHandledCount(long handledCount) throws StreamManagementCounterError {
        long ackedStanzasCount = SMUtils.calculateDelta(handledCount, serverHandledStanzasCount);
        final List<Stanza> ackedStanzas = new ArrayList<Stanza>(
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
                            catch (InterruptedException | NotConnectedException e) {
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
                            catch (InterruptedException | NotConnectedException e) {
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
     * @param defaultBundleAndDeferCallback
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
