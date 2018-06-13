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
package org.jivesoftware.smackx.omemo.util;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smackx.omemo.element.OmemoBundleElement;
import org.jivesoftware.smackx.omemo.exceptions.CorruptedOmemoKeyException;
import org.jivesoftware.smackx.omemo.internal.OmemoDevice;
import org.jivesoftware.smackx.omemo.trust.OmemoFingerprint;

/**
 * Class that is used to convert bytes to keys and vice versa.
 *
 * @param <T_IdKeyPair> IdentityKeyPair class
 * @param <T_IdKey>     IdentityKey class
 * @param <T_PreKey>    PreKey class
 * @param <T_SigPreKey> SignedPreKey class
 * @param <T_Sess>      Session class
 * @param <T_ECPub>     Elliptic Curve PublicKey class
 * @param <T_Bundle>    Bundle class
 * @author Paul Schaub
 */
public abstract class OmemoKeyUtil<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess, T_ECPub, T_Bundle> {
    private static final Logger LOGGER = Logger.getLogger(OmemoKeyUtil.class.getName());

    public final Bundle BUNDLE = new Bundle();

    /**
     * Bundle related methods.
     */
    public class Bundle {

        /**
         * Extract an IdentityKey from a OmemoBundleElement.
         *
         * @param bundle OmemoBundleElement
         * @return identityKey
         * @throws CorruptedOmemoKeyException if the key is damaged/malformed
         */
        public T_IdKey identityKey(OmemoBundleElement bundle) throws CorruptedOmemoKeyException {
            return identityKeyFromBytes(bundle.getIdentityKey());
        }

        /**
         * Extract a signedPreKey from an OmemoBundleElement.
         *
         * @param bundle OmemoBundleElement
         * @return singedPreKey
         * @throws CorruptedOmemoKeyException if the key is damaged/malformed
         */
        public T_ECPub signedPreKeyPublic(OmemoBundleElement bundle) throws CorruptedOmemoKeyException {
            return signedPreKeyPublicFromBytes(bundle.getSignedPreKey());
        }

        /**
         * Extract the id of the transported signedPreKey from the bundle.
         *
         * @param bundle OmemoBundleElement
         * @return signedPreKeyId
         */
        public int signedPreKeyId(OmemoBundleElement bundle) {
            return bundle.getSignedPreKeyId();
        }

        /**
         * Extract the signature of the signedPreKey in the bundle as a byte array.
         *
         * @param bundle OmemoBundleElement
         * @return signature
         */
        public byte[] signedPreKeySignature(OmemoBundleElement bundle) {
            return bundle.getSignedPreKeySignature();
        }

        /**
         * Extract the preKey with id 'keyId' from the bundle.
         *
         * @param bundle OmemoBundleElement
         * @param keyId  id of the preKey
         * @return the preKey
         * @throws CorruptedOmemoKeyException when the key cannot be parsed from bytes
         */
        public T_ECPub preKeyPublic(OmemoBundleElement bundle, int keyId) throws CorruptedOmemoKeyException {
            return preKeyPublicFromBytes(bundle.getPreKey(keyId));
        }

        /**
         * Break up the OmemoBundleElement into a list of crypto-lib specific bundles (T_PreKey).
         * In case of the signal library, we break the OmemoBundleElement in ~100 PreKeyBundles (one for every transported
         * preKey).
         *
         * @param bundle  OmemoBundleElement containing multiple PreKeys
         * @param contact Contact that the bundle belongs to
         * @return a HashMap with one T_Bundle per preKey and the preKeyId as key
         * @throws CorruptedOmemoKeyException when one of the keys cannot be parsed
         */
        public HashMap<Integer, T_Bundle> bundles(OmemoBundleElement bundle, OmemoDevice contact) throws CorruptedOmemoKeyException {
            HashMap<Integer, T_Bundle> bundles = new HashMap<>();
            for (int deviceId : bundle.getPreKeys().keySet()) {
                try {
                    bundles.put(deviceId, bundleFromOmemoBundle(bundle, contact, deviceId));
                } catch (CorruptedOmemoKeyException e) {
                    LOGGER.log(Level.INFO, "Cannot parse PreKeyBundle: " + e.getMessage());
                }
            }
            if (bundles.size() == 0) {
                throw new CorruptedOmemoKeyException("Bundle contained no valid preKeys.");
            }
            return bundles;
        }
    }

