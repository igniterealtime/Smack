/*
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

import org.jivesoftware.smackx.omemo.element.OmemoBundleElement;
import org.jivesoftware.smackx.omemo.exceptions.CorruptedOmemoKeyException;
import org.jivesoftware.smackx.omemo.internal.OmemoDevice;
import org.jivesoftware.smackx.omemo.trust.OmemoFingerprint;
import org.jivesoftware.smackx.omemo.util.OmemoKeyUtil;

import org.signal.libsignal.protocol.IdentityKey;
import org.signal.libsignal.protocol.IdentityKeyPair;
import org.signal.libsignal.protocol.InvalidKeyException;
import org.signal.libsignal.protocol.InvalidMessageException;
import org.signal.libsignal.protocol.ecc.Curve;
import org.signal.libsignal.protocol.ecc.ECKeyPair;
import org.signal.libsignal.protocol.ecc.ECPublicKey;
import org.signal.libsignal.protocol.state.PreKeyBundle;
import org.signal.libsignal.protocol.state.PreKeyRecord;
import org.signal.libsignal.protocol.state.SessionRecord;
import org.signal.libsignal.protocol.state.SignedPreKeyRecord;
import org.signal.libsignal.protocol.util.Medium;

/**
 * Concrete implementation of the KeyUtil for an implementation using the Signal library.
 *
 * @author Paul Schaub
 */
