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

import java.io.IOException;
import java.util.Collection;

import org.jivesoftware.smackx.ox.OpenPgpContact;
import org.jivesoftware.smackx.ox.OpenPgpMessage;
import org.jivesoftware.smackx.ox.OpenPgpSelf;
import org.jivesoftware.smackx.ox.element.CryptElement;
import org.jivesoftware.smackx.ox.element.OpenPgpContentElement;
import org.jivesoftware.smackx.ox.element.OpenPgpElement;
import org.jivesoftware.smackx.ox.element.SignElement;
import org.jivesoftware.smackx.ox.element.SigncryptElement;
import org.jivesoftware.smackx.ox.store.definition.OpenPgpStore;

import org.bouncycastle.openpgp.PGPException;
import org.pgpainless.decryption_verification.OpenPgpMetadata;

public interface OpenPgpProvider {

    /**
     * Return the {@link OpenPgpStore} instance of this provider.
     * This MUST NOT return null.
     *
     * @return store TODO javadoc me please
     */
    OpenPgpStore getStore();

    /**
     * Sign a {@link SigncryptElement} using our signing key and encrypt it for all {@code recipients} and ourselves.
     *
     * @param element {@link SigncryptElement} which contains a payload which will be transmitted.
     * @param self our own OpenPGP identity.
     * @param recipients recipients identities.
     *
     * @return signed and encrypted {@link SigncryptElement} as a {@link OpenPgpElement}, along with
     * {@link OpenPgpMetadata} about the encryption/signatures.
     *
     * @throws IOException IO is dangerous
     * @throws PGPException PGP is brittle
     */
    OpenPgpElementAndMetadata signAndEncrypt(SigncryptElement element, OpenPgpSelf self, Collection<OpenPgpContact> recipients)
            throws IOException, PGPException;

    /**
     * Sign a {@link SignElement} using our signing key.
     * @param element {@link SignElement} which contains a payload.
     * @param self our OpenPGP identity.
     *
     * @return signed {@link SignElement} as {@link OpenPgpElement}, along with {@link OpenPgpMetadata} about the
     * signatures.
     *
     * @throws IOException IO is dangerous
     * @throws PGPException PGP is brittle
     */
    OpenPgpElementAndMetadata sign(SignElement element, OpenPgpSelf self)
            throws IOException, PGPException;

    /**
     * Encrypt a {@link CryptElement} for all {@code recipients} and ourselves.
     * @param element {@link CryptElement} which contains a payload which will be transmitted.
     * @param self our own OpenPGP identity.
     * @param recipients recipient identities.
     *
     * @return encrypted {@link CryptElement} as an {@link OpenPgpElement}, along with {@link OpenPgpMetadata} about
     * the encryption.
     *
     * @throws IOException IO is dangerous
     * @throws PGPException PGP is brittle
     */
    OpenPgpElementAndMetadata encrypt(CryptElement element, OpenPgpSelf self, Collection<OpenPgpContact> recipients)
            throws IOException, PGPException;

    /**
     * Decrypt and/or verify signatures on an incoming {@link OpenPgpElement}.
     * If the message is encrypted, this method decrypts it. If it is (also) signed, the signature will be checked.
     * The resulting {@link OpenPgpMessage} contains the original {@link OpenPgpContentElement}, as well as information
     * about the encryption/signing.
     *
     * @param element signed and or encrypted {@link OpenPgpElement}.
     * @param self our OpenPGP identity.
     * @param sender OpenPGP identity of the sender.
     *
     * @return decrypted message as {@link OpenPgpMessage}.
     *
     * @throws IOException IO is dangerous
     * @throws PGPException PGP is brittle
     */
    OpenPgpMessage decryptAndOrVerify(OpenPgpElement element, OpenPgpSelf self, OpenPgpContact sender)
            throws IOException, PGPException;
}
