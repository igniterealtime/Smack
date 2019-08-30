/**
 *
 * Copyright 2017 Paul Schaub, 2019 Florian Schmaus.
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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import java.util.Stack;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.util.stringencoder.BareJidEncoder;

import org.jivesoftware.smackx.omemo.exceptions.CorruptedOmemoKeyException;
import org.jivesoftware.smackx.omemo.internal.OmemoCachedDeviceList;
import org.jivesoftware.smackx.omemo.internal.OmemoDevice;

import org.jxmpp.jid.BareJid;

/**
 * Like a rocket!
 *
 * @author Paul Schaub
 */
public abstract class FileBasedOmemoStore<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess, T_Addr, T_ECPub, T_Bundle, T_Ciph>
        extends OmemoStore<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess, T_Addr, T_ECPub, T_Bundle, T_Ciph> {

    private final FileHierarchy hierarchy;
    private static final Logger LOGGER = Logger.getLogger(FileBasedOmemoStore.class.getName());
    private static BareJidEncoder bareJidEncoder = new BareJidEncoder.UrlSafeEncoder();

    public FileBasedOmemoStore(File basePath) {
        super();
        if (basePath == null) {
            throw new IllegalStateException("No FileBasedOmemoStoreDefaultPath set in OmemoConfiguration.");
        }
        this.hierarchy = new FileHierarchy(basePath);
    }

    @Override
    public T_IdKeyPair loadOmemoIdentityKeyPair(OmemoDevice userDevice)
            throws CorruptedOmemoKeyException, IOException {
        File identityKeyPairPath = hierarchy.getIdentityKeyPairPath(userDevice);
        return keyUtil().identityKeyPairFromBytes(readBytes(identityKeyPairPath));
    }

    @Override
    public void storeOmemoIdentityKeyPair(OmemoDevice userDevice, T_IdKeyPair identityKeyPair) throws IOException {
        File identityKeyPairPath = hierarchy.getIdentityKeyPairPath(userDevice);
        writeBytes(identityKeyPairPath, keyUtil().identityKeyPairToBytes(identityKeyPair));
    }

    @Override
    public void removeOmemoIdentityKeyPair(OmemoDevice userDevice) {
        File identityKeyPairPath = hierarchy.getIdentityKeyPairPath(userDevice);
        if (!identityKeyPairPath.delete()) {
            LOGGER.log(Level.WARNING, "Could not delete OMEMO IdentityKeyPair " + identityKeyPairPath.getAbsolutePath());
        }
    }

    @Override
    public T_IdKey loadOmemoIdentityKey(OmemoDevice userDevice, OmemoDevice contactsDevice)
            throws CorruptedOmemoKeyException, IOException {
        File identityKeyPath = hierarchy.getContactsIdentityKeyPath(userDevice, contactsDevice);
        byte[] bytes = readBytes(identityKeyPath);
        return bytes != null ? keyUtil().identityKeyFromBytes(bytes) : null;
    }

    @Override
    public void storeOmemoIdentityKey(OmemoDevice userDevice, OmemoDevice contactsDevice, T_IdKey t_idKey) throws IOException {
        File identityKeyPath = hierarchy.getContactsIdentityKeyPath(userDevice, contactsDevice);
        writeBytes(identityKeyPath, keyUtil().identityKeyToBytes(t_idKey));
    }

    @Override
    public void removeOmemoIdentityKey(OmemoDevice userDevice, OmemoDevice contactsDevice) {
        File identityKeyPath = hierarchy.getContactsIdentityKeyPath(userDevice, contactsDevice);
        if (!identityKeyPath.delete()) {
            LOGGER.log(Level.WARNING, "Could not delete OMEMO identityKey " + identityKeyPath.getAbsolutePath());
        }
    }

    @Override
    public SortedSet<Integer> localDeviceIdsOf(BareJid localUser) {
        SortedSet<Integer> deviceIds = new TreeSet<>();
        File userDir = hierarchy.getUserDirectory(localUser);
        File[] list = userDir.listFiles();
        for (File d : list != null ? list : new File[] {}) {
            if (d.isDirectory()) {
                try {
                    deviceIds.add(Integer.parseInt(d.getName()));
                } catch (NumberFormatException e) {
                    // ignore
                }
            }
        }
        return deviceIds;
    }

    @Override
    public void setDateOfLastReceivedMessage(OmemoDevice userDevice, OmemoDevice contactsDevice, Date date) throws IOException {
        File lastMessageReceived = hierarchy.getLastMessageReceivedDatePath(userDevice, contactsDevice);
        writeLong(lastMessageReceived, date.getTime());
    }

    @Override
    public Date getDateOfLastReceivedMessage(OmemoDevice userDevice, OmemoDevice contactsDevice) throws IOException {
        File lastMessageReceived = hierarchy.getLastMessageReceivedDatePath(userDevice, contactsDevice);
        Long date = readLong(lastMessageReceived);
        return date != null ? new Date(date) : null;
    }

    @Override
    public void setDateOfLastDeviceIdPublication(OmemoDevice userDevice, OmemoDevice contactsDevice, Date date) throws IOException {
        File lastDeviceIdPublished = hierarchy.getLastDeviceIdPublicationDatePath(userDevice, contactsDevice);
        writeLong(lastDeviceIdPublished, date.getTime());
    }

    @Override
    public Date getDateOfLastDeviceIdPublication(OmemoDevice userDevice, OmemoDevice contactsDevice) throws IOException {
        File lastDeviceIdPublished = hierarchy.getLastDeviceIdPublicationDatePath(userDevice, contactsDevice);
        Long date = readLong(lastDeviceIdPublished);
        return date != null ? new Date(date) : null;
    }

    @Override
    public void setDateOfLastSignedPreKeyRenewal(OmemoDevice userDevice, Date date) throws IOException {
        File lastSignedPreKeyRenewal = hierarchy.getLastSignedPreKeyRenewal(userDevice);
        writeLong(lastSignedPreKeyRenewal, date.getTime());
    }

    @Override
    public Date getDateOfLastSignedPreKeyRenewal(OmemoDevice userDevice) throws IOException {
        File lastSignedPreKeyRenewal = hierarchy.getLastSignedPreKeyRenewal(userDevice);
        Long date = readLong(lastSignedPreKeyRenewal);
        return date != null ? new Date(date) : null;
    }

    @Override
    public T_PreKey loadOmemoPreKey(OmemoDevice userDevice, int preKeyId) throws IOException {
        File preKeyPath = hierarchy.getPreKeyPath(userDevice, preKeyId);
        byte[] bytes = readBytes(preKeyPath);

        if (bytes != null) {
            try {
                return keyUtil().preKeyFromBytes(bytes);
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Could not deserialize preKey from bytes.", e);
            }
        }

        return null;
    }

    @Override
    public void storeOmemoPreKey(OmemoDevice userDevice, int preKeyId, T_PreKey t_preKey) throws IOException {
        File preKeyPath = hierarchy.getPreKeyPath(userDevice, preKeyId);
        writeBytes(preKeyPath, keyUtil().preKeyToBytes(t_preKey));
    }

    @Override
    public void removeOmemoPreKey(OmemoDevice userDevice, int preKeyId) {
        File preKeyPath = hierarchy.getPreKeyPath(userDevice, preKeyId);
        if (!preKeyPath.delete()) {
            LOGGER.log(Level.WARNING, "Deleting OMEMO preKey " + preKeyPath.getAbsolutePath() + " failed.");
        }
    }

    @Override
    public TreeMap<Integer, T_PreKey> loadOmemoPreKeys(OmemoDevice userDevice) throws IOException {
        File preKeyDirectory = hierarchy.getPreKeysDirectory(userDevice);
        TreeMap<Integer, T_PreKey> preKeys = new TreeMap<>();

        if (preKeyDirectory == null) {
            return preKeys;
        }

        File[] keys = preKeyDirectory.listFiles();

        for (File f : keys != null ? keys : new File[0]) {
            byte[] bytes = readBytes(f);
            if (bytes != null) {
                try {
                    T_PreKey p = keyUtil().preKeyFromBytes(bytes);
                    preKeys.put(Integer.parseInt(f.getName()), p);
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, "Could not deserialize preKey from bytes.", e);
                }
            }
        }

        return preKeys;
    }

    @Override
    public T_SigPreKey loadOmemoSignedPreKey(OmemoDevice userDevice, int signedPreKeyId) throws IOException {
        File signedPreKeyPath = new File(hierarchy.getSignedPreKeysDirectory(userDevice), Integer.toString(signedPreKeyId));
        byte[] bytes = readBytes(signedPreKeyPath);
        if (bytes != null) {
            try {
                return keyUtil().signedPreKeyFromBytes(bytes);
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Could not deserialize signed preKey from bytes.", e);
            }
        }
        return null;
    }

    @Override
    public TreeMap<Integer, T_SigPreKey> loadOmemoSignedPreKeys(OmemoDevice userDevice) throws IOException {
        File signedPreKeysDirectory = hierarchy.getSignedPreKeysDirectory(userDevice);
        TreeMap<Integer, T_SigPreKey> signedPreKeys = new TreeMap<>();

        if (signedPreKeysDirectory == null) {
            return signedPreKeys;
        }

        File[] keys = signedPreKeysDirectory.listFiles();

        for (File f : keys != null ? keys : new File[0]) {
            byte[] bytes = readBytes(f);
            if (bytes != null) {
                try {
                    T_SigPreKey p = keyUtil().signedPreKeyFromBytes(bytes);
                    signedPreKeys.put(Integer.parseInt(f.getName()), p);
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, "Could not deserialize signed preKey.", e);
                }
            }
        }

        return signedPreKeys;
    }

    @Override
    public void storeOmemoSignedPreKey(OmemoDevice userDevice,
                                       int signedPreKeyId,
                                       T_SigPreKey signedPreKey) throws IOException {
        File signedPreKeyPath = new File(hierarchy.getSignedPreKeysDirectory(userDevice), Integer.toString(signedPreKeyId));
        writeBytes(signedPreKeyPath, keyUtil().signedPreKeyToBytes(signedPreKey));
    }

    @Override
    public void removeOmemoSignedPreKey(OmemoDevice userDevice, int signedPreKeyId) {
        File signedPreKeyPath = new File(hierarchy.getSignedPreKeysDirectory(userDevice), Integer.toString(signedPreKeyId));
        if (!signedPreKeyPath.delete()) {
            LOGGER.log(Level.WARNING, "Deleting signed OMEMO preKey " + signedPreKeyPath.getAbsolutePath() + " failed.");
        }
    }

    @Override
    public T_Sess loadRawSession(OmemoDevice userDevice, OmemoDevice contactsDevice) throws IOException {
        File sessionPath = hierarchy.getContactsSessionPath(userDevice, contactsDevice);
        byte[] bytes = readBytes(sessionPath);
        if (bytes != null) {
            try {
                return keyUtil().rawSessionFromBytes(bytes);
            } catch (IOException e) {
                LOGGER.log(Level.WARNING, "Could not deserialize raw session.", e);
            }
        }
        return null;
    }

    @Override
    public HashMap<Integer, T_Sess> loadAllRawSessionsOf(OmemoDevice userDevice, BareJid contact) throws IOException {
        File contactsDirectory = hierarchy.getContactsDir(userDevice, contact);
        HashMap<Integer, T_Sess> sessions = new HashMap<>();
        String[] devices = contactsDirectory.list();

        for (String deviceId : devices != null ? devices : new String[0]) {
            int id;
            try {
                id = Integer.parseInt(deviceId);
            } catch (NumberFormatException e) {
                continue;
            }
            OmemoDevice device = new OmemoDevice(contact, id);
            File session = hierarchy.getContactsSessionPath(userDevice, device);

            byte[] bytes = readBytes(session);

            if (bytes != null) {
                try {
                    T_Sess s = keyUtil().rawSessionFromBytes(bytes);
                    sessions.put(id, s);
                } catch (IOException e) {
                    LOGGER.log(Level.WARNING, "Could not deserialize raw session.", e);
                }
            }

        }
        return sessions;
    }

    @Override
    public void storeRawSession(OmemoDevice userDevice, OmemoDevice contactsDevice, T_Sess session) throws IOException {
        File sessionPath = hierarchy.getContactsSessionPath(userDevice, contactsDevice);
        writeBytes(sessionPath, keyUtil().rawSessionToBytes(session));
    }

    @Override
    public void removeRawSession(OmemoDevice userDevice, OmemoDevice contactsDevice) {
        File sessionPath = hierarchy.getContactsSessionPath(userDevice, contactsDevice);
        if (!sessionPath.delete()) {
            LOGGER.log(Level.WARNING, "Deleting raw OMEMO session " + sessionPath.getAbsolutePath() + " failed.");
        }
    }

    @Override
    public void removeAllRawSessionsOf(OmemoDevice userDevice, BareJid contact) {
        File contactsDirectory = hierarchy.getContactsDir(userDevice, contact);
        String[] devices = contactsDirectory.list();

        for (String deviceId : devices != null ? devices : new String[0]) {
            int id = Integer.parseInt(deviceId);
            OmemoDevice device = new OmemoDevice(contact, id);
            File session = hierarchy.getContactsSessionPath(userDevice, device);
            if (!session.delete()) {
                LOGGER.log(Level.WARNING, "Deleting raw OMEMO session " + session.getAbsolutePath() + "failed.");
            }
        }
    }

    @Override
    public boolean containsRawSession(OmemoDevice userDevice, OmemoDevice contactsDevice) {
        File session = hierarchy.getContactsSessionPath(userDevice, contactsDevice);
        return session.exists();
    }

    @Override
    public void storeOmemoMessageCounter(OmemoDevice userDevice, OmemoDevice contactsDevice, int counter) throws IOException {
        File messageCounterFile = hierarchy.getDevicesMessageCounterPath(userDevice, contactsDevice);
        writeIntegers(messageCounterFile, Collections.singleton(counter));
    }

    @Override
    public int loadOmemoMessageCounter(OmemoDevice userDevice, OmemoDevice contactsDevice) throws IOException {
        File messageCounterFile = hierarchy.getDevicesMessageCounterPath(userDevice, contactsDevice);
        Set<Integer> integers = readIntegers(messageCounterFile);

        if (integers == null || integers.isEmpty()) {
            return 0;
        }

        return integers.iterator().next();
    }

    @Override
    public OmemoCachedDeviceList loadCachedDeviceList(OmemoDevice userDevice, BareJid contact) throws IOException {
        OmemoCachedDeviceList cachedDeviceList = new OmemoCachedDeviceList();

        if (contact == null) {
            throw new IllegalArgumentException("Contact can not be null.");
        }

        // active
        File activeDevicesPath = hierarchy.getContactsActiveDevicesPath(userDevice, contact);
        Set<Integer> active = readIntegers(activeDevicesPath);
        if (active != null) {
            cachedDeviceList.getActiveDevices().addAll(active);
        }

        // inactive
        File inactiveDevicesPath = hierarchy.getContactsInactiveDevicesPath(userDevice, contact);
        Set<Integer> inactive = readIntegers(inactiveDevicesPath);
        if (inactive != null) {
            cachedDeviceList.getInactiveDevices().addAll(inactive);
        }

        return cachedDeviceList;
    }

    @Override
    public void storeCachedDeviceList(OmemoDevice userDevice,
                                      BareJid contact,
                                      OmemoCachedDeviceList contactsDeviceList) throws IOException {
        if (contact == null) {
            return;
        }

        File activeDevices = hierarchy.getContactsActiveDevicesPath(userDevice, contact);
        writeIntegers(activeDevices, contactsDeviceList.getActiveDevices());

        File inactiveDevices = hierarchy.getContactsInactiveDevicesPath(userDevice, contact);
        writeIntegers(inactiveDevices, contactsDeviceList.getInactiveDevices());
    }

    @Override
    public void purgeOwnDeviceKeys(OmemoDevice userDevice) {
        File deviceDirectory = hierarchy.getUserDeviceDirectory(userDevice);
        deleteDirectory(deviceDirectory);
    }

    private static void writeLong(File target, long i) throws IOException {
        if (target == null) {
            throw new IOException("Could not write long to null-path.");
        }

        FileHierarchy.createFile(target);

        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(target))) {
            out.writeLong(i);
        }
    }

    private static Long readLong(File target) throws IOException {
        if (target == null) {
            throw new IOException("Could not read long from null-path.");
        }

        if (!target.exists() || !target.isFile()) {
            return null;
        }

        try (DataInputStream in = new DataInputStream(new FileInputStream(target))) {
            return in.readLong();
        }
    }

    private static void writeBytes(File target, byte[] bytes) throws IOException {
        if (target == null) {
            throw new IOException("Could not write bytes to null-path.");
        }

        // Create file
        FileHierarchy.createFile(target);

        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(target))) {
            out.write(bytes);
        }
    }

    private static byte[] readBytes(File target) throws IOException {
        if (target == null) {
            throw new IOException("Could not read bytes from null-path.");
        }

        if (!target.exists() || !target.isFile()) {
            return null;
        }

        byte[] b = new byte[(int) target.length()];
        try (DataInputStream in = new DataInputStream(new FileInputStream(target))) {
            in.read(b);
        }

        return b;
    }

    private static void writeIntegers(File target, Set<Integer> integers) throws IOException {
        if (target == null) {
            throw new IOException("Could not write integers to null-path.");
        }

        FileHierarchy.createFile(target);

        try (DataOutputStream out = new DataOutputStream(new FileOutputStream(target))) {
            for (int i : integers) {
                out.writeInt(i);
            }
        }
    }

    private static Set<Integer> readIntegers(File target) throws IOException {
        if (target == null) {
            throw new IOException("Could not write integers to null-path.");
        }

        if (!target.exists() || !target.isFile()) {
            return null;
        }

        HashSet<Integer> integers = new HashSet<>();

        try (DataInputStream in = new DataInputStream(new FileInputStream(target))) {
            while (true) {
                try {
                    integers.add(in.readInt());
                } catch (EOFException e) {
                    break;
                }
            }
        }

        return integers;
    }

    /**
     * Delete a directory with all subdirectories.
     * @param root TODO javadoc me please
     */
    public static void deleteDirectory(File root) {
        File[] currList;
        Stack<File> stack = new Stack<>();
        stack.push(root);
        while (!stack.isEmpty()) {
            if (stack.lastElement().isDirectory()) {
                currList = stack.lastElement().listFiles();
                if (currList != null && currList.length > 0) {
                    for (File curr : currList) {
                        stack.push(curr);
                    }
                } else {
                    stack.pop().delete();
                }
            } else {
                stack.pop().delete();
            }
        }
    }

    /**
     * This class represents the directory structure of the FileBasedOmemoStore.
     * The directory looks as follows:
     *
     *  OMEMO_Store/
     *      'romeo@montague.lit'/                           //Our bareJid
     *          ...
     *      'juliet@capulet.lit'/                           //Our other bareJid
     *          '13371234'/                                 //deviceId
     *              identityKeyPair                         //Our identityKeyPair
     *              lastSignedPreKeyRenewal                 //Date of when the signedPreKey was last renewed.
     *              preKeys/                                //Our preKeys
     *                  '1'
     *                  '2'
     *                  ...
     *              signedPreKeys/                          //Our signedPreKeys
     *                  '1'
     *                  '2'
     *                  ...
     *              contacts/
     *                  'romeo@capulet.lit'/                //Juliets contact Romeo
     *                      activeDevice                    //List of Romeos active devices
     *                      inactiveDevices                 //List of his inactive devices
     *                      'deviceId'/                     //Romeos deviceId
     *                          identityKey                 //Romeos identityKey
     *                          session                     //Our session with romeo
     *                          trust                       //Records about the trust in romeos device
     *                          (lastReceivedMessageDate)   //Only, for our own other devices:
     *                                                          //date of the last received message
     *
     */
    public static class FileHierarchy {

        static final String STORE = "OMEMO_Store";
        static final String CONTACTS = "contacts";
        static final String IDENTITY_KEY = "identityKey";
        static final String IDENTITY_KEY_PAIR = "identityKeyPair";
        static final String PRE_KEYS = "preKeys";
        static final String LAST_MESSAGE_RECEVIED_DATE = "lastMessageReceivedDate";
        static final String LAST_DEVICEID_PUBLICATION_DATE = "lastDeviceIdPublicationDate";
        static final String SIGNED_PRE_KEYS = "signedPreKeys";
        static final String LAST_SIGNED_PRE_KEY_RENEWAL = "lastSignedPreKeyRenewal";
        static final String SESSION = "session";
        static final String DEVICE_LIST_ACTIVE = "activeDevices";
        static final String DEVICE_LIST_INAVTIVE = "inactiveDevices";
        static final String MESSAGE_COUNTER = "messageCounter";

        File basePath;

        FileHierarchy(File basePath) {
            this.basePath = basePath;
            basePath.mkdirs();
        }

        File getStoreDirectory() {
            return createDirectory(basePath, STORE);
        }

        File getUserDirectory(OmemoDevice userDevice) {
            return getUserDirectory(userDevice.getJid());
        }

        File getUserDirectory(BareJid bareJid) {
            return createDirectory(getStoreDirectory(), bareJidEncoder.encode(bareJid));
        }

        File getUserDeviceDirectory(OmemoDevice userDevice) {
            return createDirectory(getUserDirectory(userDevice.getJid()),
                    Integer.toString(userDevice.getDeviceId()));
        }

        File getContactsDir(OmemoDevice userDevice) {
            return createDirectory(getUserDeviceDirectory(userDevice), CONTACTS);
        }

        File getContactsDir(OmemoDevice userDevice, BareJid contact) {
            return createDirectory(getContactsDir(userDevice), bareJidEncoder.encode(contact));
        }

        File getContactsDir(OmemoDevice userDevice, OmemoDevice contactsDevice) {
            return createDirectory(getContactsDir(userDevice, contactsDevice.getJid()),
                    Integer.toString(contactsDevice.getDeviceId()));
        }

        File getIdentityKeyPairPath(OmemoDevice userDevice) {
            return new File(getUserDeviceDirectory(userDevice), IDENTITY_KEY_PAIR);
        }

        File getPreKeysDirectory(OmemoDevice userDevice) {
            return createDirectory(getUserDeviceDirectory(userDevice), PRE_KEYS);
        }

        File getPreKeyPath(OmemoDevice userDevice, int preKeyId) {
            return new File(getPreKeysDirectory(userDevice), Integer.toString(preKeyId));
        }

        File getLastMessageReceivedDatePath(OmemoDevice userDevice, OmemoDevice device) {
            return new File(getContactsDir(userDevice, device), LAST_MESSAGE_RECEVIED_DATE);
        }

        File getLastDeviceIdPublicationDatePath(OmemoDevice userDevice, OmemoDevice device) {
            return new File(getContactsDir(userDevice, device), LAST_DEVICEID_PUBLICATION_DATE);
        }

        File getSignedPreKeysDirectory(OmemoDevice userDevice) {
            return createDirectory(getUserDeviceDirectory(userDevice), SIGNED_PRE_KEYS);
        }

        File getLastSignedPreKeyRenewal(OmemoDevice userDevice) {
            return new File(getUserDeviceDirectory(userDevice), LAST_SIGNED_PRE_KEY_RENEWAL);
        }

        File getContactsIdentityKeyPath(OmemoDevice userDevice, OmemoDevice contactsDevice) {
            return new File(getContactsDir(userDevice, contactsDevice), IDENTITY_KEY);

        }

        File getContactsSessionPath(OmemoDevice userDevice, OmemoDevice contactsDevice) {
            return new File(getContactsDir(userDevice, contactsDevice), SESSION);
        }

        File getContactsActiveDevicesPath(OmemoDevice userDevice, BareJid contact) {
            return new File(getContactsDir(userDevice, contact), DEVICE_LIST_ACTIVE);
        }

        File getContactsInactiveDevicesPath(OmemoDevice userDevice, BareJid contact) {
            return new File(getContactsDir(userDevice, contact), DEVICE_LIST_INAVTIVE);
        }

        File getDevicesMessageCounterPath(OmemoDevice userDevice, OmemoDevice otherDevice) {
            return new File(getContactsDir(userDevice, otherDevice), MESSAGE_COUNTER);
        }

        private static File createFile(File f) throws IOException {
            File p = f.getParentFile();
            createDirectory(p);
            f.createNewFile();
            return f;

        }

        private static File createDirectory(File dir, String subdir) {
            File f = new File(dir, subdir);
            return createDirectory(f);
        }

        private static File createDirectory(File f) {
            if (f.exists() && f.isDirectory()) {
                return f;
            }

            f.mkdirs();
            return f;
        }
    }

    /**
     * Convert {@link BareJid BareJids} to Strings using the legacy {@link BareJid#toString()} method instead of the
     * proper, url safe {@link BareJid#asUrlEncodedString()} method.
     * While it is highly advised to use the new format, you can use this method to stay backwards compatible to data
     * sets created by the old implementation.
     */
    @SuppressWarnings("deprecation")
    public static void useLegacyBareJidEncoding() {
        bareJidEncoder = new BareJidEncoder.LegacyEncoder();
    }
}
