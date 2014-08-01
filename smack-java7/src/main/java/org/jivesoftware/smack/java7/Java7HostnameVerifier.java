/**
 *
 * Copyright 2014 the original author or authors
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

package org.jivesoftware.smack.java7;

import java.security.Principal;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import javax.security.auth.kerberos.KerberosPrincipal;

import sun.security.util.HostnameChecker;

/**
 * <p>
 * HostnameVerifier implementation which implements the same policy as the Java built-in
 * pre-HostnameVerifier policy.
 * </p>
 * <p>
 * Based on the <a href="found at http://kevinlocke.name/bits
 * /2012/10/03/ssl-certificate-verification-in-dispatch-and-asynchttpclient/">work by Kevin
 * Locke</a> (released under CC0 1.0 Universal / Public Domain Dedication).
 * </p>
 */
public class Java7HostnameVerifier implements HostnameVerifier {

    @Override
    public boolean verify(String hostname, SSLSession session) {
        HostnameChecker checker = HostnameChecker.getInstance(HostnameChecker.TYPE_TLS);

        boolean validCertificate = false, validPrincipal = false;
        try {
            Certificate[] peerCertificates = session.getPeerCertificates();

            if (peerCertificates.length > 0 && peerCertificates[0] instanceof X509Certificate) {
                X509Certificate peerCertificate = (X509Certificate) peerCertificates[0];

                try {
                    checker.match(hostname, peerCertificate);
                    // Certificate matches hostname
                    validCertificate = true;
                }
                catch (CertificateException ex) {
                    // Certificate does not match hostname
                }
            }
            else {
                // Peer does not have any certificates or they aren't X.509
            }
        }
        catch (SSLPeerUnverifiedException ex) {
            // Not using certificates for peers, try verifying the principal
            try {
                Principal peerPrincipal = session.getPeerPrincipal();
                if (peerPrincipal instanceof KerberosPrincipal) {
                    validPrincipal = HostnameChecker.match(hostname,
                                    (KerberosPrincipal) peerPrincipal);
                }
                else {
                    // Can't verify principal, not Kerberos
                }
            }
            catch (SSLPeerUnverifiedException ex2) {
                // Can't verify principal, no principal
            }
        }

        return validCertificate || validPrincipal;
    }
}