    /**
     * Deserialize an identityKeyPair from a byte array.
     *
     * @param data byte array
     * @return IdentityKeyPair (T_IdKeyPair)
     * @throws CorruptedOmemoKeyException if the key is damaged of malformed
     */
    public abstract T_IdKeyPair identityKeyPairFromBytes(byte[] data) throws CorruptedOmemoKeyException;

    /**
     * Deserialize an identityKey from a byte array.
     *
     * @param data byte array
     * @return identityKey (T_IdKey)
     * @throws CorruptedOmemoKeyException if the key is damaged or malformed
     */
    public abstract T_IdKey identityKeyFromBytes(byte[] data) throws CorruptedOmemoKeyException;

    /**
     * Serialize an identityKey into bytes.
     *
     * @param identityKey idKey
     * @return bytes
     */
    public abstract byte[] identityKeyToBytes(T_IdKey identityKey);

    /**
     * Deserialize an elliptic curve public key from bytes.
     *
     * @param data bytes
     * @return elliptic curve public key (T_ECPub)
     * @throws CorruptedOmemoKeyException if the key is damaged or malformed
     */
    public abstract T_ECPub ellipticCurvePublicKeyFromBytes(byte[] data) throws CorruptedOmemoKeyException;

    /**
     * Deserialize a public preKey from bytes.
     *
     * @param data preKey as bytes
     * @return preKey
     * @throws CorruptedOmemoKeyException if the key is damaged or malformed
     */
    public T_ECPub preKeyPublicFromBytes(byte[] data) throws CorruptedOmemoKeyException {
        return ellipticCurvePublicKeyFromBytes(data);
    }

    /**
     * Serialize a preKey into a byte array.
     *
     * @param preKey preKey
     * @return byte[]
     */
    public abstract byte[] preKeyToBytes(T_PreKey preKey);

    /**
     * Deserialize a preKey from a byte array.
     *
     * @param bytes byte array
     * @return deserialized preKey
     * @throws IOException when something goes wrong
     */
    public abstract T_PreKey preKeyFromBytes(byte[] bytes) throws IOException;

    /**
     * Generate 'count' new PreKeys beginning with id 'startId'.
     * These preKeys are published and can be used by contacts to establish sessions with us.
     *
     * @param startId start id
     * @param count   how many keys do we want to generate
     * @return Map of new preKeys
     */
    public abstract TreeMap<Integer, T_PreKey> generateOmemoPreKeys(int startId, int count);

    /**
     * Generate a new signed preKey.
     *
     * @param identityKeyPair identityKeyPair used to sign the preKey
     * @param signedPreKeyId  id that the preKey will have
     * @return signedPreKey
     * @throws CorruptedOmemoKeyException when the identityKeyPair is invalid
     */
    public abstract T_SigPreKey generateOmemoSignedPreKey(T_IdKeyPair identityKeyPair, int signedPreKeyId) throws CorruptedOmemoKeyException;


    /**
     * Deserialize a public signedPreKey from bytes.
     *
     * @param data bytes
     * @return signedPreKey
     * @throws CorruptedOmemoKeyException if the key is damaged or malformed
     */
    public T_ECPub signedPreKeyPublicFromBytes(byte[] data) throws CorruptedOmemoKeyException {
        return ellipticCurvePublicKeyFromBytes(data);
    }

    /**
     * Deserialize a signedPreKey from a byte array.
     *
     * @param data byte array
     * @return deserialized signed preKey
     * @throws IOException when something goes wrong
     */
    public abstract T_SigPreKey signedPreKeyFromBytes(byte[] data) throws IOException;

    /**
     * Serialize a signedPreKey into a byte array.
     *
     * @param sigPreKey signedPreKey
     * @return byte array
     */
    public abstract byte[] signedPreKeyToBytes(T_SigPreKey sigPreKey);

    /**
     * Build a crypto-lib specific PreKeyBundle (T_Bundle) using a PreKey from the OmemoBundleElement 'bundle'.
     * The PreKeyBundle will contain the identityKey, signedPreKey and signature, as well as a preKey
     * from the OmemoBundleElement.
     *
     * @param bundle  OmemoBundleElement
     * @param contact Contact that the bundle belongs to
     * @param keyId   id of the preKey that will be selected from the OmemoBundleElement and that the PreKeyBundle will contain
     * @return PreKeyBundle (T_PreKey)
     * @throws CorruptedOmemoKeyException if some key is damaged or malformed
     */
    public abstract T_Bundle bundleFromOmemoBundle(OmemoBundleElement bundle, OmemoDevice contact, int keyId) throws CorruptedOmemoKeyException;

