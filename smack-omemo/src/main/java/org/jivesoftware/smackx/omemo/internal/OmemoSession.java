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
package org.jivesoftware.smackx.omemo.internal;

import org.jivesoftware.smackx.omemo.OmemoStore;
import org.jivesoftware.smackx.omemo.exceptions.CryptoFailedException;
import org.jivesoftware.smackx.omemo.internal.CiphertextTuple;
import org.jivesoftware.smackx.omemo.internal.OmemoDevice;

/**
 * This class represents a OMEMO session between us and another device
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
public abstract class OmemoSession<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess, T_Addr, T_ECPub, T_Bundle, T_Ciph> {
    protected final T_Ciph cipher;
    protected final OmemoStore<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess, T_Addr, T_ECPub, T_Bundle, T_Ciph> omemoStore;
    protected final OmemoDevice remoteDevice;
    protected T_IdKey identityKey;
    protected int preKeyId = -1;

    /**
     * Constructor used when we establish the session
     *
     * @param omemoStore   OmemoStore where we want to store the session and get key information from
     * @param remoteDevice the OmemoDevice we want to establish the session with
     * @param identityKey  identityKey of the recipient
     */
    public OmemoSession(OmemoStore<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess, T_Addr, T_ECPub, T_Bundle, T_Ciph> omemoStore,
                        OmemoDevice remoteDevice, T_IdKey identityKey) {
        this(omemoStore, remoteDevice);
        this.identityKey = identityKey;
    }

    /**
     * Another constructor used when they establish the session with us
     *
     * @param omemoStore   OmemoStore we want to store the session and their key in
     * @param remoteDevice identityKey of the partner
     */
    public OmemoSession(OmemoStore<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess, T_Addr, T_ECPub, T_Bundle, T_Ciph> omemoStore,
                        OmemoDevice remoteDevice) {
        this.omemoStore = omemoStore;
        this.remoteDevice = remoteDevice;
        this.cipher = createCipher(omemoStore, remoteDevice);
    }

    /**
     * Create a new SessionCipher used to encrypt/decrypt keys. The cipher typically implements the ratchet and KDF-chains
     *
     * @param omemoStore OmemoStore
     * @param contact    OmemoDevice
     * @return SessionCipher
     */
    public abstract T_Ciph createCipher(OmemoStore<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess, T_Addr, T_ECPub, T_Bundle, T_Ciph> omemoStore, OmemoDevice contact);

    /**
     * Get the id of the preKey used to establish the session
     *
     * @return id
     */
    public int getPreKeyId() {
        return this.preKeyId;
    }

    /**
     * Encrypt a message key for the recipient. This key can be deciphered by the recipient with its corresponding
     * session cipher. The key is then used to decipher the message.
     *
     * @param messageKey serialized key to encrypt
     * @return A CiphertextTuple containing the ciphertext and the messageType
     */
    public abstract CiphertextTuple encryptMessageKey(byte[] messageKey) throws CryptoFailedException;

    /**
     * Decrypt a messageKey using our sessionCipher. We can use that key to decipher the actual message.
     * Same as encryptMessageKey, just the other way round.
     *
     * @param encryptedKey encrypted key
     * @return serialized decrypted key
     */
    public abstract byte[] decryptMessageKey(byte[] encryptedKey) throws CryptoFailedException;

    /**
     * Return the identityKey of the session
     *
     * @return identityKey
     */
    public T_IdKey getIdentityKey() {
        return identityKey;
    }

    /**
     * Return the fingerprint of the contacts identityKey.
     *
     * @return fingerprint or null (TODO: When is this null and how long?)
     */
    public String getFingerprint() {
        return (this.identityKey != null ? omemoStore.keyUtil().getFingerprint(this.identityKey) : null);
    }
}
