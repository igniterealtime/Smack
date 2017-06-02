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

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.XMPPException.XMPPErrorException;
import org.jivesoftware.smackx.omemo.OmemoManager;
import org.jivesoftware.smackx.omemo.OmemoService;
import org.jivesoftware.smackx.omemo.OmemoStore;
import org.jivesoftware.smackx.omemo.exceptions.CorruptedOmemoKeyException;
import org.jivesoftware.smackx.omemo.internal.OmemoDevice;
import org.whispersystems.libsignal.IdentityKey;
import org.whispersystems.libsignal.IdentityKeyPair;
import org.whispersystems.libsignal.SessionBuilder;
import org.whispersystems.libsignal.SessionCipher;
import org.whispersystems.libsignal.SignalProtocolAddress;
import org.whispersystems.libsignal.UntrustedIdentityException;
import org.whispersystems.libsignal.ecc.ECPublicKey;
import org.whispersystems.libsignal.state.PreKeyBundle;
import org.whispersystems.libsignal.state.PreKeyRecord;
import org.whispersystems.libsignal.state.SessionRecord;
import org.whispersystems.libsignal.state.SignedPreKeyRecord;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.logging.Level;

/**
 * Concrete implementation of the OmemoService using the Signal library.
 *
 * @author Paul Schaub
 */
@SuppressWarnings("unused")
public final class SignalOmemoService extends OmemoService<IdentityKeyPair, IdentityKey, PreKeyRecord, SignedPreKeyRecord, SessionRecord, SignalProtocolAddress, ECPublicKey, PreKeyBundle, SessionCipher> {

    private static SignalOmemoService INSTANCE;
    private static boolean LICENSE_ACKNOWLEDGED = false;

    public static void setup() throws InvalidKeyException, XMPPErrorException, NoSuchPaddingException, InvalidAlgorithmParameterException, UnsupportedEncodingException, IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchProviderException, SmackException, InterruptedException, CorruptedOmemoKeyException {
        if (!LICENSE_ACKNOWLEDGED) {
            throw new IllegalStateException("smack-omemo-signal is licensed under the terms of the GPLv3. Please be aware that you " +
                    "can only use this library within the terms of the GPLv3. See for example " +
                    "https://www.gnu.org/licenses/quick-guide-gplv3 for more details. Please call " +
                    "SignalOmemoService.acknowledgeLicense() prior to the setup() method in order to prevent " +
                    "this exception.");
        }
        if (INSTANCE == null) {
            INSTANCE = new SignalOmemoService();
        }
        setInstance(INSTANCE);
    }

    @Override
    public OmemoStore<IdentityKeyPair, IdentityKey, PreKeyRecord, SignedPreKeyRecord, SessionRecord, SignalProtocolAddress, ECPublicKey, PreKeyBundle, SessionCipher> createDefaultOmemoStoreBackend() {
        return new SignalFileBasedOmemoStore();
    }

    private SignalOmemoService()
            throws SmackException, InterruptedException, XMPPException.XMPPErrorException, CorruptedOmemoKeyException,
            NoSuchPaddingException, InvalidAlgorithmParameterException, UnsupportedEncodingException,
            IllegalBlockSizeException, BadPaddingException, NoSuchAlgorithmException, NoSuchProviderException,
            java.security.InvalidKeyException {
        super();
    }

    public static void acknowledgeLicense() {
        LICENSE_ACKNOWLEDGED = true;
    }

    @Override
    protected void processBundle(OmemoManager omemoManager, PreKeyBundle preKeyBundle, OmemoDevice contact) throws CorruptedOmemoKeyException {
        SignalOmemoStoreConnector connector = new SignalOmemoStoreConnector(omemoManager, getOmemoStoreBackend());
        SessionBuilder builder = new SessionBuilder(connector, connector, connector, connector,
                getOmemoStoreBackend().keyUtil().omemoDeviceAsAddress(contact));
        try {
            builder.process(preKeyBundle);
            LOGGER.log(Level.INFO, "Session built with " + contact);
            getOmemoStoreBackend().getOmemoSessionOf(omemoManager, contact); //method puts session in session map.
        } catch (org.whispersystems.libsignal.InvalidKeyException e) {
            throw new CorruptedOmemoKeyException(e.getMessage());
        } catch (UntrustedIdentityException e) {
            // This should never happen.
            throw new AssertionError(e);
        }
    }
}
