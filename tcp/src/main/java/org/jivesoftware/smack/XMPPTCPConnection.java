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
package org.jivesoftware.smack;

import org.jivesoftware.smack.SmackException.AlreadyLoggedInException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.SmackException.ConnectionException;
import org.jivesoftware.smack.compression.XMPPInputOutputStream;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.parsing.ParsingExceptionCallback;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.dns.HostAddress;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.PasswordCallback;
import javax.security.sasl.SaslException;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Constructor;
import java.net.Socket;
import java.security.KeyStore;
import java.security.Provider;
import java.security.Security;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Creates a socket connection to a XMPP server. This is the default connection
 * to a Jabber server and is specified in the XMPP Core (RFC 3920).
 * 
 * @see XMPPConnection
 * @author Matt Tucker
 */
public class XMPPTCPConnection extends XMPPConnection {

    private static final Logger LOGGER = Logger.getLogger(XMPPTCPConnection.class.getName());

    /**
     * The socket which is used for this connection.
     */
    Socket socket;

    String connectionID = null;
    private String user = null;
    private boolean connected = false;
    // socketClosed is used concurrent
    // by XMPPTCPConnection, PacketReader, PacketWriter
    private volatile boolean socketClosed = false;

    /**
     * Flag that indicates if the user is currently authenticated with the server.
     */
    private boolean authenticated = false;
    /**
     * Flag that indicates if the user was authenticated with the server when the connection
     * to the server was closed (abruptly or not).
     */
    private boolean wasAuthenticated = false;
    private boolean anonymous = false;
    private boolean usingTLS = false;

    private ParsingExceptionCallback parsingExceptionCallback = SmackConfiguration.getDefaultParsingExceptionCallback();

    PacketWriter packetWriter;
    PacketReader packetReader;

    /**
     * Collection of available stream compression methods offered by the server.
     */
    private Collection<String> compressionMethods;

    /**
     * Set to true by packet writer if the server acknowledged the compression
     */
    private boolean serverAckdCompression = false;


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

    public String getConnectionID() {
        if (!isConnected()) {
            return null;
        }
        return connectionID;
    }

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
    public synchronized void login(String username, String password, String resource) throws XMPPException, SmackException, SaslException, IOException {
        if (!isConnected()) {
            throw new NotConnectedException();
        }
        if (authenticated) {
            throw new AlreadyLoggedInException();
        }
        // Do partial version of nameprep on the username.
        username = username.toLowerCase(Locale.US).trim();

        String response;
        if (saslAuthentication.hasNonAnonymousAuthentication()) {
            // Authenticate using SASL
            if (password != null) {
                response = saslAuthentication.authenticate(username, password, resource);
            }
            else {
                response = saslAuthentication.authenticate(resource, config.getCallbackHandler());
            }
        } else {
            throw new SaslException("No non-anonymous SASL authentication mechanism available");
        }

        // Set the user.
        if (response != null) {
            this.user = response;
            // Update the serviceName with the one returned by the server
            config.setServiceName(StringUtils.parseServer(response));
        }
        else {
            this.user = username + "@" + getServiceName();
            if (resource != null) {
                this.user += "/" + resource;
            }
        }

        // If compression is enabled then request the server to use stream compression
        if (config.isCompressionEnabled()) {
            useCompression();
        }

        // Indicate that we're now authenticated.
        authenticated = true;
        anonymous = false;

        // Set presence to online.
        if (config.isSendPresence()) {
            packetWriter.sendPacket(new Presence(Presence.Type.available));
        }

        // Stores the authentication for future reconnection
        config.setLoginInfo(username, password, resource);

        // If debugging is enabled, change the the debug window title to include the
        // name we are now logged-in as.
        // If DEBUG_ENABLED was set to true AFTER the connection was created the debugger
        // will be null
        if (config.isDebuggerEnabled() && debugger != null) {
            debugger.userHasLogged(user);
        }
        callConnectionAuthenticatedListener();
    }

