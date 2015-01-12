/**
 *
 * Copyright 2009 Jonas Ådahl.
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
package org.jivesoftware.smack.serverless;


import org.jivesoftware.smack.AbstractXMPPConnection;
import org.jivesoftware.smack.ConnectionListener;
import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.SmackException.NotConnectedException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.PlainStreamElement;
import org.jivesoftware.smack.packet.XMPPError;
import org.xmlpull.v1.XmlPullParser;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;


/**
 * Link-local XMPP connection according to XEP-0174 connection. Automatically
 * created by LLService and closed by inactivity.
 *
 */
public class XMPPLLConnection extends AbstractXMPPConnection {

    private LLService service;
    private LLPresence localPresence, remotePresence;
    private boolean initiator;
    private long lastActivity = 0;
    // FIXME this should become a boolean, it's purpose is to detect "inactive" connections
    protected XMPPLLConnection connection;
    private Thread timeoutThread;
    private Socket socket;

    /**
     * Protected access level because of unit test purposes
     */
    protected Object packetWriter;

    /**
     * Protected access level because of unit test purposes
     */
    protected Object packetReader;

    /**
     * Instantiate a new link-local connection. Use the config parameter to
     * specify if the connection is acting as server or client.
     *
     * @param config specification about how the new connection is to be set up.
     */
    XMPPLLConnection(LLService service, LLConnectionConfiguration config) {
        super(config);
        // Always append the "from" attribute
        setFromMode(FromMode.USER);
        connection = this;
        this.service = service;
        updateLastActivity();

        // A timeout thread's purpose is to close down inactive connections
        // after a certain amount of seconds (defaults to 15).
        timeoutThread = new Thread() {
            public void run() {
                try {
                    while (connection != null) {
                        //synchronized (connection) {
                            Thread.sleep(14000);
                            long currentTime = new Date().getTime();
                            if (currentTime - lastActivity > 15000) {
                                shutdown();
                                break;
                            }
                        //}
                    }
                } catch (InterruptedException ie) {
                    shutdown();
                }
            }
        };

        timeoutThread.setName("Smack Link-local Connection Timeout (" + connection.connectionCounterValue + ")");
        timeoutThread.setDaemon(true);

        // Move to LLConnectionConfiguration#init
        if (config.isInitiator()) {
            // we are connecting to remote host
            localPresence = config.getLocalPresence();
            remotePresence = config.getRemotePresence();
            initiator = true;
        } else {
            // a remote host connected to us
            localPresence = config.getLocalPresence();
            remotePresence = null;
            initiator = false;
            socket = config.getSocket();
        }
    }

    /**
     * Return this connection's LLService
     */
    public LLService getService() {
        return service;
    }

    /**
     * Tells if this connection instance is the initiator.
     *
     * @return true if this instance is the one connecting to a remote peer.
     */
    public boolean isInitiator() {
        return initiator;
    }

//    /**
//     * Return the user name of the remote peer (service name).
//     *
//     * @return the remote hosts service name / username
//     */
//    public String getUser() {
//        // username is the service name of the local presence
//        return localPresence.getServiceName();
//    }

    /**
     * Sets the name of the service provided in the <stream:stream ...> from the remote peer.
     *
     * @param serviceName the name of the service
     */
    public void setServiceName(String serviceName) {
//        ((LLConnectionConfiguration)config).setServiceName(serviceName);
        //((LLConnectionConfiguration)config).setServiceName(remotePresence.getServiceName());
        //LLConnectionConfiguration llconfig = new LLConnectionConfiguration(localPresence, remotePresence);
        //llconfig.setServiceName("Test");

    }


    /**
     * Set the remote presence. Used when being connected,
     * will not know the remote service name until stream is initiated.
     *
     * @param remotePresence presence information about the connecting client.
     */
    void setRemotePresence(LLPresence remotePresence) {
        this.remotePresence = remotePresence;
    }

    /**
     * Start listen for data and a stream tag.
     */
    void initListen() throws XMPPException, IOException, SmackException {
        initConnection();
    }

    /**
     * Create a socket, connect to the remote peer and initiate a XMPP stream session.
     */
    public void connect() throws IOException, SmackException, XMPPException.XMPPErrorException {
        String host = remotePresence.getHost();
        int port = remotePresence.getPort();

        try {
            socket = new Socket(host, port);
        } catch (Exception e) {
            // TODO
            throw new SmackException(e);
        }
//        catch (UnknownHostException uhe) {
//            String errorMessage = "Could not connect to " + host + ":" + port + ".";
//            throw new XMPPException.XMPPErrorException(errorMessage, new XMPPError(
//                    XMPPError.Condition.remote_server_timeout, errorMessage),
//                    uhe);
//        }
//        catch (IOException ioe) {
//            String errorMessage = "Error connecting to " + host + ":"
//                    + port + ".";
//            throw new XMPPException.XMPPErrorException(errorMessage, new XMPPError(
//                    XMPPError.Condition.remote_server_error, errorMessage), ioe);
//        }
        initConnection();

        notifyLLListenersConnected();
    }


