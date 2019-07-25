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
package org.jivesoftware.smackx.ox.crypto;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.util.Objects;
import org.jivesoftware.smack.util.stringencoder.Base64;

import org.jivesoftware.smackx.ox.OpenPgpContact;
import org.jivesoftware.smackx.ox.OpenPgpMessage;
import org.jivesoftware.smackx.ox.OpenPgpSelf;
import org.jivesoftware.smackx.ox.element.CryptElement;
import org.jivesoftware.smackx.ox.element.OpenPgpElement;
import org.jivesoftware.smackx.ox.element.SignElement;
import org.jivesoftware.smackx.ox.element.SigncryptElement;
import org.jivesoftware.smackx.ox.store.definition.OpenPgpStore;

import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.util.io.Streams;
import org.pgpainless.PGPainless;
import org.pgpainless.decryption_verification.DecryptionStream;
import org.pgpainless.decryption_verification.MissingPublicKeyCallback;
import org.pgpainless.decryption_verification.OpenPgpMetadata;
import org.pgpainless.encryption_signing.EncryptionStream;

public class PainlessOpenPgpProvider implements OpenPgpProvider {

    private static final Logger LOGGER = Logger.getLogger(PainlessOpenPgpProvider.class.getName());

    private final XMPPConnection connection;
    private final OpenPgpStore store;

    public PainlessOpenPgpProvider(XMPPConnection connection, OpenPgpStore store) {
        this.connection = Objects.requireNonNull(connection);
        this.store = Objects.requireNonNull(store);
    }

    @Override
    public OpenPgpStore getStore() {
        return store;
    }

    @Override
    public OpenPgpElementAndMetadata signAndEncrypt(SigncryptElement element, OpenPgpSelf self, Collection<OpenPgpContact> recipients)
            throws IOException, PGPException {
        InputStream plainText = element.toInputStream();
        ByteArrayOutputStream cipherText = new ByteArrayOutputStream();

        ArrayList<PGPPublicKeyRingCollection> recipientKeys = new ArrayList<>();
        for (OpenPgpContact contact : recipients) {
            PGPPublicKeyRingCollection keys = contact.getTrustedAnnouncedKeys();
            if (keys != null) {
                recipientKeys.add(keys);
            } else {
                LOGGER.log(Level.WARNING, "There are no suitable keys for contact " + contact.getJid().toString());
            }
        }

        EncryptionStream cipherStream = PGPainless.createEncryptor().onOutputStream(cipherText)
                .toRecipients(recipientKeys.toArray(new PGPPublicKeyRingCollection[] {}))
                .andToSelf(self.getTrustedAnnouncedKeys())
                .usingSecureAlgorithms()
                .signWith(getStore().getKeyRingProtector(), self.getSigningKeyRing())
                .noArmor();

        Streams.pipeAll(plainText, cipherStream);
        plainText.close();
        cipherStream.flush();
        cipherStream.close();
        cipherText.close();

        String base64 = Base64.encodeToString(cipherText.toByteArray());
        OpenPgpElement openPgpElement = new OpenPgpElement(base64);

        return new OpenPgpElementAndMetadata(openPgpElement, cipherStream.getResult());
    }

    @Override
    public OpenPgpElementAndMetadata sign(SignElement element, OpenPgpSelf self)
            throws IOException, PGPException {
        InputStream plainText = element.toInputStream();
        ByteArrayOutputStream cipherText = new ByteArrayOutputStream();

        EncryptionStream cipherStream = PGPainless.createEncryptor().onOutputStream(cipherText)
                .doNotEncrypt()
                .signWith(getStore().getKeyRingProtector(), self.getSigningKeyRing())
                .noArmor();

        Streams.pipeAll(plainText, cipherStream);
        plainText.close();
        cipherStream.flush();
        cipherStream.close();
        cipherText.close();

        String base64 = Base64.encodeToString(cipherText.toByteArray());
        OpenPgpElement openPgpElement = new OpenPgpElement(base64);

        return new OpenPgpElementAndMetadata(openPgpElement, cipherStream.getResult());
    }

