/**
 *
 * Copyright the original author or authors
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
package org.jivesoftware.smackx.bytestreams.socks5;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.SmackException;

/**
 * The Socks5Proxy class represents a local SOCKS5 proxy server. It can be enabled/disabled by
 * invoking {@link #setLocalSocks5ProxyEnabled(boolean)}. The proxy is enabled by default.
 * <p>
 * The port of the local SOCKS5 proxy can be configured by invoking
 * {@link #setLocalSocks5ProxyPort(int)}. Default port is 7777. If you set the port to a negative
 * value Smack tries to the absolute value and all following until it finds an open port.
 * <p>
 * If your application is running on a machine with multiple network interfaces or if you want to
 * provide your public address in case you are behind a NAT router, invoke
 * {@link #addLocalAddress(String)} or {@link #replaceLocalAddresses(Collection)} to modify the list of
 * local network addresses used for outgoing SOCKS5 Bytestream requests.
 * <p>
 * The local SOCKS5 proxy server refuses all connections except the ones that are explicitly allowed
 * in the process of establishing a SOCKS5 Bytestream (
 * {@link Socks5BytestreamManager#establishSession(org.jxmpp.jid.Jid)}).
 * <p>
 * This Implementation has the following limitations:
 * <ul>
 * <li>only supports the no-authentication authentication method</li>
 * <li>only supports the <code>connect</code> command and will not answer correctly to other
 * commands</li>
 * <li>only supports requests with the domain address type and will not correctly answer to requests
 * with other address types</li>
 * </ul>
 * (see <a href="http://tools.ietf.org/html/rfc1928">RFC 1928</a>)
 * 
 * @author Henning Staib
 */
public class Socks5Proxy {
    private static final Logger LOGGER = Logger.getLogger(Socks5Proxy.class.getName());

    /* SOCKS5 proxy singleton */
    private static Socks5Proxy socks5Server;

    private static boolean localSocks5ProxyEnabled = true;

    /**
     * The port of the local Socks5 Proxy. If this value is negative, the next ports will be tried
     * until a unused is found.
     */
    private static int localSocks5ProxyPort = -7777;

    /* reusable implementation of a SOCKS5 proxy server process */
    private Socks5ServerProcess serverProcess;

    /* thread running the SOCKS5 server process */
    private Thread serverThread;

    /* server socket to accept SOCKS5 connections */
    private ServerSocket serverSocket;

    /* assigns a connection to a digest */
    private final Map<String, Socket> connectionMap = new ConcurrentHashMap<String, Socket>();

    /* list of digests connections should be stored */
    private final List<String> allowedConnections = Collections.synchronizedList(new LinkedList<String>());

    private final Set<String> localAddresses = new LinkedHashSet<String>(4);

    /**
     * Private constructor.
     */
    private Socks5Proxy() {
        this.serverProcess = new Socks5ServerProcess();

        Enumeration<NetworkInterface> networkInterfaces;
        try {
            networkInterfaces = NetworkInterface.getNetworkInterfaces();
        } catch (SocketException e) {
            throw new IllegalStateException(e);
        }
        Set<String> localHostAddresses = new HashSet<String>();
        for (NetworkInterface networkInterface : Collections.list(networkInterfaces)) {
            // We can't use NetworkInterface.getInterfaceAddresses here, which
            // would return a List instead the deprecated Enumeration, because
            // it's Android API 9 and Smack currently uses 8. Change that when
            // we raise Smack's minimum Android API.
            Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
            for (InetAddress address : Collections.list(inetAddresses)) {
                localHostAddresses.add(address.getHostAddress());
            }
        }
        if (localHostAddresses.isEmpty()) {
            throw new IllegalStateException("Could not determine any local host address");
        }
        replaceLocalAddresses(localHostAddresses);
    }

   /**
    * Returns true if the local Socks5 proxy should be started. Default is true.
    * 
    * @return if the local Socks5 proxy should be started
    */
   public static boolean isLocalSocks5ProxyEnabled() {
       return localSocks5ProxyEnabled;
   }

   /**
    * Sets if the local Socks5 proxy should be started. Default is true.
    * 
    * @param localSocks5ProxyEnabled if the local Socks5 proxy should be started
    */
   public static void setLocalSocks5ProxyEnabled(boolean localSocks5ProxyEnabled) {
       Socks5Proxy.localSocks5ProxyEnabled = localSocks5ProxyEnabled;
   }

   /**
    * Return the port of the local Socks5 proxy. Default is 7777.
    * 
    * @return the port of the local Socks5 proxy
    */
   public static int getLocalSocks5ProxyPort() {
       return localSocks5ProxyPort;
   }