    @Override
    public synchronized void loginAnonymously() throws XMPPException, SmackException, SaslException, IOException {
        if (!isConnected()) {
            throw new NotConnectedException();
        }
        if (authenticated) {
            throw new AlreadyLoggedInException();
        }

        String response;
        if (saslAuthentication.hasAnonymousAuthentication()) {
            response = saslAuthentication.authenticateAnonymously();
        }
        else {
            throw new SaslException("No anonymous SASL authentication mechanism available");
        }

        // Set the user value.
        this.user = response;
        // Update the serviceName with the one returned by the server
        config.setServiceName(StringUtils.parseServer(response));

        // If compression is enabled then request the server to use stream compression
        if (config.isCompressionEnabled()) {
            useCompression();
        }

        // Set presence to online.
        packetWriter.sendPacket(new Presence(Presence.Type.available));

        // Indicate that we're now authenticated.
        authenticated = true;
        anonymous = true;

        // If debugging is enabled, change the the debug window title to include the
        // name we are now logged-in as.
        // If DEBUG_ENABLED was set to true AFTER the connection was created the debugger
        // will be null
        if (config.isDebuggerEnabled() && debugger != null) {
            debugger.userHasLogged(user);
        }
        callConnectionAuthenticatedListener();
    }

    public boolean isConnected() {
        return connected;
    }

    public boolean isSecureConnection() {
        return isUsingTLS();
    }

    public boolean isSocketClosed() {
        return socketClosed;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public boolean isAnonymous() {
        return anonymous;
    }

    /**
     * Closes the connection by setting presence to unavailable then closing the stream to
     * the XMPP server. The shutdown logic will be used during a planned disconnection or when
     * dealing with an unexpected disconnection. Unlike {@link #disconnect()} the connection's
     * packet reader, packet writer, and {@link Roster} will not be removed; thus
     * connection's state is kept.
     *
     * @param unavailablePresence the presence packet to send during shutdown.
     */
    protected void shutdown(Presence unavailablePresence) {
        // Set presence to offline.
        if (packetWriter != null) {
                packetWriter.sendPacket(unavailablePresence);
        }

        this.setWasAuthenticated(authenticated);
        authenticated = false;

        if (packetReader != null) {
                packetReader.shutdown();
        }
        if (packetWriter != null) {
                packetWriter.shutdown();
        }

        // Wait 150 ms for processes to clean-up, then shutdown.
        try {
            Thread.sleep(150);
        }
        catch (Exception e) {
            // Ignore.
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
        // In most cases the close() should be successful, so set
        // connected to false here.
        connected = false;

        reader = null;
        writer = null;

        saslAuthentication.init();
    }

    public synchronized void disconnect(Presence unavailablePresence) {
        // If not connected, ignore this request.
        if (packetReader == null || packetWriter == null) {
            return;
        }

        if (!isConnected()) {
            return;
        }

        shutdown(unavailablePresence);

        wasAuthenticated = false;
    }

    void sendPacketInternal(Packet packet) {
        packetWriter.sendPacket(packet);
    }

    private void connectUsingConfiguration(ConnectionConfiguration config) throws SmackException, IOException {
        Exception exception = null;
        try {
            config.maybeResolveDns();
        }
        catch (Exception e) {
            throw new SmackException(e);
        }
        Iterator<HostAddress> it = config.getHostAddresses().iterator();
        List<HostAddress> failedAddresses = new LinkedList<HostAddress>();
        while (it.hasNext()) {
            exception = null;
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
                host = hostAddress.getFQDN();
                port = hostAddress.getPort();
                break;
            }
            hostAddress.setException(exception);
            failedAddresses.add(hostAddress);
            if (!it.hasNext()) {
                // There are no more host addresses to try
                // throw an exception and report all tried
                // HostAddresses in the exception
                throw new ConnectionException(failedAddresses);
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
        serverAckdCompression = false;

        // Set the reader and writer instance variables
        initReaderAndWriter();

        try {
            if (isFirstInitialization) {
                packetWriter = new PacketWriter(this);
                packetReader = new PacketReader(this);

                // If debugging is enabled, we should start the thread that will listen for
                // all packets and then log them.
                if (config.isDebuggerEnabled()) {
                    addPacketListener(debugger.getReaderListener(), null);
                    if (debugger.getWriterListener() != null) {
                        addPacketSendingListener(debugger.getWriterListener(), null);
                    }
                }
            }
            else {
                packetWriter.init();
                packetReader.init();
            }

            // Start the packet writer. This will open a XMPP stream to the server
            packetWriter.startup();
            // Start the packet reader. The startup() method will block until we
            // get an opening stream packet back from server.
            packetReader.startup();

            // Make note of the fact that we're now connected.
            connected = true;

            if (isFirstInitialization) {
                // Notify listeners that a new connection has been established
                for (ConnectionCreationListener listener : getConnectionCreationListeners()) {
                    listener.connectionCreated(this);
                }
            }

        }
        catch (Exception ex) {
            // An exception occurred in setting up the connection. Make sure we shut down the
            // readers and writers and close the socket.

            if (packetWriter != null) {
                try {
                    packetWriter.shutdown();
                }
                catch (Throwable ignore) { /* ignore */ }
                packetWriter = null;
            }
            if (packetReader != null) {
                try {
                    packetReader.shutdown();
                }
                catch (Throwable ignore) { /* ignore */ }
                packetReader = null;
            }
            if (reader != null) {
                try {
                    reader.close();
                }
                catch (Throwable ignore) { /* ignore */ }
                reader = null;
            }
            if (writer != null) {
                try {
                    writer.close();
                }
                catch (Throwable ignore) {  /* ignore */}
                writer = null;
            }
            if (socket != null) {
                try {
                    socket.close();
                }
                catch (Exception e) { /* ignore */ }
                socket = null;
            }
            this.setWasAuthenticated(authenticated);
            authenticated = false;
            connected = false;

            throw new SmackException(ex);        // Everything stoppped. Now throw the exception.
        }
    }

    private void initReaderAndWriter() throws IOException {
        try {
            if (compressionHandler == null) {
                reader =
                        new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
                writer = new BufferedWriter(
                        new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));
            }
            else {
                try {
                    OutputStream os = compressionHandler.getOutputStream(socket.getOutputStream());
                    writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));

                    InputStream is = compressionHandler.getInputStream(socket.getInputStream());
                    reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
                }
                catch (Exception e) {
                    LOGGER.log(Level.WARNING, "initReaderAndWriter()", e);
                    compressionHandler = null;
                    reader = new BufferedReader(
                            new InputStreamReader(socket.getInputStream(), "UTF-8"));
                    writer = new BufferedWriter(
                            new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));
                }
            }
        }
        catch (UnsupportedEncodingException ioe) {
            throw new IllegalStateException(ioe);
        }

