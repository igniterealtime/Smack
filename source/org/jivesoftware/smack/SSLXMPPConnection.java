/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright (C) 2002-2003 Jive Software. All rights reserved.
 * ====================================================================
 * The Jive Software License (based on Apache Software License, Version 1.1)
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *       "This product includes software developed by
 *        Jive Software (http://www.jivesoftware.com)."
 *    Alternately, this acknowledgment may appear in the software itself,
 *    if and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "Smack" and "Jive Software" must not be used to
 *    endorse or promote products derived from this software without
 *    prior written permission. For written permission, please
 *    contact webmaster@jivesoftware.com.
 *
 * 5. Products derived from this software may not be called "Smack",
 *    nor may "Smack" appear in their name, without prior written
 *    permission of Jive Software.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL JIVE SOFTWARE OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 */

package org.jivesoftware.smack;

import java.io.*;
import java.net.*;
import java.security.NoSuchAlgorithmException;
import java.security.KeyManagementException;
import java.security.cert.*;
import javax.net.ssl.*;
import javax.net.*;
import com.sun.net.ssl.*;

/**
 * Creates an SSL connection to a XMPP (Jabber) server.
 *
 * @author Matt Tucker
 */
public class SSLXMPPConnection extends XMPPConnection {

    public SSLXMPPConnection(String host) throws XMPPException {
        this(host, 5223);
    }

    public SSLXMPPConnection(String host, int port) throws XMPPException {
        this.host = host;
        this.port = port;
        try {
            SSLSocketFactory sslFactory = new DummySSLSocketFactory();
            this.socket = sslFactory.createSocket(host, port);
        }
        catch (UnknownHostException uhe) {
            throw new XMPPException("Could not connect to " + host + ":" + port + ".", uhe);
        }
        catch (IOException ioe) {
            throw new XMPPException("Error connecting to " + host + ":" + port + ".", ioe);
        }
        super.init();
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
                                new TrustManager[] { new DummyTrustManager() },
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

    /**
     * Trust manager which accepts certificates without any validation
     * except date validation.
     */
    private static class DummyTrustManager implements X509TrustManager {

        public boolean isClientTrusted(X509Certificate[] cert) {
            return true;
        }

        public boolean isServerTrusted(X509Certificate[] cert) {
            try {
                cert[0].checkValidity();
                return true;
            }
            catch (CertificateExpiredException e) {
                return false;
            }
            catch (CertificateNotYetValidException e) {
                return false;
            }
        }

        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }
}
