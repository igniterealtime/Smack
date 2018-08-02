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

import org.jivesoftware.smackx.ox.store.definition.OpenPgpStore;

import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPSecretKeyRing;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.jxmpp.jid.BareJid;
import org.pgpainless.key.OpenPgpV4Fingerprint;
import org.pgpainless.util.BCUtil;

/**
 * This class acts as our own OpenPGP identity. It can be seen as a special view on the {@link OpenPgpStore}, giving
 * access to our own encryption keys etc.
 */
public class OpenPgpSelf extends OpenPgpContact {

    /**
     * Constructor.
     *
     * @param jid our own {@link BareJid}. This is needed to access our keys in the store.
     * @param store the store.
     */
    OpenPgpSelf(BareJid jid, OpenPgpStore store) {
        super(jid, store);
    }

    /**
     * Return true, if we have a usable secret key available.
     * @return true if we have secret key, otherwise false.
     * @throws IOException IO is dangerous
     * @throws PGPException PGP is brittle
     */
    public boolean hasSecretKeyAvailable() throws IOException, PGPException {
        return getSecretKeys() != null;
    }

    /**
     * Return a {@link PGPSecretKeyRingCollection} which contains all of our {@link PGPSecretKeyRing}s.
     * @return collection of our secret keys
     * @throws IOException IO is dangerous
     * @throws PGPException PGP is brittle
     */
    public PGPSecretKeyRingCollection getSecretKeys() throws IOException, PGPException {
        return store.getSecretKeysOf(jid);
    }

    /**
     * Return the {@link PGPSecretKeyRing} which we will use to sign our messages.
     * @return signing key
     * @throws IOException IO is dangerous
     * @throws PGPException PGP is brittle
     */
    public PGPSecretKeyRing getSigningKeyRing() throws IOException, PGPException {
        PGPSecretKeyRingCollection secretKeyRings = getSecretKeys();
        if (secretKeyRings == null) {
            return null;
        }

        PGPSecretKeyRing signingKeyRing = null;
        for (PGPSecretKeyRing ring : secretKeyRings) {
            if (signingKeyRing == null) {
                signingKeyRing = ring;
                continue;
            }

            if (ring.getPublicKey().getCreationTime().after(signingKeyRing.getPublicKey().getCreationTime())) {
                signingKeyRing = ring;
            }
        }

        return signingKeyRing;
    }

    /**
     * Return the {@link OpenPgpV4Fingerprint} of our signing key.
     * @return fingerprint of signing key
     * @throws IOException IO is dangerous
     * @throws PGPException PGP is brittle
     */
    public OpenPgpV4Fingerprint getSigningKeyFingerprint() throws IOException, PGPException {
        PGPSecretKeyRing signingKeyRing = getSigningKeyRing();
        return signingKeyRing != null ? new OpenPgpV4Fingerprint(signingKeyRing.getPublicKey()) : null;
    }

    /**
     * Return a {@link PGPPublicKeyRingCollection} containing only the public keys belonging to our signing key ring.
     * TODO: Add support for public keys of other devices of the owner.
     *
     * @return public keys
     *
     * @throws IOException IO is dangerous.
     * @throws PGPException PGP is brittle.
     */
    @Override
    public PGPPublicKeyRingCollection getAnnouncedPublicKeys() throws IOException, PGPException {
        PGPSecretKeyRing secretKeys = getSigningKeyRing();
        PGPPublicKeyRing publicKeys = getAnyPublicKeys().getPublicKeyRing(secretKeys.getPublicKey().getKeyID());
        publicKeys = BCUtil.removeUnassociatedKeysFromKeyRing(publicKeys, secretKeys.getPublicKey());
        return new PGPPublicKeyRingCollection(Collections.singleton(publicKeys));
    }
}
