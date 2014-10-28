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
import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.PacketListener;
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
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.compress.packet.Compress;
import org.jivesoftware.smack.packet.Element;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.StreamOpen;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.StartTls;
import org.jivesoftware.smack.parsing.ParsingExceptionCallback;
import org.jivesoftware.smack.parsing.UnparsablePacket;
import org.jivesoftware.smack.sasl.packet.SaslStreamElements;
import org.jivesoftware.smack.sasl.packet.SaslStreamElements.Challenge;
import org.jivesoftware.smack.sasl.packet.SaslStreamElements.SASLFailure;
import org.jivesoftware.smack.sasl.packet.SaslStreamElements.Success;
import org.jivesoftware.smack.packet.PlainStreamElement;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smack.tcp.sm.SMUtils;
import org.jivesoftware.smack.tcp.sm.StreamManagementException;
import org.jivesoftware.smack.tcp.sm.StreamManagementException.StreamManagementNotEnabledException;
import org.jivesoftware.smack.tcp.sm.StreamManagementException.StreamIdDoesNotMatchException;
import org.jivesoftware.smack.tcp.sm.packet.StreamManagement;
import org.jivesoftware.smack.tcp.sm.packet.StreamManagement.AckAnswer;
import org.jivesoftware.smack.tcp.sm.packet.StreamManagement.AckRequest;
import org.jivesoftware.smack.tcp.sm.packet.StreamManagement.Enable;
import org.jivesoftware.smack.tcp.sm.packet.StreamManagement.Enabled;
import org.jivesoftware.smack.tcp.sm.packet.StreamManagement.Failed;
import org.jivesoftware.smack.tcp.sm.packet.StreamManagement.Resume;
import org.jivesoftware.smack.tcp.sm.packet.StreamManagement.Resumed;
import org.jivesoftware.smack.tcp.sm.packet.StreamManagement.StreamManagementFeature;
import org.jivesoftware.smack.tcp.sm.predicates.Predicate;
import org.jivesoftware.smack.tcp.sm.provider.ParseStreamManagement;
import org.jivesoftware.smack.util.ArrayBlockingQueueWithShutdown;
import org.jivesoftware.smack.util.PacketParserUtils;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.TLSUtils;
import org.jivesoftware.smack.util.dns.HostAddress;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
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
import java.net.Socket;
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
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Creates a socket connection to a XMPP server. This is the default connection
 * to a XMPP server and is specified in the XMPP Core (RFC 6120).
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

    private String connectionID = null;
    private boolean connected = false;

    /**
     * 
     */
    private boolean disconnectedButResumeable = false;

    // socketClosed is used concurrent
    // by XMPPTCPConnection, PacketReader, PacketWriter
    private volatile boolean socketClosed = false;

    private boolean usingTLS = false;

    private ParsingExceptionCallback parsingExceptionCallback = SmackConfiguration.getDefaultParsingExceptionCallback();

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
    private long serverHandledStanzasCount = 0;
    private long clientHandledStanzasCount = 0;
    private BlockingQueue<Packet> unacknowledgedStanzas;

    /**
     * This listeners are invoked for every stanza that got acknowledged.
     * <p>
     * We use a {@link ConccurrentLinkedQueue} here in order to allow the listeners to remove
     * themselves after they have been invoked.
     * </p>
     */
    private final Collection<PacketListener> stanzaAcknowledgedListeners = new ConcurrentLinkedQueue<PacketListener>();

    /**
     * This listeners are invoked for a acknowledged stanza that has the given stanza ID. They will
     * only be invoked once and automatically removed after that.
     */
    private final Map<String, PacketListener> idStanzaAcknowledgedListeners = new ConcurrentHashMap<String, PacketListener>();

    /**
     * Predicates that determine if an stream management ack should be requested from the server.
     * <p>
     * We use a linked hash set here, so that the order how the predicates are added matches the
     * order in which they are invoked in order to determine if an ack request should be send or not.
     * </p>
     */
    private final Set<PacketFilter> requestAckPredicates = new LinkedHashSet<PacketFilter>();

    /**
     * Creates a new connection to the specified XMPP server. A DNS SRV lookup will be
     * performed to determine the IP address and port corresponding to the
     * service name; if that lookup fails, it's assumed that server resides at
     * <tt>serviceName</tt> with the default port of 5222. Encrypted connections (TLS)
     * will be used if available, stream compression is disabled, and standard SASL
     * mechanisms will be used for authentication.<p>
     * <p/>
     * This is the simplest constructor for connecting to an XMPP server. Alternatively,
     * you can get fine-grained control over connection settings using the
     * {@link #XMPPTCPConnection(ConnectionConfiguration)} constructor.<p>
     * <p/>
     * Note that XMPPTCPConnection constructors do not establish a connection to the server
     * and you must call {@link #connect()}.<p>
     * <p/>
     * The CallbackHandler will only be used if the connection requires the client provide
     * an SSL certificate to the server. The CallbackHandler must handle the PasswordCallback
     * to prompt for a password to unlock the keystore containing the SSL certificate.
     *
     * @param serviceName the name of the XMPP server to connect to; e.g. <tt>example.com</tt>.
     * @param callbackHandler the CallbackHandler used to prompt for the password to the keystore.
     */
    public XMPPTCPConnection(String serviceName, CallbackHandler callbackHandler) {
        // Create the configuration for this new connection
        super(new ConnectionConfiguration(serviceName));
        config.setCallbackHandler(callbackHandler);
    }

    /**
     * Creates a new XMPP connection in the same way {@link #XMPPTCPConnection(String,CallbackHandler)} does, but
     * with no callback handler for password prompting of the keystore.  This will work
     * in most cases, provided the client is not required to provide a certificate to 
     * the server.
     *
     * @param serviceName the name of the XMPP server to connect to; e.g. <tt>example.com</tt>.
     */
    public XMPPTCPConnection(String serviceName) {
        // Create the configuration for this new connection
        super(new ConnectionConfiguration(serviceName));
    }

    /**
     * Creates a new XMPP connection in the same way {@link #XMPPTCPConnection(ConnectionConfiguration,CallbackHandler)} does, but
     * with no callback handler for password prompting of the keystore.  This will work
     * in most cases, provided the client is not required to provide a certificate to 
     * the server.
     *
     *
     * @param config the connection configuration.
     */
    public XMPPTCPConnection(ConnectionConfiguration config) {
        super(config);
    }

    /**
     * Creates a new XMPP connection using the specified connection configuration.<p>
     * <p/>
     * Manually specifying connection configuration information is suitable for
     * advanced users of the API. In many cases, using the
     * {@link #XMPPTCPConnection(String)} constructor is a better approach.<p>
     * <p/>
     * Note that XMPPTCPConnection constructors do not establish a connection to the server
     * and you must call {@link #connect()}.<p>
     * <p/>
     *
     * The CallbackHandler will only be used if the connection requires the client provide
     * an SSL certificate to the server. The CallbackHandler must handle the PasswordCallback
     * to prompt for a password to unlock the keystore containing the SSL certificate.
     *
     * @param config the connection configuration.
     * @param callbackHandler the CallbackHandler used to prompt for the password to the keystore.
     */
    public XMPPTCPConnection(ConnectionConfiguration config, CallbackHandler callbackHandler) {
        super(config);
        config.setCallbackHandler(callbackHandler);
    }

    @Override
    public String getConnectionID() {
        if (!isConnected()) {
            return null;
        }
        return connectionID;
    }

    @Override
    public String getUser() {
        if (!isAuthenticated()) {
            return null;
        }
        return user;
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

    @Override
    public synchronized void login(String username, String password, String resource) throws XMPPException, SmackException, IOException {
        if (!isConnected()) {
            throw new NotConnectedException();
        }
        if (authenticated && !disconnectedButResumeable) {
            throw new AlreadyLoggedInException();
        }

        // Do partial version of nameprep on the username.
        if (username != null) {
            username = username.toLowerCase(Locale.US).trim();
        }

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
                afterSuccessfulLogin(false, true);
                return;
            }
            // SM resumption failed, what Smack does here is to report success of
            // lastFeaturesReceived in case of sm resumption was answered with 'failed' so that
            // normal resource binding can be tried.
            LOGGER.fine("Stream resumption failed, continuing with normal stream establishment process");
        }

        bindResourceAndEstablishSession(resource);

        List<Packet> previouslyUnackedStanzas = new LinkedList<Packet>();
        if (unacknowledgedStanzas != null) {
            // There was a previous connection with SM enabled but that was either not resumable or
            // failed to resume. Make sure that we (re-)send the unacknowledged stanzas.
            unacknowledgedStanzas.drainTo(previouslyUnackedStanzas);
        }
        if (isSmAvailable() && useSm) {
            // Remove what is maybe left from previously stream managed sessions
            unacknowledgedStanzas = new ArrayBlockingQueue<Packet>(QUEUE_SIZE);
            clientHandledStanzasCount = 0;
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
        for (Packet stanza : previouslyUnackedStanzas) {
            sendPacketInternal(stanza);
        }

        // Stores the authentication for future reconnection
        setLoginInfo(username, password, resource);
        afterSuccessfulLogin(false, false);
    }

    @Override
    public synchronized void loginAnonymously() throws XMPPException, SmackException, IOException {
        if (!isConnected()) {
            throw new NotConnectedException();
        }
        if (authenticated) {
            throw new AlreadyLoggedInException();
        }

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

        afterSuccessfulLogin(true, false);
    }

    @Override
    public boolean isConnected() {
        return connected;
    }

    @Override
    public boolean isSecureConnection() {
        return usingTLS;
    }

    public boolean isSocketClosed() {
        return socketClosed;
    }

    @Override
    public boolean isAuthenticated() {
        return authenticated;
    }

    /**
     * Shuts the current connection down. After this method returns, the connection must be ready
     * for re-use by connect.
     */
    @Override
    protected void shutdown() {
        shutdown(false);
    }

    /**
     * Performs an unclean disconnect and shutdown of the connection. Does not send a closing stream stanza.
     */
    public void instantShutdown() {
        shutdown(true);
    }

    private void shutdown(boolean instant) {
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

        setWasAuthenticated(authenticated);
        // If we are able to resume the stream, then don't set
        // connected/authenticated/usingTLS to false since we like behave like we are still
        // connected (e.g. sendPacket should not throw a NotConnectedException).
        if (isSmResumptionPossible() && instant) {
            disconnectedButResumeable = true;
        } else {
            authenticated = false;
            connected = false;
            usingTLS = false;
            disconnectedButResumeable = false;
        }
        reader = null;
        writer = null;
        maybeCompressFeaturesReceived.init();
        compressSyncPoint.init();
        smResumedSyncPoint.init();
        smEnabledSyncPoint.init();
        initalOpenStreamSend.init();
    }

    @Override
    public void send(PlainStreamElement element) throws NotConnectedException {
        packetWriter.sendStreamElement(element);
    }

    @Override
    protected void sendPacketInternal(Packet packet) throws NotConnectedException {
        packetWriter.sendStreamElement(packet);
        if (isSmEnabled()) {
            for (PacketFilter requestAckPredicate : requestAckPredicates) {
                if (requestAckPredicate.accept(packet)) {
                    requestSmAcknowledgementInternal();
                    break;
                }
            }
        }
    }

    private void connectUsingConfiguration(ConnectionConfiguration config) throws SmackException, IOException {
        try {
            maybeResolveDns();
        }
        catch (Exception e) {
            throw new SmackException(e);
        }
        Iterator<HostAddress> it = config.getHostAddresses().iterator();
        List<HostAddress> failedAddresses = new LinkedList<HostAddress>();
        while (it.hasNext()) {
            Exception exception = null;
            HostAddress hostAddress = it.next();
            String host = hostAddress.getFQDN();
            int port = hostAddress.getPort();
            try {
                if (config.getSocketFactory() == null) {
                    this.socket = new Socket(host, port);
                }
                else {
                    this.socket = config.getSocketFactory().createSocket(host, port);
                }
            } catch (Exception e) {
                exception = e;
            }
            if (exception == null) {
                // We found a host to connect to, break here
                this.host = host;
                this.port = port;
                break;
            }
            hostAddress.setException(exception);
            failedAddresses.add(hostAddress);
            if (!it.hasNext()) {
                // There are no more host addresses to try
                // throw an exception and report all tried
                // HostAddresses in the exception
                throw ConnectionException.from(failedAddresses);
            }
        }
        socketClosed = false;
        initConnection();
    }

    /**
     * Initializes the connection by creating a packet reader and writer and opening a
     * XMPP stream to the server.
     *
     * @throws XMPPException if establishing a connection to the server fails.
     * @throws SmackException if the server failes to respond back or if there is anther error.
     * @throws IOException 
     */
    private void initConnection() throws SmackException, IOException {
        boolean isFirstInitialization = packetReader == null || packetWriter == null;
        compressionHandler = null;

        // Set the reader and writer instance variables
        initReaderAndWriter();

        try {
            if (isFirstInitialization) {
                packetWriter = new PacketWriter();
                packetReader = new PacketReader();

                // If debugging is enabled, we should start the thread that will listen for
                // all packets and then log them.
                if (config.isDebuggerEnabled()) {
                    addPacketListener(debugger.getReaderListener(), null);
                    if (debugger.getWriterListener() != null) {
                        addPacketSendingListener(debugger.getWriterListener(), null);
                    }
                }
            }
            // Start the packet writer. This will open a XMPP stream to the server
            packetWriter.init();
            // Start the packet reader. The startup() method will block until we
            // get an opening stream packet back from server
            packetReader.init();

            if (isFirstInitialization) {
                // Notify listeners that a new connection has been established
                for (ConnectionCreationListener listener : getConnectionCreationListeners()) {
                    listener.connectionCreated(this);
                }
            }

        }
        catch (SmackException ex) {
            // An exception occurred in setting up the connection. Note that
            // it's important here that we do an instant shutdown here, as this
            // will not send a closing stream element, which will destroy
            // Stream Management state on the server, which is not what we want.
            instantShutdown();
            // Everything stopped. Now throw the exception.
            throw ex;
        }
    }

    private void initReaderAndWriter() throws IOException, SmackException {
        try {
            InputStream is = socket.getInputStream();
            OutputStream os = socket.getOutputStream();
            if (compressionHandler != null) {
                is = compressionHandler.getInputStream(is);
                os =  compressionHandler.getOutputStream(os);
            }
            // OutputStreamWriter is already buffered, no need to wrap it into a BufferedWriter
            writer = new OutputStreamWriter(os, "UTF-8");
            reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
        }
        catch (IOException e) {
            throw e;
        }
        catch (Exception e) {
            throw new SmackException(e);
        }

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
                plain.getInetAddress().getHostAddress(), plain.getPort(), true);
        // Initialize the reader and writer with the new secured version
        initReaderAndWriter();

        final SSLSocket sslSocket = (SSLSocket) socket;
        TLSUtils.setEnabledProtocolsAndCiphers(sslSocket, config.getEnabledSSLProtocols(), config.getEnabledSSLCiphers());

        // Proceed to do the handshake
        sslSocket.startHandshake();

        final HostnameVerifier verifier = getConfiguration().getHostnameVerifier();
        if (verifier == null) {
                throw new IllegalStateException("No HostnameVerifier set. Use connectionConfiguration.setHostnameVerifier() to configure.");
        } else if (!verifier.verify(getServiceName(), sslSocket.getSession())) {
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
     */
    private void useCompression() throws NotConnectedException, NoResponseException, XMPPException {
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
     */
    @Override
    protected void connectInternal() throws SmackException, IOException, XMPPException {
        if (connected && !disconnectedButResumeable) {
            throw new AlreadyConnectedException();
        }
        // Establishes the connection, readers and writers
        connectUsingConfiguration(config);

        // Wait with SASL auth until the SASL mechanisms have been received
        saslFeatureReceived.checkIfSuccessOrWaitOrThrow();

        // Make note of the fact that we're now connected.
        connected = true;
        callConnectionConnectedListener();

        // Automatically makes the login if the user was previously connected successfully
        // to the server and the connection was terminated abruptly
        if (wasAuthenticated) {
            // Make the login
            if (isAnonymous()) {
                // Make the anonymous login
                loginAnonymously();
            }
            else {
                login(config.getUsername(), config.getPassword(), config.getResource());
            }
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
     * Sends a notification indicating that the connection was reconnected successfully.
     */
    private void notifyReconnection() {
        // Notify connection listeners of the reconnection.
        for (ConnectionListener listener : getConnectionListeners()) {
            try {
                listener.reconnectionSuccessful();
            }
            catch (Exception e) {
                // Catch and print any exception so we can recover
                // from a faulty listener
                LOGGER.log(Level.WARNING, "notifyReconnection()", e);
            }
        }
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
    protected void afterFeaturesReceived() throws SecurityRequiredException, NotConnectedException {
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
     */
    void openStream() throws SmackException {
        send(new StreamOpen(getServiceName()));
        try {
            packetReader.parser = PacketParserUtils.newXmppParser(reader);
        }
        catch (XmlPullParserException e) {
            throw new SmackException(e);
        }
    }

    protected class PacketReader {

        private Thread readerThread;

        XmlPullParser parser;

        private volatile boolean done;

        /**
         * Initializes the reader in order to be used. The reader is initialized during the
         * first connection and when reconnecting due to an abruptly disconnection.
         *
         * @throws SmackException if the parser could not be reset.
         */
        void init() throws SmackException {
            done = false;

            readerThread = new Thread() {
                public void run() {
                    parsePackets();
                }
            };
            readerThread.setName("Smack Packet Reader (" + getConnectionCounter() + ")");
            readerThread.setDaemon(true);
            readerThread.start();
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
                        case IQ.ELEMENT:
                        case Presence.ELEMENT:
                            int parserDepth = parser.getDepth();
                            Packet packet;
                            try {
                                packet = PacketParserUtils.parseStanza(parser,
                                                XMPPTCPConnection.this);
                            }
                            catch (Exception e) {
                                ParsingExceptionCallback callback = getParsingExceptionCallback();
                                CharSequence content = PacketParserUtils.parseContentDepth(parser,
                                                parserDepth);
                                UnparsablePacket message = new UnparsablePacket(content, e);
                                if (callback != null) {
                                    callback.handleUnparsablePacket(message);
                                }
                                // The parser is now at the end tag of the unparsable stanza. We need to advance to the next
                                // start tag in order to avoid an exception which would again lead to the execution of the
                                // catch block becoming effectively an endless loop.
                                eventType = parser.next();
                                continue;
                            } finally {
                                clientHandledStanzasCount = SMUtils.incrementHeight(clientHandledStanzasCount);
                                reportStanzaReceived();
                            }
                            processPacket(packet);
                            break;
                        case "stream":
                            // We found an opening stream.
                            if ("jabber:client".equals(parser.getNamespace(null))) {
                                // Get the connection id.
                                for (int i=0; i<parser.getAttributeCount(); i++) {
                                    if (parser.getAttributeName(i).equals("id")) {
                                        // Save the connectionID
                                        connectionID = parser.getAttributeValue(i);
                                    }
                                    // According to RFC 6120 4.7.1 response
                                    // stream headers in c2s and s2s of the
                                    // receiving entity MUST include the 'from'
                                    // attribute.
                                    else if (parser.getAttributeName(i).equals("from")) {
                                        // Use the server name that the server says that it is.
                                        setServiceName(parser.getAttributeValue(i));
                                    }
                                }
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
                                // Mark this a aon-resumable stream by setting smSessionId to null
                                smSessionId = null;
                            }
                            smEnabledSyncPoint.reportSuccess();
                            LOGGER.fine("Stream Management (XEP-198): succesfully enabled");
                            break;
                        case Failed.ELEMENT:
                            Failed failed = ParseStreamManagement.failed(parser);
                            XMPPError xmppError = failed.getXMPPError();
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
                            List<Packet> stanzasToResend = new LinkedList<Packet>();
                            stanzasToResend.addAll(unacknowledgedStanzas);
                            for (Packet stanza : stanzasToResend) {
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
                             LOGGER.warning("Unkown top level stream element: " + name);
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
                        // This should not happen, log a warning and disconnect()
                        LOGGER.warning("Got END_DOCUMENT, aborting parsing");
                        disconnect();
                        break;
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

        private Thread writerThread;

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
            writerThread = new Thread() {
                public void run() {
                    writePackets();
                }
            };
            writerThread.setName("Smack Packet Writer (" + getConnectionCounter() + ")");
            writerThread.setDaemon(true);
            writerThread.start();
        }

        private boolean done() {
            return shutdownTimestamp != null;
        }

        /**
         * Sends the specified element to the server.
         *
         * @param element the element to send.
         * @throws NotConnectedException 
         */
        protected void sendStreamElement(Element element) throws NotConnectedException {
            if (done() && !isSmResumptionPossible()) {
                // Don't throw a NotConnectedException is there is an resumable stream available
                throw new NotConnectedException();
            }

            try {
                queue.put(element);
            }
            catch (InterruptedException ie) {
                throw new NotConnectedException();
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
                LOGGER.log(Level.WARNING, "NoResponseException", e);
            }
        }

        /**
         * Returns the next available element from the queue for writing.
         *
         * @return the next element for writing.
         */
        private Element nextStreamElement() {
            // TODO not sure if nextStreamElement and/or this done() condition still required.
            // Couldn't this be done in writePackets too?
            if (done()) {
                return null;
            }

            Element packet = null;
            try {
                packet = queue.take();
            }
            catch (InterruptedException e) {
                // Do nothing
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
                    Packet packet = null;
                    if (element instanceof Packet) {
                        packet = (Packet) element;
                    }
                    // Check if the stream element should be put to the unacknowledgedStanza
                    // queue. Note that we can not do the put() in sendPacketInternal() and the
                    // packet order is not stable at this point (sendPacketInternal() can be
                    // called concurrently).
                    if (isSmEnabled() && packet != null) {
                        // If the unacknowledgedStanza queue is nearly full, request an new ack
                        // from the server in order to drain it
                        if (unacknowledgedStanzas.size() == 0.8 * XMPPTCPConnection.QUEUE_SIZE) {
                            writer.write(AckRequest.INSTANCE.toXML().toString());
                            writer.flush();
                        }
                        try {
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
                    // Flush out the rest of the queue. If the queue is extremely large, it's
                    // possible we won't have time to entirely flush it before the socket is forced
                    // closed by the shutdown process.
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
                shutdownDone.reportSuccess();
            }
        }

        private void drainWriterQueueToUnacknowledgedStanzas() {
            List<Element> elements = new ArrayList<Element>(queue.size());
            queue.drainTo(elements);
            for (Element element : elements) {
                if (element instanceof Packet) {
                    unacknowledgedStanzas.add((Packet) element);
                }
            }
        }
    }

    public static void setUseStreamManagementDefault(boolean useSmDefault) {
        XMPPTCPConnection.useSmDefault = useSmDefault;
    }

    public static void setUseStreamManagementResumptiodDefault(boolean useSmResupmptionDefault) {
        XMPPTCPConnection.useSmResumptionDefault = useSmResupmptionDefault;
    }

    public void setUseStreamManagement(boolean useSm) {
        this.useSm = useSm;
    }

    public void setUseStreamManagementResumption(boolean useSmResumption) {
        this.useSmResumption = useSmResumption;
    }

    /**
     * Set the preferred resumption time in seconds.
     * @param resumptionTime the preferred resumption time in seconds
     */
    public void setPreferredResumptionTime(int resumptionTime) {
        smClientMaxResumptionTime = resumptionTime;
    }

    public boolean addRequestAckPredicate(PacketFilter predicate) {
        synchronized (requestAckPredicates) {
            return requestAckPredicates.add(predicate);
        }
    }

    public boolean removeRequestAckPredicate(PacketFilter predicate) {
        synchronized (requestAckPredicates) {
            return requestAckPredicates.remove(predicate);
        }
    }

    public void removeAllRequestAckPredicates() {
        synchronized (requestAckPredicates) {
            requestAckPredicates.clear();
        }
    }

    public void requestSmAcknowledgement() throws StreamManagementNotEnabledException, NotConnectedException {
        if (!isSmEnabled()) {
            throw new StreamManagementException.StreamManagementNotEnabledException();
        }
        requestSmAcknowledgementInternal();
    }

    private void requestSmAcknowledgementInternal() throws NotConnectedException {
        packetWriter.sendStreamElement(AckRequest.INSTANCE);
    }

    public void sendSmAcknowledgement() throws StreamManagementNotEnabledException, NotConnectedException {
        if (!isSmEnabled()) {
            throw new StreamManagementException.StreamManagementNotEnabledException();
        }
        sendSmAcknowledgementInternal();
    }

    private void sendSmAcknowledgementInternal() throws NotConnectedException {
        packetWriter.sendStreamElement(new AckAnswer(clientHandledStanzasCount));
    }

    public void addStanzaAcknowledgedListener(PacketListener listener) {
        stanzaAcknowledgedListeners.add(listener);
    }

    public boolean removeStanzaAcknowledgedListener(PacketListener listener) {
        return stanzaAcknowledgedListeners.remove(listener);
    }

    public void removeAllStanzaAcknowledgedListeners() {
        stanzaAcknowledgedListeners.clear();
    }

    public PacketListener addIdStanzaAcknowledgedListener(String id, PacketListener listener) {
        return idStanzaAcknowledgedListeners.put(id, listener);
    }

    public PacketListener removeIdStanzaAcknowledgedListener(String id) {
        return idStanzaAcknowledgedListeners.remove(id);
    }

    public void removeAllIdStanzaAcknowledgedListeners() {
        idStanzaAcknowledgedListeners.clear();
    }

    public boolean isSmAvailable() {
        return hasFeature(StreamManagementFeature.ELEMENT, StreamManagement.NAMESPACE);
    }

    public boolean isSmEnabled() {
        return smEnabledSyncPoint.wasSuccessful();
    }

    public boolean isDisconnectedButSmResumptionPossible() {
        return disconnectedButResumeable && isSmResumptionPossible();
    }

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
        int clientResumptionTime = smClientMaxResumptionTime > 0 ? smClientMaxResumptionTime : Integer.MAX_VALUE;
        int serverResumptionTime = smServerMaxResumptimTime > 0 ? smServerMaxResumptimTime : Integer.MAX_VALUE;
        long maxResumptionMillies = Math.max(clientResumptionTime, serverResumptionTime) * 1000;
        if (shutdownTimestamp + maxResumptionMillies > current) {
            return false;
        } else {
            return true;
        }
    }

    private void processHandledCount(long handledCount) throws NotConnectedException {
        long ackedStanzasCount = SMUtils.calculateDelta(handledCount, serverHandledStanzasCount);
        List<Packet> ackedStanzas = new ArrayList<Packet>(
                        handledCount <= Integer.MAX_VALUE ? (int) handledCount
                                        : Integer.MAX_VALUE);
        for (long i = 0; i < ackedStanzasCount; i++) {
            Packet ackedStanza = unacknowledgedStanzas.poll();
            // If the server ack'ed a stanza, then it must be in the
            // unacknowledged stanza queue. There can be no exception.
            assert(ackedStanza != null);
            ackedStanzas.add(ackedStanza);
        }
        for (Packet ackedStanza : ackedStanzas) {
            for (PacketListener listener : stanzaAcknowledgedListeners) {
                listener.processPacket(ackedStanza);
            }
            String id = ackedStanza.getPacketID();
            if (id != null) {
                PacketListener listener = idStanzaAcknowledgedListeners.remove(id);
                if (listener != null) {
                    listener.processPacket(ackedStanza);
                }
            }
        }
        serverHandledStanzasCount = handledCount;
    }
}
