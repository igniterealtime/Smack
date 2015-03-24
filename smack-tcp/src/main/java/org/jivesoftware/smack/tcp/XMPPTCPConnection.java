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

import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.ConnectionConfiguration.SecurityMode;
import org.jivesoftware.smack.StanzaListener;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.AlreadyConnectedException;
import org.jivesoftware.smack.SmackException.AlreadyLoggedInException;
import org.jivesoftware.smack.SmackException.NoResponseException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.SmackException.ConnectionException;
import org.jivesoftware.smack.SmackException.SecurityRequiredByClientException;
import org.jivesoftware.smack.SmackException.SecurityRequiredByServerException;
import org.jivesoftware.smack.SmackException.SecurityRequiredException;
import org.jivesoftware.smack.SynchronizationPoint;
import org.jivesoftware.smack.XMPPException.StreamErrorException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
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
import org.jivesoftware.smack.packet.PlainStreamElement;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smack.util.ArrayBlockingQueueWithShutdown;
import org.jivesoftware.smack.util.Async;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.TLSUtils;
import org.jivesoftware.smack.util.dns.HostAddress;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;
import org.jxmpp.util.XmppStringUtils;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import javax.net.SocketFactory;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.security.auth.callback.Callback;
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
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Provider;
import java.security.Security;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;
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

    /**
     * Flag to indicate if the socket was closed intentionally by Smack.
     * <p>
     * This boolean flag is used concurrently, therefore it is marked volatile.
     * </p>
     */
    private volatile boolean socketClosed = false;

    private boolean usingTLS = false;

    /**
     * Protected access level because of unit test purposes
     */
    protected PacketWriter packetWriter;

    /**
     * Protected access level because of unit test purposes
     */
    protected PacketReader packetReader;

    private final SynchronizationPoint<Exception> initalOpenStreamSend = new SynchronizationPoint<Exception>(this);

    /**
     * 
     */
    private final SynchronizationPoint<XMPPException> maybeCompressFeaturesReceived = new SynchronizationPoint<XMPPException>(
                    this);

    /**
     * 
     */
    private final SynchronizationPoint<XMPPException> compressSyncPoint = new SynchronizationPoint<XMPPException>(
                    this);

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

    private static boolean useSmDefault = false;

    private static boolean useSmResumptionDefault = true;

    /**
     * The stream ID of the stream that is currently resumable, ie. the stream we hold the state
     * for in {@link #clientHandledStanzasCount}, {@link #serverHandledStanzasCount} and
     * {@link #unacknowledgedStanzas}.
     */
    private String smSessionId;

    private final SynchronizationPoint<XMPPException> smResumedSyncPoint = new SynchronizationPoint<XMPPException>(
                    this);

    private final SynchronizationPoint<XMPPException> smEnabledSyncPoint = new SynchronizationPoint<XMPPException>(
                    this);

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
        this(XMPPTCPConnectionConfiguration.builder().setUsernameAndPassword(username, password).setServiceName(
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
    protected synchronized void loginNonAnonymously(String username, String password, String resource) throws XMPPException, SmackException, IOException, InterruptedException {
        if (saslAuthentication.hasNonAnonymousAuthentication()) {
            // Authenticate using SASL
            if (password != null) {
                saslAuthentication.authenticate(username, password, resource);
            }
            else {
                saslAuthentication.authenticate(resource, config.getCallbackHandler());
            }
        } else {
            throw new SmackException("No non-anonymous SASL authentication mechanism available");
        }

        // If compression is enabled then request the server to use stream compression. XEP-170
        // recommends to perform stream compression before resource binding.
        if (config.isCompressionEnabled()) {
            useCompression();
        }

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

        bindResourceAndEstablishSession(resource);

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
            unacknowledgedStanzas = null;
        }
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
    public synchronized void loginAnonymously() throws XMPPException, SmackException, IOException, InterruptedException {
        // Wait with SASL auth until the SASL mechanisms have been received
        saslFeatureReceived.checkIfSuccessOrWaitOrThrow();

        if (saslAuthentication.hasAnonymousAuthentication()) {
            saslAuthentication.authenticateAnonymously();
        }
        else {
            throw new SmackException("No anonymous SASL authentication mechanism available");
        }

        // If compression is enabled then request the server to use stream compression
        if (config.isCompressionEnabled()) {
            useCompression();
        }

        bindResourceAndEstablishSession(null);

        afterSuccessfulLogin(false);
    }

    @Override
    public boolean isSecureConnection() {
        return usingTLS;
    }

    public boolean isSocketClosed() {
        return socketClosed;
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
        if (packetReader != null) {
                packetReader.shutdown();
        }
        if (packetWriter != null) {
                packetWriter.shutdown(instant);
        }

        // Set socketClosed to true. This will cause the PacketReader
        // and PacketWriter to ignore any Exceptions that are thrown
        // because of a read/write from/to a closed stream.
        // It is *important* that this is done before socket.close()!
        socketClosed = true;
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
        usingTLS = false;
        reader = null;
        writer = null;

        maybeCompressFeaturesReceived.init();
        compressSyncPoint.init();
        smResumedSyncPoint.init();
        smEnabledSyncPoint.init();
        initalOpenStreamSend.init();
    }

    @Override
    public void send(PlainStreamElement element) throws NotConnectedException, InterruptedException {
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

    private void connectUsingConfiguration() throws IOException, ConnectionException {
        List<HostAddress> failedAddresses = populateHostAddresses();
        SocketFactory socketFactory = config.getSocketFactory();
        if (socketFactory == null) {
            socketFactory = SocketFactory.getDefault();
        }
        for (HostAddress hostAddress : hostAddresses) {
            String host = hostAddress.getFQDN();
            int port = hostAddress.getPort();
            socket = socketFactory.createSocket();
            try {
                Iterator<InetAddress> inetAddresses = Arrays.asList(InetAddress.getAllByName(host)).iterator();
                if (!inetAddresses.hasNext()) {
                    // This should not happen
                    LOGGER.warning("InetAddress.getAllByName() returned empty result array.");
                    throw new UnknownHostException(host);
                }
                innerloop: while (inetAddresses.hasNext()) {
                    final InetAddress inetAddress = inetAddresses.next();
                    final String inetAddressAndPort = inetAddress + " at port " + port;
                    LOGGER.finer("Trying to establish TCP connection to " + inetAddressAndPort);
                    try {
                        socket.connect(new InetSocketAddress(inetAddress, port), config.getConnectTimeout());
                    } catch (Exception e) {
                        if (inetAddresses.hasNext()) {
                            continue innerloop;
                        } else {
                            throw e;
                        }
                    }
                    LOGGER.finer("Established TCP connection to " + inetAddressAndPort);
                    // We found a host to connect to, return here
                    this.host = host;
                    this.port = port;
                    return;
                }
            }
            catch (Exception e) {
                hostAddress.setException(e);
                failedAddresses.add(hostAddress);
            }
        }
        // There are no more host addresses to try
        // throw an exception and report all tried
        // HostAddresses in the exception
        throw ConnectionException.from(failedAddresses);
    }

    /**
     * Initializes the connection by creating a packet reader and writer and opening a
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
    private void proceedTLSReceived() throws NoSuchAlgorithmException, CertificateException, IOException, KeyStoreException, NoSuchProviderException, UnrecoverableKeyException, KeyManagementException, SmackException {
        SSLContext context = this.config.getCustomSSLContext();
        KeyStore ks = null;
        KeyManager[] kms = null;
        PasswordCallback pcb = null;

        if(config.getCallbackHandler() == null) {
           ks = null;
        } else if (context == null) {
            if(config.getKeystoreType().equals("NONE")) {
                ks = null;
                pcb = null;
            }
            else if(config.getKeystoreType().equals("PKCS11")) {
                try {
                    Constructor<?> c = Class.forName("sun.security.pkcs11.SunPKCS11").getConstructor(InputStream.class);
                    String pkcs11Config = "name = SmartCard\nlibrary = "+config.getPKCS11Library();
                    ByteArrayInputStream config = new ByteArrayInputStream(pkcs11Config.getBytes());
                    Provider p = (Provider)c.newInstance(config);
                    Security.addProvider(p);
                    ks = KeyStore.getInstance("PKCS11",p);
                    pcb = new PasswordCallback("PKCS11 Password: ",false);
                    this.config.getCallbackHandler().handle(new Callback[]{pcb});
                    ks.load(null,pcb.getPassword());
                }
                catch (Exception e) {
                    ks = null;
                    pcb = null;
                }
            }
            else if(config.getKeystoreType().equals("Apple")) {
                ks = KeyStore.getInstance("KeychainStore","Apple");
                ks.load(null,null);
                //pcb = new PasswordCallback("Apple Keychain",false);
                //pcb.setPassword(null);
            }
            else {
                ks = KeyStore.getInstance(config.getKeystoreType());
                try {
                    pcb = new PasswordCallback("Keystore Password: ",false);
                    config.getCallbackHandler().handle(new Callback[]{pcb});
                    ks.load(new FileInputStream(config.getKeystorePath()), pcb.getPassword());
                }
                catch(Exception e) {
                    ks = null;
                    pcb = null;
                }
            }
            KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
            try {
                if(pcb == null) {
                    kmf.init(ks,null);
                } else {
                    kmf.init(ks,pcb.getPassword());
                    pcb.clearPassword();
                }
                kms = kmf.getKeyManagers();
            } catch (NullPointerException npe) {
                kms = null;
            }
        }

        // If the user didn't specify a SSLContext, use the default one
        if (context == null) {
            context = SSLContext.getInstance("TLS");
            context.init(kms, null, new java.security.SecureRandom());
        }
        Socket plain = socket;
        // Secure the plain connection
        socket = context.getSocketFactory().createSocket(plain,
                host, plain.getPort(), true);
        // Initialize the reader and writer with the new secured version
        initReaderAndWriter();

        final SSLSocket sslSocket = (SSLSocket) socket;
        TLSUtils.setEnabledProtocolsAndCiphers(sslSocket, config.getEnabledSSLProtocols(), config.getEnabledSSLCiphers());

        // Proceed to do the handshake
        sslSocket.startHandshake();

        final HostnameVerifier verifier = getConfiguration().getHostnameVerifier();
        if (verifier == null) {
                throw new IllegalStateException("No HostnameVerifier set. Use connectionConfiguration.setHostnameVerifier() to configure.");
        } else if (!verifier.verify(getServiceName().toString(), sslSocket.getSession())) {
            throw new CertificateException("Hostname verification of certificate failed. Certificate does not authenticate " + getServiceName());
        }

        // Set that TLS was successful
        usingTLS = true;
    }

    /**
     * Returns the compression handler that can be used for one compression methods offered by the server.
     * 
     * @return a instance of XMPPInputOutputStream or null if no suitable instance was found
     * 
     */
    private XMPPInputOutputStream maybeGetCompressionHandler() {
        Compress.Feature compression = getFeature(Compress.Feature.ELEMENT, Compress.NAMESPACE);
        if (compression == null) {
            // Server does not support compression
            return null;
        }
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
     * @throws XMPPException 
     * @throws NoResponseException 
     * @throws InterruptedException 
     */
    private void useCompression() throws NotConnectedException, NoResponseException, XMPPException, InterruptedException {
        maybeCompressFeaturesReceived.checkIfSuccessOrWait();
        // If stream compression was offered by the server and we want to use
        // compression then send compression request to the server
        if ((compressionHandler = maybeGetCompressionHandler()) != null) {
            compressSyncPoint.sendAndWaitForResponseOrThrow(new Compress(compressionHandler.getCompressionMethod()));
        } else {
            LOGGER.warning("Could not enable compression because no matching handler/method pair was found");
        }
    }

    /**
     * Establishes a connection to the XMPP server and performs an automatic login
     * only if the previous connection state was logged (authenticated). It basically
     * creates and maintains a socket connection to the server.<p>
     * <p/>
     * Listeners will be preserved from a previous connection if the reconnection
     * occurs after an abrupt termination.
     *
     * @throws XMPPException if an error occurs while trying to establish the connection.
     * @throws SmackException 
     * @throws IOException 
     * @throws InterruptedException 
     */
    @Override
    protected void connectInternal() throws SmackException, IOException, XMPPException, InterruptedException {
        // Establishes the TCP connection to the server and does setup the reader and writer. Throws an exception if
        // there is an error establishing the connection
        connectUsingConfiguration();

        // We connected successfully to the servers TCP port
        socketClosed = false;
        initConnection();

        // Wait with SASL auth until the SASL mechanisms have been received
        saslFeatureReceived.checkIfSuccessOrWaitOrThrow();

        // Make note of the fact that we're now connected.
        connected = true;
        callConnectionConnectedListener();

        // Automatically makes the login if the user was previously connected successfully
        // to the server and the connection was terminated abruptly
        if (wasAuthenticated) {
            login();
            notifyReconnection();
        }
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
    protected void afterFeaturesReceived() throws SecurityRequiredException, NotConnectedException, InterruptedException {
        StartTls startTlsFeature = getFeature(StartTls.ELEMENT, StartTls.NAMESPACE);
        if (startTlsFeature != null) {
            if (startTlsFeature.required() && config.getSecurityMode() == SecurityMode.disabled) {
                notifyConnectionError(new SecurityRequiredByServerException());
                return;
            }

            if (config.getSecurityMode() == ConnectionConfiguration.SecurityMode.disabled) {
                // Do not secure the connection using TLS since TLS was disabled
                return;
            }
            send(new StartTls());
        }
        // If TLS is required but the server doesn't offer it, disconnect
        // from the server and throw an error. First check if we've already negotiated TLS
        // and are secure, however (features get parsed a second time after TLS is established).
        if (!isSecureConnection() && startTlsFeature == null
                        && getConfiguration().getSecurityMode() == SecurityMode.required) {
            throw new SecurityRequiredByClientException();
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
        CharSequence to = getServiceName();
        CharSequence from = null;
        CharSequence localpart = config.getUsername();
        if (localpart != null) {
            from = XmppStringUtils.completeJidFrom(localpart, to);
        }
        String id = getStreamId();
        send(new StreamOpen(to, from, id));
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
                public void run() {
                    parsePackets();
                }
            }, "Smack Packet Reader (" + getConnectionCounter() + ")");
         }

        /**
         * Shuts the packet reader down. This method simply sets the 'done' flag to true.
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
                                String reportedServiceName = parser.getAttributeValue("", "from");
                                assert(config.getServiceName().equals(reportedServiceName));
                            }
                            break;
                        case "error":
                            throw new StreamErrorException(PacketParserUtils.parseStreamError(parser));
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
                                // We report any failure regarding TLS in the second stage of XMPP
                                // connection establishment, namely the SASL authentication
                                saslFeatureReceived.reportFailure(new SmackException(e));
                                throw e;
                            }
                            break;
                        case "failure":
                            String namespace = parser.getNamespace(null);
                            switch (namespace) {
                            case "urn:ietf:params:xml:ns:xmpp-tls":
                                // TLS negotiation has failed. The server will close the connection
                                // TODO Parse failure stanza
                                throw new XMPPErrorException("TLS negotiation has failed", null);
                            case "http://jabber.org/protocol/compress":
                                // Stream compression has been denied. This is a recoverable
                                // situation. It is still possible to authenticate and
                                // use the connection but using an uncompressed connection
                                // TODO Parse failure stanza
                                compressSyncPoint.reportFailure(new XMPPErrorException(
                                                "Could not establish compression", null));
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
                                    XMPPErrorException xmppException = new XMPPErrorException(
                                                    "Stream Management 'enabled' element with resume attribute but without session id received",
                                                    new XMPPError(
                                                                    XMPPError.Condition.bad_request));
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
                            XMPPError xmppError = new XMPPError(failed.getXMPPErrorCondition());
                            XMPPException xmppException = new XMPPErrorException("Stream Management failed", xmppError);
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
                                smEnabledSyncPoint.reportFailure(xmppException);
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
                            // First, drop the stanzas already handled by the server
                            processHandledCount(resumed.getHandledCount());
                            // Then re-send what is left in the unacknowledged queue
                            List<Stanza> stanzasToResend = new LinkedList<Stanza>();
                            stanzasToResend.addAll(unacknowledgedStanzas);
                            for (Stanza stanza : stanzasToResend) {
                                packetWriter.sendStreamElement(stanza);
                            }
                            smResumedSyncPoint.reportSuccess();
                            smEnabledSyncPoint.reportSuccess();
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
                            // Disconnect the connection
                            disconnect();
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
                // The exception can be ignored if the the connection is 'done'
                // or if the it was caused because the socket got closed
                if (!(done || isSocketClosed())) {
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
                        XMPPTCPConnection.this);

        /**
         * If set, the packet writer is shut down
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
            if (done() && !isSmResumptionPossible()) {
                // Don't throw a NotConnectedException is there is an resumable stream available
                throw new NotConnectedException();
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
         * Shuts down the packet writer. Once this method has been called, no further
         * packets will be written to the server.
         */
        void shutdown(boolean instant) {
            instantShutdown = instant;
            shutdownTimestamp = System.currentTimeMillis();
            queue.shutdown();
            try {
                shutdownDone.checkIfSuccessOrWait();
            }
            catch (NoResponseException e) {
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
                    // Check if the stream element should be put to the unacknowledgedStanza
                    // queue. Note that we can not do the put() in sendStanzaInternal() and the
                    // packet order is not stable at this point (sendStanzaInternal() can be
                    // called concurrently).
                    if (unacknowledgedStanzas != null && packet != null) {
                        // If the unacknowledgedStanza queue is nearly full, request an new ack
                        // from the server in order to drain it
                        if (unacknowledgedStanzas.size() == 0.8 * XMPPTCPConnection.QUEUE_SIZE) {
                            writer.write(AckRequest.INSTANCE.toXML().toString());
                            writer.flush();
                        }
                        try {
                            // It is important the we put the stanza in the unacknowledged stanza
                            // queue before we put it on the wire
                            unacknowledgedStanzas.put(packet);
                        }
                        catch (InterruptedException e) {
                            throw new IllegalStateException(e);
                        }
                    }
                    writer.write(element.toXML().toString());
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

                try {
                    writer.close();
                }
                catch (Exception e) {
                    // Do nothing
                }

            }
            catch (Exception e) {
                // The exception can be ignored if the the connection is 'done'
                // or if the it was caused because the socket got closed
                if (!(done() || isSocketClosed())) {
                    notifyConnectionError(e);
                } else {
                    LOGGER.log(Level.FINE, "Ignoring Exception in writePackets()", e);
                }
            } finally {
                LOGGER.fine("Reporting shutdownDone success in writer thread");
                shutdownDone.reportSuccess();
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
     */
    public static void setUseStreamManagementResumptiodDefault(boolean useSmResumptionDefault) {
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
    public StanzaListener addStanzaIdAcknowledgedListener(final String id, StanzaListener listener) throws StreamManagementNotEnabledException {
        // Prevent users from adding callbacks that will never get removed
        if (!smWasEnabledAtLeastOnce) {
            throw new StreamManagementException.StreamManagementNotEnabledException();
        }
        // Remove the listener after max. 12 hours
        final int removeAfterSeconds = Math.min(getMaxSmResumptionTime() + 60, 12 * 60 * 60);
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
        long maxResumptionMillies = getMaxSmResumptionTime() * 1000;
        if (shutdownTimestamp + maxResumptionMillies > current) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * Get the maximum resumption time in seconds after which a managed stream can be resumed.
     *
     * @return the maximum resumption time in seconds.
     */
    public int getMaxSmResumptionTime() {
        int clientResumptionTime = smClientMaxResumptionTime > 0 ? smClientMaxResumptionTime : Integer.MAX_VALUE;
        int serverResumptionTime = smServerMaxResumptimTime > 0 ? smServerMaxResumptimTime : Integer.MAX_VALUE;
        return Math.min(clientResumptionTime, serverResumptionTime);
    }

    private void processHandledCount(long handledCount) throws NotConnectedException, StreamManagementCounterError {
        long ackedStanzasCount = SMUtils.calculateDelta(handledCount, serverHandledStanzasCount);
        final List<Stanza> ackedStanzas = new ArrayList<Stanza>(
                        handledCount <= Integer.MAX_VALUE ? (int) handledCount
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
                                listener.processPacket(ackedStanza);
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
                                listener.processPacket(ackedStanza);
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
