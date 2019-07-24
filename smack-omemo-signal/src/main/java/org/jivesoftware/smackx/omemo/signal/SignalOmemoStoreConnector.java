/**
 *
 * Copyright 2017 Paul Schaub
 *
 * This file is part of smack-omemo-signal.
 *
 * smack-omemo-signal is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301  USA
 */
package org.jivesoftware.smackx.omemo.signal;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smackx.omemo.OmemoManager;
import org.jivesoftware.smackx.omemo.OmemoStore;
import org.jivesoftware.smackx.omemo.exceptions.CorruptedOmemoKeyException;
import org.jivesoftware.smackx.omemo.internal.OmemoDevice;

import org.jxmpp.jid.BareJid;
import org.jxmpp.jid.impl.JidCreate;
import org.jxmpp.stringprep.XmppStringprepException;
import org.whispersystems.libsignal.IdentityKey;
import org.whispersystems.libsignal.IdentityKeyPair;
import org.whispersystems.libsignal.InvalidKeyIdException;
import org.whispersystems.libsignal.SessionCipher;
import org.whispersystems.libsignal.SignalProtocolAddress;
import org.whispersystems.libsignal.ecc.ECPublicKey;
import org.whispersystems.libsignal.state.IdentityKeyStore;
import org.whispersystems.libsignal.state.PreKeyBundle;
import org.whispersystems.libsignal.state.PreKeyRecord;
import org.whispersystems.libsignal.state.PreKeyStore;
import org.whispersystems.libsignal.state.SessionRecord;
import org.whispersystems.libsignal.state.SessionStore;
import org.whispersystems.libsignal.state.SignedPreKeyRecord;
import org.whispersystems.libsignal.state.SignedPreKeyStore;

/**
 * Class that adapts libsignal-protocol-java's Store classes to the OmemoStore class.
 *
 * @author Paul Schaub
 */
