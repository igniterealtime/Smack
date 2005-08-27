/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright 2003-2004 Jive Software.
 *
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

package org.jivesoftware.smack;

import javax.net.SocketFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

/**
 * Creates an SSL connection to a XMPP server.
 *
 * @author Matt Tucker
 */
public class SSLXMPPConnection extends XMPPConnection {

    private static SocketFactory socketFactory = new DummySSLSocketFactory();

    /**
     * Creates a new SSL connection to the specified host on the default
     * SSL port (5223). The IP address of the server is assumed to match the
     * service name.
     *
     * @param host the XMPP host.
     * @throws XMPPException if an error occurs while trying to establish the connection.
     *      Two possible errors can occur which will be wrapped by an XMPPException --
     *      UnknownHostException (XMPP error code 504), and IOException (XMPP error code
     *      502). The error codes and wrapped exceptions can be used to present more
     *      appropiate error messages to end-users.
     */
    public SSLXMPPConnection(String host) throws XMPPException {
        this(host, 5223);
    }

    /**
     * Creates a new SSL connection to the specified host on the specified port. The IP address
     * of the server is assumed to match the service name.
     *
     * @param host the XMPP host.
     * @param port the port to use for the connection (default XMPP SSL port is 5223).
     * @throws XMPPException if an error occurs while trying to establish the connection.
     *      Two possible errors can occur which will be wrapped by an XMPPException --
     *      UnknownHostException (XMPP error code 504), and IOException (XMPP error code
     *      502). The error codes and wrapped exceptions can be used to present more
     *      appropiate error messages to end-users.
     */
    public SSLXMPPConnection(String host, int port) throws XMPPException {
        this(host, port, host);
    }

    /**
     * Creates a new SSL connection to the specified XMPP server on the given host and port.
     *
     * @param host the host name, or null for the loopback address.
     * @param port the port on the server that should be used (default XMPP SSL port is 5223).
     * @param serviceName the name of the XMPP server to connect to; e.g. <tt>jivesoftware.com</tt>.
     * @throws XMPPException if an error occurs while trying to establish the connection.
     *      Two possible errors can occur which will be wrapped by an XMPPException --
     *      UnknownHostException (XMPP error code 504), and IOException (XMPP error code
     *      502). The error codes and wrapped exceptions can be used to present more
     *      appropiate error messages to end-users.
     */
    public SSLXMPPConnection(String host, int port, String serviceName) throws XMPPException {
        super(host, port, serviceName, socketFactory);
    }

    public boolean isSecureConnection() {
        return true;
    }

    /**
     * An SSL socket factory that will let any certifacte past, even if it's expired or
     * not singed by a root CA.
     */
    private static class DummySSLSocketFactory extends SSLSocketFactory {

        private SSLSocketFactory factory;

        public DummySSLSocketFactory() {

            try {
                SSLContext sslcontent = SSLContext.getInstance("TLS");
                sslcontent.init(null, // KeyManager not required
                            new TrustManager[] { new OpenTrustManager() },
                            new java.security.SecureRandom());
                factory = sslcontent.getSocketFactory();
            }
            catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
            }
            catch (KeyManagementException e) {
                e.printStackTrace();
            }
        }

        public static SocketFactory getDefault() {
            return new DummySSLSocketFactory();
        }

        public Socket createSocket(Socket socket, String s, int i, boolean flag)
                throws IOException
        {
            return factory.createSocket(socket, s, i, flag);
        }

        public Socket createSocket(InetAddress inaddr, int i, InetAddress inaddr2, int j)
                throws IOException
        {
            return factory.createSocket(inaddr, i, inaddr2, j);
        }

        public Socket createSocket(InetAddress inaddr, int i) throws IOException {
            return factory.createSocket(inaddr, i);
        }

        public Socket createSocket(String s, int i, InetAddress inaddr, int j) throws IOException {
            return factory.createSocket(s, i, inaddr, j);
        }

        public Socket createSocket(String s, int i) throws IOException {
            return factory.createSocket(s, i);
        }

        public String[] getDefaultCipherSuites() {
            return factory.getSupportedCipherSuites();
        }

        public String[] getSupportedCipherSuites() {
            return factory.getSupportedCipherSuites();
        }
    }
}
