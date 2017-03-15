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
package org.jivesoftware.smackx.omemo.elements;

import org.jivesoftware.smack.packet.ExtensionElement;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.XmlStringBuilder;
import org.jivesoftware.smack.util.stringencoder.Base64;
import org.jivesoftware.smackx.omemo.internal.OmemoSession;
import org.jivesoftware.smackx.omemo.exceptions.CryptoFailedException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.jivesoftware.smackx.omemo.util.OmemoConstants.Crypto.*;
import static org.jivesoftware.smackx.omemo.util.OmemoConstants.Encrypted.*;
import static org.jivesoftware.smackx.omemo.util.OmemoConstants.OMEMO_NAMESPACE;

/**
 * An OMEMO (PreKey)WhisperMessage element
 *
 * @author Paul Schaub
 */
public class OmemoMessageElement implements ExtensionElement {
    private static final Logger LOGGER = Logger.getLogger(OmemoMessageElement.class.getName());
    private final OmemoHeader header;
    private final byte[] payload;

    /**
     * Create a new OmemoMessageElement from a header and a payload
     *
     * @param header  header of the message
     * @param payload payload
     */
    public OmemoMessageElement(OmemoHeader header, byte[] payload) {
        this.header = header;
        this.payload = payload;
    }

    public OmemoHeader getHeader() {
        return header;
    }

    /**
     * Return the payload of the message
     *
     * @return payload
     */
    public byte[] getPayload() {
        return payload;
    }

    /**
     * Header element of the message. The header contains information about the sender and the encrypted keys for
     * the recipients, as well as the iv element for AES
     */
    public static class OmemoHeader {
        private final int sid;
        private final ArrayList<OmemoHeader.Key> keys;
        private final byte[] iv;

        public OmemoHeader(int sid, ArrayList<OmemoHeader.Key> keys, byte[] iv) {
            this.sid = sid;
            this.keys = keys;
            this.iv = iv;
        }

        /**
         * Return the deviceId of the sender of the message
         *
         * @return senders id
         */
        public int getSid() {
            return sid;
        }

        public ArrayList<OmemoHeader.Key> getKeys() {
            ArrayList<OmemoHeader.Key> copy = new ArrayList<>();
            for (OmemoHeader.Key k : keys) {
                copy.add(new OmemoHeader.Key(k.getData(), k.getId()));
            }
            return copy;
        }

        public byte[] getIv() {
            return iv != null ? iv.clone() : null;
        }

        /**
         * Small class to collect key (byte[]), its id and whether its a prekey or not.
         */
        public static class Key {
            final byte[] data;
            final int id;
            final boolean preKey;

            public Key(byte[] data, int id) {
                this.data = data;
                this.id = id;
                this.preKey = false;
            }

            public Key(byte[] data, int id, boolean preKey) {
                this.data = data;
                this.id = id;
                this.preKey = preKey;
            }

            public int getId() {
                return this.id;
            }

            public byte[] getData() {
                return this.data;
            }

            public boolean isPreKey() {
                return this.preKey;
            }
        }
    }

    @Override
    public String getElementName() {
        return ENCRYPTED;
    }

    @Override
    public XmlStringBuilder toXML() {
        XmlStringBuilder sb = new XmlStringBuilder(this).rightAngleBracket();

        sb.halfOpenElement(HEADER).attribute(SID, header.sid).rightAngleBracket();

        for (OmemoHeader.Key k : getHeader().getKeys()) {
            if (k.isPreKey()) {
                sb.halfOpenElement(KEY).attribute(PREKEY, true).attribute(RID, k.getId()).rightAngleBracket();
            } else {
                sb.halfOpenElement(KEY).attribute(RID, k.getId()).rightAngleBracket();
            }
            sb.append(Base64.encodeToString(k.getData()));
            sb.closeElement(KEY);
        }

        sb.openElement(IV).append(Base64.encodeToString(header.iv)).closeElement(IV);

        sb.closeElement(HEADER);

        sb.openElement(PAYLOAD).append(Base64.encodeToString(payload)).closeElement(PAYLOAD);

        sb.closeElement(this);
        return sb;
    }

