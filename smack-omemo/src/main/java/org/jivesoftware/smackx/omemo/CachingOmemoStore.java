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
import java.util.Date;
import java.util.HashMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.jivesoftware.smackx.omemo.exceptions.CorruptedOmemoKeyException;
import org.jivesoftware.smackx.omemo.internal.OmemoCachedDeviceList;
import org.jivesoftware.smackx.omemo.internal.OmemoDevice;
import org.jivesoftware.smackx.omemo.util.OmemoKeyUtil;

import org.jxmpp.jid.BareJid;

/**
 * This class implements the Proxy Pattern in order to wrap an OmemoStore with a caching layer.
 * This reduces access to the underlying storage layer (eg. database, filesystem) by only accessing it for
 * missing/updated values.
 *
 * Alternatively this implementation can be used as an ephemeral keystore without a persisting backend.
 *
 * @param <T_IdKeyPair>
 * @param <T_IdKey>
 * @param <T_PreKey>
 * @param <T_SigPreKey>
 * @param <T_Sess>
 * @param <T_Addr>
 * @param <T_ECPub>
 * @param <T_Bundle>
 * @param <T_Ciph>
 */
public class CachingOmemoStore<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess, T_Addr, T_ECPub, T_Bundle, T_Ciph>
        extends OmemoStore<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess, T_Addr, T_ECPub, T_Bundle, T_Ciph> {

    private final HashMap<OmemoDevice, KeyCache<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess>> caches = new HashMap<>();
    private final OmemoStore<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess, T_Addr, T_ECPub, T_Bundle, T_Ciph> persistent;
    private final OmemoKeyUtil<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess, T_ECPub, T_Bundle> keyUtil;

    public CachingOmemoStore(OmemoKeyUtil<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess, T_ECPub, T_Bundle> keyUtil) {
        if (keyUtil == null) {
            throw new IllegalArgumentException("KeyUtil MUST NOT be null!");
        }
        this.keyUtil = keyUtil;
        persistent = null;
    }

    public CachingOmemoStore(OmemoStore<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess, T_Addr, T_ECPub, T_Bundle, T_Ciph> wrappedStore) {
        if (wrappedStore == null) {
            throw new NullPointerException("Wrapped OmemoStore MUST NOT be null!");
        }
        this.keyUtil = null;
        persistent = wrappedStore;
    }

    @Override
    public SortedSet<Integer> localDeviceIdsOf(BareJid localUser) {
        if (persistent != null) {
            return persistent.localDeviceIdsOf(localUser);
        } else {
            return new TreeSet<>(); //TODO: ?
        }
    }

    @Override
    public T_IdKeyPair loadOmemoIdentityKeyPair(OmemoDevice userDevice)
            throws CorruptedOmemoKeyException, IOException {
        T_IdKeyPair pair = getCache(userDevice).identityKeyPair;

        if (pair == null && persistent != null) {
            pair = persistent.loadOmemoIdentityKeyPair(userDevice);
            if (pair != null) {
                getCache(userDevice).identityKeyPair = pair;
            }
        }

        return pair;
    }

    @Override
    public void storeOmemoIdentityKeyPair(OmemoDevice userDevice, T_IdKeyPair identityKeyPair) throws IOException {
        getCache(userDevice).identityKeyPair = identityKeyPair;
        if (persistent != null) {
            persistent.storeOmemoIdentityKeyPair(userDevice, identityKeyPair);
        }
    }

    @Override
    public void removeOmemoIdentityKeyPair(OmemoDevice userDevice) {
        getCache(userDevice).identityKeyPair = null;
        if (persistent != null) {
            persistent.removeOmemoIdentityKeyPair(userDevice);
        }
    }

    @Override
    public T_IdKey loadOmemoIdentityKey(OmemoDevice userDevice, OmemoDevice contactsDevice)
            throws CorruptedOmemoKeyException, IOException {
        T_IdKey idKey = getCache(userDevice).identityKeys.get(contactsDevice);

        if (idKey == null && persistent != null) {
            idKey = persistent.loadOmemoIdentityKey(userDevice, contactsDevice);
            if (idKey != null) {
                getCache(userDevice).identityKeys.put(contactsDevice, idKey);
            }
        }

        return idKey;
    }

    @Override
    public void storeOmemoIdentityKey(OmemoDevice userDevice, OmemoDevice device, T_IdKey t_idKey) throws IOException {
        getCache(userDevice).identityKeys.put(device, t_idKey);
        if (persistent != null) {
            persistent.storeOmemoIdentityKey(userDevice, device, t_idKey);
        }
    }

    @Override
    public void removeOmemoIdentityKey(OmemoDevice userDevice, OmemoDevice contactsDevice) {
        getCache(userDevice).identityKeys.remove(contactsDevice);
        if (persistent != null) {
            persistent.removeOmemoIdentityKey(userDevice, contactsDevice);
        }
    }

    @Override
    public void storeOmemoMessageCounter(OmemoDevice userDevice, OmemoDevice contactsDevice, int counter) throws IOException {
        getCache(userDevice).messageCounters.put(contactsDevice, counter);
        if (persistent != null) {
            persistent.storeOmemoMessageCounter(userDevice, contactsDevice, counter);
        }
    }

    @Override
    public int loadOmemoMessageCounter(OmemoDevice userDevice, OmemoDevice contactsDevice) throws IOException {
        Integer counter = getCache(userDevice).messageCounters.get(contactsDevice);
        if (counter == null && persistent != null) {
            counter = persistent.loadOmemoMessageCounter(userDevice, contactsDevice);
        }

        if (counter == null) {
            counter = 0;
        }

        getCache(userDevice).messageCounters.put(contactsDevice, counter);

        return counter;
    }

    @Override
    public void setDateOfLastReceivedMessage(OmemoDevice userDevice, OmemoDevice from, Date date) throws IOException {
        getCache(userDevice).lastMessagesDates.put(from, date);
        if (persistent != null) {
            persistent.setDateOfLastReceivedMessage(userDevice, from, date);
        }
    }

    @Override
    public Date getDateOfLastReceivedMessage(OmemoDevice userDevice, OmemoDevice from) throws IOException {
        Date last = getCache(userDevice).lastMessagesDates.get(from);

        if (last == null && persistent != null) {
            last = persistent.getDateOfLastReceivedMessage(userDevice, from);
            if (last != null) {
                getCache(userDevice).lastMessagesDates.put(from, last);
            }
        }

        return last;
    }

    @Override
    public void setDateOfLastDeviceIdPublication(OmemoDevice userDevice, OmemoDevice contactsDevice, Date date) throws IOException {
        getCache(userDevice).lastDeviceIdPublicationDates.put(contactsDevice, date);
        if (persistent != null) {
            persistent.setDateOfLastReceivedMessage(userDevice, contactsDevice, date);
        }
    }

    @Override
    public Date getDateOfLastDeviceIdPublication(OmemoDevice userDevice, OmemoDevice contactsDevice) throws IOException {
        Date last = getCache(userDevice).lastDeviceIdPublicationDates.get(contactsDevice);

        if (last == null && persistent != null) {
            last = persistent.getDateOfLastDeviceIdPublication(userDevice, contactsDevice);
            if (last != null) {
                getCache(userDevice).lastDeviceIdPublicationDates.put(contactsDevice, last);
            }
        }

        return last;
    }

    @Override
    public void setDateOfLastSignedPreKeyRenewal(OmemoDevice userDevice, Date date) throws IOException {
        getCache(userDevice).lastRenewalDate = date;
        if (persistent != null) {
            persistent.setDateOfLastSignedPreKeyRenewal(userDevice, date);
        }
    }

    @Override
    public Date getDateOfLastSignedPreKeyRenewal(OmemoDevice userDevice) throws IOException {
        Date lastRenewal = getCache(userDevice).lastRenewalDate;

        if (lastRenewal == null && persistent != null) {
            lastRenewal = persistent.getDateOfLastSignedPreKeyRenewal(userDevice);
            if (lastRenewal != null) {
                getCache(userDevice).lastRenewalDate = lastRenewal;
            }
        }

        return lastRenewal;
    }

    @Override
    public T_PreKey loadOmemoPreKey(OmemoDevice userDevice, int preKeyId) throws IOException {
        T_PreKey preKey = getCache(userDevice).preKeys.get(preKeyId);

        if (preKey == null && persistent != null) {
            preKey = persistent.loadOmemoPreKey(userDevice, preKeyId);
            if (preKey != null) {
                getCache(userDevice).preKeys.put(preKeyId, preKey);
            }
        }

        return preKey;
    }

    @Override
    public void storeOmemoPreKey(OmemoDevice userDevice, int preKeyId, T_PreKey t_preKey) throws IOException {
        getCache(userDevice).preKeys.put(preKeyId, t_preKey);
        if (persistent != null) {
            persistent.storeOmemoPreKey(userDevice, preKeyId, t_preKey);
        }
    }

    @Override
    public void removeOmemoPreKey(OmemoDevice userDevice, int preKeyId) {
        getCache(userDevice).preKeys.remove(preKeyId);
        if (persistent != null) {
            persistent.removeOmemoPreKey(userDevice, preKeyId);
        }
    }

    @Override
    public TreeMap<Integer, T_PreKey> loadOmemoPreKeys(OmemoDevice userDevice) throws IOException {
        TreeMap<Integer, T_PreKey> preKeys = getCache(userDevice).preKeys;

        if (preKeys.isEmpty() && persistent != null) {
            preKeys.putAll(persistent.loadOmemoPreKeys(userDevice));
        }

        return new TreeMap<>(preKeys);
    }

    @Override
    public T_SigPreKey loadOmemoSignedPreKey(OmemoDevice userDevice, int signedPreKeyId) throws IOException {
        T_SigPreKey sigPreKey = getCache(userDevice).signedPreKeys.get(signedPreKeyId);

        if (sigPreKey == null && persistent != null) {
            sigPreKey = persistent.loadOmemoSignedPreKey(userDevice, signedPreKeyId);
            if (sigPreKey != null) {
                getCache(userDevice).signedPreKeys.put(signedPreKeyId, sigPreKey);
            }
        }

        return sigPreKey;
    }

    @Override
    public TreeMap<Integer, T_SigPreKey> loadOmemoSignedPreKeys(OmemoDevice userDevice) throws IOException {
        TreeMap<Integer, T_SigPreKey> sigPreKeys = getCache(userDevice).signedPreKeys;

        if (sigPreKeys.isEmpty() && persistent != null) {
            sigPreKeys.putAll(persistent.loadOmemoSignedPreKeys(userDevice));
        }

        return new TreeMap<>(sigPreKeys);
    }

    @Override
    public void storeOmemoSignedPreKey(OmemoDevice userDevice,
                                       int signedPreKeyId,
                                       T_SigPreKey signedPreKey) throws IOException {
        getCache(userDevice).signedPreKeys.put(signedPreKeyId, signedPreKey);
        if (persistent != null) {
            persistent.storeOmemoSignedPreKey(userDevice, signedPreKeyId, signedPreKey);
        }
    }

    @Override
    public void removeOmemoSignedPreKey(OmemoDevice userDevice, int signedPreKeyId) {
        getCache(userDevice).signedPreKeys.remove(signedPreKeyId);
        if (persistent != null) {
            persistent.removeOmemoSignedPreKey(userDevice, signedPreKeyId);
        }
    }

    @Override
    public T_Sess loadRawSession(OmemoDevice userDevice, OmemoDevice contactsDevice) throws IOException {
        HashMap<Integer, T_Sess> contactSessions = getCache(userDevice).sessions.get(contactsDevice.getJid());
        if (contactSessions == null) {
            contactSessions = new HashMap<>();
            getCache(userDevice).sessions.put(contactsDevice.getJid(), contactSessions);
        }

        T_Sess session = contactSessions.get(contactsDevice.getDeviceId());
        if (session == null && persistent != null) {
            session = persistent.loadRawSession(userDevice, contactsDevice);
            if (session != null) {
                contactSessions.put(contactsDevice.getDeviceId(), session);
            }
        }

        return session;
    }

    @Override
    public HashMap<Integer, T_Sess> loadAllRawSessionsOf(OmemoDevice userDevice, BareJid contact) throws IOException {
        HashMap<Integer, T_Sess> sessions = getCache(userDevice).sessions.get(contact);
        if (sessions == null) {
            sessions = new HashMap<>();
            getCache(userDevice).sessions.put(contact, sessions);
        }

        if (sessions.isEmpty() && persistent != null) {
            sessions.putAll(persistent.loadAllRawSessionsOf(userDevice, contact));
        }

        return new HashMap<>(sessions);
    }

    @Override
    public void storeRawSession(OmemoDevice userDevice, OmemoDevice contactsDevicece, T_Sess session) throws IOException {
        HashMap<Integer, T_Sess> sessions = getCache(userDevice).sessions.get(contactsDevicece.getJid());
        if (sessions == null) {
            sessions = new HashMap<>();
            getCache(userDevice).sessions.put(contactsDevicece.getJid(), sessions);
        }

        sessions.put(contactsDevicece.getDeviceId(), session);
        if (persistent != null) {
            persistent.storeRawSession(userDevice, contactsDevicece, session);
        }
    }

    @Override
    public void removeRawSession(OmemoDevice userDevice, OmemoDevice contactsDevice) {
        HashMap<Integer, T_Sess> sessions = getCache(userDevice).sessions.get(contactsDevice.getJid());
        if (sessions != null) {
            sessions.remove(contactsDevice.getDeviceId());
        }

        if (persistent != null) {
            persistent.removeRawSession(userDevice, contactsDevice);
        }
    }

    @Override
    public void removeAllRawSessionsOf(OmemoDevice userDevice, BareJid contact) {
        getCache(userDevice).sessions.remove(contact);
        if (persistent != null) {
            persistent.removeAllRawSessionsOf(userDevice, contact);
        }
    }

    @Override
    public boolean containsRawSession(OmemoDevice userDevice, OmemoDevice contactsDevice) {
        HashMap<Integer, T_Sess> sessions = getCache(userDevice).sessions.get(contactsDevice.getJid());

        return (sessions != null && sessions.get(contactsDevice.getDeviceId()) != null) ||
                (persistent != null && persistent.containsRawSession(userDevice, contactsDevice));
    }

    @Override
    public OmemoCachedDeviceList loadCachedDeviceList(OmemoDevice userDevice, BareJid contact) throws IOException {
        OmemoCachedDeviceList list = getCache(userDevice).deviceLists.get(contact);

        if (list == null && persistent != null) {
            list = persistent.loadCachedDeviceList(userDevice, contact);
            if (list != null) {
                getCache(userDevice).deviceLists.put(contact, list);
            }
        }

        return list == null ? new OmemoCachedDeviceList() : new OmemoCachedDeviceList(list);
    }

    @Override
    public void storeCachedDeviceList(OmemoDevice userDevice,
                                      BareJid contact,
                                      OmemoCachedDeviceList deviceList) throws IOException {
        getCache(userDevice).deviceLists.put(contact, new OmemoCachedDeviceList(deviceList));

        if (persistent != null) {
            persistent.storeCachedDeviceList(userDevice, contact, deviceList);
        }
    }

    @Override
    public void purgeOwnDeviceKeys(OmemoDevice userDevice) {
        caches.remove(userDevice);

        if (persistent != null) {
            persistent.purgeOwnDeviceKeys(userDevice);
        }
    }

    @Override
    public OmemoKeyUtil<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess, T_ECPub, T_Bundle>
    keyUtil() {
        if (persistent != null) {
            return persistent.keyUtil();
        } else {
            return keyUtil;
        }
    }

    /**
     * Return the {@link KeyCache} object of an {@link OmemoManager}.
     * @param device
     * @return
     */
    private KeyCache<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess> getCache(OmemoDevice device) {
        KeyCache<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess> cache = caches.get(device);
        if (cache == null) {
            cache = new KeyCache<>();
            caches.put(device, cache);
        }
        return cache;
    }

    /**
     * Cache that stores values for an {@link OmemoManager}.
     * @param <T_IdKeyPair>
     * @param <T_IdKey>
     * @param <T_PreKey>
     * @param <T_SigPreKey>
     * @param <T_Sess>
     */
    private static class KeyCache<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess> {
        private T_IdKeyPair identityKeyPair;
        private final TreeMap<Integer, T_PreKey> preKeys = new TreeMap<>();
        private final TreeMap<Integer, T_SigPreKey> signedPreKeys = new TreeMap<>();
        private final HashMap<BareJid, HashMap<Integer, T_Sess>> sessions = new HashMap<>();
        private final HashMap<OmemoDevice, T_IdKey> identityKeys = new HashMap<>();
        private final HashMap<OmemoDevice, Date> lastMessagesDates = new HashMap<>();
        private final HashMap<OmemoDevice, Date> lastDeviceIdPublicationDates = new HashMap<>();
        private final HashMap<BareJid, OmemoCachedDeviceList> deviceLists = new HashMap<>();
        private Date lastRenewalDate = null;
        private final HashMap<OmemoDevice, Integer> messageCounters = new HashMap<>();
    }
}
