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

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.util.StringUtils;

import org.jivesoftware.smackx.omemo.exceptions.CorruptedOmemoKeyException;
import org.jivesoftware.smackx.omemo.internal.CachedDeviceList;
import org.jivesoftware.smackx.omemo.internal.OmemoDevice;

import org.jxmpp.jid.BareJid;

/**
 * Like a rocket!
 *
 * @author Paul Schaub
 */
public abstract class FileBasedOmemoStore<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess, T_Addr, T_ECPub, T_Bundle, T_Ciph>
        extends OmemoStore<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess, T_Addr, T_ECPub, T_Bundle, T_Ciph> {

    private static final Logger LOGGER = Logger.getLogger(FileBasedOmemoStore.class.getSimpleName());
    private final FileHierarchy hierarchy;

    public FileBasedOmemoStore() {
        this(OmemoConfiguration.getFileBasedOmemoStoreDefaultPath());
    }

    public FileBasedOmemoStore(File basePath) {
        super();
        if (basePath == null) {
            throw new IllegalStateException("No FileBasedOmemoStoreDefaultPath set in OmemoConfiguration.");
        }
        this.hierarchy = new FileHierarchy(basePath);
    }

    @Override
    public boolean isFreshInstallation(OmemoManager omemoManager) {
        File userDirectory = hierarchy.getUserDeviceDirectory(omemoManager);
        File[] files = userDirectory.listFiles();
        return files == null || files.length == 0;
    }

    @Override
    public int getDefaultDeviceId(BareJid user) {
        try {
            return readInt(hierarchy.getDefaultDeviceIdPath(user));
        } catch (IOException e) {
            return -1;
        }
    }

    @Override
    public void setDefaultDeviceId(BareJid user, int defaultDeviceId) {
        File defaultDeviceIdPath = hierarchy.getDefaultDeviceIdPath(user);

        if (defaultDeviceIdPath == null) {
            LOGGER.log(Level.SEVERE, "defaultDeviceIdPath is null!");
        }

        try {
            writeInt(defaultDeviceIdPath, defaultDeviceId);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Could not write defaultDeviceId: " + e, e);
        }
    }

    @Override
    public int loadLastPreKeyId(OmemoManager omemoManager) {
        try {
            int l = readInt(hierarchy.getLastPreKeyIdPath(omemoManager));
            return l == -1 ? 0 : l;
        } catch (IOException e) {
            return 0;
        }
    }

    @Override
    public void storeLastPreKeyId(OmemoManager omemoManager, int currentPreKeyId) {
        try {
            writeInt(hierarchy.getLastPreKeyIdPath(omemoManager), currentPreKeyId);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Could not write lastPreKeyId: " + e, e);
        }
    }

    @Override
    public T_IdKeyPair loadOmemoIdentityKeyPair(OmemoManager omemoManager) throws CorruptedOmemoKeyException {
        File identityKeyPairPath = hierarchy.getIdentityKeyPairPath(omemoManager);
        try {
            byte[] bytes = readBytes(identityKeyPairPath);
            return bytes != null ? keyUtil().identityKeyPairFromBytes(bytes) : null;
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public void storeOmemoIdentityKeyPair(OmemoManager omemoManager, T_IdKeyPair identityKeyPair) {
        File identityKeyPairPath = hierarchy.getIdentityKeyPairPath(omemoManager);
        try {
            writeBytes(identityKeyPairPath, keyUtil().identityKeyPairToBytes(identityKeyPair));
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Could not write omemoIdentityKeyPair: " + e, e);
        }
    }

    @Override
    public T_IdKey loadOmemoIdentityKey(OmemoManager omemoManager, OmemoDevice device) throws CorruptedOmemoKeyException {
        File identityKeyPath = hierarchy.getContactsIdentityKeyPath(omemoManager, device);
        try {
            byte[] bytes = readBytes(identityKeyPath);
            return bytes != null ? keyUtil().identityKeyFromBytes(bytes) : null;
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public void storeOmemoIdentityKey(OmemoManager omemoManager, OmemoDevice device, T_IdKey t_idKey) {
        File identityKeyPath = hierarchy.getContactsIdentityKeyPath(omemoManager, device);
        try {
            writeBytes(identityKeyPath, keyUtil().identityKeyToBytes(t_idKey));
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Could not write omemoIdentityKey of " + device + ": " + e, e);
        }
    }

    @Override
    public boolean isTrustedOmemoIdentity(OmemoManager omemoManager, OmemoDevice device, OmemoFingerprint fingerprint) {
        File trustPath = hierarchy.getContactsTrustPath(omemoManager, device);
        try {
            String depositedFingerprint = new String(readBytes(trustPath), StringUtils.UTF8);

            return  depositedFingerprint.length() > 2
                    && depositedFingerprint.charAt(0) == '1'
                    && new OmemoFingerprint(depositedFingerprint.substring(2)).equals(fingerprint);
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public boolean isDecidedOmemoIdentity(OmemoManager omemoManager, OmemoDevice device, OmemoFingerprint fingerprint) {
        File trustPath = hierarchy.getContactsTrustPath(omemoManager, device);
        try {
            String depositedFingerprint = new String(readBytes(trustPath), StringUtils.UTF8);

            return  depositedFingerprint.length() > 2
                    && (depositedFingerprint.charAt(0) == '1' || depositedFingerprint.charAt(0) == '2')
                    && new OmemoFingerprint(depositedFingerprint.substring(2)).equals(fingerprint);
        } catch (IOException e) {
            return false;
        }
    }

    @Override
    public void trustOmemoIdentity(OmemoManager omemoManager, OmemoDevice device, OmemoFingerprint fingerprint) {
        File trustPath = hierarchy.getContactsTrustPath(omemoManager, device);
        try {
            writeBytes(trustPath, ("1 " + fingerprint.toString()).getBytes(StringUtils.UTF8));
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Could not trust " + device + ": " + e, e);
        }
    }

    @Override
    public void distrustOmemoIdentity(OmemoManager omemoManager, OmemoDevice device, OmemoFingerprint fingerprint) {
        File trustPath = hierarchy.getContactsTrustPath(omemoManager, device);
        try {
            writeBytes(trustPath, ("2 " + fingerprint.toString()).getBytes(StringUtils.UTF8));
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Could not distrust " + device + ": " + e, e);
        }
    }

    @Override
    public void setDateOfLastReceivedMessage(OmemoManager omemoManager, OmemoDevice from, Date date) {
        File lastMessageReceived = hierarchy.getLastMessageReceivedDatePath(omemoManager, from);
        try {
            writeLong(lastMessageReceived, date.getTime());
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Could not write date of last received message from " + from + ": " + e, e);
        }
    }

    @Override
    public Date getDateOfLastReceivedMessage(OmemoManager omemoManager, OmemoDevice from) {
        File lastMessageReceived = hierarchy.getLastMessageReceivedDatePath(omemoManager, from);
        try {
            long date = readLong(lastMessageReceived);
            return date != -1 ? new Date(date) : null;
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public void setDateOfLastSignedPreKeyRenewal(OmemoManager omemoManager, Date date) {
        File lastSignedPreKeyRenewal = hierarchy.getLastSignedPreKeyRenewal(omemoManager);
        try {
            writeLong(lastSignedPreKeyRenewal, date.getTime());
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Could not write date of last singedPreKey renewal for "
                    + omemoManager.getOwnDevice() + ": " + e, e);
        }
    }

    @Override
    public Date getDateOfLastSignedPreKeyRenewal(OmemoManager omemoManager) {
        File lastSignedPreKeyRenewal = hierarchy.getLastSignedPreKeyRenewal(omemoManager);

        try {
            long date = readLong(lastSignedPreKeyRenewal);
            return date != -1 ? new Date(date) : null;
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public T_PreKey loadOmemoPreKey(OmemoManager omemoManager, int preKeyId) {
        File preKeyPath = hierarchy.getPreKeyPath(omemoManager, preKeyId);
        try {
            byte[] bytes = readBytes(preKeyPath);
            return bytes != null ? keyUtil().preKeyFromBytes(bytes) : null;
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public void storeOmemoPreKey(OmemoManager omemoManager, int preKeyId, T_PreKey t_preKey) {
        File preKeyPath = hierarchy.getPreKeyPath(omemoManager, preKeyId);
        try {
            writeBytes(preKeyPath, keyUtil().preKeyToBytes(t_preKey));
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Could not write preKey with id " + preKeyId + ": " + e, e);
        }
    }

    @Override
    public void removeOmemoPreKey(OmemoManager omemoManager, int preKeyId) {
        File preKeyPath = hierarchy.getPreKeyPath(omemoManager, preKeyId);
        preKeyPath.delete();
    }

    @Override
    public int loadCurrentSignedPreKeyId(OmemoManager omemoManager) {
        File currentSignedPreKeyIdPath = hierarchy.getCurrentSignedPreKeyIdPath(omemoManager);
        try {
            int i = readInt(currentSignedPreKeyIdPath);
            return i == -1 ? 0 : i;
        } catch (IOException e) {
            return 0;
        }
    }

    @Override
    public void storeCurrentSignedPreKeyId(OmemoManager omemoManager, int currentSignedPreKeyId) {
        File currentSignedPreKeyIdPath = hierarchy.getCurrentSignedPreKeyIdPath(omemoManager);
        try {
            writeInt(currentSignedPreKeyIdPath, currentSignedPreKeyId);
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Could not write currentSignedPreKeyId "
                    + currentSignedPreKeyId + " for " + omemoManager.getOwnDevice() + ": "
                    + e, e);
        }
    }

    @Override
    public HashMap<Integer, T_PreKey> loadOmemoPreKeys(OmemoManager omemoManager) {
        File preKeyDirectory = hierarchy.getPreKeysDirectory(omemoManager);
        HashMap<Integer, T_PreKey> preKeys = new HashMap<>();

        if (preKeyDirectory == null) {
            return preKeys;
        }

        File[] keys = preKeyDirectory.listFiles();
        for (File f : keys != null ? keys : new File[0]) {

            try {
                byte[] bytes = readBytes(f);
                if (bytes == null) {
                    continue;
                }
                T_PreKey p = keyUtil().preKeyFromBytes(bytes);
                preKeys.put(Integer.parseInt(f.getName()), p);

            } catch (IOException e) {
                // Do nothing.
            }
        }
        return preKeys;
    }

    @Override
    public T_SigPreKey loadOmemoSignedPreKey(OmemoManager omemoManager, int signedPreKeyId) {
        File signedPreKeyPath = new File(hierarchy.getSignedPreKeysDirectory(omemoManager), Integer.toString(signedPreKeyId));
        try {
            byte[] bytes = readBytes(signedPreKeyPath);
            return bytes != null ? keyUtil().signedPreKeyFromBytes(bytes) : null;
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public HashMap<Integer, T_SigPreKey> loadOmemoSignedPreKeys(OmemoManager omemoManager) {
        File signedPreKeysDirectory = hierarchy.getSignedPreKeysDirectory(omemoManager);
        HashMap<Integer, T_SigPreKey> signedPreKeys = new HashMap<>();

        if (signedPreKeysDirectory == null) {
            return signedPreKeys;
        }

        File[] keys = signedPreKeysDirectory.listFiles();
        for (File f : keys != null ? keys : new File[0]) {

            try {
                byte[] bytes = readBytes(f);
                if (bytes == null) {
                    continue;
                }
                T_SigPreKey p = keyUtil().signedPreKeyFromBytes(bytes);
                signedPreKeys.put(Integer.parseInt(f.getName()), p);

            } catch (IOException e) {
                // Do nothing.
            }
        }
        return signedPreKeys;
    }

    @Override
    public void storeOmemoSignedPreKey(OmemoManager omemoManager, int signedPreKeyId, T_SigPreKey signedPreKey) {
        File signedPreKeyPath = new File(hierarchy.getSignedPreKeysDirectory(omemoManager), Integer.toString(signedPreKeyId));
        try {
            writeBytes(signedPreKeyPath, keyUtil().signedPreKeyToBytes(signedPreKey));
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Could not write signedPreKey " + signedPreKey
                    + " for " + omemoManager.getOwnDevice() + ": " + e, e);
        }
    }

    @Override
    public void removeOmemoSignedPreKey(OmemoManager omemoManager, int signedPreKeyId) {
        File signedPreKeyPath = new File(hierarchy.getSignedPreKeysDirectory(omemoManager), Integer.toString(signedPreKeyId));
        signedPreKeyPath.delete();
    }

    @Override
    public T_Sess loadRawSession(OmemoManager omemoManager, OmemoDevice device) {
        File sessionPath = hierarchy.getContactsSessionPath(omemoManager, device);
        try {
            byte[] bytes = readBytes(sessionPath);
            return bytes != null ? keyUtil().rawSessionFromBytes(bytes) : null;
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public HashMap<Integer, T_Sess> loadAllRawSessionsOf(OmemoManager omemoManager, BareJid contact) {
        File contactsDirectory = hierarchy.getContactsDir(omemoManager, contact);
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
            File session = hierarchy.getContactsSessionPath(omemoManager, device);

            try {
                byte[] bytes = readBytes(session);
                if (bytes == null) {
                    continue;
                }
                T_Sess s = keyUtil().rawSessionFromBytes(bytes);
                sessions.put(id, s);

            } catch (IOException e) {
                // Do nothing.
            }
        }
        return sessions;
    }

    @Override
    public void storeRawSession(OmemoManager omemoManager, OmemoDevice device, T_Sess session) {
        File sessionPath = hierarchy.getContactsSessionPath(omemoManager, device);
        try {
            writeBytes(sessionPath, keyUtil().rawSessionToBytes(session));
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Could not write session between our device " + omemoManager.getOwnDevice()
                    + " and their device " + device + ": " + e.getMessage());
        }
    }

    @Override
    public void removeRawSession(OmemoManager omemoManager, OmemoDevice device) {
        File sessionPath = hierarchy.getContactsSessionPath(omemoManager, device);
        sessionPath.delete();
    }

    @Override
    public void removeAllRawSessionsOf(OmemoManager omemoManager, BareJid contact) {
        File contactsDirectory = hierarchy.getContactsDir(omemoManager, contact);
        String[] devices = contactsDirectory.list();

        for (String deviceId : devices != null ? devices : new String[0]) {
            int id;
            try {
                id = Integer.parseInt(deviceId);
            } catch (NumberFormatException e) {
                continue;
            }
            OmemoDevice device = new OmemoDevice(contact, id);
            File session = hierarchy.getContactsSessionPath(omemoManager, device);
            session.delete();
        }
    }

    @Override
    public boolean containsRawSession(OmemoManager omemoManager, OmemoDevice device) {
        File session = hierarchy.getContactsSessionPath(omemoManager, device);
        return session.exists();
    }

    @Override
    public CachedDeviceList loadCachedDeviceList(OmemoManager omemoManager, BareJid contact) {
        CachedDeviceList cachedDeviceList = new CachedDeviceList();

        if (contact == null) {
            return null;
        }

        // active
        File activeDevicesPath = hierarchy.getContactsActiveDevicesPath(omemoManager, contact);
        try {
            cachedDeviceList.getActiveDevices().addAll(readIntegers(activeDevicesPath));
        } catch (IOException e) {
            // Don't worry...
        }

        // inactive
        File inactiveDevicesPath = hierarchy.getContactsInactiveDevicesPath(omemoManager, contact);
        try {
            cachedDeviceList.getInactiveDevices().addAll(readIntegers(inactiveDevicesPath));
        } catch (IOException e) {
            // It's ok :)
        }

        return cachedDeviceList;
    }

    @Override
    public void storeCachedDeviceList(OmemoManager omemoManager, BareJid contact, CachedDeviceList deviceList) {
        if (contact == null) {
            return;
        }

        File activeDevices = hierarchy.getContactsActiveDevicesPath(omemoManager, contact);
        try {
            writeIntegers(activeDevices, deviceList.getActiveDevices());
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Could not write active devices of deviceList of "
                    + contact + ": " + e.getMessage());
        }

        File inactiveDevices = hierarchy.getContactsInactiveDevicesPath(omemoManager, contact);
        try {
            writeIntegers(inactiveDevices, deviceList.getInactiveDevices());
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Could not write inactive devices of deviceList of "
                    + contact + ": " + e.getMessage());
        }
    }

    @Override
    public void purgeOwnDeviceKeys(OmemoManager omemoManager) {
        File deviceDirectory = hierarchy.getUserDeviceDirectory(omemoManager);
        deleteDirectory(deviceDirectory);
    }

    private void writeInt(File target, int i) throws IOException {
        if (target == null) {
            throw new IOException("Could not write integer to null-path.");
        }

        FileHierarchy.createFile(target);

        IOException io = null;
        DataOutputStream out = null;
        try {
            out = new DataOutputStream(new FileOutputStream(target));
            out.writeInt(i);
        } catch (IOException e) {
            io = e;
        } finally {
            if (out != null) {
                out.close();
            }
        }

        if (io != null) {
            throw io;
        }
    }

    private int readInt(File target) throws IOException {
        if (target == null) {
            throw new IOException("Could not read integer from null-path.");
        }

        IOException io = null;
        int i = -1;
        DataInputStream in = null;

        try {
            in = new DataInputStream(new FileInputStream(target));
            i = in.readInt();

        } catch (IOException e) {
            io = e;

        } finally {
            if (in != null) {
                in.close();
            }
        }

        if (io != null) {
            throw io;
        }
        return i;
    }

    private void writeLong(File target, long i) throws IOException {
        if (target == null) {
            throw new IOException("Could not write long to null-path.");
        }

        FileHierarchy.createFile(target);

        IOException io = null;
        DataOutputStream out = null;
        try {
            out = new DataOutputStream(new FileOutputStream(target));
            out.writeLong(i);

        } catch (IOException e) {
            io = e;

        } finally {
            if (out != null) {
                out.close();
            }
        }

        if (io != null) {
            throw io;
        }
    }

    private long readLong(File target) throws IOException {
        if (target == null) {
            throw new IOException("Could not read long from null-path.");
        }

        IOException io = null;
        long l = -1;
        DataInputStream in = null;

        try {
            in = new DataInputStream(new FileInputStream(target));
            l = in.readLong();

        } catch (IOException e) {
            io = e;

        } finally {
            if (in != null) {
                in.close();
            }
        }

        if (io != null) {
            throw io;
        }

        return l;
    }

    private void writeBytes(File target, byte[] bytes) throws IOException {
        if (target == null) {
            throw new IOException("Could not write bytes to null-path.");
        }

        // Create file
        FileHierarchy.createFile(target);

        IOException io = null;
        DataOutputStream out = null;

        try {
            out = new DataOutputStream(new FileOutputStream(target));
            out.write(bytes);

        } catch (IOException e) {
            io = e;

        } finally {
            if (out != null) {
                out.close();
            }
        }

        if (io != null) {
            throw io;
        }
    }

    private byte[] readBytes(File target) throws IOException {
        if (target == null) {
            throw new IOException("Could not read bytes from null-path.");
        }

        byte[] b = null;
        IOException io = null;
        DataInputStream in = null;

        try {
            in = new DataInputStream(new FileInputStream(target));
            b = new byte[in.available()];
            in.read(b);

        } catch (IOException e) {
            io = e;

        } finally {
            if (in != null) {
                in.close();
            }
        }

        if (io != null) {
            throw io;
        }

        return b;
    }

    private void writeIntegers(File target, Set<Integer> integers) throws IOException {
        if (target == null) {
            throw new IOException("Could not write integers to null-path.");
        }

        IOException io = null;
        DataOutputStream out = null;

        try {
            out = new DataOutputStream(new FileOutputStream(target));
            for (int i : integers) {
                out.writeInt(i);
            }

        } catch (IOException e) {
            io = e;

        } finally {
            if (out != null) {
                out.close();
            }
        }

        if (io != null) {
            throw io;
        }
    }

    private Set<Integer> readIntegers(File target) throws IOException {
        if (target == null) {
            throw new IOException("Could not write integers to null-path.");
        }

        HashSet<Integer> integers = new HashSet<>();
        IOException io = null;
        DataInputStream in = null;

        try {
            in = new DataInputStream(new FileInputStream(target));

            try {
                while (true) {
                    integers.add(in.readInt());
                }
            } catch (EOFException e) {
                // Reached end of the list.
            }

        } catch (IOException e) {
            io = e;

        } finally {
            if (in != null) {
                in.close();
            }
        }

        if (io != null) {
            throw io;
        }

        return integers;
    }

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
     * This class represents the directory structure of the FileBasedOmemoStoreV2.
     * The directory looks as follows:
     *
     *  OMEMO_Store/
     *      'romeo@montague.lit'/                           //Our bareJid
     *          ...
     *      'juliet@capulet.lit'/                           //Our other bareJid
     *          defaultDeviceId
     *          '13371234'/                                 //deviceId
     *              identityKeyPair                         //Our identityKeyPair
     *              lastPreKeyId                            //Id of the last preKey we generated
     *              currentSignedPreKeyId                   //Id of the currently used signedPreKey
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
        static final String DEFAULT_DEVICE_ID = "defaultDeviceId";
        static final String IDENTITY_KEY = "identityKey";
        static final String IDENTITY_KEY_PAIR = "identityKeyPair";
        static final String PRE_KEYS = "preKeys";
        static final String LAST_MESSAGE_RECEVIED_DATE = "lastMessageReceivedDate";
        static final String LAST_PRE_KEY_ID = "lastPreKeyId";
        static final String SIGNED_PRE_KEYS = "signedPreKeys";
        static final String CURRENT_SIGNED_PRE_KEY_ID = "currentSignedPreKeyId";
        static final String LAST_SIGNED_PRE_KEY_RENEWAL = "lastSignedPreKeyRenewal";
        static final String SESSION = "session";
        static final String DEVICE_LIST_ACTIVE = "activeDevices";
        static final String DEVICE_LIST_INAVTIVE = "inactiveDevices";
        static final String TRUST = "trust";

        File basePath;

        FileHierarchy(File basePath) {
            this.basePath = basePath;
            basePath.mkdirs();
        }

        File getStoreDirectory() {
            return createDirectory(basePath, STORE);
        }

        File getUserDirectory(BareJid bareJid) {
            return createDirectory(getStoreDirectory(), bareJid.toString());
        }

        File getUserDeviceDirectory(OmemoManager omemoManager) {
            return createDirectory(getUserDirectory(omemoManager.getOwnJid()),
                    Integer.toString(omemoManager.getDeviceId()));
        }

        File getContactsDir(OmemoManager omemoManager) {
            return createDirectory(getUserDeviceDirectory(omemoManager), CONTACTS);
        }

        File getContactsDir(OmemoManager omemoManager, BareJid contact) {
            return createDirectory(getContactsDir(omemoManager), contact.toString());
        }

        File getContactsDir(OmemoManager omemoManager, OmemoDevice omemoDevice) {
            return createDirectory(getContactsDir(omemoManager, omemoDevice.getJid()),
                    Integer.toString(omemoDevice.getDeviceId()));
        }

        File getIdentityKeyPairPath(OmemoManager omemoManager) {
            return new File(getUserDeviceDirectory(omemoManager), IDENTITY_KEY_PAIR);
        }

        File getPreKeysDirectory(OmemoManager omemoManager) {
            return createDirectory(getUserDeviceDirectory(omemoManager), PRE_KEYS);
        }

        File getPreKeyPath(OmemoManager omemoManager, int preKeyId) {
            return new File(getPreKeysDirectory(omemoManager), Integer.toString(preKeyId));
        }

        File getLastMessageReceivedDatePath(OmemoManager omemoManager, OmemoDevice device) {
            return new File(getContactsDir(omemoManager, device), LAST_MESSAGE_RECEVIED_DATE);
        }

        File getLastPreKeyIdPath(OmemoManager omemoManager) {
            return new File(getUserDeviceDirectory(omemoManager), LAST_PRE_KEY_ID);
        }

        File getSignedPreKeysDirectory(OmemoManager omemoManager) {
            return createDirectory(getUserDeviceDirectory(omemoManager), SIGNED_PRE_KEYS);
        }

        File getCurrentSignedPreKeyIdPath(OmemoManager omemoManager) {
            return new File(getUserDeviceDirectory(omemoManager), CURRENT_SIGNED_PRE_KEY_ID);
        }

        File getLastSignedPreKeyRenewal(OmemoManager omemoManager) {
            return new File(getUserDeviceDirectory(omemoManager), LAST_SIGNED_PRE_KEY_RENEWAL);
        }

        File getDefaultDeviceIdPath(BareJid bareJid) {
            return new File(getUserDirectory(bareJid), DEFAULT_DEVICE_ID);
        }

        File getContactsIdentityKeyPath(OmemoManager omemoManager, OmemoDevice omemoDevice) {
            return new File(getContactsDir(omemoManager, omemoDevice), IDENTITY_KEY);

        }

        File getContactsSessionPath(OmemoManager omemoManager, OmemoDevice omemoDevice) {
            return new File(getContactsDir(omemoManager, omemoDevice), SESSION);
        }

        File getContactsActiveDevicesPath(OmemoManager omemoManager, BareJid contact) {
            return new File(getContactsDir(omemoManager, contact), DEVICE_LIST_ACTIVE);
        }

        File getContactsInactiveDevicesPath(OmemoManager omemoManager, BareJid contact) {
            return new File(getContactsDir(omemoManager, contact), DEVICE_LIST_INAVTIVE);
        }

        File getContactsTrustPath(OmemoManager omemoManager, OmemoDevice omemoDevice) {
            return new File(getContactsDir(omemoManager, omemoDevice), TRUST);

        }

        private static File createFile(File f) throws IOException {
            File p = f.getParentFile();
            createDirectory(p);
            f.createNewFile();
            return f;

        }

        private static File createFile(File dir, String filename) throws IOException {
            return createFile(new File(dir, filename));
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
}
