/**
 *
 * Copyright 2017 Paul Schaub, 2019 Florian Schmaus
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
package org.jivesoftware.smackx.omemo.util;

import static org.jivesoftware.smackx.omemo.util.OmemoConstants.Crypto.CIPHERMODE;
import static org.jivesoftware.smackx.omemo.util.OmemoConstants.Crypto.KEYLENGTH;
import static org.jivesoftware.smackx.omemo.util.OmemoConstants.Crypto.KEYTYPE;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.util.ArrayList;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.jivesoftware.smackx.omemo.OmemoRatchet;
import org.jivesoftware.smackx.omemo.OmemoService;
import org.jivesoftware.smackx.omemo.element.OmemoElement;
import org.jivesoftware.smackx.omemo.element.OmemoElement_VAxolotl;
import org.jivesoftware.smackx.omemo.element.OmemoHeaderElement_VAxolotl;
import org.jivesoftware.smackx.omemo.element.OmemoKeyElement;
import org.jivesoftware.smackx.omemo.exceptions.CorruptedOmemoKeyException;
import org.jivesoftware.smackx.omemo.exceptions.NoIdentityKeyException;
import org.jivesoftware.smackx.omemo.exceptions.UndecidedOmemoIdentityException;
import org.jivesoftware.smackx.omemo.exceptions.UntrustedOmemoIdentityException;
import org.jivesoftware.smackx.omemo.internal.CiphertextTuple;
import org.jivesoftware.smackx.omemo.internal.OmemoDevice;
import org.jivesoftware.smackx.omemo.trust.OmemoFingerprint;
import org.jivesoftware.smackx.omemo.trust.OmemoTrustCallback;


/**
 * Class used to build OMEMO messages.
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

    private final OmemoDevice userDevice;
    private final OmemoRatchet<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess, T_Addr, T_ECPub, T_Bundle, T_Ciph> ratchet;
    private final OmemoTrustCallback trustCallback;

    private byte[] messageKey;
    private final byte[] initializationVector;

    private byte[] ciphertextMessage;
    private final ArrayList<OmemoKeyElement> keys = new ArrayList<>();

    /**
     * Create an OmemoMessageBuilder.
     *
     * @param userDevice our OmemoDevice
     * @param callback trustCallback for querying trust decisions
     * @param ratchet our OmemoRatchet
     * @param aesKey aes message key used for message encryption
     * @param iv initialization vector used for message encryption
     * @param message message we want to send
     *
     * @throws NoSuchPaddingException if the requested padding mechanism is not availble.
     * @throws BadPaddingException if the input data is not padded properly.
     * @throws InvalidKeyException if the key is invalid.
     * @throws NoSuchAlgorithmException if no such algorithm is available.
     * @throws IllegalBlockSizeException if the input data length is incorrect.
     * @throws InvalidAlgorithmParameterException if the provided arguments are invalid.
     */
    public OmemoMessageBuilder(OmemoDevice userDevice,
                               OmemoTrustCallback callback,
                               OmemoRatchet<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess, T_Addr, T_ECPub, T_Bundle, T_Ciph> ratchet,
                               byte[] aesKey,
                               byte[] iv,
                               String message)
            throws NoSuchPaddingException, BadPaddingException, InvalidKeyException, NoSuchAlgorithmException,
            IllegalBlockSizeException,
            InvalidAlgorithmParameterException {
        this.userDevice = userDevice;
        this.trustCallback = callback;
        this.ratchet = ratchet;
        this.messageKey = aesKey;
        this.initializationVector = iv;
        setMessage(message);
    }

    /**
     * Create an OmemoMessageBuilder.
     *
     * @param userDevice our OmemoDevice
     * @param callback trustCallback for querying trust decisions
     * @param ratchet our OmemoRatchet
     * @param message message we want to send
     *
     * @throws NoSuchPaddingException if the requested padding mechanism is not availble.
     * @throws BadPaddingException if the input data is not padded properly.
     * @throws InvalidKeyException if the key is invalid.
     * @throws NoSuchAlgorithmException if no such algorithm is available.
     * @throws IllegalBlockSizeException if the input data length is incorrect.
     * @throws UnsupportedEncodingException if the encoding is not supported.
     * @throws InvalidAlgorithmParameterException if the provided arguments are invalid.
     */
    public OmemoMessageBuilder(OmemoDevice userDevice,
                               OmemoTrustCallback callback,
                               OmemoRatchet<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess, T_Addr, T_ECPub, T_Bundle, T_Ciph> ratchet,
                               String message)
            throws NoSuchPaddingException, BadPaddingException, InvalidKeyException, NoSuchAlgorithmException, IllegalBlockSizeException,
            UnsupportedEncodingException, InvalidAlgorithmParameterException {
        this(userDevice, callback, ratchet, generateKey(KEYTYPE, KEYLENGTH), generateIv(), message);
    }

    /**
     * Encrypt the message with the aes key.
     * Move the AuthTag from the end of the cipherText to the end of the messageKey afterwards.
     * This prevents an attacker which compromised one recipient device to switch out the cipherText for other recipients.
     * @see <a href="https://conversations.im/omemo/audit.pdf">OMEMO security audit</a>.
     *
     * @param message plaintext message
     * @throws NoSuchPaddingException if the requested padding mechanism is not availble.
     * @throws NoSuchProviderException
     * @throws InvalidAlgorithmParameterException if the provided arguments are invalid.
     * @throws InvalidKeyException if the key is invalid.
     * @throws BadPaddingException if the input data is not padded properly.
     * @throws IllegalBlockSizeException if the input data length is incorrect.
     */
    private void setMessage(String message)
                    throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException,
                    InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        if (message == null) {
            return;
        }

        // Encrypt message body
        SecretKey secretKey = new SecretKeySpec(messageKey, KEYTYPE);
        IvParameterSpec ivSpec = new IvParameterSpec(initializationVector);
        Cipher cipher = Cipher.getInstance(CIPHERMODE);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);

        byte[] body;
        byte[] ciphertext;

        body = message.getBytes(StandardCharsets.UTF_8);
        ciphertext = cipher.doFinal(body);

        byte[] clearKeyWithAuthTag = new byte[messageKey.length + 16];
        byte[] cipherTextWithoutAuthTag = new byte[ciphertext.length - 16];

        moveAuthTag(messageKey, ciphertext, clearKeyWithAuthTag, cipherTextWithoutAuthTag);

        ciphertextMessage = cipherTextWithoutAuthTag;
        messageKey = clearKeyWithAuthTag;
    }

    /**
     * Move the auth tag from the end of the cipherText to the messageKey.
     *
     * @param messageKey source messageKey without authTag
     * @param cipherText source cipherText with authTag
     * @param messageKeyWithAuthTag destination messageKey with authTag
     * @param cipherTextWithoutAuthTag destination cipherText without authTag
     */
    static void moveAuthTag(byte[] messageKey,
                            byte[] cipherText,
                            byte[] messageKeyWithAuthTag,
                            byte[] cipherTextWithoutAuthTag) {
        // Check dimensions of arrays
        if (messageKeyWithAuthTag.length != messageKey.length + 16) {
            throw new IllegalArgumentException("Length of messageKeyWithAuthTag must be length of messageKey + " +
                    "length of AuthTag (16)");
        }

        if (cipherTextWithoutAuthTag.length != cipherText.length - 16) {
            throw new IllegalArgumentException("Length of cipherTextWithoutAuthTag must be length of cipherText " +
            "- length of AuthTag (16)");
        }

        // Move auth tag from cipherText to messageKey
        System.arraycopy(messageKey, 0, messageKeyWithAuthTag, 0, 16);
        System.arraycopy(cipherText, 0, cipherTextWithoutAuthTag, 0, cipherTextWithoutAuthTag.length);
        System.arraycopy(cipherText, cipherText.length - 16, messageKeyWithAuthTag, 16, 16);
    }

    /**
     * Add a new recipient device to the message.
     *
     * @param contactsDevice device of the recipient
     * @throws NoIdentityKeyException if we have no identityKey of that device. Can be fixed by fetching and
     *                                processing the devices bundle.
     * @throws CorruptedOmemoKeyException if the identityKey of that device is corrupted.
     * @throws UndecidedOmemoIdentityException if the user hasn't yet decided whether to trust that device or not.
     * @throws UntrustedOmemoIdentityException if the user has decided not to trust that device.
     * @throws IOException if an I/O error occured.
     */
    public void addRecipient(OmemoDevice contactsDevice)
            throws NoIdentityKeyException, CorruptedOmemoKeyException, UndecidedOmemoIdentityException,
            UntrustedOmemoIdentityException, IOException {

        OmemoFingerprint fingerprint;
        fingerprint = OmemoService.getInstance().getOmemoStoreBackend().getFingerprint(userDevice, contactsDevice);

        switch (trustCallback.getTrust(contactsDevice, fingerprint)) {

            case undecided:
                throw new UndecidedOmemoIdentityException(contactsDevice);

            case trusted:
                CiphertextTuple encryptedKey = ratchet.doubleRatchetEncrypt(contactsDevice, messageKey);
                keys.add(new OmemoKeyElement(encryptedKey.getCiphertext(), contactsDevice.getDeviceId(), encryptedKey.isPreKeyMessage()));
                break;

            case untrusted:
                throw new UntrustedOmemoIdentityException(contactsDevice, fingerprint);

        }
    }

    /**
     * Assemble an OmemoMessageElement from the current state of the builder.
     *
     * @return OmemoMessageElement TODO javadoc me please
     */
    public OmemoElement finish() {
        OmemoHeaderElement_VAxolotl header = new OmemoHeaderElement_VAxolotl(
                userDevice.getDeviceId(),
                keys,
                initializationVector
        );
        return new OmemoElement_VAxolotl(header, ciphertextMessage);
    }

    /**
     * Generate a new AES key used to encrypt the message.
     *
     * @param keyType Key Type
     * @param keyLength Key Length in bit
     * @return new AES key
     * @throws NoSuchAlgorithmException if no such algorithm is available.
     */
    public static byte[] generateKey(String keyType, int keyLength) throws NoSuchAlgorithmException {
        KeyGenerator generator = KeyGenerator.getInstance(keyType);
        generator.init(keyLength);
        return generator.generateKey().getEncoded();
    }

    /**
     * Generate a 16 byte initialization vector for AES encryption.
     *
     * @return iv TODO javadoc me please
     */
    public static byte[] generateIv() {
        SecureRandom random = new SecureRandom();
        byte[] iv = new byte[16];
        random.nextBytes(iv);
        return iv;
    }
}