public class SignalOmemoKeyUtil extends OmemoKeyUtil<IdentityKeyPair, IdentityKey, PreKeyRecord, SignedPreKeyRecord,
        SessionRecord, ECPublicKey, PreKeyBundle> {

    @Override
    public IdentityKeyPair generateOmemoIdentityKeyPair() {
        return IdentityKeyPair.generate();
    }

    @Override
    @SuppressWarnings("NonApiType")
    public TreeMap<Integer, PreKeyRecord> generateOmemoPreKeys(int currentPreKeyId, int count) {
        List<PreKeyRecord> preKeyRecords = generatePreKeys(currentPreKeyId, count);
        TreeMap<Integer, PreKeyRecord> map = new TreeMap<>();
        for (PreKeyRecord p : preKeyRecords) {
            map.put(p.getId(), p);
        }
        return map;
    }

    private static List<PreKeyRecord> generatePreKeys(int start, int count) {
        List<PreKeyRecord> results = new ArrayList<>(count);

        start--;

        for (int i = 0; i < count; i++) {
            int pkIdx = ((start + i) % (Medium.MAX_VALUE - 1)) + 1;
            results.add(new PreKeyRecord(pkIdx, Curve.generateKeyPair()));
        }

        return results;
    }

    @Override
    public SignedPreKeyRecord generateOmemoSignedPreKey(IdentityKeyPair identityKeyPair, int currentPreKeyId)
            throws CorruptedOmemoKeyException {
        try {
            return generateSignedPreKey(identityKeyPair, currentPreKeyId);
        } catch (InvalidKeyException e) {
            throw new CorruptedOmemoKeyException(e);
        }
    }

    private static SignedPreKeyRecord generateSignedPreKey(IdentityKeyPair identityKeyPair, int signedPreKeyId) throws InvalidKeyException {
        ECKeyPair keyPair   = Curve.generateKeyPair();
        byte[]    signature = Curve.calculateSignature(identityKeyPair.getPrivateKey(), keyPair.getPublicKey().serialize());

        return new SignedPreKeyRecord(signedPreKeyId, System.currentTimeMillis(), keyPair, signature);
    }

    @Override
    public SignedPreKeyRecord signedPreKeyFromBytes(byte[] data) throws IOException {
        if (data == null) return null;
        try {
            return new SignedPreKeyRecord(data);
        } catch (InvalidMessageException e) {
            throw new IOException(e);
        }
    }

    @Override
    public byte[] signedPreKeyToBytes(SignedPreKeyRecord signedPreKeyRecord) {
        return signedPreKeyRecord.serialize();
    }

    @Override
    public SessionRecord rawSessionFromBytes(byte[] data) throws IOException {
        if (data == null) return null;
        try {
            return new SessionRecord(data);
        } catch (InvalidMessageException e) {
            throw new IOException(e);
        }
    }

    @Override
    public byte[] rawSessionToBytes(SessionRecord session) {
        return session.serialize();
    }

    @Override
    public IdentityKeyPair identityKeyPairFromBytes(byte[] data) throws CorruptedOmemoKeyException {
        if (data == null) return null;
        return new IdentityKeyPair(data);
    }

    @Override
    public IdentityKey identityKeyFromBytes(byte[] data) throws CorruptedOmemoKeyException {
        if (data == null) return null;
        try {
            return new IdentityKey(data, 0);
        } catch (InvalidKeyException e) {
            throw new CorruptedOmemoKeyException(e);
        }
    }

    @Override
    public ECPublicKey ellipticCurvePublicKeyFromBytes(byte[] data) throws CorruptedOmemoKeyException {
        if (data == null) return null;
        try {
            return Curve.decodePoint(data, 0);
        } catch (InvalidKeyException e) {
            throw new CorruptedOmemoKeyException(e);
        }
    }

    @Override
    public byte[] preKeyToBytes(PreKeyRecord preKeyRecord) {
        return preKeyRecord.serialize();
    }

    @Override
    public PreKeyRecord preKeyFromBytes(byte[] bytes) throws IOException {
        if (bytes == null) return null;
        try {
            return new PreKeyRecord(bytes);
        } catch (InvalidMessageException e) {
            throw new IOException(e);
        }
    }

    @Override
    public PreKeyBundle bundleFromOmemoBundle(OmemoBundleElement bundle, OmemoDevice contact, int preKeyId)
            throws CorruptedOmemoKeyException {
        return new PreKeyBundle(0,
                contact.getDeviceId(),
                preKeyId,
                BUNDLE.preKeyPublic(bundle, preKeyId),
                BUNDLE.signedPreKeyId(bundle),
                BUNDLE.signedPreKeyPublic(bundle),
                BUNDLE.signedPreKeySignature(bundle),
                BUNDLE.identityKey(bundle));
    }

    @Override
    public byte[] signedPreKeySignatureFromKey(SignedPreKeyRecord signedPreKey) {
        return signedPreKey.getSignature();
    }

    @Override
    public int signedPreKeyIdFromKey(SignedPreKeyRecord signedPreKey) {
        return signedPreKey.getId();
    }

    @Override
    public byte[] identityKeyPairToBytes(IdentityKeyPair identityKeyPair) {
        return identityKeyPair.serialize();
    }

    @Override
    public IdentityKey identityKeyFromPair(IdentityKeyPair identityKeyPair) {
        return identityKeyPair.getPublicKey();
    }

    @Override
    public byte[] identityKeyForBundle(IdentityKey identityKey) {
        return identityKey.getPublicKey().serialize();
    }

    @Override
    public byte[] identityKeyToBytes(IdentityKey identityKey) {
        return identityKey.serialize();
    }

    @Override
    public byte[] preKeyPublicKeyForBundle(ECPublicKey preKey) {
        return preKey.serialize();
    }

    @Override
    public byte[] preKeyForBundle(PreKeyRecord preKeyRecord) {
        try {
            return preKeyRecord.getKeyPair().getPublicKey().serialize();
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public byte[] signedPreKeyPublicForBundle(SignedPreKeyRecord signedPreKey) {
        return signedPreKey.getKeyPair().getPublicKey().serialize();
    }

    @Override
    public OmemoFingerprint getFingerprintOfIdentityKey(IdentityKey identityKey) {
        if (identityKey == null) {
            return null;
        }

        String fp = identityKey.getFingerprint();
        // Cut "(byte)0x" prefixes, remove spaces and commas, cut first two digits.
        fp = fp.replace("(byte)0x", "").replace(",", "")
                .replace(" ", "").substring(2);
        return new OmemoFingerprint(fp);
    }

    @Override
    public OmemoFingerprint getFingerprintOfIdentityKeyPair(IdentityKeyPair identityKeyPair) {
        if (identityKeyPair == null) {
            return null;
        }
        return getFingerprintOfIdentityKey(identityKeyPair.getPublicKey());
    }
}
