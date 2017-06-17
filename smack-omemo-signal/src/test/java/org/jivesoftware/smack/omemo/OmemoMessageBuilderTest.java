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
package org.jivesoftware.smack.omemo;

import static org.jivesoftware.smackx.omemo.util.OmemoConstants.Crypto.CIPHERMODE;
import static org.jivesoftware.smackx.omemo.util.OmemoConstants.Crypto.KEYTYPE;
import static org.jivesoftware.smackx.omemo.util.OmemoConstants.Crypto.PROVIDER;
import static org.junit.Assert.assertArrayEquals;

import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.Security;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.jivesoftware.smack.test.util.SmackTestSuite;
import org.jivesoftware.smack.util.StringUtils;

import org.jivesoftware.smackx.omemo.util.OmemoMessageBuilder;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.Test;
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
 * Test the OmemoMessageBuilder.
 */
public class OmemoMessageBuilderTest extends SmackTestSuite {

    @Test
    public void setTextTest() throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidAlgorithmParameterException, UnsupportedEncodingException, IllegalBlockSizeException, BadPaddingException, NoSuchProviderException, InvalidKeyException {
        Security.addProvider(new BouncyCastleProvider());
        String message = "Hello World!";
        byte[] key = OmemoMessageBuilder.generateKey();
        byte[] iv = OmemoMessageBuilder.generateIv();

        SecretKey secretKey = new SecretKeySpec(key, KEYTYPE);
        IvParameterSpec ivSpec = new IvParameterSpec(iv);
        Cipher cipher = Cipher.getInstance(CIPHERMODE, PROVIDER);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, ivSpec);

        OmemoMessageBuilder<IdentityKeyPair, IdentityKey, PreKeyRecord, SignedPreKeyRecord, SessionRecord, SignalProtocolAddress, ECPublicKey, PreKeyBundle, SessionCipher>
                mb = new OmemoMessageBuilder<>(null, null, key, iv);
        mb.setMessage(message);

        byte[] expected = cipher.doFinal(message.getBytes(StringUtils.UTF8));
        byte[] messageKey = new byte[16];
        System.arraycopy(mb.getMessageKey(),0, messageKey, 0, 16);
        byte[] messagePlusTag = new byte[mb.getCiphertextMessage().length + 16];
        System.arraycopy(mb.getCiphertextMessage(),0,messagePlusTag,0,mb.getCiphertextMessage().length);
        System.arraycopy(mb.getMessageKey(), 16, messagePlusTag, mb.getCiphertextMessage().length, 16);

        assertArrayEquals(key, messageKey);
        assertArrayEquals(expected, messagePlusTag);
    }
}
