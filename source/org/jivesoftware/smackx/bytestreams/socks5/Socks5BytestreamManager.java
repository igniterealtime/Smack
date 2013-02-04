/**
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
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
package org.jivesoftware.smackx.bytestreams.socks5;

import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeoutException;

import org.jivesoftware.smack.AbstractConnectionListener;
import org.jivesoftware.smack.Connection;
import org.jivesoftware.smack.ConnectionCreationListener;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.IQ;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.XMPPError;
import org.jivesoftware.smack.util.SyncPacketSend;
import org.jivesoftware.smackx.ServiceDiscoveryManager;
import org.jivesoftware.smackx.bytestreams.BytestreamListener;
import org.jivesoftware.smackx.bytestreams.BytestreamManager;
import org.jivesoftware.smackx.bytestreams.socks5.packet.Bytestream;
import org.jivesoftware.smackx.bytestreams.socks5.packet.Bytestream.StreamHost;
import org.jivesoftware.smackx.bytestreams.socks5.packet.Bytestream.StreamHostUsed;
import org.jivesoftware.smackx.filetransfer.FileTransferManager;
import org.jivesoftware.smackx.packet.DiscoverInfo;
import org.jivesoftware.smackx.packet.DiscoverItems;
import org.jivesoftware.smackx.packet.DiscoverInfo.Identity;
import org.jivesoftware.smackx.packet.DiscoverItems.Item;

/**
 * The Socks5BytestreamManager class handles establishing SOCKS5 Bytestreams as specified in the <a
 * href="http://xmpp.org/extensions/xep-0065.html">XEP-0065</a>.
 * <p>
 * A SOCKS5 Bytestream is negotiated partly over the XMPP XML stream and partly over a separate
 * socket. The actual transfer though takes place over a separately created socket.
 * <p>
 * A SOCKS5 Bytestream generally has three parties, the initiator, the target, and the stream host.
 * The stream host is a specialized SOCKS5 proxy setup on a server, or, the initiator can act as the
 * stream host.
 * <p>
 * To establish a SOCKS5 Bytestream invoke the {@link #establishSession(String)} method. This will
 * negotiate a SOCKS5 Bytestream with the given target JID and return a socket.
 * <p>
 * If a session ID for the SOCKS5 Bytestream was already negotiated (e.g. while negotiating a file
 * transfer) invoke {@link #establishSession(String, String)}.
 * <p>
 * To handle incoming SOCKS5 Bytestream requests add an {@link Socks5BytestreamListener} to the
 * manager. There are two ways to add this listener. If you want to be informed about incoming
 * SOCKS5 Bytestreams from a specific user add the listener by invoking
 * {@link #addIncomingBytestreamListener(BytestreamListener, String)}. If the listener should
 * respond to all SOCKS5 Bytestream requests invoke
 * {@link #addIncomingBytestreamListener(BytestreamListener)}.
 * <p>
 * Note that the registered {@link Socks5BytestreamListener} will NOT be notified on incoming Socks5
 * bytestream requests sent in the context of <a
 * href="http://xmpp.org/extensions/xep-0096.html">XEP-0096</a> file transfer. (See
 * {@link FileTransferManager})
 * <p>
 * If no {@link Socks5BytestreamListener}s are registered, all incoming SOCKS5 Bytestream requests
 * will be rejected by returning a &lt;not-acceptable/&gt; error to the initiator.
 * 
 * @author Henning Staib
 */
public final class Socks5BytestreamManager implements BytestreamManager {

    /*
     * create a new Socks5BytestreamManager and register a shutdown listener on every established
     * connection
     */
    static {
        Connection.addConnectionCreationListener(new ConnectionCreationListener() {

            public void connectionCreated(Connection connection) {
                final Socks5BytestreamManager manager;
                manager = Socks5BytestreamManager.getBytestreamManager(connection);

                // register shutdown listener
                connection.addConnectionListener(new AbstractConnectionListener() {

                    public void connectionClosed() {
                        manager.disableService();
                    }

                });
            }

        });
    }

