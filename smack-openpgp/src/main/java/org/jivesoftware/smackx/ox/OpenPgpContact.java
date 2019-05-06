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
package org.jivesoftware.smackx.ox;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jivesoftware.smack.SmackException;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.util.stringencoder.Base64;

import org.jivesoftware.smackx.ox.element.PubkeyElement;
import org.jivesoftware.smackx.ox.element.PublicKeysListElement;
import org.jivesoftware.smackx.ox.exception.MissingUserIdOnKeyException;
import org.jivesoftware.smackx.ox.selection_strategy.BareJidUserId;
import org.jivesoftware.smackx.ox.store.definition.OpenPgpStore;
import org.jivesoftware.smackx.ox.store.definition.OpenPgpTrustStore;
import org.jivesoftware.smackx.ox.util.OpenPgpPubSubUtil;
import org.jivesoftware.smackx.pubsub.LeafNode;
import org.jivesoftware.smackx.pubsub.PubSubException;

import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.operator.bc.BcKeyFingerprintCalculator;
import org.jxmpp.jid.BareJid;
import org.pgpainless.key.OpenPgpV4Fingerprint;
import org.pgpainless.util.BCUtil;

/**
 * The OpenPgpContact is sort of a specialized view on the OpenPgpStore, which gives you access to the information
 * about the user. It also allows contact-specific actions like fetching the contacts keys from PubSub etc.
 */
public class OpenPgpContact {

    private final Logger LOGGER;

    protected final BareJid jid;
    protected final OpenPgpStore store;
    protected final Map<OpenPgpV4Fingerprint, Throwable> unfetchableKeys = new HashMap<>();

    /**
     * Create a new OpenPgpContact.
     *
     * @param jid {@link BareJid} of the contact.
     * @param store {@link OpenPgpStore}.
     */
    public OpenPgpContact(BareJid jid, OpenPgpStore store) {
        this.jid = jid;
        this.store = store;
        LOGGER = Logger.getLogger(OpenPgpContact.class.getName() + ":" + jid.toString());
    }

    /**
     * Return the jid of the contact.
     *
     * @return jid
     */
    public BareJid getJid() {
        return jid;
    }

    /**
     * Return any available public keys of the user. The result might also contain outdated or invalid keys.
     *
     * @return any keys of the contact.
     *
     * @throws IOException IO is dangerous
     * @throws PGPException PGP is brittle
     */
    public PGPPublicKeyRingCollection getAnyPublicKeys() throws IOException, PGPException {
        return store.getPublicKeysOf(jid);
    }

    /**
     * Return any announced public keys. This is the set returned by {@link #getAnyPublicKeys()} with non-announced
     * keys and keys which lack a user-id with the contacts jid removed.
     *
     * @return announced keys of the contact
     *
     * @throws IOException IO is dangerous
     * @throws PGPException PGP is brittle
     */
    public PGPPublicKeyRingCollection getAnnouncedPublicKeys() throws IOException, PGPException {
        PGPPublicKeyRingCollection anyKeys = getAnyPublicKeys();
        Map<OpenPgpV4Fingerprint, Date> announced = store.getAnnouncedFingerprintsOf(jid);

        BareJidUserId.PubRingSelectionStrategy userIdFilter = new BareJidUserId.PubRingSelectionStrategy();

        PGPPublicKeyRingCollection announcedKeysCollection = null;
        for (OpenPgpV4Fingerprint announcedFingerprint : announced.keySet()) {
            PGPPublicKeyRing ring = anyKeys.getPublicKeyRing(announcedFingerprint.getKeyId());

            if (ring == null) continue;

            ring = BCUtil.removeUnassociatedKeysFromKeyRing(ring, ring.getPublicKey(announcedFingerprint.getKeyId()));

            if (!userIdFilter.accept(getJid(), ring)) {
                LOGGER.log(Level.WARNING, "Ignore key " + Long.toHexString(ring.getPublicKey().getKeyID()) +
                        " as it lacks the user-id \"xmpp" + getJid().toString() + "\"");
                continue;
            }

            if (announcedKeysCollection == null) {
                announcedKeysCollection = new PGPPublicKeyRingCollection(Collections.singleton(ring));
            } else {
                announcedKeysCollection = PGPPublicKeyRingCollection.addPublicKeyRing(announcedKeysCollection, ring);
            }
        }

        return announcedKeysCollection;
    }