    /**
     * Extract the signature from a signedPreKey.
     *
     * @param signedPreKey signedPreKey
     * @return signature as byte array
     */
    public abstract byte[] signedPreKeySignatureFromKey(T_SigPreKey signedPreKey);

    /**
     * Generate a new IdentityKeyPair. We should always have only one pair and usually keep this for a long time.
     *
     * @return identityKeyPair
     */
    public abstract T_IdKeyPair generateOmemoIdentityKeyPair();

    /**
     * return the id of the given signedPreKey.
     *
     * @param signedPreKey key
     * @return id of the key
     */
    public abstract int signedPreKeyIdFromKey(T_SigPreKey signedPreKey);

    /**
     * serialize an identityKeyPair into bytes.
     *
     * @param identityKeyPair identityKeyPair
     * @return byte array
     */
    public abstract byte[] identityKeyPairToBytes(T_IdKeyPair identityKeyPair);

    /**
     * Extract the public identityKey from an identityKeyPair.
     *
     * @param pair keyPair
     * @return public key of the pair
     */
    public abstract T_IdKey identityKeyFromPair(T_IdKeyPair pair);

    /**
     * Prepare an identityKey for transport in an OmemoBundleElement (serialize it).
     *
     * @param identityKey identityKey that will be transported
     * @return key as byte array
     */
    public abstract byte[] identityKeyForBundle(T_IdKey identityKey);

    /**
     * Prepare an elliptic curve preKey for transport in an OmemoBundleElement.
     *
     * @param preKey key
     * @return key as byte array
     */
    public abstract byte[] preKeyPublicKeyForBundle(T_ECPub preKey);

    /**
     * Prepare a preKey for transport in an OmemoBundleElement.
     *
     * @param preKey preKey
     * @return key as byte array
     */
    public abstract byte[] preKeyForBundle(T_PreKey preKey);

    /**
     * Prepare a whole bunche of preKeys for transport.
     *
     * @param preKeyHashMap HashMap of preKeys
     * @return HashMap of byte arrays but with the same keyIds as key
     */
    public HashMap<Integer, byte[]> preKeyPublicKeysForBundle(TreeMap<Integer, T_PreKey> preKeyHashMap) {
        HashMap<Integer, byte[]> out = new HashMap<>();
        for (Map.Entry<Integer, T_PreKey> e : preKeyHashMap.entrySet()) {
            out.put(e.getKey(), preKeyForBundle(e.getValue()));
        }
        return out;
    }

    /**
     * Prepare a public signedPreKey for transport in a bundle.
     *
     * @param signedPreKey signedPreKey
     * @return signedPreKey as byte array
     */
    public abstract byte[] signedPreKeyPublicForBundle(T_SigPreKey signedPreKey);

    /**
     * Return the fingerprint of an identityKey.
     *
     * @param identityKey identityKey
     * @return fingerprint of the key
     */
    public abstract OmemoFingerprint getFingerprintOfIdentityKey(T_IdKey identityKey);

    /**
     * Returns the fingerprint of the public key of an identityKeyPair.
     * @param identityKeyPair IdentityKeyPair.
     * @return fingerprint of the public key.
     */
    public abstract OmemoFingerprint getFingerprintOfIdentityKeyPair(T_IdKeyPair identityKeyPair);

    /**
     * Deserialize a raw OMEMO Session from bytes.
     *
     * @param data bytes
     * @return raw OMEMO Session
     * @throws IOException when something goes wrong
     */
    public abstract T_Sess rawSessionFromBytes(byte[] data) throws IOException;

    /**
     * Serialize a raw OMEMO session into a byte array.
     *
     * @param session raw session
     * @return byte array
     */
    public abstract byte[] rawSessionToBytes(T_Sess session);

    /**
     * Add integers modulo MAX_VALUE.
     *
     * @param value base integer
     * @param added value that is added to the base value
     * @return (value plus added) modulo Integer.MAX_VALUE
     */
    public static int addInBounds(int value, int added) {
        int avail = Integer.MAX_VALUE - value;
        if (avail < added) {
            return added - avail;
        } else {
            return value + added;
        }
    }
}
