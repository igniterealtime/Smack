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

import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.omemo.exceptions.InvalidOmemoKeyException;
import org.jivesoftware.smackx.omemo.internal.CachedDeviceList;
import org.jivesoftware.smackx.omemo.internal.OmemoDevice;
import org.jxmpp.jid.BareJid;

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;

/**
 * Simple file based OmemoStore that stores values in a folder hierarchy.
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
public abstract class FileBasedOmemoStore<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess, T_Addr, T_ECPub, T_Bundle, T_Ciph>
        extends OmemoStore<T_IdKeyPair, T_IdKey, T_PreKey, T_SigPreKey, T_Sess, T_Addr, T_ECPub, T_Bundle, T_Ciph> {
    public static final String LAST_PRE_KEY_ID = "lastPreKeyId";
    public static final String IDENTITY_KEY_PAIR = "identityKeyPair";
    public static final String IDENTITY_KEY = "identityKey";
    public static final String CURRENT_SIGNED_PRE_KEY = "currentSignedPreKey";
    public static final String SESSION = "session";
    public static final String TRUST = "trust";
    public static final String DEVICE_LIST = "deviceList";

    private final File base;
    private final BareJid userJid;

    /**
     * Constructor
     *
     * @param manager omemoManager
     * @param base    base path of the store
     */
    public FileBasedOmemoStore(OmemoManager manager, File base) {
        super(manager);
        if (!base.exists()) {
            base.mkdirs();
        }
        this.base = base;
        this.userJid = manager.getConnection().getUser().asBareJid();
    }

    @Override
    public boolean isFreshInstallation() {
        File[] list = getUserPath().listFiles();
        return list == null || list.length == 0;
    }

    @Override
    public int loadOmemoDeviceId() {
        File[] devIDs = getUserPath().listFiles();
        return devIDs != null && devIDs.length > 0 ? Integer.parseInt(devIDs[0].getName()) : 0;
    }

    @Override
    public void storeOmemoDeviceId(int deviceId) {
        if (loadOmemoDeviceId() != deviceId) {
            File[] list = getUserPath().listFiles();
            if (list != null) {
                for (File f : list) {
                    f.delete();
                }
            }
            create(new File(getUserPath().getAbsolutePath() + "/" + deviceId));
        }
    }

    @Override
    public int loadLastPreKeyId() {
        File dir = getDevicePath();
        if (dir != null) {
            File[] l = dir.listFiles();
            if (l != null) {
                for (File f : l) {
                    if (f.getName().equals(LAST_PRE_KEY_ID)) {
                        int i = readInt(f);
                        if (i != -1) {
                            return i;
                        }
                    }
                }
            }
        }
        return 0;
    }

    @Override
    public void storeLastPreKeyId(int currentPreKeyId) {
        File dir = getDevicePath();
        if (dir != null) {
            writeInt(new File(dir.getAbsolutePath() + "/" + LAST_PRE_KEY_ID), currentPreKeyId);
        }
    }

    @Override
    public T_IdKeyPair loadOmemoIdentityKeyPair() throws InvalidOmemoKeyException {
        File dir = getDevicePath();
        if (dir != null) {
            byte[] bytes = readBytes(new File(dir.getAbsolutePath() + "/" + IDENTITY_KEY_PAIR));
            if (bytes != null) {
                return keyUtil().identityKeyPairFromBytes(bytes);
            }
        }
        return null;
    }

    @Override
    public void storeOmemoIdentityKeyPair(T_IdKeyPair identityKeyPair) {
        File dir = getDevicePath();
        if (dir != null) {
            writeBytes(keyUtil().identityKeyPairToBytes(identityKeyPair), new File(dir.getAbsolutePath() + "/" + IDENTITY_KEY_PAIR));
        }
    }

    @Override
    public T_IdKey loadOmemoIdentityKey(OmemoDevice device) throws InvalidOmemoKeyException {
        File dir = getContactDevicePath(device);
        if (dir != null) {
            byte[] bytes = readBytes(new File(dir.getAbsolutePath() + "/" + IDENTITY_KEY));
            if (bytes != null) {
                return keyUtil().identityKeyFromBytes(bytes);
            }
        }
        return null;
    }

    @Override
    public void storeOmemoIdentityKey(OmemoDevice device, T_IdKey identityKey) {
        File dir = getContactDevicePath(device);
        if (dir != null) {
            writeBytes(keyUtil().identityKeyToBytes(identityKey), new File(dir.getAbsolutePath() + "/" + IDENTITY_KEY));
        }
    }

    @Override
    public boolean isTrustedOmemoIdentity(OmemoDevice device, T_IdKey identityKey) {
        File dir = getContactDevicePath(device);
        if (dir != null) {
            File f = new File(dir.getAbsolutePath() + "/" + TRUST);
            byte[] bytes = readBytes(f);
            if (bytes != null && bytes.length != 0) {
                if (bytes[0] != '1') {
                    return false;
                }
                byte[] tfp = new byte[bytes.length - 1];
                System.arraycopy(bytes, 1, tfp, 0, tfp.length);
                byte[] fp;
                try {
                    fp = keyUtil().getFingerprint(identityKey).getBytes(StringUtils.UTF8);
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    return false;
                }
                return Arrays.equals(fp, tfp);
            }
        }
        return false;
    }

    @Override
    public boolean isDecidedOmemoIdentity(OmemoDevice device, T_IdKey identityKey) {
        File dir = getContactDevicePath(device);
        if (dir != null) {
            File f = new File(dir.getAbsolutePath() + "/" + TRUST);
            if (f.exists() && f.isFile()) {
                byte[] bytes = readBytes(f);
                if (bytes != null && bytes.length != 0) {
                    byte[] tfp = new byte[bytes.length - 1];
                    System.arraycopy(bytes, 1, tfp, 0, tfp.length);
                    byte[] fp;
                    try {
                        fp = keyUtil().getFingerprint(identityKey).getBytes(StringUtils.UTF8);
                    } catch (UnsupportedEncodingException e) {
                        e.printStackTrace();
                        return false;
                    }
                    return Arrays.equals(fp, tfp);
                }
            }
        }
        return false;
    }

    @Override
    public void trustOmemoIdentity(OmemoDevice device, T_IdKey identityKey) {
        File dir = getContactDevicePath(device);
        if (dir != null) {
            File f = new File(dir.getAbsolutePath() + "/" + TRUST);

            byte[] a;
            try {
                a = keyUtil().getFingerprint(identityKey).getBytes(StringUtils.UTF8);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                return;
            }
            byte[] b = new byte[a.length + 1];
            b[0] = '1';
            System.arraycopy(a, 0, b, 1, a.length);
            writeBytes(b, f);
        }
    }

    @Override
    public void distrustOmemoIdentity(OmemoDevice device, T_IdKey identityKey) {
        File dir = getContactDevicePath(device);
        if (dir != null) {
            File f = new File(dir.getAbsolutePath() + "/" + TRUST);
            byte[] a;
            try {
                a = keyUtil().getFingerprint(identityKey).getBytes(StringUtils.UTF8);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                return;
            }
            byte[] b = new byte[a.length + 1];
            b[0] = '0';
            System.arraycopy(a, 0, b, 1, a.length);
            writeBytes(b, f);
        }
    }

    @Override
    public T_PreKey loadOmemoPreKey(int preKeyId) {
        File dir = getPreKeysPath();
        if (dir != null) {
            File[] keys = dir.listFiles();
            if (keys != null) {
                for (File f : keys) {
                    if (f.getName().equals(Integer.toString(preKeyId))) {
                        byte[] bytes = readBytes(f);
                        if (bytes != null) {
                            try {
                                keyUtil().preKeyPublicFromBytes(bytes);
                            } catch (InvalidOmemoKeyException e) {
                                e.printStackTrace();
                                return null;
                            }
                        }
                    }

                }
            }
        }
        return null;
    }

    @Override
    public void storeOmemoPreKey(int preKeyId, T_PreKey preKeyRecord) {
        File dir = getPreKeysPath();
        if (dir != null) {
            File f = new File(dir.getAbsolutePath() + "/" + preKeyId);
            writeBytes(keyUtil().preKeyToBytes(preKeyRecord), f);
        }
    }

    @Override
    public void removeOmemoPreKey(int preKeyId) {
        File dir = getDevicePath();
        if (dir != null) {
            File f = new File(dir.getAbsolutePath() + "/" + preKeyId);
            f.delete();
        }
    }

    @Override
    public int loadCurrentSignedPreKeyId() {
        File dir = getDevicePath();
        if (dir != null) {
            File f = new File(dir.getAbsolutePath() + "/" + CURRENT_SIGNED_PRE_KEY);
            int i = readInt(f);
            if (i != -1) {
                return i;
            }
        }

        return 0;
    }

    @Override
    public void storeCurrentSignedPreKeyId(int currentSignedPreKeyId) {
        File dir = getDevicePath();
        if (dir != null) {
            File f = new File(dir.getAbsolutePath() + "/" + CURRENT_SIGNED_PRE_KEY);
            writeInt(f, currentSignedPreKeyId);
        }
    }

    @Override
    public HashMap<Integer, T_PreKey> loadOmemoPreKeys() {
        File dir = getPreKeysPath();
        if (dir != null) {
            File[] list = dir.listFiles();
            if (list != null) {
                HashMap<Integer, T_PreKey> preKeys = new HashMap<>();
                for (File f : list) {
                    T_PreKey preKey;
                    try {
                        byte[] bytes = readBytes(f);
                        if (bytes != null) {
                            preKey = keyUtil().preKeyFromBytes(bytes);
                            preKeys.put(Integer.parseInt(f.getName()), preKey);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                return preKeys;
            }
        }
        return null;
    }

    @Override
    public T_SigPreKey loadOmemoSignedPreKey(int signedPreKeyId) {
        File dir = getSignedPreKeysPath();
        if (dir != null) {
            File f = new File(dir.getAbsolutePath() + "/" + signedPreKeyId);
            byte[] bytes = readBytes(f);
            if (bytes != null) {
                try {
                    return keyUtil().signedPreKeyFromBytes(bytes);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    @Override
    public HashMap<Integer, T_SigPreKey> loadOmemoSignedPreKeys() {
        File dir = getSignedPreKeysPath();
        if (dir != null) {
            File[] list = dir.listFiles();
            if (list != null) {
                HashMap<Integer, T_SigPreKey> signedPreKeys = new HashMap<>();
                for (File f : list) {
                    byte[] bytes = readBytes(f);
                    try {
                        if (bytes != null) {
                            T_SigPreKey s = keyUtil().signedPreKeyFromBytes(bytes);
                            signedPreKeys.put(Integer.parseInt(f.getName()), s);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                return signedPreKeys;
            }
        }
        return null;
    }

    @Override
    public void storeOmemoSignedPreKey(int signedPreKeyId, T_SigPreKey signedPreKey) {
        File dir = getSignedPreKeysPath();
        if (dir != null) {
            File f = new File(dir.getAbsolutePath() + "/" + signedPreKeyId);
            writeBytes(keyUtil().signedPreKeyToBytes(signedPreKey), f);
        }
    }

    @Override
    public void removeOmemoSignedPreKey(int signedPreKeyId) {
        File dir = getSignedPreKeysPath();
        if (dir != null) {
            File f = new File(dir.getAbsolutePath() + "/" + signedPreKeyId);
            f.delete();
        }
    }

    @Override
    public T_Sess loadRawSession(OmemoDevice device) {
        File dir = getContactDevicePath(device);
        if (dir != null) {
            File f = new File(dir.getAbsolutePath() + "/" + SESSION);
            byte[] bytes = readBytes(f);
            if (bytes != null) {
                try {
                    return keyUtil().rawSessionFromBytes(bytes);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    @Override
    public HashMap<Integer, T_Sess> loadAllRawSessionsOf(BareJid contact) {
        File dir = getContactsPath();
        if (dir != null) {
            dir = create(new File(dir.getAbsolutePath() + "/" + contact.toString()));
            File[] list = dir.listFiles();
            if (list != null) {
                HashMap<Integer, T_Sess> sessions = new HashMap<>();
                for (File f : list) {
                    if (f.isDirectory()) {
                        try {
                            int id = Integer.parseInt(f.getName());
                            T_Sess s = loadRawSession(new OmemoDevice(contact, id));
                            if (s != null) {
                                sessions.put(id, s);
                            }
                        } catch (NumberFormatException ignored) {

                        }
                    }
                }
                return sessions;
            }
        }
        return null;
    }

    @Override
    public void storeRawSession(OmemoDevice device, T_Sess session) {
        File dir = getContactDevicePath(device);
        if (dir != null) {
            File f = new File(dir.getAbsolutePath() + "/" + SESSION);
            writeBytes(keyUtil().rawSessionToBytes(session), f);
        }
    }

    @Override
    public void removeRawSession(OmemoDevice device) {
        File dir = getContactDevicePath(device);
        if (dir != null) {
            File f = new File(dir.getAbsolutePath() + "/" + SESSION);
            f.delete();
        }
    }

    @Override
    public void removeAllRawSessionsOf(BareJid contact) {
        File dir = getContactsPath();
        if (dir != null) {
            File f = new File(dir.getAbsolutePath() + "/" + contact.toString());
            f.delete();
        }
    }

    @Override
    public boolean containsRawSession(OmemoDevice device) {
        File dir = getContactDevicePath(device);
        return dir != null && new File(dir.getAbsolutePath() + "/" + SESSION).exists();
    }

    @Override
    public CachedDeviceList loadCachedDeviceList(BareJid contact) {
        if (contact == null) {
            return null;
        }
        File dir = getContactsPath();
        if (dir != null) {
            dir = create(new File(dir.getAbsolutePath() + "/" + contact.toString()));
            File f = new File(dir.getAbsolutePath() + "/" + DEVICE_LIST);
            byte[] bytes = readBytes(f);
            if (bytes != null) {
                try {
                    String s = new String(bytes, StringUtils.UTF8);
                    if (s.contains("a:") && s.contains("i:")) {
                        CachedDeviceList cachedDeviceList = new CachedDeviceList();

                        String a = s.substring(s.indexOf("a:") + 2, s.indexOf("i:"));
                        String[] ids = a.split(",");
                        for (String id : ids) {
                            cachedDeviceList.addDevice(Integer.parseInt(id));
                        }

                        String i = s.substring(s.indexOf("i:") + 2);
                        String[] iids = i.split(",");
                        for (String iid : iids) {
                            if (iid.length() > 0) {
                                cachedDeviceList.getInactiveDevices().add(Integer.parseInt(iid));
                            }
                        }

                        return cachedDeviceList;
                    }
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                    return null;
                }
            }
        }
        return null;
    }

    @Override
    public void storeCachedDeviceList(BareJid contact, CachedDeviceList deviceList) {
        if (contact == null) {
            return;
        }
        File dir = getContactsPath();
        if (dir != null) {
            dir = create(new File(dir.getAbsolutePath() + "/" + contact.toString()));

            String s = "a:";
            for (int i : deviceList.getActiveDevices()) {
                s += i + ",";
            }
            if (s.endsWith(",")) {
                s = s.substring(0, s.length() - 1);
            }
            s += "i:";
            for (int i : deviceList.getInactiveDevices()) {
                s += i + ",";
            }
            if (s.endsWith(",")) {
                s = s.substring(0, s.length() - 1);
            }

            File f = new File(dir + "/" + DEVICE_LIST);
            try {
                writeBytes(s.getBytes(StringUtils.UTF8), f);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
        }
    }

    public File getOmemoPath() {
        return create(new File(base.getAbsolutePath() + "/omemo"));
    }

    public File getUserPath() {
        return create(new File(getOmemoPath().getAbsolutePath() + "/" + userJid.toString() + "/"));
    }

    public File getDevicePath() {
        File[] content = getUserPath().listFiles();
        if (content != null && content.length == 1) {
            int devId = Integer.parseInt(content[0].getName());
            return create(new File(getUserPath() + "/" + devId));
        } else {
            return null;
        }
    }

    public File getContactsPath() {
        File f = getDevicePath();
        if (f != null) {
            return create(new File(f.getAbsolutePath() + "/contacts"));
        }
        return null;
    }

    public File getContactDevicePath(OmemoDevice device) {
        File dir = getContactsPath();
        if (dir != null) {
            return create(new File(dir.getAbsolutePath() + "/" + device.getJid().toString() + "/" + device.getDeviceId()));
        }
        return null;
    }

    public File getIdentityKeyPath() {
        File f = getDevicePath();
        if (f != null) {
            return create(new File(f.getAbsolutePath() + "/identityKey"));
        }
        return null;
    }

    public File getSignedPreKeysPath() {
        File f = getDevicePath();
        if (f != null) {
            return create(new File(f.getAbsolutePath() + "/signedPreKeys"));
        }
        return null;
    }

    public File getPreKeysPath() {
        File f = getDevicePath();
        if (f != null) {
            return create(new File(f.getAbsolutePath() + "/preKeys"));
        }
        return null;
    }

    public File create(File path) {
        if (!path.exists()) {
            path.mkdirs();
        }
        return path;
    }

    public void writeBytes(byte[] data, File destination) {
        FileOutputStream fos = null;
        try {
            if (!destination.exists()) {
                destination.createNewFile();
            }
            fos = new FileOutputStream(destination);
            fos.write(data);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fos != null)
                    fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public byte[] readBytes(File from) {
        if (from.exists()) {
            FileInputStream fis = null;
            try {
                fis = new FileInputStream(from);
                byte[] buffer = new byte[(int) from.length()];
                fis.read(buffer);
                return buffer;
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (fis != null)
                        fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }

    public void writeInt(File to, int i) {
        try {
            writeBytes(Integer.toString(i).getBytes(StringUtils.UTF8), to);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
    }

    public int readInt(File from) {
        byte[] bytes = readBytes(from);
        if (bytes != null) {
            try {
                return Integer.parseInt(new String(bytes, StringUtils.UTF8));
            } catch (NumberFormatException | UnsupportedEncodingException ignored) {
                ignored.printStackTrace();
            }
        }
        return -1;
    }
}