   /**
    * Sets the port of the local Socks5 proxy. Default is 7777. If you set the port to a negative
    * value Smack tries the absolute value and all following until it finds an open port.
    * 
    * @param localSocks5ProxyPort the port of the local Socks5 proxy to set
    */
   public static void setLocalSocks5ProxyPort(int localSocks5ProxyPort) {
       if (Math.abs(localSocks5ProxyPort) > 65535) {
           throw new IllegalArgumentException("localSocks5ProxyPort must be within (-65535,65535)");
       }
       Socks5Proxy.localSocks5ProxyPort = localSocks5ProxyPort;
   }

    /**
     * Returns the local SOCKS5 proxy server.
     * 
     * @return the local SOCKS5 proxy server
     */
    public static synchronized Socks5Proxy getSocks5Proxy() {
        if (socks5Server == null) {
            socks5Server = new Socks5Proxy();
        }
        if (isLocalSocks5ProxyEnabled()) {
            socks5Server.start();
        }
        return socks5Server;
    }

    /**
     * Starts the local SOCKS5 proxy server. If it is already running, this method does nothing.
     */
    public synchronized void start() {
        if (isRunning()) {
            return;
        }
        try {
            if (getLocalSocks5ProxyPort() < 0) {
                int port = Math.abs(getLocalSocks5ProxyPort());
                for (int i = 0; i < 65535 - port; i++) {
                    try {
                        this.serverSocket = new ServerSocket(port + i);
                        break;
                    }
                    catch (IOException e) {
                        // port is used, try next one
                    }
                }
            }
            else {
                this.serverSocket = new ServerSocket(getLocalSocks5ProxyPort());
            }

            if (this.serverSocket != null) {
                this.serverThread = new Thread(this.serverProcess);
                this.serverThread.start();
            }
        }
        catch (IOException e) {
            // couldn't setup server
            LOGGER.log(Level.SEVERE, "couldn't setup local SOCKS5 proxy on port " + getLocalSocks5ProxyPort(), e);
        }
    }

    /**
     * Stops the local SOCKS5 proxy server. If it is not running this method does nothing.
     */
    public synchronized void stop() {
        if (!isRunning()) {
            return;
        }

        try {
            this.serverSocket.close();
        }
        catch (IOException e) {
            // do nothing
        }

        if (this.serverThread != null && this.serverThread.isAlive()) {
            try {
                this.serverThread.interrupt();
                this.serverThread.join();
            }
            catch (InterruptedException e) {
                // do nothing
            }
        }
        this.serverThread = null;
        this.serverSocket = null;

    }

    /**
     * Adds the given address to the list of local network addresses.
     * <p>
     * Use this method if you want to provide multiple addresses in a SOCKS5 Bytestream request.
     * This may be necessary if your application is running on a machine with multiple network
     * interfaces or if you want to provide your public address in case you are behind a NAT router.
     * <p>
     * The order of the addresses used is determined by the order you add addresses.
     * <p>
     * Note that the list of addresses initially contains the address returned by
     * <code>InetAddress.getLocalHost().getHostAddress()</code>. You can replace the list of
     * addresses by invoking {@link #replaceLocalAddresses(Collection)}.
     * 
     * @param address the local network address to add
     */
    public void addLocalAddress(String address) {
        if (address == null) {
            return;
        }
        synchronized (localAddresses) {
            this.localAddresses.add(address);
        }
    }

    /**
     * Removes the given address from the list of local network addresses. This address will then no
     * longer be used of outgoing SOCKS5 Bytestream requests.
     * 
     * @param address the local network address to remove
     * @return true if the address was removed.
     */
    public boolean removeLocalAddress(String address) {
        synchronized(localAddresses) {
            return localAddresses.remove(address);
        }
    }

    /**
     * Returns an set of the local network addresses that will be used for streamhost
     * candidates of outgoing SOCKS5 Bytestream requests.
     * 
     * @return set of the local network addresses
     */
    public List<String> getLocalAddresses() {
        synchronized (localAddresses) {
            return new LinkedList<String>(localAddresses);
        }
    }

    /**
     * Replaces the list of local network addresses.
     * <p>
     * Use this method if you want to provide multiple addresses in a SOCKS5 Bytestream request and
     * want to define their order. This may be necessary if your application is running on a machine
     * with multiple network interfaces or if you want to provide your public address in case you
     * are behind a NAT router.
     * 
     * @param addresses the new list of local network addresses
     */
    public void replaceLocalAddresses(Collection<String> addresses) {
        if (addresses == null) {
            throw new IllegalArgumentException("list must not be null");
        }
        synchronized(localAddresses) {
            localAddresses.clear();
            localAddresses.addAll(addresses);
        }
    }

    /**
     * Returns the port of the local SOCKS5 proxy server. If it is not running -1 will be returned.
     * 
     * @return the port of the local SOCKS5 proxy server or -1 if proxy is not running
     */
    public int getPort() {
        if (!isRunning()) {
            return -1;
        }
        return this.serverSocket.getLocalPort();
    }

