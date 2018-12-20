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

import static org.jivesoftware.smackx.omemo.util.OmemoConstants.PRE_KEY_COUNT_PER_BUNDLE;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smackx.omemo.element.OmemoBundleElement_VAxolotl;
import org.jivesoftware.smackx.omemo.element.OmemoDeviceListElement;
import org.jivesoftware.smackx.omemo.exceptions.CannotEstablishOmemoSessionException;
import org.jivesoftware.smackx.omemo.exceptions.CorruptedOmemoKeyException;
import org.jivesoftware.smackx.omemo.exceptions.NoIdentityKeyException;
import org.jivesoftware.smackx.omemo.internal.OmemoCachedDeviceList;
import org.jivesoftware.smackx.omemo.internal.OmemoDevice;
import org.jivesoftware.smackx.omemo.trust.OmemoFingerprint;
import org.jivesoftware.smackx.omemo.util.OmemoKeyUtil;

import org.jxmpp.jid.BareJid;

/**
 * Class that presents some methods that are used to load/generate/store keys and session data needed for OMEMO.
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
public abstract class OmemoStore<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess, T_Addr, T_ECPub, T_Bundle, T_Ciph> {
    private static final Logger LOGGER = Logger.getLogger(OmemoStore.class.getName());

    /**
     * Create a new OmemoStore.
     */
    public OmemoStore() {

    }

    /**
     * Returns a sorted set of all the deviceIds, the localUser has had data stored under in the store.
     * Basically this returns the deviceIds of all "accounts" of localUser, which are known to the store.
     * @param localUser BareJid of the user.
     * @return set of deviceIds with available data.
     */
    public abstract SortedSet<Integer> localDeviceIdsOf(BareJid localUser);

    /**
     * Check, if our freshly generated deviceId is available (unique) in our deviceList.
     *
     * @param userDevice    our current device.
     * @param id            deviceId to check for.
     * @return true if list did not contain our id, else false
     */
    boolean isAvailableDeviceId(OmemoDevice userDevice, int id) {
        LOGGER.log(Level.INFO, "Check if id " + id + " is available...");

        // Lookup local cached device list
        BareJid ownJid = userDevice.getJid();
        OmemoCachedDeviceList cachedDeviceList;

        cachedDeviceList = loadCachedDeviceList(userDevice, ownJid);

        if (cachedDeviceList == null) {
            cachedDeviceList = new OmemoCachedDeviceList();
        }
        // Does the list already contain that id?
        return !cachedDeviceList.contains(id);
    }

    /**
     * Merge the received OmemoDeviceListElement with the one we already have. If we had none, the received one is saved.
     *
     * @param userDevice our OmemoDevice.
     * @param contact Contact we received the list from.
     * @param list    List we received.
     */
    OmemoCachedDeviceList mergeCachedDeviceList(OmemoDevice userDevice, BareJid contact, OmemoDeviceListElement list) {
        OmemoCachedDeviceList cached = loadCachedDeviceList(userDevice, contact);

        if (cached == null) {
            cached = new OmemoCachedDeviceList();
        }

        if (list == null) {
            return cached;
        }

        for (int devId : list.getDeviceIds()) {
            if (!cached.contains(devId)) {
                setDateOfLastDeviceIdPublication(userDevice, new OmemoDevice(contact, devId), new Date());
            }
        }

        cached.merge(list.getDeviceIds());
        storeCachedDeviceList(userDevice, contact, cached);

        return cached;
    }

    /**
     * Renew our singed preKey. This should be done once every 7-14 days.
     * The old signed PreKey should be kept for around a month or so (look it up in the XEP).
     *
     * @param userDevice our OmemoDevice.
     * @throws CorruptedOmemoKeyException when our identityKey is invalid.
     * @throws IllegalStateException when our IdentityKeyPair is null.
     */
    void changeSignedPreKey(OmemoDevice userDevice)
            throws CorruptedOmemoKeyException {

        T_IdKeyPair idKeyPair = loadOmemoIdentityKeyPair(userDevice);
        if (idKeyPair == null) {
            throw new IllegalStateException("Our IdentityKeyPair is null.");
        }

        TreeMap<Integer, T_SigPreKey> signedPreKeys = loadOmemoSignedPreKeys(userDevice);
        if (signedPreKeys.size() == 0) {
            T_SigPreKey newKey = generateOmemoSignedPreKey(idKeyPair, 1);
            storeOmemoSignedPreKey(userDevice, 1, newKey);
        } else {
            int lastId = signedPreKeys.lastKey();
            T_SigPreKey newKey = generateOmemoSignedPreKey(idKeyPair, lastId + 1);
            storeOmemoSignedPreKey(userDevice, lastId + 1, newKey);
        }

        setDateOfLastSignedPreKeyRenewal(userDevice, new Date());
        removeOldSignedPreKeys(userDevice);
    }

    /**
     * Remove the oldest signedPreKey until there are only MAX_NUMBER_OF_STORED_SIGNED_PREKEYS left.
     *
     * @param userDevice our OmemoDevice.
     */
    private void removeOldSignedPreKeys(OmemoDevice userDevice) {
        if (OmemoConfiguration.getMaxNumberOfStoredSignedPreKeys() <= 0) {
            return;
        }

        TreeMap<Integer, T_SigPreKey> signedPreKeys = loadOmemoSignedPreKeys(userDevice);

        for (int i = 0; i < signedPreKeys.keySet().size() - OmemoConfiguration.getMaxNumberOfStoredSignedPreKeys(); i++) {
            int keyId = signedPreKeys.firstKey();
            LOGGER.log(Level.INFO, "Remove signedPreKey " + keyId + ".");
            removeOmemoSignedPreKey(userDevice, i);
            signedPreKeys = loadOmemoSignedPreKeys(userDevice);
        }
    }

    /**
     * Pack a OmemoBundleElement containing our key material.
     *
     * @param userDevice our OmemoDevice.
     * @return OmemoBundleElement
     * @throws CorruptedOmemoKeyException when a key could not be loaded
     */
    OmemoBundleElement_VAxolotl packOmemoBundle(OmemoDevice userDevice)
            throws CorruptedOmemoKeyException {

        int currentSignedPreKeyId = loadCurrentOmemoSignedPreKeyId(userDevice);
        T_SigPreKey currentSignedPreKey = loadOmemoSignedPreKeys(userDevice).get(currentSignedPreKeyId);

        return new OmemoBundleElement_VAxolotl(
                currentSignedPreKeyId,
                keyUtil().signedPreKeyPublicForBundle(currentSignedPreKey),
                keyUtil().signedPreKeySignatureFromKey(currentSignedPreKey),
                keyUtil().identityKeyForBundle(keyUtil().identityKeyFromPair(loadOmemoIdentityKeyPair(userDevice))),
                keyUtil().preKeyPublicKeysForBundle(loadOmemoPreKeys(userDevice))
        );
    }

    /**
     * Replenish our supply of keys. If we are missing any type of keys, generate them fresh.
     * @param userDevice
     * @throws CorruptedOmemoKeyException
     */
    public void replenishKeys(OmemoDevice userDevice)
            throws CorruptedOmemoKeyException {

        T_IdKeyPair identityKeyPair = loadOmemoIdentityKeyPair(userDevice);
        if (identityKeyPair == null) {
            identityKeyPair = generateOmemoIdentityKeyPair();
            storeOmemoIdentityKeyPair(userDevice, identityKeyPair);
        }

        TreeMap<Integer, T_SigPreKey> signedPreKeys = loadOmemoSignedPreKeys(userDevice);
        if (signedPreKeys.size() == 0) {
            changeSignedPreKey(userDevice);
        }

        TreeMap<Integer, T_PreKey> preKeys = loadOmemoPreKeys(userDevice);
        int newKeysCount = PRE_KEY_COUNT_PER_BUNDLE - preKeys.size();
        int startId = preKeys.size() == 0 ? 0 : preKeys.lastKey();

        if (newKeysCount > 0) {
            TreeMap<Integer, T_PreKey> newKeys = generateOmemoPreKeys(startId + 1, newKeysCount);
            storeOmemoPreKeys(userDevice, newKeys);
        }
    }

    // *sigh*

    /**
     * Generate a new IdentityKeyPair. We should always have only one pair and usually keep this for a long time.
     *
     * @return identityKeyPair
     */
    public T_IdKeyPair generateOmemoIdentityKeyPair() {
        return keyUtil().generateOmemoIdentityKeyPair();
    }

    /**
     * Load our identityKeyPair from storage.
     * Return null, if we have no identityKeyPair.
     *
     * @param userDevice our OmemoDevice.
     * @return identityKeyPair
     * @throws CorruptedOmemoKeyException Thrown, if the stored key is damaged (*hands up* not my fault!)
     */
    public abstract T_IdKeyPair loadOmemoIdentityKeyPair(OmemoDevice userDevice)
            throws CorruptedOmemoKeyException;

    /**
     * Store our identityKeyPair in storage. It would be a cool feature, if the key could be stored in a encrypted
     * database or something similar.
     *
     * @param userDevice our OmemoDevice.
     * @param identityKeyPair identityKeyPair
     */
    public abstract void storeOmemoIdentityKeyPair(OmemoDevice userDevice, T_IdKeyPair identityKeyPair);

    /**
     * Remove the identityKeyPair of a user.
     * @param userDevice our device.
     */
    public abstract void removeOmemoIdentityKeyPair(OmemoDevice userDevice);

    /**
     * Load the public identityKey of a device.
     *
     * @param userDevice our OmemoDevice.
     * @param contactsDevice the device of which we want to load the identityKey.
     * @return identityKey
     * @throws CorruptedOmemoKeyException when the key in question is corrupted and cant be deserialized.
     */
    public abstract T_IdKey loadOmemoIdentityKey(OmemoDevice userDevice, OmemoDevice contactsDevice)
            throws CorruptedOmemoKeyException;

    /**
     * Store the public identityKey of the device.
     *
     * @param userDevice our OmemoDevice.
     * @param contactsDevice device.
     * @param contactsKey    identityKey belonging to the contactsDevice.
     */
    public abstract void storeOmemoIdentityKey(OmemoDevice userDevice, OmemoDevice contactsDevice, T_IdKey contactsKey);

    /**
     * Removes the identityKey of a device.
     *
     * @param userDevice our omemoDevice.
     * @param contactsDevice device of which we want to delete the identityKey.
     */
    public abstract void removeOmemoIdentityKey(OmemoDevice userDevice, OmemoDevice contactsDevice);

    /**
     * Store the number of messages we sent to a device since we last received a message back.
     * This counter gets reset to 0 whenever we receive a message from the contacts device.
     *
     * @param userDevice our omemoDevice.
     * @param contactsDevice device of which we want to set the message counter.
     * @param counter counter value.
     */
    public abstract void storeOmemoMessageCounter(OmemoDevice userDevice, OmemoDevice contactsDevice, int counter);

    /**
     * Return the current value of the message counter.
     * This counter represents the number of message we sent to the contactsDevice without getting a reply back.
     * The default value for this counter is 0.
     *
     * @param userDevice our omemoDevice
     * @param contactsDevice device of which we want to get the message counter.
     * @return counter value.
     */
    public abstract int loadOmemoMessageCounter(OmemoDevice userDevice, OmemoDevice contactsDevice);

    /**
     * Set the date of the last message that was received from a device.
     *
     * @param userDevice omemoManager of our device.
     * @param contactsDevice device in question
     * @param date date of the last received message
     */
    public abstract void setDateOfLastReceivedMessage(OmemoDevice userDevice, OmemoDevice contactsDevice, Date date);

    /**
     * Return the date of the last message that was received from device 'from'.
     *
     * @param userDevice our OmemoDevice.
     * @param contactsDevice device in question
     * @return date if existent, null
     */
    public abstract Date getDateOfLastReceivedMessage(OmemoDevice userDevice, OmemoDevice contactsDevice);

    /**
     * Set the date of the last time the deviceId was published. This method only gets called, when the deviceId
     * was inactive/non-existent before it was published.
     *
     * @param userDevice our OmemoDevice
     * @param contactsDevice OmemoDevice in question
     * @param date date of the last publication after not being published
     */
    public abstract void setDateOfLastDeviceIdPublication(OmemoDevice userDevice, OmemoDevice contactsDevice, Date date);

    /**
     * Return the date of the last time the deviceId was published after previously being not published.
     * (Point in time, where the status of the deviceId changed from inactive/non-existent to active).
     *
     * @param userDevice our OmemoDevice
     * @param contactsDevice OmemoDevice in question
     * @return date of the last publication after not being published
     */
    public abstract Date getDateOfLastDeviceIdPublication(OmemoDevice userDevice, OmemoDevice contactsDevice);

    /**
     * Set the date of the last time the signed preKey was renewed.
     *
     * @param userDevice our OmemoDevice.
     * @param date date
     */
    public abstract void setDateOfLastSignedPreKeyRenewal(OmemoDevice userDevice, Date date);

    /**
     * Get the date of the last time the signed preKey was renewed.
     *
     * @param userDevice our OmemoDevice.
     * @return date if existent, otherwise null
     */
    public abstract Date getDateOfLastSignedPreKeyRenewal(OmemoDevice userDevice);

    /**
     * Generate 'count' new PreKeys beginning with id 'startId'.
     * These preKeys are published and can be used by contacts to establish sessions with us.
     *
     * @param startId start id
     * @param count   how many keys do we want to generate
     * @return Map of new preKeys
     */
    public TreeMap<Integer, T_PreKey> generateOmemoPreKeys(int startId, int count) {
        return keyUtil().generateOmemoPreKeys(startId, count);
    }

    /**
     * Load the preKey with id 'preKeyId' from storage.
     *
     * @param userDevice our OmemoDevice.
     * @param preKeyId id of the key to be loaded
     * @return loaded preKey
     */
    public abstract T_PreKey loadOmemoPreKey(OmemoDevice userDevice, int preKeyId);

    /**
     * Store a PreKey in storage.
     *
     * @param userDevice our OmemoDevice.
     * @param preKeyId id of the key
     * @param preKey   key
     */
    public abstract void storeOmemoPreKey(OmemoDevice userDevice, int preKeyId, T_PreKey preKey);

    /**
     * Store a whole bunch of preKeys.
     *
     * @param userDevice our OmemoDevice.
     * @param preKeyHashMap HashMap of preKeys
     */
    public void storeOmemoPreKeys(OmemoDevice userDevice, TreeMap<Integer, T_PreKey> preKeyHashMap) {
        for (Map.Entry<Integer, T_PreKey> entry : preKeyHashMap.entrySet()) {
            storeOmemoPreKey(userDevice, entry.getKey(), entry.getValue());
        }
    }

    /**
     * Remove a preKey from storage. This is called, when a contact used one of our preKeys to establish a session
     * with us.
     *
     * @param userDevice our OmemoDevice.
     * @param preKeyId id of the used key that will be deleted
     */
    public abstract void removeOmemoPreKey(OmemoDevice userDevice, int preKeyId);

    /**
     * Return all our current OmemoPreKeys.
     *
     * @param userDevice our OmemoDevice.
     * @return Map containing our preKeys
     */
    public abstract TreeMap<Integer, T_PreKey> loadOmemoPreKeys(OmemoDevice userDevice);

    /**
     * Return the signedPreKey with the id 'singedPreKeyId'.
     *
     * @param userDevice our OmemoDevice.
     * @param signedPreKeyId id of the key
     * @return key
     */
    public abstract T_SigPreKey loadOmemoSignedPreKey(OmemoDevice userDevice, int signedPreKeyId);

    public int loadCurrentOmemoSignedPreKeyId(OmemoDevice userDevice) {
        return loadOmemoSignedPreKeys(userDevice).lastKey();
    }

    /**
     * Load all our signed PreKeys.
     *
     * @param userDevice our OmemoDevice.
     * @return HashMap of our singedPreKeys
     */
    public abstract TreeMap<Integer, T_SigPreKey> loadOmemoSignedPreKeys(OmemoDevice userDevice);

    /**
     * Generate a new signed preKey.
     *
     * @param identityKeyPair identityKeyPair used to sign the preKey
     * @param signedPreKeyId  id that the preKey will have
     * @return signedPreKey
     * @throws CorruptedOmemoKeyException when something goes wrong
     */
    public T_SigPreKey generateOmemoSignedPreKey(T_IdKeyPair identityKeyPair, int signedPreKeyId)
            throws CorruptedOmemoKeyException {
        return keyUtil().generateOmemoSignedPreKey(identityKeyPair, signedPreKeyId);
    }

    /**
     * Store a signedPreKey in storage.
     *
     * @param userDevice our OmemoDevice.
     * @param signedPreKeyId id of the signedPreKey
     * @param signedPreKey   the key itself
     */
    public abstract void storeOmemoSignedPreKey(OmemoDevice userDevice, int signedPreKeyId, T_SigPreKey signedPreKey);

    /**
     * Remove a signedPreKey from storage.
     *
     * @param userDevice our OmemoDevice.
     * @param signedPreKeyId id of the key that will be removed
     */
    public abstract void removeOmemoSignedPreKey(OmemoDevice userDevice, int signedPreKeyId);

    /**
     * Load the crypto-lib specific session object of the device from storage.
     *
     * @param userDevice our OmemoDevice.
     * @param contactsDevice device whose session we want to load
     * @return crypto related session
     */
    public abstract T_Sess loadRawSession(OmemoDevice userDevice, OmemoDevice contactsDevice);

    /**
     * Load all crypto-lib specific session objects of contact 'contact'.
     *
     * @param userDevice our OmemoDevice.
     * @param contact BareJid of the contact we want to get all sessions from
     * @return TreeMap of deviceId and sessions of the contact
     */
    public abstract HashMap<Integer, T_Sess> loadAllRawSessionsOf(OmemoDevice userDevice, BareJid contact);

    /**
     * Store a crypto-lib specific session to storage.
     *
     * @param userDevice our OmemoDevice.
     * @param contactsDevice  OmemoDevice whose session we want to store
     * @param session session
     */
    public abstract void storeRawSession(OmemoDevice userDevice, OmemoDevice contactsDevice, T_Sess session);

    /**
     * Remove a crypto-lib specific session from storage.
     *
     * @param userDevice our OmemoDevice.
     * @param contactsDevice device whose session we want to delete
     */
    public abstract void removeRawSession(OmemoDevice userDevice, OmemoDevice contactsDevice);

    /**
     * Remove all crypto-lib specific session of a contact.
     *
     * @param userDevice our OmemoDevice.
     * @param contact BareJid of the contact
     */
    public abstract void removeAllRawSessionsOf(OmemoDevice userDevice, BareJid contact);

    /**
     * Return true, if we have a session with the device, otherwise false.
     * Hint for Signal: Do not try 'return getSession() != null' since this will create a new session.
     *
     * @param userDevice our OmemoDevice.
     * @param contactsDevice device
     * @return true if we have session, otherwise false
     */
    public abstract boolean containsRawSession(OmemoDevice userDevice, OmemoDevice contactsDevice);

    /**
     * Load a list of deviceIds from contact 'contact' from the local cache.
     *
     * @param userDevice our OmemoDevice.
     * @param contact contact we want to get the deviceList of
     * @return CachedDeviceList of the contact
     */
    public abstract OmemoCachedDeviceList loadCachedDeviceList(OmemoDevice userDevice, BareJid contact);

    /**
     * Load a list of deviceIds from our own devices.
     * @param userDevice
     * @return
     */
    public OmemoCachedDeviceList loadCachedDeviceList(OmemoDevice userDevice) {
        return loadCachedDeviceList(userDevice, userDevice.getJid());
    }

    /**
     * Store the DeviceList of the contact in local storage.
     * See this as a cache.
     *
     * @param userDevice our OmemoDevice.
     * @param contact    Contact
     * @param contactsDeviceList list of the contacts devices' ids.
     */
    public abstract void storeCachedDeviceList(OmemoDevice userDevice,
                                               BareJid contact,
                                               OmemoCachedDeviceList contactsDeviceList);

    /**
     * Delete this device's IdentityKey, PreKeys, SignedPreKeys and Sessions.
     *
     * @param userDevice our OmemoDevice.
     */
    public abstract void purgeOwnDeviceKeys(OmemoDevice userDevice);

    /**
     * Return a concrete KeyUtil object that we can use as a utility to create keys etc.
     *
     * @return KeyUtil object
     */
    public abstract OmemoKeyUtil<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess, T_ECPub, T_Bundle> keyUtil();

    /**
     * Return our identityKeys fingerprint.
     *
     * @param userDevice our OmemoDevice.
     * @return fingerprint of our identityKeyPair
     *
     * @throws CorruptedOmemoKeyException if the identityKey of userDevice is corrupted.
     */
    public OmemoFingerprint getFingerprint(OmemoDevice userDevice)
            throws CorruptedOmemoKeyException {

        T_IdKeyPair keyPair = loadOmemoIdentityKeyPair(userDevice);
        if (keyPair == null) {
            return null;
        }

        return keyUtil().getFingerprintOfIdentityKey(keyUtil().identityKeyFromPair(keyPair));
    }

    /**
     * Return the fingerprint of the identityKey belonging to contactsDevice.
     *
     * @param userDevice our OmemoDevice.
     * @param contactsDevice OmemoDevice we want to have the fingerprint for.
     * @return fingerprint of the userDevices IdentityKey.
     * @throws CorruptedOmemoKeyException if the IdentityKey is corrupted.
     * @throws NoIdentityKeyException if no IdentityKey for contactsDevice has been found locally.
     */
    public OmemoFingerprint getFingerprint(OmemoDevice userDevice, OmemoDevice contactsDevice)
            throws CorruptedOmemoKeyException, NoIdentityKeyException {

        T_IdKey identityKey = loadOmemoIdentityKey(userDevice, contactsDevice);
        if (identityKey == null) {
            throw new NoIdentityKeyException(contactsDevice);
        }
        return keyUtil().getFingerprintOfIdentityKey(identityKey);
    }

    /**
     * Return the fingerprint of the given devices announced identityKey.
     * If we have no local copy of the identityKey of the contact, build a fresh session in order to get the key.
     *
     * @param managerGuard authenticated OmemoManager
     * @param contactsDevice OmemoDevice we want to get the fingerprint from
     * @return fingerprint
     *
     * @throws CannotEstablishOmemoSessionException If we have no local copy of the identityKey of the contact
     *                                              and are unable to build a fresh session
     * @throws CorruptedOmemoKeyException           If the identityKey we have of the contact is corrupted
     * @throws SmackException.NotConnectedException
     * @throws InterruptedException
     * @throws SmackException.NoResponseException
     */
    public OmemoFingerprint getFingerprintAndMaybeBuildSession(OmemoManager.LoggedInOmemoManager managerGuard, OmemoDevice contactsDevice)
            throws CannotEstablishOmemoSessionException, CorruptedOmemoKeyException,
            SmackException.NotConnectedException, InterruptedException, SmackException.NoResponseException {
        OmemoManager omemoManager = managerGuard.get();

        // Load identityKey
        T_IdKey identityKey = loadOmemoIdentityKey(omemoManager.getOwnDevice(), contactsDevice);
        if (identityKey == null) {
            // Key cannot be loaded. Maybe it doesn't exist. Fetch a bundle to get it...
            OmemoService.getInstance().buildFreshSessionWithDevice(omemoManager.getConnection(),
                    omemoManager.getOwnDevice(), contactsDevice);
        }

        // Load identityKey again
        identityKey = loadOmemoIdentityKey(omemoManager.getOwnDevice(), contactsDevice);
        if (identityKey == null) {
            return null;
        }

        return keyUtil().getFingerprintOfIdentityKey(identityKey);
    }
}