    @Override
    public String getNamespace() {
        return OMEMO_NAMESPACE;
    }

    @Override
    public String toString() {
        try {
            String s = "Encrypted:\n" +
                    "   header: sid: " + getHeader().getSid() + "\n";
            for (OmemoHeader.Key k : getHeader().getKeys()) {
                s += "      key: prekey: " + k.isPreKey() + " rid: " + k.getId() + " " + new String(k.getData(), StringUtils.UTF8) + "\n";
            }
            s += "      iv: " + new String(getHeader().getIv(), StringUtils.UTF8) + "\n";
            s += "  payload: " + new String(getPayload(), StringUtils.UTF8);
            return s;
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Try to decrypt the message.
     * First decrypt the message key using our session with the sender.
     * Second use the decrypted key to decrypt the message.
     * The decrypted content of the 'encrypted'-element becomes the body of the clear text message
     *
     * @param session OmemoSession with the sender device
     * @param keyId   the key we want to decrypt (usually our own device id)
     * @return message as plaintext
     */
    public Message decrypt(OmemoSession<?, ?, ?, ?, ?, ?, ?, ?, ?> session, int keyId) throws CryptoFailedException {
        String plain;
        byte[] cipherText = getPayload();
        byte[] messageKey = new byte[16];
        byte[] unpackedKey = null;

        for (OmemoHeader.Key k : getHeader().getKeys()) {
            if (k.getId() == keyId) {
                try {
                    unpackedKey = session.decryptMessageKey(k.getData());
                    break;
                } catch (CryptoFailedException ignored) {
                    ignored.printStackTrace();
                    //TODO: Wise to ignore the exception?
                    //The issue is, there might be multiple keys with our id, but we can only decrypt one.
                    //So we can't throw the exception, when decrypting the first duplicate which is not for us.
                    //Just print the exception for now.
                }
            }
        }

        if (unpackedKey == null) {
            LOGGER.log(Level.WARNING, "Could not find a key that could be decrypted.");
            throw new CryptoFailedException("OmemoMessageElement could not be decrypted, since the message key could not be unpacked.");
        }

        // Check, if key includes the auth-tag
        // See https://github.com/ChatSecure/ChatSecure-iOS/issues/647
        if (unpackedKey.length == 32) {
            LOGGER.log(Level.INFO, "Combined Key/Auth Tag");
            System.arraycopy(unpackedKey, 0, messageKey, 0, 16);
            byte[] newCiphertext = new byte[cipherText.length + 16];
            System.arraycopy(cipherText, 0, newCiphertext, 0, cipherText.length);
            System.arraycopy(unpackedKey, 16, newCiphertext, cipherText.length, 16);
            cipherText = newCiphertext;
        } else {
            messageKey = unpackedKey;
        }

        // Create cipher with message key
        try {
            Cipher cipher = Cipher.getInstance(CIPHERMODE, PROVIDER);
            SecretKeySpec keySpec = new SecretKeySpec(messageKey, KEYTYPE);
            IvParameterSpec ivSpec = new IvParameterSpec(header.getIv());

            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

            // Decrypt the message and store it in the body of a new Message object
            plain = new String(cipher.doFinal(cipherText), StringUtils.UTF8);
            Message cleartext = new Message();
            cleartext.setBody(plain);
            return cleartext;
        } catch (NoSuchAlgorithmException | InvalidKeyException |
                InvalidAlgorithmParameterException | BadPaddingException |
                NoSuchPaddingException | NoSuchProviderException |
                IllegalBlockSizeException | UnsupportedEncodingException e) {
            LOGGER.log(Level.WARNING, "Decryption failed: " + e.getMessage());
            throw new CryptoFailedException(e);
        }
    }
}
