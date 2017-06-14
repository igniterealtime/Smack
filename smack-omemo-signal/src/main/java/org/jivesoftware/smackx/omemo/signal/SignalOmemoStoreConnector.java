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
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smackx.omemo.OmemoManager;
import org.jivesoftware.smackx.omemo.OmemoStore;
import org.jivesoftware.smackx.omemo.exceptions.CorruptedOmemoKeyException;

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

    private final OmemoManager omemoManager;
    private final OmemoStore<IdentityKeyPair, IdentityKey, PreKeyRecord, SignedPreKeyRecord, SessionRecord, SignalProtocolAddress, ECPublicKey, PreKeyBundle, SessionCipher>
            omemoStore;

    public SignalOmemoStoreConnector(OmemoManager omemoManager, OmemoStore<IdentityKeyPair, IdentityKey, PreKeyRecord, SignedPreKeyRecord, SessionRecord, SignalProtocolAddress, ECPublicKey, PreKeyBundle, SessionCipher> store) {
        this.omemoManager = omemoManager;
        this.omemoStore = store;
    }

    @Override
    public IdentityKeyPair getIdentityKeyPair() {
        try {
            return omemoStore.loadOmemoIdentityKeyPair(omemoManager);
        } catch (CorruptedOmemoKeyException e) {
            LOGGER.log(Level.SEVERE, "getIdentityKeyPair has failed: " + e, e);
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
    public void saveIdentity(SignalProtocolAddress signalProtocolAddress, IdentityKey identityKey) {
        try {
            omemoStore.storeOmemoIdentityKey(omemoManager, omemoStore.keyUtil().addressAsOmemoDevice(signalProtocolAddress), identityKey);
        } catch (XmppStringprepException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public boolean isTrustedIdentity(SignalProtocolAddress signalProtocolAddress, IdentityKey identityKey) {
        //Disable internal trust management. Instead we use OmemoStore.isTrustedOmemoIdentity() before encrypting for a
        //recipient.
        return true;
    }

    @Override
    public PreKeyRecord loadPreKey(int i) throws InvalidKeyIdException {
        PreKeyRecord pr = omemoStore.loadOmemoPreKey(omemoManager, i);
        if (pr == null) {
            throw new InvalidKeyIdException("No PreKey with Id " + i + " found!");
        }
        return pr;
    }

    @Override
    public void storePreKey(int i, PreKeyRecord preKeyRecord) {
        omemoStore.storeOmemoPreKey(omemoManager, i, preKeyRecord);
    }

    @Override
    public boolean containsPreKey(int i) {
        try {
            return (loadPreKey(i) != null);
        } catch (InvalidKeyIdException e) {
            LOGGER.log(Level.WARNING, "containsPreKey has failed: " + e.getMessage());
            return false;
        }
    }

    @Override
    public void removePreKey(int i) {
        omemoStore.removeOmemoPreKey(omemoManager, i);
    }

    @Override
    public SessionRecord loadSession(SignalProtocolAddress signalProtocolAddress) {
        try {
            SessionRecord s = omemoStore.loadRawSession(omemoManager, omemoStore.keyUtil().addressAsOmemoDevice(signalProtocolAddress));
            return (s != null ? s : new SessionRecord());
        } catch (XmppStringprepException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public List<Integer> getSubDeviceSessions(String s) {
        HashMap<Integer, SessionRecord> contactsSessions;
        try {
            contactsSessions = omemoStore.loadAllRawSessionsOf(omemoManager, JidCreate.bareFrom(s));
        } catch (XmppStringprepException e) {
            throw new AssertionError(e);
        }
        if (contactsSessions != null) {
            return new ArrayList<>(contactsSessions.keySet());
        }
        return new ArrayList<>();
    }

    @Override
    public void storeSession(SignalProtocolAddress signalProtocolAddress, SessionRecord sessionRecord) {
        try {
            omemoStore.storeRawSession(omemoManager, omemoStore.keyUtil().addressAsOmemoDevice(signalProtocolAddress), sessionRecord);
        } catch (XmppStringprepException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public boolean containsSession(SignalProtocolAddress signalProtocolAddress) {
        try {
            return omemoStore.containsRawSession(omemoManager, omemoStore.keyUtil().addressAsOmemoDevice(signalProtocolAddress));
        } catch (XmppStringprepException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public void deleteSession(SignalProtocolAddress signalProtocolAddress) {
        try {
            omemoStore.removeRawSession(omemoManager, omemoStore.keyUtil().addressAsOmemoDevice(signalProtocolAddress));
        } catch (XmppStringprepException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public void deleteAllSessions(String s) {
        try {
            omemoStore.removeAllRawSessionsOf(omemoManager, JidCreate.bareFrom(s));
        } catch (XmppStringprepException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public SignedPreKeyRecord loadSignedPreKey(int i) throws InvalidKeyIdException {
        SignedPreKeyRecord spkr = omemoStore.loadOmemoSignedPreKey(omemoManager, i);
        if (spkr == null) {
            throw new InvalidKeyIdException("No SignedPreKey with Id " + i + " found!");
        }
        return spkr;
    }

    @Override
    public List<SignedPreKeyRecord> loadSignedPreKeys() {
        HashMap<Integer, SignedPreKeyRecord> signedPreKeyRecordHashMap = omemoStore.loadOmemoSignedPreKeys(omemoManager);
        List<SignedPreKeyRecord> signedPreKeyRecordList = new ArrayList<>();
        signedPreKeyRecordList.addAll(signedPreKeyRecordHashMap.values());
        return signedPreKeyRecordList;
    }

    @Override
    public void storeSignedPreKey(int i, SignedPreKeyRecord signedPreKeyRecord) {
        omemoStore.storeOmemoSignedPreKey(omemoManager, i, signedPreKeyRecord);
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
        omemoStore.removeOmemoSignedPreKey(omemoManager, i);
    }
}
