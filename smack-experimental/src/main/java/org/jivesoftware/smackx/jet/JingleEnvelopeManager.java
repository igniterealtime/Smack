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
 * Interface that provides methods that need to be implemented by potential JingleEnvelopeManagers.
 * An JingleEnvelopeManager can be used to secure a JET encrypted Transport.
 */
public interface JingleEnvelopeManager {

    /**
     * Encrypt a serialized encryption key (Transport Secret) and return the resulting {@link ExtensionElement} (Envelope).
     * @param recipient recipient of the transfer.
     * @param keyData Serialized key. This is referred to in the XEP as Transport Secret.
     * @return encrypted Transport Secret as Envelope element.
     * @throws JingleEncryptionException JET encryption fails.
     * @throws InterruptedException
     * @throws NoSuchAlgorithmException
     * @throws SmackException.NotConnectedException
     * @throws SmackException.NoResponseException
     */
    ExtensionElement encryptJingleTransfer(FullJid recipient, byte[] keyData)
            throws JingleEncryptionException, InterruptedException, NoSuchAlgorithmException,
            SmackException.NotConnectedException, SmackException.NoResponseException;

    /**
     * Decrypt a serialized encryption key (Transport Secret) from an {@link ExtensionElement} (Envelope).
     * @param sender sender of the Envelope.
     * @param envelope encrypted key as Envelope element.
     * @return decrypted Transport Secret.
     * @throws JingleEncryptionException JET encryption fails.
     * @throws InterruptedException
     * @throws XMPPException.XMPPErrorException
     * @throws SmackException.NotConnectedException
     * @throws SmackException.NoResponseException
     */
    byte[] decryptJingleTransfer(FullJid sender, ExtensionElement envelope)
            throws JingleEncryptionException, InterruptedException, XMPPException.XMPPErrorException,
            SmackException.NotConnectedException, SmackException.NoResponseException;

    /**
     * Exception that wraps possible exceptions that might occur during encryption/decryption.
     */
    class JingleEncryptionException extends Exception {
        private static final long serialVersionUID = 1L;

        public JingleEncryptionException(Throwable throwable) {
            super(throwable);
        }
    }

    /**
     * Return the connection of the manager.
     * @return connection.
     */
    XMPPConnection getConnection();

    /**
     * Return the namespace of the Envelope method.
     * @return namespace.
     */
    String getJingleEnvelopeNamespace();
}
