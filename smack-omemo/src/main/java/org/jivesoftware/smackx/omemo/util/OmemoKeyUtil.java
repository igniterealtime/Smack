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

import org.jivesoftware.smackx.omemo.OmemoFingerprint;
import org.jivesoftware.smackx.omemo.OmemoManager;
import org.jivesoftware.smackx.omemo.OmemoStore;
import org.jivesoftware.smackx.omemo.element.OmemoBundleVAxolotlElement;
import org.jivesoftware.smackx.omemo.exceptions.CorruptedOmemoKeyException;
import org.jivesoftware.smackx.omemo.internal.OmemoDevice;
import org.jivesoftware.smackx.omemo.internal.OmemoSession;
import org.jxmpp.stringprep.XmppStringprepException;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Class that is used to convert bytes to keys and vice versa.
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
public abstract class OmemoKeyUtil<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess, T_Addr, T_ECPub, T_Bundle, T_Ciph> {
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
        public T_IdKey identityKey(OmemoBundleVAxolotlElement bundle) throws CorruptedOmemoKeyException {
            return identityKeyFromBytes(bundle.getIdentityKey());
        }

        /**
         * Extract a signedPreKey from an OmemoBundleElement.
         *
         * @param bundle OmemoBundleElement
         * @return singedPreKey
         * @throws CorruptedOmemoKeyException if the key is damaged/malformed
         */
        public T_ECPub signedPreKeyPublic(OmemoBundleVAxolotlElement bundle) throws CorruptedOmemoKeyException {
            return signedPreKeyPublicFromBytes(bundle.getSignedPreKey());
        }

        /**
         * Extract the id of the transported signedPreKey from the bundle.
         *
         * @param bundle OmemoBundleElement
         * @return signedPreKeyId
         */
        public int signedPreKeyId(OmemoBundleVAxolotlElement bundle) {
            return bundle.getSignedPreKeyId();
        }

        /**
         * Extract the signature of the signedPreKey in the bundle as a byte array.
         *
         * @param bundle OmemoBundleElement
         * @return signature
         */
        public byte[] signedPreKeySignature(OmemoBundleVAxolotlElement bundle) {
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
        public T_ECPub preKeyPublic(OmemoBundleVAxolotlElement bundle, int keyId) throws CorruptedOmemoKeyException {
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
        public HashMap<Integer, T_Bundle> bundles(OmemoBundleVAxolotlElement bundle, OmemoDevice contact) throws CorruptedOmemoKeyException {
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
    public abstract HashMap<Integer, T_PreKey> generateOmemoPreKeys(int startId, int count);

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
    public abstract T_Bundle bundleFromOmemoBundle(OmemoBundleVAxolotlElement bundle, OmemoDevice contact, int keyId) throws CorruptedOmemoKeyException;

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
    public HashMap<Integer, byte[]> preKeyPublisKeysForBundle(HashMap<Integer, T_PreKey> preKeyHashMap) {
        HashMap<Integer, byte[]> out = new HashMap<>();
        for (Map.Entry<Integer, T_PreKey> e : preKeyHashMap.entrySet()) {
            out.put(e.getKey(), preKeyForBundle(e.getValue()));
        }
        return out;
    }

    /**
     * Prepare a public signedPreKey for transport in a bundle.
     *
     * @param signedPreKey signedPrekey
     * @return signedPreKey as byte array
     */
    public abstract byte[] signedPreKeyPublicForBundle(T_SigPreKey signedPreKey);

    /**
     * Return the fingerprint of an identityKey.
     *
     * @param identityKey identityKey
     * @return fingerprint of the key
     */
    public abstract OmemoFingerprint getFingerprint(T_IdKey identityKey);

    /**
     * Create a new crypto-specific Session object.
     *
     * @param omemoManager  omemoManager of our device.
     * @param omemoStore    omemoStore where we can save the session, get keys from etc.
     * @param from          the device we want to create the session with.
     * @return a new session
     */
    public abstract OmemoSession<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess, T_Addr, T_ECPub, T_Bundle, T_Ciph>
    createOmemoSession(OmemoManager omemoManager, OmemoStore<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess, T_Addr, T_ECPub, T_Bundle, T_Ciph> omemoStore,
                       OmemoDevice from);

    /**
     * Create a new concrete OmemoSession with a contact.
     *
     * @param omemoManager  omemoManager of our device.
     * @param omemoStore    omemoStore
     * @param device        device to establish the session with
     * @param identityKey   identityKey of the device
     * @return concrete OmemoSession
     */
    public abstract OmemoSession<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess, T_Addr, T_ECPub, T_Bundle, T_Ciph>
    createOmemoSession(OmemoManager omemoManager, OmemoStore<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess, T_Addr, T_ECPub, T_Bundle, T_Ciph> omemoStore,
                       OmemoDevice device, T_IdKey identityKey);

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
     * Convert an OmemoDevice to a crypto-lib specific contact format.
     *
     * @param contact omemoContact
     * @return crypto-lib specific contact object
     */
    public abstract T_Addr omemoDeviceAsAddress(OmemoDevice contact);

    /**
     * Convert a crypto-lib specific contact object into an OmemoDevice.
     *
     * @param address contact
     * @return as OmemoDevice
     * @throws XmppStringprepException if the address is not a valid BareJid
     */
    public abstract OmemoDevice addressAsOmemoDevice(T_Addr address) throws XmppStringprepException;

    public static String prettyFingerprint(OmemoFingerprint fingerprint) {
        return prettyFingerprint(fingerprint.toString());
    }

    /**
     * Split the fingerprint in blocks of 8 characters with spaces between.
     *
     * @param ugly fingerprint as continuous string
     * @return fingerprint with spaces for better readability
     */
    public static String prettyFingerprint(String ugly) {
        if (ugly == null) return null;
        String pretty = "";
        for (int i = 0; i < 8; i++) {
            if (i != 0) pretty += " ";
            pretty += ugly.substring(8 * i, 8 * (i + 1));
        }
        return pretty;
    }

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
