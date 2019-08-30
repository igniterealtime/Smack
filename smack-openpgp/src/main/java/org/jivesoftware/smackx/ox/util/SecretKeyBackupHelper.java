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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Set;

import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smack.util.stringencoder.Base64;

import org.jivesoftware.smackx.ox.crypto.OpenPgpProvider;
import org.jivesoftware.smackx.ox.element.SecretkeyElement;
import org.jivesoftware.smackx.ox.exception.InvalidBackupCodeException;
import org.jivesoftware.smackx.ox.exception.MissingOpenPgpKeyException;

import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.jxmpp.jid.BareJid;
import org.pgpainless.PGPainless;
import org.pgpainless.algorithm.SymmetricKeyAlgorithm;
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
    public static String generateBackupPassword() {
        return StringUtils.secureOfflineAttackSafeRandomString();
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
                                                    String backupCode)
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
                                                          String backupCode)
            throws PGPException, IOException {
        byte[] encrypted = PGPainless.encryptWithPassword(keys, new Passphrase(backupCode.toCharArray()),
                SymmetricKeyAlgorithm.AES_256);
        return new SecretkeyElement(Base64.encode(encrypted));
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
    public static PGPSecretKeyRing restoreSecretKeyBackup(SecretkeyElement backup, String backupCode)
            throws InvalidBackupCodeException, IOException, PGPException {
        byte[] encrypted = Base64.decode(backup.getB64Data());

        byte[] decrypted;
        try {
            decrypted = PGPainless.decryptWithPassword(encrypted, new Passphrase(backupCode.toCharArray()));
        } catch (IOException | PGPException e) {
            throw new InvalidBackupCodeException("Could not decrypt secret key backup. Possibly wrong passphrase?", e);
        }

        return PGPainless.readKeyRing().secretKeyRing(decrypted);
    }
}
