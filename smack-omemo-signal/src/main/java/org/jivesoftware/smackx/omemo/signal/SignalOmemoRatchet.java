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

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smackx.omemo.OmemoManager;
import org.jivesoftware.smackx.omemo.OmemoRatchet;
import org.jivesoftware.smackx.omemo.OmemoStore;
import org.jivesoftware.smackx.omemo.element.OmemoElement;
import org.jivesoftware.smackx.omemo.exceptions.CorruptedOmemoKeyException;
import org.jivesoftware.smackx.omemo.exceptions.CryptoFailedException;
import org.jivesoftware.smackx.omemo.exceptions.NoRawSessionException;
import org.jivesoftware.smackx.omemo.exceptions.UntrustedOmemoIdentityException;
import org.jivesoftware.smackx.omemo.internal.CiphertextTuple;
import org.jivesoftware.smackx.omemo.internal.OmemoDevice;

import org.whispersystems.libsignal.DuplicateMessageException;
import org.whispersystems.libsignal.IdentityKey;
import org.whispersystems.libsignal.IdentityKeyPair;
import org.whispersystems.libsignal.InvalidKeyException;
import org.whispersystems.libsignal.InvalidKeyIdException;
import org.whispersystems.libsignal.InvalidMessageException;
import org.whispersystems.libsignal.InvalidVersionException;
import org.whispersystems.libsignal.LegacyMessageException;
import org.whispersystems.libsignal.NoSessionException;
import org.whispersystems.libsignal.SessionCipher;
import org.whispersystems.libsignal.SignalProtocolAddress;
import org.whispersystems.libsignal.UntrustedIdentityException;
import org.whispersystems.libsignal.ecc.ECPublicKey;
import org.whispersystems.libsignal.protocol.CiphertextMessage;
import org.whispersystems.libsignal.protocol.PreKeySignalMessage;
import org.whispersystems.libsignal.protocol.SignalMessage;
import org.whispersystems.libsignal.state.PreKeyBundle;
import org.whispersystems.libsignal.state.PreKeyRecord;
import org.whispersystems.libsignal.state.SessionRecord;
import org.whispersystems.libsignal.state.SignedPreKeyRecord;

public class SignalOmemoRatchet
        extends OmemoRatchet<IdentityKeyPair, IdentityKey, PreKeyRecord, SignedPreKeyRecord, SessionRecord,
                SignalProtocolAddress, ECPublicKey, PreKeyBundle, SessionCipher> {

    private static final Logger LOGGER = Logger.getLogger(OmemoRatchet.class.getName());
    private final SignalOmemoStoreConnector storeConnector;

    SignalOmemoRatchet(OmemoManager omemoManager,
                              OmemoStore<IdentityKeyPair, IdentityKey, PreKeyRecord, SignedPreKeyRecord,
                                             SessionRecord, SignalProtocolAddress, ECPublicKey, PreKeyBundle,
                                             SessionCipher> store) {
        super(omemoManager, store);
        this.storeConnector = new SignalOmemoStoreConnector(omemoManager, store);
    }

    @Override
    public byte[] doubleRatchetDecrypt(OmemoDevice sender, byte[] encryptedKey)
            throws CorruptedOmemoKeyException, NoRawSessionException, CryptoFailedException,
            UntrustedOmemoIdentityException, IOException {

        SessionCipher cipher = getCipher(sender);
        byte[] decryptedKey;

        // Try to handle the message as a PreKeySignalMessage...
        try {
            PreKeySignalMessage preKeyMessage = new PreKeySignalMessage(encryptedKey);

            if (!preKeyMessage.getPreKeyId().isPresent()) {
                throw new CryptoFailedException("PreKeyMessage did not contain a preKeyId.");
            }

            IdentityKey messageIdentityKey = preKeyMessage.getIdentityKey();
            IdentityKey previousIdentityKey = store.loadOmemoIdentityKey(storeConnector.getOurDevice(), sender);

            if (previousIdentityKey != null &&
                    !previousIdentityKey.getFingerprint().equals(messageIdentityKey.getFingerprint())) {
                throw new UntrustedOmemoIdentityException(sender,
                        store.keyUtil().getFingerprintOfIdentityKey(previousIdentityKey),
                        store.keyUtil().getFingerprintOfIdentityKey(messageIdentityKey));
            }

            try {
                decryptedKey = cipher.decrypt(preKeyMessage);
            }
            catch (UntrustedIdentityException e) {
                throw new AssertionError("Signals trust management MUST be disabled.");
            }
            catch (LegacyMessageException | InvalidKeyException e) {
                throw new CryptoFailedException(e);
            }
            catch (InvalidKeyIdException e) {
                throw new NoRawSessionException(sender, e);
            }
            catch (DuplicateMessageException e) {
                LOGGER.log(Level.INFO, "Decryption of PreKeyMessage from " + sender +
                        " failed, since the message has been decrypted before.");
                return null;
            }

        } catch (InvalidVersionException | InvalidMessageException noPreKeyMessage) {
            // ...if that fails, handle it as a SignalMessage
            try {
                SignalMessage message = new SignalMessage(encryptedKey);
                decryptedKey = getCipher(sender).decrypt(message);
            }
            catch (UntrustedIdentityException e) {
                throw new AssertionError("Signals trust management MUST be disabled.");
            }
            catch (InvalidMessageException | NoSessionException e) {
                throw new NoRawSessionException(sender, e);
            }
            catch (LegacyMessageException e) {
                throw new CryptoFailedException(e);
            }
            catch (DuplicateMessageException e1) {
                LOGGER.log(Level.INFO, "Decryption of SignalMessage from " + sender +
                        " failed, since the message has been decrypted before.");
                return null;
            }
        }

        return decryptedKey;
    }

    @Override
    public CiphertextTuple doubleRatchetEncrypt(OmemoDevice recipient, byte[] messageKey) {
        CiphertextMessage ciphertextMessage;
        try {
            ciphertextMessage = getCipher(recipient).encrypt(messageKey);
        } catch (UntrustedIdentityException e) {
            throw new AssertionError("Signals trust management MUST be disabled.");
        }

        // TODO: Figure out, if this is enough...
        int type = ciphertextMessage.getType() == CiphertextMessage.PREKEY_TYPE ?
                OmemoElement.TYPE_OMEMO_PREKEY_MESSAGE : OmemoElement.TYPE_OMEMO_MESSAGE;

        return new CiphertextTuple(ciphertextMessage.serialize(), type);
    }

    private SessionCipher getCipher(OmemoDevice device) {
        return new SessionCipher(storeConnector, storeConnector, storeConnector, storeConnector,
                SignalOmemoStoreConnector.asAddress(device));
    }
}
