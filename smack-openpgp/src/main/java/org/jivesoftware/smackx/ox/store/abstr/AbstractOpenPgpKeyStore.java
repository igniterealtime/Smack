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
package org.jivesoftware.smackx.ox.store.abstr;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smackx.ox.exception.MissingUserIdOnKeyException;
import org.jivesoftware.smackx.ox.selection_strategy.BareJidUserId;
import org.jivesoftware.smackx.ox.store.definition.OpenPgpKeyStore;

import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.jxmpp.jid.BareJid;
import org.pgpainless.PGPainless;
import org.pgpainless.key.OpenPgpV4Fingerprint;
import org.pgpainless.key.collection.PGPKeyRing;
import org.pgpainless.util.BCUtil;

public abstract class AbstractOpenPgpKeyStore implements OpenPgpKeyStore {

    protected static final Logger LOGGER = Logger.getLogger(AbstractOpenPgpKeyStore.class.getName());

    protected Map<BareJid, PGPPublicKeyRingCollection> publicKeyRingCollections = new HashMap<>();
    protected Map<BareJid, PGPSecretKeyRingCollection> secretKeyRingCollections = new HashMap<>();
    protected Map<BareJid, Map<OpenPgpV4Fingerprint, Date>> keyFetchDates = new HashMap<>();

    /**
     * Read a {@link PGPPublicKeyRingCollection} from local storage.
     * This method returns null, if no keys were found.
     *
     * @param owner owner of the keys
     * @return public keys
     *
     * @throws IOException IO is dangerous
     * @throws PGPException PGP is brittle
     */
    protected abstract PGPPublicKeyRingCollection readPublicKeysOf(BareJid owner) throws IOException, PGPException;

    /**
     * Write the {@link PGPPublicKeyRingCollection} of a user to local storage.
     *
     * @param owner owner of the keys
     * @param publicKeys keys
     *
     * @throws IOException IO is dangerous
     */
    protected abstract void writePublicKeysOf(BareJid owner, PGPPublicKeyRingCollection publicKeys) throws IOException;

    /**
     * Read a {@link PGPSecretKeyRingCollection} from local storage.
     * This method returns null, if no keys were found.
     *
     * @param owner owner of the keys
     * @return secret keys
     *
     * @throws IOException IO is dangerous
     * @throws PGPException PGP is brittle
     */
    protected abstract PGPSecretKeyRingCollection readSecretKeysOf(BareJid owner) throws IOException, PGPException;

    /**
     * Write the {@link PGPSecretKeyRingCollection} of a user to local storage.
     *
     * @param owner owner of the keys
     * @param secretKeys secret keys
     *
     * @throws IOException IO is dangerous
     */
    protected abstract void writeSecretKeysOf(BareJid owner, PGPSecretKeyRingCollection secretKeys) throws IOException;

    /**
     * Read the key fetch dates for a users keys from local storage.
     *
     * @param owner owner
     * @return fetch dates for the owners keys
     *
     * @throws IOException IO is dangerous
     */
    protected abstract Map<OpenPgpV4Fingerprint, Date> readKeyFetchDates(BareJid owner) throws IOException;

    /**
     * Write the key fetch dates for a users keys to local storage.
     *
     * @param owner owner
     * @param dates fetch dates for the owners keys
     *
     * @throws IOException IO is dangerous
     */
    protected abstract void writeKeyFetchDates(BareJid owner, Map<OpenPgpV4Fingerprint, Date> dates) throws IOException;

    @Override
    public Map<OpenPgpV4Fingerprint, Date> getPublicKeyFetchDates(BareJid contact) throws IOException {
        Map<OpenPgpV4Fingerprint, Date> dates = keyFetchDates.get(contact);
        if (dates == null) {
            dates = readKeyFetchDates(contact);
            keyFetchDates.put(contact, dates);
        }
        return dates;
    }

    @Override
    public void setPublicKeyFetchDates(BareJid contact, Map<OpenPgpV4Fingerprint, Date> dates) throws IOException {
        keyFetchDates.put(contact, dates);
        writeKeyFetchDates(contact, dates);
    }

    @Override
    public PGPPublicKeyRingCollection getPublicKeysOf(BareJid owner) throws IOException, PGPException {
        PGPPublicKeyRingCollection keys = publicKeyRingCollections.get(owner);
        if (keys == null) {
            keys = readPublicKeysOf(owner);
            if (keys != null) {
                publicKeyRingCollections.put(owner, keys);
            }
        }
        return keys;
    }

    @Override
    public PGPSecretKeyRingCollection getSecretKeysOf(BareJid owner) throws IOException, PGPException {
        PGPSecretKeyRingCollection keys = secretKeyRingCollections.get(owner);
        if (keys == null) {
            keys = readSecretKeysOf(owner);
            if (keys != null) {
                secretKeyRingCollections.put(owner, keys);
            }
        }
        return keys;
    }