public class SignalOmemoStoreConnector
        implements IdentityKeyStore, SessionStore, PreKeyStore, SignedPreKeyStore {

    private static final Logger LOGGER = Logger.getLogger(SignalOmemoStoreConnector.class.getName());

    private final OmemoStore<IdentityKeyPair, IdentityKey, PreKeyRecord, SignedPreKeyRecord, SessionRecord,
            SignalProtocolAddress, ECPublicKey, PreKeyBundle, SessionCipher>
            omemoStore;
    private final OmemoManager omemoManager;

    public SignalOmemoStoreConnector(OmemoManager omemoManager, OmemoStore<IdentityKeyPair,
            IdentityKey, PreKeyRecord, SignedPreKeyRecord, SessionRecord, SignalProtocolAddress, ECPublicKey,
            PreKeyBundle, SessionCipher> store) {
        this.omemoManager = omemoManager;
        this.omemoStore = store;
    }

    OmemoDevice getOurDevice() {
        return omemoManager.getOwnDevice();
    }

    @Override
    public IdentityKeyPair getIdentityKeyPair() {
        try {
            return omemoStore.loadOmemoIdentityKeyPair(getOurDevice());
        } catch (CorruptedOmemoKeyException e) {
            LOGGER.log(Level.SEVERE, "IdentityKeyPair seems to be invalid.", e);
            return null;
        }
    }

    /**
     * We don't use this.
     * @return dummy
     */
    @Override
    public int getLocalRegistrationId() {
        return 0;
    }

    @Override
    public boolean saveIdentity(SignalProtocolAddress signalProtocolAddress, IdentityKey identityKey) {
        OmemoDevice device;
        try {
            device = asOmemoDevice(signalProtocolAddress);
        } catch (XmppStringprepException e) {
            throw new AssertionError(e);
        }

        omemoStore.storeOmemoIdentityKey(getOurDevice(), device, identityKey);
        return true;
    }

    @Override
    public boolean isTrustedIdentity(SignalProtocolAddress signalProtocolAddress,
                                     IdentityKey identityKey,
                                     Direction direction) {
        // Disable internal trust management. Instead we use OmemoStore.isTrustedOmemoIdentity() before encrypting
        // for a recipient.
        return true;
    }

    @Override
    public PreKeyRecord loadPreKey(int i) throws InvalidKeyIdException {
        PreKeyRecord preKey = omemoStore.loadOmemoPreKey(getOurDevice(), i);

        if (preKey == null) {
            throw new InvalidKeyIdException("No PreKey with Id " + i + " found.");
        }

        return preKey;
    }

    @Override
    public void storePreKey(int i, PreKeyRecord preKeyRecord) {
        omemoStore.storeOmemoPreKey(getOurDevice(), i, preKeyRecord);
    }

    @Override
    public boolean containsPreKey(int i) {
        try {
            return loadPreKey(i) != null;
        } catch (InvalidKeyIdException e) {
            return false;
        }
    }

    @Override
    public void removePreKey(int i) {
        omemoStore.removeOmemoPreKey(getOurDevice(), i);
    }

    @Override
    public SessionRecord loadSession(SignalProtocolAddress signalProtocolAddress) {
        OmemoDevice device;
        try {
            device = asOmemoDevice(signalProtocolAddress);
        } catch (XmppStringprepException e) {
            throw new AssertionError(e);
        }

        SessionRecord record = omemoStore.loadRawSession(getOurDevice(), device);

        if (record != null) {
            return record;
        } else {
            return new SessionRecord();
        }
    }

    @Override
    public List<Integer> getSubDeviceSessions(String s) {
        BareJid jid;
        try {
            jid = JidCreate.bareFrom(s);
        } catch (XmppStringprepException e) {
            throw new AssertionError(e);
        }

        return new ArrayList<>(omemoStore.loadAllRawSessionsOf(getOurDevice(), jid).keySet());
    }

    @Override
    public void storeSession(SignalProtocolAddress signalProtocolAddress, SessionRecord sessionRecord) {
        OmemoDevice device;
        try {
            device = asOmemoDevice(signalProtocolAddress);
        } catch (XmppStringprepException e) {
            throw new AssertionError(e);
        }

        omemoStore.storeRawSession(getOurDevice(), device, sessionRecord);
    }

    @Override
    public boolean containsSession(SignalProtocolAddress signalProtocolAddress) {
        OmemoDevice device;
        try {
            device = asOmemoDevice(signalProtocolAddress);
        } catch (XmppStringprepException e) {
            throw new AssertionError(e);
        }

        return omemoStore.containsRawSession(getOurDevice(), device);
    }

    @Override
    public void deleteSession(SignalProtocolAddress signalProtocolAddress) {
        OmemoDevice device;
        try {
            device = asOmemoDevice(signalProtocolAddress);
        } catch (XmppStringprepException e) {
            throw new AssertionError(e);
        }

        omemoStore.removeRawSession(getOurDevice(), device);
    }

    @Override
    public void deleteAllSessions(String s) {
        BareJid jid;
        try {
            jid = JidCreate.bareFrom(s);
        } catch (XmppStringprepException e) {
            throw new AssertionError(e);
        }

        omemoStore.removeAllRawSessionsOf(getOurDevice(), jid);
    }

    @Override
    public SignedPreKeyRecord loadSignedPreKey(int i) throws InvalidKeyIdException {
        SignedPreKeyRecord signedPreKeyRecord = omemoStore.loadOmemoSignedPreKey(getOurDevice(), i);
        if (signedPreKeyRecord == null) {
            throw new InvalidKeyIdException("No signed preKey with id " + i + " found.");
        }
        return signedPreKeyRecord;
    }

    @Override
    public List<SignedPreKeyRecord> loadSignedPreKeys() {

        TreeMap<Integer, SignedPreKeyRecord> signedPreKeyRecordHashMap =
                omemoStore.loadOmemoSignedPreKeys(getOurDevice());
        return new ArrayList<>(signedPreKeyRecordHashMap.values());
    }

    @Override
    public void storeSignedPreKey(int i, SignedPreKeyRecord signedPreKeyRecord) {
        omemoStore.storeOmemoSignedPreKey(getOurDevice(), i, signedPreKeyRecord);
    }

    @Override
    public boolean containsSignedPreKey(int i) {
        try {
            return loadSignedPreKey(i) != null;
        } catch (InvalidKeyIdException e) {
            LOGGER.log(Level.WARNING, "containsSignedPreKey has failed: " + e.getMessage());
            return false;
        }
    }

    @Override
    public void removeSignedPreKey(int i) {
        omemoStore.removeOmemoSignedPreKey(getOurDevice(), i);
    }

    private static OmemoDevice asOmemoDevice(SignalProtocolAddress address) throws XmppStringprepException {
        return new OmemoDevice(JidCreate.bareFrom(address.getName()), address.getDeviceId());
    }

    public static SignalProtocolAddress asAddress(OmemoDevice device) {
        return new SignalProtocolAddress(device.getJid().toString(), device.getDeviceId());
    }
}