    /**
     * The XMPP namespace of the SOCKS5 Bytestream
     */
    public static final String NAMESPACE = "http://jabber.org/protocol/bytestreams";

    /* prefix used to generate session IDs */
    private static final String SESSION_ID_PREFIX = "js5_";

    /* random generator to create session IDs */
    private final static Random randomGenerator = new Random();

    /* stores one Socks5BytestreamManager for each XMPP connection */
    private final static Map<Connection, Socks5BytestreamManager> managers = new HashMap<Connection, Socks5BytestreamManager>();

    /* XMPP connection */
    private final Connection connection;

    /*
     * assigns a user to a listener that is informed if a bytestream request for this user is
     * received
     */
    private final Map<String, BytestreamListener> userListeners = new ConcurrentHashMap<String, BytestreamListener>();

    /*
     * list of listeners that respond to all bytestream requests if there are not user specific
     * listeners for that request
     */
    private final List<BytestreamListener> allRequestListeners = Collections.synchronizedList(new LinkedList<BytestreamListener>());

    /* listener that handles all incoming bytestream requests */
    private final InitiationListener initiationListener;

    /* timeout to wait for the response to the SOCKS5 Bytestream initialization request */
    private int targetResponseTimeout = 10000;

    /* timeout for connecting to the SOCKS5 proxy selected by the target */
    private int proxyConnectionTimeout = 10000;

    /* blacklist of errornous SOCKS5 proxies */
    private final List<String> proxyBlacklist = Collections.synchronizedList(new LinkedList<String>());

    /* remember the last proxy that worked to prioritize it */
    private String lastWorkingProxy = null;

    /* flag to enable/disable prioritization of last working proxy */
    private boolean proxyPrioritizationEnabled = true;

    /*
     * list containing session IDs of SOCKS5 Bytestream initialization packets that should be
     * ignored by the InitiationListener
     */
    private List<String> ignoredBytestreamRequests = Collections.synchronizedList(new LinkedList<String>());

    /**
     * Returns the Socks5BytestreamManager to handle SOCKS5 Bytestreams for a given
     * {@link Connection}.
     * <p>
     * If no manager exists a new is created and initialized.
     * 
     * @param connection the XMPP connection or <code>null</code> if given connection is
     *        <code>null</code>
     * @return the Socks5BytestreamManager for the given XMPP connection
     */
    public static synchronized Socks5BytestreamManager getBytestreamManager(Connection connection) {
        if (connection == null) {
            return null;
        }
        Socks5BytestreamManager manager = managers.get(connection);
        if (manager == null) {
            manager = new Socks5BytestreamManager(connection);
            managers.put(connection, manager);
            manager.activate();
        }
        return manager;
    }

    /**
     * Private constructor.
     * 
     * @param connection the XMPP connection
     */
    private Socks5BytestreamManager(Connection connection) {
        this.connection = connection;
        this.initiationListener = new InitiationListener(this);
    }

    /**
     * Adds BytestreamListener that is called for every incoming SOCKS5 Bytestream request unless
     * there is a user specific BytestreamListener registered.
     * <p>
     * If no listeners are registered all SOCKS5 Bytestream request are rejected with a
     * &lt;not-acceptable/&gt; error.
     * <p>
     * Note that the registered {@link BytestreamListener} will NOT be notified on incoming Socks5
     * bytestream requests sent in the context of <a
     * href="http://xmpp.org/extensions/xep-0096.html">XEP-0096</a> file transfer. (See
     * {@link FileTransferManager})
     * 
     * @param listener the listener to register
     */
    public void addIncomingBytestreamListener(BytestreamListener listener) {
        this.allRequestListeners.add(listener);
    }

