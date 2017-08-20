/**
 *
 * Copyright 2017 Paul Schaub
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
package org.jivesoftware.smackx.jet;

import java.security.NoSuchAlgorithmException;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.packet.ExtensionElement;

import org.jxmpp.jid.FullJid;

/**
 * Classes that implement this interface can be used to encrypt Jingle File Transfers.
 */
public interface JingleEnvelopeManager {

    ExtensionElement encryptJingleTransfer(FullJid recipient, byte[] keyData)
            throws JingleEncryptionException, InterruptedException, NoSuchAlgorithmException,
            SmackException.NotConnectedException, SmackException.NoResponseException;

    byte[] decryptJingleTransfer(FullJid sender, ExtensionElement envelope)
            throws JingleEncryptionException, InterruptedException, XMPPException.XMPPErrorException,
            SmackException.NotConnectedException, SmackException.NoResponseException;

    class JingleEncryptionException extends Exception {
        private static final long serialVersionUID = 1L;

        public JingleEncryptionException(Throwable throwable) {
            super(throwable);
        }
    }

    XMPPConnection getConnection();

    String getJingleEnvelopeNamespace();
}