    /**
     * Handles the opening of a stream after a remote client has connected and opened a stream.
     * @throws XMPPException if service name is missing or service is unknown to the mDNS daemon.
     */
    public void streamInitiatingReceived() throws XMPPException {
        if (config.getServiceName() == null) {
            shutdown();
        } else {
//            packetWriter = new LLPacketWriter();
            if (debugger != null) {
                if (debugger.getWriterListener() != null) {
                    addPacketListener(debugger.getWriterListener(), null);
                }
            }
            // TODO
//            packetWriter.startup();
            notifyLLListenersConnected();
        }
    }

    /**
     * Notify new connection listeners that a new connection has been established.
     */
    private void notifyLLListenersConnected() {
        for (ConnectionListener listener : getConnectionListeners()) {
            listener.connected(this);
        }
    }

    /**
     * Update the timer telling when the last activity happend. Used by timeout
     * thread to tell how long the connection has been inactive.
     */
    void updateLastActivity() {
        lastActivity = new Date().getTime();
    }

    /**
     * Sends the specified packet to the remote peer.
     *
     * @param packet the packet to send
     */
    @Override
    public void sendPacket(Packet packet) throws SmackException.NotConnectedException {
        updateLastActivity();
        super.sendPacket(packet);
    }

    /**
     * Initializes the connection by creating a packet reader and writer and opening a
     * XMPP stream to the server.
     *
     * @throws XMPPException if establishing a connection to the server fails.
     */
    private void initConnection() throws IOException, XMPPException.XMPPErrorException, SmackException {
        try {
            // Set the reader and writer instance variables
            initReaderAndWriter();
            timeoutThread.start();
            // Don't initialize packet writer until we know it's a valid connection
            // unless we are the initiator. If we are NOT the initializer, we instead
            // wait for a stream initiation before doing anything.
//            if (isInitiator())
//                packetWriter = new LLPacketWriter();

//            // Initialize packet reader
//            packetReader = new LLPacketReader();

            // If debugging is enabled, we should start the thread that will listen for
            // all packets and then log them.
            // XXX FIXME debugging enabled not working
            if (false) {//configuration.isDebuggerEnabled()) {
                addPacketListener(debugger.getReaderListener(), null);
            }

            // Make note of the fact that we're now connected.
            connected = true;

            // If we are the initiator start the packet writer. This will open a XMPP
            // stream to the server. If not, a packet writer will be started after
            // receiving an initial stream start tag.
            // TODO
//            if (isInitiator())
//                packetWriter.startup();
            // Start the packet reader. The startup() method will block until we
            // get an opening stream packet back from server.
//            packetReader.startup();
        }
        catch (XMPPException.XMPPErrorException ex) {
            // An exception occurred in setting up the connection. Make sure we shut down the
            // readers and writers and close the socket.

            shutdownPacketReadersAndWritersAndCloseSocket();

            throw ex;        // Everything stopped. Now throw the exception.
        }
    }

    private void shutdownPacketReadersAndWritersAndCloseSocket() {
        if (packetWriter != null) {
            try {
//                packetWriter.shutdown();
            }
            catch (Throwable ignore) { /* ignore */ }
            packetWriter = null;
        }
        if (packetReader != null) {
            try {
//                packetReader.shutdown();
            }
            catch (Throwable ignore) { /* ignore */ }
            packetReader = null;
        }
        if (socket != null) {
            try {
                socket.close();
            }
            catch (Exception e) { /* ignore */ }
            socket = null;
        }
        // closing reader after socket since reader.close() blocks otherwise
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
            catch (Throwable ignore) {  /* ignore */ }
            writer = null;
        }
        connected = false;
    }

    private void initReaderAndWriter() throws XMPPException.XMPPErrorException {
        try {
            reader =
                    new BufferedReader(new InputStreamReader(socket.getInputStream(), "UTF-8"));
            writer = new BufferedWriter(
                    new OutputStreamWriter(socket.getOutputStream(), "UTF-8"));
        }
        catch (IOException ioe) {
            // TODO
            throw new RuntimeException(ioe);
//            throw new XMPPException.XMPPErrorException(
//                    "XMPPError establishing connection with server.",
//                    new XMPPError(XMPPError.Condition.remote_server_error,
//                            "XMPPError establishing connection with server."),
//                    ioe);
        }

        // If debugging is enabled, we open a window and write out all network traffic.
        initDebugger();
    }

    protected void shutdown() {
        connection = null;

//        if (packetReader != null)
//            packetReader.shutdown();
//        if (packetWriter != null)
//            packetWriter.shutdown();

        // Wait 150 ms for processes to clean-up, then shutdown.
        try {
            Thread.sleep(150);
        }
        catch (Exception e) {
            // Ignore.
        }

        // Close down the readers and writers.
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

        try {
            socket.close();
        }
        catch (Exception e) {
            // Ignore.
        }
    } 

    public void disconnect() {
        // If not connected, ignore this request.
        if (packetReader == null || packetWriter == null) {
            return;
        }

        shutdown();

        packetWriter = null;
        packetReader = null;
    }

