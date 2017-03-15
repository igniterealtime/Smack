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
package org.jivesoftware.smackx.omemo;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.roster.Roster;
import org.jivesoftware.smack.roster.RosterEntry;
import org.jivesoftware.smackx.omemo.elements.OmemoBundleElement;
import org.jivesoftware.smackx.omemo.elements.OmemoDeviceListElement;
import org.jivesoftware.smackx.omemo.internal.OmemoSession;
import org.jivesoftware.smackx.omemo.internal.CachedDeviceList;
import org.jivesoftware.smackx.omemo.exceptions.InvalidOmemoKeyException;
import org.jivesoftware.smackx.omemo.util.KeyUtil;
import org.jivesoftware.smackx.omemo.internal.OmemoDevice;
import org.jivesoftware.smackx.pubsub.LeafNode;
import org.jxmpp.jid.BareJid;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

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
     * How many preKeys do we want to publish?
     */
    public static final int TARGET_PRE_KEY_COUNT = 100;

    protected final OmemoManager omemoManager;
    protected OmemoService<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess, T_Addr, T_ECPub, T_Bundle, T_Ciph> omemoService;

    protected HashMap<OmemoDevice, OmemoSession<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess, T_Addr, T_ECPub, T_Bundle, T_Ciph>>
            omemoSessions;
    protected LeafNode ownDeviceListNode;

    /**
     * Create a new OmemoStore
     *
     * @param manager OmemoManager that we want to serve
     */
    public OmemoStore(OmemoManager manager) {
        this.omemoManager = manager;
    }

    /**
     * Return true if this is a fresh installation.
     *
     * @return true or false.
     */
    public abstract boolean isFreshInstallation();

    /**
     * Generate a new Identity (deviceId, identityKeys, preKeys...)
     *
     * @throws InvalidOmemoKeyException in case something goes wrong
     */
    void regenerate() throws InvalidOmemoKeyException {
        LOGGER.log(Level.INFO, "Regenerating...");
        //TODO: Flush complete store

        int nextPreKeyId = 1;
        storeOmemoIdentityKeyPair(generateOmemoIdentityKeyPair());
        storeOmemoPreKeys(generateOmemoPreKeys(nextPreKeyId, TARGET_PRE_KEY_COUNT));
        storeLastPreKeyId(keyUtil().addInBounds(nextPreKeyId, TARGET_PRE_KEY_COUNT));
        storeCurrentSignedPreKeyId(-1); //Set back to no-value default

        changeSignedPreKey();
    }

    /**
     * Preload all OMEMO sessions for our devices and our contacts
     */
    public void initializeOmemoSessions() {
        BareJid ownJid = omemoManager.getConnection().getUser().asBareJid();
        HashMap<Integer, T_Sess> ourDevices = loadAllRawSessionsOf(ownJid);
        ourDevices.remove(loadOmemoDeviceId());
        this.omemoSessions = new HashMap<>();
        this.omemoSessions.putAll(buildOmemoSessionsFor(ownJid, ourDevices));
        for (RosterEntry rosterEntry : Roster.getInstanceFor(omemoManager.getConnection()).getEntries()) {
            HashMap<Integer, T_Sess> contactDevices = loadAllRawSessionsOf(rosterEntry.getJid().asBareJid());
            this.omemoSessions.putAll(buildOmemoSessionsFor(rosterEntry.getJid().asBareJid(), contactDevices));
        }
    }

    /**
     * Check, if our freshly generated deviceId is available (unique) in our deviceList
     *
     * @param id our deviceId
     * @return true if list did not contain our id, else false
     */
    boolean isAvailableDeviceId(int id) {
        LOGGER.log(Level.INFO, "Check if id " + id + " is available...");

        //Lookup local cached device list
        CachedDeviceList cachedDeviceList = loadCachedDeviceList(omemoManager.getConnection().getUser().asBareJid());
        if (cachedDeviceList == null) {
            cachedDeviceList = new CachedDeviceList();
        }
        if (cachedDeviceList.contains(id)) {
            return false;
        }

        //If Id is still available, get fresh list from the server and merge with local list to check again
        if (ownDeviceListNode != null) {
            try {
                OmemoDeviceListElement serverDeviceList = omemoManager.getOmemoService().getPubSubHelper().extractDeviceListFrom(ownDeviceListNode);
                if (serverDeviceList != null) {
                    cachedDeviceList.merge(serverDeviceList);
                }
            } catch (XMPPException.XMPPErrorException | SmackException.NotConnectedException | InterruptedException | SmackException.NoResponseException e) {
                e.printStackTrace();
            }
        }
        //Does the list already contain that id?
        return !cachedDeviceList.contains(id);
    }

    /**
     * Create OmemoSession objects for all T_Sess objects of the contact.
     * The T_Sess objects will be wrapped inside a OmemoSession for every device of the contact
     *
     * @param contact     BareJid of the contact
     * @param rawSessions HashMap of Integers (deviceIds) and T_Sess sessions.
     * @return HashMap of OmemoContacts and OmemoSessions
     */
    HashMap<OmemoDevice, OmemoSession<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess, T_Addr, T_ECPub, T_Bundle, T_Ciph>>
    buildOmemoSessionsFor(BareJid contact, HashMap<Integer, T_Sess> rawSessions) {

        HashMap<OmemoDevice, OmemoSession<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess, T_Addr, T_ECPub, T_Bundle, T_Ciph>>
                sessions = new HashMap<>();

        for (Map.Entry<Integer, T_Sess> e : rawSessions.entrySet()) {
            OmemoDevice omemoDevice = new OmemoDevice(contact, e.getKey());
            try {
                sessions.put(omemoDevice, createOmemoSession(omemoDevice, loadOmemoIdentityKey(omemoDevice)));
            } catch (InvalidOmemoKeyException e1) {
                e1.printStackTrace();
            }
        }
        return sessions;
    }

    /**
     * Return the OmemoSession for the OmemoDevice
     *
     * @param device OmemoDevice
     * @return OmemoSession
     */
    public OmemoSession<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess, T_Addr, T_ECPub, T_Bundle, T_Ciph>
    getOmemoSessionOf(OmemoDevice device) {
        OmemoSession<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess, T_Addr, T_ECPub, T_Bundle, T_Ciph> s =
                omemoSessions.get(device);
        if (s == null) {
            T_IdKey identityKey = null;
            try {
                identityKey = loadOmemoIdentityKey(device);
            } catch (InvalidOmemoKeyException e) {
                e.printStackTrace();
            }
            if (identityKey != null) {
                s = createOmemoSession(device, identityKey);
            } else {
                LOGGER.log(Level.INFO, "No IdentityKey for device " + device);
                s = createOmemoSession(device, null);
            }
            omemoSessions.put(device, s);
        }
        return s;
    }

    /**
     * Merge the received OmemoDeviceListElement with the one we already have. If we had none, the received one is saved.
     *
     * @param contact Contact we received the list from.
     * @param list    List we received.
     */
    void mergeCachedDeviceList(BareJid contact, OmemoDeviceListElement list) {
        CachedDeviceList cached = loadCachedDeviceList(contact);
        if (cached == null) cached = new CachedDeviceList();
        cached.merge(list);
        storeCachedDeviceList(contact, cached);
    }

    /**
     * Renew our singed preKey. This should be done once every 7-14 days. TODO: JUST DO IT!
     * The old signed PreKey should be kept for around a month or so (look it up in the XEP)
     *
     * @throws InvalidOmemoKeyException when our identityKey is invalid
     */
    void changeSignedPreKey() throws InvalidOmemoKeyException {
        int lastSignedPreKeyId = loadCurrentSignedPreKeyId();
        try {
            T_SigPreKey newSignedPreKey = generateOmemoSignedPreKey(loadOmemoIdentityKeyPair(), lastSignedPreKeyId + 1);
            storeOmemoSignedPreKey(lastSignedPreKeyId + 1, newSignedPreKey);
            storeCurrentSignedPreKeyId(lastSignedPreKeyId + 1);
        } catch (InvalidOmemoKeyException e) {
            LOGGER.log(Level.INFO, "Couldn't generate SignedPreKey: " + e.getMessage());
            throw e;
        }
    }

    /**
     * Pack a OmemoBundleElement containing our key material.
     * If we used up n preKeys since we last published our bundle, generate n new preKeys and add them to the bundle.
     * We should always publish TARGET_PRE_KEY_COUNT keys.
     *
     * @return OmemoBundleElement
     * @throws InvalidOmemoKeyException when a key could not be loaded
     */
    public OmemoBundleElement packOmemoBundle() throws InvalidOmemoKeyException {

        int currentSignedPreKeyId = loadCurrentSignedPreKeyId();
        T_SigPreKey currentSignedPreKey = loadOmemoSignedPreKey(currentSignedPreKeyId);
        T_IdKeyPair identityKeyPair = loadOmemoIdentityKeyPair();

        HashMap<Integer, T_PreKey> preKeys = loadOmemoPreKeys();
        int newKeysCount = TARGET_PRE_KEY_COUNT - preKeys.size();

        if (newKeysCount > 0) {
            HashMap<Integer, T_PreKey> newKeys = generateOmemoPreKeys(loadLastPreKeyId() + 1, newKeysCount);
            storeOmemoPreKeys(newKeys);
            preKeys.putAll(newKeys);
            storeLastPreKeyId(loadLastPreKeyId() + newKeysCount);
        }

        return new OmemoBundleElement(
                currentSignedPreKeyId,
                keyUtil().signedPreKeyPublicForBundle(currentSignedPreKey),
                keyUtil().signedPreKeySignatureFromKey(currentSignedPreKey),
                keyUtil().identityKeyForBundle(keyUtil().identityKeyFromPair(identityKeyPair)),
                keyUtil().preKeyPublisKeysForBundle(preKeys)
        );
    }

    // *sigh*

    /**
     * Load our deviceId
     *
     * @return the deviceId of this installation.
     */
    public abstract int loadOmemoDeviceId();

    /**
     * Store our deviceId
     *
     * @param deviceId the deviceId of this installation.
     */
    public abstract void storeOmemoDeviceId(int deviceId);

    /**
     * Generate a new deviceId.
     * This is a random integer between 1 and 2^31-1.
     *
     * @return random device ID.
     */
    public int generateOmemoDeviceId() {
        int i = new Random().nextInt(Integer.MAX_VALUE);
        if(i > 0) {
            return i;
        }
        if(i < 0) {
            return -i;
        }
        return generateOmemoDeviceId();
    }

    /**
     * Return the id of the last generated preKey.
     * This is used to generate new preKeys without preKeyId collisions.
     *
     * @return id of the last preKey
     */
    public abstract int loadLastPreKeyId();

    /**
     * Store the id of the last preKey we generated.
     *
     * @param currentPreKeyId the id of the last generated PreKey
     */
    public abstract void storeLastPreKeyId(int currentPreKeyId);

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
     *
     * @return identityKeyPair
     * @throws InvalidOmemoKeyException Thrown, if the stored key is damaged (*hands up* not my fault!)
     */
    public abstract T_IdKeyPair loadOmemoIdentityKeyPair() throws InvalidOmemoKeyException;

    /**
     * Store our identityKeyPair in storage. It would be a cool feature, if the key could be stored in a encrypted
     * database or something similar.
     *
     * @param identityKeyPair identityKeyPair
     */
    public abstract void storeOmemoIdentityKeyPair(T_IdKeyPair identityKeyPair);

    /**
     * Load the public identityKey of the device.
     *
     * @param device device
     * @return identityKey
     */
    public abstract T_IdKey loadOmemoIdentityKey(OmemoDevice device) throws InvalidOmemoKeyException;

    /**
     * Store the public identityKey of the device.
     *
     * @param device device
     * @param key    identityKey
     */
    public abstract void storeOmemoIdentityKey(OmemoDevice device, T_IdKey key);

    /**
     * Decide, whether a identityKey of a device is trusted or not.
     * If you want to use this module, you should memorize, whether the user has trusted this key or not, since
     * the owner of the identityKey will be able to read sent messages when this method returned 'true' for their
     * identityKey. Either you let the user decide whether you trust a key every time you see a new key, or you
     * implement something like 'blind trust' (see https://gultsch.de/trust.html)
     *
     * @param device      Owner of the key
     * @param identityKey identityKey
     * @return true, if the user trusts the key and wants to send messages to it, otherwise false
     */
    public abstract boolean isTrustedOmemoIdentity(OmemoDevice device, T_IdKey identityKey);

    /**
     * Did the user yet made a decision about whether to trust or distrust this device?
     *
     * @param device      device
     * @param identityKey IdentityKey
     * @return true, if the user either trusted or distrusted the device. Return false, if the user did not yet decide.
     */
    public abstract boolean isDecidedOmemoIdentity(OmemoDevice device, T_IdKey identityKey);

    /**
     * Trust an OmemoIdentity. This involves marking the key as trusted.
     *
     * @param device      device
     * @param identityKey identityKey
     */
    public abstract void trustOmemoIdentity(OmemoDevice device, T_IdKey identityKey);

    /**
     * Distrust an OmemoIdentity. This involved marking the key as distrusted.
     *
     * @param device      device
     * @param identityKey identityKey
     */
    public abstract void distrustOmemoIdentity(OmemoDevice device, T_IdKey identityKey);

    /**
     * Generate 'count' new PreKeys beginning with id 'startId'.
     * These preKeys are published and can be used by contacts to establish sessions with us.
     *
     * @param startId start id
     * @param count   how many keys do we want to generate
     * @return Map of new preKeys
     */
    public HashMap<Integer, T_PreKey> generateOmemoPreKeys(int startId, int count) {
        return keyUtil().generateOmemoPreKeys(startId, count);
    }

    /**
     * Load the preKey with id 'preKeyId' from storage
     *
     * @param preKeyId id of the key to be loaded
     * @return loaded preKey
     */
    public abstract T_PreKey loadOmemoPreKey(int preKeyId);

    /**
     * Store a PreKey in storage.
     *
     * @param preKeyId id of the key
     * @param preKey   key
     */
    public abstract void storeOmemoPreKey(int preKeyId, T_PreKey preKey);

    /**
     * Store a whole bunch of preKeys.
     *
     * @param preKeyHashMap HashMap of preKeys
     */
    public void storeOmemoPreKeys(HashMap<Integer, T_PreKey> preKeyHashMap) {
        for (Map.Entry<Integer, T_PreKey> e : preKeyHashMap.entrySet()) {
            storeOmemoPreKey(e.getKey(), e.getValue());
        }
    }

    /**
     * remove a preKey from storage. This is called, when a contact used one of our preKeys to establish a session
     * with us.
     *
     * @param preKeyId id of the used key that will be deleted
     */
    public abstract void removeOmemoPreKey(int preKeyId);

    /**
     * Return the id of the currently used signed preKey.
     * This is used to avoid collisions when generating a new signedPreKey
     *
     * @return id
     */
    public abstract int loadCurrentSignedPreKeyId();

    /**
     * Store the id of the currently used signedPreKey
     *
     * @param currentSignedPreKeyId if of the signedPreKey that is currently in use
     */
    public abstract void storeCurrentSignedPreKeyId(int currentSignedPreKeyId);

    /**
     * Return all our current OmemoPreKeys
     *
     * @return Map containing our preKeys
     */
    public abstract HashMap<Integer, T_PreKey> loadOmemoPreKeys();

    /**
     * Return the signedPreKey with the id 'singedPreKeyId'
     *
     * @param signedPreKeyId id of the key
     * @return key
     */
    public abstract T_SigPreKey loadOmemoSignedPreKey(int signedPreKeyId);

    /**
     * Load all our signed PreKeys
     *
     * @return HashMap of our singedPreKeys
     */
    public abstract HashMap<Integer, T_SigPreKey> loadOmemoSignedPreKeys();

    /**
     * Generate a new signed preKey
     *
     * @param identityKeyPair identityKeyPair used to sign the preKey
     * @param signedPreKeyId  id that the preKey will have
     * @return signedPreKey
     * @throws InvalidOmemoKeyException when something goes wrong
     */
    public T_SigPreKey generateOmemoSignedPreKey(T_IdKeyPair identityKeyPair, int signedPreKeyId) throws InvalidOmemoKeyException {
        return keyUtil().generateOmemoSignedPreKey(identityKeyPair, signedPreKeyId);
    }

    /**
     * Store a signedPreKey in storage
     *
     * @param signedPreKeyId id of the signedPreKey
     * @param signedPreKey   the key itself
     */
    public abstract void storeOmemoSignedPreKey(int signedPreKeyId, T_SigPreKey signedPreKey);

    /**
     * Remove a signedPreKey from storage
     *
     * @param signedPreKeyId id of the key that will be removed
     */
    public abstract void removeOmemoSignedPreKey(int signedPreKeyId);

    /**
     * Create a new concrete OmemoSession with a contact
     *
     * @param device      device to establish the session with
     * @param identityKey identityKey of the device
     * @return concrete OmemoSession
     */
    public OmemoSession<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess, T_Addr, T_ECPub, T_Bundle, T_Ciph>
    createOmemoSession(OmemoDevice device, T_IdKey identityKey) {
        return keyUtil().createOmemoSession(this, device, identityKey);
    }

    /**
     * Load the crypto-lib specific session object of the device from storage
     *
     * @param device device whose session we want to load
     * @return crypto related session
     */
    public abstract T_Sess loadRawSession(OmemoDevice device);

    /**
     * Load all crypto-lib specific session objects of contact 'contact'
     *
     * @param contact BareJid of the contact we want to get all sessions from
     * @return HashMap of deviceId and sessions of the contact
     */
    public abstract HashMap<Integer, T_Sess> loadAllRawSessionsOf(BareJid contact);

    /**
     * Store a crypto-lib specific session to storage
     *
     * @param device  OmemoDevice whose session we want to store
     * @param session session
     */
    public abstract void storeRawSession(OmemoDevice device, T_Sess session);

    /**
     * Remove a crypto-lib specific session from storage
     *
     * @param device device whose session we want to delete
     */
    public abstract void removeRawSession(OmemoDevice device);

    /**
     * Remove all crypto-lib specific session of a contact
     *
     * @param contact BareJid of the contact
     */
    public abstract void removeAllRawSessionsOf(BareJid contact);

    /**
     * Return true, if we have a session with the device, otherwise false
     * Hint for Signal: Do not try 'return getSession() != null' since this will create a new session
     *
     * @param device device
     * @return true if we have session, otherwise false
     */
    public abstract boolean containsRawSession(OmemoDevice device);

    /**
     * Load a list of deviceIds from contact 'contact' from the local cache.
     *
     * @param contact contact we want to get the deviceList of
     * @return CachedDeviceList of the contact
     */
    public abstract CachedDeviceList loadCachedDeviceList(BareJid contact);

    /**
     * Store the DeviceList of the contact in local storage.
     * See this as a cache.
     *
     * @param contact    Contact
     * @param deviceList list of the contacts devices' ids.
     */
    public abstract void storeCachedDeviceList(BareJid contact, CachedDeviceList deviceList);

    /**
     * Set the OmemoService object that we will use.
     *
     * @param service OmemoService
     */
    public void setOmemoService(OmemoService<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess, T_Addr, T_ECPub, T_Bundle, T_Ciph> service) {
        this.omemoService = service;
    }

    /**
     * Return our OmemoService object
     *
     * @return omemoService
     */
    public OmemoService<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess, T_Addr, T_ECPub, T_Bundle, T_Ciph> getOmemoService() {
        return this.omemoService;
    }

    /**
     * Return a concrete KeyUtil object that we can use as a utility to create keys etc.
     *
     * @return KeyUtil object
     */
    public abstract KeyUtil<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess, T_Addr, T_ECPub, T_Bundle, T_Ciph> keyUtil();

    /**
     * Return our identityKeys fingerprint
     *
     * @return fingerprint of our identityKeyPair
     */
    public String getFingerprint() {
        try {
            return keyUtil().getFingerprint(keyUtil().identityKeyFromPair(loadOmemoIdentityKeyPair()));
        } catch (InvalidOmemoKeyException e) {
            e.printStackTrace();
            return null;
        }
    }

    public String getFingerprint(OmemoDevice device) {
        try {
            return keyUtil().getFingerprint(loadOmemoIdentityKey(device));
        } catch (InvalidOmemoKeyException e) {
            e.printStackTrace();
            return null;
        }
    }
}
