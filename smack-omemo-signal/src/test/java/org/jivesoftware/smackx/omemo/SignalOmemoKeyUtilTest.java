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
package org.jivesoftware.smackx.omemo;

import java.util.Arrays;
import java.util.Collection;

import org.jivesoftware.smackx.omemo.signal.SignalOmemoKeyUtil;
import org.jivesoftware.smackx.omemo.util.OmemoKeyUtil;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.signal.libsignal.protocol.IdentityKey;
import org.signal.libsignal.protocol.IdentityKeyPair;
import org.signal.libsignal.protocol.ecc.ECPublicKey;
import org.signal.libsignal.protocol.state.PreKeyBundle;
import org.signal.libsignal.protocol.state.PreKeyRecord;
import org.signal.libsignal.protocol.state.SessionRecord;
import org.signal.libsignal.protocol.state.SignedPreKeyRecord;

/**
 * smack-omemo-signal implementation of {@link OmemoKeyUtilTest}.
 * This class executes tests of its super class with available implementations of {@link OmemoKeyUtil}.
 * So far this includes {@link SignalOmemoKeyUtil}.
 */
@RunWith(value = Parameterized.class)
public class SignalOmemoKeyUtilTest
        extends OmemoKeyUtilTest<IdentityKeyPair, IdentityKey, PreKeyRecord, SignedPreKeyRecord, SessionRecord, ECPublicKey, PreKeyBundle> {

    public SignalOmemoKeyUtilTest(OmemoKeyUtil<IdentityKeyPair, IdentityKey, PreKeyRecord, SignedPreKeyRecord, SessionRecord, ECPublicKey, PreKeyBundle> keyUtil) {
        super(keyUtil);
    }

    @Parameterized.Parameters
    public static Collection<Object[]> getParameters() {
        return Arrays.asList(new Object[][] {
                { new SignalOmemoKeyUtil()}
        });
    }
}
