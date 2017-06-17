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

import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smackx.omemo.OmemoManager;
import org.jivesoftware.smackx.omemo.OmemoStore;
import org.jivesoftware.smackx.omemo.element.OmemoElement;
import org.jivesoftware.smackx.omemo.exceptions.NoRawSessionException;
import org.jivesoftware.smackx.omemo.internal.CiphertextTuple;
import org.jivesoftware.smackx.omemo.internal.OmemoDevice;
import org.jivesoftware.smackx.omemo.internal.OmemoSession;

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

/**
 * Concrete implementation of the OmemoSession using the Signal library.
 *
 * @author Paul Schaub
 */
public class SignalOmemoSession extends OmemoSession<IdentityKeyPair, IdentityKey, PreKeyRecord, SignedPreKeyRecord, SessionRecord, SignalProtocolAddress, ECPublicKey, PreKeyBundle, SessionCipher> {
    private static final Logger LOGGER = Logger.getLogger(SignalOmemoSession.class.getName());

    /**
     * Constructor used when the remote user initialized the session using a PreKeyOmemoMessage.
     *
     * @param omemoManager  omemoManager
     * @param omemoStore    omemoStoreConnector that can be used to get information from
     * @param remoteContact omemoDevice of the remote contact
     * @param identityKey   identityKey of the remote contact
     */
    SignalOmemoSession(OmemoManager omemoManager, OmemoStore<IdentityKeyPair, IdentityKey, PreKeyRecord, SignedPreKeyRecord, SessionRecord, SignalProtocolAddress, ECPublicKey, PreKeyBundle, SessionCipher> omemoStore,
                       OmemoDevice remoteContact, IdentityKey identityKey) {
        super(omemoManager, omemoStore, remoteContact, identityKey);
    }

    /**
     * Constructor used when we initiate a new Session with the remote user.
     *
     * @param omemoManager  omemoManager
     * @param omemoStore    omemoStore used to get information from
     * @param remoteContact omemoDevice of the remote contact
     */
    SignalOmemoSession(OmemoManager omemoManager,
                       OmemoStore<IdentityKeyPair, IdentityKey, PreKeyRecord, SignedPreKeyRecord, SessionRecord, SignalProtocolAddress, ECPublicKey, PreKeyBundle, SessionCipher> omemoStore,
                       OmemoDevice remoteContact) {
        super(omemoManager, omemoStore, remoteContact);
    }

    @Override
    public SessionCipher createCipher(OmemoDevice contact) {
        SignalOmemoStoreConnector connector = new SignalOmemoStoreConnector(omemoManager, omemoStore);
        return new SessionCipher(connector, connector, connector, connector,
                omemoStore.keyUtil().omemoDeviceAsAddress(contact));
    }

    @Override
    public CiphertextTuple encryptMessageKey(byte[] messageKey) {
        CiphertextMessage ciphertextMessage;
        ciphertextMessage = cipher.encrypt(messageKey);
        int type = (ciphertextMessage.getType() == CiphertextMessage.PREKEY_TYPE ?
                OmemoElement.TYPE_OMEMO_PREKEY_MESSAGE : OmemoElement.TYPE_OMEMO_MESSAGE);
        return new CiphertextTuple(ciphertextMessage.serialize(), type);
    }

    @Override
    public byte[] decryptMessageKey(byte[] encryptedKey) throws NoRawSessionException {
        byte[] decryptedKey = null;
        try {
            try {
                PreKeySignalMessage message = new PreKeySignalMessage(encryptedKey);
                if (!message.getPreKeyId().isPresent()) {
                    LOGGER.log(Level.WARNING, "PreKeySignalMessage did not contain a PreKeyId");
                    return null;
                }
                LOGGER.log(Level.INFO, "PreKeySignalMessage received, new session ID: " + message.getSignedPreKeyId() + "/" + message.getPreKeyId().get());
                IdentityKey messageIdentityKey = message.getIdentityKey();
                if (this.identityKey != null && !this.identityKey.equals(messageIdentityKey)) {
                    LOGGER.log(Level.INFO, "Had session with fingerprint " + getFingerprint() +
                            ", received message with different fingerprint " + omemoStore.keyUtil().getFingerprint(messageIdentityKey) +
                            ". Silently drop the message.");
                } else {
                    this.identityKey = messageIdentityKey;
                    decryptedKey = cipher.decrypt(message);
                    this.preKeyId = message.getPreKeyId().get();
                }
            } catch (InvalidMessageException | InvalidVersionException e) {
                SignalMessage message = new SignalMessage(encryptedKey);
                decryptedKey = cipher.decrypt(message);
            } catch (InvalidKeyIdException e) {
                throw new NoRawSessionException(e);
            }
            catch (InvalidKeyException | UntrustedIdentityException e) {
                LOGGER.log(Level.SEVERE, "Error decrypting message header, " + e.getClass().getName() + ": " + e.getMessage());
            }
        } catch (InvalidMessageException | NoSessionException e) {
            throw new NoRawSessionException(e);
        } catch (LegacyMessageException | DuplicateMessageException e) {
            LOGGER.log(Level.SEVERE, "Error decrypting message header, " + e.getClass().getName() + ": " + e.getMessage());
        }
        return decryptedKey;
    }
}
