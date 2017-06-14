/**
 *
 * Copyright 2017 Paul Schaub
 *
 * This file is part of smack-omemo-signal-integration-test.
 *
 * smack-omemo-signal-integration-test is free software; you can redistribute it and/or modify
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
package org.igniterealtime.smack.inttest.smack_omemo_signal;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPException;

import org.jivesoftware.smackx.omemo.exceptions.CorruptedOmemoKeyException;
import org.jivesoftware.smackx.omemo.signal.SignalOmemoService;

import org.igniterealtime.smack.inttest.SmackIntegrationTestFramework;

public class SmackOmemoSignalIntegrationTestFramework {

    public static void main(String[] args) throws InvalidKeyException, NoSuchPaddingException,
                    InvalidAlgorithmParameterException, IllegalBlockSizeException,
                    BadPaddingException, NoSuchAlgorithmException, NoSuchProviderException, SmackException,
                    InterruptedException, CorruptedOmemoKeyException, KeyManagementException, IOException, XMPPException {
        SignalOmemoService.acknowledgeLicense();
        SignalOmemoService.setup();

        final String[] smackOmemoPackages = new String[] { "org.jivesoftware.smackx.omemo" };
        SmackIntegrationTestFramework.main(smackOmemoPackages);
    }

}