    /**
     * Return a {@link PGPPublicKeyRingCollection}, which contains all keys from {@code keys}, which are marked with the
     * {@link OpenPgpTrustStore.Trust} state of {@code trust}.
     *
     * @param keys {@link PGPPublicKeyRingCollection}
     * @param trust {@link OpenPgpTrustStore.Trust}
     *
     * @return all keys from {@code keys} with trust state {@code trust}.
     *
     * @throws IOException IO error
     */
    protected PGPPublicKeyRingCollection getPublicKeysOfTrustState(PGPPublicKeyRingCollection keys,
                                                                   OpenPgpTrustStore.Trust trust)
            throws IOException {

        if (keys == null) {
            return null;
        }

        Set<PGPPublicKeyRing> toRemove = new HashSet<>();
        Iterator<PGPPublicKeyRing> iterator = keys.iterator();
        while (iterator.hasNext()) {
            PGPPublicKeyRing ring = iterator.next();
            OpenPgpV4Fingerprint fingerprint = new OpenPgpV4Fingerprint(ring);
            if (store.getTrust(getJid(), fingerprint) != trust) {
                toRemove.add(ring);
            }
        }

        for (PGPPublicKeyRing ring : toRemove) {
            keys = PGPPublicKeyRingCollection.removePublicKeyRing(keys, ring);
        }

        if (!keys.iterator().hasNext()) {
            return null;
        }

        return keys;
    }

    /**
     * Return a {@link PGPPublicKeyRingCollection} which contains all public keys of the contact, which are announced,
     * as well as marked as {@link OpenPgpStore.Trust#trusted}.
     *
     * @return announced, trusted keys.
     *
     * @throws IOException IO error
     * @throws PGPException PGP error
     */
    public PGPPublicKeyRingCollection getTrustedAnnouncedKeys()
            throws IOException, PGPException {
        PGPPublicKeyRingCollection announced = getAnnouncedPublicKeys();
        PGPPublicKeyRingCollection trusted = getPublicKeysOfTrustState(announced, OpenPgpTrustStore.Trust.trusted);
        return trusted;
    }

    /**
     * Return a {@link Set} of {@link OpenPgpV4Fingerprint}s of all keys of the contact, which have the trust state
     * {@link OpenPgpStore.Trust#trusted}.
     *
     * @return trusted fingerprints
     *
     * @throws IOException IO error
     * @throws PGPException PGP error
     */
    public Set<OpenPgpV4Fingerprint> getTrustedFingerprints()
            throws IOException, PGPException {
        return getFingerprintsOfKeysWithState(getAnyPublicKeys(), OpenPgpTrustStore.Trust.trusted);
    }

    /**
     * Return a {@link Set} of {@link OpenPgpV4Fingerprint}s of all keys of the contact, which have the trust state
     * {@link OpenPgpStore.Trust#untrusted}.
     *
     * @return untrusted fingerprints
     *
     * @throws IOException IO error
     * @throws PGPException PGP error
     */
    public Set<OpenPgpV4Fingerprint> getUntrustedFingerprints()
            throws IOException, PGPException {
        return getFingerprintsOfKeysWithState(getAnyPublicKeys(), OpenPgpTrustStore.Trust.untrusted);
    }

    /**
     * Return a {@link Set} of {@link OpenPgpV4Fingerprint}s of all keys of the contact, which have the trust state
     * {@link OpenPgpStore.Trust#undecided}.
     *
     * @return undecided fingerprints
     *
     * @throws IOException IO error
     * @throws PGPException PGP error
     */
    public Set<OpenPgpV4Fingerprint> getUndecidedFingerprints()
            throws IOException, PGPException {
        return getFingerprintsOfKeysWithState(getAnyPublicKeys(), OpenPgpTrustStore.Trust.undecided);
    }

