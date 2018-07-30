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
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.jivesoftware.smackx.ox.store.definition.OpenPgpMetadataStore;

import org.jxmpp.jid.BareJid;
import org.pgpainless.key.OpenPgpV4Fingerprint;

public abstract class AbstractOpenPgpMetadataStore implements OpenPgpMetadataStore {

    private final Map<BareJid, Map<OpenPgpV4Fingerprint, Date>> announcedFingerprints = new HashMap<>();

    @Override
    public Map<OpenPgpV4Fingerprint, Date> getAnnouncedFingerprintsOf(BareJid contact) throws IOException {
        Map<OpenPgpV4Fingerprint, Date> fingerprints = announcedFingerprints.get(contact);
        if (fingerprints == null) {
            fingerprints = readAnnouncedFingerprintsOf(contact);
            announcedFingerprints.put(contact, fingerprints);
        }
        return fingerprints;
    }

    @Override
    public void setAnnouncedFingerprintsOf(BareJid contact, Map<OpenPgpV4Fingerprint, Date> data) throws IOException {
        announcedFingerprints.put(contact, data);
        writeAnnouncedFingerprintsOf(contact, data);
    }

    /**
     * Read the fingerprints and modification dates of announced keys of a user from local storage.
     *
     * @param contact contact
     * @return contacts announced key fingerprints and latest modification dates
     *
     * @throws IOException IO is dangerous
     */
    protected abstract Map<OpenPgpV4Fingerprint, Date> readAnnouncedFingerprintsOf(BareJid contact) throws IOException;

    /**
     * Write the fingerprints and modification dates of announced keys of a user to local storage.
     *
     * @param contact contact
     * @param metadata announced key fingerprints and latest modification dates
     *
     * @throws IOException IO is dangerous
     */
    protected abstract void writeAnnouncedFingerprintsOf(BareJid contact, Map<OpenPgpV4Fingerprint, Date> metadata) throws IOException;
}
