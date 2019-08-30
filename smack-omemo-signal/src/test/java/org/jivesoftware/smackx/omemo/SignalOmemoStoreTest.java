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
package org.jivesoftware.smackx.omemo;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import org.jivesoftware.smackx.omemo.signal.SignalCachingOmemoStore;
import org.jivesoftware.smackx.omemo.signal.SignalFileBasedOmemoStore;
import org.jivesoftware.smackx.omemo.signal.SignalOmemoKeyUtil;

import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.jxmpp.stringprep.XmppStringprepException;
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
 * smack-omemo-signal implementation of {@link OmemoStoreTest}.
 * This class executes tests of its super class with available implementations of {@link OmemoStore}.
 * So far this includes {@link SignalFileBasedOmemoStore}, {@link SignalCachingOmemoStore}.
 */
@RunWith(value = Parameterized.class)
public class SignalOmemoStoreTest extends OmemoStoreTest<IdentityKeyPair, IdentityKey, PreKeyRecord, SignedPreKeyRecord, SessionRecord, SignalProtocolAddress, ECPublicKey, PreKeyBundle, SessionCipher> {

    public SignalOmemoStoreTest(OmemoStore<IdentityKeyPair, IdentityKey, PreKeyRecord, SignedPreKeyRecord, SessionRecord, SignalProtocolAddress, ECPublicKey, PreKeyBundle, SessionCipher> store)
            throws XmppStringprepException {
        super(store);
    }

    /**
     * We are running this Test with multiple available OmemoStore implementations.
     * @return
     * @throws IOException if an I/O error occured.
     */
    @Parameterized.Parameters
    public static Collection<Object[]> getParameters() throws IOException {
        TemporaryFolder temp = initStaticTemp();
        return Arrays.asList(new Object[][] {
                // Simple file based store
                { new SignalFileBasedOmemoStore(temp.newFolder("sigFileBased"))},
                // Ephemeral caching store
                { new SignalCachingOmemoStore()},
                // Caching file based store
                { new SignalCachingOmemoStore(new SignalFileBasedOmemoStore(temp.newFolder("cachingSigFileBased")))}
        });
    }

    @Test
    public void keyUtilTest() {
        assertTrue(store.keyUtil() instanceof SignalOmemoKeyUtil);
    }
}