    /**
     * Removes the given listener from the list of listeners for all incoming SOCKS5 Bytestream
     * requests.
     * 
     * @param listener the listener to remove
     */
    public void removeIncomingBytestreamListener(BytestreamListener listener) {
        this.allRequestListeners.remove(listener);
    }

    /**
     * Adds BytestreamListener that is called for every incoming SOCKS5 Bytestream request from the
     * given user.
     * <p>
     * Use this method if you are awaiting an incoming SOCKS5 Bytestream request from a specific
     * user.
     * <p>
     * If no listeners are registered all SOCKS5 Bytestream request are rejected with a
     * &lt;not-acceptable/&gt; error.
     * <p>
     * Note that the registered {@link BytestreamListener} will NOT be notified on incoming Socks5
     * bytestream requests sent in the context of <a
     * href="http://xmpp.org/extensions/xep-0096.html">XEP-0096</a> file transfer. (See
     * {@link FileTransferManager})
     * 
     * @param listener the listener to register
     * @param initiatorJID the JID of the user that wants to establish a SOCKS5 Bytestream
     */
    public void addIncomingBytestreamListener(BytestreamListener listener, String initiatorJID) {
        this.userListeners.put(initiatorJID, listener);
    }

    /**
     * Removes the listener for the given user.
     * 
     * @param initiatorJID the JID of the user the listener should be removed
     */
    public void removeIncomingBytestreamListener(String initiatorJID) {
        this.userListeners.remove(initiatorJID);
    }

    /**
     * Use this method to ignore the next incoming SOCKS5 Bytestream request containing the given
     * session ID. No listeners will be notified for this request and and no error will be returned
     * to the initiator.
     * <p>
     * This method should be used if you are awaiting a SOCKS5 Bytestream request as a reply to
     * another packet (e.g. file transfer).
     * 
     * @param sessionID to be ignored
     */
    public void ignoreBytestreamRequestOnce(String sessionID) {
        this.ignoredBytestreamRequests.add(sessionID);
    }

    /**
     * Disables the SOCKS5 Bytestream manager by removing the SOCKS5 Bytestream feature from the
     * service discovery, disabling the listener for SOCKS5 Bytestream initiation requests and
     * resetting its internal state.
     * <p>
     * To re-enable the SOCKS5 Bytestream feature invoke {@link #getBytestreamManager(Connection)}.
     * Using the file transfer API will automatically re-enable the SOCKS5 Bytestream feature.
     */
    public synchronized void disableService() {

        // remove initiation packet listener
        this.connection.removePacketListener(this.initiationListener);

        // shutdown threads
        this.initiationListener.shutdown();

        // clear listeners
        this.allRequestListeners.clear();
        this.userListeners.clear();

        // reset internal state
        this.lastWorkingProxy = null;
        this.proxyBlacklist.clear();
        this.ignoredBytestreamRequests.clear();

        // remove manager from static managers map
        managers.remove(this.connection);

        // shutdown local SOCKS5 proxy if there are no more managers for other connections
        if (managers.size() == 0) {
            Socks5Proxy.getSocks5Proxy().stop();
        }

        // remove feature from service discovery
        ServiceDiscoveryManager serviceDiscoveryManager = ServiceDiscoveryManager.getInstanceFor(this.connection);

        // check if service discovery is not already disposed by connection shutdown
        if (serviceDiscoveryManager != null) {
            serviceDiscoveryManager.removeFeature(NAMESPACE);
        }

    }

    /**
     * Returns the timeout to wait for the response to the SOCKS5 Bytestream initialization request.
     * Default is 10000ms.
     * 
     * @return the timeout to wait for the response to the SOCKS5 Bytestream initialization request
     */
    public int getTargetResponseTimeout() {
        if (this.targetResponseTimeout <= 0) {
            this.targetResponseTimeout = 10000;
        }
        return targetResponseTimeout;
    }