    /**
     * Return a {@link Set} of {@link OpenPgpV4Fingerprint}s of all keys in {@code publicKeys}, which are marked with the
     * {@link OpenPgpTrustStore.Trust} of {@code trust}.
     *
     * @param publicKeys {@link PGPPublicKeyRingCollection} of keys which are iterated.
     * @param trust {@link OpenPgpTrustStore.Trust} state.
     * @return {@link Set} of fingerprints
     *
     * @throws IOException IO error
     */
    public Set<OpenPgpV4Fingerprint> getFingerprintsOfKeysWithState(PGPPublicKeyRingCollection publicKeys,
                                                                    OpenPgpTrustStore.Trust trust)
            throws IOException {
        PGPPublicKeyRingCollection keys = getPublicKeysOfTrustState(publicKeys, trust);
        Set<OpenPgpV4Fingerprint> fingerprints = new HashSet<>();

        if (keys == null) {
            return fingerprints;
        }

        for (PGPPublicKeyRing ring : keys) {
            fingerprints.add(new OpenPgpV4Fingerprint(ring));
        }

        return fingerprints;
    }

    /**
     * Determine the {@link OpenPgpTrustStore.Trust} state of the key identified by the {@code fingerprint}.
     *
     * @param fingerprint {@link OpenPgpV4Fingerprint} of the key
     * @return trust record
     *
     * @throws IOException IO error
     */
    public OpenPgpTrustStore.Trust getTrust(OpenPgpV4Fingerprint fingerprint)
            throws IOException {
        return store.getTrust(getJid(), fingerprint);
    }

    /**
     * Determine, whether the key identified by the {@code fingerprint} is marked as
     * {@link OpenPgpTrustStore.Trust#trusted} or not.
     *
     * @param fingerprint {@link OpenPgpV4Fingerprint} of the key
     * @return true, if the key is marked as trusted, false otherwise
     *
     * @throws IOException IO error
     */
    public boolean isTrusted(OpenPgpV4Fingerprint fingerprint)
            throws IOException {
        return getTrust(fingerprint) == OpenPgpTrustStore.Trust.trusted;
    }

    /**
     * Mark a key as {@link OpenPgpStore.Trust#trusted}.
     *
     * @param fingerprint {@link OpenPgpV4Fingerprint} of the key to mark as trusted.
     *
     * @throws IOException IO error
     */
    public void trust(OpenPgpV4Fingerprint fingerprint)
            throws IOException {
        store.setTrust(getJid(), fingerprint, OpenPgpTrustStore.Trust.trusted);
    }

    /**
     * Mark a key as {@link OpenPgpStore.Trust#untrusted}.
     *
     * @param fingerprint {@link OpenPgpV4Fingerprint} of the key to mark as untrusted.
     *
     * @throws IOException IO error
     */
    public void distrust(OpenPgpV4Fingerprint fingerprint)
            throws IOException {
        store.setTrust(getJid(), fingerprint, OpenPgpTrustStore.Trust.untrusted);
    }

    /**
     * Determine, whether there are keys available, for which we did not yet decided whether to trust them or not.
     *
     * @return more than 0 keys with trust state {@link OpenPgpTrustStore.Trust#undecided}.
     *
     * @throws IOException I/O error reading the keys or trust records.
     * @throws PGPException PGP error reading the keys.
     */
    public boolean hasUndecidedKeys()
            throws IOException, PGPException {
        return getUndecidedFingerprints().size() != 0;
    }

    /**
     * Return a {@link Map} of any unfetchable keys fingerprints and the cause of them not being fetched.
     *
     * @return unfetchable keys
     */
    public Map<OpenPgpV4Fingerprint, Throwable> getUnfetchableKeys() {
        return new HashMap<>(unfetchableKeys);
    }

