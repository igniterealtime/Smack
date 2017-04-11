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
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.util.StringUtils;

/**
 * Simple SOCKS5 proxy for testing purposes. It is almost the same as the Socks5Proxy class but the
 * port can be configured more easy and it all connections are allowed.
 * 
 * @author Henning Staib
 */
public final class Socks5TestProxy {
    private static final Logger LOGGER = Logger.getLogger(Socks5TestProxy.class.getName());

    /* SOCKS5 proxy singleton */
    private static Socks5TestProxy socks5Server;

    /* reusable implementation of a SOCKS5 proxy server process */
    private Socks5ServerProcess serverProcess;

    /* thread running the SOCKS5 server process */
    private Thread serverThread;

    /* server socket to accept SOCKS5 connections */
    private ServerSocket serverSocket;

    /* assigns a connection to a digest */
    private final Map<String, Socket> connectionMap = new ConcurrentHashMap<String, Socket>();

    /* port of the test proxy */
    private int port = 7777;

    private boolean startupComplete;

    /**
     * Private constructor.
     */
    private Socks5TestProxy(int port) {
        this.serverProcess = new Socks5ServerProcess();
        this.port = port;
    }

    /**
     * Returns the local SOCKS5 proxy server.
     * 
     * @param port of the test proxy
     * @return the local SOCKS5 proxy server
     */
    public static synchronized Socks5TestProxy getProxy(int port) {
        if (socks5Server == null) {
            socks5Server = new Socks5TestProxy(port);
            socks5Server.start();
        }
        return socks5Server;
    }

    /**
     * Stops the test proxy.
     */
    public static synchronized void stopProxy() {
        if (socks5Server != null) {
            socks5Server.stop();
            socks5Server = null;
        }
    }

    /**
     * Starts the local SOCKS5 proxy server. If it is already running, this method does nothing.
     */
    public synchronized void start() {
        if (isRunning()) {
            return;
        }
        try {
            this.serverSocket = new ServerSocket(this.port);
            this.serverThread = new Thread(this.serverProcess);
            this.serverThread.start();
        }
        catch (IOException e) {
            LOGGER.log(Level.SEVERE, "exception", e);
            // do nothing
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
            LOGGER.log(Level.SEVERE, "exception", e);
        }

        if (this.serverThread != null && this.serverThread.isAlive()) {
            try {
                this.serverThread.interrupt();
                this.serverThread.join();
            }
            catch (InterruptedException e) {
                // do nothing
                LOGGER.log(Level.SEVERE, "exception", e);
            }
        }
        this.serverThread = null;
        this.serverSocket = null;

    }

    /**
     * Returns the host address of the local SOCKS5 proxy server.
     * 
     * @return the host address of the local SOCKS5 proxy server
     */
    public static String getAddress() {
        try {
            return InetAddress.getLocalHost().getHostAddress();
        }
        catch (UnknownHostException e) {
            return null;
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
     * Returns the socket for the given digest.
     * 
     * @param digest identifying the connection
     * @return socket or null if there is no socket for the given digest
     */
    @SuppressWarnings("WaitNotInLoop")
    public Socket getSocket(String digest) {
        synchronized(this) {
            if (!startupComplete) {
                try {
                    wait(5000);
                } catch (InterruptedException e) {
                    LOGGER.log(Level.SEVERE, "exception", e);
                }
            }
        }
        if (!startupComplete) {
            throw new IllegalStateException("Startup of Socks5TestProxy failed within 5 seconds");
        }
        return this.connectionMap.get(digest);
    }

    /**
     * Returns true if the local SOCKS5 proxy server is running, otherwise false.
     * 
     * @return true if the local SOCKS5 proxy server is running, otherwise false
     */
    public boolean isRunning() {
        return this.serverSocket != null;
    }

    /**
     * Implementation of a simplified SOCKS5 proxy server.
     * 
     * @author Henning Staib
     */
    class Socks5ServerProcess implements Runnable {

        @Override
        public void run() {
            while (true) {
                Socket socket = null;

                try {

                    if (Socks5TestProxy.this.serverSocket.isClosed()
                                    || Thread.currentThread().isInterrupted()) {
                        return;
                    }

                    // accept connection
                    socket = Socks5TestProxy.this.serverSocket.accept();

                    // initialize connection
                    establishConnection(socket);

                    synchronized (this) {
                        startupComplete = true;
                        notify();
                    }
                }
                catch (SocketException e) {
                    /* do nothing */
                }
                catch (Exception e) {
                    try {
                        LOGGER.log(Level.SEVERE, "exception", e);
                        socket.close();
                    }
                    catch (IOException e1) {
                        /* Do Nothing */
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
        private void establishConnection(Socket socket) throws IOException, SmackException {
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
            String responseDigest = new String(connectionRequest, 5, connectionRequest[4], StringUtils.UTF8);

            connectionRequest[1] = (byte) 0x00; // set return status to 0 (success)
            out.write(connectionRequest);
            out.flush();

            // store connection
            Socks5TestProxy.this.connectionMap.put(responseDigest, socket);
        }

    }

}
