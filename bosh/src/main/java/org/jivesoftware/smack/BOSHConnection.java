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
import java.io.PipedReader;
import java.io.PipedWriter;
import java.io.Writer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smack.util.StringUtils;

import org.igniterealtime.jbosh.BOSHClient;
import org.igniterealtime.jbosh.BOSHClientConfig;
import org.igniterealtime.jbosh.BOSHClientConnEvent;
import org.igniterealtime.jbosh.BOSHClientConnListener;
import org.igniterealtime.jbosh.BOSHClientRequestListener;
import org.igniterealtime.jbosh.BOSHClientResponseListener;
import org.igniterealtime.jbosh.BOSHException;
import org.igniterealtime.jbosh.BOSHMessageEvent;
import org.igniterealtime.jbosh.BodyQName;
import org.igniterealtime.jbosh.ComposableBody;

/**
 * Creates a connection to a XMPP server via HTTP binding.
 * This is specified in the XEP-0206: XMPP Over BOSH.
 * 
 * @see Connection
 * @author Guenther Niess
 */
public class BOSHConnection extends Connection {

    /**
     * The XMPP Over Bosh namespace.
     */
    public static final String XMPP_BOSH_NS = "urn:xmpp:xbosh";

    /**
     * The BOSH namespace from XEP-0124.
     */
    public static final String BOSH_URI = "http://jabber.org/protocol/httpbind";

    /**
     * The used BOSH client from the jbosh library.
     */
    private BOSHClient client;

    /**
     * Holds the initial configuration used while creating the connection.
     */
    private final BOSHConfiguration config;

    // Some flags which provides some info about the current state.
    private boolean connected = false;
    private boolean authenticated = false;
    private boolean anonymous = false;
    private boolean isFirstInitialization = true;
    private boolean wasAuthenticated = false;
    private boolean done = false;

    /**
     * The Thread environment for sending packet listeners.
     */
    private ExecutorService listenerExecutor;

    // The readerPipe and consumer thread are used for the debugger.
    private PipedWriter readerPipe;
    private Thread readerConsumer;

    /**
     * The BOSH equivalent of the stream ID which is used for DIGEST authentication.
     */
    protected String authID = null;

    /**
     * The session ID for the BOSH session with the connection manager.
     */
    protected String sessionID = null;

    /**
     * The full JID of the authenticated user.
     */
    private String user = null;

    /**
     * The roster maybe also called buddy list holds the list of the users contacts.
     */
    private Roster roster = null;


    /**
     * Create a HTTP Binding connection to a XMPP server.
     * 
     * @param https true if you want to use SSL
     *             (e.g. false for http://domain.lt:7070/http-bind).
     * @param host the hostname or IP address of the connection manager
     *             (e.g. domain.lt for http://domain.lt:7070/http-bind).
     * @param port the port of the connection manager
     *             (e.g. 7070 for http://domain.lt:7070/http-bind).
     * @param filePath the file which is described by the URL
     *             (e.g. /http-bind for http://domain.lt:7070/http-bind).
     * @param xmppDomain the XMPP service name
     *             (e.g. domain.lt for the user alice@domain.lt)
     */
    public BOSHConnection(boolean https, String host, int port, String filePath, String xmppDomain) {
        super(new BOSHConfiguration(https, host, port, filePath, xmppDomain));
        this.config = (BOSHConfiguration) getConfiguration();
    }

    /**
     * Create a HTTP Binding connection to a XMPP server.
     * 
     * @param config The configuration which is used for this connection.
     */
    public BOSHConnection(BOSHConfiguration config) {
        super(config);
        this.config = config;
    }

