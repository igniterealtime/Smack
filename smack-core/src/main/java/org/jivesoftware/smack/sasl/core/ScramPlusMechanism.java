/**
 *
 * Copyright 2016 Florian Schmaus
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
package org.jivesoftware.smack.sasl.core;

import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;

import javax.net.ssl.SSLPeerUnverifiedException;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.util.TLSUtils;

/**
 * SCRAM-X-PLUS implementation. Due limitations of the Java API, this mechanism only supports the 'tls-server-end-point'
 * channel binding type. But on the other hand, the other relevant channel binding type 'tls-unique' has some flaws (see
 * 3SHAKE, RFC 7627).
 *
 * @author Florian Schmaus
 */
public abstract class ScramPlusMechanism extends ScramMechanism {

    protected ScramPlusMechanism(ScramHmac scramHmac) {
        super(scramHmac);
    }

    @Override
    public String getName() {
        return super.getName() + "-PLUS";
    }

    @Override
    protected String getChannelBindingName() {
        return "p=tls-server-end-point";
    }

    @Override
    protected byte[] getChannelBindingData() throws SmackException {
        byte[] cbData;
        try {
            cbData = TLSUtils.getChannelBindingTlsServerEndPoint(sslSession);
        }
        catch (SSLPeerUnverifiedException | CertificateEncodingException | NoSuchAlgorithmException e) {
            throw new SmackException(e);
        }
        return cbData;
    }
}