        // If debugging is enabled, we open a window and write out all network traffic.
        initDebugger();
    }

    /***********************************************
     * TLS code below
     **********************************************/

    /**
     * Returns true if the connection to the server has successfully negotiated TLS. Once TLS
     * has been negotiatied the connection has been secured.
     *
     * @return true if the connection to the server has successfully negotiated TLS.
     */
    public boolean isUsingTLS() {
        return usingTLS;
    }

    /**
     * Notification message saying that the server supports TLS so confirm the server that we
     * want to secure the connection.
     *
     * @param required true when the server indicates that TLS is required.
     * @throws IOException if an exception occurs.
     */
    void startTLSReceived(boolean required) throws IOException {
        if (required && config.getSecurityMode() ==
                ConnectionConfiguration.SecurityMode.disabled) {
            notifyConnectionError(new IllegalStateException(
                    "TLS required by server but not allowed by connection configuration"));
            return;
        }

        if (config.getSecurityMode() == ConnectionConfiguration.SecurityMode.disabled) {
            // Do not secure the connection using TLS since TLS was disabled
            return;
        }
        writer.write("<starttls xmlns=\"urn:ietf:params:xml:ns:xmpp-tls\"/>");
        writer.flush();
    }

    /**
     * The server has indicated that TLS negotiation can start. We now need to secure the
     * existing plain connection and perform a handshake. This method won't return until the
     * connection has finished the handshake or an error occurred while securing the connection.
     *
     * @throws Exception if an exception occurs.
     */
    void proceedTLSReceived() throws Exception {
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
        // Proceed to do the handshake
        ((SSLSocket) socket).startHandshake();
        //if (((SSLSocket) socket).getWantClientAuth()) {
        //    System.err.println("XMPPConnection wants client auth");
        //}
        //else if (((SSLSocket) socket).getNeedClientAuth()) {
        //    System.err.println("XMPPConnection needs client auth");
        //}
        //else {
        //    System.err.println("XMPPConnection does not require client auth");
       // }
        // Set that TLS was successful
        usingTLS = true;

        // Set the new  writer to use
        packetWriter.setWriter(writer);
        // Send a new opening stream to the server
        packetWriter.openStream();
    }

    /**
     * Sets the available stream compression methods offered by the server.
     *
     * @param methods compression methods offered by the server.
     */
    void setAvailableCompressionMethods(Collection<String> methods) {
        compressionMethods = methods;
    }

    /**
     * Returns the compression handler that can be used for one compression methods offered by the server.
     * 
     * @return a instance of XMPPInputOutputStream or null if no suitable instance was found
     * 
     */
    private XMPPInputOutputStream maybeGetCompressionHandler() {
        if (compressionMethods != null) {
            for (XMPPInputOutputStream handler : SmackConfiguration.getCompresionHandlers()) {
                String method = handler.getCompressionMethod();
                if (compressionMethods.contains(method))
                    return handler;
            }
        }
        return null;
    }

    public boolean isUsingCompression() {
        return compressionHandler != null && serverAckdCompression;
    }

    /**
     * Starts using stream compression that will compress network traffic. Traffic can be
     * reduced up to 90%. Therefore, stream compression is ideal when using a slow speed network
     * connection. However, the server and the client will need to use more CPU time in order to
     * un/compress network data so under high load the server performance might be affected.
     * <p>
     * <p>
     * Stream compression has to have been previously offered by the server. Currently only the
     * zlib method is supported by the client. Stream compression negotiation has to be done
     * before authentication took place.<p>
     * <p>
     *
     * @return true if stream compression negotiation was successful.
     * @throws IOException if the compress stanza could not be send
     */
    private boolean useCompression() throws IOException {
        // If stream compression was offered by the server and we want to use
        // compression then send compression request to the server
        if (authenticated) {
            throw new IllegalStateException("Compression should be negotiated before authentication.");
        }

        if ((compressionHandler = maybeGetCompressionHandler()) != null) {
            synchronized (this) {
                requestStreamCompression(compressionHandler.getCompressionMethod());
                // Wait until compression is being used or a timeout happened
                try {
                    wait(getPacketReplyTimeout());
                }
                catch (InterruptedException e) {
                    // Ignore.
                }
            }
            return isUsingCompression();
        }
        return false;
    }

    /**
     * Request the server that we want to start using stream compression. When using TLS
     * then negotiation of stream compression can only happen after TLS was negotiated. If TLS
     * compression is being used the stream compression should not be used.
     * @throws IOException if the compress stanza could not be send
     */
    private void requestStreamCompression(String method) throws IOException {
        writer.write("<compress xmlns='http://jabber.org/protocol/compress'>");
        writer.write("<method>" + method + "</method></compress>");
        writer.flush();
    }

    /**
     * Start using stream compression since the server has acknowledged stream compression.
     *
     * @throws IOException if there is an exception starting stream compression.
     */
    void startStreamCompression() throws IOException {
        serverAckdCompression = true;
        // Initialize the reader and writer with the new secured version
        initReaderAndWriter();

        // Set the new  writer to use
        packetWriter.setWriter(writer);
        // Send a new opening stream to the server
        packetWriter.openStream();
        // Notify that compression is being used
        streamCompressionNegotiationDone();
    }

    /**
     * Notifies the XMPP connection that stream compression negotiation is done so that the
     * connection process can proceed.
     */
    synchronized void streamCompressionNegotiationDone() {
        this.notify();
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
    public void connect() throws SmackException, IOException, XMPPException {
        // Establishes the connection, readers and writers
        connectUsingConfiguration(config);
        // TODO is there a case where connectUsing.. does not throw an exception but connected is
        // still false?
        if (connected) {
            callConnectionConnectedListener();
        }
        // Automatically makes the login if the user was previously connected successfully
        // to the server and the connection was terminated abruptly
        if (connected && wasAuthenticated) {
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
     * Sets whether the connection has already logged in the server.
     *
     * @param wasAuthenticated true if the connection has already been authenticated.
     */
    private void setWasAuthenticated(boolean wasAuthenticated) {
        if (!this.wasAuthenticated) {
            this.wasAuthenticated = wasAuthenticated;
        }
    }

    /**
     * Sends out a notification that there was an error with the connection
     * and closes the connection. Also prints the stack trace of the given exception
     *
     * @param e the exception that causes the connection close event.
     */
    synchronized void notifyConnectionError(Exception e) {
        // Listeners were already notified of the exception, return right here.
        if ((packetReader == null || packetReader.done) &&
                (packetWriter == null || packetWriter.done)) return;

        if (packetReader != null)
            packetReader.done = true;
        if (packetWriter != null)
            packetWriter.done = true;
        // Closes the connection temporary. A reconnection is possible
        shutdown(new Presence(Presence.Type.unavailable));
        // Notify connection listeners of the error.
        callConnectionClosedOnErrorListener(e);
    }

    /**
     * Sends a notification indicating that the connection was reconnected successfully.
     */
    protected void notifyReconnection() {
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
}
