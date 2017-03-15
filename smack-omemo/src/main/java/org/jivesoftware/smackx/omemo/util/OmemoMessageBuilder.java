/**
 * Copyright the original author or authors
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jivesoftware.smackx.omemo.util;

import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.omemo.internal.OmemoSession;
import org.jivesoftware.smackx.omemo.OmemoStore;
import org.jivesoftware.smackx.omemo.elements.OmemoMessageElement;
import org.jivesoftware.smackx.omemo.exceptions.CannotEstablishOmemoSessionException;
import org.jivesoftware.smackx.omemo.exceptions.CryptoFailedException;
import org.jivesoftware.smackx.omemo.exceptions.InvalidOmemoKeyException;
import org.jivesoftware.smackx.omemo.exceptions.UndecidedOmemoIdentityException;
import org.jivesoftware.smackx.omemo.internal.CiphertextTuple;
import org.jivesoftware.smackx.omemo.internal.OmemoDevice;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.*;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.jivesoftware.smackx.omemo.util.OmemoConstants.Crypto.*;

/**
 * Class used to build OMEMO messages
 *
 * @param <T_IdKeyPair> IdentityKeyPair class
 * @param <T_IdKey>     IdentityKey class
 * @param <T_PreKey>    PreKey class
 * @param <T_SigPreKey> SignedPreKey class
 * @param <T_Sess>      Session class
 * @param <T_Addr>      Address class
 * @param <T_ECPub>     Elliptic Curve PublicKey class
 * @param <T_Bundle>    Bundle class
 * @param <T_Ciph>      Cipher class
 * @author Paul Schaub
 */
public class OmemoMessageBuilder<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess, T_Addr, T_ECPub, T_Bundle, T_Ciph> {
    private static final Logger LOGGER = Logger.getLogger(OmemoMessageBuilder.class.getName());
    private final OmemoStore<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess, T_Addr, T_ECPub, T_Bundle, T_Ciph> omemoStore;

    private byte[] messageKey = generateKey();
    private final byte[] initializationVector = generateIv();

    private byte[] ciphertextMessage;
    private final ArrayList<OmemoMessageElement.OmemoHeader.Key> keys = new ArrayList<>();

    public OmemoMessageBuilder(OmemoStore<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess, T_Addr, T_ECPub, T_Bundle, T_Ciph> omemoStore, String message)
            throws CryptoFailedException {
        this.omemoStore = omemoStore;
        this.setMessage(message);
    }

    /**
     * Create an AES messageKey and use it to encrypt the message.
     * Optionally append the Auth Tag of the encrypted message to the messageKey afterwards.
     *
     * @param message content of the body
     * @throws CryptoFailedException if something goes wrong
     */
    private void setMessage(String message) throws CryptoFailedException {
        //Encrypt message body
        try {
            SecretKey secretKey = new SecretKeySpec(messageKey, KEYTYPE);
            IvParameterSpec ivSpec = new IvParameterSpec(initializationVector);
            Cipher cipher = Cipher.getInstance(CIPHERMODE, PROVIDER);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);

            byte[] body = (message.getBytes(StringUtils.UTF8));
            byte[] ciphertext = cipher.doFinal(body);

            if (ciphertext == null) {
                throw new CryptoFailedException("Could not encrypt message body.");
            }

            if (OmemoConstants.APPEND_AUTH_TAG_TO_MESSAGE_KEY) {
                byte[] clearKeyWithAuthTag = new byte[messageKey.length + 16];
                byte[] cipherTextWithoutAuthTag = new byte[ciphertext.length - 16];

                System.arraycopy(messageKey, 0, clearKeyWithAuthTag, 0, 16);
                System.arraycopy(ciphertext, 0, cipherTextWithoutAuthTag, 0, cipherTextWithoutAuthTag.length);
                System.arraycopy(ciphertext, ciphertext.length - 16, clearKeyWithAuthTag, 16, 16);

                ciphertextMessage = cipherTextWithoutAuthTag;
                messageKey = clearKeyWithAuthTag;
            } else {
                ciphertextMessage = ciphertext;
            }
        } catch (NoSuchAlgorithmException | NoSuchPaddingException | InvalidKeyException
                | IllegalBlockSizeException | BadPaddingException | NoSuchProviderException
                | InvalidAlgorithmParameterException | UnsupportedEncodingException e) {
            throw new CryptoFailedException(e);
        }
    }

    /**
     * Add a new recipient device to the message
     *
     * @param device recipient device
     * @throws CannotEstablishOmemoSessionException when no session can be established
     * @throws CryptoFailedException                when encrypting the messageKey fails
     */
    public void addRecipient(OmemoDevice device) throws CannotEstablishOmemoSessionException,
            CryptoFailedException, UndecidedOmemoIdentityException, InvalidOmemoKeyException {
        //For each recipient device: Encrypt message key with session key
        if (!omemoStore.containsRawSession(device)) {
            omemoStore.getOmemoService().buildSessionFromOmemoBundle(device);
        }

        OmemoSession<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess, T_Addr, T_ECPub, T_Bundle, T_Ciph> session =
                omemoStore.getOmemoSessionOf(device);

        if (session != null) {
            if (!omemoStore.isDecidedOmemoIdentity(device, session.getIdentityKey())) {
                //Warn user of undecided device
                throw new UndecidedOmemoIdentityException(device);
            }

            if (omemoStore.isTrustedOmemoIdentity(device, session.getIdentityKey())) {
                //Encrypt key and save to header
                CiphertextTuple encryptedKey = session.encryptMessageKey(messageKey);
                keys.add(new OmemoMessageElement.OmemoHeader.Key(encryptedKey.getCiphertext(), device.getDeviceId(), encryptedKey.isPreKeyMessage()));
            }
        } else {
            throw new CannotEstablishOmemoSessionException("Can't find or establish session with " + device);
        }
    }

    /**
     * Assemble an OmemoMessageElement from the current state of the builder
     *
     * @return OmemoMessageElement
     */
    public OmemoMessageElement finish() {
        OmemoMessageElement.OmemoHeader header = new OmemoMessageElement.OmemoHeader(
                omemoStore.loadOmemoDeviceId(),
                keys,
                initializationVector
        );
        return new OmemoMessageElement(header, ciphertextMessage);
    }

    /**
     * Generate a new AES key used to encrypt the message
     *
     * @return new AES key
     */
    private static byte[] generateKey() {
        try {
            KeyGenerator generator = KeyGenerator.getInstance(KEYTYPE);
            generator.init(128);
            return generator.generateKey().getEncoded();
        } catch (NoSuchAlgorithmException e) {
            LOGGER.log(Level.INFO, "Error generating key: " + e.getClass() + " " + e.getMessage());
            return null;
        }
    }

    /**
     * Generate a 16 byte initialization vector for AES encryption
     *
     * @return iv
     */
    private static byte[] generateIv() {
        SecureRandom random = new SecureRandom();
        byte[] iv = new byte[16];
        random.nextBytes(iv);
        return iv;
    }
}