    @Override
    public OpenPgpElementAndMetadata encrypt(CryptElement element, OpenPgpSelf self, Collection<OpenPgpContact> recipients)
            throws IOException, PGPException {
        InputStream plainText = element.toInputStream();
        ByteArrayOutputStream cipherText = new ByteArrayOutputStream();

        ArrayList<PGPPublicKeyRingCollection> recipientKeys = new ArrayList<>();
        for (OpenPgpContact contact : recipients) {
            PGPPublicKeyRingCollection keys = contact.getTrustedAnnouncedKeys();
            if (keys != null) {
                recipientKeys.add(keys);
            } else {
                LOGGER.log(Level.WARNING, "There are no suitable keys for contact " + contact.getJid().toString());
            }
        }

        EncryptionStream cipherStream = PGPainless.createEncryptor().onOutputStream(cipherText)
                .toRecipients(recipientKeys.toArray(new PGPPublicKeyRingCollection[] {}))
                .andToSelf(self.getTrustedAnnouncedKeys())
                .usingSecureAlgorithms()
                .doNotSign()
                .noArmor();

        Streams.pipeAll(plainText, cipherStream);
        plainText.close();
        cipherStream.flush();
        cipherStream.close();
        cipherText.close();

        String base64 = Base64.encodeToString(cipherText.toByteArray());
        OpenPgpElement openPgpElement = new OpenPgpElement(base64);

        return new OpenPgpElementAndMetadata(openPgpElement, cipherStream.getResult());
    }

    @Override
    public OpenPgpMessage decryptAndOrVerify(OpenPgpElement element, final OpenPgpSelf self, final OpenPgpContact sender) throws IOException, PGPException {
        ByteArrayOutputStream plainText = new ByteArrayOutputStream();
        InputStream cipherText = element.toInputStream();

        PGPPublicKeyRingCollection announcedPublicKeys = sender.getAnnouncedPublicKeys();
        if (announcedPublicKeys == null) {
            LOGGER.log(Level.INFO, "Received a message from " + sender.getJid() + " but we have no keys yet. Try fetching them.");
            try {
                sender.updateKeys(connection);
                announcedPublicKeys = sender.getAnnouncedPublicKeys();
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Fetching keys of " + sender.getJid() + " failed. Abort decryption and discard message.", e);
                throw new PGPException("Abort decryption due to lack of keys.", e);
            }
        }

        MissingPublicKeyCallback missingPublicKeyCallback = new MissingPublicKeyCallback() {
            @Override
            public PGPPublicKey onMissingPublicKeyEncountered(Long keyId) {
                try {
                    sender.updateKeys(connection);
                    return sender.getAnyPublicKeys().getPublicKey(keyId);
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Cannot fetch missing key " + keyId, e);
                    return null;
                }
            }
        };

        DecryptionStream cipherStream = PGPainless.createDecryptor().onInputStream(cipherText)
                .decryptWith(getStore().getKeyRingProtector(), self.getSecretKeys())
                .verifyWith(announcedPublicKeys)
                .handleMissingPublicKeysWith(missingPublicKeyCallback)
                .build();

        Streams.pipeAll(cipherStream, plainText);

        cipherText.close();
        cipherStream.close();
        plainText.close();

        OpenPgpMetadata info = cipherStream.getResult();

        OpenPgpMessage.State state;
        if (info.isSigned()) {
            if (info.isEncrypted()) {
                state = OpenPgpMessage.State.signcrypt;
            } else {
                state = OpenPgpMessage.State.sign;
            }
        } else if (info.isEncrypted()) {
            state = OpenPgpMessage.State.crypt;
        } else {
            throw new PGPException("Received message appears to be neither encrypted, nor signed.");
        }

        return new OpenPgpMessage(plainText.toByteArray(), state, info);
    }
}