    /**
     * Sets the timeout to wait for the response to the SOCKS5 Bytestream initialization request.
     * Default is 10000ms.
     * 
     * @param targetResponseTimeout the timeout to set
     */
    public void setTargetResponseTimeout(int targetResponseTimeout) {
        this.targetResponseTimeout = targetResponseTimeout;
    }

    /**
     * Returns the timeout for connecting to the SOCKS5 proxy selected by the target. Default is
     * 10000ms.
     * 
     * @return the timeout for connecting to the SOCKS5 proxy selected by the target
     */
    public int getProxyConnectionTimeout() {
        if (this.proxyConnectionTimeout <= 0) {
            this.proxyConnectionTimeout = 10000;
        }
        return proxyConnectionTimeout;
    }

    /**
     * Sets the timeout for connecting to the SOCKS5 proxy selected by the target. Default is
     * 10000ms.
     * 
     * @param proxyConnectionTimeout the timeout to set
     */
    public void setProxyConnectionTimeout(int proxyConnectionTimeout) {
        this.proxyConnectionTimeout = proxyConnectionTimeout;
    }

    /**
     * Returns if the prioritization of the last working SOCKS5 proxy on successive SOCKS5
     * Bytestream connections is enabled. Default is <code>true</code>.
     * 
     * @return <code>true</code> if prioritization is enabled, <code>false</code> otherwise
     */
    public boolean isProxyPrioritizationEnabled() {
        return proxyPrioritizationEnabled;
    }

    /**
     * Enable/disable the prioritization of the last working SOCKS5 proxy on successive SOCKS5
     * Bytestream connections.
     * 
     * @param proxyPrioritizationEnabled enable/disable the prioritization of the last working
     *        SOCKS5 proxy
     */
    public void setProxyPrioritizationEnabled(boolean proxyPrioritizationEnabled) {
        this.proxyPrioritizationEnabled = proxyPrioritizationEnabled;
    }

    /**
     * Establishes a SOCKS5 Bytestream with the given user and returns the Socket to send/receive
     * data to/from the user.
     * <p>
     * Use this method to establish SOCKS5 Bytestreams to users accepting all incoming Socks5
     * bytestream requests since this method doesn't provide a way to tell the user something about
     * the data to be sent.
     * <p>
     * To establish a SOCKS5 Bytestream after negotiation the kind of data to be sent (e.g. file
     * transfer) use {@link #establishSession(String, String)}.
     * 
     * @param targetJID the JID of the user a SOCKS5 Bytestream should be established
     * @return the Socket to send/receive data to/from the user
     * @throws XMPPException if the user doesn't support or accept SOCKS5 Bytestreams, if no Socks5
     *         Proxy could be found, if the user couldn't connect to any of the SOCKS5 Proxies
     * @throws IOException if the bytestream could not be established
     * @throws InterruptedException if the current thread was interrupted while waiting
     */
    public Socks5BytestreamSession establishSession(String targetJID) throws XMPPException,
                    IOException, InterruptedException {
        String sessionID = getNextSessionID();
        return establishSession(targetJID, sessionID);
    }