    public void connect() throws XMPPException {
        if (connected) {
            throw new IllegalStateException("Already connected to a server.");
        }
        done = false;
        try {
            // Ensure a clean starting state
            if (client != null) {
                client.close();
                client = null;
            }
            saslAuthentication.init();
            sessionID = null;
            authID = null;

            // Initialize BOSH client
            BOSHClientConfig.Builder cfgBuilder = BOSHClientConfig.Builder
                    .create(config.getURI(), config.getServiceName());
            if (config.isProxyEnabled()) {
                cfgBuilder.setProxy(config.getProxyAddress(), config.getProxyPort());
            }
            client = BOSHClient.create(cfgBuilder.build());

            // Create an executor to deliver incoming packets to listeners.
            // We'll use a single thread with an unbounded queue.
            listenerExecutor = Executors
                    .newSingleThreadExecutor(new ThreadFactory() {
                        public Thread newThread(Runnable runnable) {
                            Thread thread = new Thread(runnable,
                                    "Smack Listener Processor ("
                                            + connectionCounterValue + ")");
                            thread.setDaemon(true);
                            return thread;
                        }
                    });
            client.addBOSHClientConnListener(new BOSHConnectionListener(this));
            client.addBOSHClientResponseListener(new BOSHPacketReader(this));

            // Initialize the debugger
            if (config.isDebuggerEnabled()) {
                initDebugger();
                if (isFirstInitialization) {
                    if (debugger.getReaderListener() != null) {
                        addPacketListener(debugger.getReaderListener(), null);
                    }
                    if (debugger.getWriterListener() != null) {
                        addPacketSendingListener(debugger.getWriterListener(), null);
                    }
                }
            }

            // Send the session creation request
            client.send(ComposableBody.builder()
                    .setNamespaceDefinition("xmpp", XMPP_BOSH_NS)
                    .setAttribute(BodyQName.createWithPrefix(XMPP_BOSH_NS, "version", "xmpp"), "1.0")
                    .build());
        } catch (Exception e) {
            throw new XMPPException("Can't connect to " + getServiceName(), e);
        }

        // Wait for the response from the server
        synchronized (this) {
            if (!connected) {
                try {
                    wait(SmackConfiguration.getDefaultPacketReplyTimeout()*6);
                }
                catch (InterruptedException e) {}
            }
        }

        // If there is no feedback, throw an remote server timeout error
        if (!connected && !done) {
            done = true;
            String errorMessage = "Timeout reached for the connection to " 
                    + getHost() + ":" + getPort() + ".";
            throw new XMPPException(
                    errorMessage,
                    new XMPPError(XMPPError.Condition.remote_server_timeout, errorMessage));
        }
    }

    public String getConnectionID() {
        if (!connected) {
            return null;
        } else if (authID != null) {
            return authID;
        } else {
            return sessionID;
        }
    }