    @Override
    public void importSecretKey(BareJid owner, PGPSecretKeyRing secretKeys)
            throws IOException, PGPException, MissingUserIdOnKeyException {

        // TODO: Avoid 'new' use instance method.
        if (!new BareJidUserId.SecRingSelectionStrategy().accept(owner, secretKeys)) {
            throw new MissingUserIdOnKeyException(owner, new OpenPgpV4Fingerprint(secretKeys));
        }

        PGPSecretKeyRing importKeys = BCUtil.removeUnassociatedKeysFromKeyRing(secretKeys, secretKeys.getPublicKey());

        PGPSecretKeyRingCollection secretKeyRings = getSecretKeysOf(owner);
        try {
            if (secretKeyRings != null) {
                secretKeyRings = PGPSecretKeyRingCollection.addSecretKeyRing(secretKeyRings, importKeys);
            } else {
                secretKeyRings = BCUtil.keyRingsToKeyRingCollection(importKeys);
            }
        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.INFO, "Skipping secret key ring " + Long.toHexString(importKeys.getPublicKey().getKeyID()) +
                    " as it is already in the key ring of " + owner.toString());
        }
        this.secretKeyRingCollections.put(owner, secretKeyRings);
        writeSecretKeysOf(owner, secretKeyRings);
    }

    @Override
    public void importPublicKey(BareJid owner, PGPPublicKeyRing publicKeys) throws IOException, PGPException, MissingUserIdOnKeyException {

        if (!new BareJidUserId.PubRingSelectionStrategy().accept(owner, publicKeys)) {
            throw new MissingUserIdOnKeyException(owner, new OpenPgpV4Fingerprint(publicKeys));
        }

        PGPPublicKeyRing importKeys = BCUtil.removeUnassociatedKeysFromKeyRing(publicKeys, publicKeys.getPublicKey());

        PGPPublicKeyRingCollection publicKeyRings = getPublicKeysOf(owner);
        try {
            if (publicKeyRings != null) {
                publicKeyRings = PGPPublicKeyRingCollection.addPublicKeyRing(publicKeyRings, importKeys);
            } else {
                publicKeyRings = BCUtil.keyRingsToKeyRingCollection(importKeys);
            }
        } catch (IllegalArgumentException e) {
            LOGGER.log(Level.INFO, "Skipping public key ring " + Long.toHexString(importKeys.getPublicKey().getKeyID()) +
                    " as it is already in the key ring of " + owner.toString());
        }
        this.publicKeyRingCollections.put(owner, publicKeyRings);
        writePublicKeysOf(owner, publicKeyRings);
    }

    @Override
    public PGPPublicKeyRing getPublicKeyRing(BareJid owner, OpenPgpV4Fingerprint fingerprint) throws IOException, PGPException {
        PGPPublicKeyRingCollection publicKeyRings = getPublicKeysOf(owner);

        if (publicKeyRings != null) {
            return publicKeyRings.getPublicKeyRing(fingerprint.getKeyId());
        }

        return null;
    }

    @Override
    public PGPSecretKeyRing getSecretKeyRing(BareJid owner, OpenPgpV4Fingerprint fingerprint) throws IOException, PGPException {
        PGPSecretKeyRingCollection secretKeyRings = getSecretKeysOf(owner);

        if (secretKeyRings != null) {
            return secretKeyRings.getSecretKeyRing(fingerprint.getKeyId());
        }

        return null;
    }

    @Override
    public void deletePublicKeyRing(BareJid owner, OpenPgpV4Fingerprint fingerprint) throws IOException, PGPException {
        PGPPublicKeyRingCollection publicKeyRings = getPublicKeysOf(owner);
        if (publicKeyRings.contains(fingerprint.getKeyId())) {
            publicKeyRings = PGPPublicKeyRingCollection.removePublicKeyRing(publicKeyRings, publicKeyRings.getPublicKeyRing(fingerprint.getKeyId()));
            if (!publicKeyRings.iterator().hasNext()) {
                publicKeyRings = null;
            }
            this.publicKeyRingCollections.put(owner, publicKeyRings);
            writePublicKeysOf(owner, publicKeyRings);
        }
    }

    @Override
    public void deleteSecretKeyRing(BareJid owner, OpenPgpV4Fingerprint fingerprint) throws IOException, PGPException {
        PGPSecretKeyRingCollection secretKeyRings = getSecretKeysOf(owner);
        if (secretKeyRings.contains(fingerprint.getKeyId())) {
            secretKeyRings = PGPSecretKeyRingCollection.removeSecretKeyRing(secretKeyRings, secretKeyRings.getSecretKeyRing(fingerprint.getKeyId()));
            if (!secretKeyRings.iterator().hasNext()) {
                secretKeyRings = null;
            }
            this.secretKeyRingCollections.put(owner, secretKeyRings);
            writeSecretKeysOf(owner, secretKeyRings);
        }
    }

    @Override
    public PGPKeyRing generateKeyRing(BareJid owner)
            throws PGPException, NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException {
        return PGPainless.generateKeyRing().simpleEcKeyRing("xmpp:" + owner.toString());
    }
}