    /**
     * Establishes a SOCKS5 Bytestream with the given user using the given session ID and returns
     * the Socket to send/receive data to/from the user.
     * 
     * @param targetJID the JID of the user a SOCKS5 Bytestream should be established
     * @param sessionID the session ID for the SOCKS5 Bytestream request
     * @return the Socket to send/receive data to/from the user
     * @throws XMPPException if the user doesn't support or accept SOCKS5 Bytestreams, if no Socks5
     *         Proxy could be found, if the user couldn't connect to any of the SOCKS5 Proxies
     * @throws IOException if the bytestream could not be established
     * @throws InterruptedException if the current thread was interrupted while waiting
     */
    public Socks5BytestreamSession establishSession(String targetJID, String sessionID)
                    throws XMPPException, IOException, InterruptedException {

        XMPPException discoveryException = null;
        // check if target supports SOCKS5 Bytestream
        if (!supportsSocks5(targetJID)) {
            throw new XMPPException(targetJID + " doesn't support SOCKS5 Bytestream");
        }

        List<String> proxies = new ArrayList<String>();
        // determine SOCKS5 proxies from XMPP-server
        try {
            determineProxies();
        } catch (XMPPException e) {
            // don't abort here, just remember the exception thrown by determineProxies()
            // determineStreamHostInfos() will at least add the local Socks5 proxy (if enabled)
            discoveryException = e;
        }

        // determine address and port of each proxy
        List<StreamHost> streamHosts = determineStreamHostInfos(proxies);

        if (streamHosts.isEmpty()) {
            throw discoveryException != null ? discoveryException : new XMPPException("no SOCKS5 proxies available");
        }

        // compute digest
        String digest = Socks5Utils.createDigest(sessionID, this.connection.getUser(), targetJID);

        // prioritize last working SOCKS5 proxy if exists
        if (this.proxyPrioritizationEnabled && this.lastWorkingProxy != null) {
            StreamHost selectedStreamHost = null;
            for (StreamHost streamHost : streamHosts) {
                if (streamHost.getJID().equals(this.lastWorkingProxy)) {
                    selectedStreamHost = streamHost;
                    break;
                }
            }
            if (selectedStreamHost != null) {
                streamHosts.remove(selectedStreamHost);
                streamHosts.add(0, selectedStreamHost);
            }

        }

        Socks5Proxy socks5Proxy = Socks5Proxy.getSocks5Proxy();
        try {

            // add transfer digest to local proxy to make transfer valid
            socks5Proxy.addTransfer(digest);

            // create initiation packet
            Bytestream initiation = createBytestreamInitiation(sessionID, targetJID, streamHosts);

            // send initiation packet
            Packet response = SyncPacketSend.getReply(this.connection, initiation,
                            getTargetResponseTimeout());

            // extract used stream host from response
            StreamHostUsed streamHostUsed = ((Bytestream) response).getUsedHost();
            StreamHost usedStreamHost = initiation.getStreamHost(streamHostUsed.getJID());

            if (usedStreamHost == null) {
                throw new XMPPException("Remote user responded with unknown host");
            }

            // build SOCKS5 client
            Socks5Client socks5Client = new Socks5ClientForInitiator(usedStreamHost, digest,
                            this.connection, sessionID, targetJID);

            // establish connection to proxy
            Socket socket = socks5Client.getSocket(getProxyConnectionTimeout());

            // remember last working SOCKS5 proxy to prioritize it for next request
            this.lastWorkingProxy = usedStreamHost.getJID();

            // negotiation successful, return the output stream
            return new Socks5BytestreamSession(socket, usedStreamHost.getJID().equals(
                            this.connection.getUser()));

        }
        catch (TimeoutException e) {
            throw new IOException("Timeout while connecting to SOCKS5 proxy");
        }
        finally {

            // remove transfer digest if output stream is returned or an exception
            // occurred
            socks5Proxy.removeTransfer(digest);

        }
    }

    /**
     * Returns <code>true</code> if the given target JID supports feature SOCKS5 Bytestream.
     * 
     * @param targetJID the target JID
     * @return <code>true</code> if the given target JID supports feature SOCKS5 Bytestream
     *         otherwise <code>false</code>
     * @throws XMPPException if there was an error querying target for supported features
     */
    private boolean supportsSocks5(String targetJID) throws XMPPException {
        ServiceDiscoveryManager serviceDiscoveryManager = ServiceDiscoveryManager.getInstanceFor(this.connection);
        DiscoverInfo discoverInfo = serviceDiscoveryManager.discoverInfo(targetJID);
        return discoverInfo.containsFeature(NAMESPACE);
    }

