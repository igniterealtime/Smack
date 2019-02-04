/**
 *
 * Copyright 2015-2018 Florian Schmaus
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
package org.jivesoftware.smack.util.dns.minidns;

import java.security.KeyManagementException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;
import java.util.logging.Logger;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.jivesoftware.smack.util.CloseableUtil;
import org.jivesoftware.smack.util.dns.SmackDaneVerifier;

import org.minidns.dane.DaneVerifier;
import org.minidns.dane.ExpectingTrustManager;

public class MiniDnsDaneVerifier implements SmackDaneVerifier {
    private static final Logger LOGGER = Logger.getLogger(MiniDnsDaneVerifier.class.getName());

    private static final DaneVerifier VERIFIER = new DaneVerifier();

    private ExpectingTrustManager expectingTrustManager;

    // Package protected constructor. Use MiniDnsDane.newInstance() to create the verifier.
    MiniDnsDaneVerifier() {
    }

    @Override
    public void init(SSLContext context, KeyManager[] km, X509TrustManager tm, SecureRandom random) throws KeyManagementException {
        if (expectingTrustManager != null) {
            throw new IllegalStateException("DaneProvider was initialized before. Use newInstance() instead.");
        }
        expectingTrustManager = new ExpectingTrustManager(tm);
        context.init(km, new TrustManager[] {expectingTrustManager}, random);
    }

    @Override
    public void finish(SSLSocket sslSocket) throws CertificateException {
        if (VERIFIER.verify(sslSocket)) {
            // DANE verification was the only requirement according to the TLSA RR. We can return here.
            return;
        }

        // DANE verification was successful, but according to the TLSA RR we also must perform PKIX validation.
        if (expectingTrustManager.hasException()) {
            // PKIX validation has failed. Throw an exception but close the socket first.
            CloseableUtil.maybeClose(sslSocket, LOGGER);
            throw expectingTrustManager.getException();
        }
    }

    @Override
    public void finish(SSLSession sslSession) throws CertificateException {
        if (VERIFIER.verify(sslSession)) {
            // DANE verification was the only requirement according to the TLSA RR. We can return here.
            return;
        }

        // DANE verification was successful, but according to the TLSA RR we also must perform PKIX validation.
        if (expectingTrustManager.hasException()) {
         // PKIX validation has failed. Throw an exception.
            throw expectingTrustManager.getException();
        }
    }
}
