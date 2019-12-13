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
package org.jivesoftware.smackx.omemo;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;

import org.jivesoftware.smackx.omemo.element.OmemoElement;
import org.jivesoftware.smackx.omemo.element.OmemoKeyElement;
import org.jivesoftware.smackx.omemo.exceptions.CorruptedOmemoKeyException;
import org.jivesoftware.smackx.omemo.exceptions.CryptoFailedException;
import org.jivesoftware.smackx.omemo.exceptions.MultipleCryptoFailedException;
import org.jivesoftware.smackx.omemo.exceptions.NoRawSessionException;
import org.jivesoftware.smackx.omemo.exceptions.UntrustedOmemoIdentityException;
import org.jivesoftware.smackx.omemo.internal.CipherAndAuthTag;
import org.jivesoftware.smackx.omemo.internal.CiphertextTuple;
import org.jivesoftware.smackx.omemo.internal.OmemoDevice;

public abstract class OmemoRatchet<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess, T_Addr, T_ECPub, T_Bundle, T_Ciph> {
    private static final Logger LOGGER = Logger.getLogger(OmemoRatchet.class.getName());

    protected final OmemoManager omemoManager;
    protected final OmemoStore<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess, T_Addr, T_ECPub, T_Bundle, T_Ciph> store;

    /**
     * Constructor.
     *
     * @param omemoManager omemoManager
     * @param store omemoStore
     */
    public OmemoRatchet(OmemoManager omemoManager,
                        OmemoStore<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess, T_Addr, T_ECPub, T_Bundle, T_Ciph> store) {
        this.omemoManager = omemoManager;
        this.store = store;
    }

    /**
     * Decrypt a double-ratchet-encrypted message key.
     *
     * @param sender sender of the message.
     * @param encryptedKey key encrypted with the ratchet of the sender.
     * @return decrypted message key.
     *
     * @throws CorruptedOmemoKeyException if the OMEMO key is corrupted.
     * @throws NoRawSessionException when no double ratchet session was found.
     * @throws CryptoFailedException if the OMEMO cryptography failed.
     * @throws UntrustedOmemoIdentityException if the OMEMO identity is not trusted.
     * @throws IOException if an I/O error occurred.
     */
    public abstract byte[] doubleRatchetDecrypt(OmemoDevice sender, byte[] encryptedKey)
            throws CorruptedOmemoKeyException, NoRawSessionException, CryptoFailedException,
            UntrustedOmemoIdentityException, IOException;

    /**
     * Encrypt a messageKey with the double ratchet session of the recipient.
     *
     * @param recipient recipient of the message.
     * @param messageKey key we want to encrypt.
     * @return encrypted message key.
     */
    public abstract CiphertextTuple doubleRatchetEncrypt(OmemoDevice recipient, byte[] messageKey);

    /**
     * Try to decrypt the transported message key using the double ratchet session.
     *
     * @param element omemoElement
     * @return tuple of cipher generated from the unpacked message key and the auth-tag
     *
     * @throws CryptoFailedException if decryption using the double ratchet fails
     * @throws NoRawSessionException if we have no session, but the element was NOT a PreKeyMessage
     * @throws IOException if an I/O error occurred.
     */
    CipherAndAuthTag retrieveMessageKeyAndAuthTag(OmemoDevice sender, OmemoElement element) throws CryptoFailedException,
            NoRawSessionException, IOException {
        int keyId = omemoManager.getDeviceId();
        byte[] unpackedKey = null;
        List<CryptoFailedException> decryptExceptions = new ArrayList<>();
        List<OmemoKeyElement> keys = element.getHeader().getKeys();

        boolean preKey = false;

        // Find key with our ID.
        for (OmemoKeyElement k : keys) {
            if (k.getId() == keyId) {
                try {
                    unpackedKey = doubleRatchetDecrypt(sender, k.getData());
                    preKey = k.isPreKey();
                    break;
                } catch (CryptoFailedException e) {
                    // There might be multiple keys with our id, but we can only decrypt one.
                    // So we can't throw the exception, when decrypting the first duplicate which is not for us.
                    decryptExceptions.add(e);
                } catch (CorruptedOmemoKeyException e) {
                    decryptExceptions.add(new CryptoFailedException(e));
                } catch (UntrustedOmemoIdentityException e) {
                    LOGGER.log(Level.WARNING, "Received message from " + sender + " contained unknown identityKey. Ignore message.", e);
                }
            }
        }

        if (unpackedKey == null) {
            if (!decryptExceptions.isEmpty()) {
                throw MultipleCryptoFailedException.from(decryptExceptions);
            }

            throw new CryptoFailedException("Transported key could not be decrypted, since no suitable message key " +
                    "was provided. Provides keys: " + keys);
        }

        // Split in AES auth-tag and key
        byte[] messageKey = new byte[16];
        byte[] authTag = null;

        if (unpackedKey.length == 32) {
            authTag = new byte[16];
            // copy key part into messageKey
            System.arraycopy(unpackedKey, 0, messageKey, 0, 16);
            // copy tag part into authTag
            System.arraycopy(unpackedKey, 16, authTag, 0, 16);
        } else if (element.isKeyTransportElement() && unpackedKey.length == 16) {
            messageKey = unpackedKey;
        } else {
            throw new CryptoFailedException("MessageKey has wrong length: "
                    + unpackedKey.length + ". Probably legacy auth tag format.");
        }

        return new CipherAndAuthTag(messageKey, element.getHeader().getIv(), authTag, preKey);
    }

    /**
     * Use the symmetric key in cipherAndAuthTag to decrypt the payload of the omemoMessage.
     * The decrypted payload will be the body of the returned Message.
     *
     * @param element omemoElement containing a payload.
     * @param cipherAndAuthTag cipher and authentication tag.
     * @return decrypted plain text.
     *
     * @throws CryptoFailedException if decryption using AES key fails.
     */
    static String decryptMessageElement(OmemoElement element, CipherAndAuthTag cipherAndAuthTag)
            throws CryptoFailedException {
        if (!element.isMessageElement()) {
            throw new IllegalArgumentException("decryptMessageElement cannot decrypt OmemoElement which is no MessageElement!");
        }

        if (cipherAndAuthTag.getAuthTag() == null || cipherAndAuthTag.getAuthTag().length != 16) {
            throw new CryptoFailedException("AuthenticationTag is null or has wrong length: "
                    + (cipherAndAuthTag.getAuthTag() == null ? "null" : cipherAndAuthTag.getAuthTag().length));
        }

        byte[] encryptedBody = payloadAndAuthTag(element, cipherAndAuthTag.getAuthTag());

        try {
            String plaintext = new String(cipherAndAuthTag.getCipher().doFinal(encryptedBody), StandardCharsets.UTF_8);
            return plaintext;
        } catch (IllegalBlockSizeException | BadPaddingException e) {
            throw new CryptoFailedException("decryptMessageElement could not decipher message body: "
                    + e.getMessage());
        }
    }

    /**
     * Return the concatenation of the payload of the OmemoElement and the given auth tag.
     *
     * @param element omemoElement (message element)
     * @param authTag authTag
     * @return payload + authTag
     */
    static byte[] payloadAndAuthTag(OmemoElement element, byte[] authTag) {
        if (!element.isMessageElement()) {
            throw new IllegalArgumentException("OmemoElement has no payload.");
        }

        byte[] payload = new byte[element.getPayload().length + authTag.length];
        System.arraycopy(element.getPayload(), 0, payload, 0, element.getPayload().length);
        System.arraycopy(authTag, 0, payload, element.getPayload().length, authTag.length);
        return payload;
    }

}
