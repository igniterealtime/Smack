/**
 *
 * Copyright 2014 Florian Schmaus
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
package org.jivesoftware.smack.util;

import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.SmackException.SecurityNotPossibleException;


public class TLSUtils {

    public static final String SSL = "SSL";
    public static final String TLS = "TLS";
    public static final String PROTO_SSL3 = SSL + "v3";
    public static final String PROTO_TLSV1 = TLS + "v1";
    public static final String PROTO_TLSV1_1 = TLS + "v1.1";
    public static final String PROTO_TLSV1_2 = TLS + "v1.2";

    /**
     * Enable only TLS. Connections created with the given ConnectionConfiguration will only support TLS.
     * <p>
     * According to the <a
     * href="https://raw.githubusercontent.com/stpeter/manifesto/master/manifesto.txt">Encrypted
     * XMPP Manifesto</a>, TLSv1.2 shall be deployed, providing fallback support for SSLv3 and
     * TLSv1.1. This method goes one step boyond and upgrades the handshake to use TLSv1 or better.
     * This method requires the underlying OS to support all of TLSv1.2 , 1.1 and 1.0.
     * </p>
     * 
     * @param conf the configuration to apply this setting to
     */
    public static void setTLSOnly(ConnectionConfiguration conf) {
        conf.setEnabledSSLProtocols(new String[] { PROTO_TLSV1_2,  PROTO_TLSV1_1, PROTO_TLSV1 });
    }

    /**
     * Enable only TLS and SSLv3. Connections created with the given ConnectionConfiguration will
     * only support TLS and SSLv3.
     * <p>
     * According to the <a
     * href="https://raw.githubusercontent.com/stpeter/manifesto/master/manifesto.txt">Encrypted
     * XMPP Manifesto</a>, TLSv1.2 shall be deployed, providing fallback support for SSLv3 and
     * TLSv1.1.
     * </p>
     * 
     * @param conf the configuration to apply this setting to
     */
    public static void setSSLv3AndTLSOnly(ConnectionConfiguration conf) {
        conf.setEnabledSSLProtocols(new String[] { PROTO_TLSV1_2,  PROTO_TLSV1_1, PROTO_TLSV1, PROTO_SSL3 });
    }

    /**
     * Accept all SSL/TLS certificates.
     * <p>
     * <b>Warning</b> Use with care. Only use this method if you understand the implications.
     * </p>
     * 
     * @param conf
     * @throws NoSuchAlgorithmException
     * @throws KeyManagementException
     */
    public static void acceptAllCertificates(ConnectionConfiguration conf) throws NoSuchAlgorithmException, KeyManagementException {
        SSLContext context = SSLContext.getInstance(TLS);
        context.init(null, new TrustManager[] { new AcceptAllTrustManager() }, new SecureRandom());
        conf.setCustomSSLContext(context);
    }

    public static void setEnabledProtocolsAndCiphers(final SSLSocket sslSocket,
                    String[] enabledProtocols, String[] enabledCiphers)
                    throws SecurityNotPossibleException {
        if (enabledProtocols != null) {
            Set<String> enabledProtocolsSet = new HashSet<String>(Arrays.asList(enabledProtocols));
            Set<String> supportedProtocolsSet = new HashSet<String>(
                            Arrays.asList(sslSocket.getSupportedProtocols()));
            Set<String> protocolsIntersection = new HashSet<String>(supportedProtocolsSet);
            protocolsIntersection.retainAll(enabledProtocolsSet);
            if (protocolsIntersection.isEmpty()) {
                throw new SecurityNotPossibleException("Request to enable SSL/TLS protocols '"
                                + StringUtils.collectionToString(enabledProtocolsSet)
                                + "', but only '"
                                + StringUtils.collectionToString(supportedProtocolsSet)
                                + "' are supported.");
            }

            // Set the enabled protocols
            enabledProtocols = new String[protocolsIntersection.size()];
            enabledProtocols = protocolsIntersection.toArray(enabledProtocols);
            sslSocket.setEnabledProtocols(enabledProtocols);
        }

        if (enabledCiphers != null) {
            Set<String> enabledCiphersSet = new HashSet<String>(Arrays.asList(enabledCiphers));
            Set<String> supportedCiphersSet = new HashSet<String>(
                            Arrays.asList(sslSocket.getEnabledCipherSuites()));
            Set<String> ciphersIntersection = new HashSet<String>(supportedCiphersSet);
            ciphersIntersection.retainAll(enabledCiphersSet);
            if (ciphersIntersection.isEmpty()) {
                throw new SecurityNotPossibleException("Request to enable SSL/TLS ciphers '"
                                + StringUtils.collectionToString(enabledCiphersSet)
                                + "', but only '"
                                + StringUtils.collectionToString(supportedCiphersSet)
                                + "' are supported.");
            }

            enabledCiphers = new String[ciphersIntersection.size()];
            enabledCiphers = ciphersIntersection.toArray(enabledCiphers);
            sslSocket.setEnabledCipherSuites(enabledCiphers);
        }
    }

    public static class AcceptAllTrustManager implements X509TrustManager {

        @Override
        public void checkClientTrusted(X509Certificate[] arg0, String arg1)
                        throws CertificateException {
            // Nothing to do here
        }

        @Override
        public void checkServerTrusted(X509Certificate[] arg0, String arg1)
                        throws CertificateException {
            // Nothing to do here
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            throw new UnsupportedOperationException();
        }
    }
}