    /**
     * Returns a list of JIDs of SOCKS5 proxies by querying the XMPP server. The SOCKS5 proxies are
     * in the same order as returned by the XMPP server.
     * 
     * @return list of JIDs of SOCKS5 proxies
     * @throws XMPPException if there was an error querying the XMPP server for SOCKS5 proxies
     */
    private List<String> determineProxies() throws XMPPException {
        ServiceDiscoveryManager serviceDiscoveryManager = ServiceDiscoveryManager.getInstanceFor(this.connection);

        List<String> proxies = new ArrayList<String>();

        // get all items form XMPP server
        DiscoverItems discoverItems = serviceDiscoveryManager.discoverItems(this.connection.getServiceName());
        Iterator<Item> itemIterator = discoverItems.getItems();

        // query all items if they are SOCKS5 proxies
        while (itemIterator.hasNext()) {
            Item item = itemIterator.next();

            // skip blacklisted servers
            if (this.proxyBlacklist.contains(item.getEntityID())) {
                continue;
            }

            try {
                DiscoverInfo proxyInfo;
                proxyInfo = serviceDiscoveryManager.discoverInfo(item.getEntityID());
                Iterator<Identity> identities = proxyInfo.getIdentities();

                // item must have category "proxy" and type "bytestream"
                while (identities.hasNext()) {
                    Identity identity = identities.next();

                    if ("proxy".equalsIgnoreCase(identity.getCategory())
                                    && "bytestreams".equalsIgnoreCase(identity.getType())) {
                        proxies.add(item.getEntityID());
                        break;
                    }

                    /*
                     * server is not a SOCKS5 proxy, blacklist server to skip next time a Socks5
                     * bytestream should be established
                     */
                    this.proxyBlacklist.add(item.getEntityID());

                }
            }
            catch (XMPPException e) {
                // blacklist errornous server
                this.proxyBlacklist.add(item.getEntityID());
            }
        }

        return proxies;
    }

    /**
     * Returns a list of stream hosts containing the IP address an the port for the given list of
     * SOCKS5 proxy JIDs. The order of the returned list is the same as the given list of JIDs
     * excluding all SOCKS5 proxies who's network settings could not be determined. If a local
     * SOCKS5 proxy is running it will be the first item in the list returned.
     * 
     * @param proxies a list of SOCKS5 proxy JIDs
     * @return a list of stream hosts containing the IP address an the port
     */
    private List<StreamHost> determineStreamHostInfos(List<String> proxies) {
        List<StreamHost> streamHosts = new ArrayList<StreamHost>();

        // add local proxy on first position if exists
        List<StreamHost> localProxies = getLocalStreamHost();
        if (localProxies != null) {
            streamHosts.addAll(localProxies);
        }

        // query SOCKS5 proxies for network settings
        for (String proxy : proxies) {
            Bytestream streamHostRequest = createStreamHostRequest(proxy);
            try {
                Bytestream response = (Bytestream) SyncPacketSend.getReply(this.connection,
                                streamHostRequest);
                streamHosts.addAll(response.getStreamHosts());
            }
            catch (XMPPException e) {
                // blacklist errornous proxies
                this.proxyBlacklist.add(proxy);
            }
        }

        return streamHosts;
    }

    /**
     * Returns a IQ packet to query a SOCKS5 proxy its network settings.
     * 
     * @param proxy the proxy to query
     * @return IQ packet to query a SOCKS5 proxy its network settings
     */
    private Bytestream createStreamHostRequest(String proxy) {
        Bytestream request = new Bytestream();
        request.setType(IQ.Type.GET);
        request.setTo(proxy);
        return request;
    }

