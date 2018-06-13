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

import org.jivesoftware.smackx.omemo.OmemoStore;
import org.jivesoftware.smackx.omemo.util.OmemoKeyUtil;

import org.whispersystems.libsignal.IdentityKey;
import org.whispersystems.libsignal.IdentityKeyPair;
import org.whispersystems.libsignal.SessionCipher;
import org.whispersystems.libsignal.SignalProtocolAddress;
import org.whispersystems.libsignal.ecc.ECPublicKey;
import org.whispersystems.libsignal.state.PreKeyBundle;
import org.whispersystems.libsignal.state.PreKeyRecord;
import org.whispersystems.libsignal.state.SessionRecord;
import org.whispersystems.libsignal.state.SignedPreKeyRecord;

/**
 * Implementation of the OmemoStore using the Signal library.
 *
 * @author Paul Schaub
 */
@SuppressWarnings("unused")
public abstract class SignalOmemoStore
        extends OmemoStore<IdentityKeyPair, IdentityKey, PreKeyRecord, SignedPreKeyRecord, SessionRecord,
        SignalProtocolAddress, ECPublicKey, PreKeyBundle, SessionCipher> {

    private final SignalOmemoKeyUtil signalKeyUtil = new SignalOmemoKeyUtil();

    @Override
    public OmemoKeyUtil<IdentityKeyPair, IdentityKey, PreKeyRecord, SignedPreKeyRecord, SessionRecord, ECPublicKey, PreKeyBundle> keyUtil() {
        return signalKeyUtil;
    }
}
