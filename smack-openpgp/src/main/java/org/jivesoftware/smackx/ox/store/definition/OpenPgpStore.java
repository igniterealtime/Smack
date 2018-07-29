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
package org.jivesoftware.smackx.ox.store.definition;

import org.jivesoftware.smackx.ox.OpenPgpContact;
import org.jivesoftware.smackx.ox.callback.SecretKeyPassphraseCallback;

import org.jxmpp.jid.BareJid;
import org.pgpainless.key.protection.SecretKeyRingProtector;
import org.pgpainless.key.protection.UnprotectedKeysProtector;

public interface OpenPgpStore extends OpenPgpKeyStore, OpenPgpMetadataStore, OpenPgpTrustStore {

    /**
     * Return an {@link OpenPgpContact} for a contacts jid.
     *
     * @param contactsJid {@link BareJid} of the contact.
     * @return {@link OpenPgpContact} object of the contact.
     */
    OpenPgpContact getOpenPgpContact(BareJid contactsJid);

    /**
     * Set a {@link SecretKeyRingProtector} which is used to decrypt password protected secret keys.
     *
     * @param unlocker unlocker which unlocks encrypted secret keys.
     */
    void setKeyRingProtector(SecretKeyRingProtector unlocker);

    /**
     * Return the {@link SecretKeyRingProtector} which is used to decrypt password protected secret keys.
     * In case no {@link SecretKeyRingProtector} has been set, this method MUST return an {@link UnprotectedKeysProtector}.
     *
     * @return secret key unlocker.
     */
    SecretKeyRingProtector getKeyRingProtector();

    /**
     * Set a {@link SecretKeyPassphraseCallback} which is called in case we stumble over a secret key for which we have
     * no passphrase.
     *
     * @param callback callback. MUST NOT be null.
     */
    void setSecretKeyPassphraseCallback(SecretKeyPassphraseCallback callback);

}
