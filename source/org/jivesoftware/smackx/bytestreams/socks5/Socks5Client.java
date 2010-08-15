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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smackx.bytestreams.socks5.packet.Bytestream.StreamHost;

/**
 * The SOCKS5 client class handles establishing a connection to a SOCKS5 proxy. Connecting to a
 * SOCKS5 proxy requires authentication. This implementation only supports the no-authentication
 * authentication method.
 * 
 * @author Henning Staib
 */
class Socks5Client {

    /* stream host containing network settings and name of the SOCKS5 proxy */
    protected StreamHost streamHost;

    /* SHA-1 digest identifying the SOCKS5 stream */
    protected String digest;

    /**
     * Constructor for a SOCKS5 client.
     * 
     * @param streamHost containing network settings of the SOCKS5 proxy
     * @param digest identifying the SOCKS5 Bytestream
     */
    public Socks5Client(StreamHost streamHost, String digest) {
        this.streamHost = streamHost;
        this.digest = digest;
    }

    /**
     * Returns the initialized socket that can be used to transfer data between peers via the SOCKS5
     * proxy.
     * 
     * @param timeout timeout to connect to SOCKS5 proxy in milliseconds
     * @return socket the initialized socket
     * @throws IOException if initializing the socket failed due to a network error
     * @throws XMPPException if establishing connection to SOCKS5 proxy failed
     * @throws TimeoutException if connecting to SOCKS5 proxy timed out
     * @throws InterruptedException if the current thread was interrupted while waiting
     */
    public Socket getSocket(int timeout) throws IOException, XMPPException, InterruptedException,
                    TimeoutException {

        // wrap connecting in future for timeout
        FutureTask<Socket> futureTask = new FutureTask<Socket>(new Callable<Socket>() {

            public Socket call() throws Exception {

                // initialize socket
                Socket socket = new Socket();
                SocketAddress socketAddress = new InetSocketAddress(streamHost.getAddress(),
                                streamHost.getPort());
                socket.connect(socketAddress);

                // initialize connection to SOCKS5 proxy
                if (!establish(socket)) {

                    // initialization failed, close socket
                    socket.close();
                    throw new XMPPException("establishing connection to SOCKS5 proxy failed");

                }

                return socket;
            }

        });
        Thread executor = new Thread(futureTask);
        executor.start();

        // get connection to initiator with timeout
        try {
            return futureTask.get(timeout, TimeUnit.MILLISECONDS);
        }
        catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause != null) {
                // case exceptions to comply with method signature
                if (cause instanceof IOException) {
                    throw (IOException) cause;
                }
                if (cause instanceof XMPPException) {
                    throw (XMPPException) cause;
                }
            }

            // throw generic IO exception if unexpected exception was thrown
            throw new IOException("Error while connection to SOCKS5 proxy");
        }

    }

    /**
     * Initializes the connection to the SOCKS5 proxy by negotiating authentication method and
     * requesting a stream for the given digest. Currently only the no-authentication method is
     * supported by the Socks5Client.
     * <p>
     * Returns <code>true</code> if a stream could be established, otherwise <code>false</code>. If
     * <code>false</code> is returned the given Socket should be closed.
     * 
     * @param socket connected to a SOCKS5 proxy
     * @return <code>true</code> if if a stream could be established, otherwise <code>false</code>.
     *         If <code>false</code> is returned the given Socket should be closed.
     * @throws IOException if a network error occurred
     */
    protected boolean establish(Socket socket) throws IOException {

        /*
         * use DataInputStream/DataOutpuStream to assure read and write is completed in a single
         * statement
         */
        DataInputStream in = new DataInputStream(socket.getInputStream());
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());

        // authentication negotiation
        byte[] cmd = new byte[3];

        cmd[0] = (byte) 0x05; // protocol version 5
        cmd[1] = (byte) 0x01; // number of authentication methods supported
        cmd[2] = (byte) 0x00; // authentication method: no-authentication required

        out.write(cmd);
        out.flush();

        byte[] response = new byte[2];
        in.readFully(response);

        // check if server responded with correct version and no-authentication method
        if (response[0] != (byte) 0x05 || response[1] != (byte) 0x00) {
            return false;
        }

        // request SOCKS5 connection with given address/digest
        byte[] connectionRequest = createSocks5ConnectRequest();
        out.write(connectionRequest);
        out.flush();

        // receive response
        byte[] connectionResponse;
        try {
            connectionResponse = Socks5Utils.receiveSocks5Message(in);
        }
        catch (XMPPException e) {
            return false; // server answered in an unsupported way
        }

        // verify response
        connectionRequest[1] = (byte) 0x00; // set expected return status to 0
        return Arrays.equals(connectionRequest, connectionResponse);
    }

    /**
     * Returns a SOCKS5 connection request message. It contains the command "connect", the address
     * type "domain" and the digest as address.
     * <p>
     * (see <a href="http://tools.ietf.org/html/rfc1928">RFC1928</a>)
     * 
     * @return SOCKS5 connection request message
     */
    private byte[] createSocks5ConnectRequest() {
        byte addr[] = this.digest.getBytes();

        byte[] data = new byte[7 + addr.length];
        data[0] = (byte) 0x05; // version (SOCKS5)
        data[1] = (byte) 0x01; // command (1 - connect)
        data[2] = (byte) 0x00; // reserved byte (always 0)
        data[3] = (byte) 0x03; // address type (3 - domain name)
        data[4] = (byte) addr.length; // address length
        System.arraycopy(addr, 0, data, 5, addr.length); // address
        data[data.length - 2] = (byte) 0; // address port (2 bytes always 0)
        data[data.length - 1] = (byte) 0;

        return data;
    }

}