    public Roster getRoster() {
        if (roster == null) {
            return null;
        }
        if (!config.isRosterLoadedAtLogin()) {
            roster.reload();
        }
        // If this is the first time the user has asked for the roster after calling
        // login, we want to wait for the server to send back the user's roster.
        // This behavior shields API users from having to worry about the fact that
        // roster operations are asynchronous, although they'll still have to listen
        // for changes to the roster. Note: because of this waiting logic, internal
        // Smack code should be wary about calling the getRoster method, and may
        // need to access the roster object directly.
        if (!roster.rosterInitialized) {
            try {
                synchronized (roster) {
                    long waitTime = SmackConfiguration.getDefaultPacketReplyTimeout();
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
            } catch (InterruptedException ie) {
                // Ignore.
            }
        }
        return roster;
    }

    public String getUser() {
        return user;
    }

    public boolean isAnonymous() {
        return anonymous;
    }

    public boolean isAuthenticated() {
        return authenticated;
    }

    public boolean isConnected() {
        return connected;
    }

    public boolean isSecureConnection() {
        // TODO: Implement SSL usage
        return false;
    }

    public boolean isUsingCompression() {
        // TODO: Implement compression
        return false;
    }

    public void login(String username, String password, String resource)
            throws XMPPException {
        if (!isConnected()) {
            throw new IllegalStateException("Not connected to server.");
        }
        if (authenticated) {
            throw new IllegalStateException("Already logged in to server.");
        }
        // Do partial version of nameprep on the username.
        username = username.toLowerCase().trim();

        String response;
        if (saslAuthentication.hasNonAnonymousAuthentication()) {
            // Authenticate using SASL
            if (password != null) {
                response = saslAuthentication.authenticate(username, password, resource);
            } else {
                response = saslAuthentication.authenticate(resource, config.getCallbackHandler());
            }
        } else {
            throw new XMPPException("No non-anonymous SASL authentication mechanism available");
        }

        // Set the user.
        if (response != null) {
            this.user = response;
            // Update the serviceName with the one returned by the server
            config.setServiceName(StringUtils.parseServer(response));
        } else {
            this.user = username + "@" + getServiceName();
            if (resource != null) {
                this.user += "/" + resource;
            }
        }

        // Create the roster if it is not a reconnection.
        if (this.roster == null) {
            this.roster = new Roster(this);
        }
        if (config.isRosterLoadedAtLogin()) {
            this.roster.reload();
        }

        // Set presence to online.
        if (config.isSendPresence()) {
            sendPacket(new Presence(Presence.Type.available));
        }

        // Indicate that we're now authenticated.
        authenticated = true;
        anonymous = false;

        // Stores the autentication for future reconnection
        config.setLoginInfo(username, password, resource);

        // If debugging is enabled, change the the debug window title to include
        // the
        // name we are now logged-in as.l
        if (config.isDebuggerEnabled() && debugger != null) {
            debugger.userHasLogged(user);
        }
    }

    public void loginAnonymously() throws XMPPException {
    	if (!isConnected()) {
            throw new IllegalStateException("Not connected to server.");
        }
        if (authenticated) {
            throw new IllegalStateException("Already logged in to server.");
        }

        String response;
        if (saslAuthentication.hasAnonymousAuthentication()) {
            response = saslAuthentication.authenticateAnonymously();
        }
        else {
            // Authenticate using Non-SASL
            throw new XMPPException("No anonymous SASL authentication mechanism available");
        }

        // Set the user value.
        this.user = response;
        // Update the serviceName with the one returned by the server
        config.setServiceName(StringUtils.parseServer(response));

        // Anonymous users can't have a roster.
        roster = null;

        // Set presence to online.
        if (config.isSendPresence()) {
            sendPacket(new Presence(Presence.Type.available));
        }

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
    }

    public void sendPacket(Packet packet) {
        if (!isConnected()) {
            throw new IllegalStateException("Not connected to server.");
        }
        if (packet == null) {
            throw new NullPointerException("Packet is null.");
        }
        if (!done) {
            // Invoke interceptors for the new packet that is about to be sent.
            // Interceptors
            // may modify the content of the packet.
            firePacketInterceptors(packet);

            try {
                send(ComposableBody.builder().setPayloadXML(packet.toXML())
                        .build());
            } catch (BOSHException e) {
                e.printStackTrace();
                return;
            }

            // Process packet writer listeners. Note that we're using the
            // sending
            // thread so it's expected that listeners are fast.
            firePacketSendingListeners(packet);
        }
    }

    public void disconnect(Presence unavailablePresence) {
        if (!connected) {
            return;
        }
        shutdown(unavailablePresence);

        // Cleanup
        // TODO still needed? Smack 4.0.0 BOSH
//        if (roster != null) {
//            roster.cleanup();
//            roster = null;
//        }
        sendListeners.clear();
        recvListeners.clear();
        collectors.clear();
        interceptors.clear();

        // Reset the connection flags
        wasAuthenticated = false;
        isFirstInitialization = true;

        // Notify connection listeners of the connection closing if done hasn't already been set.
        for (ConnectionListener listener : getConnectionListeners()) {
            try {
                listener.connectionClosed();
            }
            catch (Exception e) {
                // Catch and print any exception so we can recover
                // from a faulty listener and finish the shutdown process
                e.printStackTrace();
            }
        }
    }

    /**
     * Closes the connection by setting presence to unavailable and closing the 
     * HTTP client. The shutdown logic will be used during a planned disconnection or when
     * dealing with an unexpected disconnection. Unlike {@link #disconnect()} the connection's
     * BOSH packet reader and {@link Roster} will not be removed; thus
     * connection's state is kept.
     *
     * @param unavailablePresence the presence packet to send during shutdown.
     */
    protected void shutdown(Presence unavailablePresence) {
        setWasAuthenticated(authenticated);
        authID = null;
        sessionID = null;
        done = true;
        authenticated = false;
        connected = false;
        isFirstInitialization = false;

        try {
            client.disconnect(ComposableBody.builder()
                    .setNamespaceDefinition("xmpp", XMPP_BOSH_NS)
                    .setPayloadXML(unavailablePresence.toXML())
                    .build());
            // Wait 150 ms for processes to clean-up, then shutdown.
            Thread.sleep(150);
        }
        catch (Exception e) {
            // Ignore.
        }

        // Close down the readers and writers.
        if (readerPipe != null) {
            try {
                readerPipe.close();
            }
            catch (Throwable ignore) { /* ignore */ }
            reader = null;
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
            catch (Throwable ignore) { /* ignore */ }
            writer = null;
        }

        // Shut down the listener executor.
        if (listenerExecutor != null) {
            listenerExecutor.shutdown();
        }
        readerConsumer = null;
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
     * Send a HTTP request to the connection manager with the provided body element.
     * 
     * @param body the body which will be sent.
     */
    protected void send(ComposableBody body) throws BOSHException {
        if (!connected) {
            throw new IllegalStateException("Not connected to a server!");
        }
        if (body == null) {
            throw new NullPointerException("Body mustn't be null!");
        }
        if (sessionID != null) {
            body = body.rebuild().setAttribute(
                    BodyQName.create(BOSH_URI, "sid"), sessionID).build();
        }
        client.send(body);
    }

    /**
     * Processes a packet after it's been fully parsed by looping through the
     * installed packet collectors and listeners and letting them examine the
     * packet to see if they are a match with the filter.
     * 
     * @param packet the packet to process.
     */
    protected void processPacket(Packet packet) {
        if (packet == null) {
            return;
        }

        // Loop through all collectors and notify the appropriate ones.
        for (PacketCollector collector : getPacketCollectors()) {
            collector.processPacket(packet);
        }

        // Deliver the incoming packet to listeners.
        listenerExecutor.submit(new ListenerNotification(packet));
    }

    /**
     * Initialize the SmackDebugger which allows to log and debug XML traffic.
     */
    protected void initDebugger() {
        // TODO: Maybe we want to extend the SmackDebugger for simplification
        //       and a performance boost.

        // Initialize a empty writer which discards all data.
        writer = new Writer() {
                public void write(char[] cbuf, int off, int len) { /* ignore */}
                public void close() { /* ignore */ }
                public void flush() { /* ignore */ }
            };

        // Initialize a pipe for received raw data.
        try {
            readerPipe = new PipedWriter();
            reader = new PipedReader(readerPipe);
        }
        catch (IOException e) {
            // Ignore
        }

        // Call the method from the parent class which initializes the debugger.
        super.initDebugger();

        // Add listeners for the received and sent raw data.
        client.addBOSHClientResponseListener(new BOSHClientResponseListener() {
            public void responseReceived(BOSHMessageEvent event) {
                if (event.getBody() != null) {
                    try {
                        readerPipe.write(event.getBody().toXML());
                        readerPipe.flush();
                    } catch (Exception e) {
                        // Ignore
                    }
                }
            }
        });
        client.addBOSHClientRequestListener(new BOSHClientRequestListener() {
            public void requestSent(BOSHMessageEvent event) {
                if (event.getBody() != null) {
                    try {
                        writer.write(event.getBody().toXML());
                    } catch (Exception e) {
                        // Ignore
                    }
                }
            }
        });

        // Create and start a thread which discards all read data.
        readerConsumer = new Thread() {
            private Thread thread = this;
            private int bufferLength = 1024;

            public void run() {
                try {
                    char[] cbuf = new char[bufferLength];
                    while (readerConsumer == thread && !done) {
                        reader.read(cbuf, 0, bufferLength);
                    }
                } catch (IOException e) {
                    // Ignore
                }
            }
        };
        readerConsumer.setDaemon(true);
        readerConsumer.start();
    }

    /**
     * Sends out a notification that there was an error with the connection
     * and closes the connection.
     *
     * @param e the exception that causes the connection close event.
     */
    protected void notifyConnectionError(Exception e) {
        // Closes the connection temporary. A reconnection is possible
        shutdown(new Presence(Presence.Type.unavailable));
        // Print the stack trace to help catch the problem
        e.printStackTrace();
        // Notify connection listeners of the error.
        for (ConnectionListener listener : getConnectionListeners()) {
            try {
                listener.connectionClosedOnError(e);
            }
            catch (Exception e2) {
                // Catch and print any exception so we can recover
                // from a faulty listener
                e2.printStackTrace();
            }
        }
    }


    /**
     * A listener class which listen for a successfully established connection
     * and connection errors and notifies the BOSHConnection.
     * 
     * @author Guenther Niess
     */
    private class BOSHConnectionListener implements BOSHClientConnListener {

        private final BOSHConnection connection;

        public BOSHConnectionListener(BOSHConnection connection) {
            this.connection = connection;
        }

        /**
         * Notify the BOSHConnection about connection state changes.
         * Process the connection listeners and try to login if the
         * connection was formerly authenticated and is now reconnected.
         */
        public void connectionEvent(BOSHClientConnEvent connEvent) {
            try {
                if (connEvent.isConnected()) {
                    connected = true;
                    if (isFirstInitialization) {
                        isFirstInitialization = false;
                        for (ConnectionCreationListener listener : getConnectionCreationListeners()) {
                            listener.connectionCreated(connection);
                        }
                    }
                    else {
                        try {
                            if (wasAuthenticated) {
                                connection.login(
                                        config.getUsername(),
                                        config.getPassword(),
                                        config.getResource());
                            }
                            for (ConnectionListener listener : getConnectionListeners()) {
                                 listener.reconnectionSuccessful();
                            }
                        }
                        catch (XMPPException e) {
                            for (ConnectionListener listener : getConnectionListeners()) {
                                listener.reconnectionFailed(e);
                           }
                        }
                    }
                }
                else {
                    if (connEvent.isError()) {
                        try {
                            connEvent.getCause();
                        }
                        catch (Exception e) {
                            notifyConnectionError(e);
                        }
                    }
                    connected = false;
                }
            }
            finally {
                synchronized (connection) {
                    connection.notifyAll();
                }
            }
        }
    }

    /**
     * This class notifies all listeners that a packet was received.
     */
    private class ListenerNotification implements Runnable {

        private Packet packet;

        public ListenerNotification(Packet packet) {
            this.packet = packet;
        }

        public void run() {
            for (ListenerWrapper listenerWrapper : recvListeners.values()) {
                listenerWrapper.notifyListener(packet);
            }
        }
    }
}
