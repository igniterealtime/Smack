/**
 *
 * Copyright 2015-2016 Florian Schmaus
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
package org.jivesoftware.smack.util.dns;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.X509TrustManager;
import java.security.KeyManagementException;
import java.security.SecureRandom;
import java.security.cert.CertificateException;

/**
 * Implementations of this interface define a class that is capable of enabling DANE on a connection.
 */
public interface SmackDaneVerifier {
    void init(SSLContext context, KeyManager[] km, X509TrustManager tm, SecureRandom random) throws KeyManagementException;

    void finish(SSLSocket socket) throws CertificateException;
}
