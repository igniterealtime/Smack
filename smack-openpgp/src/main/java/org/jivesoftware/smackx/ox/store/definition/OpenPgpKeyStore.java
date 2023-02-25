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

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Date;
import java.util.Map;

import org.jivesoftware.smackx.ox.exception.MissingUserIdOnKeyException;

import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.jxmpp.jid.BareJid;
import org.pgpainless.key.OpenPgpV4Fingerprint;

public interface OpenPgpKeyStore {

    /**
     * Return the {@link PGPPublicKeyRingCollection} containing all public keys of {@code owner} that are locally
     * available.
     * This method might return null.
     *
     * @param owner {@link BareJid} of the user we want to get keys from.
     * @return {@link PGPPublicKeyRingCollection} of the user.
     *
     * @throws IOException IO is dangerous
     * @throws PGPException PGP is brittle
     */
    PGPPublicKeyRingCollection getPublicKeysOf(BareJid owner) throws IOException, PGPException;

    /**
     * Return the {@link PGPSecretKeyRingCollection} containing all secret keys of {@code owner} which are locally
     * available.
     * This method might return null.
     *
     * @param owner {@link BareJid} of the user we want to get keys from.
     * @return {@link PGPSecretKeyRingCollection} of the user.
     *
     * @throws IOException IO is dangerous
     * @throws PGPException PGP is brittle
     */
    PGPSecretKeyRingCollection getSecretKeysOf(BareJid owner) throws IOException, PGPException;

    /**
     * Return the {@link PGPPublicKeyRing} of {@code owner} which contains the key described by {@code fingerprint}.
     * This method might return null.
     *
     * @param owner {@link BareJid} of the keys owner
     * @param fingerprint {@link OpenPgpV4Fingerprint} of a key contained in the key ring
     * @return {@link PGPPublicKeyRing} which contains the key described by {@code fingerprint}.
     *
     * @throws IOException IO is dangerous
     * @throws PGPException PGP is brittle
     */
    PGPPublicKeyRing getPublicKeyRing(BareJid owner, OpenPgpV4Fingerprint fingerprint) throws IOException, PGPException;

    /**
     * Return the {@link PGPSecretKeyRing} of {@code owner} which contains the key described by {@code fingerprint}.
     * This method might return null.
     *
     * @param owner {@link BareJid} of the keys owner
     * @param fingerprint {@link OpenPgpV4Fingerprint} of a key contained in the key ring
     * @return {@link PGPSecretKeyRing} which contains the key described by {@code fingerprint}.
     *
     * @throws IOException IO is dangerous
     * @throws PGPException PGP is brittle
     */
    PGPSecretKeyRing getSecretKeyRing(BareJid owner, OpenPgpV4Fingerprint fingerprint) throws IOException, PGPException;

    /**
     * Remove a {@link PGPPublicKeyRing} which contains the key described by {@code fingerprint} from the
     * {@link PGPPublicKeyRingCollection} of {@code owner}.
     *
     * @param owner owner of the key ring
     * @param fingerprint fingerprint of the key whose key ring will be removed.
     *
     * @throws IOException IO is dangerous
     * @throws PGPException PGP is brittle
     */
    void deletePublicKeyRing(BareJid owner, OpenPgpV4Fingerprint fingerprint) throws IOException, PGPException;

    /**
     * Remove a {@link PGPSecretKeyRing} which contains the key described by {@code fingerprint} from the
     * {@link PGPSecretKeyRingCollection} of {@code owner}.
     *
     * @param owner owner of the key ring
     * @param fingerprint fingerprint of the key whose key ring will be removed.
     *
     * @throws IOException IO is dangerous
     * @throws PGPException PGP is brittle
     */
    void deleteSecretKeyRing(BareJid owner, OpenPgpV4Fingerprint fingerprint) throws IOException, PGPException;

    /**
     * Generate a new {@link PGPSecretKeyRing} for {@code owner}.
     * The key will have a user-id containing the users {@link BareJid} (eg. "xmpp:juliet@capulet.lit").
     * This method MUST NOT return null.
     *
     * @param owner owner of the key ring.
     * @return key ring
     *
     * @throws PGPException PGP is brittle
     * @throws NoSuchAlgorithmException in case there is no {@link java.security.Provider} registered for the used
     * OpenPGP algorithms.
     * @throws NoSuchProviderException in case there is no suitable {@link java.security.Provider} registered.
     * @throws InvalidAlgorithmParameterException in case an invalid algorithms configuration is used.
     */
    PGPSecretKeyRing generateKeyRing(BareJid owner) throws PGPException, NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException;

    /**
     * Import a {@link PGPSecretKeyRing} of {@code owner}.
     * In case the key ring is already available locally, the keys are skipped.
     *
     * @param owner owner of the keys
     * @param secretKeys secret keys
     *
     * @throws IOException IO is dangerous
     * @throws PGPException PGP is brittle
     * @throws MissingUserIdOnKeyException in case the secret keys are lacking a user-id with the owners jid.
     */
    void importSecretKey(BareJid owner, PGPSecretKeyRing secretKeys) throws IOException, PGPException, MissingUserIdOnKeyException;

    /**
     * Import a {@link PGPPublicKeyRing} of {@code owner}.
     * In case the key ring is already available locally, the keys are skipped.
     *
     * @param owner owner of the keys
     * @param publicKeys public keys
     *
     * @throws IOException IO is dangerous
     * @throws PGPException PGP is brittle
     * @throws MissingUserIdOnKeyException in case the public keys are lacking a user-id with the owners jid.
     */
    void importPublicKey(BareJid owner, PGPPublicKeyRing publicKeys) throws IOException, PGPException, MissingUserIdOnKeyException;

    /**
     * Return the last date on which keys of {@code contact} were fetched from PubSub.
     * This method MUST NOT return null.
     *
     * @param contact contact in which we are interested.
     * @return dates of last key fetching.
     *
     * @throws IOException IO is dangerous
     */
    Map<OpenPgpV4Fingerprint, Date> getPublicKeyFetchDates(BareJid contact) throws IOException;

    /**
     * Set the last date on which keys of {@code contact} were fetched from PubSub.
     *
     * @param contact contact in which we are interested.
     * @param dates dates of last key fetching.
     *
     * @throws IOException IO is dangerous
     */
    void setPublicKeyFetchDates(BareJid contact, Map<OpenPgpV4Fingerprint, Date> dates) throws IOException;
}
