/**
 * $RCSfile: STUNResolver.java,v $
 * $Revision: 1.1 $
 * $Date: 15/11/2006
 *
 * Copyright 2003-2006 Jive Software.
 *
 * All rights reserved. Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jivesoftware.smackx.jingle.nat;

import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.jingle.JingleSession;
import org.jivesoftware.smackx.jingle.SmackLogger;
import org.xmlpull.mxp1.MXParser;
import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import de.javawi.jstun.test.BindingLifetimeTest;
import de.javawi.jstun.test.DiscoveryInfo;
import de.javawi.jstun.test.DiscoveryTest;

/**
 * Transport resolver using the JSTUN library, to discover public IP and use it as a candidate.
 *
 * The goal of this resolver is to take possible to establish and manage out-of-band connections between two XMPP entities, even if they are behind Network Address Translators (NATs) or firewalls.
 *
 * @author Thiago Camargo
 */
public class STUNResolver extends TransportResolver {

	private static final SmackLogger LOGGER = SmackLogger.getLogger(STUNResolver.class);

	// The filename where the STUN servers are stored.
    public final static String STUNSERVERS_FILENAME = "META-INF/stun-config.xml";

    // Current STUN server we are using
    protected STUNService currentServer;

    protected Thread resolverThread;

    protected int defaultPort;

    protected String resolvedPublicIP;
    protected String resolvedLocalIP;

    /**
     * Constructor with default STUN server.
     */
    public STUNResolver() {
        super();

        this.defaultPort = 0;
        this.currentServer = new STUNService();
    }

    /**
     * Constructor with a default port.
     *
     * @param defaultPort Port to use by default.
     */
    public STUNResolver(int defaultPort) {
        this();

        this.defaultPort = defaultPort;
    }

    /**
     * Return true if the service is working.
     *
     * @see TransportResolver#isResolving()
     */
    public boolean isResolving() {
        return super.isResolving() && resolverThread != null;
    }

    /**
     * Set the STUN server name and port
     *
     * @param ip   the STUN server name
     * @param port the STUN server port
     */
    public void setSTUNService(String ip, int port) {
        currentServer = new STUNService(ip, port);
    }

    /**
     * Get the name of the current STUN server.
     *
     * @return the name of the STUN server
     */
    public String getCurrentServerName() {
        if (!currentServer.isNull()) {
            return currentServer.getHostname();
        } else {
            return null;
        }
    }

    /**
     * Get the port of the current STUN server.
     *
     * @return the port of the STUN server
     */
    public int getCurrentServerPort() {
        if (!currentServer.isNull()) {
            return currentServer.getPort();
        } else {
            return 0;
        }
    }

    /**
     * Load the STUN configuration from a stream.
     *
     * @param stunConfigStream An InputStream with the configuration file.
     * @return A list of loaded servers
     */
    public ArrayList<STUNService> loadSTUNServers(java.io.InputStream stunConfigStream) {
        ArrayList<STUNService> serversList = new ArrayList<STUNService>();
        String serverName;
        int serverPort;

        try {
            XmlPullParser parser = new MXParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, true);
            parser.setInput(stunConfigStream, "UTF-8");

            int eventType = parser.getEventType();
            do {
                if (eventType == XmlPullParser.START_TAG) {

                    // Parse a STUN server definition
                    if (parser.getName().equals("stunServer")) {

                        serverName = null;
                        serverPort = -1;

                        // Parse the hostname
                        parser.next();
                        parser.next();
                        serverName = parser.nextText();

                        // Parse the port
                        parser.next();
                        parser.next();
                        try {
                            serverPort = Integer.parseInt(parser.nextText());
                        }
                        catch (Exception e) {
                        }

                        // If we have a valid hostname and port, add
                        // it to the list.
                        if (serverName != null && serverPort != -1) {
                            STUNService service = new STUNService(serverName, serverPort);

                            serversList.add(service);
                        }
                    }
                }
                eventType = parser.next();

            }
            while (eventType != XmlPullParser.END_DOCUMENT);

        }
        catch (XmlPullParserException e) {
            LOGGER.error(e.getMessage(), e);
        }
        catch (IOException e) {
            LOGGER.error(e.getMessage(), e);
        }

