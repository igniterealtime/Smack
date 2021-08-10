/**
 *
 * Copyright 2018 Paul Schaub.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jivesoftware.smackx.ox.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.stringencoder.Base64;

import org.jivesoftware.smackx.ox.OpenPgpSecretKeyBackupPassphrase;
import org.jivesoftware.smackx.ox.crypto.OpenPgpProvider;
import org.jivesoftware.smackx.ox.element.SecretkeyElement;
import org.jivesoftware.smackx.ox.exception.InvalidBackupCodeException;
import org.jivesoftware.smackx.ox.exception.MissingOpenPgpKeyException;

import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.util.io.Streams;
import org.jxmpp.jid.BareJid;
import org.pgpainless.PGPainless;
import org.pgpainless.algorithm.SymmetricKeyAlgorithm;
import org.pgpainless.decryption_verification.ConsumerOptions;
import org.pgpainless.decryption_verification.DecryptionStream;
import org.pgpainless.encryption_signing.EncryptionOptions;
import org.pgpainless.encryption_signing.EncryptionStream;
import org.pgpainless.encryption_signing.ProducerOptions;
import org.pgpainless.exception.MissingDecryptionMethodException;
import org.pgpainless.key.OpenPgpV4Fingerprint;
import org.pgpainless.util.Passphrase;

/**
 * Helper class which provides some functions needed for backup/restore of the users secret key to/from their private
 * PubSub node.
 */
public class SecretKeyBackupHelper {

    /**
     * Generate a secure backup code.
     * This code can be used to encrypt a secret key backup and follows the form described in XEP-0373 §5.3.
     *
     * @see <a href="https://xmpp.org/extensions/xep-0373.html#backup-encryption">
     *     XEP-0373 §5.4 Encrypting the Secret Key Backup</a>
     *
     * @return backup code
     */
    public static OpenPgpSecretKeyBackupPassphrase generateBackupPassword() {
        return new OpenPgpSecretKeyBackupPassphrase(StringUtils.secureOfflineAttackSafeRandomString());
    }

    /**
     * Create a {@link SecretkeyElement} which contains the secret keys listed in {@code fingerprints} and is encrypted
     * symmetrically using the {@code backupCode}.
     *
     * @param provider {@link OpenPgpProvider} for symmetric encryption.
     * @param owner owner of the secret keys (usually our jid).
     * @param fingerprints set of {@link OpenPgpV4Fingerprint}s of the keys which are going to be backed up.
     * @param backupCode passphrase for symmetric encryption.
     * @return {@link SecretkeyElement}
     *
     * @throws PGPException PGP is brittle
     * @throws IOException IO is dangerous
     * @throws MissingOpenPgpKeyException in case one of the keys whose fingerprint is in {@code fingerprints} is
     * not accessible.
     */
    public static SecretkeyElement createSecretkeyElement(OpenPgpProvider provider,
                                                          BareJid owner,
                                                          Set<OpenPgpV4Fingerprint> fingerprints,
                                                          OpenPgpSecretKeyBackupPassphrase backupCode)
            throws PGPException, IOException, MissingOpenPgpKeyException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();

        for (OpenPgpV4Fingerprint fingerprint : fingerprints) {

            PGPSecretKeyRing key = provider.getStore().getSecretKeyRing(owner, fingerprint);
            if (key == null) {
                throw new MissingOpenPgpKeyException(owner, fingerprint);
            }

            byte[] bytes = key.getEncoded();
            buffer.write(bytes);
        }
        return createSecretkeyElement(buffer.toByteArray(), backupCode);
    }

    /**
     * Create a {@link SecretkeyElement} which contains the secret keys which are serialized in {@code keys} and is
     * symmetrically encrypted using the {@code backupCode}.
     *
     * @see <a href="https://xmpp.org/extensions/xep-0373.html#backup-encryption">
     *     XEP-0373 §5.4 Encrypting the Secret Key Backup</a>
     *
     * @param keys serialized OpenPGP secret keys in transferable key format
     * @param backupCode passphrase for symmetric encryption
     * @return {@link SecretkeyElement}
     *
     * @throws PGPException PGP is brittle
     * @throws IOException IO is dangerous
     */
    public static SecretkeyElement createSecretkeyElement(byte[] keys,
                                                          OpenPgpSecretKeyBackupPassphrase backupCode)
            throws PGPException, IOException {
        InputStream keyStream = new ByteArrayInputStream(keys);
        ByteArrayOutputStream cryptOut = new ByteArrayOutputStream();
        EncryptionOptions encOpts = new EncryptionOptions()
                .addPassphrase(Passphrase.fromPassword(backupCode.toString()));
        encOpts.overrideEncryptionAlgorithm(SymmetricKeyAlgorithm.AES_256);

        EncryptionStream encryptionStream = PGPainless.encryptAndOrSign()
                .onOutputStream(cryptOut)
                .withOptions(ProducerOptions.encrypt(encOpts)
                        .setAsciiArmor(false));

        Streams.pipeAll(keyStream, encryptionStream);
        encryptionStream.close();

        return new SecretkeyElement(Base64.encode(cryptOut.toByteArray()));
    }

    /**
     * Decrypt a secret key backup and return the {@link PGPSecretKeyRing} contained in it.
     * TODO: Return a PGPSecretKeyRingCollection instead?
     *
     * @param backup encrypted {@link SecretkeyElement} containing the backup
     * @param backupCode passphrase for decrypting the {@link SecretkeyElement}.
     * @return the TODO javadoc me please
     * @throws InvalidBackupCodeException in case the provided backup code is invalid.
     * @throws IOException IO is dangerous.
     * @throws PGPException PGP is brittle.
     */
    public static PGPSecretKeyRing restoreSecretKeyBackup(SecretkeyElement backup, OpenPgpSecretKeyBackupPassphrase backupCode)
            throws InvalidBackupCodeException, IOException, PGPException {
        byte[] encrypted = Base64.decode(backup.getB64Data());
        InputStream encryptedIn = new ByteArrayInputStream(encrypted);
        ByteArrayOutputStream plaintextOut = new ByteArrayOutputStream();

        try {
            DecryptionStream decryptionStream = PGPainless.decryptAndOrVerify()
                    .onInputStream(encryptedIn)
                    .withOptions(new ConsumerOptions()
                            .addDecryptionPassphrase(Passphrase.fromPassword(backupCode.toString())));

            Streams.pipeAll(decryptionStream, plaintextOut);
            decryptionStream.close();
        } catch (MissingDecryptionMethodException e) {
            throw new InvalidBackupCodeException("Could not decrypt secret key backup. Possibly wrong passphrase?", e);
        }

        byte[] decrypted = plaintextOut.toByteArray();
        return PGPainless.readKeyRing().secretKeyRing(decrypted);
    }
}