    /**
     * Returns the socket for the given digest. A socket will be returned if the given digest has
     * been in the list of allowed transfers (see {@link #addTransfer(String)}) while the peer
     * connected to the SOCKS5 proxy.
     * 
     * @param digest identifying the connection
     * @return socket or null if there is no socket for the given digest
     */
    protected Socket getSocket(String digest) {
        return this.connectionMap.get(digest);
    }

    /**
     * Add the given digest to the list of allowed transfers. Only connections for allowed transfers
     * are stored and can be retrieved by invoking {@link #getSocket(String)}. All connections to
     * the local SOCKS5 proxy that don't contain an allowed digest are discarded.
     * 
     * @param digest to be added to the list of allowed transfers
     */
    protected void addTransfer(String digest) {
        this.allowedConnections.add(digest);
    }

    /**
     * Removes the given digest from the list of allowed transfers. After invoking this method
     * already stored connections with the given digest will be removed.
     * <p>
     * The digest should be removed after establishing the SOCKS5 Bytestream is finished, an error
     * occurred while establishing the connection or if the connection is not allowed anymore.
     * 
     * @param digest to be removed from the list of allowed transfers
     */
    protected void removeTransfer(String digest) {
        this.allowedConnections.remove(digest);
        this.connectionMap.remove(digest);
    }

    /**
     * Returns <code>true</code> if the local SOCKS5 proxy server is running, otherwise
     * <code>false</code>.
     * 
     * @return <code>true</code> if the local SOCKS5 proxy server is running, otherwise
     *         <code>false</code>
     */
    public boolean isRunning() {
        return this.serverSocket != null;
    }

    /**
     * Implementation of a simplified SOCKS5 proxy server.
     */
    private class Socks5ServerProcess implements Runnable {

        public void run() {
            while (true) {
                Socket socket = null;

                try {

                    if (Socks5Proxy.this.serverSocket.isClosed()
                                    || Thread.currentThread().isInterrupted()) {
                        return;
                    }

                    // accept connection
                    socket = Socks5Proxy.this.serverSocket.accept();

                    // initialize connection
                    establishConnection(socket);

                }
                catch (SocketException e) {
                    /*
                     * do nothing, if caused by closing the server socket, thread will terminate in
                     * next loop
                     */
                }
                catch (Exception e) {
                    try {
                        if (socket != null) {
                            socket.close();
                        }
                    }
                    catch (IOException e1) {
                        /* do nothing */
                    }
                }
            }

        }

        /**
         * Negotiates a SOCKS5 connection and stores it on success.
         * 
         * @param socket connection to the client
         * @throws SmackException if client requests a connection in an unsupported way
         * @throws IOException if a network error occurred
         */
        private void establishConnection(Socket socket) throws SmackException, IOException {
            DataOutputStream out = new DataOutputStream(socket.getOutputStream());
            DataInputStream in = new DataInputStream(socket.getInputStream());

            // first byte is version should be 5
            int b = in.read();
            if (b != 5) {
                throw new SmackException("Only SOCKS5 supported");
            }

            // second byte number of authentication methods supported
            b = in.read();

            // read list of supported authentication methods
            byte[] auth = new byte[b];
            in.readFully(auth);

            byte[] authMethodSelectionResponse = new byte[2];
            authMethodSelectionResponse[0] = (byte) 0x05; // protocol version

            // only authentication method 0, no authentication, supported
            boolean noAuthMethodFound = false;
            for (int i = 0; i < auth.length; i++) {
                if (auth[i] == (byte) 0x00) {
                    noAuthMethodFound = true;
                    break;
                }
            }

            if (!noAuthMethodFound) {
                authMethodSelectionResponse[1] = (byte) 0xFF; // no acceptable methods
                out.write(authMethodSelectionResponse);
                out.flush();
                throw new SmackException("Authentication method not supported");
            }

            authMethodSelectionResponse[1] = (byte) 0x00; // no-authentication method
            out.write(authMethodSelectionResponse);
            out.flush();

            // receive connection request
            byte[] connectionRequest = Socks5Utils.receiveSocks5Message(in);

            // extract digest
            String responseDigest = new String(connectionRequest, 5, connectionRequest[4]);

            // return error if digest is not allowed
            if (!Socks5Proxy.this.allowedConnections.contains(responseDigest)) {
                connectionRequest[1] = (byte) 0x05; // set return status to 5 (connection refused)
                out.write(connectionRequest);
                out.flush();

                throw new SmackException("Connection is not allowed");
            }

            connectionRequest[1] = (byte) 0x00; // set return status to 0 (success)
            out.write(connectionRequest);
            out.flush();

            // store connection
            Socks5Proxy.this.connectionMap.put(responseDigest, socket);
        }

    }

}