        currentServer = bestSTUNServer(serversList);

        return serversList;
    }

    /**
     * Load a list of services: STUN servers and ports. Some public STUN servers
     * are:
     * <p/>
     * <pre>
     *               iphone-stun.freenet.de:3478
     *               larry.gloo.net:3478
     *               stun.xten.net:3478
     *               stun.fwdnet.net
     *               stun.fwd.org (no DNS SRV record)
     *               stun01.sipphone.com (no DNS SRV record)
     *               stun.softjoys.com (no DNS SRV record)
     *               stun.voipbuster.com (no DNS SRV record)
     *               stun.voxgratia.org (no DNS SRV record)
     *               stun.noc.ams-ix.net
     * </pre>
     * <p/>
     * This list should be contained in a file in the "META-INF" directory
     *
     * @return a list of services
     */
    public ArrayList<STUNService> loadSTUNServers() {
        ArrayList<STUNService> serversList = new ArrayList<STUNService>();

        // Load the STUN configuration
        try {
            // Get an array of class loaders to try loading the config from.
            ClassLoader[] classLoaders = new ClassLoader[2];
            classLoaders[0] = new STUNResolver() {
            }.getClass().getClassLoader();
            classLoaders[1] = Thread.currentThread().getContextClassLoader();

            for (int i = 0; i < classLoaders.length; i++) {
                Enumeration<URL> stunConfigEnum = classLoaders[i]
                        .getResources(STUNSERVERS_FILENAME);

                while (stunConfigEnum.hasMoreElements() && serversList.isEmpty()) {
                    URL url = stunConfigEnum.nextElement();
                    java.io.InputStream stunConfigStream = null;

                    stunConfigStream = url.openStream();
                    serversList.addAll(loadSTUNServers(stunConfigStream));
                    stunConfigStream.close();
                }
            }
        }
        catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
        }

        return serversList;
    }

    /**
     * Get the best usable STUN server from a list.
     *
     * @return the best STUN server that can be used.
     */
    private STUNService bestSTUNServer(ArrayList<STUNService> listServers) {
        if (listServers.isEmpty()) {
            return null;
        } else {
            // TODO: this should use some more advanced criteria...
            return listServers.get(0);
        }
    }

    /**
     * Resolve the IP and obtain a valid transport method.
     */
    public synchronized void resolve(JingleSession session) throws XMPPException {

        setResolveInit();

        clearCandidates();

        TransportCandidate candidate = new TransportCandidate.Fixed(
                resolvedPublicIP, getFreePort());
        candidate.setLocalIp(resolvedLocalIP);

        LOGGER.debug("RESOLVING : " + resolvedPublicIP + ":" + candidate.getPort());

        addCandidate(candidate);

        setResolveEnd();

    }

    /**
     * Initialize the resolver.
     *
     * @throws XMPPException
     */
    public void initialize() throws XMPPException {
        LOGGER.debug("Initialized");
        if (!isResolving()&&!isResolved()) {
            // Get the best STUN server available
            if (currentServer.isNull()) {
                loadSTUNServers();
            }
            // We should have a valid STUN server by now...
            if (!currentServer.isNull()) {

                clearCandidates();

                resolverThread = new Thread(new Runnable() {
                    public void run() {
                        // Iterate through the list of interfaces, and ask
                        // to the STUN server for our address.
                        try {
                            Enumeration<NetworkInterface> ifaces = NetworkInterface.getNetworkInterfaces();
                            String candAddress;
                            int candPort;

                            while (ifaces.hasMoreElements()) {

                                NetworkInterface iface =  ifaces.nextElement();
                                Enumeration<InetAddress> iaddresses = iface.getInetAddresses();

                                while (iaddresses.hasMoreElements()) {
                                    InetAddress iaddress = iaddresses.nextElement();
                                    if (!iaddress.isLoopbackAddress()
                                            && !iaddress.isLinkLocalAddress()) {

                                        // Reset the candidate
                                        candAddress = null;
                                        candPort = -1;

                                        DiscoveryTest test = new DiscoveryTest(iaddress,
                                                currentServer.getHostname(),
                                                currentServer.getPort());
                                        try {
                                            // Run the tests and get the
                                            // discovery
                                            // information, where all the
                                            // info is stored...
                                            DiscoveryInfo di = test.test();

                                            candAddress = di.getPublicIP() != null ?
                                                    di.getPublicIP().getHostAddress() : null;

                                            // Get a valid port
                                            if (defaultPort == 0) {
                                                candPort = getFreePort();
                                            } else {
                                                candPort = defaultPort;
                                            }

                                            // If we have a valid candidate,
                                            // add it to the list.
                                            if (candAddress != null && candPort >= 0) {
                                                TransportCandidate candidate = new TransportCandidate.Fixed(
                                                        candAddress, candPort);
                                                candidate.setLocalIp(iaddress.getHostAddress() != null ? iaddress.getHostAddress() : iaddress.getHostName());
                                                addCandidate(candidate);

                                                resolvedPublicIP = candidate.getIp();
                                                resolvedLocalIP = candidate.getLocalIp();
                                                return;
                                            }
                                        }
                                        catch (Exception e) {
                                            LOGGER.error(e.getMessage(), e);
                                        }
                                    }
                                }
                            }
                        }
                        catch (SocketException e) {
                            LOGGER.error(e.getMessage(), e);
                        }
                        finally {
                            setInitialized();
                        }
                    }
                }, "Waiting for all the transport candidates checks...");

                resolverThread.setName("STUN resolver");
                resolverThread.start();
            } else {
                throw new IllegalStateException("No valid STUN server found.");
            }
        }
    }

    /**
     * Cancel any operation.
     *
     * @see TransportResolver#cancel()
     */
    public synchronized void cancel() throws XMPPException {
        if (isResolving()) {
            resolverThread.interrupt();
            setResolveEnd();
        }
    }

    /**
     * Clear the list of candidates and start the resolution again.
     *
     * @see TransportResolver#clear()
     */
    public synchronized void clear() throws XMPPException {
        this.defaultPort = 0;
        super.clear();
    }

    /**
     * STUN service definition.
     */
    protected class STUNService {

        private String hostname; // The hostname of the service

        private int port; // The port number

        /**
         * Basic constructor, with the hostname and port
         *
         * @param hostname The hostname
         * @param port     The port
         */
        public STUNService(String hostname, int port) {
            super();

            this.hostname = hostname;
            this.port = port;
        }

        /**
         * Default constructor, without name and port.
         */
        public STUNService() {
            this(null, -1);
        }

        /**
         * Get the host name of the STUN service.
         *
         * @return The host name
         */
        public String getHostname() {
            return hostname;
        }

        /**
         * Set the hostname of the STUN service.
         *
         * @param hostname The host name of the service.
         */
        public void setHostname(String hostname) {
            this.hostname = hostname;
        }

        /**
         * Get the port of the STUN service
         *
         * @return The port number where the STUN server is waiting.
         */
        public int getPort() {
            return port;
        }

        /**
         * Set the port number for the STUN service.
         *
         * @param port The port number.
         */
        public void setPort(int port) {
            this.port = port;
        }

        /**
         * Basic format test: the service is not null.
         *
         * @return true if the hostname and port are null
         */
        public boolean isNull() {
            if (hostname == null) {
                return true;
            } else if (hostname.length() == 0) {
                return true;
            } else if (port < 0) {
                return true;
            } else {
                return false;
            }
        }

        /**
         * Check a binding with the STUN currentServer.
         * <p/>
         * Note: this function blocks for some time, waiting for a response.
         *
         * @return true if the currentServer is usable.
         */
        public boolean checkBinding() {
            boolean result = false;

            try {
                BindingLifetimeTest binding = new BindingLifetimeTest(hostname, port);

                binding.test();

                while (true) {
                    Thread.sleep(5000);
                    if (binding.getLifetime() != -1) {
                        if (binding.isCompleted()) {
                            return true;
                        }
                    } else {
                        break;
                    }
                }
            }
            catch (Exception e) {
                LOGGER.error(e.getMessage(), e);
            }

            return result;
        }
    }
}