//    protected class LLPacketReader extends PacketReader {
//
//        private boolean mGotStreamOpenedStanza = false;
//
//        LLPacketReader() throws SmackException {
//        }
//
//        public synchronized void startup() throws IOException, SmackException {
//            readerThread.start();
//
//            try {
//                // Wait until either:
//                // - the remote peer's stream initialization stanza has been parsed
//                // - an exception is thrown while parsing
//                // - the timeout occurs
//                if (connection.isInitiator())
//                    wait(getPacketReplyTimeout());
//            }
//            catch (InterruptedException ie) {
//                // Ignore.
//                ie.printStackTrace();
//            }
//            if (connection.isInitiator() && !mGotStreamOpenedStanza) {
//                throwConnectionExceptionOrNoResponse();
//            }
//        }
//
//        @Override
//        protected void handleStreamOpened(XmlPullParser parser) throws Exception {
//            super.handleStreamOpened(parser);
//
//            // if we are the initiator, this means stream has been initiated
//            // if we aren't the initiator, this means we have to respond with
//            // stream initiator.
//            if (connection.isInitiator()) {
//                mGotStreamOpenedStanza = true;
//                connection.connectionID = connection.getServiceName();
//                //releaseConnectionIDLock();
//            }
//            else {
//                // Check if service name is a known entity
//                // if it is, open the stream and keep it open
//                // otherwise open and immediately close it
//                if (connection.getServiceName() == null) {
//                    System.err.println("No service name specified in stream initiation, canceling.");
//                    shutdown();
//                } else {
//                    // Check if service name is known, if so
//                    // we will continue the session
//                    LLPresence presence = service.getPresenceByServiceName(connection.getServiceName());
//                    if (presence != null) {
//                        connection.setRemotePresence(presence);
//                        connectionID = connection.getServiceName();
//                        connection.streamInitiatingReceived();
//                        //releaseConnectionIDLock();
//                    } else {
//                        System.err.println("Unknown service name '" +
//                                connection.getServiceName() +
//                                "' specified in stream initation, canceling.");
//                        shutdown();
//                    }
//                }
//            }
//        }
//    }
//
//    protected class LLPacketWriter extends PacketWriter {
//
//
//        @Override
//        protected void openStream() throws IOException {
//            // Unlike traditional XMPP Stream initiation,
//            // we must provide our XEP-0174 Service Name
//            // in a "from" attribute
//            StringBuilder stream = new StringBuilder();
//            stream.append("<stream:stream");
//            stream.append(" to=\"").append(getServiceName()).append("\"");
//            if (initiator)
//                stream.append(" from=\"").append(((LLConnectionConfiguration) config).getLocalPresence().getServiceName()).append("\"");
//            else {
//                // TODO: We should be able to access the service name from the
//                // stream opening stanza that this is a response to.
//                String localServiceName = ((LLConnectionConfiguration) config).getLocalPresence().getJID();
//                localServiceName = localServiceName.substring(0, localServiceName.lastIndexOf("."));
//                stream.append(" from=\"").append(localServiceName).append("\"");
//            }
//            stream.append(" xmlns=\"jabber:client\"");
//            stream.append(" xmlns:stream=\"http://etherx.jabber.org/streams\"");
//            stream.append(" version=\"1.0\">");
//            writer.write(stream.toString());
//            writer.flush();
//        }
//    }

	@Override
	public String getConnectionID() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean isSecureConnection() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected void sendPacketInternal(Packet packet)
			throws NotConnectedException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void send(PlainStreamElement element) throws NotConnectedException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public boolean isUsingCompression() {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	protected void connectInternal() throws SmackException, IOException,
			XMPPException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void loginAnonymously() throws XMPPException, SmackException,
			IOException {
		// TODO Auto-generated method stub
		
	}

    @Override
    protected void loginNonAnonymously() throws XMPPException, SmackException,
            IOException {
        // TODO Auto-generated method stub
        
    }
}
