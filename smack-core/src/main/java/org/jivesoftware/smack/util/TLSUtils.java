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

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
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
     * @param builder the configuration builder to apply this setting to
     */
    public static <B extends ConnectionConfiguration.Builder<B,?>> B setTLSOnly(B builder) {
        builder.setEnabledSSLProtocols(new String[] { PROTO_TLSV1_2,  PROTO_TLSV1_1, PROTO_TLSV1 });
        return builder;
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
     * @param builder the configuration builder to apply this setting to
     */
    public static <B extends ConnectionConfiguration.Builder<B,?>> B setSSLv3AndTLSOnly(B builder) {
        builder.setEnabledSSLProtocols(new String[] { PROTO_TLSV1_2,  PROTO_TLSV1_1, PROTO_TLSV1, PROTO_SSL3 });
        return builder;
    }

    /**
     * Accept all TLS certificates.
     * <p>
     * <b>Warning:</b> Use with care. This method make the Connection use {@link AcceptAllTrustManager} and essentially
     * <b>invalidates all security guarantees provided by TLS</b>. Only use this method if you understand the
     * implications.
     * </p>
     * 
     * @param builder a connection configuration builder.
     * @throws NoSuchAlgorithmException
     * @throws KeyManagementException
     * @return the given builder.
     */
    public static <B extends ConnectionConfiguration.Builder<B,?>> B acceptAllCertificates(B builder) throws NoSuchAlgorithmException, KeyManagementException {
        SSLContext context = SSLContext.getInstance(TLS);
        context.init(null, new TrustManager[] { new AcceptAllTrustManager() }, new SecureRandom());
        builder.setCustomSSLContext(context);
        return builder;
    }

    private static final HostnameVerifier DOES_NOT_VERIFY_VERIFIER = new HostnameVerifier() {
        @Override
        public boolean verify(String hostname, SSLSession session) {
            // This verifier doesn't verify the hostname, it always returns true.
            return true;
        }
    };

    /**
     * Disable the hostname verification of TLS certificates.
     * <p>
     * <b>Warning:</b> Use with care. This disables hostname verification of TLS certificates and essentially
     * <b>invalidates all security guarantees provided by TLS</b>. Only use this method if you understand the
     * implications.
     * </p>
     * 
     * @param builder a connection configuration builder.
     * @return the given builder.
     */
    public static <B extends ConnectionConfiguration.Builder<B,?>> B disableHostnameVerificationForTlsCertificicates(B builder) {
        builder.setHostnameVerifier(DOES_NOT_VERIFY_VERIFIER);
        return builder;
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

    /**
     * A {@link X509TrustManager} that <b>doesn't validate</b> X.509 certificates.
     * <p>
     * Connections that use this TrustManager will just be encrypted, without any guarantee that the
     * counter part is actually the intended one. Man-in-the-Middle attacks will be possible, since
     * any certificate presented by the attacker will be considered valid.
     * </p>
     */
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
            return new X509Certificate[0];
        }
    }
}