    /**
     * Update the contacts keys by consulting the users PubSub nodes.
     * This method fetches the users metadata node and then tries to fetch any announced keys.
     *
     * @param connection our {@link XMPPConnection}.
     *
     * @throws InterruptedException In case the thread gets interrupted.
     * @throws SmackException.NotConnectedException in case the connection is not connected.
     * @throws SmackException.NoResponseException in case the server doesn't respond.
     * @throws XMPPException.XMPPErrorException in case of an XMPP protocol error.
     * @throws PubSubException.NotALeafNodeException in case the metadata node is not a {@link LeafNode}.
     * @throws PubSubException.NotAPubSubNodeException in case the metadata node is not a PubSub node.
     * @throws IOException IO is brittle.
     */
    public void updateKeys(XMPPConnection connection) throws InterruptedException, SmackException.NotConnectedException,
            SmackException.NoResponseException, XMPPException.XMPPErrorException, PubSubException.NotALeafNodeException,
            PubSubException.NotAPubSubNodeException, IOException {
        PublicKeysListElement metadata = OpenPgpPubSubUtil.fetchPubkeysList(connection, getJid());
        if (metadata == null) {
            return;
        }

        updateKeys(connection, metadata);
    }

    /**
     * Update the contacts keys using a prefetched {@link PublicKeysListElement}.
     *
     * @param connection our {@link XMPPConnection}.
     * @param metadata pre-fetched OX metadata node of the contact.
     *
     * @throws InterruptedException in case the thread gets interrupted.
     * @throws SmackException.NotConnectedException in case the connection is not connected.
     * @throws SmackException.NoResponseException in case the server doesn't respond.
     * @throws IOException IO is dangerous.
     */
    public void updateKeys(XMPPConnection connection, PublicKeysListElement metadata)
            throws InterruptedException, SmackException.NotConnectedException, SmackException.NoResponseException,
            IOException {

        Map<OpenPgpV4Fingerprint, Date> fingerprintsAndDates = new HashMap<>();
        for (OpenPgpV4Fingerprint fingerprint : metadata.getMetadata().keySet()) {
            fingerprintsAndDates.put(fingerprint, metadata.getMetadata().get(fingerprint).getDate());
        }

        store.setAnnouncedFingerprintsOf(getJid(), fingerprintsAndDates);
        Map<OpenPgpV4Fingerprint, Date> fetchDates = store.getPublicKeyFetchDates(getJid());

        for (OpenPgpV4Fingerprint fingerprint : metadata.getMetadata().keySet()) {
            Date fetchDate = fetchDates.get(fingerprint);
            if (fetchDate != null && fingerprintsAndDates.get(fingerprint) != null && fetchDate.after(fingerprintsAndDates.get(fingerprint))) {
                LOGGER.log(Level.FINE, "Skip key " + Long.toHexString(fingerprint.getKeyId()) + " as we already have the most recent version. " +
                        "Last announced: " + fingerprintsAndDates.get(fingerprint).toString() + " Last fetched: " + fetchDate.toString());
                continue;
            }
            try {
                PubkeyElement key = OpenPgpPubSubUtil.fetchPubkey(connection, getJid(), fingerprint);
                unfetchableKeys.remove(fingerprint);
                fetchDates.put(fingerprint, new Date());
                if (key == null) {
                    LOGGER.log(Level.WARNING, "Public key " + Long.toHexString(fingerprint.getKeyId()) +
                            " can not be imported: Is null");
                    unfetchableKeys.put(fingerprint, new NullPointerException("Public key is null."));
                    continue;
                }
                PGPPublicKeyRing keyRing = new PGPPublicKeyRing(Base64.decode(key.getDataElement().getB64Data()), new BcKeyFingerprintCalculator());
                store.importPublicKey(getJid(), keyRing);
            } catch (PubSubException.NotAPubSubNodeException | PubSubException.NotALeafNodeException |
                    XMPPException.XMPPErrorException e) {
                LOGGER.log(Level.WARNING, "Error fetching public key " + Long.toHexString(fingerprint.getKeyId()), e);
                unfetchableKeys.put(fingerprint, e);
            } catch (PGPException | IOException e) {
                LOGGER.log(Level.WARNING, "Public key " + Long.toHexString(fingerprint.getKeyId()) +
                        " can not be imported.", e);
                unfetchableKeys.put(fingerprint, e);
            } catch (MissingUserIdOnKeyException e) {
                LOGGER.log(Level.WARNING, "Public key " + Long.toHexString(fingerprint.getKeyId()) +
                        " is missing the user-id \"xmpp:" + getJid() + "\". Refuse to import it.", e);
                unfetchableKeys.put(fingerprint, e);
            }
        }
        store.setPublicKeyFetchDates(getJid(), fetchDates);
    }
}