    /**
     * Returns the stream host information of the local SOCKS5 proxy containing the IP address and
     * the port or null if local SOCKS5 proxy is not running.
     * 
     * @return the stream host information of the local SOCKS5 proxy or null if local SOCKS5 proxy
     *         is not running
     */
    private List<StreamHost> getLocalStreamHost() {

        // get local proxy singleton
        Socks5Proxy socks5Server = Socks5Proxy.getSocks5Proxy();

        if (socks5Server.isRunning()) {
            List<String> addresses = socks5Server.getLocalAddresses();
            int port = socks5Server.getPort();

            if (addresses.size() >= 1) {
                List<StreamHost> streamHosts = new ArrayList<StreamHost>();
                for (String address : addresses) {
                    StreamHost streamHost = new StreamHost(this.connection.getUser(), address);
                    streamHost.setPort(port);
                    streamHosts.add(streamHost);
                }
                return streamHosts;
            }

        }

        // server is not running or local address could not be determined
        return null;
    }

    /**
     * Returns a SOCKS5 Bytestream initialization request packet with the given session ID
     * containing the given stream hosts for the given target JID.
     * 
     * @param sessionID the session ID for the SOCKS5 Bytestream
     * @param targetJID the target JID of SOCKS5 Bytestream request
     * @param streamHosts a list of SOCKS5 proxies the target should connect to
     * @return a SOCKS5 Bytestream initialization request packet
     */
    private Bytestream createBytestreamInitiation(String sessionID, String targetJID,
                    List<StreamHost> streamHosts) {
        Bytestream initiation = new Bytestream(sessionID);

        // add all stream hosts
        for (StreamHost streamHost : streamHosts) {
            initiation.addStreamHost(streamHost);
        }

        initiation.setType(IQ.Type.SET);
        initiation.setTo(targetJID);

        return initiation;
    }

    /**
     * Responses to the given packet's sender with a XMPP error that a SOCKS5 Bytestream is not
     * accepted.
     * 
     * @param packet Packet that should be answered with a not-acceptable error
     */
    protected void replyRejectPacket(IQ packet) {
        XMPPError xmppError = new XMPPError(XMPPError.Condition.no_acceptable);
        IQ errorIQ = IQ.createErrorResponse(packet, xmppError);
        this.connection.sendPacket(errorIQ);
    }

    /**
     * Activates the Socks5BytestreamManager by registering the SOCKS5 Bytestream initialization
     * listener and enabling the SOCKS5 Bytestream feature.
     */
    private void activate() {
        // register bytestream initiation packet listener
        this.connection.addPacketListener(this.initiationListener,
                        this.initiationListener.getFilter());

        // enable SOCKS5 feature
        enableService();
    }

    /**
     * Adds the SOCKS5 Bytestream feature to the service discovery.
     */
    private void enableService() {
        ServiceDiscoveryManager manager = ServiceDiscoveryManager.getInstanceFor(this.connection);
        if (!manager.includesFeature(NAMESPACE)) {
            manager.addFeature(NAMESPACE);
        }
    }

    /**
     * Returns a new unique session ID.
     * 
     * @return a new unique session ID
     */
    private String getNextSessionID() {
        StringBuilder buffer = new StringBuilder();
        buffer.append(SESSION_ID_PREFIX);
        buffer.append(Math.abs(randomGenerator.nextLong()));
        return buffer.toString();
    }

    /**
     * Returns the XMPP connection.
     * 
     * @return the XMPP connection
     */
    protected Connection getConnection() {
        return this.connection;
    }

    /**
     * Returns the {@link BytestreamListener} that should be informed if a SOCKS5 Bytestream request
     * from the given initiator JID is received.
     * 
     * @param initiator the initiator's JID
     * @return the listener
     */
    protected BytestreamListener getUserListener(String initiator) {
        return this.userListeners.get(initiator);
    }

    /**
     * Returns a list of {@link BytestreamListener} that are informed if there are no listeners for
     * a specific initiator.
     * 
     * @return list of listeners
     */
    protected List<BytestreamListener> getAllRequestListeners() {
        return this.allRequestListeners;
    }

    /**
     * Returns the list of session IDs that should be ignored by the InitialtionListener
     * 
     * @return list of session IDs
     */
    protected List<String> getIgnoredBytestreamRequests() {
        return ignoredBytestreamRequests;
    }

}
