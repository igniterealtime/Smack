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
import java.util.Observable;

import org.jivesoftware.smack.util.Objects;
import org.jivesoftware.smackx.ox.OpenPgpContact;
import org.jivesoftware.smackx.ox.callback.SecretKeyPassphraseCallback;
import org.jivesoftware.smackx.ox.exception.MissingUserIdOnKeyException;
import org.jivesoftware.smackx.ox.store.definition.OpenPgpKeyStore;
import org.jivesoftware.smackx.ox.store.definition.OpenPgpMetadataStore;
import org.jivesoftware.smackx.ox.store.definition.OpenPgpStore;
import org.jivesoftware.smackx.ox.store.definition.OpenPgpTrustStore;

import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.jxmpp.jid.BareJid;
import org.pgpainless.key.OpenPgpV4Fingerprint;
import org.pgpainless.key.collection.PGPKeyRing;
import org.pgpainless.key.protection.SecretKeyRingProtector;
import org.pgpainless.key.protection.UnprotectedKeysProtector;

public abstract class AbstractOpenPgpStore extends Observable implements OpenPgpStore {

    protected final OpenPgpKeyStore keyStore;
    protected final OpenPgpMetadataStore metadataStore;
    protected final OpenPgpTrustStore trustStore;

    protected SecretKeyPassphraseCallback secretKeyPassphraseCallback;
    protected SecretKeyRingProtector unlocker = new UnprotectedKeysProtector();
    protected final Map<BareJid, OpenPgpContact> contacts = new HashMap<>();

    @Override
    public void deletePublicKeyRing(BareJid owner, OpenPgpV4Fingerprint fingerprint) throws IOException, PGPException {
        keyStore.deletePublicKeyRing(owner, fingerprint);
    }

    @Override
    public void deleteSecretKeyRing(BareJid owner, OpenPgpV4Fingerprint fingerprint) throws IOException, PGPException {
        keyStore.deleteSecretKeyRing(owner, fingerprint);
    }

    protected AbstractOpenPgpStore(OpenPgpKeyStore keyStore,
                                   OpenPgpMetadataStore metadataStore,
                                   OpenPgpTrustStore trustStore) {
        this.keyStore = Objects.requireNonNull(keyStore);
        this.metadataStore = Objects.requireNonNull(metadataStore);
        this.trustStore = Objects.requireNonNull(trustStore);
    }

    @Override
    public OpenPgpContact getOpenPgpContact(BareJid jid) {
        OpenPgpContact contact = contacts.get(jid);
        if (contact == null) {
            contact = new OpenPgpContact(jid, this);
            contacts.put(jid, contact);
        }
        return contact;
    }

    @Override
    public void setKeyRingProtector(SecretKeyRingProtector protector) {
        this.unlocker = protector;
    }

    @Override
    public SecretKeyRingProtector getKeyRingProtector() {
        return unlocker;
    }

    @Override
    public void setSecretKeyPassphraseCallback(SecretKeyPassphraseCallback callback) {
        this.secretKeyPassphraseCallback = callback;
    }

    /*
    OpenPgpKeyStore
     */

    @Override
    public PGPPublicKeyRingCollection getPublicKeysOf(BareJid owner) throws IOException, PGPException {
        return keyStore.getPublicKeysOf(owner);
    }

    @Override
    public PGPSecretKeyRingCollection getSecretKeysOf(BareJid owner) throws IOException, PGPException {
        return keyStore.getSecretKeysOf(owner);
    }

    @Override
    public PGPPublicKeyRing getPublicKeyRing(BareJid owner, OpenPgpV4Fingerprint fingerprint) throws IOException, PGPException {
        return keyStore.getPublicKeyRing(owner, fingerprint);
    }

    @Override
    public PGPSecretKeyRing getSecretKeyRing(BareJid owner, OpenPgpV4Fingerprint fingerprint) throws IOException, PGPException {
        return keyStore.getSecretKeyRing(owner, fingerprint);
    }

    @Override
    public PGPKeyRing generateKeyRing(BareJid owner) throws PGPException, NoSuchAlgorithmException, NoSuchProviderException, InvalidAlgorithmParameterException {
        return keyStore.generateKeyRing(owner);
    }

    @Override
    public void importSecretKey(BareJid owner, PGPSecretKeyRing secretKeys) throws IOException, PGPException, MissingUserIdOnKeyException {
        keyStore.importSecretKey(owner, secretKeys);
    }

    @Override
    public void importPublicKey(BareJid owner, PGPPublicKeyRing publicKeys) throws IOException, PGPException, MissingUserIdOnKeyException {
        keyStore.importPublicKey(owner, publicKeys);
    }

    @Override
    public Map<OpenPgpV4Fingerprint, Date> getPublicKeyFetchDates(BareJid contact) throws IOException {
        return keyStore.getPublicKeyFetchDates(contact);
    }

    @Override
    public void setPublicKeyFetchDates(BareJid contact, Map<OpenPgpV4Fingerprint, Date> dates) throws IOException {
        keyStore.setPublicKeyFetchDates(contact, dates);
    }

    /*
    OpenPgpMetadataStore
     */

    @Override
    public Map<OpenPgpV4Fingerprint, Date> getAnnouncedFingerprintsOf(BareJid contact) throws IOException {
        return metadataStore.getAnnouncedFingerprintsOf(contact);
    }

    @Override
    public void setAnnouncedFingerprintsOf(BareJid contact, Map<OpenPgpV4Fingerprint, Date> data) throws IOException {
        metadataStore.setAnnouncedFingerprintsOf(contact, data);
    }

    /*
    OpenPgpTrustStore
     */

    @Override
    public Trust getTrust(BareJid owner, OpenPgpV4Fingerprint fingerprint) throws IOException {
        return trustStore.getTrust(owner, fingerprint);
    }

    @Override
    public void setTrust(BareJid owner, OpenPgpV4Fingerprint fingerprint, Trust trust) throws IOException {
        trustStore.setTrust(owner, fingerprint, trust);
    }
}
